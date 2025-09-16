package com.stdnullptr.pokeball.util;

import lombok.Getter;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;

@Getter
public final class Keys {
    private final NamespacedKey ballId;

    private final NamespacedKey capturedType;

    private final NamespacedKey capturedDataVersion;

    private final NamespacedKey projectileBall;

    private final NamespacedKey projectileReleaseId;

    public Keys(final Plugin plugin) {
        this.ballId = key(plugin, "ball_id");
        this.capturedType = key(plugin, "captured_type");
        this.capturedDataVersion = key(plugin, "captured_data_version");
        this.projectileBall = key(plugin, "projectile_ball");
        this.projectileReleaseId = key(plugin, "projectile_release_id");
    }

    private static NamespacedKey key(final Plugin plugin, final String path) {
        return new NamespacedKey(plugin, path);
    }
}
