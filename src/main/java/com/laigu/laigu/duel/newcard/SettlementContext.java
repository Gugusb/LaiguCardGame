package com.laigu.laigu.duel.newcard;

import java.util.List;

/** 回合结算时提供给独立卡牌规则的统一只读/计分上下文。 */
public final class SettlementContext
{
    private final BattleState state;
    private final int side;
    private final int slot;
    private double contributionKeep = 1.0;   // 破阵削弱后本卡贡献保留比例（1.0 未被削）

    public SettlementContext(BattleState state, int side)
    {
        this(state, side, -1);
    }

    public SettlementContext(BattleState state, int side, int slot)
    {
        this.state = state;
        this.side = side;
        this.slot = slot;
    }

    public int side() { return side; }
    public int slot() { return slot; }
    /** 当前卡牌实例的可变状态；用于读取实例计数器与持久加成。 */
    public CardRuntimeState selfState() { return state.cardStateAt(side, slot); }
    /** 当前结算的卡牌本体。 */
    public DuelCard self() { return state.cardAt(side, slot).orElseThrow(); }
    public List<Integer> activeDice() { return state.allActiveDice(side); }
    /** 对手全场骰总数（旧引擎 g.fieldDice(1-side) 语义）。 */
    public int opponentDiceCount() { return state.allActiveDice(1 - side).size(); }
    /** 对手场上卡牌数。 */
    public long opponentFieldCount() { return state.field(1 - side).stream().filter(java.util.Objects::nonNull).count(); }
    /** 本卡文物 id（已剥稀有度后缀），用于查询朝代等元数据。 */
    public String selfCardId()
    {
        return state.cardAt(side, slot).map(card -> card.id().replaceAll("_(common|gold)$", "")).orElse("");
    }
    /** 上轮幸存标记：本轮开始时仍在线的卡为 true。 */
    public boolean lastedLastRound() { return selfState().lastedLastRound(); }
    /** 本卡朝代（来自文物元数据）。 */
    public String selfDynasty() { return com.laigu.laigu.card.CardInfo.dynastyOf(selfCardId()); }
    /** 指定场位卡牌的朝代；空位返回 null。鲁王金焕章：该侧朝代视图激活时统一视为最左侧卡的朝代。 */
    public String dynastyAt(int side, int slot)
    {
        if (state.dynastyView(side))
        {
            for (int s = 0; s < BattleState.SLOTS; s++)
            {
                String leftmost = rawDynastyAt(side, s);
                if (leftmost != null) return leftmost;
            }
            return null;
        }
        return rawDynastyAt(side, slot);
    }

    private String rawDynastyAt(int side, int slot)
    {
        return state.cardAt(side, slot)
                .map(card -> com.laigu.laigu.card.CardInfo.dynastyOf(card.id().replaceAll("_(common|gold)$", "")))
                .orElse(null);
    }

    // ================= 对齐清单（2026-09-03）辅助方法 =================

    /** 对方场上未放有骰子的卡牌数（永固杯 x）。 */
    public int oppositeCardsWithoutDice()
    {
        int count = 0;
        for (int s = 0; s < BattleState.SLOTS; s++)
        {
            CardRuntimeState runtime = state.cardStateAt(1 - side, s);
            if (state.cardAt(1 - side, s).isPresent() && runtime.dice().isEmpty()) count++;
        }
        return count;
    }

    /** 对位是否为对方场上有效骰数最少的卡牌之一（飞天金焕章）。 */
    public boolean oppositeIsFewestOpponentDice()
    {
        if (slot < 0 || state.cardAt(1 - side, slot).isEmpty()) return false;
        int mine = state.cardStateAt(1 - side, slot).activeDice().size();
        int min = Integer.MAX_VALUE;
        for (int s = 0; s < BattleState.SLOTS; s++)
        {
            if (state.cardAt(1 - side, s).isEmpty()) continue;
            min = Math.min(min, state.cardStateAt(1 - side, s).activeDice().size());
        }
        return mine <= min;
    }

    /** 本回合已消耗行动力（观星金焕章）。 */
    public int actionPointsSpentThisRound() { return state.actionPointsSpent(side); }

    /** 万工轿金焕章视骰是否生效。 */
    public boolean parityViewActive() { return state.parityView(side) != 0; }

    /** 视骰模式下全部骰视为奇数（true）或偶数（false）。 */
    public boolean parityViewOdd() { return state.parityView(side) == 1; }

    /** 共享骰池剩余点数之和（天球仪金焕章）。 */
    public int sharedPoolPointSum() { return state.sharedPool().stream().mapToInt(Integer::intValue).sum(); }

    /** 破阵成功判定：本卡骰面和 > 对位骰面和（越剑恒定成功时按持久标记）。 */
    public boolean poZhenSuccess()
    {
        return oppositeDiceSum() < selfDice().stream().mapToInt(Integer::intValue).sum();
    }

    /** 封锁对位卡牌（牛尊金焕章：不可更换不可焕章）。 */
    public void markOppositeLocked()
    {
        if (slot >= 0 && state.cardAt(1 - side, slot).isPresent())
            state.cardStateAt(1 - side, slot).setLocked(true);
    }

