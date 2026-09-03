package com.laigu.laigu.duel.newcard;

import com.laigu.laigu.util.CardNbt;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/** 将真实卡牌 ItemStack 映射为新架构独立 DuelCard；不改变旧 DuelGame 状态。 */
public final class CardItemAdapter
{
    private CardItemAdapter()
    {
    }

    public static Optional<DuelCard> create(ItemStack stack)
    {
        return inspect(stack).cardOptional();
    }

    public static CardMappingResult inspect(ItemStack stack)
    {
        if (stack == null || stack.isEmpty())
            return new CardMappingResult(CardMappingResult.Status.EMPTY, "", "", null);
        CardRegistry.initialize();
        String path = CardNbt.pathOf(stack);
        String rarity = CardNbt.rarityOfPath(path);
        if (rarity == null)
            return new CardMappingResult(CardMappingResult.Status.NOT_CARD, path, "", null);
        String cardId = CardNbt.stripRaritySuffix(path) + "_" + rarity;
        if (!CardFactory.contains(cardId))
            return new CardMappingResult(CardMappingResult.Status.UNMIGRATED, path, cardId, null);
        return new CardMappingResult(CardMappingResult.Status.MAPPED, path, cardId, CardFactory.create(cardId));
    }
}
