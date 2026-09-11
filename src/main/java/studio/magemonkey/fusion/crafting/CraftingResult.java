package studio.magemonkey.fusion.crafting;

public enum CraftingResult {
    SUCCESS, NOT_FOUND, NOT_READY, REQUIREMENTS_NOT_MET, EVENT_CANCELLED, STORAGE_FAILED;

    public void report(org.bukkit.entity.Player player) {
        if (this == SUCCESS) return;
        String message = switch (this) {
            case NOT_FOUND -> "That craft is no longer available. Please refresh the menu.";
            case NOT_READY -> "That craft is not finished yet.";
            case REQUIREMENTS_NOT_MET -> "You do not meet the requirements for this craft.";
            case EVENT_CANCELLED -> "This crafting action was cancelled by a server plugin.";
            case STORAGE_FAILED -> "Crafting storage is unavailable. The action could not complete; please try again.";
            default -> "Unable to complete crafting.";
        };
        player.sendMessage(org.bukkit.ChatColor.RED + message);
    }
}
