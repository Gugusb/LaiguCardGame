package com.laigu.laigu.duel.newcard.cards;

import com.laigu.laigu.duel.CardClass;
import com.laigu.laigu.duel.newcard.AnimationEvent;
import com.laigu.laigu.duel.newcard.BattleEvent;
import com.laigu.laigu.duel.newcard.CardContext;
import com.laigu.laigu.duel.newcard.DuelCard;

/** 激活·江山普通版：激活进度达到2时增加2倍率。 */
public final class QianLiJiangShanCommonCard implements DuelCard
{
    @Override public String id() { return "qian_li_jiang_shan_common"; }
    @Override public String displayName() { return "激活·江山"; }
    @Override public CardClass cardClass() { return CardClass.DING; }

    @Override
    public void onEvent(BattleEvent event, CardContext context)
    {
        if (event.type() != BattleEvent.Type.ACTIVATION || context.selfActivation() < 2) return;
        context.addMultiplier(2);
        context.emit(new AnimationEvent(AnimationEvent.Type.CARD_ACTIVATE, event.side(), event.slot(), id()));
        context.emit(new AnimationEvent(AnimationEvent.Type.MULTIPLIER_POPUP, event.side(), event.slot(), id()));
    }
}
