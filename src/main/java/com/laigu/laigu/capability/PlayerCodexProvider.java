package com.laigu.laigu.capability;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 把 {@link IPlayerCodex} 附加到玩家实体，并负责与玩家 NBT 双向读写。
 * <p>
 * 玩家数据随玩家存档保存/加载（ICapabilitySerializable），绑定玩家个人且永久存储。
 */
public class PlayerCodexProvider implements ICapabilitySerializable<CompoundTag>
{
    public static final String TAG_UNLOCKED = "unlocked";

    private final PlayerCodex codex = new PlayerCodex();
    private final LazyOptional<IPlayerCodex> optional = LazyOptional.of(() -> codex);

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side)
    {
        return cap == ModCapabilities.PLAYER_CODEX ? optional.cast() : LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT()
    {
        CompoundTag tag = new CompoundTag();
        tag.putIntArray(TAG_UNLOCKED, codex.unlocked());
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag)
    {
        if (tag.contains(TAG_UNLOCKED))
        {
            codex.loadFrom(tag.getIntArray(TAG_UNLOCKED));
        }
    }
}
