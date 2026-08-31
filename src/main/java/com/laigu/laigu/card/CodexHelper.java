package com.laigu.laigu.card;

import com.laigu.laigu.capability.IPlayerCodex;
import com.laigu.laigu.capability.ModCapabilities;
import com.laigu.laigu.item.CardItem;
import com.laigu.laigu.util.CardNbt;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * 图鉴解锁逻辑（服务端）。
 * <p>
 * 规则：玩家<b>首次</b>获得一张<b>属于自己</b>的卡牌时解锁对应图鉴条目。
 * 遍历背包与副手，凡 owner 为该玩家的卡牌都会触发首次解锁判定；
 * 非本人卡牌（从箱子摸到、捡到等未署名的）不计入图鉴。
 * <p>
 * 触发点：开包（{@code CardPackItem}）与交换台交换完成后调用。
 */
public final class CodexHelper
{
    private CodexHelper()
    {
    }

    /** 扫描玩家背包（含副手），解锁其名下卡牌的图鉴条目（须服务端调用）。 */
    public static void scanAndUnlock(ServerPlayer player)
    {
        if (player == null)
        {
            return;
        }
        for (ItemStack stack : player.getInventory().items)
        {
            unlockIfOwn(player, stack);
        }
        unlockIfOwn(player, player.getOffhandItem());
    }

    private static void unlockIfOwn(ServerPlayer player, ItemStack stack)
    {
        if (stack.isEmpty() || !(stack.getItem() instanceof CardItem))
        {
            return;
        }
        // 必须是玩家本人的卡牌（开包产出才会署名）才计入图鉴
        if (!CardNbt.isOwnedBy(stack, player))
        {
            return;
        }
        String cardId = CardNbt.stripRaritySuffix(CardNbt.pathOf(stack));
        int index = CardCatalog.CARD_IDS.indexOf(cardId);
        if (index < 0)
        {
            return;
        }
        IPlayerCodex codex = ModCapabilities.of(player);
        if (codex == null)
        {
            return;
        }
        // 首次获得才解锁并提示
        if (codex.unlock(index))
        {
            player.displayClientMessage(Component.translatable("message.laigu.codex_unlock",
                    Component.translatable("item.laigu." + cardId + "_common")), true);
        }
    }
}
