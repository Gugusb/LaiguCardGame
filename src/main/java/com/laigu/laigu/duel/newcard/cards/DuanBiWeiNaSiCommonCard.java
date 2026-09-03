package com.laigu.laigu.duel.newcard.cards;

import com.laigu.laigu.duel.CardClass;
import com.laigu.laigu.duel.newcard.ArtifactCardNames;
import com.laigu.laigu.duel.newcard.CardRarity;
import com.laigu.laigu.duel.newcard.DuelCard;
import com.laigu.laigu.duel.newcard.OnSettlement;
import com.laigu.laigu.duel.newcard.SettlementContext;

/** 短臂维纳斯普通版：总局数落后 → +20 额外分。 */
public final class DuanBiWeiNaSiCommonCard implements DuelCard, OnSettlement
{
    @Override public String id() { return "duan_bi_wei_na_si_common"; }
    @Override public String displayName() { return ArtifactCardNames.variantName("duan_bi_wei_na_si", rarity()); }
    @Override public CardClass cardClass() { return CardClass.SHOU; }
    @Override public CardRarity rarity() { return CardRarity.COMMON; }

    @Override public void onSettlement(SettlementContext context)
    {
        if (context.opponentWins() > context.wins()) context.addExtra(behindBonus());
    }

    int behindBonus() { return 20; }
}
