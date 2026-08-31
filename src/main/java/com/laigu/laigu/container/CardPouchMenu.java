package com.laigu.laigu.container;

import com.laigu.laigu.item.CardPouchItem;
import com.laigu.laigu.registry.ModMenuTypes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * 卡袋菜单：27 格（3×9，等同木桶）卡袋区 + 玩家背包。
 * <p>
 * 卡袋内容持久化在卡袋物品 NBT（见 {@link CardPouchContainer}）。
 * 客户端创建菜单时没有物品栈，槽位内容由服务端同步。
 */
public class CardPouchMenu extends AbstractContainerMenu
{
    private final CardPouchContainer container;

    public CardPouchMenu(int id, Inventory playerInventory)
    {
        this(id, playerInventory, null);
    }

    public CardPouchMenu(int id, Inventory playerInventory, @Nullable ItemStack pouchStack)
    {
        super(ModMenuTypes.CARD_POUCH.get(), id);
        this.container = new CardPouchContainer(pouchStack != null ? pouchStack : ItemStack.EMPTY);

        // 卡袋 3×9（等同木桶布局：物品区 3 行，玩家背包在其下）
        for (int row = 0; row < 3; row++)
        {
            for (int col = 0; col < 9; col++)
            {
                addSlot(new CardOnlySlot(container, col + row * 9, 8 + col * 18, 18 + row * 18));
            }
        }

        // 玩家背包 27 + 快捷栏 9（与 vanilla 木桶槽位一致：85/103/121 + 143，
        // 落在 generic_54 下方玩家背包槽格（贴图 139/157/175/197 的内部）上）
        for (int row = 0; row < 3; row++)
        {
            for (int col = 0; col < 9; col++)
            {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 85 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++)
        {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, 143));
        }
    }

    /** 卡袋行数（3 行，等同木桶）；客户端渲染背景时按行数裁剪贴图（见 CardPouchScreen.renderBg）。 */
    public int getRows()
    {
        return 3;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index)
    {
        ItemStack carried = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem())
        {
            ItemStack stack = slot.getItem();
            carried = stack.copy();
            if (index < CardPouchContainer.SLOT_COUNT)
            {
                // 卡袋 → 玩家背包
                if (!this.moveItemStackTo(stack, CardPouchContainer.SLOT_COUNT, this.slots.size(), true))
                {
                    return ItemStack.EMPTY;
                }
            }
            else
            {
                // 玩家背包 → 卡袋（非卡牌会被 Slot.mayPlace 拒绝）
                if (!this.moveItemStackTo(stack, 0, CardPouchContainer.SLOT_COUNT, false))
                {
                    return ItemStack.EMPTY;
                }
            }

            if (stack.isEmpty())
            {
                slot.set(ItemStack.EMPTY);
            }
            else
            {
                slot.setChanged();
            }
            if (stack.getCount() == carried.getCount())
            {
                return ItemStack.EMPTY;
            }
            slot.onTake(player, stack);
        }
        return carried;
    }

    @Override
    public boolean stillValid(Player player)
    {
        // 主手或副手持卡袋时菜单保持打开
        return player.getMainHandItem().getItem() instanceof CardPouchItem
                || player.getOffhandItem().getItem() instanceof CardPouchItem;
    }

    @Override
    public void removed(Player player)
    {
        super.removed(player);
        this.container.save();
    }
}
