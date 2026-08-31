package com.laigu.laigu.client;

import com.laigu.laigu.duel.DuelActions;
import com.laigu.laigu.network.DuelActionC2SPacket;
import com.laigu.laigu.network.ModPackets;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * 战斗设置界面（主机专属，由第一个提交卡组的玩家打开）。
 * 可开关「黑暗对决」，可补一位 AI 对手，点「开始对战」开打。
 * <p>
 * 界面状态由服务端回包刷新（{@code ui=3}）：第二侧（AI 或真人玩家）就绪后，
 * 「开始对战」才点亮；AI 加入后按钮变为不可用。点击后立即释放按钮焦点，
 * 避免按钮保持「按下/高亮」的卡住观感。
 */
@OnlyIn(Dist.CLIENT)
public class DuelHostSetupScreen extends Screen
{
    private static final int PANEL_W = 340;
    private static final int PANEL_H = 176;

    private final BlockPos pos;
    private boolean darkMode = false;
    private boolean aiReady = false;      // 服务端确认 AI 已加入
    private boolean humanJoined = false;  // 服务端确认第二位真人玩家已登记
    private Button aiButton;
    private Button startButton;

    public DuelHostSetupScreen(BlockPos pos)
    {
        super(Component.literal("战斗设置"));
        this.pos = pos;
    }

    @Override
    public boolean isPauseScreen()
    {
        return false;
    }

    @Override
    protected void init()
    {
        int cx = (width - PANEL_W) / 2;
        int cy = (height - PANEL_H) / 2;
        int w = 240, h = 20;
        addRenderableWidget(Button.builder(darkLabel(), b ->
        {
            darkMode = !darkMode;
            b.setMessage(darkLabel());
        }).bounds(cx + 50, cy + 30, w, h).build());
        aiButton = Button.builder(Component.literal("添加 AI 对手"), b ->
        {
            aiReady = true; // 乐观更新，服务端回包会再次确认
            refreshButtons();
            ModPackets.CHANNEL.sendToServer(new DuelActionC2SPacket(pos, DuelActions.ADD_AI, 0, 0));
        }).bounds(cx + 50, cy + 56, w, h).build();
        startButton = Button.builder(Component.literal("开始对战"), b ->
                ModPackets.CHANNEL.sendToServer(new DuelActionC2SPacket(
                        pos, DuelActions.HOST_SETTINGS, darkMode ? 1 : 0, 0)))
                .bounds(cx + 50, cy + 82, w, h).build();
        addRenderableWidget(aiButton);
        addRenderableWidget(startButton);
        refreshButtons();
    }

    /** 服务端状态回包（ui=3）：AI / 真人玩家是否已就绪。 */
    public void refreshStatus(boolean ai, boolean human)
    {
        aiReady = ai;
        humanJoined = human;
        refreshButtons();
    }

    private void refreshButtons()
    {
        if (aiButton != null)
        {
            aiButton.active = !aiReady && !humanJoined;
            aiButton.setMessage(Component.literal(aiReady ? "AI·来古 已加入" : "添加 AI 对手"));
        }
        if (startButton != null)
        {
            startButton.active = aiReady || humanJoined;
        }
    }

    private Component darkLabel()
    {
        return Component.literal("黑暗对决：" + (darkMode ? "开启（抢夺败者卡）" : "关闭"));
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button)
    {
        boolean r = super.mouseClicked(mx, my, button);
        // 点击后立即释放按钮焦点，避免按钮持续显示「按下」高亮
        this.setFocused(null);
        return r;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt)
    {
        renderBackground(g);
        int cx = (width - PANEL_W) / 2;
        int cy = (height - PANEL_H) / 2;
        g.fill(cx, cy, cx + PANEL_W, cy + PANEL_H, 0xDD101018);
        g.fill(cx + 1, cy + 1, cx + PANEL_W - 1, cy + PANEL_H - 1, 0xDD1A1A26);
        g.drawString(font, "战斗设置（你是主机）", cx + 20, cy + 10, 0xFFFFFFFF);
        String status;
        if (aiReady && humanJoined) status = "AI·来古 与玩家已就绪，可以开始";
        else if (aiReady) status = "AI·来古 已就绪，可以开始";
        else if (humanJoined) status = "第二位玩家已就绪，可以开始";
        else status = "尚未就绪：添加 AI 或等待第二位玩家";
        g.drawString(font, status, cx + 20, cy + 112, aiReady || humanJoined ? 0xFF55FF55 : 0xFFAAAAAA);
        g.drawString(font, "黑暗对决：对局结束后，胜者随机夺得败者卡组一张卡，署名改为胜者", cx + 20, cy + 126, 0xFF888888);
        g.drawString(font, "按 E 返回", cx + 20, cy + 140, 0xFF666666);
        super.render(g, mx, my, pt);
    }
}
