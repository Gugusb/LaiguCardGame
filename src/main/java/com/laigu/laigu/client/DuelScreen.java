package com.laigu.laigu.client;

import com.laigu.laigu.Laigu;
import com.laigu.laigu.config.LaiguConfig;
import com.laigu.laigu.duel.CardClass;
import com.laigu.laigu.duel.DuelActions;
import com.laigu.laigu.duel.DuelCardCatalog;
import com.laigu.laigu.duel.DuelCardData;
import com.laigu.laigu.duel.DuelGame;
import com.laigu.laigu.duel.newcard.AnimationEvent;
import com.laigu.laigu.duel.newcard.CardItemAdapter;
import com.laigu.laigu.duel.newcard.DuelCard;
import com.laigu.laigu.network.DuelActionC2SPacket;
import com.laigu.laigu.network.ModPackets;
import com.laigu.laigu.util.CardNbt;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * 对战界面（自绘，非容器菜单）。按分区图重做布局：
 * <pre>
 *   ┌─左列──────────────┬─中列(黄框·大战场)──────┬─右列────────────┐
 *   │ 蓝·战斗播报log     │ 黄顶条                 │ 红·对手骰池      │
 *   │  （可滚动，战场信息）│ 褐·敌方战场            │                 │
 *   │ 黑·大比分          │ 黄分隔                 │ 白·公共骰子区     │
 *   │                   │ 紫·我方战场            │                 │
 *   │ 绿·战斗播报log     │ 黄分隔                 │ 红·我方骰池      │
 *   │  （可滚动，战场信息）│ 粉·我方手牌            │                 │
 *   │                   │ 黄底条                 │ 粉·基础分        │
 *   │                   │                       │ 绿·额外分        │
 *   │                   │                       │ 黄·倍率          │
 *   │                   │                       │ 蓝·总分          │
 *   └───────────────────┴───────────────────────┴─────────────────┘
 * </pre>
 * 面板自适应窗口尺寸（不出界并尽量占满）；交互通过点击 + 底部按钮，
 * 操作包发给服务端，状态由 {@link DuelStateS2CPacket} 全量刷新。
 * 卡名悬停显示文物本名；对局结束后不再显示认输按钮。
 * 蓝区/公共骰池超出时自动换行/换行缩尺寸；选中手牌/骰子有金色光效与跳跃
 * 提示；部署时手牌飞出（前移 + 旋转 360° + 重重落下并发出落点音效）。
 */
@OnlyIn(Dist.CLIENT)
public class DuelScreen extends Screen
{
    private static final int MAX_HAND_SHOWN = 8;

    private static final int[] DIE_COLORS = {
            0, 0xFFE04848, 0xFFE08A3C, 0xFFE0CC3C, 0xFF5FBF5F, 0xFF3FC0C8, 0xFFB06AD0
    };
    private static final ResourceLocation CARD_BACK =
            ResourceLocation.fromNamespaceAndPath(Laigu.MODID, "textures/item/card_back.png");

    /** 对战界面全屏背景（MC 石砌砖纹 × 物华弥新博物馆夜色，256×256 可平铺）。 */
    private static final ResourceLocation DUEL_BG =
            ResourceLocation.fromNamespaceAndPath(Laigu.MODID, "textures/gui/duel_bg.png");

    /** 战场槽背景贴图（32×32，卡面区 (9,6)-(23,26)，骰子列 (2,3)-(6,28)）。 */
    private static final ResourceLocation CARD_BG =
            ResourceLocation.fromNamespaceAndPath(Laigu.MODID, "textures/gui/card_bg.png");

    /** 手牌背景贴图（30×30，卡面区 (6,3)-(23,26)）。 */
    private static final ResourceLocation HAND_CARD_BG =
            ResourceLocation.fromNamespaceAndPath(Laigu.MODID, "textures/gui/hand_card_bg.png");

    private final BlockPos pos;
    private DuelView v;
    private int selectedPool = -1;
    private int selectedHand = -1;
    private final List<AnimationEvent> newAnimationEvents = new ArrayList<>();
    // ---- 阶段17：新系统动画事件（事件驱动；来源 NewAnimationEventS2CPacket） ----
    /** 待消费的新系统事件队列（每 tick 处理一条）。 */
    private final ArrayDeque<AnimationEvent> newAnimQueue = new ArrayDeque<>();
    private int newJumpSide = -1, newJumpSlot = -1, newJumpAge = -1;    // 跳跃（CARD_TRIGGER）
    private int newFlashSide = -1, newFlashSlot = -1, newFlashAge = -1; // 激活闪光（CARD_ACTIVATE）
    private int newMarkSide = -1, newMarkSlot = -1, newMarkAge = -1;    // 销毁标记（CARD_DESTROY_MARK）
    private String newPopupText = "";                                    // 弹出文本（+N / ×N）
    private int newPopupColor = 0, newPopupAge = -1, newPopupSide = -1, newPopupSlot = -1;

    /** 新版动画包消费入口：事件入队逐 tick 播放；旧全量状态动画保持不变。 */
    public void acceptNewAnimationEvents(List<AnimationEvent> events)
    {
        if (events == null) return;
        newAnimationEvents.addAll(events);
        if (newAnimationEvents.size() > 128)
            newAnimationEvents.subList(0, newAnimationEvents.size() - 128).clear();
        for (AnimationEvent e : events) if (e != null)
        {
            newAnimQueue.addLast(e);
            if (newAnimQueue.size() > 64) newAnimQueue.pollFirst();
        }
    }

    /** 阶段17：每 tick 消费一条新系统事件，映射为跳跃/弹出/闪光/销毁标记。 */
    private void processNewAnimations()
    {
        if (newJumpAge >= 0 && ++newJumpAge > 14) { newJumpAge = -1; newJumpSide = -1; newJumpSlot = -1; }
        if (newFlashAge >= 0 && ++newFlashAge > 12) { newFlashAge = -1; newFlashSide = -1; newFlashSlot = -1; }
        if (newMarkAge >= 0 && ++newMarkAge > 40) { newMarkAge = -1; newMarkSide = -1; newMarkSlot = -1; }
        if (newPopupAge >= 0 && ++newPopupAge > 24) { newPopupAge = -1; newPopupText = ""; }
        AnimationEvent e = newAnimQueue.pollFirst();
        if (e == null) return;
        switch (e.type())
        {
            case CARD_TRIGGER -> { newJumpSide = e.side(); newJumpSlot = e.slot(); newJumpAge = 0; }
            case CARD_ACTIVATE -> { newFlashSide = e.side(); newFlashSlot = e.slot(); newFlashAge = 0; playUi(SoundEvents.EXPERIENCE_ORB_PICKUP, 0.4f); }
            case CARD_DESTROY_MARK -> { newMarkSide = e.side(); newMarkSlot = e.slot(); newMarkAge = 0; }
            case SCORE_POPUP -> { newPopupText = "+" + e.value(); newPopupColor = 0xFF7DF77D; newPopupAge = 0; newPopupSide = e.side(); newPopupSlot = e.slot(); playUi(SoundEvents.ITEM_PICKUP, 0.4f); }
            case MULTIPLIER_POPUP -> { newPopupText = "x" + e.value(); newPopupColor = 0xFFFFE066; newPopupAge = 0; newPopupSide = e.side(); newPopupSlot = e.slot(); playUi(SoundEvents.EXPERIENCE_ORB_PICKUP, 0.5f); }
            default -> { }
        }
    }

    /** 阶段17：卡位覆盖动画（跳跃位移、激活闪光、销毁标记、弹出文本）。 */
    private void drawNewAnimOverlays(GuiGraphics g, int sideAbs, int slot, int x, int y)
    {
        boolean jump = newJumpAge >= 0 && newJumpSide == sideAbs && newJumpSlot == slot;
        if (jump) drawSelectionGlow(g, x - 2, y - 2, fieldW + 4, fieldH + 4, 0xFFFFE066);
        if (newFlashAge >= 0 && newFlashSide == sideAbs && newFlashSlot == slot)
        {
            int alpha = (int) (140 * (1f - newFlashAge / 12f));
            g.fill(x, y, x + fieldW, y + fieldH, (alpha << 24) | 0xFFE066);
        }
        if (newMarkAge >= 0 && newMarkSide == sideAbs && newMarkSlot == slot)
        {
            drawSelectionGlow(g, x - 2, y - 2, fieldW + 4, fieldH + 4, 0xFFFF5555);
            g.drawString(font, "销", x + fieldW / 2 - 4, y + 2, 0xFFFF5555);
        }
        if (newPopupAge >= 0 && newPopupSide == sideAbs && newPopupSlot == slot)
        {
            int rise = Math.round(newPopupAge * 0.8f);
            int alpha = Math.max(0, 255 - newPopupAge * 10);
            g.drawString(font, newPopupText, x + fieldW / 2 - font.width(newPopupText) / 2,
                    y - 10 - rise, (alpha << 24) | (newPopupColor & 0xFFFFFF));
        }
    }

    // ---- 动画状态 ----
    private float selJump = 1f;        // 选中跳跃进度 0→1
    private int selJumpHand = -1;      // 正在跳跃的手牌
    private int selJumpDie = -1;       // 正在跳跃的骰子
    private int selJumpField = -1;     // 正在跳跃的战场槽位
    private CardFlight flight;         // 部署入场飞行动画
    private float flightT = -1f;       // 飞行进度 -1=未播放

    // ---- 计分动画 ----
    private List<DuelView.ScoreStepView> scoreSteps = new ArrayList<>();
    private boolean playingScore = false;
    private boolean scorePlayed = false;   // 本组步骤是否已播完（防重复包重播）
    private int playIdx = 0, playTick = 0, scoreFrame = 0;
    private int scoreFinaleT = -1;   // 总分终幕（烟花+放大）计时帧；-1=未激活
    private float totalAnim = 0f;    // 总分单跳进度 0..1（卡片阶段结束后才开始）
    private int finishedRoundPlayed = -1;   // 已播完计分动画的终局回合号（用于胜负界面时机）
    private final List<FwP>[] fwParts = new List[]{new ArrayList<>(), new ArrayList<>()};   // 双方各一套烟花粒子
    private int fwSpawnTick = 0;
    // 小局胜利烟花：上一小局获胜方（绝对侧 0/1；-1=未知/平局），用于本回合总分终幕时持续补放
    private int roundWinner = -1;
    // 总分终幕结束时清空双方烟花粒子
    private void clearFireworks()
    {
        fwParts[0].clear();
        fwParts[1].clear();
        fwSpawnTick = 0;
    }
    private static final int[] FW_COLORS = {0xFFFF6666, 0xFFFFE066, 0xFF66FF88, 0xFF66CCFF, 0xFFD066FF, 0xFFFFAA55, 0xFF66FFFF, 0xFFFF9FF9};

    /** 金卡小星星：位置/速度/寿命/形状。 */
    private static final class StarP
    {
        float x, y, vx, vy, life;
        int design;   // 0/1/2 三种小星
        int ownerKey; // 双方侧位 + 槽位，避免不同金卡共用粒子坐标
        StarP(float x, float y, float vx, float vy, float life, int design, int ownerKey)
        { this.x = x; this.y = y; this.vx = vx; this.vy = vy; this.life = life; this.design = design; this.ownerKey = ownerKey; }
    }
    private final List<StarP> goldStars = new ArrayList<>();
    private final Map<Integer, Integer> goldStarTicks = new HashMap<>();
    // #6：最终计分动画期间，log/大比分保持旧值，动画结束才刷新
    private List<String> gatedLog = new ArrayList<>();
    private int gatedMyWins = 0, gatedOppWins = 0;
    private boolean gatedInit = false;

    /** 烟花粒子：位置/速度/寿命/颜色。 */
    private static final class FwP
    {
        float x, y, vx, vy, life;
        int color, size;
        FwP(float x, float y, float vx, float vy, float life, int color, int size)
        { this.x = x; this.y = y; this.vx = vx; this.vy = vy; this.life = life; this.color = color; this.size = size; }
    }
    private int stepsRound = 0;
    private int scoreJumpSide = -1, scoreJumpSlot = -1, scoreJumpKind = 0;
    private float goldT = 0f;   // 金卡粒子时间游标（持续上浮循环）

    // ---- 抽卡动画 ----
    private int prevHandSize = -1;                    // 上一状态手牌数（增量为抽卡）
    private final List<DrawFlight> drawQueue = new ArrayList<>();
    private int deckX, deckY, deckW, deckH;           // 牌库（左下角）位置与尺寸

    private static class CardFlight
    {
        final float sx, sy, tx, ty;
        final ItemStack card;
        final boolean spin;   // true=部署入场（旋转360°）；false=抽卡（轻抛不转）
        CardFlight(float sx, float sy, float tx, float ty, ItemStack card, boolean spin)
        {
            this.sx = sx; this.sy = sy; this.tx = tx; this.ty = ty; this.card = card; this.spin = spin;
        }
    }

