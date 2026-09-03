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
 * 破阵·鸟尊（金质）：破阵成功 → 削弱对手该槽位 100%；
 * 焕章：破阵成功时，回合结束破坏对位卡牌。
 */
public final class JinHouNiaoZunGoldCard implements DuelCard, NewSettlementCalculator.PoZhenHandler, OnPoZhen, OnSettlement
{
    @Override public String id() { return "jin_hou_niao_zun_gold"; }
    @Override public String displayName() { return ArtifactCardNames.variantName("jin_hou_niao_zun", rarity()); }
    @Override public CardClass cardClass() { return CardClass.GONG; }
    @Override public CardRarity rarity() { return CardRarity.GOLD; }

    @Override public boolean poZhenAlwaysSuccess() { return false; }
    @Override public boolean poZhenFullHalve() { return true; }

    @Override public void onPoZhen(CardContext context)
    {
        context.emit(new AnimationEvent(AnimationEvent.Type.CARD_TRIGGER, context.side(), context.slot(), id()));
    }

    @Override public void onSettlement(SettlementContext context)
    {
        if (context.poZhenSuccess())
        {
            context.markOppositeDestroyAtRoundEnd();
            context.emit(new AnimationEvent(AnimationEvent.Type.CARD_DESTROY_MARK,
                    1 - context.side(), context.slot(), id()));
        }
    }
}
