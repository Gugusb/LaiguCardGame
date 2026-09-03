package com.laigu.laigu.duel.newcard.cards;

import com.laigu.laigu.duel.CardClass;
import com.laigu.laigu.duel.newcard.ArtifactCardNames;
import com.laigu.laigu.duel.newcard.CardRarity;
import com.laigu.laigu.duel.newcard.DuelCard;
import com.laigu.laigu.duel.newcard.OnSettlement;
import com.laigu.laigu.duel.newcard.SettlementContext;

/** 向日葵金质版：两侧均非本卡朝代（或空位）→ +2 倍率 +20 额外分。 */
public final class XiangRiKuiGoldCard implements DuelCard, OnSettlement
{
    @Override public String id() { return "xiang_ri_kui_gold"; }
    @Override public String displayName() { return ArtifactCardNames.variantName("xiang_ri_kui", rarity()); }
    @Override public CardClass cardClass() { return CardClass.MOU; }
    @Override public CardRarity rarity() { return CardRarity.GOLD; }

    @Override public void onSettlement(SettlementContext context)
    {
        if (XiangRiKuiCommonCard.isIsolatedByDynasty(context))
        {
            context.addMultiplier(isolatedMultiplier());
            context.addExtra(isolatedExtra());
        }
    }

    int isolatedMultiplier() { return 2; }

    int isolatedExtra() { return 20; }
}
