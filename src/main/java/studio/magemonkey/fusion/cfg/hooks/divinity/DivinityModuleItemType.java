package studio.magemonkey.fusion.cfg.hooks.divinity;

import org.bukkit.inventory.ItemStack;
import studio.magemonkey.codex.api.items.ItemType;
import studio.magemonkey.divinity.modules.ModuleItem;
import studio.magemonkey.divinity.utils.DivinityProvider;
import studio.magemonkey.divinity.utils.LoreUT;

/**
 * ItemType wrapper for DIV_ module-prefixed items (ITEMGEN, GEM, ESSENCE, RUNE, EXTRACTOR,
 * ARROW, CONSUMABLE, CUSTOM, FORTIFY, IDENTIFY, DUST, REPAIR, DISMANTLE, REFINE).
 * Extends DivinityItemType to support optional enchantment stripping (noEnch) for generator items.
 */
public class DivinityModuleItemType extends DivinityProvider.DivinityItemType {

    private final boolean noEnch;

    public DivinityModuleItemType(ModuleItem moduleItem, int level, ItemType material, boolean noEnch) {
        super(moduleItem, level, material);
        this.noEnch = noEnch;
    }

    @Override
    public ItemStack create() {
        ItemStack item = super.create();
        if (noEnch) {
            LoreUT.removeEnchants(item);
        }
        return item;
    }
}
