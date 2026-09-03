package com.laigu.laigu.duel.newcard.cards;

import com.laigu.laigu.duel.CardClass;
import com.laigu.laigu.duel.newcard.ArtifactCardNames;
import com.laigu.laigu.duel.newcard.CardRarity;
import com.laigu.laigu.duel.newcard.DuelCard;
import com.laigu.laigu.duel.newcard.DraftPlanHandle;
import com.laigu.laigu.duel.newcard.OnDraftPlan;
import com.laigu.laigu.duel.newcard.OnSettlement;
import com.laigu.laigu.duel.newcard.SettlementContext;

/**
 * 天球仪（金质）：【抢骰】双方各少抓 2 次骰（对齐清单）。
 * 焕章：回合结算时，获得等同于公共骰池剩余点数的额外分。
 */
public final class TianQiuYiGoldCard implements DuelCard, OnDraftPlan, OnSettlement
{
    @Override public String id() { return "tian_qiu_yi_gold"; }
    @Override public String displayName() { return ArtifactCardNames.variantName("tian_qiu_yi", rarity()); }
    @Override public CardClass cardClass() { return CardClass.MOU; }
    @Override public CardRarity rarity() { return CardRarity.GOLD; }

    @Override public void onDraftPlan(DraftPlanHandle handle)
    {
        handle.addTurnsBoth(-2);
    }

    /** 焕章：结算时公共骰池剩余点数 → 额外分。 */
    @Override public void onSettlement(SettlementContext context)
    {
        int poolPoints = context.sharedPoolPointSum();
        if (poolPoints > 0) context.addExtraScaled(poolPoints);
    }
}
