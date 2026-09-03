package com.laigu.laigu.duel.newcard.cards;

import com.laigu.laigu.duel.CardClass;
import com.laigu.laigu.duel.newcard.ArtifactCardNames;
import com.laigu.laigu.duel.newcard.CardRarity;
import com.laigu.laigu.duel.newcard.DuelCard;
import com.laigu.laigu.duel.newcard.OnSettlement;
import com.laigu.laigu.duel.newcard.SettlementContext;

import java.util.List;

/** 白石三乐普通版：你的骰含相邻两数 → +10 额外分。 */
public final class BaiShiSanLeCommonCard implements DuelCard, OnSettlement
{
    @Override public String id() { return "bai_shi_san_le_common"; }
    @Override public String displayName() { return ArtifactCardNames.variantName("bai_shi_san_le", rarity()); }
    @Override public CardClass cardClass() { return CardClass.MOU; }
    @Override public CardRarity rarity() { return CardRarity.COMMON; }

    @Override public void onSettlement(SettlementContext context)
    {
        if (hasConsecutive(context.selfDice())) context.addExtra(consecutiveBonus());
    }

    static boolean hasConsecutive(List<Integer> dice)
    {
        boolean[] seen = new boolean[7];
        for (int v : dice)
            if (v >= 1 && v <= 6) seen[v] = true;
        for (int v = 1; v < 6; v++)
            if (seen[v] && seen[v + 1]) return true;
        return false;
    }

    int consecutiveBonus() { return 10; }
}
