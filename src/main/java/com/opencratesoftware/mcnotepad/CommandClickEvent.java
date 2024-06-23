package com.opencratesoftware.mcnotepad;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;

public class CommandClickEvent implements ClickCallback<Audience>
{
    CommandClickEvent(String inClickCommand)
    {
        lastClickTick = Bukkit.getServer().getCurrentTick() - 1000;

        clickCommand = inClickCommand;
        
        if (clickCommand.indexOf("/", 0) == 0)
        {
            clickCommand = clickCommand.substring(1);
        }
    }
    @Override
    public void accept(Audience audience)
    {
        if (Math.abs(lastClickTick - Bukkit.getServer().getCurrentTick()) > 10)
        {
            lastClickTick = Bukkit.getServer().getCurrentTick();
            return;
        }

        Player player = (Player)audience;
        player.performCommand(clickCommand);
    }

    String clickCommand = "";
    int lastClickTick = -1;
}
