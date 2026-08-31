package com.laigu.laigu.container;

import com.laigu.laigu.item.CardItem;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 只允许放来古牌（{@link CardItem}）的槽位。用于卡袋/卡组匣的卡槽，
 * 在容器层 {@code canPlaceItem} 之外再于槽位层兜底，杜绝非卡物品（卡包/其他物品）混入。
 * 玩家背包槽不用它（背包本就应能放任意物品）。
 */
public class CardOnlySlot extends Slot
{
    public CardOnlySlot(Container container, int index, int x, int y)
    {
        super(container, index, x, y);
    }

    @Override
    public boolean mayPlace(ItemStack stack)
    {
        return stack.getItem() instanceof CardItem;
    }
}
