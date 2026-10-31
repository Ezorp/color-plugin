package dev.color.nametag;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks the fake color teams that exist on each viewer's client and sends the packets needed to
 * move from what the client has to what it should have.
 *
 * The client errors on packets for a team it doesn't have (or creating one it already has), so the
 * tracked state must match the client exactly: it starts empty on join and is dropped on quit.
 */
public final class ClientTeams {

    private static final String TEAM_PREFIX = "colorplg_";

    /** Viewer → color → player names in that fake team, as last sent. Main thread only. */
    private static final Map<UUID, Map<ChatColor, Set<String>>> sentTeams = new HashMap<>();
    /** Viewer → every name currently in one of their fake teams. Read from packet threads by {@link ProtocolLibHook}. */
    private static final Map<UUID, Set<String>> coloredNames = new ConcurrentHashMap<>();

    private ClientTeams() {}

    public static boolean isOwnTeam(String team) {
        return team.startsWith(TEAM_PREFIX);
    }

    public static Set<String> coloredNames(UUID viewer) {
        Set<String> names = coloredNames.get(viewer);
        return names == null ? Collections.<String>emptySet() : names;
    }

    /**
     * Brings the viewer's client to the {@code desired} teams. With {@code reassert}, members already sent
     * are added again, which pulls them back if another plugin's team packet moved them out.
     */
    public static void sync(Player viewer, Map<ChatColor, Set<String>> desired, boolean reassert) {
        Map<ChatColor, Set<String>> sent = sentTeams.get(viewer.getUniqueId());
        if (sent == null) sent = Collections.emptyMap();

        Map<ChatColor, Set<String>> next = new EnumMap<>(ChatColor.class);
        Map<String, ChatColor> leaving = new HashMap<>(); // name → fake team it was in
        for (Map.Entry<ChatColor, Set<String>> e : sent.entrySet()) {
            for (String name : e.getValue()) leaving.put(name, e.getKey());
        }
        for (Map.Entry<ChatColor, Set<String>> e : desired.entrySet()) {
            if (e.getValue().isEmpty()) continue;
            next.put(e.getKey(), new HashSet<>(e.getValue()));
            leaving.keySet().removeAll(e.getValue());
        }

        // Record the new state before sending, so the ProtocolLib hook judges our packets against it
        sentTeams.put(viewer.getUniqueId(), next);
        Set<String> names = new HashSet<>();
        for (Set<String> members : next.values()) names.addAll(members);
        coloredNames.put(viewer.getUniqueId(), Collections.unmodifiableSet(names));

        Set<ChatColor> colors = EnumSet.noneOf(ChatColor.class);
        colors.addAll(sent.keySet());
        colors.addAll(next.keySet());
        for (ChatColor color : colors) {
            Set<String> before = sent.get(color);
            Set<String> after = next.get(color);
            if (before == null) {
                TeamPackets.send(viewer, teamName(color), TeamPackets.CREATE, color.toString(), after);
            } else if (after == null) {
                TeamPackets.send(viewer, teamName(color), TeamPackets.REMOVE, null, Collections.<String>emptyList());
            } else {
                List<String> toAdd = new ArrayList<>(after);
                if (!reassert) toAdd.removeAll(before);
                // Adding a player moves them out of whatever team they were in, so moves between colors need nothing else
                if (!toAdd.isEmpty()) TeamPackets.send(viewer, teamName(color), TeamPackets.ADD_PLAYERS, null, toAdd);
            }
        }

        // Players no longer colored go back to their real team on the viewer's scoreboard, if any
        for (Map.Entry<String, ChatColor> e : leaving.entrySet()) {
            String name = e.getKey();
            Team realTeam = viewer.getScoreboard().getEntryTeam(name);
            if (realTeam != null) {
                TeamPackets.send(viewer, realTeam.getName(), TeamPackets.ADD_PLAYERS, null, Collections.singletonList(name));
            } else if (next.containsKey(e.getValue())) {
                TeamPackets.send(viewer, teamName(e.getValue()), TeamPackets.REMOVE_PLAYERS, null, Collections.singletonList(name));
            }
        }

        if (next.isEmpty()) forget(viewer.getUniqueId());
    }

    /** Removes every fake team from the viewer's client and restores real teams. */
    public static void clear(Player viewer) {
        sync(viewer, Collections.<ChatColor, Set<String>>emptyMap(), false);
    }

    /** Forgets the viewer's client state without sending anything (they disconnected). */
    public static void forget(UUID viewer) {
        sentTeams.remove(viewer);
        coloredNames.remove(viewer);
    }

    private static String teamName(ChatColor color) {
        return TEAM_PREFIX + color.ordinal();
    }
}
