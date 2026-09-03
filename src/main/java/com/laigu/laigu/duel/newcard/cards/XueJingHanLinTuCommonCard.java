package com.laigu.laigu.duel.newcard.cards;

import com.laigu.laigu.duel.CardClass;
import com.laigu.laigu.duel.newcard.ArtifactCardNames;
import com.laigu.laigu.duel.newcard.CardRarity;
import com.laigu.laigu.duel.newcard.DuelCard;
import com.laigu.laigu.duel.newcard.OnSettlement;
import com.laigu.laigu.duel.newcard.SettlementContext;

/** 泰然·雪景翰林图（普通）：我方每有 1 张鼎·盛卡 → +1 倍率。 */
public final class XueJingHanLinTuCommonCard implements DuelCard, OnSettlement
{
    @Override public String id() { return "xue_jing_han_lin_tu_common"; }
    @Override public String displayName() { return ArtifactCardNames.variantName("xue_jing_han_lin_tu", rarity()); }
    @Override public CardClass cardClass() { return CardClass.DING; }
    @Override public CardRarity rarity() { return CardRarity.COMMON; }

    @Override public void onSettlement(SettlementContext context)
    {
        context.addMultiplierScaled(perDingCard() * context.ownClassCount(CardClass.DING));
    }

    int perDingCard() { return 1; }
}
