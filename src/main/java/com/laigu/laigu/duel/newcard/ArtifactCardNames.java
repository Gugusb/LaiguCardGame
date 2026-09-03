package com.laigu.laigu.duel.newcard;

import java.util.Map;

/** 真实文物名称基线。卡牌规则层禁止以旧版抽象效果名替代这些名称。 */
public final class ArtifactCardNames
{
    private ArtifactCardNames() {}

    private static final Map<String, String> NAMES = Map.ofEntries(
            Map.entry("t_xing_bo_hua", "T形帛画"), Map.entry("wan_he_song_feng_tu", "万壑松风图"),
            Map.entry("wan_gong_jiao", "万工轿"), Map.entry("san_tu_zao_jing", "三兔藻井"),
            Map.entry("shang_yang_tai_tie", "上阳台帖"), Map.entry("yun_lei_wen_da_nao", "云雷纹大铙"),
            Map.entry("wu_xian_pi_pa", "五弦琵琶"), Map.entry("wu_xing_chu_dong_fang", "五星出东方"),
            Map.entry("ya_chang_niu_zun", "亚长牛尊"), Map.entry("tu_xing_tao_xun", "兔形陶埙"),
            Map.entry("li_gui", "利簋"), Map.entry("shi_er_hua_hui_bei", "十二花卉杯"),
            Map.entry("qian_li_jiang_shan", "千里江山图"), Map.entry("wu_men", "午门"),
            Map.entry("juan_yun_jin_la_ba", "卷云金喇叭"), Map.entry("xiang_ri_kui", "向日葵"),
            Map.entry("shang_zhou_shi_gong", "商周十供"), Map.entry("si_long_si_feng_zuo", "四龙四凤座"),
            Map.entry("wai_xiao_bi_zhi", "外销壁纸"), Map.entry("tian_wang_shi_ke", "天王石刻"),
            Map.entry("tian_qiu_yi", "天球仪"), Map.entry("tai_yang_shen_niao", "太阳神鸟"),
            Map.entry("song_jin_xiang_shi", "宋金项饰"), Map.entry("xiao_song_xiang_lu", "小宋香炉"),
            Map.entry("guang_cai_miao_jin_hu", "广彩描金壶"), Map.entry("pang_bei", "庞贝"),
            Map.entry("cai_feng_ming_qi", "彩凤鸣岐"), Map.entry("wei_suo_jia_ju", "微缩家具"),
            Map.entry("yuan_wang_bei", "愿望杯"), Map.entry("dun_huang_fei_tian", "敦煌飞天"),
            Map.entry("duan_bi_wei_na_si", "断臂维纳斯"), Map.entry("xing_yue_ye", "星月夜"),
            Map.entry("jin_hou_niao_zun", "晋侯鸟尊"), Map.entry("zeng_hou_yi_bian_zhong", "曾侯乙编钟"),
            Map.entry("tao_yuan_xian_jing_tu", "桃源仙境图"), Map.entry("mao_gong_ding", "毛公鼎"),
            Map.entry("shui_jing_bei", "水晶杯"), Map.entry("han_mo_la_bi_fa_dian", "汉谟拉比法典"),
            Map.entry("luo_shen_fu_tu", "洛神赋图"), Map.entry("hun_tian_yi", "浑天仪"),
            Map.entry("hai_shui_jiang_ya_lu", "海水江崖炉"), Map.entry("hai_cuo_tu", "海错图"),
            Map.entry("xi_shan_xing_lv_tu", "溪山行旅图"), Map.entry("li_mao_pan", "狸猫盘"),
            Map.entry("wang_shi_shu_han_juan", "王氏书翰卷"), Map.entry("bai_shi_san_le", "白石散乐"),
            Map.entry("bai_hua_tu_juan", "百花图卷"), Map.entry("zhen_zhu_bao_chuang", "真珠宝幢"),
            Map.entry("shui_lian", "睡莲"), Map.entry("qiu_cao_bei", "秋操杯"),
            Map.entry("qin_gong_bo", "秦公镈"), Map.entry("su_sha_dan_yi", "素纱单衣"),
            Map.entry("fu_rong_lu", "芙蓉炉"), Map.entry("mo_gao_ku_220", "莫高窟220"),
            Map.entry("mo_gao_ku_ji", "莫高窟记"), Map.entry("lian_tang_ru_ya_tu", "莲塘乳鸭图"),
            Map.entry("fu_tao_ping", "蝠桃瓶"), Map.entry("jia_hu_gu_di", "贾湖骨笛"),
            Map.entry("chi_bi_fu_ye", "赤壁赋页"), Map.entry("yue_wang_gou_jian_jian", "越王勾践剑"),
            Map.entry("jiu_zhang", "酒帐"), Map.entry("jin_guan_dai", "金冠带"),
            Map.entry("jin_ou_yong_gu_bei", "金瓯永固杯"), Map.entry("jin_shi_lu", "金石录"),
            Map.entry("jin_chan_yu_ye", "金蝉玉叶"), Map.entry("liu_jin_qi_shi", "鎏金骑士"),
            Map.entry("tong_zuo_long", "铜坐龙"), Map.entry("tong_hu_di_lou", "铜壶滴漏"),
            Map.entry("tong_ben_ma", "铜奔马"), Map.entry("tong_che_ma", "铜车马"),
            Map.entry("yin_que_shan_han_jian_1", "银雀山汉简_1"), Map.entry("yin_que_shan_han_jian_2", "银雀山汉简_2"),
            Map.entry("yin_xiang_nang", "银香囊"), Map.entry("xue_jing_han_lin_tu", "雪景寒林图"),
            Map.entry("qing_ci_lian_hua_zun", "青瓷莲花尊"), Map.entry("qing_tong_xian_he", "青铜仙鹤"),
            Map.entry("niao_yin_shan_shui_zhong", "鸟音山水钟"), Map.entry("lu_wang_ben_sheng_tu", "鹿王本生图"),
            Map.entry("lin_zhi_ma_ti_jin", "麟趾马蹄金")
    );

    public static String commonName(String artifactId)
    {
        String name = NAMES.get(artifactId);
        if (name == null) throw new IllegalArgumentException("未知文物 ID：" + artifactId);
        return name;
    }

    public static String variantName(String artifactId, CardRarity rarity)
    {
        String name = commonName(artifactId);
        return rarity == CardRarity.GOLD ? name + "·金质" : name;
    }

    public static Map<String, String> all() { return NAMES; }
}
