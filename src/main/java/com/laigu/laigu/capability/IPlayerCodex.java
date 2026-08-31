package com.laigu.laigu.capability;

/**
 * 玩家图鉴：记录「首次获得」的卡牌。
 * <p>
 * 以卡牌 id 在 {@link com.laigu.laigu.card.CardCatalog#CARD_IDS} 中的下标为索引（数组存储）。
 * 服务端写入；数据持久化在玩家 NBT（见 {@link PlayerCodexProvider}），绑定玩家个人。
 */
public interface IPlayerCodex
{
    /**
     * 尝试解锁某张卡牌（按目录下标）。
     * 已解锁返回 false；本次首次解锁返回 true（可用于提示）。
     */
    boolean unlock(int cardIndex);

    /** 是否已解锁某张卡牌。 */
    boolean has(int cardIndex);

    /** 卡牌总数（数组长度）。 */
    int size();

    /** 已解锁的卡牌下标数组（升序），用于序列化。 */
    int[] unlocked();
}
