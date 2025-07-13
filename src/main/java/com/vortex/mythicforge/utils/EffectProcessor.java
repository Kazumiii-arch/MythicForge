package com.vortex.mythicforge.utils;

import com.vortex.mythicforge.MythicForge;
import com.vortex.mythicforge.enchants.CustomEnchant;
import com.vortex.mythicforge.enchants.Rune;
import com.vortex.mythicforge.enchants.SetBonus;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;

/**
 * A final, static utility class that processes and executes all triggered effects
 * from enchantments, runes, and set bonuses. This is the core scripting engine.
 *
 * @author Vortex
 * @version 1.0.3
 */
public final class EffectProcessor {

    private static final ScriptEngine scriptEngine = new ScriptEngineManager().getEngineByName("JavaScript");
    private static final Map<UUID, Map<String, Long>> cooldowns = new HashMap<>();

    // Private constructor to prevent instantiation of this utility class.
    private EffectProcessor() {}

    public enum TriggerType {
        ATTACK, DEFEND
    }

    /**
     * The main entry point for processing combat events. It gathers all possible
     * triggered effects from both the attacker and defender and executes them.
     * @param event The EntityDamageByEntityEvent to process.
     */
    public static void processCombatEvent(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof LivingEntity) {
            processAllEffectsFor((LivingEntity) event.getDamager(), TriggerType.ATTACK, event);
        }
        if (event.getEntity() instanceof LivingEntity) {
            processAllEffectsFor((LivingEntity) event.getEntity(), TriggerType.DEFEND, event);
        }
    }

    /**
     * Gathers and processes all effects for a specific entity and trigger type.
     */
    private static void processAllEffectsFor(LivingEntity entity, TriggerType trigger, EntityDamageByEntityEvent event) {
        List<Map<?, ?>> allEffectGroups = new ArrayList<>();
        Map<String, Integer> allEnchantsOnEntity = new HashMap<>();

        // 1. Get effects from equipped items (enchants and runes)
        for (ItemStack item : getEquippedItems(entity)) {
            if (item == null || !item.hasItemMeta()) continue;
            Map<String, Integer> itemEnchants = MythicForge.getInstance().getItemManager().getEnchants(item.getItemMeta());
            allEnchantsOnEntity.putAll(itemEnchants);

            for (String enchantId : itemEnchants.keySet()) {
                CustomEnchant enchant = MythicForge.getInstance().getEnchantmentManager().getEnchantById(enchantId);
                if (enchant != null) allEffectGroups.addAll(enchant.getEffects());
            }
        }

        // 2. Get effects from active Set Bonuses (if the entity is a player)
        if (entity instanceof Player) {
            Optional<SetBonus.ActiveBonus> activeBonusOpt = MythicForge.getInstance().getSetBonusManager().getActiveBonusFor((Player) entity);
            activeBonusOpt.ifPresent(activeBonus -> allEffectGroups.addAll(activeBonus.tier().getTriggeredEffects()));
        }


        // 3. Process all gathered effect groups
        for (Map<?, ?> effectGroup : allEffectGroups) {
            if (trigger.name().equalsIgnoreCase(String.valueOf(effectGroup.get("trigger")))) {
                // This assumes a default level of 1 for set/rune effects.
                // It could be expanded to have levels on those too.
                int level = 1;
                String cooldownId = entity.getUniqueId().toString() + ":" + effectGroup.hashCode();
                
                if (checkConditions(effectGroup, entity, level, event, cooldownId)) {
                    executeEffects(effectGroup, entity, level, event);
                    if (getCooldownDuration(effectGroup) > 0) {
                        startCooldown(entity.getUniqueId(), cooldownId);
                    }
                }
            }
        }
    }

    private static boolean checkConditions(Map<?, ?> effectGroup, LivingEntity owner, int level, EntityDamageByEntityEvent event, String cooldownId) {
        List<?> conditions = (List<?>) effectGroup.get("conditions");
        if (conditions == null || conditions.isEmpty()) return true;

        for (Object conditionObj : conditions) {
            String condition = String.valueOf(conditionObj);
            String[] parts = condition.split(" ", 2);
            switch (parts[0].toLowerCase()) {
                case "chance":
                    if (ThreadLocalRandom.current().nextDouble(100) >= evaluateExpression(parts[1], level, event)) return false;
                    break;
                case "health_below_percent":
                    AttributeInstance maxHealth = owner.getAttribute(Attribute.GENERIC_MAX_HEALTH);
                    if (maxHealth == null || (owner.getHealth() / maxHealth.getValue()) * 100 > Double.parseDouble(parts[1])) return false;
                    break;
                case "is_projectile":
                    if (!event.getCause().name().contains("PROJECTILE")) return false;
                    break;
                case "cooldown":
                    if (isOnCooldown(owner.getUniqueId(), cooldownId, Long.parseLong(parts[1]) * 1000)) return false;
                    break;
            }
        }
        return true;
    }

    private static void executeEffects(Map<?, ?> effectGroup, LivingEntity owner, int level, EntityDamageByEntityEvent event) {
        List<?> effects = (List<?>) effectGroup.get("effects");
        if (effects == null) return;
        
        LivingEntity target = (event.getEntity() instanceof LivingEntity) ? (LivingEntity) event.getEntity() : null;

        for (Object effectObj : effects) {
            String effect = String.valueOf(effectObj);
            String[] parts = effect.split(":", 2);
            String type = parts[0].toUpperCase();
            String args = parts.length > 1 ? parts[1] : "";

            try {
                switch (type) {
                    case "DEAL_DAMAGE":
                        if (target != null) {
                            event.setDamage(event.getDamage() + evaluateExpression(args, level, event));
                        }
                        break;
                    case "HEAL":
                        AttributeInstance maxHealth = owner.getAttribute(Attribute.GENERIC_MAX_HEALTH);
                        if(maxHealth != null) {
                            double amount = evaluateExpression(args, level, event);
                            owner.setHealth(Math.min(maxHealth.getValue(), owner.getHealth() + amount));
                        }
                        break;
                    case "TARGET_POTION":
                        if (target != null) applyPotion(target, args);
                        break;
                    case "ATTACKER_POTION":
                        if (event.getDamager() instanceof LivingEntity) applyPotion((LivingEntity) event.getDamager(), args);
                        break;
                    case "SOUND":
                         String[] sParts = args.split(" ");
                         owner.getWorld().playSound(owner.getLocation(), Sound.valueOf(sParts[0].toUpperCase()), 1.0f, Float.parseFloat(sParts[1]));
                         break;
                }
            } catch (Exception e) {
                MythicForge.getInstance().getLogger().warning("Could not execute effect: " + effect + " | Error: " + e.getMessage());
            }
        }
    }

    private static double evaluateExpression(String expression, int level, Event event) {
        try {
            double damage = (event instanceof EntityDamageByEntityEvent) ? ((EntityDamageByEntityEvent) event).getFinalDamage() : 0;
            String processed = expression
                    .replace("{level_number}", String.valueOf(level))
                    .replace("{damage}", String.valueOf(damage));
            return Double.parseDouble(scriptEngine.eval(processed).toString());
        } catch (Exception e) { return 0.0; }
    }

    private static void applyPotion(LivingEntity entity, String args) {
        String[] parts = args.split(":");
        PotionEffectType type = PotionEffectType.getByName(parts[0].toUpperCase());
        if (type != null) {
            int amplifier = Integer.parseInt(parts[1]);
            int duration = Integer.parseInt(parts[2]);
            entity.addPotionEffect(new PotionEffect(type, duration, amplifier));
        }
    }

    private static boolean isOnCooldown(UUID uuid, String id, long duration) {
        return cooldowns.getOrDefault(uuid, Collections.emptyMap()).getOrDefault(id, 0L) + duration > System.currentTimeMillis();
    }

    private static void startCooldown(UUID uuid, String id) {
        cooldowns.computeIfAbsent(uuid, k -> new HashMap<>()).put(id, System.currentTimeMillis());
    }

    private static long getCooldownDuration(Map<?, ?> effectGroup) {
        List<?> conditions = (List<?>) effectGroup.get("conditions");
        if (conditions == null) return 0;
        for (Object conditionObj : conditions) {
            String condition = String.valueOf(conditionObj);
            if (condition.toLowerCase().startsWith("cooldown")) {
                return Long.parseLong(condition.split(" ")[1]);
            }
        }
        return 0;
    }

    private static List<ItemStack> getEquippedItems(LivingEntity entity) {
        if (entity.getEquipment() == null) return Collections.emptyList();
        List<ItemStack> items = new ArrayList<>(Arrays.asList(entity.getEquipment().getArmorContents()));
        items.add(entity.getEquipment().getItemInMainHand());
        items.removeIf(Objects::isNull);
        return items;
    }
                                }
