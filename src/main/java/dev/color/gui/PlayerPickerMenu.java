package dev.color.gui;

import dev.color.service.ColorMode;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.SkullType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/** Paginated list of online players' heads; clicking one opens the {@link ColorMenu} for them. */
public final class PlayerPickerMenu extends Menu {

    private static final int PAGE_SIZE = 45; // 5 rows of heads, last row holds the controls
    private static final int PREVIOUS_SLOT = 45;
    private static final int CLOSE_SLOT = 49;
    private static final int NEXT_SLOT = 53;

    private final ColorMode mode;
    private final int page;
    private final List<UUID> shown = new ArrayList<>();

    private PlayerPickerMenu(ColorMode mode, List<Player> players, int page, int pageCount) {
        super(54, ChatColor.DARK_GRAY + "Pick a Player" + ChatColor.GRAY + " (" + (page + 1) + "/" + pageCount + ")");
        this.mode = mode;
        this.page = page;

        int start = page * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, players.size());
        for (int i = start; i < end; i++) {
            Player player = players.get(i);
            shown.add(player.getUniqueId());
            getInventory().setItem(i - start, skullOf(player));
        }

        getInventory().setItem(PREVIOUS_SLOT, item(Material.ARROW, (short) 0, ChatColor.YELLOW + "Previous"));
        getInventory().setItem(CLOSE_SLOT, item(Material.BARRIER, (short) 0, ChatColor.RED + "Close"));
        getInventory().setItem(NEXT_SLOT, item(Material.ARROW, (short) 0, ChatColor.YELLOW + "Next"));
    }

    /** Opens the given page, clamped to the pages that currently exist. */
    public static void open(Player viewer, ColorMode mode, int page) {
        List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
        int pageCount = Math.max(1, (players.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        int clamped = Math.max(0, Math.min(page, pageCount - 1));
        new PlayerPickerMenu(mode, players, clamped, pageCount).open(viewer);
    }

    @Override
    protected void onClick(Player viewer, int slot) {
        if (slot == PREVIOUS_SLOT) {
            open(viewer, mode, page - 1);
        } else if (slot == NEXT_SLOT) {
            open(viewer, mode, page + 1);
        } else if (slot == CLOSE_SLOT) {
            close(viewer);
        } else if (slot < shown.size()) {
            Player target = Bukkit.getPlayer(shown.get(slot));
            if (target != null) ColorMenu.open(viewer, mode, Collections.singletonList(target));
        }
    }

    private static ItemStack skullOf(Player player) {
        ItemStack skull = item(Material.SKULL_ITEM, (short) SkullType.PLAYER.ordinal(), ChatColor.WHITE + player.getName());
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        meta.setOwner(player.getName());
        skull.setItemMeta(meta);
        return skull;
    }
}
