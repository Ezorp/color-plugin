package dev.color.service;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;

import java.util.*;

public final class ViewColorService {

    private static final Map<UUID, List<UUID>> pendingTargetsByViewer = new HashMap<UUID, List<UUID>>();
    private static final Map<UUID, Map<UUID, ChatColor>> viewerToTargetColor = new HashMap<UUID, Map<UUID, ChatColor>>();
    private static final Map<UUID, Scoreboard> viewerScoreboard = new HashMap<UUID, Scoreboard>();

    private ViewColorService() {}

    public static void rememberPendingTargets(Player viewer, List<Player> targets) {
        List<UUID> uuids = new ArrayList<UUID>();
        for (Player p : targets) uuids.add(p.getUniqueId());
        pendingTargetsByViewer.put(viewer.getUniqueId(), uuids);
    }

    public static List<Player> consumePendingTargets(Player viewer) {
        List<UUID> uuids = pendingTargetsByViewer.remove(viewer.getUniqueId());
        if (uuids == null) return Collections.emptyList();
        List<Player> players = new ArrayList<Player>();
        for (UUID id : uuids) {
            Player p = Bukkit.getPlayer(id);
            if (p != null && p.isOnline()) players.add(p);
        }
        return players;
    }

    public static void clearPendingTargets(Player viewer) {
        if (viewer == null) return;
        pendingTargetsByViewer.remove(viewer.getUniqueId());
    }

    public static void applyColorForViewer(Player viewer, List<Player> targets, ChatColor color) {
        Map<UUID, ChatColor> map = viewerToTargetColor.get(viewer.getUniqueId());
        if (map == null) {
            map = new HashMap<UUID, ChatColor>();
            viewerToTargetColor.put(viewer.getUniqueId(), map);
        }
        for (Player t : targets) {
            if (color == null) map.remove(t.getUniqueId()); else map.put(t.getUniqueId(), color);
        }
        refreshViewerScoreboard(viewer);
    }

    public static void resetViewer(Player viewer) {
        viewerToTargetColor.remove(viewer.getUniqueId());
        pendingTargetsByViewer.remove(viewer.getUniqueId());
        // no packet mode
        // If we created a fallback scoreboard for this viewer, restore main scoreboard
        Scoreboard sb = viewerScoreboard.remove(viewer.getUniqueId());
        if (sb != null) {
            ScoreboardManager mgr = Bukkit.getScoreboardManager();
            if (mgr != null) viewer.setScoreboard(mgr.getMainScoreboard());
        }
    }

    /**
     * Clean up transient viewer state on disconnect, but keep color mappings so they persist across reconnects.
     */
    public static void detachViewer(Player viewer) {
        if (viewer == null) return;
        pendingTargetsByViewer.remove(viewer.getUniqueId());
        Scoreboard sb = viewerScoreboard.remove(viewer.getUniqueId());
        if (sb != null) {
            ScoreboardManager mgr = Bukkit.getScoreboardManager();
            if (mgr != null) viewer.setScoreboard(mgr.getMainScoreboard());
        }
    }

    public static void refreshAllViewers() {
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            refreshViewerScoreboard(viewer);
        }
    }

    public static void refreshViewerScoreboard(Player viewer) {
        Scoreboard sb = viewerScoreboard.get(viewer.getUniqueId());
        ScoreboardManager mgr = Bukkit.getScoreboardManager();
        if (mgr == null) return;
        if (sb == null) {
            sb = mgr.getNewScoreboard();
            viewerScoreboard.put(viewer.getUniqueId(), sb);
        }

        // Clear previous teams
        for (Team t : new ArrayList<Team>(sb.getTeams())) {
            t.unregister();
        }

        Map<UUID, ChatColor> map = viewerToTargetColor.get(viewer.getUniqueId());
        if (map == null) map = Collections.emptyMap();

        // Create a team per color and add entries
        Map<ChatColor, Team> colorTeam = new EnumMap<ChatColor, Team>(ChatColor.class);
        for (Map.Entry<UUID, ChatColor> e : map.entrySet()) {
            Player target = Bukkit.getPlayer(e.getKey());
            if (target == null) continue;
            
            // Skip invisible players to prevent nametag visibility issue
            if (target.hasPotionEffect(PotionEffectType.INVISIBILITY)) continue;
            
            ChatColor c = e.getValue();
            if (c == null) continue;
            Team team = colorTeam.get(c);
            if (team == null) {
                String teamName = ("clr_" + c.ordinal());
                if (teamName.length() > 16) teamName = teamName.substring(0, 16);
                team = sb.registerNewTeam(teamName);
                team.setPrefix(c.toString());
                colorTeam.put(c, team);
            }
            // Add by entry name (player name) for 1.8 compatibility
            team.addPlayer(target);
        }

        viewer.setScoreboard(sb);
    }

    public static void shutdown() {
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            try { resetViewer(viewer); } catch (Throwable ignored) {}
        }
        pendingTargetsByViewer.clear();
        viewerToTargetColor.clear();
        viewerScoreboard.clear();
    }
}



