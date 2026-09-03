package com.laigu.laigu.duel.newcard.cards;

import com.laigu.laigu.duel.CardClass;
import com.laigu.laigu.duel.newcard.ArtifactCardNames;
import com.laigu.laigu.duel.newcard.CardRarity;
import com.laigu.laigu.duel.newcard.DuelCard;
import com.laigu.laigu.duel.newcard.OnSettlement;
import com.laigu.laigu.duel.newcard.SettlementContext;

import java.util.List;

/** 离卦金质版：本卡 2 颗骰相同 → +4 倍率。 */
public final class LiGuiGoldCard implements DuelCard, OnSettlement
{
    @Override public String id() { return "li_gui_gold"; }
    @Override public String displayName() { return ArtifactCardNames.variantName("li_gui", rarity()); }
    @Override public CardClass cardClass() { return CardClass.GONG; }
    @Override public CardRarity rarity() { return CardRarity.GOLD; }

    @Override public void onSettlement(SettlementContext context)
    {
        List<Integer> dice = context.selfDice();
        if (dice.size() == 2 && dice.get(0).equals(dice.get(1))) context.addMultiplier(pairBonus());
    }

    int pairBonus() { return 4; }
}
