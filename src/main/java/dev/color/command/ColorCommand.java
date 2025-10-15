package dev.color.command;

import dev.color.gui.ColorMenu;
import dev.color.gui.PlayerPickerMenu;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ColorCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }
        Player viewer = (Player) sender;

        if (args.length == 0) {
            PlayerPickerMenu.open(viewer, 0);
            return true;
        }

        // Deduplicate and cap at 10 unique online targets (preserve order)
        List<Player> targets = new ArrayList<Player>();
        java.util.Set<String> seen = new java.util.LinkedHashSet<String>();
        for (int i = 0; i < args.length && seen.size() < 10; i++) {
            String name = args[i];
            if (name == null) continue;
            String key = name.toLowerCase(java.util.Locale.ENGLISH);
            if (seen.contains(key)) continue;
            Player t = Bukkit.getPlayerExact(name);
            if (t != null && t.isOnline()) {
                seen.add(key);
                targets.add(t);
            }
        }
        if (targets.isEmpty()) {
            viewer.sendMessage(ChatColor.RED + "No valid online targets.");
            return true;
        }

        ColorMenu.open(viewer, targets);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> suggestions = new ArrayList<String>();
        String prefix = args.length == 0 ? "" : args[args.length - 1].toLowerCase(Locale.ENGLISH);
        java.util.Set<String> already = new java.util.HashSet<String>();
        for (int i = 0; i < args.length - 1; i++) {
            already.add(args[i].toLowerCase(Locale.ENGLISH));
        }
        for (Player p : Bukkit.getOnlinePlayers()) {
            String name = p.getName();
            String lower = name.toLowerCase(Locale.ENGLISH);
            if (!already.contains(lower) && lower.startsWith(prefix)) {
                suggestions.add(name);
            }
        }
        return suggestions;
    }
}



