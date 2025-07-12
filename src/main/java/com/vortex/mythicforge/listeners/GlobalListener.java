package com.vortex.mythicforge.listeners;

import com.vortex.mythicforge.utils.EffectProcessor;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.inventory.ItemStack;

/**
 * A global listener for core gameplay events that can trigger custom enchantments,
 * runes, or set bonus effects. This class acts as the main entry point for delegating
 * actions to the EffectProcessor.
 *
 * @author Vortex
 * @version 1.0.0
 */
public final class GlobalListener implements Listener {

    /**
     * Handles all combat between entities to trigger ATTACK and DEFEND effects.
     * Listens on HIGH priority to get the final event details after other plugins
     * (like protection plugins) have had a chance to act.
     *
     * @param event The damage event.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityCombat(EntityDamageByEntityEvent event) {
        // --- Process Attacker's Effects ---
        if (event.getDamager() instanceof LivingEntity) {
            LivingEntity attacker = (LivingEntity) event.getDamager();
            ItemStack weapon = attacker.getEquipment() != null ? attacker.getEquipment().getItemInMainHand() : null;

            // Process enchantments on the held weapon
            if (weapon != null && !weapon.getType().isAir()) {
                EffectProcessor.processItemEffects(attacker, weapon, "ATTACK", event);
            }
            
            // Process triggered effects from the attacker's equipped runes and set bonuses
            EffectProcessor.processPassiveTriggers(attacker, "ATTACK", event);
        }

        // --- Process Defender's Effects ---
        if (event.getEntity() instanceof LivingEntity) {
            LivingEntity defender = (LivingEntity) event.getEntity();
            if (defender.getEquipment() == null) return;

            // Process enchantments on all four armor pieces
            for (ItemStack armorPiece : defender.getEquipment().getArmorContents()) {
                if (armorPiece != null && !armorPiece.getType().isAir()) {
                    EffectProcessor.processItemEffects(defender, armorPiece, "DEFEND", event);
                }
            }
            
            // Process triggered effects from the defender's equipped runes and set bonuses
            EffectProcessor.processPassiveTriggers(defender, "DEFEND", event);
        }
    }

    /**
     * Handles block breaking to trigger MINE effects.
     *
     * @param event The block break event.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        ItemStack tool = player.getInventory().getItemInMainHand();

        if (tool.getType().isAir()) {
            return;
        }

        // The EffectProcessor would need to be expanded to handle this event type
        // EffectProcessor.processItemEffects(player, tool, "MINE", event);
    }
    
    /**
     * Handles bow shooting to trigger SHOOT_BOW effects.
     * This allows applying effects to the arrow itself before it is fired.
     *
     * @param event The bow shoot event.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBowShoot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        
        Player shooter = (Player) event.getEntity();
        ItemStack bow = event.getBow();

        if (bow == null || bow.getType().isAir()) {
            return;
        }

        // The EffectProcessor would need to be expanded to handle this event type
        // EffectProcessor.processItemEffects(shooter, bow, "SHOOT_BOW", event);
    }
}
