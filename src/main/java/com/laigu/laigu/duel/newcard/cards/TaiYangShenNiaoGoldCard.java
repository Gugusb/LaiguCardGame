package com.laigu.laigu.duel.newcard.cards;

import com.laigu.laigu.duel.CardClass;
import com.laigu.laigu.duel.newcard.ArtifactCardNames;
import com.laigu.laigu.duel.newcard.CardRarity;
import com.laigu.laigu.duel.newcard.DuelCard;
import com.laigu.laigu.duel.newcard.OnSettlement;
import com.laigu.laigu.duel.newcard.SettlementContext;

/** 盛世·太阳神鸟（金质）：我方每有 1 张金质卡 → +2 倍率。 */
public final class TaiYangShenNiaoGoldCard implements DuelCard, OnSettlement
{
    @Override public String id() { return "tai_yang_shen_niao_gold"; }
    @Override public String displayName() { return ArtifactCardNames.variantName("tai_yang_shen_niao", rarity()); }
    @Override public CardClass cardClass() { return CardClass.DING; }
    @Override public CardRarity rarity() { return CardRarity.GOLD; }

    @Override public void onSettlement(SettlementContext context)
    {
        context.addMultiplierScaled(perGoldMult() * (int) context.ownGoldCount());
    }

    int perGoldMult() { return 2; }
}
