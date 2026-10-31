package dev.color.nametag;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Sends scoreboard team packets straight to one player, without touching any server-side scoreboard.
 * Uses reflection on the 1.8.8 server internals so the build only needs the Spigot API.
 */
public final class TeamPackets {

    public static final int CREATE = 0;
    public static final int REMOVE = 1;
    public static final int ADD_PLAYERS = 3;
    public static final int REMOVE_PLAYERS = 4;

    private static Constructor<?> newPacket;
    private static Field nameField;
    private static Field prefixField;
    private static Field suffixField;
    private static Field playersField;
    private static Field modeField;
    private static Method getHandle;
    private static Field playerConnection;
    private static Method sendPacket;

    private TeamPackets() {}

    /** Looks up the server internals once; fails if the server isn't a 1.8.x CraftBukkit/Spigot build. */
    public static void init() throws ReflectiveOperationException {
        String version = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
        String nms = "net.minecraft.server." + version + ".";

        Class<?> packetClass = Class.forName(nms + "PacketPlayOutScoreboardTeam");
        newPacket = packetClass.getConstructor();
        // Obfuscated 1.8 field names: a = team name, c = prefix, d = suffix, g = player names, h = mode
        nameField = accessible(packetClass, "a");
        prefixField = accessible(packetClass, "c");
        suffixField = accessible(packetClass, "d");
        playersField = accessible(packetClass, "g");
        modeField = accessible(packetClass, "h");

        getHandle = Class.forName("org.bukkit.craftbukkit." + version + ".entity.CraftPlayer").getMethod("getHandle");
        playerConnection = Class.forName(nms + "EntityPlayer").getField("playerConnection");
        sendPacket = Class.forName(nms + "PlayerConnection").getMethod("sendPacket", Class.forName(nms + "Packet"));
    }

    /** Sends a team packet. {@code prefix} and {@code suffix} are only used by {@link #CREATE}. */
    public static void send(Player viewer, String team, int mode, String prefix, String suffix, Collection<String> players) {
        try {
            Object packet = newPacket.newInstance();
            nameField.set(packet, team);
            modeField.setInt(packet, mode);
            if (prefix != null) prefixField.set(packet, prefix);
            if (suffix != null) suffixField.set(packet, suffix);
            playersField.set(packet, new ArrayList<>(players));
            sendPacket.invoke(playerConnection.get(getHandle.invoke(viewer)), packet);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Could not send team packet", e);
        }
    }

    private static Field accessible(Class<?> owner, String name) throws NoSuchFieldException {
        Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }
}
