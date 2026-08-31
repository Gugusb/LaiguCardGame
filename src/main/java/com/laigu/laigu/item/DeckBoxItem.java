package com.laigu.laigu.item;

import com.laigu.laigu.container.DeckBoxMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 卡组匣：右键打开 16 格卡组编辑（只收对战卡）。
 * 内容持久化在物品 NBT；手持满 16 张的卡组匣右键对战方块即可登记对局。
 */
public class DeckBoxItem extends Item
{
    /** 创造卡组：无视规则（可重复/不足16张/全金卡/金卡可直接放置仍触发焕章）。 */
    public final boolean creative;

    public DeckBoxItem(Properties properties)
    {
        this(properties, false);
    }

    public DeckBoxItem(Properties properties, boolean creative)
    {
        super(properties);
        this.creative = creative;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
    {
        if (!level.isClientSide)
        {
            ItemStack boxStack = player.getItemInHand(hand);
            player.openMenu(new MenuProvider()
            {
                @Override
                public Component getDisplayName()
                {
                    return Component.translatable("item.laigu.deck_box");
                }

                @Override
                public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player p)
                {
                    return new DeckBoxMenu(containerId, inventory, boxStack);
                }
            });
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }

    @Override
    public boolean canFitInsideContainerItems()
    {
        return true;
    }
}
