package com.laigu.laigu.duel;

/**
 * 对战卡效果类型枚举。参数 p1/p2 存放在 DuelCardData 中，
 * ScoreEngine 在结算阶段按此枚举判定并累加基础分/倍率/额外分。
 *
 * 约定：
 *  - 所有条件在【结算阶段】判定；
 *  - 【消耗】在结算后把该卡移出对局（槽位空置不填补）。
 */
public enum EffectType
{
    /** 本卡每有1颗骰 +p1 额外分 */
    PER_DIE_EXTRA,
    /** 本卡每有1颗骰 +p1 基础分 */
    PER_DIE_BASE,
    /** 本卡骰子 ≥2颗 +p1 额外分 */
    DICE_GE2_EXTRA,
    /** 若你的骰全大(≥4) +p1 额外分 */
    ALL_HIGH_EXTRA,
    /** 【消耗】若你的骰为顺子，本卡每有1颗骰 +p1 倍率 */
    STRAIGHT_PER_DIE_MULT_CONSUME,
    /** 无条件 +p1 额外分 */
    FLAT_EXTRA,
    /** 固定增加 p1 倍率（用于激活奖励）。 */
    FLAT_MULT,
    /** 本卡骰子为0颗 +p1 额外分 */
    ZERO_DICE_EXTRA,
    /** 你的骰含1 +p1 额外分 */
    HAS_ONE_EXTRA,
    /** 若你的骰全小(≤3) +p1 额外分 */
    ALL_LOW_EXTRA,
    /** 上轮也在场上 +p1 额外分 */
    LAST_ROUND_EXTRA,
    /** 若你的骰为顺子 +p1 倍率 */
    STRAIGHT_MULT,
    /** 若你的骰全奇 +p1 倍率 */
    ALL_ODD_MULT,
    /** 相邻(左/右)有卡 +p1 倍率 */
    NEIGHBOR_MULT,
    /** 两侧均非本卡朝代 +p1 倍率 +p2 额外分 */
    ISOLATED_MULT_EXTRA,
    /** 若你的骰全偶 +p1 额外分 */
    ALL_EVEN_EXTRA,
    /** 本卡每有1颗骰 +p1 倍率 */
    PER_DIE_MULT,
    /** 我方每有1张{p1朝代}卡 +p2 基础分 */
    DYN_CNT_BASE,
    /** 我方每有1张{p1职业}卡 +p2 倍率 */
    CLASS_CNT_MULT,
    /** 【消耗】结算时 本轮基础分 ×2 */
    BASE_DOUBLE_CONSUME,
    /** 我方每有1张卡 +p1 基础分 */
    CARD_CNT_BASE,
    /** 若你的骰全大(≥4) +p1 倍率 */
    ALL_HIGH_MULT,
    /** 我方每有1张{p1职业}卡 +p2 基础分 */
    CLASS_CNT_BASE,
    /** 我方每有1张金质卡 +p1 倍率 */
    GOLD_CNT_MULT,

    // ================= 第 2 批（50 个新效果 / 见 docs/对战-效果设计.md） =================

    // ---- 攻·炽：吃骰质变（本卡单骰/双骰 判定） ----
    /** 本卡第1颗骰 +p1 额外分，第2颗再 +p2 额外分（阶梯吃骰） */
    DIE_FIRST_BONUS,
    /** 本卡骰面全部相同；第n颗骰按 p1 × p2^(n-1) 获得额外分。 */
    SAME_FACE_EXP_EXTRA,
    /** 每轮在奇数/偶数骰之间轮换，匹配骰每颗获得 p1 额外分。 */
    TOGGLE_PARITY_EXTRA,
    /** 结算时按清单定义的动态状态产生额外分。 */
    SHARED_POOL_SUM_EXTRA,
    /** 激活达成时，将当前基础分复制为额外分。 */
    COPY_CURRENT_BASE_TO_EXTRA,
    /** 达到骰数阈值后同时增加基础/倍率/额外分，p1为阈值，p2为基础/倍率/额外固定值。 */
    DICE_GT_MULTI_REWARD,
    /** 激活无法推进时的金卡额外奖励。 */
    ACTIVATION_FAILED_EXTRA,
    /** 每次友方卡被激活时的额外奖励。 */
    ANY_FRIENDLY_ACTIVATE_EXTRA,
    /** 激活达成时的一次性基础分。 */
    FLAT_BASE,
    /** 激活达成时，按对方场上无骰卡数量增加倍率。 */
    OPP_EMPTY_CARD_MULT,
    /** 本卡每颗奇数骰 +p1 额外分 */
    ODD_DIE_EXTRA,
    /** 本卡每颗偶数骰 +p1 额外分 */
    EVEN_DIE_EXTRA,
    /** 本卡每颗 ≥4 骰 +p1 额外分 */
    DIE_GE4_EXTRA,
    /** 本卡每颗 ≤3 骰 +p1 额外分 */
    DIE_LE3_EXTRA,
    /** 本卡骰面和 ≥p1 → +p2 额外分 */
    DIE_SUM_GE_EXTRA,
    /** 本卡骰面和 ≥p1 → +p2 倍率 */
    DIE_SUM_GE_MULT,
    /** 本卡 ≥1 颗骰 → +p1 额外分（保底吃骰） */
    DICE_GE1_EXTRA,
    /** 本卡骰面和为奇数 → +p1 额外分 */
    DIE_SUM_ODD_EXTRA,
    /** 本卡 2 颗骰相同 → +p1 倍率 */
    SAME_FACE_MULT,

