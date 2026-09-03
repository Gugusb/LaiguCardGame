package com.laigu.laigu.duel.newcard.cards;

import com.laigu.laigu.duel.CardClass;
import com.laigu.laigu.duel.newcard.ArtifactCardNames;
import com.laigu.laigu.duel.newcard.CardRarity;
import com.laigu.laigu.duel.newcard.DuelCard;
import com.laigu.laigu.duel.newcard.OnSettlement;
import com.laigu.laigu.duel.newcard.SettlementContext;

/** 万壑松风图（普通）：无条件 +10 额外分。 */
public final class WanHeSongFengTuCommonCard implements DuelCard, OnSettlement
{
    @Override public String id() { return "wan_he_song_feng_tu_common"; }
    @Override public String displayName() { return ArtifactCardNames.variantName("wan_he_song_feng_tu", rarity()); }
    @Override public CardClass cardClass() { return CardClass.SHOU; }
    @Override public CardRarity rarity() { return CardRarity.COMMON; }

    @Override public void onSettlement(SettlementContext context)
    {
        context.addExtra(10);
    }
}
