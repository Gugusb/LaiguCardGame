package com.laigu.laigu.duel.newcard;

import java.util.List;

/** 新旧计分系统可比较的最小快照。 */
public record ScoreSnapshot(List<SideScore> sides)
{
    public ScoreSnapshot
    {
        sides = List.copyOf(sides);
        if (sides.size() != BattleState.SIDES) throw new IllegalArgumentException("必须有双方计分");
    }

    public record SideScore(int base, int multiplier, int extra, int total)
    {
        public static SideScore of(int base, int multiplier, int extra)
        {
            return new SideScore(base, multiplier, extra, base * multiplier + extra);
        }
    }
}
