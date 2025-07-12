package com.vortex.mythicforge.hooks;

import com.vortex.mythicforge.MythicForge;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.trait.Owner;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.Optional;

/**
 * Handles all interactions with the Citizens plugin API.
 * This class provides a safe communication layer, allowing MythicForge to function
 * without Citizens being a required dependency. It encapsulates all Citizens-related
 * logic to keep the rest of the plugin clean.
 *
 * @author Vortex
 * @version 1.0.0
 */
public final class CitizensHook {

    private final boolean isEnabled;
    /**
     * The metadata key used to store a MythicForge role on a Citizens NPC.
     * Using a static final key ensures consistency across the plugin.
     */
    private static final String NPC_ROLE_METADATA_KEY = "mythicforge_role";

    public CitizensHook(MythicForge plugin) {
        // Check if the Citizens plugin is present and enabled on the server.
        if (plugin.getServer().getPluginManager().getPlugin("Citizens") != null) {
            this.isEnabled = true;
            plugin.getLogger().info("Successfully hooked into Citizens.");
        } else {
            this.isEnabled = false;
            plugin.getLogger().warning("Citizens not found. NPC-related features will be disabled.");
        }
    }

    /**
     * Checks if the hook to Citizens is active and ready to be used.
     *
     * @return true if Citizens is installed and enabled, false otherwise.
     */
    public boolean isEnabled() {
        return isEnabled;
    }

    /**
     * Assigns a MythicForge role to a Citizens NPC, making it persistent across server restarts.
     *
     * @param npc The target NPC to assign the role to. Must not be null.
     * @param role The role name to assign (e.g., "enchant_shop", "set_shop").
     */
    public void setNpcRole(NPC npc, String role) {
        if (!isEnabled || npc == null) return;
        // Ensure the NPC has the Persist trait so our metadata saves correctly.
        if (!npc.hasTrait(net.citizensnpcs.api.trait.Persist.class)) {
            npc.addTrait(net.citizensnpcs.api.trait.Persist.class);
        }
        npc.data().setPersistent(NPC_ROLE_METADATA_KEY, role.toLowerCase());
    }

    /**
     * Removes a MythicForge role from a Citizens NPC.
     *
     * @param npc The target NPC to remove the role from. Must not be null.
     */
    public void removeNpcRole(NPC npc) {
        if (!isEnabled || npc == null) return;
        npc.data().removePersistent(NPC_ROLE_METADATA_KEY);
    }

    /**
     * Gets the MythicForge role of a Citizens NPC from an entity interaction.
     *
     * @param entity The entity that was interacted with.
     * @return An Optional containing the role name if the entity is a valid NPC with a role, otherwise an empty Optional.
     */
    public Optional<String> getNpcRole(Entity entity) {
        if (!isEnabled || entity == null || !CitizensAPI.getNPCRegistry().isNPC(entity)) {
            return Optional.empty();
        }
        NPC npc = CitizensAPI.getNPCRegistry().getNPC(entity);
        if (npc != null && npc.data().has(NPC_ROLE_METADATA_KEY)) {
            return Optional.ofNullable(npc.data().get(NPC_ROLE_METADATA_KEY));
        }
        return Optional.empty();
    }

    /**
     * Gets the NPC that a player has selected via the Citizens command "/npc select".
     *
     * @param player The player whose selection should be checked.
     * @return An Optional containing the selected NPC, or an empty Optional if none is selected.
     */
    public Optional<NPC> getSelectedNpc(Player player) {
        if (!isEnabled || player == null) return Optional.empty();
        NPC npc = CitizensAPI.getDefaultNPCSelector().getSelected(player);
        return Optional.ofNullable(npc);
    }
}
