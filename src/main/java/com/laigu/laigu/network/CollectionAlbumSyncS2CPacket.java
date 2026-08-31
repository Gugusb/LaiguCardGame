package com.laigu.laigu.network;

import com.laigu.laigu.client.CollectionAlbumScreen;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 服务端 → 客户端：收藏册每次放置/取回后的最新数据包。
 * 客户端打开着收藏册界面时直接刷新，未打开则仅提示结果消息。
 */
public class CollectionAlbumSyncS2CPacket
{
    private final boolean ok;
    /** 提示消息语言键（空串表示不提示） */
    private final String messageKey;
    private final ItemStack albumStack;

    public CollectionAlbumSyncS2CPacket(boolean ok, String messageKey, ItemStack albumStack)
    {
        this.ok = ok;
        this.messageKey = messageKey == null ? "" : messageKey;
        this.albumStack = albumStack.copy();
    }

    public static void encode(CollectionAlbumSyncS2CPacket msg, FriendlyByteBuf buf)
    {
        buf.writeBoolean(msg.ok);
        buf.writeUtf(msg.messageKey);
        buf.writeItem(msg.albumStack);
    }

    public static CollectionAlbumSyncS2CPacket decode(FriendlyByteBuf buf)
    {
        return new CollectionAlbumSyncS2CPacket(buf.readBoolean(), buf.readUtf(), buf.readItem());
    }

    public static void handle(CollectionAlbumSyncS2CPacket msg, Supplier<NetworkEvent.Context> ctx)
    {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                        () -> () -> CollectionAlbumScreen.handleSync(msg.ok, msg.messageKey, msg.albumStack)));
        ctx.get().setPacketHandled(true);
    }
}
