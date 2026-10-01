package pw.elytrix.elytrixbattlepass.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ShopSettings {
    public String menuTitle = "&#F8BEFB&lМагазин боевого пропуска";
    public Map<String, ShopItem> items = new LinkedHashMap<>();

    public static class ShopItem {
        public boolean enabled = true;
        public int slot = -1;
        public int cost = 0;
        public String name = "";
        public List<String> lore = new ArrayList<>();
        public String iconType = "STONE";
        public String iconTexture = "";
        public boolean iconEnchanted = false;
        public boolean premiumOnly = false;
        public boolean randomCommand = false;
        public boolean confirmPurchase = false;
        public boolean closeAfterPurchase = false;
        public String requiredPermission = "";
        public String broadcast = "";
        public String sound = "BLOCK_END_PORTAL_FRAME_FILL";
        public List<String> commands = new ArrayList<>();
        public List<RewardCommand> rewards = new ArrayList<>();
    }

    public static class RewardCommand {
        public String command = "";
        public List<String> commands = new ArrayList<>();
        public String display = "";
        public int weight = 1;

        public List<String> allCommands() {
            List<String> result = new ArrayList<>();
            if (command != null && !command.isBlank()) result.add(command);
            if (commands != null) {
                for (String cmd : commands) {
                    if (cmd != null && !cmd.isBlank()) result.add(cmd);
                }
            }
            return result;
        }
    }
}
