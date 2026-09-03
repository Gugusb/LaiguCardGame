package com.laigu.laigu.duel.newcard;

import com.laigu.laigu.duel.newcard.cards.QingTongXianHeCommonCard;
import com.laigu.laigu.duel.newcard.cards.ZengHouYiBianZhongCommonCard;
import com.laigu.laigu.duel.newcard.cards.QingTongXianHeGoldCard;
import com.laigu.laigu.duel.newcard.cards.DunHuangFeiTianCommonCard;
import com.laigu.laigu.duel.newcard.cards.DunHuangFeiTianGoldCard;
import com.laigu.laigu.duel.newcard.cards.QianLiJiangShanCommonCard;
import com.laigu.laigu.duel.newcard.cards.QianLiJiangShanGoldCard;
import com.laigu.laigu.duel.newcard.cards.GuangCaiMiaoJinHuCommonCard;
import com.laigu.laigu.duel.newcard.cards.GuangCaiMiaoJinHuGoldCard;
import com.laigu.laigu.duel.newcard.cards.XiShanXingLvTuCommonCard;
import com.laigu.laigu.duel.newcard.cards.XiShanXingLvTuGoldCard;
import com.laigu.laigu.duel.newcard.cards.HaiCuoTuCommonCard;
import com.laigu.laigu.duel.newcard.cards.HaiCuoTuGoldCard;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NewCardArchitectureTest
{
    @BeforeAll
    static void registerCards()
    {
        CardRegistry.initialize();
    }

    @Test
    void oneIndependentCardCanDeclareMultipleIndependentEffects()
    {
        // 阶段15：结算词条不再用 CardEffects.settlement，改为类内显式 OnSettlement。
        DuelCard card = new MultiEffectTestCard();
        NewCardBattle battle = new NewCardBattle();
        battle.placeCard(0, 0, card);
        battle.state().cardStateAt(0, 0).setDice(List.of(2));
        assertEquals(9, NewSettlementCalculator.calculate(battle, new SettlementRuleRegistry()).sides().get(0).total());
        battle.dispatch(new BattleEvent(BattleEvent.Type.LEAVE, 0, 0));
        assertEquals(2, battle.state().handSize(0));
    }

    @Test
    void m4AndM5RulesUseIndependentArtifactClasses()
    {
        NewCardBattle pattern = new NewCardBattle();
        pattern.placeCard(0, 0, CardFactory.create("yin_que_shan_han_jian_1_common"));
        pattern.state().cardStateAt(0, 0).setDice(List.of(1, 2, 3));
        ScoreSnapshot patternScore = NewSettlementCalculator.calculate(pattern, new SettlementRuleRegistry());
        assertEquals(5, patternScore.sides().get(0).multiplier());

        NewCardBattle comeback = new NewCardBattle();
        comeback.placeCard(0, 0, CardFactory.create("duan_bi_wei_na_si_common"));
        comeback.state().setWins(0, 0);
        comeback.state().setWins(1, 1);
        ScoreSnapshot comebackScore = NewSettlementCalculator.calculate(comeback, new SettlementRuleRegistry());
        assertEquals(20, comebackScore.sides().get(0).extra());
    }

    @Test
    void m3PerDieExtraUsesIndependentArtifactClasses()
    {
        NewCardBattle commonBattle = new NewCardBattle();
        commonBattle.placeCard(0, 0, CardFactory.create("tong_che_ma_common"));
        commonBattle.state().cardStateAt(0, 0).setDice(List.of(2, 5));
        ScoreSnapshot common = NewSettlementCalculator.calculate(commonBattle, new SettlementRuleRegistry());
        assertEquals(7, common.sides().get(0).base());
        assertEquals(12, common.sides().get(0).extra());

        NewCardBattle goldBattle = new NewCardBattle();
        goldBattle.placeCard(0, 0, CardFactory.create("tong_che_ma_gold"));
        goldBattle.state().cardStateAt(0, 0).setDice(List.of(2, 5));
        ScoreSnapshot gold = NewSettlementCalculator.calculate(goldBattle, new SettlementRuleRegistry());
        assertEquals(24, gold.sides().get(0).extra());
        assertNotEquals(commonBattle.placements().get(0).card().getClass(), goldBattle.placements().get(0).card().getClass());
    }

    @Test
    void migrationBaselineRequiresIndependentCommonAndGoldClassesForAllArtifacts()
    {
        assertEquals(79, CardMigrationBaseline.artifactCount());
        assertEquals(158, CardMigrationBaseline.variantCount());
        assertEquals(79, ArtifactCardNames.all().size());
        assertEquals("千里江山图", ArtifactCardNames.variantName("qian_li_jiang_shan", CardRarity.COMMON));
        assertEquals("千里江山图·金质", ArtifactCardNames.variantName("qian_li_jiang_shan", CardRarity.GOLD));
    }

    @Test
    void registeredCardsUseArtifactNamesAndIndependentVariantClasses()
    {
        assertTrue(CardRegistryValidator.validateRegisteredCards().isEmpty(),
                () -> String.join("\n", CardRegistryValidator.validateRegisteredCards()));
        CardFactory.validateIndependentVariants("qing_tong_xian_he");
        assertEquals(0, CardRegistryValidator.missingVariantIds().size());
        assertEquals(158, CardFactory.registeredIds().size());
        for (String artifactId : com.laigu.laigu.card.CardCatalog.CARD_IDS)
            CardFactory.validateIndependentVariants(artifactId);
    }

    @Test
    void registryInitializationIsIdempotentAndFactoryCreatesIndependentInstances()
    {
        CardRegistry.initialize();
        DuelCard first = CardFactory.create("hai_cuo_tu_common");
        DuelCard second = CardFactory.create("hai_cuo_tu_common");
        assertEquals(first.getClass(), second.getClass());
        assertNotEquals(first, second);
    }

    @Test
    void haiCuoCommonAndGoldUseChargeForLeftAndRightActivation()
    {
        // 清单：充能2——至少2颗骰 → 结算时激活我方所有卡1次（金2次）；金焕章=不可激活卡尝试+10。
        NewCardBattle commonBattle = new NewCardBattle();
        commonBattle.placeCard(0, 0, new ZengHouYiBianZhongCommonCard());
        commonBattle.placeCard(0, 2, new HaiCuoTuCommonCard());
        commonBattle.state().cardStateAt(0, 2).setDice(List.of(1, 2));
        NewSettlementCalculator.calculate(commonBattle, new SettlementRuleRegistry());
        assertEquals(1, commonBattle.state().cardStateAt(0, 0).activation());

        NewCardBattle shortDice = new NewCardBattle();
        shortDice.placeCard(0, 0, new ZengHouYiBianZhongCommonCard());
        shortDice.placeCard(0, 2, new HaiCuoTuCommonCard());
        shortDice.state().cardStateAt(0, 2).setDice(List.of(1));
        NewSettlementCalculator.calculate(shortDice, new SettlementRuleRegistry());
        assertEquals(0, shortDice.state().cardStateAt(0, 0).activation());

        NewCardBattle goldBattle = new NewCardBattle();
        goldBattle.placeCard(0, 0, new ZengHouYiBianZhongCommonCard());
        goldBattle.placeCard(0, 2, new HaiCuoTuGoldCard());
        goldBattle.state().cardStateAt(0, 2).setDice(List.of(1, 2));
        NewSettlementCalculator.calculate(goldBattle, new SettlementRuleRegistry());
        assertEquals(2, goldBattle.state().cardStateAt(0, 0).activation());
        // 焕章：不可激活卡（海错金自身）× 2 次 → +20。
        assertEquals(20, goldBattle.state().extraScore(0));
    }

    @Test
    void xiShanCommonAndGoldActivateLeftCardIndependently()
    {
        // 清单：充能x——结算时每颗骰激活左侧1次（金=每骰2次）；+5词条已移至千里江山金焕章。
        NewCardBattle commonBattle = new NewCardBattle();
        commonBattle.placeCard(0, 0, new ZengHouYiBianZhongCommonCard());
        commonBattle.placeCard(0, 1, new XiShanXingLvTuCommonCard());
        commonBattle.state().cardStateAt(0, 1).setDice(List.of(1, 2));
        NewSettlementCalculator.calculate(commonBattle, new SettlementRuleRegistry());
        assertEquals(2, commonBattle.state().cardStateAt(0, 0).activation());
        assertEquals(1, commonBattle.state().multiplier(0));

        NewCardBattle goldBattle = new NewCardBattle();
        goldBattle.placeCard(0, 0, new ZengHouYiBianZhongCommonCard());
        goldBattle.placeCard(0, 1, new XiShanXingLvTuGoldCard());
        goldBattle.state().cardStateAt(0, 1).setDice(List.of(1, 2));
        NewSettlementCalculator.calculate(goldBattle, new SettlementRuleRegistry());
        // 4 次激活：第 3 次达编钟阈值触发并清零，第 4 次回到 1；编钟本卡无骰 → 倍率不变。
        assertEquals(1, goldBattle.state().cardStateAt(0, 0).activation());
        assertEquals(1, goldBattle.state().multiplier(0));
    }

    @Test
    void miaoJinHuCommonAndGoldActivateIndependently()
    {
        NewCardBattle commonBattle = new NewCardBattle();
        commonBattle.placeCard(0, 0, new GuangCaiMiaoJinHuCommonCard());
        commonBattle.state().cardStateAt(0, 0).setActivation(1);
        commonBattle.dispatch(new BattleEvent(BattleEvent.Type.ACTIVATION, 0, 0));

        NewCardBattle goldBattle = new NewCardBattle();
        goldBattle.placeCard(0, 0, new GuangCaiMiaoJinHuGoldCard());
        goldBattle.state().cardStateAt(0, 0).setActivation(1);
        goldBattle.dispatch(new BattleEvent(BattleEvent.Type.ACTIVATION, 0, 0));

        assertEquals(3, commonBattle.state().timingBase(0));
        // 清单：金=+6 基础分；焕章=发动时我方当前基础分的额外分（此处未结算 → 0）。
        assertEquals(6, goldBattle.state().timingBase(0));
        assertEquals(0, commonBattle.state().timingExtra(0));
        assertEquals(0, goldBattle.state().timingExtra(0));
    }

    @Test
    void qianLiJiangShanCommonAndGoldActivateIndependently()
    {
        NewCardBattle commonBattle = new NewCardBattle();
        commonBattle.placeCard(0, 0, new QianLiJiangShanCommonCard());
        commonBattle.state().cardStateAt(0, 0).setActivation(2);
        commonBattle.dispatch(new BattleEvent(BattleEvent.Type.ACTIVATION, 0, 0));

        NewCardBattle goldBattle = new NewCardBattle();
        goldBattle.placeCard(0, 0, new QianLiJiangShanGoldCard());
        goldBattle.state().cardStateAt(0, 0).setActivation(2);
        goldBattle.dispatch(new BattleEvent(BattleEvent.Type.ACTIVATION, 0, 0));

        assertEquals(2, commonBattle.state().timingMult(0));
        assertEquals(4, goldBattle.state().timingMult(0));
    }

    @Test
    void shadowCalculatorReadsNewStateWithoutMutation()
    {
        BattleState state = new BattleState();
        state.drawCards(0, 2);
        ScoreSnapshot snapshot = ShadowScoreCalculator.calculate(state);
        assertEquals(0, snapshot.sides().get(0).total());
        assertEquals(2, state.handSize(0));
    }

    @Test
    void settlementRuleCombinesWithBaseDiceScore()
    {
        BattleState state = new BattleState();
        state.cardStateAt(0, 0).setDice(List.of(2, 3));
        com.laigu.laigu.duel.DuelGame legacy = new com.laigu.laigu.duel.DuelGame(net.minecraft.util.RandomSource.create());
        RoundScoreController controller = new RoundScoreController(true, context -> {
            if (context.side() == 0) context.addExtra(4);
        });

        ScoreSnapshot result = controller.settle(state, legacy);

        assertEquals(5, result.sides().get(0).base());
        assertEquals(4, result.sides().get(0).extra());
        assertEquals(9, result.sides().get(0).total());
    }

    @Test
    void settlementRuleFailureFallsBackToLegacySnapshot()
    {
        BattleState state = new BattleState();
        com.laigu.laigu.duel.DuelGame legacy = new com.laigu.laigu.duel.DuelGame(net.minecraft.util.RandomSource.create());
        ScoreSnapshot fallback = BattleScoreAdapter.fromLegacy(legacy);
        RoundScoreController controller = new RoundScoreController(true, context -> { throw new IllegalStateException("规则失败"); });

        assertEquals(fallback, controller.settle(state, legacy));
    }

    @Test
    void roundSettlementDispatchesThroughNewBattleBeforeScoring()
    {
        NewCardBattle battle = new NewCardBattle();
        battle.placeCard(0, 0, new QingTongXianHeCommonCard());
        battle.state().cardStateAt(0, 0).setDice(List.of(2));
        com.laigu.laigu.duel.DuelGame legacy = new com.laigu.laigu.duel.DuelGame(net.minecraft.util.RandomSource.create());

        ScoreSnapshot result = new RoundScoreController(true).settle(battle, battle.state(), legacy);

        assertEquals(2, result.sides().get(0).base());
        // 阶段17：结算按设计产生动画事件（每张卡 CARD_TRIGGER 等），不再断言为空。
        assertTrue(!battle.state().animations().isEmpty());
    }

    @Test
    void dicePatternMultiplierMatchesStraightAndParityRules()
    {
        DicePatternMultiplier rules = new DicePatternMultiplier(2, 1, 3, 0, 0);
        assertEquals(2, rules.apply(List.of(1, 2, 3)));
        assertEquals(1, rules.apply(List.of(1, 3, 5)));
        assertEquals(3, rules.apply(List.of(2, 4, 6)));
    }

    @Test
    void roundScoreControllerUsesNewRuleOnlyWhenEnabled()
    {
        BattleState state = new BattleState();
        state.cardStateAt(0, 0).setDice(List.of(3));
        com.laigu.laigu.duel.DuelGame legacy = new com.laigu.laigu.duel.DuelGame(net.minecraft.util.RandomSource.create());
        ScoreSnapshot fallback = BattleScoreAdapter.fromLegacy(legacy);

        ScoreSnapshot disabled = new RoundScoreController(false).settle(state, legacy);
        ScoreSnapshot enabled = new RoundScoreController(true).settle(state, legacy);

        assertEquals(fallback, disabled);
        assertEquals(3, enabled.sides().get(0).base());
    }

    @Test
    void singleRuleControllerCalculatesBaseScoreFromRuntimeDice()
    {
        BattleState state = new BattleState();
        state.placeCard(0, 0, new QingTongXianHeCommonCard());
        state.cardStateAt(0, 0).setDice(List.of(2, 5));
        ScoreSnapshot snapshot = new SingleRuleScoreController(true).calculate(state);
        assertEquals(7, snapshot.sides().get(0).base());
        assertEquals(7, snapshot.sides().get(0).total());
    }

    @Test
    void disabledSingleRuleFallsBackToLegacySnapshot()
    {
        BattleState state = new BattleState();
        ScoreSnapshot fallback = new ScoreSnapshot(List.of(
                ScoreSnapshot.SideScore.of(9, 2, 1), ScoreSnapshot.SideScore.of(0, 1, 0)));
        ScoreSnapshot actual = new SingleRuleScoreController(false).calculateOrFallback(state, fallback);
        assertEquals(fallback, actual);
    }

    @Test
    void migratedSettlementCardsCombineWithDiceScore()
    {
        // 阶段14/15：描金壶结算注册表规则（无激活也 +3/+50 的双算路径）已拆除；
        // 显式 OnSettlement 卡与骰面分正常叠加。
        NewCardBattle battle = new NewCardBattle();
        battle.placeCard(0, 0, new com.laigu.laigu.duel.newcard.cards.WanHeSongFengTuGoldCard());
        battle.state().cardStateAt(0, 0).setDice(List.of(2));
        com.laigu.laigu.duel.DuelGame legacy = new com.laigu.laigu.duel.DuelGame(net.minecraft.util.RandomSource.create());

        ScoreSnapshot result = new RoundScoreController(true).settle(battle, battle.state(), legacy);

        assertEquals(2, result.sides().get(0).base());
        assertEquals(20, result.sides().get(0).extra());
        assertEquals(22, result.sides().get(0).total());
    }

    @Test
    void animationEventsRoundTripThroughNetworkPacketModel()
    {
        List<AnimationEvent> source = List.of(
                new AnimationEvent(AnimationEvent.Type.CARD_TRIGGER, 0, 4, "hai_cuo_tu_gold"),
                new AnimationEvent(AnimationEvent.Type.SCORE_POPUP, 1, 2, "qian_li_jiang_shan_common"));
        net.minecraft.network.FriendlyByteBuf buffer = new net.minecraft.network.FriendlyByteBuf(
                io.netty.buffer.Unpooled.buffer());
        NewAnimationEventPacket packet = new NewAnimationEventPacket(source);
        packet.encode(buffer);
        assertEquals(source, NewAnimationEventPacket.decode(buffer).events());
    }

    @Test
    void battleStatePersistenceRoundTripsFiveSlotDiceAndHand()
    {
        BattleState state = new BattleState();
        state.setRound(4);
        state.drawCards(0, 3);
        state.cardStateAt(0, 0).setDice(List.of(1, 6));
        state.cardStateAt(1, 4).setDice(List.of(2));
        net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
        BattleStatePersistence.save(state, tag);

        BattleState restored = BattleStatePersistence.load(tag);

        assertEquals(4, restored.round());
        assertEquals(3, restored.handSize(0));
        assertEquals(List.of(1, 6), restored.cardStateAt(0, 0).dice());
        assertEquals(List.of(2), restored.cardStateAt(1, 4).dice());
    }

    @Test
    void legacyTagWithoutNewStateLoadsDefaults()
    {
        BattleState restored = BattleStatePersistence.load(new net.minecraft.nbt.CompoundTag());
        assertEquals(1, restored.round());
        assertEquals(0, restored.handSize(0));
        assertTrue(restored.allActiveDice(0).isEmpty());
    }

    @Test
    void frameworkFiveEventRulesEmptyAndInlineBehaviorStillDispatches()
    {
        // 阶段14：默认事件注册表清空，原 12 条规则由卡类 onEvent 内联承载；
        // 分发走 dispatchByInterface（接口 + onEvent 兜底）路径。
        assertTrue(FrameworkFiveEventRules.defaultRegistry().snapshot().isEmpty());

        NewCardBattle battle = new NewCardBattle();
        battle.placeCard(0, 0, new QingTongXianHeCommonCard());
        int before = battle.state().handSize(0);
        battle.dispatch(new BattleEvent(BattleEvent.Type.LEAVE, 0, 0));
        assertEquals(before + 1, battle.state().handSize(0));

        NewCardBattle ambushBattle = new NewCardBattle();
        ambushBattle.placeCard(0, 0, new DunHuangFeiTianCommonCard());
        ambushBattle.dispatch(new BattleEvent(BattleEvent.Type.AMBUSH_FAIL, 0, 0));
        assertEquals(30, ambushBattle.state().timingExtra(0));

        // 清单：溪山金改走 OnSettlement（每骰激活左侧）+ OnSummon 焕章，激活事件不再直接触发它。
        NewCardBattle activationBattle = new NewCardBattle();
        activationBattle.placeCard(0, 0, new QingTongXianHeCommonCard());
        activationBattle.placeCard(0, 1, new XiShanXingLvTuGoldCard());
        activationBattle.state().cardStateAt(0, 1).setDice(List.of(2));
        activationBattle.dispatch(new BattleEvent(BattleEvent.Type.ACTIVATION, 0, 1));
        assertEquals(0, activationBattle.state().cardStateAt(0, 0).activation());
        assertEquals(0, activationBattle.state().timingExtra(0));
    }

    @Test
    void inlineEventRulesHandleComplexTargetsAfterRegistryEmptying()
    {
        // 阶段14：注册表清空（12 条规则已内联），复杂目标派发改由卡类 onEvent 承担。
        assertTrue(FrameworkFiveEventRules.defaultRegistry().snapshot().isEmpty());

        // 飞天金：伏击成功 +60（OnAmbushSuccess）；破坏对位改由结算判定（对方骰最少之一）。
        NewCardBattle ambush = new NewCardBattle();
        ambush.placeCard(0, 0, new DunHuangFeiTianGoldCard());
        ambush.placeCard(1, 0, new QingTongXianHeCommonCard());
        ambush.placeCard(1, 1, new QingTongXianHeGoldCard());
        ambush.state().cardStateAt(1, 0).setDice(List.of(2));
        ambush.state().cardStateAt(1, 1).setDice(List.of(1, 3));
        ambush.dispatch(new BattleEvent(BattleEvent.Type.AMBUSH_SUCCESS, 0, 0));
        assertEquals(60, ambush.state().timingExtra(0));
        assertTrue(ambush.state().destroyAtRoundEnd().isEmpty());

        NewSettlementCalculator.calculate(ambush, new SettlementRuleRegistry());
        assertTrue(ambush.state().destroyAtRoundEnd().contains(new CardContext.CardTarget(1, 0, 1)));
        assertFalse(ambush.state().destroyAtRoundEnd().contains(new CardContext.CardTarget(1, 1, 2)));

        // 海错金：结算时激活我方所有卡2次；仙鹤与海错金自身均无激活词条 → 焕章 +10/次（3张×2次=60）。
        NewCardBattle haiCuo = new NewCardBattle();
        haiCuo.placeCard(0, 0, new QingTongXianHeCommonCard());
        haiCuo.placeCard(0, 1, new HaiCuoTuGoldCard());
        haiCuo.placeCard(0, 2, new QingTongXianHeGoldCard());
        haiCuo.state().cardStateAt(0, 1).setDice(List.of(1, 2, 3));
        NewSettlementCalculator.calculate(haiCuo, new SettlementRuleRegistry());
        assertEquals(60, haiCuo.state().extraScore(0));
    }

    @Test
    void frameworkFiveMigratesActivationThresholdScoreEffects()
    {
        NewCardBattle battle = new NewCardBattle();
        battle.placeCard(0, 0, new QianLiJiangShanGoldCard());
        battle.state().cardStateAt(0, 0).setActivation(2);
        battle.dispatchFrameworkFive(new BattleEvent(BattleEvent.Type.ACTIVATION, 0, 0));
        assertEquals(4, battle.state().timingMult(0));

        NewCardBattle pot = new NewCardBattle();
        pot.placeCard(0, 0, new GuangCaiMiaoJinHuGoldCard());
        pot.state().cardStateAt(0, 0).setActivation(1);
        pot.dispatchFrameworkFive(new BattleEvent(BattleEvent.Type.ACTIVATION, 0, 0));
        // 清单：金=+6 基础分；焕章=发动时当前基础分（未结算 → 0）。
        assertEquals(6, pot.state().timingBase(0));
        assertEquals(0, pot.state().timingExtra(0));
    }

    @Test
    void frameworkFiveSettlementRegistryEmptyAndCustomRulesStillCompose()
    {
        // 阶段14：默认结算注册表清空（原规则由激活卡 onEvent / 显式 OnSettlement 承担）；
        // 自定义注册表组合能力保持不变。
        SettlementRuleRegistry registry = FrameworkFiveRules.defaultRegistry();
        assertTrue(registry.snapshot().isEmpty());
        assertFalse(registry.contains("qian_li_jiang_shan_common"));
        assertFalse(registry.contains("hai_cuo_tu_gold"));
        assertFalse(registry.contains("guang_cai_miao_jin_hu_gold"));

        BattleState state = new BattleState();
        NewCardBattle battle = new NewCardBattle(state);
        battle.placeCard(0, 0, new QingTongXianHeCommonCard());
        state.cardStateAt(0, 0).setDice(List.of(4));
        SettlementRuleRegistry custom = new SettlementRuleRegistry()
                .register("qing_tong_xian_he_common", context -> {
                    context.addBase(2);
                    context.addExtra(3);
                });
        ScoreSnapshot result = NewSettlementCalculator.calculate(battle, custom);
        assertEquals(6, result.sides().get(0).base());
        assertEquals(3, result.sides().get(0).extra());
        assertEquals(9, result.sides().get(0).total());
    }

    @Test
    void scoreSnapshotsCompareEveryScoreComponent()
    {
        ScoreSnapshot expected = new ScoreSnapshot(List.of(
                ScoreSnapshot.SideScore.of(10, 2, 3),
                ScoreSnapshot.SideScore.of(4, 1, 0)));
        ScoreSnapshot actual = new ScoreSnapshot(List.of(
                ScoreSnapshot.SideScore.of(10, 2, 3),
                ScoreSnapshot.SideScore.of(4, 2, 0)));

        ScoreComparison comparison = ScoreComparison.compare(expected, actual);

        assertTrue(!comparison.matches());
        assertTrue(comparison.differences().stream().anyMatch(d -> d.contains("side1.multiplier")));
    }

    @Test
    void cardMappingResultDistinguishesEmptyNotCardAndUnmigrated()
    {
        CardMappingResult empty = CardItemAdapter.inspect(null);
        assertEquals(CardMappingResult.Status.EMPTY, empty.status());
        assertTrue(!empty.mapped());
    }

    @Test
    void registryContainsAllMigratedCardPairs()
    {
        assertTrue(CardFactory.contains("qing_tong_xian_he_common"));
        assertTrue(CardFactory.contains("qing_tong_xian_he_gold"));
        assertTrue(CardFactory.contains("dun_huang_fei_tian_common"));
        assertTrue(CardFactory.contains("dun_huang_fei_tian_gold"));
        assertTrue(CardFactory.contains("qian_li_jiang_shan_common"));
        assertTrue(CardFactory.contains("qian_li_jiang_shan_gold"));
        assertTrue(CardFactory.contains("guang_cai_miao_jin_hu_common"));
        assertTrue(CardFactory.contains("guang_cai_miao_jin_hu_gold"));
        assertTrue(CardFactory.contains("xi_shan_xing_lv_tu_common"));
        assertTrue(CardFactory.contains("xi_shan_xing_lv_tu_gold"));
        assertTrue(CardFactory.contains("hai_cuo_tu_common"));
        assertTrue(CardFactory.contains("hai_cuo_tu_gold"));
    }

    @Test
    void commonAndGoldAreIndependentClasses()
    {
        DuelCard common = CardFactory.create("qing_tong_xian_he_common");
        DuelCard gold = CardFactory.create("qing_tong_xian_he_gold");

        assertNotEquals(common.getClass(), gold.getClass());
        assertEquals("qing_tong_xian_he_common", common.id());
        assertEquals("qing_tong_xian_he_gold", gold.id());
    }

    @Test
    void commonLeaveDrawsOneAndEmitsTrigger()
    {
        DuelCard card = new QingTongXianHeCommonCard();
        CardTestContext context = new CardTestContext(card);

        card.onEvent(new BattleEvent(BattleEvent.Type.LEAVE, 0, 2), context);

        assertEquals(1, context.draws);
        assertEquals(0, context.multiplier);
        assertEquals(1, context.animations.size());
        assertEquals(AnimationEvent.Type.CARD_TRIGGER, context.animations.get(0).type());
    }

    @Test
    void goldLeaveDrawsTwoAddsMultiplierAndEmitsAnimations()
    {
        DuelCard card = new QingTongXianHeGoldCard();
        CardTestContext context = new CardTestContext(card);

        card.onEvent(new BattleEvent(BattleEvent.Type.LEAVE, 1, 3), context);

        assertEquals(2, context.draws);
        assertEquals(4, context.multiplier);
        assertTrue(context.animations.stream().anyMatch(e -> e.type() == AnimationEvent.Type.CARD_TRIGGER));
        assertTrue(context.animations.stream().anyMatch(e -> e.type() == AnimationEvent.Type.MULTIPLIER_POPUP));
    }

    @Test
    void dunHuangCommonOnlyRewardsAmbushFailure()
    {
        DuelCard card = new DunHuangFeiTianCommonCard();
        CardTestContext context = new CardTestContext(card);

        card.onEvent(new BattleEvent(BattleEvent.Type.AMBUSH_FAIL, 0, 1), context);

        assertEquals(30, context.extra);
        assertEquals(1, context.animations.size());
    }

    @Test
    void dunHuangGoldDestroysFewestOpponentDiceCardAtSettlement()
    {
        // 清单：焕章=结算时对位为对方骰子最少之一 → 破坏。
        NewCardBattle battle = new NewCardBattle();
        battle.placeCard(0, 0, new DunHuangFeiTianGoldCard());
        battle.placeCard(1, 0, new QingTongXianHeCommonCard());
        battle.placeCard(1, 1, new QingTongXianHeGoldCard());
        battle.state().cardStateAt(1, 0).setDice(List.of(2));
        battle.state().cardStateAt(1, 1).setDice(List.of(1, 3));

        NewSettlementCalculator.calculate(battle, new SettlementRuleRegistry());
        assertTrue(battle.state().destroyAtRoundEnd().contains(new CardContext.CardTarget(1, 0, 1)));
        assertFalse(battle.state().destroyAtRoundEnd().contains(new CardContext.CardTarget(1, 1, 2)));
    }

    @Test
    void productionContextStoresCardStateAndAnimations()
    {
        DuelCard common = new QingTongXianHeCommonCard();
        BattleState state = new BattleState();
        BattleEventDispatcher dispatcher = new BattleEventDispatcher();

        state.placeCard(0, 2, common);
        dispatcher.dispatch(new BattleEvent(BattleEvent.Type.LEAVE, 0, 2),
                List.of(new CardPlacement(0, 2, common)), state);

        assertEquals(1, state.handSize(0));
        assertEquals(1, state.animations().size());
    }

    @Test
    void battleStateStoresFiveIndependentSlotsPerSide()
    {
        BattleState state = new BattleState();
        DuelCard common = new QingTongXianHeCommonCard();
        DuelCard gold = new QingTongXianHeGoldCard();

        state.placeCard(0, 0, common);
        state.placeCard(0, 4, gold);

        assertEquals(common, state.cardAt(0, 0).orElseThrow());
        assertEquals(gold, state.cardAt(0, 4).orElseThrow());
        assertTrue(state.cardAt(0, 1).isEmpty());
        assertEquals(5, state.field(1).size());
    }

    @Test
    void contextMutatesOnlyItsBoundRuntimeState()
    {
        BattleState state = new BattleState();
        DuelCard card = new QingTongXianHeCommonCard();
        state.placeCard(1, 3, card);
        BattleCardContext context = new BattleCardContext(card, 1, 3, state);

        context.addSelfDie(4);
        context.incrementSelfActivation();
        context.setSelfFaceDown(true);
        context.setSelfLocked(true);
        context.markSelfDestroyAtRoundEnd();

        assertEquals(List.of(4), context.selfDice());
        assertEquals(1, context.selfActivation());
        assertTrue(context.selfFaceDown());
        assertTrue(context.selfLocked());
        assertTrue(state.cardStateAt(1, 3).destroyAtRoundEnd());
        assertTrue(state.cardStateAt(0, 3).dice().isEmpty());
    }

    @Test
    void runtimeStateTracksDiceAndCardFlagsPerSlot()
    {
        BattleState state = new BattleState();
        state.placeCard(0, 2, new QingTongXianHeCommonCard());
        CardRuntimeState runtime = state.cardStateAt(0, 2);

        runtime.addDie(6);
        runtime.incrementActivation();
        runtime.setFaceDown(true);
        runtime.setLocked(true);
        runtime.setDestroyAtRoundEnd(true);

        assertEquals(List.of(6), runtime.dice());
        assertEquals(1, runtime.activation());
        assertTrue(runtime.faceDown());
        assertTrue(runtime.locked());
        assertTrue(runtime.destroyAtRoundEnd());
        assertEquals(0, state.cardStateAt(0, 1).dice().size());
    }

    @Test
    void newCardBattleDispatchesAgainstSharedProductionState()
    {
        NewCardBattle battle = new NewCardBattle();
        battle.placeCard(0, 0, new QingTongXianHeCommonCard());
        battle.placeCard(0, 1, new QingTongXianHeGoldCard());

        List<AnimationEvent> animations = battle.dispatch(new BattleEvent(BattleEvent.Type.LEAVE, 0, 0));

        assertEquals(3, battle.state().handSize(0));
        assertEquals(4, battle.state().timingMult(0));
        assertEquals(3, animations.size());
        assertEquals(2, battle.placements().size());
    }

    @Test
    void battleRejectsOccupiedSlotAndDuplicateCardInstance()
    {
        NewCardBattle battle = new NewCardBattle();
        DuelCard card = new QingTongXianHeCommonCard();
        battle.placeCard(0, 0, card);

        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> battle.placeCard(0, 0, new QingTongXianHeGoldCard()));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> battle.placeCard(0, 1, card));
    }

    @Test
    void replaceCardLeavesOldCardBeforeSummoningNewCard()
    {
        NewCardBattle battle = new NewCardBattle();
        DuelCard oldCard = new QingTongXianHeCommonCard();
        DuelCard newCard = new QingTongXianHeGoldCard();
        battle.placeCard(0, 1, oldCard);

        List<AnimationEvent> events = battle.replaceCard(0, 1, newCard);

        assertEquals(newCard, battle.state().cardAt(0, 1).orElseThrow());
        assertEquals(1, battle.state().handSize(0));
        assertEquals(1, events.size());
        assertEquals(newCard, battle.placements().get(0).card());
    }

    @Test
    void commandExecutorUsesUnifiedLifecycleCommands()
    {
        NewCardBattle battle = new NewCardBattle();
        BattleCommandExecutor executor = new BattleCommandExecutor(battle);
        DuelCard common = new QingTongXianHeCommonCard();
        DuelCard gold = new QingTongXianHeGoldCard();

        BattleCommandResult placed = executor.execute(new BattleCommand.Place(0, 0, common, true));
        BattleCommandResult replaced = executor.execute(new BattleCommand.Replace(0, 0, gold));

        assertTrue(placed.success());
        assertTrue(replaced.success());
        assertEquals(gold, battle.state().cardAt(0, 0).orElseThrow());
        assertEquals(1, battle.state().handSize(0));
        assertEquals(1, replaced.animations().size());
    }

    @Test
    void roundEndRemovesMarkedCardAndAdvancesRound()
    {
        NewCardBattle battle = new NewCardBattle();
        DuelCard card = new QingTongXianHeCommonCard();
        battle.placeCard(0, 0, card);
        battle.state().cardStateAt(0, 0).setDestroyAtRoundEnd(true);

        battle.startRound();
        battle.endRound();

        assertTrue(battle.state().cardAt(0, 0).isEmpty());
        assertEquals(2, battle.state().round());
    }

    @Test
    void commandExecutorReturnsFailureWithoutMutatingState()
    {
        NewCardBattle battle = new NewCardBattle();
        BattleCommandExecutor executor = new BattleCommandExecutor(battle);
        BattleCommandResult result = executor.execute(new BattleCommand.Leave(0, 0));

        assertTrue(!result.success());
        assertTrue(result.message().contains("没有卡牌"));
        assertTrue(battle.state().cardAt(0, 0).isEmpty());
    }

    @Test
    void leaveCardDispatchesBeforeRemovingCardAndClearsRuntimeState()
    {
        NewCardBattle battle = new NewCardBattle();
        DuelCard card = new QingTongXianHeCommonCard();
        battle.placeCard(0, 2, card);
        battle.state().cardStateAt(0, 2).addDie(6);

        List<AnimationEvent> events = battle.leaveCard(0, 2);

        assertEquals(1, battle.state().handSize(0));
        assertTrue(battle.state().cardAt(0, 2).isEmpty());
        assertTrue(battle.state().cardStateAt(0, 2).dice().isEmpty());
        assertEquals(1, events.size());
    }

    @Test
    void dispatcherRoutesLeaveEventToEveryCardAndCollectsAnimations()
    {
        DuelCard common = CardFactory.create("qing_tong_xian_he_common");
        DuelCard gold = CardFactory.create("qing_tong_xian_he_gold");
        List<CardTestContext> contexts = new java.util.ArrayList<>();
        BattleEventDispatcher dispatcher = new BattleEventDispatcher();

        List<AnimationEvent> animations = dispatcher.dispatch(
                new BattleEvent(BattleEvent.Type.LEAVE, 0, 1),
                java.util.List.of(common, gold),
                (card, events) -> {
                    CardTestContext context = new CardTestContext(card, events);
                    contexts.add(context);
                    return context;
                });

        assertEquals(2, contexts.size());
        assertEquals(3, contexts.get(0).draws + contexts.get(1).draws);
        assertEquals(4, contexts.get(1).multiplier);
        assertEquals(3, animations.size());
    }

    /** 阶段15：多词条声明的测试替身——显式 OnSettlement + effects() 事件词条。 */
    private static class MultiEffectTestCard implements DuelCard, OnSettlement
    {
        @Override public String id() { return "t_xing_bo_hua_common"; }
        @Override public String displayName() { return "T形帛画"; }
        @Override public com.laigu.laigu.duel.CardClass cardClass() { return com.laigu.laigu.duel.CardClass.GONG; }
        @Override public void onSettlement(SettlementContext context) { context.addExtra(7); }
        @Override public java.util.List<CardEffect> effects()
        {
            return java.util.List.of(
                    CardEffects.event(BattleEvent.Type.LEAVE, context -> context.drawCards(2)));
        }
    }
}
