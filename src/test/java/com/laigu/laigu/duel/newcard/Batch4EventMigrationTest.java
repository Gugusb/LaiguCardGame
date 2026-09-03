package com.laigu.laigu.duel.newcard;

import com.laigu.laigu.duel.newcard.cards.LianTangRuYaTuCommonCard;
import com.laigu.laigu.duel.newcard.cards.LianTangRuYaTuGoldCard;
import com.laigu.laigu.duel.newcard.cards.MoGaoKu220CommonCard;
import com.laigu.laigu.duel.newcard.cards.MoGaoKu220GoldCard;
import com.laigu.laigu.duel.newcard.cards.ShuiJingBeiCommonCard;
import com.laigu.laigu.duel.newcard.cards.ShuiJingBeiGoldCard;
import com.laigu.laigu.duel.newcard.cards.SiLongSiFengZuoCommonCard;
import com.laigu.laigu.duel.newcard.cards.SiLongSiFengZuoGoldCard;
import com.laigu.laigu.duel.newcard.cards.XiaoSongXiangLuCommonCard;
import com.laigu.laigu.duel.newcard.cards.XiaoSongXiangLuGoldCard;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 批次四迁移测试：入场/轮开始/轮结束事件词条的接口式实现与新旧结果对照。 */
class Batch4EventMigrationTest
{
    @Test
    void shuiJingBeiRoundStartDrawMatchesLegacy()
    {
        NewCardBattle battle = new NewCardBattle();
        battle.placeCard(0, 0, new ShuiJingBeiCommonCard());
        battle.state().setRound(2);
        battle.startRound();
        assertEquals(1, battle.state().handSize(0));

        // 两张静水各触发一次（与旧引擎 triggerRoundStart 逐槽循环一致）。
        NewCardBattle two = new NewCardBattle();
        two.placeCard(0, 0, new ShuiJingBeiCommonCard());
        two.placeCard(0, 1, new ShuiJingBeiCommonCard());
        two.startRound();
        assertEquals(2, two.state().handSize(0));

        // 金卡：入场抽 2（旧引擎 triggerSummon 硬编码）+ 每轮开始抽 2（清单口径）= 4。
        NewCardBattle gold = new NewCardBattle();
        gold.placeCard(0, 0, new ShuiJingBeiGoldCard(), true);
        gold.startRound();
        assertEquals(4, gold.state().handSize(0));
    }

    @Test
    void moGaoKu220RoundStartDrawMatchesLegacy()
    {
        NewCardBattle battle = new NewCardBattle();
        battle.placeCard(0, 0, new MoGaoKu220CommonCard());
        battle.startRound();
        assertEquals(1, battle.state().handSize(0));

        // 抽卡类词条金卡不翻倍（旧目录 rawBoth 规则）。
        NewCardBattle gold = new NewCardBattle();
        gold.placeCard(0, 0, new MoGaoKu220GoldCard());
        gold.startRound();
        assertEquals(1, gold.state().handSize(0));
    }

    @Test
    void siLongSiFengZuoSummonRestoreApAndFlatExtra()
    {
        // 入场回复 1 行动力。
        NewCardBattle battle = new NewCardBattle();
        battle.state().setActionPoints(0, 1);
        battle.placeCard(0, 0, new SiLongSiFengZuoCommonCard(), true);
        assertEquals(2, battle.state().actionPoints(0));

        // 回复不超过每轮上限 3。
        NewCardBattle capped = new NewCardBattle();
        capped.state().setActionPoints(0, 3);
        capped.placeCard(0, 0, new SiLongSiFengZuoCommonCard(), true);
        assertEquals(3, capped.state().actionPoints(0));

        // 金卡回复 2 点行动力（清单口径）。
        NewCardBattle goldAp = new NewCardBattle();
        goldAp.state().setActionPoints(0, 0);
        goldAp.placeCard(0, 0, new SiLongSiFengZuoGoldCard(), true);
        assertEquals(2, goldAp.state().actionPoints(0));

        // 清单：普通无额外分词条；金焕章=每消耗1行动力+15（本回合未消耗 → 0）。
        assertEquals(0, NewSettlementCalculator.calculate(battle, new SettlementRuleRegistry()).sides().get(0).extra());
        assertEquals(0, NewSettlementCalculator.calculate(goldAp, new SettlementRuleRegistry()).sides().get(0).extra());
    }

