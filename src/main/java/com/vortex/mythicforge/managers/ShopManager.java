package com.vortex.mythicforge.managers;

import com.vortex.mythicforge.MythicForge;
import com.vortex.mythicforge.enchants.CustomEnchant;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.AbstractMap.SimpleEntry;
import java.util.Map.Entry;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * Manages the lifecycle of the Rotating Enchantment Shop, including
 * stock generation, persistence, and automatic refreshing.
 *
 * @author Vortex
 * @version 1.0.0
 */
public final class ShopManager {

    private final MythicForge plugin;
    private final File dataFile;
    private List<Entry<ItemStack, Double>> currentStock;
    private long nextRefreshTime;

    public ShopManager(MythicForge plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "shop-data.yml");
        this.currentStock = new ArrayList<>();
        loadShopData();
        scheduleRefreshTask();
    }

    /**
     * Forces an immediate refresh of the shop's stock, generates new items,
     * and saves the new data. Can be called by an admin command.
     */
    public void forceRefreshStock() {
        plugin.getLogger().info("Force-generating new stock for the rotating shop...");
        currentStock.clear();
        FileConfiguration config = plugin.getConfig();
        List<String> stockPoolEntries = config.getStringList("enchant_shop.stock_pool");
        int shopSlots = config.getInt("enchant_shop.gui_rows", 3) * 9;

        // --- Weighted Random Selection Logic ---
        List<WeightedStockItem> weightedPool = new ArrayList<>();
        double totalWeight = 0;
        for (String entry : stockPoolEntries) {
            try {
                String[] parts = entry.split(":");
                double weight = Double.parseDouble(parts[4]);
                weightedPool.add(new WeightedStockItem(entry, weight));
                totalWeight += weight;
            } catch (Exception e) {
                plugin.getLogger().warning("Skipping malformed stock_pool entry: " + entry);
            }
        }

        if (weightedPool.isEmpty()) {
            plugin.getLogger().severe("Shop stock pool is empty or invalid! The shop will be empty.");
            return;
        }

        for (int i = 0; i < shopSlots; i++) {
            double random = ThreadLocalRandom.current().nextDouble() * totalWeight;
            for (WeightedStockItem weightedItem : weightedPool) {
                random -= weightedItem.weight;
                if (random <= 0) {
                    parseStockEntry(weightedItem.entryString).ifPresent(currentStock::add);
                    break;
                }
            }
        }
        
        nextRefreshTime = System.currentTimeMillis() + (config.getLong("enchant_shop.refresh_interval_minutes") * 60 * 1000);
        saveShopData();
        plugin.getLogger().info("Shop stock has been refreshed with " + currentStock.size() + " items.");
    }

    private void scheduleRefreshTask() {
        long interval = 20L * 60; // Check every minute
        new BukkitRunnable() {
            @Override
            public void run() {
                if (System.currentTimeMillis() > nextRefreshTime) {
                    // Run the refresh on the main thread if it involves Bukkit API for item creation
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            forceRefreshStock();
                        }
                    }.runTask(plugin);
                }
            }
        }.runTaskTimerAsynchronously(plugin, interval, interval);
    }

    private void saveShopData() {
        FileConfiguration dataConfig = new YamlConfiguration();
        dataConfig.set("next-refresh-time", nextRefreshTime);
        // Save a "dehydrated" version of the items to avoid Bukkit serialization issues
        List<String> dehydratedStock = currentStock.stream()
            .map(entry -> dehydrate(entry.getKey(), entry.getValue()))
            .collect(Collectors.toList());
        dataConfig.set("current-stock", dehydratedStock);
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save shop data to file!", e);
        }
    }

    private void loadShopData() {
        if (!dataFile.exists()) {
            forceRefreshStock();
            return;
        }
        FileConfiguration dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        this.nextRefreshTime = dataConfig.getLong("next-refresh-time");

        if (System.currentTimeMillis() > nextRefreshTime) {
            forceRefreshStock();
            return;
        }

        List<String> dehydratedStock = dataConfig.getStringList("current-stock");
        currentStock.clear();
        for (String entry : dehydratedStock) {
            parseStockEntry(entry).ifPresent(currentStock::add);
        }
        plugin.getLogger().info("Loaded " + currentStock.size() + " items from shop-data.yml.");
    }

    private java.util.Optional<Entry<ItemStack, Double>> parseStockEntry(String entry) {
        try {
            String[] parts = entry.split(":");
            String type = parts[0];
            double price = Double.parseDouble(parts[parts.length-2]);
            ItemStack item = null;

            if (type.equalsIgnoreCase("TIER")) {
                String tier = parts[1]; // Not used to get enchant, but could be used for filtering
                CustomEnchant enchant = plugin.getEnchantmentManager().getEnchantById(parts[1]); // Assuming ID matches tier for simplicity
                if(enchant == null) return java.util.Optional.empty();
                int level = Integer.parseInt(parts[2]);
                item = plugin.getTomeManager().createScroll(enchant, level, 1);
            } else if (type.equalsIgnoreCase("ITEM")) {
                String itemId = parts[1];
                int amount = Integer.parseInt(parts[2]);
                // This would require a more robust TomeManager, for now we simulate
                item = plugin.getTomeManager().createCustomItem(itemId, amount);
            }

            if (item != null) {
                return java.util.Optional.of(new SimpleEntry<>(item, price));
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to parse stock entry: " + entry);
        }
        return java.util.Optional.empty();
    }

    private String dehydrate(ItemStack item, double price) {
        // Reverse of parseStockEntry logic, to save items back to a string format
        // This is complex and would require reading NBT data from the item
        return ""; // Placeholder
    }
    
    public List<Entry<ItemStack, Double>> getCurrentStock() {
        return currentStock;
    }

    public long getNextRefreshTime() {
        return nextRefreshTime;
    }
    
    // Helper class for weighted randomization
    private static class WeightedStockItem {
        final String entryString;
        final double weight;
        WeightedStockItem(String entry, double weight) {
            this.entryString = entry;
            this.weight = weight;
        }
    }
                  }
