package com.laigu.laigu.duel.newcard;

import com.laigu.laigu.duel.CardClass;

import java.util.Optional;

/** 场面判断公共工具；只做判断，不代表任何具体卡牌。 */
public final class FieldConditions
{
    private FieldConditions() {}

    public static boolean hasAdjacentCard(CardContext context)
    {
        return context.leftCard().isPresent() || context.rightCard().isPresent();
    }

    /** 相邻卡牌是否与自身同朝代（以 CardClass 职业近似判定，需同职业）。 */
    public static boolean hasSameClass(CardContext context)
    {
        return adjacentMatches(context, context.self().cardClass());
    }

    public static boolean hasSameDynasty(CardContext context)
    {
        return hasSameClass(context);
    }

    public static boolean hasGoldCard(CardContext context)
    {
        return context.selfState() != null && context.self().rarity() == CardRarity.GOLD;
    }

    /** 对手场上是否满 5 个场位。 */
    public static boolean opponentFieldFull(BattleState state, int side)
    {
        return state.field(1 - side).stream().allMatch(java.util.Objects::nonNull);
    }

    /** 自身是否孤立：左右都没有相邻卡牌。 */
    public static boolean isIsolated(CardContext context)
    {
        return context.leftCard().isEmpty() && context.rightCard().isEmpty();
    }

    private static boolean adjacentMatches(CardContext context, CardClass cardClass)
    {
        for (Optional<CardContext.CardTarget> adjacent : java.util.List.of(context.leftCard(), context.rightCard()))
        {
            if (adjacent.isEmpty()) continue;
            CardContext.CardTarget target = adjacent.get();
            if (stateCardClass(context, target).filter(c -> c == cardClass).isPresent()) return true;
        }
        return false;
    }

    private static java.util.Optional<CardClass> stateCardClass(CardContext context, CardContext.CardTarget target)
    {
        if (!(context instanceof BattleCardContext battleContext)) return java.util.Optional.empty();
        return battleContext.state().cardAt(target.side(), target.slot())
                .map(card -> ((DuelCard) card).cardClass());
    }
}
