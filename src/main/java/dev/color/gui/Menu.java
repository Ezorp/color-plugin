package dev.color.gui;

import dev.color.ColorPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

/**
 * Base class for the plugin's chest GUIs. The menu is the inventory's holder, so
 * {@link MenuListener} can recognise it (and reach its state) without parsing titles.
 */
public abstract class Menu implements InventoryHolder {

    private final Inventory inventory;

    protected Menu(int size, String title) {
        this.inventory = Bukkit.createInventory(this, size, title);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    /** Called for clicks on a slot of this menu (top inventory only). */
    protected abstract void onClick(Player viewer, int slot);

    public void open(Player viewer) {
        // Opening during a click handler is unsafe, so always defer to the next tick
        runNextTick(() -> viewer.openInventory(inventory));
    }

    protected static void close(Player viewer) {
        runNextTick(viewer::closeInventory);
    }

    protected static void runNextTick(Runnable task) {
        Bukkit.getScheduler().runTask(ColorPlugin.getInstance(), task);
    }

    protected static ItemStack item(Material material, short data, String name, String... lore) {
        ItemStack item = new ItemStack(material, 1, data);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore.length > 0) meta.setLore(Arrays.asList(lore));
            item.setItemMeta(meta);
        }
        return item;
    }
}
