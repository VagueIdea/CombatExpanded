package com.willy.combatexpanded;

import com.willy.combatexpanded.commands.CE;
import com.willy.combatexpanded.commands.Artifice;

import com.willy.combatexpanded.listener.SlamListener;
import com.willy.combatexpanded.listener.StaminaListener;
import com.willy.combatexpanded.listener.DashListener;
import com.willy.combatexpanded.listener.GrappleListener;

import com.willy.combatexpanded.manager.DashManager;
import com.willy.combatexpanded.manager.SlamManager;
import com.willy.combatexpanded.manager.StaminaManager;
import com.willy.combatexpanded.manager.GrappleManager;

import com.willy.combatexpanded.placeholder.CombatExpandedPlaceholder;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class CombatExpanded extends JavaPlugin {

    private final Map<UUID, Long> lastMovedTime = new HashMap<>();

    private StaminaManager staminaManager;
    private DashManager dashManager;
    private SlamManager slamManager;
    private GrappleManager grappleManager;

    private StaminaListener staminaListener;
    private DashListener dashListener;
    private SlamListener slamListener;
    private GrappleListener grappleListener;

    private static CombatExpanded instance;

    // Global enable/disable state
    private boolean pluginEnabled = true;
    private boolean hasArtifice;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        pluginEnabled = getConfig().getBoolean("plugin-enabled", true);

        staminaManager = new StaminaManager(lastMovedTime);
        dashManager = new DashManager(this);
        slamManager = new SlamManager(this);
        grappleManager = new GrappleManager(this);

        staminaListener = new StaminaListener(lastMovedTime);
        slamListener = new SlamListener(slamManager, dashManager, staminaManager);
        dashListener = new DashListener(dashManager, staminaManager);
        grappleListener = new GrappleListener(grappleManager);

        dashManager.setSlamListener(slamListener);

        getServer().getPluginManager().registerEvents(staminaListener, this);
        getServer().getPluginManager().registerEvents(dashListener, this);
        getServer().getPluginManager().registerEvents(slamListener, this);
        getServer().getPluginManager().registerEvents(grappleListener, this);

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new CombatExpandedPlaceholder(this).register();
            getLogger().info("PlaceholderAPI hook registered!");
        }

        getLogger().info("CombatExpanded enabled.");

        // Register commands
        Objects.requireNonNull(getCommand("ce")).setExecutor(new CE(this));
        Objects.requireNonNull(getCommand("artifice")).setExecutor(new Artifice(this));
    }

    public void reloadPlugin(CommandSender sender) {
        reloadConfig();
        sender.sendMessage("§aCombatExpanded config reloaded!");

        pluginEnabled = getConfig().getBoolean("plugin-enabled", true);
        staminaManager.reloadConfigValues();
        dashManager.reloadConfigValues();
        slamManager.reloadConfigValues();
        grappleManager.reloadConfigValues();

        sender.sendMessage("§aAll managers updated with new config values.");
    }

    public static CombatExpanded getInstance() { return instance; }

    public StaminaManager getStaminaManager() { return staminaManager; }
    public DashManager getDashManager() { return dashManager; }
    public SlamManager getSlamManager() { return slamManager; }
    public GrappleManager getGrappleManager() { return grappleManager; }
    public StaminaListener getStaminaListener() { return staminaListener; }

    // Plugin enable/disable state
    public boolean isPluginEnabled() { return pluginEnabled; }
    public void setPluginEnabled(boolean enabled) {
        this.pluginEnabled = enabled;
        getConfig().set("plugin-enabled", enabled);
        saveConfig();
    }


    private final Map<UUID, Boolean> artificeToggle = new HashMap<>();
    public boolean hasArtifice(Player player) {
        return artificeToggle.getOrDefault(player.getUniqueId(), true);
    }

    public void toggleArtifice(Player player) {
        UUID id = player.getUniqueId();
        boolean newValue = !hasArtifice(player);
        artificeToggle.put(id, newValue);
    }

}
