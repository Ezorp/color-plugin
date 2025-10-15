package dev.color.listener;

import dev.color.service.ViewColorService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class JoinListener implements Listener {

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        // Refresh all viewers when someone joins so teams include the newcomer where needed
        ViewColorService.refreshAllViewers();
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        // Clean up transient state but keep mappings for reconnects
        ViewColorService.detachViewer(event.getPlayer());
        ViewColorService.refreshAllViewers();
    }
}



