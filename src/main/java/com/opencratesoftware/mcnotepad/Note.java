package com.opencratesoftware.mcnotepad;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import com.opencratesoftware.mcnotepad.utils.Utils;

public class Note 
{
    private File noteFile;

    private Long lineCount;

    private String contents;
    
    private NoteType noteType;

    private boolean hasInitialized = false;

    Note(File file)
    {
        noteFile = file;
        initialize();
    }

    public void initialize()
    {
        if (!noteFile.exists())
        {
            return;
        }

        try 
        {
            BufferedReader fileIn = new BufferedReader(new FileReader(noteFile));
            String fileCurrentLine;

            while ((fileCurrentLine = fileIn.readLine()) != null)
            {
                contents += fileCurrentLine + "\n";
            }
        } 
        catch (Exception e) 
        {
            Utils.logError(e.getMessage());
        }
        hasInitialized = true;
    }

    public void initialize(NoteType type)
    {
        if (!noteFile.exists())
        {
            try 
            {
                noteFile.createNewFile();
                Utils.setFileContents(type.toString(), noteFile);
            } 
            catch (Exception e) 
            {
                Utils.logError(e.getMessage());
            }
        }

        initialize();
    }

    public void updateInformation()
    {
        

    }

    boolean addLineAt(String lineToAdd, Long lineIndex)
    {
        return Utils.addLineToFileAt(lineToAdd, noteFile, lineIndex + 1);
    }

    boolean addLine(String lineToAdd)
    {
        contents += lineToAdd;
        return Utils.setFileContents(contents, noteFile);
    }
}
