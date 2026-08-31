package com.laigu.laigu.client;

import com.laigu.laigu.Laigu;
import com.laigu.laigu.registry.ModMenuTypes;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * 客户端注册：把卡袋菜单与 GUI 绑定；注册卡牌「端详」物品模型 predicate。
 */
@Mod.EventBusSubscriber(modid = Laigu.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetup
{
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event)
    {
        event.enqueueWork(() ->
        {
            MenuScreens.register(ModMenuTypes.CARD_POUCH.get(), CardPouchScreen::new);
            MenuScreens.register(ModMenuTypes.CARD_EXCHANGE.get(), CardExchangeScreen::new);
            MenuScreens.register(ModMenuTypes.DECK_BOX.get(), DeckBoxScreen::new);
            MenuScreens.register(ModMenuTypes.DUEL_TABLE.get(), DuelTableScreen::new);

            // 端详 3D：端详中把卡牌模型从扁平切换到 3D 牌框模型（见 InspectAnimator#isInspectionVisual）。
            // 用 registerGeneric 全局注册，predicate 内部只对卡牌返回 1。
            ItemProperties.registerGeneric(new ResourceLocation(Laigu.MODID + ":inspecting"),
                    (stack, level, entity, seed) -> InspectAnimator.isInspectionVisual(stack) ? 1.0F : 0.0F);
        });
    }
}
