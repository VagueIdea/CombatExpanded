package com.willy.combatexpanded.listener;

import com.willy.combatexpanded.CombatExpanded;
import com.willy.combatexpanded.manager.GrappleManager;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;
import org.bukkit.GameMode;

public class GrappleListener implements Listener {

    private final GrappleManager grappleManager;

    public GrappleListener(GrappleManager grappleManager) {
        this.grappleManager = grappleManager;
    }

    @EventHandler
    public void onPlayerSwapHands(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();

        if (!CombatExpanded.getInstance().hasArtifice(player) || !CombatExpanded.getInstance().isPluginEnabled() || player.isFlying() || player.getVehicle() != null) return;
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
        if (!CombatExpanded.getInstance().hasArtifice(player) || !CombatExpanded.getInstance().isPluginEnabled() || player.isFlying() || player.getVehicle() != null) return;
        grappleManager.cancelGrapple(player);
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        if (!CombatExpanded.getInstance().hasArtifice(player) || !CombatExpanded.getInstance().isPluginEnabled() || player.isFlying() || player.getVehicle() != null) return;
        grappleManager.cancelGrapple(player);
    }
}
