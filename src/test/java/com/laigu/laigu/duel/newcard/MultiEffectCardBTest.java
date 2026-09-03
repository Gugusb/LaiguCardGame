package com.laigu.laigu.duel.newcard;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 阶段十：卡牌 B 式多词条测试。
 * 卡牌 B 规则：入场 a+1；激活阈值1 a+1；回合结束骰型为顺子 a+1；
 * 回合结算基础分 += a。
 */
class MultiEffectCardBTest
{
    /** 多词条测试卡：实现 OnSummon/OnActivation/OnRoundEnd/OnSettlement 四个接口。 */
    private static class TestCardB implements DuelCard, OnSummon, OnActivation, OnRoundEnd, OnSettlement
    {
        @Override public String id() { return "test_card_b_common"; }
        @Override public String displayName() { return "测试卡牌B"; }
        @Override public com.laigu.laigu.duel.CardClass cardClass() { return com.laigu.laigu.duel.CardClass.GONG; }

        @Override public int activationThreshold() { return 1; }

        @Override public void onSummon(CardContext context) { context.counter("a").add(1); }

        @Override public void onActivation(CardContext context)
        {
            context.counter("a").add(1);
            // 激活达到阈值后清零，验证激活进度生命周期。
            context.selfState().setActivation(0);
        }

        @Override public void onRoundEnd(CardContext context)
        {
            if (DicePatterns.isStraight(context.selfDice())) context.counter("a").add(1);
        }

        @Override public void onSettlement(SettlementContext context)
        {
            context.addBase(context.selfState().counter("a"));
        }
    }

    private static final class TestCardBGold extends TestCardB
    {
        @Override public String id() { return "test_card_b_gold"; }
    }

    @Test
    void counterAccumulatesThroughSummonActivationRoundEndSettlement()
    {
        NewCardBattle battle = new NewCardBattle();
        battle.placeCard(0, 0, new TestCardB());
        battle.state().cardStateAt(0, 0).addDie(1);
        battle.state().cardStateAt(0, 0).addDie(2);
        battle.state().cardStateAt(0, 0).addDie(3);

        assertEquals(0, battle.state().cardStateAt(0, 0).counter("a"));

        battle.dispatch(new BattleEvent(BattleEvent.Type.SUMMON, 0, 0));
        assertEquals(1, battle.state().cardStateAt(0, 0).counter("a"));

        battle.state().cardStateAt(0, 0).incrementActivation();
        battle.dispatch(new BattleEvent(BattleEvent.Type.ACTIVATION, 0, 0));
        assertEquals(2, battle.state().cardStateAt(0, 0).counter("a"));

        battle.dispatch(new BattleEvent(BattleEvent.Type.ROUND_END, 0, 0));
        assertEquals(3, battle.state().cardStateAt(0, 0).counter("a"));

        // 结算基础分 = 骰面和 1+2+3 + 卡牌词条加的 a=3。
        NewSettlementCalculator.calculate(battle, new SettlementRuleRegistry());
        assertEquals(9, battle.state().baseScore(0));
    }

    @Test
    void nonStraightRoundEndDoesNotAddCounter()
    {
        NewCardBattle battle = new NewCardBattle();
        battle.placeCard(0, 0, new TestCardB());
        battle.state().cardStateAt(0, 0).addDie(1);
        battle.state().cardStateAt(0, 0).addDie(1);
        battle.state().cardStateAt(0, 0).addDie(1);
        battle.dispatch(new BattleEvent(BattleEvent.Type.SUMMON, 0, 0));
        battle.dispatch(new BattleEvent(BattleEvent.Type.ROUND_END, 0, 0));
        // 入场 +1；非顺子回合结束不加。
        assertEquals(1, battle.state().cardStateAt(0, 0).counter("a"));
    }

    @Test
    void separateInstancesHaveIndependentCounters()
    {
        NewCardBattle battle = new NewCardBattle();
        battle.placeCard(0, 0, new TestCardB());
        battle.placeCard(1, 0, new TestCardB());
        battle.dispatch(new BattleEvent(BattleEvent.Type.SUMMON, 0, 0));
        battle.dispatch(new BattleEvent(BattleEvent.Type.SUMMON, 1, 0));

        battle.state().cardStateAt(1, 0).addCounter("a", 5);

        assertEquals(1, battle.state().cardStateAt(0, 0).counter("a"));
        assertEquals(6, battle.state().cardStateAt(1, 0).counter("a"));
    }

    @Test
    void commonAndGoldCountersAreIndependent()
    {
        NewCardBattle battle = new NewCardBattle();
        battle.placeCard(0, 0, new TestCardB());
        battle.placeCard(1, 0, new TestCardBGold());
        battle.dispatch(new BattleEvent(BattleEvent.Type.SUMMON, 0, 0));
        battle.dispatch(new BattleEvent(BattleEvent.Type.SUMMON, 1, 0));

        battle.state().cardStateAt(0, 0).addCounter("a", 4);

        assertEquals(5, battle.state().cardStateAt(0, 0).counter("a"));
        assertEquals(1, battle.state().cardStateAt(1, 0).counter("a"));
        assertNotEquals(battle.placements().get(0).card().getClass(), battle.placements().get(1).card().getClass());
    }

