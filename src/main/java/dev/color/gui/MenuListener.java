package dev.color.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/** Routes inventory interactions to the open {@link Menu}, and locks its contents. */
public final class MenuListener implements Listener {

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        InventoryHolder holder = top.getHolder();
        if (!(holder instanceof Menu)) return;

        // Cancel every click while a menu is open, including shift-clicks from the player's inventory
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player)) return;
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= top.getSize()) return;
        ((Menu) holder).onClick((Player) event.getWhoClicked(), slot);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof Menu) {
            event.setCancelled(true);
        }
    }
}
