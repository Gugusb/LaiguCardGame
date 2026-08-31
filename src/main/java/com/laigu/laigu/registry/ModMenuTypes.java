package com.laigu.laigu.registry;

import com.laigu.laigu.Laigu;
import com.laigu.laigu.container.CardExchangeMenu;
import com.laigu.laigu.container.CardPouchMenu;
import com.laigu.laigu.container.DeckBoxMenu;
import com.laigu.laigu.container.DuelTableMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/**
 * 容器菜单类型注册表。
 */
public class ModMenuTypes
{
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, Laigu.MODID);

    public static final RegistryObject<MenuType<CardPouchMenu>> CARD_POUCH =
            MENUS.register("card_pouch", () -> new MenuType<>(CardPouchMenu::new, FeatureFlags.DEFAULT_FLAGS));

    /** 卡组匣（10 格） */
    public static final RegistryObject<MenuType<DeckBoxMenu>> DECK_BOX =
            MENUS.register("deck_box", () -> new MenuType<>(DeckBoxMenu::new, FeatureFlags.DEFAULT_FLAGS));

    /** 卡牌交换台（客户端经网络缓冲还原菜单，见 {@link CardExchangeMenu#fromNetwork}） */
    public static final RegistryObject<MenuType<CardExchangeMenu>> CARD_EXCHANGE =
            MENUS.register("card_exchange", () -> IForgeMenuType.create(CardExchangeMenu::fromNetwork));

    /** 对战方块登记界面（10 格卡组 + 提交；pos 经网络缓冲传给客户端） */
    public static final RegistryObject<MenuType<DuelTableMenu>> DUEL_TABLE =
            MENUS.register("duel_table", () -> IForgeMenuType.create(DuelTableMenu::fromNetwork));
}
