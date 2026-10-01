package ru.rooyzee.elytrixquests.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import ru.rooyzee.elytrixquests.Main;
import ru.rooyzee.elytrixquests.util.ColorUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ConfigManager {

    private final Main plugin;
    private FileConfiguration config;
    private FileConfiguration messages;
    private FileConfiguration quests;
    private File messagesFile;
    private File questsFile;

    public ConfigManager(Main plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.saveDefaultConfig();
        config = plugin.getConfig();

        messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) plugin.saveResource("messages.yml", false);
        messages = YamlConfiguration.loadConfiguration(messagesFile);

        questsFile = new File(plugin.getDataFolder(), "quests.yml");
        if (!questsFile.exists()) plugin.saveResource("quests.yml", false);
        quests = YamlConfiguration.loadConfiguration(questsFile);

        mergeMissingMessageKeys();
        messages = YamlConfiguration.loadConfiguration(messagesFile);
    }

    /**
     * Если в jar появились новые сообщения (например, после обновления плагина),
     * а в существующем messages.yml их ещё нет — дописываем их в КОНЕЦ файла.
     * Пользовательские правки и уже существующие ключи не трогаются.
     * (quests.yml намеренно не трогаем — это игровые данные, а не шаблон.)
     */
    private void mergeMissingMessageKeys() {
        if (!messagesFile.exists()) {
            return;
        }
        try (InputStream in = plugin.getResource("messages.yml")) {
            if (in == null) {
                return;
            }
            YamlConfiguration def = YamlConfiguration
                    .loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
            YamlConfiguration cur = YamlConfiguration.loadConfiguration(messagesFile);
            List<String> missing = new ArrayList<>();
            for (String key : def.getKeys(true)) {
                if (def.isConfigurationSection(key)) {
                    continue;
                }
                if (!cur.contains(key)) {
                    missing.add(key);
                }
            }
            if (missing.isEmpty()) {
                return;
            }
            StringBuilder sb = new StringBuilder();
            sb.append("\n# --- Новые сообщения (автодополнено плагином) ---\n");
            for (String key : missing) {
                Object value = def.get(key);
                if (value instanceof String) {
                    sb.append(key).append(": \"").append(escapeYaml((String) value)).append("\"\n");
                }
            }
            Files.write(messagesFile.toPath(), sb.toString().getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.APPEND, StandardOpenOption.CREATE);
            plugin.getLogger().info("messages.yml дополнен новыми сообщениями: " + missing.size());
        } catch (IOException e) {
            plugin.getLogger().warning("Не удалось дополнить messages.yml: " + e.getMessage());
        }
    }

    private static String escapeYaml(String v) {
        return v.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    public void reload() {
        plugin.reloadConfig();
        config = plugin.getConfig();
        messages = YamlConfiguration.loadConfiguration(messagesFile);
        quests = YamlConfiguration.loadConfiguration(questsFile);
    }

    public FileConfiguration getQuestsConfig() {
        return quests;
    }

    public String getMessage(String path) {
        String msg = messages.getString(path, "&cMessage not found: " + path);
        return ColorUtils.colorize(msg);
    }

    public String getMessage(String path, Map<String, String> placeholders) {
        String msg = messages.getString(path, "&cMessage not found: " + path);
        for (Map.Entry<String, String> e : placeholders.entrySet()) {
            msg = msg.replace("%" + e.getKey() + "%", e.getValue());
        }
        return ColorUtils.colorize(msg);
    }

    public List<String> getMessageList(String path) {
        return ColorUtils.colorize(messages.getStringList(path));
    }
}