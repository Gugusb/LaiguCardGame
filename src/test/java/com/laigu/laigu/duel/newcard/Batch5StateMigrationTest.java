package com.laigu.laigu.duel.newcard;

import com.laigu.laigu.duel.newcard.cards.BaiHuaTuJuanCommonCard;
import com.laigu.laigu.duel.newcard.cards.BaiHuaTuJuanGoldCard;
import com.laigu.laigu.duel.newcard.cards.HunTianYiCommonCard;
import com.laigu.laigu.duel.newcard.cards.JinOuYongGuBeiGoldCard;
import com.laigu.laigu.duel.newcard.cards.LuWangBenShengTuCommonCard;
import com.laigu.laigu.duel.newcard.cards.QianLiJiangShanCommonCard;
import com.laigu.laigu.duel.newcard.cards.ShuiLianCommonCard;
import com.laigu.laigu.duel.newcard.cards.ShuiLianGoldCard;
import com.laigu.laigu.duel.newcard.cards.YuanWangBeiCommonCard;
import com.laigu.laigu.duel.newcard.cards.YuanWangBeiGoldCard;
import com.laigu.laigu.duel.newcard.cards.YueWangGouJianJianCommonCard;
import com.laigu.laigu.duel.newcard.cards.YueWangGouJianJianGoldCard;
import com.laigu.laigu.duel.newcard.cards.ZengHouYiBianZhongCommonCard;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 批次五迁移测试：破阵（削弱比例/恒定成功/邻槽削弱）、伏击（无效化/镜像）、
 * 激活链（逐次激活/达成奖励清零/永固杯链式激活/朝代联动）。
 */
class Batch5StateMigrationTest
{
    @BeforeAll
    static void registerCards()
    {
        CardRegistry.initialize();
    }

    /** 固定贡献测试卡：结算时无条件 +8 基础分（走缩放加成，供破阵削弱验证）。 */
    private static final class FixedBaseCard implements DuelCard, OnSettlement
    {
        @Override public String id() { return "fixed_base_common"; }
        @Override public String displayName() { return "测试固定基础分"; }
        @Override public com.laigu.laigu.duel.CardClass cardClass() { return com.laigu.laigu.duel.CardClass.DING; }
        @Override public void onSettlement(SettlementContext context) { context.addBaseScaled(8); }
    }

    @Test
    void poZhenCommonHalvesOpponentContribution()
    {
        NewCardBattle battle = new NewCardBattle();
        battle.placeCard(0, 0, new YueWangGouJianJianCommonCard());
        battle.placeCard(1, 0, new FixedBaseCard());
        battle.state().cardStateAt(0, 0).setDice(List.of(5, 5));
        battle.state().cardStateAt(1, 0).setDice(List.of(2));

        NewSettlementCalculator.calculate(battle);

        // 对手贡献按 keep=0.5 缩放：基础分 = 2（骰）+ floor(8*0.5) = 6。
        assertEquals(10, battle.state().baseScore(0));
        assertEquals(6, battle.state().baseScore(1));
    }

    @Test
    void poZhenGoldFullHalveAndPersistentSuccess()
    {
        NewCardBattle battle = new NewCardBattle();
        battle.placeCard(0, 0, new YueWangGouJianJianGoldCard());
        battle.placeCard(1, 0, new FixedBaseCard());
        battle.state().cardStateAt(0, 0).setDice(List.of(1));
        battle.state().cardStateAt(1, 0).setDice(List.of(5, 5));

        NewSettlementCalculator.calculate(battle);
        assertEquals(1, battle.state().baseScore(0));
        assertEquals(18, battle.state().baseScore(1));
        assertEquals(0, battle.state().extraScore(0));

        battle.dispatch(new BattleEvent(BattleEvent.Type.PO_ZHEN, 0, 0));

        NewSettlementCalculator.calculate(battle);
        assertEquals(1, battle.state().baseScore(0));
        // 清单：越剑金无破阵额外分词条（恒定成功即全部）。
        assertEquals(0, battle.state().extraScore(0));
        assertEquals(10, battle.state().baseScore(1));
    }

    @Test
    void poZhenGoldCupHalvesNeighborSlots()
    {
        NewCardBattle battle = new NewCardBattle();
        battle.placeCard(0, 1, new YuanWangBeiGoldCard());
        battle.placeCard(1, 0, new FixedBaseCard());
        battle.placeCard(1, 1, new FixedBaseCard());
        battle.state().cardStateAt(0, 1).setDice(List.of(5));
        battle.state().cardStateAt(1, 0).setDice(List.of(2));
        battle.state().cardStateAt(1, 1).setDice(List.of(1));

        NewSettlementCalculator.calculate(battle);

        // 对位槽 1 全削（金），邻槽 0 削半；清单：越杯金无破阵额外分词条。
        assertEquals(5, battle.state().baseScore(0));
        assertEquals(0, battle.state().extraScore(0));
        assertEquals(7, battle.state().baseScore(1));
    }
    @Test
    void shuiLianCommonInvalidatesLeadingDiceOfOpponent()
    {
        ShuiLianCommonCard card = new ShuiLianCommonCard();
        CardTestContext context = new CardTestContext(card);
        context.oppositeRuntimeState = new CardRuntimeState();
        context.oppositeRuntimeState.setDice(List.of(4, 3, 2));

        card.onAmbushSuccess(context);
        assertEquals(List.of(2), context.oppositeRuntimeState.activeDice());

        // 清单口径（推翻批次五裁定）：伏击失败无收益，睡莲不再实现 OnAmbushFail。
        assertEquals(0, context.extra);
    }

