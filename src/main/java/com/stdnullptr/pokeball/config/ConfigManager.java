package com.stdnullptr.pokeball.config;

import com.stdnullptr.pokeball.config.sections.CaptureConfig;
import com.stdnullptr.pokeball.config.sections.EffectsConfig;
import com.stdnullptr.pokeball.config.sections.ItemConfig;
import com.stdnullptr.pokeball.config.sections.MessagesConfig;
import com.stdnullptr.pokeball.config.sections.StasisConfig;
import org.bukkit.plugin.Plugin;

/**
 * Main configuration manager acting as a facade for all configuration sections
 * Replaces the monolithic PluginConfig with a properly structured, immutable design
 */
public final class ConfigManager {

    private final ConfigLoader configLoader;

    private ConfigLoader.ConfigData configData;

    public ConfigManager(final Plugin plugin) {
        this.configLoader = new ConfigLoader(plugin);
        reload();
    }

    /**
     * Reloads all configuration from the config file
     */
    public void reload() {
        this.configData = configLoader.loadConfig();
    }

    /**
     * @return Configuration for pokeball item properties (name, lore, custom model data)
     */
    public ItemConfig items() {
        return configData.itemConfig();
    }

    /**
     * @return Configuration for entity capture mechanics and permissions
     */
    public CaptureConfig capture() {
        return configData.captureConfig();
    }

    /**
     * @return Configuration for entity stasis storage location and limits
     */
    public StasisConfig stasis() {
        return configData.stasisConfig();
    }

    /**
     * @return Configuration for visual and audio effects
     */
    public EffectsConfig effects() {
        return configData.effectsConfig();
    }

    /**
     * @return Configuration for all user-facing messages
     */
    public MessagesConfig messages() {
        return configData.messagesConfig();
    }

}
