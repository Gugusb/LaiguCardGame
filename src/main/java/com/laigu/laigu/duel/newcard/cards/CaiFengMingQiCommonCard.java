package com.laigu.laigu.duel.newcard.cards;

import com.laigu.laigu.duel.CardClass;
import com.laigu.laigu.duel.newcard.ArtifactCardNames;
import com.laigu.laigu.duel.newcard.CardRarity;
import com.laigu.laigu.duel.newcard.DuelCard;
import com.laigu.laigu.duel.newcard.OnSettlement;
import com.laigu.laigu.duel.newcard.SettlementContext;

/** 守望·彩缝鸣器（普通）：上轮也在场上 → +12 额外分。 */
public final class CaiFengMingQiCommonCard implements DuelCard, OnSettlement
{
    @Override public String id() { return "cai_feng_ming_qi_common"; }
    @Override public String displayName() { return ArtifactCardNames.variantName("cai_feng_ming_qi", rarity()); }
    @Override public CardClass cardClass() { return CardClass.SHOU; }
    @Override public CardRarity rarity() { return CardRarity.COMMON; }

    @Override public void onSettlement(SettlementContext context)
    {
        if (context.lastedLastRound()) context.addExtraScaled(veteranBonus());
    }

    int veteranBonus() { return 12; }
}
