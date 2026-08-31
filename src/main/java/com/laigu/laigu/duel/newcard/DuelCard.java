package com.laigu.laigu.duel.newcard;

import com.laigu.laigu.duel.CardClass;

/** 新卡牌架构中的单张运行时卡牌。普通版和金卡版必须是不同实现类。 */
public interface DuelCard
{
    String id();
    String displayName();
    CardClass cardClass();

    default void onEvent(BattleEvent event, CardContext context)
    {
    }
}
