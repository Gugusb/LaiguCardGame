package com.laigu.laigu.network;

import com.laigu.laigu.client.PackOpenAnimator;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 服务端 → 客户端：通知开包玩家播放「卡包炸开」的第一人称动画
 * （镜头前出现 → 原地旋转一圈 → 沿视线飞走，见 {@link PackOpenAnimator}）。
 * 粒子与音效由服务端直接广播给附近玩家，本包只负责触发开包玩家的第一人称动画。
 */
public class PackOpenEffectPacket
{
    private final ItemStack packStack;

    public PackOpenEffectPacket(ItemStack packStack)
    {
        this.packStack = packStack;
    }

    public static void encode(PackOpenEffectPacket msg, FriendlyByteBuf buf)
    {
        buf.writeItem(msg.packStack);
    }

    public static PackOpenEffectPacket decode(FriendlyByteBuf buf)
    {
        return new PackOpenEffectPacket(buf.readItem());
    }

    public static void handle(PackOpenEffectPacket msg, Supplier<NetworkEvent.Context> ctx)
    {
        // 本包仅由服务端下发给开包玩家（客户端方向），安全由发送方保证；
        // 用 unsafeRunWhenOn 跳过 safeRunWhenOn 对 lambda 捕获变量的字节码安全检查（会误报 FATAL 导致动画不播）
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> PackOpenAnimator.start(msg.packStack)));
        ctx.get().setPacketHandled(true);
    }
}
