package com.talismans;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
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
            sendHelp(p);
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
        
        if (args[0].equalsIgnoreCase("create") && args.length >= 3) {
            String id = args[1];
            String name = args[2].replace("&", "§");
            
            if (plugin.getTalismans().containsKey(id)) {
                p.sendMessage(ChatColor.RED + "Талисман с таким ID уже существует!");
                return true;
            }
            
            ItemStack itemInHand = p.getInventory().getItemInMainHand();
            if (itemInHand.getType() == Material.AIR) {
                p.sendMessage(ChatColor.RED + "Возьми предмет в руку!");
                return true;
            }
            
            String material = itemInHand.getType().name();
            
            Map<Attribute, Double> attributes = new HashMap<>();
            List<PotionEffect> effects = new ArrayList<>();
            
            plugin.saveTalisman(id, name, material, id, attributes, effects);
            p.sendMessage(ChatColor.GREEN + "Талисман " + id + " создан!");
            p.sendMessage(ChatColor.GRAY + "Теперь добавь атрибуты командой /talisman attribute add <атрибут> <значение>");
            p.sendMessage(ChatColor.GRAY + "Доступные атрибуты: ATTACK_DAMAGE, ARMOR, MAX_HEALTH, MOVEMENT_SPEED");
            return true;
        }
        
        if (args[0].equalsIgnoreCase("attribute") && args.length >= 4 && args[1].equalsIgnoreCase("add")) {
            String id = null;
            for (String talismanId : plugin.getTalismans().keySet()) {
                id = talismanId;
                break;
            }
            
            if (id == null) {
                p.sendMessage(ChatColor.RED + "Сначала создай талисман!");
                return true;
            }
            
            String attrName = args[2].toUpperCase();
            double value;
            try {
                value = Double.parseDouble(args[3]);
            } catch (NumberFormatException e) {
                p.sendMessage(ChatColor.RED + "Значение должно быть числом!");
                return true;
            }
            
            Attribute attr = getAttribute(attrName);
            if (attr == null) {
                p.sendMessage(ChatColor.RED + "Неверный атрибут! Доступны: ATTACK_DAMAGE, ARMOR, MAX_HEALTH, MOVEMENT_SPEED");
                return true;
            }
            
            TalismanData data = plugin.getTalismans().get(id);
            Map<Attribute, Double> newAttributes = new HashMap<>(data.attributes);
            newAttributes.put(attr, value);
            
            plugin.saveTalisman(id, data.name, data.material, id, newAttributes, data.effects);
            p.sendMessage(ChatColor.GREEN + "Атрибут " + attrName + " = " + value + " добавлен!");
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
        
        sendHelp(p);
        return true;
    }
    
    private Attribute getAttribute(String name) {
        switch (name.toUpperCase()) {
            case "ATTACK_DAMAGE": return Attribute.ATTACK_DAMAGE;
            case "ARMOR": return Attribute.ARMOR;
            case "MAX_HEALTH": return Attribute.MAX_HEALTH;
            case "MOVEMENT_SPEED": return Attribute.MOVEMENT_SPEED;
            default: return null;
        }
    }
    
    private void sendHelp(Player p) {
        p.sendMessage(ChatColor.YELLOW + "=== TalismansPlugin ===");
        p.sendMessage(ChatColor.GRAY + "/talisman create <id> <название>");
        p.sendMessage(ChatColor.GRAY + "/talisman attribute add <атрибут> <значение>");
        p.sendMessage(ChatColor.GRAY + "/talisman give <id> <игрок>");
        p.sendMessage(ChatColor.GRAY + "/talisman list");
        p.sendMessage(ChatColor.GRAY + "");
        p.sendMessage(ChatColor.GRAY + "Пример создания талисмана +4 урона, -2 брони, +2 сердца:");
        p.sendMessage(ChatColor.GRAY + "1. /talisman create battle &aБоевой талисман");
        p.sendMessage(ChatColor.GRAY + "2. /talisman attribute add ATTACK_DAMAGE 4");
        p.sendMessage(ChatColor.GRAY + "3. /talisman attribute add ARMOR -2");
        p.sendMessage(ChatColor.GRAY + "4. /talisman attribute add MAX_HEALTH 2");
        p.sendMessage(ChatColor.GRAY + "5. /talisman give battle Игрок");
    }
}
