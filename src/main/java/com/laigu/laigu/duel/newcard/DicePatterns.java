package com.laigu.laigu.duel.newcard;

import java.util.List;

/** 骰型判断公共工具；只做判断，不代表任何具体卡牌。 */
public final class DicePatterns
{
    private DicePatterns() {}

    public static int sum(List<Integer> dice)
    {
        return dice.stream().mapToInt(Integer::intValue).sum();
    }

    /** 顺子：连续递增（如 1-2-3），至少 2 枚骰。 */
    public static boolean isStraight(List<Integer> dice)
    {
        if (dice.size() < 2) return false;
        List<Integer> sorted = dice.stream().distinct().sorted().toList();
        if (sorted.size() != dice.size()) return false;
        for (int i = 1; i < sorted.size(); i++)
            if (sorted.get(i) != sorted.get(i - 1) + 1) return false;
        return true;
    }

    /**
     * 顺子（间隔1）：去重排序后相邻差全为 1、且恰允许一处差 2（浑天金焕章「顺子可以间隔1」）；至少 3 枚骰。
     * 例：3-4-6-7、2-4-5-6 成立；1-2-4-6（两处缺口）不成立。
     */
    public static boolean isStraightWithOneGap(List<Integer> dice)
    {
        if (dice.size() < 3) return false;
        List<Integer> sorted = dice.stream().distinct().sorted().toList();
        int gaps = 0;
        for (int i = 1; i < sorted.size(); i++)
        {
            int diff = sorted.get(i) - sorted.get(i - 1);
            if (diff == 1) continue;
            if (diff == 2 && gaps == 0) { gaps++; continue; }
            return false;
        }
        return true;
    }

    public static boolean isAllHigh(List<Integer> dice)
    {
        return !dice.isEmpty() && dice.stream().allMatch(v -> v >= 4);
    }

    public static boolean isAllLow(List<Integer> dice)
    {
        return !dice.isEmpty() && dice.stream().allMatch(v -> v <= 3);
    }

    public static boolean isAllOdd(List<Integer> dice)
    {
        return !dice.isEmpty() && dice.stream().allMatch(v -> v % 2 == 1);
    }

    public static boolean isAllEven(List<Integer> dice)
    {
        return !dice.isEmpty() && dice.stream().allMatch(v -> v % 2 == 0);
    }

    public static boolean isAllSame(List<Integer> dice)
    {
        return !dice.isEmpty() && dice.stream().distinct().count() == 1;
    }

    public static boolean isTwoPair(List<Integer> dice)
    {
        return pairCount(dice) == 2;
    }

    public static boolean isFullHouse(List<Integer> dice)
    {
        return dice.size() == 5 && pairCount(dice) == 2;
    }

    /** 三条：恰好一个三张（不含葫芦）。 */
    public static boolean isTriple(List<Integer> dice)
    {
        java.util.Map<Integer, Long> counts = kindCounts(dice);
        return dice.size() >= 3 && counts.containsValue(3L)
                && counts.values().stream().noneMatch(c -> c > 3);
    }

    /** 一对：恰好一个对子。 */
    public static boolean isPair(List<Integer> dice)
    {
        return pairCount(dice) == 1;
    }

    private static long pairCount(List<Integer> dice)
    {
        return kindCounts(dice).values().stream().filter(c -> c >= 2).count();
    }

    private static java.util.Map<Integer, Long> kindCounts(List<Integer> dice)
    {
        return dice.stream().collect(java.util.stream.Collectors.groupingBy(v -> v, java.util.stream.Collectors.counting()));
    }
}
