package com.vortex.mythicforge;

import com.vortex.mythicforge.commands.MythicForgeCommand;
import com.vortex.mythicforge.hooks.FancyNpcHook;
import com.vortex.mythicforge.hooks.MythicForgeExpansion;
import com.vortex.mythicforge.hooks.VaultHook;
import com.vortex.mythicforge.listeners.GlobalListener;
import com.vortex.mythicforge.listeners.InventoryListener;
import com.vortex.mythicforge.listeners.NpcListener;
import com.vortex.mythicforge.listeners.TomeListener;
import com.vortex.mythicforge.managers.*;
import com.vortex.mythicforge.tasks.ActiveEffectTask;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * The main class for the MythicForge plugin. This class handles the core
 * functionality, including the enabling and disabling of the plugin, initialization
 * of all managers, registration of listeners and commands, and scheduling of tasks.
 * It serves as the central hub for all plugin components.
 *
 * @author Vortex
 * @version 1.0.2
 */
public final class MythicForge extends JavaPlugin {

    private static MythicForge instance;

    // Managers
    private EnchantmentManager enchantmentManager;
    private ItemManager itemManager;
    private TomeManager tomeManager;
    private RuneManager runeManager;
    private SetBonusManager setBonusManager;
    private ShopManager shopManager;
    private SetShopManager setShopManager;
    
    // API Hooks
    private VaultHook vaultHook;
    private FancyNpcHook fancyNpcHook;

    @Override
    public void onEnable() {
        instance = this;
        
        // --- 1. Configuration & Data Loading ---
        saveDefaultConfig();
        
        // --- 2. Initialize All Managers ---
        // Data managers that read from files are loaded first.
        this.enchantmentManager = new EnchantmentManager(this);
        this.runeManager = new RuneManager(this);
        this.setBonusManager = new SetBonusManager(this);
        this.setShopManager = new SetShopManager(this);
        
        // Functional managers that may depend on data.
        this.itemManager = new ItemManager(this);
        this.tomeManager = new TomeManager(this);
        this.shopManager = new ShopManager(this);
        
        // --- 3. Initialize API Hooks ---
        this.vaultHook = new VaultHook(this);
        this.fancyNpcHook = new FancyNpcHook(this);
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new MythicForgeExpansion(this).register();
        }

        // --- 4. Register Persistent Event Listeners ---
        // Our GUI framework handles registering/unregistering its own listeners,
        // so we only need to register the main, always-on listeners here.
        getServer().getPluginManager().registerEvents(new GlobalListener(), this);
        getServer().getPluginManager().registerEvents(new InventoryListener(this), this);
        getServer().getPluginManager().registerEvents(new TomeListener(this), this);
        getServer().getPluginManager().registerEvents(new NpcListener(), this);

        // --- 5. Register Commands ---
        MythicForgeCommand commandExecutor = new MythicForgeCommand();
        getCommand("mythicforge").setExecutor(commandExecutor);
        getCommand("mythicforge").setTabCompleter(commandExecutor);
        
        // --- 6. Schedule Repeating Tasks ---
        // This starts last, after everything else is fully loaded.
        new ActiveEffectTask(this).runTaskTimer(this, 100L, 20L);

        getLogger().info("MythicForge v" + getDescription().getVersion() + " by Vortex has been fully enabled.");
    }

    @Override
    public void onDisable() {
        // Future logic for saving data on shutdown could go here.
        // For now, cancel all tasks to ensure a clean shutdown.
        getServer().getScheduler().cancelTasks(this);
        getLogger().info("MythicForge has been disabled.");
    }

    // --- Getters for all Managers and Hooks ---

    public static MythicForge getInstance() { return instance; }
    public EnchantmentManager getEnchantmentManager() { return enchantmentManager; }
    public ItemManager getItemManager() { return itemManager; }
    public TomeManager getTomeManager() { return tomeManager; }
    public RuneManager getRuneManager() { return runeManager; }
    public SetBonusManager getSetBonusManager() { return setBonusManager; }
    public ShopManager getShopManager() { return shopManager; }
    public SetShopManager getSetShopManager() { return setShopManager; }
    public VaultHook getVaultHook() { return vaultHook; }
    public FancyNpcHook getFancyNpcHook() { return fancyNpcHook; }
                                        }
