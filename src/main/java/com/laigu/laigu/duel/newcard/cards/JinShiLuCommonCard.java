package com.laigu.laigu.duel.newcard.cards;

import com.laigu.laigu.duel.CardClass;
import com.laigu.laigu.duel.newcard.ArtifactCardNames;
import com.laigu.laigu.duel.newcard.CardRarity;
import com.laigu.laigu.duel.newcard.DuelCard;
import com.laigu.laigu.duel.newcard.OnSettlement;
import com.laigu.laigu.duel.newcard.SettlementContext;

/** 敕令·金石录（普通）：我方每有 1 张唐代卡 → +2 基础分。 */
public final class JinShiLuCommonCard implements DuelCard, OnSettlement
{
    private static final String TARGET_DYNASTY = "唐";

    @Override public String id() { return "jin_shi_lu_common"; }
    @Override public String displayName() { return ArtifactCardNames.variantName("jin_shi_lu", rarity()); }
    @Override public CardClass cardClass() { return CardClass.DING; }
    @Override public CardRarity rarity() { return CardRarity.COMMON; }

    @Override public void onSettlement(SettlementContext context)
    {
        context.addBaseScaled(perDynastyCard() * context.ownDynastyCount(TARGET_DYNASTY));
    }

    int perDynastyCard() { return 2; }
}
