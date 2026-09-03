package com.laigu.laigu.duel.newcard;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

final class CardTestContext implements CardContext
{
    private final DuelCard self;
    int draws;
    int restoredActionPoints;
    int winnerLast = -1;
    int base;
    int multiplier;
    int extra;
    private final CardRuntimeState runtimeState = new CardRuntimeState();
    CardContext.CardTarget fewestTarget;
    CardContext.CardTarget leftTarget;
    CardContext.CardTarget rightTarget;
    CardContext.CardTarget activatedTarget;
    int activationTimes;
    CardContext.CardTarget markedTarget;
    final List<AnimationEvent> animations;

    CardTestContext(DuelCard self)
    {
        this(self, new ArrayList<>());
    }

    CardTestContext(DuelCard self, List<AnimationEvent> animations)
    {
        this.self = self;
        this.animations = animations;
    }

    @Override public DuelCard self() { return self; }
    @Override public void drawCards(int amount) { draws += amount; }
    @Override public void restoreActionPoints(int amount) { restoredActionPoints += amount; }
    @Override public int winnerLast() { return winnerLast; }
    @Override public int side() { return 0; }
    @Override public int slot() { return 0; }
    @Override public void addTimingBase(int amount) { base += amount; }
    @Override public void addTimingMult(int amount) { multiplier += amount; }
    @Override public void addTimingExtra(int amount) { extra += amount; }
    @Override public void addBaseScore(int amount) { base += amount; }
    @Override public void addMultiplier(int amount) { multiplier += amount; }
    @Override public void addExtraScore(int amount) { extra += amount; }
    @Override public CardRuntimeState selfState() { return runtimeState; }
    CardRuntimeState oppositeRuntimeState;
    @Override public Optional<CardRuntimeState> oppositeState() { return Optional.ofNullable(oppositeRuntimeState); }
    @Override public List<Integer> oppositeDice() { return oppositeRuntimeState == null ? List.of() : oppositeRuntimeState.dice(); }
    @Override public Counter counter(String name) { return new TestCounter(name); }
    @Override public List<Integer> selfDice() { return runtimeState.dice(); }
    @Override public void addSelfDie(int value) { runtimeState.addDie(value); }
    @Override public int selfActivation() { return runtimeState.activation(); }
    @Override public void incrementSelfActivation() { runtimeState.incrementActivation(); }
    @Override public boolean selfFaceDown() { return runtimeState.faceDown(); }
    @Override public void setSelfFaceDown(boolean faceDown) { runtimeState.setFaceDown(faceDown); }
    @Override public boolean selfLocked() { return runtimeState.locked(); }
    @Override public void setSelfLocked(boolean locked) { runtimeState.setLocked(locked); }
    @Override public void markSelfDestroyAtRoundEnd() { runtimeState.setDestroyAtRoundEnd(true); }
    @Override public Optional<CardContext.CardTarget> leftCard() { return Optional.ofNullable(leftTarget); }
    @Override public Optional<CardContext.CardTarget> rightCard() { return Optional.ofNullable(rightTarget); }
    @Override public void activate(CardContext.CardTarget target) { activate(target, 1); }
    @Override public void activate(CardContext.CardTarget target, int times) { activatedTarget = target; activationTimes += times; }
    @Override public Optional<CardContext.CardTarget> opponentTargetWithFewestDice()
    {
        return Optional.ofNullable(fewestTarget);
    }
    @Override public void markDestroyAtRoundEnd(CardContext.CardTarget target)
    {
        markedTarget = target;
    }
    @Override public int ownGoldCardCount() { return self().rarity() == CardRarity.GOLD ? 1 : 0; }
    @Override public void emit(AnimationEvent event) { animations.add(event); }

    private final class TestCounter implements CardContext.Counter
    {
        private final String name;

        private TestCounter(String name) { this.name = name; }

        @Override public int value() { return runtimeState.counter(name); }
        @Override public void add(int amount) { runtimeState.addCounter(name, amount); }
        @Override public void set(int value) { runtimeState.setCounter(name, value); }
    }
}
