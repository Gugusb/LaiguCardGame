package com.laigu.laigu.duel.newcard.cards;

import com.laigu.laigu.duel.CardClass;
import com.laigu.laigu.duel.newcard.ArtifactCardNames;
import com.laigu.laigu.duel.newcard.BattleEvent;
import com.laigu.laigu.duel.newcard.CardContext;
import com.laigu.laigu.duel.newcard.CardRarity;
import com.laigu.laigu.duel.newcard.DuelCard;

/** 追锋·无限琵琶（普通）：你使用其他手牌时 → 抽 1 张牌（不含刚下的新卡）。 */
public final class WuXianPiPaCommonCard implements DuelCard
{
    @Override public String id() { return "wu_xian_pi_pa_common"; }
    @Override public String displayName() { return ArtifactCardNames.variantName("wu_xian_pi_pa", rarity()); }
    @Override public CardClass cardClass() { return CardClass.GONG; }
    @Override public CardRarity rarity() { return CardRarity.COMMON; }

    @Override public void onEvent(BattleEvent event, CardContext context)
    {
        if (event.type() != BattleEvent.Type.SUMMON) return;
        // 己方其他场位有新卡入场时触发（旧引擎 triggerOtherUse 跳过刚下的新卡）。
        if (event.side() != context.side() || event.slot() == context.slot()) return;
        context.drawCards(drawPerOtherUse());
    }

    int drawPerOtherUse() { return 1; }
}
