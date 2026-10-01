package pw.elytrix.elytrixbattlepass.storage;

import pw.elytrix.elytrixbattlepass.ElytrixBattlePass;
import pw.elytrix.elytrixbattlepass.config.Settings;

public class StorageFactory {
    public static DataStorage createStorage(ElytrixBattlePass plugin) {
        Settings.MysqlSettings mysql = plugin.getConfigManager().getSettings().storage.mysql;
        if (mysql.enabled) {
            MySQLStorage mysqlStorage = new MySQLStorage(plugin, mysql.host, mysql.port, mysql.database, mysql.username, mysql.password);
            if (mysqlStorage.isConnected()) {
                plugin.getLogger().info("Storage: MySQL");
                return mysqlStorage;
            }
            plugin.getLogger().warning("MySQL недоступен, используется SQLite");
        }
        plugin.getLogger().info("Storage: SQLite");
        return new LocalStorage(plugin);
    }
}