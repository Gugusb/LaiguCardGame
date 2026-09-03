package com.laigu.laigu.duel.newcard.cards;

import com.laigu.laigu.duel.CardClass;
import com.laigu.laigu.duel.newcard.ArtifactCardNames;
import com.laigu.laigu.duel.newcard.CardRarity;
import com.laigu.laigu.duel.newcard.DuelCard;
import com.laigu.laigu.duel.newcard.OnSettlement;
import com.laigu.laigu.duel.newcard.SettlementContext;

/** 余烬·贾湖骨笛（金质）：每张未抽卡组牌 → +2 基础分。 */
public final class JiaHuGuDiGoldCard implements DuelCard, OnSettlement
{
    @Override public String id() { return "jia_hu_gu_di_gold"; }
    @Override public String displayName() { return ArtifactCardNames.variantName("jia_hu_gu_di", rarity()); }
    @Override public CardClass cardClass() { return CardClass.DING; }
    @Override public CardRarity rarity() { return CardRarity.GOLD; }

    @Override public void onSettlement(SettlementContext context)
    {
        context.addBaseScaled(perDeckBase() * context.deckSize());
    }

    int perDeckBase() { return 2; }
}
