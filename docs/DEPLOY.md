## Deployment Guide

### Requirements
- Spigot 1.8.8 server (other versions are untested)

### Install
1. Build the plugin (see `docs/BUILD.md`) or download a JAR from the GitHub releases.
2. Copy the JAR to your server's `plugins/` folder.
3. Start or restart the server.

### Usage
- Commands:
  - `/color` opens a player picker (heads). Click a player to open the color GUI.
  - `/color <player> <player2> ...` opens the color GUI for the listed targets (up to 10).
- The color GUI shows 16 wool colors. White resets the targets to their default color.
- Only the caller sees the changed name color (nametag and tab list). Everyone else sees the default.
- Disconnecting and reconnecting keeps the caller's choices. A full server restart resets everything.

### How it works / Compatibility
- Each player who has colored someone gets a private scoreboard with one team per color. The team prefix sets the name color.
- Because of this, a player who uses `/color` no longer sees the main scoreboard's sidebar or objectives. Players who never used `/color`, or who reset every color to white, keep the server's main scoreboard.
- Invisible players (invisibility potion) are left off the teams so that their nametag stays hidden. Their color comes back when the effect ends.

### Troubleshooting
- Command not found: check that the plugin loaded (`/pl`) and look at the server console for errors.
- GUI not opening: make sure the target names are online and spelled exactly. Use tab completion.
- Colors missing after reconnecting: make sure you reconnected as the same player. Mappings are stored in memory only and are wiped when the server restarts.
- Another plugin's scoreboard disappears for a player: see "How it works / Compatibility" above.

### Uninstall
1. Remove the plugin JAR.
2. Restart the server. All state is kept in memory and is cleared on restart.
