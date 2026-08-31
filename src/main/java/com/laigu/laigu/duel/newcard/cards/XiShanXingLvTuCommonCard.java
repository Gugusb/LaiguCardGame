package com.laigu.laigu.duel.newcard.cards;

import com.laigu.laigu.duel.CardClass;
import com.laigu.laigu.duel.newcard.AnimationEvent;
import com.laigu.laigu.duel.newcard.BattleEvent;
import com.laigu.laigu.duel.newcard.CardContext;
import com.laigu.laigu.duel.newcard.DuelCard;

/** 激活左·溪山普通版：伏击触发时激活左侧卡牌。 */
public final class XiShanXingLvTuCommonCard implements DuelCard
{
    @Override public String id() { return "xi_shan_xing_lv_tu_common"; }
    @Override public String displayName() { return "激活左·溪山"; }
    @Override public CardClass cardClass() { return CardClass.SHOU; }

    @Override public void onEvent(BattleEvent event, CardContext context)
    {
        if (event.type() != BattleEvent.Type.ACTIVATION) return;
        context.leftCard().ifPresent(context::activate);
        context.emit(new AnimationEvent(AnimationEvent.Type.CARD_TRIGGER, event.side(), event.slot(), id()));
    }
}
