package com.willy.combatexpanded.commands;

import com.willy.combatexpanded.CombatExpanded;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CE implements CommandExecutor {

    private final CombatExpanded plugin;

    public CE(CombatExpanded plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!sender.hasPermission("combatexpanded.admin")) {
            sender.sendMessage("§cYou do not have permission to use this command.");
            return true;
        }

        if (args.length == 0) {
            return false;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can run this command.");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "enable":
                plugin.setPluginEnabled(true);
                sender.sendMessage("§aCombatExpanded enabled!");
                break;
            case "disable":
                plugin.setPluginEnabled(false);
                sender.sendMessage("§cCombatExpanded disabled!");
                break;
            case "reload":
                plugin.reloadPlugin(sender);
                break;
            default:
                sender.sendMessage("§eUsage: /ce <enable|disable|reload>");
        }
        return true;
    }
}

