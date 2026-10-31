package dev.color.gui;

import dev.color.service.ColorMode;
import dev.color.service.ViewColorService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Wool color picker applying the chosen name or dot color to a set of targets, for the viewer only. */
public final class ColorMenu extends Menu {

    private static final ChatColor[] COLORS = {
            ChatColor.BLACK, ChatColor.DARK_BLUE, ChatColor.DARK_GREEN, ChatColor.DARK_AQUA,
            ChatColor.DARK_RED, ChatColor.DARK_PURPLE, ChatColor.GOLD, ChatColor.GRAY,
            ChatColor.DARK_GRAY, ChatColor.BLUE, ChatColor.GREEN, ChatColor.AQUA,
            ChatColor.RED, ChatColor.LIGHT_PURPLE, ChatColor.YELLOW, ChatColor.WHITE
    };
    private static final int FIRST_COLOR_SLOT = 9;
    private static final int REMOVE_DOT_SLOT = 4;

    private final ColorMode mode;
    private final List<UUID> targets = new ArrayList<>();

    private ColorMenu(ColorMode mode, List<Player> targets) {
        super(27, ChatColor.DARK_GRAY + (mode == ColorMode.DOT ? "Choose a Dot Color" : "Choose a Color"));
        this.mode = mode;
        for (Player target : targets) this.targets.add(target.getUniqueId());
        for (int i = 0; i < COLORS.length; i++) {
            ChatColor color = COLORS[i];
            getInventory().setItem(FIRST_COLOR_SLOT + i, item(Material.WOOL, woolData(color),
                    color + displayName(color), ChatColor.GRAY + "Click to apply"));
        }
        // White is a valid dot color, so dots get their own reset button
        if (mode == ColorMode.DOT) {
            getInventory().setItem(REMOVE_DOT_SLOT, item(Material.BARRIER, (short) 0, ChatColor.RED + "Remove dot"));
        }
    }

    public static void open(Player viewer, ColorMode mode, List<Player> targets) {
        new ColorMenu(mode, targets).open(viewer);
    }

    @Override
    protected void onClick(Player viewer, int slot) {
        ChatColor chosen;
        if (mode == ColorMode.DOT && slot == REMOVE_DOT_SLOT) {
            chosen = null;
        } else {
            int index = slot - FIRST_COLOR_SLOT;
            if (index < 0 || index >= COLORS.length) return;
            chosen = COLORS[index];
            // For names, white means "reset to default"
            if (mode == ColorMode.NAME && chosen == ChatColor.WHITE) chosen = null;
        }

        List<Player> online = new ArrayList<>();
        for (UUID id : targets) {
            Player target = Bukkit.getPlayer(id);
            if (target != null) online.add(target);
        }
        ViewColorService.applyColorForViewer(viewer, mode, online, chosen);
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
