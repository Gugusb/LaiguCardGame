package com.laigu.laigu.duel.newcard.cards;

import com.laigu.laigu.duel.CardClass;
import com.laigu.laigu.duel.newcard.ArtifactCardNames;
import com.laigu.laigu.duel.newcard.CardRarity;
import com.laigu.laigu.duel.newcard.DuelCard;
import com.laigu.laigu.duel.newcard.OnSettlement;
import com.laigu.laigu.duel.newcard.SettlementContext;

/** 开疆·银雀山汉简2号（金质）：我方每有 1 张攻·炽卡 → +4 基础分（分值类金卡×2）。 */
public final class YinQueShanHanJian2GoldCard implements DuelCard, OnSettlement
{
    @Override public String id() { return "yin_que_shan_han_jian_2_gold"; }
    @Override public String displayName() { return ArtifactCardNames.variantName("yin_que_shan_han_jian_2", rarity()); }
    @Override public CardClass cardClass() { return CardClass.DING; }
    @Override public CardRarity rarity() { return CardRarity.GOLD; }

    @Override public void onSettlement(SettlementContext context)
    {
        context.addBaseScaled(4 * context.ownClassCount(CardClass.GONG));
    }
}
