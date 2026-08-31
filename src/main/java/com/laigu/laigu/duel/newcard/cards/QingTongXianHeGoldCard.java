package com.laigu.laigu.duel.newcard.cards;

import com.laigu.laigu.duel.CardClass;
import com.laigu.laigu.duel.newcard.AnimationEvent;
import com.laigu.laigu.duel.newcard.BattleEvent;
import com.laigu.laigu.duel.newcard.CardContext;
import com.laigu.laigu.duel.newcard.DuelCard;

/** 青铜仙鹤金卡：离场时抽两张牌，并获得本回合四点倍率。 */
public final class QingTongXianHeGoldCard implements DuelCard
{
    @Override public String id() { return "qing_tong_xian_he_gold"; }
    @Override public String displayName() { return "磐石·金"; }
    @Override public CardClass cardClass() { return CardClass.SHOU; }

    @Override
    public void onEvent(BattleEvent event, CardContext context)
    {
        if (event.type() != BattleEvent.Type.LEAVE) return;
        context.drawCards(2);
        context.addMultiplier(4);
        context.emit(new AnimationEvent(AnimationEvent.Type.CARD_TRIGGER, event.side(), event.slot(), id()));
        context.emit(new AnimationEvent(AnimationEvent.Type.MULTIPLIER_POPUP, event.side(), event.slot(), id()));
    }
}
