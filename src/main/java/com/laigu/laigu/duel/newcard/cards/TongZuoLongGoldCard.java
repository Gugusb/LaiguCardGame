package com.laigu.laigu.duel.newcard.cards;

import com.laigu.laigu.duel.CardClass;
import com.laigu.laigu.duel.newcard.ArtifactCardNames;
import com.laigu.laigu.duel.newcard.CardRarity;
import com.laigu.laigu.duel.newcard.DuelCard;
import com.laigu.laigu.duel.newcard.OnSettlement;
import com.laigu.laigu.duel.newcard.SettlementContext;

/** 金铢·铜坐龙（金质）：每张金质卡 → +40 额外分。 */
public final class TongZuoLongGoldCard implements DuelCard, OnSettlement
{
    @Override public String id() { return "tong_zuo_long_gold"; }
    @Override public String displayName() { return ArtifactCardNames.variantName("tong_zuo_long", rarity()); }
    @Override public CardClass cardClass() { return CardClass.DING; }
    @Override public CardRarity rarity() { return CardRarity.GOLD; }

    @Override public void onSettlement(SettlementContext context)
    {
        context.addExtraScaled(perGoldExtra() * (int) context.ownGoldCount());
    }

    int perGoldExtra() { return 40; }
}
