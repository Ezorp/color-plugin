package dev.color.listener;

import dev.color.service.ViewColorService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/** Keeps every viewer's colors in sync as players come and go. Kicks also fire {@link PlayerQuitEvent}. */
public class JoinListener implements Listener {

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        // The newcomer sees their saved colors, and appears in other viewers' colors
        ViewColorService.refreshAllViewers();
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        // Others drop the leaving player on the next periodic refresh
        ViewColorService.detachViewer(event.getPlayer());
    }
}
