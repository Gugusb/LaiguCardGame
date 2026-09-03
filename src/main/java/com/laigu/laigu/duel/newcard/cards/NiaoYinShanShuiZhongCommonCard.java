package com.laigu.laigu.duel.newcard.cards;

import com.laigu.laigu.duel.CardClass;
import com.laigu.laigu.duel.newcard.ArtifactCardNames;
import com.laigu.laigu.duel.newcard.BattleEvent;
import com.laigu.laigu.duel.newcard.CardContext;
import com.laigu.laigu.duel.newcard.CardRarity;
import com.laigu.laigu.duel.newcard.DuelCard;

/** 鸟音山水钟（普通）：重骰——抓骰时重骰共享池 > 抓取点数的骰（本轮限 1 次）。 */
public final class NiaoYinShanShuiZhongCommonCard implements DuelCard
{
    @Override public String id() { return "niao_yin_shan_shui_zhong_common"; }
    @Override public String displayName() { return ArtifactCardNames.variantName("niao_yin_shan_shui_zhong", rarity()); }
    @Override public CardClass cardClass() { return CardClass.MOU; }
    @Override public CardRarity rarity() { return CardRarity.COMMON; }

    /** 重骰限次：普通每轮 1 次、金卡每轮 2 次（确认稿 Q4）。 */
    private int rerollLimit() { return 1; }

    @Override public void onEvent(BattleEvent event, CardContext context)
    {
        if (event.type() != BattleEvent.Type.DRAFT) return;
        if (context.rerollUses() >= rerollLimit()) return;
        context.rerollSharedPoolAbove(event.value());
        context.useReroll();
    }
}
