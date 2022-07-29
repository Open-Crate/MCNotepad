package com.opencratesoftware.mcnotepad.structs;

import java.util.UUID;

import com.opencratesoftware.mcnotepad.utils.Utils;

public class PlayerListEntry 
{
    public PlayerListEntry(UUID inUUID)
    {
        uuid = inUUID;
    }

    public PlayerListEntry(String Line)
    {
        CommandData LineFormatted = Utils.formatCommand(Line, " | ");
        uuid = UUID.fromString(LineFormatted.name);
        
        Attributes = new Variable[LineFormatted.params.length];
        for (int i = 0; i < Attributes.length; i++) 
        {
            CommandData AttributeFormatted = Utils.formatCommand(LineFormatted.params[i], " ");
            Attributes[i] = new Variable(AttributeFormatted.name, Utils.mergeArray(AttributeFormatted.params, " "));    
        }
        
    }

    public UUID uuid;
    
    public Variable[] Attributes;
}
