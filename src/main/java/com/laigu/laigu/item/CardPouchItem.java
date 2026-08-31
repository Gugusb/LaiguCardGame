package com.laigu.laigu.item;

import com.laigu.laigu.container.CardPouchMenu;
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
 * 卡袋：右键打开 6 格存储（仅能放入来古牌）。
 * <p>
 * 内容持久化在物品 NBT，无需方块实体。卡袋还能在副手触发
 * 卡牌羁绊 buff（见 {@code CardSynergy}）。
 */
public class CardPouchItem extends Item
{
    public CardPouchItem(Properties properties)
    {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
    {
        if (!level.isClientSide)
        {
            ItemStack pouchStack = player.getItemInHand(hand);
            player.openMenu(new MenuProvider()
            {
                @Override
                public Component getDisplayName()
                {
                    return Component.translatable("item.laigu.card_pouch");
                }

                @Override
                public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player p)
                {
                    return new CardPouchMenu(containerId, inventory, pouchStack);
                }
            });
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }

    @Override
    public boolean canFitInsideContainerItems()
    {
        // 允许把卡袋放进箱子/潜影盒等容器
        return true;
    }
}
