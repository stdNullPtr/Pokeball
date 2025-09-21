package com.stdnullptr.pokeball.item;

import com.stdnullptr.pokeball.Pokeball;
import com.stdnullptr.pokeball.config.ConfigManager;
import com.stdnullptr.pokeball.util.Keys;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.CustomModelDataComponent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class PokeballItemFactory {
    private final ConfigManager cfg;
    private final MiniMessage mini;

    private final Keys keys;

    public PokeballItemFactory(final Pokeball plugin, final ConfigManager cfg, final Keys keys) {
        this.cfg = cfg;
        this.mini = plugin.mini();
        this.keys = keys;
    }

    public ItemStack createEmptyBall() {
        final ItemStack item = new ItemStack(Material.SNOWBALL, 1);
        final ItemMeta meta = item.getItemMeta();
        applyCommonMeta(
                meta,
                cfg
                        .items()
                        .name(),
                cfg
                        .items()
                        .lore()
        );
        final PersistentDataContainer pdc = meta.getPersistentDataContainer();
        // Unique ID ensures non-stacking
        pdc.set(
                keys.getBallId(),
                PersistentDataType.STRING,
                UUID
                        .randomUUID()
                        .toString()
        );
        // Ensure empty state (no captured type)
        pdc.remove(keys.getCapturedType());
        pdc.set(keys.getCapturedDataVersion(), PersistentDataType.INTEGER, 1);
        item.setItemMeta(meta);
        return item;
    }

    public boolean isNotPokeball(final ItemStack stack) {
        if (stack == null) {
            return true;
        }
        final ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return true;
        }
        return !meta
                .getPersistentDataContainer()
                .has(keys.getBallId(), PersistentDataType.STRING);
    }

    public boolean isFilled(final ItemStack stack) {
        if (isNotPokeball(stack)) {
            return false;
        }
        final ItemMeta meta = stack.getItemMeta();
        return meta
                .getPersistentDataContainer()
                .has(keys.getCapturedType(), PersistentDataType.STRING);
    }

    public void markCaptured(
            final ItemStack stack,
            final EntityType type,
            final boolean bypass,
            final String bypassAnnotation
    ) {
        final ItemMeta meta = stack.getItemMeta();
        final PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(keys.getCapturedType(), PersistentDataType.STRING, type.name());
        // Update lore to show captured type
        applyCommonMeta(
                meta,
                cfg
                        .items()
                        .name(),
                cfg
                        .items()
                        .lore(),
                type,
                (bypass && bypassAnnotation != null) ? bypassAnnotation : null
        );
        stack.setItemMeta(meta);
    }

    /**
     * Creates a filled Pokeball item bound to an existing ball id and captured type.
     * Used to refund the original filled ball on release failure without changing linkage.
     */
    public ItemStack createFilledBall(final UUID ballId, final EntityType type, final boolean bypass, final String bypassAnnotation) {
        final ItemStack item = new ItemStack(Material.SNOWBALL, 1);
        final ItemMeta meta = item.getItemMeta();
        applyCommonMeta(meta,
                        cfg.items().name(),
                        cfg.items().lore(),
                        type,
                        (bypass && bypassAnnotation != null) ? bypassAnnotation : null
        );
        final PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(keys.getBallId(), PersistentDataType.STRING, ballId.toString());
        pdc.set(keys.getCapturedType(), PersistentDataType.STRING, type.name());
        pdc.set(keys.getCapturedDataVersion(), PersistentDataType.INTEGER, 1);
        item.setItemMeta(meta);
        return item;
    }

    private void applyCommonMeta(final ItemMeta meta, final String name, final List<String> lore) {
        applyCommonMeta(meta, name, lore, null, null);
    }

    private void applyCommonMeta(
            final ItemMeta meta,
            final String name,
            final List<String> lore,
            final EntityType captured,
            final String extraAnnotation
    ) {
        // Keep base display name unchanged; show state only in lore
        final Component display = mini.deserialize(name);
        meta.displayName(display);
        final ArrayList<Component> loreLines = new ArrayList<>();
        if (lore != null && !lore.isEmpty()) loreLines.addAll(lore.stream().map(mini::deserialize).toList());
        loreLines.add(mini.deserialize(captured == null ? "<gray>Contents: <red>Empty</red></gray>" : "<gray>Contents: <green>" + captured.name() + "</green></gray>"));
        if (extraAnnotation != null && !extraAnnotation.isEmpty()) {
            loreLines.add(mini.deserialize(extraAnnotation));
        }
        meta.lore(loreLines);
        if (cfg
                .items()
                .customModelData() != null) {
            try {
                final CustomModelDataComponent cmd = meta.getCustomModelDataComponent();
                cmd.setFloats(List.of(cfg
                                              .items()
                        .customModelData()
                        .floatValue()));
                meta.setCustomModelDataComponent(cmd);
            } catch (final NoSuchMethodError | NoClassDefFoundError ignored) {
                // Fallback for older API
                meta.setCustomModelData(cfg
                                                .items()
                                                .customModelData());
            }
        }
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        // Add enchant glint and hide enchants list
        try {
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        } catch (final Exception ignored) {
        }
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
    }
}
