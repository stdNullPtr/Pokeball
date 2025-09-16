package com.stdnullptr.pokeball.config;

import com.stdnullptr.pokeball.config.sections.CaptureConfig;
import com.stdnullptr.pokeball.config.sections.EffectsConfig;
import com.stdnullptr.pokeball.config.sections.ItemConfig;
import com.stdnullptr.pokeball.config.sections.MessagesConfig;
import com.stdnullptr.pokeball.config.sections.StasisConfig;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.slf4j.Logger;

/**
 * Handles loading and reloading of configuration from FileConfiguration
 * Centralizes the config loading logic and provides clean separation
 */
public final class ConfigLoader {

    private final Plugin plugin;

    private final Logger logger;

    public ConfigLoader(final Plugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getSLF4JLogger();
    }

    /**
     * Loads all configuration sections from the plugin's config file
     *
     * @return ConfigData containing all parsed configuration sections
     */
    public ConfigData loadConfig() {
        try {
            // Ensure default config is saved
            plugin.saveDefaultConfig();

            // Reload to get latest changes
            plugin.reloadConfig();

            final FileConfiguration config = plugin.getConfig();

            // Load each section independently
            final var itemConfig = new ItemConfig(config);
            final var captureConfig = new CaptureConfig(config, logger);
            final var stasisConfig = new StasisConfig(config);
            final var effectsConfig = new EffectsConfig(config);
            final var messagesConfig = new MessagesConfig(config);

            logger.info("Configuration loaded successfully");

            return new ConfigData(
                    itemConfig,
                    captureConfig,
                    stasisConfig,
                    effectsConfig,
                    messagesConfig
            );

        } catch (final Exception e) {
            logger.error("Failed to load configuration", e);
            throw new IllegalStateException("Configuration loading failed", e);
        }
    }

    /**
     * Record to hold all configuration sections
     * Provides immutable access to all config data
     */
    public record ConfigData(
            ItemConfig itemConfig,
            CaptureConfig captureConfig,
            StasisConfig stasisConfig,
            EffectsConfig effectsConfig,
            MessagesConfig messagesConfig
    ) {
    }
}
