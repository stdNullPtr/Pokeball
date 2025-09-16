package com.stdnullptr.pokeball.util;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;

public final class Keys {
    private static Plugin plugin;

    public static NamespacedKey BALL_ID;
    public static NamespacedKey CAPTURED_TYPE;
    public static NamespacedKey CAPTURED_DATA_VERSION;
    public static NamespacedKey PROJECTILE_BALL;
    public static NamespacedKey PROJECTILE_RELEASE_ID;

    private Keys() { }

    public static void init(Plugin p) {
        plugin = p;
        BALL_ID = key("ball_id");
        CAPTURED_TYPE = key("captured_type");
        CAPTURED_DATA_VERSION = key("captured_data_version");
        PROJECTILE_BALL = key("projectile_ball");
        PROJECTILE_RELEASE_ID = key("projectile_release_id");
    }

    private static NamespacedKey key(String path) {
        return new NamespacedKey(plugin, path);
    }
}
