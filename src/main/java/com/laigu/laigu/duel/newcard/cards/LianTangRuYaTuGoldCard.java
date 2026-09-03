package com.laigu.laigu.duel.newcard.cards;

import com.laigu.laigu.duel.CardClass;
import com.laigu.laigu.duel.newcard.ArtifactCardNames;
import com.laigu.laigu.duel.newcard.CardContext;
import com.laigu.laigu.duel.newcard.CardRarity;
import com.laigu.laigu.duel.newcard.DuelCard;
import com.laigu.laigu.duel.newcard.OnRoundEnd;

/** 连环·连堂儒雅图（金质）：结算后若本轮你输抽 3 张牌（清单口径）。 */
public final class LianTangRuYaTuGoldCard implements DuelCard, OnRoundEnd
{
    @Override public String id() { return "lian_tang_ru_ya_tu_gold"; }
    @Override public String displayName() { return ArtifactCardNames.variantName("lian_tang_ru_ya_tu", rarity()); }
    @Override public CardClass cardClass() { return CardClass.MOU; }
    @Override public CardRarity rarity() { return CardRarity.GOLD; }

    @Override public void onRoundEnd(CardContext context)
    {
        if (context.winnerLast() == 1 - context.side()) context.drawCards(loseDraw());
    }

    int loseDraw() { return 3; }
}
