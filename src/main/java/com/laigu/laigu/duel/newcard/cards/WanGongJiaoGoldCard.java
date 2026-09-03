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
 * 万工轿（金质）：本卡每颗奇数骰 +24 额外分，每回合切换（奇数轮看奇数骰、偶数轮看偶数骰）。
 * 焕章：入场时本回合所有骰子视为奇数/偶数（随当轮基础模式而定）。
 */
public final class WanGongJiaoGoldCard implements DuelCard, OnSettlement, OnSummon
{
    @Override public String id() { return "wan_gong_jiao_gold"; }
    @Override public String displayName() { return ArtifactCardNames.variantName("wan_gong_jiao", rarity()); }
    @Override public CardClass cardClass() { return CardClass.GONG; }
    @Override public CardRarity rarity() { return CardRarity.GOLD; }

    @Override public void onSettlement(SettlementContext context)
    {
        context.addExtra(WanGongJiaoCommonCard.matchingDice(context) * perDieBonus());
    }

    /** 焕章：入场设定视骰（随当轮基础模式）。 */
    @Override public void onSummon(CardContext context)
    {
        if (context.oddDiceRound()) context.setParityViewOdd();
        else context.setParityViewEven();
    }

    int perDieBonus() { return 24; }
}
