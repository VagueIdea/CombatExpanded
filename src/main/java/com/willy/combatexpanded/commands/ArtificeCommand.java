package com.willy.combatexpanded.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import com.willy.combatexpanded.CombatExpanded;

public record ArtificeCommand(CombatExpanded plugin) implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {

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
