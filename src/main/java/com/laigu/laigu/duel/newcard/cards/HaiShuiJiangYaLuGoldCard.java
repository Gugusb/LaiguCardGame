package com.laigu.laigu.duel.newcard.cards;

import com.laigu.laigu.duel.CardClass;
import com.laigu.laigu.duel.newcard.ArtifactCardNames;
import com.laigu.laigu.duel.newcard.CardRarity;
import com.laigu.laigu.duel.newcard.DuelCard;
import com.laigu.laigu.duel.newcard.DicePatterns;
import com.laigu.laigu.duel.newcard.OnSettlement;
import com.laigu.laigu.duel.newcard.SettlementContext;

/** 海水江崖庐金质版：你的骰有两对 → +6 倍率。 */
public final class HaiShuiJiangYaLuGoldCard implements DuelCard, OnSettlement
{
    @Override public String id() { return "hai_shui_jiang_ya_lu_gold"; }
    @Override public String displayName() { return ArtifactCardNames.variantName("hai_shui_jiang_ya_lu", rarity()); }
    @Override public CardClass cardClass() { return CardClass.MOU; }
    @Override public CardRarity rarity() { return CardRarity.GOLD; }

    @Override public void onSettlement(SettlementContext context)
    {
        if (DicePatterns.isTwoPair(context.selfDice())) context.addMultiplier(twoPairBonus());
    }

    int twoPairBonus() { return 6; }
}
