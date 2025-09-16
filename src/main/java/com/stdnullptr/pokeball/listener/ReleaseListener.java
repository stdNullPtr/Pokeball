package com.stdnullptr.pokeball.listener;

import com.stdnullptr.pokeball.Pokeball;
import com.stdnullptr.pokeball.config.PluginConfig;
import com.stdnullptr.pokeball.item.PokeballItemFactory;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public final class ReleaseListener implements Listener {
    private final Pokeball plugin;
    private final PokeballItemFactory items;
    private final PluginConfig cfg;

    public ReleaseListener(Pokeball plugin, PokeballItemFactory items, PluginConfig cfg) {
        this.plugin = plugin;
        this.items = items;
        this.cfg = cfg;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        // Only main hand to avoid double-fire
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (!items.isPokeball(hand) || !items.isFilled(hand)) return; // need filled ball

        if (!player.hasPermission("pokeball.use.release")) {
            player.sendMessage(msg(cfg.msgPrefix + " " + cfg.msgReleaseFailPermission));
            return;
        }
        if (!worldAllowed(player.getWorld().getName())) {
            player.sendMessage(msg(cfg.msgPrefix + " " + cfg.msgReleaseFailWorld));
            return;
        }

        EntityType type = items.getCapturedType(hand);
        if (type == null) {
            player.sendMessage(msg(cfg.msgPrefix + " " + cfg.msgReleaseFailEmpty));
            return;
        }

        Location spawnAt = player.getLocation().add(player.getLocation().getDirection().normalize());
        var spawned = player.getWorld().spawnEntity(spawnAt, type);
        // Apply basic attributes we stored
        try {
            var meta = hand.getItemMeta();
            var pdc = meta.getPersistentDataContainer();
            if (spawned instanceof org.bukkit.entity.Ageable ageable) {
                Byte baby = pdc.get(com.stdnullptr.pokeball.util.Keys.CAPTURED_IS_BABY, org.bukkit.persistence.PersistentDataType.BYTE);
                if (baby != null) {
                    if (baby == 1) ageable.setBaby(); else ageable.setAdult();
                }
            }
            if (spawned instanceof org.bukkit.entity.Sheep sheep) {
                String color = pdc.get(com.stdnullptr.pokeball.util.Keys.CAPTURED_VARIANT, org.bukkit.persistence.PersistentDataType.STRING);
                if (color != null) sheep.setColor(org.bukkit.DyeColor.valueOf(color));
            }
        } catch (Throwable ignored) {}

        if (cfg.consumeOnRelease()) {
            // consume one item
            int amt = hand.getAmount();
            if (amt <= 1) {
                player.getInventory().setItemInMainHand(null);
            } else {
                hand.setAmount(amt - 1);
            }
        } else {
            // revert to empty state, preserving non-stackable UUID
            items.clearCaptured(hand);
        }

        player.sendMessage(msg(cfg.msgPrefix + " " + cfg.msgReleaseSuccess.replace("<type>", type.name())));
        event.setCancelled(true);
    }

    private boolean worldAllowed(String world) {
        return cfg.allowedWorlds().isEmpty() || cfg.allowedWorlds().contains(world);
    }

    private Component msg(String mm) { return plugin.mini().deserialize(mm); }
}
