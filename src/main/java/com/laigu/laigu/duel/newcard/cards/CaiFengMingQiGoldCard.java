package com.laigu.laigu.duel.newcard.cards;

import com.laigu.laigu.duel.CardClass;
import com.laigu.laigu.duel.newcard.ArtifactCardNames;
import com.laigu.laigu.duel.newcard.CardRarity;
import com.laigu.laigu.duel.newcard.DuelCard;
import com.laigu.laigu.duel.newcard.OnSettlement;
import com.laigu.laigu.duel.newcard.SettlementContext;

/** 守望·彩缝鸣器（金质）：上轮也在场上 → +24 额外分（分值类金卡×2）。 */
public final class CaiFengMingQiGoldCard implements DuelCard, OnSettlement
{
    @Override public String id() { return "cai_feng_ming_qi_gold"; }
    @Override public String displayName() { return ArtifactCardNames.variantName("cai_feng_ming_qi", rarity()); }
    @Override public CardClass cardClass() { return CardClass.SHOU; }
    @Override public CardRarity rarity() { return CardRarity.GOLD; }

    @Override public void onSettlement(SettlementContext context)
    {
        if (context.lastedLastRound()) context.addExtraScaled(24);
    }
}
