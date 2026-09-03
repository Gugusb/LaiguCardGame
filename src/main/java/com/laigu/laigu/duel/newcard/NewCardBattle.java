package com.laigu.laigu.duel.newcard;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/** 新卡牌架构的最小战斗适配层；当前与旧 DuelGame 并行运行。 */
public final class NewCardBattle
{
    private final BattleState state;
    private final BattleEventDispatcher dispatcher;
    private final List<CardPlacement> placements = new ArrayList<>();

    public NewCardBattle() { this(new BattleState()); }
    public NewCardBattle(BattleState state)
    {
        this.state = Objects.requireNonNull(state);
        this.dispatcher = new BattleEventDispatcher();
        // 逐次激活语义：激活进度 +1 后立即定向派发激活事件，由被激活卡的激活词条结算奖励。
        state.setActivationDispatcher((side, slot) ->
                dispatchToCard(side, slot, new BattleEvent(BattleEvent.Type.ACTIVATION, side, slot)));
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

    /** 影子同步专用：只更新场位，不触发新规则事件。 */
    public void synchronizeCard(int side, int slot, DuelCard card)
    {
        CardPlacement old = placements.stream().filter(p -> p.side() == side && p.slot() == slot).findFirst().orElse(null);
        if (old != null && old.card().getClass() == card.getClass()) return;
        if (old != null) { placements.remove(old); state.removeCard(side, slot); }
        placements.removeIf(p -> p.card() == card);
        placements.add(new CardPlacement(side, slot, card));
        state.placeCard(side, slot, card);
    }

    public void synchronizeEmpty(int side, int slot)
    {
        placements.removeIf(p -> p.side() == side && p.slot() == slot);
        state.removeCard(side, slot);
    }

    public List<AnimationEvent> placeCard(int side, int slot, DuelCard card, boolean dispatchSummon)
    {
        placeCard(side, slot, card);
        return dispatchSummon ? dispatch(new BattleEvent(BattleEvent.Type.SUMMON, side, slot)) : List.of();
    }

    public List<AnimationEvent> leaveCard(int side, int slot)
    {
        CardPlacement placement = placements.stream().filter(p -> p.side() == side && p.slot() == slot).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("场位没有卡牌：" + side + "/" + slot));
        List<AnimationEvent> events = dispatch(new BattleEvent(BattleEvent.Type.LEAVE, side, slot));
        placements.remove(placement);
        state.removeCard(side, slot);
        return events;
    }

    public List<AnimationEvent> replaceCard(int side, int slot, DuelCard card)
    {
        CardPlacement old = placements.stream().filter(p -> p.side() == side && p.slot() == slot).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("场位没有可替换卡牌：" + side + "/" + slot));
        if (placements.stream().anyMatch(p -> p.card() == card)) throw new IllegalArgumentException("同一卡牌实例不能重复放置：" + card.id());
        List<AnimationEvent> events = new ArrayList<>(leaveCard(side, slot));
        placeCard(side, slot, card);
        int animationStart = state.animationCount();
        dispatch(new BattleEvent(BattleEvent.Type.SUMMON, side, slot));
        events.addAll(state.animationsFrom(animationStart));
        return List.copyOf(events);
    }

    public void placeCards(Collection<CardPlacement> placements) { for (CardPlacement p : placements) placeCard(p.side(), p.slot(), p.card()); }
    public List<AnimationEvent> dispatch(BattleEvent event) { return dispatcher.dispatch(event, placements, state); }

    /** 将事件只派发给指定场位的卡牌（激活链等定向触发；对齐旧引擎单卡激活语义）。 */
    public List<AnimationEvent> dispatchToCard(int side, int slot, BattleEvent event)
    {
        for (CardPlacement placement : placements)
        {
            if (placement.side() != side || placement.slot() != slot) continue;
            return dispatcher.dispatch(event, List.of(placement), state);
        }
        return List.of();
    }
    public List<AnimationEvent> dispatch(BattleEvent event, CardEventRuleRegistry registry)
    {
        return dispatcher.dispatch(event, placements, state, registry);
    }

    /** 使用框架5默认事件规则；未迁移卡牌仍由原卡牌实现处理。 */
    public List<AnimationEvent> dispatchFrameworkFive(BattleEvent event)
    {
        return dispatch(event, FrameworkFiveEventRules.defaultRegistry());
    }

    /** 回合开始：清空上轮抓取计划与重骰计数；推进场上卡状态（对齐旧引擎：幸存卡标记上轮在场、连续在场轮数+1、激活进度清零）。 */
    public List<AnimationEvent> startRound()
    {
        state.clearDraftPlan();
        state.clearRoundScopedViews();
        state.resetRerollUses();
        // 回合开始推进场上卡状态（对齐旧引擎：幸存卡标记上轮在场、连续在场轮数+1、激活进度清零）。
        for (CardPlacement placement : List.copyOf(placements))
        {
            CardRuntimeState runtime = state.cardStateAt(placement.side(), placement.slot());
            runtime.setLastedLastRound(true);
            runtime.incrementRoundsOnField();
            runtime.setActivation(0);
        }
        return dispatch(new BattleEvent(BattleEvent.Type.ROUND_START, -1, -1));
    }

    public List<AnimationEvent> endRound()
    {
        List<AnimationEvent> events = new ArrayList<>(dispatch(new BattleEvent(BattleEvent.Type.ROUND_END, -1, -1)));
        for (CardPlacement placement : List.copyOf(placements))
        {
            if (state.cardStateAt(placement.side(), placement.slot()).destroyAtRoundEnd())
                events.addAll(leaveCard(placement.side(), placement.slot()));
        }
        state.setRound(state.round() + 1);
        return List.copyOf(events);
    }

    public BattleState state() { return state; }
    public List<CardPlacement> placements() { return List.copyOf(placements); }

    /**
     * 抓骰副作用（阶段16；对齐旧 pickDie 的副作用序列）：向抓取方派发 DRAFT 事件
     * （value=抓取点数），返回 {向共享池补入的随机骰颗数, 抓取得分额外分}。
     */
    public int[] onGrabEffects(int side, int face)
    {
        int before = state.extraScore(side);
        dispatch(new BattleEvent(BattleEvent.Type.DRAFT, side, -1, face));
        int scoreDelta = state.extraScore(side) - before;
        // 星月夜金卡：任一方抓骰后共享池 +1 随机骰（旧引擎硬编码的忠实迁移，确认稿 Q3）。
        int poolExtra = 0;
        if (placements.stream().anyMatch(p -> "xing_yue_ye_gold".equals(p.card().id())))
        {
            state.addSharedPoolDie(state.rollDie());
            poolExtra = 1;
        }
        return new int[] {poolExtra, scoreDelta};
    }
}
