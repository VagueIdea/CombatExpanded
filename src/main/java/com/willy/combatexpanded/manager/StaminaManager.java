package com.willy.combatexpanded.manager;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import com.willy.combatexpanded.CombatExpanded;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class StaminaManager {

    // Configurable fields
    private double maxStamina;
    private double normalRegenAmount;
    private double stillRegenAmount;
    private double dashCost;
    private double slamCost;

    private static final long REGEN_INTERVAL_TICKS = 5L;
    private static final long STILL_THRESHOLD_MS = 500;

    private final Map<UUID, Double> staminaMap = new HashMap<>();
    private final Map<UUID, Long> lastMovedTime;
    private final CombatExpanded plugin;

    public StaminaManager(Map<UUID, Long> lastMovedTime, CombatExpanded plugin) {
        this.lastMovedTime = lastMovedTime;
        this.plugin = plugin;
        reloadConfigValues();
        startRegenTask();
    }

    /** Reload config values from CombatExpanded config.yml */
    public void reloadConfigValues() {
        CombatExpanded plugin = CombatExpanded.getInstance();
        maxStamina = plugin.getConfig().getDouble("stamina.max-stamina", 4);
        normalRegenAmount = plugin.getConfig().getDouble("stamina.normal-regen", 0.125);
        stillRegenAmount = plugin.getConfig().getDouble("stamina.still-regen", 0.25);
        dashCost = plugin.getConfig().getDouble("stamina.dash-cost", 1);
        slamCost = plugin.getConfig().getDouble("stamina.slam-cost", 1);
    }

    // =============================
    // ==== Public Query Methods ===
    // =============================
    public double getMaxStamina() {
        return maxStamina;
    }

    public double getStamina(Player player) {
        return staminaMap.getOrDefault(player.getUniqueId(), maxStamina);
    }

    public boolean hasEnoughStamina(Player player, double cost) {
        return getStamina(player) >= cost;
    }

    public boolean hasEnoughForDash(Player player) { return hasEnoughStamina(player, dashCost); }
    public boolean hasEnoughForSlam(Player player) { return hasEnoughStamina(player, slamCost); }

    // ============================
    // ==== Use/Consume Methods ===
    // ============================

    public void useDashStamina(Player player) {
        useStamina(player, dashCost);
    }
    public void useSlamStamina(Player player) {
        useStamina(player, slamCost);
    }

    public void useStamina(Player player, double cost) {
        if (!hasEnoughStamina(player, cost)) return;
        consumeStamina(player, cost);
    }

    private void consumeStamina(Player player, double amount) {
        UUID uuid = player.getUniqueId();
        double currentStamina = staminaMap.getOrDefault(uuid, maxStamina);
        double newStamina = Math.max(0, currentStamina - amount);
        staminaMap.put(uuid, newStamina);
    }

    // ============================
    // ====== Regen Handling ======
    // ============================

    private void startRegenTask() {
        new BukkitRunnable() {
            @Override
            public void run() {

                for (Player player : Bukkit.getOnlinePlayers()) {
                    UUID uuid = player.getUniqueId();
                    if (plugin.isPluginEnabled()) continue;
                    double currentStamina = staminaMap.getOrDefault(uuid, maxStamina);
                    if (currentStamina >= maxStamina) continue;

                    long now = System.currentTimeMillis();
                    long lastMoved = lastMovedTime.getOrDefault(uuid, 0L);
                    boolean isStandingStill = (now - lastMoved) >= STILL_THRESHOLD_MS;

                    double regenAmount = isStandingStill ? stillRegenAmount : normalRegenAmount;
                    double newStamina = Math.min(maxStamina, currentStamina + regenAmount);
                    staminaMap.put(uuid, newStamina);
                }
            }
        }.runTaskTimer(CombatExpanded.getInstance(), 0L, REGEN_INTERVAL_TICKS);
    }
}
