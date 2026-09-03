package com.laigu.laigu.duel.newcard.cards;

import com.laigu.laigu.duel.CardClass;
import com.laigu.laigu.duel.newcard.ArtifactCardNames;
import com.laigu.laigu.duel.newcard.CardRarity;
import com.laigu.laigu.duel.newcard.DuelCard;
import com.laigu.laigu.duel.newcard.OnSettlement;
import com.laigu.laigu.duel.newcard.SettlementContext;

/** 白石三乐金质版：你的骰含相邻两数 → +20 额外分。 */
public final class BaiShiSanLeGoldCard implements DuelCard, OnSettlement
{
    @Override public String id() { return "bai_shi_san_le_gold"; }
    @Override public String displayName() { return ArtifactCardNames.variantName("bai_shi_san_le", rarity()); }
    @Override public CardClass cardClass() { return CardClass.MOU; }
    @Override public CardRarity rarity() { return CardRarity.GOLD; }

    @Override public void onSettlement(SettlementContext context)
    {
        if (BaiShiSanLeCommonCard.hasConsecutive(context.selfDice())) context.addExtra(consecutiveBonus());
    }

    int consecutiveBonus() { return 20; }
}
