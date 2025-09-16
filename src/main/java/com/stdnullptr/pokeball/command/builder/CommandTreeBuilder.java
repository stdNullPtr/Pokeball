package com.stdnullptr.pokeball.command.builder;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.stdnullptr.pokeball.command.executor.AdminCommandExecutor;
import com.stdnullptr.pokeball.command.executor.BasicCommandExecutor;
import com.stdnullptr.pokeball.command.executor.GiveCommandExecutor;
import com.stdnullptr.pokeball.command.suggestion.PokeballSuggestionProviders;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;

/**
 * Builds the complete Pokeball command tree using a fluent interface
 */
public final class CommandTreeBuilder {

    private final BasicCommandExecutor basicExecutor;
    private final GiveCommandExecutor giveExecutor;
    private final AdminCommandExecutor adminExecutor;
    private final PokeballSuggestionProviders suggestions;

    public CommandTreeBuilder(
            final BasicCommandExecutor basicExecutor,
            final GiveCommandExecutor giveExecutor,
            final AdminCommandExecutor adminExecutor,
            final PokeballSuggestionProviders suggestions
    ) {
        this.basicExecutor = basicExecutor;
        this.giveExecutor = giveExecutor;
        this.adminExecutor = adminExecutor;
        this.suggestions = suggestions;
    }

    /**
     * Builds the complete command tree for the pokeball command
     */
    public LiteralArgumentBuilder<CommandSourceStack> buildRootCommand() {
        return Commands.literal("pokeball")
                .executes(basicExecutor::showHelp)
                .then(buildHelpCommand())
                .then(buildVersionCommand())
                .then(buildReloadCommand())
                .then(buildGiveCommand())
                .then(buildAdminCommands());
    }

    private LiteralArgumentBuilder<CommandSourceStack> buildHelpCommand() {
        return Commands.literal("help")
                .executes(basicExecutor::showHelp);
    }

    private LiteralArgumentBuilder<CommandSourceStack> buildVersionCommand() {
        return Commands.literal("version")
                .executes(basicExecutor::showVersion);
    }

    private LiteralArgumentBuilder<CommandSourceStack> buildReloadCommand() {
        return Commands.literal("reload")
                .requires(src -> src.getSender().hasPermission("pokeball.admin.reload"))
                .executes(basicExecutor::reload);
    }

    private LiteralArgumentBuilder<CommandSourceStack> buildGiveCommand() {
        return Commands.literal("give")
                .requires(src -> src.getSender().hasPermission("pokeball.admin.give"))
                .executes(giveExecutor::showUsage)
                .then(Commands.argument("player", ArgumentTypes.player())
                        .executes(giveExecutor::giveDefault)
                        .then(Commands.argument("amount", IntegerArgumentType.integer(1, 64))
                                .suggests(suggestions.amounts())
                                .executes(giveExecutor::giveWithAmount)));
    }

    private LiteralArgumentBuilder<CommandSourceStack> buildAdminCommands() {
        return Commands.literal("admin")
                .requires(src -> src.getSender().hasPermission("pokeball.admin"))
                .executes(adminExecutor::showUsage)
                .then(buildListCommand())
                .then(buildTeleportCommand())
                .then(buildCleanCommand())
                .then(buildCapacityCommands())
                .then(buildRefundCommands());
    }

    private LiteralArgumentBuilder<CommandSourceStack> buildListCommand() {
        return Commands.literal("list")
                .executes(adminExecutor::list);
    }

    private LiteralArgumentBuilder<CommandSourceStack> buildTeleportCommand() {
        return Commands.literal("tp")
                .executes(adminExecutor::showTpUsage)
                .then(Commands.argument("id", StringArgumentType.word())
                        .suggests(suggestions.stasisIds())
                        .executes(adminExecutor::teleport));
    }

    private LiteralArgumentBuilder<CommandSourceStack> buildCleanCommand() {
        return Commands.literal("clean")
                .executes(adminExecutor::showCleanUsage)
                .then(Commands.argument("target", StringArgumentType.word())
                        .suggests(suggestions.stasisIdsWithAll())
                        .executes(adminExecutor::clean));
    }

    private LiteralArgumentBuilder<CommandSourceStack> buildCapacityCommands() {
        return Commands.literal("cap")
                .executes(adminExecutor::showCapUsage)
                .then(Commands.literal("get")
                        .executes(adminExecutor::getCapacity))
                .then(Commands.literal("set")
                        .then(Commands.argument("enabled", BoolArgumentType.bool())
                                .then(Commands.argument("maxTotal", IntegerArgumentType.integer(1, 100000))
                                        .suggests(suggestions.capacityLimits())
                                        .executes(adminExecutor::setCapacity))));
    }

    private LiteralArgumentBuilder<CommandSourceStack> buildRefundCommands() {
        return Commands.literal("refund")
                .then(Commands.literal("get")
                        .executes(adminExecutor::getRefundMode))
                .then(Commands.literal("set")
                        .then(Commands.argument("mode", StringArgumentType.word())
                                .suggests(suggestions.refundModes())
                                .executes(adminExecutor::setRefundMode)));
    }
}