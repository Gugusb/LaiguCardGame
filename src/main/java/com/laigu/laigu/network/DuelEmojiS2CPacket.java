package com.laigu.laigu.network;

import com.laigu.laigu.client.DuelScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 服务端 → 客户端：对局内表情气泡。
 * side 为绝对侧位（0/1）；观战者发送时为 -1（双方屏幕上显示为中立气泡）。
 * 只有对战界面开着时才在屏内展示，双方玩家与观战者都收得到。
 */
public class DuelEmojiS2CPacket
{
    private final BlockPos pos;
    private final int side;
    private final int emoji;

    public DuelEmojiS2CPacket(BlockPos pos, int side, int emoji)
    {
        this.pos = pos;
        this.side = side;
        this.emoji = emoji;
    }

    public static void encode(DuelEmojiS2CPacket msg, FriendlyByteBuf buf)
    {
        buf.writeBlockPos(msg.pos);
        buf.writeInt(msg.side);
        buf.writeInt(msg.emoji);
    }

    public static DuelEmojiS2CPacket decode(FriendlyByteBuf buf)
    {
        return new DuelEmojiS2CPacket(buf.readBlockPos(), buf.readInt(), buf.readInt());
    }

    public static void handle(DuelEmojiS2CPacket msg, Supplier<NetworkEvent.Context> ctx)
    {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                {
                    Minecraft mc = Minecraft.getInstance();
                    if (mc.player == null) return;
                    if (mc.screen instanceof DuelScreen ds && ds.isSamePos(msg.pos))
                    {
                        ds.onEmoji(msg.side, msg.emoji);
                    }
                }));
        ctx.get().setPacketHandled(true);
    }
}
