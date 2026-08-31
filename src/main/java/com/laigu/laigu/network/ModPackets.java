package com.laigu.laigu.network;

import com.laigu.laigu.Laigu;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import com.laigu.laigu.network.DuelActionC2SPacket;
import com.laigu.laigu.network.DuelStateS2CPacket;

/**
 * 模组网络通道。目前仅有一条客户端方向数据包：
 * {@link PackOpenEffectPacket}——开包时让客户端播不死图腾式的第一人称包面放大动画。
 */
public final class ModPackets
{
    private static final String VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(Laigu.MODID, "main"),
            () -> VERSION,
            VERSION::equals,
            VERSION::equals);

    private static int nextId = 0;

    public static void register()
    {
        CHANNEL.registerMessage(nextId++,
                PackOpenEffectPacket.class,
                PackOpenEffectPacket::encode,
                PackOpenEffectPacket::decode,
                PackOpenEffectPacket::handle);
        CHANNEL.registerMessage(nextId++,
                DuelActionC2SPacket.class,
                DuelActionC2SPacket::encode,
                DuelActionC2SPacket::decode,
                DuelActionC2SPacket::handle);
        CHANNEL.registerMessage(nextId++,
                DuelStateS2CPacket.class,
                DuelStateS2CPacket::encode,
                DuelStateS2CPacket::decode,
                DuelStateS2CPacket::handle);
        CHANNEL.registerMessage(nextId++,
                DuelEmojiS2CPacket.class,
                DuelEmojiS2CPacket::encode,
                DuelEmojiS2CPacket::decode,
                DuelEmojiS2CPacket::handle);
        CHANNEL.registerMessage(nextId++,
                CollectionAlbumOpenS2CPacket.class,
                CollectionAlbumOpenS2CPacket::encode,
                CollectionAlbumOpenS2CPacket::decode,
                CollectionAlbumOpenS2CPacket::handle);
        CHANNEL.registerMessage(nextId++,
                CollectionAlbumSyncS2CPacket.class,
                CollectionAlbumSyncS2CPacket::encode,
                CollectionAlbumSyncS2CPacket::decode,
                CollectionAlbumSyncS2CPacket::handle);
        CHANNEL.registerMessage(nextId++,
                CollectionAlbumActionC2SPacket.class,
                CollectionAlbumActionC2SPacket::encode,
                CollectionAlbumActionC2SPacket::decode,
                CollectionAlbumActionC2SPacket::handle);
    }

    private ModPackets()
    {
    }
}
