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
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

/**
 * Periodically scans all online players' equipment to apply passive effects from Runes and Set Bonuses.
 * This is the core task that brings the advanced RPG systems to life.
 *
 * @author Vortex
 * @version 1.0.2
 */
public final class ActiveEffectTask extends BukkitRunnable {

    private final MythicForge plugin;
    // Tracks which passive potion effects were applied last tick to handle removal.
    private final Map<UUID, Set<PotionEffectType>> lastAppliedPotions = new HashMap<>();

    public ActiveEffectTask(MythicForge plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            try {
                // These maps will aggregate all effects from all sources for this player.
                final Map<PotionEffectType, Integer> passivePotions = new HashMap<>();
                final Map<Attribute, Double> attributeModifiers = new HashMap<>();

                List<ItemStack> equippedItems = getEquippedItems(player);
                
                // 1. Gather effects from Runes
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

                // 2. Gather effects from Set Bonuses
                List<String> equippedEnchantIds = getEnchantIdsFromItems(equippedItems);
                for (SetBonus set : plugin.getSetBonusManager().getAllSets()) {
                    long equippedCount = set.getRequiredEnchantments().stream().filter(equippedEnchantIds::contains).count();
                    if (equippedCount > 0) {
                        List<String> setPassiveEffects = set.getEffectsForPieceCount((int) equippedCount);
                        parseEffects(setPassiveEffects, passivePotions, attributeModifiers);
                    }
                }
                
                // 3. Apply all collected effects
                applyAttributeModifiers(player, attributeModifiers);
                applyPotionEffects(player, passivePotions);

            } catch (Exception e) {
                plugin.getLogger().severe("Error updating active effects for player " + player.getName() + ": " + e.getMessage());
            }
        }
    }

    private void applyAttributeModifiers(Player player, Map<Attribute, Double> modifiers) {
        for (Attribute attribute : Attribute.values()) {
            if (!isModifiableAttribute(attribute)) continue;
            AttributeInstance instance = player.getAttribute(attribute);
            if (instance == null) continue;

            // Remove any old modifiers from this plugin to prevent stacking
            for (AttributeModifier modifier : instance.getModifiers()) {
                if (modifier.getName().startsWith("MythicForge-")) {
                    instance.removeModifier(modifier);
                }
            }

            // Apply the new, aggregated modifier if one exists for this attribute
            if (modifiers.containsKey(attribute)) {
                double value = modifiers.get(attribute);
                // Create a modifier with a unique name and UUID based on the player and attribute
                AttributeModifier modifier = new AttributeModifier(
                    UUID.nameUUIDFromBytes(("MythicForge-" + attribute.name() + "-" + player.getUniqueId()).getBytes()),
                    "MythicForge-" + attribute.name(),
                    value,
                    AttributeModifier.Operation.ADD_NUMBER
                );
                instance.addModifier(modifier);
            }
        }
    }
    
    private void applyPotionEffects(Player player, Map<PotionEffectType, Integer> effects) {
        Set<PotionEffectType> lastEffects = lastAppliedPotions.getOrDefault(player.getUniqueId(), new HashSet<>());
        Set<PotionEffectType> currentEffects = effects.keySet();

        // Remove effects the player no longer qualifies for
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
    
    private void parseEffects(List<String> effects, Map<PotionEffectType, Integer> potions, Map<Attribute, Double> attributes) {
        for (String effect : effects) {
            String[] parts = effect.split(":");
            try {
                if (parts[0].equalsIgnoreCase("POTION")) {
                    PotionEffectType type = PotionEffectType.getByName(parts[1]);
                    int amplifier = Integer.parseInt(parts[2]);
                    if (type != null) potions.merge(type, amplifier, Math::max);
                } else if (parts[0].equalsIgnoreCase("ATTRIBUTE")) {
                    Attribute attribute = Attribute.valueOf("GENERIC_" + parts[1]);
                    double value = Double.parseDouble(parts[3]);
                    if(parts[2].equalsIgnoreCase("ADD")) attributes.merge(attribute, value, Double::sum);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Could not parse passive effect: " + effect);
            }
        }
    }

    private List<ItemStack> getEquippedItems(Player player) {
        PlayerInventory inv = player.getInventory();
        List<ItemStack> items = new ArrayList<>(Arrays.asList(inv.getArmorContents()));
        items.add(inv.getItemInMainHand());
        items.removeIf(Objects::isNull);
        return items;
    }
    
    private List<String> getEnchantIdsFromItems(List<ItemStack> items) {
        List<String> ids = new ArrayList<>();
        for (ItemStack item : items) {
            if (item != null && item.hasItemMeta()) {
                ids.addAll(plugin.getItemManager().getEnchants(item.getItemMeta()).keySet());
            }
        }
        return new ArrayList<>(new HashSet<>(ids)); // Return list of unique IDs
    }

    private boolean isModifiableAttribute(Attribute attribute) {
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
