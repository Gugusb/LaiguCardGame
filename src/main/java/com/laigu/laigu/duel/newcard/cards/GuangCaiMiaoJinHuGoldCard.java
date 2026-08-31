package com.laigu.laigu.duel.newcard.cards;

import com.laigu.laigu.duel.CardClass;
import com.laigu.laigu.duel.newcard.AnimationEvent;
import com.laigu.laigu.duel.newcard.BattleEvent;
import com.laigu.laigu.duel.newcard.CardContext;
import com.laigu.laigu.duel.newcard.DuelCard;

/** 激活·描金壶金卡：激活进度达到1时增加3基础分和50额外分。 */
public final class GuangCaiMiaoJinHuGoldCard implements DuelCard
{
    @Override public String id() { return "guang_cai_miao_jin_hu_gold"; }
    @Override public String displayName() { return "激活·描金壶·金"; }
    @Override public CardClass cardClass() { return CardClass.DING; }

    @Override public void onEvent(BattleEvent event, CardContext context)
    {
        if (event.type() != BattleEvent.Type.ACTIVATION || context.selfActivation() != 1) return;
        context.addBaseScore(3);
        context.addExtraScore(50);
        context.emit(new AnimationEvent(AnimationEvent.Type.CARD_ACTIVATE, event.side(), event.slot(), id()));
        context.emit(new AnimationEvent(AnimationEvent.Type.SCORE_POPUP, event.side(), event.slot(), id()));
    }
}
