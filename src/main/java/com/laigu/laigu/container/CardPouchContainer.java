package com.laigu.laigu.container;

import com.laigu.laigu.item.CardItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;

/**
 * 卡袋存储容器（27 格，容量等同木桶）。
 * <p>
 * 数据直接写回所绑定卡袋物品的 NBT（TAG_ITEMS），因此无需方块实体。
 * 只允许放入来古牌（{@link CardItem}）——UI 与 load/save 双层校验。
 */
public class CardPouchContainer extends SimpleContainer
{
    /** 卡袋容量：与木桶相同的 27 格（3×9） */
    public static final int SLOT_COUNT = 27;
    public static final String TAG_ITEMS = "laigu.pouch_items";

    /** 绑定的卡袋物品（客户端可能为 EMPTY） */
    private final ItemStack pouchStack;

    public CardPouchContainer(ItemStack pouchStack)
    {
        super(SLOT_COUNT);
        this.pouchStack = pouchStack;
        load();
    }

    private void load()
    {
        if (pouchStack.isEmpty() || pouchStack.getTag() == null)
        {
            return;
        }
        ListTag list = pouchStack.getTag().getList(TAG_ITEMS, Tag.TAG_COMPOUND);
        for (int i = 0; i < Math.min(list.size(), SLOT_COUNT); i++)
        {
            ItemStack stack = ItemStack.of(list.getCompound(i));
            if (stack.getItem() instanceof CardItem)
            {
                setItem(i, stack);
            }
        }
    }

    /** 只允许来古牌（槽位点击/移动时由 {@code Slot#mayPlace} 调用） */
    @Override
    public boolean canPlaceItem(int index, ItemStack stack)
    {
        return stack.getItem() instanceof CardItem;
    }

    @Override
    public void setChanged()
    {
        super.setChanged();
        save();
    }

    /** 把 6 格内容写回卡袋物品 NBT。 */
    public void save()
    {
        if (pouchStack.isEmpty())
        {
            return;
        }
        ListTag list = new ListTag();
        for (int i = 0; i < SLOT_COUNT; i++)
        {
            list.add(getItem(i).save(new CompoundTag()));
        }
        pouchStack.getOrCreateTag().put(TAG_ITEMS, list);
    }
}
