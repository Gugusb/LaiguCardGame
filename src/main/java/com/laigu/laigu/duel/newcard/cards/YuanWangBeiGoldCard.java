package com.laigu.laigu.duel.newcard.cards;

import com.laigu.laigu.duel.CardClass;
import com.laigu.laigu.duel.newcard.AnimationEvent;
import com.laigu.laigu.duel.newcard.ArtifactCardNames;
import com.laigu.laigu.duel.newcard.CardContext;
import com.laigu.laigu.duel.newcard.CardRarity;
import com.laigu.laigu.duel.newcard.DuelCard;
import com.laigu.laigu.duel.newcard.NewSettlementCalculator;
import com.laigu.laigu.duel.newcard.OnPoZhen;
import com.laigu.laigu.duel.newcard.OnSettlement;
import com.laigu.laigu.duel.newcard.SettlementContext;

/**
 * 破阵·越杯（金质）：破阵成功 → 削弱对手该槽位 100% + 相邻槽位 ±1 各削弱 50%。
 * 对齐清单：金卡无破阵额外分词条。
 */
public final class YuanWangBeiGoldCard implements DuelCard, NewSettlementCalculator.PoZhenHandler, OnPoZhen, OnSettlement
{
    @Override public String id() { return "yuan_wang_bei_gold"; }
    @Override public String displayName() { return ArtifactCardNames.variantName("yuan_wang_bei", rarity()); }
    @Override public CardClass cardClass() { return CardClass.GONG; }
    @Override public CardRarity rarity() { return CardRarity.GOLD; }

    @Override public boolean poZhenAlwaysSuccess() { return false; }
    @Override public boolean poZhenFullHalve() { return false; }
    @Override public boolean poZhenHalveNeighbors() { return true; }

    @Override public void onPoZhen(CardContext context)
    {
        context.emit(new AnimationEvent(AnimationEvent.Type.CARD_TRIGGER, context.side(), context.slot(), id()));
    }

    @Override public void onSettlement(SettlementContext context)
    {
        // 破阵判定与削弱由结算器统一处理（PoZhenHandler.poZhenHalveNeighbors），无额外结算词条。
    }
}
