package com.laigu.laigu.duel.newcard.cards;

import com.laigu.laigu.duel.CardClass;
import com.laigu.laigu.duel.newcard.ArtifactCardNames;
import com.laigu.laigu.duel.newcard.CardRarity;
import com.laigu.laigu.duel.newcard.DuelCard;
import com.laigu.laigu.duel.newcard.DraftPlanHandle;
import com.laigu.laigu.duel.newcard.OnDraftPlan;

/** 天球仪（普通）：【抢骰】双方各少抓 1 次骰。 */
public final class TianQiuYiCommonCard implements DuelCard, OnDraftPlan
{
    @Override public String id() { return "tian_qiu_yi_common"; }
    @Override public String displayName() { return ArtifactCardNames.variantName("tian_qiu_yi", rarity()); }
    @Override public CardClass cardClass() { return CardClass.MOU; }
    @Override public CardRarity rarity() { return CardRarity.COMMON; }

    @Override public void onDraftPlan(DraftPlanHandle handle)
    {
        handle.addTurnsBoth(-1);    }
}
