package com.laigu.laigu.duel.newcard.cards;

import com.laigu.laigu.duel.CardClass;
import com.laigu.laigu.duel.newcard.ArtifactCardNames;
import com.laigu.laigu.duel.newcard.CardRarity;
import com.laigu.laigu.duel.newcard.DuelCard;
import com.laigu.laigu.duel.newcard.OnSettlement;
import com.laigu.laigu.duel.newcard.SettlementContext;

/** 盛象·鎏金骑士（金质）：若你有金质卡 → +2 倍率。 */
public final class LiuJinQiShiGoldCard implements DuelCard, OnSettlement
{
    @Override public String id() { return "liu_jin_qi_shi_gold"; }
    @Override public String displayName() { return ArtifactCardNames.variantName("liu_jin_qi_shi", rarity()); }
    @Override public CardClass cardClass() { return CardClass.DING; }
    @Override public CardRarity rarity() { return CardRarity.GOLD; }

    @Override public void onSettlement(SettlementContext context)
    {
        if (context.ownGoldCount() > 0) context.addMultiplierScaled(hasGoldMult());
    }

    int hasGoldMult() { return 2; }
}
