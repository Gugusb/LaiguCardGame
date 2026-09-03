package com.laigu.laigu.duel.newcard.cards;

import com.laigu.laigu.duel.CardClass;
import com.laigu.laigu.duel.newcard.ArtifactCardNames;
import com.laigu.laigu.duel.newcard.CardRarity;
import com.laigu.laigu.duel.newcard.DuelCard;
import com.laigu.laigu.duel.newcard.OnSettlement;
import com.laigu.laigu.duel.newcard.SettlementContext;

/** 赤壁赋页普通版：本卡每有 1 颗骰 → +2 基础分。 */
public final class ChiBiFuYeCommonCard implements DuelCard, OnSettlement
{
    @Override public String id() { return "chi_bi_fu_ye_common"; }
    @Override public String displayName() { return ArtifactCardNames.variantName("chi_bi_fu_ye", rarity()); }
    @Override public CardClass cardClass() { return CardClass.GONG; }
    @Override public CardRarity rarity() { return CardRarity.COMMON; }

    @Override public void onSettlement(SettlementContext context)
    {
        context.addBase(perDieBonus() * context.selfDice().size());
    }

    int perDieBonus() { return 2; }
}
