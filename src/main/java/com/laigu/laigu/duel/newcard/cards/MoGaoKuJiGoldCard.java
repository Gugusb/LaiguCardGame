package com.laigu.laigu.duel.newcard.cards;

import com.laigu.laigu.duel.CardClass;
import com.laigu.laigu.duel.newcard.ArtifactCardNames;
import com.laigu.laigu.duel.newcard.CardRarity;
import com.laigu.laigu.duel.newcard.DicePattern;
import com.laigu.laigu.duel.newcard.DuelCard;
import com.laigu.laigu.duel.newcard.OnSettlement;
import com.laigu.laigu.duel.newcard.SettlementContext;

/** 莫高窟记金质版：若你的骰全小(≤3) → +60 额外分。 */
public final class MoGaoKuJiGoldCard implements DuelCard, OnSettlement
{
    @Override public String id() { return "mo_gao_ku_ji_gold"; }
    @Override public String displayName() { return ArtifactCardNames.variantName("mo_gao_ku_ji", rarity()); }
    @Override public CardClass cardClass() { return CardClass.SHOU; }
    @Override public CardRarity rarity() { return CardRarity.GOLD; }

    @Override public void onSettlement(SettlementContext context)
    {
        if (DicePattern.allLow(context.selfDice())) context.addExtra(allLowBonus());
    }

    int allLowBonus() { return 60; }
}
