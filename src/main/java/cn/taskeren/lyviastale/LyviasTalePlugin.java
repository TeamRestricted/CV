package cn.taskeren.lyviastale;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class LyviasTalePlugin extends JavaPlugin {

    public static final Logger LOGGER = LogManager.getLogger("lyvias-tale");

    public static LyviasTalePlugin instance;
    public static File dataFolder = new File("plugins/lyvias_tale");

    public LyviasTalePlugin() {
        LyviasTalePlugin.instance = this;
    }

    @Override
    public void onEnable() {
        LOGGER.info("Lyvia's Tale has begun.");
        LyviasTalePlugin.dataFolder = this.getDataFolder();
        LyviasTaleInitializer.INSTANCE.init();
    }

    @Override
    public void onDisable() {
        LOGGER.info("Lyvia's Tale has been disabled!");
    }
}
