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

import java.io.File;
import java.util.*;

public class Main extends JavaPlugin implements Listener {
    
    private Map<UUID, List<AttributeModifier>> activeAttributes = new HashMap<>();
    private Map<UUID, List<PotionEffect>> activeEffects = new HashMap<>();
    private Map<String, TalismanData> talismans = new HashMap<>();
    
    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        loadTalismans();
        
        getCommand("talisman").setExecutor(new TalismanCommand(this));
        
        getLogger().info("=====================================");
        getLogger().info("TalismansPlugin v2.0 ВКЛЮЧЕН!");
        getLogger().info("=====================================");
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
            Player p = getServer().getPlayer(uuid);
            if (p != null) {
                for (PotionEffect effect : activeEffects.get(uuid)) {
                    p.removePotionEffect(effect.getType());
                }
            }
        }
        getLogger().info("TalismansPlugin выключен!");
    }
    
    private void removeAttributeModifier(Player p, AttributeModifier mod) {
        // Упрощённая версия — просто обновляем атрибуты при снятии
        p.getAttribute(Attribute.ATTACK_DAMAGE).removeModifier(mod);
        p.getAttribute(Attribute.ARMOR).removeModifier(mod);
        p.getAttribute(Attribute.MAX_HEALTH).removeModifier(mod);
        p.getAttribute(Attribute.MOVEMENT_SPEED).removeModifier(mod);
        p.getAttribute(Attribute.ATTACK_SPEED).removeModifier(mod);
        p.getAttribute(Attribute.LUCK).removeModifier(mod);
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
            
            if (entry.getKey() == Attribute.ATTACK_DAMAGE) {
                p.getAttribute(Attribute.ATTACK_DAMAGE).addModifier(mod);
            } else if (entry.getKey() == Attribute.ARMOR) {
                p.getAttribute(Attribute.ARMOR).addModifier(mod);
            } else if (entry.getKey() == Attribute.MAX_HEALTH) {
                p.getAttribute(Attribute.MAX_HEALTH).addModifier(mod);
                double newHealth = p.getHealth() + entry.getValue();
                if (newHealth > p.getAttribute(Attribute.MAX_HEALTH).getValue()) {
                    newHealth = p.getAttribute(Attribute.MAX_HEALTH).getValue();
                }
                p.setHealth(newHealth);
            } else if (entry.getKey() == Attribute.MOVEMENT_SPEED) {
                p.getAttribute(Attribute.MOVEMENT_SPEED).addModifier(mod);
            } else if (entry.getKey() == Attribute.ATTACK_SPEED) {
                p.getAttribute(Attribute.ATTACK_SPEED).addModifier(mod);
            } else if (entry.getKey() == Attribute.LUCK) {
                p.getAttribute(Attribute.LUCK).addModifier(mod);
            } else if (entry.getKey() == Attribute.KNOCKBACK_RESISTANCE) {
                p.getAttribute(Attribute.KNOCKBACK_RESISTANCE).addModifier(mod);
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
                p.getAttribute(Attribute.ATTACK_DAMAGE).removeModifier(mod);
                p.getAttribute(Attribute.ARMOR).removeModifier(mod);
                p.getAttribute(Attribute.MAX_HEALTH).removeModifier(mod);
                p.getAttribute(Attribute.MOVEMENT_SPEED).removeModifier(mod);
                p.getAttribute(Attribute.ATTACK_SPEED).removeModifier(mod);
                p.getAttribute(Attribute.LUCK).removeModifier(mod);
                p.getAttribute(Attribute.KNOCKBACK_RESISTANCE).removeModifier(mod);
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
            String color = value > 0 ? "§a+" : "§c";
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
