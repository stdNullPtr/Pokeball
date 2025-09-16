package com.stdnullptr.pokeball.config.models;

public record FlightSpec(
        boolean enabled,
        boolean glow,
        int tickPeriod,
        boolean dust,
        float dustSize,
        int dustCount,
        boolean endRod,
        int endRodPoints,
        double endRodStep
) {

    public FlightSpec {
        if (tickPeriod <= 0) {
            throw new IllegalArgumentException("Tick period must be positive: " + tickPeriod);
        }
        if (dustSize <= 0.0f) {
            throw new IllegalArgumentException("Dust size must be positive: " + dustSize);
        }
        if (dustCount < 0) {
            throw new IllegalArgumentException("Dust count cannot be negative: " + dustCount);
        }
        if (endRodPoints < 0) {
            throw new IllegalArgumentException("End rod points cannot be negative: " + endRodPoints);
        }
        if (endRodStep <= 0.0) {
            throw new IllegalArgumentException("End rod step must be positive: " + endRodStep);
        }
    }
}
