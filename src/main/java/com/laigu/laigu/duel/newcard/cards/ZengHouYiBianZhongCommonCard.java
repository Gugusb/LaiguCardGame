package com.laigu.laigu.duel.newcard.cards;

import com.laigu.laigu.duel.CardClass;
import com.laigu.laigu.duel.newcard.AnimationEvent;
import com.laigu.laigu.duel.newcard.ArtifactCardNames;
import com.laigu.laigu.duel.newcard.CardContext;
import com.laigu.laigu.duel.newcard.CardRarity;
import com.laigu.laigu.duel.newcard.DuelCard;
import com.laigu.laigu.duel.newcard.OnActivation;

/**
 * 激活·编钟（普通）：激活进度达到 3 → 每颗骰 +1 倍率（至少 1 骰），进度清零。
 * 按「id 重复以第 3 批词条为准」决策：编钟为激活卡，取代此前误迁移的消耗版实现。
 */
public final class ZengHouYiBianZhongCommonCard implements DuelCard, OnActivation
{
    @Override public String id() { return "zeng_hou_yi_bian_zhong_common"; }
    @Override public String displayName() { return ArtifactCardNames.variantName("zeng_hou_yi_bian_zhong", rarity()); }
    @Override public CardClass cardClass() { return CardClass.DING; }
    @Override public CardRarity rarity() { return CardRarity.COMMON; }

    @Override public int activationThreshold() { return 3; }

    @Override public void onActivation(CardContext context)
    {
        context.addTimingMult(perDieMult() * context.selfDice().size());
        context.selfState().setActivation(0);
        context.emit(new AnimationEvent(AnimationEvent.Type.CARD_ACTIVATE, context.side(), context.slot(), id()));
        context.emit(new AnimationEvent(AnimationEvent.Type.MULTIPLIER_POPUP, context.side(), context.slot(), id()));
    }

    int perDieMult() { return 1; }
}
