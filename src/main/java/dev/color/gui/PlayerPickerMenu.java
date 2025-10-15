package dev.color.gui;

import dev.color.ColorPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.SkullType;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class PlayerPickerMenu implements Listener {

    private static final int PAGE_SIZE = 45; // 5 rows of heads, last row controls

    public static void open(Player viewer, int page) {
        List<Player> online = getOnlinePlayersList();
        int total = online.size();
        int maxPage = Math.max(0, (total - 1) / PAGE_SIZE);
        if (page < 0) page = 0; else if (page > maxPage) page = maxPage;

        final Inventory inv = Bukkit.createInventory(null, 54, ChatColor.DARK_GRAY + "Pick a Player" + ChatColor.GRAY + " (" + (page + 1) + "/" + (maxPage + 1) + ")");

        int start = page * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, total);
        for (int i = start, slot = 0; i < end; i++, slot++) {
            Player p = online.get(i);
            inv.setItem(slot, skullOf(p));
        }

        // Controls: previous / next / close
        inv.setItem(45, controlItem(Material.ARROW, ChatColor.YELLOW + "Previous"));
        inv.setItem(49, controlItem(Material.BARRIER, ChatColor.RED + "Close"));
        inv.setItem(53, controlItem(Material.ARROW, ChatColor.YELLOW + "Next"));

        // Open on next tick to avoid inventory modification during click handling
        Bukkit.getScheduler().runTask(ColorPlugin.getInstance(), new java.lang.Runnable() {
            public void run() {
                viewer.openInventory(inv);
            }
        });
        // store page in viewer metadata via title parsing on click
    }

    private static ItemStack skullOf(Player p) {
        ItemStack skull = new ItemStack(Material.SKULL_ITEM, 1, (short) SkullType.PLAYER.ordinal());
        ItemMeta m = skull.getItemMeta();
        if (m instanceof SkullMeta) {
            SkullMeta meta = (SkullMeta) m;
            meta.setOwner(p.getName());
            meta.setDisplayName(ChatColor.WHITE + p.getName());
            skull.setItemMeta(meta);
        } else if (m != null) {
            m.setDisplayName(ChatColor.WHITE + p.getName());
            skull.setItemMeta(m);
        }
        return skull;
    }

    private static ItemStack controlItem(Material mat, String name) {
        ItemStack it = new ItemStack(mat);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            it.setItemMeta(meta);
        }
        return it;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        HumanEntity clicker = event.getWhoClicked();
        if (!(clicker instanceof Player)) return;

        InventoryView view = event.getView();
        if (view == null) return;
        String title = view.getTitle();
        if (title == null) return;
        String stripped = ChatColor.stripColor(title);
        if (!stripped.startsWith("Pick a Player")) return;

        // Only handle clicks in the GUI area (top inventory slots by raw slot bounds)
        if (event.getRawSlot() < 0 || event.getRawSlot() >= view.getTopInventory().getSize()) return;

        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null) return;

        Player viewer = (Player) clicker;
        // Parse current page from stripped title "Pick a Player (x/y)"
        int page = 0;
        int i = stripped.indexOf('(');
        int j = stripped.indexOf('/');
        if (i != -1 && j != -1 && j > i) {
            try { page = Integer.parseInt(stripped.substring(i + 1, j)) - 1; } catch (NumberFormatException ignored) {}
        }

        Material type = clicked.getType();
        if (type == Material.ARROW) {
            String name = clicked.hasItemMeta() && clicked.getItemMeta().hasDisplayName() ? ChatColor.stripColor(clicked.getItemMeta().getDisplayName()) : "";
            if (name.equalsIgnoreCase("Previous")) {
                open(viewer, Math.max(0, page - 1));
            } else if (name.equalsIgnoreCase("Next")) {
                // compute max from current online count
                int total = getOnlinePlayersList().size();
                int maxPage = Math.max(0, (total - 1) / PAGE_SIZE);
                open(viewer, Math.min(maxPage, page + 1));
            }
            return;
        }
        if (type == Material.BARRIER) {
            Bukkit.getScheduler().runTask(ColorPlugin.getInstance(), new java.lang.Runnable() {
                public void run() {
                    viewer.closeInventory();
                }
            });
            return;
        }

        if (type == Material.SKULL_ITEM) {
            ItemMeta meta = clicked.getItemMeta();
            String name = meta != null && meta.getDisplayName() != null ? ChatColor.stripColor(meta.getDisplayName()) : null;
            if (name != null && !name.isEmpty()) {
                Player target = Bukkit.getPlayerExact(name);
                if (target != null && target.isOnline()) {
                    java.util.List<Player> targets = new java.util.ArrayList<Player>();
                    targets.add(target);
                    // Open on next tick to avoid click-modification issues
                    Bukkit.getScheduler().runTask(ColorPlugin.getInstance(), new java.lang.Runnable() {
                        public void run() {
                            ColorMenu.open(viewer, targets);
                        }
                    });
                }
            }
        }
    }

    private static List<Player> getOnlinePlayersList() {
        try {
            // 1.8 returns a Collection<Player>
            java.util.Collection<? extends Player> col = Bukkit.getOnlinePlayers();
            return new java.util.ArrayList<Player>(col);
        } catch (Throwable ignored) {
        }
        // Fallback if API variation occurs
        Player[] arr;
        try {
            arr = (Player[]) org.bukkit.Bukkit.class.getMethod("getOnlinePlayers").invoke(null);
        } catch (Throwable t) {
            arr = new Player[0];
        }
        return new java.util.ArrayList<Player>(java.util.Arrays.asList(arr));
    }
}



