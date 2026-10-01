package ru.rooyzee.elytrixquests.listener;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.entity.EntityTameEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerLevelChangeEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerShearEntityEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.projectiles.ProjectileSource;
import ru.rooyzee.elytrixquests.Main;
import ru.rooyzee.elytrixquests.quest.QuestManager;
import ru.rooyzee.elytrixquests.quest.QuestType;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QuestTrackingListener implements Listener {

    private final Main plugin;
    private final Map<UUID, UUID> lastDamager = new HashMap<>();

    private static final Pattern LP_PARENT = Pattern.compile(
            "(?:^|\\s)(?:lp|luckperms)\\s+user\\s+(\\S+)\\s+parent\\s+(?:add|set|addtemp|settemp)\\s+(\\S+)",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern LP_PARENT_SHORT = Pattern.compile(
            "(?:^|\\s)user\\s+(\\S+)\\s+parent\\s+(?:add|set|addtemp|settemp)\\s+(\\S+)",
            Pattern.CASE_INSENSITIVE
    );

    public QuestTrackingListener(Main plugin) {
        this.plugin = plugin;
    }

    private QuestManager qm() {
        return plugin.getQuestManager();
    }

    private String normalizeLabel(String raw) {
        String label = raw.toLowerCase(Locale.ROOT).split(" ")[0];
        if (label.contains(":")) {
            label = label.substring(label.indexOf(':') + 1);
        }
        return label;
    }

    private boolean matchesTrigger(String trigger, String label) {
        if (trigger == null || trigger.isEmpty()) return false;
        for (String part : trigger.toLowerCase(Locale.ROOT).split(",")) {
            if (part.trim().equals(label)) return true;
        }
        return false;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent e) {
        String raw = e.getMessage().substring(1).trim();
        if (raw.isEmpty()) return;

        String lower = raw.toLowerCase(Locale.ROOT);
        String label = normalizeLabel(lower);
        Player player = e.getPlayer();

        if (!label.equals("warp") && !label.equals("warps")) {
            qm().incrementProgress(player, QuestType.COMMAND_TRIGGER,
                    q -> matchesTrigger(q.getStringData(), label), 1);
        }


        if (label.equals("warp") || label.equals("warps")) {
            qm().markAwaitingWarp(player);
        }

        if (lower.startsWith("rg claim") || lower.startsWith("region claim")
                || lower.startsWith("rg create") || lower.startsWith("region create")) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (plugin.getWorldGuardHook() != null
                        && plugin.getWorldGuardHook().isEnabled()
                        && plugin.getWorldGuardHook().hasRegion(player)) {
                    qm().incrementProgress(player, QuestType.REGION_CLAIM, q -> true, 1);
                }
            }, 20L);
        }

        if (isKitCommand(label, lower)) {
            final String kitName = extractKitName(lower);
            if (kitName != null) {
                plugin.getServer().getScheduler().runTaskLater(plugin, () ->
                        qm().handleKitClaim(player, kitName), 10L);
            }
        }

        handleLpLikeString(raw);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player player = (Player) e.getWhoClicked();
        if (e.getView() == null) return;

        // Кастомный шлем Солнца создаётся наковальней: после забора результата
        // книга исчезает, а готовый шлем появляется в инвентаре.
        if (e.getRawSlot() == 2 && e.getView().getTopInventory() instanceof org.bukkit.inventory.AnvilInventory
                && e.getCurrentItem() != null && e.getCurrentItem().getType() == Material.GOLDEN_HELMET
                && hasSunText(e.getCurrentItem())) {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                for (org.bukkit.inventory.ItemStack item : player.getInventory().getContents()) {
                    if (item != null && item.getType() == Material.GOLDEN_HELMET && hasSunText(item)) {
                        qm().incrementProgress(player, QuestType.CRAFT_SUN_HELMET, q -> true, 1);
                        break;
                    }
                }
            });
        }

        String title = e.getView().getTitle().toLowerCase(Locale.ROOT);
        if (title.contains("кит") || title.contains("kit") || title.contains("набор")) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                List<String> kits = qm().getDonateKitsForPlayer(player);
                for (String k : kits) {
                    qm().handleKitClaim(player, k);
                }
            }, 10L);
        }
    }

    private boolean hasSunText(org.bukkit.inventory.ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        String text = (meta.hasDisplayName() ? meta.getDisplayName() : "").toLowerCase(Locale.ROOT);
        if (text.contains("солн") || text.contains("sun")) return true;
        if (meta.hasLore() && meta.getLore() != null) {
            for (String line : meta.getLore()) {
                String lower = line.toLowerCase(Locale.ROOT);
                if (lower.contains("солнеч") || lower.contains("sun shackles")) return true;
            }
        }
        return false;
    }

    private boolean isKitCommand(String label, String fullLower) {
        if (label.equals("kit") || label.equals("kits") || label.equals("playerkits")
                || label.equals("pk") || label.equals("kitclaim")) return true;
        return fullLower.startsWith("playerkits ") || fullLower.startsWith("pk ");
    }

    private String extractKitName(String lower) {
        String[] parts = lower.trim().split("\\s+");
        if (parts.length < 2) return null;

        if (parts[0].contains(":")) {
            parts[0] = parts[0].substring(parts[0].indexOf(':') + 1);
        }

        int i;
        if (parts[0].equals("pk") || parts[0].equals("playerkits")) {
            i = 1;
            if (i < parts.length && (parts[i].equals("kit") || parts[i].equals("kits"))) {
                i++;
            }
        } else {
            i = 1;
        }

        if (i >= parts.length) return null;

        if (parts[i].equals("claim") || parts[i].equals("get")
                || parts[i].equals("preview") || parts[i].equals("open")) {
            i++;
        }

        if (i >= parts.length) return null;

        String name = parts[i];
        if (name.equals("claim") || name.equals("list") || name.equals("help") || name.equals("gui")) {
            return null;
        }
        return name;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent e) {
        PlayerTeleportEvent.TeleportCause cause = e.getCause();
        if (cause != PlayerTeleportEvent.TeleportCause.PLUGIN
                && cause != PlayerTeleportEvent.TeleportCause.COMMAND
                && cause != PlayerTeleportEvent.TeleportCause.UNKNOWN) {
            return;
        }
        if (e.getFrom() == null || e.getTo() == null) return;
        if (e.getFrom().getWorld() == null || e.getTo().getWorld() == null) return;

        if (e.getFrom().getWorld().equals(e.getTo().getWorld())
                && e.getFrom().distanceSquared(e.getTo()) < 4.0) {
            return;
        }

        qm().handleWarpTeleport(e.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onServerCommand(ServerCommandEvent e) {
        handleLpLikeString(e.getCommand());
    }

    private void handleLpLikeString(String command) {
        if (command == null || command.isEmpty()) return;

        String cmd = command.trim();
        if (cmd.startsWith("/")) cmd = cmd.substring(1);

        Matcher m = LP_PARENT.matcher(cmd);
        if (!m.find()) {
            m = LP_PARENT_SHORT.matcher(cmd);
            if (!m.find()) return;
        }

        String playerName = m.group(1);
        qm().handleDonateReceive(playerName);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTotem(EntityResurrectEvent e) {
        if (!(e.getEntity() instanceof Player)) return;
        Player player = (Player) e.getEntity();
        qm().incrementProgress(player, QuestType.USE_TOTEM, q -> true, 1);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof Player)) return;
        Player victim = (Player) e.getEntity();
        Player attacker = null;

        if (e.getDamager() instanceof Player) {
            attacker = (Player) e.getDamager();
        } else if (e.getDamager() instanceof org.bukkit.entity.Projectile) {
            ProjectileSource src = ((org.bukkit.entity.Projectile) e.getDamager()).getShooter();
            if (src instanceof Player) attacker = (Player) src;
        }

        if (attacker != null && !attacker.getUniqueId().equals(victim.getUniqueId())) {
            lastDamager.put(victim.getUniqueId(), attacker.getUniqueId());
            if (e.getFinalDamage() > 0) {
                qm().handleCombatHit(attacker, victim);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent e) {
        Player killer = e.getEntity().getKiller();

        if (e.getEntity() instanceof Player) {
            Player victim = (Player) e.getEntity();
            if (killer == null) {
                UUID lastId = lastDamager.remove(victim.getUniqueId());
                if (lastId != null) {
                    killer = org.bukkit.Bukkit.getPlayer(lastId);
                }
            } else {
                lastDamager.remove(victim.getUniqueId());
            }
            if (killer != null) {
                qm().handleUniquePlayerKill(killer, victim);
                qm().incrementProgress(killer, QuestType.KILL_PLAYER, q -> true, 1);
            }
            return;
        }

        if (killer == null) return;
        EntityType type = e.getEntityType();
        qm().incrementProgress(killer, QuestType.KILL_MOB,
                q -> q.getEntityType() == null || q.getEntityType() == type, 1);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent e) {
        Material mat = e.getBlock().getType();
        qm().incrementProgress(e.getPlayer(), QuestType.BREAK_BLOCK,
                q -> q.getMaterial() == null || q.getMaterial() == mat, 1);
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent e) {
        Material mat = e.getBlock().getType();
        qm().incrementProgress(e.getPlayer(), QuestType.PLACE_BLOCK,
                q -> q.getMaterial() == null || q.getMaterial() == mat, 1);
        if (mat == Material.SPAWNER) {
            qm().incrementProgress(e.getPlayer(), QuestType.PLACE_SPAWNER, q -> true, 1);
        }
    }

    @EventHandler
    public void onCraft(CraftItemEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();
        if (e.getRecipe() == null || e.getRecipe().getResult() == null) return;
        Material mat = e.getRecipe().getResult().getType();
        int amount = e.getRecipe().getResult().getAmount();
        qm().incrementProgress(p, QuestType.CRAFT_ITEM,
                q -> q.getMaterial() == null || q.getMaterial() == mat, amount);
        if (mat == Material.GOLDEN_HELMET) {
            qm().incrementProgress(p, QuestType.CRAFT_SUN_HELMET, q -> true, amount);
        }
    }

    @EventHandler
    public void onSmelt(FurnaceExtractEvent e) {
        qm().incrementProgress(e.getPlayer(), QuestType.SMELT_ITEM,
                q -> q.getMaterial() == null || q.getMaterial() == e.getItemType(), e.getItemAmount());
    }

    @EventHandler
    public void onFish(PlayerFishEvent e) {
        if (e.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;
        qm().incrementProgress(e.getPlayer(), QuestType.FISH_ITEM, q -> true, 1);
    }

    @EventHandler
    public void onEnchant(EnchantItemEvent e) {
        qm().incrementProgress(e.getEnchanter(), QuestType.ENCHANT_ITEM, q -> true, 1);
    }

    @EventHandler
    public void onSunHelmetAnvil(PrepareAnvilEvent e) {
        if (!(e.getView().getPlayer() instanceof Player)) return;
        if (e.getResult() == null || e.getResult().getType() != Material.GOLDEN_HELMET) return;
        boolean sun = false;
        if (e.getResult().hasItemMeta()) {
            org.bukkit.inventory.meta.ItemMeta meta = e.getResult().getItemMeta();
            String name = meta.hasDisplayName() ? meta.getDisplayName().toLowerCase(Locale.ROOT) : "";
            sun = name.contains("солн") || name.contains("sun");
            if (!sun && meta.hasLore() && meta.getLore() != null) {
                for (String line : meta.getLore()) {
                    String lower = line.toLowerCase(Locale.ROOT);
                    if (lower.contains("солнеч") || lower.contains("sun shackles")) { sun = true; break; }
                }
            }
        }
        if (sun) qm().incrementProgress((Player) e.getView().getPlayer(), QuestType.CRAFT_SUN_HELMET, q -> true, 1);
    }

    @EventHandler
    public void onTame(EntityTameEvent e) {
        if (!(e.getOwner() instanceof Player)) return;
        Player p = (Player) e.getOwner();
        EntityType type = e.getEntityType();
        qm().incrementProgress(p, QuestType.TAME_ANIMAL,
                q -> q.getEntityType() == null || q.getEntityType() == type, 1);
    }

    @EventHandler
    public void onBreed(EntityBreedEvent e) {
        if (!(e.getBreeder() instanceof Player)) return;
        qm().incrementProgress((Player) e.getBreeder(), QuestType.BREED_ANIMAL, q -> true, 1);
    }

    @EventHandler
    public void onShear(PlayerShearEntityEvent e) {
        qm().incrementProgress(e.getPlayer(), QuestType.SHEAR_SHEEP, q -> true, 1);
    }

    @EventHandler
    public void onConsume(PlayerItemConsumeEvent e) {
        Material mat = e.getItem().getType();
        qm().incrementProgress(e.getPlayer(), QuestType.CONSUME_ITEM,
                q -> q.getMaterial() == null || q.getMaterial() == mat, 1);
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent e) {
        qm().incrementProgress(e.getPlayer(), QuestType.CHAT_MESSAGE, q -> true, 1);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        qm().incrementProgress(e.getEntity(), QuestType.DEATH, q -> true, 1);
    }

    @EventHandler
    public void onLevelChange(PlayerLevelChangeEvent e) {
        qm().setProgressAbsolute(e.getPlayer(), QuestType.LEVEL_UP, q -> true, e.getNewLevel());
    }

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        if (e.getFrom().getWorld() == null || e.getTo() == null) return;
        if (e.getFrom().getWorld() != e.getTo().getWorld()) return;
        double distance = e.getFrom().distance(e.getTo());
        if (distance < 0.01 || distance > 10) return;
        qm().incrementProgress(e.getPlayer(), QuestType.TRAVEL_DISTANCE, q -> true, (int) distance);
    }

    @EventHandler
    public void onAdvancement(PlayerAdvancementDoneEvent e) {
        String key = e.getAdvancement().getKey().toString();
        qm().incrementProgress(e.getPlayer(), QuestType.ADVANCEMENT,
                q -> q.getStringData() != null && q.getStringData().equalsIgnoreCase(key), 1);
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent e) {
        if (!(e.getEntity() instanceof Player)) return;
        Player p = (Player) e.getEntity();
        Material mat = e.getItem().getItemStack().getType();
        int amount = e.getItem().getItemStack().getAmount();
        qm().incrementProgress(p, QuestType.ITEM_PICKUP,
                q -> q.getMaterial() == null || q.getMaterial() == mat, amount);
        plugin.getServer().getScheduler().runTask(plugin, () -> qm().updateFlowerCollection(p));
    }

    @EventHandler
    public void onPotion(EntityPotionEffectEvent e) {
        if (!(e.getEntity() instanceof Player)) return;
        if (e.getNewEffect() == null) return;
        Player p = (Player) e.getEntity();
        String effectName = e.getNewEffect().getType().getName();
        qm().incrementProgress(p, QuestType.POTION_EFFECT,
                q -> q.getStringData() == null || q.getStringData().equalsIgnoreCase(effectName), 1);
    }
}