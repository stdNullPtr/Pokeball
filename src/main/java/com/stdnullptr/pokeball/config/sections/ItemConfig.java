package com.stdnullptr.pokeball.config.sections;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;

/**
 * Configuration section for pokeball item properties
 */
public final class ItemConfig {

    private final String name;

    private final List<String> lore;

    private final Integer customModelData;

    public ItemConfig(final FileConfiguration config) {
        this.name = config.getString("item.name", "<yellow>Pokeball");
        this.lore = config.getStringList("item.lore");
        this.customModelData = config.get("item.custom-model-data") instanceof final Number n ? n.intValue() : null;

        if (name.isBlank()) {
            throw new IllegalArgumentException("Item name cannot be null or blank");
        }
        if (customModelData != null && customModelData < 0) {
            throw new IllegalArgumentException("Custom model data cannot be negative: " + customModelData);
        }
    }

    public String name() {
        return name;
    }

    public List<String> lore() {
        return List.copyOf(lore);
    } // Return immutable copy

    public Integer customModelData() {
        return customModelData;
    }
}
