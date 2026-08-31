package com.laigu.laigu.item;

import com.laigu.laigu.album.CollectionAlbumData;
import com.laigu.laigu.network.CollectionAlbumOpenS2CPacket;
import com.laigu.laigu.network.ModPackets;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.PacketDistributor;

import java.util.List;

/**
 * 收藏册：右键打开按朝代分页的收藏界面。
 * <p>
 * 玩家把自己获得的来古牌「放」进对应槽位（实物从背包消耗、完整 NBT 留在册中），
 * 集齐一整页有闪光特效；槽位可悬停查看卡牌详细信息，点击已收集的卡可再取回。
 * 内容存在物品 NBT（见 {@link com.laigu.laigu.album.CollectionAlbumData}），无需方块实体。
 */
public class CollectionAlbumItem extends Item
{
    public CollectionAlbumItem(Properties properties)
    {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
    {
        if (!level.isClientSide && player instanceof ServerPlayer sp)
        {
            ItemStack album = player.getItemInHand(hand);
            // 找到收藏册在背包里的槽位（服务端据此消费/返还卡牌）
            int slot = inventorySlotOf(player, album);
            if (slot < 0)
            {
                slot = player.getInventory().selected;
            }
            ModPackets.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sp),
                    new CollectionAlbumOpenS2CPacket(slot, album));
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag)
    {
        String owner = CollectionAlbumData.ownerNameOf(stack);
        if (owner != null && !owner.isEmpty())
        {
            tooltip.add(Component.translatable("tooltip.laigu.album_owner", owner)
                    .withStyle(ChatFormatting.GOLD));
        }
        super.appendHoverText(stack, level, tooltip, flag);
    }

    private static int inventorySlotOf(Player player, ItemStack target)
    {
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++)
        {
            if (inv.getItem(i) == target)
            {
                return i;
            }
        }
        return -1;
    }

    @Override
    public boolean canFitInsideContainerItems()
    {
        // 允许把收藏册放进箱子/卡袋等容器
        return true;
    }
}
