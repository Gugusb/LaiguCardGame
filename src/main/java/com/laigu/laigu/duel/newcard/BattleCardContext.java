package com.laigu.laigu.duel.newcard;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** 将一张独立卡牌绑定到 BattleState 的生产上下文。 */
public final class BattleCardContext implements CardContext
{
    private final DuelCard self;
    private final int side;
    private final int slot;
    private final BattleState state;

    public BattleCardContext(DuelCard self, int side, int slot, BattleState state)
    {
        this.self = Objects.requireNonNull(self);
        this.side = side;
        this.slot = slot;
        this.state = Objects.requireNonNull(state);
        if (side < 0 || side > 1) throw new IllegalArgumentException("无效对战方：" + side);
        if (slot < 0 || slot >= BattleState.SLOTS) throw new IllegalArgumentException("无效场位：" + slot);
    }

    @Override public DuelCard self() { return self; }
    @Override public void drawCards(int amount) { state.drawCards(side, amount); }
    @Override public void addBaseScore(int amount) { state.addBaseScore(side, amount); }
    @Override public void addMultiplier(int amount) { state.addMultiplier(side, amount); }
    @Override public void addExtraScore(int amount) { state.addExtraScore(side, amount); }
    @Override public CardRuntimeState selfState() { return state.cardStateAt(side, slot); }
    @Override public List<Integer> selfDice() { return selfState().dice(); }
    @Override public void addSelfDie(int value) { selfState().addDie(value); }
    @Override public int selfActivation() { return selfState().activation(); }
    @Override public void incrementSelfActivation() { selfState().incrementActivation(); }
    @Override public boolean selfFaceDown() { return selfState().faceDown(); }
    @Override public void setSelfFaceDown(boolean faceDown) { selfState().setFaceDown(faceDown); }
    @Override public boolean selfLocked() { return selfState().locked(); }
    @Override public void setSelfLocked(boolean locked) { selfState().setLocked(locked); }
    @Override public void markSelfDestroyAtRoundEnd() { selfState().setDestroyAtRoundEnd(true); }
    @Override public Optional<CardTarget> leftCard()
    {
        int left = slot - 1;
        return left < 0 ? Optional.empty() : state.cardAt(side, left)
                .map(card -> new CardTarget(side, left, state.cardStateAt(side, left).dice().size()));
    }
    @Override public void activate(CardTarget target)
    {
        state.cardStateAt(target.side(), target.slot()).incrementActivation();
        state.emit(new AnimationEvent(AnimationEvent.Type.CARD_ACTIVATE, target.side(), target.slot(), self.id()));
    }
    @Override public Optional<CardTarget> opponentTargetWithFewestDice() { return state.opponentTargetWithFewestDice(); }
    @Override public void markDestroyAtRoundEnd(CardTarget target) { state.markDestroyAtRoundEnd(target); }
    @Override public void emit(AnimationEvent event) { state.emit(event); }

    private int selfSlot()
    {
        return state.field(side).indexOf(self);
    }

    public int side() { return side; }
    public int slot() { return slot; }
    public BattleState state() { return state; }
}
