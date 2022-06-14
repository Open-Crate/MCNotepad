package com.opencratesoftware.commands;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Scanner;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import net.md_5.bungee.api.ChatColor;

public class NotepadCommand implements CommandExecutor
{
    // returns a File that points to the directory where all altlists can be found
    private File getAltListsDir()
    {
        File file = new File(getDataDir() + "/altlists/");
        if (!file.exists())
        {
            file.mkdirs();
        }
        return file;
    }

    // returns a File that points to the directory where all trustlists can be found
    private File getTrustListsDir()
    {
        File file = new File(getDataDir() + "/trustlists/");
        if (!file.exists())
        {
            file.mkdirs();
        }
        return file;
    }
    
    // get a trustfile for a specific user
    private File getUserTrustFile(UUID userUUID)
    {
        return new File(getTrustListsDir().toString() + "/" + userUUID.toString());
    }

    // get a altlist for a specific user
    private File getUserAltFile(UUID userUUID)
    {
        return new File(getAltListsDir().toString() + "/" + userUUID.toString());
    }

    // returns true if the trustee is on the truster's trust list 
    private boolean playerTrustsPlayer(UUID trusteeUUID, UUID trusterUUID)
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

    // gets UUID from player username
    private UUID getNameUUID(String name)
    {
        if (Bukkit.getServer().getPlayerUniqueId(name) == null)
        {
            return new UUID(0, 0);
        }
        return Bukkit.getServer().getPlayerUniqueId(name);
    }

    // gets UUID from CommandSender type
    private UUID getSenderUUID(CommandSender sender)
    {
        return getNameUUID(sender.getName());
    }

    // returns a string in file format pointing to the data folder for the plugin
    private String getDataDir()
    {
        return Bukkit.getServer().getPluginManager().getPlugin("Notepad").getDataFolder() + "/";
    }

    // gets the directory where all player notes folders can be found
    private String getNotesDir()
    {
        return getDataDir() + "/notes/";
    }

    private String getNoteExt() // this might be configurable eventually so use a getter to avoid having to update it everywhere
    {
        return "";
    }

    // retrieves note file as the sender, checking trustlists, altdirs and the like. Can return a non-existent file if the file is not found or user does not have required trust.
    private File getNoteFile(CommandSender sender, String noteName, boolean IgnoreAltDirs)
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

    
    /* File Management */

    private void listAction(CommandSender sender, String[] args)
    {
        File notesDirectory = new File(getNotesDir() + getSenderUUID(sender));
        File[] notes = notesDirectory.listFiles();
        for (File file : notes) 
        {
            sender.sendMessage(sender.getName() + ":" + file.getName().replace(getNoteExt(), ""));    
        }
    }

    private void newFileAction(CommandSender sender, String[] args)
    {
        if(args.length == 1)
        {
            sender.sendMessage(ChatColor.RED + "Usage: /notepad new <New Note Name> or Usage: /notepad new <New Note Name> <Note Type>");
            return;
        }
       
        File file = getNoteFile(sender, args[1], true);
        
        if(!getSenderUUID(sender).equals(UUID.fromString(file.getParentFile().getName())))
        {
            sender.sendMessage(ChatColor.RED + "Cannot create files for other players.");
            return;
        }

        if (file.exists())
        {
            sender.sendMessage(ChatColor.RED + "File already exists");
        }
        else
        {
            String type = "list";
            try 
            {
                file.getParentFile().mkdirs();
                file.createNewFile();
                if (args.length > 2)
                {

                    if(args[2].equalsIgnoreCase("list"))
                    {
                        type = "list";
                    }
                    else if(args[2].equalsIgnoreCase("note"))
                    {
                        type = "note";
                    }
                }

                BufferedWriter fileOut = new BufferedWriter(new FileWriter(file));
                fileOut.write(type);
                fileOut.close();
                    
                
                sender.sendMessage(ChatColor.GREEN + "Successfully created new " + type + ".");
            } 
            catch (Exception e) 
            {
                sender.sendMessage(ChatColor.RED + "Error: Failed to create file. Error information sent to logs");
                Bukkit.getLogger().log(Level.SEVERE, e.getMessage());
            }
        }
    }

