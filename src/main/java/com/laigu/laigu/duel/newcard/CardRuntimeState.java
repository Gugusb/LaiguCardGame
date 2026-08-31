package com.laigu.laigu.duel.newcard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 一张场上卡牌的可变战斗状态，与 DuelCard 的静态规则实现分离。 */
public final class CardRuntimeState
{
    private final List<Integer> dice = new ArrayList<>();
    private int activation;
    private boolean faceDown;
    private boolean locked;
    private boolean destroyAtRoundEnd;

    public List<Integer> dice() { return Collections.unmodifiableList(dice); }

    public void addDie(int value)
    {
        if (value < 1 || value > 6) throw new IllegalArgumentException("骰面必须在1到6之间");
        dice.add(value);
    }

    public void clearDice() { dice.clear(); }
    public int activation() { return activation; }
    public void setActivation(int activation)
    {
        if (activation < 0) throw new IllegalArgumentException("激活进度不能为负数");
        this.activation = activation;
    }
    public void incrementActivation() { activation++; }
    public boolean faceDown() { return faceDown; }
    public void setFaceDown(boolean faceDown) { this.faceDown = faceDown; }
    public boolean locked() { return locked; }
    public void setLocked(boolean locked) { this.locked = locked; }
    public boolean destroyAtRoundEnd() { return destroyAtRoundEnd; }
    public void setDestroyAtRoundEnd(boolean destroyAtRoundEnd) { this.destroyAtRoundEnd = destroyAtRoundEnd; }
}
