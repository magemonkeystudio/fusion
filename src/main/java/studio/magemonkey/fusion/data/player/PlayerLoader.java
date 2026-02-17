package studio.magemonkey.fusion.data.player;

import org.bukkit.entity.Player;
import studio.magemonkey.fusion.Fusion;
import studio.magemonkey.fusion.cfg.sql.SQLManager;

import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

public class PlayerLoader {

    private static final Map<UUID, FusionPlayer> cachedPlayers = new TreeMap<>();

    public static FusionPlayer getPlayer(UUID uuid) {
        // Always return a FusionPlayer instance. Previously this returned null when the player was marked as
        // locked in the database (during async save), which caused NullPointerExceptions at many call sites.
        // Returning a FusionPlayer ensures call sites remain stable; the locking is handled at the persistence layer.
        if (!cachedPlayers.containsKey(uuid)) {
            cachedPlayers.put(uuid, new FusionPlayer(uuid));
        }
        return cachedPlayers.get(uuid);
    }

    public static FusionPlayer getPlayer(Player player) {
        return getPlayer(player.getUniqueId());
    }

    /**
     * Returns the FusionPlayer if it is not currently locked for saving, otherwise returns null.
     * This provides an explicit, safe way for call sites that cannot proceed during a save window.
     */
    public static FusionPlayer getPlayerIfReady(UUID uuid) {
        FusionPlayer fp = getPlayer(uuid);
        if (fp == null) return null; // defensive, but getPlayer never returns null
        if (fp.isLocked()) return null;
        return fp;
    }

    public static FusionPlayer getPlayerIfReady(Player player) {
        return getPlayerIfReady(player.getUniqueId());
    }

    /**
     * Blocks (polling) until the player's DB lock is cleared or until timeoutMs is reached.
     * Returns the FusionPlayer if the lock cleared within the timeout, otherwise returns null.
     * Note: Blocking the main server thread is dangerous; call this from an async thread or keep timeout small.
     */
    public static FusionPlayer getPlayerBlocking(UUID uuid, long timeoutMs) {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < timeoutMs) {
            boolean locked = SQLManager.players().isLocked(uuid);
            if (!locked) {
                return getPlayer(uuid);
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        return null;
    }

    public static FusionPlayer getPlayerBlocking(Player player, long timeoutMs) {
        return getPlayerBlocking(player.getUniqueId(), timeoutMs);
    }

    public static void loadPlayer(Player player) {
        cachedPlayers.put(player.getUniqueId(), new FusionPlayer(player.getUniqueId()));
    }

    public static void unloadPlayer(Player player) {
        if (cachedPlayers.containsKey(player.getUniqueId())) {
            FusionPlayer fusionPlayer = cachedPlayers.get(player.getUniqueId());
            fusionPlayer.save(true);
            cachedPlayers.remove(player.getUniqueId());
        }
    }

    public static void clearCache() {
        for (FusionPlayer fusionPlayer : cachedPlayers.values())
            fusionPlayer.save(true);
        cachedPlayers.clear();
    }
}
