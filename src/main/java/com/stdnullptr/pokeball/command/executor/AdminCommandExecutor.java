package com.stdnullptr.pokeball.command.executor;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.stdnullptr.pokeball.Pokeball;
import com.stdnullptr.pokeball.config.PluginConfig;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.UUID;

/**
 * Executes admin subcommands for managing stasis entries and plugin configuration
 */
public final class AdminCommandExecutor {

    private final Pokeball plugin;
    private final PluginConfig config;

    public AdminCommandExecutor(final Pokeball plugin, final PluginConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    /**
     * Shows admin command usage
     */
    public int showUsage(final CommandContext<CommandSourceStack> ctx) {
        ctx.getSource()
           .getSender()
           .sendMessage(msg("<yellow>Usage:</yellow> /pokeball admin <list|tp|clean|cap|refund>"));

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
     * Cleans stasis entries (specific ID or all)
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

    private int cleanAll(final org.bukkit.command.CommandSender sender) {
        final var ids = new HashSet<>(plugin.stasis().ids());

        for (final String id : ids) {
            plugin.stasis().remove(UUID.fromString(id));
        }

        sender.sendMessage(msg("<green>Cleared all stasis entries.</green>"));
        return Command.SINGLE_SUCCESS;
    }

    private int cleanSingle(final org.bukkit.command.CommandSender sender, final String target) {
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

    // CAPACITY COMMANDS

    /**
     * Shows capacity command usage
     */
    public int showCapUsage(final CommandContext<CommandSourceStack> ctx) {
        ctx.getSource()
           .getSender()
           .sendMessage(msg("<yellow>Usage:</yellow> /pokeball admin cap <get|set <enabled> <max>>"));

        return Command.SINGLE_SUCCESS;
    }

    /**
     * Gets current capacity settings
     */
    public int getCapacity(final CommandContext<CommandSourceStack> ctx) {
        final var sender = ctx.getSource().getSender();
        final boolean enabled = config.stasisCapEnabled();
        final int maxTotal = config.stasisCapTotal();

        sender.sendMessage(msg("<gray>Cap enabled: " + enabled + ", max-total: " + maxTotal));

        return Command.SINGLE_SUCCESS;
    }

    /**
     * Sets capacity settings
     */
    public int setCapacity(final CommandContext<CommandSourceStack> ctx) {
        final boolean enabled = BoolArgumentType.getBool(ctx, "enabled");
        final int maxTotal = IntegerArgumentType.getInteger(ctx, "maxTotal");

        final var conf = plugin.getConfig();
        conf.set("stasis.cap.enabled", enabled);
        conf.set("stasis.cap.max-total", maxTotal);
        plugin.saveConfig();
        config.reload();

        ctx.getSource()
           .getSender()
           .sendMessage(msg("<green>Updated cap: enabled=" + enabled + ", max=" + maxTotal + "</green>"));

        return Command.SINGLE_SUCCESS;
    }

    // REFUND COMMANDS

    /**
     * Gets current refund mode
     */
    public int getRefundMode(final CommandContext<CommandSourceStack> ctx) {
        final String mode = config.refundMode().name();

        ctx.getSource()
           .getSender()
           .sendMessage(msg("<gray>Refund mode: <yellow>" + mode + "</yellow></gray>"));

        return Command.SINGLE_SUCCESS;
    }

    /**
     * Sets refund mode
     */
    public int setRefundMode(final CommandContext<CommandSourceStack> ctx) {
        final String modeArg = StringArgumentType.getString(ctx, "mode");
        final PluginConfig.RefundMode mode;

        try {
            mode = PluginConfig.RefundMode.valueOf(modeArg.toUpperCase());
        } catch (final Exception e) {
            ctx.getSource()
               .getSender()
               .sendMessage(msg("<red>Invalid mode. Use GIVE or DROP.</red>"));
            return 0;
        }

        final var conf = plugin.getConfig();
        conf.set("refund.mode", mode.name());
        plugin.saveConfig();
        config.reload();

        ctx.getSource()
           .getSender()
           .sendMessage(msg("<green>Refund mode set to <yellow>" + mode.name() + "</yellow></green>"));

        return Command.SINGLE_SUCCESS;
    }

    private Component msg(final String miniMessage) {
        return plugin.mini().deserialize(miniMessage);
    }
}