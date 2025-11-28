package com.willy.combatexpanded.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.util.Vector;
import com.willy.combatexpanded.CombatExpanded;
import com.willy.combatexpanded.manager.DashManager;
import com.willy.combatexpanded.manager.StaminaManager;

public record DashListener(DashManager dashManager, StaminaManager staminaManager, CombatExpanded plugin) implements Listener {

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!plugin.hasArtifice(player) || plugin.isPluginEnabled() || player.isFlying() || player.getVehicle() != null)
            return;

        Vector from = event.getFrom().toVector();
        Vector to = event.getTo().toVector();
        Vector diff = to.clone().subtract(from);
        diff.setY(0);

        dashManager.onPlayerMove(player, diff);
    }

    @EventHandler
    public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        if (!plugin.hasArtifice(player) || plugin.isPluginEnabled() || player.isFlying() || player.getVehicle() != null)
            return;

        if (!event.isSneaking()) return;

        if (!staminaManager.hasEnoughForDash(player)) {
            return;
        }

        boolean dashed = dashManager.performDash(player);
        if (dashed) {
            staminaManager.useDashStamina(player);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        dashManager.onPlayerQuit(event.getPlayer().getUniqueId());
    }
}
