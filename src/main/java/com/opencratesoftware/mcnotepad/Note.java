package com.opencratesoftware.mcnotepad;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.opencratesoftware.mcnotepad.structs.TrustPermissions;
import com.opencratesoftware.mcnotepad.utils.Config;
import com.opencratesoftware.mcnotepad.utils.Utils;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentBuilder;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TextComponent.Builder;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.ClickCallback.Options;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.util.Buildable;
import net.md_5.bungee.api.ChatColor;

public class Note 
{
    private File noteFile;

    private int lineCount;

    private String contents;
    
    private NoteType type;

    private boolean hasInitialized = false;

    private String noteName;

    private UUID owner;

    Note(File file)
    {
        noteFile = file;
        initialize();
    }

    Note(File file, NoteType type)
    {
        noteFile = file;
        initialize(type);
    }

    public void initialize()
    {
        if (!noteFile.exists())
        {
            try
            {
                noteFile.createNewFile();
            }
            catch (Exception e)
            {
                Bukkit.getLogger().log(Level.SEVERE, "Failed to initialize note due to noteFile failing to be created " + noteFile.toString());
                
            }
        }
        
        noteName = noteFile.getName();

        owner = UUID.fromString(noteFile.getParentFile().getName());

        updateInformation();

        hasInitialized = true;  
    }

    
    public void initialize(NoteType noteType)
    {
        noteFile.getParentFile().mkdirs();

        initialize();
    }

    public Component getViewableContents(boolean showLineNumbers, UUID requester)
    {
        Builder componentBuilder = Component.text();
        TrustPermissions Permissions = TrustList.GetUserPermissionsForNote(requester, owner.toString() + ":" + getFile().getName());

        if (!Permissions.read)
        {
            return Component.text("Failed to find file.", Style.style(TextColor.color(Color.RED.asRGB())));
        }

        String noteContentsString = contents.substring(contents.indexOf('\n', 0) + 1);
        
        if (noteContentsString.endsWith("\n"))
        {
            noteContentsString = noteContentsString.substring(0, noteContentsString.length() - 1);
        }

        if (showLineNumbers)
        {
            noteContentsString = "0. " + noteContentsString;
            int newLinePos = 0;
            int currentLine = 1;
            while ((newLinePos = noteContentsString.indexOf('\n', newLinePos + 1)) != -1) 
            {
                if (newLinePos > noteContentsString.length() - 2)
                {
                    break;
                }

                noteContentsString = noteContentsString.substring(0, newLinePos + 1) + String.valueOf(currentLine) + ". " + noteContentsString.substring(newLinePos + 1);
                newLinePos = newLinePos + (String.valueOf(currentLine) + ". ").length();
                currentLine++;
            }
        }

        while (true)
        {
            int bracketStartIndex = noteContentsString.indexOf("[");   
            
            if (bracketStartIndex < 0)
            {
                componentBuilder.append(Component.text(noteContentsString));
                noteContentsString = "";
                break;
            }

            int bracketEndIndex = noteContentsString.indexOf("]", bracketStartIndex + 1);

            if (bracketEndIndex < 0)
            {
                componentBuilder.append(Component.text(noteContentsString));
                noteContentsString = "";
                break;
            }
            
            if (noteContentsString.indexOf("\n", bracketStartIndex) < bracketEndIndex && noteContentsString.indexOf("\n", bracketStartIndex) > bracketStartIndex)
            {
                componentBuilder.append(Component.text(noteContentsString.substring(0, bracketEndIndex + 1)));
                noteContentsString = noteContentsString.substring(bracketEndIndex + 1);
                continue;
            }

            String clickableText = noteContentsString.substring(bracketStartIndex + 1, bracketEndIndex);

            int commandEndIndex = noteContentsString.indexOf("\")", bracketEndIndex + 1);

            if (commandEndIndex < 0)
            {
                componentBuilder.append(Component.text(noteContentsString.substring(0, bracketEndIndex + 1)));
                noteContentsString = noteContentsString.substring(bracketEndIndex + 1);
                continue;
            }
            
            if (!noteContentsString.substring(bracketEndIndex + 1, bracketEndIndex + 3).equals("(\""))
            {
                componentBuilder.append(Component.text(noteContentsString.substring(0, commandEndIndex + 1)));
                noteContentsString = noteContentsString.substring(commandEndIndex + 1);
                continue;
            }

            String clickCommand = noteContentsString.substring(bracketEndIndex + 3, commandEndIndex);
            
            componentBuilder.append(Component.text(noteContentsString.substring(0, bracketStartIndex)));

            Component clickableComponent = Component.text(clickableText)
                .style(Style.style(TextColor.color(0.75f, 0.6f, 1.0f)))
                .hoverEvent(Component.text("Will execute '" + clickCommand + "' as though you entered it upon being double clicked."))
                .clickEvent(ClickEvent.callback(new CommandClickEvent(clickCommand), Options.builder().uses(-1).build()));

            componentBuilder.append(clickableComponent);
            if (noteContentsString.length() > commandEndIndex + 1)
            {
                noteContentsString = noteContentsString.substring(commandEndIndex + 2);
            }
            else
            {
                componentBuilder.append(Component.text(noteContentsString));
                noteContentsString = "";
                break;
            }
        }

        return componentBuilder.asComponent();
    }

