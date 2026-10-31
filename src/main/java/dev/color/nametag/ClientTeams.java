package dev.color.nametag;

import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks the fake teams that exist on each viewer's client and sends the packets needed to
 * move from what the client has to what it should have. There is one fake team per {@link TeamStyle}.
 *
 * The client errors on packets for a team it doesn't have (or creating one it already has), so the
 * tracked state must match the client exactly: it starts empty on join and is dropped on quit.
 */
public final class ClientTeams {

    private static final String TEAM_PREFIX = "colorplg_";

    /** Viewer → style → fake team, as last sent. Main thread only. */
    private static final Map<UUID, Map<TeamStyle, SentTeam>> sentTeams = new HashMap<>();
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
    public static void sync(Player viewer, Map<TeamStyle, Set<String>> desired, boolean reassert) {
        Map<TeamStyle, SentTeam> sent = sentTeams.get(viewer.getUniqueId());
        if (sent == null) sent = Collections.emptyMap();

        Map<TeamStyle, SentTeam> next = new HashMap<>();
        Map<String, String> leaving = new HashMap<>(); // name → fake team it was in
        Set<String> usedNames = new HashSet<>();
        for (SentTeam team : sent.values()) {
            usedNames.add(team.name);
            for (String name : team.members) leaving.put(name, team.name);
        }
        int nextId = 0;
        for (Map.Entry<TeamStyle, Set<String>> e : desired.entrySet()) {
            if (e.getValue().isEmpty()) continue;
            SentTeam old = sent.get(e.getKey());
            String teamName;
            if (old != null) {
                teamName = old.name;
            } else {
                // Never reuse the name of a team sent before, even one removed below, so packet order doesn't matter
                while (usedNames.contains(TEAM_PREFIX + nextId)) nextId++;
                teamName = TEAM_PREFIX + nextId;
                usedNames.add(teamName);
            }
            next.put(e.getKey(), new SentTeam(teamName, new HashSet<>(e.getValue())));
            leaving.keySet().removeAll(e.getValue());
        }

        // Record the new state before sending, so the ProtocolLib hook judges our packets against it
        sentTeams.put(viewer.getUniqueId(), next);
        Set<String> names = new HashSet<>();
        Set<String> nextTeamNames = new HashSet<>();
        for (SentTeam team : next.values()) {
            names.addAll(team.members);
            nextTeamNames.add(team.name);
        }
        coloredNames.put(viewer.getUniqueId(), Collections.unmodifiableSet(names));

        for (Map.Entry<TeamStyle, SentTeam> e : next.entrySet()) {
            TeamStyle style = e.getKey();
            SentTeam after = e.getValue();
            SentTeam before = sent.get(style);
            if (before == null) {
                TeamPackets.send(viewer, after.name, TeamPackets.CREATE, style.prefix(), style.suffix(), after.members);
            } else {
                List<String> toAdd = new ArrayList<>(after.members);
                if (!reassert) toAdd.removeAll(before.members);
                // Adding a player moves them out of whatever team they were in, so moves between styles need nothing else
                if (!toAdd.isEmpty()) TeamPackets.send(viewer, after.name, TeamPackets.ADD_PLAYERS, null, null, toAdd);
            }
        }
        for (Map.Entry<TeamStyle, SentTeam> e : sent.entrySet()) {
            if (!next.containsKey(e.getKey())) {
                TeamPackets.send(viewer, e.getValue().name, TeamPackets.REMOVE, null, null, Collections.<String>emptyList());
            }
        }

        // Players no longer colored go back to their real team on the viewer's scoreboard, if any
        for (Map.Entry<String, String> e : leaving.entrySet()) {
            String name = e.getKey();
            Team realTeam = viewer.getScoreboard().getEntryTeam(name);
            if (realTeam != null) {
                TeamPackets.send(viewer, realTeam.getName(), TeamPackets.ADD_PLAYERS, null, null, Collections.singletonList(name));
            } else if (nextTeamNames.contains(e.getValue())) {
                TeamPackets.send(viewer, e.getValue(), TeamPackets.REMOVE_PLAYERS, null, null, Collections.singletonList(name));
            }
        }

        if (next.isEmpty()) forget(viewer.getUniqueId());
    }

    /** Removes every fake team from the viewer's client and restores real teams. */
    public static void clear(Player viewer) {
        sync(viewer, Collections.<TeamStyle, Set<String>>emptyMap(), false);
    }

    /** Forgets the viewer's client state without sending anything (they disconnected). */
    public static void forget(UUID viewer) {
        sentTeams.remove(viewer);
        coloredNames.remove(viewer);
    }

    private static final class SentTeam {
        final String name;
        final Set<String> members;

        SentTeam(String name, Set<String> members) {
            this.name = name;
            this.members = members;
        }
    }
}
