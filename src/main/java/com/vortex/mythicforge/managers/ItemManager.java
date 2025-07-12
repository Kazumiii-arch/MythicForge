package com.vortex.mythicforge.managers;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.vortex.mythicforge.MythicForge;
import com.vortex.mythicforge.enchants.CustomEnchant;
import com.vortex.mythicforge.enchants.Rune;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages all direct modifications to ItemStacks, including applying and reading
 * custom NBT data and updating item lore for enchantments, sockets, and runes.
 * This class is the single source of truth for item data manipulation.
 *
 * @author Vortex
 * @version 1.0.0
 */
public final class ItemManager {

    private final MythicForge plugin;
    // NBT Keys
    private final NamespacedKey enchantsKey;
    private final NamespacedKey socketsKey;
    // Gson for data serialization
    private final Gson gson;
    private final Type enchantMapType;
    private final Type socketListType;

    public ItemManager(MythicForge plugin) {
        this.plugin = plugin;
        this.enchantsKey = new NamespacedKey(plugin, "mythic_enchants_json");
        this.socketsKey = new NamespacedKey(plugin, "mythic_sockets_json");
        this.gson = new Gson();
        this.enchantMapType = new TypeToken<Map<String, Integer>>() {}.getType();
        this.socketListType = new TypeToken<List<String>>() {}.getType();
    }

    // --- ENCHANTMENT METHODS ---

    public void applyEnchant(ItemStack item, CustomEnchant enchant, int level) {
        if (item == null || item.getItemMeta() == null || enchant == null) return;
        ItemMeta meta = item.getItemMeta();
        Map<String, Integer> enchants = getEnchants(meta);
        enchants.put(enchant.getId().toLowerCase(), level);
        saveEnchants(meta, enchants);
        item.setItemMeta(meta);
    }

    public Map<String, Integer> getEnchants(ItemMeta meta) {
        if (meta == null) return new HashMap<>();
        String json = meta.getPersistentDataContainer().get(enchantsKey, PersistentDataType.STRING);
        if (json == null || json.isEmpty()) return new HashMap<>();
        try {
            return gson.fromJson(json, enchantMapType);
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    private void saveEnchants(ItemMeta meta, Map<String, Integer> enchants) {
        String json = gson.toJson(enchants);
        meta.getPersistentDataContainer().set(enchantsKey, PersistentDataType.STRING, json);
        refreshLore(meta); // Refresh the entire lore block
    }

    // --- SOCKET & RUNE METHODS ---

    public void addSocket(ItemStack item) {
        if (item == null || item.getItemMeta() == null) return;
        ItemMeta meta = item.getItemMeta();
        List<String> sockets = getSockets(meta);
        sockets.add("empty");
        saveSockets(meta, sockets);
        item.setItemMeta(meta);
    }

    public void applyRune(ItemStack item, Rune rune) {
        if (item == null || item.getItemMeta() == null || rune == null) return;
        ItemMeta meta = item.getItemMeta();
        List<String> sockets = getSockets(meta);
        int emptySocketIndex = sockets.indexOf("empty");
        if (emptySocketIndex == -1) return; // No empty sockets

        sockets.set(emptySocketIndex, rune.getId());
        saveSockets(meta, sockets);
        item.setItemMeta(meta);
    }

    public List<String> getSockets(ItemMeta meta) {
        if (meta == null) return new ArrayList<>();
        String json = meta.getPersistentDataContainer().get(socketsKey, PersistentDataType.STRING);
        if (json == null || json.isEmpty()) return new ArrayList<>();
        try {
            return gson.fromJson(json, socketListType);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private void saveSockets(ItemMeta meta, List<String> sockets) {
        String json = gson.toJson(sockets);
        meta.getPersistentDataContainer().set(socketsKey, PersistentDataType.STRING, json);
        refreshLore(meta); // Refresh the entire lore block
    }
    
    // --- LORE MANAGEMENT ---

    /**
     * Completely re-generates the MythicForge section of an item's lore based on its
     * current enchantments and sockets. This ensures a consistent and clean appearance.
     *
     * @param meta The ItemMeta to update.
     */
    public void refreshLore(ItemMeta meta) {
        if (meta == null) return;
        
        // Note: This implementation replaces all lore. A more advanced version could
        // try to preserve lore lines that don't belong to this plugin.
        List<String> newLore = new ArrayList<>();
        Map<String, Integer> enchants = getEnchants(meta);
        List<String> sockets = getSockets(meta);

        // Add enchantment lines
        enchants.forEach((id, level) -> {
            CustomEnchant enchant = plugin.getEnchantmentManager().getEnchantById(id);
            if (enchant != null) {
                String displayName = enchant.getDisplayName()
                        .replace("{level_roman}", toRoman(level))
                        .replace("{level_number}", String.valueOf(level));
                newLore.add(ChatColor.translateAlternateColorCodes('&', displayName));
            }
        });

        // Add a spacer if there are both enchants and sockets
        if (!enchants.isEmpty() && !sockets.isEmpty()) {
            newLore.add(""); // Spacer
        }

        // Add socket lines
        String emptyFormat = plugin.getConfig().getString("mechanics.socket_system.socket_lore.empty", "&7[ &8Empty Socket &7]");
        String filledFormat = plugin.getConfig().getString("mechanics.socket_system.socket_lore.filled", "&7[ {rune_name} &7]");
        
        sockets.forEach(socketId -> {
            if (socketId.equals("empty")) {
                newLore.add(ChatColor.translateAlternateColorCodes('&', emptyFormat));
            } else {
                Rune rune = plugin.getRuneManager().getRuneById(socketId);
                if (rune != null) {
                    String filledLine = filledFormat.replace("{rune_name}", rune.getDisplayName());
                    newLore.add(ChatColor.translateAlternateColorCodes('&', filledLine));
                }
            }
        });

        meta.setLore(newLore);
    }

    private String toRoman(int number) {
        // ... (roman numeral conversion logic) ...
        return String.valueOf(number);
    }
          }
