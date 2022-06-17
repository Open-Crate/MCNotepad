package com.opencratesoftware.mcnotepad.utils;

import org.bukkit.configuration.Configuration;

public class Config 
{
    private Config(){}

    private static int maxMemorizedNotes = 0;

    private static int maxMemorizedNotesPerPlayer = 0;

    public static void setConfigValues(Configuration config)
    {
        maxMemorizedNotes = config.getInt("max-notes-in-memory");
        maxMemorizedNotesPerPlayer = config.getInt("max-notes-in-memory-per-player");
    }    

    public static int getMaxMemorizedNotes()
    {
        return maxMemorizedNotes;
    }

    public static int getMaxMemorizedNotesPerPlayer()
    {
        return maxMemorizedNotesPerPlayer;
    }
}
