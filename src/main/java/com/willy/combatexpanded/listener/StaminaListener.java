package com.willy.combatexpanded.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import com.willy.combatexpanded.CombatExpanded;

import java.util.Map;
import java.util.UUID;

public record StaminaListener(Map<UUID, Long> lastMovedTime, CombatExpanded plugin) implements Listener {

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        if (plugin.isPluginEnabled())
            return;

        double distanceSquared = event.getFrom().distanceSquared(event.getTo());
        double thresholdSquared = 0.01; // about 0.1 blocks

        if (distanceSquared > thresholdSquared) {
            lastMovedTime.put(uuid, System.currentTimeMillis());
        }
    }
}
