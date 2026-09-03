package com.laigu.laigu.duel.newcard.cards;

import com.laigu.laigu.duel.CardClass;
import com.laigu.laigu.duel.newcard.ArtifactCardNames;
import com.laigu.laigu.duel.newcard.CardRarity;
import com.laigu.laigu.duel.newcard.DuelCard;
import com.laigu.laigu.duel.newcard.OnSettlement;
import com.laigu.laigu.duel.newcard.SettlementContext;

/**
 * 万工轿（普通）：本卡每颗匹配骰 +12 额外分，每回合切换（奇数轮看奇数骰、偶数轮看偶数骰）。
 * 万工轿金焕章的视骰生效时，全部骰按当轮模式计。
 */
public final class WanGongJiaoCommonCard implements DuelCard, OnSettlement
{
    @Override public String id() { return "wan_gong_jiao_common"; }
    @Override public String displayName() { return ArtifactCardNames.variantName("wan_gong_jiao", rarity()); }
    @Override public CardClass cardClass() { return CardClass.GONG; }
    @Override public CardRarity rarity() { return CardRarity.COMMON; }

    @Override public void onSettlement(SettlementContext context)
    {
        context.addExtra(matchingDice(context) * perDieBonus());
    }

    /** 视骰生效 → 全部骰计入；否则按当轮奇偶模式数骰。 */
    static int matchingDice(SettlementContext context)
    {
        if (context.parityViewActive()) return context.selfDice().size();
        boolean countOdd = context.round() % 2 == 1;
        return (int) context.selfDice().stream().filter(v -> (v % 2 == 1) == countOdd).count();
    }

    int perDieBonus() { return 12; }
}
