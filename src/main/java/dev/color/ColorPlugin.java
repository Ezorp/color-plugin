package dev.color;

import dev.color.command.ColorCommand;
import dev.color.gui.MenuListener;
import dev.color.listener.JoinListener;
import dev.color.nametag.ProtocolLibHook;
import dev.color.nametag.TeamPackets;
import dev.color.service.ColorMode;
import dev.color.service.ViewColorService;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

public final class ColorPlugin extends JavaPlugin {

    private static ColorPlugin instance;

    public static ColorPlugin getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;

        try {
            TeamPackets.init();
        } catch (ReflectiveOperationException | RuntimeException e) {
            getLogger().log(Level.SEVERE, "Unsupported server version, Color requires Spigot 1.8.8. Disabling.", e);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        registerCommand("color", ColorMode.NAME);
        registerCommand("colordot", ColorMode.DOT);

        PluginManager plugins = getServer().getPluginManager();
        plugins.registerEvents(new JoinListener(), this);
        plugins.registerEvents(new MenuListener(), this);

        boolean packetHook = plugins.isPluginEnabled("ProtocolLib");
        if (packetHook) {
            ProtocolLibHook.register(this);
            getLogger().info("ProtocolLib found: colors are re-applied as soon as another plugin overrides them.");
        }
        ViewColorService.start(this, packetHook);

        getLogger().info("Color enabled.");
    }

    private void registerCommand(String name, ColorMode mode) {
        PluginCommand command = getCommand(name);
        ColorCommand executor = new ColorCommand(mode);
        command.setExecutor(executor);
        command.setTabCompleter(executor);
    }

    @Override
    public void onDisable() {
        ViewColorService.shutdown();
        getLogger().info("Color disabled.");
        instance = null;
    }
}
