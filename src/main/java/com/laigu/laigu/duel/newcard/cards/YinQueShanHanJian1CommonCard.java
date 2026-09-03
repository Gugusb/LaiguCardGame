package com.laigu.laigu.duel.newcard.cards;

import com.laigu.laigu.duel.CardClass;
import com.laigu.laigu.duel.newcard.ArtifactCardNames;
import com.laigu.laigu.duel.newcard.CardRarity;
import com.laigu.laigu.duel.newcard.DicePattern;
import com.laigu.laigu.duel.newcard.DuelCard;
import com.laigu.laigu.duel.newcard.OnSettlement;
import com.laigu.laigu.duel.newcard.SettlementContext;

/** 铜奔马式骰型卡：阴缺山寒涧剑·上普通版：若你的骰为顺子 → +4 倍率。 */
public final class YinQueShanHanJian1CommonCard implements DuelCard, OnSettlement
{
    @Override public String id() { return "yin_que_shan_han_jian_1_common"; }
    @Override public String displayName() { return ArtifactCardNames.variantName("yin_que_shan_han_jian_1", rarity()); }
    @Override public CardClass cardClass() { return CardClass.MOU; }
    @Override public CardRarity rarity() { return CardRarity.COMMON; }

    @Override public void onSettlement(SettlementContext context)
    {
        if (DicePattern.straight(context.selfDice())) context.addMultiplier(straightBonus());
    }

    int straightBonus() { return 4; }
}
