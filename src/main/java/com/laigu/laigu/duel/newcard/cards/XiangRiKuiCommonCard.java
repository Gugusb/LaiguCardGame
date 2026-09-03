package com.laigu.laigu.duel.newcard.cards;

import com.laigu.laigu.duel.CardClass;
import com.laigu.laigu.duel.newcard.ArtifactCardNames;
import com.laigu.laigu.duel.newcard.CardRarity;
import com.laigu.laigu.duel.newcard.DuelCard;
import com.laigu.laigu.duel.newcard.OnSettlement;
import com.laigu.laigu.duel.newcard.SettlementContext;

/** 向日葵普通版：两侧均非本卡朝代（或空位）→ +1 倍率 +10 额外分。 */
public final class XiangRiKuiCommonCard implements DuelCard, OnSettlement
{
    @Override public String id() { return "xiang_ri_kui_common"; }
    @Override public String displayName() { return ArtifactCardNames.variantName("xiang_ri_kui", rarity()); }
    @Override public CardClass cardClass() { return CardClass.MOU; }
    @Override public CardRarity rarity() { return CardRarity.COMMON; }

    @Override public void onSettlement(SettlementContext context)
    {
        if (isIsolatedByDynasty(context))
        {
            context.addMultiplier(isolatedMultiplier());
            context.addExtra(isolatedExtra());
        }
    }

    /** 对齐旧引擎 bothSidesNotDynasty：相邻空位视为"非本朝代"。 */
    static boolean isIsolatedByDynasty(SettlementContext context)
    {
        return checkSide(context, context.slot() - 1) && checkSide(context, context.slot() + 1);
    }

    private static boolean checkSide(SettlementContext context, int slot)
    {
        if (slot < 0 || slot >= 5) return true;
        String dynasty = context.dynastyAt(context.side(), slot);
        return dynasty == null || !dynasty.equals(context.selfDynasty());
    }

    int isolatedMultiplier() { return 1; }

    int isolatedExtra() { return 10; }
}
