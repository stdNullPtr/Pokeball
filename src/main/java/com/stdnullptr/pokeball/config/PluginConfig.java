package com.stdnullptr.pokeball.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.Plugin;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class PluginConfig {
    private final Plugin plugin;

    private String itemName;
    private List<String> itemLore;
    private Integer customModelData;
    private boolean consumeOnRelease;
    private boolean creativeBypassEnabled;
    private String creativeBypassPermission;
    private boolean creativeBypassAnnotate;
    private String creativeBypassAnnotation;
    private String stasisWorld;
    private double stasisX;
    private double stasisY;
    private double stasisZ;
    private boolean stasisCapEnabled;
    private int stasisCapTotal;

    // Effects
    public static final class EffectSpec {
        public boolean particles;
        public String particle;
        public int particleCount;
        public String sound;
        public float volume;
        public float pitch;
    }
    private EffectSpec captureEffect;
    private EffectSpec releaseEffect;
    public static final class FlightSpec {
        public boolean enabled;
        public boolean glow;
        public int tickPeriod;
        public boolean dust;
        public float dustSize;
        public int dustCount;
        public boolean endRod;
        public int endRodPoints;
        public double endRodStep;
    }
    private FlightSpec flightSpec;
    private double releaseOffsetNormal;
    private double releaseOffsetUp;
    private int releaseProbeSteps;
    private Set<String> allowedWorlds;
    private Set<EntityType> allowedTypes;

    private RefundMode refundMode;

    public PluginConfig(final Plugin plugin) {
        this.plugin = plugin;
        reload();
    }

    // Messages
    public String msgPrefix;
    public String msgCaptureSuccess;
    public String msgCaptureFailBlocked;
    public String msgCaptureFailPlayer;
    public String msgCaptureFailWorld;
    public String msgReleaseSuccess;
    public String msgGiven;
    public String msgReloaded;

    public void reload() {
        plugin.reloadConfig();
        final FileConfiguration c = plugin.getConfig();

        this.itemName = c.getString("item.name", "<yellow>Pokeball");
        this.itemLore = c.getStringList("item.lore");
        this.customModelData = c.get("item.custom-model-data") instanceof final Number n ? n.intValue() : null;

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
        this.msgCaptureFailPlayer = c.getString("messages.capture-fail-player", "<yellow>Really?</yellow>");
        this.msgCaptureFailWorld = c.getString("messages.capture-fail-world", "<red>Capturing is not allowed in this world.");
        this.msgReleaseSuccess = c.getString("messages.release-success", "<green>Released a <yellow><type></yellow>.");
        this.msgGiven = c.getString("messages.given", "<green>Gave <yellow><count></yellow> Pokeball(s) to <yellow><player></yellow>.");
        this.msgReloaded = c.getString("messages.reloaded", "<green>Configuration reloaded.");

        this.stasisWorld = c.getString("stasis.world", "world");
        this.stasisX = c.getDouble("stasis.x", 0.0);
        this.stasisY = c.getDouble("stasis.y", 320.0);
        this.stasisZ = c.getDouble("stasis.z", 0.0);
        this.stasisCapEnabled = c.getBoolean("stasis.cap.enabled", false);
        this.stasisCapTotal = c.getInt("stasis.cap.max-total", 500);

        this.captureEffect = readEffect(c, "effects.capture");
        this.releaseEffect = readEffect(c, "effects.release");
        this.flightSpec = readFlight(c);
        this.releaseOffsetNormal = c.getDouble("release.offset-normal", 0.31);
        this.releaseOffsetUp = c.getDouble("release.offset-up", 0.05);
        this.releaseProbeSteps = Math.max(1, c.getInt("release.probe-max-steps", 3));

        // Refund behavior
        final String refund = c.getString("refund.mode", "GIVE");
        RefundMode mode;
        try {
            mode = RefundMode.valueOf(refund.toUpperCase());
        } catch (final Exception e) {
            mode = RefundMode.GIVE;
        }
        this.refundMode = mode;
    }

    private EntityType toEntityType(final String name) {
        try {
            return EntityType.valueOf(name);
        } catch (final Exception e) {
            plugin
                    .getLogger()
                    .warning("Invalid entity type in config: " + name);
            return null;
        }
    }

    public RefundMode refundMode() {
        return refundMode;
    }

    public String itemName() { return itemName; }
    public List<String> itemLore() { return itemLore; }
    public Integer customModelData() { return customModelData; }
    public boolean consumeOnRelease() { return consumeOnRelease; }
    public boolean creativeBypassEnabled() { return creativeBypassEnabled; }
    public String creativeBypassPermission() { return creativeBypassPermission; }
    public boolean creativeBypassAnnotate() { return creativeBypassAnnotate; }
    public String creativeBypassAnnotation() { return creativeBypassAnnotation; }
    public String stasisWorld() { return stasisWorld; }
    public double stasisX() { return stasisX; }
    public double stasisY() { return stasisY; }
    public double stasisZ() { return stasisZ; }
    public boolean stasisCapEnabled() { return stasisCapEnabled; }
    public int stasisCapTotal() { return stasisCapTotal; }
    public EffectSpec captureEffect() { return captureEffect; }
    public EffectSpec releaseEffect() { return releaseEffect; }
    public FlightSpec flight() { return flightSpec; }
    public double releaseOffsetNormal() { return releaseOffsetNormal; }
    public double releaseOffsetUp() { return releaseOffsetUp; }
    public int releaseProbeSteps() { return releaseProbeSteps; }

    private EffectSpec readEffect(final FileConfiguration c, final String path) {
        final EffectSpec e = new EffectSpec();
        e.particles = c.getBoolean(path + ".particles", true);
        e.particle = c.getString(path + ".particle", "CLOUD");
        e.particleCount = c.getInt(path + ".particle-count", 10);
        e.sound = c.getString(path + ".sound", "entity.player.levelup");
        e.volume = (float) c.getDouble(path + ".volume", 1.0);
        e.pitch = (float) c.getDouble(path + ".pitch", 1.0);
        return e;
    }

    private FlightSpec readFlight(final FileConfiguration c) {
        final FlightSpec f = new FlightSpec();
        final String flightConfigPath = "effects.flight";
        f.enabled = c.getBoolean(flightConfigPath + ".enabled", true);
        f.glow = c.getBoolean(flightConfigPath + ".glow", true);
        f.tickPeriod = Math.max(1, c.getInt(flightConfigPath + ".tick-period", 1));
        f.dust = c.getBoolean(flightConfigPath + ".dust", true);
        f.dustSize = (float) c.getDouble(flightConfigPath + ".dust-size", 0.9);
        f.dustCount = c.getInt(flightConfigPath + ".dust-count", 1);
        f.endRod = c.getBoolean(flightConfigPath + ".end-rod", true);
        f.endRodPoints = c.getInt(flightConfigPath + ".end-rod-points", 2);
        f.endRodStep = c.getDouble(flightConfigPath + ".end-rod-step", 0.15);
        return f;
    }

    public enum RefundMode {GIVE, DROP}
    public Set<String> allowedWorlds() { return allowedWorlds; }
    public Set<EntityType> allowedTypes() { return allowedTypes; }

}
