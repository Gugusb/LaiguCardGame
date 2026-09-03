package com.laigu.laigu.duel.newcard.cards;

import com.laigu.laigu.duel.CardClass;
import com.laigu.laigu.duel.newcard.AnimationEvent;
import com.laigu.laigu.duel.newcard.ArtifactCardNames;
import com.laigu.laigu.duel.newcard.BattleEvent;
import com.laigu.laigu.duel.newcard.CardContext;
import com.laigu.laigu.duel.newcard.CardRarity;
import com.laigu.laigu.duel.newcard.DuelCard;
import com.laigu.laigu.duel.newcard.OnActivation;

/**
 * 激活·江山（金质）：激活进度达到 2 → +4 倍率，进度清零。
 * 焕章：我方有卡被激活时 +5 额外分（含本卡自身；每次激活事件计一次）。
 */
public final class QianLiJiangShanGoldCard implements DuelCard, OnActivation
{
    @Override public String id() { return "qian_li_jiang_shan_gold"; }
    @Override public String displayName() { return ArtifactCardNames.variantName("qian_li_jiang_shan", rarity()); }
    @Override public CardClass cardClass() { return CardClass.DING; }
    @Override public CardRarity rarity() { return CardRarity.GOLD; }

    @Override public int activationThreshold() { return 2; }

    @Override public void onActivation(CardContext context)
    {
        context.addTimingMult(multBonus());
        context.selfState().setActivation(0);
        context.emit(new AnimationEvent(AnimationEvent.Type.CARD_ACTIVATE, context.side(), context.slot(), id()));
        context.emit(new AnimationEvent(AnimationEvent.Type.MULTIPLIER_POPUP, context.side(), context.slot(), id()));
    }

    /** 焕章：监听己方任意激活事件（事件由派发器带目标场位广播）。 */
    @Override public void onEvent(BattleEvent event, CardContext context)
    {
        if (event.type() == BattleEvent.Type.ACTIVATION && event.side() == context.side())
            context.addTimingExtra(activatedBonus());
    }

    int multBonus() { return 4; }
    int activatedBonus() { return 5; }
}
