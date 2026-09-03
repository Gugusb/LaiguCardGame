package com.laigu.laigu.duel.newcard.cards;

import com.laigu.laigu.duel.CardClass;
import com.laigu.laigu.duel.newcard.ArtifactCardNames;
import com.laigu.laigu.duel.newcard.CardRarity;
import com.laigu.laigu.duel.newcard.DuelCard;
import com.laigu.laigu.duel.newcard.OnSettlement;
import com.laigu.laigu.duel.newcard.SettlementContext;

/** 敕令·金石录（金质）：我方每有 1 张金质卡 → +10 额外分（目录焕章词条替换）。 */
public final class JinShiLuGoldCard implements DuelCard, OnSettlement
{
    @Override public String id() { return "jin_shi_lu_gold"; }
    @Override public String displayName() { return ArtifactCardNames.variantName("jin_shi_lu", rarity()); }
    @Override public CardClass cardClass() { return CardClass.DING; }
    @Override public CardRarity rarity() { return CardRarity.GOLD; }

    @Override public void onSettlement(SettlementContext context)
    {
        // 主效果：每张唐代卡 +4 基础分（金卡独立，不替换白卡效果）。
        context.addBaseScaled(4 * context.ownDynastyCount("唐"));
        // 焕章：每张金质卡 +10 额外分。
        context.addExtraScaled(10 * (int) context.ownGoldCount());
    }
}
