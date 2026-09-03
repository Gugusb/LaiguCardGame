package com.laigu.laigu.duel.newcard.cards;

import com.laigu.laigu.duel.CardClass;
import com.laigu.laigu.duel.newcard.ArtifactCardNames;
import com.laigu.laigu.duel.newcard.CardContext;
import com.laigu.laigu.duel.newcard.CardRarity;
import com.laigu.laigu.duel.newcard.DuelCard;
import com.laigu.laigu.duel.newcard.OnSummon;

/** 观星·四灵四凤座（普通）：入场时回复 1 点行动力（对齐清单：无其他效果）。 */
public final class SiLongSiFengZuoCommonCard implements DuelCard, OnSummon
{
    @Override public String id() { return "si_long_si_feng_zuo_common"; }
    @Override public String displayName() { return ArtifactCardNames.variantName("si_long_si_feng_zuo", rarity()); }
    @Override public CardClass cardClass() { return CardClass.MOU; }
    @Override public CardRarity rarity() { return CardRarity.COMMON; }

    @Override public void onSummon(CardContext context)
    {
        context.restoreActionPoints(restoreAp());
    }

    int restoreAp() { return 1; }
}
