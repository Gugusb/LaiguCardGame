package com.laigu.laigu.duel.newcard.cards;

import com.laigu.laigu.duel.CardClass;
import com.laigu.laigu.duel.newcard.ArtifactCardNames;
import com.laigu.laigu.duel.newcard.CardRarity;
import com.laigu.laigu.duel.newcard.DuelCard;
import com.laigu.laigu.duel.newcard.OnSettlement;
import com.laigu.laigu.duel.newcard.SettlementContext;

/** 短臂维纳斯金质版：总局数落后 → +40 额外分。 */
public final class DuanBiWeiNaSiGoldCard implements DuelCard, OnSettlement
{
    @Override public String id() { return "duan_bi_wei_na_si_gold"; }
    @Override public String displayName() { return ArtifactCardNames.variantName("duan_bi_wei_na_si", rarity()); }
    @Override public CardClass cardClass() { return CardClass.SHOU; }
    @Override public CardRarity rarity() { return CardRarity.GOLD; }

    @Override public void onSettlement(SettlementContext context)
    {
        if (context.opponentWins() > context.wins()) context.addExtra(behindBonus());
    }

    int behindBonus() { return 40; }
}