    /** 一次待播放的抽卡飞行（牌库 → 手牌）。 */
    private static class DrawFlight
    {
        final ItemStack card;
        final int handIndex;   // 目标手牌下标（用于计算落点）
        DrawFlight(ItemStack card, int handIndex)
        {
            this.card = card;
            this.handIndex = handIndex;
        }
    }

    // ---- 布局（init 时按窗口计算，全部为绝对屏幕坐标） ----
    private int px, py, panelW, panelH;
    private int leftX0, leftX1, centerX0, centerX1, rightX0, rightX1;
    private int blueY0, blueY1, blackY0, blackY1, greenY0, greenY1;
    private int cTopY0, cTopY1, brownY0, brownY1, cSep1Y0, cSep1Y1,
            purpleY0, purpleY1, cSep2Y0, cSep2Y1, pinkY0, pinkY1, cBotY0, cBotY1;
    private int oppScoreY0, oppScoreY1, whiteY0, whiteY1, myScoreY0, myScoreY1;
    // 计分区：对手（上） + 我方（下），各 4 行（基础/额外/倍率/总分）
    private int oppScoreZoneY0, oppScoreZoneY1, myScoreZoneY0, myScoreZoneY1;
    private int btnY;
    private int fieldW, fieldH, handW, handH, die;
    private int handShown;
    private int oppCardsY, myCardsY, handCardsY;
    private final int cardGap = 4, fieldGap = 3, dieGap = 3;
    // 公共骰池布局（换行自适应）
    private int sharedDie, sharedChipsPerRow, sharedShown, sharedX0, sharedY;

    // ---- 战斗播报 log（可滚动） ----
    private int logScroll = 0;
    private boolean logDragActive = false;

    // ---- 表情系统（始终在最上层） ----
    private boolean emojiOpen = false;
    private int emojiPanelX, emojiPanelY, emojiPanelW, emojiPanelH;
    private static final int EMOJI_CELL = 26, EMOJI_COLS = 4;
    private static final int BUBBLE_LIFE = 70, BUBBLE_FADE = 20;

    /** 一个正在展示的表情气泡：side=发送者绝对侧位（观战者为 -1）。 */
    private static class Bubble
    {
        final int side;
        final int emoji;
        int age;

        Bubble(int side, int emoji)
        {
            this.side = side;
            this.emoji = emoji;
        }
    }

    private final List<Bubble> bubbles = new ArrayList<>();

    public DuelScreen(BlockPos pos, CompoundTag state)
    {
        super(Component.literal("来古牌对战"));
        this.pos = pos;
        this.v = new DuelView(state);
        this.scoreSteps = new ArrayList<>(v.scoreSteps);
        // 重进/首次打开不播抽卡动画（手牌已就位）
        this.prevHandSize = v.hand.size();
        // 重进时若已是结算/结束状态，不重播计分动画（只在开局后第一次结算时播）
        this.scorePlayed = v.hasResult;
        startScoreIfNeeded();
    }

    public boolean isSamePos(BlockPos p)
    {
        return pos.equals(p);
    }

    /** 服务端广播来的表情：以气泡形式展示（发送者按绝对侧位定位）。 */
    public void onEmoji(int side, int emoji)
    {
        if (emoji < 0 || emoji >= DuelActions.EMOJI_COUNT) return;
        if (bubbles.size() >= 6) bubbles.remove(0);
        bubbles.add(new Bubble(side, emoji));
        playUi(SoundEvents.EXPERIENCE_ORB_PICKUP, 0.4f);
    }

    public void updateState(CompoundTag state)
    {
        DuelView nv = new DuelView(state);
        // 抽卡检测：手牌比上状态多 → 新增牌从牌库（左下角）飞入手牌
        if (prevHandSize >= 0 && nv.hand.size() > prevHandSize)
        {
            queueDrawFlights(nv, nv.hand.size() - prevHandSize);
        }
        prevHandSize = nv.hand.size();
        // 抓骰反馈：每次服务端确认一颗骰子后，让对应的抓骰卡牌跳跃一次。
        if (nv.draftPickSerial > v.draftPickSerial)
        {
            int slot = findDraftScoreCardSlot(nv.myField);
            if (slot >= 0)
            {
                selJumpField = slot;
                selJump = 0f;
            }
        }
        this.v = nv;
        this.selectedPool = -1;
        this.selectedHand = -1;
        this.scoreSteps = new ArrayList<>(v.scoreSteps);
        startScoreIfNeeded();
        clearWidgets();
        refreshButtons();
    }

    private int findDraftScoreCardSlot(List<DuelView.FieldView> field)
    {
        for (DuelView.FieldView f : field)
        {
            if (f.hidden || f.card.isEmpty()) continue;
            DuelCardData d = DuelCardCatalog.of(f.card);
            if (d != null && d.effect == com.laigu.laigu.duel.EffectType.DRAFT_SCORE_EXTRA) return f.slot;
        }
        return -1;
    }

    private void queueDrawFlights(DuelView nv, int n)
    {
        int sz = nv.hand.size();
        int shown = Math.min(sz, handShown);
        for (int k = 0; k < n; k++)
        {
            int src = sz - n + k;                       // 新牌在完整手牌中的下标
            int idx = Math.min(shown - 1, src);         // 落点（超出显示窗口则落到最后一张显示位）
            if (idx < 0) idx = 0;
            drawQueue.add(new DrawFlight(nv.hand.get(src), idx));
        }
    }

    @Override
    public boolean isPauseScreen()
    {
        return false;
    }

    @Override
    protected void init()
    {
        computeLayout();
        refreshButtons();
    }

    /** 按窗口尺寸与参考图分区比例计算所有区域坐标。 */
    private void computeLayout()
    {
        panelW = Math.min(620, Math.max(360, width - 8));
        panelH = Math.min(420, Math.max(240, height - 8));
        px = (width - panelW) / 2;
        py = (height - panelH) / 2;

        int zTop = py + 4;
        int zBot = py + panelH - 28;      // 底部留 24px 按钮条 + 4px 边距
        int zH = zBot - zTop;

        // 左右列收紧，把更多宽度让给战场卡牌
        int leftW = Math.max(80, Math.min(140, Math.round(panelW * 0.16f)));
        int rightW = Math.max(100, Math.min(200, Math.round(panelW * 0.22f)));
        int centerW = panelW - leftW - rightW;
        leftX0 = px; leftX1 = px + leftW;
        centerX0 = leftX1; centerX1 = centerX0 + centerW;
        rightX0 = centerX1; rightX1 = px + panelW;

        // 分区比例取自 duel_bg.png 实测行号（4..392 共 388 行），累积式边界防漂移，末段精确落在 zBot
        blackY0 = zTop; blackY1 = zTop + (int) (zH *  52.0 / 388.0);
        blueY0 = blackY1; blueY1 = zBot;
        greenY0 = zBot; greenY1 = zBot;   // 合并进 log 区，不再单独
        // 中列：累积边界 N=31/137/180/277/302/364（对应纹理行 35/141/184/281/306/368）
        cTopY0 = zTop;
        brownY0  = zTop + (int) (zH *  31.0 / 388.0);
        brownY1  = zTop + (int) (zH * 137.0 / 388.0);
        cSep1Y0  = brownY1;  cSep1Y1  = zTop + (int) (zH * 180.0 / 388.0);
        purpleY0 = cSep1Y1;  purpleY1  = zTop + (int) (zH * 277.0 / 388.0);
        cSep2Y0  = purpleY1; cSep2Y1  = zTop + (int) (zH * 302.0 / 388.0);
        pinkY0   = cSep2Y1;  pinkY1    = zTop + (int) (zH * 364.0 / 388.0);
        cTopY1 = brownY0;
        cBotY0 = pinkY1; cBotY1 = zBot;
        // 右列：累积边界 N=124/211（对应纹理行 128/215）
        oppScoreY0 = zTop; oppScoreY1 = zTop + (int) (zH * 124.0 / 388.0);
        whiteY0 = oppScoreY1; whiteY1 = zTop + (int) (zH * 211.0 / 388.0);
        myScoreY0 = whiteY1; myScoreY1 = zBot;
        // 旧的分开计分区字段弃用（合并进综合面板）
        oppScoreZoneY0 = oppScoreY1; oppScoreZoneY1 = oppScoreY1;
        myScoreZoneY0 = myScoreY1; myScoreZoneY1 = myScoreY1;

        btnY = zBot;

        // 卡尺寸
        int brownH = brownY1 - brownY0;
        int purpleH = purpleY1 - purpleY0;
        int pinkH = pinkY1 - pinkY0;
        fieldW = Math.max(30, (centerW - 8 - (DuelGame.FIELD_SLOTS - 1) * fieldGap) / DuelGame.FIELD_SLOTS);
        fieldW = Math.min(fieldW, 130);
        fieldH = Math.max(26, Math.min(104, brownH - 12));   // 顶部留 10px 标签位
        handW = Math.max(24, Math.min(fieldW,
                (centerW - 8 - (MAX_HAND_SHOWN - 1) * cardGap) / MAX_HAND_SHOWN));
        handH = Math.max(18, Math.min(pinkH - 12, fieldH));
        handShown = Math.min(MAX_HAND_SHOWN,
                Math.max(3, (centerW - 8 + cardGap) / (handW + cardGap)));
        die = Math.min(20, Math.max(12, rightW / 8));

        oppCardsY = brownY0 + 10;
        myCardsY = purpleY0 + 10;
        handCardsY = pinkY0 + 10;

        // 牌库（最左下角）：中列底部黄条左侧，作为抽卡动画起点
        int cBotH = cBotY1 - cBotY0;
        deckW = Math.max(16, Math.min(handW, 26));
        deckH = Math.max(10, cBotH - 4);
        deckX = centerX0 + 4;
        deckY = cBotY0 + Math.max(1, (cBotH - deckH) / 2);

        // 表情面板：2 行 × 4 列，位于左下角「表情」按钮上方
        emojiPanelW = EMOJI_COLS * (EMOJI_CELL + 4) + 8;
        emojiPanelH = 2 * (EMOJI_CELL + 4) + 8;
        emojiPanelX = px + 12;
        emojiPanelY = btnY - emojiPanelH - 4;
    }

    // ---- 底部按钮 ----

    private void refreshButtons()
    {
        int y = btnY + 2;
        int h = 20, w = 90;
        if (v.spectate)
        {
            // 观战模式：唯一操作是退出观战（ESC/按钮都走 onClose → SPECTATE_LEAVE）
            addRenderableWidget(Button.builder(Component.literal("退出观战"),
                    b -> onClose()).bounds(px + panelW / 2 - w / 2, y, w, h).build());
            return;
        }
        if (!v.started && v.phase != DuelGame.Phase.FINISHED)
        {
            addRenderableWidget(Button.builder(Component.literal("关闭"),
                    b -> onClose()).bounds(px + panelW / 2 - w / 2, y, w, h).build());
            return;
        }
        boolean finished = v.phase == DuelGame.Phase.FINISHED;
        if (finished)
        {
            int total = 2 * w + 10;
            int bx = px + (panelW - total) / 2;
            addRenderableWidget(Button.builder(Component.literal("再来一局"),
                    b -> send(DuelActions.REMATCH, 0, 0)).bounds(bx, y, w, h).build());
            addRenderableWidget(Button.builder(Component.literal("关闭"),
                    b -> onClose()).bounds(bx + w + 10, y, w, h).build());
        }
        else
        {
            switch (v.phase)
            {
                case DEPLOY -> addRenderableWidget(phaseButton("确认部署", v.myDeployDone,
                        () -> send(DuelActions.DEPLOY_CONFIRM, 0, 0), y, w, h));
                case DRAFT ->
                {
                    if (v.currentPicker == v.mySide)
                        addRenderableWidget(Button.builder(Component.literal("放弃拿骰"),
                                b -> send(DuelActions.SKIP_DRAFT, 0, 0)).bounds(px + panelW - 20 - 70 - 74, y, 70, h).build());
                }
                case PLACE -> addRenderableWidget(phaseButton("确认布置", v.myPlaceDone,
                        () -> send(DuelActions.PLACE_CONFIRM, 0, 0), y, w, h));
                case ROUND_END -> addRenderableWidget(phaseButton("下一轮", v.myRoundEndDone,
                        () -> send(DuelActions.NEXT_ROUND, 0, 0), y, w, h));
                default -> {}
            }
            // 对局结束后不再显示认输按钮
            addRenderableWidget(Button.builder(Component.literal("认输退出"),
                    b -> send(DuelActions.FORFEIT, 0, 0)).bounds(px + panelW - 20 - 70, y, 70, h).build());
        }
    }

