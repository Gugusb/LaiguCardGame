package com.laigu.laigu.duel.newcard.cards;

import com.laigu.laigu.duel.CardClass;
import com.laigu.laigu.duel.newcard.AnimationEvent;
import com.laigu.laigu.duel.newcard.DuelCard;
import com.laigu.laigu.duel.newcard.OnSettlement;
import com.laigu.laigu.duel.newcard.SettlementContext;

/**
 * 激活左·海错（金质）：充能2——结算时至少2颗骰则激活我方所有卡牌2次。
 * 焕章：激活无法被激活的卡牌时，每次尝试 +10 额外分。
 */
public final class HaiCuoTuGoldCard implements DuelCard, OnSettlement
{
    @Override public String id() { return "hai_cuo_tu_gold"; }
    @Override public String displayName() { return "海错图·金质"; }
    @Override public CardClass cardClass() { return CardClass.SHOU; }

    @Override public void onSettlement(SettlementContext context)
    {
        if (context.selfDice().size() < diceThreshold()) return;
        int failed = context.activateAllOwnCards(activations());
        if (failed > 0) context.addExtra(failed * failActivationBonus());
        context.emit(new AnimationEvent(AnimationEvent.Type.CARD_TRIGGER, context.side(), context.slot(), id()));
    }

    int diceThreshold() { return 2; }
    int activations() { return 2; }
    int failActivationBonus() { return 10; }
}
