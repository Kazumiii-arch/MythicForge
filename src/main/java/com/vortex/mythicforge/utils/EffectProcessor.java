package com.vortex.mythicforge.utils;

import com.vortex.mythicforge.MythicForge;
import com.vortex.mythicforge.enchants.CustomEnchant;
import com.vortex.mythicforge.enchants.Rune;
import com.vortex.mythicforge.enchants.SetBonus;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.util.*;

/**
 * A final, static utility class that processes and executes all triggered effects
 * from enchantments, runes, and set bonuses. This is the core scripting engine.
 *
 * @author Vortex
 * @version 1.0.1
 */
public final class EffectProcessor {

    private static final ScriptEngine scriptEngine = new ScriptEngineManager().getEngineByName("JavaScript");
    private static final Map<UUID, Map<String, Long>> cooldowns = new HashMap<>();

    public enum TriggerType {
        ATTACK, DEFEND, MINE, SHOOT_BOW
    }

    /**
     * The main entry point for processing all triggered effects for a player action.
     * @param trigger The type of action that occurred.
     * @param player The player performing the action.
     * @param event The event associated with the action.
     */
    public static void processTrigger(TriggerType trigger, Player player, Event event) {
        List<ItemStack> equippedItems = getEquippedItems(player);
        List<Map<?, ?>> allEffectGroups = new ArrayList<>();
        Map<String, Integer> allEnchants = new HashMap<>();

        // 1. Gather all effects from all equipped items (enchants and runes)
        for (ItemStack item : equippedItems) {
            if (item == null || !item.hasItemMeta()) continue;
            Map<String, Integer> itemEnchants = MythicForge.getInstance().getItemManager().getEnchants(item.getItemMeta());
            allEnchants.putAll(itemEnchants);

            for (Map.Entry<String, Integer> entry : itemEnchants.entrySet()) {
                CustomEnchant enchant = MythicForge.getInstance().getEnchantmentManager().getEnchantById(entry.getKey());
                if (enchant != null) allEffectGroups.addAll(enchant.getEffects());
            }
            
            // Similar logic would be here for Runes with triggered effects
        }

        // 2. Gather all effects from active Set Bonuses
        // SetBonus activeSet = MythicForge.getInstance().getSetBonusManager().getActiveSet(player);
        // if (activeSet != null) {
        //     allEffectGroups.addAll(activeSet.getTriggeredEffectsForPieceCount(...));
        // }

        // 3. Process all gathered effects
        for (Map<?, ?> effectGroup : allEffectGroups) {
            if (trigger.name().equalsIgnoreCase(String.valueOf(effectGroup.get("trigger")))) {
                // For now, we assume level 1 for set/rune effects. This could be expanded.
                int level = allEnchants.getOrDefault(effectGroup.get("enchant_id"), 1); 
                String cooldownId = player.getUniqueId().toString() + ":" + effectGroup.hashCode();
                
                if (checkConditions(effectGroup, player, level, event, cooldownId)) {
                    executeEffects(effectGroup, player, level, event);
                    if (getCooldownDuration(effectGroup) > 0) {
                        startCooldown(player.getUniqueId(), cooldownId);
                    }
                }
            }
        }
    }

    private static boolean checkConditions(Map<?, ?> effectGroup, Player player, int level, Event event, String cooldownId) {
        // ... (Full condition checking logic from previous response)
        // ... (Includes chance, health_below_percent, cooldown, is_projectile)
        return true;
    }

    private static void executeEffects(Map<?, ?> effectGroup, Player player, int level, Event event) {
        List<String> effects = (List<String>) effectGroup.get("effects");
        if (effects == null) return;

        LivingEntity target = null;
        LivingEntity attacker = null;
        if (event instanceof EntityDamageByEntityEvent) {
            EntityDamageByEntityEvent damageEvent = (EntityDamageByEntityEvent) event;
            if (damageEvent.getEntity() instanceof LivingEntity) target = (LivingEntity) damageEvent.getEntity();
            if (damageEvent.getDamager() instanceof LivingEntity) attacker = (LivingEntity) damageEvent.getDamager();
        }

        for (String effect : effects) {
            String[] parts = effect.split(":", 2);
            String type = parts[0].toUpperCase();
            String args = parts.length > 1 ? parts[1] : "";

            try {
                switch (type) {
                    case "DEAL_DAMAGE":
                        if (event instanceof EntityDamageByEntityEvent && target != null) {
                            double extraDamage = evaluateExpression(args, level, event);
                            ((EntityDamageByEntityEvent) event).setDamage(((EntityDamageByEntityEvent) event).getDamage() + extraDamage);
                        }
                        break;
                    case "TARGET_POTION":
                        if (target != null) applyPotion(target, args);
                        break;
                    case "ATTACKER_POTION":
                        if (attacker != null) applyPotion(attacker, args);
                        break;
                    case "ATTACKER_FIRE":
                        if (attacker != null) {
                            int ticks = Integer.parseInt(args);
                            attacker.setFireTicks(attacker.getFireTicks() + ticks);
                        }
                        break;
                    case "AOE_EFFECT":
                        if (target != null) {
                            String[] aoeParts = args.split(" ", 3);
                            String aoeTargetType = aoeParts[0].split(":")[1];
                            double radius = Double.parseDouble(aoeParts[1].split(":")[1]);
                            String innerEffect = aoeParts[2];
                            for (Entity nearby : target.getNearbyEntities(radius, radius, radius)) {
                                if (nearby instanceof LivingEntity && nearby != player) {
                                    // A real implementation would have team/enemy checks
                                    String[] innerParts = innerEffect.split(":", 2);
                                    if (innerParts[0].equalsIgnoreCase("'POTION")) {
                                        applyPotion((LivingEntity) nearby, innerParts[1].replace("'", ""));
                                    }
                                }
                            }
                        }
                        break;
                    // ... other effects like HEAL, PARTICLE, SOUND ...
                }
            } catch (Exception e) {
                MythicForge.getInstance().getLogger().warning("Could not execute effect: " + effect);
            }
        }
    }

    // --- All Helper Methods ---
    // evaluateExpression(..), applyPotion(..), isOnCooldown(..), startCooldown(..) etc.
    // getEquippedItems(..)
                                }
