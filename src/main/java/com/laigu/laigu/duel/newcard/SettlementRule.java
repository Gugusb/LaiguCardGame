package com.laigu.laigu.duel.newcard;

/** 独立结算规则，卡牌效果只通过该接口修改结算上下文。 */
@FunctionalInterface
public interface SettlementRule
{
    void apply(SettlementContext context);
}
