package com.laigu.laigu.duel.newcard;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** 阶段5事件型效果注册表：把事件规则与卡牌类实现解耦。 */
public final class CardEventRuleRegistry
{
    @FunctionalInterface
    public interface Rule { void apply(BattleEvent event, CardContext context); }

    private final Map<String, Rule> rules = new LinkedHashMap<>();

    public CardEventRuleRegistry register(String cardId, Rule rule)
    {
        if (cardId == null || cardId.isBlank()) throw new IllegalArgumentException("卡牌 ID 不能为空");
        if (rules.putIfAbsent(cardId, Objects.requireNonNull(rule)) != null)
            throw new IllegalArgumentException("重复注册事件规则：" + cardId);
        return this;
    }

    public Rule resolve(String cardId) { return rules.get(cardId); }
    public boolean contains(String cardId) { return rules.containsKey(cardId); }
    public Map<String, Rule> snapshot() { return Map.copyOf(rules); }
}
