package com.opencratesoftware.mcnotepad;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.UUID;

import com.opencratesoftware.mcnotepad.structs.PlayerListEntry;
import com.opencratesoftware.mcnotepad.structs.Variable;
import com.opencratesoftware.mcnotepad.utils.Config;
import com.opencratesoftware.mcnotepad.utils.Utils;

import net.md_5.bungee.api.ChatColor;

/* Class for lists that store player UUIDs for any purpose */
public class PlayerList 
{
    protected PlayerListEntry[] entries;

    protected String contents = "";

    protected File file;

    protected Integer uuidCount;

    protected UUID owner;

    boolean Initialized = false;

    protected PlayerList(){}

    PlayerList(File listFile, int capacity)
    {
        file = listFile;
        entries = new PlayerListEntry[capacity];
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
            returnValue[i] = entries[i].uuid;    
        }

        return returnValue;
    }

    public PlayerListEntry[] getEntries()
    {
        PlayerListEntry[] returnValue = new PlayerListEntry[getUUIDCount()];

        for (int i = 0; i < returnValue.length; i++) 
        {
            returnValue[i] = entries[i]; 
        }

        return returnValue;
    }

    // returns all UUIDs (including null ones) no longer recommended over getEntriesRaw()
    public UUID[] getUUIDsRaw()
    {
        UUID[] returnValue = new UUID[entries.length];

        for (int i = 0; i < returnValue.length; i++) 
        {
            if (entries[i] != null)
            {
                returnValue[i] = entries[i].uuid;    
            }
        }

        return returnValue;
    }

    public PlayerListEntry[] getEntriesRaw()
    {
        return entries;
    }

    public String getContents()
    {
        return contents;
    }

    public int getCapacity()
    {
        return entries.length;
    }

    public int getUUIDCount(boolean countSlow)
    {
        if(!countSlow){ return getUUIDCount(); }

        int returnValue = 0;

        for (PlayerListEntry entry : entries) 
        {
            if(entry != null)
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

    public boolean isValid()
    {
        return Initialized;
    }

    public void updateInformation()
    {
        try 
        {
            BufferedReader fileIn = new BufferedReader(new FileReader(file));
            String fileCurrentLine;
            int lineIndex = 0;
            uuidCount = 0;
            while ((fileCurrentLine = fileIn.readLine()) != null)
            {
                contents += fileCurrentLine + "\n";
                if(lineIndex < entries.length)
                {
                    if (fileCurrentLine.length() > 30)
                    {
                        entries[uuidCount] = new PlayerListEntry(fileCurrentLine);
                        uuidCount++;
                    }
                }
                lineIndex++;
            }

            fileIn.close();

        } 
        catch (Exception e) 
        {
            Utils.logError(e.getMessage());
        }

        owner = UUID.fromString(file.getName());
    }
    
    /* child classes may override this to modify the entry being added by the 'add' function */
    protected PlayerListEntry child_AddEntryModifier(PlayerListEntry entry)
    {
        return entry;
    }

    public FunctionResult add(PlayerListEntry addition)
    {
        if (addition == null) { return new FunctionResult(false, ChatColor.RED + "Failed to add entry due to not existing. (Entry was null)"); }

        for (int i = 0; i < getEntries().length; i++) 
        {
            if (entries[i].uuid.equals(addition.uuid))
            {
                remove(entries[i].uuid); // if this player is already added to the list, remove them so this replaces them.
            }
        }

        if (getUUIDCount() >= getCapacity()){ return new FunctionResult(false, ChatColor.RED + "List has reached maximum capacity.", "full"); } // do not add if we've reached capacity
        
        for (int i = 0; i < addition.Attributes.length; i++) 
        {
            for (int j = 0; j < addition.Attributes.length; j++) 
            {
                if (addition.Attributes[i].Name.equals(addition.Attributes[j].Name))
                {
                    if (j == i) { continue; } // if we found ourself, continue.

                    addition.Attributes[i].Name = "";
                    break;
                }
            }
        }

        addition = child_AddEntryModifier(addition);
        String attributesStringized = Utils.mergeArray(addition.Attributes, " | ", true);
        if (attributesStringized.length() > 0)
        {
            attributesStringized = " | " + attributesStringized;
        }
        contents += addition.uuid.toString() +  attributesStringized + "\n";
        entries[getUUIDCount()] = addition;

        uuidCount++;

        return Utils.setFileContents(contents, file);
    }
    
    public FunctionResult remove(UUID uuidToRemove)
    {
        int removedUUIDIndex = -1;
        for (int i = 0; i < entries.length; i++) 
        {
            if(entries[i] == null){ continue; }

            if (entries[i].uuid.equals(uuidToRemove))
            {
                entries[i] = null;
                removedUUIDIndex = i;
                break;
            }
        }

        if (removedUUIDIndex == -1) { return new FunctionResult(false, ChatColor.RED + "Could not find player in list.", "notfound"); } // if we didn't find it then stop

        for (int i = removedUUIDIndex + 1; i < entries.length; i++) // new for loop starting where we left off instead of same with branch to skip running the branch when searching
        {
            entries[i - 1] = entries[i];
        }

        contents = Utils.removeLineFromString(contents, removedUUIDIndex); 

        uuidCount--; 

        return Utils.setFileContents(contents, file); 
    }

    public FunctionResult addAttributesToEntry(PlayerListEntry addition)
    {
        PlayerListEntry oldEntry = getEntryByUUID(addition.uuid);
        PlayerListEntry newEntry = addition;
        
        if (oldEntry != null)
        {
            int oldAttributesLength = oldEntry.Attributes.length;
            Variable[] oldAttributes = oldEntry.Attributes;
            oldEntry.Attributes = new Variable[oldEntry.Attributes.length + newEntry.Attributes.length];

            for (int i = 0; i < oldAttributesLength; i++) 
            {
                oldEntry.Attributes[i] = oldAttributes[i];
            }

            for (int i = 0; i < oldEntry.Attributes.length - oldAttributesLength; i++) 
            {
                oldEntry.Attributes[i + oldAttributesLength] = newEntry.Attributes[i];
            }

            newEntry.Attributes = oldEntry.Attributes;
        }

        return add(newEntry);
    }

    public FunctionResult removeAttributesFromEntry(PlayerListEntry removal) // this function may be slow, perhaps optimize in future?
    {
        PlayerListEntry entry = getEntryByUUID(removal.uuid);
        
        if (entry != null)
        {
            for (int i = 0; i < entry.Attributes.length; i++) 
            {
                if (Utils.findInArray(removal.Attributes, entry.Attributes[i]) != -1)
                {
                    Variable[] newAttributes = new Variable[entry.Attributes.length -1];
                    
                    int oldAttributesIndex = 0;
                    for (int j = 0; j < newAttributes.length; j++) 
                    {
                        if (oldAttributesIndex != i)
                        {
                            newAttributes[j] = entry.Attributes[oldAttributesIndex];
                        }
                        else
                        {
                            j--; // dont continue to next newattributes index as it will be left null
                        }

                        oldAttributesIndex++; // but do add to oldattributes to skip the unwanted index
                    }

                    entry.Attributes = newAttributes; // update attributes
                    
                    i--;
                }
            }
        }

        return add(entry);
    }

    /* Request your playerlist to end itself, how cruel :( */
    public void delete()
    {
        removeListFromMemory(this);
        file.delete();
    }

    public PlayerListEntry getEntryByUUID(UUID uuid)
    {
        PlayerListEntry[] validEntries = getEntries();

        for (PlayerListEntry entry : validEntries) 
        {
            if (entry.uuid.equals(uuid)) 
            {
                return entry;
            }
        }

        return null;
    }

    ///////////////////////
    // memory management //
    ///////////////////////

    protected static PlayerList[] lists;

    public static void initializeListMemory()
    {
        lists = new PlayerList[Config.getMaxPlayerListsInMemory()];
        Utils.log("Player list array (memory) initialized with a capacity of " + Config.getMaxPlayerListsInMemory());
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

    public static FunctionResult removeListFromMemory(PlayerList list)
    {
        for (int i = 0; i < lists.length; i++)
        {
            if (lists[i] == list)
            {
                for (int j = i; j > 0; j--) 
                {
                    lists[j] = lists[j - 1];
                }
                lists[0] = null;
                
                return new FunctionResult(true, "Successfully removed list from memory.");
            }
        }

        return new FunctionResult(false, "Could not locate list in memory.", "notfound");    
    }
}
