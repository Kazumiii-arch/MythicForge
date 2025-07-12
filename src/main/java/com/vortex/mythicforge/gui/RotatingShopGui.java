package com.vortex.mythicforge.gui;

import com.vortex.mythicforge.managers.ShopManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The GUI implementation for the dynamic, rotating enchantment shop.
 * Extends PaginatedGui to automatically handle multiple pages of items.
 */
public class RotatingShopGui extends PaginatedGui {

    private final ShopManager shopManager;

    public RotatingShopGui(Player player) {
        // We have 5 rows (45 slots). The bottom row (9 slots) is for navigation.
        // This leaves 36 slots per page for shop items.
        super(player, 36);
        this.shopManager = plugin.getShopManager();
        
        // This must be called to fill the item list before opening.
        populateItems();
        
        // This builds and shows the GUI.
        open();
    }

    @Override
    protected Inventory createInventory() {
        String title = plugin.getConfig().getString("enchant_shop.gui_title", "&5Mysterious Wares");
        int rows = plugin.getConfig().getInt("enchant_shop.gui_rows", 3) + 1; // Add 1 row for navigation
        if (rows > 6) rows = 6;
        
        Inventory inv = Bukkit.createInventory(this, rows * 9, ChatColor.translateAlternateColorCodes('&', title));
        refreshInventory(); // This parent method populates items and nav buttons
        return inv;
    }

    @Override
    protected void populateItems() {
        // Get the current stock from the manager
        List<Map.Entry<ItemStack, Double>> stock = shopManager.getCurrentStock();
        
        // For each item, add the price to its lore and add it to the master list
        for (Map.Entry<ItemStack, Double> entry : stock) {
            ItemStack item = entry.getKey().clone();
            double price = entry.getValue();
            ItemMeta meta = item.getItemMeta();
            
            List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
            lore.add(""); // Spacer
            lore.add(ChatColor.YELLOW + "Price: " + ChatColor.GOLD + plugin.getVaultHook().format(price));
            meta.setLore(lore);
            item.setItemMeta(meta);
            
            this.itemsToDisplay.add(item);
        }
    }

    @Override
    protected void handleContentClick(InventoryClickEvent event) {
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;
        
        // Find the original item and price from the manager to prevent lore exploits
        Map.Entry<ItemStack, Double> stockEntry = shopManager.findStockEntry(clickedItem);
        if (stockEntry == null) return;
        
        double price = stockEntry.getValue();

        // Check if the player can afford the item
        if (plugin.getVaultHook().hasEnough(player, price)) {
            // Attempt to withdraw money
            if (plugin.getVaultHook().withdraw(player, price).transactionSuccess()) {
                player.getInventory().addItem(stockEntry.getKey()); // Give the original, clean item
                player.sendMessage(ChatColor.GREEN + "Purchase successful!");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_YES, 1.0f, 1.0f);
            } else {
                player.sendMessage(ChatColor.RED + "An unexpected economy error occurred.");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            }
        } else {
            player.sendMessage(ChatColor.RED + "You cannot afford this item.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        }
    }
}
