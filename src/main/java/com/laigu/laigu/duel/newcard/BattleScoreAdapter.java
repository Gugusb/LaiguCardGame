package com.laigu.laigu.duel.newcard;

import com.laigu.laigu.duel.DuelGame;

import java.util.List;

/** 将新旧引擎的可见计分字段转换为同一快照；仅比较，不接管结算。 */
public final class BattleScoreAdapter
{
    private BattleScoreAdapter() {}

    public static ScoreSnapshot fromNewState(BattleState state)
    {
        return new ScoreSnapshot(List.of(
                ScoreSnapshot.SideScore.of(state.baseScore(0), state.multiplier(0), state.extraScore(0)),
                ScoreSnapshot.SideScore.of(state.baseScore(1), state.multiplier(1), state.extraScore(1))));
    }

    public static ScoreSnapshot fromLegacy(DuelGame game)
    {
        return new ScoreSnapshot(List.of(
                ScoreSnapshot.SideScore.of(game.lastBase(0), game.lastMult(0), game.lastExtra(0)),
                ScoreSnapshot.SideScore.of(game.lastBase(1), game.lastMult(1), game.lastExtra(1))));
    }

    public static ScoreComparison compare(BattleState state, DuelGame game)
    {
        return ScoreComparison.compare(fromLegacy(game), fromNewState(state));
    }
}
