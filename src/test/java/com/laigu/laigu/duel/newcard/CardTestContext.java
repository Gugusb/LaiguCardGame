package com.laigu.laigu.duel.newcard;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

final class CardTestContext implements CardContext
{
    private final DuelCard self;
    int draws;
    int base;
    int multiplier;
    int extra;
    private final CardRuntimeState runtimeState = new CardRuntimeState();
    CardContext.CardTarget fewestTarget;
    CardContext.CardTarget leftTarget;
    CardContext.CardTarget activatedTarget;
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
    @Override public void addBaseScore(int amount) { base += amount; }
    @Override public void addMultiplier(int amount) { multiplier += amount; }
    @Override public void addExtraScore(int amount) { extra += amount; }
    @Override public CardRuntimeState selfState() { return runtimeState; }
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
    @Override public void activate(CardContext.CardTarget target) { activatedTarget = target; }
    @Override public Optional<CardContext.CardTarget> opponentTargetWithFewestDice()
    {
        return Optional.ofNullable(fewestTarget);
    }
    @Override public void markDestroyAtRoundEnd(CardContext.CardTarget target)
    {
        markedTarget = target;
    }
    @Override public void emit(AnimationEvent event) { animations.add(event); }
}
