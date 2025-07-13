package com.vortex.mythicforge.gui;

import com.vortex.mythicforge.MythicForge;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;

public abstract class AbstractGui implements Listener {

    protected final MythicForge plugin = MythicForge.getInstance();
    protected final Player player;
    protected Inventory inventory;

    public AbstractGui(Player player) {
        this.player = player;
    }

    protected abstract Inventory createInventory();

    protected abstract void handleClick(InventoryClickEvent event);

    public void open() {
        this.inventory = createInventory();
        Bukkit.getPluginManager().registerEvents(this, plugin);
        player.openInventory(this.inventory);
    }

    @EventHandler
    public final void onInventoryClick(InventoryClickEvent event) {
        if (!event.getInventory().equals(inventory)) return;
        if (event.getClickedInventory() != inventory && event.getClickedInventory() != null) return;
        handleClick(event);
    }

    @EventHandler
    public final void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().equals(this.inventory)) {
            HandlerList.unregisterAll(this);
        }
    }
    
    public Inventory getInventory() {
        return inventory;
    }
            }