    @Test
    void xiaoSongXiangLuWinDrawAndWinLastMult()
    {
        NewCardBattle win = new NewCardBattle();
        win.placeCard(0, 0, new XiaoSongXiangLuCommonCard());
        win.state().setWinnerLast(0);
        win.endRound();
        assertEquals(2, win.state().handSize(0));
        // 清单：侧击无倍率词条 → 基线 1。
        assertEquals(1, NewSettlementCalculator.calculate(win, new SettlementRuleRegistry()).sides().get(0).multiplier());

        NewCardBattle lost = new NewCardBattle();
        lost.placeCard(0, 0, new XiaoSongXiangLuCommonCard());
        lost.state().setWinnerLast(1);
        lost.endRound();
        assertEquals(0, lost.state().handSize(0));
        assertEquals(1, NewSettlementCalculator.calculate(lost, new SettlementRuleRegistry()).sides().get(0).multiplier());

        NewCardBattle gold = new NewCardBattle();
        gold.placeCard(0, 0, new XiaoSongXiangLuGoldCard());
        gold.state().setWinnerLast(0);
        gold.endRound();
        assertEquals(3, gold.state().handSize(0));
        // 清单：金=获胜后抽3、无倍率词条 → 基线 1。
        assertEquals(1, NewSettlementCalculator.calculate(gold, new SettlementRuleRegistry()).sides().get(0).multiplier());
    }

    @Test
    void lianTangRuYaTuLoseDrawMatchesLegacy()
    {
        NewCardBattle lost = new NewCardBattle();
        lost.placeCard(0, 0, new LianTangRuYaTuCommonCard());
        lost.state().setWinnerLast(1);
        lost.endRound();
        assertEquals(2, lost.state().handSize(0));

        NewCardBattle win = new NewCardBattle();
        win.placeCard(0, 0, new LianTangRuYaTuCommonCard());
        win.state().setWinnerLast(0);
        win.endRound();
        assertEquals(0, win.state().handSize(0));

        NewCardBattle gold = new NewCardBattle();
        gold.placeCard(0, 0, new LianTangRuYaTuGoldCard());
        gold.state().setWinnerLast(1);
        gold.endRound();
        assertEquals(3, gold.state().handSize(0));
    }

    @Test
    void roundEndDrawRespectsSideIsolation()
    {
        // 0 侧侧击（你赢抽2）、1 侧连环（你输抽2）：0 侧赢 → 双方各按自己的条件抽 2。
        NewCardBattle battle = new NewCardBattle();
        battle.placeCard(0, 0, new XiaoSongXiangLuCommonCard());
        battle.placeCard(1, 0, new LianTangRuYaTuCommonCard());
        battle.state().setWinnerLast(0);
        battle.endRound();
        assertEquals(2, battle.state().handSize(0));
        assertEquals(2, battle.state().handSize(1));
    }

    @Test
    void batch4CardsUseEventInterfacesNotOnSettlementOnly()
    {
        assertTrue(OnRoundStart.class.isAssignableFrom(ShuiJingBeiCommonCard.class));
        assertTrue(OnSummon.class.isAssignableFrom(ShuiJingBeiGoldCard.class));
        assertTrue(OnRoundStart.class.isAssignableFrom(MoGaoKu220GoldCard.class));
        assertTrue(OnSummon.class.isAssignableFrom(SiLongSiFengZuoCommonCard.class));
        assertTrue(OnSettlement.class.isAssignableFrom(SiLongSiFengZuoGoldCard.class));
        assertTrue(OnRoundEnd.class.isAssignableFrom(XiaoSongXiangLuCommonCard.class));
        assertTrue(OnRoundEnd.class.isAssignableFrom(LianTangRuYaTuGoldCard.class));
        // 独立类：普通与金质互不相同。
        assertNotEquals(ShuiJingBeiCommonCard.class, ShuiJingBeiGoldCard.class);
        assertNotEquals(XiaoSongXiangLuCommonCard.class, XiaoSongXiangLuGoldCard.class);
        assertNotEquals(LianTangRuYaTuCommonCard.class, LianTangRuYaTuGoldCard.class);
    }

    @Test
    void dispatchDoesNotFireRoundStartWithoutCards()
    {
        NewCardBattle battle = new NewCardBattle();
        battle.startRound();
        assertEquals(0, battle.state().handSize(0));
        assertEquals(0, battle.state().handSize(1));
    }
}
