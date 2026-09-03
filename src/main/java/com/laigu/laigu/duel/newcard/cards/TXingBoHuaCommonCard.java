package com.laigu.laigu.duel.newcard.cards;

import com.laigu.laigu.duel.CardClass;
import com.laigu.laigu.duel.newcard.ArtifactCardNames;
import com.laigu.laigu.duel.newcard.CardRarity;
import com.laigu.laigu.duel.newcard.DuelCard;
import com.laigu.laigu.duel.newcard.OnSettlement;
import com.laigu.laigu.duel.newcard.SettlementContext;

/** T形帛画（普通）：骰子点数都相同时，第 N 颗骰 +4×2^(N-1) 额外分。 */
public final class TXingBoHuaCommonCard implements DuelCard, OnSettlement
{
    @Override public String id() { return "t_xing_bo_hua_common"; }
    @Override public String displayName() { return ArtifactCardNames.variantName("t_xing_bo_hua", rarity()); }
    @Override public CardClass cardClass() { return CardClass.GONG; }
    @Override public CardRarity rarity() { return CardRarity.COMMON; }

    @Override public void onSettlement(SettlementContext context)
    {
        java.util.List<Integer> dice = context.selfDice();
        if (dice.isEmpty() || !allSamePoints(dice)) return;
        for (int i = 0; i < dice.size(); i++) context.addExtra(firstDieBonus() * (int) Math.pow(2, i));
    }

    /** 清单条件：本卡上骰子点数都相同。 */
    static boolean allSamePoints(java.util.List<Integer> dice)
    {
        return dice.stream().distinct().count() == 1;
    }

    int firstDieBonus() { return 4; }
}