    private void deleteFileAction(CommandSender sender, String[] args)
    {
        if (args.length < 2)
        {
            sender.sendMessage(ChatColor.RED + "Usage: /notepad delete <Note Name>");
        }
        File file = getNoteFile(sender, args[1], true);
        if (UUID.fromString(file.getParentFile().getName()).equals(getSenderUUID(sender))) // do not allow users to delete files not in their directory.
        {
            if (file.exists())
            {
                file.delete();
                sender.sendMessage(ChatColor.GREEN + "Successfully deleted note.");
            }
            else
            {
                sender.sendMessage(ChatColor.RED + "Could not find file.");
            }
        }
        else
        {
            sender.sendMessage(ChatColor.RED + "Did not delete note. Did not detect command user as owner.");
            return;
        }
    }

    /* File editing/viewing */

    private void viewAction(CommandSender sender, String[] args)
    {
        if(args.length < 2)
        {
            sender.sendMessage(ChatColor.RED + "Usage: /notepad view <Note Name> <Should Show Line Numbers>");
            return;
        }
        
        Player player = (Player) sender;

        File file = getNoteFile(sender, args[1], false);

        if (!file.exists())
        {
            sender.sendMessage(ChatColor.RED + "Failed to find file.");
            return;
        }

        String contents = new String();
        try 
        {
            boolean showLineNumbers = false;

            FileReader fileReader = new FileReader(file);
            Scanner in = new Scanner(fileReader);

            String Type = in.nextLine();
            if (Type.equalsIgnoreCase("list"))
            {
                showLineNumbers = true;
            }
            if (args.length > 2)
            {
                if (args[2].equalsIgnoreCase("true"))
                {
                    showLineNumbers = true;
                }
                else
                {
                    showLineNumbers = false;
                }
            }

            int currentLineNumber = 0;
            player.sendMessage("--------------------------------------------------");
            while (in.hasNext()) 
            {
                String currentLine = new String();
                if (showLineNumbers)
                {
                    currentLine = currentLineNumber + ". " + in.nextLine();
                }
                else
                {
                    currentLine = in.nextLine();
                }

                contents += "\n" + currentLine;

                player.sendMessage(currentLine);
                currentLineNumber++;
            }
            player.sendMessage("--------------------------------------------------");
            fileReader.close();
            in.close();

        } catch (Exception e) 
        {
          
        }

    }

    private void addAction(CommandSender sender, String[] args)
    {
        if (args.length < 3)
        {
            sender.sendMessage(ChatColor.RED + "Usage: /notepad add <note name> <note text>");
            return;
        }

        File file = getNoteFile(sender, args[1], false);

        if (!file.exists())
        {
            sender.sendMessage(ChatColor.RED + "Failed to find file.");
            return;
        }

        String noteText = "";

        for (int i = 2; i < args.length; i++) 
        {
            noteText += args[i];
            if (i < args.length - 1)
            {
                noteText += " ";
            }
        }
        try 
        {
 
            BufferedReader fileIn = new BufferedReader(new FileReader(file));
            String fileContent = "";
            String fileCurrentLine;
            while ((fileCurrentLine = fileIn.readLine()) != null)
            {
                fileContent += fileCurrentLine + "\n";
            }
            fileIn.close();

            BufferedWriter fileOut = new BufferedWriter(new FileWriter(file));
            fileOut.write(fileContent + noteText);
            fileOut.close();  
                
            sender.sendMessage(ChatColor.GREEN + "Successfully added '" + noteText + "' to note '" + args[1] + "'.");
        } 
        catch (Exception e) 
        {
            sender.sendMessage(ChatColor.RED + "Error: Failed to write to file. Error information sent to logs.");
            Bukkit.getLogger().log(Level.SEVERE, e.getMessage());
        }
        
    }

