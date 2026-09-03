package com.laigu.laigu.duel.newcard.cards;

import com.laigu.laigu.duel.CardClass;
import com.laigu.laigu.duel.newcard.ArtifactCardNames;
import com.laigu.laigu.duel.newcard.CardRarity;
import com.laigu.laigu.duel.newcard.DuelCard;
import com.laigu.laigu.duel.newcard.OnSettlement;
import com.laigu.laigu.duel.newcard.SettlementContext;

/** 卫锁嘉据普通版：上轮你赢 → +1 倍率。 */
public final class WeiSuoJiaJuCommonCard implements DuelCard, OnSettlement
{
    @Override public String id() { return "wei_suo_jia_ju_common"; }
    @Override public String displayName() { return ArtifactCardNames.variantName("wei_suo_jia_ju", rarity()); }
    @Override public CardClass cardClass() { return CardClass.SHOU; }
    @Override public CardRarity rarity() { return CardRarity.COMMON; }

    @Override public void onSettlement(SettlementContext context)
    {
        if (context.winnerLast() == context.side()) context.addMultiplier(winBonus());
    }

    int winBonus() { return 1; }
}
