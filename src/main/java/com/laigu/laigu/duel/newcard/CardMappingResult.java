package com.laigu.laigu.duel.newcard;

import java.util.Objects;
import java.util.Optional;

/** ItemStack 到新版卡牌规则的可审计映射结果。 */
public record CardMappingResult(Status status, String itemPath, String cardId, DuelCard card)
{
    public enum Status { MAPPED, EMPTY, NOT_CARD, UNMIGRATED }

    public CardMappingResult
    {
        Objects.requireNonNull(status);
        itemPath = itemPath == null ? "" : itemPath;
        cardId = cardId == null ? "" : cardId;
    }

    public Optional<DuelCard> cardOptional() { return Optional.ofNullable(card); }
    public boolean mapped() { return status == Status.MAPPED && card != null; }
}
