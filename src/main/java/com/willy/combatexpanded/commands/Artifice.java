package com.willy.combatexpanded.commands;

import com.willy.combatexpanded.CombatExpanded;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class Artifice implements CommandExecutor {

    private final CombatExpanded plugin;

    public Artifice(CombatExpanded plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can run this command.");
            return true;
        }

        plugin.toggleArtifice(player);

        if (plugin.hasArtifice(player)) {
            player.sendMessage("§aArtifice enabled!");
        } else {
            player.sendMessage("§cArtifice disabled.");
        }

        return true;
    }
}
