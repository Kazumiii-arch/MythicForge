package com.vortex.mythicforge.gui;

import com.vortex.mythicforge.MythicForge;
import com.vortex.mythicforge.managers.ShopManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The GUI implementation for the dynamic, rotating enchantment shop.
 * Extends PaginatedGui to automatically handle multiple pages of items for sale.
 *
 * @author Vortex
 * @version 1.0.1
 */
public final class RotatingShopGui extends PaginatedGui {

    private final ShopManager shopManager;
    // NBT key to securely identify which shop item was clicked
    private final NamespacedKey stockIndexKey;

    public RotatingShopGui(Player player) {
        // We have 5 rows total (45 slots). The bottom row (9 slots) is for navigation.
        // This leaves 36 slots per page for shop items.
        super(player, 36);
        if (player != null) { // Null check for dummy registration
            this.shopManager = plugin.getShopManager();
            this.stockIndexKey = new NamespacedKey(plugin, "mythic_shop_index");
            populateItems(); // Fill the master item list
            open(); // Build and show the GUI
        } else {
            this.shopManager = null;
            this.stockIndexKey = null;
        }
    }

    @Override
    protected Inventory createInventory() {
        String title = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("enchant_shop.gui_title", "&5Mysterious Wares"));
        int rows = plugin.getConfig().getInt("enchant_shop.gui_rows", 3) + 1; // Add 1 row for navigation
        if (rows > 6) rows = 6;

        // CORRECTED: Use 'null' as the owner to prevent InventoryHolder errors.
        Inventory inv = Bukkit.createInventory(null, rows * 9, title);
        
        // This parent method populates the first page and adds navigation buttons.
        refreshInventory();
        return inv;
    }

    @Override
    protected void populateItems() {
        List<Map.Entry<ItemStack, Double>> stock = shopManager.getCurrentStock();
        if (stock == null) return;

        // For each item from the manager, add price lore and an ID tag, then add it to the display list.
        for (int i = 0; i < stock.size(); i++) {
            Map.Entry<ItemStack, Double> entry = stock.get(i);
            ItemStack item = entry.getKey().clone();
            double price = entry.getValue();
            ItemMeta meta = item.getItemMeta();

            List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
            lore.add(""); // Spacer
            lore.add(ChatColor.YELLOW + "Price: " + ChatColor.GOLD + plugin.getVaultHook().format(price));
            meta.setLore(lore);

            // Securely tag the item with its index in the shop stock list.
            meta.getPersistentDataContainer().set(stockIndexKey, PersistentDataType.INTEGER, i);
            item.setItemMeta(meta);
            
            this.itemsToDisplay.add(item);
        }
    }

    @Override
    protected void handleContentClick(InventoryClickEvent event) {
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || !clickedItem.hasItemMeta()) return;

        ItemMeta meta = clickedItem.getItemMeta();
        Integer stockIndex = meta.getPersistentDataContainer().get(stockIndexKey, PersistentDataType.INTEGER);

        // If the item doesn't have our index tag, it's not a valid shop item.
        if (stockIndex == null) return;

        List<Map.Entry<ItemStack, Double>> stock = shopManager.getCurrentStock();
        if (stockIndex < 0 || stockIndex >= stock.size()) return; // Index out of bounds

        // Get the authoritative item and price directly from the manager.
        Map.Entry<ItemStack, Double> stockEntry = stock.get(stockIndex);
        double price = stockEntry.getValue();

        // --- Purchase Logic ---
        if (plugin.getVaultHook().hasEnough(player, price)) {
            if (plugin.getVaultHook().withdraw(player, price).transactionSuccess()) {
                player.getInventory().addItem(stockEntry.getKey().clone()); // Give a clean clone of the original item
                player.sendMessage(ChatColor.GREEN + "Purchase successful!");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_YES, 1.0f, 1.0f);
                player.closeInventory(); // Close GUI on successful purchase
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