    // ---- 守·衡：对位·顺势·持久 ----
    /** 对手放置骰比你多 → +p1 额外分 */
    OPP_MORE_DICE_EXTRA,
    /** 对手场上满 4 张 → +p1 额外分 */
    OPP_FIELD_FULL_EXTRA,
    /** 上轮你输 → +p1 额外分（翻盘） */
    LOSE_LAST_EXTRA,
    /** 上轮平局 → +p1 额外分 */
    DRAW_LAST_EXTRA,
    /** 上轮你赢 → +p1 倍率（滚雪球） */
    WIN_LAST_MULT,
    /** 总局数落后 → +p1 额外分 */
    BEHIND_WINS_EXTRA,
    /** 手牌为 0 → +p1 额外分 */
    HAND_EMPTY_EXTRA,
    /** 连续在场 ≥2 轮 → +p1 额外分（老兵） */
    LASTED_2_EXTRA,
    /** 轮次 ≥2 → +p1 额外分（后期发力） */
    ROUND_GE2_EXTRA,

    // ---- 谋·策：全盘骰型 + 槽位阵型 ----
    /** 你的骰有两对 → +p1 倍率 */
    TWO_PAIR_MULT,
    /** 你的骰满堂彩（三同+一对）→ +p1 倍率 */
    FULL_HOUSE_MULT,
    /** 你的骰全相同（≥3 颗）→ +p1 倍率 */
    ALL_SAME_MULT,
    /** 你的骰含 6 → +p1 额外分 */
    HAS_SIX_EXTRA,
    /** 你的骰面和 ≤9 或 ≥18 → +p1 额外分（两极） */
    SUM_RANGE_EXTRA,
    /** 你的骰含相邻两数 → +p1 额外分 */
    CONSEC_NEAR_EXTRA,
    /** 本卡在两端槽位(0/3) → +p1 额外分 */
    EDGE_EXTRA,
    /** 本卡在中间槽位(1/2) → +p1 倍率 */
    CENTER_MULT,
    /** 相邻有同职业卡 → +p1 倍率 */
    ADJ_SAME_CLASS_MULT,
    /** 相邻有不同职业卡 → +p1 额外分 */
    ADJ_DIFF_CLASS_EXTRA,

    // ---- 鼎·盛：资源引擎 + 金质引擎 + 朝代 ----
    /** 每张手牌 +p1 倍率 */
    HAND_CNT_MULT,
    /** 每颗未布置骰 +p1 基础分 */
    POOL_CNT_BASE,
    /** 每颗未布置骰 +p1 倍率 */
    POOL_CNT_MULT,
    /** 每张未抽卡组牌 +p1 基础分 */
    DECK_CNT_BASE,
    /** 共享骰池每颗 +p1 额外分 */
    SHARED_POOL_EXTRA,
    /** 每张金质卡 +p1 基础分 */
    GOLD_CNT_BASE,
    /** 每颗放在金质卡上的骰 +p1 倍率 */
    GOLD_DIE_MULT,
    /** 若你有金质卡 → +p1 倍率（激活型） */
    GOLD_DYN_MULT,
    /** 每张金质卡 +p1 额外分 */
    GOLD_EXTRA,
    /** 相邻有同朝代卡 → +p1 额外分 */
    ADJ_SAME_DYN_EXTRA,

    // ---- 献祭：消耗系（结算后移出对局） ----
    /** 【消耗】本卡每颗骰 +p1 倍率 */
    CONSUME_PER_DIE_MULT,
    /** 【消耗】本卡每颗骰 +p1 基础分 */
    CONSUME_PER_DIE_BASE,
    /** 【消耗】本轮基础分 +p1 */
    CONSUME_BASE_FLAT,
    /** 【消耗】本轮额外分 ×2 */
    CONSUME_EXTRA_DOUBLE,
    /** 【消耗】每张手牌 +p1 倍率 */
    CONSUME_HAND_MULT,
    /** 【消耗】对手每颗骰 +p1 额外分 */
    CONSUME_OPP_DICE_EXTRA,

