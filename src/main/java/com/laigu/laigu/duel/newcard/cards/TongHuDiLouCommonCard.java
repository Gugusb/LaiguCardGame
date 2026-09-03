package com.laigu.laigu.duel.newcard.cards;

import com.laigu.laigu.duel.CardClass;
import com.laigu.laigu.duel.newcard.ArtifactCardNames;
import com.laigu.laigu.duel.newcard.CardRarity;
import com.laigu.laigu.duel.newcard.DuelCard;
import com.laigu.laigu.duel.newcard.DraftPlanHandle;
import com.laigu.laigu.duel.newcard.OnDraftPlan;

/** 铜壶滴漏（普通）：【抢骰】本方抓取次数 +1。 */
public final class TongHuDiLouCommonCard implements DuelCard, OnDraftPlan
{
    @Override public String id() { return "tong_hu_di_lou_common"; }
    @Override public String displayName() { return ArtifactCardNames.variantName("tong_hu_di_lou", rarity()); }
    @Override public CardClass cardClass() { return CardClass.MOU; }
    @Override public CardRarity rarity() { return CardRarity.COMMON; }

    @Override public void onDraftPlan(DraftPlanHandle handle)
    {
        handle.addTurnsSelf(1);    }
}
