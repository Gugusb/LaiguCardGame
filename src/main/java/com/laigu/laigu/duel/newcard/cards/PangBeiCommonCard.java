package com.laigu.laigu.duel.newcard.cards;

import com.laigu.laigu.duel.CardClass;
import com.laigu.laigu.duel.newcard.ArtifactCardNames;
import com.laigu.laigu.duel.newcard.CardRarity;
import com.laigu.laigu.duel.newcard.DuelCard;
import com.laigu.laigu.duel.newcard.OnSettlement;
import com.laigu.laigu.duel.newcard.SettlementContext;

/** 偃盆普通版：上轮平局 → +30 额外分。 */
public final class PangBeiCommonCard implements DuelCard, OnSettlement
{
    @Override public String id() { return "pang_bei_common"; }
    @Override public String displayName() { return ArtifactCardNames.variantName("pang_bei", rarity()); }
    @Override public CardClass cardClass() { return CardClass.SHOU; }
    @Override public CardRarity rarity() { return CardRarity.COMMON; }

    @Override public void onSettlement(SettlementContext context)
    {
        if (isDrawLast(context)) context.addExtra(drawBonus());
    }

    static boolean isDrawLast(SettlementContext context)
    {
        return context.round() > 1 && context.winnerLast() == -1;
    }

    int drawBonus() { return 30; }
}
