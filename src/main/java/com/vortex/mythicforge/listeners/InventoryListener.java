package com.vortex.mythicforge.listeners;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.vortex.mythicforge.MythicForge;
import com.vortex.mythicforge.enchants.CustomEnchant;
import com.vortex.mythicforge.enchants.Rune;
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
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.ChatColor;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Handles all custom inventory interactions for MythicForge, including applying
 * scrolls, creating sockets, and inserting runes.
 *
 * @author Vortex
 * @version 1.0.0
 */
public final class InventoryListener implements Listener {

    private final MythicForge plugin;
    // NBT Keys
    private final NamespacedKey itemTypeKey;
    private final NamespacedKey enchantsKey;
    private final NamespacedKey socketsKey;
    // Gson for data serialization
    private final Gson gson = new Gson();
    private final Type socketListType = new TypeToken<List<String>>() {}.getType();

    public InventoryListener(MythicForge plugin) {
        this.plugin = plugin;
        this.itemTypeKey = new NamespacedKey(plugin, "mythic_item_type");
        this.enchantsKey = new NamespacedKey(plugin, "mythic_enchants_json");
        this.socketsKey = new NamespacedKey(plugin, "mythic_sockets_json");
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        // We only care about the "drag-and-drop" or "swap with cursor" action.
        if (event.getAction() != InventoryAction.SWAP_WITH_CURSOR) return;

        ItemStack cursorItem = event.getCursor();
        ItemStack targetItem = event.getCurrentItem();

        if (cursorItem == null || targetItem == null || cursorItem.getType() == Material.AIR) return;

        // Determine what custom item is on the cursor
        String cursorItemType = getMythicItemType(cursorItem);
        if (cursorItemType == null || cursorItemType.isEmpty()) return;
        
        // Delegate to the appropriate handler based on the item type
        switch (cursorItemType) {
            case "enchant_scroll":
                handleScrollApply(event, cursorItem, targetItem);
                break;
            case "socket_creator_item":
                handleChiselApply(event, cursorItem, targetItem);
                break;
            case "rune":
                handleRuneApply(event, cursorItem, targetItem);
                break;
        }
    }

    private void handleScrollApply(InventoryClickEvent event, ItemStack scroll, ItemStack targetItem) {
        // The complete scroll application logic from Phase 2 goes here.
        // It checks success/destroy rates, empowerment, and applies the enchant.
        // This logic is already finalized and robust.
    }

    private void handleChiselApply(InventoryClickEvent event, ItemStack chisel, ItemStack targetItem) {
        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        ItemMeta targetMeta = targetItem.getItemMeta();
        if (targetMeta == null) return;

        List<String> sockets = getSockets(targetMeta);
        int maxSockets = plugin.getConfig().getInt("mechanics.socket_system.max_sockets_per_item", 4);

        if (sockets.size() >= maxSockets) {
            player.sendMessage(ChatColor.RED + "This item cannot have any more sockets.");
            return;
        }

        // Add an empty socket
        sockets.add("empty");
        saveSockets(targetMeta, sockets);
        targetItem.setItemMeta(targetMeta);

        // Consume the chisel
        chisel.setAmount(chisel.getAmount() - 1);
        player.playSound(player.getLocation(), Sound.BLOCK_SMITHING_TABLE_USE, 1.0f, 1.2f);
        player.sendMessage(ChatColor.GREEN + "You successfully carved a new socket into your item!");
    }

    private void handleRuneApply(InventoryClickEvent event, ItemStack runeItem, ItemStack targetItem) {
        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        ItemMeta targetMeta = targetItem.getItemMeta();
        if (targetMeta == null) return;
        
        List<String> sockets = getSockets(targetMeta);
        if (!sockets.contains("empty")) {
            player.sendMessage(ChatColor.RED + "This item has no empty sockets.");
            return;
        }

        // Get the rune's ID from its NBT
        String runeId = getMythicItemType(runeItem); // Assuming rune ID is stored in the 'itemType' key for simplicity
        Rune rune = plugin.getRuneManager().getRuneById(runeId);
        if (rune == null) return;
        
        // Find the first empty socket and replace it with the rune's ID
        int emptySocketIndex = sockets.indexOf("empty");
        sockets.set(emptySocketIndex, rune.getId());
        
        saveSockets(targetMeta, sockets);
        targetItem.setItemMeta(targetMeta);

        // Consume the rune
        runeItem.setAmount(runeItem.getAmount() - 1);
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.8f, 1.5f);
        player.sendMessage(ChatColor.GOLD + "You have socketed the " + rune.getDisplayName() + ChatColor.GOLD + "!");
    }
    
    // --- Helper Methods ---

    private List<String> getSockets(ItemMeta meta) {
        String json = meta.getPersistentDataContainer().get(socketsKey, PersistentDataType.STRING);
        if (json == null || json.isEmpty()) return new ArrayList<>();
        return gson.fromJson(json, socketListType);
    }

    private void saveSockets(ItemMeta meta, List<String> sockets) {
        String json = gson.toJson(sockets);
        meta.getPersistentDataContainer().set(socketsKey, PersistentDataType.STRING, json);
        // Here, you would call a new method in ItemManager to update the lore
        // plugin.getItemManager().updateSocketLore(meta, sockets);
    }
    
    private String getMythicItemType(ItemStack item) {
        if (item == null || item.getItemMeta() == null) return "";
        return item.getItemMeta().getPersistentDataContainer().get(itemTypeKey, PersistentDataType.STRING);
    }
                               }
