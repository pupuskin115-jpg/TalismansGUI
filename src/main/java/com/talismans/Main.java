package com.talismans;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;

import java.io.File;
import java.util.*;

public class Main extends JavaPlugin implements Listener {
    
    private Map<UUID, List<AttributeModifier>> activeAttributes = new HashMap<>();
    private Map<UUID, List<PotionEffect>> activeEffects = new HashMap<>();
    private Map<String, TalismanData> talismans = new HashMap<>();
    private Map<UUID, String> editingTalisman = new HashMap<>();
    
    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        loadTalismans();
        
        getCommand("talisman").setExecutor((sender, cmd, label, args) -> {
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
                openMainMenu(p);
                return true;
            }
            
            if (args[0].equalsIgnoreCase("give") && args.length >= 3) {
                String id = args[1];
                Player target = getServer().getPlayer(args[2]);
                
                if (!talismans.containsKey(id)) {
                    p.sendMessage(ChatColor.RED + "Талисман не найден!");
                    return true;
                }
                
                if (target == null) {
                    p.sendMessage(ChatColor.RED + "Игрок не найден!");
                    return true;
                }
                
                giveTalismanItem(target, id);
                p.sendMessage(ChatColor.GREEN + "Талисман выдан игроку " + target.getName());
                return true;
            }
            
            if (args[0].equalsIgnoreCase("list")) {
                if (talismans.isEmpty()) {
                    p.sendMessage(ChatColor.RED + "Нет созданных талисманов!");
                } else {
                    p.sendMessage(ChatColor.GOLD + "Талисманы:");
                    for (String id : talismans.keySet()) {
                        p.sendMessage(ChatColor.GREEN + "- " + id);
                    }
                }
                return true;
            }
            
            if (args[0].equalsIgnoreCase("delete") && args.length >= 2) {
                String id = args[1];
                if (!talismans.containsKey(id)) {
                    p.sendMessage(ChatColor.RED + "Талисман не найден!");
                    return true;
                }
                deleteTalisman(id);
                p.sendMessage(ChatColor.GREEN + "Талисман " + id + " удалён!");
                return true;
            }
            
            openMainMenu(p);
            return true;
        });
        
        getLogger().info("=====================================");
        getLogger().info("TalismansGUI v2.0 ВКЛЮЧЕН!");
        getLogger().info("Команда: /talisman - открыть GUI");
        getLogger().info("=====================================");
    }
    
    private void openMainMenu(Player p) {
        Inventory gui = Bukkit.createInventory(null, 27, ChatColor.DARK_PURPLE + "Талисманы");
        
        // Создание нового талисмана
        ItemStack createItem = new ItemStack(Material.NETHER_STAR);
        ItemMeta createMeta = createItem.getItemMeta();
        createMeta.setDisplayName(ChatColor.GREEN + "Создать талисман");
        createMeta.setLore(List.of(ChatColor.GRAY + "Возьми предмет в руку и нажми"));
        createItem.setItemMeta(createMeta);
        gui.setItem(11, createItem);
        
        // Список талисманов
        ItemStack listItem = new ItemStack(Material.BOOK);
        ItemMeta listMeta = listItem.getItemMeta();
        listMeta.setDisplayName(ChatColor.YELLOW + "Список талисманов");
        listMeta.setLore(List.of(ChatColor.GRAY + "Все созданные талисманы"));
        listItem.setItemMeta(listMeta);
        gui.setItem(13, listItem);
        
        // Выдать талисман
        ItemStack giveItem = new ItemStack(Material.GOLDEN_APPLE);
        ItemMeta giveMeta = giveItem.getItemMeta();
        giveMeta.setDisplayName(ChatColor.AQUA + "Выдать талисман");
        giveMeta.setLore(List.of(ChatColor.GRAY + "Выдать талисман игроку"));
        giveItem.setItemMeta(giveMeta);
        gui.setItem(15, giveItem);
        
        p.openInventory(gui);
    }
    
    private void openTalismanListMenu(Player p) {
        int size = Math.min(54, ((talismans.size() + 8) / 9) * 9 + 9);
        Inventory gui = Bukkit.createInventory(null, size, ChatColor.DARK_PURPLE + "Список талисманов");
        
        int slot = 0;
        for (String id : talismans.keySet()) {
            TalismanData data = talismans.get(id);
            ItemStack item = new ItemStack(Material.getMaterial(data.material));
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(ChatColor.GREEN + data.name);
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "ID: " + id);
            lore.add("");
            for (Map.Entry<Attribute, Double> entry : data.attributes.entrySet()) {
                String attrName = getAttributeName(entry.getKey());
                double value = entry.getValue();
                String color = value > 0 ? "§a+" : "§c";
                lore.add(ChatColor.GRAY + attrName + ": " + color + value);
            }
            for (PotionEffect effect : data.effects) {
                lore.add(ChatColor.GRAY + "Эффект: " + effect.getType().getName() + " " + (effect.getAmplifier() + 1));
            }
            lore.add("");
            lore.add(ChatColor.YELLOW + "ЛКМ - Выдать себе");
            lore.add(ChatColor.RED + "ПКМ - Удалить");
            meta.setLore(lore);
            item.setItemMeta(meta);
            gui.setItem(slot, item);
            slot++;
        }
        
        p.openInventory(gui);
    }
    
    private void openCreateMenu(Player p) {
        ItemStack itemInHand = p.getInventory().getItemInMainHand();
        if (itemInHand.getType() == Material.AIR) {
            p.sendMessage(ChatColor.RED + "Возьми предмет в руку!");
            p.closeInventory();
            return;
        }
        
        String id = "talisman_" + System.currentTimeMillis();
        String name = ChatColor.GREEN + "Новый талисман";
        String material = itemInHand.getType().name();
        
        Map<Attribute, Double> attributes = new HashMap<>();
        List<PotionEffect> effects = new ArrayList<>();
        
        saveTalisman(id, name, material, id, attributes, effects);
        editingTalisman.put(p.getUniqueId(), id);
        
        openEditMenu(p, id);
    }
    
    private void openEditMenu(Player p, String id) {
        TalismanData data = talismans.get(id);
        if (data == null) return;
        
        Inventory gui = Bukkit.createInventory(null, 54, ChatColor.DARK_PURPLE + "Редактирование: " + data.name);
        
        // Название
        ItemStack nameItem = new ItemStack(Material.NAME_TAG);
        ItemMeta nameMeta = nameItem.getItemMeta();
        nameMeta.setDisplayName(ChatColor.YELLOW + "Изменить название");
        nameMeta.setLore(List.of(ChatColor.GRAY + "Текущее: " + ChatColor.RESET + data.name));
        nameItem.setItemMeta(nameMeta);
        gui.setItem(0, nameItem);
        
        // Атрибуты
        ItemStack attrItem = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta attrMeta = attrItem.getItemMeta();
        attrMeta.setDisplayName(ChatColor.RED + "Атрибуты");
        attrMeta.setLore(getAttributeLore(data.attributes));
        attrItem.setItemMeta(attrMeta);
        gui.setItem(20, attrItem);
        
        // Эффекты
        ItemStack effectItem = new ItemStack(Material.POTION);
        ItemMeta effectMeta = effectItem.getItemMeta();
        effectMeta.setDisplayName(ChatColor.LIGHT_PURPLE + "Эффекты зелий");
        effectMeta.setLore(getEffectLore(data.effects));
        effectItem.setItemMeta(effectMeta);
        gui.setItem(22, effectItem);
        
        // Сохранить и выдать
        ItemStack saveItem = new ItemStack(Material.EMERALD);
        ItemMeta saveMeta = saveItem.getItemMeta();
        saveMeta.setDisplayName(ChatColor.GREEN + "Сохранить и выдать себе");
        saveItem.setItemMeta(saveMeta);
        gui.setItem(49, saveItem);
        
        p.openInventory(gui);
    }
    
    private void openAttributeMenu(Player p, String id) {
        TalismanData data = talismans.get(id);
        if (data == null) return;
        
        Inventory gui = Bukkit.createInventory(null, 27, ChatColor.RED + "Атрибуты");
        
        String[] attrs = {"ATTACK_DAMAGE", "ARMOR", "MAX_HEALTH", "MOVEMENT_SPEED", "ATTACK_SPEED", "LUCK", "KNOCKBACK_RESISTANCE"};
        String[] displayNames = {"🗡 Урон", "🛡 Броня", "❤ Здоровье", "🏃 Скорость", "⚡ Скорость атаки", "🍀 Удача", "💪 Сопротивление откату"};
        
        for (int i = 0; i < attrs.length; i++) {
            Attribute attr = getAttributeByName(attrs[i]);
            double current = data.attributes.getOrDefault(attr, 0.0);
            
            ItemStack item = new ItemStack(Material.PAPER);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(ChatColor.YELLOW + displayNames[i]);
            String color = current > 0 ? "§a+" : (current < 0 ? "§c" : "§7");
            meta.setLore(List.of(
                ChatColor.GRAY + "Текущее значение: " + color + current,
                "",
                ChatColor.GREEN + "ЛКМ: +1",
                ChatColor.RED + "ПКМ: -1",
                ChatColor.GOLD + "Shift+ЛКМ: +10",
                ChatColor.GOLD + "Shift+ПКМ: -10"
            ));
            item.setItemMeta(meta);
            gui.setItem(i, item);
        }
        
        // Кнопка назад
        ItemStack backItem = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backItem.getItemMeta();
        backMeta.setDisplayName(ChatColor.GRAY + "Назад");
        backItem.setItemMeta(backMeta);
        gui.setItem(26, backItem);
        
        p.openInventory(gui);
        editingTalisman.put(p.getUniqueId(), id);
    }
    
    private List<String> getAttributeLore(Map<Attribute, Double> attributes) {
        List<String> lore = new ArrayList<>();
        for (Map.Entry<Attribute, Double> entry : attributes.entrySet()) {
            String name = getAttributeName(entry.getKey());
            double value = entry.getValue();
            String color = value > 0 ? "§a+" : (value < 0 ? "§c" : "§7");
            lore.add(ChatColor.GRAY + name + ": " + color + value);
        }
        if (lore.isEmpty()) {
            lore.add(ChatColor.GRAY + "Нет атрибутов");
        }
        lore.add("");
        lore.add(ChatColor.YELLOW + "Нажми для редактирования");
        return lore;
    }
    
    private List<String> getEffectLore(List<PotionEffect> effects) {
        List<String> lore = new ArrayList<>();
        for (PotionEffect effect : effects) {
            lore.add(ChatColor.GRAY + effect.getType().getName() + " " + (effect.getAmplifier() + 1));
        }
        if (lore.isEmpty()) {
            lore.add(ChatColor.GRAY + "Нет эффектов");
        }
        lore.add("");
        lore.add(ChatColor.YELLOW + "Нажми для редактирования");
        return lore;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("talisman")) {
            if (args.length == 0) {
                openMainMenu((Player) sender);
                return true;
            }
        }
        return false;
    }
    
    @EventHandler
    public void onInventoryClick(org.bukkit.event.inventory.InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player p = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();
        
        if (title.equals(ChatColor.DARK_PURPLE + "Талисманы")) {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            
            if (slot == 11) {
                openCreateMenu(p);
            } else if (slot == 13) {
                openTalismanListMenu(p);
            } else if (slot == 15) {
                p.sendMessage(ChatColor.YELLOW + "Используй команду: /talisman give <id> <игрок>");
            }
        }
        else if (title.equals(ChatColor.DARK_PURPLE + "Список талисманов")) {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            if (slot >= 0 && slot < talismans.size()) {
                String id = talismans.keySet().toArray(new String[0])[slot];
                if (event.isLeftClick()) {
                    giveTalismanItem(p, id);
                    p.sendMessage(ChatColor.GREEN + "Талисман выдан себе!");
                } else if (event.isRightClick()) {
                    deleteTalisman(id);
                    p.sendMessage(ChatColor.RED + "Талисман удалён!");
                    openTalismanListMenu(p);
                }
            }
        }
        else if (title.contains(ChatColor.RED + "Атрибуты")) {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            String id = editingTalisman.get(p.getUniqueId());
            if (id == null) return;
            
            TalismanData data = talismans.get(id);
            if (data == null) return;
            
            if (slot == 26) {
                openEditMenu(p, id);
                return;
            }
            
            String[] attrs = {"ATTACK_DAMAGE", "ARMOR", "MAX_HEALTH", "MOVEMENT_SPEED", "ATTACK_SPEED", "LUCK", "KNOCKBACK_RESISTANCE"};
            if (slot >= 0 && slot < attrs.length) {
                Attribute attr = getAttributeByName(attrs[slot]);
                double current = data.attributes.getOrDefault(attr, 0.0);
                double delta = 0;
                
                if (event.isShiftClick()) {
                    delta = event.isLeftClick() ? 10 : (event.isRightClick() ? -10 : 0);
                } else {
                    delta = event.isLeftClick() ? 1 : (event.isRightClick() ? -1 : 0);
                }
                
                if (delta != 0) {
                    double newValue = current + delta;
                    Map<Attribute, Double> newAttributes = new HashMap<>(data.attributes);
                    if (Math.abs(newValue) < 0.01) {
                        newAttributes.remove(attr);
                    } else {
                        newAttributes.put(attr, newValue);
                    }
                    saveTalisman(id, data.name, data.material, id, newAttributes, data.effects);
                    openAttributeMenu(p, id);
                }
            }
        }
        else if (title.contains(ChatColor.DARK_PURPLE + "Редактирование:")) {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            String id = null;
            for (String talismanId : talismans.keySet()) {
                id = talismanId;
                break;
            }
            if (id == null) return;
            
            if (slot == 20) {
                openAttributeMenu(p, id);
            } else if (slot == 49) {
                giveTalismanItem(p, id);
                p.sendMessage(ChatColor.GREEN + "Талисман выдан себе!");
                p.closeInventory();
            }
        }
    }
    
    @Override
    public void onDisable() {
        for (UUID uuid : activeAttributes.keySet()) {
            Player p = getServer().getPlayer(uuid);
            if (p != null) {
                for (AttributeModifier mod : activeAttributes.get(uuid)) {
                    removeAttributeModifier(p, mod);
                }
            }
        }
        for (UUID uuid : activeEffects.keySet()) {
            Player p = getServer().Player(uuid);
            if (p != null) {
                for (PotionEffect effect : activeEffects.get(uuid)) {
                    p.removePotionEffect(effect.getType());
                }
            }
        }
        getLogger().info("TalismansGUI выключен!");
    }
    
    private void removeAttributeModifier(Player p, AttributeModifier mod) {
        if (p.getAttribute(Attribute.ATTACK_DAMAGE) != null)
            p.getAttribute(Attribute.ATTACK_DAMAGE).removeModifier(mod);
        if (p.getAttribute(Attribute.ARMOR) != null)
            p.getAttribute(Attribute.ARMOR).removeModifier(mod);
        if (p.getAttribute(Attribute.MAX_HEALTH) != null)
            p.getAttribute(Attribute.MAX_HEALTH).removeModifier(mod);
        if (p.getAttribute(Attribute.MOVEMENT_SPEED) != null)
            p.getAttribute(Attribute.MOVEMENT_SPEED).removeModifier(mod);
        if (p.getAttribute(Attribute.ATTACK_SPEED) != null)
            p.getAttribute(Attribute.ATTACK_SPEED).removeModifier(mod);
        if (p.getAttribute(Attribute.LUCK) != null)
            p.getAttribute(Attribute.LUCK).removeModifier(mod);
        if (p.getAttribute(Attribute.KNOCKBACK_RESISTANCE) != null)
            p.getAttribute(Attribute.KNOCKBACK_RESISTANCE).removeModifier(mod);
    }
    
    public void loadTalismans() {
        File file = new File(getDataFolder(), "talismans.yml");
        if (!file.exists()) {
            saveResource("talismans.yml", false);
        }
        
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        
        for (String key : config.getKeys(false)) {
            try {
                String name = config.getString(key + ".name");
                String material = config.getString(key + ".material");
                String lore = key;
                
                Map<Attribute, Double> attributes = new HashMap<>();
                if (config.contains(key + ".attributes")) {
                    for (String attrStr : config.getConfigurationSection(key + ".attributes").getKeys(false)) {
                        Attribute attr = getAttributeByName(attrStr);
                        if (attr != null) {
                            attributes.put(attr, config.getDouble(key + ".attributes." + attrStr));
                        }
                    }
                }
                
                List<PotionEffect> effects = new ArrayList<>();
                List<String> effectsList = config.getStringList(key + ".effects");
                for (String effectStr : effectsList) {
                    String[] parts = effectStr.split(":");
                    PotionEffectType type = PotionEffectType.getByName(parts[0]);
                    if (type != null) {
                        int level = Integer.parseInt(parts[1]);
                        effects.add(new PotionEffect(type, Integer.MAX_VALUE, level, true, false));
                    }
                }
                
                talismans.put(key, new TalismanData(name, material, lore, attributes, effects));
            } catch (Exception e) {
                getLogger().warning("Ошибка загрузки талисмана " + key);
            }
        }
        getLogger().info("Загружено " + talismans.size() + " талисманов");
    }
    
    public void saveTalisman(String id, String name, String material, String lore, 
                              Map<Attribute, Double> attributes, List<PotionEffect> effects) {
        File file = new File(getDataFolder(), "talismans.yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        
        config.set(id + ".name", name);
        config.set(id + ".material", material);
        
        for (Map.Entry<Attribute, Double> entry : attributes.entrySet()) {
            String attrName = getAttributeName(entry.getKey());
            config.set(id + ".attributes." + attrName, entry.getValue());
        }
        
        List<String> effectsList = new ArrayList<>();
        for (PotionEffect effect : effects) {
            effectsList.add(effect.getType().getName() + ":" + effect.getAmplifier());
        }
        config.set(id + ".effects", effectsList);
        
        try {
            config.save(file);
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        talismans.put(id, new TalismanData(name, material, lore, attributes, effects));
    }
    
    public void deleteTalisman(String id) {
        File file = new File(getDataFolder(), "talismans.yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        config.set(id, null);
        try {
            config.save(file);
        } catch (Exception e) {
            e.printStackTrace();
        }
        talismans.remove(id);
    }
    
    private Attribute getAttributeByName(String name) {
        switch (name.toUpperCase()) {
            case "ATTACK_DAMAGE": return Attribute.ATTACK_DAMAGE;
            case "ARMOR": return Attribute.ARMOR;
            case "MAX_HEALTH": return Attribute.MAX_HEALTH;
            case "MOVEMENT_SPEED": return Attribute.MOVEMENT_SPEED;
            case "ATTACK_SPEED": return Attribute.ATTACK_SPEED;
            case "LUCK": return Attribute.LUCK;
            case "KNOCKBACK_RESISTANCE": return Attribute.KNOCKBACK_RESISTANCE;
            default: return null;
        }
    }
    
    private String getAttributeName(Attribute attr) {
        if (attr == Attribute.ATTACK_DAMAGE) return "ATTACK_DAMAGE";
        if (attr == Attribute.ARMOR) return "ARMOR";
        if (attr == Attribute.MAX_HEALTH) return "MAX_HEALTH";
        if (attr == Attribute.MOVEMENT_SPEED) return "MOVEMENT_SPEED";
        if (attr == Attribute.ATTACK_SPEED) return "ATTACK_SPEED";
        if (attr == Attribute.LUCK) return "LUCK";
        if (attr == Attribute.KNOCKBACK_RESISTANCE) return "KNOCKBACK_RESISTANCE";
        return "UNKNOWN";
    }
    
    public void applyTalisman(Player p, String id) {
        TalismanData data = talismans.get(id);
        if (data == null) return;
        
        List<AttributeModifier> modifiers = new ArrayList<>();
        for (Map.Entry<Attribute, Double> entry : data.attributes.entrySet()) {
            AttributeModifier mod = new AttributeModifier(UUID.randomUUID(), "talisman_" + id, 
                entry.getValue(), AttributeModifier.Operation.ADD_NUMBER);
            
            if (p.getAttribute(entry.getKey()) != null) {
                p.getAttribute(entry.getKey()).addModifier(mod);
            }
            modifiers.add(mod);
        }
        activeAttributes.put(p.getUniqueId(), modifiers);
        
        List<PotionEffect> appliedEffects = new ArrayList<>();
        for (PotionEffect effect : data.effects) {
            PotionEffect newEffect = new PotionEffect(effect.getType(), Integer.MAX_VALUE, effect.getAmplifier(), true, false);
            p.addPotionEffect(newEffect);
            appliedEffects.add(newEffect);
        }
        activeEffects.put(p.getUniqueId(), appliedEffects);
    }
    
    public void removeTalisman(Player p) {
        if (activeAttributes.containsKey(p.getUniqueId())) {
            for (AttributeModifier mod : activeAttributes.get(p.getUniqueId())) {
                if (mod.getAttribute() != null && p.getAttribute(mod.getAttribute()) != null) {
                    p.getAttribute(mod.getAttribute()).removeModifier(mod);
                }
            }
            activeAttributes.remove(p.getUniqueId());
        }
        
        if (activeEffects.containsKey(p.getUniqueId())) {
            for (PotionEffect effect : activeEffects.get(p.getUniqueId())) {
                p.removePotionEffect(effect.getType());
            }
            activeEffects.remove(p.getUniqueId());
        }
    }
    
    public void giveTalismanItem(Player target, String id) {
        TalismanData data = talismans.get(id);
        if (data == null) return;
        
        ItemStack item = new ItemStack(Objects.requireNonNull(Material.getMaterial(data.material)));
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', data.name));
        
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "ID: " + id);
        lore.add("");
        
        for (Map.Entry<Attribute, Double> entry : data.attributes.entrySet()) {
            String attrName = getAttributeName(entry.getKey());
            double value = entry.getValue();
            String color = value > 0 ? "§a+" : (value < 0 ? "§c" : "§7");
            lore.add(ChatColor.GRAY + attrName + ": " + color + value);
        }
        
        for (PotionEffect effect : data.effects) {
            String effectName = effect.getType().getName();
            int level = effect.getAmplifier() + 1;
            lore.add(ChatColor.GRAY + "Эффект: " + effectName + " " + level);
        }
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        
        target.getInventory().addItem(item);
    }
    
    @EventHandler
    public void onItemHeld(PlayerItemHeldEvent event) {
        Player p = event.getPlayer();
        removeTalisman(p);
        
        ItemStack item = p.getInventory().getItem(event.getNewSlot());
        checkAndApply(p, item);
    }
    
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player p = event.getPlayer();
        checkAndApply(p, p.getInventory().getItemInMainHand());
    }
    
    private void checkAndApply(Player p, ItemStack item) {
        if (item == null || !item.hasItemMeta()) return;
        
        ItemMeta meta = item.getItemMeta();
        if (!meta.hasLore()) return;
        
        List<String> lore = meta.getLore();
        if (lore == null || lore.isEmpty()) return;
        
        String firstLine = lore.get(0);
        if (firstLine.startsWith(ChatColor.GRAY + "ID: ")) {
            String id = firstLine.substring((ChatColor.GRAY + "ID: ").length());
            if (talismans.containsKey(id)) {
                applyTalisman(p, id);
            }
        }
    }
    
    public Map<String, TalismanData> getTalismans() {
        return talismans;
    }
    
    class TalismanData {
        String name;
        String material;
        String lore;
        Map<Attribute, Double> attributes;
        List<PotionEffect> effects;
        
        TalismanData(String name, String material, String lore, Map<Attribute, Double> attributes, List<PotionEffect> effects) {
            this.name = name;
            this.material = material;
            this.lore = lore;
            this.attributes = attributes;
            this.effects = effects;
        }
    }
}
