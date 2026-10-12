package ru.rooyzee.elytrixquests.util;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

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

    /** Applies a base64 texture to a player head, including Spigot 1.16 CraftMetaSkull. */
    public ItemBuilder texture(String value) {
        if (!(meta instanceof SkullMeta) || value == null || value.isEmpty()) {
            return this;
        }

        try {
            Class<?> profileClass = Class.forName("com.mojang.authlib.GameProfile");
            Object profile = profileClass.getConstructor(UUID.class, String.class)
                    .newInstance(UUID.randomUUID(), "Elytrix");

            Object properties = profileClass.getMethod("getProperties").invoke(profile);
            Class<?> propertyClass = Class.forName("com.mojang.authlib.properties.Property");
            Object property = propertyClass.getConstructor(String.class, String.class)
                    .newInstance("textures", value);
            properties.getClass().getMethod("put", Object.class, Object.class)
                    .invoke(properties, "textures", property);

            if (!applyUsingSetter((SkullMeta) meta, profileClass, profile)) {
                Field profileField = findProfileField(meta.getClass(), profileClass);
                profileField.setAccessible(true);
                profileField.set(meta, profile);
            }
        } catch (Throwable ignored) {
            // Keep a regular player head on servers that do not expose their profile implementation.
        }
        return this;
    }

    private boolean applyUsingSetter(SkullMeta skullMeta, Class<?> profileClass, Object profile) {
        try {
            Method setter = skullMeta.getClass().getMethod("setProfile", profileClass);
            setter.invoke(skullMeta, profile);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private Field findProfileField(Class<?> metaClass, Class<?> profileClass) throws NoSuchFieldException {
        for (Class<?> type = metaClass; type != null; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                if (field.getName().equalsIgnoreCase("profile")
                        && field.getType().isAssignableFrom(profileClass)) {
                    return field;
                }
            }
        }
        throw new NoSuchFieldException("CraftMetaSkull profile field not found");
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
