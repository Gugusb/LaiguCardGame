package com.laigu.laigu.duel.newcard;

/** 新卡牌架构的统一战斗事件。value 携带事件数值（当前：DRAFT 的抓取点数；其余事件为 0）。 */
public record BattleEvent(Type type, int side, int slot, int value)
{
    public BattleEvent(Type type, int side, int slot) { this(type, side, slot, 0); }

    public enum Type
    {
        SUMMON,
        LEAVE,
        ROUND_START,
        ROUND_END,
        DRAFT,
        PLACE,
        SETTLEMENT,
        AMBUSH_SUCCESS,
        AMBUSH_FAIL,
        PO_ZHEN,
        ACTIVATION
    }
}
