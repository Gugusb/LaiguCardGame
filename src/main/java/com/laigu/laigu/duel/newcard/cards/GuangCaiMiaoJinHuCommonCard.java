package com.laigu.laigu.duel.newcard.cards;

import com.laigu.laigu.duel.CardClass;
import com.laigu.laigu.duel.newcard.AnimationEvent;
import com.laigu.laigu.duel.newcard.ArtifactCardNames;
import com.laigu.laigu.duel.newcard.CardContext;
import com.laigu.laigu.duel.newcard.CardRarity;
import com.laigu.laigu.duel.newcard.DuelCard;
import com.laigu.laigu.duel.newcard.OnActivation;

/** 激活·描金壶（普通）：激活进度达到 1 → +3 基础分，进度清零。 */
public final class GuangCaiMiaoJinHuCommonCard implements DuelCard, OnActivation
{
    @Override public String id() { return "guang_cai_miao_jin_hu_common"; }
    @Override public String displayName() { return ArtifactCardNames.variantName("guang_cai_miao_jin_hu", rarity()); }
    @Override public CardClass cardClass() { return CardClass.DING; }
    @Override public CardRarity rarity() { return CardRarity.COMMON; }

    @Override public int activationThreshold() { return 1; }

    @Override public void onActivation(CardContext context)
    {
        context.addTimingBase(baseBonus());
        context.selfState().setActivation(0);
        context.emit(new AnimationEvent(AnimationEvent.Type.CARD_ACTIVATE, context.side(), context.slot(), id()));
        context.emit(new AnimationEvent(AnimationEvent.Type.SCORE_POPUP, context.side(), context.slot(), id()));
    }

    int baseBonus() { return 3; }
}
