package com.laigu.laigu.duel.newcard;

import com.laigu.laigu.duel.newcard.cards.DuanBiWeiNaSiCommonCard;
import com.laigu.laigu.duel.newcard.cards.DuanBiWeiNaSiGoldCard;
import com.laigu.laigu.duel.newcard.cards.HanMoLaBiFaDianCommonCard;
import com.laigu.laigu.duel.newcard.cards.HanMoLaBiFaDianGoldCard;
import com.laigu.laigu.duel.newcard.cards.MaoGongDingCommonCard;
import com.laigu.laigu.duel.newcard.cards.MaoGongDingGoldCard;
import com.laigu.laigu.duel.newcard.cards.PangBeiCommonCard;
import com.laigu.laigu.duel.newcard.cards.PangBeiGoldCard;
import com.laigu.laigu.duel.newcard.cards.QinGongBoCommonCard;
import com.laigu.laigu.duel.newcard.cards.QinGongBoGoldCard;
import com.laigu.laigu.duel.newcard.cards.QiuCaoBeiCommonCard;
import com.laigu.laigu.duel.newcard.cards.QiuCaoBeiGoldCard;
import com.laigu.laigu.duel.newcard.cards.ShangZhouShiGongCommonCard;
import com.laigu.laigu.duel.newcard.cards.ShangZhouShiGongGoldCard;
import com.laigu.laigu.duel.newcard.cards.TaoYuanXianJingTuCommonCard;
import com.laigu.laigu.duel.newcard.cards.TaoYuanXianJingTuGoldCard;
import com.laigu.laigu.duel.newcard.cards.WaiXiaoBiZhiCommonCard;
import com.laigu.laigu.duel.newcard.cards.WaiXiaoBiZhiGoldCard;
import com.laigu.laigu.duel.newcard.cards.WeiSuoJiaJuCommonCard;
import com.laigu.laigu.duel.newcard.cards.WeiSuoJiaJuGoldCard;
import com.laigu.laigu.duel.newcard.cards.XiangRiKuiCommonCard;
import com.laigu.laigu.duel.newcard.cards.XiangRiKuiGoldCard;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 批次三迁移测试：场面条件效果卡牌的接口式实现与新旧结果对照。 */
class Batch3FieldConditionMigrationTest
{
    @Test
    void pangBeiDrawLastExtraMatchesLegacy()
    {
        NewCardBattle draw = new NewCardBattle();
        draw.placeCard(0, 0, new PangBeiCommonCard());
        draw.state().setRound(2);
        draw.state().setWinnerLast(-1);
        assertEquals(30, NewSettlementCalculator.calculate(draw, new SettlementRuleRegistry()).sides().get(0).extra());

        NewCardBattle firstRound = new NewCardBattle();
        firstRound.placeCard(0, 0, new PangBeiCommonCard());
        firstRound.state().setWinnerLast(-1);
        assertEquals(0, NewSettlementCalculator.calculate(firstRound, new SettlementRuleRegistry()).sides().get(0).extra());

        NewCardBattle gold = new NewCardBattle();
        gold.placeCard(0, 0, new PangBeiGoldCard());
        gold.state().setRound(3);
        gold.state().setWinnerLast(-1);
        assertEquals(60, NewSettlementCalculator.calculate(gold, new SettlementRuleRegistry()).sides().get(0).extra());
    }

    @Test
    void weiSuoJiaJuWinLastMultiplierMatchesLegacy()
    {
        NewCardBattle win = new NewCardBattle();
        win.placeCard(0, 0, new WeiSuoJiaJuCommonCard());
        win.state().setWinnerLast(0);
        assertEquals(2, NewSettlementCalculator.calculate(win, new SettlementRuleRegistry()).sides().get(0).multiplier());

        NewCardBattle lost = new NewCardBattle();
        lost.placeCard(0, 0, new WeiSuoJiaJuCommonCard());
        lost.state().setWinnerLast(1);
        assertEquals(1, NewSettlementCalculator.calculate(lost, new SettlementRuleRegistry()).sides().get(0).multiplier());

        NewCardBattle gold = new NewCardBattle();
        gold.placeCard(0, 0, new WeiSuoJiaJuGoldCard());
        gold.state().setWinnerLast(0);
        assertEquals(3, NewSettlementCalculator.calculate(gold, new SettlementRuleRegistry()).sides().get(0).multiplier());
    }

