package com.laigu.laigu.item;

import com.laigu.laigu.card.CardInfo;
import com.laigu.laigu.client.InspectAnimator;
import com.laigu.laigu.duel.DuelCardCatalog;
import com.laigu.laigu.duel.DuelCardData;
import com.laigu.laigu.util.CardNbt;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import java.util.List;
import java.util.function.Consumer;

/**
 * 卡牌物品。每张贴图对应一个独立的 CardItem 实例。
 * <ul>
 *   <li>右键卡牌进入「端详」：物品开始持续使用，第一人称由
 *       {@link InspectAnimator} 驱动端详动画。</li>
 *   <li>tooltip 显示文物朝代与类型（见 {@link CardInfo}）。</li>
 *   <li>可携带 NBT：所有者、附魔光泽（开包产出）。</li>
 * </ul>
 */
public class CardItem extends Item
{
    /** 端详时长（tick）：设为 1 小时，视为「无限」——只要按住右键就一直端详，松手才回落 */
    public static final int INSPECT_DURATION = 72000;

    public CardItem(Properties properties)
    {
        super(properties);
    }

    // ---- 端详（右键查看卡牌） ----

    @Override
    public int getUseDuration(ItemStack stack)
    {
        return INSPECT_DURATION;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack)
    {
        // 不启用原版咀嚼/格挡等动画，端详姿态由客户端钩子接管
        return UseAnim.NONE;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
    {
        player.startUsingItem(hand);
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer)
    {
        consumer.accept(new IClientItemExtensions()
        {
            @Override
            public boolean applyForgeHandTransform(PoseStack poseStack, LocalPlayer player, HumanoidArm arm,
                    ItemStack itemInHand, float partialTick, float equipProcess, float swingProcess)
            {
                return InspectAnimator.apply(poseStack, player, arm, itemInHand, partialTick);
            }
        });
    }

    // ---- 附魔光泽（开包产出时以 NBT 标记） ----

    @Override
    public boolean isFoil(ItemStack stack)
    {
        return CardNbt.isGlinted(stack);
    }

    // ---- tooltip：朝代 / 类型 / 所有者 ----

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag)
    {
        CardInfo info = CardInfo.of(stack);
        tooltip.add(Component.translatable("tooltip.laigu.dynasty", info.dynasty)
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.laigu.type", info.type)
                .withStyle(ChatFormatting.GRAY));

        // 对战效果（来古牌 ↔ 效果索引）。有配置的对战效果时追加显示。
        DuelCardData duel = DuelCardCatalog.of(stack);
        if (duel != null)
        {
            boolean gold = DuelCardData.isGold(stack);
            tooltip.add(Component.literal(duel.cls.displayName + (gold ? " · 金质" : " · 普通"))
                    .withStyle(duel.cls.color));
            // 主效果、激活目标和金卡焕章分别按各自参数规则渲染，避免激活卡复用主效果 p1/p2。
            String cardDesc = duel.activateCap > 0 ? duel.activationDescFor(stack) : duel.descFor(stack);
            tooltip.add(Component.literal(cardDesc)
                    .withStyle(gold ? ChatFormatting.GOLD : ChatFormatting.WHITE));
            if (gold)
            {
                tooltip.add(Component.literal(duel.goldDescFor(stack)).withStyle(ChatFormatting.GOLD));
            }
        }

        String owner = CardNbt.ownerOf(stack);
        if (owner != null && !owner.isEmpty())
        {
            tooltip.add(Component.translatable("tooltip.laigu.owner", owner)
                    .withStyle(ChatFormatting.GOLD));
        }

        // 创造模式拿取/旧版无实例数据的卡：没有唯一编号，标注「获得于创造模式」（斜体灰）
        String uid = CardNbt.uidOf(stack);
        if (uid == null)
        {
            tooltip.add(Component.translatable("tooltip.laigu.creative_obtained")
                    .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        }
        if (uid != null)
        {
            tooltip.add(Component.translatable("tooltip.laigu.uid", shortUid(uid))
                    .withStyle(ChatFormatting.GRAY));
        }
        long obtained = CardNbt.obtainedOf(stack);
        if (obtained > 0)
        {
            tooltip.add(Component.translatable("tooltip.laigu.obtained", formatDate(obtained))
                    .withStyle(ChatFormatting.GRAY));
        }
        int wins = CardNbt.winsOf(stack);
        tooltip.add(Component.translatable("tooltip.laigu.wins", wins)
                .withStyle(ChatFormatting.GRAY));

        super.appendHoverText(stack, level, tooltip, flag);
    }

    /** 唯一编号缩写：纯数字编号取末尾 10 位（12345-67890）；旧 UUID 编号仍取前 8 位。 */
    public static String shortUid(String uid)
    {
        if (uid == null || uid.isEmpty()) return "";
        if (uid.chars().allMatch(Character::isDigit))
        {
            String s = uid;
            if (s.length() > 10) s = s.substring(s.length() - 10);
            return s.length() > 5 ? s.substring(0, s.length() - 5) + "-" + s.substring(s.length() - 5) : s;
        }
        String compact = uid.replace("-", "");
        if (compact.length() < 8) return uid;
        return compact.substring(0, 4) + "-" + compact.substring(4, 8);
    }

    /** 获得日期格式化：yyyy-MM-dd HH:mm（本地时区）。 */
    public static String formatDate(long epochMillis)
    {
        return java.time.Instant.ofEpochMilli(epochMillis)
                .atZone(java.time.ZoneId.systemDefault())
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }
}
