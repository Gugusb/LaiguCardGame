package com.laigu.laigu.duel.newcard;

/** 抢骰计划构建句柄：OnDraftPlan 卡牌通过它申报对抓取计划的修正。 */
public interface DraftPlanHandle
{
    /** 申报卡牌所属对战方（0/1）。 */
    int side();

    /** 双方抓取次数同加 delta（可为负）。 */
    void addTurnsBoth(int delta);
    /** 本方抓取次数 +delta。 */
    void addTurnsSelf(int delta);
    /** 对方抓取次数 +delta（压制词条传负数）。 */
    void addTurnsOpponent(int delta);
    /** 本方每次抓取颗数 +delta。 */
    void addGrabSizeSelf(int delta);
}
