package dev.color.gui;

import dev.color.service.ViewColorService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Wool color picker applying the chosen color to a set of targets, for the viewer only. */
public final class ColorMenu extends Menu {

    private static final ChatColor[] COLORS = {
            ChatColor.BLACK, ChatColor.DARK_BLUE, ChatColor.DARK_GREEN, ChatColor.DARK_AQUA,
            ChatColor.DARK_RED, ChatColor.DARK_PURPLE, ChatColor.GOLD, ChatColor.GRAY,
            ChatColor.DARK_GRAY, ChatColor.BLUE, ChatColor.GREEN, ChatColor.AQUA,
            ChatColor.RED, ChatColor.LIGHT_PURPLE, ChatColor.YELLOW, ChatColor.WHITE
    };
    private static final int FIRST_COLOR_SLOT = 9;

    private final List<UUID> targets = new ArrayList<>();

    private ColorMenu(List<Player> targets) {
        super(27, ChatColor.DARK_GRAY + "Choose a Color");
        for (Player target : targets) this.targets.add(target.getUniqueId());
        for (int i = 0; i < COLORS.length; i++) {
            ChatColor color = COLORS[i];
            getInventory().setItem(FIRST_COLOR_SLOT + i, item(Material.WOOL, woolData(color),
                    color + displayName(color), ChatColor.GRAY + "Click to apply"));
        }
    }

    public static void open(Player viewer, List<Player> targets) {
        new ColorMenu(targets).open(viewer);
    }

    @Override
    protected void onClick(Player viewer, int slot) {
        int index = slot - FIRST_COLOR_SLOT;
        if (index < 0 || index >= COLORS.length) return;
        ChatColor chosen = COLORS[index];

        List<Player> online = new ArrayList<>();
        for (UUID id : targets) {
            Player target = Bukkit.getPlayer(id);
            if (target != null) online.add(target);
        }
        // White means "reset to default"
        ViewColorService.applyColorForViewer(viewer, online, chosen == ChatColor.WHITE ? null : chosen);
        close(viewer);
    }

    private static String displayName(ChatColor color) {
        String name = color.name().toLowerCase().replace('_', ' ');
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    /** Closest 1.8 wool shade for each chat color. */
    private static short woolData(ChatColor color) {
        DyeColor dye;
        switch (color) {
            case BLACK:        dye = DyeColor.BLACK; break;
            case DARK_BLUE:
            case BLUE:         dye = DyeColor.BLUE; break;
            case DARK_GREEN:   dye = DyeColor.GREEN; break;
            case DARK_AQUA:    dye = DyeColor.CYAN; break;
            case DARK_RED:
            case RED:          dye = DyeColor.RED; break;
            case DARK_PURPLE:  dye = DyeColor.PURPLE; break;
            case GOLD:         dye = DyeColor.ORANGE; break;
            case GRAY:         dye = DyeColor.SILVER; break;
            case DARK_GRAY:    dye = DyeColor.GRAY; break;
            case GREEN:        dye = DyeColor.LIME; break;
            case AQUA:         dye = DyeColor.LIGHT_BLUE; break;
            case LIGHT_PURPLE: dye = DyeColor.MAGENTA; break;
            case YELLOW:       dye = DyeColor.YELLOW; break;
            default:           dye = DyeColor.WHITE; break;
        }
        return dye.getWoolData();
    }
}