    @Test
    void leavingClearsStateAndReentryStartsFresh()
    {
        NewCardBattle battle = new NewCardBattle();
        battle.placeCard(0, 0, new TestCardB());
        battle.dispatch(new BattleEvent(BattleEvent.Type.SUMMON, 0, 0));
        battle.state().cardStateAt(0, 0).addCounter("a", 9);

        battle.leaveCard(0, 0);
        assertTrue(battle.state().cardAt(0, 0).isEmpty());
        assertEquals(0, battle.state().cardStateAt(0, 0).counter("a"));

        battle.placeCard(0, 0, new TestCardB());
        battle.dispatch(new BattleEvent(BattleEvent.Type.SUMMON, 0, 0));
        assertEquals(1, battle.state().cardStateAt(0, 0).counter("a"));
    }

    @Test
    void countersSurviveSaveAndLoad()
    {
        BattleState state = new BattleState();
        state.placeCard(0, 0, new TestCardB());
        state.cardStateAt(0, 0).addCounter("a", 7);
        state.cardStateAt(0, 0).addPersistentBaseBonus(3);
        net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
        BattleStatePersistence.save(state, tag);

        BattleState restored = BattleStatePersistence.load(tag);

        assertEquals(7, restored.cardStateAt(0, 0).counter("a"));
        assertEquals(3, restored.cardStateAt(0, 0).persistentBaseBonus());
    }

    // ================= 阶段十：触发组合矩阵（每种组合至少一测） =================

    private static NewCardBattle straightDiceBattle()
    {
        NewCardBattle battle = new NewCardBattle();
        battle.placeCard(0, 0, new TestCardB());
        battle.state().cardStateAt(0, 0).addDie(1);
        battle.state().cardStateAt(0, 0).addDie(2);
        battle.state().cardStateAt(0, 0).addDie(3);
        return battle;
    }

    @Test
    void comboSummonPlusActivation()
    {
        NewCardBattle battle = straightDiceBattle();
        battle.dispatch(new BattleEvent(BattleEvent.Type.SUMMON, 0, 0));
        battle.state().cardStateAt(0, 0).incrementActivation();
        battle.dispatch(new BattleEvent(BattleEvent.Type.ACTIVATION, 0, 0));
        // 入场 +1、激活 +1；未结算不产生分数。
        assertEquals(2, battle.state().cardStateAt(0, 0).counter("a"));
        assertEquals(0, battle.state().baseScore(0));
    }

    @Test
    void comboSummonPlusSettlement()
    {
        NewCardBattle battle = straightDiceBattle();
        battle.dispatch(new BattleEvent(BattleEvent.Type.SUMMON, 0, 0));
        NewSettlementCalculator.calculate(battle, new SettlementRuleRegistry());
        // 骰面和 6 + 计数器 a=1。
        assertEquals(7, battle.state().baseScore(0));
    }

    @Test
    void comboActivationPlusSettlement()
    {
        NewCardBattle battle = straightDiceBattle();
        battle.state().cardStateAt(0, 0).incrementActivation();
        battle.dispatch(new BattleEvent(BattleEvent.Type.ACTIVATION, 0, 0));
        NewSettlementCalculator.calculate(battle, new SettlementRuleRegistry());
        // 未入场只走激活：骰面和 6 + 计数器 a=1。
        assertEquals(7, battle.state().baseScore(0));
    }

    @Test
    void comboRoundEndPlusSettlement()
    {
        NewCardBattle battle = straightDiceBattle();
        battle.dispatch(new BattleEvent(BattleEvent.Type.ROUND_END, 0, 0));
        NewSettlementCalculator.calculate(battle, new SettlementRuleRegistry());
        // 顺子回合结束 +1：骰面和 6 + 计数器 a=1。
        assertEquals(7, battle.state().baseScore(0));
    }

    @Test
    void comboSummonActivationRoundEndSettlement()
    {
        NewCardBattle battle = straightDiceBattle();
        battle.dispatch(new BattleEvent(BattleEvent.Type.SUMMON, 0, 0));
        battle.state().cardStateAt(0, 0).incrementActivation();
        battle.dispatch(new BattleEvent(BattleEvent.Type.ACTIVATION, 0, 0));
        battle.dispatch(new BattleEvent(BattleEvent.Type.ROUND_END, 0, 0));
        NewSettlementCalculator.calculate(battle, new SettlementRuleRegistry());
        // 全链路：骰面和 6 + 计数器 a=3。
        assertEquals(9, battle.state().baseScore(0));
    }

    @Test
    void activationOnlyFiresWhenThresholdReached()
    {
        NewCardBattle battle = new NewCardBattle();
        battle.placeCard(0, 0, new TestCardB());
        battle.dispatch(new BattleEvent(BattleEvent.Type.SUMMON, 0, 0));

        battle.dispatch(new BattleEvent(BattleEvent.Type.ACTIVATION, 0, 0));
        assertEquals(1, battle.state().cardStateAt(0, 0).counter("a"));

        battle.state().cardStateAt(0, 0).incrementActivation();
        battle.dispatch(new BattleEvent(BattleEvent.Type.ACTIVATION, 0, 0));
        assertEquals(2, battle.state().cardStateAt(0, 0).counter("a"));
        assertEquals(0, battle.state().cardStateAt(0, 0).activation());
    }
}
