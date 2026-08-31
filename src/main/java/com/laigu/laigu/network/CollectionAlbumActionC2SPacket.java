package com.laigu.laigu.network;

import com.laigu.laigu.album.CollectionAlbumData;
import com.laigu.laigu.card.CardInfo;
import com.laigu.laigu.item.CardItem;
import com.laigu.laigu.item.CollectionAlbumItem;
import com.laigu.laigu.util.CardNbt;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

/**
 * 客户端 → 服务端：收藏册放置 / 取回操作。
 * <ul>
 *   <li>{@link #ACTION_PLACE}：把背包 {invSlot} 的卡牌放进它对应的槽位（实物消耗，NBT 留存）。</li>
 *   <li>{@link #ACTION_TAKE}：把收藏册里 {cardId} 槽位的卡取回背包。</li>
 * </ul>
 * 服务端权威校验后回发 {@link CollectionAlbumSyncS2CPacket} 同步最新数据。
 */
public class CollectionAlbumActionC2SPacket
{
    public static final int ACTION_PLACE = 0;
    public static final int ACTION_TAKE = 1;

    private final int albumSlot;
    private final int action;
    private final int invSlot;   // ACTION_PLACE 用
    private final String cardId; // ACTION_TAKE 用

    public CollectionAlbumActionC2SPacket(int albumSlot, int action, int invSlot, String cardId)
    {
        this.albumSlot = albumSlot;
        this.action = action;
        this.invSlot = invSlot;
        this.cardId = cardId == null ? "" : cardId;
    }

    public static void encode(CollectionAlbumActionC2SPacket msg, FriendlyByteBuf buf)
    {
        buf.writeVarInt(msg.albumSlot);
        buf.writeVarInt(msg.action);
        buf.writeVarInt(msg.invSlot);
        buf.writeUtf(msg.cardId);
    }

    public static CollectionAlbumActionC2SPacket decode(FriendlyByteBuf buf)
    {
        return new CollectionAlbumActionC2SPacket(buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readUtf());
    }

    public static void handle(CollectionAlbumActionC2SPacket msg, Supplier<NetworkEvent.Context> ctx)
    {
        ctx.get().enqueueWork(() ->
        {
            ServerPlayer player = ctx.get().getSender();
            if (player == null || player.level().isClientSide)
            {
                return;
            }
            Inventory inv = player.getInventory();
            if (msg.albumSlot < 0 || msg.albumSlot >= inv.getContainerSize())
            {
                return;
            }
            ItemStack album = inv.getItem(msg.albumSlot);
            boolean ok = false;
            String key = "";
            if (album.getItem() instanceof CollectionAlbumItem)
            {
                if (msg.action == ACTION_PLACE)
                {
                    ItemStack card = msg.invSlot >= 0 && msg.invSlot < inv.getContainerSize()
                            ? inv.getItem(msg.invSlot) : ItemStack.EMPTY;
                    if (!(card.getItem() instanceof CardItem))
                    {
                        key = "need_card";
                    }
                    else
                    {
                        String cardId = CardInfo.of(card).cardId;
                        if (!CollectionAlbumData.cardIn(album, cardId).isEmpty())
                        {
                            key = "already";
                        }
                        else
                        {
                            // 署名校验：已署名的册只能放入其所属玩家的卡牌
                            String albumOwner = CollectionAlbumData.ownerUuidOf(album);
                            java.util.UUID cardOwner = CardNbt.ownerUuidOf(card);
                            if (albumOwner != null && (cardOwner == null || !cardOwner.toString().equals(albumOwner)))
                            {
                                key = "owner_only";
                            }
                            else
                            {
                                ItemStack stored = card.copy();
                                if (CollectionAlbumData.putCard(album, cardId, stored))
                                {
                                    inv.setItem(msg.invSlot, ItemStack.EMPTY);
                                    // 未署名：放入第一张卡时署名给它的所属玩家
                                    if (albumOwner == null)
                                    {
                                        CollectionAlbumData.signOwner(album, card);
                                    }
                                    ok = true;
                                    key = "placed";
                                }
                                else
                                {
                                    key = "already";
                                }
                            }
                        }
                    }
                }
                else if (msg.action == ACTION_TAKE)
                {
                    ItemStack card = CollectionAlbumData.takeCard(album, msg.cardId);
                    if (!card.isEmpty())
                    {
                        if (!inv.add(card))
                        {
                            player.drop(card, false);
                        }
                        ok = true;
                        key = "taken";
                    }
                    else
                    {
                        key = "taken_fail";
                    }
                }
            }
            else
            {
                key = "lost";
            }
            ModPackets.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                    new CollectionAlbumSyncS2CPacket(ok, key, album));
        });
        ctx.get().setPacketHandled(true);
    }
}
