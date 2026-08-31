package com.laigu.laigu.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 卡牌 NBT 读写工具。
 * <p>
 * 卡牌物品上会携带额外数据：
 * <ul>
 *   <li>所有者（开包时标记，tooltip 显示）</li>
 *   <li>附魔光泽（开包时有 1% 概率触发，{@code isFoil} 渲染）</li>
 *   <li>实例数据（{@link #ensureInstance}）：唯一编号 / 获得日期 / 参战胜利次数</li>
 * </ul>
 */
public final class CardNbt
{
    public static final String TAG_OWNER = "laigu.owner";
    public static final String TAG_OWNER_UUID = "laigu.owner_uuid";
    public static final String TAG_GLINT = "laigu.glint";
    /** 卡牌实例唯一编号（纯数字字符串，开包/获得时生成，永不重复） */
    public static final String TAG_UID = "laigu.uid";
    /** 编号自增计数器（与时间戳合并成纯数字编号，同毫秒内也不重复） */
    private static final AtomicLong UID_COUNTER = new AtomicLong(0);
    /** 获得日期（epoch millis，long） */
    public static final String TAG_OBTAINED = "laigu.obtained";
    /** 该卡参与过的战斗的胜利次数（int，获胜方部署过的卡每胜一场 +1） */
    public static final String TAG_WINS = "laigu.wins";

    private CardNbt()
    {
    }

    /** 是否带附魔光泽。 */
    public static boolean isGlinted(ItemStack stack)
    {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.getBoolean(TAG_GLINT);
    }

    /** 设置/取消附魔光泽。 */
    public static void setGlinted(ItemStack stack, boolean glint)
    {
        stack.getOrCreateTag().putBoolean(TAG_GLINT, glint);
    }

    /** 标记所有者（玩家名 + UUID）。 */
    public static void setOwner(ItemStack stack, Player player)
    {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putString(TAG_OWNER, player.getGameProfile().getName());
        tag.putUUID(TAG_OWNER_UUID, player.getUUID());
    }

    /** 按「玩家名 + UUID」标记所有者（交换台换主后重写；字段可为 null 则跳过）。 */
    public static void setOwnerBy(ItemStack stack, String name, UUID uuid)
    {
        CompoundTag tag = stack.getOrCreateTag();
        if (name != null)
        {
            tag.putString(TAG_OWNER, name);
        }
        if (uuid != null)
        {
            tag.putUUID(TAG_OWNER_UUID, uuid);
        }
    }

    /** 读取所有者 UUID；无则返回 null。 */
    public static UUID ownerUuidOf(ItemStack stack)
    {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TAG_OWNER_UUID))
        {
            return null;
        }
        return tag.getUUID(TAG_OWNER_UUID);
    }

    /** 该卡牌是否属于某玩家（按 UUID 判断，容忍改名）。 */
    public static boolean isOwnedBy(ItemStack stack, Player player)
    {
        UUID uuid = ownerUuidOf(stack);
        return uuid != null && uuid.equals(player.getUUID());
    }

    /** 物品注册 id 的路径部分（如 {@code qian_li_jiang_shan_common}）；未知返回空串。 */
    public static String pathOf(ItemStack stack)
    {
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return key != null ? key.getPath() : "";
    }

    /** 读取所有者玩家名；无则返回 null。 */
    public static String ownerOf(ItemStack stack)
    {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TAG_OWNER))
        {
            return null;
        }
        return tag.getString(TAG_OWNER);
    }

    /** 从物品 id 路径（如 {@code qian_li_jiang_shan_common}）剥离稀有度后缀，得到卡牌 id。 */
    public static String stripRaritySuffix(String itemPath)
    {
        if (itemPath.endsWith("_gold"))
        {
            return itemPath.substring(0, itemPath.length() - 5);
        }
        if (itemPath.endsWith("_common"))
        {
            return itemPath.substring(0, itemPath.length() - 7);
        }
        return itemPath;
    }

    /** 从物品 id 路径读取稀有度（"common"/"gold"），非卡牌返回 null。 */
    public static String rarityOfPath(String itemPath)
    {
        if (itemPath.endsWith("_gold"))
        {
            return "gold";
        }
        if (itemPath.endsWith("_common"))
        {
            return "common";
        }
        return null;
    }

    // ================= 实例数据：唯一编号 / 获得日期 / 参战胜利次数 =================

    /** 为卡牌补全实例数据（唯一编号 + 获得日期 + 胜利次数）。旧卡/创造拿取未标注时补全。 */
    public static void ensureInstance(ItemStack stack)
    {
        if (stack == null || stack.isEmpty()) return;
        CompoundTag tag = stack.getOrCreateTag();
        if (!tag.contains(TAG_UID))
        {
            // 纯数字编号：高 42 位 = 时间戳毫秒，低 22 位 = 自增计数（同毫秒内也不重复）
            long n = (System.currentTimeMillis() << 22) | (UID_COUNTER.incrementAndGet() & 0x3FFFFF);
            tag.putString(TAG_UID, Long.toString(n));
        }
        if (!tag.contains(TAG_OBTAINED))
        {
            tag.putLong(TAG_OBTAINED, System.currentTimeMillis());
        }
        if (!tag.contains(TAG_WINS))
        {
            tag.putInt(TAG_WINS, 0);
        }
    }

    /** 唯一编号；未标注返回 null（旧卡）。 */
    public static String uidOf(ItemStack stack)
    {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.contains(TAG_UID) ? tag.getString(TAG_UID) : null;
    }

    /** 获得日期 epoch millis；未标注返回 0。 */
    public static long obtainedOf(ItemStack stack)
    {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.contains(TAG_OBTAINED) ? tag.getLong(TAG_OBTAINED) : 0L;
    }

    /** 参战胜利次数；未标注返回 0。 */
    public static int winsOf(ItemStack stack)
    {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.contains(TAG_WINS) ? tag.getInt(TAG_WINS) : 0;
    }

    /** 参战胜利次数 +1（对局获胜方部署过的卡）。 */
    public static void addWin(ItemStack stack)
    {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putInt(TAG_WINS, tag.getInt(TAG_WINS) + 1);
    }
}
