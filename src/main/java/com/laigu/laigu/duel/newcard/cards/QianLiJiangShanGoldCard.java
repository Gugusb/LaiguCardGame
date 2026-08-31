package com.laigu.laigu.duel.newcard.cards;

import com.laigu.laigu.duel.CardClass;
import com.laigu.laigu.duel.newcard.AnimationEvent;
import com.laigu.laigu.duel.newcard.BattleEvent;
import com.laigu.laigu.duel.newcard.CardContext;
import com.laigu.laigu.duel.newcard.DuelCard;

/** 激活·江山金卡：达成激活2时增加4倍率（普通效果+焕章效果）。 */
public final class QianLiJiangShanGoldCard implements DuelCard
{
    @Override public String id() { return "qian_li_jiang_shan_gold"; }
    @Override public String displayName() { return "激活·江山·金"; }
    @Override public CardClass cardClass() { return CardClass.DING; }

    @Override
    public void onEvent(BattleEvent event, CardContext context)
    {
        if (event.type() != BattleEvent.Type.ACTIVATION || context.selfActivation() < 2) return;
        context.addMultiplier(4);
        context.emit(new AnimationEvent(AnimationEvent.Type.CARD_ACTIVATE, event.side(), event.slot(), id()));
        context.emit(new AnimationEvent(AnimationEvent.Type.MULTIPLIER_POPUP, event.side(), event.slot(), id()));
    }
}
