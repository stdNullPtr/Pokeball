package com.stdnullptr.pokeball.config.sections;

import com.stdnullptr.pokeball.config.models.EffectSpec;
import com.stdnullptr.pokeball.config.models.FlightSpec;
import com.stdnullptr.pokeball.config.models.RefundMode;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Configuration section for visual and audio effects
 */
public final class EffectsConfig {

    private final EffectSpec captureEffect;

    private final EffectSpec releaseEffect;

    private final FlightSpec flightSpec;

    private final double releaseOffsetNormal;

    private final double releaseOffsetUp;

    private final int releaseProbeSteps;

    private final RefundMode refundMode;

    public EffectsConfig(final FileConfiguration config) {
        this.captureEffect = readEffect(config, "effects.capture");
        this.releaseEffect = readEffect(config, "effects.release");
        this.flightSpec = readFlight(config);
        this.releaseOffsetNormal = config.getDouble("release.offset-normal", 0.31);
        this.releaseOffsetUp = config.getDouble("release.offset-up", 0.05);
        this.releaseProbeSteps = Math.max(1, config.getInt("release.probe-max-steps", 3));
        this.refundMode = parseRefundMode(config.getString("refund.mode", "GIVE"));

        // Validation
        if (releaseOffsetNormal < 0.0) {
            throw new IllegalArgumentException("Release offset normal cannot be negative: " + releaseOffsetNormal);
        }
        if (releaseOffsetUp < 0.0) {
            throw new IllegalArgumentException("Release offset up cannot be negative: " + releaseOffsetUp);
        }
    }

    private EffectSpec readEffect(final FileConfiguration config, final String path) {
        return new EffectSpec(
                config.getBoolean(path + ".particles", true),
                config.getString(path + ".particle", "CLOUD"),
                config.getInt(path + ".particle-count", 10),
                config.getString(path + ".sound", "entity.player.levelup"),
                (float) config.getDouble(path + ".volume", 1.0),
                (float) config.getDouble(path + ".pitch", 1.0)
        );
    }

    private FlightSpec readFlight(final FileConfiguration config) {
        final String basePath = "effects.flight";
        return new FlightSpec(
                config.getBoolean(basePath + ".enabled", true),
                config.getBoolean(basePath + ".glow", true),
                Math.max(1, config.getInt(basePath + ".tick-period", 1)),
                config.getBoolean(basePath + ".dust", true),
                (float) config.getDouble(basePath + ".dust-size", 0.9),
                config.getInt(basePath + ".dust-count", 1),
                config.getBoolean(basePath + ".end-rod", true),
                config.getInt(basePath + ".end-rod-points", 2),
                config.getDouble(basePath + ".end-rod-step", 0.15)
        );
    }

    private RefundMode parseRefundMode(final String mode) {
        try {
            return RefundMode.valueOf(mode.toUpperCase());
        } catch (final Exception e) {
            return RefundMode.GIVE; // Default fallback
        }
    }

    public EffectSpec captureEffect() {
        return captureEffect;
    }

    public EffectSpec releaseEffect() {
        return releaseEffect;
    }

    public FlightSpec flight() {
        return flightSpec;
    }

    public double releaseOffsetNormal() {
        return releaseOffsetNormal;
    }

    public double releaseOffsetUp() {
        return releaseOffsetUp;
    }

    public int releaseProbeSteps() {
        return releaseProbeSteps;
    }

    public RefundMode refundMode() {
        return refundMode;
    }
}