    private void removeLineAction(CommandSender sender, String[] args, boolean Silent)
    {
        if ((args.length < 3) || (args[1].equalsIgnoreCase("help")))
        {
            if (!Silent)
            {
                sender.sendMessage(ChatColor.RED + "Usage: /notepad removeline <note name> <line number>");
                sender.sendMessage(ChatColor.GOLD + "Tip: Use '/notepad view <note name> true' to see line numbers on non-list files.");
            }
            return;
        }
        
        File file = getNoteFile(sender, args[1], false);

        if (!file.exists())
        {
            if (!Silent)
                sender.sendMessage(ChatColor.RED + "Could not find file.");
            return;
        }

        try 
        {
            String removedLine = null;
            BufferedReader fileIn = new BufferedReader(new FileReader(file));
            String fileContent = "";
            String fileCurrentLine;
            int fileCurrentLineIndex = 0;
            while ((fileCurrentLine = fileIn.readLine()) != null)
            {
                if (fileCurrentLineIndex == Integer.parseInt(args[2]) + 1) // add 1 because the first line is always the note type
                {
                    removedLine = fileCurrentLine;
                }
                else
                {
                    fileContent += fileCurrentLine + "\n";
                }
                fileCurrentLineIndex ++;
            }
            fileIn.close();

            BufferedWriter fileOut = new BufferedWriter(new FileWriter(file));
            fileOut.write(fileContent);
            fileOut.close();  
            if (removedLine == null)
            {
                if (!Silent)
                    sender.sendMessage(ChatColor.RED + "Did not remove line.");
            }
            else
            {
                if (!Silent)
                    sender.sendMessage(ChatColor.GREEN + "Successfully removed line '" + removedLine + "' from note '" + args[1] + "'.");
            }

        } 
        catch (Exception e) 
        {
            sender.sendMessage(ChatColor.RED + "Error: Failed to write to file. Error information sent to logs.");
            Bukkit.getLogger().log(Level.SEVERE, e.getMessage());
        }

    }

    private void moveLineAction(CommandSender sender, String[] args)
    {
        if (args.length < 4)
        {
            sender.sendMessage(ChatColor.RED + "Usage: /notepad move <note name> <line number> <destination line number>");
            return;
        }

        File noteFile = getNoteFile(sender, args[1], false);
        if (!noteFile.exists())
        {
            sender.sendMessage("Could not find note.");
            return;
        }

        try
        {
            BufferedReader fileIn = new BufferedReader(new FileReader(noteFile));
            String fileCurrentLine = null;
            String lineToMove = null;
            int currentLine = 0;
            while ((fileCurrentLine = fileIn.readLine()) != null)
            {   
                if (currentLine == Integer.parseInt(args[2]) + 1)
                {
                    lineToMove = fileCurrentLine;
                }
                currentLine++;
            }
            fileIn.close();

            if (lineToMove == null)
            {
                sender.sendMessage(ChatColor.RED + "Could not find line in note.");
            }

            if ((Integer.parseInt(args[2]) > (Integer.parseInt(args[3]))))
            {
                String[] insertArgs = {"insert", args[1], args[3], lineToMove};
                insertAction(sender, insertArgs, true);
                String[] removelineArgs = {"removeline", args[1], String.valueOf(Integer.parseInt(args[2]) + 1)};
                removeLineAction(sender, removelineArgs, true);
            }
            else if (((Integer.parseInt(args[2]) < (Integer.parseInt(args[3])))))
            {
                String[] insertArgs = {"insert", args[1],String.valueOf(Integer.parseInt(args[3]) + 1), lineToMove};
                insertAction(sender, insertArgs, true);
                String[] removelineArgs = {"removeline", args[1], args[2]};
                removeLineAction(sender, removelineArgs, true);
            }
            else 
            {
                sender.sendMessage(ChatColor.RED + "Cannot move line to location line is already in.");
                return;
            }
            sender.sendMessage(ChatColor.GREEN + "Successfully moved lines.");      
        }
        catch (Exception e) 
        {
            sender.sendMessage(ChatColor.RED + "Error: Error information sent to logs.");
            Bukkit.getLogger().log(Level.SEVERE, e.getMessage());
            return;
        }
    }

