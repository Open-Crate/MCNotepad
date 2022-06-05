package com.opencratesoftware.commands;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Scanner;
import java.util.UUID;
import java.util.logging.Level;

import javax.print.attribute.standard.Severity;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import net.md_5.bungee.api.ChatColor;

public class NotepadCommand implements CommandExecutor
{
    private File getTrustListsDir()
    {
        File file = new File(getDataDir() + "/trustlists/");
        if (!file.exists())
        {
            file.mkdirs();
        }
        return file;
    }
    
    private File getUserTrustFile(UUID userUUID)
    {
        return new File(getTrustListsDir().toString() + "/" + userUUID.toString());
    }

    private boolean playerTrustsPlayer(UUID trusteeUUID, UUID trusterUUID)
    {
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

    private UUID getNameUUID(String name)
    {
        if (Bukkit.getServer().getPlayerUniqueId(name) == null)
        {
            return new UUID(0, 0);
        }
        return Bukkit.getServer().getPlayerUniqueId(name);
    }

    private UUID getSenderUUID(CommandSender sender)
    {
        return getNameUUID(sender.getName());
    }

    private String getDataDir()
    {
        return Bukkit.getServer().getPluginManager().getPlugin("Notepad").getDataFolder() + "/";
    }

    private String getNotesDir()
    {
        return getDataDir() + "/notes/";
    }

    private String getNoteExt() // this might be configurable eventually so use a getter to avoid having to update it everywhere
    {
        return "";
    }

    private File getNoteFile(CommandSender sender, String noteName)
    {
        if(noteName.contains(":"))
        {
            String NoteOwnerName = noteName.substring(0, noteName.indexOf(":"));
            if (playerTrustsPlayer(getNameUUID(sender.getName()), getNameUUID(NoteOwnerName)))
            {
                return new File(getNotesDir() + getNameUUID(NoteOwnerName), noteName.substring( Math.min (noteName.indexOf(":") + 1, noteName.length() - 1)) + getNoteExt());
            }
            return new File("getNotesDir()" + "ThisDirectoryProbablyDoesntExist/justlikethisdoesntprobably/alsothis/con/thisshouldbesafe.txt/incaseitisnt.exe/anddoublyso.app/andfinally.sh/done.finished//\\");
            // return something that probably doesn't exist
        }
        return new File(getNotesDir() + getNameUUID(sender.getName()), noteName + getNoteExt());
    }

    private void viewAction(CommandSender sender, String[] args)
    {
        if(args.length < 2)
        {
            sender.sendMessage(ChatColor.RED + "Usage: /notepad view <Note Name> <Should Show Line Numbers>");
            return;
        }
        
        Player player = (Player) sender;

        File file = getNoteFile(sender, args[1]);

        if (!file.exists())
        {
            sender.sendMessage(ChatColor.RED + "Failed to find file");
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
            player.sendMessage("----------------------------------------");
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
                player.sendMessage("----------------------------------------");
                currentLineNumber++;
            }

            fileReader.close();
            in.close();

        } catch (Exception e) 
        {
          
        }

    }

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
       
        File file = getNoteFile(sender, args[1]);
        
        if(getSenderUUID(sender) != getNameUUID(file.getParentFile().getName()))
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
            String type = "note";
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
                }

                BufferedWriter fileOut = new BufferedWriter(new FileWriter(file));
                fileOut.write(type);
                fileOut.close();
                    
                
                sender.sendMessage(ChatColor.GREEN + "Successfully created new " + type + ".");
            } 
            catch (Exception e) 
            {
                sender.sendMessage(ChatColor.RED + "Unknown Error: Failed to create file.");
            }
        }
    }

    private void addAction(CommandSender sender, String[] args)
    {
        if (args.length < 3)
        {
            sender.sendMessage(ChatColor.RED + "Usage: /notepad add <note name> <note text>");
            return;
        }

        File file = getNoteFile(sender, args[1]);

        if (!file.exists())
        {
            sender.sendMessage(ChatColor.RED + "Failed to find file");
            return;
        }
        else
        {
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
                sender.sendMessage(ChatColor.RED + "Unknown Error: Failed to write to file.");
            }
        }
    }

    private void deleteFileAction(CommandSender sender, String[] args)
    {
        if (args.length < 2)
        {
            sender.sendMessage(ChatColor.RED + "Usage: /notepad delete <Note Name>");
        }
        File file = getNoteFile(sender, args[1]);
        if (file.getParentFile().getName() == (getSenderUUID(sender).toString())) // do not allow users to delete files not in their directory.
        {
            if (file.exists())
            {
                file.delete();
                sender.sendMessage(ChatColor.GREEN + "Successfully deleted note.");
            }
        }
        else
        {
            sender.sendMessage(ChatColor.RED + "Did not delete note. Did not detect command user as owner.");
            return;
        }
    }

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
                sender.sendMessage(ChatColor.RED + "Error: Unknown error trying to create file.");
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
            sender.sendMessage(ChatColor.RED + "Error: Unknown error trying to write to file."); // I could start giving the exception info to the server, maybe in future? Oh, also maybe start adding comments more... a lot more.
            return;
        }
    }

    private void untrustUserAction(CommandSender sender, String[] args)
    {
        if (args.length < 2)
        {
            sender.sendMessage(ChatColor.RED + "Usage: /notepad untrust <username>");
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
            sender.sendMessage(ChatColor.RED + "Error: Unknown error trying to write to file.");
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
            sender.sendMessage(ChatColor.RED + "Error: Unknown error.");
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

    private void outputHelp(CommandSender sender, String[] args)
    {
        sender.sendMessage("Actions: new, list, view, add, delete, trust, untrust, listtrusted, cleartrusted, help");
    }

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

            default:
                sender.sendMessage(ChatColor.RED + "Unknown action.");
                break;
        }

        return true;
    }
}