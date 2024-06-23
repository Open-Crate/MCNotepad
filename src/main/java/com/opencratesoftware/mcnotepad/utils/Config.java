package com.opencratesoftware.mcnotepad.utils;

import java.util.List;

import org.bukkit.configuration.Configuration;

public class Config 
{
    private Config(){}

    private static int maxMemorizedNotes = 0;

    private static int maxNoteSize = 0;

    private static int maxNotesPerPlayer = 0;

    private static int maxPlayerListsInMemory = 0;

    private static int playerListCapacity = 0;

    private static int maxCharactersPerLine = 0;

    private static boolean useFilenameCharWhitelist = false;

    private static List<Character> filenameCharWhitelist;

    private static int maxFilenameCharacters = 0;

    private static boolean perWorldNotes = false;

    private static boolean perDimensionNotes = false;

    public static void setConfigValues(Configuration config)
    {
        maxMemorizedNotes = config.getInt("max-notes-in-memory");
        maxPlayerListsInMemory = config.getInt("max-player-lists-in-memory");
        playerListCapacity = config.getInt("player-list-capacity");
        maxNoteSize = config.getInt("max-note-file-size");
        maxNotesPerPlayer = config.getInt("max-notes-per-user");
        maxCharactersPerLine = config.getInt("max-characters-per-line");
        useFilenameCharWhitelist = config.getBoolean("use-filename-character-whitelist");
        filenameCharWhitelist = config.getCharacterList("filename-whitelisted-characters");
        maxFilenameCharacters = config.getInt("filename-max-characters");
        perWorldNotes = config.getBoolean("per-world-notes");
        perDimensionNotes = config.getBoolean("per-dimension-notes");
    }    

    public static int getMaxMemorizedNotes() { return maxMemorizedNotes; }

    public static int getMaxPlayerListsInMemory() { return maxPlayerListsInMemory; }

    public static int getPlayerListCapacity() { return playerListCapacity; }
    
    public static int getMaxNotesPerPlayer() { return maxNotesPerPlayer; }

    public static int getMaxNoteSize() { return maxNoteSize; }
    
    public static int getMaxCharactersPerLine() { return maxCharactersPerLine; }

    public static List<Character> getFilenameCharacterWhitelist() { return filenameCharWhitelist; }
    
    public static boolean getUseFilenameCharacterWhitelist() { return useFilenameCharWhitelist; } 

    public static int getMaxFilenameCharacters() { return maxFilenameCharacters; }

    public static boolean getPerWorldNotes() { return perWorldNotes; }

    public static boolean getPerDimensionNotes() { return perDimensionNotes; }
}
