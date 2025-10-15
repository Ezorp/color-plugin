package dev.color.gui;

import dev.color.ColorPlugin;
import dev.color.service.ViewColorService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class ColorMenu implements Listener {

    private static final ChatColor[] COLORS = new ChatColor[] {
            ChatColor.BLACK, ChatColor.DARK_BLUE, ChatColor.DARK_GREEN, ChatColor.DARK_AQUA,
            ChatColor.DARK_RED, ChatColor.DARK_PURPLE, ChatColor.GOLD, ChatColor.GRAY,
            ChatColor.DARK_GRAY, ChatColor.BLUE, ChatColor.GREEN, ChatColor.AQUA,
            ChatColor.RED, ChatColor.LIGHT_PURPLE, ChatColor.YELLOW, ChatColor.WHITE
    };

    public static void open(Player viewer, List<Player> targets) {
        // Remember targets before opening in case a close event fires for the previous view
        ViewColorService.rememberPendingTargets(viewer, targets);

        final Inventory inv = Bukkit.createInventory(null, 27, ChatColor.DARK_GRAY + "Choose a Color");
        // 16 wool colors + filler
        for (int i = 0; i < COLORS.length; i++) {
            inv.setItem(9 + i, createWool(COLORS[i]));
        }
        // Open on next tick to avoid modifying inventories during click handling
        Bukkit.getScheduler().runTask(ColorPlugin.getInstance(), new java.lang.Runnable() {
            public void run() {
                viewer.openInventory(inv);
            }
        });
    }

    private static ItemStack createWool(ChatColor color) {
        short data = woolDataFromColor(color);
        ItemStack wool = new ItemStack(Material.WOOL, 1, data);
        ItemMeta meta = wool.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(color + capitalize(color.name().toLowerCase().replace('_', ' ')));
            meta.setLore(new ArrayList<String>(Arrays.asList(ChatColor.GRAY + "Click to apply")));
            wool.setItemMeta(meta);
        }
        return wool;
    }

    private static String capitalize(String s) {
        return s.length() == 0 ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static short woolDataFromColor(ChatColor c) {
        // Map ChatColor to closest 1.8 wool data values (0-15)
        if (c == ChatColor.WHITE) return 0;            // White
        if (c == ChatColor.GOLD) return 1;             // Orange (closest)
        if (c == ChatColor.LIGHT_PURPLE) return 2;     // Magenta (closest)
        if (c == ChatColor.AQUA) return 3;             // Light Blue
        if (c == ChatColor.YELLOW) return 4;           // Yellow
        if (c == ChatColor.GREEN) return 5;            // Lime (closest)
        if (c == ChatColor.RED) return 14;             // Red
        if (c == ChatColor.GRAY) return 7;             // Gray
        if (c == ChatColor.DARK_GRAY) return 8;        // Light Gray
        if (c == ChatColor.DARK_AQUA) return 9;        // Cyan
        if (c == ChatColor.DARK_PURPLE) return 10;     // Purple
        if (c == ChatColor.BLUE || c == ChatColor.DARK_BLUE) return 11; // Blue
        if (c == ChatColor.DARK_GREEN) return 13;      // Green
        if (c == ChatColor.BLACK) return 15;           // Black
        return 0;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        HumanEntity clicker = event.getWhoClicked();
        if (!(clicker instanceof Player)) return;

        InventoryView view = event.getView();
        if (view == null) return;
        String title = view.getTitle();
        if (title == null || !ChatColor.stripColor(title).equalsIgnoreCase("Choose a Color")) return;

        // Only handle clicks in the top inventory (the GUI itself)
        if (event.getClickedInventory() != view.getTopInventory()) return;
        if (event.getRawSlot() < 0 || event.getRawSlot() >= view.getTopInventory().getSize()) return;

        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() != Material.WOOL) return;

        ItemMeta meta = clicked.getItemMeta();
        if (meta == null || meta.getDisplayName() == null) return;
        String name = ChatColor.stripColor(meta.getDisplayName()).toUpperCase().replace(' ', '_');
        ChatColor chosen = null;
        try { chosen = ChatColor.valueOf(name); } catch (IllegalArgumentException ignored) {}
        if (chosen == null) return;

        Player viewer = (Player) clicker;
        List<Player> targets = ViewColorService.consumePendingTargets(viewer);
        if (targets == null || targets.isEmpty()) return;
        if (chosen == ChatColor.WHITE) {
            ViewColorService.applyColorForViewer(viewer, targets, null); // reset to default
        } else {
            ViewColorService.applyColorForViewer(viewer, targets, chosen);
        }
        // Close on next tick per Bukkit guidance
        Bukkit.getScheduler().runTask(ColorPlugin.getInstance(), new java.lang.Runnable() {
            public void run() {
                viewer.closeInventory();
            }
        });
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        InventoryView view = event.getView();
        if (view == null || view.getTitle() == null) return;
        String title = view.getTitle();
        if (!ChatColor.stripColor(title).equalsIgnoreCase("Choose a Color")) return;
        Player viewer = (Player) event.getPlayer();
        ViewColorService.clearPendingTargets(viewer);
    }
}



