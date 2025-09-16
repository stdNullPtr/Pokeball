package com.stdnullptr.pokeball.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.stdnullptr.pokeball.Pokeball;
import com.stdnullptr.pokeball.config.PluginConfig;
import com.stdnullptr.pokeball.item.PokeballItemFactory;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

/**
 * Brigadier-based command registration for /pokeball
 */
public final class PokeballCommands {
    private final Pokeball plugin;

    private final PokeballItemFactory items;

    private final PluginConfig cfg;

    public PokeballCommands(final Pokeball plugin, final PokeballItemFactory items, final PluginConfig cfg) {
        this.plugin = plugin;
        this.items = items;
        this.cfg = cfg;
    }

    public void register() {
        plugin
                .getLifecycleManager()
                .registerEventHandler(
                        LifecycleEvents.COMMANDS, event -> {
                            final var commands = event.registrar();

                            final SuggestionProvider<CommandSourceStack> suggestAmounts = (ctx, builder) -> {
                                for (final String s : List.of("1", "2", "3", "5", "10", "16", "32", "64"))
                                    builder.suggest(s);
                                return builder.buildFuture();
                            };
                            final SuggestionProvider<CommandSourceStack> suggestIds = (ctx, builder) -> {
                                for (final String id : plugin
                                        .stasis()
                                        .ids())
                                    builder.suggest(id);
                                return builder.buildFuture();
                            };
                            final SuggestionProvider<CommandSourceStack> suggestIdsPlusAll = (ctx, builder) -> {
                                builder.suggest("all");
                                for (final String id : plugin
                                        .stasis()
                                        .ids())
                                    builder.suggest(id);
                                return builder.buildFuture();
                            };
                            final SuggestionProvider<CommandSourceStack> suggestCaps = (ctx, builder) -> {
                                for (final String s : List.of("100", "250", "500", "1000")) builder.suggest(s);
                                return builder.buildFuture();
                            };

                            final LiteralArgumentBuilder<CommandSourceStack> root = Commands
                                    .literal("pokeball")
                                    .executes(this::showHelp)
                                    .then(Commands
                                            .literal("help")
                                            .executes(this::showHelp))
                                    .then(Commands
                                            .literal("version")
                                            .executes(this::version))
                                    .then(Commands
                                            .literal("reload")
                                            .requires(src -> src
                                                    .getSender()
                                                    .hasPermission("pokeball.admin.reload"))
                                            .executes(this::reload))
                                    .then(Commands
                                            .literal("give")
                                            .requires(src -> src
                                                    .getSender()
                                                    .hasPermission("pokeball.admin.give"))
                                            .executes(this::giveUsage)
                                            .then(Commands
                                                    .argument("player", ArgumentTypes.player())
                                                    .executes(ctx -> give(ctx, 1))
                                                    .then(Commands
                                                            .argument("amount", IntegerArgumentType.integer(1, 64))
                                                            .suggests(suggestAmounts)
                                                            .executes(ctx -> give(
                                                                    ctx,
                                                                    IntegerArgumentType.getInteger(ctx, "amount")
                                                            )))))
                                    .then(Commands
                                            .literal("admin")
                                            .requires(src -> src
                                                    .getSender()
                                                    .hasPermission("pokeball.admin"))
                                            .executes(this::adminUsage)
                                            .then(Commands
                                                    .literal("list")
                                                    .executes(this::adminList))
                                            .then(Commands
                                                    .literal("tp")
                                                    .executes(this::adminTpUsage)
                                                    .then(Commands
                                                            .argument("id", StringArgumentType.word())
                                                            .suggests(suggestIds)
                                                            .executes(this::adminTp)))
                                            .then(Commands
                                                    .literal("clean")
                                                    .executes(this::adminCleanUsage)
                                                    .then(Commands
                                                            .argument("target", StringArgumentType.word())
                                                            .suggests(suggestIdsPlusAll)
                                                            .executes(this::adminClean)))
                                            .then(Commands
                                                    .literal("cap")
                                                    .executes(this::adminCapUsage)
                                                    .then(Commands
                                                            .literal("get")
                                                            .executes(this::adminCapGet))
                                                    .then(Commands
                                                            .literal("set")
                                                            .then(Commands
                                                                    .argument("enabled", BoolArgumentType.bool())
                                                                    .then(Commands
                                                                            .argument(
                                                                                    "maxTotal",
                                                                                    IntegerArgumentType.integer(
                                                                                            1,
                                                                                            100000
                                                                                    )
                                                                            )
                                                                            .suggests(suggestCaps)
                                                                            .executes(this::adminCapSet))))));

                            // Add refund get/set admin branch
                            root.then(Commands
                                    .literal("admin")
                                    .requires(src -> src
                                            .getSender()
                                            .hasPermission("pokeball.admin"))
                                    .then(Commands
                                            .literal("refund")
                                            .then(Commands
                                                    .literal("get")
                                                    .executes(this::adminRefundGet))
                                            .then(Commands
                                                    .literal("set")
                                                    .then(Commands
                                                            .argument("mode", StringArgumentType.word())
                                                            .suggests((ctx, b) -> {
                                                                b.suggest("GIVE");
                                                                b.suggest("DROP");
                                                                return b.buildFuture();
                                                            })
                                                            .executes(this::adminRefundSet)))));

                            // Register with description and alias
                            commands.register(root.build(), "Pokeball commands: give, reload, admin", List.of("pb"));
                        }
                );
    }

