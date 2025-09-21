package com.stdnullptr.pokeball.listener;

import com.stdnullptr.pokeball.Pokeball;
import com.stdnullptr.pokeball.config.ConfigManager;
import com.stdnullptr.pokeball.config.models.RefundMode;
import com.stdnullptr.pokeball.item.PokeballItemFactory;
import com.stdnullptr.pokeball.service.StasisService;
import com.stdnullptr.pokeball.util.Keys;
import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
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
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.Objects;
import java.util.UUID;

public final class ProjectileListeners implements Listener {
    private final Pokeball plugin;
    private final PokeballItemFactory items;

    private final ConfigManager cfg;
    private final StasisService stasis;

    private final Keys keys;

    public ProjectileListeners(
            final Pokeball plugin,
            final PokeballItemFactory items,
            final ConfigManager cfg,
            final StasisService stasis,
            final Keys keys
    ) {
        this.plugin = plugin;
        this.items = items;
        this.cfg = cfg;
        this.stasis = stasis;
        this.keys = keys;
    }

    @EventHandler
    public void onLaunch(final ProjectileLaunchEvent event) {
        if (!(event
                .getEntity()
                .getShooter() instanceof final Player player)) {
            return;
        }
        final ItemStack hand = player
                .getInventory()
                .getItemInMainHand();
        if (items.isNotPokeball(hand)) {
            return;
        }
        // If filled, this is a release throw; else capture throw
        if (items.isFilled(hand)) {
            final String idStr = hand
                    .getItemMeta()
                    .getPersistentDataContainer()
                    .get(keys.getBallId(), PersistentDataType.STRING);
            if (idStr != null) {
                event
                        .getEntity()
                        .getPersistentDataContainer()
                        .set(keys.getProjectileReleaseId(), PersistentDataType.STRING, idStr);
            }
        } else {
            // Tag projectile so we know it's a Pokeball capture throw
            event
                    .getEntity()
                    .getPersistentDataContainer()
                    .set(keys.getProjectileBall(), PersistentDataType.BYTE, (byte) 1);
        }
        final var flightCfg = cfg
                .effects()
                .flight();
        if (flightCfg != null && flightCfg.glow()) {
            event.getEntity().setGlowing(true);
        }
        // Flight particle trail (configurable)
        final var proj = event.getEntity();
        new BukkitRunnable() {
            @Override public void run() {
                if (!proj.isValid() || proj.isDead()) { cancel(); return; }
                final var loc = proj.getLocation();
                final var world = proj.getWorld();
                final var flight = cfg
                        .effects()
                        .flight();
                if (flight != null && flight.enabled()) {
                    if (flight.dust()) {
                        world.spawnParticle(
                                Particle.DUST,
                                loc,
                                Math.max(0, flight.dustCount()),
                                0.0,
                                0.0,
                                0.0,
                                new Particle.DustOptions(Color.fromRGB(220, 40, 40), Math.max(0.1f, flight.dustSize()))
                        );
                    }
                    if (flight.endRod()) {
                        final Vector v = proj.getVelocity();
                        if (v.lengthSquared() > 1.0E-4) {
                            final Vector dir = v
                                    .clone()
                                    .normalize();
                            final double step = flight.endRodStep() <= 0 ? 0.15 : flight.endRodStep();
                            final int streak = Math.max(0, flight.endRodPoints());
                            for (int i = 1; i <= streak; i++) {
                                final Location p = loc
                                        .clone()
                                        .subtract(dir
                                                .clone()
                                                .multiply(step * i));
                                world.spawnParticle(Particle.END_ROD, p, 1, 0.0, 0.0, 0.0, 0.0);
                            }
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, Math.max(1L, (flightCfg != null ? flightCfg.tickPeriod() : 1)));

        // Consume the Pokeball from hand for all gamemodes (avoid creative dupes)
        final int amt = hand.getAmount();
        if (amt <= 1) {
            player.getInventory().setItemInMainHand(null);
        } else {
            hand.setAmount(amt - 1);
        }
    }

    @EventHandler
    public void onHit(final ProjectileHitEvent event) {
        final Projectile proj = event.getEntity();
        final var pdc = proj.getPersistentDataContainer();
        final String releaseId = pdc.get(keys.getProjectileReleaseId(), PersistentDataType.STRING);
        final Byte isCapture = pdc.get(keys.getProjectileBall(), PersistentDataType.BYTE);

        if (releaseId == null && isCapture == null) return; // Not our projectile

        if (!(proj.getShooter() instanceof final Player player)) {
            proj.remove();
            return;
        }
        // Release case
        if (releaseId != null) {
            final UUID ballId = UUID.fromString(releaseId);
            final var mobType = plugin
                    .stasis()
                    .peekType(ballId);
            final Location spawnAt = resolveImpactSpawn(event, proj, mobType);
            if (!worldAllowed(spawnAt.getWorld().getName())) {
                final String releaseWorldMsg = cfg.messages().getReleaseFailWorld();
                player.sendMessage(msg(cfg.messages().getPrefix() + " " +
                                               (releaseWorldMsg != null ? releaseWorldMsg : "<red>Releasing is not allowed in this world.</red>")));
                // Refund the filled ball linked to this stasis entry
                if (mobType != null) {
                    giveOrDrop(player, items.createFilledBall(ballId, mobType, false, null), spawnAt);
                } else {
                    // Fallback: return empty ball if type cannot be determined
                    giveOrDrop(player, items.createEmptyBall(), spawnAt);
                }
                proj.remove();
                return;
            }
            // Impact flash
            try {
                spawnAt
                        .getWorld()
                        .spawnParticle(Particle.CLOUD, spawnAt, 8, 0.2, 0.2, 0.2, 0.0);
            } catch (final Exception ignored) {
            }
            final boolean ok = plugin
                    .stasis()
                    .release(ballId, spawnAt);
            if (!ok) {
                player.sendMessage(msg(cfg
                                               .messages()
                                               .getPrefix() + " <red>Release failed (stored mob not found).</red>"));
            } else {
                // Optionally inform type
                final var type = plugin
                        .stasis()
                        .peekType(ballId);
                if (type != null) {
                    final String releaseMessage = cfg.messages().getReleaseSuccess();
                    player.sendMessage(msg(cfg.messages().getPrefix() + " " +
                                                   (releaseMessage != null ? releaseMessage : "<green>Released a <yellow><type></yellow>.")
                                                           .replace("<type>", type.name())));
                }
                // Refund empty ball if not configured to consume on release
                if (!cfg
                        .capture()
                        .consumeOnRelease()) {
                    giveOrDrop(player, items.createEmptyBall(), spawnAt);
                }
            }
            proj.remove();
            return;
        }

        final Entity hit = event.getHitEntity();
        if (hit != null) {
            // Impact flash
            try {
                hit
                        .getWorld()
                        .spawnParticle(Particle.CLOUD, hit.getLocation(), 8, 0.2, 0.2, 0.2, 0.0);
            } catch (final Exception ignored) {
            }
            handleCapture(player, hit, hit.getLocation());
            proj.remove();
            return;
        }
        // If hit block or missed: return empty ball to player (we consumed on launch)
        final var impact = proj.getLocation();
        try {
            impact
                    .getWorld()
                    .spawnParticle(Particle.CLOUD, impact, 6, 0.2, 0.2, 0.2, 0.0);
        } catch (final Exception ignored) {
        }
        giveOrDrop(player, items.createEmptyBall(), impact);
        proj.remove();
    }

    private Location resolveImpactSpawn(final ProjectileHitEvent event, final Projectile proj, final EntityType type) {
        final Block hitBlock = event.getHitBlock();
        if (hitBlock != null) {
            BlockFace face = event.getHitBlockFace();
            if (face == null) {
                face = BlockFace.SELF;
            }
            // Start with the block just outside the face that was hit
            Block candidate = hitBlock.getRelative(face);
            final Vector n = new Vector(face.getModX(), face.getModY(), face.getModZ());
            final int max = Math.max(
                    1,
                    cfg
                            .effects()
                            .releaseProbeSteps()
            );
            final double offN = cfg
                    .effects()
                    .releaseOffsetNormal();
            final double offUp = cfg
                    .effects()
                    .releaseOffsetUp();
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

    private boolean isSafeSpawn(final Block block, final EntityType mobType) {
        try {
            // Require head and feet space to be passable (prevents spawning inside walls/ceilings)
            final boolean feet = block.isPassable();
            final boolean head = block
                    .getRelative(BlockFace.UP)
                    .isPassable();
            final boolean core = feet && head;
            if (!core) return false;
            // Wider mobs like spiders need extra lateral clearance
            final boolean wide = (mobType == EntityType.SPIDER || mobType == EntityType.CAVE_SPIDER);
            if (!wide) return true;
            // Check cardinal neighbors for 2x2 passable area
            final Block[] neighbors = {block.getRelative(BlockFace.NORTH), block.getRelative(BlockFace.SOUTH), block.getRelative(
                    BlockFace.EAST), block.getRelative(BlockFace.WEST)
            };
            for (final Block nb : neighbors) {
                if (!(nb.isPassable() && nb
                        .getRelative(BlockFace.UP)
                        .isPassable())) {
                    return false;
                }
            }
            return true;
        } catch (final Exception t) {
            // Fallback for older API: use non-solid
            final var mat = block.getType();
            final var matUp = block
                    .getRelative(BlockFace.UP)
                    .getType();
            if (mat.isSolid() || matUp.isSolid()) return false;
            // Coarse wide check: ensure at least two side blocks are non-solid
            final var n = block
                    .getRelative(BlockFace.NORTH)
                    .getType()
                    .isSolid();
            final var s = block
                    .getRelative(BlockFace.SOUTH)
                    .getType()
                    .isSolid();
            final var e = block
                    .getRelative(BlockFace.EAST)
                    .getType()
                    .isSolid();
            final var w = block
                    .getRelative(BlockFace.WEST)
                    .getType()
                    .isSolid();
            final boolean wideType = (mobType == EntityType.SPIDER || mobType == EntityType.CAVE_SPIDER);
            return !wideType || ((!n && !s) || (!e && !w));
        }
    }

    private Location centerOf(final Block b) {
        return new Location(b.getWorld(), b.getX() + 0.5, b.getY(), b.getZ() + 0.5);
    }


    @EventHandler
    public void onDamage(final EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof final Projectile proj)) {
            return;
        }
        if (proj
                .getPersistentDataContainer()
                .get(keys.getProjectileBall(), PersistentDataType.BYTE) != null) {
            // Prevent damage from Pokeball projectile
            event.setCancelled(true);
        }
    }

    private void handleCapture(final Player player, final Entity target, final Location dropAt) {
        if (!worldAllowed(player.getWorld().getName())) {
            player.sendMessage(msg(cfg
                                           .messages()
                                           .getPrefix() + " " + cfg
                    .messages()
                    .getCaptureFailWorld()));
            giveOrDrop(player, items.createEmptyBall(), dropAt);
            return;
        }
        final EntityType type = target.getType();

        boolean specialCapture = false;
        boolean usedSpecialPermission = false;
        final String perm = cfg
                .capture()
                .specialCapturePermission();
        if (perm != null && !perm.isBlank()) {
            specialCapture = player.hasPermission(perm);
        }

        if (target instanceof Player) {
            player.sendMessage(msg(cfg
                                           .messages()
                                           .getPrefix() + " " + cfg
                    .messages()
                    .getCaptureFailPlayer()));
            giveOrDrop(player, items.createEmptyBall(), dropAt);
            return;
        }

        if (!cfg.capture().allowedTypes().contains(type)) {
            if (!specialCapture) {
                // No permission and not allowed - fail
                player.sendMessage(msg(cfg.messages().getPrefix() + " " + cfg.messages().getCaptureFailBlocked()));
                // Return empty ball on failure
                giveOrDrop(player, items.createEmptyBall(), dropAt);
                return;
            }
            // Has permission and mob not in allowed list - mark that we used special permission
            usedSpecialPermission = true;
        }


        // Success: park target in stasis and give filled ball linked to it
        final var filled = items.createEmptyBall();
        final var idStr = filled
                .getItemMeta()
                .getPersistentDataContainer()
                .get(keys.getBallId(), PersistentDataType.STRING);
        final UUID ballId = UUID.fromString(Objects.requireNonNull(idStr));
        try {
            stasis.park(target, ballId);
        } catch (final IllegalStateException cap) {
            player.sendMessage(msg(cfg
                                           .messages()
                                           .getPrefix() + " <red>Capture refused: storage is full.</red>"));
            // Refund empty ball
            giveOrDrop(player, items.createEmptyBall(), dropAt);
            return;
        }
        // Effects: capture
        plugin
                .stasis()
                .playCaptureEffects(target.getLocation());
        final String annotation = (usedSpecialPermission && cfg
                .capture()
                .specialCaptureAnnotate()) ? cfg
                .capture()
                .specialCaptureAnnotation() : null;
        items.markCaptured(filled, type, usedSpecialPermission, annotation);
        giveOrDrop(player, filled, dropAt);
        final String captureMessage = cfg.messages().getCaptureSuccess();
        player.sendMessage(msg(cfg.messages().getPrefix() + " " +
                                       (captureMessage != null ? captureMessage : "<green>Captured a <yellow><type></yellow>!")
                                               .replace("<type>", type.name())));
    }

    private void giveOrDrop(final Player player, final ItemStack stack, final Location dropAt) {
        final var world = dropAt.getWorld();
        if (cfg
                .effects()
                .refundMode() == RefundMode.DROP) {
            if (world != null) {
                try {
                    world.dropItemNaturally(dropAt, stack);
                } catch (final Exception ignored) {
                }
            }
            return;
        }
        final var leftovers = player
                .getInventory()
                .addItem(stack);
        if (leftovers.isEmpty()) {
            return;
        }
        if (world == null) {
            return;
        }
        for (final ItemStack remain : leftovers.values()) {
            if (remain == null || remain.getAmount() <= 0) {
                continue;
            }
            try {
                world.dropItemNaturally(dropAt, remain);
            } catch (final Exception ignored) {
            }
        }
    }

    private boolean worldAllowed(final String world) {
        return cfg
                .stasis()
                .allowedWorlds()
                .isEmpty() || cfg
                .stasis()
                .allowedWorlds()
                .contains(world);
    }

    private Component msg(final String mm) {
        return plugin
                .mini()
                .deserialize(mm);
    }
}
