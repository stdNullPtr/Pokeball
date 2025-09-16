package com.stdnullptr.pokeball.listener;

import com.stdnullptr.pokeball.Pokeball;
import com.stdnullptr.pokeball.service.StasisService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.UUID;

public final class StasisCleanupListener implements Listener {
    private final Pokeball plugin;
    private final StasisService stasis;

    public StasisCleanupListener(Pokeball plugin, StasisService stasis) {
        this.plugin = plugin;
        this.stasis = stasis;
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        UUID uuid = event.getEntity().getUniqueId();
        // If a stasis entity somehow dies, remove stale mapping
        for (String id : stasis.ids()) {
            try {
                var mapped = java.util.UUID.fromString(stasis.data().getString(id + ".uuid", ""));
                if (uuid.equals(mapped)) {
                    stasis.remove(java.util.UUID.fromString(id));
                    break;
                }
            } catch (Exception ignored) {}
        }
    }
}

