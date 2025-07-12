package com.vortex.mythicforge.gui;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * An advanced abstract GUI template for any menu that needs to display items across multiple pages.
 * It automatically handles page calculation, navigation button creation, and page-turning logic.
 *
 * @author Vortex
 * @version 1.0.0
 */
public abstract class PaginatedGui extends AbstractGui {

    protected int currentPage = 0;
    protected final int slotsPerPage;
    protected final List<ItemStack> itemsToDisplay = new ArrayList<>();

    /**
     * Constructs a new PaginatedGui.
     * @param player The player viewing the GUI.
     * @param slotsPerPage The number of slots available for content items (excluding navigation).
     */
    public PaginatedGui(Player player, int slotsPerPage) {
        super(player);
        this.slotsPerPage = slotsPerPage;
    }

    /**
     * This method must be called by the subclass to populate the list of items
     * that will be displayed across the pages.
     */
    protected abstract void populateItems();

    /**
     * The specific logic for handling a click on a content item (not a navigation button).
     * @param event The inventory click event.
     */
    protected abstract void handleContentClick(InventoryClickEvent event);

    @Override
    protected final void handleClick(InventoryClickEvent event) {
        // Prevent taking items from the GUI by default
        event.setCancelled(true);
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

        // --- Handle Navigation Clicks ---
        int maxPages = (int) Math.ceil((double) itemsToDisplay.size() / slotsPerPage);
        String displayName = clickedItem.getItemMeta().getDisplayName();
        String nextPageName = getNavButtonName("next_page");
        String prevPageName = getNavButtonName("previous_page");

        if (displayName.equals(nextPageName)) {
            if (currentPage < maxPages - 1) {
                currentPage++;
                refreshInventory(); // Refresh to show the new page
            }
            return;
        }

        if (displayName.equals(prevPageName)) {
            if (currentPage > 0) {
                currentPage--;
                refreshInventory(); // Refresh to show the new page
            }
            return;
        }
        
        // If it wasn't a navigation click, let the subclass handle it.
        handleContentClick(event);
    }

    /**
     * Refreshes the items displayed in the inventory based on the current page.
     */
    protected void refreshInventory() {
        inventory.clear();
        addNavigationButtons();
        populatePageItems();
    }

    /**
     * Adds the "Next Page" and "Previous Page" buttons to the inventory.
     */
    private void addNavigationButtons() {
        int maxPages = (int) Math.ceil((double) itemsToDisplay.size() / slotsPerPage);
        int inventorySize = inventory.getSize();

        if (currentPage > 0) {
            inventory.setItem(inventorySize - 9, createNavButton("previous_page"));
        }

        if (currentPage < maxPages - 1) {
            inventory.setItem(inventorySize - 1, createNavButton("next_page"));
        }
    }

    /**
     * Fills the main body of the GUI with the items for the current page.
     */
    private void populatePageItems() {
        int startIndex = currentPage * slotsPerPage;
        for (int i = 0; i < slotsPerPage; i++) {
            int itemIndex = startIndex + i;
            if (itemIndex >= itemsToDisplay.size()) break;
            inventory.setItem(i, itemsToDisplay.get(itemIndex));
        }
    }

    private ItemStack createNavButton(String type) {
        ConfigurationSection config = plugin.getConfig().getConfigurationSection("gui_settings.pagination_items." + type);
        Material material = Material.valueOf(config.getString("material", "STONE").toUpperCase());
        ItemStack button = new ItemStack(material);
        ItemMeta meta = button.getItemMeta();
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', config.getString("display_name")));
        meta.setLore((List<String>) config.getList("lore"));
        button.setItemMeta(meta);
        return button;
    }

    private String getNavButtonName(String type) {
        return ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString("gui_settings.pagination_items." + type + ".display_name"));
    }
                                             }
