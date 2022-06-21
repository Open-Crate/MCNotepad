package com.opencratesoftware.mcnotepad.utils;

import org.bukkit.configuration.Configuration;

public class Config 
{
    private Config(){}

    private static int maxMemorizedNotes = 0;

    private static int maxMemorizedNotesPerPlayer = 0;

    private static int maxPlayerListsInMemory = 0;

    private static int playerListCapacity = 0;

    public static void setConfigValues(Configuration config)
    {
        maxMemorizedNotes = config.getInt("max-notes-in-memory");
        maxMemorizedNotesPerPlayer = config.getInt("max-notes-in-memory-per-player");
        maxPlayerListsInMemory = config.getInt("max-player-lists-in-memory");
        playerListCapacity = config.getInt("player-list-capacity");
    }    

    public static int getMaxMemorizedNotes() { return maxMemorizedNotes; }

    public static int getMaxMemorizedNotesPerPlayer() { return maxMemorizedNotesPerPlayer; }

    public static int getMaxPlayerListsInMemory() { return maxPlayerListsInMemory; }

    public static int getPlayerListCapacity() { return playerListCapacity; }
    
}
