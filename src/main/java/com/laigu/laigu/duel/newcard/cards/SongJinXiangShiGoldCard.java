package com.laigu.laigu.duel.newcard.cards;

import com.laigu.laigu.duel.CardClass;
import com.laigu.laigu.duel.newcard.ArtifactCardNames;
import com.laigu.laigu.duel.newcard.CardContext;
import com.laigu.laigu.duel.newcard.CardRarity;
import com.laigu.laigu.duel.newcard.DuelCard;
import com.laigu.laigu.duel.newcard.OnRoundStart;
import com.laigu.laigu.duel.newcard.OnSettlement;
import com.laigu.laigu.duel.newcard.SettlementContext;

/**
 * 藏锋·宋锦香市（金质）：每轮开始时抽 x 张牌（x = 站场轮数 +1，上限 3）。
 * 焕章：抽牌时若手牌超限，本卡永久提供 +3 基础分（可叠加）。
 */
public final class SongJinXiangShiGoldCard implements DuelCard, OnRoundStart, OnSettlement
{
    private static final int DRAW_CAP = 3;

    @Override public String id() { return "song_jin_xiang_shi_gold"; }
    @Override public String displayName() { return ArtifactCardNames.variantName("song_jin_xiang_shi", rarity()); }
    @Override public CardClass cardClass() { return CardClass.MOU; }
    @Override public CardRarity rarity() { return CardRarity.GOLD; }

    @Override public void onRoundStart(CardContext context)
    {
        int draw = Math.min(DRAW_CAP, context.selfState().roundsOnField() + 1);
        // 焕章：抽牌超限 → 永久 +3 基础分（每次超限叠加；结算时计入）。
        if (context.handSize() + draw > context.maxHandSize()) context.addPersistentBaseBonus(3);
        context.drawCards(draw);
    }

    /** 焕章消费：结算时把累计的永久基础分计入（破阵削弱照常缩放）。 */
    @Override public void onSettlement(SettlementContext context)
    {
        int persistent = context.selfState().persistentBaseBonus();
        if (persistent > 0) context.addBaseScaled(persistent);
    }
}