    /**
     * 阶段确认按钮：已确认则置灰并改文案「已确认 ✓ 等待对方」，
     * 避免玩家重复点击仍以为没确认（服务端本就会忽略重复确认）。
     */
    private Button phaseButton(String label, boolean confirmed, Runnable action, int y, int w, int h)
    {
        if (confirmed)
        {
            Button b = Button.builder(Component.literal("已确认 ✓ 等待对方"), b2 -> {})
                    .bounds(px + panelW / 2 - 65, y, 130, h).build();
            b.active = false;
            return b;
        }
        return Button.builder(Component.literal(label), b2 -> action.run())
                .bounds(px + panelW / 2 - w / 2, y, w, h).build();
    }

    private void send(int action, int a, int b)
    {
        ModPackets.CHANNEL.sendToServer(new DuelActionC2SPacket(pos, action, a, b));
    }

    @Override
    public void onClose()
    {
        // 关闭界面 = 离开房间：对局进行中按认输处理（也即离开）；
        // 已结束或尚未开始直接退房（LEAVE_ROOM），避免对方「再来一局」把我拉回。
        // 观战模式只退出观战（不动房间与对局）。
        if (v != null)
        {
            if (v.spectate)
            {
                send(DuelActions.SPECTATE_LEAVE, 0, 0);
            }
            else if (v.started && v.phase != DuelGame.Phase.FINISHED)
            {
                send(DuelActions.FORFEIT, 0, 0);
            }
            else
            {
                send(DuelActions.LEAVE_ROOM, 0, 0);
            }
        }
        super.onClose();
    }

    // ---- 点击 ----

