package com.laigu.laigu.duel.newcard.cards;

import com.laigu.laigu.duel.CardClass;
import com.laigu.laigu.duel.newcard.ArtifactCardNames;
import com.laigu.laigu.duel.newcard.CardRarity;
import com.laigu.laigu.duel.newcard.DuelCard;
import com.laigu.laigu.duel.newcard.OnSettlement;
import com.laigu.laigu.duel.newcard.SettlementContext;

/** 蓄骰·赙陶瓶（金质）：每颗未布置骰 → +2 倍率。 */
public final class FuTaoPingGoldCard implements DuelCard, OnSettlement
{
    @Override public String id() { return "fu_tao_ping_gold"; }
    @Override public String displayName() { return ArtifactCardNames.variantName("fu_tao_ping", rarity()); }
    @Override public CardClass cardClass() { return CardClass.DING; }
    @Override public CardRarity rarity() { return CardRarity.GOLD; }

    @Override public void onSettlement(SettlementContext context)
    {
        context.addMultiplierScaled(perPoolMult() * context.poolSize());
    }

    int perPoolMult() { return 2; }
}
