package com.laigu.laigu.block;

import com.laigu.laigu.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * 对战方块：双人（或一人 + AI）在对战盘上交付卡组开打。
 * <ul>
 *   <li>手持卡组匣（16 张）右键 → 登记为 A/B 侧；空手右键 → 追加 AI 对手。</li>
 *   <li>对局中的玩家右键 → 重新打开/同步对战界面。</li>
 *   <li>交互与对局状态机都在 {@link DuelTableBlockEntity}。</li>
 * </ul>
 */
public class DuelTableBlock extends Block implements EntityBlock
{
    public DuelTableBlock(BlockBehaviour.Properties properties)
    {
        super(properties);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit)
    {
        if (level.isClientSide)
        {
            return InteractionResult.CONSUME;
        }
        if (level.getBlockEntity(pos) instanceof DuelTableBlockEntity table)
        {
            if (table.handleUse(player))
            {
                table.broadcastTo(player);
            }
        }
        return InteractionResult.CONSUME;
    }

    /** 方块被不可抗力移除：对局作废，登记卡组返还玩家。 */
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos,
            BlockState newState, boolean isMoving)
    {
        if (!state.is(newState.getBlock()))
        {
            if (level.getBlockEntity(pos) instanceof DuelTableBlockEntity table)
            {
                table.handleBlockRemoved();
            }
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type)
    {
        if (level.isClientSide) return null;
        if (type != ModBlockEntities.DUEL_TABLE.get()) return null;
        return (level1, pos1, state1, be) -> DuelTableBlockEntity.serverTick(level1, pos1, state1,
                (DuelTableBlockEntity) be);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state)
    {
        return new DuelTableBlockEntity(pos, state);
    }
}
