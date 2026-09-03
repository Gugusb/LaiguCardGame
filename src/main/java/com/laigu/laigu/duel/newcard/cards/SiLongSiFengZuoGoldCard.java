package com.laigu.laigu.duel.newcard.cards;

import com.laigu.laigu.duel.CardClass;
import com.laigu.laigu.duel.newcard.ArtifactCardNames;
import com.laigu.laigu.duel.newcard.CardContext;
import com.laigu.laigu.duel.newcard.CardRarity;
import com.laigu.laigu.duel.newcard.DuelCard;
import com.laigu.laigu.duel.newcard.OnSettlement;
import com.laigu.laigu.duel.newcard.OnSummon;
import com.laigu.laigu.duel.newcard.SettlementContext;

/**
 * 观星·四灵四凤座（金质）：入场时回复 2 点行动力。
 * 焕章：本回合每消耗 1 点行动力 +15 额外分。
 */
public final class SiLongSiFengZuoGoldCard implements DuelCard, OnSummon, OnSettlement
{
    @Override public String id() { return "si_long_si_feng_zuo_gold"; }
    @Override public String displayName() { return ArtifactCardNames.variantName("si_long_si_feng_zuo", rarity()); }
    @Override public CardClass cardClass() { return CardClass.MOU; }
    @Override public CardRarity rarity() { return CardRarity.GOLD; }

    @Override public void onSummon(CardContext context)
    {
        context.restoreActionPoints(restoreAp());
    }

    /** 焕章：结算时按本轮消耗的行动力点数计 +15/点。 */
    @Override public void onSettlement(SettlementContext context)
    {
        int spent = context.actionPointsSpentThisRound();
        if (spent > 0) context.addExtraScaled(spent * perApExtra());
    }

    int restoreAp() { return 2; }
    int perApExtra() { return 15; }
}
