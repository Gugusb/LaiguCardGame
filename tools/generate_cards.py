# -*- coding: utf-8 -*-
"""
来古牌（Laigu）卡牌资源生成器
================================
把「卡牌输出」目录里做好的卡牌贴图接入模组资源：
  1. 拷贝 普通版/金质版(V1) 卡牌贴图 + 4 种卡包贴图 → assets/laigu/textures/item/
  2. 为每张贴图生成一个物品模型 JSON（minecraft:item/generated）
  3. 生成 Java 卡目录 CardCatalog.java（每个 id 对应普通+金质两张卡）
  4. 生成中英文语言文件（每张卡 + 每种稀有度一条）

用法：python tools/generate_cards.py
生成产物已覆盖后无需重复运行（贴图变化时才需要）。
"""
import io
import json
import os
import shutil

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..'))
SRC_DIR = r"D:\GuguFiles\AI测试项目\MC卡牌游戏开发\Wuhuamixian\像素画头像_64x64\卡牌输出"
ASSET = os.path.join(ROOT, 'src', 'main', 'resources', 'assets', 'laigu')
TEX_DIR = os.path.join(ASSET, 'textures', 'item')
MODEL_DIR = os.path.join(ASSET, 'models', 'item')
LANG_DIR = os.path.join(ASSET, 'lang')

# 稀有度：普通 / 金质（V1）。物品 id 后缀。
RARITIES = ["common", "gold"]

