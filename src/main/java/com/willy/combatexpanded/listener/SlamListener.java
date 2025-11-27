package com.willy.combatexpanded.listener;

import com.willy.combatexpanded.CombatExpanded;
import com.willy.combatexpanded.manager.DashManager;
import com.willy.combatexpanded.manager.SlamManager;
import com.willy.combatexpanded.manager.StaminaManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SlamListener implements Listener {

    private static final long SLAM_SNEAK_HOLD_DELAY_TICKS = 5;

    private final SlamManager slamManager;
    private final DashManager dashManager;
    private final StaminaManager staminaManager;

    private final Map<UUID, BukkitTask> sneakHoldTasks = new HashMap<>();

    public SlamListener(SlamManager slamManager, DashManager dashManager, StaminaManager staminaManager) {
        this.slamManager = slamManager;
        this.dashManager = dashManager;
        this.staminaManager = staminaManager;
    }

    /** Called by DashManager after a dash to mark the player ready for slam */
    public void allowSlamAfterDash(Player player) {
        UUID uuid = player.getUniqueId();
        Bukkit.getScheduler().runTaskLater(CombatExpanded.getInstance(), () -> {
            if (!player.isOnGround()) {
                slamManager.markCanSlam(uuid);
            }
        }, SLAM_SNEAK_HOLD_DELAY_TICKS);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        if (!CombatExpanded.getInstance().hasArtifice(player) || !CombatExpanded.getInstance().isPluginEnabled() || player.isFlying() || player.getVehicle() != null) return;

        if (player.isOnGround()) {
            if (slamManager.isSlamming(uuid)) {
                slamManager.onLand(player);
            }
            slamManager.reset(uuid);
            dashManager.resetDashForPlayer(player);
            cancelScheduledTask(uuid);
        }
    }

    @EventHandler
    public void onFallDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        if (slamManager.isSlamming(player.getUniqueId()) &&
                event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (!CombatExpanded.getInstance().hasArtifice(player) || !CombatExpanded.getInstance().isPluginEnabled() || player.isFlying() || player.getVehicle() != null) return;

        // Sneak released
        if (!event.isSneaking()) {
            cancelScheduledTask(uuid);
            if (slamManager.isSlamming(uuid)) {
                slamManager.cancelSlam(player);
            }
            return;
        }
        if (!player.isOnGround() && slamManager.canSlam(uuid)) {
            BukkitTask task = Bukkit.getScheduler().runTaskLater(CombatExpanded.getInstance(), () -> {
                if (!player.isSneaking()) return;

                if (slamManager.canSlam(uuid)) {
                    if (staminaManager.hasEnoughForSlam(player)) {
                        staminaManager.useSlamStamina(player);

                        // Cancel grapple if active
                        if (CombatExpanded.getInstance().getGrappleManager().isGrappling(player)) {
                            CombatExpanded.getInstance().getGrappleManager().cancelGrapple(player);
                        }
                        slamManager.startSlam(player);
                    }
                }
            }, SLAM_SNEAK_HOLD_DELAY_TICKS);
            sneakHoldTasks.put(uuid, task);
        }
    }

    private void cancelScheduledTask(UUID uuid) {
        BukkitTask task = sneakHoldTasks.remove(uuid);
        if (task != null) task.cancel();
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        cancelScheduledTask(uuid);
        slamManager.reset(uuid);
    }
}
