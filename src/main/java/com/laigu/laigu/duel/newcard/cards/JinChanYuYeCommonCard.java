package com.laigu.laigu.duel.newcard.cards;

import com.laigu.laigu.duel.CardClass;
import com.laigu.laigu.duel.newcard.ArtifactCardNames;
import com.laigu.laigu.duel.newcard.CardRarity;
import com.laigu.laigu.duel.newcard.DuelCard;
import com.laigu.laigu.duel.newcard.OnSettlement;
import com.laigu.laigu.duel.newcard.SettlementContext;

/** 鎏金·金蝉玉叶（普通）：每颗放在金质卡上的骰 → +1 倍率。 */
public final class JinChanYuYeCommonCard implements DuelCard, OnSettlement
{
    @Override public String id() { return "jin_chan_yu_ye_common"; }
    @Override public String displayName() { return ArtifactCardNames.variantName("jin_chan_yu_ye", rarity()); }
    @Override public CardClass cardClass() { return CardClass.DING; }
    @Override public CardRarity rarity() { return CardRarity.COMMON; }

    @Override public void onSettlement(SettlementContext context)
    {
        context.addMultiplierScaled(perGoldDieMult() * context.ownGoldCardDice().size());
    }

    int perGoldDieMult() { return 1; }
}
