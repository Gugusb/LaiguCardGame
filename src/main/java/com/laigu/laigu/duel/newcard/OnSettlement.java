package com.laigu.laigu.duel.newcard;

/** 回合结算触发词条；在回合结束之后执行计分。 */
public interface OnSettlement
{
    void onSettlement(SettlementContext context);
}
