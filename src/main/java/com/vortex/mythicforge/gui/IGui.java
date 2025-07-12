package com.vortex.mythicforge.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

/**
 * The core interface for all custom GUIs in MythicForge.
 * It defines the essential contract that every GUI must adhere to, ensuring
 * they can be properly managed by the GuiManager and interacted with by the system.
 *
 * @author Vortex
 * @version 1.0.0
 */
public interface IGui {

    /**
     * Opens the GUI for the player this instance was constructed for.
     * This method is responsible for creating the inventory and showing it to the player.
     */
    void open();

    /**
     * Gets the underlying Bukkit Inventory object for this GUI.
     * This is crucial for event handlers to check if a clicked inventory
     * belongs to this specific GUI instance.
     *
     * @return The Bukkit Inventory associated with this GUI.
     */
    Inventory getInventory();

}
