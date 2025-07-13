package com.vortex.mythicforge.listeners;

import com.vortex.mythicforge.MythicForge;
import com.vortex.mythicforge.gui.RotatingShopGui;
import com.vortex.mythicforge.gui.SalvageGUI;
import com.vortex.mythicforge.gui.SetShopGui;
import de.oliver.fancynpcs.api.FancyNpcs;
import de.oliver.fancynpcs.api.Npc;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Listens for player interactions with FancyNpcs and routes them to the correct
 * MythicForge system. This class handles both opening GUIs for players and
 * processing role assignments for administrators.
 *
 * @author Vortex
 * @version 1.0.1
 */
public final class NpcListener implements Listener {

    private final MythicForge plugin = MythicForge.getInstance();
    // A temporary map to store which admin is assigning which role.
    private static final Map<UUID, String> roleSetters = new HashMap<>();

    /**
     * Called by the MythicForgeCommand to flag an admin for role assignment.
     * Their next right-click on a valid NPC will assign the specified role.
     *
     * @param player The admin player assigning a role.
     * @param role The role to be assigned on the next click.
     */
    public static void setPlayerForRoleAssignment(Player player, String role) {
        roleSetters.put(player.getUniqueId(), role);
    }

    /**
     * Handles right-clicks on any entity to check if it's a MythicForge NPC.
     * This method contains two main logical paths: one for admins setting roles,
     * and one for players opening GUIs.
     *
     * @param event The entity interaction event.
     */
    @EventHandler
    public void onNpcInteract(PlayerInteractEntityEvent event) {
        // Ensure the interaction is a primary right-click to avoid double-firing.
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        // Use the modern API to get the NPC object from the clicked entity.
        Npc npc = FancyNpcs.api().getNpc(event.getRightClicked());

        // If the clicked entity is not a valid FancyNPC, do nothing.
        if (npc == null) {
            return;
        }

        Player player = event.getPlayer();

        // --- ADMIN PATH: Role Assignment ---
        // Check if the interacting player is currently in the role assignment map.
        if (roleSetters.containsKey(player.getUniqueId())) {
            event.setCancelled(true); // Prevent any other action
            String roleToSet = roleSetters.remove(player.getUniqueId());

            plugin.getFancyNpcHook().setNpcRole(npc, roleToSet);
            player.sendMessage(ChatColor.GREEN + "Successfully set NPC '" + npc.getData().getName() + "' role to: " + roleToSet);
            return; // Stop further processing after assigning the role.
        }

        // --- PLAYER PATH: Open GUI ---
        // If the player is not assigning a role, check if the NPC has a role to interact with.
        Optional<String> roleOptional = plugin.getFancyNpcHook().getNpcRole(event.getRightClicked());

        if (roleOptional.isPresent()) {
            // Prevent any default NPC behavior (like text popups).
            event.setCancelled(true);
            String role = roleOptional.get();

            // Use a switch to handle different roles and open the correct GUI.
            switch (role) {
                case "enchant_shop":
                    new RotatingShopGui(player);
                    break;
                case "set_shop":
                    new SetShopGui(player);
                    break;
                case "salvage_station":
                    new SalvageGUI(player);
                    break;
                case "enchanter":
                case "rune_trader":
                    player.sendMessage(ChatColor.GOLD + "[NPC] " + ChatColor.WHITE + "This feature is coming soon!");
                    break;
            }
        }
    }
}
