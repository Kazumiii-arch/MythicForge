package com.vortex.mythicforge.listeners;

import com.vortex.mythicforge.MythicForge;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles all inventory interactions for empowering Enchantment Scrolls with
 * Success Dust and Protection Orbs.
 *
 * @author Vortex
 * @version 1.0.0
 */
public final class TomeListener implements Listener {

    private final MythicForge plugin;
    // NBT Keys to identify our custom items and their stored data
    private final NamespacedKey itemTypeKey;
    private final NamespacedKey boostKey;
    private final NamespacedKey protectionKey;

    public TomeListener(MythicForge plugin) {
        this.plugin = plugin;
        this.itemTypeKey = new NamespacedKey(plugin, "mythic_item_type");
        this.boostKey = new NamespacedKey(plugin, "mythic_boost_amount");
        this.protectionKey = new NamespacedKey(plugin, "mythic_is_protected");
    }

    /**
     * Handles the drag-and-drop action of applying Dust or Orbs onto a Scroll.
     * @param event The inventory click event.
     */
    @EventHandler
    public void onEmpowerScroll(InventoryClickEvent event) {
        // We only care about the "drag-and-drop" or "swap with cursor" action.
        if (event.getAction() != InventoryAction.SWAP_WITH_CURSOR) return;

        ItemStack empowermentItem = event.getCursor();
        ItemStack scroll = event.getCurrentItem();

        if (empowermentItem == null || scroll == null || empowermentItem.getType() == Material.AIR) return;

        ItemMeta scrollMeta = scroll.getItemMeta();
        ItemMeta empowermentMeta = empowermentItem.getItemMeta();
        if (scrollMeta == null || empowermentMeta == null) return;

        String scrollType = scrollMeta.getPersistentDataContainer().get(itemTypeKey, PersistentDataType.STRING);
        String empowermentItemType = empowermentMeta.getPersistentDataContainer().get(itemTypeKey, PersistentDataType.STRING);

        // Check if the target is a scroll and the cursor is one of our empowerment items
        if (scrollType == null || !scrollType.equals("enchant_scroll") || empowermentItemType == null) {
            return;
        }

        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        boolean changed = false;

        switch (empowermentItemType) {
            case "success_dust":
                changed = applyBoost(scrollMeta, player);
                break;
            case "protection_orb":
                changed = applyProtection(scrollMeta, player);
                break;
        }

        if (changed) {
            empowermentItem.setAmount(empowermentItem.getAmount() - 1);
            scroll.setItemMeta(scrollMeta);
        }
    }

    private boolean applyBoost(ItemMeta scrollMeta, Player player) {
        PersistentDataContainer pdc = scrollMeta.getPersistentDataContainer();
        double currentBoost = pdc.getOrDefault(boostKey, PersistentDataType.DOUBLE, 0.0);
        double maxBoost = plugin.getConfig().getDouble("mechanics.scrolls.max_success_boost", 100.0);

        if (currentBoost >= maxBoost) {
            player.sendMessage(ChatColor.RED + "This scroll's success chance cannot be boosted any further.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return false;
        }

        double boostAmount = plugin.getConfig().getDouble("mechanics.custom_items.success_dust.boost_amount", 5.0);
        pdc.set(boostKey, PersistentDataType.DOUBLE, currentBoost + boostAmount);
        updateScrollLore(scrollMeta);
        player.playSound(player.getLocation(), Sound.BLOCK_BREWING_STAND_BREW, 1.0f, 1.5f);
        return true;
    }

    private boolean applyProtection(ItemMeta scrollMeta, Player player) {
        PersistentDataContainer pdc = scrollMeta.getPersistentDataContainer();
        if (pdc.has(protectionKey, PersistentDataType.BYTE)) {
            player.sendMessage(ChatColor.RED + "This scroll is already protected!");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return false;
        }
        pdc.set(protectionKey, PersistentDataType.BYTE, (byte) 1);
        updateScrollLore(scrollMeta);
        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.5f);
        return true;
    }

    /**
     * Updates a scroll's lore to reflect its current empowerment status.
     * This method cleanly removes old status lines before adding the new ones.
     * @param scrollMeta The ItemMeta of the scroll to update.
     */
    private void updateScrollLore(ItemMeta scrollMeta) {
        List<String> lore = scrollMeta.getLore() != null ? new ArrayList<>(scrollMeta.getLore()) : new ArrayList<>();

        // Remove old status lines to prevent duplicates
        lore.removeIf(line -> line.contains("Success Boost:") || line.contains("[Protected]"));

        PersistentDataContainer pdc = scrollMeta.getPersistentDataContainer();
        double boost = pdc.getOrDefault(boostKey, PersistentDataType.DOUBLE, 0.0);
        if (boost > 0) {
            lore.add(ChatColor.GREEN + "Success Boost: +" + String.format("%.1f", boost) + "%");
        }

        if (pdc.has(protectionKey, PersistentDataType.BYTE)) {
            lore.add(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "[Protected]");
        }
        
        scrollMeta.setLore(lore);
    }
          }
