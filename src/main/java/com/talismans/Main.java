package com.talismans;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;

import java.util.*;

public class Main extends JavaPlugin implements Listener {
    
    private Map<String, TalismanData> talismans = new HashMap<>();
    
    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        loadTalismans();
        
        getCommand("talisman").setExecutor((sender, cmd, label, args) -> {
            if (!(sender instanceof Player)) return true;
            Player p = (Player) sender;
            if (!p.isOp()) {
                p.sendMessage(ChatColor.RED + "Нет прав!");
                return true;
            }
            openMainMenu(p);
            return true;
        });
        
        getLogger().info("TalismansGUI включён! Используй /talisman");
    }
    
    private void openMainMenu(Player p) {
        Inventory gui = Bukkit.createInventory(null, 27, ChatColor.DARK_PURPLE + "Талисманы");
        
        ItemStack create = new ItemStack(Material.NETHER_STAR);
        ItemMeta cm = create.getItemMeta();
        cm.setDisplayName(ChatColor.GREEN + "Создать талисман");
        cm.setLore(List.of(ChatColor.GRAY + "Возьми предмет в руку и нажми"));
        create.setItemMeta(cm);
        gui.setItem(11, create);
        
        ItemStack list = new ItemStack(Material.BOOK);
        ItemMeta lm = list.getItemMeta();
        lm.setDisplayName(ChatColor.YELLOW + "Список талисманов");
        list.setItemMeta(lm);
        gui.setItem(13, list);
        
        ItemStack give = new ItemStack(Material.GOLDEN_APPLE);
        ItemMeta gm = give.getItemMeta();
        gm.setDisplayName(ChatColor.AQUA + "Выдать талисман");
        gm.setLore(List.of(ChatColor.GRAY + "Используй команду /talisman give <id> <игрок>"));
        give.setItemMeta(gm);
        gui.setItem(15, give);
        
        p.openInventory(gui);
    }
    
    private void openTalismanList(Player p) {
        int size = Math.min(54, ((talismans.size() + 8) / 9) * 9 + 9);
        Inventory gui = Bukkit.createInventory(null, size, ChatColor.DARK_PURPLE + "Список талисманов");
        int slot = 0;
        for (String id : talismans.keySet()) {
            TalismanData data = talismans.get(id);
            ItemStack item = new ItemStack(Material.getMaterial(data.material));
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(ChatColor.GREEN + data.name);
            meta.setLore(List.of(ChatColor.GRAY + "ID: " + id, "", ChatColor.YELLOW + "ЛКМ - выдать себе", ChatColor.RED + "ПКМ - удалить"));
            item.setItemMeta(meta);
            gui.setItem(slot++, item);
        }
        p.openInventory(gui);
    }
    
    @EventHandler
    public void onInventoryClick(org.bukkit.event.inventory.InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();
        String title = e.getView().getTitle();
        e.setCancelled(true);
        
        if (title.equals(ChatColor.DARK_PURPLE + "Талисманы")) {
            int slot = e.getRawSlot();
            if (slot == 11) {
                ItemStack hand = p.getInventory().getItemInMainHand();
                if (hand.getType() == Material.AIR) {
                    p.sendMessage(ChatColor.RED + "Возьми предмет в руку!");
                    return;
                }
                String id = "talisman_" + System.currentTimeMillis();
                String name = ChatColor.GREEN + "Новый талисман";
                String material = hand.getType().name();
                talismans.put(id, new TalismanData(name, material));
                saveTalismans();
                p.sendMessage(ChatColor.GREEN + "Талисман создан! ID: " + id);
                p.closeInventory();
            } else if (slot == 13) {
                openTalismanList(p);
            }
        } else if (title.equals(ChatColor.DARK_PURPLE + "Список талисманов")) {
            int slot = e.getRawSlot();
            if (slot >= 0 && slot < talismans.size()) {
                String id = talismans.keySet().toArray(new String[0])[slot];
                if (e.isLeftClick()) {
                    giveTalismanItem(p, id);
                    p.sendMessage(ChatColor.GREEN + "Талисман выдан себе!");
                } else if (e.isRightClick()) {
                    talismans.remove(id);
                    saveTalismans();
                    p.sendMessage(ChatColor.RED + "Талисман удалён!");
                    openTalismanList(p);
                }
            }
        }
    }
    
    private void giveTalismanItem(Player p, String id) {
        TalismanData data = talismans.get(id);
        if (data == null) return;
        ItemStack item = new ItemStack(Objects.requireNonNull(Material.getMaterial(data.material)));
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(data.name);
        meta.setLore(List.of(ChatColor.GRAY + "ID: " + id));
        item.setItemMeta(meta);
        p.getInventory().addItem(item);
    }
    
    private void loadTalismans() {
        // Заглушка — потом добавим сохранение в файл
    }
    
    private void saveTalismans() {
        // Заглушка
    }
    
    @EventHandler
    public void onItemHeld(PlayerItemHeldEvent e) {
        // Пока без эффектов
    }
    
    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        // Пока без эффектов
    }
    
    class TalismanData {
        String name;
        String material;
        TalismanData(String name, String material) {
            this.name = name;
            this.material = material;
        }
    }
}
