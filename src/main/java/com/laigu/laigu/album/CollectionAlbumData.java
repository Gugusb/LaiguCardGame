package com.laigu.laigu.album;

import com.laigu.laigu.util.CardNbt;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 收藏册内容存储：直接读写收藏册物品 NBT（{@link #TAG_ALBUM} 为 卡牌id → 卡牌完整 NBT 的复合表）。
 * <p>
 * 每个卡槽唯一对应一张卡（cardId）。放进去的是玩家实物卡——背包消耗、完整实例数据
 * （唯一编号/获得日期/胜利次数等）原样留在收藏册里；取回时完整还原。
 * <p>
 * 收藏册在放入第一张卡牌时会**署名**为这张卡牌的所属玩家（{@link #signOwner}），
 * 署名后只能放入该玩家的卡牌（服务端在放置时校验，见 {@code CollectionAlbumActionC2SPacket}）。
 */
public final class CollectionAlbumData
{
    /** 收藏册 NBT 根键 */
    public static final String TAG_ALBUM = "laigu.album";
    /** 署名：所属玩家 UUID（字符串）；未署名则无此键 */
    public static final String TAG_OWNER_UUID = "laigu.album_owner_uuid";
    /** 署名：所属玩家名（展示用） */
    public static final String TAG_OWNER_NAME = "laigu.album_owner_name";

    private CollectionAlbumData()
    {
    }

    /** 收藏册所属玩家 UUID 字符串；未署名返回 null。 */
    public static String ownerUuidOf(ItemStack album)
    {
        if (album.isEmpty() || album.getTag() == null)
        {
            return null;
        }
        CompoundTag tag = album.getTag();
        return tag.contains(TAG_OWNER_UUID) ? tag.getString(TAG_OWNER_UUID) : null;
    }

    /** 收藏册所属玩家名；未署名返回 null。 */
    public static String ownerNameOf(ItemStack album)
    {
        if (album.isEmpty() || album.getTag() == null)
        {
            return null;
        }
        CompoundTag tag = album.getTag();
        return tag.contains(TAG_OWNER_NAME) ? tag.getString(TAG_OWNER_NAME) : null;
    }

    /**
     * 收藏册署名：写入第一张放入卡牌的所属玩家（UUID + 名字）。
     * 卡牌无所有者时署名失败（保持未署名状态）。返回是否成功署名。
     */
    public static boolean signOwner(ItemStack album, ItemStack card)
    {
        if (album.isEmpty() || card.isEmpty())
        {
            return false;
        }
        UUID uuid = CardNbt.ownerUuidOf(card);
        if (uuid == null)
        {
            return false;
        }
        CompoundTag tag = album.getOrCreateTag();
        tag.putString(TAG_OWNER_UUID, uuid.toString());
        String name = CardNbt.ownerOf(card);
        if (name != null && !name.isEmpty())
        {
            tag.putString(TAG_OWNER_NAME, name);
        }
        return true;
    }

    /** 已收集卡牌：cardId → 完整卡牌 ItemStack（含实例数据）。 */
    public static Map<String, ItemStack> storedCards(ItemStack album)
    {
        Map<String, ItemStack> out = new LinkedHashMap<>();
        if (album.isEmpty() || album.getTag() == null)
        {
            return out;
        }
        CompoundTag root = album.getTag().getCompound(TAG_ALBUM);
        for (String key : root.getAllKeys())
        {
            ItemStack card = ItemStack.of(root.getCompound(key));
            if (!card.isEmpty())
            {
                out.put(key, card);
            }
        }
        return out;
    }

    /** 某格槽位上的卡牌；未收集返回 EMPTY。 */
    public static ItemStack cardIn(ItemStack album, String cardId)
    {
        if (album.isEmpty() || album.getTag() == null)
        {
            return ItemStack.EMPTY;
        }
        CompoundTag root = album.getTag().getCompound(TAG_ALBUM);
        return root.contains(cardId) ? ItemStack.of(root.getCompound(cardId)) : ItemStack.EMPTY;
    }

    /** 放入一张卡牌到其对应槽位；槽位已占用返回 false。 */
    public static boolean putCard(ItemStack album, String cardId, ItemStack card)
    {
        if (album.isEmpty() || card.isEmpty())
        {
            return false;
        }
        CompoundTag tag = album.getOrCreateTag();
        CompoundTag root = tag.getCompound(TAG_ALBUM);
        if (root.contains(cardId))
        {
            return false; // 该槽位已收集，不覆盖
        }
        root.put(cardId, card.save(new CompoundTag()));
        tag.put(TAG_ALBUM, root);
        return true;
    }

    /** 从收藏册取回某槽位卡牌（移除并返回该卡）；无此卡返回 EMPTY。 */
    public static ItemStack takeCard(ItemStack album, String cardId)
    {
        if (album.isEmpty() || album.getTag() == null)
        {
            return ItemStack.EMPTY;
        }
        CompoundTag tag = album.getTag();
        CompoundTag root = tag.getCompound(TAG_ALBUM);
        if (!root.contains(cardId))
        {
            return ItemStack.EMPTY;
        }
        ItemStack card = ItemStack.of(root.getCompound(cardId));
        root.remove(cardId);
        tag.put(TAG_ALBUM, root);
        return card;
    }

    /** 某朝代是否已集齐（该朝代全部卡牌都已放入收藏册）。 */
    public static boolean dynastyComplete(ItemStack album, String dynasty)
    {
        if (album.isEmpty())
        {
            return false;
        }
        Map<String, ItemStack> stored = storedCards(album);
        for (String cardId : AlbumPages.cardIdsOf(dynasty))
        {
            if (!stored.containsKey(cardId))
            {
                return false;
            }
        }
        return true;
    }

    /** 某朝代已收集张数。 */
    public static int collectedCount(ItemStack album, String dynasty)
    {
        if (album.isEmpty())
        {
            return 0;
        }
        Map<String, ItemStack> stored = storedCards(album);
        int n = 0;
        for (String cardId : AlbumPages.cardIdsOf(dynasty))
        {
            if (stored.containsKey(cardId))
            {
                n++;
            }
        }
        return n;
    }
}
