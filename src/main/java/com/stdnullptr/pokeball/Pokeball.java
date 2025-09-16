package com.stdnullptr.pokeball;

import com.stdnullptr.pokeball.command.PokeballCommands;
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
    private StasisService stasis;

    @Override
    public void onEnable() {
        final PluginConfig configModel;
        final Keys keys;
        final PokeballItemFactory itemFactory;
        this.logger = getSLF4JLogger();
        this.miniMessage = MiniMessage.miniMessage();

        // Save default config if not present
        saveDefaultConfig();
        configModel = new PluginConfig(this);

        // Prepare Keys instance (DI-friendly)
        keys = new Keys(this);

        // Services / Factories
        itemFactory = new PokeballItemFactory(this, configModel, keys);
        this.stasis = new StasisService(this, configModel);

        // Commands (Paper Brigadier via lifecycle)
        new PokeballCommands(this, itemFactory, configModel).register();

        // Listeners: capture and release via thrown projectile, plus cleanup
        getServer()
                .getPluginManager()
                .registerEvents(new ProjectileListeners(this, itemFactory, configModel, stasis, keys), this);
        getServer()
                .getPluginManager()
                .registerEvents(new StasisCleanupListener(stasis), this);

        // Cleanup stale stasis entries asynchronously on startup (batched per tick)
        stasis.cleanupInvalidAsync(logger);

        logger.info("Pokeball enabled.");
    }

    @Override
    public void onDisable() {
        logger.info("Pokeball disabled.");
    }

    public MiniMessage mini() { return miniMessage; }

    public StasisService stasis() { return stasis; }
}
