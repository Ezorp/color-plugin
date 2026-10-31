# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Spigot plugin (target: **Minecraft 1.8.8 only**, Java 8) that lets a player privately color other players' nametags and tab-list names via `/color`, or put a colored dot before them via `/colordot`. Built for RPG/UHC games to mark players as "sus" or "safe". Each viewer's colors are visible only to that viewer.

## Build

```bash
mvn clean package -DskipTests   # JAR lands in target/color-<version>.jar
```

There are no tests and no linter. Verification is manual: drop the JAR into a Spigot 1.8.8 server's `plugins/` folder and test in-game, with and without ProtocolLib.

When bumping the version, update it in **both** `pom.xml` and `src/main/resources/plugin.yml`.

## Constraints

- Code must compile against `spigot-api 1.8.8-R0.1-SNAPSHOT` with Java 8 source/target. Avoid APIs added after 1.8.8 (e.g. no `EntityPotionEffectEvent`, no `Material` names from 1.13+ — use `WOOL` with data values, `SKULL_ITEM`, etc.). Bugs that only reproduce on other versions are out of scope.

## Architecture

The plugin never modifies server scoreboards. Colors are shown by sending each viewer **fake team packets** that exist only on their client.

- `service/ViewColorService` is the source of truth: per `ColorMode` (`NAME` for `/color`, `DOT` for `/colordot`), viewer UUID → (target UUID → `ChatColor`), stored as static maps. It is kept in memory only: it's wiped on server restart by design, but it survives a viewer disconnecting and reconnecting. `desiredTeams(viewer)` turns it into `TeamStyle` (prefix + suffix) → target names. A client puts a player in only one team, so the dot and name color share one prefix: `<dot>● <name color>`. With a dot but no name color, the target's real team prefix/suffix is kept after the dot. It leaves out targets that are offline, invisible, or whose real team (on the viewer's current scoreboard) hides their nametag from the viewer, since our teams always show nametags.
- `nametag/ClientTeams` tracks which fake teams (one per `TeamStyle`, named `colorplg_<n>` with `n` allocated per viewer, never reusing a name within a sync) and members each viewer's client has, and `sync` sends only the difference. Players who leave our teams are re-added to their real team (mode 3), or removed (mode 4) if they have none. **The tracked state must match the client exactly.** The 1.8 client errors on create-existing / modify-unknown team packets, and on removing a player from a team they're not in. That's why the state is updated before sending, and dropped on quit with `detachViewer`.
- `nametag/TeamPackets` builds `PacketPlayOutScoreboardTeam` by reflection on the 1.8.8 server internals (obfuscated fields `a` name, `c` prefix, `d` suffix, `g` players, `h` mode), so the build only needs the Spigot API. `init()` runs in `onEnable`, and the plugin disables itself if it fails.
- `nametag/ProtocolLibHook` is optional (`softdepend`). It is only loaded when ProtocolLib is enabled, so don't reference ProtocolLib classes anywhere else. It watches other plugins' team packets: if one adds a colored target to another team, it schedules `ViewColorService.reassert` for the next tick. It also strips colored targets from their mode-4 removals, editing a `deepClone`, because packet objects are shared between players.
- `ViewColorService.start` runs a sync every 10 ticks. That picks up invisibility and team changes (1.8.8 has no potion event). Without ProtocolLib it also *reasserts*: it re-sends every member, which undoes other plugins' team packets within half a second.

Flow: `ColorCommand(mode)` (`/color`/`/colordot` → `PlayerPickerMenu`; with `<p1> ... <p10>` → `ColorMenu`) → wool click → `ViewColorService.applyColorForViewer(viewer, mode, ...)` (`null` = reset: white wool for `NAME`, the barrier in slot 4 for `DOT`) → `ClientTeams.sync`.

GUI (`gui/`):
- Every menu extends `Menu`, which is the `InventoryHolder` of its own inventory and keeps its state (targets, page). `MenuListener` is the only registered listener for menus: it recognizes a menu with `getTopInventory().getHolder() instanceof Menu`, cancels every click and drag, and passes top-inventory clicks to `Menu.onClick(viewer, rawSlot)`. Titles are only for display, so don't parse them.
- Slots carry meaning: `ColorMenu` maps slot `9 + i` → `COLORS[i]`, and `PlayerPickerMenu` keeps the UUIDs it shows in slot order.
- `Menu.open`/`Menu.close` defer to the next tick, because opening or closing an inventory inside a click handler isn't safe.
