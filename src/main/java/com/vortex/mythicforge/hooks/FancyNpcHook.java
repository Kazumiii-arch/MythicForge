package com.vortex.mythicforge.hooks;

import com.vortex.mythicforge.MythicForge;
import de.oliver.fancynpcs.api.FancyNpcs;
import de.oliver.fancynpcs.api.Npc;
import org.bukkit.entity.Entity;
import java.util.Optional;

/**
 * Handles all interactions with the FancyNpcs plugin API.
 * This class provides a safe communication layer, allowing MythicForge to function
 * even if FancyNpcs is not installed on the server.
 *
 * @author Vortex
 * @version 1.0.1
 */
public final class FancyNpcHook {

    private final boolean isEnabled;
    private static final String NPC_ROLE_METADATA_KEY = "mythicforge_role";

    public FancyNpcHook(MythicForge plugin) {
        if (plugin.getServer().getPluginManager().getPlugin("FancyNpcs") != null) {
            this.isEnabled = true;
            plugin.getLogger().info("Successfully hooked into FancyNpcs.");
        } else {
            this.isEnabled = false;
            plugin.getLogger().warning("FancyNpcs not found. NPC-related features will be disabled.");
        }
    }

    /**
     * Checks if the hook to FancyNpcs is active and ready to be used.
     *
     * @return true if FancyNpcs is installed and enabled, false otherwise.
     */
    public boolean isEnabled() {
        return isEnabled;
    }

    /**
     * Assigns a MythicForge role to a FancyNPC, making it persistent.
     *
     * @param npc The FancyNPC to assign the role to.
     * @param role The role name to assign (e.g., "enchant_shop").
     */
    public void setNpcRole(Npc npc, String role) {
        if (!isEnabled || npc == null) return;
        // Uses the correct, modern API method to add persistent data.
        npc.getData().addPersistentData(NPC_ROLE_METADATA_KEY, role.toLowerCase());
    }

    /**
     * Gets the MythicForge role of a FancyNPC from an entity interaction.
     *
     * @param entity The entity that was interacted with.
     * @return An Optional containing the role name if the entity is a valid NPC with a role, otherwise empty.
     */
    public Optional<String> getNpcRole(Entity entity) {
        if (!isEnabled || entity == null) return Optional.empty();
        
        // Uses the correct, modern API method to get an NPC from an entity.
        Npc npc = FancyNpcs.api().getNpc(entity);
        
        if (npc != null) {
            return Optional.ofNullable(npc.getData().getPersistentData(NPC_ROLE_METADATA_KEY));
        }
        return Optional.empty();
    }
}
