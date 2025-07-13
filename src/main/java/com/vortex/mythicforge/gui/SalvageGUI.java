package com.vortex.mythicforge.gui;

import com.vortex.mythicforge.MythicForge;
import com.vortex.mythicforge.enchants.CustomEnchant;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Handles the GUI and logic for the item salvaging system. This class extends
 * AbstractGui to inherit all boilerplate event handling and management, focusing
 * only on the logic specific to salvaging.
 *
 * @author Vortex
 * @version 1.0.1
 */
public final class SalvageGUI extends AbstractGui {

    // NBT key to read enchantment data from the item being salvaged.
    private final NamespacedKey enchantsKey;

    // GUI Layout constants for easy modification and readability.
    private static final int INPUT_SLOT = 11;
    private static final int CONFIRM_SLOT = 13;
    private static final int OUTPUT_SLOT = 15;

    public SalvageGUI(Player player) {
        super(player);
        if (player != null) { // Null check for dummy registration
             this.enchantsKey = new NamespacedKey(plugin, "mythic_enchants_json");
             open(); // Immediately open the GUI upon creation for a player.
        } else {
            this.enchantsKey = null; // Should not be used by dummy instance
        }
    }

    /**
     * Creates the static layout of the Salvage Station GUI.
     * @return The constructed Inventory object.
     */
    @Override
    protected Inventory createInventory() {
        String title = ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString("mechanics.salvage_system.gui_title", "&8Salvage Station"));
        Inventory gui = Bukkit.createInventory(player, 27, title);

        // Fill the background with decorative panes
        ItemStack pane = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta paneMeta = pane.getItemMeta();
        paneMeta.setDisplayName(" ");
        pane.setItemMeta(paneMeta);

        for (int i = 0; i < gui.getSize(); i++) {
            // Leave the interactive slots empty
            if (i != INPUT_SLOT && i != OUTPUT_SLOT) {
                gui.setItem(i, pane);
            }
        }

        // Place the initial confirm button
        updateSalvageButton(gui, 0);

        return gui;
    }

    /**
     * Handles all player click events within this specific GUI instance.
     * @param event The inventory click event provided by the parent AbstractGui.
     */
    @Override
    protected void handleClick(InventoryClickEvent event) {
        // Allow players to place/remove items from the input slot.
        if (event.getSlot() == INPUT_SLOT) {
            event.setCancelled(false);
            // Schedule a task to update the button 1 tick later, after the inventory has changed.
            new BukkitRunnable() {
                @Override
                public void run() {
                    updateSalvageButton(inventory, calculateDustYield(inventory.getItem(INPUT_SLOT)));
                }
            }.runTaskLater(plugin, 1L);
            return;
        }

        // Allow players to take items from the output slot.
        if (event.getSlot() == OUTPUT_SLOT && event.getCurrentItem() != null) {
            event.setCancelled(false);
            return;
        }

        // For all other slots, cancel the event to prevent item moving.
        event.setCancelled(true);

        // Handle the confirm button click logic.
        if (event.getSlot() == CONFIRM_SLOT) {
            ItemStack itemToSalvage = inventory.getItem(INPUT_SLOT);

            if (itemToSalvage == null || itemToSalvage.getType() == Material.AIR) {
                player.sendMessage(ChatColor.RED + "Please place an item in the salvage slot.");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                return;
            }

            int dustYield = calculateDustYield(itemToSalvage);
            if (dustYield <= 0) {
                player.sendMessage(ChatColor.YELLOW + "This item would not yield any Mythic Dust.");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                return;
            }

            inventory.setItem(INPUT_SLOT, null); // Consume the item
            inventory.setItem(OUTPUT_SLOT, plugin.getTomeManager().createMythicDust(dustYield)); // Give dust
            updateSalvageButton(inventory, 0); // Reset the confirm button
            player.playSound(player.getLocation(), Sound.BLOCK_GRINDSTONE_USE, 1.0f, 1.2f);
        }
    }

    /**
     * Calculates the amount of dust yielded from an item's enchantments.
     * @param item The item to calculate the yield for.
     * @return The total amount of dust to be returned.
     */
    private int calculateDustYield(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return 0;
        
        Map<String, Integer> enchants = plugin.getItemManager().getEnchants(item.getItemMeta());
        if (enchants.isEmpty()) return 0;

        int totalDust = 0;
        FileConfiguration config = plugin.getConfig();

        for (Map.Entry<String, Integer> entry : enchants.entrySet()) {
            CustomEnchant enchant = plugin.getEnchantmentManager().getEnchantById(entry.getKey());
            if (enchant == null) continue;
            
            int level = entry.getValue();
            int dustPerLevel = config.getInt("mechanics.salvage_system.dust_yield." + enchant.getTier(), 0);
            totalDust += (level * dustPerLevel);
        }
        return totalDust;
    }

    /**
     * Updates the confirm button's name, lore, and material based on the potential dust yield.
     * @param inventory The GUI inventory.
     * @param dustYield The calculated dust yield.
     */
    private void updateSalvageButton(Inventory inventory, int dustYield) {
        boolean canSalvage = dustYield > 0;
        Material material = canSalvage ? Material.LIME_STAINED_GLASS_PANE : Material.ANVIL;
        ItemStack confirmButton = new ItemStack(material);
        ItemMeta confirmMeta = confirmButton.getItemMeta();

        if (canSalvage) {
            confirmMeta.setDisplayName(ChatColor.GREEN + "" + ChatColor.BOLD + "Confirm Salvage");
            confirmMeta.setLore(Arrays.asList(
                ChatColor.GRAY + "This will destroy the item.",
                "",
                ChatColor.YELLOW + "Yield: " + ChatColor.WHITE + dustYield + " Mythic Dust"
            ));
        } else {
            confirmMeta.setDisplayName(ChatColor.RED + "" + ChatColor.BOLD + "Salvage Item");
            confirmMeta.setLore(Arrays.asList(
                ChatColor.GRAY + "Place an enchanted item to the left.",
                ChatColor.GRAY + "Click here to break it down into dust."
            ));
        }
        confirmButton.setItemMeta(confirmMeta);
        inventory.setItem(CONFIRM_SLOT, confirmButton);
    }
            }
