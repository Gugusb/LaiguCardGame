package com.laigu.laigu.duel.newcard.cards;

import com.laigu.laigu.duel.CardClass;
import com.laigu.laigu.duel.newcard.AnimationEvent;
import com.laigu.laigu.duel.newcard.BattleEvent;
import com.laigu.laigu.duel.newcard.CardContext;
import com.laigu.laigu.duel.newcard.DuelCard;

/** 敦煌飞天金卡：伏击成功时标记对方骰子最少的一张卡于回合结束破坏。 */
public final class DunHuangFeiTianGoldCard implements DuelCard
{
    @Override public String id() { return "dun_huang_fei_tian_gold"; }
    @Override public String displayName() { return "伏击·飞天·金"; }
    @Override public CardClass cardClass() { return CardClass.MOU; }

    @Override
    public void onEvent(BattleEvent event, CardContext context)
    {
        if (event.type() != BattleEvent.Type.AMBUSH_SUCCESS) return;
        context.opponentTargetWithFewestDice().ifPresent(target -> {
            context.markDestroyAtRoundEnd(target);
            context.emit(new AnimationEvent(AnimationEvent.Type.CARD_DESTROY_MARK,
                    target.side(), target.slot(), id()));
        });
    }
}
