package com.laigu.laigu.duel.newcard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** 新卡牌架构的战斗状态容器，不依赖旧 DuelGame。 */
public final class BattleState
{
    public static final int SIDES = 2;
    public static final int SLOTS = 4;

    private final List<Integer> handSizes = new ArrayList<>(List.of(0, 0));
    private final List<Integer> baseScores = new ArrayList<>(List.of(0, 0));
    private final List<Integer> multipliers = new ArrayList<>(List.of(0, 0));
    private final List<Integer> extraScores = new ArrayList<>(List.of(0, 0));
    private final List<List<DuelCard>> fields = new ArrayList<>();
    private final List<List<CardRuntimeState>> fieldStates = new ArrayList<>();
    private final List<CardContext.CardTarget> opponentTargets = new ArrayList<>();
    private final List<CardContext.CardTarget> destroyAtRoundEnd = new ArrayList<>();
    private final List<AnimationEvent> animations = new ArrayList<>();
    private int round = 1;

    public BattleState()
    {
        for (int side = 0; side < SIDES; side++)
        {
            fields.add(new ArrayList<>(Collections.nCopies(SLOTS, null)));
            fieldStates.add(new ArrayList<>(java.util.stream.IntStream.range(0, SLOTS)
                    .mapToObj(i -> new CardRuntimeState()).toList()));
        }
    }

    public void drawCards(int side, int amount)
    {
        checkSide(side);
        if (amount < 0) throw new IllegalArgumentException("抽牌数量不能为负数");
        handSizes.set(side, handSizes.get(side) + amount);
    }

    public int handSize(int side) { checkSide(side); return handSizes.get(side); }
    public int baseScore(int side) { checkSide(side); return baseScores.get(side); }
    public int multiplier(int side) { checkSide(side); return multipliers.get(side); }
    public int extraScore(int side) { checkSide(side); return extraScores.get(side); }
    public int round() { return round; }
    public void setRound(int round) { if (round < 1) throw new IllegalArgumentException("回合必须从1开始"); this.round = round; }

    void addBaseScore(int side, int amount) { checkSide(side); baseScores.set(side, baseScores.get(side) + amount); }
    void addMultiplier(int side, int amount) { checkSide(side); multipliers.set(side, multipliers.get(side) + amount); }
    void addExtraScore(int side, int amount) { checkSide(side); extraScores.set(side, extraScores.get(side) + amount); }

    public void placeCard(int side, int slot, DuelCard card)
    {
        checkSlot(side, slot);
        fields.get(side).set(slot, Objects.requireNonNull(card));
    }

    public Optional<DuelCard> cardAt(int side, int slot)
    {
        checkSlot(side, slot);
        return Optional.ofNullable(fields.get(side).get(slot));
    }

    public Optional<DuelCard> removeCard(int side, int slot)
    {
        checkSlot(side, slot);
        DuelCard removed = fields.get(side).set(slot, null);
        fieldStates.get(side).set(slot, new CardRuntimeState());
        return Optional.ofNullable(removed);
    }

    public List<DuelCard> field(int side)
    {
        checkSide(side);
        return Collections.unmodifiableList(new ArrayList<>(fields.get(side)));
    }

    public CardRuntimeState cardStateAt(int side, int slot)
    {
        checkSlot(side, slot);
        return fieldStates.get(side).get(slot);
    }

    public void setOpponentTargets(List<CardContext.CardTarget> targets)
    {
        opponentTargets.clear();
        opponentTargets.addAll(Objects.requireNonNull(targets));
    }

    Optional<CardContext.CardTarget> opponentTargetWithFewestDice()
    {
        return opponentTargets.stream().min((a, b) -> Integer.compare(a.diceCount(), b.diceCount()));
    }

    void markDestroyAtRoundEnd(CardContext.CardTarget target)
    {
        if (!destroyAtRoundEnd.contains(target)) destroyAtRoundEnd.add(target);
    }

    public List<CardContext.CardTarget> destroyAtRoundEnd() { return Collections.unmodifiableList(destroyAtRoundEnd); }
    void emit(AnimationEvent event) { animations.add(Objects.requireNonNull(event)); }
    int animationCount() { return animations.size(); }
    List<AnimationEvent> animationsFrom(int start)
    {
        return List.copyOf(animations.subList(start, animations.size()));
    }
    public List<AnimationEvent> animations() { return Collections.unmodifiableList(animations); }

    private static void checkSide(int side)
    {
        if (side < 0 || side >= SIDES) throw new IllegalArgumentException("无效对战方：" + side);
    }

    private static void checkSlot(int side, int slot)
    {
        checkSide(side);
        if (slot < 0 || slot >= SLOTS) throw new IllegalArgumentException("无效场位：" + slot);
    }
}
