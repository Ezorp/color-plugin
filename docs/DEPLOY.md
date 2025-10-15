## Deployment Guide

### Requirements
- Spigot 1.8.8 server
- ProtocolLib (recommended for best experience)

### Install
1. Build the plugin (see `docs/BUILD.md`).
2. Copy the JAR from RELEASES or `target/` to your server's `plugins/` folder.
3. Ensure `ProtocolLib` JAR is also in `plugins/`.
4. Start or restart the server.

### Usage
- Commands:
  - `/color` — opens a player picker (heads). Click a player to open the color GUI.
  - `/color <player> <player2> ...` — directly opens the color GUI for listed targets (up to 10).
- A GUI opens for the caller with 16 wool colors; white resets.
- Only the caller sees the changed name color (nametag + tab list). Others see default.
- Disconnect/reconnect preserves the caller's choices. Full server restart resets everything.

### Compatibility
- With ProtocolLib: the plugin does not override the server's scoreboards; it sends per-viewer team packets so only name colors change.
- Without ProtocolLib: it falls back to a lightweight per-viewer scoreboard which may temporarily replace the viewer's scoreboard.

### Troubleshooting
- Command not found: verify the plugin loaded (`/pl`) and check `plugins/Color` folder logs.
- Colors not applying in tab/name: ensure `ProtocolLib` is installed and compatible with your server build.
- GUI not opening: confirm target names are online and spelled exactly; use tab completion.
- After reconnect, colors missing: ensure you reconnected as the same caller; mappings are stored in memory only and are wiped on server restart.

### Uninstall
1. Remove the plugin JAR (and ProtocolLib if unused elsewhere).
2. Restart the server. All runtime-only state is in memory and will be cleared.

