package com.laigu.laigu.duel.newcard.cards;

import com.laigu.laigu.duel.CardClass;
import com.laigu.laigu.duel.newcard.ArtifactCardNames;
import com.laigu.laigu.duel.newcard.BattleEvent;
import com.laigu.laigu.duel.newcard.CardContext;
import com.laigu.laigu.duel.newcard.CardRarity;
import com.laigu.laigu.duel.newcard.DuelCard;

/** 青瓷莲花尊（金质）：抓骰得分 (6-抓取点数)×6 额外分（清单口径，取代确认稿 Q1 的 ×10）。 */
public final class QingCiLianHuaZunGoldCard implements DuelCard
{
    @Override public String id() { return "qing_ci_lian_hua_zun_gold"; }
    @Override public String displayName() { return ArtifactCardNames.variantName("qing_ci_lian_hua_zun", rarity()); }
    @Override public CardClass cardClass() { return CardClass.MOU; }
    @Override public CardRarity rarity() { return CardRarity.GOLD; }

    /** 抓骰得分：抓取点数 f -> (6-f)×6 额外分。 */
    @Override public void onEvent(BattleEvent event, CardContext context)
    {
        if (event.type() != BattleEvent.Type.DRAFT) return;
        int face = event.value();
        context.addExtraScore((6 - face) * 6);
    }
}
