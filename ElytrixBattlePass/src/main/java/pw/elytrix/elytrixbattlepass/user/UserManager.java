package pw.elytrix.elytrixbattlepass.user;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.entity.Player;
import pw.elytrix.elytrixbattlepass.storage.DataStorage;

public class UserManager {
    private final ConcurrentHashMap<UUID, User> users = new ConcurrentHashMap<>();
    private final DataStorage storage;

    public UserManager(DataStorage storage) {
        this.storage = storage;
    }

    public User createNewUser(Player player) {
        User user = User.create(player);
        users.put(player.getUniqueId(), user);
        return user;
    }

    public User getUser(Player player) {
        User cached = users.get(player.getUniqueId());
        if (cached != null) {
            cached.attachPlayer(player);
            return cached;
        }

        User loaded = storage.loadUser(player);
        if (loaded == null) {
            loaded = User.create(player);
        } else {
            loaded.attachPlayer(player);
        }

        users.put(player.getUniqueId(), loaded);
        return loaded;
    }

    public User getUserFromCache(Player player) {
        return getUserFromCache(player.getUniqueId());
    }

    public User getUserFromCache(UUID uuid) {
        return users.get(uuid);
    }

    public void updateUser(User user) {
        if (user == null) {
            return;
        }
        users.put(user.getUuid(), user);
        storage.saveUser(user);
    }

    public void deleteUserFromCache(Player player) {
        users.remove(player.getUniqueId());
    }

    public ConcurrentHashMap<UUID, User> getUsers() {
        return users;
    }
}