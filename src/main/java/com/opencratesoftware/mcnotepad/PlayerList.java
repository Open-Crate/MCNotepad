package com.opencratesoftware.mcnotepad;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.UUID;

import com.opencratesoftware.mcnotepad.utils.Config;
import com.opencratesoftware.mcnotepad.utils.Utils;

/* Class for lists that store player UUIDs for any purpose */
public class PlayerList 
{
    protected UUID[] uuids;

    protected String contents = "";

    protected File file;

    protected Integer uuidCount;

    boolean Initialized = false;

    protected PlayerList(){}

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

    // in most cases using this will get better performance than getUUIDsRaw as this will only have non null UUIDS in the array, however the overhead of this may not be worth it in some cases, and in those cases use getUUIDsRaw
    public UUID[] getUUIDs()
    {
        UUID[] returnValue = new UUID[getUUIDCount()];

        for (int i = 0; i < returnValue.length; i++) 
        {
            returnValue[i] = uuids[i];    
        }

        return returnValue;
    }

    // returns all UUIDs (including null ones)
    public UUID[] getUUIDsRaw()
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

    public int getUUIDCount(boolean countSlow)
    {
        if(!countSlow){ return getUUIDCount(); }

        int returnValue = 0;
        for (UUID uuid : uuids) 
        {
            if(uuid != null)
            {
                returnValue++;
            }
        }

        return returnValue;
    }

    public int getUUIDCount()
    {
        return uuidCount;
    }

    public File getFile()
    {
        return file;
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

        uuidCount = getUUIDCount(true);
    }
    
    public boolean add(UUID addition)
    {
        if (getUUIDCount() >= getCapacity()){ return false; } // do not add if we've reached capacity
        contents += addition.toString() + "\n";
        uuids[getUUIDCount()] = addition;

        uuidCount++;

        return Utils.setFileContents(contents, file);
    }

    public boolean remove(UUID uuidToRemove)
    {
        int removedUUIDIndex = -1;
        for (int i = 0; i < uuids.length; i++) 
        {
            if(uuids[i] == null){ continue; }

            if (uuids[i].equals(uuidToRemove))
            {
                uuids[i] = null;
                removedUUIDIndex = i;
                break;
            }
        }

        if (removedUUIDIndex == -1) { return false; } // if we didn't find it then stop

        for (int i = removedUUIDIndex + 1; i < uuids.length; i++) // new for loop starting where we left off instead of same with branch to skip running the branch when searching
        {
            uuids[i - 1] = uuids[i];
        }

        contents = Utils.removeLineFromString(contents, removedUUIDIndex); 

        uuidCount--; 

        return Utils.setFileContents(contents, file); 
    }
    
    ///////////////////////
    // memory management //
    ///////////////////////

    protected static PlayerList[] lists;

    public static void initializeListMemory()
    {
        lists = new PlayerList[Config.getMaxPlayerListsInMemory()];
    }
    
    public static PlayerList getList(File listFile)
    {
        for (int i = 0; i < lists.length; i++)
        {
            if (lists[i] != null)
            {
                if (lists[i].getFile() == listFile)
                {
                    return lists[i];
                }
            }
        }

        PlayerList newList = new PlayerList(listFile, Config.getPlayerListCapacity());
        
        addListToMemory(newList);

        return newList;
    }

    public static int addListToMemory(PlayerList list)
    {
        for (int i = 0; i < lists.length; i++)
        {
            if (lists[i] == null)
            {
                lists[i] = list;
                return i;
            }
        }

        for (int i = 0; i < lists.length - 1; i++) 
        {
            lists[i] = lists[i + 1];
        }

        lists[lists.length - 1] = list;

        return lists.length - 1;
    }
}
