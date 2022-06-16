package com.opencratesoftware.notepad;

import com.opencratesoftware.commands.NotepadCommand;
import com.opencratesoftware.utils.Utils;

import org.bukkit.plugin.java.JavaPlugin;

public final class Notepad  extends JavaPlugin
{
    @Override
    public void onEnable()
    {
        getConfig().options().copyDefaults();
        saveDefaultConfig();

        Utils.updateConfiguration();

        getCommand("notepad").setExecutor(new NotepadCommand());
    }
}
