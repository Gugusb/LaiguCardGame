package com.laigu.laigu.duel.newcard.cards;

import com.laigu.laigu.duel.CardClass;
import com.laigu.laigu.duel.newcard.ArtifactCardNames;
import com.laigu.laigu.duel.newcard.CardRarity;
import com.laigu.laigu.duel.newcard.DuelCard;
import com.laigu.laigu.duel.newcard.OnSettlement;
import com.laigu.laigu.duel.newcard.SettlementContext;

/** 激活右·朝代（普通）：结算时激活右侧卡牌 x 次（x = 己方场上单一朝代最大卡牌数）。 */
public final class LuWangBenShengTuCommonCard implements DuelCard, OnSettlement
{
    @Override public String id() { return "lu_wang_ben_sheng_tu_common"; }
    @Override public String displayName() { return ArtifactCardNames.variantName("lu_wang_ben_sheng_tu", rarity()); }
    @Override public CardClass cardClass() { return CardClass.MOU; }
    @Override public CardRarity rarity() { return CardRarity.COMMON; }

    @Override public void onSettlement(SettlementContext context)
    {
        context.activateRightCard(dynastyActivations(context));
    }

    int dynastyActivations(SettlementContext context) { return context.ownMaxDynastyCount(); }
}
