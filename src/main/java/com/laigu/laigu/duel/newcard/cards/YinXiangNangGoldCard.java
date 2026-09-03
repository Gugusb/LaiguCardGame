package com.laigu.laigu.duel.newcard.cards;

import com.laigu.laigu.duel.CardClass;
import com.laigu.laigu.duel.newcard.ArtifactCardNames;
import com.laigu.laigu.duel.newcard.CardRarity;
import com.laigu.laigu.duel.newcard.DuelCard;
import com.laigu.laigu.duel.newcard.DraftPlanHandle;
import com.laigu.laigu.duel.newcard.OnDraftPlan;

/** 印香囊（金质）：【抢骰】对方抓取次数 -2（对齐清单）。 */
public final class YinXiangNangGoldCard implements DuelCard, OnDraftPlan
{
    @Override public String id() { return "yin_xiang_nang_gold"; }
    @Override public String displayName() { return ArtifactCardNames.variantName("yin_xiang_nang", rarity()); }
    @Override public CardClass cardClass() { return CardClass.MOU; }
    @Override public CardRarity rarity() { return CardRarity.GOLD; }

    @Override public void onDraftPlan(DraftPlanHandle handle)
    {
        handle.addTurnsOpponent(-2);
    }
}
