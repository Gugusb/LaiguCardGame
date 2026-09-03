package com.laigu.laigu.duel.newcard.cards;

import com.laigu.laigu.duel.CardClass;
import com.laigu.laigu.duel.newcard.ArtifactCardNames;
import com.laigu.laigu.duel.newcard.CardRarity;
import com.laigu.laigu.duel.newcard.DuelCard;
import com.laigu.laigu.duel.newcard.OnSettlement;
import com.laigu.laigu.duel.newcard.SettlementContext;

/** 土形陶埙（普通）：本卡至少 1 颗骰 → +12/+24 额外分。 */
public final class TuXingTaoXunCommonCard implements DuelCard, OnSettlement
{
    @Override public String id() { return "tu_xing_tao_xun_common"; }
    @Override public String displayName() { return ArtifactCardNames.variantName("tu_xing_tao_xun", rarity()); }
    @Override public CardClass cardClass() { return CardClass.GONG; }
    @Override public CardRarity rarity() { return CardRarity.COMMON; }

    @Override public void onSettlement(SettlementContext context)
    {
        if (!context.selfDice().isEmpty()) context.addExtra(12);
    }
}
