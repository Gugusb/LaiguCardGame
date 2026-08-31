package com.laigu.laigu.duel.newcard.cards;

import com.laigu.laigu.duel.CardClass;
import com.laigu.laigu.duel.newcard.AnimationEvent;
import com.laigu.laigu.duel.newcard.BattleEvent;
import com.laigu.laigu.duel.newcard.CardContext;
import com.laigu.laigu.duel.newcard.DuelCard;

/** 激活·描金壶普通版：激活进度达到1时增加3基础分。 */
public final class GuangCaiMiaoJinHuCommonCard implements DuelCard
{
    @Override public String id() { return "guang_cai_miao_jin_hu_common"; }
    @Override public String displayName() { return "激活·描金壶"; }
    @Override public CardClass cardClass() { return CardClass.DING; }

    @Override public void onEvent(BattleEvent event, CardContext context)
    {
        if (event.type() != BattleEvent.Type.ACTIVATION || context.selfActivation() != 1) return;
        context.addBaseScore(3);
        context.emit(new AnimationEvent(AnimationEvent.Type.CARD_ACTIVATE, event.side(), event.slot(), id()));
    }
}
