package com.laigu.laigu.network;

import com.laigu.laigu.client.CollectionAlbumScreen;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 服务端 → 客户端：打开收藏册界面。
 * 携带收藏册在玩家背包里的槽位与完整物品栈（含已收集卡牌 NBT），客户端据此渲染分页界面。
 */
public class CollectionAlbumOpenS2CPacket
{
    private final int albumSlot;
    private final ItemStack albumStack;

    public CollectionAlbumOpenS2CPacket(int albumSlot, ItemStack albumStack)
    {
        this.albumSlot = albumSlot;
        this.albumStack = albumStack.copy();
    }

    public static void encode(CollectionAlbumOpenS2CPacket msg, FriendlyByteBuf buf)
    {
        buf.writeVarInt(msg.albumSlot);
        buf.writeItem(msg.albumStack);
    }

    public static CollectionAlbumOpenS2CPacket decode(FriendlyByteBuf buf)
    {
        return new CollectionAlbumOpenS2CPacket(buf.readVarInt(), buf.readItem());
    }

    public static void handle(CollectionAlbumOpenS2CPacket msg, Supplier<NetworkEvent.Context> ctx)
    {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                        () -> () -> CollectionAlbumScreen.open(msg.albumSlot, msg.albumStack)));
        ctx.get().setPacketHandled(true);
    }
}
