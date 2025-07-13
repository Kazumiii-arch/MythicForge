package com.vortex.mythicforge.hooks;

import com.vortex.mythicforge.MythicForge;
import de.oliver.fancynpcs.api.FancyNpcs;
import de.oliver.fancynpcs.api.Npc;
import de.oliver.fancynpcs.api.NpcData;
import org.bukkit.entity.Entity;

import java.util.Optional;

/**
 * Handles all interactions with the FancyNpcs plugin API.
 * Provides a safe layer of communication for all NPC-related features.
 *
 * @author Vortex
 * @version 1.0.0
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

    public boolean isEnabled() {
        return isEnabled;
    }

    /**
     * Assigns a MythicForge role to a FancyNPC.
     * @param npc The FancyNPC to assign the role to.
     * @param role The role to assign (e.g., "enchant_shop").
     */
    public void setNpcRole(Npc npc, String role) {
        if (!isEnabled || npc == null) return;
        npc.getData().set(NPC_ROLE_METADATA_KEY, role.toLowerCase());
        // FancyNpcs typically handles saving automatically
    }

    /**
     * Gets the MythicForge role of a FancyNPC.
     * @param entity The entity that was interacted with.
     * @return An Optional containing the role name if it exists, otherwise empty.
     */
    public Optional<String> getNpcRole(Entity entity) {
        if (!isEnabled) return Optional.empty();
        
        Npc npc = FancyNpcs.getInstance().getNpcManager().getNpc(entity);
        if (npc != null) {
            String role = npc.getData().get(NPC_ROLE_METADATA_KEY);
            return Optional.ofNullable(role);
        }
        return Optional.empty();
    }
}
