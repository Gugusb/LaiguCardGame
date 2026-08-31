package com.laigu.laigu.duel.newcard;

/** 新卡牌架构的统一战斗事件。 */
public record BattleEvent(Type type, int side, int slot)
{
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
