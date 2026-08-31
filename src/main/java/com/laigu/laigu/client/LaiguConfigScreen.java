package com.laigu.laigu.client;

import com.laigu.laigu.config.LaiguConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * 模组设置界面：积分动画速度倍率（Mods → laigu → Config 进入）。
 * 只影响计分动画，部署/选中动画不受影响；改动即时生效。
 */
@OnlyIn(Dist.CLIENT)
public class LaiguConfigScreen extends Screen
{
    private static final double MIN = 0.25, MAX = 4.0;
    private final Screen parent;

    public LaiguConfigScreen(Screen parent)
    {
        super(Component.literal("来古牌 · 对战设置"));
        this.parent = parent;
    }

    @Override
    protected void init()
    {
        int w = 260, h = 20;
        int cx = this.width / 2;
        double cur = LaiguConfig.SCORE_ANIM_SPEED.get();
        double rel = (cur - MIN) / (MAX - MIN);
        AbstractSliderButton slider = new AbstractSliderButton(cx - w / 2, 70, w, h, Component.literal(""), rel)
        {
            @Override
            protected void updateMessage()
            {
                setMessage(Component.literal("积分动画速度倍率：" + String.format("%.2f", MIN + value * (MAX - MIN)) + "（越大越快）"));
            }

            @Override
            protected void applyValue()
            {
                double v = MIN + value * (MAX - MIN);
                LaiguConfig.SCORE_ANIM_SPEED.set(v);
                LaiguConfig.SCORE_ANIM_SPEED.save();
            }
        };
        slider.setMessage(Component.literal("积分动画速度倍率：" + String.format("%.2f", cur) + "（越大越快）"));
        addRenderableWidget(slider);
        addRenderableWidget(Button.builder(Component.literal("完成"), b -> onClose())
                .bounds(cx - 60, 110, 120, h).build());
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt)
    {
        renderBackground(g);
        g.drawCenteredString(font, "来古牌 · 对战设置", this.width / 2, 30, 0xFFFFFFFF);
        g.drawCenteredString(font, "只影响积分动画速度，不影响部署/选中动画", this.width / 2, 48, 0xFFCCCCCC);
        super.render(g, mx, my, pt);
    }

    @Override
    public void onClose()
    {
        minecraft.setScreen(parent);
    }
}
