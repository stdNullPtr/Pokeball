package com.stdnullptr.pokeball.command.executor;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.stdnullptr.pokeball.Pokeball;
import com.stdnullptr.pokeball.config.ConfigManager;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Executes basic Pokeball commands: help, version, reload
 */
public final class BasicCommandExecutor {

    private final Pokeball plugin;

    private final ConfigManager config;

    public BasicCommandExecutor(final Pokeball plugin, final ConfigManager config) {
        this.plugin = plugin;
        this.config = config;
    }

    /**
     * Shows help message with available commands based on sender permissions
     */
    public int showHelp(final CommandContext<CommandSourceStack> ctx) {
        final var sender = ctx.getSource().getSender();
        final List<Component> lines = new ArrayList<>();

        lines.add(msg("<yellow>/pokeball help</yellow> <gray>- Show this help</gray>"));
        lines.add(msg("<yellow>/pokeball version</yellow> <gray>- Show plugin version</gray>"));

        if (sender.hasPermission("pokeball.admin.give")) {
            lines.add(msg("<yellow>/pokeball give <player> [amount]</yellow> <gray>- Give balls</gray>"));
        }

        if (sender.hasPermission("pokeball.admin.reload")) {
            lines.add(msg("<yellow>/pokeball reload</yellow> <gray>- Reload configuration</gray>"));
        }

        if (sender.hasPermission("pokeball.admin")) {
            lines.add(msg("<yellow>/pokeball admin list</yellow> <gray>- List stasis entries</gray>"));
            lines.add(msg("<yellow>/pokeball admin tp <id></yellow> <gray>- Teleport to stasis</gray>"));
            lines.add(msg("<yellow>/pokeball admin clean <id|all></yellow> <gray>- Remove entries</gray>"));
            lines.add(msg("<yellow>/pokeball admin cap [max]</yellow> <gray>- Storage cap</gray>"));
            lines.add(msg("<yellow>/pokeball admin refund [mode]</yellow> <gray>- Refund behavior</gray>"));
        }

        sender.sendMessage(msg(config
                                       .messages()
                                       .getPrefix()));
        for (final Component line : lines) {
            sender.sendMessage(line);
        }

        return Command.SINGLE_SUCCESS;
    }

    /**
     * Shows plugin version information
     */
    public int showVersion(final CommandContext<CommandSourceStack> ctx) {
        final var meta = plugin.getPluginMeta();
        final String name = (!meta.getDisplayName().isBlank()) ? meta.getDisplayName() : meta.getName();
        final String version = meta.getVersion();
        final String api = meta.getAPIVersion();

        ctx.getSource()
           .getSender()
           .sendMessage(msg("<yellow>" + name + "</yellow> <gray>v</gray><green>" + version + "</green> <gray>(api " + api + ")</gray>"));

        return Command.SINGLE_SUCCESS;
    }

    /**
     * Reloads plugin configuration
     */
    public int reload(final CommandContext<CommandSourceStack> ctx) {
        config.reload();
        ctx.getSource()
           .getSender()
           .sendMessage(msg(config
                                    .messages()
                                    .getPrefix() + " " + config
                   .messages()
                   .getReloaded()));

        return Command.SINGLE_SUCCESS;
    }

    private Component msg(final String miniMessage) {
        return plugin.mini().deserialize(miniMessage);
    }
}
