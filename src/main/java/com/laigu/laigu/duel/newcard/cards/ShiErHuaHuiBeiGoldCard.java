package com.laigu.laigu.duel.newcard.cards;

import com.laigu.laigu.duel.CardClass;
import com.laigu.laigu.duel.newcard.ArtifactCardNames;
import com.laigu.laigu.duel.newcard.CardRarity;
import com.laigu.laigu.duel.newcard.DuelCard;
import com.laigu.laigu.duel.newcard.OnSettlement;
import com.laigu.laigu.duel.newcard.SettlementContext;

/** 十二花卉杯（金质）：本卡骰面和为奇数 → +14/+28 额外分。 */
public final class ShiErHuaHuiBeiGoldCard implements DuelCard, OnSettlement
{
    @Override public String id() { return "shi_er_hua_hui_bei_gold"; }
    @Override public String displayName() { return ArtifactCardNames.variantName("shi_er_hua_hui_bei", rarity()); }
    @Override public CardClass cardClass() { return CardClass.MOU; }
    @Override public CardRarity rarity() { return CardRarity.GOLD; }

    @Override public void onSettlement(SettlementContext context)
    {
        if (context.selfDice().stream().mapToInt(Integer::intValue).sum() % 2 == 1) context.addExtra(28);
    }
}
