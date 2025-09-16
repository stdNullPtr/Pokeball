package com.stdnullptr.pokeball.listener;

import com.stdnullptr.pokeball.service.StasisService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.UUID;

public final class StasisCleanupListener implements Listener {
    private final StasisService stasis;

    public StasisCleanupListener(final StasisService stasis) {
        this.stasis = stasis;
    }

    @EventHandler
    public void onDeath(final EntityDeathEvent event) {
        final UUID uuid = event
                .getEntity()
                .getUniqueId();
        // If a stasis entity somehow dies, remove stale mapping
        for (final String id : stasis.ids()) {
            try {
                final var mapped = UUID.fromString(stasis
                        .data()
                        .getString(id + ".uuid", ""));
                if (uuid.equals(mapped)) {
                    stasis.remove(UUID.fromString(id));
                    break;
                }
            } catch (final Exception ignored) {
            }
        }
    }
}

