package studio.magemonkey.fusion.api.events;

import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import studio.magemonkey.fusion.api.FusionAPI;
import studio.magemonkey.fusion.data.player.FusionPlayer;
import studio.magemonkey.fusion.data.recipes.CraftingTable;

@Getter
public class FusionEvent extends Event implements Cancellable {


    private static final HandlerList HANDLERS = new HandlerList();

    /**
     * The name of the profession
     */
    private final String professionName;
    /**
     * The crafting table
     */
    private final CraftingTable craftingTable;
    /**
     * The player
     */
    private final Player player;
    /**
     * The fusion player instance of the player
     */
    private final FusionPlayer fusionPlayer;

    /**
     * Constructor for the FusionEvent
     *
     * @param professionName The name of the profession
     * @param craftingTable The crafting table
     * @param player The player
     */
    public FusionEvent(String professionName, CraftingTable craftingTable, Player player) {
        this.professionName = professionName;
        this.craftingTable = craftingTable;
        this.player = player;
        this.fusionPlayer = FusionAPI.getPlayerManager().getPlayer(player);
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public void setCancelled(boolean b) {
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }
}
