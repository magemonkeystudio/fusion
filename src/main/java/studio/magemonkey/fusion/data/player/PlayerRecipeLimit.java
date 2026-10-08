package studio.magemonkey.fusion.data.player;

import lombok.Getter;
import studio.magemonkey.fusion.cfg.sql.SQLManager;
import java.util.UUID;

@Getter
public class PlayerRecipeLimit {

    private final String recipeName;
    private       int    limit;
    private       long   cooldownTimestamp;
    private final UUID owner;

    public PlayerRecipeLimit(String recipeName, int limit, long cooldownTimestamp) {
        this(null, recipeName, limit, cooldownTimestamp);
    }

    public PlayerRecipeLimit(UUID owner, String recipeName, int limit, long cooldownTimestamp) {
        this.owner = owner;
        this.recipeName = recipeName;
        this.limit = limit;
        this.cooldownTimestamp = cooldownTimestamp;
    }

    public void incrementLimit(int amount) {
        if (owner != null) SQLManager.recipeLimits().incrementLimit(owner, recipeName, amount, 0);
        limit += amount;
    }

    public void updateCooldown(int seconds) {
        long timestamp = System.currentTimeMillis() + (seconds * 1000L);
        if (owner != null) SQLManager.recipeLimits().updateCooldown(owner, recipeName, timestamp);
        cooldownTimestamp = timestamp;
    }

    public boolean hasCooldown() {
        return cooldownTimestamp > System.currentTimeMillis();
    }

    public void resetLimit() {
        if (owner != null) SQLManager.recipeLimits().resetLimit(owner, recipeName);
        limit = 0;
        cooldownTimestamp = -1;
    }
}
