package com.laigu.laigu.duel.newcard;

import java.util.List;

/** 可组合的骰型倍率规则。 */
public record DicePatternMultiplier(int straight, int allOdd, int allEven, int allHigh, int allLow)
{
    public int apply(List<Integer> dice)
    {
        int result = 0;
        if (DicePattern.straight(dice)) result += straight;
        if (DicePattern.allOdd(dice)) result += allOdd;
        if (DicePattern.allEven(dice)) result += allEven;
        if (DicePattern.allHigh(dice)) result += allHigh;
        if (DicePattern.allLow(dice)) result += allLow;
        return result;
    }
}