    @Override
    public boolean mouseClicked(double mx, double my, int button)
    {
        if (button != 0) return super.mouseClicked(mx, my, button);
        // 左下角「表情」按钮（自绘，非 widget，避免与点外面关面板双重切换）
        // 注意：必须先于左列 log 拖拽判断，否则点表情/面板会被 log 区吞掉。
        if (hitEmojiBtn(mx, my))
        {
            emojiOpen = !emojiOpen;
            playUi(SoundEvents.UI_BUTTON_CLICK.value(), 0.5f);
            return true;
        }
        // 表情面板：点击表情发出；点面板内空白只关面板；点面板外交回正常处理
        if (emojiOpen)
        {
            int e = emojiAt(mx, my);
            if (e >= 0)
            {
                send(DuelActions.EMOJI, e, 0);
                emojiOpen = false;
                playUi(SoundEvents.UI_BUTTON_CLICK.value(), 0.5f);
                return true;
            }
            if (hit(mx, my, emojiPanelX, emojiPanelY, emojiPanelW, emojiPanelH))
            {
                emojiOpen = false;
                return true;
            }
            emojiOpen = false;
        }
        // 左列 log 区：按住左键可拖拽滚动（拖拽过程中不处理其它点击）
        if (hit(mx, my, leftX0, blueY0, leftX1 - leftX0, greenY1 - blueY0))
        {
            logDragActive = true;
            return true;
        }
        if (!v.started || v.spectate) return super.mouseClicked(mx, my, button);
        switch (v.phase)
        {
            case DEPLOY ->
            {
                int h = hitHand(mx, my);
                if (h >= 0)
                {
                    selectedHand = selectedHand == h ? -1 : h;
                    if (selectedHand == h) { selJumpHand = h; selJump = 0f; }
                    playUi(SoundEvents.UI_BUTTON_CLICK.value(), 0.5f);
                    return true;
                }
                if (selectedHand >= 0 && selectedHand < v.hand.size())
                {
                    int f = hitFieldSlot(mx, my, myCardsY);
                    if (f >= 0)
                    {
                        // 行动力用完或已确认部署时服务端会拒绝，此时不播飞行动画
                        if (v.myAp <= 0 || v.myDeployDone)
                        {
                            playUi(SoundEvents.UI_BUTTON_CLICK.value(), 0.4f);
                            return true;
                        }
                        ItemStack hc = v.hand.get(selectedHand);
                        // 创造对局(creative)：金卡可直接放置，不做「替换同名白卡」的本地预判
                        if (DuelCardData.isGold(hc) && !v.creative)
                        {
                            // 金卡只能替换场上同名白卡：先本地预判，不匹配则拒绝（不发包不播动画）
                            ItemStack target = ItemStack.EMPTY;
                            for (DuelView.FieldView fv : v.myField)
                            {
                                if (fv.slot == f) { target = fv.card; break; }
                            }
                            boolean match = !target.isEmpty()
                                    && CardNbt.stripRaritySuffix(CardNbt.pathOf(hc))
                                        .equals(CardNbt.stripRaritySuffix(CardNbt.pathOf(target)))
                                    && !DuelCardData.isGold(target);
                            if (!match)
                            {
                                playUi(SoundEvents.UI_BUTTON_CLICK.value(), 0.4f);
                                return true;
                            }
                        }
                        send(DuelActions.DEPLOY_PUT, selectedHand, f);
                        startFlight(selectedHand, f);
                        selectedHand = -1;
                        return true;
                    }
                }
            }
            case DRAFT ->
            {
                if (v.currentPicker == v.mySide)
                {
                    int s = hitShared(mx, my);
                    if (s >= 0)
                    {
                        send(DuelActions.PICK_DIE, s, 0);
                        playUi(SoundEvents.UI_BUTTON_CLICK.value(), 0.5f);
                        return true;
                    }
                }
            }
            case PLACE ->
            {
                int p = hitMyPool(mx, my);
                if (p >= 0)
                {
                    selectedPool = selectedPool == p ? -1 : p;
                    if (selectedPool == p) { selJumpDie = p; selJump = 0f; }
                    playUi(SoundEvents.UI_BUTTON_CLICK.value(), 0.5f);
                    return true;
                }
                if (selectedPool >= 0 && selectedPool < v.myPool.size())
                {
                    int f = hitFieldSlot(mx, my, myCardsY);
                    if (f >= 0)
                    {
                        send(DuelActions.PLACE_DIE, selectedPool, f);
                        selJumpField = f; selJump = 0f;
                        playUi(SoundEvents.UI_BUTTON_CLICK.value(), 0.5f);
                        selectedPool = -1;
                        return true;
                    }
                }
                int[] td = hitFieldDie(mx, my);
                if (td != null)
                {
                    send(DuelActions.PLACE_TAKE_DIE, td[0], td[1]);
                    playUi(SoundEvents.UI_BUTTON_CLICK.value(), 0.5f);
                    return true;
                }
            }
            default -> {}
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy)
    {
        if (logDragActive)
        {
            logScroll -= Math.round(dy / 9.0);
            clampLogScroll();
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button)
    {
        logDragActive = false;
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta)
    {
        // 左列 log 区上滚/下滚查看历史
        if (hit(mx, my, leftX0, blueY0, leftX1 - leftX0, greenY1 - blueY0))
        {
            logScroll += Math.round(delta);
            clampLogScroll();
            return true;
        }
        return super.mouseScrolled(mx, my, delta);
    }

    private void clampLogScroll()
    {
        int total = wrappedLineCount();
        int max = Math.max(0, total - logLinesVisible());
        logScroll = Math.max(0, Math.min(logScroll, max));
    }

    /** 左列可滚动 log 区的可视行数（由蓝+绿两区实际高度算出，换行行数参与计行）。 */
    private int logLinesVisible()
    {
        int blue = Math.max(0, (blueY1 - blueY0 - 4) / 7);
        int green = Math.max(0, (greenY1 - greenY0 - 4) / 7);
        return Math.max(1, blue + green);
    }

    /** 当前 battleLog 全部换行后的总行数（scroll 单位）。 */
    private int wrappedLineCount()
    {
        int n = 0;
        for (String ln : v.battleLog) n += wrap(ln.substring(Math.min(2, ln.length())), logMaxChars()).size();
        return n;
    }

    private int logMaxChars()
    {
        return Math.max(1, (leftW() - 8) / 6);
    }

    private int hitFieldSlot(double mx, double my, int y)
    {
        for (int i = 0; i < DuelGame.FIELD_SLOTS; i++)
        {
            int x = fieldX(i);
            if (hit(mx, my, x, y, fieldW, fieldH)) return i;
        }
        return -1;
    }

    /** 布置阶段：命中我方场上某卡左侧的骰子 → {槽位, 该卡骰下标}（确认前可取下退回骰池）。 */
    private int[] hitFieldDie(double mx, double my)
    {
        int fd = fieldDieSize();
        for (DuelView.FieldView f : v.myField)
        {
            if (f.hidden || f.card.isEmpty() || f.dice.length == 0) continue;
            int x = fieldX(f.slot);
            int y = myCardsY;
            for (int k = 0; k < f.dice.length; k++)
            {
                // 骰子在卡牌左侧竖排：x+1, y+4 + k*(fd+2)，与 renderFieldCard 一致
                int dx = x + 1;
                int dy = y + 4 + k * (fd + 2);
                if (hit(mx, my, dx, dy, fd, fd)) return new int[]{f.slot, k};
            }
        }
        return null;
    }

    private int hitHand(double mx, double my)
    {
        int n = Math.min(v.hand.size(), handShown);
        for (int i = 0; i < n; i++)
        {
            int x = handX(i, n);
            if (hit(mx, my, x, handCardsY, handW, handH)) return i;
        }
        return -1;
    }

    private int hitShared(double mx, double my)
    {
        if (v.shared.isEmpty()) return -1;
        layoutShared();
        for (int i = 0; i < sharedShown; i++)
        {
            int c = i % sharedChipsPerRow;
            int r = i / sharedChipsPerRow;
            int x = sharedX0 + c * (sharedDie + dieGap);
            int y = sharedY + r * (sharedDie + dieGap);
            if (mx >= x && mx < x + sharedDie && my >= y && my < y + sharedDie) return i;
        }
        return -1;
    }

    private int rightW0()
    {
        return rightX1 - rightX0;
    }

    private int hitMyPool(double mx, double my)
    {
        for (int i = 0; i < v.myPool.size(); i++)
        {
            int x = poolChipX(i);
            int y = poolChipY(myScoreY0, i);
            if (mx >= x && mx < x + die && my >= y && my < y + die) return i;
        }
        return -1;
    }

    // ---- 动画 ----

    private void playUi(SoundEvent s, float vol)
    {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) mc.player.playSound(s, vol, 1.0f);
    }

    private void advanceAnimations()
    {
        // 总分终幕：计分结束后，总分从 0 单跳到最终（约2s，随速度缩放），再放大+烟花
        if (scoreFinaleT >= 0)
        {
            scoreFinaleT++;
            totalAnim = Math.min(1f, totalAnim + 1f / (framesPerTick() * 9f));   // ~1.5s 跳完总分
            if (scoreFinaleT > framesPerTick() * 16) { scoreFinaleT = -1; totalAnim = 0f; clearFireworks(); }
        }
        // 烟花粒子更新：重力 + 寿命衰减（双方各自）
        for (List<FwP> list : fwParts)
            for (int i = list.size() - 1; i >= 0; i--)
            {
                FwP p = list.get(i);
                p.vy += 0.35f;
                p.x += p.vx; p.y += p.vy;
                p.life -= 0.028f;
                if (p.life <= 0) list.remove(i);
            }
        if (selJump < 1f)
        {
            selJump = Math.min(1f, selJump + 0.12f);
            if (selJump >= 1f) { selJumpHand = -1; selJumpDie = -1; selJumpField = -1; }
        }
        if (flight != null)
        {
            flightT += 0.045f;
            if (flightT >= 1f)
            {
                playUi(flight.spin ? SoundEvents.ANVIL_LAND : SoundEvents.ITEM_PICKUP,
                        flight.spin ? 0.7f : 0.35f);
                flight = null;
            }
        }
        else if (!drawQueue.isEmpty())
        {
            startNextDraw();   // 上段飞行结束 → 接下一张抽卡
        }
        goldT = (goldT + 0.05f) % 1f;   // 金卡粒子循环游标
        if (!bubbles.isEmpty())
        {
            bubbles.removeIf(b -> ++b.age > BUBBLE_LIFE);   // 表情气泡老化
        }
        processNewAnimations();
        advanceScore();
    }

    /** 从队列取一张抽卡，从牌库（左下角）飞向手牌对应位置。 */
    private void startNextDraw()
    {
        if (drawQueue.isEmpty()) return;
        DrawFlight d = drawQueue.remove(0);
        int shown = Math.min(v.hand.size(), handShown);
        int idx = Math.max(0, Math.min(d.handIndex, shown - 1));
        float sx = deckX + deckW / 2.0f;
        float sy = deckY + deckH / 2.0f;
        float tx = handX(idx, shown) + handW / 2.0f;
        float ty = handCardsY + handH / 2.0f;
        flight = new CardFlight(sx, sy, tx, ty, d.card, false);
        flightT = 0f;
    }

    // ---- 计分动画 ----

    /** 新一轮结算到来且该组步骤尚未播过 → 开始逐卡计分动画。 */
    private void startScoreIfNeeded()
    {
        if (v.hasResult && !scoreSteps.isEmpty())
        {
            if (!playingScore && (!scorePlayed || stepsRound != v.round)) startScorePlayback();
        }
        else
        {
            playingScore = false;
            scorePlayed = false;
            playIdx = 0; playTick = 0; scoreFrame = 0;
            stepsRound = v.round;
            scoreJumpSide = -1; scoreJumpSlot = -1;
            scoreFinaleT = -1; totalAnim = 0f;   // 下一轮：停止烟花/总分动画
            roundWinner = -1;   // 清零上一小局获胜方
        }
    }

    private void startScorePlayback()
    {
        playingScore = true;
        scorePlayed = false;
        playIdx = 0; playTick = 0; scoreFrame = 0;
        stepsRound = v.round;
        if (!scoreSteps.isEmpty())
        {
            DuelView.ScoreStepView st = scoreSteps.get(0);
            scoreJumpSide = st.side;
            scoreJumpSlot = st.slot;
            scoreJumpKind = st.kind;
            // 第一步也在卡片起跳的同一帧配音效（避免动画先行/音效迟到的错位感）
            playJumpSound(scoreJumpKind, st);
        }
    }

    /** 每帧推进：每 framesPerTick() 帧完成一次跳动（tick），换卡并配音效。 */
    private void advanceScore()
    {
        if (!playingScore) return;
        scoreFrame++;
        if (scoreFrame >= framesPerTick())
        {
            scoreFrame = 0;
            playTick++;
            if (playTick >= scoreSteps.get(playIdx).ticks)
            {
                playTick = 0;
                playIdx++;
                if (playIdx >= scoreSteps.size())
                {
                    playingScore = false;
                    scorePlayed = true;
                    scoreJumpSide = -1;
                    scoreJumpSlot = -1;
                    scoreFinaleT = 0;   // 开始总分终幕（放大+烟花）
                    roundWinner = v.winnerLast;   // 记录本小局获胜方（-1=平局/未知），终幕烟花用
                    finishedRoundPlayed = stepsRound;   // 终局回合计分动画已播完
                    return;
                }
            }
            DuelView.ScoreStepView st = scoreSteps.get(playIdx);
            scoreJumpSide = st.side;
            scoreJumpSlot = st.slot;
            scoreJumpKind = st.kind;
            playJumpSound(scoreJumpKind, st);
        }
    }

    /** 当前播放进度下某侧面板显示的数值（0=基础 1=倍率 2=额外）。 */
    private int dispFor(int sideAbs, int which)
    {
        if (scoreSteps.isEmpty()) return which == 1 ? 1 : 0;
        if (!playingScore)
        {
            // 播放结束：显示该侧最终累计
            DuelView.ScoreStepView last = prevStepOf(sideAbs, scoreSteps.size());
            return last == null ? (which == 1 ? 1 : 0) : whichVal(last, which);
        }
        int idx = Math.min(playIdx, scoreSteps.size() - 1);
        DuelView.ScoreStepView cur = scoreSteps.get(idx);
        if (cur.side != sideAbs)
        {
            // 当前播到对方侧 → 本侧显示其最后已结算的累计值
            DuelView.ScoreStepView last = prevStepOf(sideAbs, idx);
            return last == null ? (which == 1 ? 1 : 0) : whichVal(last, which);
        }
        // 当前卡是该侧的 → 在 prev→cur 之间按 ticks 时长线性跳动
        DuelView.ScoreStepView prev = prevStepOf(sideAbs, idx);
        int pv = prev == null ? (which == 1 ? 1 : 0) : whichVal(prev, which);
        int cv = whichVal(cur, which);
        float p = Math.min(1f, ((float) playTick + (float) scoreFrame / framesPerTick()) / cur.ticks);
        return pv + Math.round((cv - pv) * p);
    }

    /** 下标 < idx 的最后一个属于该侧的步骤。 */
    private DuelView.ScoreStepView prevStepOf(int sideAbs, int idx)
    {
        for (int k = idx - 1; k >= 0; k--)
            if (scoreSteps.get(k).side == sideAbs) return scoreSteps.get(k);
        return null;
    }

    private static int whichVal(DuelView.ScoreStepView st, int which)
    {
        return switch (which) { case 1 -> st.mult; case 2 -> st.extra; default -> st.base; };
    }

    private int displayBase(int sideAbs)
    {
        if (!scoreSteps.isEmpty()) return dispFor(sideAbs, 0);
        return sideAbs == v.mySide ? v.timingBase : v.oppTimingBase;
    }

    private int displayMult(int sideAbs)
    {
        if (!scoreSteps.isEmpty()) return dispFor(sideAbs, 1);
        return sideAbs == v.mySide ? 1 + v.timingMult : 1 + v.oppTimingMult;
    }

    private int displayExtra(int sideAbs)
    {
        if (!scoreSteps.isEmpty()) return dispFor(sideAbs, 2);
        return sideAbs == v.mySide ? v.timingExtra : v.oppTimingExtra;
    }

    private float jumpProgress()
    {
        return Math.min(1f, ((float) scoreFrame) / framesPerTick());
    }
    /** 每次跳动的帧数，随 mod 设置里的积分动画速度倍率变化（实时生效）。速度越大越快。 */
    private int framesPerTick()
    {
        double m = LaiguConfig.SCORE_ANIM_SPEED.get();
        return Math.max(1, (int) Math.round(9 / Math.max(0.1, (float) m)));
    }

    // ================= 计分跳跃音效：与每张卡的跳跃动画严格同步 =================
    // 每次跳跃（每 tick）播放一次短音效；按特效类型/数值分量区分音色与音调。
    // kind 0=普通 1=激活 2=伏击 3=破阵；base/mult/extra 为本步骤后累计值。

    /** 步骤所属类型对应的音色（原版音效区分）。 */
    private static SoundEvent scoreSoundForKind(int kind)
    {
        switch (kind)
        {
            case 1: return SoundEvents.NOTE_BLOCK_CHIME.value();   // 激活：清亮钟声
            case 2: return SoundEvents.NOTE_BLOCK_PLING.value();   // 伏击：短促拨弦
            case 3: return SoundEvents.NOTE_BLOCK_HAT.value();     // 破阵：短促高音
            default: return SoundEvents.EXPERIENCE_ORB_PICKUP;     // 普通
        }
    }

    /** 本次跳跃音调：仅基础分变化=1.0，仅额外分变化=0.9（低沉），仅倍率变化=1.15（高昂）；其余=1.0。 */
    private static float scorePitchFor(int kind, DuelView.ScoreStepView st, DuelView.ScoreStepView prev)
    {
        if (kind != 0) return 1.0f;   // 特效类已用音色区分，音调统一
        if (prev == null) return 1.0f;
        boolean b = st.base != prev.base;
        boolean m = st.mult != prev.mult;
        boolean e = st.extra != prev.extra;
        if (e && !b && !m) return 0.9f;    // 仅额外分 → 低沉
        if (m && !b && !e) return 1.15f;   // 仅倍率 → 高昂
        return 1.0f;                       // 基础分 / 混合 / 未知
    }

    /** 播放一次跳跃音效（与卡片跳动同步）。 */
    private void playJumpSound(int kind, DuelView.ScoreStepView st)
    {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        DuelView.ScoreStepView prev = prevStepOf(st.side, playIdx);
        mc.player.playSound(scoreSoundForKind(kind), 0.55f, scorePitchFor(kind, st, prev));
    }

    /** 计分跳动时的粒子光效（环绕扩散 + 淡出）。 */
    private void drawScoreParticles(GuiGraphics g, int cx, int cy, int color)
    {
        float p = jumpProgress();
        if (p <= 0.05f || p >= 0.95f) return;
        for (int k = 0; k < 10; k++)
        {
            double ang = Math.PI * 2 * k / 10.0 + 0.35;
            float r = 6 + p * 20;
            int px = (int) Math.round(cx + Math.cos(ang) * r);
            int py = (int) Math.round(cy + Math.sin(ang) * r - 8);
            int a = (int) (230 * (1 - p));
            g.fill(px - 2, py - 2, px + 2, py + 2, (a << 24) | (color & 0xFFFFFF));
        }
    }

    /** 金卡小星星：每个金卡槽位独立计时，避免只有最后绘制的金卡持续产生特效。 */
    private void drawGoldParticles(GuiGraphics g, int x, int y, int w, int h, int slot, int side)
    {
        int ownerKey = side * DuelGame.FIELD_SLOTS + slot;
        int tick = goldStarTicks.getOrDefault(ownerKey, 0) + 1;
        goldStarTicks.put(ownerKey, tick);
        int cx = x + w / 2, cy = y + h / 2;
        if (tick % 18 == 0)
        {
            int ix = x + (int) (w * 0.14f), iy = y + (int) (h * 0.14f);
            int iw = w - (int) (w * 0.28f), ih = h - (int) (h * 0.28f);
            java.util.Random rr = new java.util.Random();
            for (int k = 0; k < 7; k++)
            {
                float ex, ey;
                switch (rr.nextInt(4))
                {
                    case 0 -> { ex = ix + rr.nextFloat() * iw; ey = iy; }
                    case 1 -> { ex = ix + rr.nextFloat() * iw; ey = iy + ih; }
                    case 2 -> { ex = ix; ey = iy + rr.nextFloat() * ih; }
                    default -> { ex = ix + iw; ey = iy + rr.nextFloat() * ih; }
                }
                float dx = ex - cx, dy = ey - cy;
                float len = Math.max(1f, (float) Math.sqrt(dx * dx + dy * dy));
                float spd = 0.15f + rr.nextFloat() * 0.25f;
                goldStars.add(new StarP(ex, ey, dx / len * spd, dy / len * spd,
                        22 + rr.nextInt(14), rr.nextInt(3), ownerKey));
            }
        }
        java.util.Iterator<StarP> it = goldStars.iterator();
        while (it.hasNext())
        {
            StarP s = it.next();
            if (s.ownerKey != ownerKey) continue;
            s.x += s.vx; s.y += s.vy; s.life -= 0.7f;
            if (s.life <= 0) { it.remove(); continue; }
            int a = (int) Math.min(255, s.life * 12);
            drawStar(g, Math.round(s.x), Math.round(s.y), s.design, (a << 24) | 0xFFF7D06A);
        }
    }

    /** 画三种小星形（1x1 十字 / 小菱形 / 实心点），很小。 */
    private void drawStar(GuiGraphics g, int x, int y, int design, int color)
    {
        switch (design)
        {
            case 0 -> { g.fill(x - 2, y, x + 2, y + 1, color); g.fill(x, y - 2, x + 1, y + 2, color); }
            case 1 -> { g.fill(x - 2, y, x - 1, y + 1, color); g.fill(x + 1, y, x + 2, y + 1, color);
                        g.fill(x, y - 2, x + 1, y - 1, color); g.fill(x, y + 1, x + 1, y + 2, color);
                        g.fill(x - 1, y - 1, x + 2, y + 2, color); }
            default -> g.fill(x - 1, y - 1, x + 1, y + 1, color);
        }
    }

    /** 按槽位取场上某张卡的视图（无则 null）。 */
    private DuelView.FieldView findField(List<DuelView.FieldView> field, int slot)
    {
        for (DuelView.FieldView f : field) if (f.slot == slot) return f;
        return null;
    }

    private void startFlight(int h, int f)
    {
        if (h < 0 || h >= v.hand.size() || f < 0 || f >= DuelGame.FIELD_SLOTS) return;
        int hn = Math.min(v.hand.size(), handShown);
        ItemStack card = v.hand.get(h);
        float sx = handX(h, hn) + handW / 2.0f;
        float sy = handCardsY + handH / 2.0f;
        float tx = fieldX(f) + fieldW / 2.0f;
        float ty = myCardsY + fieldH / 2.0f;
        flight = new CardFlight(sx, sy, tx, ty, card, true);
        flightT = 0f;
    }

    /** 飞行卡面：部署 = 前移+旋转360°+高高抛起落下；抽卡 = 轻抛小弧、不旋转。 */
    private void drawFlight(GuiGraphics g)
    {
        if (flight == null) return;
        float t = flightT;
        int w = Math.round(lerp(handW, fieldW, t));
        int h = Math.round(lerp(handH, fieldH, t));
        float bx = lerp(flight.sx, flight.tx, easeInOut(t));
        float arc = flight.spin ? 44f : 18f;
        float by = lerp(flight.sy, flight.ty, t) - arc * (float) Math.sin(Math.PI * t);
        PoseStack ps = g.pose();
        ps.pushPose();
        ps.translate(bx, by, 0);
        if (flight.spin) ps.mulPose(Axis.ZP.rotationDegrees(t * 360f));
        ps.translate(-w / 2.0f, -h / 2.0f, 0);
        drawCardFace(g, flight.card, 0, 0, w, h);
        ps.popPose();
    }

    /** 左下角牌库：卡背叠层 + 剩余张数。 */
    private void renderDeck(GuiGraphics g)
    {
        g.blit(CARD_BACK, deckX, deckY, 0, 0, deckW, deckH, deckW, deckH);
        String s = "牌库 " + v.deckCount;
        g.drawString(font, s, deckX + deckW + 4, deckY + Math.max(0, (deckH - 9) / 2), 0xFFFFE066);
    }

    private static float lerp(float a, float b, float t) { return a + (b - a) * t; }
    private static float easeInOut(float t) { return t * t * (3 - 2 * t); }

    // ---- 分区背景贴图（纹理 620x420） ----

    /**
     * 面板背景：每块纹理切片拉伸 blit 到 computeLayout 算出的实时分区矩形上。
     * 切片在纹理内互相邻接（同一 620x420 图），分区矩形随窗口动态变化，
     * 因此边框永远贴合分区边缘，任何窗口/GUI 缩放下都不会错位。
     */
    private void renderPanelBackground(GuiGraphics g)
    {
        g.fill(0, 0, width, height, 0xFF0A0A0E);          // 面板外全屏深底
        g.fill(px, py, px + panelW, py + panelH, 0xFF0F0E0C);
        // 采样区行号对应 duel_bg.png 实测：每条带的 u/v 都从金线外一圈深色内页开始，
        // 金线（左列 y4-56、中列横向金线、右列 y4-128 等）留给相邻分区绘制，避免双绘/漏缝
        strip(g, 1,   5,  96,  49, leftX0, blackY0, leftX1, blackY1);    // 左·大比分
        strip(g, 1,  57,  96, 333, leftX0, blueY0, leftX1, blueY1);      // 左·战斗播报
        strip(g, 100,   5,  382,  34, centerX0, cTopY0, centerX1, cTopY1);  // 中·页眉
        strip(g, 100,  40,  382, 105, centerX0, brownY0, centerX1, brownY1);// 中·敌方战场
        strip(g, 100, 146,  382,  34, centerX0, cSep1Y0, centerX1, cSep1Y1);// 中·页眉分隔1
        strip(g, 100, 181,  382,  104, centerX0, purpleY0, centerX1, purpleY1);// 中·我方战场
        strip(g, 100, 286,  382,  16, centerX0, cSep2Y0, centerX1, cSep2Y1);// 中·页眉分隔2
        strip(g, 100, 303,  382,  69, centerX0, pinkY0, centerX1, pinkY1);  // 中·手牌区
        strip(g, 100, 373,  382,  19, centerX0, cBotY0, centerX1, cBotY1);  // 中·页眉底条
        strip(g, 485,  5, 133, 122, rightX0, oppScoreY0, rightX1, oppScoreY1); // 右·对手
        strip(g, 485,128, 133, 87, rightX0, whiteY0, rightX1, whiteY1); // 右·公共骰池
        strip(g, 485,216, 133, 176, rightX0, myScoreY0, rightX1, myScoreY1);   // 右·我方
    }

    /** 把纹理 (u,v,uw,vh) 切片拉伸贴到分区矩形 (x0,y0)-(x1,y1)；过小的分区退化为纯色。 */
    private void strip(GuiGraphics g, int u, int v, int uw, int vh,
                       int x0, int y0, int x1, int y1)
    {
        int w = x1 - x0, h = y1 - y0;
        if (w < 4 || h < 4)
        {
            g.fill(x0, y0, x1, y1, 0xFF14120F);
            return;
        }
        g.blit(DUEL_BG, x0, y0, w, h, u, v, uw, vh, 620, 420);
    }

    // ---- 渲染 ----

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt)
    {
        renderPanelBackground(g);
        // 分区底色已由背景贴图承担，不再整面覆盖 fill（否则会盖住贴图）

        if (!v.started && v.phase != DuelGame.Phase.FINISHED)
        {
            g.drawString(font, "对局尚未开始", px + 20, py + 40, 0xFFAAAAAA);
            g.drawString(font, "手持卡组匣右键方块登记，或空手右键添加 AI", px + 20, py + 54, 0xFF888888);
            super.render(g, mx, my, pt);
            return;
        }

        // #6：任何回合计分动画(含1.5s总分单跳)播完前不刷新 log/大比分；播完才更新（随动画速度缩放）
        boolean scorePhase = v.phase == DuelGame.Phase.ROUND_END || v.phase == DuelGame.Phase.FINISHED;
        boolean animDone = scorePlayed && !playingScore && (scoreFinaleT < 0 || totalAnim >= 1f);
        if (!scorePhase || animDone)
        {
            gatedLog = new ArrayList<>(v.battleLog);
            gatedMyWins = v.myWins();
            gatedOppWins = v.oppWins();
            gatedInit = true;
        }
        advanceAnimations();
        renderZoneBackgrounds(g);
        renderBlueZone(g);
        renderBlackZone(g);
        renderCenterField(g, mx, my);
        renderDeck(g);
        renderWhiteZone(g);
        renderRedTopZone(g);
        renderRedBottomZone(g);
        drawFlight(g);

        super.render(g, mx, my, pt);
        renderCardTooltip(g, mx, my);
        if (v.started) renderEmojiButton(g);
        if (v.started && v.phase == DuelGame.Phase.FINISHED
                && scorePlayed && !playingScore && (scoreFinaleT < 0 || totalAnim >= 1f)
                && finishedRoundPlayed == v.round) renderResultBanner(g);
        renderEmojiOverlay(g, mx, my);
        renderBubbles(g);
    }

