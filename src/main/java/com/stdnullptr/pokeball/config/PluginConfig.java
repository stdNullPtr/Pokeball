package com.stdnullptr.pokeball.config;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.Plugin;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class PluginConfig {
    private final Plugin plugin;
    private final MiniMessage mini = MiniMessage.miniMessage();

    private String itemName;
    private List<String> itemLore;
    private Integer customModelData;
    private boolean consumeOnRelease;
    private boolean creativeBypassEnabled;
    private String creativeBypassPermission;
    private boolean creativeBypassAnnotate;
    private String creativeBypassAnnotation;
    private Set<String> allowedWorlds;
    private Set<EntityType> allowedTypes;

    // Messages
    public String msgPrefix;
    public String msgCaptureSuccess;
    public String msgCaptureFailBlocked;
    public String msgCaptureFailInvalid;
    public String msgCaptureFailPermission;
    public String msgCaptureFailWorld;
    public String msgReleaseSuccess;
    public String msgReleaseFailEmpty;
    public String msgReleaseFailPermission;
    public String msgReleaseFailWorld;
    public String msgGiven;
    public String msgReloaded;

    public PluginConfig(Plugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        plugin.reloadConfig();
        FileConfiguration c = plugin.getConfig();

        this.itemName = c.getString("item.name", "<yellow>Pokeball");
        this.itemLore = c.getStringList("item.lore");
        this.customModelData = c.get("item.custom-model-data") instanceof Number n ? n.intValue() : null;

        this.consumeOnRelease = c.getBoolean("capture.consume-on-release", false);

        this.creativeBypassEnabled = c.getBoolean("creative-bypass.enabled", true);
        this.creativeBypassPermission = c.getString("creative-bypass.permission", "pokeball.bypass.creative");
        this.creativeBypassAnnotate = c.getBoolean("creative-bypass.annotate", true);
        this.creativeBypassAnnotation = c.getString("creative-bypass.annotation-line", "<gray>Captured via <red>Creative Bypass</red></gray>");

        this.allowedWorlds = new HashSet<>(c.getStringList("compat.worlds"));

        this.allowedTypes = c.getStringList("capture.allowed-entity-types").stream()
            .map(this::toEntityType).collect(Collectors.toCollection(HashSet::new));

        this.msgPrefix = c.getString("messages.prefix", "<gray>[<yellow>Pokeball</yellow>]");
        this.msgCaptureSuccess = c.getString("messages.capture-success", "<green>Captured a <yellow><type></yellow>!");
        this.msgCaptureFailBlocked = c.getString("messages.capture-fail-blocked", "<red>You cannot capture that mob.");
        this.msgCaptureFailInvalid = c.getString("messages.capture-fail-invalid", "<red>Use a Pokeball on a valid mob.");
        this.msgCaptureFailPermission = c.getString("messages.capture-fail-permission", "<red>You lack permission to capture.");
        this.msgCaptureFailWorld = c.getString("messages.capture-fail-world", "<red>Capturing is not allowed in this world.");
        this.msgReleaseSuccess = c.getString("messages.release-success", "<green>Released a <yellow><type></yellow>.");
        this.msgReleaseFailEmpty = c.getString("messages.release-fail-empty", "<red>This Pokeball is empty.");
        this.msgReleaseFailPermission = c.getString("messages.release-fail-permission", "<red>You lack permission to release.");
        this.msgReleaseFailWorld = c.getString("messages.release-fail-world", "<red>Releasing is not allowed in this world.");
        this.msgGiven = c.getString("messages.given", "<green>Gave <yellow><count></yellow> Pokeball(s) to <yellow><player></yellow>.");
        this.msgReloaded = c.getString("messages.reloaded", "<green>Configuration reloaded.");
    }

    private EntityType toEntityType(String name) {
        try {
            return EntityType.valueOf(name);
        } catch (Exception e) {
            Bukkit.getLogger().warning("Unknown entity type in config: " + name);
            return null;
        }
    }

    public String itemName() { return itemName; }
    public List<String> itemLore() { return itemLore; }
    public Integer customModelData() { return customModelData; }
    public boolean consumeOnRelease() { return consumeOnRelease; }
    public boolean creativeBypassEnabled() { return creativeBypassEnabled; }
    public String creativeBypassPermission() { return creativeBypassPermission; }
    public boolean creativeBypassAnnotate() { return creativeBypassAnnotate; }
    public String creativeBypassAnnotation() { return creativeBypassAnnotation; }
    public Set<String> allowedWorlds() { return allowedWorlds; }
    public Set<EntityType> allowedTypes() { return allowedTypes; }
    
}
