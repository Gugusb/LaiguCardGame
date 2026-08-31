package com.laigu.laigu.duel;

/** 不直接增加分数、而是改变后续结算上下文的规则操作。 */
public record ResolutionModifier(ModifierType type, int p1, int animationKind)
{
    public enum ModifierType
    {
        REDUCE_OPPONENT_CONTRIBUTION
    }
}
