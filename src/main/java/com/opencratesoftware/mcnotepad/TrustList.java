package com.opencratesoftware.mcnotepad;

import java.io.File;
import java.util.UUID;

import com.opencratesoftware.mcnotepad.utils.Config;

public class TrustList extends PlayerList 
{
    TrustList(File listFile, int capacity) { super(listFile, capacity); }

    public static TrustList getList(File listFile)
    {
        for (int i = 0; i < lists.length; i++)
        {
            if (lists[i] != null)
            {
                if (lists[i].getFile() == listFile)
                {
                    return (TrustList) lists[i];
                }
            }
        }

        TrustList newList = new TrustList(listFile, Config.getPlayerListCapacity());
        
        addListToMemory(newList);

        return newList;
    }

}
