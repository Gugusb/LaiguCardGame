package com.laigu.laigu.client;

import com.laigu.laigu.container.DeckBoxContainer;
import com.laigu.laigu.container.DeckBoxMenu;
import com.laigu.laigu.duel.DuelCardCatalog;
import com.laigu.laigu.item.DeckBoxItem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.HashMap;
import java.util.Map;

/**
 * 卡组匣 GUI：16 格（8×2）卡组区 + 玩家背包。
 * 放置时不拦截卡牌（重复/无效果卡也能放入）；点「确认构建」统一检查：
 * 重复卡 / 无效果卡 / 空槽对应槽位背景标红，全部合法（16 张、无重复、全有效）时全亮绿。
 */
@OnlyIn(Dist.CLIENT)
public class DeckBoxScreen extends AbstractContainerScreen<DeckBoxMenu>
{
    private static final int SLOT_COUNT = DeckBoxContainer.SLOT_COUNT;
    // 专用 8 列卡组背景（tools 生成的 deck_box.png，176x150）：卡组区 8×2=16 槽，无原版 9 列幻影槽。
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("laigu", "textures/gui/deck_box.png");

    /** 每槽校验结果（true=合法）；null 表示尚未构建。 */
    private boolean[] validity;
    /** 上次构建时槽位内容指纹；内容变化则校验状态失效。 */
    private String contentKey;
    private String statusText;
    private int statusColor;

    public DeckBoxScreen(DeckBoxMenu menu, Inventory playerInventory, Component title)
    {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 150; // 2 行卡组(顶高 53)+物品栏标签+背包 3 行+快捷栏(96)，同卡袋两段 blit 几何
    }

    @Override
    protected void init()
    {
        super.init();
        // 「确认构建」放标题条右上（卡组槽只占 8 列且从 y18 起，标题条右侧留白；按钮高 14 避免压槽）
        addRenderableWidget(Button.builder(Component.literal("确认构建"),
                b -> onBuildClick())
                .bounds(leftPos + imageWidth - 62, topPos + 2, 54, 14).build());
    }

    /** 点「确认构建」：本地校验红/绿；本地通过后走原生存器按钮协议让服务端权威校验并落「已构建」标记。 */
    private void onBuildClick()
    {
        boolean allValid = validateNow();
        if (allValid && this.minecraft != null && this.minecraft.gameMode != null)
        {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 0);
        }
    }

    /** 客户端判创造卡组：查玩家主手物品（客户端菜单没有卡组包，不能用 menu.isCreative()）。 */
    private boolean isCreativeDeck()
    {
        if (this.minecraft != null && this.minecraft.player != null)
        {
            ItemStack held = this.minecraft.player.getMainHandItem();
            return held.getItem() instanceof DeckBoxItem di && di.creative;
        }
        return false;
    }

    /** 点击「确认构建」：计算每槽合法性与整体状态，更新槽位背景色。返回是否全部合法。 */
    private boolean validateNow()
    {
        if (isCreativeDeck())
        {
            validity = new boolean[SLOT_COUNT];
            java.util.Arrays.fill(validity, true);
            statusText = "创造模式（无规则限制）";
            statusColor = 0xFF55FF55;
            contentKey = fingerprint();
            return true;
        }
        Map<String, Integer> cnt = new HashMap<>();
        ItemStack[] arr = new ItemStack[SLOT_COUNT];
        for (int i = 0; i < SLOT_COUNT; i++)
        {
            ItemStack s = menu.slots.get(i).getItem();
            arr[i] = s;
            if (s.isEmpty()) continue;
            if (DuelCardCatalog.of(s) == null) continue;
            cnt.merge(DeckBoxContainer.itemKeyOf(s), 1, Integer::sum);
        }

        validity = new boolean[SLOT_COUNT];
        int problems = 0;
        for (int i = 0; i < SLOT_COUNT; i++)
        {
            ItemStack s = arr[i];
            boolean ok = false;
            if (!s.isEmpty() && DuelCardCatalog.of(s) != null)
            {
                ok = cnt.getOrDefault(DeckBoxContainer.itemKeyOf(s), 0) == 1;
            }
            if (!ok) problems++;
            validity[i] = ok;
        }

        int filled = 0;
        for (ItemStack s : arr) if (!s.isEmpty()) filled++;
        boolean allValid = problems == 0 && filled == SLOT_COUNT;
        if (allValid)
        {
            statusText = "卡组构建完成";
            statusColor = 0xFF55FF55;
        }
        else
        {
            statusText = "构建未通过（" + problems + " 处）";
            statusColor = 0xFFFF5555;
        }
        contentKey = fingerprint();
        return allValid;
    }

    /** 槽位内容指纹：任一槽变化则旧校验结果失效。 */
    private String fingerprint()
    {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < SLOT_COUNT; i++)
        {
            ItemStack s = menu.slots.get(i).getItem();
            sb.append(s.getDescriptionId()).append('#').append(s.getCount()).append('|');
        }
        return sb.toString();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick)
    {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        if (validity != null && !contentKey.equals(fingerprint()))
        {
            validity = null; // 内容已改动，背景色回到默认
            statusText = null;
        }
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY)
    {
        // 标题居中；构建状态文本居中放在卡组槽下方（替代原版「物品栏」标签位）。
        guiGraphics.drawString(font, this.title,
                leftPos + imageWidth / 2 - font.width(this.title) / 2, topPos + 6, 0x404040);
        if (statusText != null)
        {
            guiGraphics.drawString(font, statusText,
                    leftPos + imageWidth / 2 - font.width(statusText) / 2, topPos + 56, statusColor);
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY)
    {
        // 一次贴满专用 8 列卡组背景（deck_box.png，176x150，已含标题条 + 8×2 卡组槽 + 物品栏标签 + 背包 + 快捷栏）。
        // 纹理尺寸参数传实际 176×150（否则按 256 拉伸只显左上角）。
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0,
                this.imageWidth, this.imageHeight, 176, 150);

        // 卡组槽默认填充由背景的灰槽格承担；这里只绘制「确认构建」红/绿校验覆盖。
        for (int row = 0; row < 2; row++)
        {
            for (int col = 0; col < 8; col++)
            {
                int i = col + row * 8;
                int x = leftPos + 8 + col * 18;
                int y = topPos + 18 + row * 18;
                if (validity != null)
                {
                    int c = validity[i] ? 0xFF55AA55 : 0xFFAA5555;
                    guiGraphics.fill(x, y, x + 16, y + 1, c);        // 上
                    guiGraphics.fill(x, y + 15, x + 16, y + 16, c);  // 下
                    guiGraphics.fill(x, y, x + 1, y + 16, c);        // 左
                    guiGraphics.fill(x + 15, y, x + 16, y + 16, c);  // 右
                    guiGraphics.fill(x + 1, y + 1, x + 15, y + 15,
                            validity[i] ? 0x6622AA22 : 0x66AA2222);
                }
            }
        }
    }
}
