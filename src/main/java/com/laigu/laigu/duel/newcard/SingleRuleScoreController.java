package com.laigu.laigu.duel.newcard;

import java.util.List;

/** 阶段4的首个可开关规则：新版仅接管场上有效骰子的基础分。 */
public final class SingleRuleScoreController
{
    private final boolean enabled;
    private final DicePatternMultiplier patternMultiplier;

    public SingleRuleScoreController(boolean enabled)
    {
        this(enabled, new DicePatternMultiplier(0, 0, 0, 0, 0));
    }

    public SingleRuleScoreController(boolean enabled, DicePatternMultiplier patternMultiplier)
    {
        this.enabled = enabled;
        this.patternMultiplier = java.util.Objects.requireNonNull(patternMultiplier);
    }

    public ScoreSnapshot calculate(BattleState state)
    {
        if (!enabled) throw new IllegalStateException("新版基础分规则未启用");
        state.clearScores();
        for (int side = 0; side < BattleState.SIDES; side++)
        {
            int base = 0;
            for (int slot = 0; slot < BattleState.SLOTS; slot++)
                for (int die : state.cardStateAt(side, slot).activeDice()) base += die;
            state.addBaseScoreForRule(side, base);
            int multiplier = 1 + state.overflowDrawMultiplier(side);
            List<Integer> allDice = new java.util.ArrayList<>();
            for (int slot = 0; slot < BattleState.SLOTS; slot++)
                allDice.addAll(state.cardStateAt(side, slot).activeDice());
            state.setMultiplierForRule(side, multiplier + patternMultiplier.apply(allDice));
        }
        return BattleScoreAdapter.fromNewState(state);
    }

    public ScoreSnapshot calculateOrFallback(BattleState state, ScoreSnapshot fallback)
    {
        try { return calculate(state); }
        catch (RuntimeException ignored) { return fallback; }
    }
}
