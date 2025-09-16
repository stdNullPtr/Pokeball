package com.stdnullptr.pokeball.config.sections;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashSet;
import java.util.Set;

/**
 * Configuration section for stasis storage settings
 */
public final class StasisConfig {

    private final String world;

    private final double x;

    private final double y;

    private final double z;

    private final int capTotal;

    private final Set<String> allowedWorlds;

    public StasisConfig(final FileConfiguration config) {
        this.world = config.getString("stasis.world", "world");
        this.x = config.getDouble("stasis.x", 0.0);
        this.y = config.getDouble("stasis.y", 320.0);
        this.z = config.getDouble("stasis.z", 0.0);
        this.capTotal = config.getInt("stasis.cap.max-total", 500);
        this.allowedWorlds = new HashSet<>(config.getStringList("compat.worlds"));

        if (world.isBlank()) {
            throw new IllegalArgumentException("Stasis world cannot be null or blank");
        }
        if (capTotal < 0) {
            throw new IllegalArgumentException("Stasis cap total cannot be negative: " + capTotal);
        }
        if (y < -64 || y > 320) {
            throw new IllegalArgumentException("Stasis Y coordinate must be between -64 and 320: " + y);
        }
    }

    public String world() {
        return world;
    }

    public double x() {
        return x;
    }

    public double y() {
        return y;
    }

    public double z() {
        return z;
    }

    public int capTotal() {
        return capTotal;
    }

    public Set<String> allowedWorlds() {
        return Set.copyOf(allowedWorlds);
    } // Return immutable copy

}
