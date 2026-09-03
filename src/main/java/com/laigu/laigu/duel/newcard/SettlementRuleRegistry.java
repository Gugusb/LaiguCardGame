package com.laigu.laigu.duel.newcard;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 框架5：按卡牌 ID 管理回合结算规则。
 * 规则注册与卡牌实例解耦，允许后续把复杂卡牌拆成多个可组合规则。
 */
public final class SettlementRuleRegistry
{
    private final Map<String, SettlementRule> rules = new LinkedHashMap<>();

    public SettlementRuleRegistry register(String cardId, SettlementRule rule)
    {
        Objects.requireNonNull(cardId);
        if (cardId.isBlank()) throw new IllegalArgumentException("卡牌 ID 不能为空");
        if (rules.putIfAbsent(cardId, Objects.requireNonNull(rule)) != null)
            throw new IllegalArgumentException("重复注册结算规则：" + cardId);
        return this;
    }

    public SettlementRule resolve(String cardId)
    {
        return rules.getOrDefault(cardId, context -> { });
    }

    public boolean contains(String cardId) { return rules.containsKey(cardId); }
    public Map<String, SettlementRule> snapshot() { return Map.copyOf(rules); }
}
