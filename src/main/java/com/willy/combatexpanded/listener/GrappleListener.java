package com.willy.combatexpanded.listener;

import org.bukkit.GameMode;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import com.willy.combatexpanded.CombatExpanded;
import com.willy.combatexpanded.manager.GrappleManager;

public record GrappleListener(GrappleManager grappleManager, CombatExpanded plugin) implements Listener {

    @EventHandler
    public void onPlayerSwapHands(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();

        if (!plugin.hasArtifice(player) || plugin.isPluginEnabled() || player.isFlying() || player.getVehicle() != null)
            return;
        if (!grappleManager.hasPendingGrapple(player)) return;

        // 🔹 Distance check before starting grapple
        LivingEntity target = grappleManager.getPendingGrappleTarget(player);
        if (target == null || !target.isValid()) {
            grappleManager.cancelGrapple(player);
            return;
        }

        double distance = player.getLocation().distance(target.getLocation());
        if (distance > 50) {
            grappleManager.cancelGrapple(player);
            return;
        }

        boolean started = grappleManager.tryStartChainPull(player);
        if (started) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE) {
            player.setAllowFlight(true);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (!plugin.hasArtifice(player) || plugin.isPluginEnabled() || player.isFlying() || player.getVehicle() != null)
            return;
        grappleManager.cancelGrapple(player);
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        if (!plugin.hasArtifice(player) || plugin.isPluginEnabled() || player.isFlying() || player.getVehicle() != null)
            return;
        grappleManager.cancelGrapple(player);
    }
}
