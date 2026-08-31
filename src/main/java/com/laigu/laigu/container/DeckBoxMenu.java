package com.laigu.laigu.container;

import com.laigu.laigu.duel.DuelGame;
import com.laigu.laigu.item.DeckBoxItem;
import com.laigu.laigu.registry.ModMenuTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 卡组匣菜单：16 格（8×2）卡组区 + 玩家背包。
 * 「确认构建」走原生存器按钮协议（客户端 handleInventoryButtonClick → 服务端 {@link #clickMenuButton}），
 * 合法则给卡组包物品落「已构建」标记；内容被改动时标记自动清除。
 */
public class DeckBoxMenu extends AbstractContainerMenu
{
    private final DeckBoxContainer container;
    private final ItemStack boxStack;

    /** 客户端构造（槽位内容由服务端同步）。 */
    public DeckBoxMenu(int id, Inventory playerInventory)
    {
        this(id, playerInventory, null);
    }

    public DeckBoxMenu(int id, Inventory playerInventory, @Nullable ItemStack boxStack)
    {
        super(ModMenuTypes.DECK_BOX.get(), id);
        this.boxStack = boxStack != null ? boxStack : ItemStack.EMPTY;
        this.container = new DeckBoxContainer(this.boxStack);
        // 打开卡组界面 → 「合法tag」归 false，需重新「确认构建」才可登记（创造卡组不受此限）
        if (!this.boxStack.isEmpty()) DeckBoxContainer.setBuilt(this.boxStack, false);

        // 卡组 8×2（贴左）
        int startX = 8;
        for (int row = 0; row < 2; row++)
        {
            for (int col = 0; col < 8; col++)
            {
                addSlot(new CardOnlySlot(container, col + row * 8, startX + col * 18, 18 + row * 18));
            }
        }

        // 玩家背包 27 + 快捷栏 9
        // 画generic_54 两段 blit 后，玩家背包槽顶边框在屏幕 y = 顶高(53)+14=67（见 DeckBoxScreen.renderBg），
        // 所以玩家背包槽 y 67/85/103、快捷栏 125（列 8 + col*18，9 列对齐原版 176 宽背景）。
        for (int row = 0; row < 3; row++)
        {
            for (int col = 0; col < 9; col++)
            {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 67 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++)
        {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, 125));
        }
    }

    /** 卡组槽行数（2 行）；客户端渲染背景时按行数裁剪贴图（见 DeckBoxScreen.renderBg）。 */
    public int getRows()
    {
        return 2;
    }

    /** 是否创造卡组（无视规则，可重复/不足16/全金卡，金卡可直接放并触发焕章）。 */
    public boolean isCreative()
    {
        return boxStack.getItem() instanceof DeckBoxItem di && di.creative;
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
        return player.getMainHandItem().getItem() instanceof DeckBoxItem
                || player.getOffhandItem().getItem() instanceof DeckBoxItem;
    }

    /** 「确认构建」：客户端点按钮 → 原生存器按钮点击包 → 此处执行。合法则给卡组包落「已构建」标记。 */
    @Override
    public boolean clickMenuButton(Player player, int id)
    {
        if (id == 0)
        {
            List<ItemStack> deck = container.snapshot();
            if (isCreative() || DuelGame.isDeckLegal(deck))
            {
                DeckBoxContainer.setBuilt(boxStack, true);
                player.displayClientMessage(Component.translatable(isCreative()
                        ? "message.laigu.creative_built_ok" : "message.laigu.deck_built_ok"), true);
            }
            else
            {
                player.displayClientMessage(Component.translatable("message.laigu.deck_built_fail"), true);
            }
            return true;
        }
        return super.clickMenuButton(player, id);
    }

    @Override
    public void removed(Player player)
    {
        super.removed(player);
        this.container.save();
    }
}
