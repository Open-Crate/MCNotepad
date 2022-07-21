package com.opencratesoftware.mcnotepad.commands;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.opencratesoftware.mcnotepad.AltList;
import com.opencratesoftware.mcnotepad.FunctionResult;
import com.opencratesoftware.mcnotepad.Note;
import com.opencratesoftware.mcnotepad.NoteType;
import com.opencratesoftware.mcnotepad.Notepad;
import com.opencratesoftware.mcnotepad.TrustList;
import com.opencratesoftware.mcnotepad.utils.Config;
import com.opencratesoftware.mcnotepad.utils.Utils;

import net.md_5.bungee.api.ChatColor;

public class NotepadCommand implements CommandExecutor
{

    ///////////
    /* Utils */
    ///////////

    private File getNoteFile(CommandSender sender, String noteName, boolean IgnoreAltDirs)
    {
        return Utils.getNoteFile(sender, noteName, IgnoreAltDirs);
    }

    private UUID getSenderUUID(CommandSender sender)
    {
        return Utils.getSenderUUID(sender);
    }

    private File getUserTrustFile(UUID userUUID)
    {
        return Utils.getUserTrustFile(userUUID);
    }

    private UUID getNameUUID(String name)
    {
        return Utils.getNameUUID(name);
    }
    

    /////////////////////
    /* File Management */
    /////////////////////

