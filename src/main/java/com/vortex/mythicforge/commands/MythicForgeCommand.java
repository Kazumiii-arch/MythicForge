package com.vortex.mythicforge.commands;

import com.vortex.mythicforge.MythicForge;
import com.vortex.mythicforge.enchants.CustomEnchant;
import com.vortex.mythicforge.enchants.Rune;
import com.vortex.mythicforge.gui.SalvageGUI;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.trait.Owner;
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
 * Implements TabCompleter for a user-friendly command experience.
 */
public class MythicForgeCommand implements CommandExecutor, TabCompleter {

    private final MythicForge plugin = MythicForge.getInstance();

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.GOLD + "MythicForge v" + plugin.getDescription().getVersion() + " by Vortex.");
            sender.sendMessage(ChatColor.GRAY + "Use /mf help for a list of commands.");
            return true;
        }

        String subCommand = args[0].toLowerCase();

        // Use a switch for clean command delegation
        switch (subCommand) {
            case "give":
                return handleGiveCommand(sender, args);
            case "reload":
                return handleReloadCommand(sender);
            case "salvage":
                return handleSalvageCommand(sender);
            case "npc":
                return handleNpcCommand(sender, args);
            // Add cases for 'shop', 'help', etc. here
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
        // Updated Usage: /mf give <player> <type> <id> [amount/level]
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
            try {
                amount = Integer.parseInt(args[4]);
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Invalid amount/level.");
                return true;
            }
        }

        // Delegate to specific give methods based on type
        // For example:
        if (type.equals("enchant")) {
            ItemStack itemInHand = target.getInventory().getItemInMainHand();
            CustomEnchant enchant = plugin.getEnchantmentManager().getEnchantById(id);
            if (enchant == null) {
                sender.sendMessage(ChatColor.RED + "Enchantment '" + id + "' not found.");
                return true;
            }
            plugin.getItemManager().applyEnchant(itemInHand, enchant, amount);
            sender.sendMessage(ChatColor.GREEN + "Applied " + id + " to " + target.getName() + "'s item.");
        } else if (type.equals("rune")) {
            // ... Logic to give a rune item using TomeManager ...
        } else if (type.equals("item")) {
            // ... Logic to give dust, orbs, chisel using TomeManager ...
        } else if (type.equals("setpiece")) {
            // ... Logic to give a set piece using SetShopManager ...
        } else {
            sender.sendMessage(ChatColor.RED + "Invalid item type. Use: enchant, rune, item, setpiece.");
        }
        return true;
    }

    private boolean handleReloadCommand(CommandSender sender) {
        // ... (permission check) ...
        // Reload all configurations
        plugin.reloadConfig();
        plugin.getEnchantmentManager().loadEnchantments();
        plugin.getRuneManager().loadRunes();
        plugin.getSetBonusManager().loadSets();
        plugin.getSetShopManager().loadSetShopConfig();
        sender.sendMessage(ChatColor.GREEN + "MythicForge configurations reloaded.");
        return true;
    }

    private boolean handleSalvageCommand(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
            return true;
        }
        if (!sender.hasPermission("mythicforge.command.salvage")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission.");
            return true;
        }
        new SalvageGUI().open((Player) sender);
        return true;
    }

    private boolean handleNpcCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("mythicforge.admin.npc")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission.");
            return true;
        }
        // Usage: /mf npc set <role>
        if (args.length < 3 || !args[1].equalsIgnoreCase("set")) {
            sender.sendMessage(ChatColor.RED + "Usage: /mf npc set <role>");
            return true;
        }
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "You must be a player to set an NPC's role.");
            return true;
        }

        // This requires the Citizens API to be present
        if (!plugin.getCitizensHook().isEnabled()) {
            sender.sendMessage(ChatColor.RED + "Citizens plugin not found. This feature is disabled.");
            return true;
        }

        NPC selectedNpc = CitizensAPI.getDefaultNPCSelector().getSelected(sender);
        if (selectedNpc == null) {
            sender.sendMessage(ChatColor.RED + "You must have an NPC selected. Use /npc select.");
            return true;
        }
        
        String role = args[2].toLowerCase();
        plugin.getCitizensHook().setNpcRole(selectedNpc, role);
        sender.sendMessage(ChatColor.GREEN + "Successfully set NPC " + selectedNpc.getName() + "'s role to: " + role);
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
                    // Suggest online player names
                    for (Player p : Bukkit.getOnlinePlayers()) { completions.add(p.getName()); }
                    break;
                case "npc":
                    if (sender.hasPermission("mythicforge.admin.npc")) { completions.add("set"); }
                    break;
                // Add cases for 'shop', etc.
            }
        } else if (args.length == 3) {
            switch (args[0].toLowerCase()) {
                case "give":
                    completions.addAll(Arrays.asList("enchant", "rune", "item", "setpiece"));
                    break;
                case "npc":
                    if (args[1].equalsIgnoreCase("set")) {
                        completions.addAll(Arrays.asList("enchant_shop", "set_shop", "salvage_station"));
                    }
                    break;
            }
        } else if (args.length == 4) {
            if (args[0].equalsIgnoreCase("give")) {
                switch (args[2].toLowerCase()) {
                    case "enchant":
                        completions.addAll(plugin.getEnchantmentManager().getRegisteredEnchants().keySet());
                        break;
                    case "rune":
                        // completions.addAll(plugin.getRuneManager().getRegisteredRunes().keySet());
                        break;
                    case "item":
                        // completions.addAll(plugin.getConfig().getConfigurationSection("mechanics.custom_items").getKeys(false));
                        break;
                }
            }
        }
        
        return StringUtil.copyPartialMatches(args[args.length-1], completions, new ArrayList<>());
    }
                   }
