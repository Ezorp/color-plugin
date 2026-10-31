package dev.color.nametag;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.StructureModifier;
import dev.color.service.ViewColorService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Watches team packets from the server and other plugins. When one moves a colored player into
 * another team, the color is re-applied on the next tick. Only loaded when ProtocolLib is installed.
 */
public final class ProtocolLibHook extends PacketAdapter {

    // 1.8 team packet layout: strings[0] = team name, ints[1] = mode
    private static final int NAME_INDEX = 0;
    private static final int MODE_INDEX = 1;

    private final Set<UUID> pendingReassert = ConcurrentHashMap.newKeySet();

    private ProtocolLibHook(Plugin plugin) {
        super(plugin, PacketType.Play.Server.SCOREBOARD_TEAM);
    }

    public static void register(Plugin plugin) {
        ProtocolLibrary.getProtocolManager().addPacketListener(new ProtocolLibHook(plugin));
    }

    @Override
    public void onPacketSending(PacketEvent event) {
        PacketContainer packet = event.getPacket();
        if (ClientTeams.isOwnTeam(packet.getStrings().read(NAME_INDEX))) return;

        Player viewer = event.getPlayer();
        Set<String> colored = ClientTeams.coloredNames(viewer.getUniqueId());
        if (colored.isEmpty()) return;

        int mode = packet.getIntegers().read(MODE_INDEX);
        @SuppressWarnings("unchecked")
        Collection<String> players = packet.getSpecificModifier(Collection.class).read(0);
        if (players == null || Collections.disjoint(players, colored)) return;

        if (mode == TeamPackets.CREATE || mode == TeamPackets.ADD_PLAYERS) {
            scheduleReassert(viewer);
        } else if (mode == TeamPackets.REMOVE_PLAYERS) {
            // On this client the player is in our team, not theirs; removing them from theirs would error client-side.
            // The packet object can be shared with other players, so edit a copy.
            List<String> kept = new ArrayList<>(players);
            kept.removeAll(colored);
            PacketContainer copy = packet.deepClone();
            @SuppressWarnings({"unchecked", "rawtypes"})
            StructureModifier<Collection> collections = copy.getSpecificModifier(Collection.class);
            collections.write(0, kept);
            event.setPacket(copy);
        }
    }

    private void scheduleReassert(Player viewer) {
        UUID id = viewer.getUniqueId();
        if (!pendingReassert.add(id)) return;
        Bukkit.getScheduler().runTask(getPlugin(), () -> {
            pendingReassert.remove(id);
            Player online = Bukkit.getPlayer(id);
            if (online != null) ViewColorService.reassert(online);
        });
    }
}
