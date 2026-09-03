package com.laigu.laigu.duel.newcard;

import java.util.List;
import java.util.Optional;

/** 卡牌规则可使用的最小战斗能力边界。运行时状态由上下文实现保存。 */
public interface CardContext
{
    DuelCard self();

    void drawCards(int amount);
    /** 回复行动力（不超过每轮上限；对应旧 SUMMON_RESTORE_AP）。 */
    void restoreActionPoints(int amount);
    /** 上轮胜者（-1 平局/首轮；对应旧 winnerLast，供结算后条件触发读取）。 */
    int winnerLast();
    /** 本卡所属对战方（0/1）。 */
    int side();
    /** 本卡所在槽位（0-4）。 */
    int slot();
    void addBaseScore(int amount);
    void addMultiplier(int amount);
    void addExtraScore(int amount);

    CardRuntimeState selfState();
    /** 同槽位对手卡的运行时状态；对手槽位无卡时为空（对应旧伏击的对位卡牌）。 */
    Optional<CardRuntimeState> oppositeState();
    /** 同槽位对手卡的骰面列表（含已无效化段；对手无卡时为空表）。 */
    List<Integer> oppositeDice();
    /** 实例级计数器视图；属于当前卡牌实例，不与其他实例共享。 */
    Counter counter(String name);

    /** 卡牌实例计数器的最小操作边界。 */
    interface Counter
    {
        int value();
        void add(int amount);
        void set(int value);
    }

    List<Integer> selfDice();
    void addSelfDie(int value);
    int selfActivation();
    void incrementSelfActivation();
    boolean selfFaceDown();
    void setSelfFaceDown(boolean faceDown);
    boolean selfLocked();
    void setSelfLocked(boolean locked);
    void markSelfDestroyAtRoundEnd();
    Optional<CardTarget> leftCard();
    Optional<CardTarget> rightCard();
    void activate(CardTarget target);
    void activate(CardTarget target, int times);

    Optional<CardTarget> opponentTargetWithFewestDice();
    void markDestroyAtRoundEnd(CardTarget target);
    /** 己方场上金质卡数量（含本卡；对应旧 GOLD_CNT_MULT 的 countGold）。 */
    int ownGoldCardCount();

    void emit(AnimationEvent event);

    // ================= 抢骰（阶段16；默认空实现，生产上下文接入 BattleState） =================

    /** 掷一颗骰（1-6；由战斗随机源驱动）。 */
    default int rollDie() { return 1; }
    /** 向共享骰池加入一颗指定点数的骰（1-6）。 */
    default void addSharedPoolDie(int face) { }
    /** 重骰共享池中所有 > face 的骰，返回重骰颗数。 */
    default int rerollSharedPoolAbove(int face) { return 0; }
    /** 本轮重骰已用次数。 */
    default int rerollUses() { return 0; }
    /** 记一次重骰使用。 */
    default void useReroll() { }
    /** Q7 拍板：本方获得一次性抓骰次数加成（仅当前回合计划构建生效）。 */
    default void addDraftTurnBonusSelf(int amount) { }

    // ================= 阶段18：局内时机通道与激活目标 =================

    /** 局内时机通道：事件触发的得分/倍率写入（结算时并入并清零；对齐旧 timingBase/timingMult/timingExtra）。 */
    default void addTimingBase(int amount) { }
    /** 局内时机通道：事件触发的倍率写入。 */
    default void addTimingMult(int amount) { }
    /** 局内时机通道：事件触发的额外分写入。 */
    default void addTimingExtra(int amount) { }

    /** 己方场上可激活的卡位（实现激活接口且阈值>0；对应旧 activateCardDirect 的 activateCap>0 过滤）。 */
    default List<CardTarget> ownActivatableTargets() { return List.of(); }
    /** 己方全部有卡场位（含不可激活卡；溪山金焕章「激活所有卡牌」用）。 */
    default List<CardTarget> ownCardTargets() { return List.of(); }
    /** 尝试激活己方全部场位卡各 times 次；无可激活词条的卡=尝试失败（不累计进度）。返回失败次数（卡数×times）。 */
    default int attemptActivateOwnCards(int times) { return 0; }

    // ================= 对齐清单（2026-09-03）新增默认能力 =================

    /** 万工轿金焕章：本回合己方所有骰视为奇数/偶数（随当轮基础模式）。 */
    default void setParityViewOdd() { }
    default void setParityViewEven() { }
    default boolean parityViewActive() { return false; }
    default boolean parityViewOdd() { return true; }
    /** 鲁王金焕章：本回合己方所有卡视为最左侧卡的朝代。 */
    default void setDynastyViewToLeftmost() { }
    /** 万工轿「每回合切换」：奇数轮看奇数骰、偶数轮看偶数骰。 */
    default boolean oddDiceRound() { return true; }
    /** 当前手牌数与手牌上限（抽牌超限类焕章判定）。 */
    default int handSize() { return 0; }
    default int maxHandSize() { return 8; }
    /** 对方场上未放有骰子的卡牌数（永固杯 x）。 */
    default int oppositeCardsWithoutDice() { return 0; }
    /** 我方当前基础分（描金壶金焕章「发动时当前基础分」；不一定是最终值）。 */
    default int ownBaseScore() { return 0; }
    /** 本卡永久基础分加成（藏锋金焕章；跨回合保存）。 */
    default void addPersistentBaseBonus(int amount) { }
    default int persistentBaseBonus() { return 0; }

    record CardTarget(int side, int slot, int diceCount)
    {
    }
}
