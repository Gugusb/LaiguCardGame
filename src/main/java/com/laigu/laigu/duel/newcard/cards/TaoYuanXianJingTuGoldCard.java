package com.laigu.laigu.duel.newcard.cards;

import com.laigu.laigu.duel.CardClass;
import com.laigu.laigu.duel.newcard.ArtifactCardNames;
import com.laigu.laigu.duel.newcard.CardRarity;
import com.laigu.laigu.duel.newcard.DuelCard;
import com.laigu.laigu.duel.newcard.OnSettlement;
import com.laigu.laigu.duel.newcard.SettlementContext;

/** 桃园仙境图金质版：连续在场 ≥2 轮 → +32 额外分。 */
public final class TaoYuanXianJingTuGoldCard implements DuelCard, OnSettlement
{
    @Override public String id() { return "tao_yuan_xian_jing_tu_gold"; }
    @Override public String displayName() { return ArtifactCardNames.variantName("tao_yuan_xian_jing_tu", rarity()); }
    @Override public CardClass cardClass() { return CardClass.SHOU; }
    @Override public CardRarity rarity() { return CardRarity.GOLD; }

    @Override public void onSettlement(SettlementContext context)
    {
        // roundsOnField 从 0 起算（旧版 FieldCard.roundsOnField 从 1 起算）；在场第 2 轮时值为 1。
        if (context.selfState().roundsOnField() >= roundsThreshold()) context.addExtra(veteranBonus());
    }

    int roundsThreshold() { return 1; }

    int veteranBonus() { return 40; }
}
