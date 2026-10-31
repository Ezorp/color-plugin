package dev.color.service;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Polls for invisibility gained or lost and refreshes every viewer's scoreboard when it changes.
 * Polling is needed because 1.8.8 has no potion effect event.
 */
public final class InvisibilityMonitor {

    private static final long INITIAL_DELAY_TICKS = 20L;
    private static final long PERIOD_TICKS = 10L;

    private Set<UUID> invisible = new HashSet<>();
    private BukkitTask task;

    public void start(Plugin plugin) {
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::check, INITIAL_DELAY_TICKS, PERIOD_TICKS);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        invisible.clear();
    }

    private void check() {
        Set<UUID> current = new HashSet<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPotionEffect(PotionEffectType.INVISIBILITY)) current.add(player.getUniqueId());
        }
        if (!current.equals(invisible)) {
            invisible = current;
            ViewColorService.refreshAllViewers();
        }
    }
}
