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
    @Override public void restoreActionPoints(int amount) { state.restoreActionPoints(side, amount); }
    @Override public int winnerLast() { return state.winnerLast(); }
    @Override public void addBaseScore(int amount) { state.addBaseScore(side, amount); }
    @Override public void addMultiplier(int amount) { state.addMultiplier(side, amount); }
    @Override public void addExtraScore(int amount) { state.addExtraScore(side, amount); }
    @Override public CardRuntimeState selfState() { return state.cardStateAt(side, slot); }
    @Override public Optional<CardRuntimeState> oppositeState()
    {
        return state.cardAt(1 - side, slot).map(card -> state.cardStateAt(1 - side, slot));
    }
    @Override public List<Integer> oppositeDice()
    {
        return oppositeState().map(CardRuntimeState::dice).orElse(List.of());
    }
    @Override public Counter counter(String name) { return new RuntimeCounter(name); }
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
    @Override public Optional<CardTarget> rightCard()
    {
        int right = slot + 1;
        return right >= BattleState.SLOTS ? Optional.empty() : state.cardAt(side, right)
                .map(card -> new CardTarget(side, right, state.cardStateAt(side, right).dice().size()));
    }
    @Override public void activate(CardTarget target) { activate(target, 1); }
    @Override public void activate(CardTarget target, int times)
    {
        if (times < 0) throw new IllegalArgumentException("激活次数不能为负数");
        for (int i = 0; i < times; i++)
        {
            state.cardStateAt(target.side(), target.slot()).incrementActivation();
            state.emit(new AnimationEvent(AnimationEvent.Type.CARD_ACTIVATE, target.side(), target.slot(), self.id()));
            state.dispatchActivation(target.side(), target.slot());
        }
    }
    @Override public Optional<CardTarget> opponentTargetWithFewestDice() { return state.opponentTargetWithFewestDice(); }
    @Override public void markDestroyAtRoundEnd(CardTarget target) { state.markDestroyAtRoundEnd(target); }
    @Override public int ownGoldCardCount()
    {
        int count = 0;
        for (int slot = 0; slot < BattleState.SLOTS; slot++)
        {
            DuelCard card = state.cardAt(side, slot).orElse(null);
            if (card != null && card.rarity() == CardRarity.GOLD) count++;
        }
        return count;
    }
    @Override public void emit(AnimationEvent event) { state.emit(event); }

    // ================= 抢骰（阶段16） =================

    @Override public int rollDie() { return state.rollDie(); }
    @Override public void addSharedPoolDie(int face) { state.addSharedPoolDie(face); }
    @Override public int rerollSharedPoolAbove(int face) { return state.rerollSharedPoolAbove(face); }
    @Override public int rerollUses() { return state.rerollUses(); }
    @Override public void useReroll() { state.useReroll(); }
    @Override public void addDraftTurnBonusSelf(int amount) { state.addDraftTurnBonus(side, amount); }

    // ================= 阶段18：局内时机通道与激活目标 =================

    @Override public void addTimingBase(int amount) { state.addTimingBase(side, amount); }
    @Override public void addTimingMult(int amount) { state.addTimingMult(side, amount); }
    @Override public void addTimingExtra(int amount) { state.addTimingExtra(side, amount); }

    @Override public java.util.List<CardTarget> ownActivatableTargets()
    {
        java.util.List<CardTarget> targets = new java.util.ArrayList<>();
        for (int s = 0; s < BattleState.SLOTS; s++)
        {
            DuelCard card = state.cardAt(side, s).orElse(null);
            if (card instanceof OnActivation handler && handler.activationThreshold() > 0)
                targets.add(new CardTarget(side, s, state.cardStateAt(side, s).dice().size()));
        }
        return targets;
    }

    @Override public java.util.List<CardTarget> ownCardTargets()
    {
        java.util.List<CardTarget> targets = new java.util.ArrayList<>();
        for (int s = 0; s < BattleState.SLOTS; s++)
        {
            if (state.cardAt(side, s).isEmpty()) continue;
            targets.add(new CardTarget(side, s, state.cardStateAt(side, s).dice().size()));
        }
        return targets;
    }

    @Override public int attemptActivateOwnCards(int times)
    {
        if (times < 0) throw new IllegalArgumentException("激活次数不能为负数");
        int failed = 0;
        for (int s = 0; s < BattleState.SLOTS; s++)
        {
            DuelCard card = state.cardAt(side, s).orElse(null);
            if (card == null) continue;
            CardTarget target = new CardTarget(side, s, state.cardStateAt(side, s).dice().size());
            if (card instanceof OnActivation handler && handler.activationThreshold() > 0) activate(target, times);
            else failed += times;
        }
        return failed;
    }

    @Override public int oppositeCardsWithoutDice()
    {
        int count = 0;
        for (int s = 0; s < BattleState.SLOTS; s++)
        {
            if (state.cardAt(1 - side, s).isPresent() && state.cardStateAt(1 - side, s).dice().isEmpty()) count++;
        }
        return count;
    }
    @Override public int ownBaseScore() { return state.baseScore(side); }
    @Override public void setParityViewOdd() { state.setParityView(side, 1); }
    @Override public void setParityViewEven() { state.setParityView(side, 2); }
    @Override public boolean parityViewActive() { return state.parityView(side) != 0; }
    @Override public boolean parityViewOdd() { return state.parityView(side) == 1; }
    @Override public void setDynastyViewToLeftmost() { state.setDynastyView(side, true); }
    @Override public boolean oddDiceRound() { return state.round() % 2 == 1; }
    @Override public int handSize() { return state.handSize(side); }
    @Override public int maxHandSize() { return 8; }
    @Override public void addPersistentBaseBonus(int amount) { selfState().addPersistentBaseBonus(amount); }
    @Override public int persistentBaseBonus() { return selfState().persistentBaseBonus(); }

    /** 绑定到当前场位 CardRuntimeState 的计数器视图。 */
    private final class RuntimeCounter implements CardContext.Counter
    {
        private final String name;

        private RuntimeCounter(String name) { this.name = Objects.requireNonNull(name); }

        @Override public int value() { return selfState().counter(name); }
        @Override public void add(int amount) { selfState().addCounter(name, amount); }
        @Override public void set(int value) { selfState().setCounter(name, value); }
    }

    public int side() { return side; }
    @Override public int slot() { return slot; }
    public BattleState state() { return state; }
}
