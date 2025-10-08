package studio.magemonkey.fusion.data.player;

import org.bukkit.entity.Player;
import studio.magemonkey.fusion.cfg.sql.SQLManager;

import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

public class PlayerLoader {

    private static final Map<UUID, FusionPlayer> cachedPlayers = new TreeMap<>();

    public static FusionPlayer getPlayer(UUID uuid) {
        if (!cachedPlayers.containsKey(uuid)) {
            if(SQLManager.players().isLocked(uuid)) return null;
            cachedPlayers.put(uuid, new FusionPlayer(uuid));
        }
        return cachedPlayers.get(uuid);
    }

    public static FusionPlayer getPlayer(Player player) {
        return getPlayer(player.getUniqueId());
    }

    public static void loadPlayer(Player player) {
        cachedPlayers.put(player.getUniqueId(), new FusionPlayer(player.getUniqueId()));
    }

    public static void unloadPlayer(Player player) {
        if (cachedPlayers.containsKey(player.getUniqueId())) {
            FusionPlayer fusionPlayer = cachedPlayers.get(player.getUniqueId());
            fusionPlayer.save();
            cachedPlayers.remove(player.getUniqueId());
        }
    }

    public static void clearCache() {
        for (FusionPlayer fusionPlayer : cachedPlayers.values())
            fusionPlayer.save();
        cachedPlayers.clear();
    }
}
