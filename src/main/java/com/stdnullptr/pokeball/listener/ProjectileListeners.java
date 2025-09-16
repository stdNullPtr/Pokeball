package com.stdnullptr.pokeball.listener;

import com.stdnullptr.pokeball.Pokeball;
import com.stdnullptr.pokeball.config.PluginConfig;
import com.stdnullptr.pokeball.item.PokeballItemFactory;
import com.stdnullptr.pokeball.util.Keys;
import com.stdnullptr.pokeball.service.StasisService;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

public final class ProjectileListeners implements Listener {
    private final Pokeball plugin;
    private final PokeballItemFactory items;
    private final PluginConfig cfg;
    private final StasisService stasis;

    public ProjectileListeners(Pokeball plugin, PokeballItemFactory items, PluginConfig cfg, StasisService stasis) {
        this.plugin = plugin;
        this.items = items;
        this.cfg = cfg;
        this.stasis = stasis;
    }

    @EventHandler
    public void onLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity().getShooter() instanceof Player player)) return;
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (!items.isPokeball(hand)) return;
        // If filled, this is a release throw; else capture throw
        if (items.isFilled(hand)) {
            String idStr = hand.getItemMeta().getPersistentDataContainer().get(Keys.BALL_ID, PersistentDataType.STRING);
            if (idStr != null) {
                event.getEntity().getPersistentDataContainer().set(Keys.PROJECTILE_RELEASE_ID, PersistentDataType.STRING, idStr);
            }
        } else {
            // Tag projectile so we know it's a Pokeball capture throw
            event.getEntity().getPersistentDataContainer().set(Keys.PROJECTILE_BALL, PersistentDataType.BYTE, (byte)1);
        }
        var flightCfg = cfg.flight();
        if (flightCfg != null && flightCfg.glow) {
            event.getEntity().setGlowing(true);
        }
        // Flight particle trail (configurable)
        final var proj = event.getEntity();
        new org.bukkit.scheduler.BukkitRunnable() {
            @Override public void run() {
                if (!proj.isValid() || proj.isDead()) { cancel(); return; }
                var loc = proj.getLocation();
                var world = proj.getWorld();
                var flight = cfg.flight();
                if (flight != null && flight.enabled) {
                    if (flight.dust) {
                        world.spawnParticle(org.bukkit.Particle.DUST, loc, Math.max(0, flight.dustCount), 0.0, 0.0, 0.0,
                            new org.bukkit.Particle.DustOptions(org.bukkit.Color.fromRGB(220, 40, 40), Math.max(0.1f, flight.dustSize)));
                    }
                    if (flight.endRod) {
                        org.bukkit.util.Vector v = proj.getVelocity();
                        if (v.lengthSquared() > 1.0E-4) {
                            org.bukkit.util.Vector dir = v.clone().normalize();
                            double step = flight.endRodStep <= 0 ? 0.15 : flight.endRodStep;
                            int streak = Math.max(0, flight.endRodPoints);
                            for (int i = 1; i <= streak; i++) {
                                org.bukkit.Location p = loc.clone().subtract(dir.clone().multiply(step * i));
                                world.spawnParticle(org.bukkit.Particle.END_ROD, p, 1, 0.0, 0.0, 0.0, 0.0);
                            }
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, Math.max(1L, (flightCfg != null ? flightCfg.tickPeriod : 1)));

        // Consume the Pokeball from hand for all gamemodes (avoid creative dupes)
        int amt = hand.getAmount();
        if (amt <= 1) {
            player.getInventory().setItemInMainHand(null);
        } else {
            hand.setAmount(amt - 1);
        }
    }

    @EventHandler
    public void onHit(ProjectileHitEvent event) {
        Projectile proj = event.getEntity();
        var pdc = proj.getPersistentDataContainer();
        String releaseId = pdc.get(Keys.PROJECTILE_RELEASE_ID, PersistentDataType.STRING);
        Byte isCapture = pdc.get(Keys.PROJECTILE_BALL, PersistentDataType.BYTE);

        if (releaseId == null && isCapture == null) return; // Not our projectile

        if (!(proj.getShooter() instanceof Player player)) {
            proj.remove();
            return;
        }
        // Release case
        if (releaseId != null) {
            java.util.UUID ballId = java.util.UUID.fromString(releaseId);
            var mobType = ((com.stdnullptr.pokeball.Pokeball)plugin).stasis().peekType(ballId);
            org.bukkit.Location spawnAt = resolveImpactSpawn(event, proj, mobType);
            // Impact flash
            try { spawnAt.getWorld().spawnParticle(org.bukkit.Particle.CLOUD, spawnAt, 8, 0.2, 0.2, 0.2, 0.0); } catch (Exception ignored) {}
            boolean ok = ((com.stdnullptr.pokeball.Pokeball)plugin).stasis().release(ballId, spawnAt);
            if (!ok) {
                player.sendMessage(msg(cfg.msgPrefix + " <red>Release failed (stored mob not found).</red>"));
            } else {
                // Optionally inform type
                var type = ((com.stdnullptr.pokeball.Pokeball)plugin).stasis().peekType(ballId);
                if (type != null) player.sendMessage(msg(cfg.msgPrefix + " " + cfg.msgReleaseSuccess.replace("<type>", type.name())));
                // Refund empty ball if not configured to consume on release
                if (!cfg.consumeOnRelease()) {
                    player.getInventory().addItem(items.createEmptyBall());
                }
            }
            proj.remove();
            return;
        }

        // Capture case
        if (isCapture != null) {
            Entity hit = event.getHitEntity();
            if (hit != null) {
                // Impact flash
                try { hit.getWorld().spawnParticle(org.bukkit.Particle.CLOUD, hit.getLocation(), 8, 0.2, 0.2, 0.2, 0.0); } catch (Exception ignored) {}
                handleCapture(player, hit);
                proj.remove();
                return;
            }
            // If hit block or missed: return empty ball to player (we consumed on launch)
            var impact = proj.getLocation();
            try { impact.getWorld().spawnParticle(org.bukkit.Particle.CLOUD, impact, 6, 0.2, 0.2, 0.2, 0.0); } catch (Exception ignored) {}
            player.getInventory().addItem(items.createEmptyBall());
            proj.remove();
        }
    }

    private org.bukkit.Location resolveImpactSpawn(org.bukkit.event.entity.ProjectileHitEvent event, org.bukkit.entity.Projectile proj, EntityType type) {
        org.bukkit.block.Block hitBlock = event.getHitBlock();
        if (hitBlock != null) {
            org.bukkit.block.BlockFace face = event.getHitBlockFace();
            if (face == null) face = org.bukkit.block.BlockFace.SELF;
            // Start with the block just outside the face that was hit
            org.bukkit.block.Block candidate = hitBlock.getRelative(face);
            org.bukkit.util.Vector n = new org.bukkit.util.Vector(face.getModX(), face.getModY(), face.getModZ());
            int max = Math.max(1, cfg.releaseProbeSteps());
            double offN = cfg.releaseOffsetNormal();
            double offUp = cfg.releaseOffsetUp();
            for (int i = 0; i < max; i++) {
                if (isSafeSpawn(candidate, type)) {
                    return centerOf(candidate).add(n.clone().multiply(offN)).add(0, offUp, 0);
                }
                candidate = candidate.getRelative(face);
            }
            // Fallback to just outside the block center
            return centerOf(hitBlock.getRelative(face)).add(n.clone().multiply(offN)).add(0, offUp, 0);
        }
        // If we hit an entity or air, use projectile location slightly up
        return proj.getLocation().clone().add(0, 0.1, 0);
    }

    private boolean isSafeSpawn(org.bukkit.block.Block block, EntityType mobType) {
        try {
            // Require head and feet space to be passable (prevents spawning inside walls/ceilings)
            boolean feet = block.isPassable();
            boolean head = block.getRelative(org.bukkit.block.BlockFace.UP).isPassable();
            boolean core = feet && head;
            if (!core) return false;
            // Wider mobs like spiders need extra lateral clearance
            boolean wide = (mobType == EntityType.SPIDER || mobType == EntityType.CAVE_SPIDER);
            if (!wide) return true;
            // Check cardinal neighbors as well for 2x2 passable area
            org.bukkit.block.Block[] neighbors = new org.bukkit.block.Block[] {
                block.getRelative(org.bukkit.block.BlockFace.NORTH),
                block.getRelative(org.bukkit.block.BlockFace.SOUTH),
                block.getRelative(org.bukkit.block.BlockFace.EAST),
                block.getRelative(org.bukkit.block.BlockFace.WEST)
            };
            for (org.bukkit.block.Block nb : neighbors) {
                if (!(nb.isPassable() && nb.getRelative(org.bukkit.block.BlockFace.UP).isPassable())) {
                    return false;
                }
            }
            return true;
        } catch (Throwable t) {
            // Fallback for older API: use non-solid
            var mat = block.getType();
            var matUp = block.getRelative(org.bukkit.block.BlockFace.UP).getType();
            if (mat.isSolid() || matUp.isSolid()) return false;
            // Coarse wide check: ensure at least two side blocks are non-solid
            var n = block.getRelative(org.bukkit.block.BlockFace.NORTH).getType().isSolid();
            var s = block.getRelative(org.bukkit.block.BlockFace.SOUTH).getType().isSolid();
            var e = block.getRelative(org.bukkit.block.BlockFace.EAST).getType().isSolid();
            var w = block.getRelative(org.bukkit.block.BlockFace.WEST).getType().isSolid();
            boolean wideType = (mobType == EntityType.SPIDER || mobType == EntityType.CAVE_SPIDER);
            return !wideType || ((!n && !s) || (!e && !w));
        }
    }

    private org.bukkit.Location centerOf(org.bukkit.block.Block b) {
        return new org.bukkit.Location(b.getWorld(), b.getX() + 0.5, b.getY(), b.getZ() + 0.5);
    }
    

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Projectile proj)) return;
        if (proj.getPersistentDataContainer().get(Keys.PROJECTILE_BALL, PersistentDataType.BYTE) != null) {
            // Prevent damage from Pokeball projectile
            event.setCancelled(true);
        }
    }

    private void handleCapture(Player player, Entity target) {
        if (!player.hasPermission("pokeball.use.capture")) {
            player.sendMessage(msg(cfg.msgPrefix + " " + cfg.msgCaptureFailPermission));
            // Return ball since action not allowed
            player.getInventory().addItem(items.createEmptyBall());
            return;
        }
        if (!worldAllowed(player.getWorld().getName())) {
            player.sendMessage(msg(cfg.msgPrefix + " " + cfg.msgCaptureFailWorld));
            player.getInventory().addItem(items.createEmptyBall());
            return;
        }
        EntityType type = target.getType();

        boolean creativeBypass = false;
        if (cfg.creativeBypassEnabled() && player.getGameMode() == org.bukkit.GameMode.CREATIVE) {
            String perm = cfg.creativeBypassPermission();
            creativeBypass = perm == null || perm.isBlank() || player.hasPermission(perm);
        }

        if (target instanceof org.bukkit.entity.Player) {
            player.sendMessage(msg(cfg.msgPrefix + " " + cfg.msgCaptureFailPlayer));
            player.getInventory().addItem(items.createEmptyBall());
            return;
        }

        if (!creativeBypass) {
            // Allow-list only: if not explicitly allowed, it's blocked
            if (type == null || !cfg.allowedTypes().contains(type)) {
                player.sendMessage(msg(cfg.msgPrefix + " " + cfg.msgCaptureFailBlocked));
                // Return empty ball on failure
                player.getInventory().addItem(items.createEmptyBall());
                return;
            }
        }

        // Success: park target in stasis and give filled ball linked to it
        var filled = items.createEmptyBall();
        var idStr = filled.getItemMeta().getPersistentDataContainer().get(Keys.BALL_ID, org.bukkit.persistence.PersistentDataType.STRING);
        java.util.UUID ballId = java.util.UUID.fromString(idStr);
        try {
            stasis.park(target, ballId);
        } catch (IllegalStateException cap) {
            player.sendMessage(msg(cfg.msgPrefix + " <red>Capture refused: storage is full.</red>"));
            // Refund empty ball
            player.getInventory().addItem(items.createEmptyBall());
            return;
        }
        // Effects: capture
        ((com.stdnullptr.pokeball.Pokeball)plugin).stasis().playCaptureEffects(target.getLocation());
        String annotation = (creativeBypass && cfg.creativeBypassAnnotate()) ? cfg.creativeBypassAnnotation() : null;
        items.markCaptured(filled, type, creativeBypass, annotation);
        player.getInventory().addItem(filled);
        player.sendMessage(msg(cfg.msgPrefix + " " + cfg.msgCaptureSuccess.replace("<type>", type.name())));
    }

    private boolean worldAllowed(String world) {
        return cfg.allowedWorlds().isEmpty() || cfg.allowedWorlds().contains(world);
    }

    private Component msg(String mm) { return plugin.mini().deserialize(mm); }
}
