package com.laigu.laigu.duel.newcard.cards;

import com.laigu.laigu.duel.CardClass;
import com.laigu.laigu.duel.newcard.ArtifactCardNames;
import com.laigu.laigu.duel.newcard.CardRarity;
import com.laigu.laigu.duel.newcard.DuelCard;
import com.laigu.laigu.duel.newcard.OnSettlement;
import com.laigu.laigu.duel.newcard.SettlementContext;

/** 破门·吴门墓板画（普通）：【消耗】本卡每颗骰 → +3 基础分。 */
public final class WuMenCommonCard implements DuelCard, OnSettlement
{
    @Override public String id() { return "wu_men_common"; }
    @Override public String displayName() { return ArtifactCardNames.variantName("wu_men", rarity()); }
    @Override public CardClass cardClass() { return CardClass.GONG; }
    @Override public CardRarity rarity() { return CardRarity.COMMON; }

    @Override public void onSettlement(SettlementContext context)
    {
        int dice = context.selfDice().size();
        if (dice == 0) return;
        context.addBaseScaled(perDieBase() * dice);
        context.selfState().markConsumed();
    }

    int perDieBase() { return 3; }
}
