package com.laigu.laigu.duel.newcard.cards;

import com.laigu.laigu.duel.CardClass;
import com.laigu.laigu.duel.newcard.AnimationEvent;
import com.laigu.laigu.duel.newcard.DuelCard;
import com.laigu.laigu.duel.newcard.OnSettlement;
import com.laigu.laigu.duel.newcard.SettlementContext;

/** 激活左·海错（普通）：充能2——结算时至少2颗骰则激活我方所有卡牌1次。 */
public final class HaiCuoTuCommonCard implements DuelCard, OnSettlement
{
    @Override public String id() { return "hai_cuo_tu_common"; }
    @Override public String displayName() { return "海错图"; }
    @Override public CardClass cardClass() { return CardClass.SHOU; }

    @Override public void onSettlement(SettlementContext context)
    {
        if (context.selfDice().size() < diceThreshold()) return;
        context.activateAllOwnCards(activations());
        context.emit(new AnimationEvent(AnimationEvent.Type.CARD_TRIGGER, context.side(), context.slot(), id()));
    }

    int diceThreshold() { return 2; }
    int activations() { return 1; }
}
