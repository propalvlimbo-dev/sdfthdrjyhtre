package pw.elytrix.elytrixbattlepass.tasks;

import org.bukkit.event.Listener;
import pw.elytrix.elytrixbattlepass.ElytrixBattlePass;

public abstract class TaskType implements Listener {
    private final String name;
    protected final ElytrixBattlePass plugin;

    protected TaskType(String name, ElytrixBattlePass plugin) {
        this.name = name;
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public String getName() {
        return name;
    }
}