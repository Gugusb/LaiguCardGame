package com.laigu.laigu.duel.newcard;

import com.laigu.laigu.duel.CardClass;

/** 新卡牌架构中的单张运行时卡牌。普通版和金卡版必须是不同实现类。 */
public interface DuelCard
{
    String id();
    /** 实际文物名称；禁止使用“炎刃”“千钧”等抽象效果名。 */
    String displayName();
    CardClass cardClass();
    default CardRarity rarity()
    {
        return id().endsWith("_gold") ? CardRarity.GOLD : CardRarity.COMMON;
    }

    /** 本卡全部词条；可同时拥有入场、离场、激活、伏击、结算等多个效果。 */
    default java.util.List<CardEffect> effects() { return java.util.List.of(); }

    /** 卡面主描述（新系统表达；tooltip 主行与对局内卡面共用）。 */
    default String description() { return CardDescriptionBook.description(id()); }

    /** 金卡焕章行描述；无焕章加成时为「焕章：无」。普通卡不展示此行。 */
    default String goldDescription() { return CardDescriptionBook.goldDescription(id()); }

    default void onEvent(BattleEvent event, CardContext context)
    {
        for (CardEffect effect : effects()) effect.onEvent(event, context);
    }

    /** 回合计分阶段执行本卡全部结算词条。 */
    default void onSettlement(SettlementContext context)
    {
        for (CardEffect effect : effects()) effect.onSettlement(context);
    }
}
