package com.laigu.laigu.duel.newcard.cards;

import com.laigu.laigu.duel.CardClass;
import com.laigu.laigu.duel.newcard.ArtifactCardNames;
import com.laigu.laigu.duel.newcard.CardRarity;
import com.laigu.laigu.duel.newcard.DuelCard;
import com.laigu.laigu.duel.newcard.OnSettlement;
import com.laigu.laigu.duel.newcard.SettlementContext;

/** 商周十供金质版：对手放置骰比你多 → +30 额外分。 */
public final class ShangZhouShiGongGoldCard implements DuelCard, OnSettlement
{
    @Override public String id() { return "shang_zhou_shi_gong_gold"; }
    @Override public String displayName() { return ArtifactCardNames.variantName("shang_zhou_shi_gong", rarity()); }
    @Override public CardClass cardClass() { return CardClass.SHOU; }
    @Override public CardRarity rarity() { return CardRarity.GOLD; }

    @Override public void onSettlement(SettlementContext context)
    {
        if (context.opponentDiceCount() > context.activeDice().size()) context.addExtra(behindDiceBonus());
    }

    int behindDiceBonus() { return 30; }
}
