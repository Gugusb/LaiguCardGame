package com.laigu.laigu.duel.newcard.cards;

import com.laigu.laigu.duel.CardClass;
import com.laigu.laigu.duel.newcard.ArtifactCardNames;
import com.laigu.laigu.duel.newcard.CardRarity;
import com.laigu.laigu.duel.newcard.DuelCard;
import com.laigu.laigu.duel.newcard.OnSettlement;
import com.laigu.laigu.duel.newcard.SettlementContext;

/** 商阳台帖（普通）：本卡骰子为 0 → +14/+28 额外分。 */
public final class ShangYangTaiTieCommonCard implements DuelCard, OnSettlement
{
    @Override public String id() { return "shang_yang_tai_tie_common"; }
    @Override public String displayName() { return ArtifactCardNames.variantName("shang_yang_tai_tie", rarity()); }
    @Override public CardClass cardClass() { return CardClass.SHOU; }
    @Override public CardRarity rarity() { return CardRarity.COMMON; }

    @Override public void onSettlement(SettlementContext context)
    {
        if (context.selfDice().isEmpty()) context.addExtra(14);
    }
}
