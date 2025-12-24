package com.willy.combatexpanded;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import com.willy.combatexpanded.commands.ArtificeCommand;
import com.willy.combatexpanded.commands.MainCommand;
import com.willy.combatexpanded.listener.DashListener;
import com.willy.combatexpanded.listener.GrappleListener;
import com.willy.combatexpanded.listener.SlamListener;
import com.willy.combatexpanded.listener.StaminaListener;
import com.willy.combatexpanded.manager.DashManager;
import com.willy.combatexpanded.manager.GrappleManager;
import com.willy.combatexpanded.manager.SlamManager;
import com.willy.combatexpanded.manager.StaminaManager;
import com.willy.combatexpanded.placeholder.CombatExpandedPlaceholder;

import java.io.*;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

public class CombatExpanded extends JavaPlugin {

    private final Map<UUID, Long> lastMovedTime = new HashMap<>();
    private final Map<UUID, Boolean> artificeToggle = new HashMap<>();
    private final Map<UUID, BukkitTask> sneakHoldTasks = new HashMap<>();
    private StaminaManager staminaManager;
    private DashManager dashManager;
    private SlamManager slamManager;
    private GrappleManager grappleManager;
    private static CombatExpanded instance;
    private boolean pluginEnabled = true;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        reloadConfig();
        copyResourceDirectory(this,"resources");
        staminaManager = new StaminaManager(lastMovedTime, this);
        dashManager = new DashManager(this);
        slamManager = new SlamManager(this);
        grappleManager = new GrappleManager(this);

        StaminaListener staminaListener = new StaminaListener(lastMovedTime, this);
        SlamListener slamListener = new SlamListener(slamManager, dashManager, staminaManager, sneakHoldTasks,this);
        DashListener dashListener = new DashListener(dashManager, staminaManager, this);
        GrappleListener grappleListener = new GrappleListener(grappleManager, this);

        dashManager.setSlamListener(slamListener);

        getServer().getPluginManager().registerEvents(staminaListener, this);
        getServer().getPluginManager().registerEvents(dashListener, this);
        getServer().getPluginManager().registerEvents(slamListener, this);
        getServer().getPluginManager().registerEvents(grappleListener, this);

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new CombatExpandedPlaceholder(this).register();
            getLogger().info("PlaceholderAPI hook registered!");
        }

        applyConfig();
        getLogger().info("CombatExpanded enabled.");

        // Register commands
        Objects.requireNonNull(getCommand("ce")).setExecutor(new MainCommand(this));
        Objects.requireNonNull(getCommand("artifice")).setExecutor(new ArtificeCommand(this));
    }

    public static void copyResourceDirectory(JavaPlugin plugin, String directory) {
        try {
            var jarUrl = plugin.getClass()
                    .getProtectionDomain()
                    .getCodeSource()
                    .getLocation();

            try (var jis = new JarInputStream(jarUrl.openStream())) {
                var target = plugin.getDataPath();

                JarEntry entry;
                while ((entry = jis.getNextJarEntry()) != null) {
                    var name = entry.getName();

                    if (!name.startsWith(directory + "/")) continue;
                    if (entry.isDirectory()) continue;

                    var rel = name.substring((directory + "/").length());
                    var out = target.resolve(rel);

                    Files.createDirectories(out.getParent());

                    try (var os = Files.newOutputStream(out)) {
                        jis.transferTo(os);
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to copy resources", e);
        }
    }

    public void reloadPlugin(CommandSender sender) {
        reloadConfig();
        applyConfig();
        sender.sendMessage("§aCombatExpanded config reloaded!");
    }

    private void applyConfig() {
        pluginEnabled = getConfig().getBoolean("plugin-enabled", true);
        staminaManager.reloadConfigValues();
        dashManager.reloadConfigValues();
        slamManager.reloadConfigValues();
        grappleManager.reloadConfigValues();
    }

    public static CombatExpanded getInstance() { return instance; }

    public StaminaManager getStaminaManager() { return staminaManager; }
    public DashManager getDashManager() { return dashManager; }
    public SlamManager getSlamManager() { return slamManager; }
    public GrappleManager getGrappleManager() { return grappleManager; }

    public boolean isPluginEnabled() { return !pluginEnabled; }
    public void setPluginEnabled(boolean enabled) {
        this.pluginEnabled = enabled;
        getConfig().set("plugin-enabled", enabled);
        saveConfig();
    }

    public boolean hasArtifice(Player player) {
        return artificeToggle.getOrDefault(player.getUniqueId(), true);
    }
    public void toggleArtifice(Player player) {
        UUID id = player.getUniqueId();
        boolean newValue = !hasArtifice(player);
        artificeToggle.put(id, newValue);
    }

}
