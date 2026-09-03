package com.laigu.laigu.duel.newcard;

/** 抢骰或抓骰触发词条：回合开始分配骰子阶段执行。 */
public interface OnDraft
{
    void onDraft(CardContext context);
}
