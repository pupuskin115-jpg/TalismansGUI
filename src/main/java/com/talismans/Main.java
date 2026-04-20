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
        getLogger().info("Команды: /talisman, /talisman give, /talisman list");
        getLogger().info("=====================================");
    }
    
    @Override
    public void onDisable() {
        // Снимаем все эффекты и атрибуты
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
        // Удаляем модификатор атрибута
        if (mod.getAttribute().equals(Attribute.GENERIC_ATTACK_DAMAGE)) {
            p.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).removeModifier(mod);
        } else if (mod.getAttribute().equals(Attribute.GENERIC_ARMOR)) {
            p.getAttribute(Attribute.GENERIC_ARMOR).removeModifier(mod);
        } else if (mod.getAttribute().equals(Attribute.GENERIC_MAX_HEALTH)) {
            p.getAttribute(Attribute.GENERIC_MAX_HEALTH).removeModifier(mod);
        } else if (mod.getAttribute().equals(Attribute.GENERIC_MOVEMENT_SPEED)) {
            p.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).removeModifier(mod);
        } else if (mod.getAttribute().equals(Attribute.GENERIC_ATTACK_SPEED)) {
            p.getAttribute(Attribute.GENERIC_ATTACK_SPEED).removeModifier(mod);
        } else if (mod.getAttribute().equals(Attribute.GENERIC_LUCK)) {
            p.getAttribute(Attribute.GENERIC_LUCK).removeModifier(mod);
        } else if (mod.getAttribute().equals(Attribute.GENERIC_KNOCKBACK_RESISTANCE)) {
            p.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE).removeModifier(mod);
        }
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
                String lore = config.getString(key + ".lore");
                
                // Загрузка атрибутов
                Map<Attribute, Double> attributes = new HashMap<>();
                if (config.contains(key + ".attributes")) {
                    for (String attrStr : config.getConfigurationSection(key + ".attributes").getKeys(false)) {
                        Attribute attr = getAttributeByName(attrStr);
                        if (attr != null) {
                            attributes.put(attr, config.getDouble(key + ".attributes." + attrStr));
                        }
                    }
                }
                
                // Загрузка эффектов зелий
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
        config.set(id + ".lore", lore);
        
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
            case "ATTACK_DAMAGE": return Attribute.GENERIC_ATTACK_DAMAGE;
            case "ARMOR": return Attribute.GENERIC_ARMOR;
            case "MAX_HEALTH": return Attribute.GENERIC_MAX_HEALTH;
            case "MOVEMENT_SPEED": return Attribute.GENERIC_MOVEMENT_SPEED;
            case "ATTACK_SPEED": return Attribute.GENERIC_ATTACK_SPEED;
            case "LUCK": return Attribute.GENERIC_LUCK;
            case "KNOCKBACK_RESISTANCE": return Attribute.GENERIC_KNOCKBACK_RESISTANCE;
            default: return null;
        }
    }
    
    private String getAttributeName(Attribute attr) {
        if (attr.equals(Attribute.GENERIC_ATTACK_DAMAGE)) return "ATTACK_DAMAGE";
        if (attr.equals(Attribute.GENERIC_ARMOR)) return "ARMOR";
        if (attr.equals(Attribute.GENERIC_MAX_HEALTH)) return "MAX_HEALTH";
        if (attr.equals(Attribute.GENERIC_MOVEMENT_SPEED)) return "MOVEMENT_SPEED";
        if (attr.equals(Attribute.GENERIC_ATTACK_SPEED)) return "ATTACK_SPEED";
        if (attr.equals(Attribute.GENERIC_LUCK)) return "LUCK";
        if (attr.equals(Attribute.GENERIC_KNOCKBACK_RESISTANCE)) return "KNOCKBACK_RESISTANCE";
        return "UNKNOWN";
    }
    
    public void applyTalisman(Player p, String id) {
        TalismanData data = talismans.get(id);
        if (data == null) return;
        
        // Применяем атрибуты
        List<AttributeModifier> modifiers = new ArrayList<>();
        for (Map.Entry<Attribute, Double> entry : data.attributes.entrySet()) {
            AttributeModifier mod = new AttributeModifier(UUID.randomUUID(), "talisman_" + id, 
                entry.getValue(), AttributeModifier.Operation.ADD_NUMBER);
            
            if (entry.getKey().equals(Attribute.GENERIC_ATTACK_DAMAGE)) {
                p.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).addModifier(mod);
            } else if (entry.getKey().equals(Attribute.GENERIC_ARMOR)) {
                p.getAttribute(Attribute.GENERIC_ARMOR).addModifier(mod);
            } else if (entry.getKey().equals(Attribute.GENERIC_MAX_HEALTH)) {
                p.getAttribute(Attribute.GENERIC_MAX_HEALTH).addModifier(mod);
                double newHealth = p.getHealth() + entry.getValue();
                if (newHealth > p.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue()) {
                    newHealth = p.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
                }
                p.setHealth(newHealth);
            } else if (entry.getKey().equals(Attribute.GENERIC_MOVEMENT_SPEED)) {
                p.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).addModifier(mod);
            } else if (entry.getKey().equals(Attribute.GENERIC_ATTACK_SPEED)) {
                p.getAttribute(Attribute.GENERIC_ATTACK_SPEED).addModifier(mod);
            } else if (entry.getKey().equals(Attribute.GENERIC_LUCK)) {
                p.getAttribute(Attribute.GENERIC_LUCK).addModifier(mod);
            } else if (entry.getKey().equals(Attribute.GENERIC_KNOCKBACK_RESISTANCE)) {
                p.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE).addModifier(mod);
            }
            modifiers.add(mod);
        }
        activeAttributes.put(p.getUniqueId(), modifiers);
        
        // Применяем эффекты зелий
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
                removeAttributeModifier(p, mod);
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
