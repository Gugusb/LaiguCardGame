package com.laigu.laigu.duel.newcard.cards;

import com.laigu.laigu.duel.CardClass;
import com.laigu.laigu.duel.newcard.ArtifactCardNames;
import com.laigu.laigu.duel.newcard.CardRarity;
import com.laigu.laigu.duel.newcard.DuelCard;
import com.laigu.laigu.duel.newcard.OnSettlement;
import com.laigu.laigu.duel.newcard.SettlementContext;

/** 泰然·雪景翰林图（金质）：我方每有 1 张金质卡 → +1 倍率（目录金卡词条替换）。 */
public final class XueJingHanLinTuGoldCard implements DuelCard, OnSettlement
{
    @Override public String id() { return "xue_jing_han_lin_tu_gold"; }
    @Override public String displayName() { return ArtifactCardNames.variantName("xue_jing_han_lin_tu", rarity()); }
    @Override public CardClass cardClass() { return CardClass.DING; }
    @Override public CardRarity rarity() { return CardRarity.GOLD; }

    @Override public void onSettlement(SettlementContext context)
    {
        // 主效果：每张鼎·盛卡 +2 倍率（金卡独立，不替换白卡效果）。
        context.addMultiplierScaled(2 * context.ownClassCount(com.laigu.laigu.duel.CardClass.DING));
        // 焕章：每张金质卡 +1 倍率。
        context.addMultiplierScaled(1 * (int) context.ownGoldCount());
    }
}
