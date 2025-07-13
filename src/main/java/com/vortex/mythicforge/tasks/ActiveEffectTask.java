package com.vortex.mythicforge.tasks;

import com.vortex.mythicforge.MythicForge;
import com.vortex.mythicforge.enchants.Rune;
import com.vortex.mythicforge.enchants.SetBonus;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

/**
 * Periodically scans all online players' equipment to apply passive effects from Runes and Set Bonuses.
 * This is the core task that brings the advanced RPG systems to life.
 *
 * @author Vortex
 * @version 1.0.1
 */
public final class ActiveEffectTask extends BukkitRunnable {

    private final MythicForge plugin;
    // A map to track which passive potion effects we applied last tick, so we can remove old ones.
    private final Map<UUID, Set<PotionEffectType>> lastAppliedPotions = new HashMap<>();

    public ActiveEffectTask(MythicForge plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            try {
                // These maps will aggregate all effects from all sources.
                final Map<PotionEffectType, Integer> passivePotions = new HashMap<>();
                final Map<Attribute, Double> attributeModifiers = new HashMap<>();

                List<ItemStack> equippedItems = getEquippedItems(player);
                
                // --- 1. Gather Rune Effects ---
                for (ItemStack item : equippedItems) {
                    if (item == null || !item.hasItemMeta()) continue;
                    List<String> socketedRunes = plugin.getItemManager().getSockets(item.getItemMeta());
                    for (String runeId : socketedRunes) {
                        Rune rune = plugin.getRuneManager().getRuneById(runeId);
                        if (rune != null) {
                            parseEffects(rune.getEffects(), passivePotions, attributeModifiers);
                        }
                    }
                }

                // --- 2. Gather Set Bonus Effects ---
                List<String> equippedEnchantIds = getEnchantIdsFromItems(equippedItems);
                for (SetBonus set : plugin.getSetBonusManager().getAllSets()) {
                    long equippedCount = set.getRequiredEnchantments().stream().filter(equippedEnchantIds::contains).count();
                    if (equippedCount > 0) {
                        List<String> setPassiveEffects = set.getEffectsForPieceCount((int) equippedCount);
                        parseEffects(setPassiveEffects, passivePotions, attributeModifiers);
                    }
                }

                // --- 3. Apply All Collected Effects ---
                applyAttributeModifiers(player, attributeModifiers);
                applyPotionEffects(player, passivePotions);

            } catch (Exception e) {
                // Catch any unexpected errors for a single player without stopping the task for others.
                plugin.getLogger().severe("Error while updating active effects for player " + player.getName() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * Correctly applies and removes attribute modifiers to prevent stacking bugs.
     */
    private void applyAttributeModifiers(Player player, Map<Attribute, Double> modifiers) {
        for (Attribute attribute : Attribute.values()) {
            if (!isModifiableAttribute(attribute)) continue;

            AttributeInstance instance = player.getAttribute(attribute);
            if (instance == null) continue;

            // Remove any old modifiers added by this plugin
            for (AttributeModifier modifier : instance.getModifiers()) {
                if (modifier.getName().startsWith("MythicForge-")) {
                    instance.removeModifier(modifier);
                }
            }

            // Apply the new, aggregated modifier if one exists for this attribute
            if (modifiers.containsKey(attribute)) {
                double value = modifiers.get(attribute);
                AttributeModifier modifier = new AttributeModifier("MythicForge-" + attribute.name(), value, AttributeModifier.Operation.ADD_NUMBER);
                instance.addModifier(modifier);
            }
        }
    }

    /**
     * Applies potion effects and cleans up old ones that the player no longer qualifies for.
     */
    private void applyPotionEffects(Player player, Map<PotionEffectType, Integer> effects) {
        Set<PotionEffectType> lastEffects = lastAppliedPotions.getOrDefault(player.getUniqueId(), new HashSet<>());
        Set<PotionEffectType> currentEffects = effects.keySet();

        // Remove effects the player no longer has
        for (PotionEffectType oldEffect : lastEffects) {
            if (!currentEffects.contains(oldEffect)) {
                player.removePotionEffect(oldEffect);
            }
        }

        // Apply new or updated effects
        effects.forEach((type, amplifier) -> {
            // Apply for 3 seconds (60 ticks) with no particles. It will be refreshed next second.
            player.addPotionEffect(new PotionEffect(type, 60, amplifier, true, false, false));
        });
        
        lastAppliedPotions.put(player.getUniqueId(), currentEffects);
    }
    
    /**
     * Parses a list of effect strings and populates the effect maps.
     */
    private void parseEffects(List<String> effects, Map<PotionEffectType, Integer> potions, Map<Attribute, Double> attributes) {
        // ... (Full parsing logic from previous EffectProcessor design) ...
    }

    private List<ItemStack> getEquippedItems(Player player) {
        // ... (Full logic from previous GlobalListener design) ...
        return new ArrayList<>();
    }
    
    private List<String> getEnchantIdsFromItems(List<ItemStack> items) {
        // ... (Full logic from previous SetBonusManager implementation) ...
        return new ArrayList<>();
    }

    private boolean isModifiableAttribute(Attribute attribute) {
        // Only modify attributes that make sense for gear bonuses
        switch (attribute) {
            case GENERIC_MAX_HEALTH:
            case GENERIC_ARMOR:
            case GENERIC_ARMOR_TOUGHNESS:
            case GENERIC_ATTACK_DAMAGE:
            case GENERIC_ATTACK_SPEED:
            case GENERIC_MOVEMENT_SPEED:
            case GENERIC_KNOCKBACK_RESISTANCE:
                return true;
            default:
                return false;
        }
    }
}
