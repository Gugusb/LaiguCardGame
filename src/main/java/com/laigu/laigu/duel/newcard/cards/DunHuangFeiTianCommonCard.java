package com.laigu.laigu.duel.newcard.cards;

import com.laigu.laigu.duel.CardClass;
import com.laigu.laigu.duel.newcard.AnimationEvent;
import com.laigu.laigu.duel.newcard.BattleEvent;
import com.laigu.laigu.duel.newcard.CardContext;
import com.laigu.laigu.duel.newcard.DuelCard;

/** 敦煌飞天普通版：伏击失败时获得30额外分。 */
public final class DunHuangFeiTianCommonCard implements DuelCard
{
    @Override public String id() { return "dun_huang_fei_tian_common"; }
    @Override public String displayName() { return "敦煌飞天"; }
    @Override public CardClass cardClass() { return CardClass.MOU; }

    @Override
    public void onEvent(BattleEvent event, CardContext context)
    {
        if (event.type() != BattleEvent.Type.AMBUSH_FAIL) return;
        context.addTimingExtra(30);
        context.emit(new AnimationEvent(AnimationEvent.Type.SCORE_POPUP, event.side(), event.slot(), id()));
    }
}

