package com.laigu.laigu.duel.newcard;

import com.laigu.laigu.duel.newcard.cards.QingTongXianHeCommonCard;
import com.laigu.laigu.duel.newcard.cards.QingTongXianHeGoldCard;
import com.laigu.laigu.duel.newcard.cards.DunHuangFeiTianCommonCard;
import com.laigu.laigu.duel.newcard.cards.DunHuangFeiTianGoldCard;
import com.laigu.laigu.duel.newcard.cards.QianLiJiangShanCommonCard;
import com.laigu.laigu.duel.newcard.cards.QianLiJiangShanGoldCard;
import com.laigu.laigu.duel.newcard.cards.GuangCaiMiaoJinHuCommonCard;
import com.laigu.laigu.duel.newcard.cards.GuangCaiMiaoJinHuGoldCard;
import com.laigu.laigu.duel.newcard.cards.XiShanXingLvTuCommonCard;
import com.laigu.laigu.duel.newcard.cards.XiShanXingLvTuGoldCard;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    void xiShanCommonAndGoldActivateLeftCardIndependently()
    {
        CardContext.CardTarget left = new CardContext.CardTarget(0, 0, 2);
        CardTestContext commonContext = new CardTestContext(new XiShanXingLvTuCommonCard());
        CardTestContext goldContext = new CardTestContext(new XiShanXingLvTuGoldCard());
        commonContext.leftTarget = left;
        goldContext.leftTarget = left;

        commonContext.self().onEvent(new BattleEvent(BattleEvent.Type.ACTIVATION, 0, 1), commonContext);
        goldContext.self().onEvent(new BattleEvent(BattleEvent.Type.ACTIVATION, 0, 1), goldContext);

        assertEquals(left, commonContext.activatedTarget);
        assertEquals(left, goldContext.activatedTarget);
        assertEquals(0, commonContext.extra);
        assertEquals(5, goldContext.extra);
    }

    @Test
    void miaoJinHuCommonAndGoldActivateIndependently()
    {
        DuelCard common = new GuangCaiMiaoJinHuCommonCard();
        DuelCard gold = new GuangCaiMiaoJinHuGoldCard();
        CardTestContext commonContext = new CardTestContext(common);
        CardTestContext goldContext = new CardTestContext(gold);
        commonContext.incrementSelfActivation();
        goldContext.incrementSelfActivation();

        common.onEvent(new BattleEvent(BattleEvent.Type.ACTIVATION, 0, 0), commonContext);
        gold.onEvent(new BattleEvent(BattleEvent.Type.ACTIVATION, 0, 1), goldContext);

        assertEquals(3, commonContext.base);
        assertEquals(3, goldContext.base);
        assertEquals(0, commonContext.extra);
        assertEquals(50, goldContext.extra);
    }

    @Test
    void qianLiJiangShanCommonAndGoldActivateIndependently()
    {
        DuelCard common = new QianLiJiangShanCommonCard();
        DuelCard gold = new QianLiJiangShanGoldCard();
        CardTestContext commonContext = new CardTestContext(common);
        CardTestContext goldContext = new CardTestContext(gold);
        commonContext.incrementSelfActivation();
        commonContext.incrementSelfActivation();
        goldContext.incrementSelfActivation();
        goldContext.incrementSelfActivation();

        common.onEvent(new BattleEvent(BattleEvent.Type.ACTIVATION, 0, 0), commonContext);
        gold.onEvent(new BattleEvent(BattleEvent.Type.ACTIVATION, 0, 1), goldContext);

        assertEquals(2, commonContext.multiplier);
        assertEquals(4, goldContext.multiplier);
        assertEquals(2, commonContext.animations.size());
        assertEquals(2, goldContext.animations.size());
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
    void dunHuangGoldMarksFewestOpponentCardOnSuccess()
    {
        DuelCard card = new DunHuangFeiTianGoldCard();
        CardTestContext context = new CardTestContext(card);
        context.fewestTarget = new CardContext.CardTarget(1, 2, 0);

        card.onEvent(new BattleEvent(BattleEvent.Type.AMBUSH_SUCCESS, 0, 1), context);

        assertEquals(context.fewestTarget, context.markedTarget);
        assertEquals(AnimationEvent.Type.CARD_DESTROY_MARK, context.animations.get(0).type());
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
    void battleStateStoresFourIndependentSlotsPerSide()
    {
        BattleState state = new BattleState();
        DuelCard common = new QingTongXianHeCommonCard();
        DuelCard gold = new QingTongXianHeGoldCard();

        state.placeCard(0, 0, common);
        state.placeCard(0, 3, gold);

        assertEquals(common, state.cardAt(0, 0).orElseThrow());
        assertEquals(gold, state.cardAt(0, 3).orElseThrow());
        assertTrue(state.cardAt(0, 1).isEmpty());
        assertEquals(4, state.field(1).size());
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
        assertEquals(4, battle.state().multiplier(0));
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
}