# ---------------------------------------------------------------------------
# 卡牌目录： (中文名, 拼音 id, 英文名)
# id 只允许 [a-z0-9_]，全小写；每张卡会生成 <id>_common 与 <id>_gold 两个物品。
# ---------------------------------------------------------------------------
CARDS = [
    ("T形帛画", "t_xing_bo_hua", "T-Shaped Silk Painting"),
    ("万壑松风图", "wan_he_song_feng_tu", "Wind in Pines among a Myriad Valleys"),
    ("万工轿", "wan_gong_jiao", "Wan Gong Ceremonial Sedan Chair"),
    ("三兔藻井", "san_tu_zao_jing", "Three Hares Coffer Ceiling"),
    ("上阳台帖", "shang_yang_tai_tie", "Shangyangtai Calligraphy"),
    ("云雷纹大铙", "yun_lei_wen_da_nao", "Cloud-Thunder Pattern Nao Bell"),
    ("五弦琵琶", "wu_xian_pi_pa", "Five-String Pipa"),
    ("五星出东方", "wu_xing_chu_dong_fang", "Five Stars Rise in the East"),
    ("亚长牛尊", "ya_chang_niu_zun", "Yachang Ox Zun"),
    ("兔形陶埙", "tu_xing_tao_xun", "Rabbit-Shaped Pottery Xun"),
    ("利簋", "li_gui", "Li Gui Ritual Vessel"),
    ("十二花卉杯", "shi_er_hua_hui_bei", "Twelve-Flower Cup"),
    ("千里江山图", "qian_li_jiang_shan", "A Thousand Li of Rivers and Mountains"),
    ("午门", "wu_men", "Meridian Gate"),
    ("卷云金喇叭", "juan_yun_jin_la_ba", "Golden Trumpet with Cloud Scrolls"),
    ("向日葵", "xiang_ri_kui", "Sunflower"),
    ("商周十供", "shang_zhou_shi_gong", "Ten Shang-Zhou Ritual Offerings"),
    ("四龙四凤座", "si_long_si_feng_zuo", "Four Dragons & Four Phoenixes Seat"),
    ("外销壁纸", "wai_xiao_bi_zhi", "Export Wallpaper"),
    ("天王石刻", "tian_wang_shi_ke", "Heavenly King Stone Carving"),
    ("天球仪", "tian_qiu_yi", "Celestial Globe"),
    ("太阳神鸟", "tai_yang_shen_niao", "Sunbird Gold Ornament"),
    ("宋金项饰", "song_jin_xiang_shi", "Song Dynasty Gold Necklace"),
    ("小宋香炉", "xiao_song_xiang_lu", "Small Song Incense Burner"),
    ("广彩描金壶", "guang_cai_miao_jin_hu", "Guangdong Gold-Painted Porcelain Pot"),
    ("庞贝", "pang_bei", "Pompeii Mosaic"),
    ("彩凤鸣岐", "cai_feng_ming_qi", "Colorful Phoenix of Mingqi"),
    ("微缩家具", "wei_suo_jia_ju", "Miniature Furniture"),
    ("愿望杯", "yuan_wang_bei", "Wish Cup"),
    ("敦煌飞天", "dun_huang_fei_tian", "Dunhuang Flying Apsara"),
    ("断臂维纳斯", "duan_bi_wei_na_si", "Venus de Milo"),
    ("星月夜", "xing_yue_ye", "The Starry Night"),
    ("晋侯鸟尊", "jin_hou_niao_zun", "Marquis of Jin Bird Zun"),
    ("曾侯乙编钟", "zeng_hou_yi_bian_zhong", "Marquis Yi's Bronze Bells"),
    ("桃源仙境图", "tao_yuan_xian_jing_tu", "Peach Blossom Wonderland"),
    ("毛公鼎", "mao_gong_ding", "Mao Gong Ding Cauldron"),
    ("水晶杯", "shui_jing_bei", "Crystal Cup"),
    ("汉谟拉比法典", "han_mo_la_bi_fa_dian", "Code of Hammurabi"),
    ("洛神赋图", "luo_shen_fu_tu", "Nymph of the Luo River"),
    ("浑天仪", "hun_tian_yi", "Armillary Sphere"),
    ("海水江崖炉", "hai_shui_jiang_ya_lu", "Sea-Cliff Pattern Furnace"),
    ("海错图", "hai_cuo_tu", "Album of Sea Creatures"),
    ("溪山行旅图", "xi_shan_xing_lv_tu", "Travelers Among Mountains and Streams"),
    ("狸猫盘", "li_mao_pan", "Leopard Cat Plate"),
    ("王氏书翰卷", "wang_shi_shu_han_juan", "Wang's Calligraphy Scroll"),
    ("白石散乐", "bai_shi_san_le", "White Stone Music Relief"),
    ("百花图卷", "bai_hua_tu_juan", "Hundred Flowers Scroll"),
    ("真珠宝幢", "zhen_zhu_bao_chuang", "Jeweled Pagoda Banner"),
    ("睡莲", "shui_lian", "Water Lilies"),
    ("秋操杯", "qiu_cao_bei", "Autumn Drill Cup"),
    ("秦公镈", "qin_gong_bo", "Duke of Qin Bo Bell"),
    ("素纱单衣", "su_sha_dan_yi", "Plain Gauze Robe"),
    ("芙蓉炉", "fu_rong_lu", "Hibiscus Furnace"),
    ("莫高窟220", "mo_gao_ku_220", "Mogao Cave 220"),
    ("莫高窟记", "mo_gao_ku_ji", "Mogao Caves Chronicle"),
    ("莲塘乳鸭图", "lian_tang_ru_ya_tu", "Ducklings in Lotus Pond"),
    ("蝠桃瓶", "fu_tao_ping", "Bat-Peach Vase"),
    ("贾湖骨笛", "jia_hu_gu_di", "Jiahu Bone Flute"),
    ("赤壁赋页", "chi_bi_fu_ye", "Red Cliff Rhapsody Page"),
    ("越王勾践剑", "yue_wang_gou_jian_jian", "Sword of Goujian"),
    ("酒帐", "jiu_zhang", "Wine Ledger"),
    ("金冠带", "jin_guan_dai", "Gold Headdress"),
    ("金瓯永固杯", "jin_ou_yong_gu_bei", "Gold Unbreakable Goblet"),
    ("金石录", "jin_shi_lu", "Jinshilu Catalogue"),
    ("金蝉玉叶", "jin_chan_yu_ye", "Golden Cicada on Jade Leaf"),
    ("鎏金骑士", "liu_jin_qi_shi", "Gilded Knight"),
    ("铜坐龙", "tong_zuo_long", "Bronze Sitting Dragon"),
    ("铜壶滴漏", "tong_hu_di_lou", "Bronze Water Clock"),
    ("铜奔马", "tong_ben_ma", "Bronze Galloping Horse"),
    ("铜车马", "tong_che_ma", "Bronze Chariot & Horses"),
    ("银雀山汉简_1", "yin_que_shan_han_jian_1", "Yinqueshan Han Bamboo Slips I"),
    ("银雀山汉简_2", "yin_que_shan_han_jian_2", "Yinqueshan Han Bamboo Slips II"),
    ("银香囊", "yin_xiang_nang", "Silver Incense Sachet"),
    ("雪景寒林图", "xue_jing_han_lin_tu", "Snowy Forest in Cold Mountains"),
    ("青瓷莲花尊", "qing_ci_lian_hua_zun", "Celadon Lotus Zun"),
    ("青铜仙鹤", "qing_tong_xian_he", "Bronze Crane"),
    ("鸟音山水钟", "niao_yin_shan_shui_zhong", "Bird-Song Landscape Clock"),
    ("鹿王本生图", "lu_wang_ben_sheng_tu", "Jataka of the Deer King"),
    ("麟趾马蹄金", "lin_zhi_ma_ti_jin", "Unicorn-Hoof Gold Coin"),
]

