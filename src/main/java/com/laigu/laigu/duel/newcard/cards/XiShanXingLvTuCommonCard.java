package com.laigu.laigu.duel.newcard.cards;

import com.laigu.laigu.duel.CardClass;
import com.laigu.laigu.duel.newcard.AnimationEvent;
import com.laigu.laigu.duel.newcard.DuelCard;
import com.laigu.laigu.duel.newcard.OnSettlement;
import com.laigu.laigu.duel.newcard.SettlementContext;

/** 激活左·溪山（普通）：充能x——结算时每颗本卡骰激活左侧卡牌1次。 */
public final class XiShanXingLvTuCommonCard implements DuelCard, OnSettlement
{
    @Override public String id() { return "xi_shan_xing_lv_tu_common"; }
    @Override public String displayName() { return "溪山行旅图"; }
    @Override public CardClass cardClass() { return CardClass.SHOU; }

    @Override public void onSettlement(SettlementContext context)
    {
        context.activateLeftCard(perDieActivations() * context.selfDice().size());
        context.emit(new AnimationEvent(AnimationEvent.Type.CARD_TRIGGER, context.side(), context.slot(), id()));
    }

    int perDieActivations() { return 1; }
}
