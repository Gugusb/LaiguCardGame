package com.laigu.laigu.duel.newcard.cards;

import com.laigu.laigu.duel.CardClass;
import com.laigu.laigu.duel.newcard.ArtifactCardNames;
import com.laigu.laigu.duel.newcard.CardRarity;
import com.laigu.laigu.duel.newcard.DuelCard;
import com.laigu.laigu.duel.newcard.OnSettlement;
import com.laigu.laigu.duel.newcard.SettlementContext;

/** 王世贞书画册普通版：你的骰面和 ≤9 或 ≥18 → +18 额外分。 */
public final class WangShiShuHanJuanCommonCard implements DuelCard, OnSettlement
{
    @Override public String id() { return "wang_shi_shu_han_juan_common"; }
    @Override public String displayName() { return ArtifactCardNames.variantName("wang_shi_shu_han_juan", rarity()); }
    @Override public CardClass cardClass() { return CardClass.MOU; }
    @Override public CardRarity rarity() { return CardRarity.COMMON; }

    @Override public void onSettlement(SettlementContext context)
    {
        int sum = context.selfDice().stream().mapToInt(Integer::intValue).sum();
        if (sum <= lowThreshold() || sum >= highThreshold()) context.addExtra(rangeBonus());
    }

    int lowThreshold() { return 9; }

    int highThreshold() { return 18; }

    int rangeBonus() { return 18; }
}
