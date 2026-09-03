package com.laigu.laigu.duel.newcard.cards;

import com.laigu.laigu.duel.CardClass;
import com.laigu.laigu.duel.newcard.ArtifactCardNames;
import com.laigu.laigu.duel.newcard.CardRarity;
import com.laigu.laigu.duel.newcard.DuelCard;
import com.laigu.laigu.duel.newcard.OnSettlement;
import com.laigu.laigu.duel.newcard.SettlementContext;

/** 铜车马普通版：本卡每有 1 颗骰 → +6 额外分。 */
public final class TongCheMaCommonCard implements DuelCard, OnSettlement
{
    @Override public String id() { return "tong_che_ma_common"; }
    @Override public String displayName() { return ArtifactCardNames.variantName("tong_che_ma", rarity()); }
    @Override public CardClass cardClass() { return CardClass.GONG; }
    @Override public CardRarity rarity() { return CardRarity.COMMON; }

    @Override public void onSettlement(SettlementContext context)
    {
        context.addExtra(perDieBonus() * context.selfDice().size());
    }

    int perDieBonus() { return 6; }
}
