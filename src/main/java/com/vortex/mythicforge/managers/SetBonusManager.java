package com.vortex.mythicforge.managers;

import com.vortex.mythicforge.MythicForge;
import com.vortex.mythicforge.enchants.SetBonus;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;
import java.util.logging.Level;

/**
 * Manages the loading, storage, and retrieval of all gear set bonuses
 * from the /sets/ directory.
 *
 * @author Vortex
 * @version 1.0.0
 */
public final class SetBonusManager {

    private final MythicForge plugin;
    private final Map<String, SetBonus> registeredSets = new HashMap<>();

    public SetBonusManager(MythicForge plugin) {
        this.plugin = plugin;
    }

    /**
     * Clears existing set bonuses from memory and loads all .yml files from the
     * "plugins/MythicForge/sets/" directory. This method is safe for reloads.
     */
    public void loadSets() {
        registeredSets.clear();

        File setsDir = new File(plugin.getDataFolder(), "sets");
        if (!setsDir.exists()) {
            setsDir.mkdirs();
            // Save example files for the user
            plugin.saveResource("sets/wither_king.yml", false);
            plugin.saveResource("sets/frozen_king.yml", false);
        }

        File[] setFiles = setsDir.listFiles((dir, name) -> name.endsWith(".yml"));
        if (setFiles == null) {
            plugin.getLogger().warning("Could not read files from the /sets/ directory.");
            return;
        }

        for (File file : setFiles) {
            FileConfiguration config = YamlConfiguration.loadConfiguration(file);
            try {
                String setId = config.getString("set_id");
                if (setId == null || setId.isEmpty()) {
                    plugin.getLogger().warning("Skipping file " + file.getName() + ": Missing 'set_id' field.");
                    continue;
                }

                // --- Parse Bonus Tiers ---
                List<Map<?, ?>> rawBonusTiers = config.getMapList("bonuses");
                if (rawBonusTiers.isEmpty()) {
                    plugin.getLogger().warning("Skipping set '" + setId + "': No 'bonuses' section found.");
                    continue;
                }

                List<SetBonus.BonusTier> bonusTiers = new ArrayList<>();
                for (Map<?, ?> rawTier : rawBonusTiers) {
                    int piecesRequired = (int) rawTier.get("pieces_required");
                    List<String> passiveEffects = (List<String>) rawTier.getOrDefault("passive_effects", Collections.emptyList());
                    List<Map<?, ?>> triggeredEffects = (List<Map<?, ?>>) rawTier.getOrDefault("triggered_effects", Collections.emptyList());

                    bonusTiers.add(new SetBonus.BonusTier(piecesRequired, passiveEffects, triggeredEffects));
                }
                
                // --- Create Final SetBonus Object ---
                SetBonus setBonus = new SetBonus(
                        setId,
                        config.getString("set_display_name", setId),
                        config.getStringList("required_enchantments"),
                        bonusTiers
                );

                registeredSets.put(setId.toLowerCase(), setBonus);

            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "An error occurred while loading set bonus file: " + file.getName(), e);
            }
        }
        plugin.getLogger().info("Successfully loaded " + registeredSets.size() + " gear sets.");
    }

    /**
     * Retrieves a set bonus by its unique ID.
     *
     * @param id The case-insensitive ID of the set.
     * @return The SetBonus object, or null if not found.
     */
    public SetBonus getSetById(String id) {
        if (id == null) return null;
        return registeredSets.get(id.toLowerCase());
    }

    /**
     * Gets an unmodifiable view of all registered set bonuses.
     * This is used by the ActiveEffectTask to check player gear against all possible sets.
     *
     * @return An unmodifiable Collection of all SetBonus objects.
     */
    public Collection<SetBonus> getAllSets() {
        return Collections.unmodifiableCollection(registeredSets.values());
    }
}
