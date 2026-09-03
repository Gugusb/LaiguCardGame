package com.laigu.laigu.duel.newcard.cards;

import com.laigu.laigu.duel.CardClass;
import com.laigu.laigu.duel.newcard.ArtifactCardNames;
import com.laigu.laigu.duel.newcard.CardRarity;
import com.laigu.laigu.duel.newcard.DuelCard;
import com.laigu.laigu.duel.newcard.OnSettlement;
import com.laigu.laigu.duel.newcard.SettlementContext;

/** 珍珠宝钏（金质）：中间两槽位（1/2 号槽）→ +1/+2 倍率。 */
public final class ZhenZhuBaoChuangGoldCard implements DuelCard, OnSettlement
{
    @Override public String id() { return "zhen_zhu_bao_chuang_gold"; }
    @Override public String displayName() { return ArtifactCardNames.variantName("zhen_zhu_bao_chuang", rarity()); }
    @Override public CardClass cardClass() { return CardClass.MOU; }
    @Override public CardRarity rarity() { return CardRarity.GOLD; }

    @Override public void onSettlement(SettlementContext context)
    {
        if (context.slot() == 1 || context.slot() == 2) context.addMultiplier(2);
    }
}
