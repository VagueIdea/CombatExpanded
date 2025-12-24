package com.willy.combatexpanded.manager;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.*;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import com.willy.combatexpanded.CombatExpanded;

import java.util.*;

public class GrappleManager {

    private final CombatExpanded plugin;

    // Config values
    private long pendingExpire;
    private long timeout;

    private final double pullSpeed = 1.5;
    private final double smoothness = 0.25;
    private final double arrivalDistance = 1.0;

    public String tagNormal = "grapple";
    public String tagReverse = "reverse_grapple";

    // Grapple state
    private final Map<UUID, LivingEntity> grappleTargets = new HashMap<>();
    private final Map<UUID, LivingEntity> pendingGrappleTargets = new HashMap<>();
    private final Map<UUID, Integer> grappleTasks = new HashMap<>();
    private final Map<UUID, Integer> pendingTasks = new HashMap<>();
    private final Map<UUID, Integer> grappleTimeoutTasks = new HashMap<>();

    // Reverse grapple state
    private final Map<UUID, UUID> reverseTargets = new HashMap<>();

    // Active tethers for grapples
    private final Map<UUID, List<BlockDisplay>> activeTethers = new HashMap<>();

    public GrappleManager(CombatExpanded plugin) {
        this.plugin = plugin;
        reloadConfigValues();
    }

    public void reloadConfigValues() {
        pendingExpire = plugin.getConfig().getLong("grapple.pending-expire", 60L);
        timeout = plugin.getConfig().getLong("grapple.timeout", 60L);
    }

