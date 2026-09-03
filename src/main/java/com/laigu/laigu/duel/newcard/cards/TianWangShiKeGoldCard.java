package com.laigu.laigu.duel.newcard.cards;

import com.laigu.laigu.duel.CardClass;
import com.laigu.laigu.duel.newcard.ArtifactCardNames;
import com.laigu.laigu.duel.newcard.CardRarity;
import com.laigu.laigu.duel.newcard.DicePattern;
import com.laigu.laigu.duel.newcard.DuelCard;
import com.laigu.laigu.duel.newcard.OnSettlement;
import com.laigu.laigu.duel.newcard.SettlementContext;

/** 天王实刻金质版：若你的骰全大(≥4) → +6 倍率。 */
public final class TianWangShiKeGoldCard implements DuelCard, OnSettlement
{
    @Override public String id() { return "tian_wang_shi_ke_gold"; }
    @Override public String displayName() { return ArtifactCardNames.variantName("tian_wang_shi_ke", rarity()); }
    @Override public CardClass cardClass() { return CardClass.DING; }
    @Override public CardRarity rarity() { return CardRarity.GOLD; }

    @Override public void onSettlement(SettlementContext context)
    {
        if (DicePattern.allHigh(context.selfDice())) context.addMultiplier(allHighBonus());
    }

    int allHighBonus() { return 6; }
}
