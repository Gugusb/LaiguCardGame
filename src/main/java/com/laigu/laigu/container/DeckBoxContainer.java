package com.laigu.laigu.container;

import com.laigu.laigu.item.CardItem;
import com.laigu.laigu.item.DeckBoxItem;
import com.laigu.laigu.util.CardNbt;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 卡组匣存储容器（16 格）。数据写回卡组匣物品 NBT，无需方块实体。
 * <ul>
 *   <li>只允许放入来古牌（{@link CardItem}），放置时不做合法性拦截（重复/无效果卡也能放入）。</li>
 *   <li>合法性由「确认构建」统一检查（见 {@link com.laigu.laigu.client.DeckBoxScreen}）：重卡/无效卡标红。</li>
 *   <li>只有确认构建成功（合法）的卡组才能参与对战（NBT 标记 {@link #TAG_BUILT}）；任何内容改动会使构建状态失效。</li>
 *   <li>卡牌不可堆叠：每格强制单张。</li>
 * </ul>
 */
public class DeckBoxContainer extends SimpleContainer
{
    public static final int SLOT_COUNT = 16;
    public static final String TAG_ITEMS = "laigu.deck_items";
    /** 已确认构建（卡组合法、可参与对战）标记。 */
    public static final String TAG_BUILT = "laigu.deck_built";

    private final ItemStack boxStack;
    /** 加载/填充期间不触发「内容变化→清除构建状态」。 */
    private boolean suppressBuiltClear = false;

    public DeckBoxContainer(ItemStack boxStack)
    {
        super(SLOT_COUNT);
        this.boxStack = boxStack;
        load();
    }

    private void load()
    {
        if (boxStack.isEmpty() || boxStack.getTag() == null) return;
        suppressBuiltClear = true;
        try
        {
            ListTag list = boxStack.getTag().getList(TAG_ITEMS, Tag.TAG_COMPOUND);
            for (int i = 0; i < Math.min(list.size(), SLOT_COUNT); i++)
            {
                ItemStack stack = ItemStack.of(list.getCompound(i));
                if (stack.getItem() instanceof CardItem)
                {
                    stack.setCount(1); // 卡牌不可堆叠：旧数据强制单张
                    setItem(i, stack);
                }
            }
        }
        finally
        {
            suppressBuiltClear = false;
        }
    }

    /** 从另一个卡组包 NBT 只读填充内容（登记预览用，不写回）。 */
    public void loadFrom(ItemStack src)
    {
        suppressBuiltClear = true;
        try
        {
            clearContent();
            if (src.isEmpty() || src.getTag() == null) return;
            ListTag list = src.getTag().getList(TAG_ITEMS, Tag.TAG_COMPOUND);
            for (int i = 0; i < Math.min(list.size(), SLOT_COUNT); i++)
            {
                ItemStack stack = ItemStack.of(list.getCompound(i));
                if (stack.getItem() instanceof CardItem)
                {
                    stack.setCount(1); // 卡牌不可堆叠：旧数据强制单张
                    setItem(i, stack);
                }
            }
        }
        finally
        {
            suppressBuiltClear = false;
        }
    }

    @Override
    public void setItem(int index, ItemStack stack)
    {
        // 内容实际变化（放/取/替换）→ 构建状态失效；加载/填充阶段除外
        if (!suppressBuiltClear && !ItemStack.isSameItemSameTags(getItem(index), stack))
        {
            setBuilt(boxStack, false);
        }
        super.setItem(index, stack);
    }

    @Override
    public boolean canPlaceItem(int index, ItemStack stack)
    {
        return stack.getItem() instanceof CardItem;
    }

    /** 唯一性判据：完整物品 id（含稀有度后缀，普通/金质视为不同卡）。 */
    public static String itemKeyOf(ItemStack stack)
    {
        return CardNbt.pathOf(stack);
    }

    /** 卡组包是否已确认构建（可参与对战）。创造卡组无视规则，恒视为已构建。 */
    public static boolean isBuilt(ItemStack boxStack)
    {
        if (boxStack == null || boxStack.isEmpty()) return false;
        if (boxStack.getItem() instanceof DeckBoxItem di && di.creative) return true;
        return boxStack.getTag() != null && boxStack.getTag().getBoolean(TAG_BUILT);
    }

    /** 写入/清除卡组包的构建标记（直接改物品 NBT，无需保存）。 */
    public static void setBuilt(ItemStack boxStack, boolean built)
    {
        if (boxStack == null || boxStack.isEmpty()) return;
        if (built)
        {
            boxStack.getOrCreateTag().putBoolean(TAG_BUILT, true);
        }
        else
        {
            boxStack.getOrCreateTag().remove(TAG_BUILT);
        }
    }

    @Override
    public void setChanged()
    {
        super.setChanged();
        save();
    }

    public void save()
    {
        if (boxStack.isEmpty()) return;
        ListTag list = new ListTag();
        for (int i = 0; i < SLOT_COUNT; i++)
        {
            list.add(getItem(i).save(new CompoundTag()));
        }
        boxStack.getOrCreateTag().put(TAG_ITEMS, list);
    }

    /** 当前容器内容（卡组槽位非空卡副本），供「确认构建」校验。 */
    public List<ItemStack> snapshot()
    {
        List<ItemStack> out = new ArrayList<>();
        for (int i = 0; i < SLOT_COUNT; i++)
        {
            if (!getItem(i).isEmpty()) out.add(getItem(i).copy());
        }
        return out;
    }

    /** 登记对战时读取卡组：返回卡组匣里的卡（空位跳过）。 */
    public static List<ItemStack> readDeck(ItemStack boxStack)
    {
        List<ItemStack> deck = new ArrayList<>();
        if (boxStack.isEmpty() || boxStack.getTag() == null) return deck;
        ListTag list = boxStack.getTag().getList(TAG_ITEMS, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++)
        {
            ItemStack stack = ItemStack.of(list.getCompound(i));
            if (!stack.isEmpty() && stack.getItem() instanceof CardItem)
            {
                deck.add(stack);
            }
        }
        return deck;
    }
}
