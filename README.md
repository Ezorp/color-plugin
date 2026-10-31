## Color (Spigot 1.8.8)
This plugin has been made originally to be used alongside a minecraft RPG UHC game. The goal is to visually mark other players if they are sus or safe.

Inspired if not serving the exact same purpose as /lg color command in Lapin's Loup Garou UHC Plugin.

Code is under MIT Licence, if you want any updates / Bugfixes, ask nicely or do it yourself.

I have no experience making Spigot Plugin and made this nearly entierly using Cursor. I don't care if the code is AI written as long as I can read it and it works.

Any other version that is not 1.8.8 is not tested, it may works, but I won't do bugfixes if the bug don't happend in 1.8.8 server.

The plugin sets the color of the player's nametag (above their head) and tab list name to the color chosen with the /color command. As a second way to tell players apart, /colordot puts a colored dot (`●`) before their name.

The plugin never modifies the server's scoreboards. Sidebars, health under names and in the tab list, and other plugins' teams all keep working. Colors are sent only to the player who chose them. ProtocolLib is optional but recommended when other plugins also manage nametags (see `docs/DEPLOY.md`).

### Requirements
- Server: Spigot 1.8.8. ProtocolLib is optional but recommended.
- Building: JDK 8 and Maven 3.8+

### Build
See `docs/BUILD.md`.

### Deploy
See `docs/DEPLOY.md`.

### Usage
- `/color` opens a player picker GUI (with online player's heads) for quick selection.
- `/color <player> <player2> ...` (up to 10 targets) opens a GUI for the caller.
- Caller selects a wool: 16 classic colors; white resets to default for the caller's view.
- `/colordot` and `/colordot <player> <player2> ...` work the same way but add a colored dot before the name. White is a white dot here; the barrier removes the dot. A player can have both a name color and a dot.
- Colors are local to the caller and non-persistent (disconnecting and reconnecting to the server don't reset the sets colors, but if the server restarts, everything is cleared (and yes it's a feature)).
