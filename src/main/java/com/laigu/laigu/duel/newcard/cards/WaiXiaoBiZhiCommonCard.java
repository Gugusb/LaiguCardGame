package com.laigu.laigu.duel.newcard.cards;

import com.laigu.laigu.duel.CardClass;
import com.laigu.laigu.duel.newcard.ArtifactCardNames;
import com.laigu.laigu.duel.newcard.CardRarity;
import com.laigu.laigu.duel.newcard.DuelCard;
import com.laigu.laigu.duel.newcard.OnSettlement;
import com.laigu.laigu.duel.newcard.SettlementContext;

/** 外销壁纸普通版：对手场上满 5 张 → +20 额外分（旧目录文案写 4，引擎按 FIELD_SLOTS=5 判满）。 */
public final class WaiXiaoBiZhiCommonCard implements DuelCard, OnSettlement
{
    @Override public String id() { return "wai_xiao_bi_zhi_common"; }
    @Override public String displayName() { return ArtifactCardNames.variantName("wai_xiao_bi_zhi", rarity()); }
    @Override public CardClass cardClass() { return CardClass.SHOU; }
    @Override public CardRarity rarity() { return CardRarity.COMMON; }

    @Override public void onSettlement(SettlementContext context)
    {
        if (context.opponentFieldCount() >= BattleStateSlots.FIELD_SLOTS) context.addExtra(fullFieldBonus());
    }

    /** 对齐旧引擎 DuelGame.FIELD_SLOTS。 */
    static final class BattleStateSlots
    {
        static final int FIELD_SLOTS = com.laigu.laigu.duel.DuelGame.FIELD_SLOTS;
    }

    int fullFieldBonus() { return 20; }
}
