package com.vortex.mythicforge.enchants;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A final, immutable data object representing a single Rune's properties
 * as loaded from the runes.yml configuration file. This class ensures that
 * rune data remains consistent and safe throughout the plugin's runtime.
 *
 * @author Vortex
 * @version 1.0.0
 */
public final class Rune {

    /** The unique, case-insensitive internal ID for this rune (e.g., 'rune_of_haste'). */
    private final String id;

    /** The tier ID which can determine rarity or other mechanics (e.g., 'advanced'). */
    private final String tier;

    /** The formatted string for the rune's display name. */
    private final String displayName;

    /** The Bukkit Material for the rune's physical item representation. */
    private final Material itemMaterial;

    /** The formatted list of strings for the rune item's lore. */
    private final List<String> itemLore;

    /** The list of effect strings this rune applies (e.g., 'POTION:SPEED:0'). */
    private final List<String> effects;

    /** Determines if the item should have an enchantment glow when this rune is socketed. */
    private final boolean glow;

    /**
     * Constructs a new, immutable Rune object.
     *
     * @param id The unique internal ID. Must not be null.
     * @param tier The tier ID. Must not be null.
     * @param displayName The display name. Must not be null.
     * @param itemMaterial The material of the item. Must not be null.
     * @param itemLore The lore for the item. Must not be null.
     * @param effects The list of effects the rune provides. Must not be null.
     * @param glow Whether the rune should make the host item glow.
     */
    public Rune(String id, String tier, String displayName, Material itemMaterial, List<String> itemLore, List<String> effects, boolean glow) {
        // Ensure critical data is never null to prevent runtime errors.
        this.id = Objects.requireNonNull(id, "Rune ID cannot be null.");
        this.tier = Objects.requireNonNull(tier, "Rune tier cannot be null.");
        this.displayName = Objects.requireNonNull(displayName, "Rune displayName cannot be null.");
        this.itemMaterial = Objects.requireNonNull(itemMaterial, "Rune itemMaterial cannot be null.");

        // Create defensive copies of collections to ensure immutability.
        this.itemLore = new ArrayList<>(Objects.requireNonNull(itemLore, "Rune itemLore cannot be null."));
        this.effects = new ArrayList<>(Objects.requireNonNull(effects, "Rune effects cannot be null."));
        
        this.glow = glow;
    }

    /**
     * @return The unique internal ID of the rune.
     */
    public String getId() {
        return id;
    }

    /**
     * @return The tier ID of the rune.
     */
    public String getTier() {
        return tier;
    }

    /**
     * @return The display name, including color codes.
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * @return The Bukkit Material of the rune item.
     */
    public Material getItemMaterial() {
        return itemMaterial;
    }

    /**
     * @return An unmodifiable list of the rune item's lore lines.
     */
    public List<String> getItemLore() {
        return Collections.unmodifiableList(itemLore);
    }

    /**
     * @return An unmodifiable list of the rune's effect strings.
     */
    public List<String> getEffects() {
        return Collections.unmodifiableList(effects);
    }

    /**

     * @return true if this rune should make the host item glow, false otherwise.
     */
    public boolean hasGlow() {
        return glow;
    }
}
