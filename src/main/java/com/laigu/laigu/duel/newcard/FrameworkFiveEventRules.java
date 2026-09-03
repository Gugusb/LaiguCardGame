package com.laigu.laigu.duel.newcard;

/** 框架5事件型规则目录（阶段14已清空，仅保留回滚兜底；行为已内联到各卡类 onEvent）。 */
public final class FrameworkFiveEventRules
{
    private FrameworkFiveEventRules() {}

    public static CardEventRuleRegistry defaultRegistry()
    {
        return new CardEventRuleRegistry();
    }
}
