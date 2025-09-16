package com.stdnullptr.pokeball.util;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;

public final class Keys {
    private static Plugin plugin;

    public static NamespacedKey BALL_ID;
    public static NamespacedKey CAPTURED_TYPE;
    public static NamespacedKey CAPTURED_DATA_VERSION;
    public static NamespacedKey PROJECTILE_BALL;
    public static NamespacedKey CAPTURED_IS_BABY;
    public static NamespacedKey CAPTURED_VARIANT;

    private Keys() { }

    public static void init(Plugin p) {
        plugin = p;
        BALL_ID = key("ball_id");
        CAPTURED_TYPE = key("captured_type");
        CAPTURED_DATA_VERSION = key("captured_data_version");
        PROJECTILE_BALL = key("projectile_ball");
        CAPTURED_IS_BABY = key("captured_is_baby");
        CAPTURED_VARIANT = key("captured_variant");
    }

    private static NamespacedKey key(String path) {
        return new NamespacedKey(plugin, path);
    }
}
