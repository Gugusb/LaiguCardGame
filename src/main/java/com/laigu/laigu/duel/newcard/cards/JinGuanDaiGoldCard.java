package com.laigu.laigu.duel.newcard.cards;

import com.laigu.laigu.duel.CardClass;
import com.laigu.laigu.duel.newcard.ArtifactCardNames;
import com.laigu.laigu.duel.newcard.CardRarity;
import com.laigu.laigu.duel.newcard.DuelCard;
import com.laigu.laigu.duel.newcard.OnSettlement;
import com.laigu.laigu.duel.newcard.SettlementContext;

/** 金辉·金冠带（金质）：每张金质卡 → +16 基础分。 */
public final class JinGuanDaiGoldCard implements DuelCard, OnSettlement
{
    @Override public String id() { return "jin_guan_dai_gold"; }
    @Override public String displayName() { return ArtifactCardNames.variantName("jin_guan_dai", rarity()); }
    @Override public CardClass cardClass() { return CardClass.DING; }
    @Override public CardRarity rarity() { return CardRarity.GOLD; }

    @Override public void onSettlement(SettlementContext context)
    {
        context.addBaseScaled(perGoldBase() * (int) context.ownGoldCount());
    }

    int perGoldBase() { return 16; }
}
