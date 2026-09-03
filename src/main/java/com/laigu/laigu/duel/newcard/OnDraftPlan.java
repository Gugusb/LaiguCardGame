package com.laigu.laigu.duel.newcard;

/** 抢骰计划构建词条：进入抢骰、构建抓取计划时执行一次。 */
public interface OnDraftPlan
{
    void onDraftPlan(DraftPlanHandle handle);
}