    /** 表情面板/气泡永远盖在所有图层之上（含结算遮罩/提示条）。 */
    private void renderEmojiOverlay(GuiGraphics g, double mx, double my)
    {
        if (emojiOpen) renderEmojiPalette(g, mx, my);
    }

    /**
     * 分区背景已由贴图（renderPanelBackground）承担，这里只按所选方案
     * 叠加极低不透明度的分区色相（保留原有的红/白/粉/紫/褐分区暗示），
     * 不再绘制黄色分隔条与边框（已画进纹理）。
     */
    private void renderZoneBackgrounds(GuiGraphics g)
    {
        // 左列：黑(大比分)顶部压暗 + log 区轻染
        fill(g, leftX0, blackY0, leftX1, blackY1, 0x28000000);
        // 右列：对手/我方综合面板轻染红相，公共骰池轻染白相
        fill(g, rightX0, oppScoreY0, rightX1, oppScoreY1, 0x20AC3232);
        fill(g, rightX0, whiteY0, rightX1, whiteY1, 0x18FFFFFF);
        fill(g, rightX0, myScoreY0, rightX1, myScoreY1, 0x20AC3232);
        // 中列内容区：战场/手牌极淡的色相暗示（敌褐/我紫/手牌粉）
        fill(g, centerX0, brownY0, centerX1, brownY1, 0x188F563B);
        fill(g, centerX0, purpleY0, centerX1, purpleY1, 0x1876428A);
        fill(g, centerX0, pinkY0, centerX1, pinkY1, 0x18FF58CA);
    }

    /** 蓝·战斗播报 log（上半）+ 绿·战斗播报 log（下半）：拼成一块完整滚动区（黑色大比分条保留在中间）。 */
    private void renderBlueZone(GuiGraphics g)
    {
        renderLogZone(g, blueY0, blueY1);
    }

    /** 绿·战斗播报 log（下半）：内容与蓝区合并，由 renderBlueZone 一并绘制。 */
    private void renderGreenZone(GuiGraphics g)
    {
    }

    /**
     * 左侧「战斗播报」滚动区：把 battleLog 全部换行后的行序列（最新在末尾），
     * 取末尾 visible 行作为可视窗（logScroll=0 紧跟最新，新消息即时出现），
     * 向上滚可回看历史。绘制时跳过中间的黑色大比分条（蓝区 → 跳过 → 绿区）。
     */
    private void renderLogZone(GuiGraphics g, int top, int bottom)
    {
        int x0 = leftX0 + 4, x1 = leftX1 - 4;
        if (gatedLog.isEmpty())
        {
            g.drawString(font, "（战场播报）", x0, top + 4, 0x88888888);
            return;
        }
        float sc = 0.85f;                 // 字体更小
        int lineH = Math.round(12 * sc);  // 行距更大
        int visible = Math.max(1, (bottom - top - 4) / lineH);
        // 展开为显示行：header=一条新消息的首行，后续是换行折行
        List<Object[]> rows = new ArrayList<>();   // {text, color, header, italic}
        for (String ln : gatedLog)
        {
            int side = ln.startsWith("A ") ? 0 : (ln.startsWith("B ") ? 1 : -1);
            String msg = ln.length() >= 2 ? ln.substring(2) : ln;
            boolean key = isKeyLine(msg);
            int col = side == -1 ? 0xFFDDDDDD : (side == v.mySide ? 0xFF7DF77D : 0xFFF77D7D);
            List<String> wl = wrap(msg, logMaxChars());
            for (int wi = 0; wi < wl.size(); wi++)
                rows.add(new Object[]{wl.get(wi), col, wi == 0, !key});
        }
        int total = rows.size();
        int end = total - logScroll;
        int start = Math.max(0, end - visible);
        PoseStack ps = g.pose();
        ps.pushPose();
        ps.scale(sc, sc, 1f);
        for (int k = start; k < end; k++)
        {
            Object[] r = rows.get(k);
            String txt = (String) r[0];
            int col = (int) r[1];
            boolean header = (boolean) r[2];
            boolean italic = (boolean) r[3];
            int y = Math.round((top + 2 + (k - start) * lineH) / sc);
            String display = header ? ("▸ " + txt) : ("   " + txt);
            if (italic) g.drawString(font, Component.literal(display).withStyle(ChatFormatting.ITALIC), x0, y, col);
            else g.drawString(font, display, x0, y, col);
        }
        ps.popPose();
        if (logScroll > 0) g.drawString(font, "▲", x1 - 6, top + 1, 0x66FFFFFF);
    }

    /** 关键词（得分/倍率/总分/破阵/伏击/激活等）= 重要信息（正体），其余斜体。 */
    private boolean isKeyLine(String s)
    {
        return s.indexOf('+') >= 0 || s.indexOf('×') >= 0 || s.indexOf('分') >= 0
                || s.indexOf("破阵") >= 0 || s.indexOf("伏击") >= 0 || s.indexOf("激活") >= 0
                || s.indexOf("总分") >= 0 || s.indexOf("倍率") >= 0;
    }

    private void renderBlackZone(GuiGraphics g)
    {
        String me = v.displayMyName();
        String opp = v.displayOppName();
        int cx = (leftX0 + leftX1) / 2;
        g.drawString(font, me, leftX0 + 6, blackY0 + 2, 0xFFFFDD66);
        g.drawString(font, opp, leftX1 - 6 - font.width(opp), blackY0 + 2, 0xFFFFDD66);
        // 大比分居中放大（我方视角）
        String sc = gatedMyWins + " : " + gatedOppWins;
        float s = (blackY1 - blackY0) >= 34 ? 2.0f : 1.5f;
        int sw = Math.round(font.width(sc) * s);
        PoseStack ps = g.pose();
        ps.pushPose();
        ps.translate(cx - sw / 2.0, blackY0 + 12, 0);
        ps.scale(s, s, 1.0f);
        g.drawString(font, sc, 0, 0, 0xFFFFFFFF);
        ps.popPose();
    }