    private int showHelp(final CommandContext<CommandSourceStack> ctx) {
        final var sender = ctx
                .getSource()
                .getSender();
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
            lines.add(msg("<yellow>/pokeball admin cap get|set <enabled> <max></yellow> <gray>- Storage cap</gray>"));
            lines.add(msg("<yellow>/pokeball admin refund get|set <GIVE|DROP></yellow> <gray>- Refund behavior</gray>"));
        }
        sender.sendMessage(msg(cfg.msgPrefix));
        for (final Component c : lines) sender.sendMessage(c);
        return Command.SINGLE_SUCCESS;
    }

    private int reload(final CommandContext<CommandSourceStack> ctx) {
        cfg.reload();
        ctx
                .getSource()
                .getSender()
                .sendMessage(msg(cfg.msgPrefix + " " + cfg.msgReloaded));
        return Command.SINGLE_SUCCESS;
    }

    private int giveUsage(final CommandContext<CommandSourceStack> ctx) {
        ctx
                .getSource()
                .getSender()
                .sendMessage(msg("<yellow>Usage:</yellow> /pokeball give <player> [amount]"));
        return Command.SINGLE_SUCCESS;
    }

    private int version(final CommandContext<CommandSourceStack> ctx) {
        final var meta = plugin.getPluginMeta();
        final String name = (!meta
                .getDisplayName()
                .isBlank()) ? meta.getDisplayName() : meta.getName();
        final String version = meta.getVersion();
        final String api = meta.getAPIVersion();
        ctx
                .getSource()
                .getSender()
                .sendMessage(msg("<yellow>" + name + "</yellow> <gray>v</gray><green>" + version + "</green> <gray>(api " + api + ")</gray>"));
        return Command.SINGLE_SUCCESS;
    }

    private int give(final CommandContext<CommandSourceStack> ctx, final int amount) {
        final PlayerSelectorArgumentResolver sel = ctx.getArgument("player", PlayerSelectorArgumentResolver.class);
        final List<Player> players;
        try {
            players = sel.resolve(ctx.getSource());
        } catch (final CommandSyntaxException e) {
            ctx
                    .getSource()
                    .getSender()
                    .sendMessage(msg("<red>Invalid player selector.</red>"));
            return 0;
        }
        if (players.isEmpty()) {
            ctx
                    .getSource()
                    .getSender()
                    .sendMessage(msg("<red>No player matched.</red>"));
            return 0;
        }
        final Player target = players.getFirst();
        final int amt = Math.max(1, amount);
        for (int i = 0; i < amt; i++)
            target
                    .getInventory()
                    .addItem(items.createEmptyBall());
        ctx
                .getSource()
                .getSender()
                .sendMessage(msg(cfg.msgPrefix + " " + cfg.msgGiven
                        .replace("<count>", String.valueOf(amt))
                        .replace("<player>", target.getName())));
        return Command.SINGLE_SUCCESS;
    }

    private int adminList(final CommandContext<CommandSourceStack> ctx) {
        final var sender = ctx
                .getSource()
                .getSender();
        final var ids = plugin
                .stasis()
                .ids();
        sender.sendMessage(msg("<gray>Stasis entries: <yellow>" + ids.size() + "</yellow>"));
        int i = 0;
        for (final String id : ids) {
            final var type = plugin
                    .stasis()
                    .typeOf(UUID.fromString(id));
            sender.sendMessage(msg("<gray>- [" + (i++) + "] <yellow>" + id.substring(
                    0,
                    8
            ) + "</yellow> <green>" + (type == null ? "UNKNOWN" : type.name()) + "</green>"));
            if (i >= 20) {
                sender.sendMessage(msg("<gray>... (showing first 20)"));
                break;
            }
        }
        return Command.SINGLE_SUCCESS;
    }

    private int adminUsage(final CommandContext<CommandSourceStack> ctx) {
        final var s = ctx
                .getSource()
                .getSender();
        s.sendMessage(msg("<yellow>Usage:</yellow> /pokeball admin <list|tp|clean|cap>"));
        return Command.SINGLE_SUCCESS;
    }

    private int adminTpUsage(final CommandContext<CommandSourceStack> ctx) {
        ctx
                .getSource()
                .getSender()
                .sendMessage(msg("<yellow>Usage:</yellow> /pokeball admin tp <id>"));
        return Command.SINGLE_SUCCESS;
    }

    private int adminCleanUsage(final CommandContext<CommandSourceStack> ctx) {
        ctx
                .getSource()
                .getSender()
                .sendMessage(msg("<yellow>Usage:</yellow> /pokeball admin clean <id|all>"));
        return Command.SINGLE_SUCCESS;
    }

    private int adminCapUsage(final CommandContext<CommandSourceStack> ctx) {
        ctx
                .getSource()
                .getSender()
                .sendMessage(msg("<yellow>Usage:</yellow> /pokeball admin cap <get|set <enabled> <max>>"));
        return Command.SINGLE_SUCCESS;
    }

    private int adminTp(final CommandContext<CommandSourceStack> ctx) {
        final var sender = ctx
                .getSource()
                .getSender();
        if (!(sender instanceof final Player p)) {
            sender.sendMessage(msg("<red>Players only.</red>"));
            return 0;
        }
        final String idArg = StringArgumentType.getString(ctx, "id");
        try {
            final UUID id = UUID.fromString(idArg);
            final var loc = plugin
                    .stasis()
                    .locationOf(id);
            if (loc == null) {
                sender.sendMessage(msg("<red>Unknown id or location.</red>"));
                return 0;
            }
            p.teleport(loc);
            sender.sendMessage(msg("<green>Teleported to stasis location.</green>"));
            return Command.SINGLE_SUCCESS;
        } catch (final IllegalArgumentException ex) {
            sender.sendMessage(msg("<red>Invalid UUID.</red>"));
            return 0;
        }
    }

    private int adminClean(final CommandContext<CommandSourceStack> ctx) {
        final var sender = ctx
                .getSource()
                .getSender();
        final String target = StringArgumentType.getString(ctx, "target");
        if ("all".equalsIgnoreCase(target)) {
            final var ids = new HashSet<>(plugin
                    .stasis()
                    .ids());
            for (final String id : ids)
                plugin
                        .stasis()
                        .remove(UUID.fromString(id));
            sender.sendMessage(msg("<green>Cleared all stasis entries.</green>"));
            return Command.SINGLE_SUCCESS;
        }
        try {
            final UUID id = UUID.fromString(target);
            plugin
                    .stasis()
                    .remove(id);
            sender.sendMessage(msg("<green>Removed " + id + ".</green>"));
            return Command.SINGLE_SUCCESS;
        } catch (final IllegalArgumentException ex) {
            sender.sendMessage(msg("<red>Invalid UUID.</red>"));
            return 0;
        }
    }

    private int adminCapGet(final CommandContext<CommandSourceStack> ctx) {
        final var sender = ctx
                .getSource()
                .getSender();
        sender.sendMessage(msg("<gray>Cap enabled: " + cfg.stasisCapEnabled() + ", max-total: " + cfg.stasisCapTotal()));
        return Command.SINGLE_SUCCESS;
    }

    private int adminCapSet(final CommandContext<CommandSourceStack> ctx) {
        final boolean enabled = BoolArgumentType.getBool(ctx, "enabled");
        final int max = IntegerArgumentType.getInteger(ctx, "maxTotal");
        final var conf = plugin.getConfig();
        conf.set("stasis.cap.enabled", enabled);
        conf.set("stasis.cap.max-total", max);
        plugin.saveConfig();
        cfg.reload();
        ctx
                .getSource()
                .getSender()
                .sendMessage(msg("<green>Updated cap: enabled=" + enabled + ", max=" + max + "</green>"));
        return Command.SINGLE_SUCCESS;
    }

    private int adminRefundGet(final CommandContext<CommandSourceStack> ctx) {
        ctx
                .getSource()
                .getSender()
                .sendMessage(msg("<gray>Refund mode: <yellow>" + cfg
                        .refundMode()
                        .name() + "</yellow></gray>"));
        return Command.SINGLE_SUCCESS;
    }

    private int adminRefundSet(final CommandContext<CommandSourceStack> ctx) {
        final String mode = StringArgumentType.getString(ctx, "mode");
        final PluginConfig.RefundMode parsed;
        try {
            parsed = PluginConfig.RefundMode.valueOf(mode.toUpperCase());
        } catch (final Exception e) {
            ctx
                    .getSource()
                    .getSender()
                    .sendMessage(msg("<red>Invalid mode. Use GIVE or DROP.</red>"));
            return 0;
        }
        final var conf = plugin.getConfig();
        conf.set("refund.mode", parsed.name());
        plugin.saveConfig();
        cfg.reload();
        ctx
                .getSource()
                .getSender()
                .sendMessage(msg("<green>Refund mode set to <yellow>" + parsed.name() + "</yellow></green>"));
        return Command.SINGLE_SUCCESS;
    }

    private Component msg(final String mm) {
        return plugin
                .mini()
                .deserialize(mm);
    }
}
