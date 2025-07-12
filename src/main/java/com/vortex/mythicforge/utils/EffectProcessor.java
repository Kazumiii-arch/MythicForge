package com.vortex.mythicforge.utils;

import com.vortex.mythicforge.MythicForge;
import com.vortex.mythicforge.enchants.CustomEnchant;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * A final, static utility class that processes and executes all triggered effects
 * from enchantments, runes, and set bonuses. This is the core scripting engine.
 *
 * @author Vortex
 * @version 1.0.0
 */
public final class EffectProcessor {

    private static final ScriptEngine scriptEngine = new ScriptEngineManager().getEngineByName("JavaScript");
    // Cooldown Manager: Player UUID -> Cooldown ID -> Timestamp of last use
    private static final Map<UUID, Map<String, Long>> cooldowns = new HashMap<>();

    /**
     * The main entry point for processing triggered effects from a specific item.
     * @param owner The entity who owns the item (the one with the enchantment).
     * @param item The item that has the enchantment.
     * @param trigger The trigger to check for (e.g., "ATTACK").
     * @param event The event context.
     */
    public static void processItemEffects(LivingEntity owner, ItemStack item, String trigger, EntityDamageByEntityEvent event) {
        if (item == null || !item.hasItemMeta()) return;

        Map<String, Integer> enchants = MythicForge.getInstance().getItemManager().getEnchants(item.getItemMeta());
        if (enchants.isEmpty()) return;

        enchants.forEach((id, level) -> {
            CustomEnchant enchant = MythicForge.getInstance().getEnchantmentManager().getEnchantById(id);
            if (enchant != null) {
                processEffectGroups(enchant.getEffects(), trigger, owner, level, event);
            }
        });
    }

    /**
     * Processes triggered effects from non-item sources, like Set Bonuses.
     * @param owner The entity who has the set bonus.
     * @param trigger The trigger to check for.
     * @param event The event context.
     */
    public static void processTriggeredSetBonusEffects(LivingEntity owner, String trigger, EntityDamageByEntityEvent event) {
        // ... Logic to get the player's active set bonus ...
        // ... Get the 'triggered_effects' list from the set bonus ...
        // ... Call processEffectGroups(...) with the list of effects ...
    }

    private static void processEffectGroups(List<Map<?, ?>> effectGroups, String trigger, LivingEntity owner, int level, EntityDamageByEntityEvent event) {
        for (Map<?, ?> effectGroup : effectGroups) {
            if (trigger.equalsIgnoreCase(String.valueOf(effectGroup.get("trigger")))) {
                // Generate a unique ID for this effect group for cooldowns
                String cooldownId = owner.getUniqueId().toString() + ":" + effectGroup.hashCode();
                if (checkConditions(effectGroup, owner, level, event, cooldownId)) {
                    executeEffects(effectGroup, owner, level, event);
                    // If a cooldown was required, start it now
                    if (getCooldownDuration(effectGroup) > 0) {
                        startCooldown(owner.getUniqueId(), cooldownId);
                    }
                }
            }
        }
    }

    private static boolean checkConditions(Map<?, ?> effectGroup, LivingEntity owner, int level, EntityDamageByEntityEvent event, String cooldownId) {
        List<String> conditions = (List<String>) effectGroup.get("conditions");
        if (conditions == null || conditions.isEmpty()) return true;

        for (String condition : conditions) {
            String[] parts = condition.split(" ", 2);
            String type = parts[0].toLowerCase();

            switch (type) {
                case "chance":
                    double chance = evaluateExpression(parts[1], level, event);
                    if (ThreadLocalRandom.current().nextDouble(100) >= chance) return false;
                    break;
                case "health_below_percent":
                    double percent = Double.parseDouble(parts[1]);
                    if ((owner.getHealth() / Objects.requireNonNull(owner.getAttribute(Attribute.GENERIC_MAX_HEALTH)).getValue()) * 100 > percent) return false;
                    break;
                case "is_projectile":
                    if (!event.getCause().name().contains("PROJECTILE")) return false;
                    break;
                case "cooldown":
                    long duration = Long.parseLong(parts[1]) * 1000; // Cooldown in seconds
                    if (isOnCooldown(owner.getUniqueId(), cooldownId, duration)) return false;
                    break;
            }
        }
        return true;
    }

    private static void executeEffects(Map<?, ?> effectGroup, LivingEntity owner, int level, EntityDamageByEntityEvent event) {
        List<String> effects = (List<String>) effectGroup.get("effects");
        if (effects == null) return;
        
        LivingEntity target = (event.getEntity() instanceof LivingEntity) ? (LivingEntity) event.getEntity() : null;
        LivingEntity attacker = (event.getDamager() instanceof LivingEntity) ? (LivingEntity) event.getDamager() : null;

        for (String effect : effects) {
            String[] parts = effect.split(":", 2);
            String type = parts[0].toUpperCase();
            String args = parts[1];

            switch (type) {
                case "DEAL_DAMAGE":
                    if (target != null) {
                        double extraDamage = evaluateExpression(args, level, event);
                        event.setDamage(event.getDamage() + extraDamage);
                    }
                    break;
                case "TARGET_POTION":
                    if (target != null) {
                        applyPotion(target, args);
                    }
                    break;
                case "ATTACKER_POTION":
                     if (attacker != null) {
                        applyPotion(attacker, args);
                    }
                    break;
                // ... Other effects like HEAL, PARTICLE, SOUND ...
            }
        }
    }
    
    // --- Cooldown Logic ---
    private static boolean isOnCooldown(UUID uuid, String id, long duration) {
        if (!cooldowns.containsKey(uuid) || !cooldowns.get(uuid).containsKey(id)) {
            return false;
        }
        long lastUsed = cooldowns.get(uuid).get(id);
        return (System.currentTimeMillis() - lastUsed) < duration;
    }

    private static void startCooldown(UUID uuid, String id) {
        cooldowns.computeIfAbsent(uuid, k -> new HashMap<>()).put(id, System.currentTimeMillis());
    }

    private static int getCooldownDuration(Map<?, ?> effectGroup) {
        List<String> conditions = (List<String>) effectGroup.get("conditions");
        if (conditions == null) return 0;
        for (String condition : conditions) {
            if (condition.toLowerCase().startsWith("cooldown")) {
                return Integer.parseInt(condition.split(" ")[1]);
            }
        }
        return 0;
    }

    // --- Utility Methods ---
    private static void applyPotion(LivingEntity entity, String args) {
        String[] parts = args.split(":");
        PotionEffectType type = PotionEffectType.getByName(parts[0]);
        int amplifier = Integer.parseInt(parts[1]);
        int duration = Integer.parseInt(parts[2]);
        if (type != null) {
            entity.addPotionEffect(new PotionEffect(type, duration, amplifier));
        }
    }
    
    private static double evaluateExpression(String expression, int level, EntityDamageByEntityEvent event) {
        // ... (Expression evaluation logic from previous step) ...
        return 0.0;
    }
                                           }
