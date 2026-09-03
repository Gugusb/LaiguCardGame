package com.laigu.laigu.duel.newcard.cards;

import com.laigu.laigu.duel.CardClass;
import com.laigu.laigu.duel.newcard.ArtifactCardNames;
import com.laigu.laigu.duel.newcard.CardRarity;
import com.laigu.laigu.duel.newcard.DuelCard;
import com.laigu.laigu.duel.newcard.OnSettlement;
import com.laigu.laigu.duel.newcard.SettlementContext;

/** 毛公鼎普通版：轮次 ≥2 → +12 额外分。 */
public final class MaoGongDingCommonCard implements DuelCard, OnSettlement
{
    @Override public String id() { return "mao_gong_ding_common"; }
    @Override public String displayName() { return ArtifactCardNames.variantName("mao_gong_ding", rarity()); }
    @Override public CardClass cardClass() { return CardClass.SHOU; }
    @Override public CardRarity rarity() { return CardRarity.COMMON; }

    @Override public void onSettlement(SettlementContext context)
    {
        if (context.round() >= roundThreshold()) context.addExtra(lateBonus());
    }

    int roundThreshold() { return 2; }

    int lateBonus() { return 12; }
}
