package com.laigu.laigu.client;

import com.laigu.laigu.Laigu;
import com.laigu.laigu.card.CardInfo;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 端详名牌（HUD 屏幕文字）。
 * <p>
 * 端详抬起到位后卡牌停在屏幕中央，在「卡面中间偏左、贴近左边框」位置
 * 纵向排版（每字一行）显示该卡文物名；原版像素字体放大 2×（=大一号）。
 * 出现渐变 0.8 秒、消失渐变 0.4 秒（alpha 由 {@link #alpha} 跨帧驱动，墙钟计时）。
 * 文字为屏幕固定（不随卡轻摆），且只在保持阶段显示。
 */
@Mod.EventBusSubscriber(modid = Laigu.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class InspectNameHud
{
    /** 名牌中心距屏幕中央的横向偏移（屏幕宽倍数，向左移；卡面左缘约 0.13× 屏宽，取 0.12 贴近左边框） */
    private static final float REL_X = 0.12f;
    /** 名牌中心距屏幕中央的纵向偏移（屏幕高倍数，正=下移；用户反馈往下挪一点） */
    private static final float REL_Y = 0.02f;
    /** 字号放大倍数：原版像素字体 9px → 18px（大一号，整数倍保持像素锐利） */
    private static final float SCALE = 2.0f;
    /** 出现渐变时长（秒） */
    private static final float FADE_IN_SECONDS = 0.8f;
    /** 消失渐变时长（秒） */
    private static final float FADE_OUT_SECONDS = 0.4f;
    /** 单帧渐变的最大帧间隔（秒）：封顶后即使出现卡顿帧，alpha 单帧最多走
     *  {@code MAX_DT / FADE_OUT_SECONDS}，不会从 0 一步跳到不透明（「淡出后闪一下」的诱因之一）。 */
    private static final float MAX_DT = 0.15f;

    /** 当前透明度（0~1，跨帧持续，驱动渐变） */
    private static float alpha = 0f;
    /** 最近一次端详保持中的卡（淡出期间卡已取回，用它取名，避免空栈画出 "Air"） */
    private static ItemStack shown = ItemStack.EMPTY;
    /** 上一帧墙钟（纳秒）。注意不能用 {@code Minecraft.getFrameTime()}——它返回的是
     *  {@code Timer.partialTick}（0~1 的刻内插值、每 tick 归零），不是秒，
     *  当帧间隔会让 alpha 一两帧跳满、渐变肉眼不可见。 */
    private static long lastNanos = -1L;
    /** 连续非空帧计数（防抖）：端详保持需连续 2 帧非空才把 alpha 目标抬到 1。
     *  单帧的偶发非空（已观察到 getHoldingInspectStack 偶发空/非空交替）不会把
     *  已淡出的名牌再点亮——从机制上消除「淡出后闪一下」。 */
    private static int holdFrames = 0;

    private InspectNameHud()
    {
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event)
    {
        Minecraft mc = Minecraft.getInstance();
        ItemStack stack = InspectAnimator.getHoldingInspectStack();
        if (!stack.isEmpty())
        {
            shown = stack;
        }

        // 帧间隔用墙钟（见 lastNanos 注释），避免 getFrameTime() 的 partialTick 让渐变瞬间完成；
        // 再封顶 MAX_DT：卡顿帧最多让 alpha 单帧走 MAX_DT/FADE_OUT_SECONDS，不会一步拉满。
        long now = System.nanoTime();
        float dt = lastNanos < 0L ? 0f : (now - lastNanos) / 1_000_000_000.0f;
        lastNanos = now;
        dt = Math.min(dt, MAX_DT);

        // 防抖：连续 2 帧非空才算「保持」，单帧偶发非空不会点亮已淡出的名牌
        if (!stack.isEmpty())
        {
            holdFrames = Math.min(holdFrames + 1, 3);
        }
        else
        {
            holdFrames = 0;
        }
        float target = holdFrames >= 2 ? 1f : 0f;
        if (alpha != target)
        {
            float duration = target > alpha ? FADE_IN_SECONDS : FADE_OUT_SECONDS;
            float step = duration <= 0f ? 1f : dt / duration;
            alpha = target > alpha ? Math.min(alpha + step, 1f) : Math.max(alpha - step, 0f);
        }

        // 「淡出后闪一下」的真因：MC Font.adjustColor() 会把「alpha 字节 <4」的颜色强制
        // 改写为全不透明（字节码：if ((color & 0xFC000000) == 0) color |= 0xFF000000;）。
        // 于是淡出末尾 / 淡入开头 alpha≈0.012（字节 3）的那一两帧会闪成不透明再消失。
        // 修复：低于该阈值即停画（1.6% 以下肉眼已不可见），淡出到 alpha=0 才移除 UI。
        int alphaByte = Math.round(alpha * 255f);
        if (alphaByte < 4)
        {
            if (alpha <= 0f)
            {
                shown = ItemStack.EMPTY; // 淡出真正完成 → 移除 UI
            }
            return;
        }
        if (shown.isEmpty())
        {
            return;
        }

        // 纯文物名：金质卡也显示不带「·金质」后缀的名字（用 common 语言键）
        CardInfo info = CardInfo.of(shown);
        String key = "item.laigu." + info.cardId + "_common";
        String name = I18n.get(key);
        if (name.equals(key))
        {
            name = shown.getHoverName().getString(); // 兜底：语言缺失时用显示名
        }

        int w = event.getGuiGraphics().guiWidth();
        int h = event.getGuiGraphics().guiHeight();
        float px = w / 2f - w * REL_X; // 靠左
        float py = h / 2f + h * REL_Y;
        int color = 0xFFFFFF | (alphaByte << 24);

        char[] chars = name.toCharArray();
        int lineH = mc.font.lineHeight;
        PoseStack pose = event.getGuiGraphics().pose();
        pose.pushPose();
        pose.translate(px, py, 0);
        pose.scale(SCALE, SCALE, 1);
        // 纵向排版：每字一行，整块垂直居中于锚点
        int startY = -chars.length * lineH / 2;
        for (int i = 0; i < chars.length; i++)
        {
            event.getGuiGraphics().drawCenteredString(mc.font, String.valueOf(chars[i]), 0, startY + i * lineH, color);
        }
        pose.popPose();
    }
}
