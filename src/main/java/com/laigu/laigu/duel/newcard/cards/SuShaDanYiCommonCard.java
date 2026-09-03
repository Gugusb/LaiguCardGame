package com.laigu.laigu.duel.newcard.cards;

import com.laigu.laigu.duel.CardClass;
import com.laigu.laigu.duel.newcard.ArtifactCardNames;
import com.laigu.laigu.duel.newcard.CardRarity;
import com.laigu.laigu.duel.newcard.DuelCard;
import com.laigu.laigu.duel.newcard.OnSettlement;
import com.laigu.laigu.duel.newcard.SettlementContext;

/** 握权·苏砂丹彝（普通）：每张手牌 → +1 倍率。 */
public final class SuShaDanYiCommonCard implements DuelCard, OnSettlement
{
    @Override public String id() { return "su_sha_dan_yi_common"; }
    @Override public String displayName() { return ArtifactCardNames.variantName("su_sha_dan_yi", rarity()); }
    @Override public CardClass cardClass() { return CardClass.DING; }
    @Override public CardRarity rarity() { return CardRarity.COMMON; }

    @Override public void onSettlement(SettlementContext context)
    {
        context.addMultiplierScaled(perHandMult() * context.handSize());
    }

    int perHandMult() { return 1; }
}
