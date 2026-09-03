package com.laigu.laigu.duel.newcard.cards;

import com.laigu.laigu.duel.CardClass;
import com.laigu.laigu.duel.newcard.ArtifactCardNames;
import com.laigu.laigu.duel.newcard.CardRarity;
import com.laigu.laigu.duel.newcard.DuelCard;
import com.laigu.laigu.duel.newcard.OnSettlement;
import com.laigu.laigu.duel.newcard.SettlementContext;

/** 云雷纹大铙（金质）：本卡每颗 ≥4 骰 → +12/+24 额外分。 */
public final class YunLeiWenDaNaoGoldCard implements DuelCard, OnSettlement
{
    @Override public String id() { return "yun_lei_wen_da_nao_gold"; }
    @Override public String displayName() { return ArtifactCardNames.variantName("yun_lei_wen_da_nao", rarity()); }
    @Override public CardClass cardClass() { return CardClass.GONG; }
    @Override public CardRarity rarity() { return CardRarity.GOLD; }

    @Override public void onSettlement(SettlementContext context)
    {
        int hits = (int) context.selfDice().stream().filter(v -> v >= 4).count();
        context.addExtra(hits * 24);
    }
}
