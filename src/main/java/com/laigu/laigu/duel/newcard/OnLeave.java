package com.laigu.laigu.duel.newcard;

/** 离场触发词条：卡牌离开场位时执行（离场状态仍在）。 */
public interface OnLeave
{
    void onLeave(CardContext context);
}
