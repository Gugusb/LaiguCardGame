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

/** 破阵·牛尊（金质）：破阵成功 → 削弱对手该槽位 100%（poZhenFull）；焕章：破阵成功时封锁对位卡牌（不可更换不可焕章）。 */
public final class YaChangNiuZunGoldCard implements DuelCard, NewSettlementCalculator.PoZhenHandler, OnPoZhen, OnSettlement
{
    @Override public String id() { return "ya_chang_niu_zun_gold"; }
    @Override public String displayName() { return ArtifactCardNames.variantName("ya_chang_niu_zun", rarity()); }
    @Override public CardClass cardClass() { return CardClass.GONG; }
    @Override public CardRarity rarity() { return CardRarity.GOLD; }

    @Override public boolean poZhenAlwaysSuccess() { return false; }
    @Override public boolean poZhenFullHalve() { return true; }

    @Override public void onPoZhen(CardContext context)
    {
        context.emit(new AnimationEvent(AnimationEvent.Type.CARD_TRIGGER, context.side(), context.slot(), id()));
    }

    @Override public void onSettlement(SettlementContext context)
    {
        // 焕章词条：破阵触发时 +20 额外分（由结算器判定后以 contributionKeep==0 表达不可行；
        // 这里按旧引擎 goldVariant 语义：破阵成功时削弱100%并 +20 额外分，通过对位骰面和复判）。
        if (context.poZhenSuccess()) context.markOppositeLocked();
    }
}
