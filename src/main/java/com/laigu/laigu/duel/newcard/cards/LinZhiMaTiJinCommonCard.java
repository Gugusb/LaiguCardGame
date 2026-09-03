package com.laigu.laigu.duel.newcard.cards;

import com.laigu.laigu.duel.CardClass;
import com.laigu.laigu.duel.newcard.ArtifactCardNames;
import com.laigu.laigu.duel.newcard.CardRarity;
import com.laigu.laigu.duel.newcard.DuelCard;
import com.laigu.laigu.duel.newcard.OnSettlement;
import com.laigu.laigu.duel.newcard.SettlementContext;

/** 青花智·麟趾马蹄金（普通）：【消耗】每张手牌 → +1 倍率。 */
public final class LinZhiMaTiJinCommonCard implements DuelCard, OnSettlement
{
    @Override public String id() { return "lin_zhi_ma_ti_jin_common"; }
    @Override public String displayName() { return ArtifactCardNames.variantName("lin_zhi_ma_ti_jin", rarity()); }
    @Override public CardClass cardClass() { return CardClass.MOU; }
    @Override public CardRarity rarity() { return CardRarity.COMMON; }

    @Override public void onSettlement(SettlementContext context)
    {
        int hand = context.handSize();
        if (hand == 0) return;
        context.addMultiplierScaled(perHandMult() * hand);
        context.selfState().markConsumed();
    }

    int perHandMult() { return 1; }
}
