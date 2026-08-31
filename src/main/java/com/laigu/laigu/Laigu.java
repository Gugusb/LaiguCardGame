package com.laigu.laigu;

import com.laigu.laigu.capability.ModCapabilities;
import com.laigu.laigu.client.LaiguConfigScreen;
import com.laigu.laigu.config.LaiguConfig;
import com.laigu.laigu.network.ModPackets;
import com.laigu.laigu.registry.ModBlockEntities;
import com.laigu.laigu.registry.ModBlocks;
import com.laigu.laigu.registry.ModCreativeTabs;
import com.laigu.laigu.registry.ModItems;
import com.laigu.laigu.registry.ModMenuTypes;
import com.laigu.laigu.registry.ModSounds;
import com.mojang.logging.LogUtils;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

// 来古牌（Laigu）：以「收集卡牌」为后期目标的 Minecraft 模组。
// 注解值必须与 META-INF/mods.toml 中的 modId 一致。
@Mod(Laigu.MODID)
public class Laigu
{
    // 模组 id：全局统一引用
    public static final String MODID = "laigu";
    // 直接引用一个 slf4j 日志器
    private static final Logger LOGGER = LogUtils.getLogger();

    public Laigu(FMLJavaModLoadingContext context)
    {
        IEventBus modEventBus = context.getModEventBus();

        // 注册 Deferred Register，使物品/创造页签/菜单/方块/方块实体在模组加载时被注册
        ModItems.ITEMS.register(modEventBus);
        ModCreativeTabs.CREATIVE_MODE_TABS.register(modEventBus);
        ModMenuTypes.MENUS.register(modEventBus);
        ModBlocks.BLOCKS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITY_TYPES.register(modEventBus);
        ModSounds.SOUNDS.register(modEventBus);

        // 注册客户端配置（积分动画速度倍率等，mod 设置可调）
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, LaiguConfig.SPEC);
        // Mods 列表点 laigu → Config 打开设置界面
        ModLoadingContext.get().registerExtensionPoint(
                net.minecraftforge.client.ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new net.minecraftforge.client.ConfigScreenHandler.ConfigScreenFactory(
                        (mc, parent) -> new LaiguConfigScreen(parent)));

        // 注册网络通道（客户端包面动画等数据包）
        ModPackets.register();

        // 注册玩家图鉴能力（MOD 总线事件）
        modEventBus.addListener(ModCapabilities::register);

        LOGGER.info("来古牌 Laigu 加载完成");
    }
}
