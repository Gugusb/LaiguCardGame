package com.laigu.laigu.duel.newcard;

/** 激活触发词条：激活进度达到阈值时执行。 */
public interface OnActivation
{
    /** 激活所需进度阈值；达到该值时触发 onActivation。 */
    int activationThreshold();

    void onActivation(CardContext context);
}
