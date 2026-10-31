package dev.color.listener;

import dev.color.ColorPlugin;
import dev.color.service.ViewColorService;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/** Keeps every viewer's teams in sync as players come and go. Kicks also fire {@link PlayerQuitEvent}. */
public class JoinListener implements Listener {

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        // Rebuild so the newcomer sees their saved colors and appears in others' teams
        ViewColorService.refreshAllViewers();
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        ViewColorService.detachViewer(event.getPlayer());
        // The quitting player is still listed as online during this event, so rebuild once they are gone
        Bukkit.getScheduler().runTask(ColorPlugin.getInstance(), ViewColorService::refreshAllViewers);
    }
}
