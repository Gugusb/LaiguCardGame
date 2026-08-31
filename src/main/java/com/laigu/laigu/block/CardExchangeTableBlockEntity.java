package com.laigu.laigu.block;

import com.laigu.laigu.card.CodexHelper;
import com.laigu.laigu.container.CardExchangeMenu;
import com.laigu.laigu.registry.ModBlockEntities;
import com.laigu.laigu.util.CardNbt;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * 卡牌交换台方块实体。
 * <p>
 * 单台共用两份卡牌槽（A/B 两侧），两个玩家各占一侧：
 * <ul>
 *   <li>占位：先到者为 A 侧，后者为 B 侧；两侧都被占则拒绝第三人（{@link #tryUse}）。</li>
 *   <li>防偷：菜单槽位只允许放入/取走自己那一侧（见 {@link CardExchangeMenu.ExchangeCardSlot}）。</li>
 *   <li>交换：双方都点击「确认交换」且两侧都有卡牌时（{@link #trySwap}），
 *       交换两张卡牌的位置并<b>重写所有者</b>；随后解锁双方图鉴。</li>
 *   <li>重置：交换完成后双方都拿走卡牌，台子清空回到无人状态（{@link #tryReset}）。</li>
 * </ul>
 * 玩家侧会话与交换状态持久化到方块 NBT，服务器重启不丢失。
 */
public class CardExchangeTableBlockEntity extends BlockEntity implements MenuProvider
{
    public static final int SLOT_A = 0;
    public static final int SLOT_B = 1;

    /** 数据同步下标（SimpleContainerData[3]，随菜单自动同步给双方客户端） */
    public static final int DATA_CONFIRMED_A = 0;
    public static final int DATA_CONFIRMED_B = 1;
    public static final int DATA_SWAPPED = 2;

    private final SimpleContainer container = new SimpleContainer(2);
    private final SimpleContainerData data = new SimpleContainerData(3);

    @Nullable private UUID ownerA;
    @Nullable private String ownerAName;
    @Nullable private UUID ownerB;
    @Nullable private String ownerBName;

    public CardExchangeTableBlockEntity(BlockPos pos, BlockState state)
    {
        super(ModBlockEntities.CARD_EXCHANGE_TABLE.get(), pos, state);
    }

    public SimpleContainer getContainer()
    {
        return container;
    }

    public SimpleContainerData getData()
    {
        return data;
    }

    @Nullable
    public UUID ownerOfSide(int side)
    {
        return side == SLOT_A ? ownerA : ownerB;
    }

    @Nullable
    public String nameOfSide(int side)
    {
        return side == SLOT_A ? ownerAName : ownerBName;
    }

    /** 玩家使用台子：返回是否拿到自己的侧位（A/B）。两侧都被占返回 false。 */
    public boolean tryUse(Player player)
    {
        UUID uuid = player.getUUID();
        if (uuid.equals(ownerA) || uuid.equals(ownerB))
        {
            return true;
        }
        if (ownerA == null)
        {
            ownerA = uuid;
            ownerAName = player.getGameProfile().getName();
            setChanged();
            return true;
        }
        if (ownerB == null)
        {
            ownerB = uuid;
            ownerBName = player.getGameProfile().getName();
            setChanged();
            return true;
        }
        return false;
    }

    /** 玩家是否占着该侧。 */
    public boolean isSideOwner(int side, Player player)
    {
        UUID owner = ownerOfSide(side);
        return owner != null && owner.equals(player.getUUID());
    }

    /** 玩家当前占用的侧位（SLOT_A=0 / SLOT_B=1）；未占用则 -1。 */
    public int sideOf(Player player)
    {
        if (isSideOwner(SLOT_A, player))
        {
            return SLOT_A;
        }
        if (isSideOwner(SLOT_B, player))
        {
            return SLOT_B;
        }
        return -1;
    }

    /** 菜单「确认交换」按钮回调（服务端）：点击 = 切换本人确认状态，双方都确认则执行交换。 */
    public void confirm(Player player)
    {
        if (level == null || level.isClientSide)
        {
            return;
        }
        // 交换已完成：双方取回卡牌前不可再确认，点击给提示（避免静默 return 被当成按钮失灵）
        if (data.get(DATA_SWAPPED) == 1)
        {
            if (player instanceof ServerPlayer sp)
            {
                sp.displayClientMessage(Component.translatable("message.laigu.take_cards_first"), true);
            }
            return;
        }
        int side = sideOf(player);
        if (side < 0)
        {
            // 我的侧位已被释放/重置：重新占位（台子被别人占满则忽略）
            if (!tryUse(player))
            {
                return;
            }
            side = sideOf(player);
            if (side < 0)
            {
                return;
            }
        }
        // 一侧没有卡牌：点击不生效，提示玩家先放卡（避免按钮看起来失灵/卡住）
        if (container.getItem(side).isEmpty())
        {
            if (player instanceof ServerPlayer sp)
            {
                sp.displayClientMessage(Component.translatable("message.laigu.need_card"), true);
            }
            return;
        }
        int idx = side == SLOT_A ? DATA_CONFIRMED_A : DATA_CONFIRMED_B;
        // 再点一次 = 取消确认
        data.set(idx, data.get(idx) == 1 ? 0 : 1);
        trySwap();
        setChanged();
    }

    /** 卡牌槽位变化：卡牌被取走则自动取消该侧确认，随后尝试交换/重置。 */
    public void onSlotChanged(int side)
    {
        if (level == null || level.isClientSide)
        {
            return;
        }
        if (container.getItem(side).isEmpty())
        {
            data.set(side == SLOT_A ? DATA_CONFIRMED_A : DATA_CONFIRMED_B, 0);
        }
        setChanged();
        trySwap();
        tryReset();
    }

    /** 双方都确认且两侧都有卡牌时：交换位置 + 交换所有者，并解锁双方图鉴。 */
    public void trySwap()
    {
        if (level == null || level.isClientSide)
        {
            return;
        }
        if (data.get(DATA_SWAPPED) == 1)
        {
            return;
        }
        if (data.get(DATA_CONFIRMED_A) != 1 || data.get(DATA_CONFIRMED_B) != 1)
        {
            return;
        }
        ItemStack cardA = container.getItem(SLOT_A);
        ItemStack cardB = container.getItem(SLOT_B);
        if (cardA.isEmpty() || cardB.isEmpty())
        {
            return;
        }

        // 交换所有者：cardA → ownerB，cardB → ownerA
        CardNbt.setOwnerBy(cardB, ownerAName, ownerA);
        CardNbt.setOwnerBy(cardA, ownerBName, ownerB);
        // 交换即「新获得」：补全实例数据（唯一编号/获得日期/胜利次数），旧卡也有
        CardNbt.ensureInstance(cardA);
        CardNbt.ensureInstance(cardB);
        // 交换位置
        container.setItem(SLOT_A, cardB);
        container.setItem(SLOT_B, cardA);

        data.set(DATA_CONFIRMED_A, 0);
        data.set(DATA_CONFIRMED_B, 0);
        data.set(DATA_SWAPPED, 1);
        setChanged();

        // 交换成功提示音（附近玩家都能听到）
        level.playSound(null, worldPosition, SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.BLOCKS, 1.0F, 1.0F);

        // 交换完成后双方各自首次获得的新卡牌解锁图鉴（服务端）
        if (level instanceof ServerLevel serverLevel && ownerA != null && ownerB != null)
        {
            CodexHelper.scanAndUnlock(serverLevel.getServer().getPlayerList().getPlayer(ownerA));
            CodexHelper.scanAndUnlock(serverLevel.getServer().getPlayerList().getPlayer(ownerB));
        }
    }

    /** 交换完成后双方都拿走卡牌，重置交换状态；双方占位保留，可继续下一轮，直到关闭菜单才释放。 */
    public void tryReset()
    {
        if (level == null || level.isClientSide)
        {
            return;
        }
        if (data.get(DATA_SWAPPED) != 1)
        {
            return;
        }
        if (!container.getItem(SLOT_A).isEmpty() || !container.getItem(SLOT_B).isEmpty())
        {
            return;
        }
        data.set(DATA_SWAPPED, 0);
        setChanged();
    }

    /** 释放某侧占位（菜单关闭时，若该侧无卡牌且未交换则释放，避免空占台子）。 */
    public void releaseSide(int side)
    {
        if (data.get(DATA_SWAPPED) == 1)
        {
            return;
        }
        if (side == SLOT_A && container.getItem(SLOT_A).isEmpty())
        {
            ownerA = null;
            ownerAName = null;
            data.set(DATA_CONFIRMED_A, 0);
            setChanged();
        }
        else if (side == SLOT_B && container.getItem(SLOT_B).isEmpty())
        {
            ownerB = null;
            ownerBName = null;
            data.set(DATA_CONFIRMED_B, 0);
            setChanged();
        }
    }

    // ---- MenuProvider ----

    @Override
    public Component getDisplayName()
    {
        return Component.translatable("container.laigu.card_exchange");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player)
    {
        // 侧位在服务端这里算好，客户端经 fromNetwork 的网络缓冲拿到同样的值
        return new CardExchangeMenu(id, inv, this, sideOf(player));
    }

    // ---- 持久化 ----

    @Override
    protected void saveAdditional(CompoundTag tag)
    {
        super.saveAdditional(tag);
        tag.put("cards", container.createTag());
        tag.putInt("confirmedA", data.get(DATA_CONFIRMED_A));
        tag.putInt("confirmedB", data.get(DATA_CONFIRMED_B));
        tag.putInt("swapped", data.get(DATA_SWAPPED));
        if (ownerA != null)
        {
            tag.putUUID("ownerA", ownerA);
            tag.putString("ownerAName", ownerAName == null ? "" : ownerAName);
        }
        if (ownerB != null)
        {
            tag.putUUID("ownerB", ownerB);
            tag.putString("ownerBName", ownerBName == null ? "" : ownerBName);
        }
    }

    @Override
    public void load(CompoundTag tag)
    {
        super.load(tag);
        if (tag.contains("cards"))
        {
            container.fromTag(tag.getList("cards", 10));
        }
        data.set(DATA_CONFIRMED_A, tag.getInt("confirmedA"));
        data.set(DATA_CONFIRMED_B, tag.getInt("confirmedB"));
        data.set(DATA_SWAPPED, tag.getInt("swapped"));
        ownerA = tag.contains("ownerA") ? tag.getUUID("ownerA") : null;
        ownerAName = tag.contains("ownerAName") ? tag.getString("ownerAName") : null;
        ownerB = tag.contains("ownerB") ? tag.getUUID("ownerB") : null;
        ownerBName = tag.contains("ownerBName") ? tag.getString("ownerBName") : null;
    }
}
