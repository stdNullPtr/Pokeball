package com.stdnullptr.pokeball.command;

import com.stdnullptr.pokeball.Pokeball;
import com.stdnullptr.pokeball.config.PluginConfig;
import com.stdnullptr.pokeball.item.PokeballItemFactory;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class PokeballRootCommand implements BasicCommand {
    private final Pokeball plugin;
    private final PokeballItemFactory items;
    private final PluginConfig cfg;

    public PokeballRootCommand(Pokeball plugin, PokeballItemFactory items, PluginConfig cfg) {
        this.plugin = plugin;
        this.items = items;
        this.cfg = cfg;
    }

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        CommandSender sender = source.getSender();
        String label = "pokeball"; // Paper BasicCommand does not provide label; use base label

        if (args.length == 0) {
            sender.sendMessage(msg("<yellow>/" + label + " <give|reload>"));
            return;
        }
        switch (args[0].toLowerCase()) {
            case "reload" -> {
                if (!sender.hasPermission("pokeball.admin.reload")) {
                    sender.sendMessage(msg("<red>No permission."));
                    return;
                }
                cfg.reload();
                sender.sendMessage(msg(cfg.msgPrefix + " " + cfg.msgReloaded));
            }
            case "give" -> {
                if (!sender.hasPermission("pokeball.admin.give")) {
                    sender.sendMessage(msg("<red>No permission."));
                    return;
                }
                if (args.length < 2) {
                    sender.sendMessage(msg("<red>Usage: /" + label + " give <player> [amount]"));
                    return;
                }
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) {
                    sender.sendMessage(msg("<red>Player not found."));
                    return;
                }
                int amount = 1;
                if (args.length >= 3) {
                    try { amount = Math.max(1, Integer.parseInt(args[2])); } catch (NumberFormatException ignored) {}
                }
                for (int i = 0; i < amount; i++) {
                    target.getInventory().addItem(items.createEmptyBall());
                }
                sender.sendMessage(msg(cfg.msgPrefix + " " + cfg.msgGiven
                    .replace("<count>", String.valueOf(amount))
                    .replace("<player>", target.getName())));
            }
            default -> sender.sendMessage(msg("<red>Unknown subcommand."));
        }
    }

    @Override
    public Collection<String> suggest(CommandSourceStack source, String[] args) {
        CommandSender sender = source.getSender();
        if (args.length == 1) {
            List<String> subs = new ArrayList<>();
            if (sender.hasPermission("pokeball.admin.give")) subs.add("give");
            if (sender.hasPermission("pokeball.admin.reload")) subs.add("reload");
            return StringUtil.copyPartialMatches(args[0], subs, new ArrayList<>());
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            List<String> names = Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
            return StringUtil.copyPartialMatches(args[1], names, new ArrayList<>());
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            return StringUtil.copyPartialMatches(args[2], List.of("1","2","3","5","10","16","32","64"), new ArrayList<>());
        }
        return List.of();
    }

    private Component msg(String mm) { return plugin.mini().deserialize(mm); }
}
