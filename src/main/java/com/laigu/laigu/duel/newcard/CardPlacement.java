package com.laigu.laigu.duel.newcard;

import java.util.Objects;

/** 明确记录一张场上卡牌的对战方与槽位。 */
public record CardPlacement(int side, int slot, DuelCard card)
{
    public CardPlacement
    {
        if (side < 0 || side >= BattleState.SIDES) throw new IllegalArgumentException("无效对战方：" + side);
        if (slot < 0 || slot >= BattleState.SLOTS) throw new IllegalArgumentException("无效场位：" + slot);
        Objects.requireNonNull(card, "card");
    }
}
