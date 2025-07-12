package com.vortex.mythicforge.tasks;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.vortex.mythicforge.MythicForge;
import com.vortex.mythicforge.enchants.Rune;
import com.vortex.mythicforge.enchants.SetBonus;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Periodically scans all online players' equipment to apply passive effects from Runes and Set Bonuses.
 * This is the core task that brings the advanced RPG systems to life.
 *
 * @author Vortex
 * @version 1.0.0
 */
public final class ActiveEffectTask extends BukkitRunnable {

    private final MythicForge plugin;
    // NBT Keys
    private final NamespacedKey enchantsKey;
    private final NamespacedKey socketsKey;
    // Gson for deserialization
    private final Gson gson;
    private final Type enchantMapType;
    private final Type socketListType;

    public ActiveEffectTask(MythicForge plugin) {
        this.plugin = plugin;
        this.enchantsKey = new NamespacedKey(plugin, "mythic_enchants_json");
        this.socketsKey = new NamespacedKey(plugin, "mythic_sockets_json");
        this.gson = new Gson();
        this.enchantMapType = new TypeToken<Map<String, Integer>>() {}.getType();
        this.socketListType = new TypeToken<List<String>>() {}.getType();
    }

    @Override
    public void run() {
        // Iterate through all online players to update their effects
        for (Player player : Bukkit.getOnlinePlayers()) {
            Map<PotionEffectType, Integer> potionEffects = new HashMap<>();
            Map<Attribute, Double> attributeModifiers = new HashMap<>();

            List<ItemStack> equippedItems = getEquippedItems(player);

            // --- 1. Gather all Rune effects ---
            for (ItemStack item : equippedItems) {
                if (item == null || !item.hasItemMeta()) continue;
                List<String> socketedRunes = getSockets(item.getItemMeta());
                for (String runeId : socketedRunes) {
                    Rune rune = plugin.getRuneManager().getRuneById(runeId);
                    if (rune != null) {
                        parseEffects(rune.getEffects(), potionEffects, attributeModifiers);
                    }
                }
            }

            // --- 2. Gather all Set Bonus effects ---
            List<String> equippedEnchantIds = getEnchantIdsFromItems(equippedItems);
            for (SetBonus set : plugin.getSetBonusManager().getAllSets()) {
                long equippedCount = set.getRequiredEnchantments().stream().filter(equippedEnchantIds::contains).count();
                if (equippedCount > 0) {
                    List<String> setPassiveEffects = set.getEffectsForPieceCount((int) equippedCount);
                    parseEffects(setPassiveEffects, potionEffects, attributeModifiers);
                }
            }
            
            // --- 3. Apply all collected effects ---
            applyAttributeModifiers(player, attributeModifiers);
            applyPotionEffects(player, potionEffects);
        }
    }

    /**
     * Correctly applies and removes attribute modifiers to prevent stacking bugs.
     */
    private void applyAttributeModifiers(Player player, Map<Attribute, Double> modifiers) {
        for (Attribute attribute : Attribute.values()) {
            AttributeInstance instance = player.getAttribute(attribute);
            if (instance == null) continue;

            // Remove any old modifiers added by this plugin
            for (AttributeModifier modifier : instance.getModifiers()) {
                if (modifier.getName().startsWith("MythicForge-")) {
                    instance.removeModifier(modifier);
                }
            }

            // Apply the new modifier if one exists for this attribute
            if (modifiers.containsKey(attribute)) {
                double value = modifiers.get(attribute);
                AttributeModifier modifier = new AttributeModifier("MythicForge-" + attribute.name(), value, AttributeModifier.Operation.ADD_NUMBER);
                instance.addModifier(modifier);
            }
        }
    }

    /**
     * Applies potion effects with a short duration to ensure they are removed if gear is unequipped.
     */
    private void applyPotionEffects(Player player, Map<PotionEffectType, Integer> effects) {
        effects.forEach((type, amplifier) -> {
            // Apply for 3 seconds (60 ticks) with no particles. It will be refreshed next second.
            player.addPotionEffect(new PotionEffect(type, 60, amplifier, true, false, false));
        });
    }

    /**
     * Parses a list of effect strings (e.g., 'POTION:SPEED:0') and populates the effect maps.
     */
    private void parseEffects(List<String> effects, Map<PotionEffectType, Integer> potions, Map<Attribute, Double> attributes) {
        for (String effect : effects) {
            String[] parts = effect.split(":");
            try {
                if (parts[0].equalsIgnoreCase("POTION")) {
                    PotionEffectType type = PotionEffectType.getByName(parts[1]);
                    int amplifier = Integer.parseInt(parts[2]);
                    if (type != null) {
                        // If the effect already exists, only keep the stronger one.
                        potions.merge(type, amplifier, Math::max);
                    }
                } else if (parts[0].equalsIgnoreCase("ATTRIBUTE")) {
                    Attribute attribute = Attribute.valueOf("GENERIC_" + parts[1]); // Assuming GENERIC_ prefix
                    double value = Double.parseDouble(parts[3]);
                    // Sum attribute values from different sources
                    attributes.merge(attribute, value, Double::sum);
                }
            } catch (Exception e) {
                // Ignore malformed effect strings
            }
        }
    }

    private List<String> getSockets(ItemMeta meta) {
        String json = meta.getPersistentDataContainer().get(socketsKey, PersistentDataType.STRING);
        if (json == null || json.isEmpty()) return Collections.emptyList();
        return gson.fromJson(json, socketListType);
    }
    
    private List<String> getEnchantIdsFromItems(List<ItemStack> items) {
        // ... (Logic from previous step) ...
        return new ArrayList<>();
    }

    private List<ItemStack> getEquippedItems(Player player) {
        // ... (Logic from previous step) ...
        return new ArrayList<>();
    }
                  }
