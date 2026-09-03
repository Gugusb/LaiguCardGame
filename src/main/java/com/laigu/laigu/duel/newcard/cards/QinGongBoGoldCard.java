package com.laigu.laigu.duel.newcard.cards;

import com.laigu.laigu.duel.CardClass;
import com.laigu.laigu.duel.newcard.ArtifactCardNames;
import com.laigu.laigu.duel.newcard.CardRarity;
import com.laigu.laigu.duel.newcard.DuelCard;
import com.laigu.laigu.duel.newcard.OnSettlement;
import com.laigu.laigu.duel.newcard.SettlementContext;

/** 秦公镈金质版：相邻有同朝代卡 → +32 额外分。 */
public final class QinGongBoGoldCard implements DuelCard, OnSettlement
{
    @Override public String id() { return "qin_gong_bo_gold"; }
    @Override public String displayName() { return ArtifactCardNames.variantName("qin_gong_bo", rarity()); }
    @Override public CardClass cardClass() { return CardClass.MOU; }
    @Override public CardRarity rarity() { return CardRarity.GOLD; }

    @Override public void onSettlement(SettlementContext context)
    {
        if (QinGongBoCommonCard.adjacentSameDynasty(context)) context.addExtra(sameDynastyBonus());
    }

    int sameDynastyBonus() { return 32; }
}
