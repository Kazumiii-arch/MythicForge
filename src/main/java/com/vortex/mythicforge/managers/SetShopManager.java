package com.vortex.mythicforge.managers;

import com.vortex.mythicforge.MythicForge;
import com.vortex.mythicforge.enchants.CustomEnchant;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * Manages the loading, caching, and creation of items for the Pre-Made Set Gear Shop.
 * This class pre-builds all shop items at startup for maximum performance.
 *
 * @author Vortex
 * @version 1.0.0
 */
public final class SetShopManager {

    private final MythicForge plugin;
    private FileConfiguration shopConfig;

    // A cache to hold all pre-built shop items. The key is "setId:pieceId".
    private final Map<String, ItemStack> cachedShopItems = new HashMap<>();

    public SetShopManager(MythicForge plugin) {
        this.plugin = plugin;
        loadAndCacheShopItems();
    }

    /**
     * Loads the set_shop.yml configuration and pre-builds all defined items,
     * caching them for fast retrieval by the GUI. Safe for reloads.
     */
    public void loadAndCacheShopItems() {
        cachedShopItems.clear(); // Clear old cache on reload
        File configFile = new File(plugin.getDataFolder(), "set_shop.yml");
        if (!configFile.exists()) {
            plugin.saveResource("set_shop.yml", false);
        }
        this.shopConfig = YamlConfiguration.loadConfiguration(configFile);

        ConfigurationSection gearSets = shopConfig.getConfigurationSection("gear_sets");
        if (gearSets == null) {
            plugin.getLogger().warning("Could not find 'gear_sets' section in set_shop.yml.");
            return;
        }

        for (String setId : gearSets.getKeys(false)) {
            ConfigurationSection setSection = gearSets.getConfigurationSection(setId + ".pieces");
            if (setSection == null) continue;

            for (String pieceId : setSection.getKeys(false)) {
                generateAndCachePiece(setId, pieceId);
            }
        }
        plugin.getLogger().info("Successfully loaded and cached " + cachedShopItems.size() + " items for the Set Gear Shop.");
    }

    /**
     * Generates a single set piece, applies its enchantment and lore, and stores it in the cache.
     * @param setId The ID of the set (e.g., 'wither_king').
     * @param pieceId The ID of the piece (e.g., 'helmet').
     */
    private void generateAndCachePiece(String setId, String pieceId) {
        String path = "gear_sets." + setId + ".pieces." + pieceId;
        ConfigurationSection pieceConfig = shopConfig.getConfigurationSection(path);
        if (pieceConfig == null) return;

        try {
            Material material = Material.valueOf(pieceConfig.getString("material", "STONE").toUpperCase());
            ItemStack item = new ItemStack(material);
            ItemMeta meta = item.getItemMeta();

            // Set a custom display name if one exists in the config, otherwise use the item's default
            if (pieceConfig.contains("display_name")) {
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', pieceConfig.getString("display_name")));
            }

            // Apply the enchantment
            String enchantId = pieceConfig.getString("enchantment_id");
            if (enchantId != null) {
                CustomEnchant enchant = plugin.getEnchantmentManager().getEnchantById(enchantId);
                if (enchant != null) {
                    plugin.getItemManager().applyEnchant(item, enchant, 1);
                    // We get the meta again because applyEnchant changes it
                    meta = item.getItemMeta();
                }
            }

            // Add the price to the lore
            double price = pieceConfig.getDouble("price");
            List<String> lore = meta.getLore() != null ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
            lore.add(""); // Spacer
            lore.add(ChatColor.YELLOW + "Price: " + ChatColor.GOLD + plugin.getVaultHook().format(price));
            meta.setLore(lore);

            // TODO: Add NBT data for price and item ID to prevent exploits
            
            item.setItemMeta(meta);
            
            // Add the final, fully-formatted item to the cache
            cachedShopItems.put(setId + ":" + pieceId, item);

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load set piece: " + setId + ":" + pieceId, e);
        }
    }

    /**
     * Retrieves a pre-built shop item from the cache.
     * @param setId The ID of the set.
     * @param pieceId The ID of the piece.
     * @return A clone of the cached ItemStack, or null if not found.
     */
    public ItemStack getShopItem(String setId, String pieceId) {
        ItemStack item = cachedShopItems.get(setId.toLowerCase() + ":" + pieceId.toLowerCase());
        return item != null ? item.clone() : null;
    }

    /**
     * Gets the loaded shop configuration file.
     * @return The FileConfiguration for set_shop.yml.
     */
    public FileConfiguration getShopConfig() {
        return shopConfig;
    }
    }
