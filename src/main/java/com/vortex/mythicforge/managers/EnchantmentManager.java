package com.vortex.mythicforge.managers;

import com.vortex.mythicforge.MythicForge;
import com.vortex.mythicforge.enchants.CustomEnchant;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.*;
import java.util.logging.Level;

/**
 * Manages the loading, storage, and retrieval of all custom enchantments.
 * This class is the central repository for all enchantment definitions.
 *
 * @author Vortex
 * @version 1.0.0
 */
public final class EnchantmentManager {

    private final MythicForge plugin;
    private final Map<String, CustomEnchant> registeredEnchants = new HashMap<>();

    public EnchantmentManager(MythicForge plugin) {
        this.plugin = plugin;
    }

    /**
     * Clears existing enchantments from memory and loads all .yml files from the
     * "plugins/MythicForge/enchants/" directory. This method is safe to be called for reloads.
     */
    public void loadEnchantments() {
        registeredEnchants.clear();

        File enchantsDir = new File(plugin.getDataFolder(), "enchants");
        if (!enchantsDir.exists()) {
            enchantsDir.mkdirs();
            // Save example files to guide the user on their first startup.
            plugin.saveResource("enchants/frost_weapon.yml", false);
            plugin.saveResource("enchants/wither_helm.yml", false);
        }

        File[] enchantFiles = enchantsDir.listFiles((dir, name) -> name.endsWith(".yml"));
        if (enchantFiles == null) {
            plugin.getLogger().warning("Could not read files from the /enchants/ directory.");
            return;
        }

        for (File file : enchantFiles) {
            FileConfiguration config = YamlConfiguration.loadConfiguration(file);
            try {
                String id = config.getString("id");
                if (id == null || id.isEmpty()) {
                    plugin.getLogger().warning("Skipping file " + file.getName() + ": It does not contain a valid 'id' field.");
                    continue;
                }

                CustomEnchant enchant = new CustomEnchant(
                        id,
                        config.getString("tier", "common"),
                        config.getInt("max_level", 1),
                        config.getString("display_name", "&f" + id),
                        config.getStringList("description"),
                        config.getStringList("applicable_to"),
                        config.getMapList("effects")
                );

                registeredEnchants.put(id.toLowerCase(), enchant);

            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "An error occurred while loading enchantment file: " + file.getName(), e);
            }
        }
        plugin.getLogger().info("Successfully loaded " + registeredEnchants.size() + " enchantments.");
    }

    /**
     * Retrieves a custom enchantment by its unique ID.
     *
     * @param id The case-insensitive ID of the enchantment.
     * @return The CustomEnchant object, or null if not found.
     */
    public CustomEnchant getEnchantById(String id) {
        if (id == null) return null;
        return registeredEnchants.get(id.toLowerCase());
    }

    /**
     * Gets an unmodifiable view of all registered enchantments.
     *
     * @return An unmodifiable Map of enchantment IDs to CustomEnchant objects.
     */
    public Map<String, CustomEnchant> getRegisteredEnchants() {
        return Collections.unmodifiableMap(registeredEnchants);
    }

    /**
     * Checks if a given enchantment can be applied to a specific ItemStack based on the
     * 'applicable_to' list in the enchantment's configuration. This method understands
     * both specific materials and broad categories like "SWORD" or "ARMOR".
     *
     * @param enchant The enchantment to check.
     * @param item The ItemStack to check against.
     * @return true if the enchantment is applicable, false otherwise.
     */
    public boolean isApplicable(CustomEnchant enchant, ItemStack item) {
        if (enchant == null || item == null) return false;

        List<String> applicableTypes = enchant.getApplicableTo();
        if (applicableTypes.isEmpty() || applicableTypes.contains("ALL")) {
            return true;
        }

        String materialName = item.getType().name();

        for (String type : applicableTypes) {
            String upperType = type.toUpperCase();
            switch (upperType) {
                case "SWORD":
                case "SWORDS":
                    if (materialName.endsWith("_SWORD")) return true;
                    break;
                case "AXE":
                case "AXES":
                    if (materialName.endsWith("_AXE")) return true;
                    break;
                case "PICKAXE":
                case "PICKAXES":
                    if (materialName.endsWith("_PICKAXE")) return true;
                    break;
                case "ARMOR":
                    if (materialName.endsWith("_HELMET") || materialName.endsWith("_CHESTPLATE") || materialName.endsWith("_LEGGINGS") || materialName.endsWith("_BOOTS")) return true;
                    break;
                case "TOOL":
                case "TOOLS":
                     if (materialName.endsWith("_AXE") || materialName.endsWith("_PICKAXE") || materialName.endsWith("_SHOVEL") || materialName.endsWith("_HOE")) return true;
                     break;
                default:
                    // Check for a direct material match
                    if (materialName.equals(upperType)) return true;
                    break;
            }
        }
        return false;
    }
          }
