## Deployment Guide

### Requirements
- Spigot 1.8.8 server (other versions are untested, and the plugin disables itself if it can't find the 1.8.8 server internals)
- Optional: ProtocolLib (4.8.0 tested). Recommended if other plugins also manage nametags or teams (see below).

### Install
1. Build the plugin (see `docs/BUILD.md`) or download a JAR from the GitHub releases.
2. Copy the JAR to your server's `plugins/` folder, plus ProtocolLib if you want it.
3. Start or restart the server. With ProtocolLib, the console logs "ProtocolLib found".

### Usage
- Commands:
  - `/color` opens a player picker (heads). Click a player to open the color GUI.
  - `/color <player> <player2> ...` opens the color GUI for the listed targets (up to 10).
  - `/colordot` and `/colordot <player> <player2> ...` work the same way, but put a colored dot (`●`) before the name instead of coloring it.
- The color GUI shows 16 wool colors. With `/color`, white resets the targets to their default color. With `/colordot`, white is a white dot and the barrier at the top removes the dot.
- A player can have both a name color and a dot. Each is reset separately.
- Only the caller sees the changed name color and dot (nametag and tab list). Everyone else sees the default.
- Disconnecting and reconnecting keeps the caller's choices. A full server restart resets everything.

### How it works / Compatibility
- The plugin never modifies the server's scoreboards. For each player who uses `/color`, it sends their client fake teams (one per look, i.e. dot color + name color, named `colorplg_<n>`) that exist only on that client. Sidebars, health under names and in the tab list, and other plugins' teams all keep working.
- A player with a name color is shown with your color instead of any team prefix and suffix the server gives them, for you only. A player with only a dot keeps their team prefix and suffix after the dot (cut to fit the 16-character prefix limit).
- A colored or dotted player is left unchanged (no color, no dot) while either of these applies:
  - They're invisible (invisibility potion). A colored team would show their nametag.
  - Their real team hides their nametag from you (`nametagVisibility` set to `never`, or hidden for your team).
  Their color comes back when that ends.
- Other plugins may also send team packets for a colored player (a UHC plugin assigning teams, or a nametag/tab plugin):
  - **Without ProtocolLib:** the plugin re-sends every color every half second. If another plugin overrides a color, it comes back within half a second. Plugins that keep re-adding players to their teams can make the color flicker. They can also make the client log harmless "Player is either on another team" errors.
  - **With ProtocolLib:** the plugin watches team packets and re-applies the color on the next tick. It also removes from those packets the colored players that would cause the client errors.

### Troubleshooting
- Command not found: check that the plugin loaded (`/pl`) and look at the server console for errors.
- GUI not opening: make sure the target names are online and spelled exactly. Use tab completion.
- Colors missing after reconnecting: make sure you reconnected as the same player. Mappings are stored in memory only and are wiped when the server restarts.
- Colors flicker or get lost: another plugin is overriding teams. Install ProtocolLib.
- A colored player shows no color: they're invisible, or their team hides their nametag from you (see above).

### Uninstall
1. Remove the plugin JAR (and ProtocolLib if nothing else uses it).
2. Restart the server. All state is kept in memory and is cleared on restart.