    public UUID getOwner()
    {
        return owner;
    }

    public void updateInformation()
    {
        try 
        {
            BufferedReader fileIn = new BufferedReader(new FileReader(noteFile));
            String fileCurrentLine;

            contents = "";
            lineCount = 0;

            if ((fileCurrentLine = fileIn.readLine()) != null)
            {
                type = NoteType.valueOf(fileCurrentLine);
                contents += fileCurrentLine + "\n";
            }

            while ((fileCurrentLine = fileIn.readLine()) != null)
            {
                contents += fileCurrentLine + "\n";
                lineCount ++;
            }

            fileIn.close();
        } 
        catch (Exception e) 
        {
            Utils.logError(e.getMessage());
        }
    }

    public int getLineCount()
    {
        return lineCount;
    }

    public String getContents()
    {
        return contents;
    }

    public File getFile()
    {
        return noteFile;
    }

    public NoteType getType()
    {
        return type;
    }

    public boolean isValid()
    {
        return hasInitialized;
    }

    // For inserting a line at a specified location, if you want to add a line to the end then use the addline function. Returns true if no errors occur
    public FunctionResult addLineAt(String lineToAdd, int lineIndex, UUID requester)
    {
        if (noteFile.length() >= Config.getMaxNoteSize())
        {
            return new FunctionResult(false, "Adding to this note would exceed the file size limit set by the server administrators (" + ((float) Config.getMaxNoteSize()) / 1024.0 + " kilobytes) ", "filesizelimit");
        }

        if (lineIndex < 0)
        {
            return new FunctionResult(false, "Invalid line index (less than 0)");
        }

        TrustPermissions Permissions = TrustList.GetUserPermissionsForNote(requester, owner.toString() + ":" + getFile().getName());

        if (!Permissions.write) { return new FunctionResult(false, ChatColor.RED + "Failed to find file."); }


        lineToAdd = Utils.formatStringForNotes(lineToAdd, Utils.getPlayerFromUUID(requester));

        int newLinePos = 0;
        int currentLineIndex = -1;

        // keep searching through newlines in content and insert the line after the specified line is found
        while ((newLinePos = contents.indexOf('\n', newLinePos + 1)) != -1) 
        {
            if(currentLineIndex == lineIndex - 1)
            {
                String contentsPart1 = contents.substring(0, newLinePos + 1) + lineToAdd + "\n";
                contents = contentsPart1 + contents.substring(newLinePos + 1);
            }

            currentLineIndex++;
        }

        FunctionResult setFileContentsResult = Utils.setFileContents(contents, noteFile); 
        
        if (!setFileContentsResult.success)
        {
            return setFileContentsResult;
        }
        
        return new FunctionResult(true, ChatColor.GREEN + "Successfully inserted '" + lineToAdd + "' into note '" + noteName + "' at index " + String.valueOf(lineIndex) + ".");
    }

    public FunctionResult addLine(String lineToAdd, UUID requester)
    {
        if (noteFile.length() >= Config.getMaxNoteSize())
        {
            return new FunctionResult(false, "Adding to this note would exceed the file size limit set by the server administrators (" + ((float) Config.getMaxNoteSize()) / 1024.0 + " kilobytes) ", "filesizelimit");
        }
        
        TrustPermissions Permissions = TrustList.GetUserPermissionsForNote(requester, owner.toString() + ":" + getFile().getName());

        if (!Permissions.write) { return new FunctionResult(false, ChatColor.RED + "Failed to find file."); }

        lineToAdd = Utils.formatStringForNotes(lineToAdd, Utils.getPlayerFromUUID(requester));
        
        contents += lineToAdd + "\n";
        FunctionResult setFileContentsResult = Utils.setFileContents(contents, noteFile); 
        
        if (!setFileContentsResult.success)
        {
            return setFileContentsResult;
        }
        
        return new FunctionResult(true, ChatColor.GREEN + "Successfully added '" + lineToAdd + "' to note '" + noteName + "'.");
    }

