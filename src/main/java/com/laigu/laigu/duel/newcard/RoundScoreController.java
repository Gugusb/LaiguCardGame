package com.laigu.laigu.duel.newcard;

import com.laigu.laigu.duel.DuelGame;

import java.util.Objects;

/** 阶段5回合计分编排器：新版可接管时使用，否则返回旧引擎快照。 */
public final class RoundScoreController
{
    private final boolean enabled;
    private final SingleRuleScoreController baseRule;
    private final SettlementRule settlementRule;

    public RoundScoreController(boolean enabled)
    {
        this(enabled, context -> { });
    }

    public RoundScoreController(boolean enabled, SettlementRule settlementRule)
    {
        this.enabled = enabled;
        this.baseRule = new SingleRuleScoreController(enabled);
        this.settlementRule = Objects.requireNonNull(settlementRule);
    }

    public ScoreSnapshot settle(BattleState state, DuelGame legacy)
    {
        return settle(null, state, legacy);
    }

    public ScoreSnapshot settle(NewCardBattle battle, BattleState state, DuelGame legacy)
    {
        Objects.requireNonNull(state);
        Objects.requireNonNull(legacy);
        ScoreSnapshot fallback = BattleScoreAdapter.fromLegacy(legacy);
        if (!enabled) return fallback;
        try
        {
            if (battle != null)
            {
                battle.dispatch(new BattleEvent(BattleEvent.Type.SETTLEMENT, -1, -1));
                NewSettlementCalculator.calculate(battle);
            }
            else
            {
                state.clearScores();
                baseRule.calculate(state);
            }
            for (int side = 0; side < BattleState.SIDES; side++) settlementRule.apply(new SettlementContext(state, side));
            return BattleScoreAdapter.fromNewState(state);
        }
        catch (RuntimeException ignored)
        {
            return fallback;
        }
    }
}
