package com.laigu.laigu.duel.newcard.cards;

import com.laigu.laigu.duel.CardClass;
import com.laigu.laigu.duel.newcard.ArtifactCardNames;
import com.laigu.laigu.duel.newcard.CardRarity;
import com.laigu.laigu.duel.newcard.DuelCard;
import com.laigu.laigu.duel.newcard.OnSettlement;
import com.laigu.laigu.duel.newcard.SettlementContext;

/** 汉墨拉比法典普通版：手牌为 0 → +20 额外分。 */
public final class HanMoLaBiFaDianCommonCard implements DuelCard, OnSettlement
{
    @Override public String id() { return "han_mo_la_bi_fa_dian_common"; }
    @Override public String displayName() { return ArtifactCardNames.variantName("han_mo_la_bi_fa_dian", rarity()); }
    @Override public CardClass cardClass() { return CardClass.SHOU; }
    @Override public CardRarity rarity() { return CardRarity.COMMON; }

    @Override public void onSettlement(SettlementContext context)
    {
        if (context.handSize() == 0) context.addExtra(emptyHandBonus());
    }

    int emptyHandBonus() { return 20; }
}
