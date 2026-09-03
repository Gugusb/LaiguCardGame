package com.laigu.laigu.duel.newcard.cards;

import com.laigu.laigu.duel.CardClass;
import com.laigu.laigu.duel.newcard.AnimationEvent;
import com.laigu.laigu.duel.newcard.ArtifactCardNames;
import com.laigu.laigu.duel.newcard.CardContext;
import com.laigu.laigu.duel.newcard.CardRarity;
import com.laigu.laigu.duel.newcard.DuelCard;
import com.laigu.laigu.duel.newcard.OnAmbushSuccess;

/**
 * 伏击·百花（金质）：伏击成功 → 复制对位骰子给自己（每卡上限5颗；失败无收益）。
 * 焕章：复制后本卡骰子数大于4时，+5基础分、+5倍率、+5额外分。
 */
public final class BaiHuaTuJuanGoldCard implements DuelCard, OnAmbushSuccess
{
    private static final int MAX_DICE_PER_CARD = 5;

    @Override public String id() { return "bai_hua_tu_juan_gold"; }
    @Override public String displayName() { return ArtifactCardNames.variantName("bai_hua_tu_juan", rarity()); }
    @Override public CardClass cardClass() { return CardClass.MOU; }
    @Override public CardRarity rarity() { return CardRarity.GOLD; }

    @Override public void onAmbushSuccess(CardContext context)
    {
        // 复制对位骰子（实态桥回写真实 FieldCard；此处先写影子，容量对齐旧 MAX_DICE_PER_CARD=5）。
        for (int die : context.oppositeState().map(CardRuntimeDice::of).orElse(java.util.List.of()))
            if (context.selfDice().size() < MAX_DICE_PER_CARD) context.addSelfDie(die);
        // 焕章：复制后骰子数 > 4 → 三项各 +5。
        if (context.selfDice().size() > 4)
        {
            context.addTimingBase(5);
            context.addTimingMult(5);
            context.addTimingExtra(5);
        }
        context.emit(new AnimationEvent(AnimationEvent.Type.CARD_TRIGGER, context.side(), context.slot(), id()));
    }
}

/** 小工具：从 Optional<CardRuntimeState> 提取骰面。 */
final class CardRuntimeDice
{
    private CardRuntimeDice() {}
    static java.util.List<Integer> of(com.laigu.laigu.duel.newcard.CardRuntimeState state)
    {
        return state == null ? java.util.List.of() : state.activeDice();
    }
}
