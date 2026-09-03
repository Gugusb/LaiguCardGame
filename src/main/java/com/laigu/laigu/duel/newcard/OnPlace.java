package com.laigu.laigu.duel.newcard;

/** 放置阶段触发词条：在入场词条之前、放置动作本身时执行。 */
public interface OnPlace
{
    void onPlace(CardContext context);
}
