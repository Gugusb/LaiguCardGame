package com.laigu.laigu.duel.newcard.cards;

import com.laigu.laigu.duel.CardClass;
import com.laigu.laigu.duel.newcard.ArtifactCardNames;
import com.laigu.laigu.duel.newcard.CardRarity;
import com.laigu.laigu.duel.newcard.DicePattern;
import com.laigu.laigu.duel.newcard.DuelCard;
import com.laigu.laigu.duel.newcard.OnSettlement;
import com.laigu.laigu.duel.newcard.SettlementContext;

/** 阴缺山寒涧剑·上金质版：若你的骰为顺子 → +8 倍率。 */
public final class YinQueShanHanJian1GoldCard implements DuelCard, OnSettlement
{
    @Override public String id() { return "yin_que_shan_han_jian_1_gold"; }
    @Override public String displayName() { return ArtifactCardNames.variantName("yin_que_shan_han_jian_1", rarity()); }
    @Override public CardClass cardClass() { return CardClass.MOU; }
    @Override public CardRarity rarity() { return CardRarity.GOLD; }

    @Override public void onSettlement(SettlementContext context)
    {
        if (DicePattern.straight(context.selfDice())) context.addMultiplier(straightBonus());
    }

    int straightBonus() { return 8; }
}
