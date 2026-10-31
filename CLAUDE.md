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
- `docs/BUILD.md` and `docs/DEPLOY.md` mention ProtocolLib and per-viewer team packets; that is stale. ProtocolLib is not a dependency in `pom.xml` and the code only uses the scoreboard approach described below.

## Architecture

All state lives in static maps in `service/ViewColorService` (in-memory only; wiped on server restart by design, but preserved across a viewer's disconnect/reconnect):

- `viewerToTargetColor`: viewer UUID → (target UUID → `ChatColor`). The source of truth.
- `viewerScoreboard`: one private `Scoreboard` per online viewer. `refreshViewerScoreboard` unregisters all teams and rebuilds one team per color (`clr_<ordinal>`, prefix = color code), then calls `viewer.setScoreboard(...)`. This per-viewer scoreboard is how coloring is achieved, so it replaces any main-scoreboard sidebar for that viewer.
- `pendingTargetsByViewer`: targets selected for the currently open color GUI, consumed on click and cleared on GUI close.

Flow: `ColorCommand` (`/color` → `PlayerPickerMenu`; `/color <p1> ... <p10>` → `ColorMenu`) → `ColorMenu.open` stores pending targets → wool click calls `ViewColorService.applyColorForViewer` (white = remove/reset) → scoreboard rebuild.

Things that trigger a full `refreshAllViewers()`:
- `JoinListener` (join/quit) and `KickListener`; on quit/kick `detachViewer` drops the viewer's scoreboard/pending state but keeps color mappings.
- `PotionEffectListener`, which is **not** an event listener: it's a `BukkitRunnable` polling every 10 ticks for invisibility changes (1.8.8 has no potion event). Invisible targets are skipped when building teams so the colored team doesn't expose their nametag.

GUI conventions:
- Menus are identified by **inventory title** (`"Choose a Color"`, `"Pick a Player (x/y)"`), compared after `ChatColor.stripColor`. Changing a title requires updating the matching click/close handlers. The picker's current page is parsed back out of its title.
- The chosen color is recovered from the wool item's display name (`ChatColor.valueOf(name.toUpperCase().replace(' ', '_'))`), so display names must stay as the enum name.
- Opening/closing inventories from click handlers is always deferred one tick via `Bukkit.getScheduler().runTask(ColorPlugin.getInstance(), ...)`.
- `ColorMenu` and `PlayerPickerMenu` are both static-helper classes and `Listener`s registered in `ColorPlugin.onEnable`.