    // ---- 节奏：轮次/先手/空卡 ----
    /** 偶数轮 → +p1 倍率 */
    ROUND_EVEN_MULT,
    /** 若你为先手 → +p1 倍率 */
    FIRST_PICK_MULT,
    /** 本卡 0 颗骰 → +p1 倍率 */
    NO_DICE_MULT,

    // ---- 抢骰系（抢骰阶段生效，不进结算；单方扰动） ----
    /** 【抢骰】本方抓取次数 +p1 */
    DRAFT_SELF_TURNS_UP,
    /** 【抢骰】对方抓取次数 -p1 */
    DRAFT_OPP_TURNS_DOWN,
    /** 【抢骰】本方每次抓取颗数 +p1 */
    DRAFT_SELF_GRAB_UP,

    // ---- 抓取系（抢骰阶段生效，不进结算；双方/全池） ----

    /** 【抢骰】双方各少抓 p1 次骰（抓取计划变化） */
    DRAFT_TURNS_DOWN,
    /** 【抢骰】双方各多抓 p1 次骰（抓取计划变化） */
    DRAFT_TURNS_UP,
    /** 【抢骰】系统掷出的骰子 +p1 颗（共享骰池扩容） */
    DRAFT_POOL_UP,

    // ---- 触发系（事件触发，不进结算；抽卡/回复行动力） ----

    /** 入场时：抽 p1 张牌 */
    SUMMON_DRAW,
    /** 入场时：若上轮你输 → 抽 p1 张牌 */
    SUMMON_DRAW_IF_LOST_LAST,
    /** 入场时：回复 p1 点行动力（不超过每轮上限） */
    SUMMON_RESTORE_AP,
    /** 离场时（被替换/消耗移出）：抽 p1 张牌 */
    LEAVE_DRAW,
    /** 每轮开始时：抽 p1 张牌 */
    ROUND_START_DRAW,
    /** 每轮开始时：抽 x 张牌（x = 本卡站场轮数 roundsOnField） */
    ROUND_START_DRAW_STAY_TURNS,
    /** 你使用其他手牌（部署）时：抽 p1 张牌 */
    OTHER_USE_DRAW,
    /** 结算后：若本轮你赢 → 抽 p1 张牌 */
    ROUND_END_DRAW_IF_WIN,
    /** 结算后：若本轮你输 → 抽 p1 张牌 */
    ROUND_END_DRAW_IF_LOSE,

    // ================= 第 3 批（新词条/新效果，见 docs/玩法设计-新词条待实现.md） =================

    // ---- 破阵 ----
    /** 破阵：结算时若本卡槽位骰面和 > 对手同槽位骰面和，把对手该槽位卡牌的效果削弱 50%（向下取整）。 */
    PO_ZHEN_HALVE,

    // ---- 伏击 ----
    /** 伏击：部署后保持背面直到结算才揭晓；对位有卡则成功，否则失败。本效果类型只标记，具体不结算分。 */
    FUJI,
    /** 伏击失败：+p1 额外分。 */
    FUJI_FAIL_EXTRA,

    // ---- 重骰（本轮限一次） ----
    /** 重骰：抓取骰子时，重骰共享池中所有大于「抓取到的那颗骰点数」的骰子（本轮限一次）。 */
    REROLL_ON_DRAFT,

    // ---- 激活 ----
    /** 激活：触发时，对「左侧相邻」卡牌的激活进度 +1。 */
    ACTIVATE_LEFT,

    // ---- 得分时机（非最终结算，同样计入本轮总分，走 基础/倍率/额外 拆分；此处统一为额外分） ----

    /** 抓取骰子时：获得 (6-x)*p1 额外分（x = 抓取到的那颗骰点数）。 */
    DRAFT_SCORE_EXTRA,
    /** 布置骰子时：+p1 额外分。 */
    PLACE_SCORE_EXTRA,
    /** 回合开始时：+p1 额外分。 */
    ROUND_START_SCORE_EXTRA,
    /** 使用手牌（部署）时：+p1 额外分。 */
    USE_HAND_SCORE_EXTRA,
    /** 回合结束时：+p1 额外分。 */
    ROUND_END_SCORE_EXTRA;

    /** 该效果是否带【消耗】标记（结算后移出对局） */
    public boolean isConsume()
    {
        return this == STRAIGHT_PER_DIE_MULT_CONSUME || this == BASE_DOUBLE_CONSUME
                || this == CONSUME_PER_DIE_MULT || this == CONSUME_PER_DIE_BASE
                || this == CONSUME_BASE_FLAT
                || this == CONSUME_EXTRA_DOUBLE || this == CONSUME_HAND_MULT
                || this == CONSUME_OPP_DICE_EXTRA;
    }
}
