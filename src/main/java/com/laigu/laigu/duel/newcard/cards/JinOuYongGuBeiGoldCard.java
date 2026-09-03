package com.laigu.laigu.duel.newcard.cards;

import com.laigu.laigu.duel.CardClass;
import com.laigu.laigu.duel.newcard.AnimationEvent;
import com.laigu.laigu.duel.newcard.ArtifactCardNames;
import com.laigu.laigu.duel.newcard.CardContext;
import com.laigu.laigu.duel.newcard.CardRarity;
import com.laigu.laigu.duel.newcard.DuelCard;
import com.laigu.laigu.duel.newcard.OnActivation;
import com.laigu.laigu.duel.newcard.OnSettlement;
import com.laigu.laigu.duel.newcard.SettlementContext;

/**
 * 激活·永固杯（金质）：激活进度达到 2 → 获得 2x 倍率（x=对方场上未放骰子的卡牌数），进度清零。
 * 焕章：回合结算时，我方每张金质卡 +1 倍率。
 */
public final class JinOuYongGuBeiGoldCard implements DuelCard, OnActivation, OnSettlement
{
    @Override public String id() { return "jin_ou_yong_gu_bei_gold"; }
    @Override public String displayName() { return ArtifactCardNames.variantName("jin_ou_yong_gu_bei", rarity()); }
    @Override public CardClass cardClass() { return CardClass.DING; }
    @Override public CardRarity rarity() { return CardRarity.GOLD; }

    @Override public int activationThreshold() { return 2; }

    @Override public void onActivation(CardContext context)
    {
        context.addTimingMult(perTargetMult() * context.oppositeCardsWithoutDice());
        context.selfState().setActivation(0);
        context.emit(new AnimationEvent(AnimationEvent.Type.CARD_ACTIVATE, context.side(), context.slot(), id()));
        context.emit(new AnimationEvent(AnimationEvent.Type.MULTIPLIER_POPUP, context.side(), context.slot(), id()));
    }

    /** 焕章：回合结算时我方每张金质卡 +1 倍率。 */
    @Override public void onSettlement(SettlementContext context)
    {
        int goldCount = (int) context.ownGoldCount();
        if (goldCount > 0) context.addMultiplierScaled(goldCount * perGoldMult());
    }

    int perTargetMult() { return 2; }
    int perGoldMult() { return 1; }
}
