package com.vortex.mythicforge.enchants;

import java.util.*;
import java.util.stream.Collectors;

/**
 * A final, immutable data object representing a complete gear set bonus,
 * as loaded from a configuration file in the /sets/ directory. This class
 * contains all the information needed to identify a set and apply its
 * tiered bonuses to a player.
 *
 * @author Vortex
 * @version 1.0.0
 */
public final class SetBonus {

    /** The unique, case-insensitive internal ID for this set (e.g., 'wither_king'). */
    private final String setId;

    /** The display name for the set bonus, used in GUIs or messages. */
    private final String displayName;

    /** A list of the required enchantment IDs that constitute this set. */
    private final List<String> requiredEnchantments;

    /** A list of BonusTier objects, each representing a different stage of the set bonus. */
    private final List<BonusTier> bonuses;

    /**
     * Constructs a new, immutable SetBonus object.
     *
     * @param setId The unique internal ID. Must not be null.
     * @param displayName The display name. Must not be null.
     * @param requiredEnchantments A list of enchantment IDs required for this set. Must not be null.
     * @param bonusTiers A list of the configured bonus tiers. Must not be null.
     */
    public SetBonus(String setId, String displayName, List<String> requiredEnchantments, List<BonusTier> bonusTiers) {
        this.setId = Objects.requireNonNull(setId, "Set ID cannot be null.");
        this.displayName = Objects.requireNonNull(displayName, "Set display name cannot be null.");

        // Defensive copies to ensure immutability
        this.requiredEnchantments = new ArrayList<>(Objects.requireNonNull(requiredEnchantments, "Required enchantments list cannot be null."));
        this.bonuses = new ArrayList<>(Objects.requireNonNull(bonusTiers, "Bonus tiers list cannot be null."));
        
        // Sort bonuses by pieces required to make lookups easier and more reliable.
        this.bonuses.sort(Comparator.comparingInt(BonusTier::getPiecesRequired).reversed());
    }

    /**
     * Gets the passive effects for the highest bonus tier the player qualifies for.
     *
     * @param equippedPieceCount The number of set pieces the player has equipped.
     * @return A list of passive effect strings, or an empty list if no bonus is met.
     */
    public List<String> getEffectsForPieceCount(int equippedPieceCount) {
        // Since the list is sorted from highest to lowest, the first match is the best one.
        for (BonusTier tier : bonuses) {
            if (equippedPieceCount >= tier.getPiecesRequired()) {
                return tier.getPassiveEffects();
            }
        }
        return Collections.emptyList();
    }
    
    // --- Standard Getters ---

    public String getSetId() { return setId; }
    public String getDisplayName() { return displayName; }
    public List<String> getRequiredEnchantments() { return Collections.unmodifiableList(requiredEnchantments); }
    public List<BonusTier> getBonuses() { return Collections.unmodifiableList(bonuses); }


    /**
     * A static nested class to cleanly represent a single tier of a set bonus.
     * This is more professional and type-safe than using a raw Map.
     */
    public static final class BonusTier {
        private final int piecesRequired;
        private final List<String> passiveEffects;
        private final List<Map<?, ?>> triggeredEffects;

        public BonusTier(int piecesRequired, List<String> passiveEffects, List<Map<?, ?>> triggeredEffects) {
            this.piecesRequired = piecesRequired;
            this.passiveEffects = new ArrayList<>(Objects.requireNonNull(passiveEffects, "Passive effects list cannot be null."));
            this.triggeredEffects = new ArrayList<>(Objects.requireNonNull(triggeredEffects, "Triggered effects list cannot be null."));
        }

        public int getPiecesRequired() { return piecesRequired; }
        public List<String> getPassiveEffects() { return Collections.unmodifiableList(passiveEffects); }
        public List<Map<?, ?>> getTriggeredEffects() { return Collections.unmodifiableList(triggeredEffects); }
    }
}
