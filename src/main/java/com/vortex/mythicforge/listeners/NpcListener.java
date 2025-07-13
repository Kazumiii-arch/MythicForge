package com.vortex.mythicforge.listeners;

import com.vortex.mythicforge.MythicForge;
import com.vortex.mythicforge.gui.RotatingShopGui;
import com.vortex.mythicforge.gui.SalvageGUI;
import com.vortex.mythicforge.gui.SetShopGui;
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
 * @version 1.0.0
 */
public final class NpcListener implements Listener {

    private final MythicForge plugin = MythicForge.getInstance();
    // A temporary map to store which player is assigning which role.
    private static final Map<UUID, String> roleSetters = new HashMap<>();

    /**
     * Called by the command system to flag a player for role assignment.
     * @param player The admin player assigning a role.
     * @param role The role to be assigned on the next click.
     */
    public static void setPlayerForRoleAssignment(Player player, String role) {
        roleSetters.put(player.getUniqueId(), role);
    }

    @EventHandler
    public void onNpcInteract(PlayerInteractEntityEvent event) {
        // Ensure the interaction is a primary right-click to avoid double-firing.
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Player player = event.getPlayer();
        Npc npc = FancyNpcs.getInstance().getNpcManager().getNpc(event.getRightClicked());

        // The entity must be a valid FancyNPC to proceed.
        if (npc == null) {
            return;
        }
        
        // --- Admin Role Assignment Logic ---
        // Check if the interacting player is in the process of setting a role.
        if (roleSetters.containsKey(player.getUniqueId())) {
            event.setCancelled(true);
            String roleToSet = roleSetters.remove(player.getUniqueId());
            
            plugin.getFancyNpcHook().setNpcRole(npc, roleToSet);
            player.sendMessage(ChatColor.GREEN + "Successfully set NPC '" + npc.getData().getName() + "' role to: " + roleToSet);
            return; // Stop further processing
        }
        
        // --- Player GUI Opening Logic ---
        // Use our safe hook to get the NPC's role.
        Optional<String> roleOptional = plugin.getFancyNpcHook().getNpcRole(event.getRightClicked());

        if (roleOptional.isPresent()) {
            // Prevent any default NPC behavior (like opening a default editor).
            event.setCancelled(true);
            String role = roleOptional.get();

            // Use a switch to handle different roles. This is now fully functional.
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
                default:
                    // This role is not recognized by MythicForge, do nothing.
                    break;
            }
        }
    }
}