    // Pending grapple target
    public void setPendingGrappleTarget(Player player, LivingEntity target) {
        if (target == null || !target.isValid()) return;

        UUID uuid = player.getUniqueId();
        if (pendingTasks.containsKey(uuid)) Bukkit.getScheduler().cancelTask(pendingTasks.remove(uuid));

        pendingGrappleTargets.put(uuid, target);

        int taskId = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (pendingGrappleTargets.containsKey(uuid)) {
                pendingGrappleTargets.remove(uuid);
                clearReverse(player);
            }
            pendingTasks.remove(uuid);
        }, pendingExpire).getTaskId();

        pendingTasks.put(uuid, taskId);
    }

    public LivingEntity getPendingGrappleTarget(Player player) {
        return pendingGrappleTargets.get(player.getUniqueId());
    }

    public boolean hasPendingGrapple(Player player) {
        return pendingGrappleTargets.containsKey(player.getUniqueId());
    }

    public boolean tryStartChainPull(Player player) {
        LivingEntity target = getPendingGrappleTarget(player);
        if (target == null || !target.isValid()) {
            cancelGrapple(player);
            return false;
        }
        startGrapple(player, target);
        return true;
    }

    // Main grapple logic
    public void startGrapple(Player player, LivingEntity target) {
        UUID uuid = player.getUniqueId();

        if (grappleTimeoutTasks.containsKey(uuid)) Bukkit.getScheduler().cancelTask(grappleTimeoutTasks.remove(uuid));
        if (pendingTasks.containsKey(uuid)) Bukkit.getScheduler().cancelTask(pendingTasks.remove(uuid));
        pendingGrappleTargets.remove(uuid);
        if (grappleTargets.containsKey(uuid)) return;

        grappleTargets.put(uuid, target);

        int taskId = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || !target.isValid()) {
                    cancelGrapple(player);
                    return;
                }

                Vector playerLoc = player.getLocation().toVector();
                Vector targetLoc = target.getLocation().toVector();
                boolean reverse = isReverse(player, target);

                if (reverse) {
                    Vector desiredVelocity = playerLoc.clone().subtract(targetLoc).normalize().multiply(pullSpeed);
                    target.setVelocity(lerp(target.getVelocity(), desiredVelocity, smoothness));
                    target.getWorld().playSound(player.getLocation(), Sound.BLOCK_CHAIN_STEP, 0.4f, 0.6f);
                } else {
                    Vector desiredVelocity = targetLoc.clone().subtract(playerLoc).normalize().multiply(pullSpeed);
                    player.setVelocity(lerp(player.getVelocity(), desiredVelocity, smoothness));
                    player.setFallDistance(0);
                    player.getWorld().playSound(player.getLocation(), Sound.BLOCK_CHAIN_STEP, 0.4f, 0.6f);
                }

                updateTether(player, target);

                double distance = playerLoc.distance(targetLoc);
                if (distance <= arrivalDistance || target.isDead()) {
                    cancelGrapple(player);
                }
            }
        }.runTaskTimer(plugin, 0L, 1L).getTaskId();

        grappleTasks.put(uuid, taskId);

        int timeoutId = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (isGrappling(player)) {
                cancelGrapple(player);
            }
            grappleTimeoutTasks.remove(uuid);
        }, timeout).getTaskId();

        grappleTimeoutTasks.put(uuid, timeoutId);
    }

    private Vector lerp(Vector current, Vector target, double t) {
        return current.clone().multiply(1 - t).add(target.clone().multiply(t));
    }

    // State checks and cancel
    public boolean isGrappling(Player player) {
        return grappleTargets.containsKey(player.getUniqueId());
    }

    public void cancelGrapple(Player player) {
        UUID uuid = player.getUniqueId();
        if (grappleTasks.containsKey(uuid)) Bukkit.getScheduler().cancelTask(grappleTasks.remove(uuid));
        if (pendingTasks.containsKey(uuid)) Bukkit.getScheduler().cancelTask(pendingTasks.remove(uuid));
        if (grappleTimeoutTasks.containsKey(uuid)) Bukkit.getScheduler().cancelTask(grappleTimeoutTasks.remove(uuid));

        grappleTargets.remove(uuid);
        pendingGrappleTargets.remove(uuid);
        clearReverse(player);

        removeTether(player);
    }

    //* Simplified Reverse Grapple Helpers
    public void setReverse(Player player, LivingEntity target) {
        target.getScoreboardTags().remove(tagNormal);
        target.getScoreboardTags().add(tagReverse);
        reverseTargets.put(player.getUniqueId(), target.getUniqueId());
    }

    public void clearReverse(Player player) {
        UUID targetId = reverseTargets.remove(player.getUniqueId());
        if (targetId != null) {
            Entity target = Bukkit.getEntity(targetId);
            if (target instanceof LivingEntity living) {
                living.getScoreboardTags().remove(tagReverse);
            }
        }
    }

    public boolean isReverse(Player player, LivingEntity target) {
        return target.getUniqueId().equals(reverseTargets.get(player.getUniqueId()));
    }

    public boolean hasReverse(Player player) {
        return reverseTargets.containsKey(player.getUniqueId());
    }

    // Grapple tether handling
    private void updateTether(Player player, LivingEntity target) {
        World world = player.getWorld();
        BoundingBox playerBB = player.getBoundingBox();
        double playerHalfHeight = playerBB.getHeight() / 2.0;
        Vector start = playerBB.getCenter();

        BoundingBox targetBB = target.getBoundingBox();
        Vector end = targetBB.getCenter();

        List<BlockDisplay> tetherSegments = activeTethers.computeIfAbsent(player.getUniqueId(), k -> new ArrayList<>());
        while (tetherSegments.isEmpty()) {
            BlockDisplay display = (BlockDisplay) world.spawnEntity(start.toLocation(world), EntityType.BLOCK_DISPLAY);
            display.setBlock(Material.CHAIN.createBlockData());
            tetherSegments.add(display);
        }
        while (tetherSegments.size() > 1) {
            BlockDisplay display = tetherSegments.removeLast();
            if (!display.isDead()) display.remove();
        }

        BlockDisplay chainDisplay = tetherSegments.getFirst();
        if (chainDisplay.getPassengers().isEmpty()) player.addPassenger(chainDisplay);

        Vector dir = end.clone().subtract(start);
        float length = (float) dir.length();
        float dx = (float) dir.getX();
        float dy = (float) dir.getY();
        float dz = (float) dir.getZ();

        float yaw = (float) Math.atan2(-dx, dz);
        float pitch = (float) Math.atan2(dy, Math.sqrt(dx * dx + dz * dz));

        Transformation transform = new Transformation(
                new Vector3f(0, (float) -playerHalfHeight, 0),
                new Quaternionf().rotateY(-yaw).rotateX(-pitch - (float) Math.toRadians(90)),
                new Vector3f(0.15f, -length, 0.15f),
                new Quaternionf()
        );

        chainDisplay.setTransformation(transform);
    }

    private void removeTether(Player player) {
        List<BlockDisplay> tetherSegments = activeTethers.remove(player.getUniqueId());
        if (tetherSegments != null) {
            for (BlockDisplay display : tetherSegments) {
                if (!display.isDead()) display.remove();
            }
        }
    }
}