# 卡包： (中文类型名, 稀有度/物品 id 后缀, 英文名)
PACKS = [
    ("普通", "common", "Common"),
    ("末影", "ender", "Ender"),
    ("炫彩", "rainbow", "Rainbow"),
    ("金质", "gold", "Gold"),
]


def card_items():
    """所有卡牌物品 id 列表：每个 id 的 common 与 gold"""
    items = []
    for _zh, cid, _en in CARDS:
        for r in RARITIES:
            items.append(cid + "_" + r)
    return items


def ensure_dirs():
    for d in (TEX_DIR, MODEL_DIR, LANG_DIR):
        os.makedirs(d, exist_ok=True)


def copy_textures():
    """拷贝卡牌与卡包贴图到 textures/item/（英文命名）"""
    copied, missing = 0, []
    for zh, cid, _en in CARDS:
        for r, suffix in (("common", "普通版"), ("gold", "金质版")):
            src = os.path.join(SRC_DIR, zh + "_" + suffix + ".png")
            dst = os.path.join(TEX_DIR, cid + "_" + r + ".png")
            if os.path.exists(src):
                shutil.copy2(src, dst)
                copied += 1
            else:
                missing.append(zh + "_" + suffix)
    for zh, rid, _en in PACKS:
        src = os.path.join(SRC_DIR, "卡包_" + zh + ".png")
        dst = os.path.join(TEX_DIR, "card_pack_" + rid + ".png")
        if os.path.exists(src):
            shutil.copy2(src, dst)
            copied += 1
        else:
            missing.append("卡包_" + zh)
    return copied, missing


def gen_models():
    """每张贴图一个物品模型；卡牌额外生成端详 3D 模型并挂 override。

    端详 3D：卡牌模型通过 item override（laigu:inspecting predicate）在
    端详中切换到 <id>_<rarity>_3d 模型；parent 按稀有度区分：
    普通（_common_3d）→ laigu:item/card_3d，金质（_gold_3d）→ laigu:item/card_3d_gold。
    """
    count = 0
    for item_id in card_items() + ["card_pack_" + r for _, r, _ in PACKS]:
        is_card = not item_id.startswith("card_")  # 卡牌 vs 卡包/卡袋
        model = {
            "parent": "minecraft:item/generated",
            "textures": {"layer0": "laigu:item/" + item_id},
        }
        if is_card:
            model["overrides"] = [{
                "predicate": {"laigu:inspecting": 1},
                "model": "laigu:item/" + item_id + "_3d",
            }]
            model3d = {
                # 稀有度决定 3D 牌框：金质用金色边框 card_3d_gold，普通用深色橡木框 card_3d
                "parent": "laigu:item/card_3d_gold" if item_id.endswith("_gold") else "laigu:item/card_3d",
                "textures": {"layer0": "laigu:item/" + item_id},
            }
            with io.open(os.path.join(MODEL_DIR, item_id + "_3d.json"), "w", encoding="utf-8") as f:
                json.dump(model3d, f, ensure_ascii=False, indent=2)
        with io.open(os.path.join(MODEL_DIR, item_id + ".json"), "w", encoding="utf-8") as f:
            json.dump(model, f, ensure_ascii=False, indent=2)
        count += 1
    return count


def gen_catalog_java():
    """生成 CardCatalog.java：列出全部卡牌 id（每 id 对应 common+gold 两张卡）"""
    lines = [
        "package com.laigu.laigu.card;",
        "",
        "import java.util.List;",
        "",
        "/**",
        " * 卡牌目录。本文件由 tools/generate_cards.py 自动生成，勿手改。",
        " * 每个 id 对应一套「普通 + 金质」两张卡（<id>_common / <id>_gold）。",
        " */",
        "public final class CardCatalog {",
        "    public static final List<String> CARD_IDS = List.of(",
    ]
    for i, (_zh, cid, _en) in enumerate(CARDS):
        comma = "," if i < len(CARDS) - 1 else ""
        lines.append('            "%s"%s' % (cid, comma))
    lines += [
        "    );",
        "",
        "    private CardCatalog() {}",
        "}",
        "",
    ]
    path = os.path.join(ROOT, 'src', 'main', 'java', 'com', 'laigu', 'laigu', 'card', 'CardCatalog.java')
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with io.open(path, "w", encoding="utf-8", newline="\n") as f:
        f.write("\n".join(lines))
    return len(CARDS)


