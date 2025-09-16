package com.stdnullptr.pokeball.config.sections;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.slf4j.Logger;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Configuration section for capture rules and special permissions
 */
public final class CaptureConfig {

    private final boolean consumeOnRelease;

    private final String specialCapturePermission;

    private final boolean specialCaptureAnnotate;

    private final String specialCaptureAnnotation;

    private final Set<EntityType> allowedTypes;

    public CaptureConfig(final FileConfiguration config, final Logger logger) {
        this.consumeOnRelease = config.getBoolean("capture.consume-on-release", false);
        this.specialCapturePermission = config.getString("special-capture.permission", "pokeball.capture.any");
        this.specialCaptureAnnotate = config.getBoolean("special-capture.annotate", true);
        this.specialCaptureAnnotation = config.getString(
                "special-capture.annotation-line",
                "<gray>How did you get that one?</gray>"
        );

        // Parse entity types with error handling
        this.allowedTypes = config
                .getStringList("capture.allowed-entity-types")
                .stream()
                .map(name -> toEntityType(name, logger))
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(HashSet::new));

        // Validation
        if (specialCapturePermission.isBlank()) {
            throw new IllegalArgumentException("Special capture permission cannot be null or blank");
        }
        if (specialCaptureAnnotation.isBlank()) {
            throw new IllegalArgumentException("Special capture annotation cannot be null or blank");
        }
        if (allowedTypes.isEmpty()) {
            throw new IllegalArgumentException("At least one allowed entity type must be configured");
        }
    }

    private EntityType toEntityType(final String name, final Logger logger) {
        try {
            return EntityType.valueOf(name);
        } catch (final Exception e) {
            logger.warn("Invalid entity type in config: {}", name);
            return null;
        }
    }

    public boolean consumeOnRelease() {
        return consumeOnRelease;
    }

    public String specialCapturePermission() {
        return specialCapturePermission;
    }

    public boolean specialCaptureAnnotate() {
        return specialCaptureAnnotate;
    }

    public String specialCaptureAnnotation() {
        return specialCaptureAnnotation;
    }

    public Set<EntityType> allowedTypes() {
        return Set.copyOf(allowedTypes);
    } // Return immutable copy
}
