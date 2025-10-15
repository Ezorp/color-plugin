package dev.color.listener;

import dev.color.service.ViewColorService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerKickEvent;

public class KickListener implements Listener {

    @EventHandler
    public void onKick(PlayerKickEvent event) {
        ViewColorService.detachViewer(event.getPlayer());
        ViewColorService.refreshAllViewers();
    }
}