def gen_lang():
    """生成 zh_cn / en_us 语言文件"""
    zh = {"creativetab.laigu": "来古牌"}
    en = {"creativetab.laigu": "Laigu"}
    # 卡袋 / 卡牌 tag / 开包提示（新增功能文案）
    zh["item.laigu.card_pouch"] = "卡袋"
    en["item.laigu.card_pouch"] = "Card Pouch"
    zh["tooltip.laigu.dynasty"] = "朝代：%s"
    en["tooltip.laigu.dynasty"] = "Dynasty: %s"
    zh["tooltip.laigu.type"] = "类型：%s"
    en["tooltip.laigu.type"] = "Type: %s"
    zh["tooltip.laigu.owner"] = "所有者：%s"
    en["tooltip.laigu.owner"] = "Owner: %s"
    zh["message.laigu.pack_opened"] = "开包成功，获得了 %s 张卡牌！"
    en["message.laigu.pack_opened"] = "Opened the pack and got %s cards!"
    # 图鉴 / 交换台（卡牌交换）文案
    zh["message.laigu.codex_unlock"] = "图鉴解锁：%s"
    en["message.laigu.codex_unlock"] = "Codex unlocked: %s"
    zh["block.laigu.card_exchange_table"] = "卡牌交换台"
    en["block.laigu.card_exchange_table"] = "Card Exchange Table"
    zh["container.laigu.card_exchange"] = "卡牌交换"
    en["container.laigu.card_exchange"] = "Card Exchange"
    zh["button.laigu.confirm_swap"] = "确认交换"
    en["button.laigu.confirm_swap"] = "Confirm Swap"
    zh["button.laigu.confirmed"] = "已确认"
    en["button.laigu.confirmed"] = "Confirmed"
    zh["button.laigu.cancel_swap"] = "取消交换"
    en["button.laigu.cancel_swap"] = "Cancel Swap"
    zh["button.laigu.swap_done"] = "交换完成"
    en["button.laigu.swap_done"] = "Swapped"
    zh["message.laigu.exchange_busy"] = "交换台正在被其他玩家使用"
    en["message.laigu.exchange_busy"] = "The exchange table is already in use"
    zh["message.laigu.need_card"] = "请先放入一张卡牌再确认"
    en["message.laigu.need_card"] = "Place a card first to confirm"
    zh["message.laigu.take_cards_first"] = "交换完成，请先取回卡牌"
    en["message.laigu.take_cards_first"] = "Swap complete - take your cards first"
    for zh_name, cid, en_name in CARDS:
        zh["item.laigu." + cid + "_common"] = zh_name
        zh["item.laigu." + cid + "_gold"] = zh_name + "·金质"
        en["item.laigu." + cid + "_common"] = en_name
        en["item.laigu." + cid + "_gold"] = en_name + " (Gold)"
    for zh_name, rid, en_name in PACKS:
        zh["item.laigu.card_pack_" + rid] = "卡包·" + zh_name
        en["item.laigu.card_pack_" + rid] = "Card Pack (" + en_name + ")"
    for name, data in (("zh_cn.json", zh), ("en_us.json", en)):
        with io.open(os.path.join(LANG_DIR, name), "w", encoding="utf-8", newline="\n") as f:
            json.dump(data, f, ensure_ascii=False, indent=2)
    return len(zh), len(en)


def main():
    ensure_dirs()
    n_tex, missing = copy_textures()
    n_model = gen_models()
    n_cat = gen_catalog_java()
    n_zh, n_en = gen_lang()

    print("== 来古牌资源生成报告 ==")
    print("拷贝贴图: %d 张" % n_tex)
    print("生成物品模型: %d 个" % n_model)
    print("卡目录卡牌数: %d（物品 %d 个）" % (n_cat, n_cat * 2))
    print("语言条目: zh_cn %d / en_us %d" % (n_zh, n_en))
    if missing:
        print("!! 缺失源文件: %s" % missing)
    else:
        print("源文件全部就位")


if __name__ == "__main__":
    main()
