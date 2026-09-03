package com.laigu.laigu.duel.newcard.cards;

import com.laigu.laigu.duel.CardClass;
import com.laigu.laigu.duel.newcard.ArtifactCardNames;
import com.laigu.laigu.duel.newcard.CardContext;
import com.laigu.laigu.duel.newcard.CardRarity;
import com.laigu.laigu.duel.newcard.DuelCard;
import com.laigu.laigu.duel.newcard.OnSettlement;
import com.laigu.laigu.duel.newcard.OnSummon;
import com.laigu.laigu.duel.newcard.SettlementContext;

/** T形帛画（金质）：骰子点数都相同时，第 N 颗骰 +4×3^(N-1) 额外分；入场时本回合抓骰次数 +1（确认稿 Q7 拍板）。 */
public final class TXingBoHuaGoldCard implements DuelCard, OnSettlement, OnSummon
{
    @Override public String id() { return "t_xing_bo_hua_gold"; }
    @Override public String displayName() { return ArtifactCardNames.variantName("t_xing_bo_hua", rarity()); }
    @Override public CardClass cardClass() { return CardClass.GONG; }
    @Override public CardRarity rarity() { return CardRarity.GOLD; }

    @Override public void onSettlement(SettlementContext context)
    {
        java.util.List<Integer> dice = context.selfDice();
        if (dice.isEmpty() || !TXingBoHuaCommonCard.allSamePoints(dice)) return;
        for (int i = 0; i < dice.size(); i++) context.addExtra(firstDieBonus() * (int) Math.pow(3, i));
    }

    int firstDieBonus() { return 4; }

    /** 确认稿 Q7 拍板：入场时本回合抓骰次数 +1（一次性，下回合正常）。 */
    @Override public void onSummon(CardContext context) { context.addDraftTurnBonusSelf(1); }
}
