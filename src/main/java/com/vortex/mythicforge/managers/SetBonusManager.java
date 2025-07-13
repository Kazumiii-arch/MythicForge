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

public final class SetBonusManager {

    private final MythicForge plugin;
    private final Map<String, SetBonus> registeredSets = new HashMap<>();

    // A simple public record to hold the result of a set check. Clean and modern.
    public record ActiveBonus(SetBonus set, BonusTier tier) {}

    public SetBonusManager(MythicForge plugin) {
        this.plugin = plugin;
    }

    public void loadSets() {
        registeredSets.clear();
        File setsDir = new File(plugin.getDataFolder(), "sets");
        if (!setsDir.exists()) {
            setsDir.mkdirs();
            plugin.saveResource("sets/wither_king.yml", false);
        }

        File[] setFiles = setsDir.listFiles((dir, name) -> name.endsWith(".yml"));
        if (setFiles == null) return;

        for (File file : setFiles) {
            FileConfiguration config = YamlConfiguration.loadConfiguration(file);
            try {
                String setId = config.getString("set_id");
                if (setId == null || setId.isEmpty()) continue;

                List<Map<?, ?>> rawBonusTiers = config.getMapList("bonuses");
                if (rawBonusTiers == null) continue;

                List<BonusTier> bonusTiers = new ArrayList<>();
                for (Map<?, ?> rawTier : rawBonusTiers) {
                    int pieces = (int) rawTier.getOrDefault("pieces_required", 0);
                    // CORRECTED: Safe, type-checked list retrieval
                    List<String> passive = getSafelyTypedList(rawTier, "passive_effects", String.class);
                    List<Map<?, ?>> triggered = getSafelyTypedList(rawTier, "triggered_effects", Map.class);
                    bonusTiers.add(new BonusTier(pieces, passive, triggered));
                }
                
                SetBonus setBonus = new SetBonus(
                        setId,
                        config.getString("set_display_name", setId),
                        config.getStringList("required_enchantments"),
                        bonusTiers
                );
                registeredSets.put(setId.toLowerCase(), setBonus);
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Error loading set bonus file: " + file.getName(), e);
            }
        }
        plugin.getLogger().info("Loaded " + registeredSets.size() + " gear sets.");
    }

    /**
     * Checks a player's gear and determines the highest-tier set bonus they have active.
     * @param player The player to check.
     * @return An Optional containing the ActiveBonus record, or empty if none are active.
     */
    public Optional<ActiveBonus> getActiveBonusFor(Player player) {
        List<String> equippedEnchantIds = getEnchantIdsFromItems(getEquippedItems(player));
        SetBonus bestSet = null;
        int maxPieces = 0;

        for (SetBonus set : registeredSets.values()) {
            int currentPieces = (int) set.getRequiredEnchantments().stream().filter(equippedEnchantIds::contains).count();
            if (currentPieces > maxPieces) {
                maxPieces = currentPieces;
                bestSet = set;
            }
        }

        if (bestSet != null) {
            Optional<BonusTier> bestTier = bestSet.getBonusTierFor(maxPieces);
            return bestTier.map(tier -> new ActiveBonus(bestSet, tier));
        }
        return Optional.empty();
    }

    public Collection<SetBonus> getAllSets() {
        return Collections.unmodifiableCollection(registeredSets.values());
    }
    
    // --- Private Helper Methods ---

    private List<ItemStack> getEquippedItems(Player player) {
        // ... (Full implementation from ActiveEffectTask)
        return new ArrayList<>();
    }

    private List<String> getEnchantIdsFromItems(List<ItemStack> items) {
        // ... (Full implementation from ActiveEffectTask)
        return new ArrayList<>();
    }

    @SuppressWarnings("unchecked")
    private <T> List<T> getSafelyTypedList(Map<?, ?> map, String key, Class<T> type) {
        Object obj = map.get(key);
        if (obj instanceof List) {
            List<?> rawList = (List<?>) obj;
            if (rawList.isEmpty() || type.isInstance(rawList.get(0))) {
                return (List<T>) rawList;
            }
        }
        return new ArrayList<>();
    }
    }
