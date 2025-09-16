package com.stdnullptr.pokeball.service;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class StasisService {
    private final Plugin plugin;
    private final com.stdnullptr.pokeball.config.PluginConfig cfg;
    private final File file;
    private final FileConfiguration data;

    public StasisService(Plugin plugin, com.stdnullptr.pokeball.config.PluginConfig cfg) {
        this.plugin = plugin;
        this.cfg = cfg;
        if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
        this.file = new File(plugin.getDataFolder(), "stasis.yml");
        this.data = YamlConfiguration.loadConfiguration(file);
    }

    public void park(Entity entity, UUID ballId) {
        // Enforce cap if configured
        if (cfg.stasisCapEnabled()) {
            int total = data.getKeys(false).size();
            if (total >= cfg.stasisCapTotal()) {
                // refuse capture by throwing IllegalStateException; caller will handle refund
                throw new IllegalStateException("Stasis cap reached");
            }
        }
        // Freeze and hide entity at stash; do not strip equipment (simpler, no visual exposure at stash)
        if (entity instanceof LivingEntity le) {
            try { le.setAI(false); } catch (Throwable ignored) {}
            try { le.setCollidable(false); } catch (Throwable ignored) {}
            try { le.setInvisible(true); } catch (Throwable ignored) {}
            try { le.setInvulnerable(true); } catch (Throwable ignored) {}
            try { le.setSilent(true); } catch (Throwable ignored) {}
            try { le.setRemoveWhenFarAway(false); } catch (Throwable ignored) {}
            try { le.setGravity(false); } catch (Throwable ignored) {}

            // Move to stash location to avoid collisions/visibility at capture point
            Location stashLoc = computeStashLocation(le.getWorld());
            if (stashLoc != null) le.teleport(stashLoc);

            // Persist mapping (after teleport)
            String key = ballId.toString();
            var loc = le.getLocation();
            data.set(key + ".world", loc.getWorld().getName());
            data.set(key + ".uuid", le.getUniqueId().toString());
            data.set(key + ".chunkX", loc.getChunk().getX());
            data.set(key + ".chunkZ", loc.getChunk().getZ());
            data.set(key + ".type", le.getType().name());
            save();
        }
    }

    public boolean release(UUID ballId, Location target) {
        String key = ballId.toString();
        String worldName = data.getString(key + ".world");
        String uuidStr = data.getString(key + ".uuid");
        Integer cx = (data.contains(key + ".chunkX") ? data.getInt(key + ".chunkX") : null);
        Integer cz = (data.contains(key + ".chunkZ") ? data.getInt(key + ".chunkZ") : null);
        if (worldName == null || uuidStr == null || cx == null || cz == null) return false;
        World world = Bukkit.getWorld(worldName);
        if (world == null) return false;

        // Ensure source chunk is loaded so the entity becomes accessible
        Chunk ch = world.getChunkAt(cx, cz);
        if (!ch.isLoaded()) ch.load();

        UUID uuid = UUID.fromString(uuidStr);
        Entity e = Bukkit.getEntity(uuid);
        if (e == null) return false;

        // Teleport while invisible, then unfreeze a tick later
        if (e instanceof LivingEntity le) {
            le.teleport(target);
            try { le.setVelocity(new org.bukkit.util.Vector(0, 0, 0)); } catch (Throwable ignored) {}
            try { le.setFallDistance(0f); } catch (Throwable ignored) {}
            // Effects: release
            playReleaseEffects(target);
            // Re-equip after one tick to avoid any visual snap
            new org.bukkit.scheduler.BukkitRunnable() {
                @Override public void run() {
                    try { le.setAI(true); } catch (Throwable ignored) {}
                    try { le.setCollidable(true); } catch (Throwable ignored) {}
                    try { le.setInvisible(false); } catch (Throwable ignored) {}
                    try { le.setInvulnerable(false); } catch (Throwable ignored) {}
                    try { le.setSilent(false); } catch (Throwable ignored) {}
                    try { le.setGravity(true); } catch (Throwable ignored) {}
                    try { le.setGliding(false); } catch (Throwable ignored) {}
                    try { le.setSwimming(false); } catch (Throwable ignored) {}
                    try { le.setVelocity(new org.bukkit.util.Vector(0, 0, 0)); } catch (Throwable ignored) {}
                    try { le.setFallDistance(0f); } catch (Throwable ignored) {}
                }
            }.runTaskLater(plugin, 1L);
        } else {
            e.teleport(target);
        }

        // Remove mapping (one-shot)
        data.set(key, null);
        save();
        return true;
    }

    private Location computeStashLocation(World fallbackWorld) {
        if (!cfg.stasisEnabled()) return null;
        World w = Bukkit.getWorld(cfg.stasisWorld());
        if (w == null) w = fallbackWorld;
        if (w == null) return null;
        return new Location(w, cfg.stasisX(), cfg.stasisY(), cfg.stasisZ());
    }

    public EntityType peekType(UUID ballId) {
        String type = data.getString(ballId.toString() + ".type");
        if (type == null) return null;
        try { return EntityType.valueOf(type); } catch (Exception e) { return null; }
    }

    private void save() {
        try { data.save(file); } catch (IOException e) { plugin.getSLF4JLogger().error("Failed to save stasis.yml", e); }
    }

    // Utilities
    public java.util.Set<String> ids() { return data.getKeys(false); }
    public FileConfiguration data() { return data; }
    public org.bukkit.entity.EntityType typeOf(UUID id) {
        String s = data.getString(id.toString() + ".type");
        if (s == null) return null; try { return org.bukkit.entity.EntityType.valueOf(s); } catch (Exception e) { return null; }
    }
    public org.bukkit.Location locationOf(UUID id) {
        String base = id.toString();
        String worldName = data.getString(base + ".world");
        Integer cx = (data.contains(base + ".chunkX") ? data.getInt(base + ".chunkX") : null);
        Integer cz = (data.contains(base + ".chunkZ") ? data.getInt(base + ".chunkZ") : null);
        if (worldName == null || cx == null || cz == null) return null;
        World w = Bukkit.getWorld(worldName); if (w == null) return null;
        int x = cx << 4; int z = cz << 4;
        return new Location(w, x + 8, w.getHighestBlockYAt(x + 8, z + 8) + 1, z + 8);
    }
    public boolean remove(UUID id) { data.set(id.toString(), null); save(); return true; }

    public void playCaptureEffects(Location at) {
        playEffects(at, cfg.captureEffect(), true);
    }

    public void playReleaseEffects(Location at) {
        playEffects(at, cfg.releaseEffect(), false);
    }

    private void playEffects(Location at, com.stdnullptr.pokeball.config.PluginConfig.EffectSpec spec, boolean capture) {
        if (spec == null) return;
        if (spec.particles) {
            if ("FANCY".equalsIgnoreCase(spec.particle)) {
                try {
                    // Ring of red dust around the point
                    var world = at.getWorld();
                    var dust = new org.bukkit.Particle.DustOptions(org.bukkit.Color.fromRGB(220, 40, 40), 1.3f);
                    int points = Math.max(12, spec.particleCount);
                    double radius = 0.7;
                    for (int i = 0; i < points; i++) {
                        double angle = (2 * Math.PI * i) / points;
                        double x = at.getX() + Math.cos(angle) * radius;
                        double z = at.getZ() + Math.sin(angle) * radius;
                        world.spawnParticle(org.bukkit.Particle.DUST, new Location(world, x, at.getY() + 0.2, z), 1, dust);
                    }
                    // Sparkle burst
                    world.spawnParticle(org.bukkit.Particle.CRIT, at, Math.max(10, spec.particleCount / 2), 0.2, 0.2, 0.2, 0.02);
                    // Subtle swirl
                    world.spawnParticle(org.bukkit.Particle.END_ROD, at.clone().add(0, 0.5, 0), 40, 0.6, 0.6, 0.6, 0.0);
                } catch (Exception ignored) {}
            } else {
                try {
                    var p = org.bukkit.Particle.valueOf(spec.particle.toUpperCase());
                    at.getWorld().spawnParticle(p, at, Math.max(1, spec.particleCount));
                } catch (Exception ignored) {}
            }
        }
        if (spec.sound != null && !spec.sound.isEmpty()) {
            try {
                var s = org.bukkit.Sound.valueOf(spec.sound.toUpperCase());
                at.getWorld().playSound(at, s, spec.volume, spec.pitch);
            } catch (Exception ignored) {}
        }
    }
}
