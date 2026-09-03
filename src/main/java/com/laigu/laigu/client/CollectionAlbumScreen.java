package com.laigu.laigu.client;

import com.laigu.laigu.album.AlbumPages;
import com.laigu.laigu.album.CollectionAlbumData;
import com.laigu.laigu.card.CardInfo;
import com.laigu.laigu.duel.DuelCardCatalog;
import com.laigu.laigu.duel.newcard.CardItemAdapter;
import com.laigu.laigu.duel.newcard.DuelCard;
import com.laigu.laigu.duel.DuelCardData;
import com.laigu.laigu.item.CardItem;
import com.laigu.laigu.network.CollectionAlbumActionC2SPacket;
import com.laigu.laigu.network.ModPackets;
import com.laigu.laigu.registry.ModItems;
import com.laigu.laigu.util.CardNbt;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 收藏册界面：左右双书页。
 * <p>
 * 左页为图鉴：按朝代分页展示卡牌槽位 + 底部「可放入卡」条带；右页为详情：当指针
 * 指向左页某张卡（或条带卡）时，展示该卡的放大图片与完整信息。
 * 导航只有「上朝代 / 下朝代」（当前没有朝代超过一页 12 张，翻页与跳朝代重复，已去掉）。
 * 集齐一个朝代的全部卡 → 左页金色星光闪烁。
 * 所有放置/取回都经 {@link CollectionAlbumActionC2SPacket} 走服务端权威校验。
 */
@OnlyIn(Dist.CLIENT)
public class CollectionAlbumScreen extends Screen
{
    private static final ResourceLocation BOOK =
            ResourceLocation.fromNamespaceAndPath("laigu", "textures/gui/album_book_double.png");

    /** 双书页贴图原始尺寸（绘制时按此比例缩放，保持书不变形） */
    private static final int BOOK_W = 300;
    private static final int BOOK_H = 183;

    // 左页（图鉴页）纸面区域
    private static final int L_X = 14;
    private static final int L_W = 124;

    // 左页：副标题 / 跳朝代按钮
    private static final int SUB_Y = 12;
    private static final int DYN_Y = 22, DYN_W = 34, DYN_H = 9;
    private static final int DYN_X = L_X + (L_W - DYN_W * 2 - 8) / 2;            // 左按钮
    private static final int DYN_NEXT_X = DYN_X + DYN_W + 8;                     // 右按钮

    // 左页：槽位网格 4×3
    private static final int GRID_X = 23;
    private static final int GRID_Y = 36;
    private static final int SLOT_W = 22;
    private static final int SLOT_H = 26;
    private static final int SLOT_DX = 28;
    private static final int SLOT_DY = 30;

    // 左页：底部条带
    private static final int STRIP_X = 23;
    private static final int STRIP_Y = 128;
    private static final int STRIP_H = 30;
    private static final int STRIP_ICON = 16;
    private static final int STRIP_DX = 19;
    private static final int STRIP_SHOW = 5;

    // 左页：常驻操作提示行
    private static final int HINT_Y = 162;

    // 右页（详情页）纸面区域
    private static final int R_X = 162;
    private static final int R_W = 126;
    private static final int IMG_SIZE = 64;
    private static final int IMG_Y = 10;
    private static final int TEXT_X = R_X + 4;
    private static final int TEXT_W = R_W - 8;
    private static final int TEXT_Y = 80;
    private static final int LINE_H = 9;
    private static final int MAX_LINES = 9;

    private int albumSlot;
    private ItemStack albumStack = ItemStack.EMPTY;
    private Map<String, ItemStack> stored = Map.of();
    private final List<AlbumPages.Page> pages = AlbumPages.pages();
    private int pageIndex = 0;
    private int animTick = 0;
    private int stripOffset = 0;

    private double scale = 1.0;
    private int leftPos;
    private int topPos;

    /** 当前指针指向的卡（槽位或条带卡）；null 表示未指向任何卡 */
    private ItemStack hoverCard = null;
    private boolean hoverPlaced = false;
    private String hoverCardId = null;

    public CollectionAlbumScreen(int albumSlot, ItemStack albumStack)
    {
        super(Component.translatable("item.laigu.collection_album"));
        this.albumSlot = albumSlot;
        applyAlbum(albumStack);
    }

