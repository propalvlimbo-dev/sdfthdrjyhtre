package ru.rooyzee.elytrixquests.util;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

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