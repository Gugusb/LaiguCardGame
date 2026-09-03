package com.laigu.laigu.duel.newcard.cards;

import com.laigu.laigu.duel.CardClass;
import com.laigu.laigu.duel.newcard.ArtifactCardNames;
import com.laigu.laigu.duel.newcard.CardRarity;
import com.laigu.laigu.duel.newcard.DuelCard;
import com.laigu.laigu.duel.newcard.OnSettlement;
import com.laigu.laigu.duel.newcard.SettlementContext;

/** 秦公镈普通版：相邻有同朝代卡 → +16 额外分。 */
public final class QinGongBoCommonCard implements DuelCard, OnSettlement
{
    @Override public String id() { return "qin_gong_bo_common"; }
    @Override public String displayName() { return ArtifactCardNames.variantName("qin_gong_bo", rarity()); }
    @Override public CardClass cardClass() { return CardClass.MOU; }
    @Override public CardRarity rarity() { return CardRarity.COMMON; }

    @Override public void onSettlement(SettlementContext context)
    {
        if (adjacentSameDynasty(context)) context.addExtra(sameDynastyBonus());
    }

    /** 对齐旧引擎 hasAdjacentDynasty：相邻有卡且朝代与自身相同。 */
    static boolean adjacentSameDynasty(SettlementContext context)
    {
        return checkSide(context, context.slot() - 1) || checkSide(context, context.slot() + 1);
    }

    private static boolean checkSide(SettlementContext context, int slot)
    {
        if (slot < 0 || slot >= 5) return false;
        String dynasty = context.dynastyAt(context.side(), slot);
        return dynasty != null && dynasty.equals(context.selfDynasty());
    }

    int sameDynastyBonus() { return 16; }
}