    @Test
    void shuiLianGoldInvalidatesFourDiceAndReclaimsBase()
    {
        ShuiLianGoldCard card = new ShuiLianGoldCard();
        CardTestContext context = new CardTestContext(card);
        context.oppositeRuntimeState = new CardRuntimeState();
        context.oppositeRuntimeState.setDice(List.of(4, 3, 2, 2, 1));

        card.onAmbushSuccess(context);
        assertEquals(4, context.oppositeRuntimeState.invalidatedDice());
        assertEquals(11, context.base);
    }

    @Test
    void baiHuaCommonMirrorsHalfOpponentBase()
    {
        BaiHuaTuJuanCommonCard card = new BaiHuaTuJuanCommonCard();
        CardTestContext context = new CardTestContext(card);
        context.oppositeRuntimeState = new CardRuntimeState();
        context.oppositeRuntimeState.setDice(List.of(6, 4));

        card.onAmbushSuccess(context);
        assertEquals(5, context.base);

        // 清单口径：伏击失败无收益，百花不再实现 OnAmbushFail。
        assertEquals(0, context.extra);
    }

    @Test
    void baiHuaGoldCopiesOpponentDiceAndBonusesOverFour()
    {
        BaiHuaTuJuanGoldCard card = new BaiHuaTuJuanGoldCard();

        // 清单：伏击成功 → 复制对位骰子给自己；复制后不超过 4 颗则无焕章奖励。
        CardTestContext few = new CardTestContext(card);
        few.oppositeRuntimeState = new CardRuntimeState();
        few.oppositeRuntimeState.setDice(List.of(6, 4));
        card.onAmbushSuccess(few);
        assertEquals(List.of(6, 4), few.selfDice());
        assertEquals(0, few.base);
        assertEquals(0, few.extra);

        // 焕章：复制后骰子数 > 4 → +5 基础分、+5 倍率、+5 额外分。
        CardTestContext rich = new CardTestContext(card);
        rich.oppositeRuntimeState = new CardRuntimeState();
        rich.oppositeRuntimeState.setDice(List.of(6, 4, 2));
        rich.selfState().setDice(List.of(3, 2));
        card.onAmbushSuccess(rich);
        assertEquals(List.of(3, 2, 6, 4, 2), rich.selfDice());
        assertEquals(5, rich.base);
        assertEquals(5, rich.multiplier);
        assertEquals(5, rich.extra);
    }

    @Test
    void activationChainRewardsAtThresholdAndResets()
    {
        NewCardBattle battle = new NewCardBattle();
        battle.placeCard(0, 0, new ZengHouYiBianZhongCommonCard());
        battle.state().cardStateAt(0, 0).setActivation(3);
        battle.state().cardStateAt(0, 0).setDice(List.of(5, 6));

        battle.dispatch(new BattleEvent(BattleEvent.Type.ACTIVATION, 0, 0));

        // 直接派发路径无结算器基线 1：每骰 +1 × 2 骰 = 2。
        assertEquals(2, battle.state().timingMult(0));
        assertEquals(0, battle.state().cardStateAt(0, 0).activation());
    }

    @Test
    void hunTianYiStraightActivatesRightCardThreeTimes()
    {
        NewCardBattle battle = new NewCardBattle();
        battle.placeCard(0, 0, new HunTianYiCommonCard());
        battle.placeCard(0, 1, new ZengHouYiBianZhongCommonCard());
        battle.state().cardStateAt(0, 0).setDice(List.of(1, 2));
        battle.state().cardStateAt(0, 1).setDice(List.of(3, 4));

        NewSettlementCalculator.calculate(battle);

        assertEquals(3, battle.state().multiplier(0));
        assertEquals(0, battle.state().cardStateAt(0, 1).activation());
        assertEquals(10, battle.state().baseScore(0));
    }

    @Test
    void yongGuBeiGoldRewardsTwiceOpponentEmptySlots()
    {
        // 清单：金=激活2 → 2x 倍率（x=对方无骰卡牌数）；激活左侧链已删除。
        NewCardBattle battle = new NewCardBattle();
        battle.placeCard(0, 1, new JinOuYongGuBeiGoldCard());
        battle.placeCard(1, 0, new com.laigu.laigu.duel.newcard.cards.QingTongXianHeCommonCard());
        battle.state().cardStateAt(0, 1).setActivation(2);
        battle.dispatch(new BattleEvent(BattleEvent.Type.ACTIVATION, 0, 1));

        assertEquals(2, battle.state().timingMult(0));
        assertEquals(0, battle.state().cardStateAt(0, 1).activation());
    }

    @Test
    void luWangActivatesRightByDynastyMax()
    {
        NewCardBattle battle = new NewCardBattle();
        // 鲁王为「北魏」，编钟为「战国」→ 朝代最大数 x=1（当前目录中每朝代仅一张相关卡）。
        battle.placeCard(0, 0, new LuWangBenShengTuCommonCard());
        battle.placeCard(0, 1, new ZengHouYiBianZhongCommonCard());
        battle.state().cardStateAt(0, 0).setDice(List.of(1));
        battle.state().cardStateAt(0, 1).setDice(List.of(5, 6));

        NewSettlementCalculator.calculate(battle);

        // 激活右侧 1 次 → 编钟进度 +1（未达 3，不触发）。
        assertEquals(1, battle.state().multiplier(0));
        assertEquals(1, battle.state().cardStateAt(0, 1).activation());
        assertEquals(12, battle.state().baseScore(0));   // 1 + 5 + 6
    }
}
