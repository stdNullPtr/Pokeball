package com.stdnullptr.pokeball;

import com.stdnullptr.pokeball.command.PokeballRootCommand;
import com.stdnullptr.pokeball.config.PluginConfig;
import com.stdnullptr.pokeball.item.PokeballItemFactory;
import com.stdnullptr.pokeball.listener.ProjectileListeners;
import com.stdnullptr.pokeball.listener.StasisCleanupListener;
import com.stdnullptr.pokeball.service.StasisService;
import com.stdnullptr.pokeball.util.Keys;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;

public final class Pokeball extends JavaPlugin {

    private Logger logger;
    private MiniMessage miniMessage;
    private PluginConfig configModel;
    private PokeballItemFactory itemFactory;
    private StasisService stasis;

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
        this.stasis = new StasisService(this, configModel);

        // Commands (Paper's programmatic registration)
        var root = new PokeballRootCommand(this, itemFactory, configModel);
        // Register root command with structured description and alias; suggestions provided by BasicCommand implementation
        registerCommand("pokeball", "Pokeball commands: give, reload, admin", java.util.List.of("pb"), root);

        // Listeners: capture and release via thrown projectile, plus cleanup
        getServer().getPluginManager().registerEvents(new ProjectileListeners(this, itemFactory, configModel, stasis), this);
        getServer().getPluginManager().registerEvents(new StasisCleanupListener(this, stasis), this);

        // No state reapplication on load: stasis flags are persisted with entities.

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
    public StasisService stasis() { return stasis; }
}
