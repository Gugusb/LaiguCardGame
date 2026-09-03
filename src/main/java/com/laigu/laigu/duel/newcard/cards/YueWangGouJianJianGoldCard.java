package com.laigu.laigu.duel.newcard.cards;

import com.laigu.laigu.duel.CardClass;
import com.laigu.laigu.duel.newcard.AnimationEvent;
import com.laigu.laigu.duel.newcard.ArtifactCardNames;
import com.laigu.laigu.duel.newcard.CardContext;
import com.laigu.laigu.duel.newcard.CardRarity;
import com.laigu.laigu.duel.newcard.DuelCard;
import com.laigu.laigu.duel.newcard.NewSettlementCalculator;
import com.laigu.laigu.duel.newcard.OnPoZhen;
import com.laigu.laigu.duel.newcard.OnSettlement;
import com.laigu.laigu.duel.newcard.SettlementContext;

/**
 * 破阵·越剑（金质）：破阵成功 → 削弱对手该槽位 100%；
 * 首次破阵成功后本卡恒定破阵成功（旧引擎 poZhenAlwaysSuccess，持久状态）。
 * 对齐清单：金卡无破阵额外分词条（恒定成功即焕章之外的全部）。
 */
public final class YueWangGouJianJianGoldCard implements DuelCard, NewSettlementCalculator.PoZhenHandler, OnPoZhen, OnSettlement
{
    private static final String ALWAYS_SUCCESS = NewSettlementCalculator.PoZhenHandler.PERSISTENT_SUCCESS_KEY;

    @Override public String id() { return "yue_wang_gou_jian_jian_gold"; }
    @Override public String displayName() { return ArtifactCardNames.variantName("yue_wang_gou_jian_jian", rarity()); }
    @Override public CardClass cardClass() { return CardClass.GONG; }
    @Override public CardRarity rarity() { return CardRarity.GOLD; }

    @Override public boolean poZhenAlwaysSuccess() { return false; }
    @Override public boolean poZhenFullHalve() { return false; }
    @Override public boolean poZhenPersistentSuccess(com.laigu.laigu.duel.newcard.CardRuntimeState self)
    {
        return self.counter(ALWAYS_SUCCESS) > 0;
    }

    @Override public void onPoZhen(CardContext context)
    {
        // 首次成功后置恒定成功标记（结算器通过计数器读取，对齐旧引擎的 FieldCard 持久字段）。
        context.selfState().setCounter(ALWAYS_SUCCESS, 1);
        context.emit(new AnimationEvent(AnimationEvent.Type.CARD_TRIGGER, context.side(), context.slot(), id()));
    }

    @Override public void onSettlement(SettlementContext context)
    {
        // 破阵判定与削弱由结算器统一处理（PoZhenHandler），无额外结算词条。
    }
}
