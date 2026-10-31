# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Spigot plugin (target: **Minecraft 1.8.8 only**, Java 8) that lets a player privately color other players' nametags and tab-list names via `/color`. Built for RPG/UHC games to mark players as "sus" or "safe". Each viewer's colors are visible only to that viewer.

## Build

```bash
mvn clean package -DskipTests   # JAR lands in target/color-<version>.jar
```

There are no tests and no linter. Verification is manual: drop the JAR into a Spigot 1.8.8 server's `plugins/` folder and test in-game.

When bumping the version, update it in **both** `pom.xml` and `src/main/resources/plugin.yml`.

## Constraints

- Code must compile against `spigot-api 1.8.8-R0.1-SNAPSHOT` with Java 8 source/target. Avoid APIs added after 1.8.8 (e.g. no `EntityPotionEffectEvent`, no `Material` names from 1.13+ — use `WOOL` with data values, `SKULL_ITEM`, etc.). Bugs that only reproduce on other versions are out of scope.

## Architecture

All state lives in static maps in `service/ViewColorService`. It is kept in memory only: it's wiped on server restart by design, but it survives a viewer disconnecting and reconnecting.

- `viewerToTargetColor` maps viewer UUID → (target UUID → `ChatColor`). It is the source of truth. A viewer with no colors left is removed from it.
- `viewerScoreboard` holds one private `Scoreboard` per online viewer who has colors. `refreshViewerScoreboard` unregisters all teams and rebuilds one team per color (`clr_<ordinal>`, prefix = color code), then calls `viewer.setScoreboard(...)`. That private scoreboard is what applies the colors, so it hides the main scoreboard's sidebar for that viewer. Viewers without colors are put back on (or kept on) the main scoreboard.

Flow: `ColorCommand` (`/color` → `PlayerPickerMenu`; `/color <p1> ... <p10>` → `ColorMenu`) → wool click → `ViewColorService.applyColorForViewer` (white = `null` = reset) → scoreboard rebuild.

Things that trigger a full `refreshAllViewers()`:
- `JoinListener`: on join right away, and on quit one tick later, because the quitting player is still listed as online during `PlayerQuitEvent`. Kicks also fire `PlayerQuitEvent`. On quit, `detachViewer` drops the viewer's scoreboard but keeps their color mappings.
- `service/InvisibilityMonitor`: a repeating task that checks every 10 ticks whether the set of invisible players changed, since 1.8.8 has no potion effect event. Invisible targets are left off the teams so that a colored team doesn't reveal their nametag.

GUI (`gui/`):
- Every menu extends `Menu`, which is the `InventoryHolder` of its own inventory and keeps its state (targets, page). `MenuListener` is the only registered listener for menus: it recognizes a menu with `getTopInventory().getHolder() instanceof Menu`, cancels every click and drag, and passes top-inventory clicks to `Menu.onClick(viewer, rawSlot)`. Titles are only for display, so don't parse them.
- Slots carry meaning: `ColorMenu` maps slot `9 + i` → `COLORS[i]`, and `PlayerPickerMenu` keeps the UUIDs it shows in slot order.
- `Menu.open`/`Menu.close` defer to the next tick, because opening or closing an inventory inside a click handler isn't safe.
