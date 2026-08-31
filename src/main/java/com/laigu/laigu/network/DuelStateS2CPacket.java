package com.laigu.laigu.network;

import com.laigu.laigu.client.DuelHostSetupScreen;
import com.laigu.laigu.client.DuelScreen;
import com.laigu.laigu.client.DuelTableScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 服务端 → 客户端：全量对局状态同步（一人一份，视角化）。
 * <ul>
 *   <li>ui=1 → 打开主机战斗设置界面（第一个登记卡组的玩家）。</li>
 *   <li>ui=2 → 关闭登记界面，提示等待主机。</li>
 *   <li>其他 → 打开/刷新对战界面（对局开始时）。</li>
 * </ul>
 */
public class DuelStateS2CPacket
{
    private final BlockPos pos;
    private final CompoundTag state;

    public DuelStateS2CPacket(BlockPos pos, CompoundTag state)
    {
        this.pos = pos;
        this.state = state;
    }

    public static void encode(DuelStateS2CPacket msg, FriendlyByteBuf buf)
    {
        buf.writeBlockPos(msg.pos);
        buf.writeNbt(msg.state);
    }

    public static DuelStateS2CPacket decode(FriendlyByteBuf buf)
    {
        BlockPos pos = buf.readBlockPos();
        CompoundTag state = buf.readNbt();
        return new DuelStateS2CPacket(pos, state == null ? new CompoundTag() : state);
    }

    public static void handle(DuelStateS2CPacket msg, Supplier<NetworkEvent.Context> ctx)
    {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                {
                    Minecraft mc = Minecraft.getInstance();
                    if (mc.player == null) return;
                    int ui = msg.state.getInt("ui");
                    if (ui == 1)
                    {
                        mc.setScreen(new DuelHostSetupScreen(msg.pos));
                        return;
                    }
                    if (ui == 2)
                    {
                        if (mc.screen instanceof DuelTableScreen) mc.screen.onClose();
                        return;
                    }
                    if (ui == 3)
                    {
                        // 主机设置界面状态回包：对方（AI/真人）是否已就绪
                        if (mc.screen instanceof DuelHostSetupScreen s)
                        {
                            s.refreshStatus(msg.state.getBoolean("aiReady"),
                                    msg.state.getBoolean("humanJoined"));
                        }
                        return;
                    }
                    if (ui == 4)
                    {
                        // 房间解散（对方离开/认输/逃跑）：直接关闭对战界面，
                        // 不经 onClose，避免再发 LEAVE_ROOM 造成循环。
                        if (mc.screen instanceof DuelScreen) mc.setScreen(null);
                        return;
                    }
                    if (mc.screen instanceof DuelScreen ds && ds.isSamePos(msg.pos))
                    {
                        ds.updateState(msg.state);
                    }
                    else
                    {
                        mc.setScreen(new DuelScreen(msg.pos, msg.state));
                    }
                }));
        ctx.get().setPacketHandled(true);
    }
}
