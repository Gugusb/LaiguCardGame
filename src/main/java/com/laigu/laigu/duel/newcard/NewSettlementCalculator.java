package com.laigu.laigu.duel.newcard;

import java.util.Objects;

/** 执行新版卡牌结算规则并生成结果。 */
public final class NewSettlementCalculator
{
    private NewSettlementCalculator() {}

    public static ScoreSnapshot calculate(NewCardBattle battle)
    {
        return calculate(battle, FrameworkFiveRules.defaultRegistry());
    }

    public static ScoreSnapshot calculate(NewCardBattle battle, SettlementRuleRegistry registry)
    {
        Objects.requireNonNull(battle);
        Objects.requireNonNull(registry);
        BattleState state = battle.state();
        state.clearScores();
        for (int side = 0; side < BattleState.SIDES; side++)
        {
            int base = state.allActiveDice(side).stream().mapToInt(Integer::intValue).sum();
            state.addBaseScoreForRule(side, base);
            state.setMultiplierForRule(side, 1 + state.overflowDrawMultiplier(side));
        }
        // 破阵先行：按各侧破阵卡判定，生成对手槽位的贡献保留比例，供卡牌结算时缩放自身加成。
        double[] keep0 = poZhenKeep(state, 0);
        double[] keep1 = poZhenKeep(state, 1);
        for (CardPlacement placement : battle.placements())
        {
            SettlementContext context = new SettlementContext(state, placement.side(), placement.slot());
            // 本卡贡献保留比例 = 对面破阵卡打在本卡槽位的削弱（keep0/keep1 分别是对 0/1 侧的削弱）。
            context.setContributionKeep(placement.side() == 0 ? keep1[placement.slot()] : keep0[placement.slot()]);
            // 阶段17：每张卡结算前发一次触发事件（客户端跳跃；跳跃次数 = 事件数量）。
            state.emit(new AnimationEvent(AnimationEvent.Type.CARD_TRIGGER,
                    placement.side(), placement.slot(), placement.card().id()));
            registry.resolve(placement.card().id()).apply(context);
            // 一个结算事件只执行一次：实现 OnSettlement 的卡走接口，
            // 未实现的旧迁移卡走 CardEffect 兼容路径，禁止两者叠加。
            if (placement.card() instanceof OnSettlement handler) handler.onSettlement(context);
            else placement.card().onSettlement(context);
        }
        // 阶段18：局内时机通道并入（入场/离场/激活等事件写入的得分与倍率，不参与破阵削弱，
        // 对齐旧引擎 timingBase/timingMult/timingExtra 语义）；并入后清零。
        for (int side = 0; side < BattleState.SIDES; side++)
        {
            state.addBaseScoreForRule(side, state.timingBase(side));
            state.addMultiplierForRule(side, state.timingMult(side));
            state.addExtraScoreForRule(side, state.timingExtra(side));
        }
        state.clearTimingCarry();
        // 焕章破坏收集：结算中卡牌写入的 destroyAtRoundEnd 运行时标记（鸟尊金/飞天金等），
        // 在结算末统一转为破坏目标列表，供对局循环在回合结束时移除卡牌。
        for (int side = 0; side < BattleState.SIDES; side++)
        {
            for (int slot = 0; slot < BattleState.SLOTS; slot++)
            {
                if (state.cardAt(side, slot).isEmpty()) continue;
                if (!state.cardStateAt(side, slot).destroyAtRoundEnd()) continue;
                state.markDestroyAtRoundEnd(new CardContext.CardTarget(side, slot,
                        state.cardStateAt(side, slot).activeDice().size()));
            }
        }
        return BattleScoreAdapter.fromNewState(state);
    }

    /**
     * 破阵判定：本槽位骰面和 > 对手同槽位 → 对手该槽位本卡贡献被削弱
     * （普通 50%，金质 100%）。返回对手侧各槽位的贡献保留比例：
     * 1.0 未削弱，0.5 削半，0.0 全削。旧语义见 ScoreEngine 的 halvePct。
     */
    private static double[] poZhenKeep(BattleState state, int side)
    {
        // result 是「对手侧」各槽位的保留比例；破阵卡在本侧，削弱打在对面。
        double[] opponentKeep = new double[BattleState.SLOTS];
        java.util.Arrays.fill(opponentKeep, 1.0);
        for (int slot = 0; slot < BattleState.SLOTS; slot++)
        {
            DuelCard card = state.cardAt(side, slot).orElse(null);
            if (!(card instanceof PoZhenHandler handler)) continue;
            if (state.cardAt(1 - side, slot).isEmpty()) continue;
            CardRuntimeState self = state.cardStateAt(side, slot);
            int selfSum = sum(self.activeDice());
            int oppSum = sum(state.cardStateAt(1 - side, slot).activeDice());
            if (!handler.poZhenAlwaysSuccess() && !handler.poZhenPersistentSuccess(self) && selfSum <= oppSum) continue;
            boolean gold = card.rarity() == CardRarity.GOLD;
            double reduce = handler.poZhenFullHalve() || gold ? 1.0 : 0.5;
            opponentKeep[slot] = Math.min(opponentKeep[slot], 1.0 - reduce);
            // 金质越杯：破阵成功时同时削弱相邻槽位 ±1（50%）。
            if (gold && handler.poZhenHalveNeighbors())
            {
                if (slot - 1 >= 0) opponentKeep[slot - 1] = Math.min(opponentKeep[slot - 1], 0.5);
                if (slot + 1 < BattleState.SLOTS) opponentKeep[slot + 1] = Math.min(opponentKeep[slot + 1], 0.5);
            }
        }
        return opponentKeep;
    }

    private static int sum(java.util.List<Integer> dice)
    {
        return dice.stream().mapToInt(Integer::intValue).sum();
    }

    /** 破阵卡的判定接口；迁移后的破阵卡实现它以参与统一判定。 */
    public interface PoZhenHandler
    {
        /** 破阵成功后写入自身状态的持久标记键（金质越剑用）。 */
        String PERSISTENT_SUCCESS_KEY = "po_zhen_always_success";
        /** 金卡越剑：首次破阵成功后恒定成功（由卡片在 onPoZhen 中写入持久计数器，结算器读取）。 */
        boolean poZhenAlwaysSuccess();
        /** 金卡越剑：读取自身运行时状态的持久成功标记。 */
        default boolean poZhenPersistentSuccess(CardRuntimeState self) { return false; }
        /** 牛尊：无论普通金质都削弱 100%。 */
        boolean poZhenFullHalve();
        /** 金卡越杯：破阵成功时同时削弱相邻槽位 ±1（50%）。 */
        default boolean poZhenHalveNeighbors() { return false; }
    }
}
