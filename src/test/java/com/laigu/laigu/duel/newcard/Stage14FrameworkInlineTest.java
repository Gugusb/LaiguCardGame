package com.laigu.laigu.duel.newcard;

import com.laigu.laigu.duel.newcard.cards.DunHuangFeiTianCommonCard;
import com.laigu.laigu.duel.newcard.cards.DunHuangFeiTianGoldCard;
import com.laigu.laigu.duel.newcard.cards.HaiCuoTuCommonCard;
import com.laigu.laigu.duel.newcard.cards.HaiCuoTuGoldCard;
import com.laigu.laigu.duel.newcard.cards.QingTongXianHeCommonCard;
import com.laigu.laigu.duel.newcard.cards.QingTongXianHeGoldCard;
import com.laigu.laigu.duel.newcard.cards.XiShanXingLvTuCommonCard;
import com.laigu.laigu.duel.newcard.cards.XiShanXingLvTuGoldCard;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 阶段14：框架5规则内联——注册表清空、8 个原空壳类行为内联且可执行。 */
class Stage14FrameworkInlineTest
{
    private static boolean hasOnEventOverride(DuelCard card)
    {
        for (java.lang.reflect.Method m : card.getClass().getDeclaredMethods())
            if ("onEvent".equals(m.getName()) && m.getParameterCount() == 2) return true;
        // 清单对齐后部分卡改走触发接口承载行为（结算/入场/伏击/激活）。
        return card instanceof com.laigu.laigu.duel.newcard.OnSettlement
                || card instanceof com.laigu.laigu.duel.newcard.OnSummon
                || card instanceof com.laigu.laigu.duel.newcard.OnAmbushSuccess
                || card instanceof com.laigu.laigu.duel.newcard.OnAmbushFail
                || card instanceof com.laigu.laigu.duel.newcard.OnActivation;
    }

    @Test
    void frameworkRegistriesAreEmpty()
    {
        assertEquals(0, FrameworkFiveRules.defaultRegistry().snapshot().size(),
                "结算注册表必须为空（江山/描金壶重复注册已移除）");
        assertEquals(0, FrameworkFiveEventRules.defaultRegistry().snapshot().size(),
                "事件注册表必须为空（行为已内联）");
    }

    @Test
    void inlinedCardsCarryTheirOwnBehavior()
    {
        for (DuelCard card : List.of(new DunHuangFeiTianCommonCard(), new DunHuangFeiTianGoldCard(),
                new HaiCuoTuCommonCard(), new HaiCuoTuGoldCard(),
                new QingTongXianHeCommonCard(), new QingTongXianHeGoldCard(),
                new XiShanXingLvTuCommonCard(), new XiShanXingLvTuGoldCard()))
        {
            assertTrue(hasOnEventOverride(card), card.id() + " 必须自带行为（onEvent 或触发接口）");
        }
    }

    @Test
    void feiTianFailsAmbushForExtraScore()
    {
        DunHuangFeiTianCommonCard card = new DunHuangFeiTianCommonCard();
        CardTestContext ctx = new CardTestContext(card);
        card.onEvent(new BattleEvent(BattleEvent.Type.AMBUSH_FAIL, 0, 0), ctx);
        assertEquals(30, ctx.extra);
        assertEquals(1, ctx.animations.size());
    }

    @Test
    void xianHeGoldLeavesWithDrawAndMultiplier()
    {
        QingTongXianHeGoldCard card = new QingTongXianHeGoldCard();
        CardTestContext ctx = new CardTestContext(card);
        card.onEvent(new BattleEvent(BattleEvent.Type.LEAVE, 0, 0), ctx);
        assertEquals(2, ctx.draws);
        assertEquals(4, ctx.multiplier);
    }

    @Test
    void haiCuoTuActivatesAllOwnCardsOnlyWhenCharged()
    {
        // 清单：充能2——结算时至少2颗骰 → 激活我方所有卡1次（金2次）。
        NewCardBattle full = new NewCardBattle();
        full.placeCard(0, 0, new com.laigu.laigu.duel.newcard.cards.ZengHouYiBianZhongCommonCard());
        full.placeCard(0, 2, new HaiCuoTuCommonCard());
        full.state().cardStateAt(0, 2).setDice(java.util.List.of(1, 2));
        NewSettlementCalculator.calculate(full, new SettlementRuleRegistry());
        assertEquals(1, full.state().cardStateAt(0, 0).activation());

        NewCardBattle empty = new NewCardBattle();
        empty.placeCard(0, 0, new com.laigu.laigu.duel.newcard.cards.ZengHouYiBianZhongCommonCard());
        empty.placeCard(0, 2, new HaiCuoTuCommonCard());
        empty.state().cardStateAt(0, 2).setDice(java.util.List.of(1));
        NewSettlementCalculator.calculate(empty, new SettlementRuleRegistry());
        assertEquals(0, empty.state().cardStateAt(0, 0).activation());

        NewCardBattle gold = new NewCardBattle();
        gold.placeCard(0, 0, new com.laigu.laigu.duel.newcard.cards.ZengHouYiBianZhongCommonCard());
        gold.placeCard(0, 2, new HaiCuoTuGoldCard());
        gold.state().cardStateAt(0, 2).setDice(java.util.List.of(1, 2));
        NewSettlementCalculator.calculate(gold, new SettlementRuleRegistry());
        assertEquals(2, gold.state().cardStateAt(0, 0).activation());
        // 焕章：不可激活卡（海错金自身）× 2 次 → +20。
        assertEquals(20, gold.state().extraScore(0));
    }

    @Test
    void xiShanActivatesLeftCardPerDieAtSettlement()
    {
        // 清单：充能x——结算时每颗骰激活左侧1次；金焕章=入场激活己方所有卡（+5词条移至千里江山金）。
        NewCardBattle battle = new NewCardBattle();
        battle.placeCard(0, 0, new com.laigu.laigu.duel.newcard.cards.ZengHouYiBianZhongCommonCard());
        battle.placeCard(0, 1, new XiShanXingLvTuCommonCard());
        battle.state().cardStateAt(0, 1).setDice(java.util.List.of(1, 2));
        NewSettlementCalculator.calculate(battle, new SettlementRuleRegistry());
        assertEquals(2, battle.state().cardStateAt(0, 0).activation());

        NewCardBattle gold = new NewCardBattle();
        gold.placeCard(0, 0, new com.laigu.laigu.duel.newcard.cards.ZengHouYiBianZhongCommonCard());
        gold.placeCard(0, 1, new XiShanXingLvTuGoldCard());
        gold.state().cardStateAt(0, 1).setDice(java.util.List.of(1, 2));
        NewSettlementCalculator.calculate(gold, new SettlementRuleRegistry());
        // 4 次激活：第 3 次达编钟阈值触发并清零，第 4 次回到进度 1；编钟本卡无骰 → 倍率不变。
        assertEquals(1, gold.state().cardStateAt(0, 0).activation());
        assertEquals(1, gold.state().multiplier(0));
        assertEquals(0, gold.state().extraScore(0));
    }
}
