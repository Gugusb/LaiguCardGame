package com.laigu.laigu.duel.newcard;

/** 阶段3影子计分器：读取新版状态中已产生的计分分量，不修改任何状态。 */
public final class ShadowScoreCalculator
{
    private ShadowScoreCalculator() {}

    public static ScoreSnapshot calculate(BattleState state)
    {
        return BattleScoreAdapter.fromNewState(state);
    }
}
