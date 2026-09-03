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
    public static final int SLOTS = 5;

    private final List<Integer> handSizes = new ArrayList<>(List.of(0, 0));
    private final List<Integer> poolSizes = new ArrayList<>(List.of(0, 0));   // 未布置骰（每方私有骰池）
    private final List<Integer> deckSizes = new ArrayList<>(List.of(0, 0));   // 牌库剩余未抽张数
    private final List<Integer> sharedPool = new ArrayList<>();               // 共享骰池
    private final List<Integer> baseScores = new ArrayList<>(List.of(0, 0));
    private final List<Integer> multipliers = new ArrayList<>(List.of(0, 0));
    private final List<Integer> extraScores = new ArrayList<>(List.of(0, 0));
    private final List<Integer> actionPoints = new ArrayList<>(List.of(0, 0));
    private int maxActionPoints = 3;
    private final List<List<DuelCard>> fields = new ArrayList<>();
    private final List<List<CardRuntimeState>> fieldStates = new ArrayList<>();
    private final List<CardContext.CardTarget> opponentTargets = new ArrayList<>();
    private final List<CardContext.CardTarget> destroyAtRoundEnd = new ArrayList<>();
    private final List<AnimationEvent> animations = new ArrayList<>();
    private int round = 1;
    private int winnerLast = -1;
    private final List<Integer> wins = new ArrayList<>(List.of(0, 0));

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
    /** 未布置骰数量（每方私有骰池；供「每颗未布置骰」类词条）。 */
    public int poolSize(int side) { checkSide(side); return poolSizes.get(side); }
    public void setPoolSize(int side, int value)
    {
        checkSide(side);
        if (value < 0) throw new IllegalArgumentException("骰池数量不能为负数");
        poolSizes.set(side, value);
    }
    /** 牌库剩余张数（供「每张未抽卡组牌」类词条）。 */
    public int deckSize(int side) { checkSide(side); return deckSizes.get(side); }
    public void setDeckSize(int side, int value)
    {
        checkSide(side);
        if (value < 0) throw new IllegalArgumentException("牌库数量不能为负数");
        deckSizes.set(side, value);
    }
    /** 共享骰池（抢骰阶段双方抓取的公共骰；供「共享骰池每颗」类词条）。 */
    public List<Integer> sharedPool() { return Collections.unmodifiableList(sharedPool); }
    public void setSharedPool(List<Integer> values)
    {
        sharedPool.clear();
        for (int value : Objects.requireNonNull(values))
        {
            if (value < 1 || 6 < value) throw new IllegalArgumentException("骰面必须在1到6之间");
            sharedPool.add(value);
        }
    }

    // ================= 抢骰（阶段16） =================

    /** 战斗随机源（默认 JVM 随机；DuelGame 可注入 RandomSource 以对齐旧引擎）。 */
    private java.util.random.RandomGenerator random = new java.util.Random();
    /** 双方抓取计划：先手每次颗数 / 后手每次颗数（buildDraftPlan 时构建）。 */
    private final List<Integer> draftFirstSizes = new ArrayList<>();
    private final List<Integer> draftSecondSizes = new ArrayList<>();
    private int firstPicker = 0;
    /** 本轮重骰已用次数（回合开始清零）。 */
    private int rerollUses = 0;
    /** Q7 拍板：入场一次性抓骰次数加成（金 T形帛画/编钟入场时 +1；随计划构建消耗，回合开始清空）。 */
    private final int[] draftTurnBonus = new int[2];

    public void setRandom(java.util.random.RandomGenerator random)
    {
        this.random = java.util.Objects.requireNonNull(random);
    }

    /** 掷一颗骰（1-6）。 */
    public int rollDie() { return 1 + random.nextInt(6); }

    /** 向共享骰池加入一颗骰（1-6）。 */
    public void addSharedPoolDie(int face)
    {
        if (face < 1 || 6 < face) throw new IllegalArgumentException("骰面必须在1到6之间");
        sharedPool.add(face);
    }

    /** 从共享骰池移除并返回骰面（抓取动作的状态部分）。 */
    public int removeSharedPoolDie(int index)
    {
        if (index < 0 || index >= sharedPool.size()) throw new IllegalArgumentException("无效骰位");
        return sharedPool.remove(index);
    }
    /** 重骰共享池中所有 > face 的骰，返回重骰颗数（对齐旧 pickDie 重骰语义）。 */
    public int rerollSharedPoolAbove(int face)
    {
        int count = 0;
        for (int i = 0; i < sharedPool.size(); i++)
            if (sharedPool.get(i) > face) { sharedPool.set(i, rollDie()); count++; }
        return count;
    }

    public int rerollUses() { return rerollUses; }
    public void useReroll() { rerollUses++; }
    /** 回合开始清零本轮重骰次数。 */
    public void resetRerollUses() { rerollUses = 0; }

    /** Q7 拍板：入场一次性抓骰次数加成（写入方侧）。 */
    public void addDraftTurnBonus(int side, int amount)
    {
        checkSide(side);
        if (amount < 0) throw new IllegalArgumentException("加成不能为负");
        draftTurnBonus[side] += amount;
    }

    public int draftTurnBonus(int side) { checkSide(side); return draftTurnBonus[side]; }

    /** 读取并清零（计划构建时一次性消耗）。 */
    public int consumeDraftTurnBonus(int side)
    {
        checkSide(side);
        int value = draftTurnBonus[side];
        draftTurnBonus[side] = 0;
        return value;
    }

    /** 双侧加成清空（回合开始防遗留）。 */
    public void clearDraftTurnBonuses()
    {
        draftTurnBonus[0] = 0;
        draftTurnBonus[1] = 0;
    }

    public int firstPicker() { return firstPicker; }
    public void setFirstPicker(int value) { firstPicker = value; }

    public List<Integer> draftFirstSizes() { return Collections.unmodifiableList(draftFirstSizes); }
    public List<Integer> draftSecondSizes() { return Collections.unmodifiableList(draftSecondSizes); }
    /** 写入双方抓取计划（颗数序列；空表 = 该方无抓取）。 */
    public void setDraftPlan(List<Integer> first, List<Integer> second)
    {
        draftFirstSizes.clear();
        draftFirstSizes.addAll(Objects.requireNonNull(first));
        draftSecondSizes.clear();
        draftSecondSizes.addAll(Objects.requireNonNull(second));
    }
    /** 回合开始清空上轮计划与一次性入场加成。 */
    public void clearDraftPlan()
    {
        draftFirstSizes.clear();
        draftSecondSizes.clear();
        draftTurnBonus[0] = 0;
        draftTurnBonus[1] = 0;
    }
    void setHandSizeForPersistence(int side, int amount)
    {
        checkSide(side);
        if (amount < 0) throw new IllegalArgumentException("手牌数量不能为负数");
        handSizes.set(side, amount);
    }
    public int baseScore(int side) { checkSide(side); return baseScores.get(side); }
    public int multiplier(int side) { checkSide(side); return multipliers.get(side); }
    public int extraScore(int side) { checkSide(side); return extraScores.get(side); }
    public int overflowDrawMultiplier(int side) { checkSide(side); return 0; }
    public int round() { return round; }
    public int winnerLast() { return winnerLast; }
    public void setWinnerLast(int winnerLast)
    {
        if (winnerLast < -1 || winnerLast >= SIDES) throw new IllegalArgumentException("无效上轮胜者：" + winnerLast);
        this.winnerLast = winnerLast;
    }
    public int wins(int side) { checkSide(side); return wins.get(side); }
    public void setWins(int side, int value)
    {
        checkSide(side);
        if (value < 0) throw new IllegalArgumentException("胜场不能为负数");
        wins.set(side, value);
    }
    public void setRound(int round) { if (round < 1) throw new IllegalArgumentException("回合必须从1开始"); this.round = round; }

    void addBaseScore(int side, int amount) { checkSide(side); baseScores.set(side, baseScores.get(side) + amount); }

    /** 行动力：当前值 / 上限 / 回复（不超过上限；对应旧 DuelGame.actionPoints 语义）。 */
    public int actionPoints(int side) { checkSide(side); return actionPoints.get(side); }
    public void setActionPoints(int side, int value)
    {
        checkSide(side);
        if (value < 0) throw new IllegalArgumentException("行动力不能为负数");
        actionPoints.set(side, value);
    }
    public int maxActionPoints() { return maxActionPoints; }
    public void setMaxActionPoints(int maxActionPoints)
    {
        if (maxActionPoints < 1) throw new IllegalArgumentException("行动力上限至少为1");
        this.maxActionPoints = maxActionPoints;
    }
    public void restoreActionPoints(int side, int amount)
    {
        checkSide(side);
        if (amount < 0) throw new IllegalArgumentException("回复行动力不能为负数");
        actionPoints.set(side, Math.min(maxActionPoints, actionPoints.get(side) + amount));
    }
    void addBaseScoreForRule(int side, int amount) { addBaseScore(side, amount); }
    void setMultiplierForRule(int side, int amount) { checkSide(side); multipliers.set(side, amount); }
    void addMultiplier(int side, int amount) { checkSide(side); multipliers.set(side, multipliers.get(side) + amount); }
    void addMultiplierForRule(int side, int amount) { addMultiplier(side, amount); }
    void addExtraScore(int side, int amount) { checkSide(side); extraScores.set(side, extraScores.get(side) + amount); }
    void addExtraScoreForRule(int side, int amount) { addExtraScore(side, amount); }

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

    /** 阶段17：取出全部动画事件并清空（防跨轮累积）。 */
    public List<AnimationEvent> drainAnimations()
    {
        List<AnimationEvent> out = List.copyOf(animations);
        animations.clear();
        return out;
    }

    public void dispatchSettlementEvents()
    {
        // 由 NewCardBattle 绑定卡牌后执行；空实现保证未绑定的影子状态安全。
    }

    private java.util.function.BiConsumer<Integer, Integer> activationDispatcher = (side, slot) -> {};
    /** 绑定激活事件派发钩子（由 NewCardBattle 注入；逐次激活语义：每次进度+1 后立即派发）。 */
    public void setActivationDispatcher(java.util.function.BiConsumer<Integer, Integer> dispatcher)
    {
        activationDispatcher = java.util.Objects.requireNonNull(dispatcher);
    }

    /** 激活进度 +1 后派发激活事件；未绑定战斗时为空操作（影子状态安全）。 */
    void dispatchActivation(int side, int slot) { activationDispatcher.accept(side, slot); }

    public List<Integer> allActiveDice(int side)
    {
        checkSide(side);
        List<Integer> dice = new ArrayList<>();
        for (int slot = 0; slot < SLOTS; slot++) dice.addAll(fieldStates.get(side).get(slot).activeDice());
        return List.copyOf(dice);
    }

    // ================= 阶段18：局内时机通道 =================
    // 事件触发（入场/离场/激活等）写入的得分/倍率，结算时并入并清零。
    // 对齐旧引擎 timingBase/timingMult/timingExtra 生命周期；不随 clearScores 清空。

    private final int[] timingBaseCarry = new int[SIDES];
    private final int[] timingMultCarry = new int[SIDES];
    private final int[] timingExtraCarry = new int[SIDES];

    public void addTimingBase(int side, int amount) { checkSide(side); timingBaseCarry[side] += amount; }
    public void addTimingMult(int side, int amount) { checkSide(side); timingMultCarry[side] += amount; }
    public void addTimingExtra(int side, int amount) { checkSide(side); timingExtraCarry[side] += amount; }
    public int timingBase(int side) { checkSide(side); return timingBaseCarry[side]; }
    public int timingMult(int side) { checkSide(side); return timingMultCarry[side]; }
    public int timingExtra(int side) { checkSide(side); return timingExtraCarry[side]; }

    /** 结算器并入时机通道后清零（每轮结算消费一次，下轮从零开始）。 */
    public void clearTimingCarry()
    {
        for (int side = 0; side < SIDES; side++)
        {
            timingBaseCarry[side] = 0;
            timingMultCarry[side] = 0;
            timingExtraCarry[side] = 0;
        }
    }

    // 对齐清单（2026-09-03）：本轮已消耗行动力（观星金焕章）/ 视骰模式（万工轿金焕章）/ 朝代视图（鲁王金焕章）。
    private final int[] actionPointsSpent = new int[SIDES];
    private final int[] parityView = new int[SIDES];        // 0=无 1=视为奇数 2=视为偶数
    private final boolean[] dynastyView = new boolean[SIDES]; // 本回合己方所有卡视为最左侧卡朝代

    public int actionPointsSpent(int side) { checkSide(side); return actionPointsSpent[side]; }
    public void setActionPointsSpent(int side, int value)
    {
        checkSide(side);
        if (value < 0) throw new IllegalArgumentException("消耗行动力不能为负数");
        actionPointsSpent[side] = value;
    }
    public int parityView(int side) { checkSide(side); return parityView[side]; }
    public void setParityView(int side, int mode)
    {
        checkSide(side);
        if (mode < 0 || mode > 2) throw new IllegalArgumentException("无效视骰模式：" + mode);
        parityView[side] = mode;
    }
    public boolean dynastyView(int side) { checkSide(side); return dynastyView[side]; }
    public void setDynastyView(int side, boolean value) { checkSide(side); dynastyView[side] = value; }

    /** 回合开始清除本回合 scoped 的视骰/朝代视图与行动力消耗计数。 */
    public void clearRoundScopedViews()
    {
        for (int side = 0; side < SIDES; side++)
        {
            parityView[side] = 0;
            dynastyView[side] = false;
            actionPointsSpent[side] = 0;
        }
    }

    public void clearScores()
    {
        for (int side = 0; side < SIDES; side++)
        {
            baseScores.set(side, 0);
            multipliers.set(side, 0);
            extraScores.set(side, 0);
        }
    }

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
