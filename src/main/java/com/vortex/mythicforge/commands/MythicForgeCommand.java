package com.vortex.mythicforge.commands;

import com.vortex.mythicforge.MythicForge;
import com.vortex.mythicforge.enchants.CustomEnchant;
import com.vortex.mythicforge.enchants.Rune;
import com.vortex.mythicforge.gui.SalvageGUI;
import com.vortex.mythicforge.listeners.NpcListener;
import de.oliver.fancynpcs.api.Npc;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.StringUtil;
import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Handles all administrative and player commands for the MythicForge plugin.
 * Implements TabCompleter for a user-friendly, context-aware command experience.
 *
 * @author Vortex
 * @version 1.0.1
 */
public final class MythicForgeCommand implements CommandExecutor, TabCompleter {

    private final MythicForge plugin = MythicForge.getInstance();

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.GOLD + "MythicForge v" + plugin.getDescription().getVersion() + " by Vortex.");
            sender.sendMessage(ChatColor.GRAY + "Use /mf help for a list of commands.");
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "give":
                return handleGiveCommand(sender, args);
            case "reload":
                return handleReloadCommand(sender);
            case "salvage":
                return handleSalvageCommand(sender);
            case "npc":
                return handleNpcCommand(sender, args);
            case "shop":
                return handleShopCommand(sender, args);
            default:
                sender.sendMessage(ChatColor.RED + "Unknown subcommand. Use /mf help.");
                return true;
        }
    }

    private boolean handleGiveCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("mythicforge.admin.give")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission.");
            return true;
        }
        if (args.length < 4) {
            sender.sendMessage(ChatColor.RED + "Usage: /mf give <player> <type> <id> [amount/level]");
            sender.sendMessage(ChatColor.GRAY + "Types: enchant, rune, item, setpiece");
            return true;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found: " + args[1]);
            return true;
        }

        String type = args[2].toLowerCase();
        String id = args[3];
        int amount = 1;
        if (args.length > 4) {
            try { amount = Integer.parseInt(args[4]); } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Invalid amount/level.");
                return true;
            }
        }

        switch (type) {
            case "enchant":
                ItemStack itemInHand = target.getInventory().getItemInMainHand();
                if (itemInHand.getType().isAir()) {
                    sender.sendMessage(ChatColor.RED + "Target player is not holding an item.");
                    return true;
                }
                CustomEnchant enchant = plugin.getEnchantmentManager().getEnchantById(id);
                if (enchant == null) {
                    sender.sendMessage(ChatColor.RED + "Enchantment '" + id + "' not found.");
                    return true;
                }
                plugin.getItemManager().applyEnchant(itemInHand, enchant, amount);
                sender.sendMessage(ChatColor.GREEN + "Applied " + id + " to " + target.getName() + "'s item.");
                break;
            case "rune":
                // Rune rune = plugin.getRuneManager().getRuneById(id);
                // if (rune == null) { ... }
                // target.getInventory().addItem(plugin.getTomeManager().createRuneItem(rune, amount));
                break;
            case "item":
                target.getInventory().addItem(plugin.getTomeManager().createCustomItem(id, amount));
                sender.sendMessage(ChatColor.GREEN + "Gave " + amount + " of " + id + " to " + target.getName() + ".");
                break;
            default:
                sender.sendMessage(ChatColor.RED + "Invalid item type. Use: enchant, rune, item.");
                break;
        }
        return true;
    }

    private boolean handleReloadCommand(CommandSender sender) {
        if (!sender.hasPermission("mythicforge.admin.reload")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission.");
            return true;
        }
        plugin.reloadConfig();
        plugin.getEnchantmentManager().loadEnchantments();
        plugin.getRuneManager().loadRunes();
        plugin.getSetBonusManager().loadSets();
        plugin.getSetShopManager().loadAndCacheShopItems();
        plugin.getShopManager().forceRefreshStock();
        sender.sendMessage(ChatColor.GREEN + "MythicForge has been fully reloaded.");
        return true;
    }

    private boolean handleSalvageCommand(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
            return true;
        }
        new SalvageGUI((Player) sender);
        return true;
    }

    private boolean handleNpcCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("mythicforge.admin.npc")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission.");
            return true;
        }
        if (args.length < 3 || !args[1].equalsIgnoreCase("set")) {
            sender.sendMessage(ChatColor.RED + "Usage: /mf npc set <role>");
            return true;
        }
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "You must be a player to use this command.");
            return true;
        }

        Player player = (Player) sender;
        String role = args[2].toLowerCase();
        NpcListener.setPlayerForRoleAssignment(player, role);
        player.sendMessage(ChatColor.GREEN + "Please right-click the NPC you wish to assign the '" + role + "' role to.");
        return true;
    }

    private boolean handleShopCommand(CommandSender sender, String[] args) {
         if (!sender.hasPermission("mythicforge.admin.shop")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission.");
            return true;
        }
        if (args.length < 2 || !args[1].equalsIgnoreCase("refresh")) {
            sender.sendMessage(ChatColor.RED + "Usage: /mf shop refresh");
            return true;
        }
        plugin.getShopManager().forceRefreshStock();
        sender.sendMessage(ChatColor.GREEN + "The rotating shop stock has been forcefully refreshed.");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        final List<String> completions = new ArrayList<>();
        final List<String> commands = Arrays.asList("give", "reload", "salvage", "npc", "shop", "help");

        if (args.length == 1) {
            StringUtil.copyPartialMatches(args[0], commands, completions);
        } else if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "give":
                    for (Player p : Bukkit.getOnlinePlayers()) { completions.add(p.getName()); }
                    break;
                case "npc":
                    if (sender.hasPermission("mythicforge.admin.npc")) completions.add("set");
                    break;
                case "shop":
                     if (sender.hasPermission("mythicforge.admin.shop")) completions.add("refresh");
                     break;
            }
        } else if (args.length == 3) {
            switch (args[0].toLowerCase()) {
                case "give":
                    completions.addAll(Arrays.asList("enchant", "rune", "item"));
                    break;
                case "npc":
                    if (args[1].equalsIgnoreCase("set")) {
                        completions.addAll(Arrays.asList("enchant_shop", "set_shop", "salvage_station"));
                    }
                    break;
            }
        } else if (args.length == 4 && args[0].equalsIgnoreCase("give")) {
            switch (args[2].toLowerCase()) {
                case "enchant":
                    completions.addAll(plugin.getEnchantmentManager().getRegisteredEnchants().keySet());
                    break;
                case "rune":
                    completions.addAll(plugin.getRuneManager().getRegisteredRunes().keySet());
                    break;
                case "item":
                    completions.addAll(plugin.getConfig().getConfigurationSection("mechanics.custom_items").getKeys(false));
                    break;
            }
        }
        return StringUtil.copyPartialMatches(args[args.length - 1], completions, new ArrayList<>());
    }
}
