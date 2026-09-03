package com.laigu.laigu.duel.newcard.cards;

import com.laigu.laigu.duel.CardClass;
import com.laigu.laigu.duel.newcard.ArtifactCardNames;
import com.laigu.laigu.duel.newcard.CardRarity;
import com.laigu.laigu.duel.newcard.DuelCard;
import com.laigu.laigu.duel.newcard.OnSettlement;
import com.laigu.laigu.duel.newcard.SettlementContext;

/** 号角·卷云金喇叭（普通）：【消耗】对手每颗骰 → +3 额外分。 */
public final class JuanYunJinLaBaCommonCard implements DuelCard, OnSettlement
{
    @Override public String id() { return "juan_yun_jin_la_ba_common"; }
    @Override public String displayName() { return ArtifactCardNames.variantName("juan_yun_jin_la_ba", rarity()); }
    @Override public CardClass cardClass() { return CardClass.GONG; }
    @Override public CardRarity rarity() { return CardRarity.COMMON; }

    @Override public void onSettlement(SettlementContext context)
    {
        int oppDice = context.opponentDiceCount();
        if (oppDice == 0) return;
        context.addExtraScaled(perOppDieExtra() * oppDice);
        context.selfState().markConsumed();
    }

    int perOppDieExtra() { return 3; }
}
