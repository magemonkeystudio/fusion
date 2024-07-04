package studio.magemonkey.fusion.cfg.player;

import org.bukkit.entity.Player;

import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

public class PlayerLoader {

    private static final Map<UUID, FusionPlayer> cachedPlayers = new TreeMap<>();

    public static FusionPlayer getPlayer(UUID uuid) {
        if (!cachedPlayers.containsKey(uuid)) {
            cachedPlayers.put(uuid, new FusionPlayer(uuid));
        }
        return cachedPlayers.get(uuid);
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
