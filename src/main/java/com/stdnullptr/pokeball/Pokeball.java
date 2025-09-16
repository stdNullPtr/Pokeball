package com.stdnullptr.pokeball;

import com.stdnullptr.pokeball.command.PokeballRootCommand;
import com.stdnullptr.pokeball.config.PluginConfig;
import com.stdnullptr.pokeball.item.PokeballItemFactory;
import com.stdnullptr.pokeball.listener.ReleaseListener;
import com.stdnullptr.pokeball.listener.ProjectileListeners;
import com.stdnullptr.pokeball.util.Keys;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;

public final class Pokeball extends JavaPlugin {

    private Logger logger;
    private MiniMessage miniMessage;
    private PluginConfig configModel;
    private PokeballItemFactory itemFactory;

    @Override
    public void onEnable() {
        this.logger = getSLF4JLogger();
        this.miniMessage = MiniMessage.miniMessage();

        // Save default config if not present
        saveDefaultConfig();
        this.configModel = new PluginConfig(this);

        // Prepare Keys namespace
        Keys.init(this);

        // Services / Factories
        this.itemFactory = new PokeballItemFactory(this, configModel);

        // Commands (Paper's programmatic registration)
        var root = new PokeballRootCommand(this, itemFactory, configModel);
        registerCommand("pokeball", "Root command for Pokeball plugin", java.util.List.of("pb"), root);

        // Listeners
        // Capture is exclusively via thrown projectile
        getServer().getPluginManager().registerEvents(new ReleaseListener(this, itemFactory, configModel), this);
        getServer().getPluginManager().registerEvents(new ProjectileListeners(this, itemFactory, configModel), this);

        logger.info("Pokeball enabled.");
    }

    @Override
    public void onDisable() {
        logger.info("Pokeball disabled.");
    }

    public Logger logger() { return logger; }
    public MiniMessage mini() { return miniMessage; }
    public PluginConfig configModel() { return configModel; }
    public PokeballItemFactory itemFactory() { return itemFactory; }
}
