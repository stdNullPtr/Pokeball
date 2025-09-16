package com.stdnullptr.pokeball.item;

import com.stdnullptr.pokeball.Pokeball;
import com.stdnullptr.pokeball.config.PluginConfig;
import com.stdnullptr.pokeball.util.Keys;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.UUID;

public final class PokeballItemFactory {
    private final Pokeball plugin;
    private final PluginConfig cfg;
    private final MiniMessage mini;

    public PokeballItemFactory(Pokeball plugin, PluginConfig cfg) {
        this.plugin = plugin;
        this.cfg = cfg;
        this.mini = plugin.mini();
    }

    public ItemStack createEmptyBall() {
        ItemStack item = new ItemStack(Material.SNOWBALL, 1); // placeholder item
        ItemMeta meta = item.getItemMeta();
        applyCommonMeta(meta, cfg.itemName(), cfg.itemLore(), null);
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        // Unique ID ensures non-stacking
        pdc.set(Keys.BALL_ID, PersistentDataType.STRING, UUID.randomUUID().toString());
        // Ensure empty state (no captured type)
        pdc.remove(Keys.CAPTURED_TYPE);
        pdc.set(Keys.CAPTURED_DATA_VERSION, PersistentDataType.INTEGER, 1);
        item.setItemMeta(meta);
        return item;
    }

    public boolean isPokeball(ItemStack stack) {
        if (stack == null) return false;
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return false;
        return meta.getPersistentDataContainer().has(Keys.BALL_ID, PersistentDataType.STRING);
    }

    public boolean isFilled(ItemStack stack) {
        if (!isPokeball(stack)) return false;
        ItemMeta meta = stack.getItemMeta();
        return meta.getPersistentDataContainer().has(Keys.CAPTURED_TYPE, PersistentDataType.STRING);
    }

    public EntityType getCapturedType(ItemStack stack) {
        if (!isFilled(stack)) return null;
        String name = stack.getItemMeta().getPersistentDataContainer()
            .get(Keys.CAPTURED_TYPE, PersistentDataType.STRING);
        if (name == null) return null;
        try { return EntityType.valueOf(name); } catch (Exception e) { return null; }
    }

    public void markCaptured(ItemStack stack, EntityType type) {
        markCaptured(stack, type, false, null);
    }

    public void markCaptured(ItemStack stack, EntityType type, boolean bypass, String bypassAnnotation) {
        ItemMeta meta = stack.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(Keys.CAPTURED_TYPE, PersistentDataType.STRING, type.name());
        // Update lore to show captured type
        applyCommonMeta(meta, cfg.itemName(), cfg.itemLore(), type, (bypass && bypassAnnotation != null) ? bypassAnnotation : null);
        stack.setItemMeta(meta);
    }

    public void markCapturedFromEntity(ItemStack stack, Entity entity, boolean bypass, String bypassAnnotation) {
        ItemMeta meta = stack.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        EntityType type = entity.getType();
        pdc.set(Keys.CAPTURED_TYPE, PersistentDataType.STRING, type.name());
        // Basic attributes
        if (entity instanceof Ageable ageable) {
            pdc.set(Keys.CAPTURED_IS_BABY, PersistentDataType.BYTE, (byte)(ageable.isAdult() ? 0 : 1));
        }
        if (entity instanceof Sheep sheep) {
            pdc.set(Keys.CAPTURED_VARIANT, PersistentDataType.STRING, sheep.getColor().name());
        }
        applyCommonMeta(meta, cfg.itemName(), cfg.itemLore(), type, (bypass && bypassAnnotation != null) ? bypassAnnotation : null);
        stack.setItemMeta(meta);
    }

    public void clearCaptured(ItemStack stack) {
        ItemMeta meta = stack.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.remove(Keys.CAPTURED_TYPE);
        pdc.remove(Keys.CAPTURED_IS_BABY);
        pdc.remove(Keys.CAPTURED_VARIANT);
        applyCommonMeta(meta, cfg.itemName(), cfg.itemLore(), null, null);
        stack.setItemMeta(meta);
    }

    private void applyCommonMeta(ItemMeta meta, String name, List<String> lore, EntityType captured) {
        applyCommonMeta(meta, name, lore, captured, null);
    }

    private void applyCommonMeta(ItemMeta meta, String name, List<String> lore, EntityType captured, String extraAnnotation) {
        // Keep base display name unchanged; show state only in lore
        Component display = mini.deserialize(name);
        meta.displayName(display);
        java.util.ArrayList<Component> loreLines = new java.util.ArrayList<>();
        if (lore != null && !lore.isEmpty()) loreLines.addAll(lore.stream().map(mini::deserialize).toList());
        loreLines.add(mini.deserialize(captured == null ? "<gray>Contents: <red>Empty</red></gray>" : "<gray>Status: <green>Filled</green> (<yellow>" + captured.name() + "</yellow>)</gray>"));
        if (extraAnnotation != null && !extraAnnotation.isEmpty()) {
            loreLines.add(mini.deserialize(extraAnnotation));
        }
        meta.lore(loreLines);
        if (cfg.customModelData() != null) {
            meta.setCustomModelData(cfg.customModelData());
        }
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        // Add enchant glint and hide enchants list
        try {
            meta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 1, true);
        } catch (Throwable ignored) {}
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
    }
}