    private void listAction(CommandSender sender, String[] args)
    {
        File notesDirectory = new File(Utils.getNotesDir() + Utils.getSenderUUID(sender));
        File[] notes = notesDirectory.listFiles();
        for (File file : notes) 
        {
            sender.sendMessage(sender.getName() + ":" + file.getName().replace(Utils.getNoteExt(), ""));    
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

        if (file.getParentFile().exists())
        {
            if(!Utils.getSenderUUID(sender).equals(UUID.fromString(file.getParentFile().getName())))
            {
                sender.sendMessage(ChatColor.RED + "Cannot create files for other players.");
                return;
            }

            if (file.exists())
            {
                sender.sendMessage(ChatColor.RED + "File already exists");
                return;
            }
            
            File[] Notes = file.getParentFile().listFiles();

            if (Notes != null)
            {
                if (!(file.getParentFile().listFiles().length < Config.getMaxNotesPerPlayer()))
                {
                    sender.sendMessage(ChatColor.RED + "Creating a new note would exceed the max note limit per player set by the server administrators (" + Config.getMaxNotesPerPlayer() + ").");
                    return;
                }
            }

        }
        {

            Note note;
            String type = "list";

            if(args.length > 2)
            {
                note = Note.getNote(file, NoteType.valueOf(args[2].toLowerCase()));
                type = note.getType().toString();
            }
            else
            {
                note = Note.getNote(file, NoteType.list);
            }
            
            if(note.isValid())
            {
                sender.sendMessage(ChatColor.GREEN + "Successfully created new " + type + ".");
            }
            else
            {
                sender.sendMessage(ChatColor.RED + "Error: Failed to create file. Error information sent to logs");
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
            Note note = Note.getNote(file);
            if (note.isValid())
            {
                note.delete();
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

    //////////////////////////
    /* File editing/viewing */
    //////////////////////////

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

        Note note = Note.getNote(file);

        boolean showLineNumbers = false;

        if (note.getType() == NoteType.list)
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

        player.sendMessage("--------------------------------------------------");
        player.sendMessage(note.getViewableContents(showLineNumbers));
        player.sendMessage("--------------------------------------------------");
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

        Note note = Note.getNote(file);

        String noteText = "";

        for (int i = 2; i < args.length; i++) 
        {
            noteText += args[i];
            if (i < args.length - 1)
            {
                noteText += " ";
            }
        }

        FunctionResult addResult = note.addLine(noteText);
        if (addResult.successful())
        {
            sender.sendMessage(ChatColor.GREEN + "Successfully added '" + noteText + "' to note '" + args[1] + "'.");
            return;
        }
        sender.sendMessage(ChatColor.RED + addResult.getUserFriendlyMessage());

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
        
        Note note = Note.getNote(file);

        FunctionResult removeResult = note.removeLineAt(Integer.parseInt(args[2]));

        if(removeResult.successful())
        {
            if (!Silent)
            sender.sendMessage(ChatColor.GREEN + "Successfully removed line from note '" + args[1] + "' if line existed.");
        }
        else
        {
            if (!Silent)
            sender.sendMessage(ChatColor.RED + "Did not remove line:" + removeResult.getUserFriendlyMessage());
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

        Note note = Note.getNote(file);

        String lineToAdd = "";

        for (int i = 3; i < args.length; i++) 
        {
            lineToAdd += args[i];
            if (i < args.length - 1)
            {
                lineToAdd += " ";
            }
        }

        FunctionResult addResult = note.addLineAt(lineToAdd, Integer.valueOf(args[2]));

        if (!addResult.successful())
        {
            sender.sendMessage(ChatColor.RED + addResult.getUserFriendlyMessage());
        }
        else
        {
            sender.sendMessage(ChatColor.GREEN + "Successfully inserted line '" + lineToAdd + "' into note '" + args[1] + "'.");
        }
    }

    /////////////////////////////
    /* User Trust/File Sharing */
    /////////////////////////////

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

        TrustList trustList = TrustList.getList(trustFile);

        FunctionResult addResult = trustList.add(getNameUUID(args[1]));

        if (addResult.successful())
        {
            sender.sendMessage(ChatColor.GREEN + "Successfully added '" + args[1] + "' UUID to file. UUID: '" + getNameUUID(args[1]).toString() + "'");
        }
        else
        {
            sender.sendMessage(ChatColor.RED + addResult.getUserFriendlyMessage()); 
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

        TrustList trustList = TrustList.getList(trustFile);

        UUID uuidToRemove;
        if (args[1].length() > 24)
        {
            uuidToRemove = UUID.fromString(args[1]);
        }
        else
        {
            uuidToRemove = getNameUUID(args[1]);
        }

        FunctionResult removeResult = trustList.remove(uuidToRemove);

        if (removeResult.successful())
        {
            sender.sendMessage(ChatColor.GREEN + "Successfully removed from trust file.");
        }
        else
        {
            sender.sendMessage(removeResult.getUserFriendlyMessage()); 
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

        TrustList trustList = TrustList.getList(trustFile);
        UUID[] trustListContents = trustList.getUUIDs();

        sender.sendMessage("-----------------------------------");   
        for (UUID uuid : trustListContents) 
        {
            sender.sendMessage( "Name: '" + Bukkit.getOfflinePlayer(uuid).getName() + "'\n" + "UUID: '" + uuid.toString());    
        }
        sender.sendMessage("-----------------------------------");   
    }

    private void clearTrustedAction(CommandSender sender, String[] args)
    {
        File file = getUserTrustFile(getSenderUUID(sender));

        TrustList trustList = TrustList.getList(file);

        if(trustList.isValid())
        {
            trustList.delete();
            sender.sendMessage(ChatColor.GREEN + "Successfully cleared file.");  
            return;
        }
        else
        {
            sender.sendMessage(ChatColor.RED + "File did not exist."); 
            return;
        }

        
    }

    /////////////////////////////
    /* Alternative Directories */
    /////////////////////////////

    private void addAltAction(CommandSender sender, String[] args)
    {
        if (args.length < 2)
        {
            sender.sendMessage(ChatColor.RED + "Usage: /notepad addalt <username>");
            return;
        }

        UUID uuidToAdd = getNameUUID(args[1]);

        File altFile = Utils.getUserAltFile(getSenderUUID(sender));

        AltList altList = AltList.getList(altFile);

        if(!altList.isValid())
        {
            sender.sendMessage(ChatColor.RED + "Error: Could not initialize file. Error information sent to logs.");
        }

        FunctionResult addResult = altList.add(uuidToAdd);

        if (addResult.successful())
        {
            sender.sendMessage(ChatColor.GREEN + "Successfully added '" + args[1] + "' UUID to file. UUID: '" + getNameUUID(args[1]).toString() + "'");
        }
        else
        {
            sender.sendMessage(ChatColor.RED + addResult.getUserFriendlyMessage()); 
        }
    }

    private void removeAltAction(CommandSender sender, String[] args)
    {
        if (args.length < 2)
        {
            sender.sendMessage(ChatColor.RED + "Usage: /notepad removealt <username or UUID>");
            return;
        }
        
        File altFile = Utils.getUserAltFile(getSenderUUID(sender));

        AltList altList = AltList.getList(altFile);

        if(!altList.isValid())
        {
            sender.sendMessage(ChatColor.RED + "Error: Could not initialize file. Error information sent to logs.");
            return;           
        }

        UUID uuidToRemove;
        if (args[1].length() > 24)
        {
            uuidToRemove = UUID.fromString(args[1]);
        }
        else
        {
            uuidToRemove = getNameUUID(args[1]);
        }

        FunctionResult removeResult = altList.remove(uuidToRemove);

        if (removeResult.successful())
        {
            sender.sendMessage(ChatColor.GREEN + "Successfully removed from altlist.");
        }
        else
        {
            sender.sendMessage(ChatColor.RED + removeResult.getUserFriendlyMessage());
        }
    }

    private void listAltsAction(CommandSender sender, String[] args)
    {
        File altListFile = new File(Utils.getAltListsDir() + "/" + getSenderUUID(sender).toString());
        if (!altListFile.exists())
        {
            sender.sendMessage("No alt list file.");
            return;
        }

        AltList altList = AltList.getList(altListFile);

        UUID[] altListUUIDs = altList.getUUIDs();

        boolean everSentAnything = false;
        sender.sendMessage("-----------------------------------");   
        for (UUID uuid : altListUUIDs) 
        {
            sender.sendMessage("Name: '" + Bukkit.getServer().getOfflinePlayer(uuid).getName() + "' UUID: '" + uuid + "'");
            everSentAnything = true;
        }
        sender.sendMessage("-----------------------------------");
    }

    //////////
    /* Misc. */
    //////////
    private void showVersionAction(CommandSender sender, String[] args)
    {
        sender.sendMessage("Running Notepad from Open-Crate version " + Bukkit.getPluginManager().getPlugin("Notepad").getDescription().getVersion());
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
                sender.sendMessage(ChatColor.GOLD + "alts - information on alternative user directories/folders (for making shared notes easier to refer to)");
                sender.sendMessage("--------------------------------------------------");
                    break;

                case "actions":
                sender.sendMessage("--------------------------------------------------");
                sender.sendMessage("Actions:");
                sender.sendMessage(ChatColor.YELLOW + "Note/File Management: new, delete, list");
                sender.sendMessage(ChatColor.GOLD + "Editing/viewing: view, add, removeline, insert, move");
                sender.sendMessage(ChatColor.YELLOW + "Trust/Sharing: trust, untrust, listtrusted, cleartrusted");
                sender.sendMessage(ChatColor.GOLD + "Alternate User Directories/Folders: listalts, addalt, removealt");
                sender.sendMessage(ChatColor.YELLOW + "Misc: ver, version");
                sender.sendMessage("--------------------------------------------------");
                    break;

                case "alts":
                sender.sendMessage("--------------------------------------------------");
                sender.sendMessage("Alts, Alt Folders or Alt Directories are just a way for notepad to automatically search other user's notes without having to put their name or UUID. Usually when a user shares their notes with you, and you want to view them, you must refer to the note as '[Owner Name or UUID]:[Note Name]'. Alts help with this.");
                sender.sendMessage(" ");
                sender.sendMessage("Say if a user adds you to their trust list, allowing you to view and edit their notes and you don't want to have to type their name and notename, just the note name. All you have to do is add them as an altdir for your notes! This tells notepad to search through the notes of all of the users in the altdirs list when you can't find it in the command sender's notes.");
                sender.sendMessage(" ");
                sender.sendMessage("To add a user, simply use '/notepad addalt [username or uuid]'\n \nTo remove one, use '/notepad removealt [username or uuid]'\n \nAnd finally, to list all alts you have added, use '/notepad listalts'.");
                sender.sendMessage("--------------------------------------------------");
                    break;

                case "sharing":
                    sender.sendMessage("--------------------------------------------------");
                    sender.sendMessage("Sharing notes in notepad is simple, you simply must add a user whom you want to share a note with to your trustlist, and they'll be able to edit and view your note! Trusted users cannot delete the note file or create note files under your name, however they can clear a note file and add to one, so use with caution.");
                    sender.sendMessage(" ");
                    sender.sendMessage("To add a user to your trusted list, use '/notepad trust [username or UUID]'.\n \nTo remove a user, use '/notepad untrust [username or UUID]'.\n \nAnd to list all trusted users, use '/notepad listtrusted'.\n \nTo clear all trusted users instantly, use '/notepad cleartrusted'");
                    sender.sendMessage("--------------------------------------------------");
                    break;

                case "management":
                    sender.sendMessage("--------------------------------------------------");
                    sender.sendMessage("Managing notes in notepad is easy, the three main commands to know are 'new', 'delete' and 'list'.\n ");
                    sender.sendMessage("new - Adds a new file to your directory/notes folder. Usage is '/notepad new [notename]'. Optionally you can specify a type of note (note or list) by adding so to the end (ex. '/notepad new [notename] note'), the default is list. Lists automatically show numbers on each line whereas notes simply display as raw text.\n ");
                    sender.sendMessage("delete - Deletes a note. Usage is '/notepad delete [notename]'" + ChatColor.RED + " You cannot delete other user's notes, even if you are trusted by the owner.\n ");
                    sender.sendMessage("list - Lists all of your notes. Usage is '/notepad list'.");
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
        sender.sendMessage(ChatColor.GOLD + "For a full list of actions, view the topic named 'actions'. ('/notepad help actions')");
        sender.sendMessage(ChatColor.YELLOW + "Tip: You can use /notepad, /note and /notes interchangeably.");
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
            sender.sendMessage(ChatColor.YELLOW + "Usage: /notepad <action> <additional args>");
            sender.sendMessage(ChatColor.YELLOW + "Use '/notepad help' for additional help.");
            return false;
        }
        
        switch (args[0].toLowerCase()) {
            case "new":
                newFileAction(sender, args);
                break;
            
            case "list":
                listAction(sender, args);
                break;

            case "ls":
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

            case "version":
                showVersionAction(sender, args);
                break;

            case "ver":
                showVersionAction(sender, args);
                break;

            default:
                sender.sendMessage(ChatColor.RED + "Unknown action. For help on how to use notepad, use '/notepad help'.");
                break;
        }

        return true;
    }
}