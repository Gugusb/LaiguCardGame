package com.laigu.laigu.duel.newcard;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** 新架构的纯骰型判断，规则与旧 ScoreEngine.Conds 保持一致。 */
public final class DicePattern
{
    private DicePattern() {}

    public static boolean straight(List<Integer> dice)
    {
        if (dice == null || dice.size() < 3) return false;
        Set<Integer> unique = new HashSet<>(dice);
        if (unique.size() != dice.size()) return false;
        int min = dice.stream().mapToInt(Integer::intValue).min().orElse(0);
        int max = dice.stream().mapToInt(Integer::intValue).max().orElse(0);
        return max - min <= dice.size() - 1;
    }

    public static boolean allOdd(List<Integer> dice)
    {
        return !dice.isEmpty() && dice.stream().allMatch(v -> v % 2 != 0);
    }

    public static boolean allEven(List<Integer> dice)
    {
        return !dice.isEmpty() && dice.stream().allMatch(v -> v % 2 == 0);
    }

    public static boolean allHigh(List<Integer> dice)
    {
        return !dice.isEmpty() && dice.stream().allMatch(v -> v >= 4);
    }

    public static boolean allLow(List<Integer> dice)
    {
        return !dice.isEmpty() && dice.stream().allMatch(v -> v <= 3);
    }
}
