package com.stdnullptr.pokeball.command.executor;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.stdnullptr.pokeball.Pokeball;
import com.stdnullptr.pokeball.config.ConfigManager;
import com.stdnullptr.pokeball.config.models.RefundMode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Executes admin subcommands for managing stasis entries and plugin configuration
 */
public final class AdminCommandExecutor {

    private static final String ALLOWED_ENTITY_TYPES_PATH = "capture.allowed-entity-types";

    private final Pokeball plugin;

    private final ConfigManager config;

    public AdminCommandExecutor(final Pokeball plugin, final ConfigManager config) {
        this.plugin = plugin;
        this.config = config;
    }

    /**
     * Shows admin command usage
     */
    public int showUsage(final CommandContext<CommandSourceStack> ctx) {
        ctx.getSource()
           .getSender()
           .sendMessage(msg("<yellow>Usage:</yellow> /pokeball admin <list|tp|clean|cap|refund|capture>"));

        return Command.SINGLE_SUCCESS;
    }

    // LIST COMMAND

    /**
     * Lists all stasis entries with their types
     */
    public int list(final CommandContext<CommandSourceStack> ctx) {
        final var sender = ctx.getSource().getSender();
        final var ids = plugin.stasis().ids();

        sender.sendMessage(msg("<gray>Stasis entries: <yellow>" + ids.size() + "</yellow>"));

        int i = 0;
        for (final String id : ids) {
            final var type = plugin.stasis().typeOf(UUID.fromString(id));
            final String typeName = (type == null) ? "UNKNOWN" : type.name();
            final String shortId = id.substring(0, 8);

            sender.sendMessage(msg("<gray>- [" + (i++) + "] <yellow>" + shortId + "</yellow> <green>" + typeName + "</green>"));

            if (i >= 20) {
                sender.sendMessage(msg("<gray>... (showing first 20)"));
                break;
            }
        }

        return Command.SINGLE_SUCCESS;
    }

    // TELEPORT COMMANDS

    /**
     * Shows teleport command usage
     */
    public int showTpUsage(final CommandContext<CommandSourceStack> ctx) {
        ctx.getSource()
           .getSender()
           .sendMessage(msg("<yellow>Usage:</yellow> /pokeball admin tp <id>"));

        return Command.SINGLE_SUCCESS;
    }

    /**
     * Teleports player to specified stasis location
     */
    public int teleport(final CommandContext<CommandSourceStack> ctx) {
        final var sender = ctx.getSource().getSender();

        if (!(sender instanceof final Player player)) {
            sender.sendMessage(msg("<red>Players only.</red>"));
            return 0;
        }

        final String idArg = StringArgumentType.getString(ctx, "id");

        try {
            final UUID id = UUID.fromString(idArg);
            final var location = plugin.stasis().locationOf(id);

            if (location == null) {
                sender.sendMessage(msg("<red>Unknown id or location.</red>"));
                return 0;
            }

            player.teleport(location);
            sender.sendMessage(msg("<green>Teleported to stasis location.</green>"));

            return Command.SINGLE_SUCCESS;
        } catch (final IllegalArgumentException ex) {
            sender.sendMessage(msg("<red>Invalid UUID.</red>"));
            return 0;
        }
    }

    // CLEAN COMMANDS

    /**
     * Shows clean command usage
     */
    public int showCleanUsage(final CommandContext<CommandSourceStack> ctx) {
        ctx.getSource()
           .getSender()
           .sendMessage(msg("<yellow>Usage:</yellow> /pokeball admin clean <id|all>"));

        return Command.SINGLE_SUCCESS;
    }

    /**
     * Removes a specific id or all ids from stasis storage
     */
    public int clean(final CommandContext<CommandSourceStack> ctx) {
        final var sender = ctx.getSource().getSender();
        final String target = StringArgumentType.getString(ctx, "target");

        if ("all".equalsIgnoreCase(target)) {
            return cleanAll(sender);
        } else {
            return cleanSingle(sender, target);
        }
    }

