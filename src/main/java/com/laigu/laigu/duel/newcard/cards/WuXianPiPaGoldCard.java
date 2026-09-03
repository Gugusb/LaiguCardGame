package com.laigu.laigu.duel.newcard.cards;

import com.laigu.laigu.duel.CardClass;
import com.laigu.laigu.duel.newcard.ArtifactCardNames;
import com.laigu.laigu.duel.newcard.BattleEvent;
import com.laigu.laigu.duel.newcard.CardContext;
import com.laigu.laigu.duel.newcard.CardRarity;
import com.laigu.laigu.duel.newcard.DuelCard;

/**
 * 追锋·无限琵琶（金质）：使用其他手牌时抽 2 张牌。
 * 焕章：抽牌时若手牌超限，本回合 +1 倍率。
 */
public final class WuXianPiPaGoldCard implements DuelCard
{
    @Override public String id() { return "wu_xian_pi_pa_gold"; }
    @Override public String displayName() { return ArtifactCardNames.variantName("wu_xian_pi_pa", rarity()); }
    @Override public CardClass cardClass() { return CardClass.GONG; }
    @Override public CardRarity rarity() { return CardRarity.GOLD; }

    @Override public void onEvent(BattleEvent event, CardContext context)
    {
        if (event.type() != BattleEvent.Type.SUMMON) return;
        if (event.side() != context.side() || event.slot() == context.slot()) return;
        // 焕章：抽牌前预估超限（超限记一次 +1 倍率，本回合）。
        boolean overflow = context.handSize() + drawCount() > context.maxHandSize();
        context.drawCards(drawCount());
        if (overflow) context.addTimingMult(1);
    }

    int drawCount() { return 2; }
}
