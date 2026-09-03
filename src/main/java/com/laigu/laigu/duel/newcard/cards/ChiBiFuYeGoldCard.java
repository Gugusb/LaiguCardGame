package com.laigu.laigu.duel.newcard.cards;

import com.laigu.laigu.duel.CardClass;
import com.laigu.laigu.duel.newcard.ArtifactCardNames;
import com.laigu.laigu.duel.newcard.CardRarity;
import com.laigu.laigu.duel.newcard.DuelCard;
import com.laigu.laigu.duel.newcard.OnSettlement;
import com.laigu.laigu.duel.newcard.SettlementContext;

/**
 * 焚野·赤壁赋页（金质）：本卡每有 1 颗骰 → +4 基础分。
 * 焕章：本卡每有 1 颗骰 → +4 额外分。
 */
public final class ChiBiFuYeGoldCard implements DuelCard, OnSettlement
{
    @Override public String id() { return "chi_bi_fu_ye_gold"; }
    @Override public String displayName() { return ArtifactCardNames.variantName("chi_bi_fu_ye", rarity()); }
    @Override public CardClass cardClass() { return CardClass.GONG; }
    @Override public CardRarity rarity() { return CardRarity.GOLD; }

    @Override public void onSettlement(SettlementContext context)
    {
        context.addBaseScaled(perDieBonus() * context.selfDice().size());
        // 焕章：每颗骰 +4 额外分。
        context.addExtraScaled(perDieExtra() * context.selfDice().size());
    }

    int perDieExtra() { return 4; }

    int perDieBonus() { return 4; }
}