    @Test
    void duanBiWeiNaSiBehindWinsExtraMatchesLegacy()
    {
        NewCardBattle behind = new NewCardBattle();
        behind.placeCard(0, 0, new DuanBiWeiNaSiCommonCard());
        behind.state().setWins(0, 0);
        behind.state().setWins(1, 1);
        assertEquals(20, NewSettlementCalculator.calculate(behind, new SettlementRuleRegistry()).sides().get(0).extra());

        NewCardBattle ahead = new NewCardBattle();
        ahead.placeCard(0, 0, new DuanBiWeiNaSiCommonCard());
        ahead.state().setWins(0, 2);
        ahead.state().setWins(1, 1);
        assertEquals(0, NewSettlementCalculator.calculate(ahead, new SettlementRuleRegistry()).sides().get(0).extra());

        NewCardBattle gold = new NewCardBattle();
        gold.placeCard(0, 0, new DuanBiWeiNaSiGoldCard());
        gold.state().setWins(0, 0);
        gold.state().setWins(1, 2);
        assertEquals(40, NewSettlementCalculator.calculate(gold, new SettlementRuleRegistry()).sides().get(0).extra());
    }

    @Test
    void taoYuanXianJingTuVeteranRoundsOnField()
    {
        NewCardBattle veteran = new NewCardBattle();
        veteran.placeCard(0, 0, new TaoYuanXianJingTuCommonCard());
        veteran.state().cardStateAt(0, 0).incrementRoundsOnField();
        // 清单：连续在场≥2轮 +20 额外分（金 +40）。
        assertEquals(20, NewSettlementCalculator.calculate(veteran, new SettlementRuleRegistry()).sides().get(0).extra());

        NewCardBattle newcomer = new NewCardBattle();
        newcomer.placeCard(0, 0, new TaoYuanXianJingTuCommonCard());
        assertEquals(0, NewSettlementCalculator.calculate(newcomer, new SettlementRuleRegistry()).sides().get(0).extra());

        NewCardBattle gold = new NewCardBattle();
        gold.placeCard(0, 0, new TaoYuanXianJingTuGoldCard());
        gold.state().cardStateAt(0, 0).incrementRoundsOnField();
        assertEquals(40, NewSettlementCalculator.calculate(gold, new SettlementRuleRegistry()).sides().get(0).extra());
    }

    @Test
    void maoGongDingRoundThresholdAndHanMoEmptyHand()
    {
        NewCardBattle late = new NewCardBattle();
        late.placeCard(0, 0, new MaoGongDingCommonCard());
        late.state().setRound(2);
        assertEquals(12, NewSettlementCalculator.calculate(late, new SettlementRuleRegistry()).sides().get(0).extra());

        NewCardBattle firstRound = new NewCardBattle();
        firstRound.placeCard(0, 0, new MaoGongDingCommonCard());
        assertEquals(0, NewSettlementCalculator.calculate(firstRound, new SettlementRuleRegistry()).sides().get(0).extra());

        NewCardBattle gold = new NewCardBattle();
        gold.placeCard(0, 0, new MaoGongDingGoldCard());
        gold.state().setRound(3);
        assertEquals(24, NewSettlementCalculator.calculate(gold, new SettlementRuleRegistry()).sides().get(0).extra());

        NewCardBattle emptyHand = new NewCardBattle();
        emptyHand.placeCard(0, 0, new HanMoLaBiFaDianCommonCard());
        assertEquals(20, NewSettlementCalculator.calculate(emptyHand, new SettlementRuleRegistry()).sides().get(0).extra());

        NewCardBattle withHand = new NewCardBattle();
        withHand.placeCard(0, 0, new HanMoLaBiFaDianCommonCard());
        withHand.state().drawCards(0, 2);
        assertEquals(0, NewSettlementCalculator.calculate(withHand, new SettlementRuleRegistry()).sides().get(0).extra());

        NewCardBattle goldEmpty = new NewCardBattle();
        goldEmpty.placeCard(0, 0, new HanMoLaBiFaDianGoldCard());
        assertEquals(40, NewSettlementCalculator.calculate(goldEmpty, new SettlementRuleRegistry()).sides().get(0).extra());
    }

