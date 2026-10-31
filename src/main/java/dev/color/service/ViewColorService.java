package dev.color.service;

import dev.color.nametag.ClientTeams;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Holds which color each viewer assigned to each target, and shows it by putting targets in fake
 * teams that exist only on that viewer's client (see {@link ClientTeams}). Server scoreboards are
 * never modified. State is in memory only: it survives reconnects but not server restarts.
 */
public final class ViewColorService {

    private static final long REFRESH_PERIOD_TICKS = 10L;

    private static final Map<UUID, Map<UUID, ChatColor>> viewerToTargetColor = new HashMap<>();
    private static BukkitTask refreshTask;

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

    /** Sets (or clears, when {@code color} is null) the color of each target as seen by the viewer. */
    public static void applyColorForViewer(Player viewer, List<Player> targets, ChatColor color) {
        Map<UUID, ChatColor> colors = viewerToTargetColor.computeIfAbsent(viewer.getUniqueId(), id -> new HashMap<>());
        for (Player target : targets) {
            if (color == null) colors.remove(target.getUniqueId());
            else colors.put(target.getUniqueId(), color);
        }
        if (colors.isEmpty()) viewerToTargetColor.remove(viewer.getUniqueId());
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
        viewerToTargetColor.clear();
    }

    /** Color → names of the online targets the viewer should currently see in that color. */
    private static Map<ChatColor, Set<String>> desiredTeams(Player viewer) {
        Map<ChatColor, Set<String>> teams = new EnumMap<>(ChatColor.class);
        Map<UUID, ChatColor> colors = viewerToTargetColor.get(viewer.getUniqueId());
        if (colors == null) return teams;

        Scoreboard board = viewer.getScoreboard();
        Team viewerTeam = board.getEntryTeam(viewer.getName());
        for (Map.Entry<UUID, ChatColor> entry : colors.entrySet()) {
            Player target = Bukkit.getPlayer(entry.getKey());
            if (target == null || !canColor(viewer, viewerTeam, target, board)) continue;
            teams.computeIfAbsent(entry.getValue(), c -> new HashSet<>()).add(target.getName());
        }
        return teams;
    }

    /**
     * Whether moving the target into a fake team keeps what the viewer is allowed to see. Our teams always
     * show nametags, so targets whose nametag is meant to be hidden from this viewer are left alone.
     */
    private static boolean canColor(Player viewer, Team viewerTeam, Player target, Scoreboard board) {
        if (target.hasPotionEffect(PotionEffectType.INVISIBILITY)) return false;

        Team realTeam = board.getEntryTeam(target.getName());
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
