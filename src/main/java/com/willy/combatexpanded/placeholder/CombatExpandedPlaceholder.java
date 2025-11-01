package com.willy.combatexpanded.placeholder;

import com.willy.combatexpanded.CombatExpanded;
import com.willy.combatexpanded.manager.StaminaManager;
import com.willy.combatexpanded.manager.DashManager;
import com.willy.combatexpanded.manager.SlamManager;
import com.willy.combatexpanded.manager.GrappleManager;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class CombatExpandedPlaceholder extends PlaceholderExpansion {

    private final CombatExpanded plugin;

    public CombatExpandedPlaceholder(CombatExpanded plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "combatexpanded"; // %combatexpanded_<identifier>%
    }

    @Override
    public @NotNull String getAuthor() {
        return "Willy";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String identifier) {
        if (player == null) return "";

        StaminaManager staminaManager = plugin.getStaminaManager();
        DashManager dashManager = plugin.getDashManager();
        SlamManager slamManager = plugin.getSlamManager();
        GrappleManager grappleManager = plugin.getGrappleManager();

        double maxStamina = staminaManager.getMaxStamina();

        switch (identifier.toLowerCase()) {
            // --- Stamina ---
            case "stamina":
                return String.valueOf(staminaManager.getStamina(player));
            case "stamina_max":
                return String.valueOf(maxStamina);
            case "stamina_percent":
                double current = staminaManager.getStamina(player);
                double percent = (current / (double) maxStamina) * 100;
                return String.format("%.0f", percent);

            // --- Dash ---
            case "dash_ready":
                return (dashManager.canDash(player) && staminaManager.hasEnoughForDash(player)) ? "1" : "0";

            // --- Slam ---
            case "slam_ready":
                return (slamManager.canSlam(player.getUniqueId()) && staminaManager.hasEnoughForSlam(player)) ? "1" : "0";
            case "slam_active":
                return slamManager.isSlamming(player.getUniqueId()) ? "1" : "0";

            // --- Grapple ---
            case "grapple_ready":
                return grappleManager.hasPendingGrapple(player) ? "1" : "0";
            case "grapple_active":
                return grappleManager.isGrappling(player) ? "1" : "0";
            case "grapple_reverse":
                return grappleManager.hasReverse(player) ? "1" : "0";

        }

        return null;
    }
}
