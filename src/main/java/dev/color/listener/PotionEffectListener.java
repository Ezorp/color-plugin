package dev.color.listener;

import dev.color.ColorPlugin;
import dev.color.service.ViewColorService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Monitors invisibility potion effects and refreshes scoreboards when they change.
 * Uses a scheduled task for 1.8.8 compatibility since EntityPotionEffectEvent doesn't exist yet.
 */
public class PotionEffectListener {

    private final Map<UUID, Boolean> previousInvisibilityState = new HashMap<UUID, Boolean>();
    private BukkitRunnable checkTask;

    public void start() {
        // Check every 10 ticks (0.5 seconds) for invisibility changes
        checkTask = new BukkitRunnable() {
            @Override
            public void run() {
                boolean needsRefresh = false;
                
                for (Player player : Bukkit.getOnlinePlayers()) {
                    UUID uuid = player.getUniqueId();
                    boolean isInvisible = player.hasPotionEffect(PotionEffectType.INVISIBILITY);
                    Boolean wasInvisible = previousInvisibilityState.get(uuid);
                    
                    // Check if invisibility state changed
                    if (wasInvisible == null || wasInvisible != isInvisible) {
                        previousInvisibilityState.put(uuid, isInvisible);
                        needsRefresh = true;
                    }
                }
                
                // Clean up offline players from the map
                previousInvisibilityState.keySet().removeIf(uuid -> Bukkit.getPlayer(uuid) == null);
                
                // Refresh all viewers if any player's invisibility state changed
                if (needsRefresh) {
                    ViewColorService.refreshAllViewers();
                }
            }
        };
        
        // Run every 10 ticks (0.5 seconds), starting after 20 ticks (1 second)
        checkTask.runTaskTimer(ColorPlugin.getInstance(), 20L, 10L);
    }

    public void stop() {
        if (checkTask != null) {
            checkTask.cancel();
            checkTask = null;
        }
        previousInvisibilityState.clear();
    }
}

