package com.stdnullptr.pokeball.config.models;

public record EffectSpec(
        boolean particles,
        String particle,
        int particleCount,
        String sound,
        float volume,
        float pitch
) {

    public EffectSpec {
        if (particleCount < 0) {
            throw new IllegalArgumentException("Particle count cannot be negative: " + particleCount);
        }
        if (volume < 0.0f || volume > 1.0f) {
            throw new IllegalArgumentException("Volume must be between 0.0 and 1.0: " + volume);
        }
        if (pitch < 0.0f) {
            throw new IllegalArgumentException("Pitch cannot be negative: " + pitch);
        }
        if (particle == null || particle.isBlank()) {
            throw new IllegalArgumentException("Particle type cannot be null or blank");
        }
        if (sound == null || sound.isBlank()) {
            throw new IllegalArgumentException("Sound cannot be null or blank");
        }
    }
}
