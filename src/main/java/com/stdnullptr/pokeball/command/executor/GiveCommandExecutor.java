package com.stdnullptr.pokeball.command.executor;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.stdnullptr.pokeball.Pokeball;
import com.stdnullptr.pokeball.config.ConfigManager;
import com.stdnullptr.pokeball.item.PokeballItemFactory;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Executes the give command for distributing Pokeballs to players
 */
public final class GiveCommandExecutor {

    private final Pokeball plugin;

    private final ConfigManager config;
    private final PokeballItemFactory itemFactory;

    public GiveCommandExecutor(
            final Pokeball plugin,
            final ConfigManager config,
            final PokeballItemFactory itemFactory
    ) {
        this.plugin = plugin;
        this.config = config;
        this.itemFactory = itemFactory;
    }

    /**
     * Shows usage information for the give command
     */
    public int showUsage(final CommandContext<CommandSourceStack> ctx) {
        ctx.getSource()
           .getSender()
           .sendMessage(msg("<yellow>Usage:</yellow> /pokeball give <player> [amount]"));

        return Command.SINGLE_SUCCESS;
    }

    /**
     * Gives pokeballs to the specified player with a default amount of 1
     */
    public int giveDefault(final CommandContext<CommandSourceStack> ctx) {
        return executeGive(ctx, 1);
    }

    /**
     * Gives pokeballs to the specified player with the specified amount
     */
    public int giveWithAmount(final CommandContext<CommandSourceStack> ctx) {
        final int amount = IntegerArgumentType.getInteger(ctx, "amount");
        return executeGive(ctx, amount);
    }

    private int executeGive(final CommandContext<CommandSourceStack> ctx, final int amount) {
        final PlayerSelectorArgumentResolver selector = ctx.getArgument("player", PlayerSelectorArgumentResolver.class);
        final List<Player> players;

        try {
            players = selector.resolve(ctx.getSource());
        } catch (final CommandSyntaxException e) {
            ctx.getSource()
               .getSender()
               .sendMessage(msg("<red>Invalid player selector.</red>"));
            return 0;
        }

        if (players.isEmpty()) {
            ctx.getSource()
               .getSender()
               .sendMessage(msg("<red>No player matched.</red>"));
            return 0;
        }

        final Player target = players.getFirst();
        final int finalAmount = Math.max(1, amount);

        // Give the pokeballs to the target player
        for (int i = 0; i < finalAmount; i++) {
            target.getInventory().addItem(itemFactory.createEmptyBall());
        }

        // Send confirmation message
        final String givenMessage = config.messages().getGiven();
        final String confirmationMessage = config.messages().getPrefix() + " " +
                (givenMessage != null ? givenMessage : "<green>Gave <yellow><count></yellow> Pokeball(s) to <yellow><player></yellow>.")
                .replace("<count>", String.valueOf(finalAmount))
                .replace("<player>", target.getName());

        ctx.getSource()
           .getSender()
           .sendMessage(msg(confirmationMessage));

        return Command.SINGLE_SUCCESS;
    }

    private Component msg(final String miniMessage) {
        return plugin.mini().deserialize(miniMessage);
    }
}
