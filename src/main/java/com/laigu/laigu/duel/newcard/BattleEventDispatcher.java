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
        return dispatch(event, placements, state, null);
    }

    public List<AnimationEvent> dispatch(BattleEvent event, Collection<CardPlacement> placements, BattleState state,
                                         CardEventRuleRegistry registry)
    {
        Objects.requireNonNull(event, "event");
        Objects.requireNonNull(placements, "placements");
        Objects.requireNonNull(state, "state");

        List<AnimationEvent> animations = new ArrayList<>();
        for (CardPlacement placement : List.copyOf(placements))
        {
            if (event.side() >= 0 && placement.side() != event.side()) continue;
            boolean target = placement.side() == event.side() && placement.slot() == event.slot();
            CardContext context = new BattleCardContext(placement.card(), placement.side(), placement.slot(), state);
            CardEventRuleRegistry.Rule migratedRule = registry == null ? null : registry.resolve(placement.card().id());
            if (migratedRule != null) migratedRule.apply(event, context);
            else dispatchByInterface(event, placement.card(), context, target);
        }
        animations.addAll(state.animations());
        return List.copyOf(animations);
    }

    /**
     * 按触发接口派发：一张卡实现哪个接口就响应哪个时机，未实现的接口自动跳过。
     * 旧版 CardEffect.onEvent 仍作为兼容路径兜底；新卡实现接口时 effects() 为空，不会重复执行。
     */
    private static void dispatchByInterface(BattleEvent event, DuelCard card, CardContext context, boolean target)
    {
        switch (event.type())
        {
            // 阶段18：入场/离场接口只对事件目标场位生效（onEvent 广播路径不变，
            // 供「其他场位入场」类卡监听；对齐旧引擎 triggerSummon/triggerLeave 单卡语义）。
            case SUMMON -> { if (target && card instanceof OnSummon handler) handler.onSummon(context); }
            case LEAVE -> { if (target && card instanceof OnLeave handler) handler.onLeave(context); }
            case ROUND_START -> { if (card instanceof OnRoundStart handler) handler.onRoundStart(context); }
            case ROUND_END -> { if (card instanceof OnRoundEnd handler) handler.onRoundEnd(context); }
            case ACTIVATION ->
            {
                if (card instanceof OnActivation handler && context.selfActivation() >= handler.activationThreshold())
                    handler.onActivation(context);
            }
            case AMBUSH_SUCCESS -> { if (card instanceof OnAmbushSuccess handler) handler.onAmbushSuccess(context); }
            case AMBUSH_FAIL -> { if (card instanceof OnAmbushFail handler) handler.onAmbushFail(context); }
            case PO_ZHEN -> { if (card instanceof OnPoZhen handler) handler.onPoZhen(context); }
            case DRAFT -> { if (card instanceof OnDraft handler) handler.onDraft(context); }
            case PLACE -> { if (card instanceof OnPlace handler) handler.onPlace(context); }
            // 结算走 SettlementContext 与 OnSettlement 接口，不经 onEvent 派发。
            case SETTLEMENT -> {}
        }
        card.onEvent(event, context);
    }

    @FunctionalInterface
    public interface CardContextFactory
    {
        CardContext create(DuelCard card, List<AnimationEvent> animations);
    }
}
