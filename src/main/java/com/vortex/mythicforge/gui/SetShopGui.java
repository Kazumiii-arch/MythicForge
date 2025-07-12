package com.vortex.mythicforge.gui;

import com.vortex.mythicforge.MythicForge;
import com.vortex.mythicforge.managers.SetShopManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles the advanced, multi-layered GUI for the Pre-Made Set Gear Shop.
 * Manages navigation between the main category view and individual set views.
 *
 * @author Vortex
 * @version 1.0.0
 */
public final class SetShopGui extends AbstractGui {

    private final SetShopManager shopManager;
    private View currentView = View.MAIN_MENU;
    private String viewingCategory = null;

    // NBT Keys for identifying items and their data within the GUI
    private final NamespacedKey categoryKey = new NamespacedKey(plugin, "gui_category_id");
    private final NamespacedKey actionKey = new NamespacedKey(plugin, "gui_action");
    private final NamespacedKey priceKey = new NamespacedKey(plugin, "gui_price");
    private final NamespacedKey itemDataKey = new NamespacedKey(plugin, "gui_item_data");


    public SetShopGui(Player player) {
        super(player);
        this.shopManager = plugin.getSetShopManager();
        open();
    }

    @Override
    protected Inventory createInventory() {
        // The initial view is always the main menu.
        return buildMainMenuView();
    }
    
    // --- View Builders ---

    private Inventory buildMainMenuView() {
        this.currentView = View.MAIN_MENU;
        String title = ChatColor.translateAlternateColorCodes('&', shopManager.getShopConfig().getString("main_gui_title"));
        Inventory gui = Bukkit.createInventory(this, 27, title);

        ConfigurationSection categories = shopManager.getShopConfig().getConfigurationSection("categories");
        if (categories != null) {
            for (String categoryId : categories.getKeys(false)) {
                ConfigurationSection catConfig = categories.getConfigurationSection(categoryId);
                Material material = Material.valueOf(catConfig.getString("display_item", "STONE").toUpperCase());
                ItemStack catItem = new ItemStack(material);
                ItemMeta meta = catItem.getItemMeta();
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', catConfig.getString("display_name")));
                meta.setLore(colorize(catConfig.getStringList("lore")));
                meta.getPersistentDataContainer().set(categoryKey, PersistentDataType.STRING, categoryId);
                catItem.setItemMeta(meta);
                gui.setItem(catConfig.getInt("slot", 13), catItem);
            }
        }
        return gui;
    }

    private void openCategoryView(String categoryId) {
        this.currentView = View.CATEGORY;
        this.viewingCategory = categoryId;
        String title = ChatColor.translateAlternateColorCodes('&', shopManager.getShopConfig().getString("category_gui_title")
                .replace("{category_name}", shopManager.getShopConfig().getString("categories." + categoryId + ".display_name")));
        
        // Re-create the inventory for the new view
        this.inventory = Bukkit.createInventory(this, 54, title);
        
        // ... (Logic to populate the inventory with gear set pieces from the config)
        // ... (For each piece, create the item, add price to lore, and set NBT data for price/ID)

        // Add a "Back" button
        ItemStack backButton = new ItemStack(Material.BARRIER);
        ItemMeta meta = backButton.getItemMeta();
        meta.setDisplayName(ChatColor.RED + "" + ChatColor.BOLD + "Back to Categories");
        meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, "back");
        backButton.setItemMeta(meta);
        inventory.setItem(45, backButton);

        player.openInventory(inventory);
    }
    
    // --- Event Handling ---

    @Override
    protected void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getItemMeta() == null) return;

        ItemMeta meta = clickedItem.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        if (currentView == View.MAIN_MENU && pdc.has(categoryKey, PersistentDataType.STRING)) {
            // Player clicked a category, switch to that view
            String categoryId = pdc.get(categoryKey, PersistentDataType.STRING);
            openCategoryView(categoryId);
        } else if (currentView == View.CATEGORY) {
            // Player is in a category view
            if (pdc.has(actionKey, PersistentDataType.STRING) && pdc.get(actionKey, PersistentDataType.STRING).equals("back")) {
                // Player clicked the back button
                this.inventory = buildMainMenuView();
                player.openInventory(inventory);
                return;
            }

            if (pdc.has(priceKey, PersistentDataType.DOUBLE)) {
                // Player clicked an item to purchase
                handlePurchase(clickedItem);
            }
        }
    }

    private void handlePurchase(ItemStack item) {
        // ... (Full purchase logic similar to RotatingShopGui)
        // 1. Get price from the item's NBT.
        // 2. Check player balance via VaultHook.
        // 3. Withdraw money, give item, play sounds, and send messages.
    }
    
    // Helper to colorize lists of strings
    private List<String> colorize(List<String> list) {
        List<String> coloredList = new ArrayList<>();
        for (String s : list) {
            coloredList.add(ChatColor.translateAlternateColorCodes('&', s));
        }
        return coloredList;
    }
    
    // Enum to manage which GUI view is currently active
    private enum View {
        MAIN_MENU,
        CATEGORY
    }
          }
