package com.opencratesoftware.mcnotepad;

import java.io.File;
import java.util.UUID;

import com.opencratesoftware.mcnotepad.structs.CommandData;
import com.opencratesoftware.mcnotepad.structs.PlayerListEntry;
import com.opencratesoftware.mcnotepad.structs.TrustPermissions;
import com.opencratesoftware.mcnotepad.structs.Variable;
import com.opencratesoftware.mcnotepad.utils.Config;
import com.opencratesoftware.mcnotepad.utils.Utils;

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

    /* Returns trust permissions that corresponds to what trust has been given to a player by the owner of the note named noteName.
    If note name does not include ":", for security purposes we will say no permissions incase a function forgets to use ":" to specify the player.
    This means that even if the owner of the note is playerUUID then you must specify who the noteName owner is either UUID:Name */
    public static TrustPermissions GetUserPermissionsForNote(UUID playerUUID, String noteID)
    {
        // begin checks for if everything is valid, and if not, then assume no permission
        if (playerUUID == null) { return new TrustPermissions(); } // ensure playerUUID is valid

        if (noteID.length() < 38){ return new TrustPermissions(); } // ensure the length of noteID is long enough to fit a UUID, ':' and a one char long note name
        
        if (!noteID.contains(":")) { return new TrustPermissions(); } // ensure there is a : to signify the UUID definition

        UUID trusterUUID = UUID.fromString(noteID.substring(0, noteID.indexOf(':'))); // Define variable for the truster's uuid (note owner)

        if (trusterUUID == null) { return new TrustPermissions(); }  // ensure the trusterUUID is not invalid
        
        String noteName = noteID.substring(37); // the note name should be everything after 'UUID:' and UUIDs have a constant char length, remove everything before

        TrustList trustList = TrustList.getList(Utils.getUserTrustFile(trusterUUID));

        PlayerListEntry[] trustListEntries = trustList.getEntries();

        PlayerListEntry entry = null;

        for (PlayerListEntry playerListEntry : trustListEntries) 
        {
            if (playerListEntry.uuid == playerUUID) 
            { 
                entry = playerListEntry; 
            }    
        }
        
        if (entry == null) { return new TrustPermissions(); } // if we failed to find the entry, no trust for this user at all.

        TrustPermissions returnValue = new TrustPermissions();

        for (Variable attribute : entry.Attributes) 
        {
            if (attribute.Name == "\\ALL" || attribute.Name == noteName) 
            { 
                CommandData permissions = Utils.formatCommand("name " + attribute.Value, " ");
                
                TrustPermissions setPermissions = new TrustPermissions(); 

                for (String perm : permissions.params) 
                {
                    if (perm == "read")
                    {
                        setPermissions.read = true;
                    }
                    else if (perm == "write")
                    {
                        setPermissions.write = true;
                    }
                }
                
                returnValue.read = setPermissions.read;
                returnValue.write = setPermissions.write;
                /* set permissions this way so that if a user, for example defines with \ALL first, then defines with the explicit note name,
                 * as long as they leave the explicit note name define blank, it will set everything to false, or only include what they explicitly define.
                 * in this particular example, \ALL priority things shown below should make this uneccessary, however, this could just help if something 
                 * undesirable/a glitch occurs.
                 */
                if (attribute.Name == noteName)
                {
                    break; // break the loop if we've explictly defined, so that no more overrides may occur.
                }
            }
        }

        return returnValue;
    }
}
