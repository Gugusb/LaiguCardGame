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
        if (REGISTRY.putIfAbsent(Objects.requireNonNull(id), Objects.requireNonNull(constructor)) != null)
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
}