    private int cleanAll(final CommandSender sender) {
        final var ids = new HashSet<>(plugin.stasis().ids());

        for (final String id : ids) {
            plugin.stasis().remove(UUID.fromString(id));
        }

        sender.sendMessage(msg("<green>Cleared all stasis entries.</green>"));
        return Command.SINGLE_SUCCESS;
    }

    private int cleanSingle(final CommandSender sender, final String target) {
        try {
            final UUID id = UUID.fromString(target);
            plugin.stasis().remove(id);
            sender.sendMessage(msg("<green>Removed " + id + ".</green>"));

            return Command.SINGLE_SUCCESS;
        } catch (final IllegalArgumentException ex) {
            sender.sendMessage(msg("<red>Invalid UUID.</red>"));
            return 0;
        }
    }

    // CAPTURE ALLOW-LIST COMMANDS

    /**
     * Shows capture allow-list usage
     */
    public int showCaptureUsage(final CommandContext<CommandSourceStack> ctx) {
        ctx.getSource()
           .getSender()
           .sendMessage(msg("<yellow>Usage:</yellow> /pokeball admin capture <list|allow|remove>"));
        return Command.SINGLE_SUCCESS;
    }

    /**
     * Lists currently allowed entity types for capture
     */
    public int listAllowedCaptureTypes(final CommandContext<CommandSourceStack> ctx) {
        final var sender = ctx.getSource().getSender();
        final var allowed = config.capture().allowedTypes();

        if (allowed.isEmpty()) {
            sender.sendMessage(msg("<red>No allowed entity types configured.</red>"));
            return Command.SINGLE_SUCCESS;
        }

        final List<String> names = allowed
                .stream()
                .map(EntityType::name)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();

        sender.sendMessage(msg("<gray>Allowed entities (" + names.size() + "): <green>" + String.join(", ", names) + "</green>"));
        return Command.SINGLE_SUCCESS;
    }

    /**
     * Adds an entity type to the capture allow-list
     */
    public int allowCaptureEntity(final CommandContext<CommandSourceStack> ctx) {
        final var sender = ctx.getSource().getSender();
        final String rawInput = StringArgumentType.getString(ctx, "entity");
        final EntityType type = resolveEntityType(rawInput);

        if (type == null) {
            sender.sendMessage(msg("<red>Invalid entity type:</red> <gray>" + rawInput + "</gray>"));
            return 0;
        }

        final var conf = plugin.getConfig();
        final List<String> allowed = new ArrayList<>(conf.getStringList(ALLOWED_ENTITY_TYPES_PATH));
        final String typeName = type.name();

        final boolean alreadyAllowed = allowed
                .stream()
                .anyMatch(existing -> existing.equalsIgnoreCase(typeName));
        if (alreadyAllowed) {
            sender.sendMessage(msg("<yellow>" + typeName + "</yellow> is already allowed."));
            return 0;
        }

        allowed.add(typeName);
        allowed.sort(String.CASE_INSENSITIVE_ORDER);
        conf.set(ALLOWED_ENTITY_TYPES_PATH, allowed);
        plugin.saveConfig();
        config.reload();

        sender.sendMessage(msg("<green>Added <yellow>" + typeName + "</yellow> to the capture allow-list.</green>"));
        return Command.SINGLE_SUCCESS;
    }

    /**
     * Removes an entity type from the capture allow-list
     */
    public int removeCaptureEntity(final CommandContext<CommandSourceStack> ctx) {
        final var sender = ctx.getSource().getSender();
        final String rawInput = StringArgumentType.getString(ctx, "entity");
        final EntityType type = resolveEntityType(rawInput);

        if (type == null) {
            sender.sendMessage(msg("<red>Invalid entity type:</red> <gray>" + rawInput + "</gray>"));
            return 0;
        }

        final var conf = plugin.getConfig();
        final List<String> allowed = new ArrayList<>(conf.getStringList(ALLOWED_ENTITY_TYPES_PATH));
        final String typeName = type.name();

        final boolean present = allowed
                .stream()
                .anyMatch(existing -> existing.equalsIgnoreCase(typeName));
        if (!present) {
            sender.sendMessage(msg("<red>" + typeName + " is not currently allowed.</red>"));
            return 0;
        }
        if (allowed.size() <= 1) {
            sender.sendMessage(msg("<red>Cannot remove the final allowed entity type.</red>"));
            return 0;
        }

        allowed.removeIf(existing -> existing.equalsIgnoreCase(typeName));
        allowed.sort(String.CASE_INSENSITIVE_ORDER);
        conf.set(ALLOWED_ENTITY_TYPES_PATH, allowed);
        plugin.saveConfig();
        config.reload();

        sender.sendMessage(msg("<green>Removed <yellow>" + typeName + "</yellow> from the capture allow-list.</green>"));
        return Command.SINGLE_SUCCESS;
    }

