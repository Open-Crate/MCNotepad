package com.opencratesoftware.structs;

import java.util.UUID;

public class PlayerListEntry 
{
    public PlayerListEntry(UUID inUUID)
    {
        uuid = inUUID;
    }
    public UUID uuid;
    
    public Variable[] Attributes;
}