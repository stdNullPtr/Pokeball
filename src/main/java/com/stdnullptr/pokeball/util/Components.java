package com.stdnullptr.pokeball.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

public final class Components {
    private Components() {}

    public static Component mm(MiniMessage mini, String text) {
        return mini.deserialize(text);
    }

    public static Component mmf(MiniMessage mini, String pattern, String placeholder, String value) {
        return mini.deserialize(pattern.replace(placeholder, value));
    }
}

