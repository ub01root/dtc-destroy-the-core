package com.imperiordevelopment.dtc.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import com.imperiordevelopment.dtc.IDTCPlugin;
import com.imperiordevelopment.dtc.editor.EditorMenuManager;
import com.imperiordevelopment.dtc.editor.WandService;
import com.imperiordevelopment.dtc.manager.EventManager;
import com.imperiordevelopment.dtc.util.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public final class DtcCommand implements CommandExecutor, TabCompleter {

    private final IDTCPlugin plugin;
    private final EventManager eventManager;
    private final WandService wandService;
    private final EditorMenuManager editorMenuManager;

    public DtcCommand(IDTCPlugin plugin, EventManager eventManager, WandService wandService,
                      EditorMenuManager editorMenuManager) {
        this.plugin = plugin;
        this.eventManager = eventManager;
        this.wandService = wandService;
        this.editorMenuManager = editorMenuManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "start":
                if (!sender.hasPermission("idtc.admin")) {
                    MessageUtil.send(sender, plugin.getConfig(), "messages.no-permission");
                    return true;
                }
                eventManager.start(sender);
                return true;
            case "stop":
                if (!sender.hasPermission("idtc.admin")) {
                    MessageUtil.send(sender, plugin.getConfig(), "messages.no-permission");
                    return true;
                }
                eventManager.stop(true);
                return true;
            case "setcore":
                if (!sender.hasPermission("idtc.admin")) {
                    MessageUtil.send(sender, plugin.getConfig(), "messages.no-permission");
                    return true;
                }
                if (!(sender instanceof Player)) {
                    MessageUtil.send(sender, plugin.getConfig(), "messages.only-players");
                    return true;
                }
                Player player = (Player) sender;
                if (player.getTargetBlockExact(6) == null) {
                    MessageUtil.send(sender, plugin.getConfig(), "messages.setcore-no-target");
                    return true;
                }
                eventManager.setCore(player.getTargetBlockExact(6).getLocation());
                MessageUtil.send(sender, plugin.getConfig(), "messages.core-set");
                return true;
            case "reward":
                if (!sender.hasPermission("idtc.admin")) {
                    MessageUtil.send(sender, plugin.getConfig(), "messages.no-permission");
                    return true;
                }
                if (args.length < 2) {
                    MessageUtil.send(sender, plugin.getConfig(), "messages.reward-usage");
                    return true;
                }
                eventManager.runRewards(args[1]);
                return true;
            case "reload":
                if (!sender.hasPermission("idtc.admin")) {
                    MessageUtil.send(sender, plugin.getConfig(), "messages.no-permission");
                    return true;
                }
                plugin.reloadConfig();
                MessageUtil.reload(plugin);
                eventManager.reloadFromConfig();
                MessageUtil.send(sender, plugin.getConfig(), "messages.reload-done");
                return true;
            case "stats":
                if (!sender.hasPermission("idtc.stats")) {
                    MessageUtil.send(sender, plugin.getConfig(), "messages.no-permission");
                    return true;
                }
                if (args.length >= 2) {
                    String msg = MessageUtil.get(plugin.getConfig(), "messages.stats-other");
                    msg = msg.replace("%player%", args[1]);
                    msg = msg.replace("%damage%", String.valueOf((int) eventManager.getTotalDamage(args[1])));
                    sender.sendMessage(MessageUtil.colorize(msg));
                } else if (sender instanceof Player) {
                    String msg = MessageUtil.get(plugin.getConfig(), "messages.stats-self");
                    msg = msg.replace("%damage%", String.valueOf((int) eventManager.getTotalDamage(((Player) sender).getName())));
                    sender.sendMessage(MessageUtil.colorize(msg));
                } else {
                    MessageUtil.send(sender, plugin.getConfig(), "messages.usage");
                }
                return true;
            case "top":
                if (!sender.hasPermission("idtc.stats")) {
                    MessageUtil.send(sender, plugin.getConfig(), "messages.no-permission");
                    return true;
                }
                eventManager.sendTop(sender);
                return true;
            case "wand":
                if (!sender.hasPermission("idtc.wand")) {
                    MessageUtil.send(sender, plugin.getConfig(), "messages.no-permission");
                    return true;
                }
                if (!(sender instanceof Player)) {
                    MessageUtil.send(sender, plugin.getConfig(), "messages.only-players");
                    return true;
                }
                wandService.giveWand((Player) sender);
                MessageUtil.send(sender, plugin.getConfig(), "messages.wand-given");
                return true;
            case "editor":
                if (!sender.hasPermission("idtc.editor")) {
                    MessageUtil.send(sender, plugin.getConfig(), "messages.no-permission");
                    return true;
                }
                if (!(sender instanceof Player)) {
                    MessageUtil.send(sender, plugin.getConfig(), "messages.only-players");
                    return true;
                }
                editorMenuManager.openProfileList((Player) sender);
                MessageUtil.send(sender, plugin.getConfig(), "messages.editor-opened");
                return true;
            case "create":
                if (!sender.hasPermission("idtc.admin")) {
                    MessageUtil.send(sender, plugin.getConfig(), "messages.no-permission");
                    return true;
                }
                if (!(sender instanceof Player)) {
                    MessageUtil.send(sender, plugin.getConfig(), "messages.only-players");
                    return true;
                }
                if (args.length < 2) {
                    MessageUtil.send(sender, plugin.getConfig(), "messages.create-usage");
                    return true;
                }
                if (eventManager.profileExists(args[1])) {
                    MessageUtil.send(sender, plugin.getConfig(), "messages.create-failed");
                    return true;
                }
                Player createPlayer = (Player) sender;
                wandService.setPendingCreate(createPlayer, args[1]);
                wandService.giveWand(createPlayer);
                MessageUtil.send(sender, plugin.getConfig(), "messages.create-wand-prompt");
                return true;
            case "list":
                List<String> profiles = eventManager.listCoreProfiles();
                if (profiles.isEmpty()) {
                    MessageUtil.send(sender, plugin.getConfig(), "messages.list-empty");
                    return true;
                }
                String current = eventManager.getCurrentCoreName();
                sender.sendMessage(MessageUtil.colorize(MessageUtil.get(plugin.getConfig(), "messages.list-header")));
                for (String p : profiles) {
                    String line = MessageUtil.get(plugin.getConfig(), "messages.list-line")
                        .replace("%name%", p)
                        .replace("%current%", p.equalsIgnoreCase(current) ? " <active>" : "");
                    sender.sendMessage(MessageUtil.colorize(line));
                }
                return true;
            case "delete":
                if (!sender.hasPermission("idtc.admin")) {
                    MessageUtil.send(sender, plugin.getConfig(), "messages.no-permission");
                    return true;
                }
                if (args.length < 2) {
                    MessageUtil.send(sender, plugin.getConfig(), "messages.delete-usage");
                    return true;
                }
                boolean deleted = eventManager.deleteCoreProfile(args[1]);
                if (!deleted) {
                    MessageUtil.send(sender, plugin.getConfig(), "messages.delete-failed");
                    return true;
                }
                String deleteMsg = MessageUtil.get(plugin.getConfig(), "messages.delete-success")
                    .replace("%name%", args[1]);
                sender.sendMessage(MessageUtil.colorize(deleteMsg));
                return true;
            default:
                sendHelp(sender);
                return true;
        }
    }

    private void sendHelp(CommandSender sender) {
        MessageUtil.send(sender, plugin.getConfig(), "messages.usage");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> subs = new ArrayList<>();
            Collections.addAll(subs, "start", "stop", "setcore", "reward", "reload", "stats", "top", "wand", "editor", "create", "list", "delete");
            return subs;
        }
        if (args.length == 2 && "delete".equalsIgnoreCase(args[0])) {
            return eventManager.listCoreProfiles();
        }
        return Collections.emptyList();
    }
}


