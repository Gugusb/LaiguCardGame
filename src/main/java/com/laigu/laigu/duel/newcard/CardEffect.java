package com.laigu.laigu.duel.newcard;

/**
 * 新版卡牌的一个独立词条。单张独立卡牌可以同时声明任意数量的词条，
 * 每个词条分别响应入场、离场、激活、回合时机或结算。
 */
public interface CardEffect
{
    default void onEvent(BattleEvent event, CardContext context) {}
    default void onSettlement(SettlementContext context) {}
}
