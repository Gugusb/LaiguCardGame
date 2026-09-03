package com.laigu.laigu.duel.newcard;

import com.laigu.laigu.duel.newcard.cards.BaiShiSanLeCommonCard;
import com.laigu.laigu.duel.newcard.cards.BaiShiSanLeGoldCard;
import com.laigu.laigu.duel.newcard.cards.HaiShuiJiangYaLuCommonCard;
import com.laigu.laigu.duel.newcard.cards.HaiShuiJiangYaLuGoldCard;
import com.laigu.laigu.duel.newcard.cards.LiGuiCommonCard;
import com.laigu.laigu.duel.newcard.cards.LiGuiGoldCard;
import com.laigu.laigu.duel.newcard.cards.LiMaoPanCommonCard;
import com.laigu.laigu.duel.newcard.cards.LiMaoPanGoldCard;
import com.laigu.laigu.duel.newcard.cards.MoGaoKuJiCommonCard;
import com.laigu.laigu.duel.newcard.cards.MoGaoKuJiGoldCard;
import com.laigu.laigu.duel.newcard.cards.TianWangShiKeCommonCard;
import com.laigu.laigu.duel.newcard.cards.TianWangShiKeGoldCard;
import com.laigu.laigu.duel.newcard.cards.WangShiShuHanJuanCommonCard;
import com.laigu.laigu.duel.newcard.cards.WangShiShuHanJuanGoldCard;
import com.laigu.laigu.duel.newcard.cards.YinQueShanHanJian1CommonCard;
import com.laigu.laigu.duel.newcard.cards.YinQueShanHanJian1GoldCard;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 批次二迁移测试：骰型效果卡牌的接口式实现与新旧结果对照。 */
class Batch2DicePatternMigrationTest
{
    @Test
    void yinQueShanHanJian1StraightMultiplierMatchesLegacy()
    {
        // 普通：顺子 1-2-3 → +4 倍率。
        NewCardBattle common = new NewCardBattle();
        common.placeCard(0, 0, new YinQueShanHanJian1CommonCard());
        common.state().cardStateAt(0, 0).setDice(List.of(1, 2, 3));
        ScoreSnapshot commonScore = NewSettlementCalculator.calculate(common, new SettlementRuleRegistry());
        assertEquals(5, commonScore.sides().get(0).multiplier());

        // 金质：顺子 → +8 倍率。
        NewCardBattle gold = new NewCardBattle();
        gold.placeCard(0, 0, new YinQueShanHanJian1GoldCard());
        gold.state().cardStateAt(0, 0).setDice(List.of(1, 2, 3));
        assertEquals(9, NewSettlementCalculator.calculate(gold, new SettlementRuleRegistry()).sides().get(0).multiplier());
    }

    @Test
    void straightIsFalseForBrokenSequence()
    {
        // 1-2-4 不是顺子；2-3-5 跨度超出也不算（对齐旧引擎 Conds.straight 语义）。
        NewCardBattle battle = new NewCardBattle();
        battle.placeCard(0, 0, new YinQueShanHanJian1CommonCard());
        battle.state().cardStateAt(0, 0).setDice(List.of(1, 2, 4));
        assertEquals(1, NewSettlementCalculator.calculate(battle, new SettlementRuleRegistry()).sides().get(0).multiplier());
    }

    @Test
    void tianWangShiKeAllHighMultiplierMatchesLegacy()
    {
        NewCardBattle common = new NewCardBattle();
        common.placeCard(0, 0, new TianWangShiKeCommonCard());
        common.state().cardStateAt(0, 0).setDice(List.of(4, 5, 6));
        assertEquals(4, NewSettlementCalculator.calculate(common, new SettlementRuleRegistry()).sides().get(0).multiplier());

        NewCardBattle gold = new NewCardBattle();
        gold.placeCard(0, 0, new TianWangShiKeGoldCard());
        gold.state().cardStateAt(0, 0).setDice(List.of(4, 5, 6));
        assertEquals(7, NewSettlementCalculator.calculate(gold, new SettlementRuleRegistry()).sides().get(0).multiplier());

        // 含 3 → 非全大，不触发。
        NewCardBattle mixed = new NewCardBattle();
        mixed.placeCard(0, 0, new TianWangShiKeCommonCard());
        mixed.state().cardStateAt(0, 0).setDice(List.of(3, 5, 6));
        assertEquals(1, NewSettlementCalculator.calculate(mixed, new SettlementRuleRegistry()).sides().get(0).multiplier());
    }