    private void renderCenterField(GuiGraphics g, int mx, int my)
    {
        // 敌方战场
        g.drawString(font, (v.spectate ? "观战 · " : "") + "敌方战场", centerX0 + 6, brownY0 + 2, 0xFFFFCCAA);
        if (v.phase == DuelGame.Phase.DEPLOY && !v.spectate)
        {
            // 部署阶段蒙幕布：双方都能替换旧卡，故部署结束（双方确认）前，对方场上的
            // 全部信息（含卡背、占位数量）都不可见。到 DRAFT 阶段才揭示。
            // 观战模式下服务端已直接下发对方场上真实卡牌，故跳过幕布。
            int cy0 = brownY0 + 9, cy1 = brownY1 - 2;
            fill(g, centerX0 + 2, cy0, centerX1 - 2, cy1, 0xFF1A1620);
            fill(g, centerX0 + 2, cy0, centerX1 - 2, cy1, 0x66000000);
            int cw = font.width("对方部署中…");
            g.drawString(font, "对方部署中…", (centerX0 + centerX1) / 2 - cw / 2, (cy0 + cy1) / 2 - 5, 0xFFE8E0D0);
            int cw2 = font.width("双方确认部署后揭示");
            g.drawString(font, "双方确认部署后揭示", (centerX0 + centerX1) / 2 - cw2 / 2, (cy0 + cy1) / 2 + 8, 0xFF8078A0);
        }
        else
        {
            for (int i = 0; i < DuelGame.FIELD_SLOTS; i++)
            {
                int x = fieldX(i);
                int y = oppCardsY;
                boolean sj = playingScore && scoreJumpSide == 1 - v.mySide && scoreJumpSlot == i;
                if (sj) y -= Math.round(8 * Math.sin(Math.PI * jumpProgress()));
                if (sj) drawSelectionGlow(g, x - 2, y - 2, fieldW + 4, fieldH + 4, jumpColor());
                // 阶段17：新系统触发跳跃（CARD_TRIGGER）
                if (newJumpAge >= 0 && newJumpSide == 1 - v.mySide && newJumpSlot == i)
                    y -= Math.round(8 * Math.sin(Math.PI * newJumpAge / 14f));
                renderFieldCard(g, v.oppField, i, x, y);
                DuelView.FieldView fv = findField(v.oppField, i);
                if (fv != null && !fv.hidden && DuelCardData.isGold(fv.card))
                    drawGoldParticles(g, x, y, fieldW, fieldH, i, 1 - v.mySide);
                if (sj) drawScoreParticles(g, x + fieldW / 2, y + fieldH / 2, jumpColor());
                drawNewAnimOverlays(g, 1 - v.mySide, i, x, y);
            }
        }

        // 我方战场：有选中物时槽位显示目标提示
        g.drawString(font, "我方战场", centerX0 + 6, purpleY0 + 2, 0xFFFFD0E0);
        boolean picking = (v.phase == DuelGame.Phase.DEPLOY && selectedHand >= 0 && selectedHand < v.hand.size())
                || (v.phase == DuelGame.Phase.PLACE && selectedPool >= 0 && selectedPool < v.myPool.size());
        int hover = picking ? hitFieldSlot(mx, my, myCardsY) : -1;
        for (int i = 0; i < DuelGame.FIELD_SLOTS; i++)
        {
            int x = fieldX(i);
            if (picking) drawSlotOutline(g, x, myCardsY, i == hover ? 0xFFFFE066 : 0x55FFFFFF);
            int y = myCardsY;
            if (i == selJumpField && selJump < 1f)
                y -= Math.round(10 * Math.sin(Math.PI * selJump));
            boolean sj = playingScore && scoreJumpSide == v.mySide && scoreJumpSlot == i;
            if (sj) y -= Math.round(8 * Math.sin(Math.PI * jumpProgress()));
            if (sj) drawSelectionGlow(g, x - 2, y - 2, fieldW + 4, fieldH + 4, 0xFFFFE066);
            // 阶段17：新系统触发跳跃（CARD_TRIGGER）
            if (newJumpAge >= 0 && newJumpSide == v.mySide && newJumpSlot == i)
                y -= Math.round(8 * Math.sin(Math.PI * newJumpAge / 14f));
            renderFieldCard(g, v.myField, i, x, y);
            DuelView.FieldView fv = findField(v.myField, i);
            if (fv != null && !fv.hidden && DuelCardData.isGold(fv.card))
                drawGoldParticles(g, x, y, fieldW, fieldH, i, v.mySide);
            if (sj) drawScoreParticles(g, x + fieldW / 2, y + fieldH / 2, jumpColor());
            drawNewAnimOverlays(g, v.mySide, i, x, y);
        }

        // 我方手牌（观战=主机手牌，仅展示）
        int handN = Math.min(v.hand.size(), handShown);
        g.drawString(font, (v.spectate ? "观战 · " : "") + "我方手牌 " + v.hand.size() + " 张 · 行动力 " + v.myAp + "/" + v.myApMax
                + (v.hand.size() > handShown ? " · 前 " + handShown + " 张" : ""),
                centerX0 + 6, pinkY0 + 2, 0xFFFFE0F0);
        for (int i = 0; i < handN; i++)
        {
            int x = handX(i, handN);
            int y = handCardsY;
            boolean sel = i == selectedHand;
            if (sel) y -= 6;                              // 选中抬起
            if (i == selJumpHand && selJump < 1f)
                y -= Math.round(8 * Math.sin(Math.PI * selJump));
            if (sel) drawSelectionGlow(g, x - 2, y - 2, handW + 4, handH + 4, 0xFFFFE066);
            renderHandCard(g, v.hand.get(i), x, y);
        }
    }

    /** 白·公共骰池：数量多时自动换行并把骰子尺寸缩小，不横向溢出。 */
    private void renderWhiteZone(GuiGraphics g)
    {
        boolean myPick = v.phase == DuelGame.Phase.DRAFT && v.currentPicker == v.mySide;
        String label = "公共骰池" + (myPick ? " ← 轮到你选" : "");
        int lw = font.width(label);
        g.drawString(font, label, rightX0 + (rightW0() - lw) / 2, whiteY0 + 3,
                myPick ? 0xFFFFE066 : 0xFFDDDDDD);
        layoutShared();
        int n = sharedShown;
        if (n == 0) return;
        int bw = sharedChipsPerRow * sharedDie + (sharedChipsPerRow - 1) * dieGap;
        int bh = ((n + sharedChipsPerRow - 1) / sharedChipsPerRow) * (sharedDie + dieGap) - dieGap;
        // 抢骰回合轮到你时给骰区加金色边框
        if (myPick)
        {
            g.fill(sharedX0 - 2, sharedY - 2, sharedX0 + bw + 2, sharedY + bh + 2, 0xFFE0B000);
        }
        for (int i = 0; i < n; i++)
        {
            int c = i % sharedChipsPerRow;
            int r = i / sharedChipsPerRow;
            renderDie(g, sharedX0 + c * (sharedDie + dieGap),
                    sharedY + r * (sharedDie + dieGap), v.shared.get(i), sharedDie);
        }
    }

    /** 公共骰池换行布局：先缩尺寸到能整行放下，再按区域高度决定显示多少颗。 */
    private void layoutShared()
    {
        int n = v.shared.size();
        int zoneW = rightW0();
        int availW = Math.max(20, zoneW - 14);
        int availH = Math.max(14, (whiteY1 - whiteY0) - 22);
        int sd = die;
        while (sd > 9)
        {
            int cpr = Math.max(1, availW / (sd + dieGap));
            int rows = Math.max(1, (n + cpr - 1) / cpr);
            if (rows * (sd + dieGap) <= availH) break;
            sd--;
        }
        sharedDie = sd;
        sharedChipsPerRow = Math.max(1, availW / (sd + dieGap));
        int maxRows = Math.max(1, availH / (sd + dieGap));
        sharedShown = Math.min(n, sharedChipsPerRow * maxRows);
        sharedX0 = rightX0 + (zoneW - (sharedChipsPerRow * sd + (sharedChipsPerRow - 1) * dieGap)) / 2;
        sharedY = whiteY0 + 17;
    }

    /** 一侧的红色信息面板：标签 + 骰池（选中/跳跃高亮）。 */
    private void renderScorePanel(GuiGraphics g, int top, int bottom, String label, int labelColor,
                                  List<Integer> pool, int base, int mult, int extra,
                                  int sel, int jumpIdx, boolean mine)
    {
        int x0 = rightX0 + 6;
        int x1 = rightX1 - 6;
        // 浅红带：骰池文案（玩家xx骰子池：N/8）
        g.fill(x0 - 6, top, x1 + 6, top + 15, 0x66FF7777);
        g.drawString(font, label + "：" + pool.size() + "/" + DuelGame.MAX_DICE_HELD, x0, top + 3, labelColor);
        // 灰黑骰子放置区
        int dy = top + 16;
        int chipsPerRow = Math.max(1, (x1 - x0) / (die + dieGap));
        int rows = Math.max(1, (pool.size() + chipsPerRow - 1) / chipsPerRow);
        int diceH = rows * (die + dieGap) - 2;
        g.fill(x0 - 6, dy, x1 + 6, dy + diceH, 0x99333338);
        for (int i = 0; i < pool.size(); i++)
        {
            int r = i / chipsPerRow;
            int c = i % chipsPerRow;
            int cx = x0 + c * (die + dieGap);
            int cy = dy + r * (die + dieGap);
            if (i == jumpIdx && selJump < 1f) cy -= Math.round(8 * Math.sin(Math.PI * selJump));
            if (i == sel) drawSelectionGlow(g, cx - 2, cy - 2, die + 4, die + 4, 0xFFFFE066);
            renderDie(g, cx, cy, pool.get(i), die);
        }
        if (pool.isEmpty()) g.drawString(font, "（尚无骰子）", x0, dy + 2, 0xAA888888);
        // 2D 计分马赛克（参考图）：左=粉基础分(上)/绿额外分(下)，中=黄倍率，右=蓝总分(最高、最大)
        int sy = dy + diceH + 3;
        int bot = bottom - 2;
        if (bot - sy > 12)
        {
            int total = base * mult + extra;
            // 总分单独跳：卡片阶段先不跳(显示0)，计分结束后从0跳到最终；终幕再放大+烟花
            int shownTotal = scoreFinaleT >= 0 ? (int) Math.round(total * easeInOut(totalAnim))
                    : (playingScore ? 0 : total);
            int w = x1 - x0;
            int lw = (int) (w * 0.44);   // 左：粉/绿
            int mw = (int) (w * 0.25);   // 中：黄
            int rw = w - lw - mw;        // 右：蓝
            int lx = x0, mx = x0 + lw, rx = x0 + lw + mw;
            int sh = bot - sy;
            int lh = sh / 2;             // 左列上下各半
            drawScoreCell(g, lx, lx + lw, sy, sy + lh, "基础分", base, 0xFFF4A9D6, 6, mine ? 14 : 12, mine);
            drawScoreCell(g, lx, lx + lw, sy + lh, bot, "额外分", extra, 0xFF9EE56A, 6, mine ? 14 : 12, mine);
            drawScoreCell(g, mx, mx + mw, sy, bot, "倍率", mult, 0xFFF5E24E, 6, mine ? 14 : 12, mine);
            drawScoreCell(g, rx, rx + rw, sy, bot, "总分", shownTotal, 0xFF9FC5FF, 7, mine ? Math.min(21, (int) (sh * 1.4)) : Math.min(19, (int) (sh * 1.2)), mine);
            if (scoreFinaleT >= 0 && totalAnim >= 1f)
            {
                int cx = (rx + rx + rw) / 2, cy = (sy + bot) / 2;
                int thisSide = mine ? v.mySide : 1 - v.mySide;
                int myW = mine ? v.myWins() : v.oppWins();
                int oppW = mine ? v.oppWins() : v.myWins();
                boolean isWinner = (v.phase == DuelGame.Phase.FINISHED && myW > oppW)   // 终局：总胜场领先方
                        || (v.phase == DuelGame.Phase.ROUND_END && roundWinner == thisSide);  // 小局：本小局获胜方
                List<FwP> list = fwParts[thisSide];
                // 持续烟花：胜利方每 ~18 帧补一波（终局判定或小局获胜方）
                if (isWinner && (list.isEmpty() || fwSpawnTick++ % 18 == 0))
                {
                    java.util.Random rr = new java.util.Random();
                    for (int k = 0; k < 18; k++)
                    {
                        double ang = rr.nextDouble() * Math.PI * 2;
                        float spd = (0.5f + rr.nextFloat() * 1.4f) * (rw / 12f);
                        list.add(new FwP(cx, cy,
                                (float) (Math.cos(ang) * spd),
                                (float) (Math.sin(ang) * spd) - 1.6f,
                                0.75f + rr.nextFloat() * 0.4f,
                                FW_COLORS[rr.nextInt(FW_COLORS.length)],
                                1 + rr.nextInt(2)));
                    }
                }
                for (FwP p : list)
                {
                    int a = (int) Math.max(0, Math.min(255, p.life * 255));
                    g.fill(Math.round(p.x) - p.size, Math.round(p.y) - p.size,
                            Math.round(p.x) + p.size, Math.round(p.y) + p.size,
                            (a << 24) | (p.color & 0xFFFFFF));
                }
            }
        }
    }

