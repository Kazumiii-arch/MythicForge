package com.vortex.mythicforge.enchants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A final, immutable data object representing a single custom enchantment's properties
 * as loaded from a configuration file. This class ensures that enchantment data
 * remains consistent and safe throughout the plugin's runtime.
 *
 * @author Vortex
 * @version 1.0.0
 */
public final class CustomEnchant {

    /** The unique, case-insensitive internal ID for this enchantment (e.g., 'lifesteal'). */
    private final String id;

    /** The tier ID which determines rarity and mechanics (e.g., 'rare'). */
    private final String tier;

    /** The maximum level this enchantment can be upgraded to. */
    private final int maxLevel;

    /** The formatted string for the display name in an item's lore. */
    private final String displayName;

    /** The formatted list of strings for the description in an item's lore. */
    private final List<String> description;

    /** A list of Bukkit Material names this enchantment can be applied to. */
    private final List<String> applicableTo;

    /** The raw list of maps containing the logic for all triggers, conditions, and effects. */
    private final List<Map<?, ?>> effects;

    /**
     * Constructs a new, immutable CustomEnchant object.
     *
     * @param id The unique internal ID. Must not be null.
     * @param tier The tier ID. Must not be null.
     * @param maxLevel The maximum level.
     * @param displayName The display name format. Must not be null.
     * @param description The lore description lines. Must not be null.
     * @param applicableTo The list of applicable item types. Must not be null.
     * @param effects The list of effect groups. Must not be null.
     */
    public CustomEnchant(String id, String tier, int maxLevel, String displayName, List<String> description, List<String> applicableTo, List<Map<?, ?>> effects) {
        // Use Objects.requireNonNull to ensure critical data is never null, preventing future errors.
        this.id = Objects.requireNonNull(id, "Enchantment ID cannot be null.");
        this.tier = Objects.requireNonNull(tier, "Enchantment tier cannot be null.");
        this.maxLevel = maxLevel;
        this.displayName = Objects.requireNonNull(displayName, "Enchantment displayName cannot be null.");

        // Create defensive copies to ensure the lists within this object cannot be modified externally.
        this.description = new ArrayList<>(Objects.requireNonNull(description, "Description list cannot be null."));
        this.applicableTo = new ArrayList<>(Objects.requireNonNull(applicableTo, "ApplicableTo list cannot be null."));
        this.effects = new ArrayList<>(Objects.requireNonNull(effects, "Effects list cannot be null."));
    }

    /**
     * @return The unique internal ID of the enchantment.
     */
    public String getId() {
        return id;
    }

    /**
     * @return The tier ID of the enchantment.
     */
    public String getTier() {
        return tier;
    }

    /**
     * @return The maximum level this enchantment can reach.
     */
    public int getMaxLevel() {
        return maxLevel;
    }

    /**
     * @return The display name format, including color codes and placeholders.
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * @return An unmodifiable list of the description lines.
     */
    public List<String> getDescription() {
        return Collections.unmodifiableList(description);
    }

    /**
     * @return An unmodifiable list of the applicable Bukkit Material names.
     */
    public List<String> getApplicableTo() {
        return Collections.unmodifiableList(applicableTo);
    }

    /**
     * @return An unmodifiable list of the raw effect group maps.
     */
    public List<Map<?, ?>> getEffects() {
        return Collections.unmodifiableList(effects);
    }
}
