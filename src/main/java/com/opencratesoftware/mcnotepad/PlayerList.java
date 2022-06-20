package com.opencratesoftware.mcnotepad;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.UUID;

import javax.naming.ContextNotEmptyException;

import com.opencratesoftware.mcnotepad.utils.Utils;

/* Class for lists that store player UUIDs for any purpose */
public class PlayerList 
{
    private UUID[] uuids;
    private String contents;

    private File file;

    boolean Initialized = false;

    PlayerList(File listFile, int capacity)
    {
        file = listFile;
        uuids = new UUID[capacity];
        Initialize();
    }

    void Initialize()
    {
        if (!file.exists())
        {
            file.getParentFile().mkdirs();
            try 
            {
                file.createNewFile();
            } 
            catch (Exception e) 
            {
                Utils.logError(e.getMessage());
                return;
            }
        }

        updateInformation();
        Initialized = true;
    }

    public UUID[] getUUIDs()
    {
        return uuids;
    }

    public String getContents()
    {
        return contents;
    }

    public int getCapacity()
    {
        return uuids.length;
    }

    public int getUUIDCount()
    {
        int returnValue = 0;
        for (UUID uuid : uuids) 
        {
            returnValue++;
        }
        return returnValue;
    }

    public void updateInformation()
    {
        try 
        {
            BufferedReader fileIn = new BufferedReader(new FileReader(file));
            String fileCurrentLine;
            int lineIndex = 0;
            while ((fileCurrentLine = fileIn.readLine()) != null)
            {
                contents += fileCurrentLine + "\n";
                if(lineIndex < uuids.length)
                {
                    uuids[lineIndex] = UUID.fromString(fileCurrentLine);
                }
                lineIndex++;
            }

            fileIn.close();

        } 
        catch (Exception e) 
        {
            Utils.logError(e.getMessage());
        }
    }
    
    public boolean add(UUID addition)
    {
        if (getUUIDCount() >= getCapacity()){ return false; } // do not add if we've reached capacity

        contents += addition.toString() + "\n";
        uuids[getUUIDCount()] = addition;
        
        return Utils.setFileContents(contents, file);
    }

}
