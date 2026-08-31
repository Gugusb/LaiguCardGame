package com.laigu.laigu.config;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * 模组客户端配置（mod 设置界面可调）。
 */
public class LaiguConfig
{
    /** 积分动画速度倍率：越大越快，越小越慢（只影响计分动画，纯客户端）。 */
    public static final ForgeConfigSpec.DoubleValue SCORE_ANIM_SPEED;

    public static final ForgeConfigSpec SPEC;

    static
    {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        b.comment("对战界面设置").push("duel");
        SCORE_ANIM_SPEED = b
                .comment("积分动画速度倍率：1.0 默认；调大变快（如 1.5 更快），调小变慢（如 0.5 更慢）。纯客户端设置，各玩家各看各的，只影响计分动画，不影响部署/选中动画。")
                .defineInRange("scoreAnimSpeed", 0.55, 0.25, 4.0);
        b.pop();
        SPEC = b.build();
    }
}
