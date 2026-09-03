package com.laigu.laigu.duel.newcard.cards;

import com.laigu.laigu.duel.CardClass;
import com.laigu.laigu.duel.newcard.ArtifactCardNames;
import com.laigu.laigu.duel.newcard.CardRarity;
import com.laigu.laigu.duel.newcard.DuelCard;
import com.laigu.laigu.duel.newcard.OnSettlement;
import com.laigu.laigu.duel.newcard.SettlementContext;

/** 五星出东方（普通）：本卡每颗 ≤3 骰 → +12/+24 额外分。 */
public final class WuXingChuDongFangCommonCard implements DuelCard, OnSettlement
{
    @Override public String id() { return "wu_xing_chu_dong_fang_common"; }
    @Override public String displayName() { return ArtifactCardNames.variantName("wu_xing_chu_dong_fang", rarity()); }
    @Override public CardClass cardClass() { return CardClass.GONG; }
    @Override public CardRarity rarity() { return CardRarity.COMMON; }

    @Override public void onSettlement(SettlementContext context)
    {
        int hits = (int) context.selfDice().stream().filter(v -> v <= 3).count();
        context.addExtra(hits * 12);
    }
}