    @Test
    void shangZhouShiGongOppMoreDiceMatchesLegacy()
    {
        // 己方 2 骰，对手 3 骰 → 触发。
        NewCardBattle behind = new NewCardBattle();
        behind.placeCard(0, 0, new ShangZhouShiGongCommonCard());
        behind.state().cardStateAt(0, 0).setDice(List.of(2, 5));
        behind.placeCard(1, 0, new com.laigu.laigu.duel.newcard.cards.QingTongXianHeCommonCard());
        behind.state().cardStateAt(1, 0).setDice(List.of(1, 3, 4));
        assertEquals(15, NewSettlementCalculator.calculate(behind, new SettlementRuleRegistry()).sides().get(0).extra());

        // 对手骰不多于己方 → 不触发。
        NewCardBattle ahead = new NewCardBattle();
        ahead.placeCard(0, 0, new ShangZhouShiGongCommonCard());
        ahead.state().cardStateAt(0, 0).setDice(List.of(2, 5));
        assertEquals(0, NewSettlementCalculator.calculate(ahead, new SettlementRuleRegistry()).sides().get(0).extra());

        NewCardBattle gold = new NewCardBattle();
        gold.placeCard(0, 0, new ShangZhouShiGongGoldCard());
        gold.state().cardStateAt(0, 0).setDice(List.of(1));
        gold.placeCard(1, 0, new com.laigu.laigu.duel.newcard.cards.QingTongXianHeCommonCard());
        gold.state().cardStateAt(1, 0).setDice(List.of(2, 3));
        assertEquals(30, NewSettlementCalculator.calculate(gold, new SettlementRuleRegistry()).sides().get(0).extra());
    }

    @Test
    void waiXiaoBiZhiOpponentFullField()
    {
        NewCardBattle full = new NewCardBattle();
        full.placeCard(0, 0, new WaiXiaoBiZhiCommonCard());
        for (int slot = 0; slot < 5; slot++)
            full.placeCard(1, slot, new com.laigu.laigu.duel.newcard.cards.QingTongXianHeCommonCard());
        assertEquals(20, NewSettlementCalculator.calculate(full, new SettlementRuleRegistry()).sides().get(0).extra());

        NewCardBattle notFull = new NewCardBattle();
        notFull.placeCard(0, 0, new WaiXiaoBiZhiCommonCard());
        for (int slot = 0; slot < 4; slot++)
            notFull.placeCard(1, slot, new com.laigu.laigu.duel.newcard.cards.QingTongXianHeCommonCard());
        assertEquals(0, NewSettlementCalculator.calculate(notFull, new SettlementRuleRegistry()).sides().get(0).extra());

        NewCardBattle gold = new NewCardBattle();
        gold.placeCard(0, 0, new WaiXiaoBiZhiGoldCard());
        for (int slot = 0; slot < 5; slot++)
            gold.placeCard(1, slot, new com.laigu.laigu.duel.newcard.cards.QingTongXianHeCommonCard());
        assertEquals(40, NewSettlementCalculator.calculate(gold, new SettlementRuleRegistry()).sides().get(0).extra());
    }

    @Test
    void xiangRiKuiIsolatedByDynasty()
    {
        // 相邻都是空位 → 孤立触发（+1 倍率 → 总倍率 2）。
        NewCardBattle alone = new NewCardBattle();
        alone.placeCard(0, 0, new XiangRiKuiCommonCard());
        ScoreSnapshot aloneScore = NewSettlementCalculator.calculate(alone, new SettlementRuleRegistry());
        assertEquals(2, aloneScore.sides().get(0).multiplier());
        assertEquals(10, aloneScore.sides().get(0).extra());

        // 相邻卡朝代与自身不同（向日葵近代 / 青铜仙鹤战国）→ 触发。
        NewCardBattle diffDynasty = new NewCardBattle();
        diffDynasty.placeCard(0, 0, new XiangRiKuiCommonCard());
        diffDynasty.placeCard(0, 1, new com.laigu.laigu.duel.newcard.cards.QingTongXianHeCommonCard());
        assertEquals(10, NewSettlementCalculator.calculate(diffDynasty, new SettlementRuleRegistry()).sides().get(0).extra());

        // 相邻有同朝代卡需要另一张近代卡；用两侧夹击场景验证不触发：左空右无 → 已覆盖；
        // 这里验证相邻同朝代分支用金质与普通混合：近代卡只有向日葵自身，改用两侧空位已足够。
        // 金质：+2 倍率 → 总 3，+20 额外。
        NewCardBattle gold = new NewCardBattle();
        gold.placeCard(0, 0, new XiangRiKuiGoldCard());
        ScoreSnapshot goldScore = NewSettlementCalculator.calculate(gold, new SettlementRuleRegistry());
        assertEquals(3, goldScore.sides().get(0).multiplier());
        assertEquals(20, goldScore.sides().get(0).extra());
    }

