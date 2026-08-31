package com.laigu.laigu.duel.newcard.cards;

import com.laigu.laigu.duel.CardClass;
import com.laigu.laigu.duel.newcard.AnimationEvent;
import com.laigu.laigu.duel.newcard.BattleEvent;
import com.laigu.laigu.duel.newcard.CardContext;
import com.laigu.laigu.duel.newcard.DuelCard;

/** 青铜仙鹤普通版：离场时抽一张牌。 */
public final class QingTongXianHeCommonCard implements DuelCard
{
    @Override public String id() { return "qing_tong_xian_he_common"; }
    @Override public String displayName() { return "磐石"; }
    @Override public CardClass cardClass() { return CardClass.SHOU; }

    @Override
    public void onEvent(BattleEvent event, CardContext context)
    {
        if (event.type() != BattleEvent.Type.LEAVE) return;
        context.drawCards(1);
        context.emit(new AnimationEvent(AnimationEvent.Type.CARD_TRIGGER, event.side(), event.slot(), id()));
    }
}
