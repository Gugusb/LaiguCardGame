package com.laigu.laigu.block;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

/**
 * 卡牌交换台：两个玩家各放一张自己的卡牌，双方确认后交换所属。
 * <p>
 * 占位素材用原版工作台贴图；交互逻辑在 {@link CardExchangeTableBlockEntity}。
 * 右键：分配 A/B 侧并打开菜单；两侧都被占则提示忙碌。
 */
public class CardExchangeTableBlock extends Block implements EntityBlock
{
    public CardExchangeTableBlock(BlockBehaviour.Properties properties)
    {
        super(properties);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit)
    {
        if (level.isClientSide)
        {
            return InteractionResult.sidedSuccess(true);
        }
        if (level.getBlockEntity(pos) instanceof CardExchangeTableBlockEntity table)
        {
            if (table.tryUse(player))
            {
                int side = table.sideOf(player);
                NetworkHooks.openScreen((ServerPlayer) player, table,
                        buf ->
                        {
                            buf.writeBlockPos(pos);
                            buf.writeInt(side);
                        });
            }
            else
            {
                player.displayClientMessage(
                        Component.translatable("message.laigu.exchange_busy"), true);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos,
            BlockState newState, boolean isMoving)
    {
        if (!state.is(newState.getBlock()))
        {
            // 拆台时把台上的卡牌掉出来
            if (level.getBlockEntity(pos) instanceof CardExchangeTableBlockEntity table)
            {
                table.getContainer().removeAllItems().forEach(stack ->
                        popResource(level, pos, stack));
            }
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state)
    {
        return new CardExchangeTableBlockEntity(pos, state);
    }
}
