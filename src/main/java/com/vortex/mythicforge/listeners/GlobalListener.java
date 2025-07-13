package com.vortex.mythicforge.listeners;

import com.vortex.mythicforge.utils.EffectProcessor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.inventory.ItemStack;

/**
 * A global listener for core gameplay events that can trigger custom abilities.
 * This class acts as the main entry point for delegating actions to the
 * EffectProcessor to keep the code clean and efficient.
 *
 * @author Vortex
 * @version 1.0.2
 */
public final class GlobalListener implements Listener {

    /**
     * Handles all combat between entities to trigger ATTACK and DEFEND effects.
     * Listens on HIGH priority to act after most other plugins have modified the event.
     *
     * @param event The damage event.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityCombat(EntityDamageByEntityEvent event) {
        // This single call handles everything. The processor is now smart enough
        // to extract the attacker and defender and process all effects for both.
        EffectProcessor.processCombatEvent(event);
    }

    /**
     * Handles block breaking to trigger MINE effects.
     * This is a placeholder for future expansion.
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

        // TODO: Create and call a new 'EffectProcessor.processMineEvent(event)'
        // to handle enchantments like Auto-Smelt or Explosive Touch.
    }

    /**
     * Handles bow shooting to trigger SHOOT_BOW effects.
     * This is a placeholder for future expansion.
     *
     * @param event The bow shoot event.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBowShoot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        ItemStack bow = event.getBow();
        if (bow == null || bow.getType().isAir()) {
            return;
        }

        // TODO: Create and call a new 'EffectProcessor.processShootEvent(event)'
        // to handle enchantments that modify the arrow before it's fired.
    }
}
