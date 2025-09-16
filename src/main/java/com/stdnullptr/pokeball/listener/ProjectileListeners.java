package com.stdnullptr.pokeball.listener;

import com.stdnullptr.pokeball.Pokeball;
import com.stdnullptr.pokeball.config.PluginConfig;
import com.stdnullptr.pokeball.item.PokeballItemFactory;
import com.stdnullptr.pokeball.util.Keys;
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

    public ProjectileListeners(Pokeball plugin, PokeballItemFactory items, PluginConfig cfg) {
        this.plugin = plugin;
        this.items = items;
        this.cfg = cfg;
    }

    @EventHandler
    public void onLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity().getShooter() instanceof Player player)) return;
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (!items.isPokeball(hand)) return;
        // Prevent throwing filled balls; use right-click release
        if (items.isFilled(hand)) {
            event.setCancelled(true);
            player.sendMessage(msg(cfg.msgPrefix + " <red>Throwing a filled Pokeball is disabled. Right-click to release.</red>"));
            return;
        }
        // Tag projectile so we know it's a Pokeball throw
        event.getEntity().getPersistentDataContainer().set(Keys.PROJECTILE_BALL, PersistentDataType.BYTE, (byte)1);
        // Make projectile glow for visibility
        event.getEntity().setGlowing(true);
        // Subtle red particle trail
        final var proj = event.getEntity();
        new org.bukkit.scheduler.BukkitRunnable() {
            @Override public void run() {
                if (!proj.isValid() || proj.isDead()) { cancel(); return; }
                var loc = proj.getLocation();
                var world = proj.getWorld();
                world.spawnParticle(org.bukkit.Particle.DUST, loc, 1, 0.0, 0.0, 0.0,
                    new org.bukkit.Particle.DustOptions(org.bukkit.Color.fromRGB(220, 40, 40), 1.2f));
            }
        }.runTaskTimer(plugin, 0L, 1L);

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
        if (proj.getPersistentDataContainer().get(Keys.PROJECTILE_BALL, PersistentDataType.BYTE) == null) return;

        if (!(proj.getShooter() instanceof Player player)) {
            proj.remove();
            return;
        }
        // If hit entity: attempt capture
        Entity hit = event.getHitEntity();
        if (hit != null) {
            handleCapture(player, hit);
            proj.remove();
            return;
        }
        // If hit block or missed: return empty ball to player (we consumed on launch)
        player.getInventory().addItem(items.createEmptyBall());
        proj.remove();
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
            player.sendMessage(msg(cfg.msgPrefix + " " + cfg.msgCaptureFailBlocked));
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

        // Success: remove target, give filled ball
        target.remove();
        var filled = items.createEmptyBall();
        String annotation = (creativeBypass && cfg.creativeBypassAnnotate()) ? cfg.creativeBypassAnnotation() : null;
        items.markCapturedFromEntity(filled, target, creativeBypass, annotation);
        player.getInventory().addItem(filled);
        player.sendMessage(msg(cfg.msgPrefix + " " + cfg.msgCaptureSuccess.replace("<type>", type.name())));
    }

    private boolean worldAllowed(String world) {
        return cfg.allowedWorlds().isEmpty() || cfg.allowedWorlds().contains(world);
    }

    private Component msg(String mm) { return plugin.mini().deserialize(mm); }
}
