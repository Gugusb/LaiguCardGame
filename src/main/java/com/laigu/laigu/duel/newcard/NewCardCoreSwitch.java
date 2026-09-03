package com.laigu.laigu.duel.newcard;

import java.util.Set;

/**
 * 阶段十二生产切换开关：新卡核心接管回合结算分数。
 * <p>
 * 未迁移的抢骰族卡牌（无共享骰池基础设施）上场时按回合自动回退旧引擎；
 * 关闭开关可整体回滚到旧引擎（故障回滚通道）。
 */
public final class NewCardCoreSwitch
{
    /**
     * 未迁移卡牌 ID（阶段16 抢骰基础设施落地后清空 → 158/158 全部由新系统实现）。
     * 后续若新增未迁移卡牌，在此登记并与 Stage11LedgerComparisonTest 的清单保持一致。
     */
    public static final Set<String> UNMIGRATED_IDS = Set.of();

    private static volatile boolean enabled = true;

    private NewCardCoreSwitch() {}

    public static boolean enabled() { return enabled; }

    /** 故障回滚：整体关闭新核心，结算全部走旧引擎。 */
    public static void setEnabled(boolean value) { enabled = value; }

    /** 本场对局是否可由新核心结算：场上任何未迁移卡牌即回退旧引擎。 */
    public static boolean canSettle(NewCardBattle battle)
    {
        for (CardPlacement placement : battle.placements())
            if (UNMIGRATED_IDS.contains(placement.card().id())) return false;
        return true;
    }
}
