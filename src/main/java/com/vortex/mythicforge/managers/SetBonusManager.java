package com.vortex.mythicforge.managers;

import com.vortex.mythicforge.MythicForge;
import com.vortex.mythicforge.enchants.SetBonus;
import com.vortex.mythicforge.enchants.SetBonus.BonusTier;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * Manages the loading, storage, and retrieval of all gear set bonuses
 * from the /sets/ directory. It also provides utility methods to determine
 * a player's active set bonus.
 *
 * @author Vortex
 * @version 1.0.1
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
            plugin.saveResource("sets/wither_king.yml", false);
            plugin.saveResource("sets/frozen_king.yml", false);
        }

        File[] setFiles = setsDir.listFiles((dir, name) -> name.endsWith(".yml"));
        if (setFiles == null) return;

        for (File file : setFiles) {
            FileConfiguration config = YamlConfiguration.loadConfiguration(file);
            try {
                String setId = config.getString("set_id");
                if (setId == null || setId.isEmpty()) {
                    plugin.getLogger().warning("Skipping file " + file.getName() + ": Missing 'set_id' field.");
                    continue;
                }

                List<Map<?, ?>> rawBonusTiers = config.getMapList("bonuses");
                if (rawBonusTiers.isEmpty()) continue;

                List<BonusTier> bonusTiers = new ArrayList<>();
                for (Map<?, ?> rawTier : rawBonusTiers) {
                    if (!(rawTier.get("pieces_required") instanceof Integer)) continue;
                    int piecesRequired = (int) rawTier.get("pieces_required");
                    
                    // Safe parsing for lists
                    List<String> passiveEffects = getOrDefaultAsListOf(rawTier, "passive_effects", String.class);
                    List<Map<?, ?>> triggeredEffects = getOrDefaultAsListOf(rawTier, "triggered_effects", Map.class);

                    bonusTiers.add(new BonusTier(piecesRequired, passiveEffects, triggeredEffects));
                }
                
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
     * Gets an unmodifiable view of all registered set bonuses.
     * @return An unmodifiable Collection of all SetBonus objects.
     */
    public Collection<SetBonus> getAllSets() {
        return Collections.unmodifiableCollection(registeredSets.values());
    }

    /**
     * A helper method for safely getting a list of a specific type from a map.
     * @return A list of the specified type, or an empty list.
     */
    @SuppressWarnings("unchecked")
    private <T> List<T> getOrDefaultAsListOf(Map<?, ?> map, String key, Class<T> type) {
        Object obj = map.get(key);
        if (obj instanceof List && !((List<?>) obj).isEmpty()) {
            // Check if the first element is of the correct type
            if (type.isInstance(((List<?>) obj).get(0))) {
                return (List<T>) obj;
            }
        }
        return new ArrayList<>();
    }
                        }
