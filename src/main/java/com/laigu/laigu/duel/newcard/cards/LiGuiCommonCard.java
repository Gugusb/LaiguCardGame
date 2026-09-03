package com.laigu.laigu.duel.newcard.cards;

import com.laigu.laigu.duel.CardClass;
import com.laigu.laigu.duel.newcard.ArtifactCardNames;
import com.laigu.laigu.duel.newcard.CardRarity;
import com.laigu.laigu.duel.newcard.DuelCard;
import com.laigu.laigu.duel.newcard.OnSettlement;
import com.laigu.laigu.duel.newcard.SettlementContext;

import java.util.List;

/** 离卦普通版：本卡 2 颗骰相同 → +2 倍率。 */
public final class LiGuiCommonCard implements DuelCard, OnSettlement
{
    @Override public String id() { return "li_gui_common"; }
    @Override public String displayName() { return ArtifactCardNames.variantName("li_gui", rarity()); }
    @Override public CardClass cardClass() { return CardClass.GONG; }
    @Override public CardRarity rarity() { return CardRarity.COMMON; }

    @Override public void onSettlement(SettlementContext context)
    {
        List<Integer> dice = context.selfDice();
        if (dice.size() == 2 && dice.get(0).equals(dice.get(1))) context.addMultiplier(pairBonus());
    }

    int pairBonus() { return 2; }
}
