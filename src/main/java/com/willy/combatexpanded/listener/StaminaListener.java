package com.willy.combatexpanded.listener;

import com.willy.combatexpanded.CombatExpanded;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.Map;
import java.util.UUID;

public class StaminaListener implements Listener {

    private final Map<UUID, Long> lastMovedTime;

    public StaminaListener(Map<UUID, Long> lastMovedTime) {
        this.lastMovedTime = lastMovedTime;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        if (!CombatExpanded.getInstance().hasArtifice(player) || !CombatExpanded.getInstance().isPluginEnabled() || player.isFlying() || player.getVehicle() != null) return;

        double distanceSquared = event.getFrom().distanceSquared(event.getTo());
        double thresholdSquared = 0.01; // about 0.1 blocks

        if (distanceSquared > thresholdSquared) {
            lastMovedTime.put(uuid, System.currentTimeMillis());
        }
    }
}
