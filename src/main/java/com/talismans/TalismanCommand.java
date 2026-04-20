package com.talismans;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.attribute.Attribute;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class TalismanCommand implements CommandExecutor {
    
    private Main plugin;
    
    public TalismanCommand(Main plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Только для игроков!");
            return true;
        }
        
        Player p = (Player) sender;
        
        if (!p.isOp()) {
            p.sendMessage(ChatColor.RED + "Нет прав!");
            return true;
        }
        
        if (args.length == 0) {
            openCreateMenu(p);
            return true;
        }
        
        if (args[0].equalsIgnoreCase("list")) {
            if (plugin.getTalismans().isEmpty()) {
                p.sendMessage(ChatColor.RED + "Нет созданных талисманов!");
            } else {
                p.sendMessage(ChatColor.GOLD + "Талисманы:");
                for (String id : plugin.getTalismans().keySet()) {
                    p.sendMessage(ChatColor.GREEN + "- " + id);
                }
            }
            return true;
        }
        
        if (args[0].equalsIgnoreCase("give") && args.length >= 3) {
            String id = args[1];
            Player target = plugin.getServer().getPlayer(args[2]);
            
            if (!plugin.getTalismans().containsKey(id)) {
                p.sendMessage(ChatColor.RED + "Талисман не найден!");
                return true;
            }
            
            if (target == null) {
                p.sendMessage(ChatColor.RED + "Игрок не найден!");
                return true;
            }
            
            plugin.giveTalismanItem(target, id);
            p.sendMessage(ChatColor.GREEN + "Талисман выдан игроку " + target.getName());
            return true;
        }
        
        if (args[0].equalsIgnoreCase("delete") && args.length >= 2) {
            String id = args[1];
            if (!plugin.getTalismans().containsKey(id)) {
                p.sendMessage(ChatColor.RED + "Талисман не найден!");
                return true;
            }
            plugin.deleteTalisman(id);
            p.sendMessage(ChatColor.GREEN + "Талисман " + id + " удалён!");
            return true;
        }
        
        openCreateMenu(p);
        return true;
    }
    
    private void openCreateMenu(Player p) {
        p.sendMessage(ChatColor.GREEN + "=== Создание талисмана ===");
        p.sendMessage(ChatColor.GRAY + "1. Возьми в руку предмет (алмаз, звезда и т.д.)");
        p.sendMessage(ChatColor.GRAY + "2. Напиши в чат: /talisman create <id> <название>");
        p.sendMessage(ChatColor.GRAY + "Пример: /talisman create sword &aМеч силы");
        p.sendMessage(ChatColor.GRAY + "После создания используй:");
        p.sendMessage(ChatColor.GRAY + "  /talisman attribute add <атрибут> <значение>");
        p.sendMessage(ChatColor.GRAY + "  /talisman effect add <эффект> <уровень>");
        p.sendMessage(ChatColor.GRAY + "  /talisman save");
    }
}