    /** 服务端打开包回调：以新数据构造界面并打开。 */
    public static void open(int albumSlot, ItemStack albumStack)
    {
        Minecraft.getInstance().setScreen(new CollectionAlbumScreen(albumSlot, albumStack));
    }

    /** 服务端操作结果回调：打开界面则刷新数据，否则仅提示消息。 */
    public static void handleSync(boolean ok, String messageKey, ItemStack albumStack)
    {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof CollectionAlbumScreen screen)
        {
            screen.applyAlbum(albumStack);
        }
        if (messageKey != null && !messageKey.isEmpty() && mc.player != null)
        {
            mc.player.displayClientMessage(Component.translatable("message.laigu.album." + messageKey), true);
        }
    }

    private void applyAlbum(ItemStack stack)
    {
        this.albumStack = stack.copy();
        this.stored = CollectionAlbumData.storedCards(this.albumStack);
    }

    @Override
    protected void init()
    {
        // 自适应缩放：双书页完整放进窗口
        double s = Math.min((this.width - 8) / (double) BOOK_W, (this.height - 8) / (double) BOOK_H);
        this.scale = Math.max(0.6, Math.min(1.8, s));
        this.leftPos = (int) Math.round((this.width - BOOK_W * this.scale) / 2.0);
        this.topPos = (int) Math.round((this.height - BOOK_H * this.scale) / 2.0);
    }

    @Override
    public boolean isPauseScreen()
    {
        return false;
    }

    @Override
    public void tick()
    {
        animTick++;
    }

    // ================= 渲染 =================

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick)
    {
        this.renderBackground(guiGraphics);

        var pose = guiGraphics.pose();
        pose.pushPose();
        pose.translate(this.leftPos, this.topPos, 0);
        pose.scale((float) this.scale, (float) this.scale, 1.0F);

        // 双书页背景（整张贴图按原始尺寸绘制）
        guiGraphics.blit(BOOK, 0, 0, 0, 0, BOOK_W, BOOK_H, BOOK_W, BOOK_H);

        // 先重置悬停，再按优先级收集：槽位 → 条带卡
        hoverCard = null;
        hoverPlaced = false;
        hoverCardId = null;

        renderLeftPage(guiGraphics, mouseX, mouseY);
        renderRightPage(guiGraphics);

        pose.popPose();

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    /** 左页：标题 / 副标题 / 跳朝代 / 槽位网格 / 底部条带 / 操作提示。 */
    private void renderLeftPage(GuiGraphics guiGraphics, int mouseX, int mouseY)
    {
        int cx = L_X + L_W / 2;
        AlbumPages.Page page = pages.get(pageIndex);
        boolean complete = CollectionAlbumData.dynastyComplete(albumStack, page.dynasty);

        // 副标题：朝代名 + 已收集 x/y [+ ✦ 已集齐 ✦]（小一号，无标题）
        MutableComponent sub = Component.literal(page.dynasty).withStyle(ChatFormatting.WHITE);
        sub.append(" ").append(Component.translatable("album.collected",
                CollectionAlbumData.collectedCount(albumStack, page.dynasty),
                AlbumPages.cardIdsOf(page.dynasty).size()).withStyle(ChatFormatting.GRAY));
        if (complete)
        {
            sub.append(" ").append(Component.translatable("album.complete")
                    .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        }
        drawCenteredScaled(guiGraphics, sub, cx, SUB_Y, 0xFF000000, 0.8F);

        // 跳朝代按钮
        drawMiniButton(guiGraphics, DYN_X, DYN_Y, DYN_W, DYN_H,
                "<" + Component.translatable("album.prev_dyn").getString());
        drawMiniButton(guiGraphics, DYN_NEXT_X, DYN_Y, DYN_W, DYN_H,
                Component.translatable("album.next_dyn").getString() + ">");

        // 槽位网格
        for (int i = 0; i < page.cards.size(); i++)
        {
            String cardId = page.cards.get(i);
            int col = i % 4;
            int row = i / 4;
            int x = GRID_X + col * SLOT_DX;
            int y = GRID_Y + row * SLOT_DY;
            ItemStack card = stored.get(cardId);
            boolean placed = card != null && !card.isEmpty();

            if (placed)
            {
                boolean gold = "gold".equals(CardNbt.rarityOfPath(CardNbt.pathOf(card)));
                guiGraphics.fill(x, y, x + SLOT_W, y + SLOT_H, gold ? 0xAAFFD700 : 0xAA8A6A33);
                guiGraphics.fill(x + 1, y + 1, x + SLOT_W - 1, y + SLOT_H - 1, 0xBB1C1C1C);
                renderItemScaled(guiGraphics, card, x, y, SLOT_W, SLOT_H, 1.0F);
            }
            else
            {
                // 剪影 + 「+」：示意此处可放卡
                guiGraphics.fill(x, y, x + SLOT_W, y + SLOT_H, 0xAA8A6A33);
                guiGraphics.fill(x + 1, y + 1, x + SLOT_W - 1, y + SLOT_H - 1, 0xB02A2A2A);
                guiGraphics.drawString(this.font, "+", x + SLOT_W / 2 - 3, y + SLOT_H / 2 - 4, 0x778A8A8A);
            }

            if (hit(mouseX, mouseY, x, y, SLOT_W, SLOT_H))
            {
                hoverCardId = cardId;
                hoverCard = placed ? card : new ItemStack(ModItems.getCardItem(cardId, "common"));
                hoverPlaced = placed;
            }
        }

        // 集齐闪光 + 页框（左页网格区）
        if (complete)
        {
            renderSparkles(guiGraphics);
            guiGraphics.fill(GRID_X - 3, GRID_Y - 3,
                    GRID_X + 3 * SLOT_DX + SLOT_W + 3, GRID_Y + 2 * SLOT_DY + SLOT_H + 3, 0x55FFD700);
        }

        // 底部条带（会覆盖悬停状态为条带卡）
        renderStrip(guiGraphics, mouseX, mouseY);

        // 常驻操作提示（小一号）
        drawCenteredScaled(guiGraphics, Component.translatable("album.hint").withStyle(ChatFormatting.DARK_GRAY),
                cx, HINT_Y, 0xFF000000, 0.8F);
    }

    /** 右页：放大卡图 + 完整详情；未指向任何卡时显示引导提示。 */
    private void renderRightPage(GuiGraphics guiGraphics)
    {
        if (hoverCard == null)
        {
            // 放大区域画剪影占位
            int ix = R_X + (R_W - IMG_SIZE) / 2;
            guiGraphics.fill(ix - 2, IMG_Y - 2, ix + IMG_SIZE + 2, IMG_Y + IMG_SIZE + 2, 0xAA8A6A33);
            guiGraphics.fill(ix, IMG_Y, ix + IMG_SIZE, IMG_Y + IMG_SIZE, 0x662A2A2A);
            guiGraphics.drawString(this.font, "?", ix + IMG_SIZE / 2 - 3, IMG_Y + IMG_SIZE / 2 - 4, 0x88A8A8A8);
            // 引导文字
            Component hint = Component.translatable("album.hover_hint")
                    .withStyle(ChatFormatting.DARK_GRAY);
            int hx = R_X + (R_W - this.font.width(hint)) / 2;
            guiGraphics.drawString(this.font, hint, hx, TEXT_Y + 4, 0xFF000000);
            return;
        }

        // 放大卡图（已收集渲染实物；未收集渲染剪影）
        int ix = R_X + (R_W - IMG_SIZE) / 2;
        if (hoverPlaced)
        {
            renderItemScaled(guiGraphics, hoverCard, ix, IMG_Y, IMG_SIZE, IMG_SIZE, 4.0F);
        }
        else
        {
            guiGraphics.fill(ix - 2, IMG_Y - 2, ix + IMG_SIZE + 2, IMG_Y + IMG_SIZE + 2, 0xAA8A6A33);
            guiGraphics.fill(ix, IMG_Y, ix + IMG_SIZE, IMG_Y + IMG_SIZE, 0x662A2A2A);
            guiGraphics.drawString(this.font, "?", ix + IMG_SIZE / 2 - 3, IMG_Y + IMG_SIZE / 2 - 4, 0x88A8A8A8);
        }

        // 详情文字
        List<FormattedCharSequence> lines = new ArrayList<>();
        CardInfo info = CardInfo.of(hoverCard);
        lines.addAll(this.font.split(hoverCard.getHoverName().copy().withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                TEXT_W));
        MutableComponent meta = Component.translatable("tooltip.laigu.dynasty", info.dynasty)
                .withStyle(ChatFormatting.GRAY);
        meta.append("  ").append(Component.translatable("tooltip.laigu.type", info.type)
                .withStyle(ChatFormatting.GRAY));
        if (!hoverPlaced)
        {
            meta.append("  ").append(Component.translatable("album.not_collected")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
        lines.addAll(this.font.split(meta, TEXT_W));

        DuelCardData duel = DuelCardCatalog.of(hoverCard);
        if (duel != null)
        {
            boolean gold = DuelCardData.isGold(hoverCard);
            lines.addAll(this.font.split(
                    Component.literal(duel.cls.displayName + (gold ? " · 金质" : " · 普通"))
                            .withStyle(duel.cls.color), TEXT_W));
            // 阶段13：卡面描述优先读新系统（DuelCard.description/goldDescription），未映射时回退旧目录
            String mainLine = "";
            String goldLine = null;
            java.util.Optional<DuelCard> nc = CardItemAdapter.create(hoverCard);
            if (nc.isPresent())
            {
                mainLine = nc.get().description();
                if (gold) goldLine = nc.get().goldDescription();
            }
            if (mainLine.isEmpty())
            {
                mainLine = duel.descFor(hoverCard);
                if (gold) goldLine = duel.goldDesc;
            }
            lines.addAll(this.font.split(Component.literal(mainLine)
                            .withStyle(gold ? ChatFormatting.GOLD : ChatFormatting.WHITE),
                    TEXT_W));
            if (gold && goldLine != null && !goldLine.isEmpty() && !"焕章：无".equals(goldLine))
            {
                lines.addAll(this.font.split(Component.literal(goldLine)
                        .withStyle(ChatFormatting.GOLD), TEXT_W));
            }
        }

        if (hoverPlaced)
        {
            lines.addAll(this.font.split(Component.translatable("album.uid_date",
                    CardItem.shortUid(CardNbt.uidOf(hoverCard)),
                    CardItem.formatDate(CardNbt.obtainedOf(hoverCard))).withStyle(ChatFormatting.GRAY), TEXT_W));
            lines.addAll(this.font.split(Component.translatable("album.wins", CardNbt.winsOf(hoverCard))
                    .withStyle(ChatFormatting.GRAY), TEXT_W));
            lines.addAll(this.font.split(Component.translatable("album.click_take")
                    .withStyle(ChatFormatting.GOLD), TEXT_W));
        }
        else
        {
            boolean owned = ownedCardSlot(hoverCardId) >= 0;
            lines.addAll(this.font.split(Component.translatable(owned ? "album.click_place" : "album.not_owned_short")
                    .withStyle(owned ? ChatFormatting.GOLD : ChatFormatting.DARK_GRAY), TEXT_W));
        }

        int ty = TEXT_Y;
        for (int i = 0; i < lines.size() && i < MAX_LINES; i++)
        {
            guiGraphics.drawString(this.font, lines.get(i), TEXT_X, ty, 0xFFFFFFFF);
            ty += LINE_H;
        }
    }

    /** 槽位内以 16×16 为基准、可再放大渲染卡牌。 */
    private void renderItemScaled(GuiGraphics guiGraphics, ItemStack stack, int x, int y, int w, int h, float inner)
    {
        var pose = guiGraphics.pose();
        pose.pushPose();
        int item = Math.round(16 * inner);
        pose.translate(x + (w - item) / 2.0, y + (h - item) / 2.0, 0);
        pose.scale(inner, inner, 1.0F);
        guiGraphics.renderItem(stack, 0, 0, 0);
        pose.popPose();
    }

    private void renderSparkles(GuiGraphics guiGraphics)
    {
        int count = 6 + (animTick / 20) % 8;
        for (int i = 0; i < count; i++)
        {
            double t = animTick * 0.08 + i * 2.39996;
            float x = GRID_X + (i % 4) * SLOT_DX + SLOT_W / 2.0F + (float) Math.sin(t) * 12;
            float y = GRID_Y + (i % 3) * SLOT_DY + SLOT_H / 2.0F + (float) Math.cos(t * 1.3) * 9;
            int alpha = 120 + (int) (110 * Math.sin(t * 1.7 + i));
            alpha = Math.max(80, Math.min(255, alpha));
            int s = 2 + (i % 2);
            int color = (alpha << 24) | (0xFF << 16) | (0xD7 << 8);
            guiGraphics.fill((int) x, (int) y, (int) x + s, (int) y + s, color);
        }
    }

    private void drawMiniButton(GuiGraphics guiGraphics, int x, int y, int w, int h, String label)
    {
        guiGraphics.fill(x, y, x + w, y + h, 0xAA6B4E2A);
        guiGraphics.drawString(this.font, label, x + 3, y, 0xFFE8D8B0);
    }

    private void drawCentered(GuiGraphics guiGraphics, Component text, int x, int y, int color)
    {
        guiGraphics.drawString(this.font, text, x - this.font.width(text) / 2, y, color);
    }

    /** 居中绘制并整体缩小字号（scale<1），用于空间紧张的文字。 */
    private void drawCenteredScaled(GuiGraphics guiGraphics, Component text, int x, int y, int color, float scale)
    {
        var pose = guiGraphics.pose();
        pose.pushPose();
        pose.translate(x, y, 0);
        pose.scale(scale, scale, 1.0F);
        guiGraphics.drawString(this.font, text, -this.font.width(text) / 2, 0, color);
        pose.popPose();
    }

    /** 底部条带：背包中未放入收藏册的卡牌；悬停/点击时指向右页详情。 */
    private void renderStrip(GuiGraphics guiGraphics, int mouseX, int mouseY)
    {
        List<Integer> owned = collectOwnedUnplaced();
        int total = owned.size();
        int maxOffset = Math.max(0, total - STRIP_SHOW);
        if (stripOffset > maxOffset) stripOffset = maxOffset;
        if (stripOffset < 0) stripOffset = 0;

        guiGraphics.fill(STRIP_X - 6, STRIP_Y, STRIP_X + 4 * STRIP_DX + STRIP_ICON + 6, STRIP_Y + STRIP_H, 0xBB1E1208);
        guiGraphics.fill(STRIP_X - 5, STRIP_Y + 1, STRIP_X + 4 * STRIP_DX + STRIP_ICON + 5, STRIP_Y + STRIP_H - 1, 0x553A2A1A);

        for (int i = 0; i < STRIP_SHOW && stripOffset + i < total; i++)
        {
            int invSlot = owned.get(stripOffset + i);
            ItemStack card = minecraft.player.getInventory().getItem(invSlot);
            int sx = STRIP_X + i * STRIP_DX;
            int sy = STRIP_Y + (STRIP_H - STRIP_ICON) / 2;
            guiGraphics.renderItem(card, sx, sy);
            if (hit(mouseX, mouseY, sx, sy, STRIP_ICON, STRIP_ICON))
            {
                guiGraphics.fill(sx - 1, sy - 1, sx + STRIP_ICON + 1, sy + STRIP_ICON + 1, 0x66FFD700);
                // 覆盖槽位悬停：条带卡视为「未放入」
                hoverCard = card;
                hoverCardId = CardInfo.of(card).cardId;
                hoverPlaced = false;
            }
        }

        if (total > STRIP_SHOW)
        {
            guiGraphics.drawString(this.font, "<", STRIP_X - 5, STRIP_Y + (STRIP_H - 8) / 2, 0xFFE8D8B0);
            guiGraphics.drawString(this.font, ">", STRIP_X + 4 * STRIP_DX + STRIP_ICON - 1, STRIP_Y + (STRIP_H - 8) / 2, 0xFFE8D8B0);
        }
        if (total == 0)
        {
            guiGraphics.drawString(this.font, Component.translatable("album.no_cards").withStyle(ChatFormatting.DARK_GRAY),
                    STRIP_X, STRIP_Y + (STRIP_H - 8) / 2, 0xFF000000);
        }
    }

    // ================= 交互 =================

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        if (button != 0)
        {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        // 跳朝代
        if (hit(mouseX, mouseY, DYN_X, DYN_Y, DYN_W, DYN_H))
        {
            jumpDynasty(-1);
            return true;
        }
        if (hit(mouseX, mouseY, DYN_NEXT_X, DYN_Y, DYN_W, DYN_H))
        {
            jumpDynasty(1);
            return true;
        }

        // 槽位：已收集 → 取回；未收集 → 放入
        AlbumPages.Page page = pages.get(pageIndex);
        for (int i = 0; i < page.cards.size(); i++)
        {
            String cardId = page.cards.get(i);
            int col = i % 4;
            int row = i / 4;
            if (hit(mouseX, mouseY, GRID_X + col * SLOT_DX, GRID_Y + row * SLOT_DY, SLOT_W, SLOT_H))
            {
                if (stored.containsKey(cardId))
                {
                    sendAction(CollectionAlbumActionC2SPacket.ACTION_TAKE, -1, cardId);
                }
                else
                {
                    int invSlot = ownedCardSlot(cardId);
                    if (invSlot >= 0)
                    {
                        sendAction(CollectionAlbumActionC2SPacket.ACTION_PLACE, invSlot, cardId);
                    }
                    else if (minecraft.player != null)
                    {
                        minecraft.player.displayClientMessage(
                                Component.translatable("album.not_owned", CardInfo.of(cardId).cardId), true);
                    }
                }
                return true;
            }
        }

        // 条带卡牌：放入并跳到对应朝代页
        List<Integer> owned = collectOwnedUnplaced();
        for (int i = 0; i < STRIP_SHOW && stripOffset + i < owned.size(); i++)
        {
            int invSlot = owned.get(stripOffset + i);
            int sx = STRIP_X + i * STRIP_DX;
            int sy = STRIP_Y + (STRIP_H - STRIP_ICON) / 2;
            if (hit(mouseX, mouseY, sx, sy, STRIP_ICON, STRIP_ICON))
            {
                ItemStack card = minecraft.player.getInventory().getItem(invSlot);
                String cardId = CardInfo.of(card).cardId;
                jumpToCard(cardId);
                sendAction(CollectionAlbumActionC2SPacket.ACTION_PLACE, invSlot, cardId);
                return true;
            }
        }
        // 条带滚动
        int total = owned.size();
        if (total > STRIP_SHOW)
        {
            if (hit(mouseX, mouseY, STRIP_X - 6, STRIP_Y, 12, STRIP_H)) { stripOffset = Math.max(0, stripOffset - 1); return true; }
            if (hit(mouseX, mouseY, STRIP_X + 4 * STRIP_DX + STRIP_ICON - 6, STRIP_Y, 12, STRIP_H)) { stripOffset = Math.min(total - STRIP_SHOW, stripOffset + 1); return true; }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void jumpDynasty(int dir)
    {
        String cur = pages.get(pageIndex).dynasty;
        int curIdx = -1;
        String[] dyns = new String[pages.size()];
        int[] first = new int[pages.size()];
        int n = 0;
        for (int i = 0; i < pages.size(); i++)
        {
            String d = pages.get(i).dynasty;
            if (i == 0 || !d.equals(pages.get(i - 1).dynasty))
            {
                dyns[n] = d;
                first[n] = i;
                n++;
            }
            if (d.equals(cur)) curIdx = n - 1;
        }
        int t = curIdx + dir;
        if (t < 0 || t >= n) return;
        pageIndex = first[t];
        stripOffset = 0;
    }

    private void sendAction(int action, int invSlot, String cardId)
    {
        ModPackets.CHANNEL.sendToServer(new CollectionAlbumActionC2SPacket(albumSlot, action, invSlot, cardId));
    }

    private void jumpToCard(String cardId)
    {
        String dyn = AlbumPages.dynastyOf(cardId);
        for (int i = 0; i < pages.size(); i++)
        {
            if (pages.get(i).dynasty.equals(dyn) && pages.get(i).cards.contains(cardId))
            {
                pageIndex = i;
                return;
            }
        }
    }

    private boolean hit(double mx, double my, int x, int y, int w, int h)
    {
        double lx = (mx - this.leftPos) / this.scale;
        double ly = (my - this.topPos) / this.scale;
        return lx >= x && lx < x + w && ly >= y && ly < y + h;
    }

    /** 背包中该卡的槽位（未持有返回 -1）。 */
    private int ownedCardSlot(String cardId)
    {
        if (minecraft.player == null) return -1;
        var inv = minecraft.player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++)
        {
            ItemStack s = inv.getItem(i);
            if (s.getItem() instanceof CardItem && CardInfo.of(s).cardId.equals(cardId))
            {
                return i;
            }
        }
        return -1;
    }

    /** 背包中所有「未放入收藏册」的卡牌槽位列表。 */
    private List<Integer> collectOwnedUnplaced()
    {
        List<Integer> out = new ArrayList<>();
        if (minecraft.player == null) return out;
        var inv = minecraft.player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++)
        {
            ItemStack s = inv.getItem(i);
            if (s.getItem() instanceof CardItem)
            {
                String cardId = CardInfo.of(s).cardId;
                if (!stored.containsKey(cardId))
                {
                    out.add(i);
                }
            }
        }
        return out;
    }
}
