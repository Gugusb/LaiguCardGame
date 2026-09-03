package com.laigu.laigu.duel.newcard.cards;

import com.laigu.laigu.duel.CardClass;
import com.laigu.laigu.duel.newcard.ArtifactCardNames;
import com.laigu.laigu.duel.newcard.CardRarity;
import com.laigu.laigu.duel.newcard.DuelCard;
import com.laigu.laigu.duel.newcard.OnSettlement;
import com.laigu.laigu.duel.newcard.SettlementContext;

/** 里貌盘金质版：你的骰含 6 → +20 额外分。 */
public final class LiMaoPanGoldCard implements DuelCard, OnSettlement
{
    @Override public String id() { return "li_mao_pan_gold"; }
    @Override public String displayName() { return ArtifactCardNames.variantName("li_mao_pan", rarity()); }
    @Override public CardClass cardClass() { return CardClass.MOU; }
    @Override public CardRarity rarity() { return CardRarity.GOLD; }

    @Override public void onSettlement(SettlementContext context)
    {
        if (context.selfDice().contains(6)) context.addExtra(sixBonus());
    }

    int sixBonus() { return 20; }
}
