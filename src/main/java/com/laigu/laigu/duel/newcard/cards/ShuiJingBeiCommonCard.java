package com.laigu.laigu.duel.newcard.cards;

import com.laigu.laigu.duel.CardClass;
import com.laigu.laigu.duel.newcard.ArtifactCardNames;
import com.laigu.laigu.duel.newcard.CardContext;
import com.laigu.laigu.duel.newcard.CardRarity;
import com.laigu.laigu.duel.newcard.DuelCard;
import com.laigu.laigu.duel.newcard.OnRoundStart;

/** 静水·青铜水贝（普通）：每轮开始时抽 1 张牌。 */
public final class ShuiJingBeiCommonCard implements DuelCard, OnRoundStart
{
    @Override public String id() { return "shui_jing_bei_common"; }
    @Override public String displayName() { return ArtifactCardNames.variantName("shui_jing_bei", rarity()); }
    @Override public CardClass cardClass() { return CardClass.SHOU; }
    @Override public CardRarity rarity() { return CardRarity.COMMON; }

    @Override public void onRoundStart(CardContext context)
    {
        context.drawCards(roundStartDraw());
    }

    int roundStartDraw() { return 1; }
}
