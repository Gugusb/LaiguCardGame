package com.laigu.laigu.duel.newcard;

/** 入场触发词条：卡牌被放置到场位上时执行。 */
public interface OnSummon
{
    void onSummon(CardContext context);
}
