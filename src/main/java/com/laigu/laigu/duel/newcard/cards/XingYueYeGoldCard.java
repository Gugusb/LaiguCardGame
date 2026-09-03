package com.laigu.laigu.duel.newcard.cards;

import com.laigu.laigu.duel.CardClass;
import com.laigu.laigu.duel.newcard.ArtifactCardNames;
import com.laigu.laigu.duel.newcard.CardRarity;
import com.laigu.laigu.duel.newcard.DuelCard;
import com.laigu.laigu.duel.newcard.DraftPlanHandle;
import com.laigu.laigu.duel.newcard.OnDraftPlan;

/** 星月夜（金质）：【抢骰】双方各多抓 2 次骰（对齐清单）。 */
public final class XingYueYeGoldCard implements DuelCard, OnDraftPlan
{
    @Override public String id() { return "xing_yue_ye_gold"; }
    @Override public String displayName() { return ArtifactCardNames.variantName("xing_yue_ye", rarity()); }
    @Override public CardClass cardClass() { return CardClass.DING; }
    @Override public CardRarity rarity() { return CardRarity.GOLD; }

    @Override public void onDraftPlan(DraftPlanHandle handle)
    {
        handle.addTurnsBoth(2);
    }
}
