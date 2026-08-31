package com.laigu.laigu.duel;

import net.minecraft.ChatFormatting;

/** 对战四职业（战斗职责，与文物类型无关）。 */
public enum CardClass
{
    GONG("攻·炽", ChatFormatting.RED),
    SHOU("守·衡", ChatFormatting.AQUA),
    MOU("谋·策", ChatFormatting.LIGHT_PURPLE),
    DING("鼎·盛", ChatFormatting.GOLD);

    public final String displayName;
    public final ChatFormatting color;

    CardClass(String displayName, ChatFormatting color)
    {
        this.displayName = displayName;
        this.color = color;
    }
}
