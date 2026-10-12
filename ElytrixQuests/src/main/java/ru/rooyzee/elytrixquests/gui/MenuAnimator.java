package ru.rooyzee.elytrixquests.gui;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import ru.rooyzee.elytrixquests.Main;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Reveals menu icons from the center out, matching the ElytrixSeller menu animation. */
public final class MenuAnimator {

    private final Main plugin;
    private BukkitTask task;
    private boolean running;

    public MenuAnimator(Main plugin) {
        this.plugin = plugin;
    }

    public boolean isRunning() {
        return running;
    }

    /** Copies every occupied slot so static glass and buttons animate with the menu contents. */
    public Map<Integer, ItemStack> snapshot(Inventory inventory) {
        Map<Integer, ItemStack> items = new LinkedHashMap<>();
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            ItemStack item = inventory.getItem(slot);
            if (item != null) {
                items.put(slot, item.clone());
            }
        }
        return items;
    }

    public void animate(final Player player, final Inventory inventory,
                        Map<Integer, ItemStack> contents, final Runnable onComplete) {
        stop();

        if (contents == null || contents.isEmpty()) {
            runCompletion(onComplete);
            return;
        }

        final List<Map.Entry<Integer, ItemStack>> items = new ArrayList<>(contents.entrySet());
        sortFromCenter(items, inventory.getSize());

        if (!plugin.getConfig().getBoolean("gui.animation.enabled", true)) {
            placeAll(inventory, items);
            runCompletion(onComplete);
            return;
        }

        final int delay = Math.max(1, plugin.getConfig().getInt("gui.animation.delay", 1));
        final int defaultItemsPerTick = items.size() > 20 ? 2 : 1;
        final int itemsPerTick = Math.max(1,
                plugin.getConfig().getInt("gui.animation.items-per-tick", defaultItemsPerTick));
        final Sound sound = getAnimationSound();
        running = true;

        task = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            private int index;

            @Override
            public void run() {
                if (!player.isOnline() || player.getOpenInventory().getTopInventory() != inventory) {
                    stop();
                    return;
                }

                int placed = 0;
                while (index < items.size() && placed < itemsPerTick) {
                    Map.Entry<Integer, ItemStack> item = items.get(index++);
                    inventory.setItem(item.getKey(), item.getValue());
                    placed++;
                }

                if (sound != null && placed > 0) {
                    float progress = (float) index / (float) items.size();
                    float pitch = 1.1f + progress * 0.6f;
                    player.playSound(player.getLocation(), sound, 0.2f, pitch);
                }

                if (index >= items.size()) {
                    finish(onComplete);
                }
            }
        }, 0L, delay);
    }

    public void stop() {
        running = false;
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private void finish(Runnable onComplete) {
        stop();
        runCompletion(onComplete);
    }

    private void runCompletion(Runnable onComplete) {
        if (onComplete != null) {
            onComplete.run();
        }
    }

    private void placeAll(Inventory inventory, List<Map.Entry<Integer, ItemStack>> items) {
        for (Map.Entry<Integer, ItemStack> item : items) {
            inventory.setItem(item.getKey(), item.getValue());
        }
    }

    private void sortFromCenter(List<Map.Entry<Integer, ItemStack>> items, int inventorySize) {
        final double centerRow = ((inventorySize / 9.0D) - 1.0D) / 2.0D;
        final double centerColumn = 4.0D;
        Collections.sort(items, new Comparator<Map.Entry<Integer, ItemStack>>() {
            @Override
            public int compare(Map.Entry<Integer, ItemStack> first, Map.Entry<Integer, ItemStack> second) {
                double firstDistance = distanceFromCenter(first.getKey(), centerRow, centerColumn);
                double secondDistance = distanceFromCenter(second.getKey(), centerRow, centerColumn);
                return Double.compare(firstDistance, secondDistance);
            }
        });
    }

    private double distanceFromCenter(int slot, double centerRow, double centerColumn) {
        double rowDistance = slot / 9 - centerRow;
        double columnDistance = slot % 9 - centerColumn;
        return rowDistance * rowDistance + columnDistance * columnDistance;
    }

    private Sound getAnimationSound() {
        String name = plugin.getConfig().getString("gui.animation.sound", "BLOCK_NOTE_BLOCK_PLING");
        if (name == null || name.trim().isEmpty()) {
            return null;
        }
        try {
            return Sound.valueOf(name.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
