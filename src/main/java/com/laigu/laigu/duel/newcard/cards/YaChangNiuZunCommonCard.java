package com.laigu.laigu.duel.newcard.cards;

import com.laigu.laigu.duel.CardClass;
import com.laigu.laigu.duel.newcard.ArtifactCardNames;
import com.laigu.laigu.duel.newcard.CardContext;
import com.laigu.laigu.duel.newcard.CardRarity;
import com.laigu.laigu.duel.newcard.DuelCard;
import com.laigu.laigu.duel.newcard.NewSettlementCalculator;
import com.laigu.laigu.duel.newcard.OnPoZhen;

/** 破阵·牛尊（普通）：破阵成功（本槽骰面和 > 对手同槽）→ 削弱对手该槽位 50%。 */
public final class YaChangNiuZunCommonCard implements DuelCard, NewSettlementCalculator.PoZhenHandler, OnPoZhen
{
    @Override public String id() { return "ya_chang_niu_zun_common"; }
    @Override public String displayName() { return ArtifactCardNames.variantName("ya_chang_niu_zun", rarity()); }
    @Override public CardClass cardClass() { return CardClass.GONG; }
    @Override public CardRarity rarity() { return CardRarity.COMMON; }

    @Override public boolean poZhenAlwaysSuccess() { return false; }
    @Override public boolean poZhenFullHalve() { return false; }

    @Override public void onPoZhen(CardContext context)
    {
        // 削弱本身由结算器统一处理；这里只发触发特效。
        context.emit(new com.laigu.laigu.duel.newcard.AnimationEvent(
                com.laigu.laigu.duel.newcard.AnimationEvent.Type.CARD_TRIGGER, context.side(), context.slot(), id()));
    }
}
