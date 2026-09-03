package com.laigu.laigu.duel.newcard.cards;

import com.laigu.laigu.duel.CardClass;
import com.laigu.laigu.duel.newcard.AnimationEvent;
import com.laigu.laigu.duel.newcard.ArtifactCardNames;
import com.laigu.laigu.duel.newcard.CardContext;
import com.laigu.laigu.duel.newcard.CardRarity;
import com.laigu.laigu.duel.newcard.DuelCard;
import com.laigu.laigu.duel.newcard.OnAmbushSuccess;

/** 伏击·百花（普通）：伏击成功 → 对位获得基础分的一半转给自己；失败无收益（对齐清单）。 */
public final class BaiHuaTuJuanCommonCard implements DuelCard, OnAmbushSuccess
{
    @Override public String id() { return "bai_hua_tu_juan_common"; }
    @Override public String displayName() { return ArtifactCardNames.variantName("bai_hua_tu_juan", rarity()); }
    @Override public CardClass cardClass() { return CardClass.MOU; }
    @Override public CardRarity rarity() { return CardRarity.COMMON; }

    @Override public void onAmbushSuccess(CardContext context)
    {
        // 镜像：获得对位有效骰面和的一半基础分。
        int oppositeBase = context.oppositeState()
                .map(state -> state.activeDice().stream().mapToInt(Integer::intValue).sum())
                .orElse(0);
        context.addTimingBase(oppositeBase / 2);
        context.emit(new AnimationEvent(AnimationEvent.Type.CARD_TRIGGER, context.side(), context.slot(), id()));
    }
}