    // Removes line at specified line of note (does not count the type identifier at the top of note) Returns true if no errors occur
    public FunctionResult removeLineAt(int lineIndex, UUID requester)
    {
        int newLinePos = 0;
        int currentLineIndex = -1;
        
        TrustPermissions Permissions = TrustList.GetUserPermissionsForNote(requester, owner.toString() + ":" + getFile().getName());

        if (!Permissions.write) { return new FunctionResult(false, ChatColor.RED + "Failed to find file."); }

        if(lineIndex < 0)
        {
            return new FunctionResult(false, "Invalid line index (less than 0)");
        }
        while ((newLinePos = contents.indexOf('\n', newLinePos + 1)) != -1) 
        {
            if (contents.indexOf('\n', newLinePos + 1) == -1) { continue; } //if next is end, stop

            if(currentLineIndex == lineIndex - 1)
            {
                String contentsPart1 = contents.substring(0, newLinePos);
                int nextLinePos = contents.indexOf('\n', newLinePos + 1);
                if(nextLinePos == -1)
                {
                    contents = contentsPart1;
                }
                else
                {
                    contents = contentsPart1 + contents.substring(nextLinePos);
                }
                FunctionResult setContentsResult = Utils.setFileContents(contents, noteFile);
                if (!setContentsResult.success)
                {
                    return setContentsResult;
                }
                else
                {
                    return new FunctionResult(true, ChatColor.GREEN + "Successfully removed line from note.");
                }
            }
            currentLineIndex++;
        }
        return new FunctionResult(true, ChatColor.RED + "Invalid line index (greater than highest line index).");
    }

    public FunctionResult rename(String newName, CommandSender requester)
    {
        newName = Utils.clampStringCharacterCount(newName, Config.getMaxFilenameCharacters());

        String oldName = noteName;

        File newNoteFile = Utils.getNoteFile(requester, newName, true);

        if (!noteFile.renameTo(newNoteFile))
        {
            return new FunctionResult(false, ChatColor.RED + "Rename failed for an unknown reason.", "failed");
        }

        noteFile = newNoteFile;
        initialize();

        return new FunctionResult(true, ChatColor.GREEN + "Successfully renamed note '" + oldName + "' to '" + newName + "'.");
    }

    /* First the ability to request PlayerLists to end themselves, now notes :( */
    public void delete()
    {
        removeNoteFromMemory(this);
        noteFile.delete();
    }

    ///////////////////////
    /* Memory Management */
    ///////////////////////

    public static void InitializeNoteMemory()
    {
        Utils.log("Note memory initialized with a capacity of " + String.valueOf(Config.getMaxMemorizedNotes()) + " notes.");
        notes = new Note[Config.getMaxMemorizedNotes()];
        for (int i = 0; i < notes.length; i++) 
        {
            notes[i] = null;
        }
    }

    private static Note[] notes;

    public static Note[] getNotes()
    {
        return notes;
    }

    public static Note getNote(File file)
    {
        for (Note note : notes) 
        {
            if (note != null)
            {
                if(note.getFile() == file)
                {
                    return note;
                }    
            }
        }

        Note newNote = new Note(file);

        if(newNote.isValid())
        {
            addNoteToMemory(newNote);
        }

        return newNote;
    }

    public static Note getNote(File file, NoteType noteType)
    {
        for (Note note : notes) 
        {
            if(note != null)
            {
                if(note.getFile() == file)
                {
                    return note;
                }    
            }
        }

        Note newNote = new Note(file, noteType);
        addNoteToMemory(newNote);
        return newNote;
    }

    public static int addNoteToMemory(Note noteToAdd)
    {
        for (int i = 0; i < notes.length; i++) // parse for any empty space and add the note there 
        {
            if(notes[i] != null)
            {
                if(notes[i].getFile().exists())
                {
                    continue;
                }
            }
            notes[i] = noteToAdd;
            return i;
        }

        for (int i = 1; i < notes.length; i++) 
        {
            notes[i - 1] = notes[i];
        }

        notes[notes.length - 1] = noteToAdd;

        return notes.length - 1;
    }

    public static FunctionResult removeNoteFromMemory(Note noteToRemove)
    {
        for (int i = 0; i < notes.length; i++)
        {
            if (notes[i] == noteToRemove)
            {
                for (int j = i; j > 0; j--) 
                {
                    notes[j] = notes[j - 1];
                }
                notes[0] = null;
                
                return new FunctionResult(true, "Successfully removed list from memory.");
            }
        }

        return new FunctionResult(false, "Could not locate list in memory.", "notfound");  
    }

    ///////////////////////////////////
    /* User Note Storage Information */
    ///////////////////////////////////

    public static int getPlayerNoteCount(UUID uuid, World world)
    {
        File notesDirectory = new File(Utils.getNotesDir(world) + uuid.toString());

        if(!notesDirectory.exists() || notesDirectory.isFile()) { return 0; }

        return notesDirectory.listFiles().length;
    }

    public static File[] getPlayerNoteFiles(UUID uuid, World world)
    {
        File notesDirectory = new File(Utils.getNotesDir(world) + uuid.toString());

        if(!notesDirectory.exists() || notesDirectory.isFile()) { return new File[0]; }

        return notesDirectory.listFiles();
    }

    public static Note[] getPlayerNotes(UUID uuid, World world)
    {
        File[] NoteFiles = getPlayerNoteFiles(uuid, world);
        
        Note[] returnValue;

        returnValue = new Note[NoteFiles.length];

        for (int i = 0; i < NoteFiles.length; i++) 
        {
            returnValue[i] = new Note(NoteFiles[i]);
        }

        return returnValue;
    }
}
