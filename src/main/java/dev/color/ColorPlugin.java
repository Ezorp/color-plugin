package dev.color;

import dev.color.command.ColorCommand;
import dev.color.gui.MenuListener;
import dev.color.listener.JoinListener;
import dev.color.service.InvisibilityMonitor;
import dev.color.service.ViewColorService;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class ColorPlugin extends JavaPlugin {

    private static ColorPlugin instance;
    private final InvisibilityMonitor invisibilityMonitor = new InvisibilityMonitor();

    public static ColorPlugin getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;

        PluginCommand colorCmd = getCommand("color");
        ColorCommand executor = new ColorCommand();
        colorCmd.setExecutor(executor);
        colorCmd.setTabCompleter(executor);

        PluginManager plugins = getServer().getPluginManager();
        plugins.registerEvents(new JoinListener(), this);
        plugins.registerEvents(new MenuListener(), this);

        invisibilityMonitor.start(this);
        getLogger().info("Color enabled.");
    }

    @Override
    public void onDisable() {
        invisibilityMonitor.stop();
        ViewColorService.shutdown();
        getLogger().info("Color disabled.");
        instance = null;
    }
}
