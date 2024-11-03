package studio.magemonkey.fusion.data.player;

import lombok.Getter;

@Getter
public class PlayerRecipeLimit {

    private final String recipeName;
    private int limit;
    private long cooldownTimestamp;

    public PlayerRecipeLimit(String recipeName, int limit, long cooldownTimestamp) {
        this.recipeName = recipeName;
        this.limit = limit;
        this.cooldownTimestamp = cooldownTimestamp;
    }

    public void incrementLimit(int amount) {
        limit += amount;
    }

    public void updateCooldown(int seconds) {
        cooldownTimestamp = System.currentTimeMillis() + (seconds * 1000L);
    }

    public boolean hasCooldown() {
        return cooldownTimestamp > System.currentTimeMillis();
    }

    public void resetLimit() {
        limit = 0;
        cooldownTimestamp = -1;
    }
}
