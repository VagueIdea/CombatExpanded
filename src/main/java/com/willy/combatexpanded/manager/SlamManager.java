package com.willy.combatexpanded.manager;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import com.willy.combatexpanded.CombatExpanded;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SlamManager {

    private final CombatExpanded plugin;

    // Config values
    private double fallSpeed;
    private double aoeRadius;
    private double knockbackStrength;
    private double damage;

    private final Map<UUID, Boolean> canSlamThisDash = new HashMap<>();
    private final Map<UUID, Boolean> isSlamming = new HashMap<>();

    public SlamManager(CombatExpanded plugin) {
        this.plugin = plugin;
        reloadConfigValues();
    }

    public void reloadConfigValues() {
        fallSpeed = plugin.getConfig().getDouble("slam.fall-speed", -3.0);
        aoeRadius = plugin.getConfig().getDouble("slam.aoe-radius", 4.0);
        knockbackStrength = plugin.getConfig().getDouble("slam.knockback-strength", 2.0);
        damage = plugin.getConfig().getDouble("slam.damage", 15.0);
    }

    public void markCanSlam(UUID uuid) {
        canSlamThisDash.put(uuid, true);
    }

    public boolean canSlam(UUID uuid) {
        return canSlamThisDash.getOrDefault(uuid, false);
    }

    public boolean isSlamming(UUID uuid) {
        return isSlamming.getOrDefault(uuid, false);
    }

    public void reset(UUID uuid) {
        canSlamThisDash.put(uuid, false);
        isSlamming.put(uuid, false);
    }

    public void startSlam(Player player) {
        UUID uuid = player.getUniqueId();
        if (!canSlam(uuid)) return;

        isSlamming.put(uuid, true);
        canSlamThisDash.put(uuid, false);

        Vector velocity = player.getVelocity();
        velocity.setY(fallSpeed);
        player.setVelocity(velocity);
        Location loc = player.getLocation();

        player.playSound(loc, Sound.ENTITY_IRON_GOLEM_ATTACK, 1.0f, 0.8f);
        player.playSound(loc, Sound.ITEM_TRIDENT_THROW, 0.2f, 0.2f);

        // Spawn particles continuously while slamming
        Bukkit.getScheduler().runTaskTimer(plugin, task -> {
            if (!isSlamming(player.getUniqueId())) {
                task.cancel();
                return;
            }

            if (player.isOnGround()) {
                onLand(player);
                task.cancel();
                return;
            }

            // Spawn particle at current player position without mutating anything
            Location particleLoc = player.getLocation().add(0, 1, 0);
            player.getWorld().spawnParticle(
                    Particle.TRIAL_SPAWNER_DETECTION_OMINOUS,
                    particleLoc,
                    15, 0.3, 0.5, 0.3, 0.01
            );
            Location particleLoc2 = player.getLocation().add(0, 0.5, 0);
            player.getWorld().spawnParticle(
                    Particle.TRIAL_SPAWNER_DETECTION_OMINOUS,
                    particleLoc2,
                    15, 0.3, 0.5, 0.3, 0.01
            );
        }, 0L, 1L);
    }

    public void cancelSlam(Player player) {
        UUID uuid = player.getUniqueId();
        if (!isSlamming(uuid)) return;

        isSlamming.put(uuid, false);

        Vector vel = player.getVelocity();
        vel.setY(0);
        player.setVelocity(vel);
        player.setFallDistance(0f);

    }

    public void onLand(Player player) {
        UUID uuid = player.getUniqueId();
        if (!isSlamming(uuid)) return;
        isSlamming.put(uuid, false);
        player.setFallDistance(0f);

        Bukkit.getScheduler().runTask(plugin, () -> {
            Location loc = player.getLocation();

            // --- Raycast downward from slightly above bounding box bottom ---
            double startY = player.getBoundingBox().getMinY() - 0.1;
            Block block = null;

            for (double offset = 0; offset <= 1.0; offset += 0.05) {
                block = loc.getWorld().getBlockAt(
                        (int) Math.floor(loc.getX()),
                        (int) Math.floor(startY - offset),
                        (int) Math.floor(loc.getZ())
                );
                if (!block.getType().isAir()) break;
            }

            BlockData blockData;
            if (block.getType().isAir()) {
                blockData = Material.DIRT.createBlockData();
            } else {
                blockData = block.getBlockData();
            }

            player.getWorld().playSound(loc, Sound.ITEM_MACE_SMASH_GROUND_HEAVY, 0.7f, 0.8f);
            player.getWorld().playSound(loc, Sound.BLOCK_BELL_USE, 0.4f, 0.4f);
            player.getWorld().spawnParticle(
                    Particle.WAX_OFF,
                    loc,
                    15, 1, 1, 1, 0.01
            );

            final int baseParticleCount = 0;      // particles at inner radius
            final int maxParticleCount = 24;      // particles at outer radius
            final int totalTicks = 6;
            final double maxRadius = aoeRadius;
            final double radiusIncrement = maxRadius / totalTicks;

            new BukkitRunnable() {
                int tick = 0;

                @Override
                public void run() {
                    double currentRadius = radiusIncrement * tick;

                    // Particle count increases with radius
                    int particleCount = baseParticleCount +
                            (int)((maxParticleCount - baseParticleCount) * (currentRadius / maxRadius));

                    for (int i = 0; i < particleCount; i++) {
                        double angle = 2 * Math.PI * i / particleCount;
                        double x = loc.getX() + currentRadius * Math.cos(angle);
                        double z = loc.getZ() + currentRadius * Math.sin(angle);
                        double y = loc.getY();

                        player.getWorld().spawnParticle(
                                Particle.DUST_PILLAR,
                                new Location(loc.getWorld(), x, y, z),
                                8,
                                0, 0, 0,
                                0,
                                blockData
                        );
                    }

                    tick++;
                    if (tick > totalTicks) cancel();
                }
            }.runTaskTimer(plugin, 0L, 1L);

            // --- Grapple & knockback logic ---
            GrappleManager gm = plugin.getGrappleManager();
            LivingEntity pending = gm.getPendingGrappleTarget(player);

            for (Entity entity : loc.getNearbyEntities(aoeRadius, aoeRadius, aoeRadius)) {
                if (entity instanceof LivingEntity target && !target.equals(player)) {
                    Vector knockbackDir = target.getLocation().toVector().subtract(loc.toVector());
                    knockbackDir.setY(0);
                    knockbackDir.normalize().multiply(knockbackStrength);
                    knockbackDir.setY(1.0);
                    target.setVelocity(knockbackDir);

                    target.damage(damage, player);

                    boolean isIntendedTarget = (pending != null && pending.isValid() && pending.equals(target));
                    boolean hasNormalTag = target.getScoreboardTags().contains(plugin.getConfig().getString("grapple.tag-normal", "grapple"));

                    if (isIntendedTarget || hasNormalTag) {
                        // Apply reverse grapple
                        gm.setReverse(player, target);

                        // Particles & sound
                        player.getWorld().playSound(loc, Sound.BLOCK_TRIAL_SPAWNER_SPAWN_ITEM_BEGIN, 1.4f, 0.8f);
                        player.getWorld().spawnParticle(Particle.SCRAPE, target.getLocation(), 15, 1, 1, 1, 0.01);
                    }
                }
            }
        });
    }
}

