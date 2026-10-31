package dev.color.service;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Holds which color each viewer assigned to each target and renders it through a private,
 * per-viewer scoreboard (one team per color, with the color code as prefix).
 * State is in memory only: it survives reconnects but not server restarts.
 */
public final class ViewColorService {

    private static final Map<UUID, Map<UUID, ChatColor>> viewerToTargetColor = new HashMap<>();
    private static final Map<UUID, Scoreboard> viewerScoreboard = new HashMap<>();

    private ViewColorService() {}

    /** Sets (or clears, when {@code color} is null) the color of each target as seen by the viewer. */
    public static void applyColorForViewer(Player viewer, List<Player> targets, ChatColor color) {
        Map<UUID, ChatColor> colors = viewerToTargetColor.computeIfAbsent(viewer.getUniqueId(), id -> new HashMap<>());
        for (Player target : targets) {
            if (color == null) colors.remove(target.getUniqueId());
            else colors.put(target.getUniqueId(), color);
        }
        if (colors.isEmpty()) viewerToTargetColor.remove(viewer.getUniqueId());
        refreshViewerScoreboard(viewer);
    }

    /** Drops the viewer's scoreboard on disconnect; their color mappings are kept for when they come back. */
    public static void detachViewer(Player viewer) {
        viewerScoreboard.remove(viewer.getUniqueId());
    }

    public static void refreshAllViewers() {
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            refreshViewerScoreboard(viewer);
        }
    }

    public static void refreshViewerScoreboard(Player viewer) {
        Map<UUID, ChatColor> colors = viewerToTargetColor.get(viewer.getUniqueId());
        if (colors == null) {
            // Viewers who never colored anyone keep the server's scoreboard
            if (viewerScoreboard.remove(viewer.getUniqueId()) != null) restoreMainScoreboard(viewer);
            return;
        }

        Scoreboard scoreboard = viewerScoreboard.computeIfAbsent(viewer.getUniqueId(),
                id -> Bukkit.getScoreboardManager().getNewScoreboard());
        for (Team team : scoreboard.getTeams()) {
            team.unregister();
        }

        Map<ChatColor, Team> teamByColor = new EnumMap<>(ChatColor.class);
        for (Map.Entry<UUID, ChatColor> entry : colors.entrySet()) {
            Player target = Bukkit.getPlayer(entry.getKey());
            // Invisible players are left out: being on a team would reveal their nametag
            if (target == null || target.hasPotionEffect(PotionEffectType.INVISIBILITY)) continue;

            ChatColor color = entry.getValue();
            Team team = teamByColor.get(color);
            if (team == null) {
                team = scoreboard.registerNewTeam("clr_" + color.ordinal());
                team.setPrefix(color.toString());
                teamByColor.put(color, team);
            }
            team.addPlayer(target);
        }

        viewer.setScoreboard(scoreboard);
    }

    public static void shutdown() {
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (viewerScoreboard.containsKey(viewer.getUniqueId())) restoreMainScoreboard(viewer);
        }
        viewerToTargetColor.clear();
        viewerScoreboard.clear();
    }

    private static void restoreMainScoreboard(Player viewer) {
        viewer.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
    }
}