    private EntityType resolveEntityType(final String rawInput) {
        try {
            final EntityType type = EntityType.valueOf(rawInput.toUpperCase(Locale.ENGLISH));
            return type == EntityType.UNKNOWN ? null : type;
        } catch (final IllegalArgumentException ex) {
            return null;
        }
    }

    // CAPACITY COMMANDS

    /**
     * Handles capacity command - shows current cap or sets new cap
     */
    public int handleCapacity(final CommandContext<CommandSourceStack> ctx) {
        final var sender = ctx.getSource().getSender();

        try {
            // Check if maxTotal argument is provided
            final int maxTotal = IntegerArgumentType.getInteger(ctx, "maxTotal");
            return setCapacity(sender, maxTotal);
        } catch (final IllegalArgumentException e) {
            // No argument provided - show current capacity
            return showCapacity(sender);
        }
    }

    private int showCapacity(final CommandSender sender) {
        final int maxTotal = config
                .stasis()
                .capTotal();
        final String status = (maxTotal > 0) ? "" + maxTotal : "unlimited";

        sender.sendMessage(msg("<gray>Storage cap: <yellow>" + status + "</yellow></gray>"));
        return Command.SINGLE_SUCCESS;
    }

    private int setCapacity(final CommandSender sender, final int maxTotal) {
        final var conf = plugin.getConfig();
        conf.set("stasis.cap.max-total", maxTotal);
        plugin.saveConfig();
        config.reload();

        final String status = (maxTotal > 0) ? "" + maxTotal : "unlimited";
        sender.sendMessage(msg("<green>Storage cap set to: <yellow>" + status + "</yellow></green>"));

        return Command.SINGLE_SUCCESS;
    }

    // REFUND COMMANDS

    /**
     * Handles refund command - shows current mode or sets new mode
     */
    public int handleRefund(final CommandContext<CommandSourceStack> ctx) {
        final var sender = ctx
                .getSource()
                .getSender();

        try {
            // Check if mode argument is provided
            final String modeArg = StringArgumentType.getString(ctx, "mode");
            return setRefundMode(sender, modeArg);
        } catch (final IllegalArgumentException e) {
            // No argument provided - show current mode
            return showRefundMode(sender);
        }
    }

    private int showRefundMode(final CommandSender sender) {
        final String mode = config
                .effects()
                .refundMode()
                .name();
        sender.sendMessage(msg("<gray>Refund mode: <yellow>" + mode + "</yellow></gray>"));
        return Command.SINGLE_SUCCESS;
    }

    private int setRefundMode(final CommandSender sender, final String modeArg) {
        final RefundMode mode;

        try {
            mode = RefundMode.valueOf(modeArg.toUpperCase());
        } catch (final Exception e) {
            sender.sendMessage(msg("<red>Invalid mode. Use GIVE or DROP.</red>"));
            return 0;
        }

        final var conf = plugin.getConfig();
        conf.set("refund.mode", mode.name());
        plugin.saveConfig();
        config.reload();

        sender.sendMessage(msg("<green>Refund mode set to <yellow>" + mode.name() + "</yellow></green>"));
        return Command.SINGLE_SUCCESS;
    }

    private Component msg(final String miniMessage) {
        return plugin.mini().deserialize(miniMessage);
    }
}