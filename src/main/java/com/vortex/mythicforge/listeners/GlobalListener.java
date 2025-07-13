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

public final class GlobalListener implements Listener {

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityCombat(EntityDamageByEntityEvent event) {
        EffectProcessor.processCombatEvent(event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        ItemStack tool = player.getInventory().getItemInMainHand();
        if (tool.getType().isAir()) return;
        // EffectProcessor.processBlockBreakEvent(event); // Future expansion
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBowShoot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        ItemStack bow = event.getBow();
        if (bow == null || bow.getType().isAir()) return;
        // EffectProcessor.processBowShootEvent(event); // Future expansion
    }
}
