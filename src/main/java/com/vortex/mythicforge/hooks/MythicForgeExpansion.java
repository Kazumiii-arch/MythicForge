package com.vortex.mythicforge.hooks;

import com.vortex.mythicforge.MythicForge;
import com.vortex.mythicforge.managers.SetBonusManager.ActiveBonus; // Assuming you have this record/class
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * PlaceholderAPI Expansion for MythicForge.
 * This class handles all placeholder requests, allowing other plugins to
 * display live data from MythicForge.
 *
 * @author Vortex
 * @version 1.0.1
 */
public final class MythicForgeExpansion extends PlaceholderExpansion {

    private final MythicForge plugin;

    public MythicForgeExpansion(MythicForge plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "mythicforge";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Vortex";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true; // Keep this expansion loaded as long as MythicForge is enabled.
    }

    /**
     * The core method for parsing placeholders.
     * This signature correctly overrides the method from PlaceholderExpansion.
     *
     * @param player The player to get data for (can be offline).
     * @param params The placeholder identifier (e.g., "shop_timer").
     * @return The parsed value for the placeholder, or null if not found.
     */
    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        switch (params.toLowerCase()) {
            // --- Global Placeholders ---
            case "shop_timer":
                long remainingMillis = plugin.getShopManager().getNextRefreshTime() - System.currentTimeMillis();
                return formatTime(remainingMillis);
            case "total_enchants":
                return String.valueOf(plugin.getEnchantmentManager().getRegisteredEnchants().size());
            case "total_runes":
                return String.valueOf(plugin.getRuneManager().getRegisteredRunes().size());
            case "total_sets":
                return String.valueOf(plugin.getSetBonusManager().getAllSets().size());

            // --- Player-Specific Placeholders ---
            case "active_set_name":
                if (player != null && player.isOnline()) {
                    Optional<ActiveBonus> activeBonus = plugin.getSetBonusManager().getActiveBonusFor(player.getPlayer());
                    return activeBonus.map(b -> b.set().getDisplayName()).orElse("None");
                }
                return "Offline";
                
            case "held_enchant_count":
                if (player != null && player.isOnline()) {
                    Player onlinePlayer = player.getPlayer();
                    ItemStack itemInHand = onlinePlayer.getInventory().getItemInMainHand();
                    if (itemInHand != null && itemInHand.hasItemMeta()) {
                        Map<String, Integer> enchants = plugin.getItemManager().getEnchants(itemInHand.getItemMeta());
                        return String.valueOf(enchants.size());
                    }
                    return "0";
                }
                return "Offline";
        }

        return null; // Let PAPI know the placeholder was not recognized.
    }

    /**
     * Formats a duration in milliseconds into a human-readable HHh MMm SSs format.
     * @param millis The duration in milliseconds.
     * @return The formatted time string.
     */
    private String formatTime(long millis) {
        if (millis < 0) {
            return "Refreshing...";
        }
        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60;

        return String.format("%dh %dm %ds", hours, minutes, seconds);
    }
}
