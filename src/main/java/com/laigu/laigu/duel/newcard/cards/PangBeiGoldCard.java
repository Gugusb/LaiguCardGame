package com.laigu.laigu.duel.newcard.cards;

import com.laigu.laigu.duel.CardClass;
import com.laigu.laigu.duel.newcard.ArtifactCardNames;
import com.laigu.laigu.duel.newcard.CardRarity;
import com.laigu.laigu.duel.newcard.DuelCard;
import com.laigu.laigu.duel.newcard.OnSettlement;
import com.laigu.laigu.duel.newcard.SettlementContext;

/** 偃盆金质版：上轮平局 → +60 额外分。 */
public final class PangBeiGoldCard implements DuelCard, OnSettlement
{
    @Override public String id() { return "pang_bei_gold"; }
    @Override public String displayName() { return ArtifactCardNames.variantName("pang_bei", rarity()); }
    @Override public CardClass cardClass() { return CardClass.SHOU; }
    @Override public CardRarity rarity() { return CardRarity.GOLD; }

    @Override public void onSettlement(SettlementContext context)
    {
        if (PangBeiCommonCard.isDrawLast(context)) context.addExtra(drawBonus());
    }

    int drawBonus() { return 60; }
}