    @Test
    void moGaoKuJiAllLowExtraMatchesLegacy()
    {
        NewCardBattle common = new NewCardBattle();
        common.placeCard(0, 0, new MoGaoKuJiCommonCard());
        common.state().cardStateAt(0, 0).setDice(List.of(1, 2));
        assertEquals(30, NewSettlementCalculator.calculate(common, new SettlementRuleRegistry()).sides().get(0).extra());

        NewCardBattle gold = new NewCardBattle();
        gold.placeCard(0, 0, new MoGaoKuJiGoldCard());
        gold.state().cardStateAt(0, 0).setDice(List.of(1, 2));
        assertEquals(60, NewSettlementCalculator.calculate(gold, new SettlementRuleRegistry()).sides().get(0).extra());

        // 含 4 → 非全小。
        NewCardBattle mixed = new NewCardBattle();
        mixed.placeCard(0, 0, new MoGaoKuJiCommonCard());
        mixed.state().cardStateAt(0, 0).setDice(List.of(1, 4));
        assertEquals(0, NewSettlementCalculator.calculate(mixed, new SettlementRuleRegistry()).sides().get(0).extra());
    }

    @Test
    void liMaoPanHasSixExtraMatchesLegacy()
    {
        NewCardBattle common = new NewCardBattle();
        common.placeCard(0, 0, new LiMaoPanCommonCard());
        common.state().cardStateAt(0, 0).setDice(List.of(2, 6));
        assertEquals(10, NewSettlementCalculator.calculate(common, new SettlementRuleRegistry()).sides().get(0).extra());

        NewCardBattle gold = new NewCardBattle();
        gold.placeCard(0, 0, new LiMaoPanGoldCard());
        gold.state().cardStateAt(0, 0).setDice(List.of(2, 6));
        assertEquals(20, NewSettlementCalculator.calculate(gold, new SettlementRuleRegistry()).sides().get(0).extra());

        // 无 6。
        NewCardBattle none = new NewCardBattle();
        none.placeCard(0, 0, new LiMaoPanCommonCard());
        none.state().cardStateAt(0, 0).setDice(List.of(2, 5));
        assertEquals(0, NewSettlementCalculator.calculate(none, new SettlementRuleRegistry()).sides().get(0).extra());
    }

    @Test
    void haiShuiJiangYaLuTwoPairMatchesLegacy()
    {
        NewCardBattle common = new NewCardBattle();
        common.placeCard(0, 0, new HaiShuiJiangYaLuCommonCard());
        common.state().cardStateAt(0, 0).setDice(List.of(2, 2, 5, 5));
        assertEquals(4, NewSettlementCalculator.calculate(common, new SettlementRuleRegistry()).sides().get(0).multiplier());

        NewCardBattle gold = new NewCardBattle();
        gold.placeCard(0, 0, new HaiShuiJiangYaLuGoldCard());
        gold.state().cardStateAt(0, 0).setDice(List.of(2, 2, 5, 5));
        assertEquals(7, NewSettlementCalculator.calculate(gold, new SettlementRuleRegistry()).sides().get(0).multiplier());

        // 一对不算两对。
        NewCardBattle one = new NewCardBattle();
        one.placeCard(0, 0, new HaiShuiJiangYaLuCommonCard());
        one.state().cardStateAt(0, 0).setDice(List.of(2, 2, 5));
        assertEquals(1, NewSettlementCalculator.calculate(one, new SettlementRuleRegistry()).sides().get(0).multiplier());
    }

    @Test
    void liGuiSameFacePairMatchesLegacy()
    {
        NewCardBattle common = new NewCardBattle();
        common.placeCard(0, 0, new LiGuiCommonCard());
        common.state().cardStateAt(0, 0).setDice(List.of(3, 3));
        assertEquals(3, NewSettlementCalculator.calculate(common, new SettlementRuleRegistry()).sides().get(0).multiplier());

        NewCardBattle gold = new NewCardBattle();
        gold.placeCard(0, 0, new LiGuiGoldCard());
        gold.state().cardStateAt(0, 0).setDice(List.of(3, 3));
        assertEquals(5, NewSettlementCalculator.calculate(gold, new SettlementRuleRegistry()).sides().get(0).multiplier());

        // 3 颗骰相同不触发（旧规则限定 2 颗）。
        NewCardBattle three = new NewCardBattle();
        three.placeCard(0, 0, new LiGuiCommonCard());
        three.state().cardStateAt(0, 0).setDice(List.of(3, 3, 3));
        assertEquals(1, NewSettlementCalculator.calculate(three, new SettlementRuleRegistry()).sides().get(0).multiplier());
    }

