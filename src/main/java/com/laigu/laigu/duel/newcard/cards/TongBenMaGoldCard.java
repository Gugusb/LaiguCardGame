package com.laigu.laigu.duel.newcard.cards;

import com.laigu.laigu.duel.CardClass;
import com.laigu.laigu.duel.newcard.ArtifactCardNames;
import com.laigu.laigu.duel.newcard.CardRarity;
import com.laigu.laigu.duel.newcard.DuelCard;
import com.laigu.laigu.duel.newcard.OnSettlement;
import com.laigu.laigu.duel.newcard.SettlementContext;

/**
 * 千钧·铜奔马（金质）：本卡骰子 ≥2 颗 → +28 额外分。
 * 焕章：本卡有至少 5 个骰子时，+50 额外分。
 */
public final class TongBenMaGoldCard implements DuelCard, OnSettlement
{
    @Override public String id() { return "tong_ben_ma_gold"; }
    @Override public String displayName() { return ArtifactCardNames.variantName("tong_ben_ma", rarity()); }
    @Override public CardClass cardClass() { return CardClass.GONG; }
    @Override public CardRarity rarity() { return CardRarity.GOLD; }

    @Override public void onSettlement(SettlementContext context)
    {
        if (context.selfDice().size() >= diceThreshold()) context.addExtraScaled(bonus());
        // 焕章：至少 5 骰 +50 额外分。
        if (context.selfDice().size() >= fullDiceThreshold()) context.addExtraScaled(fullBonus());
    }

    int diceThreshold() { return 2; }

    int bonus() { return 28; }

    int fullDiceThreshold() { return 5; }

    int fullBonus() { return 50; }
}
