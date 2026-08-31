package com.laigu.laigu.client;

import com.laigu.laigu.container.DeckBoxContainer;
import com.laigu.laigu.container.DuelTableMenu;
import com.laigu.laigu.duel.DuelActions;
import com.laigu.laigu.duel.DuelCardCatalog;
import com.laigu.laigu.duel.DuelCardData;
import com.laigu.laigu.duel.DuelGame;
import com.laigu.laigu.item.DeckBoxItem;
import com.laigu.laigu.network.DuelActionC2SPacket;
import com.laigu.laigu.network.ModPackets;
import com.laigu.laigu.util.CardNbt;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 对战方块登记界面：展示手持卡组包的预设卡组（16 张只读槽位）+ 完整性校验结果，
 * 点「确定登记」提交。卡组须满 16 张合法卡（同名限一、全有对战效果）才可提交。
 * 第一个提交的玩家成为主机，进入战斗设置界面；其余玩家等待主机开始。
 */
@OnlyIn(Dist.CLIENT)
public class DuelTableScreen extends AbstractContainerScreen<DuelTableMenu>
{
    private Button confirmButton;

    public DuelTableScreen(DuelTableMenu menu, Inventory inv, Component title)
    {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void init()
    {
        super.init();
        int y = topPos + 54;
        confirmButton = Button.builder(Component.literal("确定登记"),
                b -> ModPackets.CHANNEL.sendToServer(
                        new DuelActionC2SPacket(menu.pos, DuelActions.REGISTER_CONFIRM, 0, 0)))
                .bounds(leftPos + imageWidth - 90 - 14, y, 90, 20).build();
        addRenderableWidget(confirmButton);
    }

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mx, int my)
    {
        renderBackground(g);
        g.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xDD101018);
        g.fill(leftPos + 1, topPos + 1, leftPos + imageWidth - 1, topPos + imageHeight - 1, 0xDD1A1A26);
        // 卡组区背景框（8×2）
        g.fill(leftPos + 5, topPos + 13, leftPos + 153, topPos + 50, 0x33101018);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt)
    {
        renderBg(g, pt, mx, my);
        super.render(g, mx, my, pt);
        g.drawString(font, "对战登记", leftPos + 14, topPos + 5, 0xFFFFFFFF);
        drawDeckStatus(g);
        renderTooltip(g, mx, my);
    }

    /** 客户端判创造卡组：查玩家主手物品（客户端菜单没有卡组包）。 */
    private boolean isCreativeDeck()
    {
        if (this.minecraft != null && this.minecraft.player != null)
        {
            ItemStack held = this.minecraft.player.getMainHandItem();
            return held.getItem() instanceof DeckBoxItem di && di.creative;
        }
        return false;
    }

    /** 卡组完整性校验 + 卡牌清单状态展示（按钮下方两行，避免遮挡槽位/按钮）。 */
    private void drawDeckStatus(GuiGraphics g)
    {
        List<ItemStack> deck = new ArrayList<>();
        for (int i = 0; i < DeckBoxContainer.SLOT_COUNT && i < menu.slots.size(); i++)
        {
            ItemStack s = menu.slots.get(i).getItem();
            if (!s.isEmpty()) deck.add(s);
        }
        int count = deck.size();
        boolean creative = isCreativeDeck();
        confirmButton.active = creative ? count > 0 : count == DuelGame.DECK_SIZE;
        if (creative)
        {
            g.drawString(font, "创造模式 · " + count + " 张（无视规则）", leftPos + 14, topPos + 77, count > 0 ? 0xFF55FF55 : 0xFFFF5555);
            return;
        }

        List<String> problems = new ArrayList<>();
        if (count < DuelGame.DECK_SIZE)
        {
            problems.add("还差 " + (DuelGame.DECK_SIZE - count) + " 张");
        }
        int invalid = 0;
        Set<String> seen = new HashSet<>();
        List<String> dupNames = new ArrayList<>();
        for (ItemStack s : deck)
        {
            if (DuelCardCatalog.of(s) == null)
            {
                invalid++;
                continue;
            }
            String key = DeckBoxContainer.itemKeyOf(s);
            if (!seen.add(key))
            {
                DuelCardData dd = DuelCardCatalog.byId(CardNbt.stripRaritySuffix(key));
                dupNames.add(dd != null ? dd.name : key);
            }
        }
        if (invalid > 0) problems.add("含无效卡 " + invalid + " 张");
        if (!dupNames.isEmpty())
        {
            String joined = "同名重复：" + String.join("、", dupNames);
            problems.add(joined.length() > 16 ? joined.substring(0, 16) + "…" : joined);
        }

        int y = topPos + 77;
        if (problems.isEmpty())
        {
            g.drawString(font, "卡组完整 · " + count + " 张合法，可提交", leftPos + 14, y, 0xFF55FF55);
        }
        else
        {
            g.drawString(font, "卡组不完整：" + problems.get(0), leftPos + 14, y, 0xFFFF5555);
            if (problems.size() > 1)
            {
                StringBuilder sb = new StringBuilder();
                for (int i = 1; i < problems.size(); i++) sb.append("，").append(problems.get(i));
                String second = sb.toString();
                g.drawString(font, second.length() > 18 ? second.substring(0, 18) + "…" : second,
                        leftPos + 14, y + 11, 0xFFFF5555);
            }
        }
    }
}
