package com.laigu.laigu.duel.newcard.cards;

import com.laigu.laigu.duel.CardClass;
import com.laigu.laigu.duel.newcard.ArtifactCardNames;
import com.laigu.laigu.duel.newcard.CardRarity;
import com.laigu.laigu.duel.newcard.DuelCard;
import com.laigu.laigu.duel.newcard.OnSettlement;
import com.laigu.laigu.duel.newcard.SettlementContext;

/** 王世贞书画册金质版：你的骰面和 ≤9 或 ≥18 → +36 额外分。 */
public final class WangShiShuHanJuanGoldCard implements DuelCard, OnSettlement
{
    @Override public String id() { return "wang_shi_shu_han_juan_gold"; }
    @Override public String displayName() { return ArtifactCardNames.variantName("wang_shi_shu_han_juan", rarity()); }
    @Override public CardClass cardClass() { return CardClass.MOU; }
    @Override public CardRarity rarity() { return CardRarity.GOLD; }

    @Override public void onSettlement(SettlementContext context)
    {
        int sum = context.selfDice().stream().mapToInt(Integer::intValue).sum();
        if (sum <= lowThreshold() || sum >= highThreshold()) context.addExtra(rangeBonus());
    }

    int lowThreshold() { return 9; }

    int highThreshold() { return 18; }

    int rangeBonus() { return 36; }
}
