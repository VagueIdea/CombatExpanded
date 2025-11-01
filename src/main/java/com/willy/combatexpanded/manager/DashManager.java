package com.willy.combatexpanded.manager;

import com.willy.combatexpanded.CombatExpanded;
import com.willy.combatexpanded.listener.SlamListener;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DashManager {

    private final CombatExpanded plugin;

    // Configurable fields
    private double DASH_SPEED;
    private double AIR_DASH_SPEED;
    private double MIN_MOVEMENT_THRESHOLD;
    private long DASH_COOLDOWN_TICKS;
    private long DASH_GRACE_TICKS;
    private long DASH_HIT_WINDOW_TICKS;
    private double DASH_DAMAGE;
    private double DASH_KNOCKBACK;
    private double DASH_KNOCKBACK_Y;

    // Player state tracking
    private final Map<UUID, Vector> lastDirections = new HashMap<>();
    private final Map<UUID, Boolean> hasDashedThisJump = new HashMap<>();
    private final Map<UUID, Long> lastDashTick = new HashMap<>();
    private final Map<UUID, Long> dashGraceTick = new HashMap<>();
    private final Map<UUID, Long> dashHitWindow = new HashMap<>();

    private SlamListener slamListener;

    public DashManager(CombatExpanded plugin) {
        this.plugin = plugin;
        reloadConfigValues();
    }

    public void reloadConfigValues() {
        DASH_SPEED = plugin.getConfig().getDouble("dash.speed", 2.0);
        AIR_DASH_SPEED = plugin.getConfig().getDouble("dash.air-speed", 1.2);
        MIN_MOVEMENT_THRESHOLD = plugin.getConfig().getDouble("dash.min-threshold", 0.15);
        DASH_COOLDOWN_TICKS = plugin.getConfig().getLong("dash.cooldown", 5);
        DASH_GRACE_TICKS = plugin.getConfig().getLong("dash.grace-period", 5);
        DASH_HIT_WINDOW_TICKS = plugin.getConfig().getLong("dash.hit-window", 8);
        DASH_DAMAGE = plugin.getConfig().getDouble("dash.damage", 7.5);
        DASH_KNOCKBACK = plugin.getConfig().getDouble("dash.knockback", 0.5);
        DASH_KNOCKBACK_Y = plugin.getConfig().getDouble("dash.knockback-y", 1.0);
    }

    public void setSlamListener(SlamListener slamListener) {
        this.slamListener = slamListener;
    }

    public void onPlayerMove(Player player, Vector movementDiff) {

        UUID uuid = player.getUniqueId();

        if (movementDiff.lengthSquared() > MIN_MOVEMENT_THRESHOLD * MIN_MOVEMENT_THRESHOLD) {
            lastDirections.put(uuid, movementDiff.normalize());
        } else {
            lastDirections.remove(uuid);
        }

        if (player.isOnGround()) {
            hasDashedThisJump.put(uuid, false);
        }

        checkDashCollisions(player);
    }

    public boolean hasDashedRecently(Player player, int maxTicksAgo) {

        UUID uuid = player.getUniqueId();
        if (!lastDashTick.containsKey(uuid)) return false;
        long currentTick = Bukkit.getCurrentTick();
        return (currentTick - lastDashTick.get(uuid)) <= maxTicksAgo;
    }

    public boolean canDash(Player player) {

        UUID uuid = player.getUniqueId();
        long currentTick = Bukkit.getCurrentTick();

        if (lastDashTick.containsKey(uuid) && (currentTick - lastDashTick.get(uuid)) < DASH_COOLDOWN_TICKS) return false;
        if (dashGraceTick.containsKey(uuid) && currentTick < dashGraceTick.get(uuid)) return false;
        return !Boolean.TRUE.equals(hasDashedThisJump.get(uuid));
    }

    public boolean performDash(Player player) {
        if (!canDash(player)) return false;

        // Cancel any active grapple chain pull before dashing
        if (plugin.getGrappleManager().isGrappling(player)) {
            plugin.getGrappleManager().cancelGrapple(player);
        }

        UUID uuid = player.getUniqueId();
        Vector dashVector;

        if (player.isOnGround()) {
            Vector lastDir = lastDirections.get(uuid);
            if (lastDir == null || lastDir.length() < MIN_MOVEMENT_THRESHOLD) return false;
            dashVector = lastDir.clone().multiply(DASH_SPEED);
        } else {
            dashVector = player.getLocation().getDirection().normalize().multiply(AIR_DASH_SPEED);
            if (slamListener != null) slamListener.allowSlamAfterDash(player);
        }

        player.setVelocity(dashVector);
        triggerDashEffects(player);

        hasDashedThisJump.put(uuid, true);
        long currentTick = Bukkit.getCurrentTick();
        lastDashTick.put(uuid, currentTick);
        dashGraceTick.put(uuid, currentTick + DASH_GRACE_TICKS);
        dashHitWindow.put(uuid, currentTick + DASH_HIT_WINDOW_TICKS);

        return true;
    }

    private void checkDashCollisions(Player player) {

        UUID uuid = player.getUniqueId();
        Long tick = dashHitWindow.get(uuid);
        if (tick == null || Bukkit.getCurrentTick() > tick) {
            dashHitWindow.remove(uuid);
            return;
        }

        for (Entity entity : player.getNearbyEntities(1.5, 1.0, 1.5)) {
            if (!(entity instanceof LivingEntity target) || target.equals(player)) continue;
            Location loc = player.getLocation();
            Location Tloc = target.getLocation();

            Vector toTarget = target.getLocation().toVector().subtract(loc.toVector()).normalize();
            Vector dashDir = player.getVelocity().clone().normalize();
            if (dashDir.dot(toTarget) < 0.7) continue;

            target.damage(DASH_DAMAGE, player);
            Vector knockback = loc.getDirection().normalize().multiply(-DASH_KNOCKBACK).setY(DASH_KNOCKBACK_Y);
            player.setVelocity(knockback);

            player.getWorld().spawnParticle(Particle.GUST, Tloc, 3, 0.5, 0.5, 0.5, 0.01);

            if (!plugin.getGrappleManager().isReverse(player, target)) {
                player.getWorld().spawnParticle(Particle.WAX_ON, Tloc,
                        15, 1, 1, 1, 0.01);
                player.getWorld().playSound(loc, Sound.BLOCK_TRIAL_SPAWNER_OPEN_SHUTTER, 0.7f, 1.2f);
            } else {
                player.getWorld().spawnParticle(Particle.SCRAPE, Tloc,
                        15, 1, 1, 1, 0.01);
                player.getWorld().playSound(loc, Sound.BLOCK_TRIAL_SPAWNER_SPAWN_ITEM_BEGIN, 1.4f, 0.8f);
            }

            plugin.getGrappleManager().setPendingGrappleTarget(player, target);

            dashHitWindow.remove(uuid);
            break;
        }
    }

    private void triggerDashEffects(Player player) {

        Location loc = player.getLocation();
        player.playSound(loc, Sound.ENTITY_ENDER_DRAGON_FLAP, 0.6f, 1.2f);
        player.getWorld().spawnParticle(Particle.GUST, loc, 5, 0.5, 0.5, 0.5, 0.01);
    }

    public void onPlayerQuit(UUID uuid) {
        lastDirections.remove(uuid);
        hasDashedThisJump.remove(uuid);
        lastDashTick.remove(uuid);
        dashGraceTick.remove(uuid);
        dashHitWindow.remove(uuid);
    }

    public void resetDashForPlayer(Player player) {
        hasDashedThisJump.put(player.getUniqueId(), false);
    }
}