    /** 回合结算后破坏对位卡牌（鸟尊金焕章）。 */
    public void markOppositeDestroyAtRoundEnd()
    {
        if (slot >= 0 && state.cardAt(1 - side, slot).isPresent())
            state.cardStateAt(1 - side, slot).setDestroyAtRoundEnd(true);
    }

    /** 指定场位卡牌的文物 id；空位返回 null。 */
    public String cardIdAt(int side, int slot)
    {
        return state.cardAt(side, slot).map(DuelCard::id).orElse(null);
    }

    /** 指定场位卡牌的职业；空位返回 null。 */
    public com.laigu.laigu.duel.CardClass cardClassAt(int side, int slot)
    {
        return state.cardAt(side, slot).map(DuelCard::cardClass).orElse(null);
    }

    /** 对位（对手同槽位）的运行时状态；空位返回 null。 */
    public CardRuntimeState oppositeState()
    {
        return slot < 0 ? null : state.cardStateAt(1 - side, slot);
    }

    /** 对位卡牌的骰面列表；空位返回空列表。 */
    public List<Integer> oppositeDice()
    {
        return slot < 0 ? List.of() : state.cardStateAt(1 - side, slot).activeDice();
    }

    /** 对位骰面和；空位为 0（对应旧 sumDice 空卡语义）。 */
    public int oppositeDiceSum()
    {
        return oppositeDice().stream().mapToInt(Integer::intValue).sum();
    }

    /** 对手全部场上卡牌（含空位 null），按槽位顺序。 */
    public List<DuelCard> opponentField()
    {
        return state.field(1 - side);
    }

    /** 己方全部场上卡牌（含空位 null），按槽位顺序。 */
    public List<DuelCard> ownField()
    {
        return state.field(side);
    }

    /** 己方场上非空卡牌中金质数量。 */
    public long ownGoldCount()
    {
        return ownField().stream()
                .filter(java.util.Objects::nonNull)
                .filter(card -> card.rarity() == CardRarity.GOLD)
                .count();
    }

    /** 己方场上非空卡牌中指定职业的数量（旧引擎 countClass 语义，含自身）。 */
    public int ownClassCount(com.laigu.laigu.duel.CardClass cardClass)
    {
        return (int) ownField().stream().filter(java.util.Objects::nonNull)
                .filter(card -> card.cardClass() == cardClass).count();
    }

    /** 己方场上非空卡牌中指定朝代的数量（旧引擎 countDynasty 语义，含自身）。 */
    public int ownDynastyCount(String dynasty)
    {
        if (dynasty == null) return 0;
        int count = 0;
        for (int s = 0; s < BattleState.SLOTS; s++)
            if (dynasty.equals(dynastyAt(side, s))) count++;
        return count;
    }

    /** 己方场上非空卡牌总数。 */
    public long ownFieldCount()
    {
        return ownField().stream().filter(java.util.Objects::nonNull).count();
    }

    /** 未布置骰数量（每方私有骰池）。 */
    public int poolSize() { return state.poolSize(side); }

    /** 己方全场有效骰面（旧引擎 fieldDice(side) 语义，供牌型判定）。 */
    public List<Integer> ownFieldDice()
    {
        List<Integer> dice = new java.util.ArrayList<>();
        for (int s = 0; s < BattleState.SLOTS; s++) dice.addAll(state.cardStateAt(side, s).activeDice());
        return dice;
    }

    /** 右侧相邻场位是否有卡。 */
    public boolean hasRightCard()
    {
        return slot >= 0 && slot + 1 < BattleState.SLOTS && state.cardAt(side, slot + 1).isPresent();
    }

    /**
     * 激活右侧相邻卡牌 n 次：每次进度 +1 后立即派发激活事件，
     * 由被激活卡自己的激活词条结算奖励并清零（对齐旧引擎逐次激活语义）。
     */
    public void activateRightCard(int times)
    {
        if (times < 0) throw new IllegalArgumentException("激活次数不能为负数");
        if (slot < 0 || !hasRightCard()) return;
        CardRuntimeState target = state.cardStateAt(side, slot + 1);
        for (int i = 0; i < times; i++)
        {
            target.incrementActivation();
            state.emit(new AnimationEvent(AnimationEvent.Type.CARD_ACTIVATE, side, slot + 1, self().id()));
            state.dispatchActivation(side, slot + 1);
        }
    }

    /** 左侧相邻场位是否有卡。 */
    public boolean hasLeftCard()
    {
        return slot >= 1 && state.cardAt(side, slot - 1).isPresent();
    }

    /** 激活左侧相邻卡牌 n 次（语义同 activateRightCard；溪山类「激活左」词条）。 */
    public void activateLeftCard(int times)
    {
        if (times < 0) throw new IllegalArgumentException("激活次数不能为负数");
        if (slot < 0 || !hasLeftCard()) return;
        CardRuntimeState target = state.cardStateAt(side, slot - 1);
        for (int i = 0; i < times; i++)
        {
            target.incrementActivation();
            state.emit(new AnimationEvent(AnimationEvent.Type.CARD_ACTIVATE, side, slot - 1, self().id()));
            state.dispatchActivation(side, slot - 1);
        }
    }

