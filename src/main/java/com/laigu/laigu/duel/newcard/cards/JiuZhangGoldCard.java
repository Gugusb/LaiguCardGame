package com.laigu.laigu.duel.newcard.cards;

import com.laigu.laigu.duel.CardClass;
import com.laigu.laigu.duel.newcard.ArtifactCardNames;
import com.laigu.laigu.duel.newcard.CardRarity;
import com.laigu.laigu.duel.newcard.DuelCard;
import com.laigu.laigu.duel.newcard.OnSettlement;
import com.laigu.laigu.duel.newcard.SettlementContext;

/** 共池·九章（金质）：共享骰池每颗 → +4 额外分。 */
public final class JiuZhangGoldCard implements DuelCard, OnSettlement
{
    @Override public String id() { return "jiu_zhang_gold"; }
    @Override public String displayName() { return ArtifactCardNames.variantName("jiu_zhang", rarity()); }
    @Override public CardClass cardClass() { return CardClass.DING; }
    @Override public CardRarity rarity() { return CardRarity.GOLD; }

    @Override public void onSettlement(SettlementContext context)
    {
        context.addExtraScaled(perPoolDieExtra() * context.sharedPool().size());
    }

    int perPoolDieExtra() { return 4; }
}
