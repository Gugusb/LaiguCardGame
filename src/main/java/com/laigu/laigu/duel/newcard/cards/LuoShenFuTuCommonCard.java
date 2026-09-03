package com.laigu.laigu.duel.newcard.cards;

import com.laigu.laigu.duel.CardClass;
import com.laigu.laigu.duel.newcard.ArtifactCardNames;
import com.laigu.laigu.duel.newcard.CardRarity;
import com.laigu.laigu.duel.newcard.DuelCard;
import com.laigu.laigu.duel.newcard.OnSettlement;
import com.laigu.laigu.duel.newcard.SettlementContext;

/**
 * 倍增·洛神赋图（普通）：【消耗】结算时本轮基础分 ×2；
 * 另有第 2 批词条「我方每有 1 张卡 → +2 基础分」。
 */
public final class LuoShenFuTuCommonCard implements DuelCard, OnSettlement
{
    @Override public String id() { return "luo_shen_fu_tu_common"; }
    @Override public String displayName() { return ArtifactCardNames.variantName("luo_shen_fu_tu", rarity()); }
    @Override public CardClass cardClass() { return CardClass.DING; }
    @Override public CardRarity rarity() { return CardRarity.COMMON; }

    @Override public void onSettlement(SettlementContext context)
    {
        // 先加「每张卡 +2 基础分」，再翻倍：翻倍按旧引擎时序作用于全部基础分（含本卡贡献）。
        context.addBaseScaled(cardCntBase() * (int) context.ownFieldCount());
        context.addBaseScaled(context.baseScore());
        context.selfState().markConsumed();
    }

    int cardCntBase() { return 2; }
}
