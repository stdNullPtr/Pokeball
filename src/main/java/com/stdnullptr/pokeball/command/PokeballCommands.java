package com.stdnullptr.pokeball.command;

import com.stdnullptr.pokeball.Pokeball;
import com.stdnullptr.pokeball.command.builder.CommandTreeBuilder;
import com.stdnullptr.pokeball.command.executor.AdminCommandExecutor;
import com.stdnullptr.pokeball.command.executor.BasicCommandExecutor;
import com.stdnullptr.pokeball.command.executor.GiveCommandExecutor;
import com.stdnullptr.pokeball.command.suggestion.PokeballSuggestionProviders;
import com.stdnullptr.pokeball.config.ConfigManager;
import com.stdnullptr.pokeball.item.PokeballItemFactory;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;

import java.util.List;

/**
 * Clean, modular command registration for /pokeball using Paper's modern Brigadier API
 */
public final class PokeballCommands {
    private final Pokeball plugin;

    private final ConfigManager config;
    private final PokeballItemFactory itemFactory;

    public PokeballCommands(final Pokeball plugin, final PokeballItemFactory itemFactory, final ConfigManager config) {
        this.plugin = plugin;
        this.config = config;
        this.itemFactory = itemFactory;
    }

    public void register() {
        plugin.getLifecycleManager()
              .registerEventHandler(LifecycleEvents.COMMANDS, event -> {
                  final var commands = event.registrar();

                  // Create all the executors and dependencies
                  final var suggestions = new PokeballSuggestionProviders(plugin.stasis());
                  final var basicExecutor = new BasicCommandExecutor(plugin, config);
                  final var giveExecutor = new GiveCommandExecutor(plugin, config, itemFactory);
                  final var adminExecutor = new AdminCommandExecutor(plugin, config);

                  // Build the command tree using the builder pattern
                  final var treeBuilder = new CommandTreeBuilder(
                          basicExecutor,
                          giveExecutor,
                          adminExecutor,
                          suggestions
                  );

                  final var rootCommand = treeBuilder.buildRootCommand();

                  // Register with description and alias
                  commands.register(
                          rootCommand.build(),
                          "Pokeball commands: give, reload, admin",
                          List.of("pb")
                  );
              });
    }
}
