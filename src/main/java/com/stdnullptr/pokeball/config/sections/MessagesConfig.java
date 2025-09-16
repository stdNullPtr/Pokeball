package com.stdnullptr.pokeball.config.sections;

import lombok.Getter;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Configuration section for all plugin messages
 */
@Getter
public final class MessagesConfig {

    private final String prefix;

    private final String captureSuccess;

    private final String captureFailBlocked;

    private final String captureFailPlayer;

    private final String captureFailWorld;

    private final String releaseSuccess;

    private final String given;

    private final String reloaded;

    public MessagesConfig(final FileConfiguration config) {
        this.prefix = config.getString("messages.prefix", "<gray>[<yellow>Pokeball</yellow>]");
        this.captureSuccess = config.getString(
                "messages.capture-success",
                "<green>Captured a <yellow><type></yellow>!"
        );
        this.captureFailBlocked = config.getString(
                "messages.capture-fail-blocked",
                "<red>You cannot capture that mob."
        );
        this.captureFailPlayer = config.getString("messages.capture-fail-player", "<yellow>Really?</yellow>");
        this.captureFailWorld = config.getString(
                "messages.capture-fail-world",
                "<red>Capturing is not allowed in this world."
        );
        this.releaseSuccess = config.getString(
                "messages.release-success",
                "<green>Released a <yellow><type></yellow>."
        );
        this.given = config.getString(
                "messages.given",
                "<green>Gave <yellow><count></yellow> Pokeball(s) to <yellow><player></yellow>."
        );
        this.reloaded = config.getString("messages.reloaded", "<green>Configuration reloaded.");

        validateMessage("messages.prefix", this.prefix);
        validateMessage("messages.capture-success", this.captureSuccess);
        validateMessage("messages.capture-fail-blocked", this.captureFailBlocked);
        validateMessage("messages.capture-fail-player", this.captureFailPlayer);
        validateMessage("messages.capture-fail-world", this.captureFailWorld);
        validateMessage("messages.release-success", this.releaseSuccess);
        validateMessage("messages.given", this.given);
        validateMessage("messages.reloaded", this.reloaded);
    }

    private void validateMessage(final String key, final String message) {
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("Message for key '" + key + "' cannot be null or blank");
        }
    }
}
