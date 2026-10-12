package ru.rooyzee.elytrixquests.util;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.Arrays;
import java.util.List;

public class ItemBuilder {

    private final ItemStack item;
    private final ItemMeta meta;

    public ItemBuilder(Material material) {
        this.item = new ItemStack(material);
        this.meta = item.getItemMeta();
    }

    public ItemBuilder name(String name) {
        meta.setDisplayName(ColorUtils.colorize(name));
        return this;
    }

    public ItemBuilder lore(String... lines) {
        meta.setLore(ColorUtils.colorize(Arrays.asList(lines)));
        return this;
    }

    public ItemBuilder lore(List<String> lines) {
        meta.setLore(ColorUtils.colorize(lines));
        return this;
    }

    public ItemBuilder glow() {
        meta.addEnchant(Enchantment.DURABILITY, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        return this;
    }

    /** Applies a base64 Minecraft texture to a player head without a hard dependency on CraftBukkit internals. */
    public ItemBuilder texture(String value) {
        if (!(meta instanceof SkullMeta) || value == null || value.isEmpty()) return this;
        try {
            Class<?> profileClass = Class.forName("com.mojang.authlib.GameProfile");
            Object profile = profileClass.getConstructor(java.util.UUID.class, String.class)
                    .newInstance(java.util.UUID.randomUUID(), null);
            Object properties = profileClass.getMethod("getProperties").invoke(profile);
            Class<?> propertyClass = Class.forName("com.mojang.authlib.properties.Property");
            Object property = propertyClass.getConstructor(String.class, String.class)
                    .newInstance("textures", value);
            properties.getClass().getMethod("put", Object.class, Object.class)
                    .invoke(properties, "textures", property);
            meta.getClass().getMethod("setProfile", profileClass).invoke(meta, profile);
        } catch (Throwable ignored) {
            // If running on an API without CraftBukkit profile support, keep a normal head.
        }
        return this;
    }

    public ItemBuilder hideFlags() {
        meta.addItemFlags(ItemFlag.values());
        return this;
    }

    public ItemBuilder amount(int amount) {
        item.setAmount(amount);
        return this;
    }

    public ItemStack build() {
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack filler(Material material) {
        return new ItemBuilder(material).name(" ").build();
    }
}