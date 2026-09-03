package com.laigu.laigu.duel.newcard.cards;

import com.laigu.laigu.duel.CardClass;
import com.laigu.laigu.duel.newcard.ArtifactCardNames;
import com.laigu.laigu.duel.newcard.CardRarity;
import com.laigu.laigu.duel.newcard.DuelCard;
import com.laigu.laigu.duel.newcard.OnSettlement;
import com.laigu.laigu.duel.newcard.SettlementContext;

/** 铜奔马普通版：本卡骰子 ≥2 颗 → +14 额外分。 */
public final class TongBenMaCommonCard implements DuelCard, OnSettlement
{
    @Override public String id() { return "tong_ben_ma_common"; }
    @Override public String displayName() { return ArtifactCardNames.variantName("tong_ben_ma", rarity()); }
    @Override public CardClass cardClass() { return CardClass.GONG; }
    @Override public CardRarity rarity() { return CardRarity.COMMON; }

    @Override public void onSettlement(SettlementContext context)
    {
        if (context.selfDice().size() >= diceThreshold()) context.addExtra(bonus());
    }

    int diceThreshold() { return 2; }

    int bonus() { return 14; }
}