    @Test
    void wangShiShuHanJuanSumRangeMatchesLegacy()
    {
        // 和 ≤9：1+2=3。
        NewCardBattle low = new NewCardBattle();
        low.placeCard(0, 0, new WangShiShuHanJuanCommonCard());
        low.state().cardStateAt(0, 0).setDice(List.of(1, 2));
        assertEquals(18, NewSettlementCalculator.calculate(low, new SettlementRuleRegistry()).sides().get(0).extra());

        // 和 ≥18：6+6+6=18。
        NewCardBattle high = new NewCardBattle();
        high.placeCard(0, 0, new WangShiShuHanJuanCommonCard());
        high.state().cardStateAt(0, 0).setDice(List.of(6, 6, 6));
        assertEquals(18, NewSettlementCalculator.calculate(high, new SettlementRuleRegistry()).sides().get(0).extra());

        // 中间地带 5+5=10 不触发。
        NewCardBattle middle = new NewCardBattle();
        middle.placeCard(0, 0, new WangShiShuHanJuanCommonCard());
        middle.state().cardStateAt(0, 0).setDice(List.of(5, 5));
        assertEquals(0, NewSettlementCalculator.calculate(middle, new SettlementRuleRegistry()).sides().get(0).extra());

        // 金质：+36。
        NewCardBattle gold = new NewCardBattle();
        gold.placeCard(0, 0, new WangShiShuHanJuanGoldCard());
        gold.state().cardStateAt(0, 0).setDice(List.of(1, 2));
        assertEquals(36, NewSettlementCalculator.calculate(gold, new SettlementRuleRegistry()).sides().get(0).extra());
    }

    @Test
    void baiShiSanLeConsecutiveNearMatchesLegacy()
    {
        NewCardBattle common = new NewCardBattle();
        common.placeCard(0, 0, new BaiShiSanLeCommonCard());
        common.state().cardStateAt(0, 0).setDice(List.of(2, 3));
        assertEquals(10, NewSettlementCalculator.calculate(common, new SettlementRuleRegistry()).sides().get(0).extra());

        // 2-4-6 无相邻；2-5 也不相邻。
        NewCardBattle gap = new NewCardBattle();
        gap.placeCard(0, 0, new BaiShiSanLeCommonCard());
        gap.state().cardStateAt(0, 0).setDice(List.of(2, 4, 6));
        assertEquals(0, NewSettlementCalculator.calculate(gap, new SettlementRuleRegistry()).sides().get(0).extra());

        NewCardBattle separated = new NewCardBattle();
        separated.placeCard(0, 0, new BaiShiSanLeCommonCard());
        separated.state().cardStateAt(0, 0).setDice(List.of(2, 5));
        assertEquals(0, NewSettlementCalculator.calculate(separated, new SettlementRuleRegistry()).sides().get(0).extra());

        NewCardBattle gold = new NewCardBattle();
        gold.placeCard(0, 0, new BaiShiSanLeGoldCard());
        gold.state().cardStateAt(0, 0).setDice(List.of(2, 3));
        assertEquals(20, NewSettlementCalculator.calculate(gold, new SettlementRuleRegistry()).sides().get(0).extra());
    }

    @Test
    void batch2CardsAreIndependentOnSettlementClasses()
    {
        assertNotEquals(YinQueShanHanJian1CommonCard.class, YinQueShanHanJian1GoldCard.class);
        assertNotEquals(HaiShuiJiangYaLuCommonCard.class, HaiShuiJiangYaLuGoldCard.class);
        // 阶段15：直映射已删除，天长歌以 OnSettlement 独立实现。
        assertTrue(OnSettlement.class.isAssignableFrom(TianWangShiKeCommonCard.class));
        assertTrue(OnSettlement.class.isAssignableFrom(MoGaoKuJiGoldCard.class));
        assertTrue(OnSettlement.class.isAssignableFrom(LiGuiCommonCard.class));
        assertTrue(OnSettlement.class.isAssignableFrom(WangShiShuHanJuanGoldCard.class));
    }
}
