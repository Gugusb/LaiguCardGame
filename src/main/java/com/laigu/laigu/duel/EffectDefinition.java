package com.laigu.laigu.duel;

/** 一个可执行的效果定义。数值含义由 EffectType 的 ValueSpec 登记表解释。 */
public record EffectDefinition(EffectType type, int p1, int p2)
{
    public EffectDefinition
    {
        if (type == null) throw new IllegalArgumentException("效果类型不能为空");
    }

    public static EffectDefinition of(EffectType type, int p1)
    {
        return new EffectDefinition(type, p1, 0);
    }
}
