package com.laigu.laigu.duel.newcard.cards;

import com.laigu.laigu.duel.CardClass;
import com.laigu.laigu.duel.newcard.ArtifactCardNames;
import com.laigu.laigu.duel.newcard.CardContext;
import com.laigu.laigu.duel.newcard.CardRarity;
import com.laigu.laigu.duel.newcard.DuelCard;
import com.laigu.laigu.duel.newcard.OnRoundStart;
import com.laigu.laigu.duel.newcard.OnSummon;

/**
 * 静水·青铜水贝（金质）：每轮开始时抽 2 张牌（对齐清单）。
 * 焕章：入场时抽 2 张牌（旧引擎金卡硬编码已由新核心承担）。
 */
public final class ShuiJingBeiGoldCard implements DuelCard, OnRoundStart, OnSummon
{
    @Override public String id() { return "shui_jing_bei_gold"; }
    @Override public String displayName() { return ArtifactCardNames.variantName("shui_jing_bei", rarity()); }
    @Override public CardClass cardClass() { return CardClass.SHOU; }
    @Override public CardRarity rarity() { return CardRarity.GOLD; }

    @Override public void onRoundStart(CardContext context)
    {
        context.drawCards(roundStartDraw());
    }

    @Override public void onSummon(CardContext context)
    {
        context.drawCards(summonDraw());
    }

    int roundStartDraw() { return 2; }
    int summonDraw() { return 2; }
}
