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
            case "admin" -> {
                if (!sender.hasPermission("pokeball.admin")) { sender.sendMessage(msg("<red>No permission.")); return; }
                if (args.length < 2) { sender.sendMessage(msg("<red>Usage: /"+label+" admin <list|tp|clean|cap> ...")); return; }
                switch (args[1].toLowerCase()) {
                    case "list" -> {
                        var ids = plugin.stasis().ids();
                        sender.sendMessage(msg("<gray>Stasis entries: <yellow>"+ids.size()+"</yellow>"));
                        int i = 0;
                        for (String id : ids) {
                            var type = plugin.stasis().typeOf(java.util.UUID.fromString(id));
                            sender.sendMessage(msg("<gray>- ["+(i++)+"] <yellow>"+id.substring(0,8)+"</yellow> <green>"+(type==null?"UNKNOWN":type.name())+"</green>"));
                            if (i>=20) { sender.sendMessage(msg("<gray>... (showing first 20)")); break; }
                        }
                    }
                    case "tp" -> {
                        if (!(sender instanceof org.bukkit.entity.Player p)) { sender.sendMessage(msg("<red>Players only.")); return; }
                        if (args.length < 3) { sender.sendMessage(msg("<red>Usage: /"+label+" admin tp <ballId>")); return; }
                        try {
                            var id = java.util.UUID.fromString(args[2]);
                            var loc = plugin.stasis().locationOf(id);
                            if (loc == null) { sender.sendMessage(msg("<red>Unknown id or location.")); return; }
                            p.teleport(loc);
                            sender.sendMessage(msg("<green>Teleported to stasis location."));
                        } catch (IllegalArgumentException ex) {
                            sender.sendMessage(msg("<red>Invalid UUID."));
                        }
                    }
                    case "clean" -> {
                        if (args.length < 3) { sender.sendMessage(msg("<red>Usage: /"+label+" admin clean <ballId|all>")); return; }
                        if (args[2].equalsIgnoreCase("all")) {
                            var ids = new java.util.HashSet<>(plugin.stasis().ids());
                            for (String id : ids) plugin.stasis().remove(java.util.UUID.fromString(id));
                            sender.sendMessage(msg("<green>Cleared all stasis entries."));
                        } else {
                            try {
                                var id = java.util.UUID.fromString(args[2]);
                                plugin.stasis().remove(id);
                                sender.sendMessage(msg("<green>Removed " + id + "."));
                            } catch (IllegalArgumentException ex) {
                                sender.sendMessage(msg("<red>Invalid UUID."));
                            }
                        }
                    }
                    case "cap" -> {
                        if (args.length < 3) { sender.sendMessage(msg("<red>Usage: /"+label+" admin cap <get|set> [value]")); return; }
                        if (args[2].equalsIgnoreCase("get")) {
                            sender.sendMessage(msg("<gray>Cap enabled: "+cfg.stasisCapEnabled()+", max-total: "+cfg.stasisCapTotal()));
                        } else if (args[2].equalsIgnoreCase("set")) {
                            if (args.length < 5) { sender.sendMessage(msg("<red>Usage: /"+label+" admin cap set <enabled:true|false> <maxTotal>")); return; }
                            boolean enabled = Boolean.parseBoolean(args[3]);
                            int max;
                            try { max = Integer.parseInt(args[4]); } catch (NumberFormatException e) { sender.sendMessage(msg("<red>Invalid max.")); return; }
                            // Apply live
                            var conf = plugin.getConfig();
                            conf.set("stasis.cap.enabled", enabled);
                            conf.set("stasis.cap.max-total", max);
                            plugin.saveConfig();
                            cfg.reload();
                            sender.sendMessage(msg("<green>Updated cap: enabled="+enabled+", max="+max));
                        } else sender.sendMessage(msg("<red>Usage: /"+label+" admin cap <get|set>"));
                    }
                    default -> sender.sendMessage(msg("<red>Unknown admin subcommand."));
                }
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
            if (sender.hasPermission("pokeball.admin")) subs.add("admin");
            return StringUtil.copyPartialMatches(args[0], subs, new ArrayList<>());
        }
        if (args.length >= 2 && args[0].equalsIgnoreCase("admin")) {
            if (args.length == 2) return StringUtil.copyPartialMatches(args[1], List.of("list","tp","clean","cap"), new ArrayList<>());
            if (args.length == 3) {
                if (args[1].equalsIgnoreCase("cap")) return StringUtil.copyPartialMatches(args[2], List.of("get","set"), new ArrayList<>());
                if (args[1].equalsIgnoreCase("tp") || args[1].equalsIgnoreCase("clean")) {
                    var ids = ((Pokeball)Bukkit.getPluginManager().getPlugin("Pokeball")).stasis().ids();
                    return StringUtil.copyPartialMatches(args[2], new ArrayList<>(ids), new ArrayList<>());
                }
            }
            if (args.length == 4 && args[1].equalsIgnoreCase("cap") && args[2].equalsIgnoreCase("set")) {
                return StringUtil.copyPartialMatches(args[3], List.of("true","false"), new ArrayList<>());
            }
            if (args.length == 5 && args[1].equalsIgnoreCase("cap") && args[2].equalsIgnoreCase("set")) {
                return StringUtil.copyPartialMatches(args[4], List.of("100","250","500","1000"), new ArrayList<>());
            }
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
