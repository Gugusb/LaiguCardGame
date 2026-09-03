package com.laigu.laigu.duel.newcard;

import com.laigu.laigu.duel.newcard.cards.QingCiLianHuaZunCommonCard;
import com.laigu.laigu.duel.newcard.cards.QingCiLianHuaZunGoldCard;
import com.laigu.laigu.duel.newcard.cards.TianQiuYiCommonCard;
import com.laigu.laigu.duel.newcard.cards.TXingBoHuaCommonCard;
import com.laigu.laigu.duel.newcard.cards.TXingBoHuaGoldCard;
import com.laigu.laigu.duel.newcard.cards.ZengHouYiBianZhongGoldCard;
import com.laigu.laigu.duel.newcard.cards.TianQiuYiGoldCard;
import com.laigu.laigu.duel.newcard.cards.TongHuDiLouCommonCard;
import com.laigu.laigu.duel.newcard.cards.TongHuDiLouGoldCard;
import com.laigu.laigu.duel.newcard.cards.YinXiangNangCommonCard;
import com.laigu.laigu.duel.newcard.cards.YinXiangNangGoldCard;
import com.laigu.laigu.duel.newcard.cards.XingYueYeCommonCard;
import com.laigu.laigu.duel.newcard.cards.XingYueYeGoldCard;
import com.laigu.laigu.duel.newcard.cards.NiaoYinShanShuiZhongCommonCard;
import com.laigu.laigu.duel.newcard.cards.NiaoYinShanShuiZhongGoldCard;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 阶段16：抢骰计划构建器与抓骰副作用的行为断言。 */
class Stage16DraftInfrastructureTest
{

    @Test
    void planModifiersApplyToDraftPlan()
    {
        // 天球仪：双方各少抓 1 次 → 先手 3-1=2 次 → 颗数 [1,1]；后手不变 [2,2]。
        NewCardBattle battle = new NewCardBattle();
        battle.placeCard(0, 0, new TianQiuYiCommonCard());
        battle.state().setFirstPicker(0);
        DraftPlanBuilder.build(battle);
        assertEquals(List.of(1, 1), battle.state().draftFirstSizes());
        assertEquals(List.of(2), battle.state().draftSecondSizes());
        // 铜壶滴漏：本方+1 次 → 先手 4 次 → [1,2,2,1]；后手不变 [2,2]。
        NewCardBattle selfUp = new NewCardBattle();
        selfUp.placeCard(0, 0, new TongHuDiLouCommonCard());
        selfUp.state().setFirstPicker(0);
        DraftPlanBuilder.build(selfUp);
        assertEquals(List.of(1, 2, 2, 1), selfUp.state().draftFirstSizes());
        assertEquals(List.of(2, 2), selfUp.state().draftSecondSizes());
        // 印香囊：对方-1 → 后手 1 次 → [2]；先手不变 [1,2,1]。
        NewCardBattle oppDown = new NewCardBattle();
        oppDown.placeCard(0, 0, new YinXiangNangCommonCard());
        oppDown.state().setFirstPicker(0);
        DraftPlanBuilder.build(oppDown);
        assertEquals(List.of(1, 2, 1), oppDown.state().draftFirstSizes());
        assertEquals(List.of(2), oppDown.state().draftSecondSizes());
        // 星月夜：双方各多抓 1 次 → 先手 4 次 [1,2,2,1]；后手 3 次 [2,2,2]。
        NewCardBattle star = new NewCardBattle();
        star.placeCard(0, 0, new XingYueYeCommonCard());
        star.state().setFirstPicker(0);
        DraftPlanBuilder.build(star);
        assertEquals(List.of(1, 2, 2, 1), star.state().draftFirstSizes());
        assertEquals(List.of(2, 2, 2), star.state().draftSecondSizes());
        // 天球仪双份（普通-1 + 金-2）：双方-3 次 → 先手 0 次 []（下限 0 保护）。
        NewCardBattle two = new NewCardBattle();
        two.placeCard(0, 0, new TianQiuYiCommonCard());
        two.placeCard(0, 1, new TianQiuYiGoldCard());
        two.state().setFirstPicker(0);
        DraftPlanBuilder.build(two);
        assertEquals(List.of(), two.state().draftFirstSizes());
    }

    @Test
    void qingCiScoreOnlyAfterQ1Ruling()
    {
        // 清单口径（取代确认稿 Q1 的 ×5/×10）：青瓷无颗数修正（先手基准 [1,2,1]）；抓 3 点骰 → (6-3)×3=9 额外分。
        NewCardBattle battle = new NewCardBattle();
        battle.placeCard(0, 0, new QingCiLianHuaZunCommonCard());
        battle.state().setFirstPicker(0);
        DraftPlanBuilder.build(battle);
        assertEquals(List.of(1, 2, 1), battle.state().draftFirstSizes());
        assertFalse(CardFactory.create("qing_ci_lian_hua_zun_common") instanceof OnDraftPlan);
        assertFalse(CardFactory.create("qing_ci_lian_hua_zun_gold") instanceof OnDraftPlan);
        int[] fx = battle.onGrabEffects(0, 3);
        assertEquals(9, battle.state().extraScore(0));
        assertEquals(0, fx[0]);
        assertEquals(9, fx[1]);
        // 金：抓 3 点骰 → (6-3)×6=18 额外分；计划同样无颗数修正。
        NewCardBattle gold = new NewCardBattle();
        gold.placeCard(0, 0, new QingCiLianHuaZunGoldCard());
        gold.state().setFirstPicker(0);
        DraftPlanBuilder.build(gold);
        assertEquals(List.of(1, 2, 1), gold.state().draftFirstSizes());
        gold.onGrabEffects(0, 3);
        assertEquals(18, gold.state().extraScore(0));
    }

