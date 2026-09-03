package com.laigu.laigu.network;

import com.laigu.laigu.client.DuelScreen;
import com.laigu.laigu.duel.newcard.AnimationEvent;
import com.laigu.laigu.duel.newcard.NewAnimationEventPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 新版卡牌动画事件的服务端到客户端增量包。 */
public final class NewAnimationEventS2CPacket
{
    private final BlockPos pos;
    private final NewAnimationEventPacket payload;

    public NewAnimationEventS2CPacket(BlockPos pos, NewAnimationEventPacket payload)
    {
        this.pos = pos;
        this.payload = payload;
    }

    public static void encode(NewAnimationEventS2CPacket packet, FriendlyByteBuf buf)
    {
        buf.writeBlockPos(packet.pos);
        packet.payload.encode(buf);
    }

    public static NewAnimationEventS2CPacket decode(FriendlyByteBuf buf)
    {
        return new NewAnimationEventS2CPacket(buf.readBlockPos(), NewAnimationEventPacket.decode(buf));
    }

    public static void handle(NewAnimationEventS2CPacket packet, Supplier<NetworkEvent.Context> context)
    {
        context.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.screen instanceof DuelScreen screen && screen.isSamePos(packet.pos))
                screen.acceptNewAnimationEvents(packet.payload.events());
        }));
        context.get().setPacketHandled(true);
    }
}
