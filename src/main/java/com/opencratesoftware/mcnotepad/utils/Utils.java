package com.opencratesoftware.mcnotepad.utils;

import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;

import com.opencratesoftware.mcnotepad.FunctionResult;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.logging.Level;

import net.md_5.bungee.api.ChatColor;

public class Utils 
{
    /////////////////////////
    /* Declare config vars */
    /////////////////////////

    private static boolean useWhiteList = true;

    private static List<Character> characterWhitelist;

    public static void log(String msg)
    {
        Bukkit.getLogger().log(Level.INFO, "[Notepad] " + msg);
    }

    public static void logError(String msg)
    {
        Bukkit.getLogger().log(Level.SEVERE, "[Notepad] " + msg);
    }

    public static FileConfiguration getConfig()
    {
        return Bukkit.getPluginManager().getPlugin("Notepad").getConfig();
    }

    public static boolean getUseWhitelist()
    {
        return useWhiteList;
    }

    public static List<?> getCharacterWhiteList()
    {
        return characterWhitelist;
    }

    public static void updateConfiguration()
    {
        useWhiteList = getConfig().getBoolean("use-character-whitelist");
        characterWhitelist = getConfig().getCharacterList("whitelisted-characters");
        Config.setConfigValues(getConfig());
    }

    public static String formatStringForNotes(String string)
    {      
        String returnValue = string;

        returnValue = whitelistFilterString(returnValue);

        return returnValue;
    }

    public static String whitelistFilterString(String string)
    {
        String returnValue = string;
        for (int i = 0; i < string.length(); i++) 
        {
            if (!isCharWhitelisted(returnValue.charAt(i)))
            {
                returnValue = returnValue.replaceAll(String.valueOf(returnValue.charAt(i)), "#");
            }
        }

        return returnValue;
    }

    public static boolean isCharWhitelisted(Character character)
    {
        if (!useWhiteList)
        {
            return true;
        }

        for (Character currentChar : characterWhitelist) 
        {
            if (currentChar == character)
            {
                return true;
            }
        }
        return false;
    }

        // gets UUID from player username
        public static UUID getNameUUID(String name)
        {
            if (Bukkit.getServer().getPlayerUniqueId(name) == null)
            {
                return new UUID(0, 0);
            }
            return Bukkit.getServer().getPlayerUniqueId(name);
        }
    
        // gets UUID from CommandSender type
        public static UUID getSenderUUID(CommandSender sender)
        {
            return getNameUUID(sender.getName());
        }

                // retrieves note file as the sender, checking trustlists, altdirs and the like. Can return a non-existent file if the file is not found or user does not have required trust.
                public static File getNoteFile(CommandSender sender, String noteName, boolean IgnoreAltDirs)
                {
                    if(noteName.contains(":")) // check if noteName specifies a directory to search through
                    {
                        String NoteOwnerName = noteName.substring(0, noteName.indexOf(":"));
                        if(NoteOwnerName.length() < 28) // check if passed as a UUID (should be about 37 chars?) or username (always less than 16)
                        {
                            if (playerTrustsPlayer(getNameUUID(sender.getName()), getNameUUID(NoteOwnerName)))
                            {
                                return new File(getNotesDir() + getNameUUID(NoteOwnerName), noteName.substring( Math.min (noteName.indexOf(":") + 1, noteName.length() - 1)) + getNoteExt());
                            }
                        }
                        else // if using UUID
                        {
                            if (playerTrustsPlayer(getNameUUID(sender.getName()), UUID.fromString(NoteOwnerName)))
                            {
                                return new File(getNotesDir() + NoteOwnerName, noteName.substring( Math.min (noteName.indexOf(":") + 1, noteName.length() - 1)) + getNoteExt());
                            }
                        }
                        
                        // if code is executing to this point then that means that the trust check failed, so return something that probably doesn't exist
                        // so that a file not found error is likely presented. 
                        return new File("getNotesDir()" + "ThisDirectoryProbablyDoesntExist/justlikethisdoesntprobably/alsothis/con/thisshouldbesafe.txt/incaseitisnt.exe/anddoublyso.app/andfinally.sh/done.finished//\\");
                        // random comment: it says "getNotesDir()" rather than getNotesDir() so it doesn't even actually look in the notes directory, wonderful.
                    }
            
                    // code executing past here means that no directory was specified to search in
            
                    File file = new File(getNotesDir() + getNameUUID(sender.getName()), noteName + getNoteExt()); // set 'file' to point to a file possibly inside of the user's note directory named the given note name
                    
                    if (!file.exists()) // check if the file exists
                    {
                        File altListFile = new File(getAltListsDir() + "/" + getSenderUUID(sender).toString()); // if the user has an alt list file this is where it would be so store that
                        if (!altListFile.exists() || IgnoreAltDirs) // if alt list file doesn't exist or we've been instructed to ignore it, return the file that doesn't exist intentionally
                        {
                            return file;
                        }
            
                        try
                        {
                            BufferedReader fileIn = new BufferedReader(new FileReader(altListFile));
                            String fileCurrentLine;
                            boolean stopSearching = false;
                            if ((fileCurrentLine = fileIn.readLine()) == null)
                            {
                                stopSearching = true;
                            }
                            while ( !stopSearching)
                            {   
                                file = getNoteFile(sender, fileCurrentLine + ":" + noteName, true); // call getNoteFile (yes, our current function) but specifying what directory to search through
                                if (file.exists())
                                {
                                    stopSearching = true; // if file is found, stop searching
                                }
            
                                if ((fileCurrentLine = fileIn.readLine()) == null)
                                {
                                    stopSearching = true; // if we reach the end of the altdir file and still are searching, stop searching (will just return the last non existent file)
                                }
                            }
                            fileIn.close();
                
                        }
                        catch (Exception e) 
                        {
                            sender.sendMessage(ChatColor.RED + "Error: Error information sent to logs.");
                            Bukkit.getLogger().log(Level.SEVERE, e.getMessage());
                            return file;
                        }
                    }
            
                    return file; // file exists if it executes here or execution decided to return an non existent file (likely meaning the command will give a file not found error)
                }
        