    /** 画一个计分格：不再整体着色（露出底下贴图），只画小标签(左上) + 大字数字。mine=我方：基础/额外右下移、倍率左移、总分右移。 */
    private void drawScoreCell(GuiGraphics g, int x0, int x1, int y0, int y1,
                               String label, int value, int tint, int labelSize, int numSize, boolean mine)
    {
        int midY = (y0 + y1) / 2;
        String txt = String.valueOf(value);
        int tw = font.width(txt);
        int ts = Math.round(tw * numSize / 9.0f), th = Math.round(9 * numSize / 9.0f);
        // 我方数值微调：基础/额外稍左下、倍率稍左、总分稍右
        int nox = 0, noy = 0;
        if (mine)
        {
            if ("基础分".equals(label) || "额外分".equals(label)) { nox -= 4; noy += 3; }
            else if ("倍率".equals(label)) { nox -= 4; }
            else if ("总分".equals(label)) { nox += 5; }
        }
        PoseStack ps = g.pose();
        ps.pushPose();
        ps.translate(x1 - ts - 2 + nox, midY - th / 2.0f + noy, 0);
        ps.scale(numSize / 9.0f, numSize / 9.0f, 1f);
        g.drawString(font, txt, 0, 0, 0xFFFFFFFF);
        ps.popPose();
        ps.pushPose();
        int labScale = labelSize;
        ps.translate(x0 + 2, y0 + 2, 0);
        ps.scale(labScale / 9.0f, labScale / 9.0f, 1f);
        g.drawString(font, label, 0, 0, 0xFFEEEEEE);
        ps.popPose();
    }

    private void renderRedTopZone(GuiGraphics g)
    {
        int s = 1 - v.mySide;
        renderScorePanel(g, oppScoreY0, oppScoreY1, v.displayOppName() + " 骰池", 0xFFFF8888,
                v.oppPool, displayBase(s), displayMult(s), displayExtra(s), -1, -1, false);
    }

    private void renderRedBottomZone(GuiGraphics g)
    {
        int s = v.mySide;
        renderScorePanel(g, myScoreY0, myScoreY1, v.displayMyName() + " 骰池", 0xFFFF8888,
                v.myPool, displayBase(s), displayMult(s), displayExtra(s), selectedPool, selJumpDie, true);
    }

    /** 单侧计分区：顶部小号名字 + 基础分/额外分/倍率/总分 四行（数值跟随计分动画跳动）。 */
    private void renderScoreZone(GuiGraphics g, int sideAbs, int y0, int y1, String name)
    {
        int x0 = rightX0 + 6, x1 = rightX1 - 6;
        g.drawString(font, name, x0, y0 + 1, 0xFFDDDDDD);
        int top = y0 + 9;
        int bot = y1 - 2;
        if (bot <= top + 16) return;
        int h = bot - top;
        int baseY0 = top;
        int extraY0 = top + (int) (h * 2.0 / 10.0);
        int multY0 = top + (int) (h * 4.0 / 10.0);
        int totalY0 = top + (int) (h * 6.0 / 10.0);
        int b = displayBase(sideAbs), m = displayMult(sideAbs), e = displayExtra(sideAbs), t = b * m + e;
        renderScoreRow(g, x0, x1, baseY0, extraY0, "基础", b, 0xFFF4A9D6, 7, 12);
        renderScoreRow(g, x0, x1, extraY0, multY0, "额外", e, 0xFF9EE56A, 7, 12);
        renderScoreRow(g, x0, x1, multY0, totalY0, "倍率", m, 0xFFF5E24E, 7, 12);
        renderScoreRow(g, x0, x1, totalY0, bot, "总分", t, 0xFF9FC5FF, 8, 15);
    }

    /** 画一行计分：左侧小标签 + 右侧大字数字（数字最大 16px，总分可到 19px）。 */
    private void renderScoreRow(GuiGraphics g, int x0, int x1, int y0, int y1,
                                String label, int value, int color, int labelSize, int numSize)
    {
        int mid = (y0 + y1) / 2;
        int numTextW = font.width(String.valueOf(value));
        int numTextH = 9;
        int numScaledW = Math.round(numTextW * numSize / 9.0f);
        int numScaledH = Math.round(9 * numSize / 9.0f);
        // 标签：竖排小字，居中于左侧
        int labY = mid - labelSize / 2;
        PoseStack ps = g.pose();
        ps.pushPose();
        ps.translate(x0 + 2, labY, 0);
        ps.scale(labelSize / 9.0f, labelSize / 9.0f, 1f);
        g.drawString(font, label, 0, 0, color);
        ps.popPose();
        // 数字：大字，居中于右侧
        ps.pushPose();
        ps.translate(x1 - numScaledW - 2, mid - numScaledH / 2.0f, 0);
        ps.scale(numSize / 9.0f, numSize / 9.0f, 1f);
        g.drawString(font, String.valueOf(value), 0, 0, 0xFFFFFFFF);
        ps.popPose();
    }

    private int leftW()
    {
        return leftX1 - leftX0;
    }

    private void renderFieldCard(GuiGraphics g, List<DuelView.FieldView> field, int slot, int x, int y)
    {
        DuelView.FieldView f = null;
        for (DuelView.FieldView c : field) if (c.slot == slot) { f = c; break; }
        // 战场槽背景贴图：整张 card_bg.png(32x32) 拉伸到槽位，
        // 卡面区 (9,6)-(23,26)、骰列区 (2,3)-(6,28) 按比例映射后放置卡面与骰子
        g.blit(CARD_BG, x, y, fieldW, fieldH, 0, 0, 32, 32, 32, 32);
        int border = 0xFF3A3A4E;
        if (f != null && !f.hidden)
        {
            DuelCardData d = DuelCardCatalog.of(f.card);
            if (d != null) border = classColor(d.cls);
        }
        // 1px 职类色描边（保留金卡/职类辨识，盖在贴图边缘）
        g.fill(x, y, x + fieldW, y + 1, border);
        g.fill(x, y + fieldH - 1, x + fieldW, y + fieldH, border);
        g.fill(x, y, x + 1, y + fieldH, border);
        g.fill(x + fieldW - 1, y, x + fieldW, y + fieldH, border);
        if (f == null) return;
        if (f.hidden)
        {
            int bs = Math.min(24, Math.max(16, fieldH / 3));
            g.blit(CARD_BACK, x + (fieldW - bs) / 2, y + (fieldH - bs) / 2, 0, 0, bs, bs, bs, bs);
            return;
        }
        // 卡面：贴入卡面区 (9,6)-(23,26)（比例映射到实际槽位大小，区内居中）
        int cx0 = x + Math.round(fieldW * 9f / 32), cy0 = y + Math.round(fieldH * 6f / 32);
        int cx1 = x + Math.round(fieldW * 23f / 32), cy1 = y + Math.round(fieldH * 26f / 32);
        float scale = Math.max(0.5f, Math.min((cx1 - cx0) / 16f, (cy1 - cy0) / 16f));
        int itemW = Math.round(16 * scale), itemH = Math.round(16 * scale);
        renderScaledItem(g, f.card, cx0 + (cx1 - cx0 - itemW) / 2, cy0 + (cy1 - cy0 - itemH) / 2, scale);
        // 骰子：贴入骰列区 (2,3)-(6,28)，竖排
        if (f.dice.length > 0)
        {
            int[] geo = fieldDieGeom(slot, y, f.dice.length);
            for (int k = 0; k < f.dice.length; k++)
                renderDie(g, geo[0], geo[1] + k * (geo[3] + 2), f.dice[k], geo[3]);
        }
        if (f.lasted)
        {
            g.fill(x + 1, y + 1, x + 6, y + 6, 0xFF55FF77);
        }
        // 【激活x】进度条：card 为激活目标时在卡顶显示 当前/cap
        DuelCardData cd = DuelCardCatalog.of(f.card);
        if (cd != null && cd.activateCap > 0)
        {
            int cap = cd.activateCap;
            int cur = Math.min(f.activation, cap);
            int bw = fieldW - 8;
            int bx = x + 4, by = y + 1;
            g.fill(bx, by, bx + bw, by + 2, 0xFF333355);
            g.fill(bx, by, bx + (int) (bw * cur / (float) cap), by + 2, 0xFF55AAFF);
            g.drawString(font, cur + "/" + cap, x + fieldW - 24, y, 0xFF55AAFF);
        }
    }

    /** 战场槽骰列几何：{x, y, w, dieSize}，位于骰列区 (2,3)-(6,28) 的比例映射内，竖排。 */
    private int[] fieldDieGeom(int slot, int cardY, int diceCount)
    {
        int x = fieldX(slot), y = cardY;
        int dx0 = x + Math.round(fieldW * 2f / 32), dy0 = y + Math.round(fieldH * 3f / 32);
        int dx1 = x + Math.round(fieldW * 6f / 32), dy1 = y + Math.round(fieldH * 28f / 32);
        int dw = Math.max(4, dx1 - dx0);
        int dh = Math.max(10, dy1 - dy0);
        int die = Math.min(dw, Math.max(5, dh / Math.max(1, diceCount) - 2));
        return new int[]{dx0, dy0, dw, die};
    }

    private void renderHandCard(GuiGraphics g, ItemStack stack, int x, int y)
    {
        DuelCardData d = DuelCardCatalog.of(stack);
        // 金卡：金色边框（金卡不能直接召唤，只能替换场上同名白卡）
        int border = DuelCardData.isGold(stack) ? 0xFFF7D06A : (d != null ? classColor(d.cls) : 0xFF3A3A4E);
        // 手牌背景贴图：hand_card_bg.png(30x30)，卡面占 (6,3)-(23,26)
        // u/v 取贴图中部 17x17（卡面区）拉伸到整槽，再叠 1px 描边
        g.blit(HAND_CARD_BG, x, y, handW, handH, 5, 2, 17, 17, 30, 30);
        // 1px 职类/金卡色描边
        g.fill(x, y, x + handW, y + 1, border);
        g.fill(x, y + handH - 1, x + handW, y + handH, border);
        g.fill(x, y, x + 1, y + handH, border);
        g.fill(x + handW - 1, y, x + handW, y + handH, border);
        float scale = itemScale(handW, handH, true);
        int itemW = Math.round(16 * scale);
        int itemH = Math.round(16 * scale);
        renderScaledItem(g, stack, x + (handW - itemW) / 2, y + (handH - itemH) / 2, scale);
    }

