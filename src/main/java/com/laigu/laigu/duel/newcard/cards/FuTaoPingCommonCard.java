package com.laigu.laigu.duel.newcard.cards;

import com.laigu.laigu.duel.CardClass;
import com.laigu.laigu.duel.newcard.ArtifactCardNames;
import com.laigu.laigu.duel.newcard.CardRarity;
import com.laigu.laigu.duel.newcard.DuelCard;
import com.laigu.laigu.duel.newcard.OnSettlement;
import com.laigu.laigu.duel.newcard.SettlementContext;

/** 蓄骰·赙陶瓶（普通）：每颗未布置骰 → +1 倍率。 */
public final class FuTaoPingCommonCard implements DuelCard, OnSettlement
{
    @Override public String id() { return "fu_tao_ping_common"; }
    @Override public String displayName() { return ArtifactCardNames.variantName("fu_tao_ping", rarity()); }
    @Override public CardClass cardClass() { return CardClass.DING; }
    @Override public CardRarity rarity() { return CardRarity.COMMON; }

    @Override public void onSettlement(SettlementContext context)
    {
        context.addMultiplierScaled(perPoolMult() * context.poolSize());
    }

    int perPoolMult() { return 1; }
}
