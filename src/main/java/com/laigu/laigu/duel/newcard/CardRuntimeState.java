package com.laigu.laigu.duel.newcard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 一张场上卡牌的可变战斗状态，与 DuelCard 的静态规则实现分离。 */
public final class CardRuntimeState
{
    private final List<Integer> dice = new ArrayList<>();
    private final Map<String, Integer> counters = new HashMap<>();
    private int invalidatedDice;   // 被无效化的前 N 颗骰（伏击·睡莲），activeDice 跳过它们
    private int activation;
    private boolean faceDown;
    private boolean locked;
    private boolean destroyAtRoundEnd;
    private int roundsOnField;
    private int persistentBaseBonus;
    private int persistentMultiplierBonus;
    private int persistentExtraBonus;
    private int temporaryBaseBonus;
    private int temporaryMultiplierBonus;
    private int temporaryExtraBonus;
    private boolean consumed;
    private boolean lastedLastRound;

    public List<Integer> dice() { return Collections.unmodifiableList(dice); }
    public List<Integer> activeDice()
    {
        return Collections.unmodifiableList(dice.subList(Math.min(invalidatedDice, dice.size()), dice.size()));
    }

    /** 无效化前 N 颗骰（伏击·睡莲）；N 超过当前骰数时按全部骰处理。 */
    public void invalidateLeadingDice(int count)
    {
        if (count < 0) throw new IllegalArgumentException("无效化数量不能为负数");
        invalidatedDice = count;
    }

    /** 当前无效化的骰数。 */
    public int invalidatedDice() { return Math.min(invalidatedDice, dice.size()); }

    public void addDie(int value)
    {
        if (value < 1 || value > 6) throw new IllegalArgumentException("骰面必须在1到6之间");
        dice.add(value);
    }

    public void clearDice() { dice.clear(); }

    public void setDice(List<Integer> values)
    {
        dice.clear();
        invalidatedDice = 0;
        for (int value : values) addDie(value);
    }
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

    /** 实例级自定义计数器；不同卡牌实例之间不共享，不能为负，跨回合保存。 */
    public int counter(String name) { return counters.getOrDefault(name, 0); }
    public void addCounter(String name, int amount)
    {
        int updated = counter(name) + amount;
        if (updated < 0) throw new IllegalArgumentException("计数器不能为负数：" + name);
        counters.put(name, updated);
    }
    public void setCounter(String name, int value)
    {
        if (value < 0) throw new IllegalArgumentException("计数器不能为负数：" + name);
        counters.put(name, value);
    }
    public Map<String, Integer> counters() { return Collections.unmodifiableMap(counters); }

    public int roundsOnField() { return roundsOnField; }
    public void incrementRoundsOnField() { roundsOnField++; }

    public int persistentBaseBonus() { return persistentBaseBonus; }
    public void addPersistentBaseBonus(int amount) { persistentBaseBonus += amount; }
    public int persistentMultiplierBonus() { return persistentMultiplierBonus; }
    public void addPersistentMultiplierBonus(int amount) { persistentMultiplierBonus += amount; }
    public int persistentExtraBonus() { return persistentExtraBonus; }
    public void addPersistentExtraBonus(int amount) { persistentExtraBonus += amount; }

    public int temporaryBaseBonus() { return temporaryBaseBonus; }
    public void addTemporaryBaseBonus(int amount) { temporaryBaseBonus += amount; }
    public int temporaryMultiplierBonus() { return temporaryMultiplierBonus; }
    public void addTemporaryMultiplierBonus(int amount) { temporaryMultiplierBonus += amount; }
    public int temporaryExtraBonus() { return temporaryExtraBonus; }
    public void addTemporaryExtraBonus(int amount) { temporaryExtraBonus += amount; }
    public void clearTemporaryBonuses()
    {
        temporaryBaseBonus = 0;
        temporaryMultiplierBonus = 0;
        temporaryExtraBonus = 0;
    }

    public boolean consumed() { return consumed; }
    public void markConsumed() { consumed = true; }

    /** 上轮幸存标记：回合推进时由战斗层维护（对应旧 FieldCard.lastedLastRound）。 */
    public boolean lastedLastRound() { return lastedLastRound; }
    public void setLastedLastRound(boolean lastedLastRound) { this.lastedLastRound = lastedLastRound; }
}
