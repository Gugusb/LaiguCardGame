package com.laigu.laigu.duel.newcard;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * 新卡牌架构的事件分发器。
 * 分发器只负责遍历战斗中的卡牌，不包含任何 cardId 专属规则。
 */
public final class BattleEventDispatcher
{
    public List<AnimationEvent> dispatch(BattleEvent event, Collection<? extends DuelCard> cards,
                                         CardContextFactory contextFactory)
    {
        Objects.requireNonNull(event, "event");
        Objects.requireNonNull(cards, "cards");
        Objects.requireNonNull(contextFactory, "contextFactory");

        List<AnimationEvent> animations = new ArrayList<>();
        for (DuelCard card : List.copyOf(cards))
        {
            CardContext context = contextFactory.create(card, animations);
            card.onEvent(event, context);
        }
        return List.copyOf(animations);
    }

    public List<AnimationEvent> dispatch(BattleEvent event, Collection<CardPlacement> placements, BattleState state)
    {
        Objects.requireNonNull(event, "event");
        Objects.requireNonNull(placements, "placements");
        Objects.requireNonNull(state, "state");

        List<AnimationEvent> animations = new ArrayList<>();
        for (CardPlacement placement : List.copyOf(placements))
        {
            if (event.side() >= 0 && placement.side() != event.side()) continue;
            CardContext context = new BattleCardContext(placement.card(), placement.side(), placement.slot(), state);
            placement.card().onEvent(event, context);
        }
        animations.addAll(state.animations());
        return List.copyOf(animations);
    }

    @FunctionalInterface
    public interface CardContextFactory
    {
        CardContext create(DuelCard card, List<AnimationEvent> animations);
    }
}
