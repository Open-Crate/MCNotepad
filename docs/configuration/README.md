# Configuration

Because notepad ultimately allows users to store files on your server, it is important to configure it correctly (and any suggestions or pull requests for additional configuration or especially security vulnerabilities are appreciated)

# General Recommendations 

Any recommendations for configuring your server with notepad will be shown here.

- Limit rate in which users can use /notepad and its aliases (/note, /notes)

# Configuration File
There are mutliple configuration properties for Notepad, they can have varying purposes from limiting storage usage, memory usage, security and preventing errors.

## Reference
Because some of these properties have important roles, it is important to understand what they do before modifying them. Below is a brief overview of what they all do.

`max-notes-per-user` - The maximum amount of notes (files) a user can have saved in their own storage/directory. 

`max-note-file-size` - The maximum filesize of a given note. It is important to know that the logic behind limiting filesizes is checking if the limit is equal to or less than the current file size, then appending the additions the user intended to add in full, meaning that this may not be enforced perfectly, how much depending on `max-characters-per-line`.

`max-characters-per-line` - Maximum characters that can be added to a note at a time, or in other words maximum amount of characters that users can add to a single added line.

`max-notes-in-memory` - The maximum amount of notes that will be stored in memory for quicker access, this is shared across all players.

`player-list-capacity` - The capacity of playerlists (trust lists, alt lists). This is essentially the maximum amount of players a player can add to lists like their trust list. This does not effect notes or list files at all.

`max-player-lists-in-memory` - The maximum amount of playerlists that will be stored in memory for quicker access, this is shared across all players.

`use-character-whitelist` - Whether to use the characters in `whitelisted-characters` as a whitelist for additions to notes. The main purpose of this is to avoid players creating binary/executable files on the server, however this may not be an issue as long as those files aren't actually executed. For security purposes, recommendation is if you do not fully trust all of your players not to attempt this, leave this on, if you do trust them however disable it for additional performance when adding to notes.

`whitelisted-characters` - List of allowed characters when adding to notes. Only used if `user-character-whitelist` is true.

`use-filename-character-whitelist` - Whether to use the characters in `filename-whitelisted-characters` as a whitelist for note/file names. This is highly recommended to stay on as security implications of disabling this and `use-character-whitelist` could be major depending on the operating system you are using, and even with `use-character-whitelist` on this can cause numerous errors. The performance cost is only felt when creating new notes, so it is very minor in practice, especially considering what advantages it brings.

`filename-whitelisted-characters` - List of allowed characters in the names of notes/files. Only used if `use-filename-character-whitelist` is true.

`filename-max-characters` - Maximum characters in a filename. Depending on OS, could technically effect storage but not much. Leaving this at a low value like 20-40 however would not be very noticeable for players.

