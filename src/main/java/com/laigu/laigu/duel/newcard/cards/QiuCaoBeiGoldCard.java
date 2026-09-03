package com.laigu.laigu.duel.newcard.cards;

import com.laigu.laigu.duel.CardClass;
import com.laigu.laigu.duel.newcard.ArtifactCardNames;
import com.laigu.laigu.duel.newcard.CardRarity;
import com.laigu.laigu.duel.newcard.DuelCard;
import com.laigu.laigu.duel.newcard.OnSettlement;
import com.laigu.laigu.duel.newcard.SettlementContext;

/** 秋草杯金质版：相邻有不同职业卡 → +32 额外分。 */
public final class QiuCaoBeiGoldCard implements DuelCard, OnSettlement
{
    @Override public String id() { return "qiu_cao_bei_gold"; }
    @Override public String displayName() { return ArtifactCardNames.variantName("qiu_cao_bei", rarity()); }
    @Override public CardClass cardClass() { return CardClass.MOU; }
    @Override public CardRarity rarity() { return CardRarity.GOLD; }

    @Override public void onSettlement(SettlementContext context)
    {
        if (QiuCaoBeiCommonCard.adjacentDiffClass(context)) context.addExtra(diffClassBonus());
    }

    int diffClassBonus() { return 32; }
}
