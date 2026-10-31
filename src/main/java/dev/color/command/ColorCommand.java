package dev.color.command;

import dev.color.gui.ColorMenu;
import dev.color.gui.PlayerPickerMenu;
import dev.color.service.ColorMode;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * {@code /color} (or {@code /colordot}) opens the player picker; with player names it opens the color GUI
 * directly. The mode decides whether the chosen color goes on the name or on a dot before it.
 */
public class ColorCommand implements CommandExecutor, TabCompleter {

    private static final int MAX_TARGETS = 10;

    private final ColorMode mode;

    public ColorCommand(ColorMode mode) {
        this.mode = mode;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }
        Player viewer = (Player) sender;

        if (args.length == 0) {
            PlayerPickerMenu.open(viewer, mode, 0);
            return true;
        }

        // Unique online targets, in the order given
        Set<Player> targets = new LinkedHashSet<>();
        for (String name : args) {
            if (targets.size() >= MAX_TARGETS) break;
            Player target = Bukkit.getPlayerExact(name);
            if (target != null) targets.add(target);
        }
        if (targets.isEmpty()) {
            viewer.sendMessage(ChatColor.RED + "No valid online targets.");
            return true;
        }

        ColorMenu.open(viewer, mode, new ArrayList<>(targets));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        String prefix = args.length == 0 ? "" : args[args.length - 1].toLowerCase(Locale.ENGLISH);
        Set<String> alreadyTyped = new HashSet<>();
        for (int i = 0; i < args.length - 1; i++) {
            alreadyTyped.add(args[i].toLowerCase(Locale.ENGLISH));
        }

        List<String> suggestions = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            String lower = player.getName().toLowerCase(Locale.ENGLISH);
            if (!alreadyTyped.contains(lower) && lower.startsWith(prefix)) {
                suggestions.add(player.getName());
            }
        }
        return suggestions;
    }
}
