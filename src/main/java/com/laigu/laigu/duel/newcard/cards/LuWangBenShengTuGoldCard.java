package com.laigu.laigu.duel.newcard.cards;

import com.laigu.laigu.duel.CardClass;
import com.laigu.laigu.duel.newcard.ArtifactCardNames;
import com.laigu.laigu.duel.newcard.CardContext;
import com.laigu.laigu.duel.newcard.CardRarity;
import com.laigu.laigu.duel.newcard.DuelCard;
import com.laigu.laigu.duel.newcard.OnSettlement;
import com.laigu.laigu.duel.newcard.OnSummon;
import com.laigu.laigu.duel.newcard.SettlementContext;

/**
 * 激活右·朝代（金质）：结算时激活右侧卡牌 2x 次（x = 己方场上单一朝代最大卡牌数；用户拍板 2×x）。
 * 焕章：入场时本回合我方所有卡牌视为最左侧卡牌的朝代。
 */
public final class LuWangBenShengTuGoldCard implements DuelCard, OnSettlement, OnSummon
{
    @Override public String id() { return "lu_wang_ben_sheng_tu_gold"; }
    @Override public String displayName() { return ArtifactCardNames.variantName("lu_wang_ben_sheng_tu", rarity()); }
    @Override public CardClass cardClass() { return CardClass.MOU; }
    @Override public CardRarity rarity() { return CardRarity.GOLD; }

    @Override public void onSettlement(SettlementContext context)
    {
        context.activateRightCard(2 * dynastyActivations(context));
    }

    /** 焕章：本回合己方所有卡视为最左侧卡的朝代（结算器 dynastyAt 统一生效）。 */
    @Override public void onSummon(CardContext context) { context.setDynastyViewToLeftmost(); }

    int dynastyActivations(SettlementContext context) { return context.ownMaxDynastyCount(); }
}
