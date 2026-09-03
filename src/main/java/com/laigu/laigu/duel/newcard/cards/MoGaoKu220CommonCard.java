package com.laigu.laigu.duel.newcard.cards;

import com.laigu.laigu.duel.CardClass;
import com.laigu.laigu.duel.newcard.ArtifactCardNames;
import com.laigu.laigu.duel.newcard.CardContext;
import com.laigu.laigu.duel.newcard.CardRarity;
import com.laigu.laigu.duel.newcard.DuelCard;
import com.laigu.laigu.duel.newcard.OnRoundStart;

/** 承前·莫高窟220窟（普通）：每轮开始时抽 1 张牌。 */
public final class MoGaoKu220CommonCard implements DuelCard, OnRoundStart
{
    @Override public String id() { return "mo_gao_ku_220_common"; }
    @Override public String displayName() { return ArtifactCardNames.variantName("mo_gao_ku_220", rarity()); }
    @Override public CardClass cardClass() { return CardClass.DING; }
    @Override public CardRarity rarity() { return CardRarity.COMMON; }

    @Override public void onRoundStart(CardContext context)
    {
        context.drawCards(roundStartDraw());
    }

    int roundStartDraw() { return 1; }
}
