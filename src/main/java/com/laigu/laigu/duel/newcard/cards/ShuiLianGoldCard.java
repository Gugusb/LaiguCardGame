package com.laigu.laigu.duel.newcard.cards;

import com.laigu.laigu.duel.CardClass;
import com.laigu.laigu.duel.newcard.AnimationEvent;
import com.laigu.laigu.duel.newcard.ArtifactCardNames;
import com.laigu.laigu.duel.newcard.CardContext;
import com.laigu.laigu.duel.newcard.CardRarity;
import com.laigu.laigu.duel.newcard.CardRuntimeState;
import com.laigu.laigu.duel.newcard.DuelCard;
import com.laigu.laigu.duel.newcard.OnAmbushSuccess;

/**
 * 伏击·睡莲（金质）：伏击成功 → 对位卡前 4 颗骰无效化并收回其基础分；失败无收益（对齐清单）。
 */
public final class ShuiLianGoldCard implements DuelCard, OnAmbushSuccess
{
    @Override public String id() { return "shui_lian_gold"; }
    @Override public String displayName() { return ArtifactCardNames.variantName("shui_lian", rarity()); }
    @Override public CardClass cardClass() { return CardClass.MOU; }
    @Override public CardRarity rarity() { return CardRarity.GOLD; }

    @Override public void onAmbushSuccess(CardContext context)
    {
        CardRuntimeState opposite = context.oppositeState().orElse(null);
        if (opposite == null) return;
        int count = Math.min(goldInvalidate(), opposite.dice().size());
        opposite.invalidateLeadingDice(count);
        // 收回被无效化骰子的基础分。
        int reclaimed = 0;
        for (int i = 0; i < count; i++) reclaimed += opposite.dice().get(i);
        context.addTimingBase(reclaimed);
        context.emit(new AnimationEvent(AnimationEvent.Type.CARD_TRIGGER, context.side(), context.slot(), id()));
    }

    int goldInvalidate() { return 4; }
}
