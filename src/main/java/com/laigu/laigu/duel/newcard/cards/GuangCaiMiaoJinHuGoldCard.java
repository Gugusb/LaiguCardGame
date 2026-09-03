package com.laigu.laigu.duel.newcard.cards;

import com.laigu.laigu.duel.CardClass;
import com.laigu.laigu.duel.newcard.AnimationEvent;
import com.laigu.laigu.duel.newcard.ArtifactCardNames;
import com.laigu.laigu.duel.newcard.CardContext;
import com.laigu.laigu.duel.newcard.CardRarity;
import com.laigu.laigu.duel.newcard.DuelCard;
import com.laigu.laigu.duel.newcard.OnActivation;

/**
 * 激活·描金壶（金质）：激活进度达到 1 → +6 基础分，进度清零。
 * 焕章：达成时额外获得等同于发动时我方当前基础分的额外分（用户拍板：非最终值）。
 */
public final class GuangCaiMiaoJinHuGoldCard implements DuelCard, OnActivation
{
    @Override public String id() { return "guang_cai_miao_jin_hu_gold"; }
    @Override public String displayName() { return ArtifactCardNames.variantName("guang_cai_miao_jin_hu", rarity()); }
    @Override public CardClass cardClass() { return CardClass.DING; }
    @Override public CardRarity rarity() { return CardRarity.GOLD; }

    @Override public int activationThreshold() { return 1; }

    @Override public void onActivation(CardContext context)
    {
        context.addTimingBase(baseBonus());
        // 焕章：以发动瞬间我方基础分（含先前卡牌贡献，非最终）作为额外分。
        context.addTimingExtra(context.ownBaseScore());
        context.selfState().setActivation(0);
        context.emit(new AnimationEvent(AnimationEvent.Type.CARD_ACTIVATE, context.side(), context.slot(), id()));
        context.emit(new AnimationEvent(AnimationEvent.Type.SCORE_POPUP, context.side(), context.slot(), id()));
    }

    int baseBonus() { return 6; }
}
