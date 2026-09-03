package com.laigu.laigu.duel.newcard;

import java.util.List;

/** 新旧计分快照的逐方比较结果。 */
public record ScoreComparison(boolean matches, List<String> differences)
{
    public ScoreComparison
    {
        differences = List.copyOf(differences);
    }

    public static ScoreComparison compare(ScoreSnapshot expected, ScoreSnapshot actual)
    {
        java.util.ArrayList<String> differences = new java.util.ArrayList<>();
        for (int side = 0; side < BattleState.SIDES; side++)
        {
            ScoreSnapshot.SideScore e = expected.sides().get(side);
            ScoreSnapshot.SideScore a = actual.sides().get(side);
            if (e.base() != a.base()) differences.add("side" + side + ".base: " + e.base() + " != " + a.base());
            if (e.multiplier() != a.multiplier()) differences.add("side" + side + ".multiplier: " + e.multiplier() + " != " + a.multiplier());
            if (e.extra() != a.extra()) differences.add("side" + side + ".extra: " + e.extra() + " != " + a.extra());
            if (e.total() != a.total()) differences.add("side" + side + ".total: " + e.total() + " != " + a.total());
        }
        return new ScoreComparison(differences.isEmpty(), differences);
    }
}
