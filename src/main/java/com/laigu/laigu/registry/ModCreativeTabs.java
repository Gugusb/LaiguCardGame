package com.laigu.laigu.registry;

import com.laigu.laigu.Laigu;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/**
 * 模组独立创造页签：「来古牌」。
 * 收纳全部卡牌与卡包物品。
 */
public class ModCreativeTabs
{
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Laigu.MODID);

    public static final RegistryObject<CreativeModeTab> LAIGU_TAB = CREATIVE_MODE_TABS.register("laigu",
            () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(ModItems.CARD_PACK_GOLD.get()))
                    .title(Component.translatable("creativetab.laigu"))
                    .displayItems((parameters, output) ->
                    {
                        // 卡牌（普通+金质成对）→ 卡包 → 卡袋 → 交换台，按注册顺序展示
                        for (RegistryObject<Item> item : ModItems.ALL_ITEMS)
                        {
                            output.accept(item.get());
                        }
                        output.accept(ModBlocks.CARD_EXCHANGE_TABLE_ITEM.get());
                        output.accept(ModBlocks.DUEL_TABLE_ITEM.get());
                    })
                    .build());
}