                    // returns a File that points to the directory where all altlists can be found
            public static File getAltListsDir()
            {
                File file = new File(getDataDir() + "/altlists/");
                if (!file.exists())
                {
                    file.mkdirs();
                }
                return file;
            }
        
            // returns a File that points to the directory where all trustlists can be found
            public static File getTrustListsDir()
            {
                File file = new File(getDataDir() + "/trustlists/");
                if (!file.exists())
                {
                    file.mkdirs();
                }
                return file;
            }
            
            // get a trustfile for a specific user
            public static File getUserTrustFile(UUID userUUID)
            {
                return new File(getTrustListsDir().toString() + "/" + userUUID.toString());
            }
        
    // get a altlist for a specific user
    public static File getUserAltFile(UUID userUUID)
    {
        return new File(getAltListsDir().toString() + "/" + userUUID.toString());
    }
        
    // returns a string in file format pointing to the data folder for the plugin
    public static String getDataDir()
    {
        return Bukkit.getServer().getPluginManager().getPlugin("Notepad").getDataFolder() + "/";
    }
            
    // gets the directory where all player notes folders can be found
    public static String getNotesDir()
    {
        return getDataDir() + "/notes/";
    }
            
    public static String getNoteExt() // this might be configurable eventually so use a getter to avoid having to update it everywhere
    {
        return "";
    }

    // returns true if the trustee is on the truster's trust list 
    public static boolean playerTrustsPlayer(UUID trusteeUUID, UUID trusterUUID)
    {
        if (trusteeUUID.equals(trusterUUID)) // immediately check if both UUIDs are the same, if they are then I sure would hope the user trusts themselves with their own notes
        {
            return true;
        }

        File file = getUserTrustFile(trusterUUID);
        if (file.exists())
        {
            try 
            {
                BufferedReader fileIn = new BufferedReader(new FileReader(file));
                String fileCurrentLine;
                boolean trustConfirmed = false;

                while (!trustConfirmed && ((fileCurrentLine = fileIn.readLine()) != null))
                {
                    if (fileCurrentLine.equalsIgnoreCase(trusteeUUID.toString()))
                    {
                        trustConfirmed = true;
                    }
                }

                fileIn.close();

                return trustConfirmed;
            } 
            catch (Exception e) 
            {
                
            }
        }

        return false;
    }

    public static FunctionResult setFileContents(String newContents, File file)
    {
        try 
        {
            BufferedWriter fileOut = new BufferedWriter(new FileWriter(file));
            fileOut.write(newContents);
            fileOut.close();
        } 
        catch (Exception e) 
        {
            logError(e.getMessage());
            return new FunctionResult(false, "Error: Error information sent to logs.");
        }
        return new FunctionResult(true, "");
    }

    public static boolean addLineToFileAt(String lineToAdd, File file, int lineIndex)
    {
        if (!file.exists())
        {
            log("Tried to add line to file \'" + file.toString() + "\' but file did not exist.");
            return false;
        }

        try 
        {
            boolean didAddLine = false;
            BufferedReader fileIn = new BufferedReader(new FileReader(file));
            String fileContent = "";
            String fileCurrentLine;
            int fileCurrentLineIndex = 0;

            while ((fileCurrentLine = fileIn.readLine()) != null)
            {
                if (fileCurrentLineIndex == lineIndex)
                {
                    fileContent += lineToAdd + "\n" + fileCurrentLine + "\n";
                    didAddLine = true;
                }
                else
                {
                    fileContent += fileCurrentLine + "\n";
                }
                fileCurrentLineIndex ++;
            }

            if (!didAddLine)
            {
                if(lineIndex < 0)
                {
                    fileContent = lineToAdd + fileContent;
                }
                else
                {
                    fileContent = fileContent + lineToAdd;
                }
            }

            fileIn.close();

            BufferedWriter fileOut = new BufferedWriter(new FileWriter(file));
            fileOut.write(fileContent);
            fileOut.close();

        } 
        catch (Exception e) 
        {
            logError(e.getMessage());
            return false;
        }

        return true;
    }

    public static String removeLineFromString(String string, int lineIndex)
    {
        int newLinePos = 0;
        int currentLineIndex = 0;
        
        String returnValue = string;

        if(lineIndex < 0)
        {
            return returnValue;
        }

        while ((newLinePos = returnValue.indexOf('\n', newLinePos + 1)) != -1) 
        {
            if(currentLineIndex == lineIndex - 1)
            {
                String contentsPart1 = returnValue.substring(0, newLinePos);
                int nextLinePos = returnValue.indexOf('\n', newLinePos + 1);
                if(nextLinePos == -1)
                {
                    returnValue = contentsPart1;
                }
                else
                {
                    returnValue = contentsPart1 + returnValue.substring(nextLinePos);
                }
                return returnValue;
            }
            currentLineIndex++;
        }
        return returnValue;
    }

}
