package com.laigu.laigu.duel.newcard.cards;

import com.laigu.laigu.duel.CardClass;
import com.laigu.laigu.duel.newcard.ArtifactCardNames;
import com.laigu.laigu.duel.newcard.CardRarity;
import com.laigu.laigu.duel.newcard.DuelCard;
import com.laigu.laigu.duel.newcard.OnSettlement;
import com.laigu.laigu.duel.newcard.SettlementContext;

/**
 * 倍增·洛神赋图（金质）：【消耗】结算时本轮基础分 ×2（翻倍类不数值翻倍，与旧引擎一致）；
 * 「我方每有 1 张卡 → +4 基础分」（分值类翻倍 2→4）。
 */
public final class LuoShenFuTuGoldCard implements DuelCard, OnSettlement
{
    @Override public String id() { return "luo_shen_fu_tu_gold"; }
    @Override public String displayName() { return ArtifactCardNames.variantName("luo_shen_fu_tu", rarity()); }
    @Override public CardClass cardClass() { return CardClass.DING; }
    @Override public CardRarity rarity() { return CardRarity.GOLD; }

    @Override public void onSettlement(SettlementContext context)
    {
        context.addBaseScaled(cardCntBase() * (int) context.ownFieldCount());
        context.addBaseScaled(context.baseScore());
        context.selfState().markConsumed();
    }

    int cardCntBase() { return 4; }
}
