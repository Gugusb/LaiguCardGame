package com.laigu.laigu.network;

import com.laigu.laigu.block.DuelTableBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 客户端 → 服务端：对局操作。action 为 {@link com.laigu.laigu.duel.DuelActions} 之一，
 * a/b 为参数（手牌/槽位/骰池下标）。服务端权威校验后广播最新状态。
 */
public class DuelActionC2SPacket
{
    private final BlockPos pos;
    private final int action;
    private final int a;
    private final int b;

    public DuelActionC2SPacket(BlockPos pos, int action, int a, int b)
    {
        this.pos = pos;
        this.action = action;
        this.a = a;
        this.b = b;
    }

    public static void encode(DuelActionC2SPacket msg, FriendlyByteBuf buf)
    {
        buf.writeBlockPos(msg.pos);
        buf.writeInt(msg.action);
        buf.writeInt(msg.a);
        buf.writeInt(msg.b);
    }

    public static DuelActionC2SPacket decode(FriendlyByteBuf buf)
    {
        return new DuelActionC2SPacket(buf.readBlockPos(), buf.readInt(), buf.readInt(), buf.readInt());
    }

    public static void handle(DuelActionC2SPacket msg, Supplier<NetworkEvent.Context> ctx)
    {
        ctx.get().enqueueWork(() ->
        {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            Level level = player.level();
            if (level.isClientSide) return;
            if (level.getBlockEntity(msg.pos) instanceof DuelTableBlockEntity be)
            {
                be.handleAction(player, msg.action, msg.a, msg.b);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
