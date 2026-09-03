package com.laigu.laigu.duel.newcard;

import java.util.ArrayList;
import java.util.List;

/**
 * 阶段16：从场上 OnDraftPlan 卡牌构建抢骰抓取计划（对齐旧 DuelGame.buildDraftPlan）。
 * 双方默认次数 3/2；先手颗数曲线 1/2/.../2/1、后手全 2；次数下限 0。
 */
public final class DraftPlanBuilder
{
    public static final int FIRST_DRAFT_TURNS = 3;
    public static final int SECOND_DRAFT_TURNS = 2;

    private DraftPlanBuilder() {}

    /** 构建并写入 battle.state 的抓取计划（firstPicker 由调用方预先写入 state）。 */
    public static void build(NewCardBattle battle)
    {
        BattleState state = battle.state();
        int[] both = new int[1];
        int[] self = new int[2];
        int[] grab = new int[2];
        int[] opp = new int[2];
        for (CardPlacement p : battle.placements())
        {
            if (!(p.card() instanceof OnDraftPlan planCard)) continue;
            int cardSide = p.side();
            planCard.onDraftPlan(new DraftPlanHandle()
            {
                @Override public int side() { return cardSide; }
                @Override public void addTurnsBoth(int delta) { both[0] += delta; }
                @Override public void addTurnsSelf(int delta) { self[cardSide] += delta; }
                @Override public void addTurnsOpponent(int delta) { opp[1 - cardSide] += delta; }
                @Override public void addGrabSizeSelf(int delta) { grab[cardSide] += delta; }
            });
        }
        // Q7 拍板：金 T形帛画/编钟入场时本回合抓骰次数 +1（一次性加成，构建即消耗）。
        self[0] += state.consumeDraftTurnBonus(0);
        self[1] += state.consumeDraftTurnBonus(1);
        int first = state.firstPicker();
        int fTurns = Math.max(0, FIRST_DRAFT_TURNS + both[0] + self[first] + opp[first]);
        int sTurns = Math.max(0, SECOND_DRAFT_TURNS + both[0] + self[1 - first] + opp[1 - first]);
        state.setDraftPlan(toList(firstSizes(fTurns), grab[first]), toList(secondSizes(sTurns), grab[1 - first]));
    }

    private static List<Integer> toList(int[] sizes, int grabAdd)
    {
        List<Integer> out = new ArrayList<>();
        for (int size : bump(sizes, grabAdd)) out.add(size);
        return out;
    }

    /** 每次抓取颗数整体 +add（每次抓取 +N 颗；下限 1）。 */
    static int[] bump(int[] sizes, int add)
    {
        if (add <= 0) return sizes;
        int[] out = new int[sizes.length];
        for (int i = 0; i < sizes.length; i++) out[i] = Math.max(1, sizes[i] + add);
        return out;
    }

    /** 先手每次抓取数：首尾各 1、中间 2（1/2/.../2/1）。 */
    static int[] firstSizes(int n)
    {
        if (n <= 0) return new int[0];
        int[] a = new int[n];
        for (int k = 0; k < n; k++) a[k] = (k == 0 || k == n - 1) ? 1 : 2;
        return a;
    }

    /** 后手每次抓数：全部 2。 */
    static int[] secondSizes(int n)
    {
        if (n <= 0) return new int[0];
        int[] a = new int[n];
        for (int k = 0; k < n; k++) a[k] = 2;
        return a;
    }
}
