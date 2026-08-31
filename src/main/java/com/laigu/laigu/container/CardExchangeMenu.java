package com.laigu.laigu.container;

import com.laigu.laigu.block.CardExchangeTableBlockEntity;
import com.laigu.laigu.item.CardItem;
import com.laigu.laigu.registry.ModMenuTypes;
import com.laigu.laigu.util.CardNbt;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * 卡牌交换台菜单：A/B 两侧卡牌槽 + 玩家背包。
 * <p>
 * 防偷规则（核心，服务端同样生效）：卡牌槽只允许放入「自己这一侧 + 属于自己」的卡牌
 * （{@link ExchangeCardSlot#mayPlace}），且只能取走自己这一侧的物品
 * （{@link ExchangeCardSlot#mayPickup}），杜绝拿走对面卡牌。
 * 「确认交换」按钮经 {@link #clickMenuButton} 走原版按钮协议触发服务端
 * {@code table.confirm}。
 */
public class CardExchangeMenu extends AbstractContainerMenu
{
    public static final int CARD_SLOTS = 2;
    public static final int CONFIRM_BUTTON_ID = 0;

    private final CardExchangeTableBlockEntity table;
    private final Player player;
    private final ContainerData data;
    /** 本菜单玩家占的侧位（0=A / 1=B），服务端打开时算好、客户端经网络缓冲拿到，避免依赖未同步的客户端 BE 占位。 */
    private final int playerSide;

    public CardExchangeMenu(int id, Inventory playerInventory, CardExchangeTableBlockEntity table, int side)
    {
        super(ModMenuTypes.CARD_EXCHANGE.get(), id);
        this.table = table;
        this.player = playerInventory.player;
        this.playerSide = side;
        this.data = table.getData();

        // A/B 两侧卡牌槽（防偷逻辑见 ExchangeCardSlot）
        addSlot(new ExchangeCardSlot(table, CardExchangeTableBlockEntity.SLOT_A, 62, 26));
        addSlot(new ExchangeCardSlot(table, CardExchangeTableBlockEntity.SLOT_B, 96, 26));

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

        addDataSlots(data);
    }

    /** 客户端网络构造：从缓冲读取方块坐标 + 服务端算好的侧位，还原菜单。 */
    public static CardExchangeMenu fromNetwork(int id, Inventory inv, FriendlyByteBuf buf)
    {
        BlockPos pos = buf.readBlockPos();
        int side = buf.readInt();
        BlockEntity be = inv.player.level().getBlockEntity(pos);
        if (be instanceof CardExchangeTableBlockEntity table)
        {
            return new CardExchangeMenu(id, inv, table, side);
        }
        throw new IllegalStateException("Card exchange table not found at " + pos);
    }

    public CardExchangeTableBlockEntity getTable()
    {
        return table;
    }

    public ContainerData getData()
    {
        return data;
    }

    /** 该玩家占的是哪一侧（0=A / 1=B）；未占则 -1。 */
    public int getPlayerSide()
    {
        return playerSide;
    }

    @Override
    public boolean clickMenuButton(Player player, int id)
    {
        if (id == CONFIRM_BUTTON_ID)
        {
            table.confirm(player);
            return true;
        }
        return false;
    }

    @Override
    public boolean stillValid(Player player)
    {
        if (table.getLevel() == null || table.getLevel().getBlockEntity(table.getBlockPos()) != table)
        {
            return false;
        }
        BlockPos pos = table.getBlockPos();
        return player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
    }

    @Override
    public void removed(Player player)
    {
        super.removed(player);
        // 关闭菜单：若该玩家一侧没有卡牌且未交换，则释放占位，避免空占台子
        if (table.getLevel() != null && !table.getLevel().isClientSide)
        {
            int side = getPlayerSide();
            if (side >= 0)
            {
                table.releaseSide(side);
            }
        }
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
            if (index < CARD_SLOTS)
            {
                // 卡牌槽 → 玩家背包（防偷：shift 拿取同样只能拿自己那一侧）
                if (!slot.mayPickup(player))
                {
                    return ItemStack.EMPTY;
                }
                if (!this.moveItemStackTo(stack, CARD_SLOTS, this.slots.size(), true))
                {
                    return ItemStack.EMPTY;
                }
            }
            else
            {
                // 玩家背包 → 卡牌槽（ExchangeCardSlot.mayPlace 会拒绝不符合规则的）
                if (!this.moveItemStackTo(stack, 0, CARD_SLOTS, false))
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

    /**
     * 卡牌交换槽位（A/B 两侧）。
     * <p>
     * 防偷核心：只能放入「自己这一侧 + 属于自己」的卡牌；
     * 只能取走自己这一侧的物品，杜绝拿走对面卡牌。
     */
    private class ExchangeCardSlot extends Slot
    {
        private final int side;

        ExchangeCardSlot(CardExchangeTableBlockEntity table, int index, int x, int y)
        {
            super(table.getContainer(), index, x, y);
            this.side = index;
        }

        @Override
        public boolean mayPlace(ItemStack stack)
        {
            if (!(stack.getItem() instanceof CardItem))
            {
                return false;
            }
            // 已交换完成后不允许再放
            if (table.getData().get(CardExchangeTableBlockEntity.DATA_SWAPPED) == 1)
            {
                return false;
            }
            // 只能放入自己那一侧
            if (!table.isSideOwner(side, player))
            {
                return false;
            }
            // 只能放属于自己的卡牌（未署名/别人的卡牌进不来）
            return CardNbt.isOwnedBy(stack, player);
        }

        @Override
        public boolean mayPickup(Player player)
        {
            // 只能取走自己那一侧的物品（防偷对面）
            return table.isSideOwner(side, player);
        }

        @Override
        public void setChanged()
        {
            super.setChanged();
            // 槽位变化：取走卡牌自动取消该侧确认，随后尝试交换/重置
            table.onSlotChanged(side);
        }
    }
}
