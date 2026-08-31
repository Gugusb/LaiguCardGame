package com.laigu.laigu.duel.newcard;

import java.util.List;
import java.util.Optional;

/** 卡牌规则可使用的最小战斗能力边界。运行时状态由上下文实现保存。 */
public interface CardContext
{
    DuelCard self();

    void drawCards(int amount);
    void addBaseScore(int amount);
    void addMultiplier(int amount);
    void addExtraScore(int amount);

    CardRuntimeState selfState();
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
    void activate(CardTarget target);

    Optional<CardTarget> opponentTargetWithFewestDice();
    void markDestroyAtRoundEnd(CardTarget target);

    void emit(AnimationEvent event);

    record CardTarget(int side, int slot, int diceCount)
    {
    }
}
