package com.laigu.laigu.duel.newcard.cards;

import com.laigu.laigu.duel.CardClass;
import com.laigu.laigu.duel.newcard.AnimationEvent;
import com.laigu.laigu.duel.newcard.CardContext;
import com.laigu.laigu.duel.newcard.DuelCard;
import com.laigu.laigu.duel.newcard.OnAmbushSuccess;
import com.laigu.laigu.duel.newcard.OnSettlement;
import com.laigu.laigu.duel.newcard.SettlementContext;

/**
 * 伏击·飞天（金质）：伏击成功 +60 额外分（失败无收益；与普通版互为镜像）。
 * 焕章：回合结算时，若对位为对方骰子最少之一的卡牌，将其破坏。
 */
public final class DunHuangFeiTianGoldCard implements DuelCard, OnAmbushSuccess, OnSettlement
{
    @Override public String id() { return "dun_huang_fei_tian_gold"; }
    @Override public String displayName() { return "敦煌飞天·金质"; }
    @Override public CardClass cardClass() { return CardClass.MOU; }

    @Override public void onAmbushSuccess(CardContext context)
    {
        context.addTimingExtra(successBonus());
        context.emit(new AnimationEvent(AnimationEvent.Type.CARD_TRIGGER, context.side(), context.slot(), id()));
    }

    /** 焕章：结算时对位为对方骰子最少之一 → 破坏对位卡牌。 */
    @Override public void onSettlement(SettlementContext context)
    {
        if (!context.oppositeIsFewestOpponentDice()) return;
        context.markOppositeDestroyAtRoundEnd();
        context.emit(new AnimationEvent(AnimationEvent.Type.CARD_DESTROY_MARK,
                1 - context.side(), context.slot(), id()));
    }

    int successBonus() { return 60; }
}
