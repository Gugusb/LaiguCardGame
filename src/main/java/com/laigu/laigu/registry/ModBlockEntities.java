package com.laigu.laigu.registry;

import com.laigu.laigu.Laigu;
import com.laigu.laigu.block.CardExchangeTableBlockEntity;
import com.laigu.laigu.block.DuelTableBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * 模组方块实体注册表。
 */
public class ModBlockEntities
{
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, Laigu.MODID);

    public static final RegistryObject<BlockEntityType<CardExchangeTableBlockEntity>> CARD_EXCHANGE_TABLE =
            BLOCK_ENTITY_TYPES.register("card_exchange_table", () ->
                    BlockEntityType.Builder.of(CardExchangeTableBlockEntity::new,
                            ModBlocks.CARD_EXCHANGE_TABLE.get()).build(null));

    public static final RegistryObject<BlockEntityType<DuelTableBlockEntity>> DUEL_TABLE =
            BLOCK_ENTITY_TYPES.register("duel_table", () ->
                    BlockEntityType.Builder.of(DuelTableBlockEntity::new,
                            ModBlocks.DUEL_TABLE.get()).build(null));
}
