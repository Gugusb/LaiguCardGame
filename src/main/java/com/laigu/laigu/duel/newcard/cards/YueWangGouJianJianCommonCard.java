package com.laigu.laigu.duel.newcard.cards;

import com.laigu.laigu.duel.CardClass;
import com.laigu.laigu.duel.newcard.AnimationEvent;
import com.laigu.laigu.duel.newcard.ArtifactCardNames;
import com.laigu.laigu.duel.newcard.CardContext;
import com.laigu.laigu.duel.newcard.CardRarity;
import com.laigu.laigu.duel.newcard.DuelCard;
import com.laigu.laigu.duel.newcard.NewSettlementCalculator;
import com.laigu.laigu.duel.newcard.OnPoZhen;

/** 破阵·越剑（普通）：破阵成功（本槽骰面和 > 对手同槽）→ 削弱对手该槽位 50%。 */
public final class YueWangGouJianJianCommonCard implements DuelCard, NewSettlementCalculator.PoZhenHandler, OnPoZhen
{
    @Override public String id() { return "yue_wang_gou_jian_jian_common"; }
    @Override public String displayName() { return ArtifactCardNames.variantName("yue_wang_gou_jian_jian", rarity()); }
    @Override public CardClass cardClass() { return CardClass.GONG; }
    @Override public CardRarity rarity() { return CardRarity.COMMON; }

    @Override public boolean poZhenAlwaysSuccess() { return false; }
    @Override public boolean poZhenFullHalve() { return false; }

    @Override public void onPoZhen(CardContext context)
    {
        context.emit(new AnimationEvent(AnimationEvent.Type.CARD_TRIGGER, context.side(), context.slot(), id()));
    }
}
