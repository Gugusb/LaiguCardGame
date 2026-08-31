package com.laigu.laigu.duel;

import java.util.EnumMap;
import java.util.Map;

/** 效果类型到执行阶段的唯一登记表，并提供卡目录绑定校验。 */
public final class EffectPhaseRegistry
{
    private static final Map<EffectType, EffectPhase> PHASES = new EnumMap<>(EffectType.class);

    static
    {
        for (EffectType type : EffectType.values()) PHASES.put(type, phaseFor(type));
    }

    private EffectPhaseRegistry() {}

    private static EffectPhase phaseFor(EffectType type)
    {
        return switch (type)
        {
            case DRAFT_SELF_TURNS_UP, DRAFT_OPP_TURNS_DOWN, DRAFT_SELF_GRAB_UP,
                    DRAFT_TURNS_DOWN, DRAFT_TURNS_UP, DRAFT_POOL_UP, REROLL_ON_DRAFT -> EffectPhase.DRAFT;
            case SUMMON_DRAW, SUMMON_DRAW_IF_LOST_LAST, SUMMON_RESTORE_AP -> EffectPhase.SUMMON;
            case LEAVE_DRAW -> EffectPhase.LEAVE;
            case ROUND_START_DRAW, ROUND_START_DRAW_STAY_TURNS, ROUND_START_SCORE_EXTRA -> EffectPhase.ROUND_START;
            case ROUND_END_DRAW_IF_WIN, ROUND_END_DRAW_IF_LOSE, ROUND_END_SCORE_EXTRA -> EffectPhase.ROUND_END;
            case PLACE_SCORE_EXTRA -> EffectPhase.PLACE;
            case FUJI, FUJI_FAIL_EXTRA -> EffectPhase.AMBUSH;
            case ACTIVATE_LEFT -> EffectPhase.ACTIVATE;
            case DRAFT_SCORE_EXTRA, USE_HAND_SCORE_EXTRA -> EffectPhase.TIMING;
            default -> EffectPhase.SETTLEMENT;
        };
    }

    public static EffectPhase phaseOf(EffectType type)
    {
        return PHASES.getOrDefault(type, EffectPhase.RESERVED);
    }

    /** 检查目录中的所有可执行绑定，禁止把预留效果直接绑定到正式卡牌。 */
    public static void validateCatalog(Iterable<DuelCardData> cards)
    {
        for (DuelCardData card : cards)
        {
            requireImplemented(card.cardId, "主效果", card.effect);
            if (card.goldEffect != null) requireImplemented(card.cardId, "金卡焕章", card.goldEffect);
            if (card.activateReward != null) requireImplemented(card.cardId, "激活奖励", card.activateReward);
        }
    }

    private static void requireImplemented(String cardId, String role, EffectType type)
    {
        if (phaseOf(type) == EffectPhase.RESERVED)
        {
            throw new IllegalStateException("未接入的效果绑定：" + cardId + " / " + role + " / " + type);
        }
    }
}
