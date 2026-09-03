package com.laigu.laigu.duel.newcard.cards;

import com.laigu.laigu.duel.CardClass;
import com.laigu.laigu.duel.newcard.ArtifactCardNames;
import com.laigu.laigu.duel.newcard.CardRarity;
import com.laigu.laigu.duel.newcard.DuelCard;
import com.laigu.laigu.duel.newcard.OnSettlement;
import com.laigu.laigu.duel.newcard.SettlementContext;

/**
 * 铜车马（金质）：本卡每有 1 颗骰 → +12 额外分。
 * 焕章：本卡每有 1 颗骰 → +1 倍率。
 */
public final class TongCheMaGoldCard implements DuelCard, OnSettlement
{
    @Override public String id() { return "tong_che_ma_gold"; }
    @Override public String displayName() { return ArtifactCardNames.variantName("tong_che_ma", rarity()); }
    @Override public CardClass cardClass() { return CardClass.GONG; }
    @Override public CardRarity rarity() { return CardRarity.GOLD; }

    @Override public void onSettlement(SettlementContext context)
    {
        context.addExtraScaled(perDieBonus() * context.selfDice().size());
        // 焕章：每颗骰 +1 倍率。
        context.addMultiplierScaled(context.selfDice().size());
    }

    int perDieBonus() { return 12; }
}
