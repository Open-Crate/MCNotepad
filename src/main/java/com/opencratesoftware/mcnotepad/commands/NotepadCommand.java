package com.opencratesoftware.mcnotepad.commands;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.List;
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
import com.opencratesoftware.mcnotepad.TrustList;
import com.opencratesoftware.mcnotepad.utils.Config;
import com.opencratesoftware.mcnotepad.utils.Utils;
import com.opencratesoftware.mcnotepad.structs.CommandData;
import com.opencratesoftware.mcnotepad.structs.PlayerListEntry;
import com.opencratesoftware.mcnotepad.structs.TrustPermissions;
import com.opencratesoftware.mcnotepad.structs.Variable;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentBuilder;
import net.kyori.adventure.text.ComponentBuilderApplicable;
import net.kyori.adventure.text.TextComponent;
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
        AltList altList = AltList.getList(Utils.getUserAltFile(getSenderUUID(sender)));

        sender.sendMessage("-----------------------------------");
        for (int i = -1; i < altList.getUUIDCount(); i++) 
        {
            File notesDirectory;
            boolean outputName = true; 
            if (i == -1) // -1 means search the senders dir
            {
                notesDirectory = new File(Utils.getNotesDir(Utils.getPlayerFromSender(sender).getWorld()) + Utils.getSenderUUID(sender));
                outputName = false; // dont output the senders name to themself (anymore)
            }
            else
            {
                notesDirectory = new File(Utils.getNotesDir(Utils.getPlayerFromSender(sender).getWorld()) + altList.getUUIDs()[i]);
                //if (!Utils.playerTrustsPlayer(getSenderUUID(sender), altList.getUUIDs()[i])){ continue; } // if not trusted then dont display the list of notes
            }

            if (!notesDirectory.exists()){ continue; }

            if (notesDirectory.listFiles() == null){ continue; }
            

            File[] notes = notesDirectory.listFiles();

            for (File file : notes) 
            {
                if (outputName)
                {
                    TrustPermissions Permissions = TrustList.GetUserPermissionsForNote(getSenderUUID(sender), altList.getUUIDs()[i].toString() + ":" + notes[i].getName());
                    if (!Permissions.read) { continue; }
                    sender.sendMessage(Bukkit.getServer().getOfflinePlayer(altList.getUUIDs()[i]).getName() + ":" + file.getName().replace(Utils.getNoteExt(), ""));    
                }
                else
                {
                    sender.sendMessage(file.getName().replace(Utils.getNoteExt(), ""));    
                }
            }
        }
        sender.sendMessage("-----------------------------------");
        if(Config.getMaxNotesPerPlayer() <= 0)
        {
            return;
        }

        File senderNotesDir = new File(Utils.getNotesDir(Utils.getPlayerFromSender(sender).getWorld()) + getSenderUUID(sender));
        int senderNoteCount = 0;
        if (senderNotesDir.exists())
        { 
            senderNoteCount = senderNotesDir.listFiles().length;
        }

        String NoteStorageBarFill = "";

        float SenderNoteCountPercentage = Float.valueOf(senderNoteCount)/Float.valueOf(Config.getMaxNotesPerPlayer());
        ChatColor FillColor = ChatColor.GREEN;
        if (SenderNoteCountPercentage > 0.4)
        {
            if (SenderNoteCountPercentage > 0.6)
            {
                if (SenderNoteCountPercentage > 0.8)
                {
                    if (SenderNoteCountPercentage >= 1.0)
                    {
                        FillColor = ChatColor.DARK_RED;
                    }
                    else
                    {
                        FillColor = ChatColor.RED;
                    }
                }
                else
                {
                    FillColor = ChatColor.GOLD;
                }
            }
            else
            {
                FillColor = ChatColor.YELLOW;
            }
        }

        for (int index = 0; index < Math.round(SenderNoteCountPercentage * 10.0f); index++) 
        {
            NoteStorageBarFill = NoteStorageBarFill + FillColor + "█";
        }

        for (int i = 0; i < 10 - Math.round(SenderNoteCountPercentage * 10.0f); i++) 
        {
            NoteStorageBarFill = NoteStorageBarFill + ChatColor.WHITE + "█";
        }

        sender.sendMessage("Note Storage: " + String.valueOf(senderNoteCount) + "/" + String.valueOf(Config.getMaxNotesPerPlayer()) + " " + NoteStorageBarFill);
        if (SenderNoteCountPercentage > 1.0)
        {
            sender.sendMessage(ChatColor.RED + "WARNING: NOTE COUNT IS MORE THAN THE LIMIT SET BY SERVER ADMINISTRATORS.");
        }
        sender.sendMessage("-----------------------------------");
    }

    private void newFileAction(CommandSender sender, String[] args)
    {
        if(args.length == 1)
        {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /notepad new <New Note Name> or Usage: /notepad new <New Note Name> <Note Type>");
            return;
        }

        File file = getNoteFile(sender, args[1], true);
        
        if(!Utils.getSenderUUID(sender).equals(UUID.fromString(file.getParentFile().getName())))
        {
            sender.sendMessage(ChatColor.RED + "Cannot create files for other players.");
            return;
        }

        if (file.getParentFile().exists())
        {
            if (file.exists())
            {
                sender.sendMessage(ChatColor.YELLOW + "File already exists");
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

    private void deleteFileAction(CommandSender sender, String[] args)
    {
        if (args.length < 2)
        {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /notepad delete <Note Name>");
            return;
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
                sender.sendMessage(ChatColor.RED + "Failed to find file.");
            }
        }
        else
        {
            sender.sendMessage(ChatColor.RED + "Did not delete note. Did not detect command user as owner.");
            return;
        }
    }

    void renameAction(CommandSender sender, String[] args)
    {
        if (args.length < 3)
        {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /notepad rename <NoteName> <NewNoteName>");
            return;
        }

        File file = getNoteFile(sender, args[1], true);

        if (UUID.fromString(file.getParentFile().getName()).equals(getSenderUUID(sender))) // do not allow users to rename files not in their directory.
        {
            Note note = Note.getNote(file);
            
            if (note.isValid())
            {
                sender.sendMessage(note.rename(args[2], sender).getUserFriendlyMessage());
                return;
            }
            else
            {
                sender.sendMessage(ChatColor.RED + "Failed to find file.");
            }
        }
        else
        {
            sender.sendMessage(ChatColor.RED + "Did not rename note. Did not detect command user as owner.");
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
            sender.sendMessage(ChatColor.YELLOW + "Usage: /notepad view <Note Name> <Should Show Line Numbers>");
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

        TrustPermissions permissions = TrustList.GetUserPermissionsForNote(getSenderUUID(sender), note.getOwner().toString() + ":" + note.getFile().getName());

        if (!permissions.read) { player.sendMessage(ChatColor.RED + "Failed to find file."); return; }

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
        player.sendMessage(note.getViewableContents(showLineNumbers, getSenderUUID(sender)));            
        player.sendMessage("--------------------------------------------------");
    }

    private void addAction(CommandSender sender, String[] args)
    {
        if (args.length < 3)
        {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /notepad add <note name> <note text>");
            sender.sendMessage(ChatColor.GOLD + "Tip: use '[POS]'' to paste your current position into the line");
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

        FunctionResult addResult = note.addLine(noteText, getSenderUUID(sender));

        sender.sendMessage(addResult.getUserFriendlyMessage());

    }

    private void removeLineAction(CommandSender sender, String[] args, boolean Silent)
    {
        if ((args.length < 3) || (args[1].equalsIgnoreCase("help")))
        {
            if (!Silent)
            {
                sender.sendMessage(ChatColor.YELLOW + "Usage: /notepad removeline <note name> <line number>");
                sender.sendMessage(ChatColor.GOLD + "Tip: Use '/notepad view <note name> true' to see line numbers on non-list files.");
            }
            return;
        }

        if (!Utils.isIntString(args[2])){sender.sendMessage(ChatColor.RED + "Line Number parameter must be an integer (number without decimal)"); return;}

        File file = getNoteFile(sender, args[1], false);

        if (!file.exists())
        {
            if (!Silent)
                sender.sendMessage(ChatColor.RED + "Failed to find file.");
            return;
        }
        
        Note note = Note.getNote(file);

        FunctionResult removeResult = note.removeLineAt(Utils.stringToInt(args[2]), getSenderUUID(sender));

        if(removeResult.successful())
        {
            if (!Silent)
            sender.sendMessage(ChatColor.GREEN + removeResult.getUserFriendlyMessage());
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
            sender.sendMessage(ChatColor.YELLOW + "Usage: /notepad move <note name> <line number> <destination line number>");
            return;
        }

        File noteFile = getNoteFile(sender, args[1], false);
        if (!noteFile.exists())
        {
            sender.sendMessage("Could not find note.");
            return;
        }

        if (!Utils.isIntString(args[2])){sender.sendMessage(ChatColor.RED + "Line Number parameter must be an integer (number without decimal)"); return;}
        if (!Utils.isIntString(args[3])){sender.sendMessage(ChatColor.RED + "Destination Line Number parameter must be an integer (number without decimal)"); return;}
        
        try
        {
            BufferedReader fileIn = new BufferedReader(new FileReader(noteFile));
            String fileCurrentLine = null;
            String lineToMove = null;
            int currentLine = 0;
            while ((fileCurrentLine = fileIn.readLine()) != null)
            {   
                if (currentLine == Utils.stringToInt(args[2]) + 1)
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

            if ((Utils.stringToInt(args[2]) > (Utils.stringToInt(args[3]))))
            {
                String[] insertArgs = {"insert", args[1], args[3], lineToMove};
                insertAction(sender, insertArgs, true);
                String[] removelineArgs = {"removeline", args[1], String.valueOf(Utils.stringToInt(args[2]) + 1)};
                removeLineAction(sender, removelineArgs, true);
            }
            else if (((Utils.stringToInt(args[2]) < (Utils.stringToInt(args[3])))))
            {
                String[] insertArgs = {"insert", args[1],String.valueOf(Utils.stringToInt(args[3]) + 1), lineToMove};
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
                sender.sendMessage(ChatColor.YELLOW + "Usage: /notepad insert <note name> <line number> <line text>");
                sender.sendMessage(ChatColor.GOLD + "Tip: Use '/notepad view <note name> true' to see line numbers on non-list files.");
                sender.sendMessage(ChatColor.GOLD + "Tip: use '[POS]'' to paste your current position into the line");
            }
            return;
        }

        if (!Utils.isIntString(args[2])){sender.sendMessage(ChatColor.RED + "Line Number parameter must be an integer (number without decimal)"); return;}

        File file = getNoteFile(sender, args[1], false);

        if (!file.exists())
        {
            if (!Silent)
                sender.sendMessage(ChatColor.RED + "Failed to find file.");
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

        FunctionResult addResult = note.addLineAt(lineToAdd, Utils.stringToInt(args[2]), getSenderUUID(sender));

        sender.sendMessage(addResult.getUserFriendlyMessage());
    }

    /////////////////////////////
    /* User Trust/File Sharing */
    /////////////////////////////

    private void trustUserAction(CommandSender sender, String[] args)
    {
        if (args.length < 2)
        {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /notepad trust <username>");
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
        
        FunctionResult addResult = trustList.add(new PlayerListEntry(getNameUUID(args[1]).toString() + " | \\ALL read write"));

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
            sender.sendMessage(ChatColor.YELLOW + "Usage: /notepad untrust <username or UUID>");
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

    private void permEditAction(CommandSender sender, String[] args)
    {
        if (args.length < 3)
        {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /notepad permedit <username> <operation> <permissions modifications>");
            sender.sendMessage(ChatColor.GOLD + "You can refer to the help topic 'advancedsharing' with '/notepad help advancedsharing' for details on using permedit.");
            return;
        }
        
        String userName = args[1];

        String operation = args[2].toLowerCase();

        if (operation.equals("list"))
        {
            File trustFile = getUserTrustFile(getSenderUUID(sender));

            TrustList trustList = TrustList.getList(trustFile);
    
            if (!trustFile.exists())    { return; }
            
            PlayerListEntry entry = trustList.getEntryByUUID(getNameUUID(userName));

            if (entry == null) { return; }

            sender.sendMessage(userName + ":\n    " + Utils.mergeArray(entry.Attributes, "\n    "));

            return;
        }

        if (!operation.equals("set") && !operation.equals("add") && !operation.equals("remove"))
        {
            sender.sendMessage(ChatColor.RED + "Valid operations are 'set', 'add', 'remove' and 'list'.");
            return;
        }

        File trustFile = getUserTrustFile(getSenderUUID(sender));

        TrustList trustList = TrustList.getList(trustFile);

        if (!trustFile.exists())    { return; }

        PlayerListEntry Entry = new PlayerListEntry(getNameUUID(userName));

        String[] permArgs = args;
        permArgs[0] = ""; // set to "" causing ignoreEmptyStrings in mergeArray to simply ignore them.
        permArgs[1] = "";
        permArgs[2] = ""; 
        String mergedPermissions = Utils.mergeArray(permArgs, " ", true);
        
        mergedPermissions = "name | " + mergedPermissions;

        CommandData Permissions = Utils.formatCommand(mergedPermissions, " | ");
        
        CommandData[] FormattedPermissions = new CommandData[Permissions.params.length];

        for (int i = 0; i < FormattedPermissions.length; i++) 
        {
            FormattedPermissions[i] = Utils.formatCommand(Permissions.params[i], " ");    
        }

        Entry.Attributes = new Variable[FormattedPermissions.length];

        for (int i = 0; i < Entry.Attributes.length; i++) 
        {
            Entry.Attributes[i] = new Variable(FormattedPermissions[i].name, Utils.mergeArray(FormattedPermissions[i].params, " "));
        }
        
        FunctionResult addResult;

        if (operation.equals("set"))
        { 
            addResult = trustList.add(Entry);
        }
        else if (operation.equals("remove"))
        {
            addResult = trustList.removeAttributesFromEntry(Entry);
        }
        else
        {
            addResult = trustList.addAttributesToEntry(Entry);
        }
        if (addResult.successful())
        {
            sender.sendMessage(ChatColor.GREEN + "Successfully applied requested modifications for UUID of '" + userName + "' on file. UUID: '" + getNameUUID(userName).toString() + "'");
        }
        else
        {
            sender.sendMessage(ChatColor.RED + addResult.getUserFriendlyMessage()); 
        }
    }

    /////////////////////////////
    /* Alternative Directories */
    /////////////////////////////

    private void addAltAction(CommandSender sender, String[] args)
    {
        if (args.length < 2)
        {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /notepad addalt <username>");
            return;
        }

        UUID uuidToAdd = getNameUUID(args[1]);

        File altFile = Utils.getUserAltFile(getSenderUUID(sender));

        AltList altList = AltList.getList(altFile);

        if(!altList.isValid())
        {
            sender.sendMessage(ChatColor.RED + "Error: Could not initialize file. Error information sent to logs.");
        }

        FunctionResult addResult = altList.add(new PlayerListEntry(uuidToAdd));

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
            sender.sendMessage(ChatColor.YELLOW + "Usage: /notepad removealt <username or UUID>");
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
        int page = 1;

        if(args.length > 1)
        {
            if (args.length > 2) { if (Utils.isIntString(args[2])) { page = Utils.stringToInt(args[2]); } } // if page arg is provided and is an int string, set that as the page number.

            switch (args[1].toLowerCase()) {
                case "guide":
                sender.sendMessage("--------------------------------------------------");
                sender.sendMessage("This brief guide will instruct you on how to get started making and using notes/lists.\n \nFirst, to create a new list, use '/notepad new <notename>'. Optionally, you can specify list or note by adding so to the end. A list shows numbers on the side by default when using view. Default note type is list.\n ");
                sender.sendMessage("Now, to add to the note use '/notepad add <notename> <text to add>' this will add a new line of text to the end of the note. You can use '[POS]' to paste your current position.\n ");
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
                sender.sendMessage(ChatColor.GOLD + "advancedsharing - information on controlling permissions given to other users, for better control.");
                sender.sendMessage(ChatColor.YELLOW + "alts - information on alternative user directories/folders (for making shared notes easier to refer to)");
                sender.sendMessage("--------------------------------------------------");
                    break;

                case "actions":
                sender.sendMessage("--------------------------------------------------");
                sender.sendMessage("Actions:");
                sender.sendMessage(ChatColor.YELLOW + "Note/File Management: new, delete, list, rename");
                sender.sendMessage(ChatColor.GOLD + "Editing/viewing: view, add, removeline, insert, move");
                sender.sendMessage(ChatColor.YELLOW + "Trust/Sharing: trust, untrust, listtrusted, cleartrusted, permedit");
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
                    sender.sendMessage(ChatColor.GOLD + "- To add a user to your trusted list, use '/notepad trust [username or UUID]'.");
                    sender.sendMessage(ChatColor.YELLOW + "- To remove a user, use '/notepad untrust [username or UUID]'.");
                    sender.sendMessage(ChatColor.GOLD + "- To list trusted users, use '/notepad listtrusted'.");
                    sender.sendMessage(ChatColor.YELLOW + "- To clear your trustedlist, use '/notepad cleartrusted'");
                    sender.sendMessage(ChatColor.WHITE + " \nView topic 'advancedsharing' to control permissions more.");
                    sender.sendMessage("--------------------------------------------------");
                    break;
                    
                case "management":
                sender.sendMessage("--------------------------------------------------");
                sender.sendMessage("Managing notes in notepad is easy, the three main commands to know are 'new', 'delete' and 'list'.\n ");
                sender.sendMessage("new - Adds a new file to your directory/notes folder. Usage is '/notepad new [notename]'. Optionally you can specify a type of note (note or list) by adding so to the end (ex. '/notepad new [notename] note'), the default is list. Lists automatically show numbers on each line whereas notes simply display as raw text.\n ");
                sender.sendMessage("delete - Deletes a note. Usage is '/notepad delete [notename]'.\nYou cannot delete other user's notes, even if you are trusted by the owner.\n ");
                sender.sendMessage("list - Lists all of your notes. Usage is '/notepad list'.");
                sender.sendMessage("--------------------------------------------------");
                break;
            
            case "advancedsharing":
                sender.sendMessage("--------------------------------------------------");
                if (page == 1)
                {
                    sender.sendMessage("Notepad provides the option to add additional parameters for increased permission control over your notes. This allows you to do things such as choose what notes users can access, and what they can do to those notes.");
                    sender.sendMessage(" \nUsing this you can do things such as grant a user access to only two notes, one note giving them full access (read and write) and the other being read-only. Or, you can give access to all except for specific ones.");
                    sender.sendMessage(ChatColor.YELLOW + " \nThis is a much more advanced and complex look into controlling permissions in notepad. You do not need this to use notepad, for most users the information in the 'sharing' topic is enough.");  
                }
                if (page == 2)
                {
                    sender.sendMessage("So in order to use this functionality, start with typing a standard '/notepad permedit [user]' but before you send this command, additionally you must specify parameters.");
                    sender.sendMessage(" \nThe first additional parameter you must specify is an operation to do, the valid operations are 'add', 'remove', 'set' and list. 'add' adds all specified parameters to the end, 'remove' removes them from anywhere in the line, and 'set' replaces entirely. 'list' lists the given permissions for a specific user.");
                }
                if (page == 3)
                {
                    sender.sendMessage("Now you can specify the permissions to perform the operation with.");
                    sender.sendMessage(" \nThese parameters are structured like this '[note] [permissions] | [note2] [permissions]'. For example, to give a user just view and add permissions to a note named 'note0' you would add 'note0 view add'.");
                    sender.sendMessage(" \nAdditionally, you may use the keyword '\\ALL' in place of the note name, to specify permissions for all notes. Explicitly identifying a note by name will stop notepad from reading permissions, whereas '\\ALL' will allow it to continue. This effectively results in '\\ALL' having less priority.");
                    sender.sendMessage(" \nYou can do multiple notes for one user by using '|' as a seperator. For example, '/notepad permedit set [player] \\ALL read write | note0 read | note1 read write'");
                }
                if (page == 4)
                {
                    sender.sendMessage("The permissions currently recognized by notepad right now are read and write. 'write' is for adding and removing lines. 'read' is for viewing notes.");
                    sender.sendMessage(" \nRead is required for users to be able to see a note when they use list with you added as an altdir.");
                    sender.sendMessage(" \nNew notes are not added with the exception of using the '\\ALL' keyword, so if you use the '\\ALL' keyword ensure you set permissions for new notes accordingly.");
                }
                sender.sendMessage(ChatColor.GOLD + " \nShowing page " + String.valueOf(page) + "/4. Append the page number you would like to view to switch pages.");
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
            sender.sendMessage(ChatColor.GOLD + "Use '/notepad help' for additional help.");
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

            case "rename":
                renameAction(sender, args);
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
            
            case "permedit":
                permEditAction(sender, args);
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