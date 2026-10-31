package dev.color.service;

import dev.color.nametag.ClientTeams;
import dev.color.nametag.TeamStyle;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Holds which name color and dot color each viewer assigned to each target, and shows them by putting
 * targets in fake teams that exist only on that viewer's client (see {@link ClientTeams}). Server
 * scoreboards are never modified. State is in memory only: it survives reconnects but not server restarts.
 */
public final class ViewColorService {

    private static final long REFRESH_PERIOD_TICKS = 10L;
    private static final String DOT = "● ";
    private static final int MAX_PREFIX_LENGTH = 16; // 1.8 team prefix limit

    /** Mode → viewer → target → color. */
    private static final Map<ColorMode, Map<UUID, Map<UUID, ChatColor>>> viewerToTargetColor = new EnumMap<>(ColorMode.class);
    private static BukkitTask refreshTask;

    static {
        for (ColorMode mode : ColorMode.values()) viewerToTargetColor.put(mode, new HashMap<>());
    }

    private ViewColorService() {}

    /**
     * Starts the periodic refresh, which picks up invisibility and team changes. Without a packet
     * hook it also re-sends every color, to undo other plugins' team packets.
     */
    public static void start(Plugin plugin, boolean packetHook) {
        boolean reassert = !packetHook;
        refreshTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player viewer : Bukkit.getOnlinePlayers()) {
                ClientTeams.sync(viewer, desiredTeams(viewer), reassert);
            }
        }, REFRESH_PERIOD_TICKS, REFRESH_PERIOD_TICKS);
    }

    /** Sets (or clears, when {@code color} is null) the name or dot color of each target as seen by the viewer. */
    public static void applyColorForViewer(Player viewer, ColorMode mode, List<Player> targets, ChatColor color) {
        Map<UUID, Map<UUID, ChatColor>> byViewer = viewerToTargetColor.get(mode);
        Map<UUID, ChatColor> colors = byViewer.computeIfAbsent(viewer.getUniqueId(), id -> new HashMap<>());
        for (Player target : targets) {
            if (color == null) colors.remove(target.getUniqueId());
            else colors.put(target.getUniqueId(), color);
        }
        if (colors.isEmpty()) byViewer.remove(viewer.getUniqueId());
        refreshViewer(viewer);
    }

    public static void refreshAllViewers() {
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            refreshViewer(viewer);
        }
    }

    public static void refreshViewer(Player viewer) {
        ClientTeams.sync(viewer, desiredTeams(viewer), false);
    }

    /** Re-sends all of the viewer's colors, after another plugin's team packet may have overridden them. */
    public static void reassert(Player viewer) {
        ClientTeams.sync(viewer, desiredTeams(viewer), true);
    }

    /** The viewer's client is gone; their color mappings are kept for when they come back. */
    public static void detachViewer(Player viewer) {
        ClientTeams.forget(viewer.getUniqueId());
    }

    public static void shutdown() {
        if (refreshTask != null) {
            refreshTask.cancel();
            refreshTask = null;
        }
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            ClientTeams.clear(viewer);
        }
        for (Map<UUID, Map<UUID, ChatColor>> byViewer : viewerToTargetColor.values()) byViewer.clear();
    }

    /** Style → names of the online targets the viewer should currently see with that style. */
    private static Map<TeamStyle, Set<String>> desiredTeams(Player viewer) {
        Map<TeamStyle, Set<String>> teams = new HashMap<>();
        Map<UUID, ChatColor> nameColors = colorsOf(ColorMode.NAME, viewer);
        Map<UUID, ChatColor> dotColors = colorsOf(ColorMode.DOT, viewer);
        if (nameColors.isEmpty() && dotColors.isEmpty()) return teams;

        Set<UUID> targets = new HashSet<>(nameColors.keySet());
        targets.addAll(dotColors.keySet());
        Scoreboard board = viewer.getScoreboard();
        Team viewerTeam = board.getEntryTeam(viewer.getName());
        for (UUID id : targets) {
            Player target = Bukkit.getPlayer(id);
            if (target == null) continue;
            Team realTeam = board.getEntryTeam(target.getName());
            if (!canColor(viewer, viewerTeam, target, realTeam)) continue;
            TeamStyle style = style(nameColors.get(id), dotColors.get(id), realTeam);
            teams.computeIfAbsent(style, s -> new HashSet<>()).add(target.getName());
        }
        return teams;
    }

    private static Map<UUID, ChatColor> colorsOf(ColorMode mode, Player viewer) {
        Map<UUID, ChatColor> colors = viewerToTargetColor.get(mode).get(viewer.getUniqueId());
        return colors == null ? Collections.<UUID, ChatColor>emptyMap() : colors;
    }

    /**
     * How a target looks: the dot (if any), then the name color. Without a name color, the target keeps
     * their real team's prefix and suffix so the dot doesn't hide e.g. a UHC team color.
     */
    private static TeamStyle style(ChatColor nameColor, ChatColor dotColor, Team realTeam) {
        if (dotColor == null) return new TeamStyle(nameColor.toString(), "");

        String namePrefix = "";
        String suffix = "";
        if (nameColor != null) {
            namePrefix = nameColor.toString();
        } else if (realTeam != null) {
            namePrefix = realTeam.getPrefix();
            suffix = realTeam.getSuffix();
        }
        // Formatting carries over from the dot, so the name needs its own color (or a reset)
        if (!startsWithColor(namePrefix)) namePrefix = ChatColor.RESET + namePrefix;

        String prefix = dotColor + DOT + namePrefix;
        if (prefix.length() > MAX_PREFIX_LENGTH) {
            prefix = prefix.substring(0, MAX_PREFIX_LENGTH);
            // Don't leave a dangling color char
            if (prefix.charAt(MAX_PREFIX_LENGTH - 1) == ChatColor.COLOR_CHAR) prefix = prefix.substring(0, MAX_PREFIX_LENGTH - 1);
        }
        return new TeamStyle(prefix, suffix);
    }

    private static boolean startsWithColor(String text) {
        if (text.length() < 2 || text.charAt(0) != ChatColor.COLOR_CHAR) return false;
        ChatColor code = ChatColor.getByChar(text.charAt(1));
        return code != null && (code.isColor() || code == ChatColor.RESET);
    }

    /**
     * Whether moving the target into a fake team keeps what the viewer is allowed to see. Our teams always
     * show nametags, so targets whose nametag is meant to be hidden from this viewer are left alone.
     */
    private static boolean canColor(Player viewer, Team viewerTeam, Player target, Team realTeam) {
        if (target.hasPotionEffect(PotionEffectType.INVISIBILITY)) return false;

        if (realTeam == null) return true;
        // Moving the viewer out of their own team would change how they see their teammates
        if (target.equals(viewer)) return false;

        boolean sameTeam = viewerTeam != null && viewerTeam.getName().equals(realTeam.getName());
        switch (realTeam.getNameTagVisibility()) {
            case NEVER:                return false;
            case HIDE_FOR_OTHER_TEAMS: return sameTeam;
            case HIDE_FOR_OWN_TEAM:    return !sameTeam;
            default:                   return true;
        }
    }
}
