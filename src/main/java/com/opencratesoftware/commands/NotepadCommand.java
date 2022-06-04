package com.opencratesoftware.commands;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Scanner;


import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import net.md_5.bungee.api.ChatColor;

public class NotepadCommand implements CommandExecutor
{
    private String getNotesDir()
    {
        return Bukkit.getServer().getPluginManager().getPlugin("Notepad").getDataFolder() + "/notes/";
    }

    private String getNoteExt() // this might be configurable eventually so use a getter to avoid having to update it everywhere
    {
        return "";
    }

    private void viewAction(CommandSender sender, String[] args)
    {
        if(args.length < 2)
        {
            sender.sendMessage(ChatColor.RED + "Usage: /notepad view <Note Name> <Should Show Line Numbers>");
            return;
        }
        
        String NoteOwner = sender.getName();

        Player player = (Player) sender;

        File file = new File(getNotesDir() + NoteOwner + "/" + args[1]);

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
        File notesDirectory = new File(getNotesDir() + sender.getName());
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
       
        File file = new File(getNotesDir() + sender.getName(), args[1] + getNoteExt());
        
        if (file.exists())
        {
            sender.sendMessage(ChatColor.RED + "File already exists");
        }
        else
        {
            String type = "note";
            try 
            {
                file.createNewFile();
                if (args.length > 2)
                {

                    if(args[2].equalsIgnoreCase("list"))
                    {
                        type = "list";
                    }
                        BufferedWriter fileOut = new BufferedWriter(new FileWriter(file));
                        fileOut.write(type);
                        fileOut.close();
                    
                }
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

        File file = new File(getNotesDir() + sender.getName(), args[1] + getNoteExt());
        
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
                BufferedWriter fileOut = new BufferedWriter(new FileWriter(file));
                FileReader fileReader = new FileReader(file);
                Scanner in = new Scanner(fileReader);
                String fileContent = "";
                while (in.hasNext())
                {
                    fileContent += in.nextLine();
                    sender.sendMessage(ChatColor.GREEN + fileContent);
                    sender.sendMessage(ChatColor.GREEN + "a");
                }
                in.close();
                fileReader.close();

                fileOut.write(fileContent + "\n" + noteText);
                fileOut.close();  
                
                sender.sendMessage(ChatColor.GREEN + "Successfully added '" + noteText + "' to note '" + args[1] + "'.");
            } 
            catch (Exception e) 
            {
                sender.sendMessage(ChatColor.RED + "Unknown Error: Failed to write to file.");
            }
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if (args.length == 0)
        {
            sender.sendMessage(ChatColor.RED + "Usage: /notepad <action> <additional args>");
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

            default:
                sender.sendMessage(ChatColor.RED + "Unknown action.");
                break;
        }

        return true;
    }
}