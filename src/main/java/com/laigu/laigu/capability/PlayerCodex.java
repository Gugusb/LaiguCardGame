package com.laigu.laigu.capability;

import com.laigu.laigu.card.CardCatalog;

import java.util.Arrays;

/**
 * 图鉴数据实现：boolean 数组，下标 = {@link CardCatalog#CARD_IDS} 中的位置。
 * <p>
 * 每个玩家一个实例（{@link PlayerCodexProvider} 挂到玩家实体），
 * 只增不删——「首次获得」即永久解锁。
 */
public class PlayerCodex implements IPlayerCodex
{
    private final boolean[] unlocked;

    public PlayerCodex()
    {
        this.unlocked = new boolean[CardCatalog.CARD_IDS.size()];
    }

    @Override
    public boolean unlock(int cardIndex)
    {
        if (cardIndex < 0 || cardIndex >= unlocked.length || unlocked[cardIndex])
        {
            return false;
        }
        unlocked[cardIndex] = true;
        return true;
    }

    @Override
    public boolean has(int cardIndex)
    {
        return cardIndex >= 0 && cardIndex < unlocked.length && unlocked[cardIndex];
    }

    @Override
    public int size()
    {
        return unlocked.length;
    }

    @Override
    public int[] unlocked()
    {
        int count = 0;
        for (boolean b : unlocked)
        {
            if (b)
            {
                count++;
            }
        }
        int[] out = new int[count];
        int i = 0;
        for (int idx = 0; idx < unlocked.length; idx++)
        {
            if (unlocked[idx])
            {
                out[i++] = idx;
            }
        }
        return out;
    }

    /** 从序列化数据恢复（deserializeNBT 时调用）。 */
    public void loadFrom(int[] indices)
    {
        Arrays.fill(unlocked, false);
        for (int idx : indices)
        {
            if (idx >= 0 && idx < unlocked.length)
            {
                unlocked[idx] = true;
            }
        }
    }
}
