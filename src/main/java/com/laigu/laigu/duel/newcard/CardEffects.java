package com.laigu.laigu.duel.newcard;

/** 可复用的词条机械实现；卡牌身份仍由每个独立卡牌类承载。 */
public final class CardEffects
{
    private CardEffects() {}

    public static CardEffect event(BattleEvent.Type type, java.util.function.Consumer<CardContext> action)
    {
        return new CardEffect()
        {
            @Override public void onEvent(BattleEvent event, CardContext context)
            {
                if (event.type() == type) action.accept(context);
            }
        };
    }
}
