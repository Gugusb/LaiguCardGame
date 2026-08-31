package com.laigu.laigu.duel;

import java.util.List;

/** 卡牌上的通用触发器：触发条件与效果列表分离，效果执行后可生成对应动画。 */
public record EffectTriggerDefinition(
        EffectTrigger trigger,
        int threshold,
        List<EffectDefinition> effects,
        int animationKind)
{
    public EffectTriggerDefinition
    {
        if (trigger == null) throw new IllegalArgumentException("触发器不能为空");
        effects = effects == null ? List.of() : List.copyOf(effects);
        if (threshold < 0) throw new IllegalArgumentException("触发阈值不能为负数");
    }

    public static EffectTriggerDefinition activation(int threshold, EffectDefinition... effects)
    {
        return new EffectTriggerDefinition(EffectTrigger.ACTIVATION, threshold, List.of(effects), 1);
    }

    public static EffectTriggerDefinition ambushSuccess(EffectDefinition... effects)
    {
        return new EffectTriggerDefinition(EffectTrigger.AMBUSH_SUCCESS, 0, List.of(effects), 2);
    }

    public static EffectTriggerDefinition ambushFail(EffectDefinition... effects)
    {
        return new EffectTriggerDefinition(EffectTrigger.AMBUSH_FAIL, 0, List.of(effects), 2);
    }

    public static EffectTriggerDefinition poZhen(EffectDefinition... effects)
    {
        return new EffectTriggerDefinition(EffectTrigger.PO_ZHEN, 0, List.of(effects), 3);
    }

    public static EffectTriggerDefinition goldActivation(EffectDefinition... effects)
    {
        return new EffectTriggerDefinition(EffectTrigger.GOLD_ACTIVATION, 0, List.of(effects), 1);
    }

}
