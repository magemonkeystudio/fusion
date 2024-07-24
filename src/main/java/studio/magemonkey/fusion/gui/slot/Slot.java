/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2024 MageMonkeyStudio
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package studio.magemonkey.fusion.gui.slot;

import lombok.Getter;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryType.SlotType;
import org.bukkit.inventory.ItemStack;
import studio.magemonkey.codex.CodexEngine;


/**
 * Represent slot properties, multiple inventory slots may use this same instance of slot object as it don't
 * represent some slot, but properties of it.
 */
@Getter
public abstract class Slot {
    public static final Slot BASE_CONTAINER_SLOT   = new Slot(SlotType.CONTAINER) {
        @Override
        public ItemStack canHoldItem(ItemStack item) {
            return item;
        }
    };
    public static final Slot BASE_HOTBAR_SLOT      = new Slot(SlotType.QUICKBAR) {
        @Override
        public ItemStack canHoldItem(ItemStack item) {
            return item;
        }
    };
    public static final Slot BASE_CRAFTING_SLOT    = new Slot(SlotType.CRAFTING) {
        @Override
        public ItemStack canHoldItem(ItemStack item) {
            return item;
        }
    };
    public static final Slot SPECIAL_CRAFTING_SLOT = new Slot(SlotType.CRAFTING) {
        @Override
        public ItemStack canHoldItem(ItemStack item) {
            if (item == null || item.getType() == Material.AIR) {
                return null;
            }
            return (CodexEngine.get().getItemManager().getItemTypes(item).isEmpty()) ? null : item;
        }
    };
    public static final Slot BASE_RESULT_SLOT      = new Slot(SlotType.RESULT) {
        @Override
        public ItemStack canHoldItem(ItemStack item) {
            return null;
        }
    };
    public static final Slot BLOCKED_SLOT          = new Slot(SlotType.CONTAINER) {
        @Override
        public ItemStack canHoldItem(ItemStack item) {
            return null;
        }
    };
    public static final Slot QUEUED_SLOT           = new Slot(SlotType.RESULT) {
        @Override
        public ItemStack canHoldItem(ItemStack item) {
            return null;
        }
    };
    public static final Slot BASE_OUTSIDE_SLOT     = new Slot(SlotType.OUTSIDE) {
        @Override
        public ItemStack canHoldItem(ItemStack item) {
            return item;
        }
    };

    /**
     * -- GETTER --
     *  Returns base slot type.
     *
     */
    protected final SlotType slotType;

    public Slot(SlotType slotType) {
        this.slotType = slotType;
    }

    /**
     * Method will check if this item can be inserted to this slot-type,
     * it will return this same item stack if it can be inserted or null
     * if it can't be inserted here. <br>
     * It may also return other itemstack with this same data but other
     * amount if only part of item can be inserted. (returned itemstack is
     * item stack that can be inserted). Like Beacon block, only one item
     * from item stack can be inserted. <br>
     * <br>
     * WARNING: it will not check if there is space in this slot, it only
     * check if this slot can hold this type of item.
     *
     * @param item item stack to check.
     * @return item stack that can be inserted here or null.
     */
    public abstract ItemStack canHoldItem(ItemStack item);

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE).appendSuper(super.toString())
                .append("slotType", this.slotType)
                .toString();
    }
}
