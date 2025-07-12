package com.vortex.mythicforge.managers;

import com.vortex.mythicforge.MythicForge;
import com.vortex.mythicforge.enchants.Rune;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * Manages the loading, storage, and retrieval of all custom runes
 * from the runes.yml configuration file.
 *
 * @author Vortex
 * @version 1.0.0
 */
public final class RuneManager {

    private final MythicForge plugin;
    private final Map<String, Rune> registeredRunes = new HashMap<>();

    public RuneManager(MythicForge plugin) {
        this.plugin = plugin;
    }

    /**
     * Clears existing runes from memory and loads all rune definitions
     * from runes.yml. This method is safe to be called for reloads.
     */
    public void loadRunes() {
        registeredRunes.clear();

        File runesFile = new File(plugin.getDataFolder(), "runes.yml");
        if (!runesFile.exists()) {
            // Save the default runes.yml with examples for the user.
            plugin.saveResource("runes.yml", false);
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(runesFile);
        ConfigurationSection runesSection = config.getConfigurationSection("runes");

        if (runesSection == null) {
            plugin.getLogger().warning("Could not find the 'runes' section in runes.yml. No runes will be loaded.");
            return;
        }

        for (String runeId : runesSection.getKeys(false)) {
            String path = "runes." + runeId;
            try {
                // --- Data Validation ---
                String materialName = config.getString(path + ".item.material");
                if (materialName == null) {
                    plugin.getLogger().warning("Skipping rune '" + runeId + "': Missing 'item.material' field.");
                    continue;
                }

                Material material = Material.matchMaterial(materialName.toUpperCase());
                if (material == null) {
                    plugin.getLogger().warning("Skipping rune '" + runeId + "': Invalid material specified: " + materialName);
                    continue;
                }

                // --- Object Creation ---
                Rune rune = new Rune(
                        runeId,
                        config.getString(path + ".tier", "common"),
                        config.getString(path + ".display_name", runeId),
                        material,
                        config.getStringList(path + ".item.lore"),
                        config.getStringList(path + ".effects"),
                        config.getBoolean(path + ".glow", false)
                );

                // Register the rune, using lowercase for case-insensitive lookups
                registeredRunes.put(runeId.toLowerCase(), rune);

            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "An error occurred while loading rune: " + runeId, e);
            }
        }
        plugin.getLogger().info("Successfully loaded " + registeredRunes.size() + " runes.");
    }

    /**
     * Retrieves a custom rune by its unique ID.
     *
     * @param id The case-insensitive ID of the rune.
     * @return The Rune object, or null if not found.
     */
    public Rune getRuneById(String id) {
        if (id == null) return null;
        return registeredRunes.get(id.toLowerCase());
    }

    /**
     * Gets an unmodifiable view of all registered runes.
     *
     * @return An unmodifiable Map of rune IDs to Rune objects.
     */
    public Map<String, Rune> getRegisteredRunes() {
        return Collections.unmodifiableMap(registeredRunes);
    }
}
