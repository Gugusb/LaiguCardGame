package com.laigu.laigu.duel.newcard.cards;

import com.laigu.laigu.duel.CardClass;
import com.laigu.laigu.duel.newcard.AnimationEvent;
import com.laigu.laigu.duel.newcard.ArtifactCardNames;
import com.laigu.laigu.duel.newcard.CardContext;
import com.laigu.laigu.duel.newcard.CardRarity;
import com.laigu.laigu.duel.newcard.DuelCard;
import com.laigu.laigu.duel.newcard.OnActivation;

/** 激活·永固杯（普通）：激活进度达到 2 → 获得 x 倍率（x=对方场上未放骰子的卡牌数），进度清零。 */
public final class JinOuYongGuBeiCommonCard implements DuelCard, OnActivation
{
    @Override public String id() { return "jin_ou_yong_gu_bei_common"; }
    @Override public String displayName() { return ArtifactCardNames.variantName("jin_ou_yong_gu_bei", rarity()); }
    @Override public CardClass cardClass() { return CardClass.DING; }
    @Override public CardRarity rarity() { return CardRarity.COMMON; }

    @Override public int activationThreshold() { return 2; }

    @Override public void onActivation(CardContext context)
    {
        context.addTimingMult(perTargetMult() * context.oppositeCardsWithoutDice());
        context.selfState().setActivation(0);
        context.emit(new AnimationEvent(AnimationEvent.Type.CARD_ACTIVATE, context.side(), context.slot(), id()));
        context.emit(new AnimationEvent(AnimationEvent.Type.MULTIPLIER_POPUP, context.side(), context.slot(), id()));
    }

    int perTargetMult() { return 1; }
}
