package com.opencratesoftware.mcnotepad;

import java.io.File;

import com.opencratesoftware.mcnotepad.utils.Config;

public class AltList extends PlayerList 
{
    AltList(File listFile, int capacity) { super(listFile, capacity); }

    public static AltList getList(File listFile)
    {
        for (int i = 0; i < lists.length; i++)
        {
            if (lists[i] != null)
            {
                if (lists[i].getFile() == listFile)
                {
                    return (AltList) lists[i];
                }
            }
        }

        AltList newList = new AltList(listFile, Config.getPlayerListCapacity());
        
        addListToMemory(newList);

        return newList;
    }

}
