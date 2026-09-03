package com.laigu.laigu.duel.newcard.cards;

import com.laigu.laigu.duel.CardClass;
import com.laigu.laigu.duel.newcard.AnimationEvent;
import com.laigu.laigu.duel.newcard.ArtifactCardNames;
import com.laigu.laigu.duel.newcard.CardContext;
import com.laigu.laigu.duel.newcard.CardRarity;
import com.laigu.laigu.duel.newcard.DuelCard;
import com.laigu.laigu.duel.newcard.OnActivation;
import com.laigu.laigu.duel.newcard.OnSummon;

/**
 * 激活·编钟（金质）：激活进度达到 3 → 每颗骰 +2 倍率，进度清零。
 * 入场时本回合抓骰次数 +1（确认稿 Q7 拍板，一次性）。
 */
public final class ZengHouYiBianZhongGoldCard implements DuelCard, OnActivation, OnSummon
{
    @Override public String id() { return "zeng_hou_yi_bian_zhong_gold"; }
    @Override public String displayName() { return ArtifactCardNames.variantName("zeng_hou_yi_bian_zhong", rarity()); }
    @Override public CardClass cardClass() { return CardClass.DING; }
    @Override public CardRarity rarity() { return CardRarity.GOLD; }

    @Override public int activationThreshold() { return 3; }

    @Override public void onActivation(CardContext context)
    {
        context.addTimingMult(perDieMult() * context.selfDice().size());
        context.selfState().setActivation(0);
        context.emit(new AnimationEvent(AnimationEvent.Type.CARD_ACTIVATE, context.side(), context.slot(), id()));
        context.emit(new AnimationEvent(AnimationEvent.Type.MULTIPLIER_POPUP, context.side(), context.slot(), id()));
    }

    int perDieMult() { return 2; }

    /** 确认稿 Q7 拍板：入场时本回合抓骰次数 +1（一次性，下回合正常）。 */
    @Override public void onSummon(CardContext context) { context.addDraftTurnBonusSelf(1); }
}
