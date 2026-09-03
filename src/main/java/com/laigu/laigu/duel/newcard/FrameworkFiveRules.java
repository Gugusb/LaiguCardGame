package com.laigu.laigu.duel.newcard;

/**
 * 框架5内置规则目录（阶段14已清空）。
 * 历史上承载未迁移卡牌的行为；如今全部 8 个空壳类已内联自身行为
 * （敦煌飞天/海错图/青铜仙鹤/溪山行旅图 ×2），江山/描金壶的重复注册已移除
 * （此前结算注册表规则会与类内 OnActivation/OnSettlement 行为叠加双算）。
 * 本类仅保留空注册表作为回滚兜底。
 */
public final class FrameworkFiveRules
{
    private FrameworkFiveRules() {}

    public static SettlementRuleRegistry defaultRegistry()
    {
        return new SettlementRuleRegistry();
    }
}
