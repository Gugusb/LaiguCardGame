package com.laigu.laigu.client;

import com.laigu.laigu.Laigu;
import com.laigu.laigu.block.CardExchangeTableBlockEntity;
import com.laigu.laigu.container.CardExchangeMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * 卡牌交换台 GUI：A/B 两侧卡牌槽 + 「确认交换」按钮 + 玩家背包。
 * <p>
 * 按钮走原版按钮协议（{@code MultiPlayerGameMode.handleInventoryButtonClick}
 * → ServerboundContainerButtonClickPacket → 服务端 menu.clickMenuButton）。
 * 确认/已确认/交换完成三种状态由菜单同步数据（ContainerData）驱动。
 */
@OnlyIn(Dist.CLIENT)
public class CardExchangeScreen extends AbstractContainerScreen<CardExchangeMenu>
{
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Laigu.MODID, "textures/gui/card_exchange.png");

    private Button confirmButton;

    public CardExchangeScreen(CardExchangeMenu menu, Inventory playerInventory, Component title)
    {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void init()
    {
        super.init();
        this.confirmButton = Button.builder(Component.translatable("button.laigu.confirm_swap"),
                        button -> this.minecraft.gameMode.handleInventoryButtonClick(
                                this.menu.containerId, CardExchangeMenu.CONFIRM_BUTTON_ID))
                .bounds(this.leftPos + 66, this.topPos + 54, 44, 18)
                .build();
        this.addRenderableWidget(this.confirmButton);
        this.refreshButtonState();
    }

    @Override
    public void containerTick()
    {
        super.containerTick();
        this.refreshButtonState();
    }

    /** 依据同步数据刷新按钮文案/可用态：交换前「确认交换」→ 本人已确认「已确认·点击取消」→ 交换完成禁用。 */
    private void refreshButtonState()
    {
        if (this.confirmButton == null)
        {
            return;
        }
        int mySide = this.menu.getPlayerSide();

        // 自愈：按钮状态除了读同步数据，还以客户端槽位内容（经 ContainerSetSlot 可靠同步）兜底，
        // 避免同步数据偶发延迟/丢包时按钮卡在错误外观（被点击/卡住的样子）。
        //  1) 自己这侧没卡牌就不可能处于已确认态（服务端确认需卡牌，取卡即自动取消）→ 强制未确认
        //  2) 双方槽位都空 = 服务端 tryReset 已把交换态清零 → 强制视为未交换
        int confirmed = mySide == 0
                ? this.menu.getData().get(CardExchangeTableBlockEntity.DATA_CONFIRMED_A)
                : this.menu.getData().get(CardExchangeTableBlockEntity.DATA_CONFIRMED_B);
        if (mySide >= 0 && this.menu.getSlot(mySide).getItem().isEmpty())
        {
            confirmed = 0;
        }
        boolean swapped = this.menu.getData().get(CardExchangeTableBlockEntity.DATA_SWAPPED) == 1;
        if (swapped
                && this.menu.getSlot(CardExchangeTableBlockEntity.SLOT_A).getItem().isEmpty()
                && this.menu.getSlot(CardExchangeTableBlockEntity.SLOT_B).getItem().isEmpty())
        {
            swapped = false;
        }

        if (swapped)
        {
            // 交换完成：保持可点（禁用 = 原版按下贴图，会像卡住）；点击由服务端校验忽略
            this.confirmButton.setMessage(Component.translatable("button.laigu.swap_done"));
            this.confirmButton.active = true;
        }
        else if (confirmed == 1)
        {
            // 已确认：保持可点，再点一次 = 取消确认（避免禁用后像卡住）
            this.confirmButton.setMessage(Component.translatable("button.laigu.cancel_swap"));
            this.confirmButton.active = true;
        }
        else
        {
            this.confirmButton.setMessage(Component.translatable("button.laigu.confirm_swap"));
            this.confirmButton.active = true;
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick)
    {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY)
    {
        // 贴图为 176×166（非 256×256），必须显式传入纹理尺寸（同卡袋屏）
        guiGraphics.blit(TEXTURE, this.leftPos - 1, this.topPos - 1, 0, 0,
                this.imageWidth, this.imageHeight, this.imageWidth, this.imageHeight);

        // 确认态：两侧槽位亮金色高亮，双方各自的确认情况都直观可见
        drawSlotGlow(guiGraphics, CardExchangeTableBlockEntity.SLOT_A);
        drawSlotGlow(guiGraphics, CardExchangeTableBlockEntity.SLOT_B);

        // 两侧槽位下方标注占位者（我的那一侧高亮金色）
        int mySide = this.menu.getPlayerSide();
        drawSideLabel(guiGraphics, CardExchangeTableBlockEntity.SLOT_A, mySide);
        drawSideLabel(guiGraphics, CardExchangeTableBlockEntity.SLOT_B, mySide);
    }

    /** 某侧确认后给槽位画亮金色高亮（半透明填充 + 边框），对方侧确认也能看到。 */
    private void drawSlotGlow(GuiGraphics gui, int side)
    {
        int confirmed = this.menu.getData().get(
                side == CardExchangeTableBlockEntity.SLOT_A
                        ? CardExchangeTableBlockEntity.DATA_CONFIRMED_A
                        : CardExchangeTableBlockEntity.DATA_CONFIRMED_B);
        if (confirmed != 1)
        {
            return;
        }
        Slot slot = this.menu.getSlot(side);
        int x = this.leftPos + slot.x;
        int y = this.topPos + slot.y;
        gui.fill(x, y, x + 16, y + 16, 0x60FFD700);       // 半透明亮金填充
        gui.fill(x, y, x + 16, y + 1, 0xFFFFD700);        // 上边框
        gui.fill(x, y + 15, x + 16, y + 16, 0xFFFFD700);  // 下边框
        gui.fill(x, y, x + 1, y + 16, 0xFFFFD700);        // 左边框
        gui.fill(x + 15, y, x + 16, y + 16, 0xFFFFD700);  // 右边框
    }

    private void drawSideLabel(GuiGraphics gui, int side, int mySide)
    {
        String name = this.menu.getTable().nameOfSide(side);
        if (name == null)
        {
            return;
        }
        int color = side == mySide ? 0xFFFFD700 : 0xFFB0B0B0;
        int x = this.leftPos + (side == CardExchangeTableBlockEntity.SLOT_A ? 55 : 89);
        gui.drawString(this.font, name, x, this.topPos + 44, color, false);
    }
}
