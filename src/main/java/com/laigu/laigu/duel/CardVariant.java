package com.laigu.laigu.duel;

/** 一张卡在指定稀有度下的完整主效果配置。数值是最终值，不再运行时隐式翻倍。 */
public record CardVariant(
        EffectType effect,
        int p1,
        int p2,
        String targetDynasty,
        CardClass targetClass,
        int charge,
        String description)
{
    public CardVariant
    {
        if (effect == null) throw new IllegalArgumentException("卡牌效果类型不能为空");
    }

    public static CardVariant of(EffectType effect, int p1, int p2, String description)
    {
        return new CardVariant(effect, p1, p2, null, null, 0, description);
    }
}
