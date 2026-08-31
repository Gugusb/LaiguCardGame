package com.laigu.laigu.duel.newcard;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * 新卡牌架构的最小战斗适配层。
 * 当前用于隔离和验证新事件链，尚未替换旧 DuelGame。
 */
public final class NewCardBattle
{
    private final BattleState state;
    private final BattleEventDispatcher dispatcher;
    private final List<CardPlacement> placements = new ArrayList<>();

    public NewCardBattle()
    {
        this(new BattleState());
    }

    public NewCardBattle(BattleState state)
    {
        this.state = Objects.requireNonNull(state);
        this.dispatcher = new BattleEventDispatcher();
    }

    public void placeCard(int side, int slot, DuelCard card)
    {
        CardPlacement placement = new CardPlacement(side, slot, Objects.requireNonNull(card));
        for (CardPlacement existing : placements)
        {
            if (existing.side() == side && existing.slot() == slot)
                throw new IllegalArgumentException("场位已被占用：" + side + "/" + slot);
            if (existing.card() == card)
                throw new IllegalArgumentException("同一卡牌实例不能重复放置：" + card.id());
        }
        placements.add(placement);
        state.placeCard(side, slot, card);
    }

    public List<AnimationEvent> placeCard(int side, int slot, DuelCard card, boolean dispatchSummon)
    {
        placeCard(side, slot, card);
        return dispatchSummon
                ? dispatch(new BattleEvent(BattleEvent.Type.SUMMON, side, slot))
                : List.of();
    }

    public List<AnimationEvent> leaveCard(int side, int slot)
    {
        CardPlacement placement = placements.stream()
                .filter(p -> p.side() == side && p.slot() == slot)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("场位没有卡牌：" + side + "/" + slot));
        List<AnimationEvent> events = dispatch(new BattleEvent(BattleEvent.Type.LEAVE, side, slot));
        placements.remove(placement);
        state.removeCard(side, slot);
        return events;
    }

    public List<AnimationEvent> replaceCard(int side, int slot, DuelCard card)
    {
        CardPlacement old = placements.stream()
                .filter(p -> p.side() == side && p.slot() == slot)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("场位没有可替换卡牌：" + side + "/" + slot));
        if (placements.stream().anyMatch(p -> p.card() == card))
            throw new IllegalArgumentException("同一卡牌实例不能重复放置：" + card.id());

        List<AnimationEvent> events = new ArrayList<>(leaveCard(side, slot));
        placeCard(side, slot, card);
        int animationStart = state.animationCount();
        dispatch(new BattleEvent(BattleEvent.Type.SUMMON, side, slot));
        events.addAll(state.animationsFrom(animationStart));
        return List.copyOf(events);
    }

    public void placeCards(Collection<CardPlacement> placements)
    {
        for (CardPlacement placement : placements)
            placeCard(placement.side(), placement.slot(), placement.card());
    }

    public List<AnimationEvent> dispatch(BattleEvent event)
    {
        return dispatcher.dispatch(event, placements, state);
    }

    public List<AnimationEvent> startRound()
    {
        return dispatch(new BattleEvent(BattleEvent.Type.ROUND_START, -1, -1));
    }

    public List<AnimationEvent> endRound()
    {
        List<AnimationEvent> events = dispatch(new BattleEvent(BattleEvent.Type.ROUND_END, -1, -1));
        for (CardPlacement placement : List.copyOf(placements))
        {
            if (state.cardStateAt(placement.side(), placement.slot()).destroyAtRoundEnd())
            {
                events = new ArrayList<>(events);
                events.addAll(leaveCard(placement.side(), placement.slot()));
            }
        }
        state.setRound(state.round() + 1);
        return List.copyOf(events);
    }

    public BattleState state()
    {
        return state;
    }

    public List<CardPlacement> placements()
    {
        return List.copyOf(placements);
    }
}
