package com.vortex.mythicforge.managers;

import com.vortex.mythicforge.MythicForge;
import com.vortex.mythicforge.enchants.CustomEnchant;
import com.vortex.mythicforge.enchants.Rune;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.stream.Collectors;

public final class TomeManager {

    private final MythicForge plugin;
    private final NamespacedKey itemTypeKey;
    private final NamespacedKey scrollEnchantKey;
    private final NamespacedKey scrollLevelKey;
    private final NamespacedKey runeIdKey;


    public TomeManager(MythicForge plugin) {
        this.plugin = plugin;
        this.itemTypeKey = new NamespacedKey(plugin, "mythic_item_type");
        this.scrollEnchantKey = new NamespacedKey(plugin, "mythic_scroll_enchant");
        this.scrollLevelKey = new NamespacedKey(plugin, "mythic_scroll_level");
        this.runeIdKey = new NamespacedKey(plugin, "mythic_rune_id");
    }

    public ItemStack createScroll(CustomEnchant enchant, int level, int amount) {
        Objects.requireNonNull(enchant, "Cannot create a scroll for a null enchantment.");
        ConfigurationSection itemConfig = plugin.getConfig().getConfigurationSection("mechanics.custom_items.enchant_scroll");
        if (itemConfig == null) return new ItemStack(Material.AIR);

        Material material = Material.matchMaterial(itemConfig.getString("material", "BOOK").toUpperCase());
        if (material == null) material = Material.BOOK;

        ItemStack scrollItem = new ItemStack(material, amount);
        ItemMeta meta = scrollItem.getItemMeta();
        if (meta == null) return scrollItem;

        ConfigurationSection scrollConfig = plugin.getConfig().getConfigurationSection("mechanics.scrolls");
        ConfigurationSection tierConfig = plugin.getConfig().getConfigurationSection("mechanics.tiers." + enchant.getTier());

        double successRate = tierConfig.getDouble("success_rate");
        double destroyRate = tierConfig.getDouble("destroy_rate");

        String displayName = scrollConfig.getString("display_name").replace("{enchant_name}", enchant.getDisplayName());
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', displayName));

        List<String> lore = scrollConfig.getStringList("lore").stream()
                .map(line -> ChatColor.translateAlternateColorCodes('&', line
                        .replace("{enchant_name}", enchant.getDisplayName())
                        .replace("{enchant_level_roman}", toRoman(level))
                        .replace("{success_rate}", String.valueOf(successRate))
                        .replace("{destroy_rate}", String.valueOf(destroyRate))
                ))
                .collect(Collectors.toList());
        meta.setLore(lore);

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(itemTypeKey, PersistentDataType.STRING, "enchant_scroll");
        pdc.set(scrollEnchantKey, PersistentDataType.STRING, enchant.getId());
        pdc.set(scrollLevelKey, PersistentDataType.INTEGER, level);

        scrollItem.setItemMeta(meta);
        return scrollItem;
    }

    public ItemStack createCustomItem(String itemId, int amount) {
        String configPath = "mechanics.custom_items." + itemId;
        ConfigurationSection itemConfig = plugin.getConfig().getConfigurationSection(configPath);
        if (itemConfig == null) {
            plugin.getLogger().warning("Attempted to create custom item '" + itemId + "' but its definition was not found.");
            return new ItemStack(Material.AIR);
        }

        String materialName = itemConfig.getString("material", "STONE").toUpperCase();
        Material material = Material.matchMaterial(materialName);
        if (material == null) {
            plugin.getLogger().severe("Invalid material '" + materialName + "' for custom item '" + itemId + "'.");
            return new ItemStack(Material.AIR);
        }

        ItemStack customItem = new ItemStack(material, amount);
        ItemMeta meta = customItem.getItemMeta();
        if (meta == null) return customItem;

        String displayName = itemConfig.getString("display_name", itemId);
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', displayName));
        List<String> lore = itemConfig.getStringList("lore").stream()
                .map(line -> ChatColor.translateAlternateColorCodes('&', line))
                .collect(Collectors.toList());
        meta.setLore(lore);

        meta.getPersistentDataContainer().set(itemTypeKey, PersistentDataType.STRING, itemId);
        customItem.setItemMeta(meta);
        return customItem;
    }
    
    public ItemStack createRuneItem(Rune rune, int amount) {
        ItemStack runeItem = new ItemStack(rune.getItemMaterial(), amount);
        ItemMeta meta = runeItem.getItemMeta();
        if (meta == null) return runeItem;
        
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', rune.getDisplayName()));
        meta.setLore(rune.getItemLore().stream().map(l -> ChatColor.translateAlternateColorCodes('&', l)).collect(Collectors.toList()));
        
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(itemTypeKey, PersistentDataType.STRING, "rune");
        pdc.set(runeIdKey, PersistentDataType.STRING, rune.getId());
        
        runeItem.setItemMeta(meta);
        return runeItem;
    }

    private String toRoman(int number) {
        if (number < 1 || number > 39) return String.valueOf(number);
        String[] r_keys = {"M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"};
        int[] v_keys = {1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};
        StringBuilder roman = new StringBuilder();
        for (int i = 0; i < r_keys.length; i++) {
            while (number >= v_keys[i]) {
                roman.append(r_keys[i]);
                number -= v_keys[i];
            }
        }
        return roman.toString();
    }
                            }
