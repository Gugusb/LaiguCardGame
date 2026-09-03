package com.laigu.laigu.duel.newcard.cards;

import com.laigu.laigu.duel.CardClass;
import com.laigu.laigu.duel.newcard.ArtifactCardNames;
import com.laigu.laigu.duel.newcard.CardRarity;
import com.laigu.laigu.duel.newcard.DicePatterns;
import com.laigu.laigu.duel.newcard.DuelCard;
import com.laigu.laigu.duel.newcard.OnSettlement;
import com.laigu.laigu.duel.newcard.SettlementContext;

/** 激活左·浑天（金质）：结算时若己方全场牌型为顺子 → 激活右侧卡牌 6 次；焕章：顺子可以间隔1。 */
public final class HunTianYiGoldCard implements DuelCard, OnSettlement
{
    @Override public String id() { return "hun_tian_yi_gold"; }
    @Override public String displayName() { return ArtifactCardNames.variantName("hun_tian_yi", rarity()); }
    @Override public CardClass cardClass() { return CardClass.MOU; }
    @Override public CardRarity rarity() { return CardRarity.GOLD; }

    @Override public void onSettlement(SettlementContext context)
    {
        if (!DicePatterns.isStraight(context.ownFieldDice())
                && !DicePatterns.isStraightWithOneGap(context.ownFieldDice())) return;
        context.activateRightCard(rightActivations());
    }

    int rightActivations() { return 6; }
}
