package com.vortex.mythicforge.hooks;

import com.vortex.mythicforge.MythicForge;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;

/**
 * Handles all interactions with the Vault plugin API for economy features.
 * This class provides a safe, robust, and full-featured interface to any
 * Vault-compatible economy plugin.
 *
 * @author Vortex
 * @version 1.0.0
 */
public final class VaultHook {

    private Economy economy = null;
    private boolean isEnabled = false;

    /**
     * Constructs the VaultHook and attempts to hook into a Vault-based economy plugin.
     *
     * @param plugin The main plugin instance, used for logging.
     */
    public VaultHook(MythicForge plugin) {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            plugin.getLogger().warning("Vault not found. All economy features will be disabled.");
            return;
        }

        RegisteredServiceProvider<Economy> rsp = Bukkit.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            plugin.getLogger().warning("Vault found, but no economy provider is registered (e.g., EssentialsX). Economy features disabled.");
            return;
        }

        economy = rsp.getProvider();
        isEnabled = (economy != null);

        if (isEnabled) {
            plugin.getLogger().info("Successfully hooked into Vault and economy provider: " + economy.getName());
        } else {
            plugin.getLogger().severe("Failed to hook into an economy provider via Vault!");
        }
    }

    /**
     * Checks if the hook to a Vault economy is active.
     *
     * @return true if an economy plugin is successfully hooked, false otherwise.
     */
    public boolean isEnabled() {
        return isEnabled;
    }

    /**
     * Gets the current balance of a player.
     *
     * @param player The player whose balance is to be checked.
     * @return The player's current balance, or 0.0 if the hook is disabled.
     */
    public double getBalance(OfflinePlayer player) {
        if (!isEnabled) return 0.0;
        return economy.getBalance(player);
    }

    /**
     * Checks if a player has at least a certain amount of money.
     *
     * @param player The player to check.
     * @param amount The amount required.
     * @return true if the player has enough money, false otherwise.
     */
    public boolean hasEnough(OfflinePlayer player, double amount) {
        if (!isEnabled) return false;
        return economy.has(player, amount);
    }

    /**
     * Withdraws a specified amount from a player's balance.
     *
     * @param player The player to withdraw from.
     * @param amount The amount to withdraw.
     * @return An EconomyResponse object detailing the transaction's success or failure.
     */
    public EconomyResponse withdraw(OfflinePlayer player, double amount) {
        if (!isEnabled) return new EconomyResponse(0, 0, EconomyResponse.ResponseType.FAILURE, "Vault hook is not enabled.");
        return economy.withdrawPlayer(player, amount);
    }

    /**
     * Deposits a specified amount into a player's balance.
     *
     * @param player The player to deposit to.
     * @param amount The amount to deposit.
     * @return An EconomyResponse object detailing the transaction's success or failure.
     */
    public EconomyResponse deposit(OfflinePlayer player, double amount) {
        if (!isEnabled) return new EconomyResponse(0, 0, EconomyResponse.ResponseType.FAILURE, "Vault hook is not enabled.");
        return economy.depositPlayer(player, amount);
    }

    /**
     * Formats an amount of money into a user-friendly string (e.g., "$1,234.56").
     *
     * @param amount The amount to format.
     * @return The formatted currency string.
     */
    public String format(double amount) {
        if (!isEnabled) return String.valueOf(amount);
        return economy.format(amount);
    }
    
    /**
     * Gets the singular name of the currency (e.g., "Dollar").
     *
     * @return The singular currency name, or an empty string.
     */
    public String getCurrencyNameSingular() {
        if (!isEnabled) return "";
        return economy.currencyNameSingular();
    }

    /**
     * Gets the plural name of the currency (e.g., "Dollars").
     *
     * @return The plural currency name, or an empty string.
     */
    public String getCurrencyNamePlural() {
        if (!isEnabled) return "";
        return economy.currencyNamePlural();
    }
}
