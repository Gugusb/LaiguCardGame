package com.laigu.laigu.duel.newcard.cards;

import com.laigu.laigu.duel.CardClass;
import com.laigu.laigu.duel.newcard.AnimationEvent;
import com.laigu.laigu.duel.newcard.CardContext;
import com.laigu.laigu.duel.newcard.DuelCard;
import com.laigu.laigu.duel.newcard.OnSettlement;
import com.laigu.laigu.duel.newcard.OnSummon;
import com.laigu.laigu.duel.newcard.SettlementContext;

/**
 * 激活左·溪山（金质）：充能x——结算时每颗本卡骰激活左侧卡牌2次。
 * 焕章：入场时激活己方所有卡牌1次（无可激活词条的卡尝试即失败，对齐清单口径）。
 */
public final class XiShanXingLvTuGoldCard implements DuelCard, OnSettlement, OnSummon
{
    @Override public String id() { return "xi_shan_xing_lv_tu_gold"; }
    @Override public String displayName() { return "溪山行旅图·金质"; }
    @Override public CardClass cardClass() { return CardClass.SHOU; }

    @Override public void onSettlement(SettlementContext context)
    {
        context.activateLeftCard(perDieActivations() * context.selfDice().size());
        context.emit(new AnimationEvent(AnimationEvent.Type.CARD_TRIGGER, context.side(), context.slot(), id()));
    }

    /** 焕章：入场激活己方所有卡牌1次（含不可激活卡的失败尝试）。 */
    @Override public void onSummon(CardContext context) { context.attemptActivateOwnCards(1); }

    int perDieActivations() { return 2; }
}
