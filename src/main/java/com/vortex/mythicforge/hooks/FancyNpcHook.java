package com.vortex.mythicforge.hooks;

import com.vortex.mythicforge.MythicForge;
import de.oliver.fancynpcs.api.FancyNpcs;
import de.oliver.fancynpcs.api.Npc;
import de.oliver.fancynpcs.api.NpcData;
import org.bukkit.entity.Entity;
import org.bukkit.persistence.PersistentDataType;
import java.util.Optional;

public final class FancyNpcHook {
    private final boolean isEnabled;
    private static final String NPC_ROLE_METADATA_KEY = "mythicforge_role";

    public FancyNpcHook(MythicForge plugin) {
        if (plugin.getServer().getPluginManager().getPlugin("FancyNpcs") != null) {
            this.isEnabled = true;
        } else {
            this.isEnabled = false;
        }
    }

    public boolean isEnabled() { return isEnabled; }

    public void setNpcRole(Npc npc, String role) {
        if (!isEnabled || npc == null) return;
        NpcData data = npc.getData();
        data.addPersistentData(NPC_ROLE_METADATA_KEY, role.toLowerCase());
    }

    public Optional<String> getNpcRole(Entity entity) {
        if (!isEnabled) return Optional.empty();
        Npc npc = FancyNpcs.api().getNpc(entity);
        if (npc != null) {
            NpcData data = npc.getData();
            return Optional.ofNullable(data.getPersistentData(NPC_ROLE_METADATA_KEY, PersistentDataType.STRING));
        }
        return Optional.empty();
    }
}
