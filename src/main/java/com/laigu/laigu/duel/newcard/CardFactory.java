package com.laigu.laigu.duel.newcard;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/** 新架构卡牌工厂；每个注册项对应一个独立的普通版或金卡类。 */
public final class CardFactory
{
    private static final Map<String, Supplier<DuelCard>> REGISTRY = new HashMap<>();

    private CardFactory()
    {
    }

    public static void register(String id, Supplier<DuelCard> constructor)
    {
        Objects.requireNonNull(id);
        Objects.requireNonNull(constructor);
        DuelCard sample = Objects.requireNonNull(constructor.get(), "卡牌构造器返回 null：" + id);
        if (!id.equals(sample.id())) throw new IllegalArgumentException("注册 ID 与卡牌 ID 不一致：" + id);
        if (sample.displayName() == null || sample.displayName().isBlank())
            throw new IllegalArgumentException("卡牌实际名称不能为空：" + id);
        if (REGISTRY.putIfAbsent(id, constructor) != null)
            throw new IllegalArgumentException("重复注册卡牌：" + id);
    }

    public static DuelCard create(String id)
    {
        Supplier<DuelCard> constructor = REGISTRY.get(id);
        if (constructor == null) throw new IllegalArgumentException("未注册卡牌：" + id);
        return constructor.get();
    }

    public static boolean contains(String id)
    {
        return REGISTRY.containsKey(id);
    }

    public static java.util.Set<String> registeredIds()
    {
        return java.util.Set.copyOf(REGISTRY.keySet());
    }

    /** 校验普通/金质版本由不同实现类承载。 */
    public static void validateIndependentVariants(String artifactId)
    {
        DuelCard common = create(artifactId + "_common");
        DuelCard gold = create(artifactId + "_gold");
        if (common.getClass() == gold.getClass())
            throw new IllegalStateException("普通与金质版必须是两个独立类：" + artifactId);
        if (common.rarity() != CardRarity.COMMON || gold.rarity() != CardRarity.GOLD)
            throw new IllegalStateException("普通/金质稀有度声明错误：" + artifactId);
    }
}
