package com.laigu.laigu.duel.newcard.cards;

import com.laigu.laigu.duel.CardClass;
import com.laigu.laigu.duel.newcard.ArtifactCardNames;
import com.laigu.laigu.duel.newcard.CardRarity;
import com.laigu.laigu.duel.newcard.DuelCard;
import com.laigu.laigu.duel.newcard.OnSettlement;
import com.laigu.laigu.duel.newcard.SettlementContext;

/** 珍珠宝钏（普通）：中间两槽位（1/2 号槽）→ +1/+2 倍率。 */
public final class ZhenZhuBaoChuangCommonCard implements DuelCard, OnSettlement
{
    @Override public String id() { return "zhen_zhu_bao_chuang_common"; }
    @Override public String displayName() { return ArtifactCardNames.variantName("zhen_zhu_bao_chuang", rarity()); }
    @Override public CardClass cardClass() { return CardClass.MOU; }
    @Override public CardRarity rarity() { return CardRarity.COMMON; }

    @Override public void onSettlement(SettlementContext context)
    {
        if (context.slot() == 1 || context.slot() == 2) context.addMultiplier(1);
    }
}
