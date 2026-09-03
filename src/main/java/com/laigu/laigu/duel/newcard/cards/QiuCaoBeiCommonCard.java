package com.laigu.laigu.duel.newcard.cards;

import com.laigu.laigu.duel.CardClass;
import com.laigu.laigu.duel.newcard.ArtifactCardNames;
import com.laigu.laigu.duel.newcard.CardRarity;
import com.laigu.laigu.duel.newcard.DuelCard;
import com.laigu.laigu.duel.newcard.OnSettlement;
import com.laigu.laigu.duel.newcard.SettlementContext;

/** 秋草杯普通版：相邻有不同职业卡 → +16 额外分。 */
public final class QiuCaoBeiCommonCard implements DuelCard, OnSettlement
{
    @Override public String id() { return "qiu_cao_bei_common"; }
    @Override public String displayName() { return ArtifactCardNames.variantName("qiu_cao_bei", rarity()); }
    @Override public CardClass cardClass() { return CardClass.MOU; }
    @Override public CardRarity rarity() { return CardRarity.COMMON; }


    @Override public void onSettlement(SettlementContext context)
    {
        if (adjacentDiffClass(context)) context.addExtra(diffClassBonus());
    }

    /** 对齐旧引擎 hasAdjacentDiffClass：只看有卡的相邻位，职业不同即触发。 */
    static boolean adjacentDiffClass(SettlementContext context)
    {
        return diffClassAt(context, context.slot() - 1) || diffClassAt(context, context.slot() + 1);
    }

    private static boolean diffClassAt(SettlementContext context, int slot)
    {
        if (slot < 0 || slot >= 5) return false;
        CardClass neighborClass = context.cardClassAt(context.side(), slot);
        return neighborClass != null && neighborClass != context.self().cardClass();
    }

    int diffClassBonus() { return 16; }
}
