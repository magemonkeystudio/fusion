package studio.magemonkey.fusion.api;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import studio.magemonkey.fusion.data.player.FusionPlayer;
import studio.magemonkey.fusion.data.player.PlayerLoader;

import java.util.UUID;

public class PlayerManager {

    /**
     * Get the FusionPlayer object of a player by his name.
     *
     * @param playerName The name of the player.
     * @return The FusionPlayer object of the player.
     */
    public FusionPlayer getPlayer(String playerName) {
        Player player = Bukkit.getPlayer(playerName);
        if(player == null) return null;
        return getPlayer(player);
    }

    /**
     * Get the FusionPlayer object of a player by his UUID.
     *
     * @param player The Player object of the player.
     * @return The FusionPlayer object of the player.
     */
    public FusionPlayer getPlayer(Player player) {
        return PlayerLoader.getPlayer(player);
    }

    /**
     * Get the FusionPlayer object of a player by his UUID.
     *
     * @param uuid The UUID of the player.
     * @return The FusionPlayer object of the player.
     */
    public FusionPlayer getPlayer(UUID uuid) {
        return PlayerLoader.getPlayer(uuid);
    }

    /**
     * Save the player data of a player.
     * This will save the player data to the database and reload the player.
     *
     * @param player The Player object of the player.
     */
    public void savePlayer(Player player) {
        FusionPlayer fusionPlayer = getPlayer(player);
        if(fusionPlayer != null) {
            fusionPlayer.save();
            PlayerLoader.unloadPlayer(player);
            PlayerLoader.loadPlayer(player);
        } else {
            FusionAPI.getInstance().getLogger().warning("Failed to save player " + player.getName() + " because he is not loaded.");
        }
    }

    /**
     * Save the player data of a player.
     * This will save all players to the database and reload them.
     *
     */
    public void saveAllPlayers() {
        Bukkit.getOnlinePlayers().forEach(this::savePlayer);
    }

    /**
     * Clear the cache of all players.
     * This will save all players to the database and clear the cache.
     * They wont be reloaded until they are accessed again.
     */
    public void clearAllPlayers() {
        PlayerLoader.clearCache();
    }
}
