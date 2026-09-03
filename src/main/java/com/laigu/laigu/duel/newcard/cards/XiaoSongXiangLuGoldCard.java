package com.laigu.laigu.duel.newcard.cards;

import com.laigu.laigu.duel.CardClass;
import com.laigu.laigu.duel.newcard.ArtifactCardNames;
import com.laigu.laigu.duel.newcard.CardContext;
import com.laigu.laigu.duel.newcard.CardRarity;
import com.laigu.laigu.duel.newcard.DuelCard;
import com.laigu.laigu.duel.newcard.OnRoundEnd;

/** 侧击·小松香炉（金质）：本轮获胜后抽 3 张牌（清单：无焕章、无倍率词条）。 */
public final class XiaoSongXiangLuGoldCard implements DuelCard, OnRoundEnd
{
    @Override public String id() { return "xiao_song_xiang_lu_gold"; }
    @Override public String displayName() { return ArtifactCardNames.variantName("xiao_song_xiang_lu", rarity()); }
    @Override public CardClass cardClass() { return CardClass.MOU; }
    @Override public CardRarity rarity() { return CardRarity.GOLD; }

    @Override public void onRoundEnd(CardContext context)
    {
        if (context.winnerLast() == context.side()) context.drawCards(winDraw());
    }

    int winDraw() { return 3; }
}