    private void insertAction(CommandSender sender, String[] args, boolean Silent)
    {
        if ((args.length < 3) || (args[1].equalsIgnoreCase("help")))
        {
            if (!Silent)
            {
                sender.sendMessage(ChatColor.RED + "Usage: /notepad insert <note name> <line number> <line text>");
                sender.sendMessage(ChatColor.YELLOW + "Tip: Use '/notepad view <note name> true' to see line numbers on non-list files.");
            }
            return;
        }

        File file = getNoteFile(sender, args[1], false);

        if (!file.exists())
        {
            if (!Silent)
                sender.sendMessage(ChatColor.RED + "Could not find file.");
            return;
        }

        try 
        {
            String lineToAdd = "";

            for (int i = 3; i < args.length; i++) 
            {
                lineToAdd += args[i];
                if (i < args.length - 1)
                {
                    lineToAdd += " ";
                }
            }

            boolean didAddLine = false;
            BufferedReader fileIn = new BufferedReader(new FileReader(file));
            String fileContent = "";
            String fileCurrentLine;
            int fileCurrentLineIndex = 0;

            while ((fileCurrentLine = fileIn.readLine()) != null)
            {
                if (fileCurrentLineIndex == Integer.parseInt(args[2]) + 1) // add 1 because the first line is always the note type
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
            fileIn.close();

            BufferedWriter fileOut = new BufferedWriter(new FileWriter(file));
            fileOut.write(fileContent);
            fileOut.close();  
            if (!Silent)
            {
                if (!didAddLine)
                {
                    sender.sendMessage(ChatColor.RED + "Did not add line.");
                }
                else
                {
                    sender.sendMessage(ChatColor.GREEN + "Successfully inserted line '" + lineToAdd + "' into note '" + args[1] + "'.");
                }
            }

        } 
        catch (Exception e) 
        {
            sender.sendMessage(ChatColor.RED + "Error: Failed to write to file. Error information sent to logs.");
            Bukkit.getLogger().log(Level.SEVERE, e.getMessage());
        }
    }

    /* User Trust/File Sharing */

    private void trustUserAction(CommandSender sender, String[] args)
    {
        if (args.length < 2)
        {
            sender.sendMessage(ChatColor.RED + "Usage: /notepad trust <username>");
            return;
        }
        File trustFile = getUserTrustFile(getSenderUUID(sender));
        if(!trustFile.exists())
        {
            trustFile.getParentFile().mkdirs();
            try 
            {
                trustFile.createNewFile();
            } 
            catch (Exception e) 
            {
                sender.sendMessage(ChatColor.RED + "Error: Error trying to create file. Error information sent to logs.");
                Bukkit.getLogger().log(Level.SEVERE, e.getMessage());
            }
            
        }
        try
        {
            BufferedReader fileIn = new BufferedReader(new FileReader(trustFile));
            String fileContent = "";
            String fileCurrentLine;
            while ((fileCurrentLine = fileIn.readLine()) != null)
            {
                fileContent += fileCurrentLine + "\n";
            }
            fileIn.close();

            BufferedWriter fileOut = new BufferedWriter(new FileWriter(trustFile));
            fileOut.write(fileContent + getNameUUID(args[1]).toString());
            fileOut.close();  
            sender.sendMessage(ChatColor.GREEN + "Successfully added '" + args[1] + "' UUID to file. UUID: '" + getNameUUID(args[1]).toString() + "'");
        }
        catch (Exception e) 
        {
            sender.sendMessage(ChatColor.RED + "Error: Error trying to write to file. Error information sent to logs."); 
            Bukkit.getLogger().log(Level.SEVERE, e.getMessage());
            return;
        }
    }

    private void untrustUserAction(CommandSender sender, String[] args)
    {
        if (args.length < 2)
        {
            sender.sendMessage(ChatColor.RED + "Usage: /notepad untrust <username or UUID>");
            return;
        }
        File trustFile = getUserTrustFile(getSenderUUID(sender));
        if(!trustFile.exists())
        {
            return;           
        }
        try
        {
            BufferedReader fileIn = new BufferedReader(new FileReader(trustFile));
            String fileContent = "";
            String fileCurrentLine;
            boolean everRemovedAnything = false;
            while ((fileCurrentLine = fileIn.readLine()) != null)
            {
                if (!(getNameUUID(args[1]).toString().equalsIgnoreCase(fileCurrentLine)) && !(args[1].equalsIgnoreCase(fileCurrentLine)))
                {
                    fileContent += fileCurrentLine + "\n";
                }
                else
                {
                    everRemovedAnything = true;
                }
            }

            if (!everRemovedAnything)
            {
                sender.sendMessage("User or UUID not found in trust file.");
            }
            else
            {
                sender.sendMessage(ChatColor.GREEN + "Successfully removed all instances found in trust file.");
            }
            
            fileIn.close();

            BufferedWriter fileOut = new BufferedWriter(new FileWriter(trustFile));
            fileOut.write(fileContent);
            fileOut.close();  
        }
        catch (Exception e) 
        {
            sender.sendMessage(ChatColor.RED + "Error: Error trying to write to file. Error information sent to logs.");
            Bukkit.getLogger().log(Level.SEVERE, e.getMessage());
            return;
        }
    }

    private void listTrustedAction(CommandSender sender, String[] args)
    {
        File trustFile = getUserTrustFile(getSenderUUID(sender));
        if(!trustFile.exists())
        {
            sender.sendMessage("No trusted users file found.");
            return;           
        }

        try
        {
            BufferedReader fileIn = new BufferedReader(new FileReader(trustFile));
            String fileCurrentLine;
            boolean everSentAnything = false;
            while ((fileCurrentLine = fileIn.readLine()) != null)
            {   
                sender.sendMessage("Name: '" + Bukkit.getServer().getOfflinePlayer(UUID.fromString(fileCurrentLine)).getName() + "' UUID: '" + fileCurrentLine + "'");
                everSentAnything = true;
            }
            fileIn.close();

            if (!everSentAnything)
            {
                sender.sendMessage("No trusted users found in file.");
            }

        }
        catch (Exception e) 
        {
            sender.sendMessage(ChatColor.RED + "Error: Error information sent to logs.");
            Bukkit.getLogger().log(Level.SEVERE, e.getMessage());
            return;
        }
    }

    private void clearTrustedAction(CommandSender sender, String[] args)
    {
        File file = getUserTrustFile(getSenderUUID(sender));

        if(file.exists())
        {
            file.delete();
            sender.sendMessage(ChatColor.GREEN + "Successfully cleared file.");  
            return;
        }
        else
        {
            sender.sendMessage(ChatColor.RED + "File did not exist."); 
            return;
        }

        
    }

    /* Alternative Directories */

    private void addAltAction(CommandSender sender, String[] args)
    {
        if (args.length < 2)
        {
            sender.sendMessage(ChatColor.RED + "Usage: /notepad addalt <username>");
            return;
        }
        File altFile = getUserAltFile(getSenderUUID(sender));
        if(!altFile.exists())
        {
            altFile.getParentFile().mkdirs();
            try 
            {
                altFile.createNewFile();
            } 
            catch (Exception e) 
            {
                sender.sendMessage(ChatColor.RED + "Error: Error trying to create file. Error information sent to logs.");
                Bukkit.getLogger().log(Level.SEVERE, e.getMessage());
            }
            
        }
        try
        {
            BufferedReader fileIn = new BufferedReader(new FileReader(altFile));
            String fileContent = "";
            String fileCurrentLine;
            while ((fileCurrentLine = fileIn.readLine()) != null)
            {
                fileContent += fileCurrentLine + "\n";
            }
            fileIn.close();

            BufferedWriter fileOut = new BufferedWriter(new FileWriter(altFile));
            fileOut.write(fileContent + getNameUUID(args[1]).toString());
            fileOut.close();  
            sender.sendMessage(ChatColor.GREEN + "Successfully added '" + args[1] + "' UUID to file. UUID: '" + getNameUUID(args[1]).toString() + "'");
        }
        catch (Exception e) 
        {
            sender.sendMessage(ChatColor.RED + "Error: Error trying to write to file. Error information sent to logs."); 
            Bukkit.getLogger().log(Level.SEVERE, e.getMessage());
            return;
        }
    }

    private void removeAltAction(CommandSender sender, String[] args)
    {
        if (args.length < 2)
        {
            sender.sendMessage(ChatColor.RED + "Usage: /notepad removealt <username or UUID>");
            return;
        }
        File altFile = getUserAltFile(getSenderUUID(sender));
        if(!altFile.exists())
        {
            return;           
        }
        try
        {
            BufferedReader fileIn = new BufferedReader(new FileReader(altFile));
            String fileContent = "";
            String fileCurrentLine;
            boolean everRemovedAnything = false;
            while ((fileCurrentLine = fileIn.readLine()) != null)
            {
                if (!(getNameUUID(args[1]).toString().equalsIgnoreCase(fileCurrentLine)) && !(args[1].equalsIgnoreCase(fileCurrentLine)))
                {
                    fileContent += fileCurrentLine + "\n";
                }
                else
                {
                    everRemovedAnything = true;
                }
            }

            if (!everRemovedAnything)
            {
                sender.sendMessage("User or UUID not found in alt list file.");
            }
            else
            {
                sender.sendMessage(ChatColor.GREEN + "Successfully removed all instances found in alt list file.");
            }
            
            fileIn.close();

            BufferedWriter fileOut = new BufferedWriter(new FileWriter(altFile));
            fileOut.write(fileContent);
            fileOut.close();  
        }
        catch (Exception e) 
        {
            sender.sendMessage(ChatColor.RED + "Error: Error trying to write to file. Error information sent to logs.");
            Bukkit.getLogger().log(Level.SEVERE, e.getMessage());
            return;
        }
    }

    private void listAltsAction(CommandSender sender, String[] args)
    {
        File altListFile = new File(getAltListsDir() + "/" + getSenderUUID(sender).toString());
        if (!altListFile.exists())
        {
            sender.sendMessage("No alt list file.");
            return;
        }

        try
        {
            BufferedReader fileIn = new BufferedReader(new FileReader(altListFile));
            String fileCurrentLine;
            boolean everSentAnything = false;
            while ((fileCurrentLine = fileIn.readLine()) != null)
            {   
                sender.sendMessage("Name: '" + Bukkit.getServer().getOfflinePlayer(UUID.fromString(fileCurrentLine)).getName() + "' UUID: '" + fileCurrentLine + "'");
                everSentAnything = true;
            }
            fileIn.close();

            if (!everSentAnything)
            {
                sender.sendMessage("No alternate directories found in file.");
            }

        }
        catch (Exception e) 
        {
            sender.sendMessage(ChatColor.RED + "Error: Error information sent to logs.");
            Bukkit.getLogger().log(Level.SEVERE, e.getMessage());
            return;
        }
    }

    //////////
    /* Help */
    //////////
    
    private void outputHelp(CommandSender sender, String[] args)
    {
        if(args.length > 1)
        {
            switch (args[1].toLowerCase()) {
                case "guide":
                sender.sendMessage("--------------------------------------------------");
                sender.sendMessage("This brief guide will instruct you on how to get started making and using notes/lists.\n \nFirst, to create a new list, use '/notepad new <notename>'. Optionally, you can specify list or note by adding so to the end. A list shows numbers on the side by default when using view. Default note type is list.\n ");
                sender.sendMessage("Now, to add to the note use '/notepad add <notename> <text to add>' this will add a new line of text to the end of the note.\n ");
                sender.sendMessage("Use '/notepad view <notename>' to see your note. If your note type is not a list, it will not show numbers on the end by default. Add ' true' to the end of the command to show numbers. To show a list without line numbers, use ' false'.\n ");
                sender.sendMessage("To remove a line from your note, use '/notepad removeline <notename> <line number>' line number should be the number to the side of whatever line you want to remove, when you use the view command with numbers showing.\n ");
                sender.sendMessage("To delete your note, use /notepad delete <notename>");
                sender.sendMessage("You can view a list of your notes using '/notepad list'");
                sender.sendMessage("--------------------------------------------------");
                    break;

                case "topics":
                sender.sendMessage("--------------------------------------------------");
                sender.sendMessage("Help Topics:");
                sender.sendMessage(ChatColor.GOLD + "actions - lists all actions");
                sender.sendMessage(ChatColor.YELLOW + "guide - guide on creating a note, adding to it and removing from it, then deleting it");
                sender.sendMessage(ChatColor.GOLD + "management - information on managing notes/files");
                sender.sendMessage(ChatColor.YELLOW + "sharing - information on trusting other users (mainly for sharing notes with others)");
                sender.sendMessage(ChatColor.GOLD + "alts - information on alternative user directories/folders");
                sender.sendMessage("--------------------------------------------------");
                    break;

                case "actions":
                sender.sendMessage("--------------------------------------------------");
                sender.sendMessage("Actions:");
                sender.sendMessage(ChatColor.YELLOW + "Note/File Management: new, delete, list");
                sender.sendMessage(ChatColor.GOLD + "Editing/viewing: view, add, removeline, insert, move");
                sender.sendMessage(ChatColor.YELLOW + "Trust/Sharing: trust, untrust, listtrusted, cleartrusted");
                sender.sendMessage(ChatColor.GOLD + "Alternate User Directories/Folders: listalts, addalt, removealt");
                sender.sendMessage("--------------------------------------------------");
                    break;

                default:
                sender.sendMessage("Unknown help topic. Use '/notepad help topics' for a list of known topics.");
                    break;
            }
            return;
        }
        sender.sendMessage("--------------------------------------------------");
        sender.sendMessage(ChatColor.YELLOW + "Use '/notepad help <topic>' to get information on a help topic");
        sender.sendMessage(ChatColor.GOLD + "Use '/notepad help topics' for a full list of help topics.");
        sender.sendMessage(ChatColor.YELLOW + "For a guide on simply making a new note or list, adding to it and removing from it, use '/notepad help guide'");
        sender.sendMessage("--------------------------------------------------");
    }

    ////////////////////////
    /* onCommand function */
    ////////////////////////

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if (args.length == 0)
        {
            sender.sendMessage(ChatColor.RED + "Usage: /notepad <action> <additional args>");
            sender.sendMessage(ChatColor.RED + "Use '/notepad help' for additional help.");
            return false;
        }
        
        switch (args[0].toLowerCase()) {
            case "new":
                newFileAction(sender, args);
                break;
            
            case "list":
                listAction(sender, args);
                break;
            
            case "view":
                viewAction(sender, args);
                break;

            case "add":
                addAction(sender, args);
                break;

            case "move":
                moveLineAction(sender, args);
                break;

            case "delete":
                deleteFileAction(sender, args);
                break;

            case "trust":
                trustUserAction(sender, args);
                break;

            case "untrust":
                untrustUserAction(sender, args);
                break;

            case "listtrusted":
                listTrustedAction(sender, args);
                break;

            case "cleartrusted":
                clearTrustedAction(sender, args);
                break;

            case "help":
                outputHelp(sender, args);
                break;
            
            case "removeline":
                removeLineAction(sender, args, false);
                break;

            case "insert":
                insertAction(sender, args, false);
                break;
                
            case "addalt":
                addAltAction(sender, args);
                break;

            case "removealt":
                removeAltAction(sender, args);
                break;

            case "listalts":
                listAltsAction(sender, args);
                break;

            default:
                sender.sendMessage(ChatColor.RED + "Unknown action.");
                break;
        }

        return true;
    }
}