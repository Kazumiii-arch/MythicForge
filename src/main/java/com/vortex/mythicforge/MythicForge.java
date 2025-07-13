package com.vortex.mythicforge;

import com.vortex.mythicforge.commands.MythicForgeCommand;
import com.vortex.mythicforge.gui.RotatingShopGui;
import com.vortex.mythicforge.gui.SalvageGUI;
import com.vortex.mythicforge.gui.SetShopGui;
import com.vortex.mythicforge.hooks.FancyNpcHook; // UPDATED
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
 *
 * @author Vortex
 * @version 1.0.0
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
    private FancyNpcHook fancyNpcHook; // UPDATED

    @Override
    public void onEnable() {
        instance = this;
        
        // --- 1. Configuration & Data Loading ---
        saveDefaultConfig();
        // The managers will handle their own specific configs (runes.yml, etc.)
        
        // --- 2. Initialize All Managers ---
        this.enchantmentManager = new EnchantmentManager(this);
        this.runeManager = new RuneManager(this);
        this.setBonusManager = new SetBonusManager(this);
        this.itemManager = new ItemManager(this);
        this.tomeManager = new TomeManager(this);
        this.setShopManager = new SetShopManager(this);
        this.shopManager = new ShopManager(this);
        
        // --- 3. Initialize API Hooks ---
        this.vaultHook = new VaultHook(this);
        this.fancyNpcHook = new FancyNpcHook(this); // UPDATED
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new MythicForgeExpansion(this).register();
        }

        // --- 4. Register Event Listeners ---
        getServer().getPluginManager().registerEvents(new GlobalListener(), this);
        getServer().getPluginManager().registerEvents(new InventoryListener(this), this);
        getServer().getPluginManager().registerEvents(new TomeListener(this), this);
        getServer().getPluginManager().registerEvents(new NpcListener(), this);
        // Registering GUI listeners (a dummy instance is fine for registration purposes)
        new SalvageGUI(null); 
        new RotatingShopGui(null);
        new SetShopGui(null);

        // --- 5. Register Commands ---
        MythicForgeCommand commandExecutor = new MythicForgeCommand();
        getCommand("mythicforge").setExecutor(commandExecutor);
        getCommand("mythicforge").setTabCompleter(commandExecutor);
        
        // --- 6. Schedule Repeating Tasks ---
        new ActiveEffectTask(this).runTaskTimer(this, 100L, 20L);

        getLogger().info("MythicForge v" + getDescription().getVersion() + " by Vortex has been fully enabled.");
    }

    @Override
    public void onDisable() {
        // Future logic for saving data on shutdown could go here.
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
    
    /**
     * Gets the handler for FancyNpcs API interactions.
     * @return The FancyNpcHook instance.
     */
    public FancyNpcHook getFancyNpcHook() { // UPDATED
        return fancyNpcHook;
    }
}
