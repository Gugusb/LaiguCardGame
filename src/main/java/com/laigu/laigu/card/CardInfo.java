package com.laigu.laigu.card;

import com.laigu.laigu.util.CardNbt;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.Map;

/**
 * 卡牌元数据：每张卡对应的文物「朝代」与「文物类型」。
 * <p>
 * 数据写在 {@link #BY_ID}（生成维护，方便后续按策划调整）。
 * 卡牌 tooltip 会显示这些信息（见 {@code CardItem#appendHoverText}），
 * 卡袋羁绊也据此计算（见 {@code CardSynergy}）。
 */
public final class CardInfo
{
    /** 卡牌 id（去 _common/_gold 后缀后的拼音） */
    public final String cardId;
    /** 所属朝代，如「唐」「宋」「汉」 */
    public final String dynasty;
    /** 文物类型，如「青铜器」「书画」「瓷器」 */
    public final String type;

    public static final String UNKNOWN_DYNASTY = "未知";
    public static final String UNKNOWN_TYPE = "未知";

    private static final Map<String, CardInfo> BY_ID = new HashMap<>();

    private CardInfo(String cardId, String dynasty, String type)
    {
        this.cardId = cardId;
        this.dynasty = dynasty;
        this.type = type;
    }

    static
    {
        put("t_xing_bo_hua", "汉", "织绣");
        put("wan_he_song_feng_tu", "宋", "书画");
        put("wan_gong_jiao", "清", "家具");
        put("san_tu_zao_jing", "隋", "壁画");
        put("shang_yang_tai_tie", "唐", "书画");
        put("yun_lei_wen_da_nao", "商", "青铜器");
        put("wu_xian_pi_pa", "唐", "乐器");
        put("wu_xing_chu_dong_fang", "汉", "织绣");
        put("ya_chang_niu_zun", "商", "青铜器");
        put("tu_xing_tao_xun", "商", "乐器");
        put("li_gui", "西周", "青铜器");
        put("shi_er_hua_hui_bei", "清", "陶瓷");
        put("qian_li_jiang_shan", "宋", "书画");
        put("wu_men", "明", "建筑");
        put("juan_yun_jin_la_ba", "清", "乐器");
        put("xiang_ri_kui", "近代", "书画");
        put("shang_zhou_shi_gong", "商", "青铜器");
        put("si_long_si_feng_zuo", "战国", "青铜器");
        put("wai_xiao_bi_zhi", "清", "杂项");
        put("tian_wang_shi_ke", "唐", "石刻");
        put("tian_qiu_yi", "清", "天文仪器");
        put("tai_yang_shen_niao", "商", "金银器");
        put("song_jin_xiang_shi", "宋", "金银器");
        put("xiao_song_xiang_lu", "宋", "陶瓷");
        put("guang_cai_miao_jin_hu", "清", "陶瓷");
        put("pang_bei", "古罗马", "壁画");
        put("cai_feng_ming_qi", "唐", "乐器");
        put("wei_suo_jia_ju", "清", "家具");
        put("yuan_wang_bei", "战国", "杂项");
        put("dun_huang_fei_tian", "唐", "壁画");
        put("duan_bi_wei_na_si", "古希腊", "雕塑");
        put("xing_yue_ye", "近代", "书画");
        put("jin_hou_niao_zun", "西周", "青铜器");
        put("zeng_hou_yi_bian_zhong", "战国", "乐器");
        put("tao_yuan_xian_jing_tu", "明", "书画");
        put("mao_gong_ding", "西周", "青铜器");
        put("shui_jing_bei", "战国", "玉器");
        put("han_mo_la_bi_fa_dian", "古巴比伦", "石刻");
        put("luo_shen_fu_tu", "晋", "书画");
        put("hun_tian_yi", "明", "天文仪器");
        put("hai_shui_jiang_ya_lu", "明", "杂项");
        put("hai_cuo_tu", "清", "书画");
        put("xi_shan_xing_lv_tu", "宋", "书画");
        put("li_mao_pan", "宋", "陶瓷");
        put("wang_shi_shu_han_juan", "唐", "书画");
        put("bai_shi_san_le", "五代", "石刻");
        put("bai_hua_tu_juan", "宋", "书画");
        put("zhen_zhu_bao_chuang", "唐", "杂项");
        put("shui_lian", "近代", "书画");
        put("qiu_cao_bei", "清", "杂项");
        put("qin_gong_bo", "春秋", "乐器");
        put("su_sha_dan_yi", "汉", "织绣");
        put("fu_rong_lu", "宋", "陶瓷");
        put("mo_gao_ku_220", "唐", "壁画");
        put("mo_gao_ku_ji", "唐", "文书");
        put("lian_tang_ru_ya_tu", "宋", "织绣");
        put("fu_tao_ping", "清", "陶瓷");
        put("jia_hu_gu_di", "新石器时代", "乐器");
        put("chi_bi_fu_ye", "宋", "书画");
        put("yue_wang_gou_jian_jian", "春秋", "青铜器");
        put("jiu_zhang", "元", "文书");
        put("jin_guan_dai", "商", "金银器");
        put("jin_ou_yong_gu_bei", "清", "金银器");
        put("jin_shi_lu", "宋", "文书");
        put("jin_chan_yu_ye", "明", "金银器");
        put("liu_jin_qi_shi", "唐", "金银器");
        put("tong_zuo_long", "辽", "青铜器");
        put("tong_hu_di_lou", "元", "天文仪器");
        put("tong_ben_ma", "汉", "青铜器");
        put("tong_che_ma", "秦", "青铜器");
        put("yin_que_shan_han_jian_1", "汉", "文书");
        put("yin_que_shan_han_jian_2", "汉", "文书");
        put("yin_xiang_nang", "唐", "金银器");
        put("xue_jing_han_lin_tu", "宋", "书画");
        put("qing_ci_lian_hua_zun", "南北朝", "陶瓷");
        put("qing_tong_xian_he", "战国", "青铜器");
        put("niao_yin_shan_shui_zhong", "清", "杂项");
        put("lu_wang_ben_sheng_tu", "北魏", "壁画");
        put("lin_zhi_ma_ti_jin", "汉", "金银器");
    }

    private static void put(String cardId, String dynasty, String type)
    {
        BY_ID.put(cardId, new CardInfo(cardId, dynasty, type));
    }

    /** 按卡牌 id 查元数据（未知卡返回「未知」占位）。 */
    public static CardInfo of(String cardId)
    {
        CardInfo info = BY_ID.get(cardId);
        return info != null ? info : new CardInfo(cardId, UNKNOWN_DYNASTY, UNKNOWN_TYPE);
    }

    /**
     * 从卡牌物品栈解析元数据。
     * 卡牌物品 id 形如 {@code <拼音>_common/_gold}，这里剥掉稀有度后缀。
     */
    public static CardInfo of(ItemStack stack)
    {
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (key == null)
        {
            return of("");
        }
        String path = key.getPath();
        return of(CardNbt.stripRaritySuffix(path));
    }

    /** 该卡所属朝代。 */
    public static String dynastyOf(String cardId)
    {
        return of(cardId).dynasty;
    }

    /** 该卡文物类型。 */
    public static String typeOf(String cardId)
    {
        return of(cardId).type;
    }
}
