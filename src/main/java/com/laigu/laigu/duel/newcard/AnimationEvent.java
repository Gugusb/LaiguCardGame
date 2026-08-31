package com.laigu.laigu.duel.newcard;

/** 规则层产生的动画事件；客户端只消费事件，不参与规则判定。 */
public record AnimationEvent(Type type, int side, int slot, String cardId)
{
    public enum Type
    {
        CARD_PLACE,
        CARD_LEAVE,
        CARD_TRIGGER,
        CARD_FLIP,
        CARD_ACTIVATE,
        CARD_LOCK,
        CARD_DESTROY_MARK,
        DICE_INVALIDATE,
        SCORE_POPUP,
        MULTIPLIER_POPUP
    }
}
