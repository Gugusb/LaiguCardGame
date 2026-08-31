package com.laigu.laigu.registry;

import com.laigu.laigu.Laigu;
import com.laigu.laigu.card.CardCatalog;
import com.laigu.laigu.item.CardItem;
import com.laigu.laigu.item.CardPackItem;
import com.laigu.laigu.item.CardPouchItem;
import com.laigu.laigu.item.CollectionAlbumItem;
import com.laigu.laigu.item.DeckBoxItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.RecordItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 模组物品注册表。
 * <p>
 * 卡牌：CardCatalog 里的每个 id 会生成「普通 + 金质」两个独立物品
 * （<id>_common / <id>_gold），一一对应每张贴图。
 * 卡包：四种（普通 / 末影 / 炫彩 / 金质），行为见 {@link CardPackItem}。
 * 卡袋：6 格卡牌存储（见 {@link CardPouchItem}）。
 */
public class ModItems
{
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, Laigu.MODID);

    /** 卡牌稀有度（物品 id 后缀），与贴图产出一致 */
    public static final List<String> RARITIES = List.of("common", "gold");

    /** 全部卡牌物品（普通 + 金质） */
    public static final List<RegistryObject<Item>> CARDS = new ArrayList<>();

    /** 卡牌物品 id（如 <拼音>_common）→ 注册对象，供开包等逻辑快速查找 */
    private static final Map<String, RegistryObject<Item>> CARD_BY_ID = new HashMap<>();

    /** 卡组匣（10 格来古牌，右键打开编辑 / 右键对战方块登记） */
    public static final RegistryObject<Item> DECK_BOX =
            ITEMS.register("deck_box", () -> new DeckBoxItem(new Item.Properties().stacksTo(1)));

    /** 创造对战卡组：无视规则（可重复/不足16张/全金卡/金卡可直接放置并触发焕章） */
    public static final RegistryObject<Item> DECK_BOX_CREATIVE =
            ITEMS.register("deck_box_creative", () -> new DeckBoxItem(new Item.Properties().stacksTo(1), true));

    /** 卡袋物品 */
    public static final RegistryObject<Item> CARD_POUCH =
            ITEMS.register("card_pouch", () -> new CardPouchItem(new Item.Properties().stacksTo(1)));

    /** 收藏册（按朝代分页收藏卡牌，见 {@link CollectionAlbumItem}） */
    public static final RegistryObject<Item> COLLECTION_ALBUM =
            ITEMS.register("collection_album", () -> new CollectionAlbumItem(new Item.Properties().stacksTo(1)));

    /** 卡包物品（单包堆叠上限 1，开一包少一包） */
    public static final RegistryObject<Item> CARD_PACK_COMMON =
            ITEMS.register("card_pack_common", () -> new CardPackItem(new Item.Properties().stacksTo(1), CardPackItem.PackType.COMMON));
    public static final RegistryObject<Item> CARD_PACK_ENDER =
            ITEMS.register("card_pack_ender", () -> new CardPackItem(new Item.Properties().stacksTo(1), CardPackItem.PackType.ENDER));
    public static final RegistryObject<Item> CARD_PACK_RAINBOW =
            ITEMS.register("card_pack_rainbow", () -> new CardPackItem(new Item.Properties().stacksTo(1), CardPackItem.PackType.RAINBOW));
    public static final RegistryObject<Item> CARD_PACK_GOLD =
            ITEMS.register("card_pack_gold", () -> new CardPackItem(new Item.Properties().stacksTo(1), CardPackItem.PackType.GOLD));

    /** 《浮生偷闲》音乐唱片。 */
    public static final RegistryObject<Item> MUSIC_DISC_FUSHENG_TOUXIAN =
            ITEMS.register("music_disc_fusheng_touxian", () ->
                    new RecordItem(1, ModSounds.FUSHENG_TOUXIAN::get,
                            new Item.Properties().stacksTo(1), 3620));

    /** 创造页签展示用：全部物品（卡牌 + 卡包 + 卡袋 + 唱片） */
    public static final List<RegistryObject<Item>> ALL_ITEMS = new ArrayList<>();

    static
    {
        // 每张卡 → 普通 + 金质 两个物品，物品 id 与贴图文件名一致
        for (String cardId : CardCatalog.CARD_IDS)
        {
            for (String rarity : RARITIES)
            {
                String itemId = cardId + "_" + rarity;
                RegistryObject<Item> ro = ITEMS.register(itemId, () -> new CardItem(new Item.Properties().stacksTo(1)));
                CARDS.add(ro);
                CARD_BY_ID.put(itemId, ro);
            }
        }

        ALL_ITEMS.addAll(CARDS);
        ALL_ITEMS.add(CARD_PACK_COMMON);
        ALL_ITEMS.add(CARD_PACK_ENDER);
        ALL_ITEMS.add(CARD_PACK_RAINBOW);
        ALL_ITEMS.add(CARD_PACK_GOLD);
        ALL_ITEMS.add(CARD_POUCH);
        ALL_ITEMS.add(COLLECTION_ALBUM);
        ALL_ITEMS.add(MUSIC_DISC_FUSHENG_TOUXIAN);

        ALL_ITEMS.add(DECK_BOX);
        ALL_ITEMS.add(DECK_BOX_CREATIVE);
    }

    /**
     * 按卡牌 id + 稀有度取卡牌物品（如 {@code getCardItem("qian_li_jiang_shan", "gold")}）。
     * 未知组合返回 {@code ItemStack.EMPTY} 对应的空物品（不会为 null）。
     */
    public static Item getCardItem(String cardId, String rarity)
    {
        RegistryObject<Item> ro = CARD_BY_ID.get(cardId + "_" + rarity);
        return ro != null ? ro.get() : net.minecraft.world.item.Items.AIR;
    }
}