    @Test
    void goldXingBoHuaAndBianZhongGrantOneShotDraftTurnOnSummon()
    {
        // Q7 拍板：金 T形帛画/编钟入场时本回合抓骰次数 +1（一次性，下回合正常）。
        NewCardBattle battle = new NewCardBattle();
        battle.placeCard(0, 0, new TXingBoHuaGoldCard(), true);
        battle.placeCard(1, 0, new ZengHouYiBianZhongGoldCard(), true);
        battle.state().setFirstPicker(0);
        DraftPlanBuilder.build(battle);
        assertEquals(List.of(1, 2, 2, 1), battle.state().draftFirstSizes());
        assertEquals(List.of(2, 2, 2), battle.state().draftSecondSizes());
        // 一次性：再次构建不再叠加（加成随计划构建消耗）。
        DraftPlanBuilder.build(battle);
        assertEquals(List.of(1, 2, 1), battle.state().draftFirstSizes());
        // 普通版无入场加成。
        NewCardBattle common = new NewCardBattle();
        common.placeCard(0, 0, new TXingBoHuaCommonCard(), true);
        common.state().setFirstPicker(0);
        DraftPlanBuilder.build(common);
        assertEquals(List.of(1, 2, 1), common.state().draftFirstSizes());
    }

    @Test
    void rerollOnDraftAppliesOncePerRound()
    {
        // 鸟音山水钟（普通）：抓 2 点骰 → 重骰共享池 >2 的骰；每轮限 1 次。
        NewCardBattle battle = new NewCardBattle();
        battle.placeCard(0, 0, new NiaoYinShanShuiZhongCommonCard());
        battle.state().setSharedPool(List.of(3, 5, 4));
        battle.onGrabEffects(0, 2);
        assertEquals(1, battle.state().rerollUses());
        assertEquals(3, battle.state().sharedPool().size());
        // 第二次抓骰：普通限次已用完 → 不再重骰（useReroll 计数不变）。
        battle.onGrabEffects(0, 2);
        assertEquals(1, battle.state().rerollUses());
        // 鸟音山水钟（金）：每轮限 2 次。
        NewCardBattle goldBattle = new NewCardBattle();
        goldBattle.placeCard(0, 0, new NiaoYinShanShuiZhongGoldCard());
        goldBattle.state().setSharedPool(List.of(3, 5, 4));
        goldBattle.onGrabEffects(0, 2);
        goldBattle.onGrabEffects(0, 2);
        assertEquals(2, goldBattle.state().rerollUses());
        // 无重骰卡 → 不消耗重骰次数。
        NewCardBattle plain = new NewCardBattle();
        plain.state().setSharedPool(List.of(3, 5, 4));
        plain.onGrabEffects(0, 2);
        assertEquals(0, plain.state().rerollUses());
    }

    @Test
    void goldStarNightRefillsSharedPoolOnAnyGrab()
    {
        // 星月夜（金）：任一方抓骰后共享池 +1（旧引擎硬编码的忠实迁移，确认稿 Q3）。
        NewCardBattle battle = new NewCardBattle();
        battle.placeCard(0, 0, new XingYueYeCommonCard());
        battle.placeCard(1, 0, new XingYueYeGoldCard());
        battle.state().setSharedPool(List.of(3));
        int[] fx = battle.onGrabEffects(0, 4);
        assertEquals(2, battle.state().sharedPool().size());
        assertEquals(1, fx[0]);
        assertEquals(0, fx[1]);
    }

    @Test
    void draftCardsImplementPlanOrEventInterfaces()
    {
        // 12 类均注册且有行为路径：计划修正（OnDraftPlan）或抓骰响应（onEvent DRAFT）。
        for (String id : List.of(
                "xing_yue_ye_common", "xing_yue_ye_gold",
                "tian_qiu_yi_common", "tian_qiu_yi_gold",
                "tong_hu_di_lou_common", "tong_hu_di_lou_gold",
                "yin_xiang_nang_common", "yin_xiang_nang_gold",
                "qing_ci_lian_hua_zun_common", "qing_ci_lian_hua_zun_gold",
                "niao_yin_shan_shui_zhong_common", "niao_yin_shan_shui_zhong_gold"))
        {
            DuelCard card = CardFactory.create(id);
            assertTrue(card instanceof OnDraftPlan || hasDraftEventOverride(card), id);
        }
        assertTrue(CardRegistryValidator.unmappedLegacyEffects().isEmpty());
        assertTrue(CardRegistryValidator.pendingDraftInfrastructureIds().isEmpty());
    }

    private static boolean hasDraftEventOverride(DuelCard card)
    {
        for (java.lang.reflect.Method m : card.getClass().getDeclaredMethods())
            if ("onEvent".equals(m.getName()) && m.getParameterCount() == 2) return true;
        return false;
    }
}
