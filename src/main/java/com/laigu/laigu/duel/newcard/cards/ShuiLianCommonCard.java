package com.laigu.laigu.duel.newcard.cards;

import com.laigu.laigu.duel.CardClass;
import com.laigu.laigu.duel.newcard.AnimationEvent;
import com.laigu.laigu.duel.newcard.CardContext;
import com.laigu.laigu.duel.newcard.ArtifactCardNames;
import com.laigu.laigu.duel.newcard.CardRarity;
import com.laigu.laigu.duel.newcard.CardRuntimeState;
import com.laigu.laigu.duel.newcard.DuelCard;
import com.laigu.laigu.duel.newcard.OnAmbushSuccess;

/** 伏击·睡莲（普通）：伏击成功 → 对位卡前 2 颗骰无效化；失败无收益（对齐清单）。 */
public final class ShuiLianCommonCard implements DuelCard, OnAmbushSuccess
{
    @Override public String id() { return "shui_lian_common"; }
    @Override public String displayName() { return ArtifactCardNames.variantName("shui_lian", rarity()); }
    @Override public CardClass cardClass() { return CardClass.MOU; }
    @Override public CardRarity rarity() { return CardRarity.COMMON; }

    @Override public void onAmbushSuccess(CardContext context)
    {
        CardRuntimeState opposite = context.oppositeState().orElse(null);
        if (opposite == null) return;
        opposite.invalidateLeadingDice(Math.min(invalidateCount(), opposite.dice().size()));
        context.emit(new AnimationEvent(AnimationEvent.Type.CARD_TRIGGER, context.side(), context.slot(), id()));
    }

    int invalidateCount() { return 2; }
}
