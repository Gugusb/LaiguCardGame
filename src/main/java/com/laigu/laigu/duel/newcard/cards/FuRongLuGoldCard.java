package com.laigu.laigu.duel.newcard.cards;

import com.laigu.laigu.duel.CardClass;
import com.laigu.laigu.duel.newcard.ArtifactCardNames;
import com.laigu.laigu.duel.newcard.CardRarity;
import com.laigu.laigu.duel.newcard.DuelCard;
import com.laigu.laigu.duel.newcard.OnSettlement;
import com.laigu.laigu.duel.newcard.SettlementContext;

/** 存骰·芙蓉炉（金质）：每颗未布置骰 → +6 基础分。 */
public final class FuRongLuGoldCard implements DuelCard, OnSettlement
{
    @Override public String id() { return "fu_rong_lu_gold"; }
    @Override public String displayName() { return ArtifactCardNames.variantName("fu_rong_lu", rarity()); }
    @Override public CardClass cardClass() { return CardClass.DING; }
    @Override public CardRarity rarity() { return CardRarity.GOLD; }

    @Override public void onSettlement(SettlementContext context)
    {
        context.addBaseScaled(perPoolBase() * context.poolSize());
    }

    int perPoolBase() { return 6; }
}
