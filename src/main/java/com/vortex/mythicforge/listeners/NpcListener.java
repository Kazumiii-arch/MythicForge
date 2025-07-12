package com.vortex.mythicforge.listeners;

import com.vortex.mythicforge.MythicForge;
import com.vortex.mythicforge.gui.RotatingShopGui;
import com.vortex.mythicforge.gui.SalvageGUI;
import com.vortex.mythicforge.gui.SetShopGui;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.Optional;

/**
 * Listens for player interactions with Citizens NPCs and opens the
 * appropriate MythicForge GUI based on the NPC's assigned role. This class
 * serves as the central router for all NPC-based interactions.
 *
 * @author Vortex
 * @version 1.0.0
 */
public final class NpcListener implements Listener {

    private final MythicForge plugin = MythicForge.getInstance();

    /**
     * Handles right-clicks on any entity to check if it's a MythicForge NPC.
     *
     * @param event The entity interaction event.
     */
    @EventHandler
    public void onNpcInteract(PlayerInteractEntityEvent event) {
        // Ensure the interaction is a primary right-click to avoid double-firing.
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        // Use our safe hook to get the NPC's role.
        Optional<String> roleOptional = plugin.getCitizensHook().getNpcRole(event.getRightClicked());

        // If the NPC has one of our roles, proceed.
        if (roleOptional.isPresent()) {
            // Prevent any default Citizens NPC behavior (like opening a default editor).
            event.setCancelled(true);

            Player player = event.getPlayer();
            String role = roleOptional.get();

            // Use a switch to handle different roles. This makes the system easily expandable.
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

                // Add placeholders for future NPC roles to guide server admins.
                case "enchanter":
                case "rune_trader":
                    player.sendMessage(ChatColor.GOLD + "[NPC] " + ChatColor.WHITE + "This feature is not yet available.");
                    break;
                    
                default:
                    // Do nothing if the role is not recognized.
                    break;
            }
        }
    }
}
