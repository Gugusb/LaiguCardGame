package com.laigu.laigu.duel.newcard.cards;

import com.laigu.laigu.duel.CardClass;
import com.laigu.laigu.duel.newcard.ArtifactCardNames;
import com.laigu.laigu.duel.newcard.CardRarity;
import com.laigu.laigu.duel.newcard.DicePatterns;
import com.laigu.laigu.duel.newcard.DuelCard;
import com.laigu.laigu.duel.newcard.OnSettlement;
import com.laigu.laigu.duel.newcard.SettlementContext;

/** 激活左·浑天（普通）：结算时若己方全场牌型为顺子 → 激活右侧卡牌 3 次。 */
public final class HunTianYiCommonCard implements DuelCard, OnSettlement
{
    @Override public String id() { return "hun_tian_yi_common"; }
    @Override public String displayName() { return ArtifactCardNames.variantName("hun_tian_yi", rarity()); }
    @Override public CardClass cardClass() { return CardClass.MOU; }
    @Override public CardRarity rarity() { return CardRarity.COMMON; }

    @Override public void onSettlement(SettlementContext context)
    {
        if (!DicePatterns.isStraight(context.ownFieldDice())) return;
        context.activateRightCard(rightActivations());
    }

    int rightActivations() { return 3; }
}
