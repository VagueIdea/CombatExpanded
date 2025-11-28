package com.willy.combatexpanded.commands;

import com.willy.combatexpanded.CombatExpanded;
import com.willy.combatexpanded.permission.Permissions;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public record InfoCommand(CombatExpanded plugin) implements CommandExecutor {
    private static final String NEW_LINE = "\n";

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] strings) {
        StringBuilder infoToShow = new StringBuilder();
        infoToShow.append("§6Combat Expanded v1.0 By WornOutWilly");

        if (sender.hasPermission(Permissions.ARTIFICE)) {
            infoToShow.append(NEW_LINE).append("§6/artifice | /ce artifice - §7Toggles The New Combat System For Players");
            infoToShow.append(NEW_LINE).append("§7Sneak While Moving Or In The Air To Dash");
            infoToShow.append(NEW_LINE).append("§7Hold Sneak After Air Dashing To Slam");
            infoToShow.append(NEW_LINE).append("§7Swap Offhand After Dashing Into A Mob To Pull You Towards Them");
            infoToShow.append(NEW_LINE).append("§7Swap Offhand After Dashing & Slamming A Mob To Pull Them Towards You");
        }

        if (sender.hasPermission(Permissions.ADMIN)) {
            infoToShow.append(NEW_LINE).append("§6/ce enable | /ce disable - §7Turn On Or Off The Plugin");
            infoToShow.append(NEW_LINE).append("§6/ce reload - §7Reloads The Plugin For Configuration Edits");
        }

        sender.sendMessage(infoToShow.toString());
        return true;
    }
}
