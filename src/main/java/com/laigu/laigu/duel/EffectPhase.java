package com.laigu.laigu.duel;

/**
 * 卡牌效果的执行阶段。阶段登记用于校验卡牌绑定，具体执行仍由 ScoreEngine 或 DuelGame 负责。
 */
public enum EffectPhase
{
    /** ScoreEngine 的最终卡牌结算。 */
    SETTLEMENT,
    /** DuelGame 的入场事件。 */
    SUMMON,
    /** DuelGame 的离场事件。 */
    LEAVE,
    /** DuelGame 的回合开始事件。 */
    ROUND_START,
    /** DuelGame 的回合结束事件。 */
    ROUND_END,
    /** DuelGame 的抢骰阶段。 */
    DRAFT,
    /** DuelGame 的布置阶段。 */
    PLACE,
    /** DuelGame 的伏击结算事件。 */
    AMBUSH,
    /** DuelGame 的激活事件。 */
    ACTIVATE,
    /** DuelGame 的非最终结算计分时机。 */
    TIMING,
    /** 仅保留设计枚举，尚未接入执行入口。 */
    RESERVED
}
