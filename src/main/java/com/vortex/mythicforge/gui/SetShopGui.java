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

import java.util.*;
import java.util.stream.Collectors;

/**
 * Handles the advanced, multi-layered GUI for the Pre-Made Set Gear Shop.
 * Manages navigation between the main category view and individual set views.
 *
 * @author Vortex
 * @version 1.0.1
 */
public final class SetShopGui extends AbstractGui {

    private final SetShopManager shopManager;
    private View currentView = View.MAIN_MENU;

    // NBT Keys to identify items and their actions within the GUI
    private final NamespacedKey categoryKey;
    private final NamespacedKey actionKey;
    private final NamespacedKey priceKey;
    private final NamespacedKey setIdKey;
    private final NamespacedKey pieceIdKey;

    public SetShopGui(Player player) {
        super(player);
        if (player != null) { // Null check allows for dummy registration
            this.shopManager = plugin.getSetShopManager();
            this.categoryKey = new NamespacedKey(plugin, "mf_gui_category_id");
            this.actionKey = new NamespacedKey(plugin, "mf_gui_action");
            this.priceKey = new NamespacedKey(plugin, "mf_gui_price");
            this.setIdKey = new NamespacedKey(plugin, "mf_gui_set_id");
            this.pieceIdKey = new NamespacedKey(plugin, "mf_gui_piece_id");
            open();
        } else {
            this.shopManager = null;
            this.categoryKey = null; this.actionKey = null; this.priceKey = null;
            this.setIdKey = null; this.pieceIdKey = null;
        }
    }

    @Override
    protected Inventory createInventory() {
        return buildMainMenuView();
    }

    private Inventory buildMainMenuView() {
        this.currentView = View.MAIN_MENU;
        String title = colorize(shopManager.getShopConfig().getString("main_gui_title"));
        Inventory gui = Bukkit.createInventory(null, 27, title);

        ConfigurationSection categories = shopManager.getShopConfig().getConfigurationSection("categories");
        if (categories != null) {
            for (String categoryId : categories.getKeys(false)) {
                ConfigurationSection catConfig = categories.getConfigurationSection(categoryId);
                if (catConfig == null) continue;

                Material material = Material.matchMaterial(catConfig.getString("display_item", "STONE"));
                ItemStack catItem = new ItemStack(material != null ? material : Material.STONE);
                ItemMeta meta = catItem.getItemMeta();
                if(meta == null) continue;

                meta.setDisplayName(colorize(catConfig.getString("display_name")));
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
        String categoryDisplayName = shopManager.getShopConfig().getString("categories." + categoryId + ".display_name", "Shop");
        String title = colorize(shopManager.getShopConfig().getString("category_gui_title")
                .replace("{category_name}", categoryDisplayName));
        
        this.inventory = Bukkit.createInventory(null, 54, title);
        
        ConfigurationSection gearSets = shopManager.getShopConfig().getConfigurationSection("gear_sets");
        if (gearSets != null) {
            int slot = 0;
            for (String setId : gearSets.getKeys(false)) {
                if (Objects.equals(gearSets.getString(setId + ".category"), categoryId)) {
                    ConfigurationSection pieces = gearSets.getConfigurationSection(setId + ".pieces");
                    if (pieces == null) continue;
                    for (String pieceId : pieces.getKeys(false)) {
                        if (slot >= 45) break;
                        ItemStack item = shopManager.getShopItem(setId, pieceId);
                        if (item != null) {
                            ItemMeta meta = item.getItemMeta();
                            double price = pieces.getDouble(pieceId + ".price");
                            
                            // Attach data to the item for the click handler to use
                            meta.getPersistentDataContainer().set(priceKey, PersistentDataType.DOUBLE, price);
                            meta.getPersistentDataContainer().set(setIdKey, PersistentDataType.STRING, setId);
                            meta.getPersistentDataContainer().set(pieceIdKey, PersistentDataType.STRING, pieceId);
                            item.setItemMeta(meta);
                            inventory.setItem(slot++, item);
                        }
                    }
                }
            }
        }
        
        // Add a "Back" button
        ItemStack backButton = new ItemStack(Material.BARRIER);
        ItemMeta meta = backButton.getItemMeta();
        meta.setDisplayName(ChatColor.RED + "" + ChatColor.BOLD + "Back to Categories");
        meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, "back");
        backButton.setItemMeta(meta);
        inventory.setItem(49, backButton); // Center of bottom row

        player.openInventory(inventory);
    }

    @Override
    protected void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getItemMeta() == null) return;

        ItemMeta meta = clickedItem.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        if (currentView == View.MAIN_MENU && pdc.has(categoryKey, PersistentDataType.STRING)) {
            String categoryId = pdc.get(categoryKey, PersistentDataType.STRING);
            openCategoryView(categoryId);
            return;
        }
        
        if (currentView == View.CATEGORY) {
            if (pdc.has(actionKey, PersistentDataType.STRING) && pdc.get(actionKey, PersistentDataType.STRING).equals("back")) {
                new SetShopGui(player); // Re-open the main menu
                return;
            }
            if (pdc.has(priceKey, PersistentDataType.DOUBLE)) {
                handlePurchase(clickedItem);
            }
        }
    }

    private void handlePurchase(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        double price = pdc.getOrDefault(priceKey, PersistentDataType.DOUBLE, -1.0);
        String setId = pdc.get(setIdKey, PersistentDataType.STRING);
        String pieceId = pdc.get(pieceIdKey, PersistentDataType.STRING);

        if (price < 0 || setId == null || pieceId == null) return;
        
        if (plugin.getVaultHook().hasEnough(player, price)) {
            if(plugin.getVaultHook().withdraw(player, price).transactionSuccess()) {
                ItemStack cleanItem = shopManager.getShopItem(setId, pieceId);
                player.getInventory().addItem(cleanItem);
                player.sendMessage(ChatColor.GREEN + "You purchased " + cleanItem.getItemMeta().getDisplayName() + "!");
            } else {
                 player.sendMessage(ChatColor.RED + "An unexpected economy error occurred.");
            }
        } else {
            player.sendMessage(ChatColor.RED + "You cannot afford this item.");
        }
    }
    
    private List<String> colorize(List<String> list) {
        return list.stream().map(this::colorize).collect(Collectors.toList());
    }
    
    private String colorize(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }
    
    private enum View { MAIN_MENU, CATEGORY }
}
