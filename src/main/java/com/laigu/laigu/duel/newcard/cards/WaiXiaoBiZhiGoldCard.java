package com.laigu.laigu.duel.newcard.cards;

import com.laigu.laigu.duel.CardClass;
import com.laigu.laigu.duel.newcard.ArtifactCardNames;
import com.laigu.laigu.duel.newcard.CardRarity;
import com.laigu.laigu.duel.newcard.DuelCard;
import com.laigu.laigu.duel.newcard.OnSettlement;
import com.laigu.laigu.duel.newcard.SettlementContext;

/** 外销壁纸金质版：对手场上满 5 张 → +40 额外分。 */
public final class WaiXiaoBiZhiGoldCard implements DuelCard, OnSettlement
{
    @Override public String id() { return "wai_xiao_bi_zhi_gold"; }
    @Override public String displayName() { return ArtifactCardNames.variantName("wai_xiao_bi_zhi", rarity()); }
    @Override public CardClass cardClass() { return CardClass.SHOU; }
    @Override public CardRarity rarity() { return CardRarity.GOLD; }

    @Override public void onSettlement(SettlementContext context)
    {
        if (context.opponentFieldCount() >= com.laigu.laigu.duel.DuelGame.FIELD_SLOTS) context.addExtra(fullFieldBonus());
    }

    int fullFieldBonus() { return 40; }
}
