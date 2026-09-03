package com.laigu.laigu.duel.newcard;

/** 回合结束触发词条；执行时机在回合结算之前、清除本轮临时状态之前。 */
public interface OnRoundEnd
{
    void onRoundEnd(CardContext context);
}
