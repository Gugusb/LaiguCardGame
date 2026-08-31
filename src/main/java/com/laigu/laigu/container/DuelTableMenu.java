package com.laigu.laigu.container;

import com.laigu.laigu.registry.ModMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * 对战方块登记菜单：16 格（8×2）卡组区 + 玩家背包。
 * 卡组容器由方块实体持有（一次性草稿，提交后即丢，不消耗本体）。
 * 槽位只收来古牌（见 {@link DeckBoxContainer#canPlaceItem}）。
 * 方块坐标经网络缓冲传给客户端（提交操作包需要定位方块）。
 */
public class DuelTableMenu extends AbstractContainerMenu
{
    private final DeckBoxContainer container;
    public final BlockPos pos;

    /** 客户端构造（槽位内容由服务端同步）。 */
    public DuelTableMenu(int id, Inventory playerInventory)
    {
        this(id, playerInventory, null, BlockPos.ZERO, false);
    }

    /** 是否创造卡组（无视规则）。 */
    public boolean isCreative()
    {
        return creative;
    }

    public DuelTableMenu(int id, Inventory playerInventory, @Nullable DeckBoxContainer container, BlockPos pos)
    {
        this(id, playerInventory, container, pos, false);
    }

    private final boolean creative;

    public DuelTableMenu(int id, Inventory playerInventory, @Nullable DeckBoxContainer container, BlockPos pos, boolean creative)
    {
        super(ModMenuTypes.DUEL_TABLE.get(), id);
        this.container = container != null ? container : new DeckBoxContainer(ItemStack.EMPTY);
        this.pos = pos;
        this.creative = creative;

        // 卡组 8×2（贴左）。卡组来自手持卡组包的预设，槽位只读，不能在登记界面改动。
        int startX = 8;
        for (int row = 0; row < 2; row++)
        {
            for (int col = 0; col < 8; col++)
            {
                addSlot(new Slot(this.container, col + row * 8, startX + col * 18, 18 + row * 18)
                {
                    @Override
                    public boolean mayPlace(ItemStack stack)
                    {
                        return false;
                    }

                    @Override
                    public boolean mayPickup(Player player)
                    {
                        return false;
                    }
                });
            }
        }

        // 玩家背包 27 + 快捷栏 9
        for (int row = 0; row < 3; row++)
        {
            for (int col = 0; col < 9; col++)
            {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++)
        {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
    }

    /** 客户端还原：读方块坐标。 */
    public static DuelTableMenu fromNetwork(int id, Inventory playerInventory, FriendlyByteBuf buf)
    {
        return new DuelTableMenu(id, playerInventory, null, buf.readBlockPos());
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
            if (index < DeckBoxContainer.SLOT_COUNT)
            {
                if (!this.moveItemStackTo(stack, DeckBoxContainer.SLOT_COUNT, this.slots.size(), true))
                {
                    return ItemStack.EMPTY;
                }
            }
            else
            {
                if (!this.moveItemStackTo(stack, 0, DeckBoxContainer.SLOT_COUNT, false))
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
        return true;
    }
}
