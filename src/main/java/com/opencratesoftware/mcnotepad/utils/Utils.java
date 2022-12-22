package com.opencratesoftware.mcnotepad.utils;

import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import com.opencratesoftware.mcnotepad.FunctionResult;
import com.opencratesoftware.mcnotepad.structs.CommandData;

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
        
        returnValue = clampStringCharacterCount(returnValue, Config.getMaxCharactersPerLine());

        return returnValue;
    }

    public static String clampStringCharacterCount(String string, int max)
    {
        if (string.length() > max)
        {
            return string.substring(0, max);
        }
        return string;
    }

    public static String whitelistFilterString(String string)
    {
        StringBuilder returnValue = new StringBuilder(string);
        for (int i = 0; i < string.length(); i++) 
        {
            if (!isCharWhitelisted(returnValue.charAt(i)))
            {
                returnValue.setCharAt(i, '_');
            }
        }

        return returnValue.toString();
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

    public static boolean isCharFilenameWhitelisted(Character character)
    {
        if (!Config.getUseFilenameCharacterWhitelist())
        {
            return true;
        }
        
        for (Character currentChar : Config.getFilenameCharacterWhitelist()) 
        {   
            if (currentChar == character)
            {
                return true;
            }
        }
        return false;
    }

    public static String fileNameWhitelistFilterString(String string)
    {
        StringBuilder returnValue = new StringBuilder(string);
        for (int i = 0; i < string.length(); i++) 
        {
            if (!isCharFilenameWhitelisted(returnValue.charAt(i)))
            {
                returnValue.setCharAt(i, '_');
            }
        }

        return returnValue.toString();
    }

    public static boolean isValidUsername(String name)
    {
        if (name.length() < 3 || name.length() > 16)
        {
            return false;
        }
        for (int i = 0; i < name.length(); i++) 
        {
            Character[] allowedUsernameChars = {'_', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9','A','B','C','D','E','F','G','H','I','J','K','L','M','N','O','P','Q','R','S','T','U','V','W','X','Y','Z','a','b','c','d','e','f','g','h','i','j','k','l','m','n','o','p','q','r','s','t','u','v','w','x','y','z'};
            boolean isAllowedChar = false;
            for (int j = 0; j < allowedUsernameChars.length; j++) 
            {
                if (name.charAt(i) == allowedUsernameChars[j])
                {
                    isAllowedChar = true;
                }    
            }
            if (!isAllowedChar)
            {
                return false;
            }
        }

        return true;
    }
    // gets UUID from player username
    public static UUID getNameUUID(String name)
    {
        if (!isValidUsername(name))
        {
            return UUID.fromString("00000000-0000-0000-0000-000000000000");
        }
        
        UUID returnValue = Bukkit.getServer().getPlayerUniqueId(name);

        if (returnValue == null)
        {
            return UUID.fromString("00000000-0000-0000-0000-000000000000");
        }
        return returnValue;
    }

    // gets UUID from CommandSender type
    public static UUID getSenderUUID(CommandSender sender)
    {
        return getNameUUID(sender.getName());
    }

    public static Player getPlayerFromUUID(UUID uuid)
    {
        return Bukkit.getServer().getPlayer(uuid);
    }

    public static Player getPlayerFromSender(CommandSender sender)
    {
        return getPlayerFromUUID(getSenderUUID(sender));
    }

    public static boolean isIntString(String str)
    {   
        String numberChars = "-0123456789";

        for (int i = 0; i < str.length(); i++) 
        {
            String character = "";

            character = character + str.charAt(i);
            
            if(!numberChars.contains(character))
            {
                return false;
            }
        }
        return true;
    }

    // retrieves note file as the sender, checking trustlists, altdirs and the like. Can return a non-existent file if the file is not found or user does not have required trust.
    public static File getNoteFile(CommandSender sender, String noteName, boolean IgnoreAltDirs)
    {   
        noteName = fileNameWhitelistFilterString(noteName);
        if(noteName.contains(":")) // check if noteName specifies a directory to search through
        {
            String NoteOwnerName = noteName.substring(0, noteName.indexOf(":"));
            noteName = clampStringCharacterCount(noteName, Config.getMaxFilenameCharacters() + NoteOwnerName.length() + 1);
            if(NoteOwnerName.length() < 28) // check if passed as a UUID (should be about 37 chars?) or username (always less than or equal to 16)
            {
                return new File(getNotesDir(Utils.getPlayerFromSender(sender).getWorld()) + getNameUUID(NoteOwnerName), noteName.substring( Math.min (noteName.indexOf(":") + 1, noteName.length() - 1)) + getNoteExt());
            }
            else // if using UUID
            {
                return new File(getNotesDir(Utils.getPlayerFromSender(sender).getWorld()) + NoteOwnerName, noteName.substring( Math.min (noteName.indexOf(":") + 1, noteName.length() - 1)) + getNoteExt());
            }
        }

        // code executing past here means that no directory was specified to search in

        noteName = clampStringCharacterCount(noteName, Config.getMaxFilenameCharacters());

        File file = new File(getNotesDir(Utils.getPlayerFromSender(sender).getWorld()) + getNameUUID(sender.getName()), noteName + getNoteExt()); // set 'file' to point to a file possibly inside of the user's note directory named the given note name
        
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
    public static String getNotesDir(World world)
    {
        return getDataDir() + "/" + world.getName() + "/" + "/notes/";
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

        if (lineIndex == 0)
        {
            if((newLinePos = returnValue.indexOf('\n', newLinePos + 1)) != -1)
            {
                String contentsPart1 = returnValue.substring(newLinePos + 1);
                if(newLinePos != -1)
                {
                    returnValue = contentsPart1;
                }
                return returnValue;
            }
            else
            {
                return ""; // this means there is only one line, which is the line we were requested to remove
            }
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

    public static String mergeArray(Object[] array, String seperator, boolean ignoreEmptyStrings)
    {
        if(array.length <= 0) { return ""; }
    
        String returnValue = array[0].toString();

        for (int i = 1; i < array.length; i++)
        {
            String str = array[i].toString();

            if (str.equals("") && ignoreEmptyStrings) { continue; }

            returnValue += seperator + str;
        }

        return returnValue;
    }

    public static String mergeArray(Object[] array, String seperator)
    {
        return mergeArray(array, seperator, false);
    }

    public static String removeAllFromStart(String str, String removeStr)
    {
        while(true)
        {  
            if (str.length() < removeStr.length()) { break; }
            if(str.indexOf(removeStr) != 0){ break; }
            str = str.substring(removeStr.length());
        }
        return str;
    }
    
    public static int findFirstOf(String str, String searchFor)
    {
        for (int i = 0; i < str.length(); i++) 
        {
            if (str.substring(i, searchFor.length()) == searchFor)
            {
                return i;
            }
        }
        return -1;
    }

    public static CommandData formatCommand(String command, String seperator)
    {
        CommandData returnValue = new CommandData();
        returnValue.params = new String[0];
        int currentParam = -1;
        while(command.length() > 0)
        {   
            String subStr;
            
            command = removeAllFromStart(command, seperator);
            
            if (command.length() <= 0) { break; }
            
            if (!command.contains(seperator))
            {
                subStr = command;
                command = "";
            }
            else
            {
                subStr = command.substring(0, command.indexOf(seperator));
                if (command.length() > command.indexOf(seperator) + 1)
                {
                    command = command.substring(command.indexOf(seperator));
                }
                else
                {
                    command = "";
                }
            }

            if (currentParam < 0) {returnValue.name = subStr; }
            else 
            {
                String[] newParams = new String[returnValue.params.length + 1];
                for (int i = 0; i < returnValue.params.length; i++) 
                {
                    newParams[i] = returnValue.params[i];    
                }

                newParams[currentParam] = subStr;

                returnValue.params = newParams;
            }

            currentParam++;
        }

        return returnValue;
    }

    public static int stringToInt(String str)
    {
        int returnValue = 0;
            
        if (str.length() > 0)
        {
            for (int i = 0; i < str.length(); i++)
            {
                switch (str.charAt(i))
                {
                case '0':
                returnValue = (returnValue * 10);
                    break;

                case '1':
                returnValue = (returnValue * 10) + 1;
                break;

                case '2':
                returnValue = (returnValue * 10) + 2;
                    break;

                case '3':
                returnValue = (returnValue * 10) + 3;
                    break;

                case '4':
                returnValue = (returnValue * 10) + 4;
                    break;

                case '5':
                returnValue = (returnValue * 10) + 5;
                break;

                case '6':
                returnValue = (returnValue * 10) + 6;
                    break;

                case '7':
                returnValue = (returnValue * 10) + 7;
                    break;

                case '8':
                returnValue = (returnValue * 10) + 8;
                    break;

                case '9':
                returnValue = (returnValue * 10) + 9;
                    break;
                }
            }

            if (str.charAt(0) == '-')
            {
                returnValue = returnValue * -1;
            }
        }

        return returnValue;
    }

    public static int findInArray(Object[] array, Object searchFor)
    {
        for (int i = 0; i < array.length; i++) 
        {
            if (array[i] != null)
            {
                if(array[i].equals(searchFor))
                {
                    return i;
                } 
            }
        }
        
        return -1;
    }
}
