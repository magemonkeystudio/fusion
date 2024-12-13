package studio.magemonkey.fusion.cfg.hooks.divinity;

import lombok.Getter;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import studio.magemonkey.codex.api.items.ItemType;
import studio.magemonkey.fusion.cfg.hooks.ItemGenEntry;
import studio.magemonkey.fusion.util.ChatUT;

@Getter
public class DivinityRecipeMeta {

    private final String       originalRecipeName;
    private final ItemGenEntry entry;
    private final int          level;
    private final int          amount;
    private final ItemType     type;

    private final String    itemName;
    private       ItemStack icon;

    public DivinityRecipeMeta(String originalRecipeName,
                              ItemGenEntry entry,
                              int level,
                              int amount,
                              ItemType type,
                              String itemName) {
        this.originalRecipeName = originalRecipeName;
        this.entry = entry;
        this.level = level;
        this.amount = amount;
        this.type = type;
        this.itemName = itemName;
    }

    public ItemStack generateItem() {
        ItemStack item = entry.getReference().create(level, -1, type);
        ItemMeta  meta = item.getItemMeta();
        meta.setDisplayName(ChatUT.hexString(itemName));
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack getIcon() {
        if (icon == null) {
            icon = DivinityService.divinityCfg.getRecipeIcon(entry, itemName, type, level);
        }
        return icon;
    }
}
