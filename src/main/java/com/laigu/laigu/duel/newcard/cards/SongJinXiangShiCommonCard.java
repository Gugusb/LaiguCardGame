package com.laigu.laigu.duel.newcard.cards;

import com.laigu.laigu.duel.CardClass;
import com.laigu.laigu.duel.newcard.ArtifactCardNames;
import com.laigu.laigu.duel.newcard.CardContext;
import com.laigu.laigu.duel.newcard.CardRarity;
import com.laigu.laigu.duel.newcard.DuelCard;
import com.laigu.laigu.duel.newcard.OnRoundStart;

/** 藏锋·宋锦香市（普通）：每轮开始时抽 x 张牌（x = 本卡站场轮数，上限 3）。 */
public final class SongJinXiangShiCommonCard implements DuelCard, OnRoundStart
{
    private static final int DRAW_CAP = 3;

    @Override public String id() { return "song_jin_xiang_shi_common"; }
    @Override public String displayName() { return ArtifactCardNames.variantName("song_jin_xiang_shi", rarity()); }
    @Override public CardClass cardClass() { return CardClass.MOU; }
    @Override public CardRarity rarity() { return CardRarity.COMMON; }

    @Override public void onRoundStart(CardContext context)
    {
        context.drawCards(Math.min(DRAW_CAP, context.selfState().roundsOnField() + goldStayBonus()));
    }

    int goldStayBonus() { return 0; }
}