    /** 激活己方全部场位卡各 n 次；无可激活词条的卡=尝试失败，返回失败次数（海错金焕章计量）。 */
    public int activateAllOwnCards(int times)
    {
        if (times < 0) throw new IllegalArgumentException("激活次数不能为负数");
        int failed = 0;
        for (int s = 0; s < BattleState.SLOTS; s++)
        {
            DuelCard card = state.cardAt(side, s).orElse(null);
            if (card == null) continue;
            if (card instanceof OnActivation handler && handler.activationThreshold() > 0)
            {
                CardRuntimeState target = state.cardStateAt(side, s);
                for (int i = 0; i < times; i++)
                {
                    target.incrementActivation();
                    state.emit(new AnimationEvent(AnimationEvent.Type.CARD_ACTIVATE, side, s, self().id()));
                    state.dispatchActivation(side, s);
                }
            }
            else failed += times;
        }
        return failed;
    }

    /** 己方场上单一朝代的最大卡牌数（旧引擎 maxDynastyOnField 语义）。 */
    public int ownMaxDynastyCount()
    {
        java.util.Map<String, Integer> counts = new java.util.HashMap<>();
        for (int s = 0; s < BattleState.SLOTS; s++)
        {
            String dynasty = dynastyAt(side, s);
            if (dynasty == null) continue;
            counts.merge(dynasty, 1, Integer::sum);
        }
        return counts.values().stream().max(Integer::compare).orElse(0);
    }

    /** 牌库剩余张数。 */
    public int deckSize() { return state.deckSize(side); }

    /** 共享骰池。 */
    public List<Integer> sharedPool() { return state.sharedPool(); }

    /** 破阵削弱后本卡贡献保留比例（1.0 未被削，0.5 削半，0.0 全削）；加成应按此缩放。 */
    public double contributionKeep() { return contributionKeep; }

    void setContributionKeep(double keep)
    {
        if (keep < 0.0 || keep > 1.0) throw new IllegalArgumentException("贡献保留比例必须在0到1之间");
        contributionKeep = keep;
    }

    /** 按 keep 缩放并累加到额外分（floor 与旧 ScoreEngine 一致）；未削弱时等价 addExtra。 */
    public void addExtraScaled(int amount)
    {
        addExtra(scale(amount));
    }

    /** 按 keep 缩放并累加到基础分；未削弱时等价 addBase。 */
    public void addBaseScaled(int amount)
    {
        addBase(scale(amount));
    }

    /** 按 keep 缩放并累加到倍率；未削弱时等价 addMultiplier。 */
    public void addMultiplierScaled(int amount)
    {
        addMultiplier(scale(amount));
    }

    private int scale(int amount)
    {
        return contributionKeep >= 1.0 ? amount : (int) Math.floor(amount * contributionKeep);
    }

    /** 己方场上每张金质卡上的骰面（供鎏金类词条）。 */
    public List<Integer> ownGoldCardDice()
    {
        List<Integer> goldDice = new java.util.ArrayList<>();
        for (int slot = 0; slot < BattleState.SLOTS; slot++)
        {
            DuelCard card = state.cardAt(side, slot).orElse(null);
            if (card == null || card.rarity() != CardRarity.GOLD) continue;
            goldDice.addAll(state.cardStateAt(side, slot).activeDice());
        }
        return goldDice;
    }
    public List<Integer> selfDice() { return slot < 0 ? List.of() : state.cardStateAt(side, slot).activeDice(); }
    public int baseScore() { return state.baseScore(side); }
    public int multiplier() { return state.multiplier(side); }
    public int extraScore() { return state.extraScore(side); }
    public int handSize() { return state.handSize(side); }
    public int round() { return state.round(); }
    public int winnerLast() { return state.winnerLast(); }
    public int wins() { return state.wins(side); }
    public int opponentWins() { return state.wins(1 - side); }
    public void addBase(int amount) { state.addBaseScoreForRule(side, amount); }
    public void addMultiplier(int amount)
    {
        state.addMultiplierForRule(side, amount);
        emitPop(new AnimationEvent(AnimationEvent.Type.MULTIPLIER_POPUP, side, slot, selfCardIdFull(), amount));
    }

    /** 阶段17：计分/倍率弹出事件（结算中自动派发，卡类无需逐个 emit）。 */
    private void emitPop(AnimationEvent event) { state.emit(event); }

    /** 本卡注册 id（slot=-1 时为空串）。 */
    private String selfCardIdFull()
    {
        return slot < 0 ? "" : state.cardAt(side, slot).map(DuelCard::id).orElse("");
    }
    public void addExtra(int amount)
    {
        state.addExtraScoreForRule(side, amount);
        emitPop(new AnimationEvent(AnimationEvent.Type.SCORE_POPUP, side, slot, selfCardIdFull(), amount));
    }
    public void emit(AnimationEvent event) { state.emit(event); }
}
