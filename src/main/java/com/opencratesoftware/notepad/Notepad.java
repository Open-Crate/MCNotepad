package com.opencratesoftware.notepad;

import com.opencratesoftware.commands.NotepadCommand;

import org.bukkit.plugin.java.JavaPlugin;

public final class Notepad  extends JavaPlugin
{
    @Override
    public void onEnable()
    {
        getCommand("notepad").setExecutor(new NotepadCommand());
    }
}
