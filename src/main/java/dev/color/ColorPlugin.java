package dev.color;

import dev.color.command.ColorCommand;
import dev.color.gui.ColorMenu;
import dev.color.gui.PlayerPickerMenu;
import dev.color.listener.JoinListener;
import dev.color.listener.KickListener;
import dev.color.service.ViewColorService;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class ColorPlugin extends JavaPlugin {

    private static ColorPlugin instance;

    public static ColorPlugin getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;

        // Register command
        PluginCommand colorCmd = getCommand("color");
        if (colorCmd != null) {
            ColorCommand executor = new ColorCommand();
            colorCmd.setExecutor(executor);
            colorCmd.setTabCompleter(executor);
        } else {
            getLogger().warning("Command 'color' is not defined in plugin.yml");
        }

        // Register listeners
        getServer().getPluginManager().registerEvents(new JoinListener(), this);
        getServer().getPluginManager().registerEvents(new ColorMenu(), this);
        getServer().getPluginManager().registerEvents(new PlayerPickerMenu(), this);
        getServer().getPluginManager().registerEvents(new KickListener(), this);
        getLogger().info("Color enabled.");
    }

    @Override
    public void onDisable() {
        ViewColorService.shutdown();
        getLogger().info("Color disabled.");
        instance = null;
    }
}



