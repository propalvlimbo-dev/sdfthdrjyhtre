package pw.elytrix.elytrixbattlepass.storage;

import java.util.UUID;
import org.bukkit.entity.Player;
import pw.elytrix.elytrixbattlepass.user.User;

public interface DataStorage {
    void saveUser(User user);

    User loadUser(Player player);

    void deleteUser(UUID uuid);

    boolean isConnected();

    default void close() {
    }
}