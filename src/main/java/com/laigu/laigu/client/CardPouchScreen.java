package com.laigu.laigu.client;

import com.laigu.laigu.container.CardPouchMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * 卡袋 GUI：27 格（3×9，等同木桶）卡牌槽 + 玩家背包。
 * 复用原版容器背景 {@code minecraft:textures/gui/container/generic_54.png}（9 列通用布局）。
 */
@OnlyIn(Dist.CLIENT)
public class CardPouchScreen extends AbstractContainerScreen<CardPouchMenu>
{
    private static final ResourceLocation TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/gui/container/generic_54.png");

    public CardPouchScreen(CardPouchMenu menu, Inventory playerInventory, Component title)
    {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 168; // 3 行卡牌 + 3 行背包 + 快捷栏
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick)
    {
        // 与原版容器一致：先画世界暗幕，再画面板，最后渲染物品 tooltip（必须在最上层）。
        // 基类 render() 不调用 renderTooltip，必须在此显式调用，否则悬停不显示物品信息。
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY)
    {
        // generic_54 是 54 格（6 行）大箱子背景：顶部 0~124 含 6 行容器槽格
        // （槽格顶 17/35/53/71/89/107），从贴图 y=126 起才是物品栏标签 + 玩家背包
        // 3 行槽格（139/157/175）+ 快捷栏槽格（197）。卡袋只有 3 行，若一次性画满
        // imageHeight(168)，会把多余的 4~6 行槽格也画出来，看起来像能放 6 行，且
        // 玩家背包槽落在那 3 行上导致下方大面积错位。
        // 与原版 ContainerScreen 一致，分两段 blit：
        //   第一段：标题条 + 3 行卡袋槽格（贴图 0 ~ rows*18+17 = 71）
        //   第二段：贴图 126 起画 96px（物品栏标签 + 玩家背包 3 行 + 快捷栏），
        //           拼在上一段正下方，跳过中间的大箱子多余槽格。
        // 纹理尺寸参数传实际尺寸 256×256（否则整张图被拉伸）。
        int topHeight = this.menu.getRows() * 18 + 17; // 3 行 → 71
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0,
                this.imageWidth, topHeight, 256, 256);
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos + topHeight, 0, 126,
                this.imageWidth, 96, 256, 256);
    }
}
