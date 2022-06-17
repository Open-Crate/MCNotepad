package com.opencratesoftware.mcnotepad;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import com.opencratesoftware.mcnotepad.utils.Config;
import com.opencratesoftware.mcnotepad.utils.Utils;

public class Note 
{
    private File noteFile;

    private int lineCount;

    private String contents;
    
    private NoteType type;

    private boolean hasInitialized = false;

    private String noteName;

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
            return;
        }

        updateInformation();

        hasInitialized = true;
    }

    
    public void initialize(NoteType noteType)
    {
        if (!noteFile.exists())
        {
            try 
            {
                noteFile.createNewFile();
                Utils.setFileContents(noteType.toString(), noteFile);
            } 
            catch (Exception e) 
            {
                Utils.logError(e.getMessage());
                return;
            }
        }

        initialize();
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
    public boolean addLineAt(String lineToAdd, int lineIndex)
    {
        int newLinePos = 0;
        int currentLineIndex = -1;
        
        if (lineIndex < 0)
        {
            return true;
        }

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

        return Utils.setFileContents(contents, noteFile);
    }

    public boolean addLine(String lineToAdd)
    {
        contents += lineToAdd + "\n";
        return Utils.setFileContents(contents, noteFile);
    }

    // Removes line at specified line of note (does not count the type identifier at the top of note) Returns true if no errors occur
    public boolean removeLineAt(int lineIndex)
    {
        int newLinePos = 0;
        int currentLineIndex = -1;
        
        if(lineIndex < 0)
        {
            return true;
        }

        while ((newLinePos = contents.indexOf('\n', newLinePos + 1)) != -1) 
        {
            if(currentLineIndex == lineIndex)
            {
                String contentsPart1 = contents.substring(0, newLinePos);
                int nextLinePos = contents.indexOf('\n', newLinePos + 1);
                if(nextLinePos == -1)
                {
                    contents = contentsPart1;
                }
                
            }
        }

        return Utils.setFileContents(contents, noteFile);
    }

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
}
