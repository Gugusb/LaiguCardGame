package com.laigu.laigu.duel.newcard.cards;

import com.laigu.laigu.duel.CardClass;
import com.laigu.laigu.duel.newcard.ArtifactCardNames;
import com.laigu.laigu.duel.newcard.CardRarity;
import com.laigu.laigu.duel.newcard.DuelCard;
import com.laigu.laigu.duel.newcard.OnSettlement;
import com.laigu.laigu.duel.newcard.SettlementContext;

/** 开疆·银雀山汉简2号（普通）：我方每有 1 张攻·炽卡 → +2 基础分。 */
public final class YinQueShanHanJian2CommonCard implements DuelCard, OnSettlement
{
    @Override public String id() { return "yin_que_shan_han_jian_2_common"; }
    @Override public String displayName() { return ArtifactCardNames.variantName("yin_que_shan_han_jian_2", rarity()); }
    @Override public CardClass cardClass() { return CardClass.DING; }
    @Override public CardRarity rarity() { return CardRarity.COMMON; }

    @Override public void onSettlement(SettlementContext context)
    {
        context.addBaseScaled(perGongCard() * context.ownClassCount(CardClass.GONG));
    }

    int perGongCard() { return 2; }
}
