package com.laigu.laigu.duel.newcard;

import com.laigu.laigu.duel.newcard.cards.ChiBiFuYeCommonCard;
import com.laigu.laigu.duel.newcard.cards.ChiBiFuYeGoldCard;
import com.laigu.laigu.duel.newcard.cards.TongBenMaCommonCard;
import com.laigu.laigu.duel.newcard.cards.TongBenMaGoldCard;
import com.laigu.laigu.duel.newcard.cards.TongCheMaCommonCard;
import com.laigu.laigu.duel.newcard.cards.TongCheMaGoldCard;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 批次一迁移测试：基础分数效果卡牌的接口式多词条实现与新旧结果对照。 */
class Batch1BaseScoreMigrationTest
{
    @Test
    void tongCheMaPerDieExtraMatchesLegacyValues()
    {
        // 普通版：每骰 +6 额外分；骰 2/5 → 骰面和 7 + 额外 12。
        NewCardBattle common = new NewCardBattle();
        common.placeCard(0, 0, new TongCheMaCommonCard());
        common.state().cardStateAt(0, 0).setDice(List.of(2, 5));
        ScoreSnapshot commonScore = NewSettlementCalculator.calculate(common, new SettlementRuleRegistry());
        assertEquals(7, commonScore.sides().get(0).base());
        assertEquals(12, commonScore.sides().get(0).extra());

        // 金质版：每骰 +12 额外分。
        NewCardBattle gold = new NewCardBattle();
        gold.placeCard(0, 0, new TongCheMaGoldCard());
        gold.state().cardStateAt(0, 0).setDice(List.of(2, 5));
        ScoreSnapshot goldScore = NewSettlementCalculator.calculate(gold, new SettlementRuleRegistry());
        assertEquals(24, goldScore.sides().get(0).extra());
        assertEquals(0, goldScore.sides().get(0).base() - 7);
    }

    @Test
    void chiBiFuYePerDieBaseMatchesLegacyValues()
    {
        // 普通版：每骰 +2 基础分；骰 1/3 → 基础 4+4=8。
        NewCardBattle common = new NewCardBattle();
        common.placeCard(0, 0, new ChiBiFuYeCommonCard());
        common.state().cardStateAt(0, 0).setDice(List.of(1, 3));
        ScoreSnapshot commonScore = NewSettlementCalculator.calculate(common, new SettlementRuleRegistry());
        assertEquals(8, commonScore.sides().get(0).base());
        assertEquals(0, commonScore.sides().get(0).extra());

        // 金质版：每骰 +4 基础分；骰面和 4 + 4×2 = 12。
        NewCardBattle gold = new NewCardBattle();
        gold.placeCard(0, 0, new ChiBiFuYeGoldCard());
        gold.state().cardStateAt(0, 0).setDice(List.of(1, 3));
        ScoreSnapshot goldScore = NewSettlementCalculator.calculate(gold, new SettlementRuleRegistry());
        assertEquals(12, goldScore.sides().get(0).base());
    }

    @Test
    void tongBenMaDiceThresholdMatchesLegacyValues()
    {
        // 普通版：≥2 颗骰 → +14 额外分；1 颗骰不触发。
        NewCardBattle common = new NewCardBattle();
        common.placeCard(0, 0, new TongBenMaCommonCard());
        common.state().cardStateAt(0, 0).setDice(List.of(6));
        assertEquals(0, NewSettlementCalculator.calculate(common, new SettlementRuleRegistry()).sides().get(0).extra());

        common.state().cardStateAt(0, 0).addDie(3);
        assertEquals(14, NewSettlementCalculator.calculate(common, new SettlementRuleRegistry()).sides().get(0).extra());

        // 金质版：≥2 颗骰 → +28 额外分。
        NewCardBattle gold = new NewCardBattle();
        gold.placeCard(0, 0, new TongBenMaGoldCard());
        gold.state().cardStateAt(0, 0).setDice(List.of(2, 2));
        assertEquals(28, NewSettlementCalculator.calculate(gold, new SettlementRuleRegistry()).sides().get(0).extra());
    }

    @Test
    void batch1CardsAreIndependentClassesNotLegacyMapped()
    {
        assertNotEquals(TongCheMaCommonCard.class, TongCheMaGoldCard.class);
        assertNotEquals(ChiBiFuYeCommonCard.class, ChiBiFuYeGoldCard.class);
        assertNotEquals(TongBenMaCommonCard.class, TongBenMaGoldCard.class);
        // 阶段15：LegacyMappedCard 直映射已删除，所有卡牌均为独立实现。
        assertTrue(OnSettlement.class.isAssignableFrom(TongBenMaCommonCard.class));
        assertTrue(OnSettlement.class.isAssignableFrom(TongCheMaCommonCard.class));
        assertTrue(OnSettlement.class.isAssignableFrom(ChiBiFuYeGoldCard.class));
    }

    @Test
    void batch1CardsShareSettlementEntryWithoutDoubleExecution()
    {
        // 与卡牌 B 同型：接口式卡牌的结算必须只执行一次（NewSettlementCalculator 二选一路径）。
        NewCardBattle battle = new NewCardBattle();
        battle.placeCard(0, 0, new TongCheMaCommonCard());
        battle.placeCard(0, 1, new ChiBiFuYeCommonCard());
        battle.state().cardStateAt(0, 0).setDice(List.of(4));
        battle.state().cardStateAt(0, 1).setDice(List.of(3, 3));
        ScoreSnapshot score = NewSettlementCalculator.calculate(battle, new SettlementRuleRegistry());
        // 骰面和 4+6=10，赤壁赋页每骰+2×2=4 → 基础 14；铜车马每骰+6 → 额外 6。
        assertEquals(14, score.sides().get(0).base());
        assertEquals(6, score.sides().get(0).extra());
    }
}
