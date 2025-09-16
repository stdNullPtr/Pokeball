package com.stdnullptr.pokeball.service;

import com.stdnullptr.pokeball.config.PluginConfig;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class StasisService {
    private final Plugin plugin;

    private final PluginConfig cfg;
    private final File file;
    private final FileConfiguration data;

    public StasisService(final Plugin plugin, final PluginConfig cfg) {
        this.plugin = plugin;
        this.cfg = cfg;
        if (!plugin
                .getDataFolder()
                .exists()) {
            plugin
                    .getDataFolder()
                    .mkdirs();
        }
        this.file = new File(plugin.getDataFolder(), "stasis.yml");
        this.data = YamlConfiguration.loadConfiguration(file);
    }

    /**
     * Clean up stale entries from stasis.yml asynchronously after startup by
     * processing a small batch per tick to avoid blocking the main thread.
     */
    public void cleanupInvalidAsync(final Logger logger) {
        final List<String> keys = new ArrayList<>(data.getKeys(false));
        if (keys.isEmpty()) {
            return;
        }
        final int batchSize = 100;
        final int[] index = {0};
        final int[] removed = {0};
        new BukkitRunnable() {
            @Override
            public void run() {
                int processed = 0;
                while (index[0] < keys.size() && processed < batchSize) {
                    final String key = keys.get(index[0]++);
                    processed++;
                    try {
                        final String worldName = data.getString(key + ".world");
                        final String uuidStr = data.getString(key + ".uuid");
                        final Integer cx = (data.contains(key + ".chunkX") ? data.getInt(key + ".chunkX") : null);
                        final Integer cz = (data.contains(key + ".chunkZ") ? data.getInt(key + ".chunkZ") : null);
                        if (worldName == null || uuidStr == null || cx == null || cz == null) {
                            data.set(key, null);
                            removed[0]++;
                            continue;
                        }
                        final World world = Bukkit.getWorld(worldName);
                        if (world == null) {
                            data.set(key, null);
                            removed[0]++;
                            continue;
                        }
                        final Chunk ch = world.getChunkAt(cx, cz);
                        if (!ch.isLoaded()) {
                            ch.load();
                        }
                        final UUID uuid = UUID.fromString(uuidStr);
                        final Entity ent = Bukkit.getEntity(uuid);
                        if (ent == null) {
                            data.set(key, null);
                            removed[0]++;
                        }
                    } catch (final Exception ex) {
                        data.set(key, null);
                        removed[0]++;
                    }
                }
                if (index[0] >= keys.size()) {
                    if (removed[0] > 0) {
                        save();
                        logger.info(
                                "Cleaned {} stale stasis entr{} on startup.",
                                removed[0],
                                removed[0] == 1 ? "y" : "ies"
                        );
                    }
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    public void park(final Entity entity, final UUID ballId) {
        // Enforce cap if configured
        if (cfg.stasisCapEnabled()) {
            final int total = data
                    .getKeys(false)
                    .size();
            if (total >= cfg.stasisCapTotal()) {
                // refuse capture by throwing IllegalStateException; caller will handle refund
                throw new IllegalStateException("Stasis cap reached");
            }
        }
        // Freeze and hide entity at stash
        if (entity instanceof final LivingEntity le) {
            try {
                le.setAI(false);
            } catch (final Exception ignored) {
            }
            try {
                le.setCollidable(false);
            } catch (final Exception ignored) {
            }
            try {
                le.setInvisible(true);
            } catch (final Exception ignored) {
            }
            try {
                le.setInvulnerable(true);
            } catch (final Exception ignored) {
            }
            try {
                le.setSilent(true);
            } catch (final Exception ignored) {
            }
            try {
                le.setRemoveWhenFarAway(false);
            } catch (final Exception ignored) {
            }
            try {
                le.setGravity(false);
            } catch (final Exception ignored) {
            }

            // Move to stash location to avoid collisions/visibility at capture point
            final Location stashLoc = computeStashLocation(le.getWorld());
            le.teleport(stashLoc);

            // Persist mapping (after teleport)
            final String key = ballId.toString();
            final var loc = le.getLocation();
            data.set(key + ".world", loc.getWorld().getName());
            data.set(key + ".uuid", le.getUniqueId().toString());
            data.set(key + ".chunkX", loc.getChunk().getX());
            data.set(key + ".chunkZ", loc.getChunk().getZ());
            data.set(key + ".type", le.getType().name());
            save();
        }
    }

    public boolean release(final UUID ballId, final Location target) {
        final String key = ballId.toString();
        final String worldName = data.getString(key + ".world");
        final String uuidStr = data.getString(key + ".uuid");
        final Integer cx = (data.contains(key + ".chunkX") ? data.getInt(key + ".chunkX") : null);
        final Integer cz = (data.contains(key + ".chunkZ") ? data.getInt(key + ".chunkZ") : null);
        if (worldName == null || uuidStr == null || cx == null || cz == null) return false;
        final World world = Bukkit.getWorld(worldName);
        if (world == null) return false;

        // Ensure source chunk is loaded so the entity becomes accessible
        final Chunk ch = world.getChunkAt(cx, cz);
        if (!ch.isLoaded()) ch.load();

        final UUID uuid = UUID.fromString(uuidStr);
        final Entity e = Bukkit.getEntity(uuid);
        if (e == null) return false;

        // Teleport while invisible, then unfreeze a tick later
        if (e instanceof final LivingEntity le) {
            le.teleport(target);
            try {
                le.setVelocity(new Vector(0, 0, 0));
            } catch (final Throwable ignored) {
            }
            try {
                le.setFallDistance(0f);
            } catch (final Throwable ignored) {
            }
            playReleaseEffects(target);
            // Re-equip after one tick to avoid any visual snap
            new BukkitRunnable() {
                @Override public void run() {
                    try {
                        le.setAI(true);
                    } catch (final Exception ignored) {
                    }
                    try {
                        le.setCollidable(true);
                    } catch (final Exception ignored) {
                    }
                    try {
                        le.setInvisible(false);
                    } catch (final Exception ignored) {
                    }
                    try {
                        le.setInvulnerable(false);
                    } catch (final Exception ignored) {
                    }
                    try {
                        le.setSilent(false);
                    } catch (final Exception ignored) {
                    }
                    try {
                        le.setGravity(true);
                    } catch (final Exception ignored) {
                    }
                    try {
                        le.setGliding(false);
                    } catch (final Exception ignored) {
                    }
                    try {
                        le.setVelocity(new Vector(0, 0, 0));
                    } catch (final Exception ignored) {
                    }
                    try {
                        le.setFallDistance(0f);
                    } catch (final Exception ignored) {
                    }
                }
            }.runTaskLater(plugin, 1L);
        } else {
            e.teleport(target);
        }

        data.set(key, null);
        save();
        return true;
    }

    private Location computeStashLocation(final World fallbackWorld) {
        World w = Bukkit.getWorld(cfg.stasisWorld());
        if (w == null) w = fallbackWorld;
        return new Location(w, cfg.stasisX(), cfg.stasisY(), cfg.stasisZ());
    }

    public EntityType peekType(final UUID ballId) {
        final String type = data.getString(ballId.toString() + ".type");
        if (type == null) return null;
        try {
            return EntityType.valueOf(type);
        } catch (final Exception e) {
            return null;
        }
    }

    private void save() {
        try {
            data.save(file);
        } catch (final IOException e) {
            plugin
                    .getSLF4JLogger()
                    .error("Failed to save stasis.yml", e);
        }
    }

    // Utilities
    public Set<String> ids() {
        return data.getKeys(false);
    }
    public FileConfiguration data() { return data; }

    public EntityType typeOf(final UUID id) {
        final String s = data.getString(id.toString() + ".type");
        if (s == null) {
            return null;
        }
        try {
            return EntityType.valueOf(s);
        } catch (final Exception e) {
            return null;
        }
    }

    public Location locationOf(final UUID id) {
        final String base = id.toString();
        final String worldName = data.getString(base + ".world");
        final Integer cx = (data.contains(base + ".chunkX") ? data.getInt(base + ".chunkX") : null);
        final Integer cz = (data.contains(base + ".chunkZ") ? data.getInt(base + ".chunkZ") : null);
        if (worldName == null || cx == null || cz == null) return null;
        final World w = Bukkit.getWorld(worldName);
        if (w == null) {
            return null;
        }
        final int x = cx << 4;
        final int z = cz << 4;
        return new Location(w, x + 8.0, w.getHighestBlockYAt(x + 8, z + 8) + 1.0, z + 8.0);
    }

    public boolean remove(final UUID id) {
        data.set(id.toString(), null);
        save();
        return true;
    }

    public void playCaptureEffects(final Location at) {
        playEffects(at, cfg.captureEffect());
    }

    public void playReleaseEffects(final Location at) {
        playEffects(at, cfg.releaseEffect());
    }

    private void playEffects(final Location at, final PluginConfig.EffectSpec spec) {
        if (spec == null) return;
        if (spec.particles) {
            if ("FANCY".equalsIgnoreCase(spec.particle)) {
                try {
                    // Ring of red dust around the point
                    final var world = at.getWorld();
                    final var dust = new Particle.DustOptions(Color.fromRGB(220, 40, 40), 1.3f);
                    final int points = Math.max(12, spec.particleCount);
                    final double radius = 0.7;
                    for (int i = 0; i < points; i++) {
                        final double angle = (2 * Math.PI * i) / points;
                        final double x = at.getX() + Math.cos(angle) * radius;
                        final double z = at.getZ() + Math.sin(angle) * radius;
                        world.spawnParticle(Particle.DUST, new Location(world, x, at.getY() + 0.2, z), 1, dust);
                    }
                    // Sparkle burst
                    world.spawnParticle(Particle.CRIT, at, Math.max(10, spec.particleCount / 2), 0.2, 0.2, 0.2, 0.02);
                    // Subtle swirl
                    world.spawnParticle(
                            Particle.END_ROD,
                            at
                                    .clone()
                                    .add(0, 0.5, 0),
                            40,
                            0.6,
                            0.6,
                            0.6,
                            0.0
                    );
                } catch (final Exception ignored) {
                }
            } else {
                try {
                    final var p = Particle.valueOf(spec.particle.toUpperCase());
                    at.getWorld().spawnParticle(p, at, Math.max(1, spec.particleCount));
                } catch (final Exception ignored) {
                }
            }
        }
        if (spec.sound != null && !spec.sound.isEmpty()) {
            try {
                final var key = NamespacedKey.fromString(spec.sound.toLowerCase());
                if (key != null) {
                    final var s = Registry.SOUNDS.get(key);
                    if (s != null) {
                        at
                                .getWorld()
                                .playSound(at, s, spec.volume, spec.pitch);
                    }
                }
            } catch (final Exception ignored) {
            }
        }
    }
}
