package com.laigu.laigu.duel.newcard.cards;

import com.laigu.laigu.duel.CardClass;
import com.laigu.laigu.duel.newcard.ArtifactCardNames;
import com.laigu.laigu.duel.newcard.CardRarity;
import com.laigu.laigu.duel.newcard.DuelCard;
import com.laigu.laigu.duel.newcard.OnSettlement;
import com.laigu.laigu.duel.newcard.SettlementContext;

/** 三兔藻井（金质）：本卡每颗偶数骰 → +8/+16 额外分。 */
public final class SanTuZaoJingGoldCard implements DuelCard, OnSettlement
{
    @Override public String id() { return "san_tu_zao_jing_gold"; }
    @Override public String displayName() { return ArtifactCardNames.variantName("san_tu_zao_jing", rarity()); }
    @Override public CardClass cardClass() { return CardClass.GONG; }
    @Override public CardRarity rarity() { return CardRarity.GOLD; }

    @Override public void onSettlement(SettlementContext context)
    {
        int hits = (int) context.selfDice().stream().filter(v -> v % 2 == 0).count();
        context.addExtra(hits * 16);
    }
}
