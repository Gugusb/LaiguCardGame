package com.laigu.laigu.registry;

import com.laigu.laigu.Laigu;
import com.laigu.laigu.block.CardExchangeTableBlock;
import com.laigu.laigu.block.DuelTableBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * 模组方块注册表。
 */
public class ModBlocks
{
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, Laigu.MODID);

    /** 卡牌交换台（素材先用原版工作台贴图占位） */
    public static final RegistryObject<Block> CARD_EXCHANGE_TABLE =
            BLOCKS.register("card_exchange_table", () ->
                    new CardExchangeTableBlock(BlockBehaviour.Properties.copy(Blocks.CRAFTING_TABLE)));

    /** 交换台方块物品（用 ModItems 的注册表，统一进入创造页签） */
    public static final RegistryObject<Item> CARD_EXCHANGE_TABLE_ITEM =
            ModItems.ITEMS.register("card_exchange_table", () ->
                    new BlockItem(CARD_EXCHANGE_TABLE.get(), new Item.Properties()));

    /** 对战方块（素材先用原版工作台贴图占位；防爆、不可破坏） */
    public static final RegistryObject<Block> DUEL_TABLE =
            BLOCKS.register("duel_table", () ->
                    new DuelTableBlock(BlockBehaviour.Properties.copy(Blocks.CRAFTING_TABLE)
                            .strength(-1.0F, 3600000.0F)));

    /** 对战方块物品 */
    public static final RegistryObject<Item> DUEL_TABLE_ITEM =
            ModItems.ITEMS.register("duel_table", () ->
                    new BlockItem(DUEL_TABLE.get(), new Item.Properties()));
}