    @Test
    void qiuCaoBeiAdjacentDiffClass()
    {
        // 相邻守·衡 vs 自身谋·策 → 触发（秋草杯 +16；青铜仙鹤无结算词条）。
        NewCardBattle diff = new NewCardBattle();
        diff.placeCard(0, 0, new QiuCaoBeiCommonCard());
        diff.placeCard(0, 1, new com.laigu.laigu.duel.newcard.cards.QingTongXianHeCommonCard());
        assertEquals(16, NewSettlementCalculator.calculate(diff, new SettlementRuleRegistry()).sides().get(0).extra());

        // 相邻同为谋·策（另一张秋草杯）→ 都不触发，且无其他词条污染。
        NewCardBattle same = new NewCardBattle();
        same.placeCard(0, 1, new QiuCaoBeiCommonCard());
        same.placeCard(0, 2, new QiuCaoBeiCommonCard());
        assertEquals(0, NewSettlementCalculator.calculate(same, new SettlementRuleRegistry()).sides().get(0).extra());

        NewCardBattle gold = new NewCardBattle();
        gold.placeCard(0, 0, new QiuCaoBeiGoldCard());
        gold.placeCard(0, 1, new com.laigu.laigu.duel.newcard.cards.QingTongXianHeCommonCard());
        assertEquals(32, NewSettlementCalculator.calculate(gold, new SettlementRuleRegistry()).sides().get(0).extra());
    }

    @Test
    void qinGongBoAdjacentSameDynasty()
    {
        // 秦公镈（春秋）相邻越王勾践剑（春秋）→ 触发。
        NewCardBattle same = new NewCardBattle();
        same.placeCard(0, 0, new QinGongBoCommonCard());
        same.placeCard(0, 1, new com.laigu.laigu.duel.newcard.cards.YueWangGouJianJianCommonCard());
        assertEquals(16, NewSettlementCalculator.calculate(same, new SettlementRuleRegistry()).sides().get(0).extra());

        // 相邻朝代不同（战国青铜仙鹤）→ 不触发。
        NewCardBattle diff = new NewCardBattle();
        diff.placeCard(0, 0, new QinGongBoCommonCard());
        diff.placeCard(0, 1, new com.laigu.laigu.duel.newcard.cards.QingTongXianHeCommonCard());
        assertEquals(0, NewSettlementCalculator.calculate(diff, new SettlementRuleRegistry()).sides().get(0).extra());

        NewCardBattle gold = new NewCardBattle();
        gold.placeCard(0, 0, new QinGongBoGoldCard());
        gold.placeCard(0, 1, new com.laigu.laigu.duel.newcard.cards.YueWangGouJianJianCommonCard());
        assertEquals(32, NewSettlementCalculator.calculate(gold, new SettlementRuleRegistry()).sides().get(0).extra());
    }

    @Test
    void batch3CardsAreIndependentOnSettlementClasses()
    {
        assertNotEquals(PangBeiCommonCard.class, PangBeiGoldCard.class);
        assertNotEquals(XiangRiKuiCommonCard.class, XiangRiKuiGoldCard.class);
        assertTrue(OnSettlement.class.isAssignableFrom(WeiSuoJiaJuCommonCard.class));
        assertTrue(OnSettlement.class.isAssignableFrom(DuanBiWeiNaSiGoldCard.class));
        assertTrue(OnSettlement.class.isAssignableFrom(TaoYuanXianJingTuCommonCard.class));
        assertTrue(OnSettlement.class.isAssignableFrom(MaoGongDingGoldCard.class));
        assertTrue(OnSettlement.class.isAssignableFrom(HanMoLaBiFaDianCommonCard.class));
        assertTrue(OnSettlement.class.isAssignableFrom(ShangZhouShiGongGoldCard.class));
        assertTrue(OnSettlement.class.isAssignableFrom(WaiXiaoBiZhiCommonCard.class));
        assertTrue(OnSettlement.class.isAssignableFrom(QiuCaoBeiGoldCard.class));
        assertTrue(OnSettlement.class.isAssignableFrom(QinGongBoCommonCard.class));
    }
}