    /** 无骰子区的纯卡面（入场动画用）。 */
    private void drawCardFace(GuiGraphics g, ItemStack stack, int x, int y, int w, int h)
    {
        DuelCardData d = DuelCardCatalog.of(stack);
        int border = DuelCardData.isGold(stack) ? 0xFFF7D06A : (d != null ? classColor(d.cls) : 0xFF3A3A4E);
        g.fill(x, y, x + w, y + h, border);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0xFF17171F);
        float scale = itemScale(w, h, true);
        int itemW = Math.round(16 * scale);
        int itemH = Math.round(16 * scale);
        renderScaledItem(g, stack, x + (w - itemW) / 2, y + (h - itemH) / 2, scale);
    }

    /** 选中光效：在卡/骰周围描一圈亮色边框（先画，内容盖住中间）。 */
    private void drawSelectionGlow(GuiGraphics g, int x, int y, int w, int h, int color)
    {
        g.fill(x - 2, y - 2, x + w + 2, y + h + 2, color);
    }

    /** 战场槽位目标提示框。 */
    private void drawSlotOutline(GuiGraphics g, int x, int y, int color)
    {
        g.fill(x - 1, y - 1, x + fieldW + 1, y + 1, color);
        g.fill(x - 1, y + fieldH - 1, x + fieldW + 1, y + fieldH + 1, color);
        g.fill(x - 1, y - 1, x + 1, y + fieldH + 1, color);
        g.fill(x + fieldW - 1, y - 1, x + fieldW + 1, y + fieldH + 1, color);
    }

    /** 卡面贴图放大倍数：卡越大放得越大，同时给下方骰子留空间。 */
    private float itemScale(int w, int h, boolean hand)
    {
        float sw = (w - 10) / 16.0f;
        float sh = (hand ? (h - 10) : (h - fieldDieSize() - 4)) / 16.0f;
        float s = Math.min(sw, sh);
        return Math.max(1.5f, Math.min(5.0f, s));
    }

    private void renderScaledItem(GuiGraphics g, ItemStack stack, int x, int y, float scale)
    {
        PoseStack ps = g.pose();
        ps.pushPose();
        ps.translate(x, y, 0);
        ps.scale(scale, scale, 1.0f);
        g.renderItem(stack, 0, 0);
        ps.popPose();
    }

    /** 按特效类型返回跳跃光晕颜色：1=激活(蓝) 2=伏击(金) 3=破阵(红) 默认=普通(金)。 */
    private int jumpColor()
    {
        return switch (scoreJumpKind)
        {
            case 1 -> 0xFF66CCFF;   // 激活：蓝
            case 2 -> 0xFFFFE066;   // 伏击：金闪
            case 3 -> 0xFFFF6666;   // 破阵：红
            default -> 0xFFFFE066;
        };
    }

    /** 对局结果大字报：居中遮罩 + 大号「你获胜/你落败/平局」+ 得分小字。 */
    private void renderResultBanner(GuiGraphics g)
    {
        String title; int color;
        if (v.winnerLast == v.mySide) { title = "你获胜！"; color = 0xFF55FF55; }
        else if (v.winnerLast == 1 - v.mySide) { title = "你落败"; color = 0xFFFF5555; }
        else { title = "平局"; color = 0xFFFFAA55; }
        g.fill(px, py, px + panelW, py + panelH, 0x99000000);
        int cx = px + panelW / 2, cy = py + panelH / 2 - 10;
        float sc = 5f;
        g.pose().pushPose();
        g.pose().translate(cx, cy, 0);
        g.pose().scale(sc, sc, 1);
        g.drawString(font, title, -font.width(title) / 2, -8, color);
        g.pose().popPose();
        String sub = "本局得分  " + v.myLastTotal + " : " + v.oppLastTotal;
        g.drawString(font, sub, cx - font.width(sub) / 2, cy + 46, 0xFFDDDDDD);
        String tip = (v.darkMode ? "黑暗对决：" : "") + "点击「再来一局」或关闭";
        g.drawString(font, tip, cx - font.width(tip) / 2, cy + 64, 0xFFAAAAAA);
    }

    private int fieldDieSize()
    {
        return Math.min(14, Math.max(10, die - 4));
    }

    private void renderDie(GuiGraphics g, int x, int y, int value, int size)
    {
        int v = Math.max(1, Math.min(6, value));
        int c = DIE_COLORS[v];
        g.fill(x, y, x + size, y + size, c);
        g.fill(x + 1, y + 1, x + size - 1, y + size - 1, 0xFF111118);
        String s = String.valueOf(v);
        g.drawString(font, s, x + (size - font.width(s)) / 2, y + (size - 8) / 2, 0xFFFFFFFF);
    }

    // ---- 悬停 tooltip：文物本名 + 效果描述 ----

    private void renderCardTooltip(GuiGraphics g, int mx, int my)
    {
        if (!v.started) return;
        ItemStack card = null;
        DuelView.FieldView fv = null;
        int n = Math.min(v.hand.size(), handShown);
        for (int i = 0; i < n; i++)
        {
            int x = handX(i, n);
            if (hit(mx, my, x, handCardsY, handW, handH)) { card = v.hand.get(i); break; }
        }
        if (card == null)
        {
            fv = hitFieldView(v.myField, mx, my, myCardsY);
            if (fv != null) card = fv.card;
        }
        if (card == null)
        {
            // 部署阶段敌方战场整块蒙幕布：即使某些旧卡数据仍下发，也别显示对面卡描述/骰子。
            fv = hitFieldView(v.oppField, mx, my, oppCardsY);
            if (fv != null && !fv.hidden && v.phase != DuelGame.Phase.DEPLOY) card = fv.card;
        }
        if (card == null || card.isEmpty()) return;

        List<Component> lines = new ArrayList<>();
        boolean gold = DuelCardData.isGold(card);
        // 卡名 = 文物本名（如「铜车马」），不用自拟效果名
        lines.add(card.getHoverName().copy().withStyle(gold ? ChatFormatting.GOLD : ChatFormatting.WHITE));
        DuelCardData d = DuelCardCatalog.of(card);
        if (d != null)
        {
            lines.add(Component.literal(DuelCardData.dynastyOf(card) + " · " + d.cls.displayName)
                    .withStyle(d.cls.color));
            // 阶段13：卡面描述优先读新系统（DuelCard.description/goldDescription），未映射时回退旧目录
            String mainLine = "";
            String goldLine = null;
            java.util.Optional<DuelCard> nc = CardItemAdapter.create(card);
            if (nc.isPresent())
            {
                mainLine = nc.get().description();
                if (gold) goldLine = nc.get().goldDescription();
            }
            if (mainLine.isEmpty())
            {
                mainLine = d.descFor(card);
                if (gold) goldLine = d.goldDesc;
            }
            lines.add(Component.literal(mainLine).withStyle(ChatFormatting.GRAY));
            if (gold && goldLine != null && !goldLine.isEmpty() && !"焕章：无".equals(goldLine))
            {
                lines.add(Component.literal(goldLine).withStyle(ChatFormatting.GOLD));
            }
        }
        if (fv != null && !fv.hidden)
        {
            if (fv.dice.length > 0)
            {
                StringBuilder sb = new StringBuilder("骰子：");
                for (int k = 0; k < fv.dice.length; k++) sb.append(k > 0 ? "、" : "").append(fv.dice[k]);
                lines.add(Component.literal(sb.toString()).withStyle(ChatFormatting.YELLOW));
            }
            if (fv.lasted) lines.add(Component.literal("已在场至少一轮（老兵）").withStyle(ChatFormatting.GREEN));
        }
        List<FormattedCharSequence> tooltip = new ArrayList<>();
        for (Component c : lines) tooltip.add(c.getVisualOrderText());
        g.renderTooltip(font, tooltip, mx, my);
    }

    private boolean hit(double mx, double my, int x, int y, int w, int h)
    {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    // ---- 表情按钮 / 面板 / 气泡 ----

    private boolean hitEmojiBtn(double mx, double my)
    {
        return v.started && hit(mx, my, px + 20, btnY + 2, 70, 20);
    }

    private void renderEmojiButton(GuiGraphics g)
    {
        int x = px + 20, y = btnY + 2, w = 70, h = 20;
        int border = emojiOpen ? 0xFFFBF236 : 0xFF3A3A4E;
        int bg = emojiOpen ? 0xFF4A4010 : 0xFF20202A;
        g.fill(x, y, x + w, y + h, border);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, bg);
        String s = emojiOpen ? "表情 ▲" : "表情 ▼";
        g.drawString(font, s, x + (w - font.width(s)) / 2, y + 6, 0xFFDDEEFF);
    }

    /** 命中的表情下标，-1=未命中。 */
    private int emojiAt(double mx, double my)
    {
        for (int i = 0; i < DuelActions.EMOJI_COUNT; i++)
        {
            int col = i % EMOJI_COLS, row = i / EMOJI_COLS;
            int cx = emojiPanelX + 4 + col * (EMOJI_CELL + 4);
            int cy = emojiPanelY + 4 + row * (EMOJI_CELL + 4);
            if (hit(mx, my, cx, cy, EMOJI_CELL, EMOJI_CELL)) return i;
        }
        return -1;
    }

    /** 表情面板：2 行 × 4 列，悬停金框。 */
    private void renderEmojiPalette(GuiGraphics g, double mx, double my)
    {
        g.fill(emojiPanelX, emojiPanelY, emojiPanelX + emojiPanelW, emojiPanelY + emojiPanelH, 0xFF20202A);
        g.fill(emojiPanelX + 1, emojiPanelY + 1, emojiPanelX + emojiPanelW - 1, emojiPanelY + emojiPanelH - 1, 0xFF15151E);
        for (int i = 0; i < DuelActions.EMOJI_COUNT; i++)
        {
            int col = i % EMOJI_COLS, row = i / EMOJI_COLS;
            int cx = emojiPanelX + 4 + col * (EMOJI_CELL + 4);
            int cy = emojiPanelY + 4 + row * (EMOJI_CELL + 4);
            if (hit(mx, my, cx, cy, EMOJI_CELL, EMOJI_CELL))
            {
                g.fill(cx - 1, cy - 1, cx + EMOJI_CELL + 1, cy + EMOJI_CELL + 1, 0xFFFFE066);
            }
            DuelEmoji.draw(g, i, cx + 3, cy + 3, EMOJI_CELL - 6);
        }
    }

    /** 表情气泡：sender 侧位为 0/1 → 显示在对应战场上方；-1（观战者）→ 面板顶部居中。 */
    private void renderBubbles(GuiGraphics g)
    {
        if (bubbles.isEmpty()) return;
        for (Bubble b : bubbles)
        {
            int cx = centerX0 + centerW0() / 2;
            int by, border;
            if (b.side == v.mySide)
            {
                by = purpleY0 + 4;
                border = 0xFFE8D54A;
            }
            else if (b.side == 1 - v.mySide)
            {
                by = brownY0 + 4;
                border = 0xFFE05A4A;
            }
            else
            {
                by = cTopY0 + 4;
                border = 0xFF9A9AB0;
            }
            by -= (int) Math.min(14, b.age * 0.3f);   // 上浮
            int alpha = 255;
            if (b.age > BUBBLE_LIFE - BUBBLE_FADE)
            {
                alpha = 255 - 255 * (b.age - (BUBBLE_LIFE - BUBBLE_FADE)) / BUBBLE_FADE;
            }
            int size = 22;
            int w = size + 10, h = size + 8;
            int x0 = cx - w / 2;
            int a = alpha << 24;
            g.fill(x0, by, x0 + w, by + h, a | 0x20202A);
            g.fill(x0, by, x0 + w, by + 1, a | border);
            g.fill(x0, by + h - 1, x0 + w, by + h, a | border);
            g.fill(x0, by, x0 + 1, by + h, a | border);
            g.fill(x0 + w - 1, by, x0 + w, by + h, a | border);
            DuelEmoji.draw(g, b.emoji, x0 + 5, by + 4, size, alpha);
        }
    }

    private DuelView.FieldView hitFieldView(List<DuelView.FieldView> field, double mx, double my, int y)
    {
        for (DuelView.FieldView f : field)
        {
            if (hit(mx, my, fieldX(f.slot), y, fieldW, fieldH)) return f;
        }
        return null;
    }

    private int fieldX(int index)
    {
        int total = DuelGame.FIELD_SLOTS * fieldW + (DuelGame.FIELD_SLOTS - 1) * fieldGap;
        return centerX0 + (centerW0() - total) / 2 + index * (fieldW + fieldGap);
    }

    private int handX(int index, int count)
    {
        int total = count * handW + (count - 1) * cardGap;
        return centerX0 + (centerW0() - total) / 2 + index * (handW + cardGap);
    }

    private int centerW0()
    {
        return centerX1 - centerX0;
    }

    private int poolChipsPerRow()
    {
        return Math.max(1, (rightX1 - rightX0 - 12) / (die + dieGap));
    }

    private int poolChipX(int i)
    {
        return rightX0 + 6 + (i % poolChipsPerRow()) * (die + dieGap);
    }

    private int poolChipY(int top, int i)
    {
        return top + 13 + (i / poolChipsPerRow()) * (die + dieGap);
    }

    private static int classColor(CardClass cls)
    {
        return switch (cls)
        {
            case GONG -> 0xFFFF5555;
            case SHOU -> 0xFF55DDFF;
            case MOU -> 0xFFCC88FF;
            case DING -> 0xFFFFCC66;
        };
    }

    /** 抓取计划数组 → "1/2/1"（界面展示用）。 */
    private static String join(int[] a)
    {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < a.length; i++)
        {
            if (i > 0) sb.append("/");
            sb.append(a[i]);
        }
        return sb.toString();
    }

    private static void fill(GuiGraphics g, int x0, int y0, int x1, int y1, int color)
    {
        if (x1 <= x0 || y1 <= y0) return;
        g.fill(x0, y0, x1, y1, color);
    }

    /** 按字符宽度硬换行（中文无空格）。 */
    private static List<String> wrap(String s, int maxChars)
    {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (char ch : s.toCharArray())
        {
            if (cur.length() >= maxChars)
            {
                out.add(cur.toString());
                cur = new StringBuilder();
            }
            cur.append(ch);
        }
        if (cur.length() > 0) out.add(cur.toString());
        return out;
    }

    private String hintText()
    {
        if (v.spectate)
        {
            return "观战模式：正在收看「" + v.displayMyName() + " vs " + v.displayOppName()
                    + "」，双方战场与操作透明可见，无法操作对战。点底部[退出观战]或按 ESC 离开。";
        }
        // 本轮得分拆解与操作提示合并为单行（左侧 log 区已不承载提示，此处仅用于结算/结束阶段提示）
        switch (v.phase)
        {
            case ROUND_END ->
            {
                return "本轮 你: " + v.lastBase + " × " + v.lastMult + " + " + v.lastExtra
                        + " = " + v.myLastTotal + "　对手: " + v.oppBase + " × " + v.oppMult + " + " + v.oppExtra
                        + " = " + v.oppLastTotal + "　点[下一轮]继续";
            }
            case FINISHED ->
            {
                return (v.winnerLast == v.mySide ? "你获胜！" : (v.winnerLast == 1 - v.mySide ? "你落败" : "平局"))
                        + (v.darkMode ? "　黑暗对决：胜者夺得败者一张卡" : "")
                        + "　点[再来一局]可重新开局";
            }
            default -> { return ""; }
        }
    }

    private static String phaseName(DuelGame.Phase p)
    {
        return switch (p)
        {
            case DEPLOY -> "部署";
            case DRAFT -> "抢骰";
            case PLACE -> "布置";
            case ROUND_END -> "结算";
            case FINISHED -> "对局结束";
            default -> "准备";
        };
    }
}
