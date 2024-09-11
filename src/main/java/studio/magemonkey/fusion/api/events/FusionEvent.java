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
    private final String professionName;
    private final CraftingTable craftingTable;
    private final Player player;
    private final FusionPlayer fusionPlayer;

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
