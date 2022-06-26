package com.opencratesoftware.mcnotepad;

import com.opencratesoftware.mcnotepad.commands.NotepadCommand;
import com.opencratesoftware.mcnotepad.utils.Utils;

import org.bukkit.plugin.java.JavaPlugin;

public final class Notepad  extends JavaPlugin
{
    @Override
    public void onEnable()
    {
        saveDefaultConfig();

        Utils.updateConfiguration();

        Note.InitializeNoteMemory();

        PlayerList.initializeListMemory();

        getCommand("notepad").setExecutor(new NotepadCommand());
    }
}
