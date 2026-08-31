package com.laigu.laigu.duel;

import com.laigu.laigu.util.CardNbt;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 对战效果索引：文物牌 id（来古牌）→ 对战效果。
 * 对战卡就是来古牌，本目录只是「效果归谁」的连接表 —— 归谁随时可改，改这里即可。
 * 朝代取自文物元数据（CardInfo），金质取自物品稀有度后缀，均不在此处配置。
 */
public final class DuelCardCatalog
{
    public static final List<DuelCardData> CARDS = List.of(
        // ---- 攻·炽（7）----
        card("tong_che_ma",          "炎刃",   CardClass.GONG, EffectType.PER_DIE_EXTRA,                 6, 0, null,   null,       0,     "本卡每有 1 颗骰 → +6 额外分",
                EffectType.PER_DIE_MULT, 1, "本卡每有 1 颗骰 → +1 倍率"),
        card("yue_wang_gou_jian_jian","炽甲",  CardClass.GONG, EffectType.SUMMON_DRAW,                   1, 0, null,   null,       "入场时：抽 1 张牌"),
        card("wu_xian_pi_pa",        "追锋",   CardClass.GONG, EffectType.OTHER_USE_DRAW,               1, 0, null,   null,       "你使用其他手牌时：抽 1 张牌"),
        card("qian_li_jiang_shan",   "破山",   CardClass.GONG, EffectType.SUMMON_DRAW_IF_LOST_LAST,     2, 0, null,   null,       0,     "入场时：若上轮你输，抽 2 张牌",
                EffectType.FLAT_EXTRA, 10, "无条件 +10 额外分"),
        card("jin_hou_niao_zun",     "天火贯日", CardClass.GONG, EffectType.STRAIGHT_PER_DIE_MULT_CONSUME, 1, 0, null,  null,       "【消耗】若你的骰为顺子，本卡每有 1 颗骰 → +1 倍率"),
        card("chi_bi_fu_ye",         "焚野",   CardClass.GONG, EffectType.PER_DIE_BASE,                  2, 0, null,   null,       "本卡每有 1 颗骰 → +2 基础分"),
        card("tong_ben_ma",          "千钧",   CardClass.GONG, EffectType.DICE_GE2_EXTRA,               14, 0, null,   null,       "本卡骰子 ≥2 颗 → +14 额外分"),

        // ---- 守·衡（7）----
        card("qing_tong_xian_he",    "磐石",   CardClass.SHOU, EffectType.LEAVE_DRAW,                    1, 0, null,   null,       0,     "离场时：抽 1 张牌",
                EffectType.LAST_ROUND_EXTRA, 12, "上轮也在场上 → +12 额外分"),
        card("shui_jing_bei",        "静水",   CardClass.SHOU, EffectType.ROUND_START_DRAW,              1, 0, null,   null,       "每轮开始时：抽 1 张牌"),
        card("dun_huang_fei_tian",   "承影",   CardClass.SHOU, EffectType.SUMMON_DRAW,                   2, 0, null,   null,       "入场时：抽 2 张牌"),
        card("mo_gao_ku_ji",         "沉璧",   CardClass.SHOU, EffectType.ALL_LOW_EXTRA,                30, 0, null,   null,       1,     "若你的骰全小(≤3) → +30 额外分",
                EffectType.DICE_GE2_EXTRA, 20, "本卡骰子 ≥2 颗 → +20 额外分"),
        card("wan_he_song_feng_tu",  "长城",   CardClass.SHOU, EffectType.FLAT_EXTRA,                    6, 0, null,   null,       1,     "无条件 +6 额外分"),
        card("cai_feng_ming_qi",     "守望",   CardClass.SHOU, EffectType.LAST_ROUND_EXTRA,             12, 0, null,   null,       "上轮也在场上 → +12 额外分"),
        card("shang_yang_tai_tie",   "不动",   CardClass.SHOU, EffectType.ZERO_DICE_EXTRA,              14, 0, null,   null,       "本卡骰子为 0 → +14 额外分"),

        // ---- 谋·策（8）----
        card("si_long_si_feng_zuo",  "观星",   CardClass.MOU, EffectType.SUMMON_RESTORE_AP,              1, 0, null,   null,       0,     "入场时：回复 1 点行动力",
                EffectType.FLAT_EXTRA, 8, "无条件 +8 额外分"),
        card("lu_wang_ben_sheng_tu", "洞察",   CardClass.MOU, EffectType.ALL_ODD_MULT,                   2, 0, null,   null,       1,     "若你的骰全奇 → +2 倍率"),
        card("xiao_song_xiang_lu",   "侧击",   CardClass.MOU, EffectType.ROUND_END_DRAW_IF_WIN,          2, 0, null,   null,       0,     "结算后：若本轮你赢，抽 2 张牌",
                EffectType.WIN_LAST_MULT, 1, "上轮你赢 → +1 倍率"),
        card("xiang_ri_kui",         "孤隐",   CardClass.MOU, EffectType.ISOLATED_MULT_EXTRA,            1, 10, null,  null,       1,     "两侧均非本卡朝代 → +1 倍率 +10 额外分"),
        card("song_jin_xiang_shi",   "藏锋",   CardClass.MOU, EffectType.ROUND_START_DRAW_STAY_TURNS,    0, 0, null,   null,       "每轮开始时：抽 x 张牌（x = 本卡站场轮数）"),
        card("yin_que_shan_han_jian_1","天机", CardClass.MOU, EffectType.STRAIGHT_MULT,                  4, 0, null,   null,       1,     "若你的骰为顺子 → +4 倍率"),
        card("lian_tang_ru_ya_tu",   "连环",   CardClass.MOU, EffectType.ROUND_END_DRAW_IF_LOSE,         2, 0, null,   null,       "结算后：若本轮你输，抽 2 张牌"),
        card("jin_ou_yong_gu_bei",   "反观",   CardClass.MOU, EffectType.ALL_EVEN_EXTRA,                30, 0, null,   null,       1,     "若你的骰全偶 → +30 额外分"),

        // ---- 鼎·盛（8）----
        card("jin_shi_lu",           "敕令",   CardClass.DING, EffectType.DYN_CNT_BASE,                  0, 2, "唐",   null,       0,     "我方每有 1 张唐代卡 → +2 基础分",
                EffectType.GOLD_EXTRA, 10, "每张金质卡 → +10 额外分"),
        card("xue_jing_han_lin_tu",  "泰然",   CardClass.DING, EffectType.CLASS_CNT_MULT,                0, 1, null,   CardClass.DING, 0,     "我方每有 1 张鼎·盛卡 → +1 倍率",
                EffectType.GOLD_CNT_MULT, 1, "每张金质卡 → +1 倍率"),
        card("mo_gao_ku_220",        "承前",   CardClass.DING, EffectType.ROUND_START_DRAW,              1, 0, null,   null,       "每轮开始时：抽 1 张牌"),
        card("luo_shen_fu_tu",       "倍增",   CardClass.DING, EffectType.BASE_DOUBLE_CONSUME,           0, 0, null,   null,       0,     "【消耗】结算时 → 本轮基础分 ×2",
                EffectType.CARD_CNT_BASE, 2, "我方每有 1 张卡 → +2 基础分"),
        card("yuan_wang_bei",        "治世",   CardClass.DING, EffectType.CARD_CNT_BASE,                 1, 0, null,   null,       0,     "我方每有 1 张卡 → +1 基础分",
                EffectType.POOL_CNT_MULT, 1, "每颗未布置骰 → +1 倍率"),
        card("tian_wang_shi_ke",     "长歌",   CardClass.DING, EffectType.ALL_HIGH_MULT,                 3, 0, null,   null,       1,     "若你的骰全大(≥4) → +3 倍率"),
        card("yin_que_shan_han_jian_2","开疆", CardClass.DING, EffectType.CLASS_CNT_BASE,                0, 2, null,   CardClass.GONG, "我方每有 1 张攻·炽卡 → +2 基础分"),
        card("tai_yang_shen_niao",   "盛世",   CardClass.DING, EffectType.GOLD_CNT_MULT,                 1, 0, null,   null,       "我方每有 1 张金质卡 → +1 倍率"),

        // ---- 抓取系（抢骰阶段生效，改变抓取计划）----
        card("hun_tian_yi",          "浑天仪", CardClass.MOU, EffectType.DRAFT_POOL_UP,                 3, 0, null,   null,       "【抢骰】系统掷出的骰子 +3 颗（共享骰池扩容）"),
        card("tian_qiu_yi",          "天球仪", CardClass.MOU, EffectType.DRAFT_TURNS_DOWN,              1, 0, null,   null,       "【抢骰】双方各少抓 1 次骰"),
        card("xing_yue_ye",          "星月夜", CardClass.DING, EffectType.DRAFT_TURNS_UP,               1, 0, null,   null,       "【抢骰】双方各多抓 1 次骰"),

        // ================= 第 2 批（50 新效果 / 见 docs/对战-效果设计.md） =================

        // ---- 攻·炽（11）：吃骰质变 + 献祭 ----
        card("t_xing_bo_hua",        "梯火",   CardClass.GONG, EffectType.DIE_FIRST_BONUS,               4, 6, null,  null,       "本卡第 1 颗骰 +4，第 2 颗再 +6 额外分"),
        card("wan_gong_jiao",        "奇袭",   CardClass.GONG, EffectType.ODD_DIE_EXTRA,                8, 0, null,   null,       "本卡每颗奇数骰 → +8 额外分"),
        card("san_tu_zao_jing",      "偶攻",   CardClass.GONG, EffectType.EVEN_DIE_EXTRA,                8, 0, null,   null,       "本卡每颗偶数骰 → +8 额外分"),
        card("yun_lei_wen_da_nao",   "雷击",   CardClass.GONG, EffectType.DIE_GE4_EXTRA,                12, 0, null,   null,       "本卡每颗 ≥4 骰 → +12 额外分"),
        card("wu_xing_chu_dong_fang","聚气",   CardClass.GONG, EffectType.DIE_LE3_EXTRA,                12, 0, null,   null,       "本卡每颗 ≤3 骰 → +12 额外分"),
        card("ya_chang_niu_zun",     "牛威",   CardClass.GONG, EffectType.DIE_SUM_GE_EXTRA,             8, 22, null,  null,       "本卡骰面和 ≥8 → +22 额外分"),
        card("tu_xing_tao_xun",      "锋刃",   CardClass.GONG, EffectType.DICE_GE1_EXTRA,               12, 0, null,   null,       "本卡 ≥1 颗骰 → +12 额外分"),
        card("li_gui",               "双子",   CardClass.GONG, EffectType.SAME_FACE_MULT,                2, 0, null,   null,       "本卡 2 颗骰相同 → +2 倍率"),
        card("shi_er_hua_hui_bei",   "曜阳",   CardClass.GONG, EffectType.DIE_SUM_ODD_EXTRA,            14, 0, null,   null,       "本卡骰面和为奇数 → +14 额外分"),
        card("wu_men",               "破门",   CardClass.GONG, EffectType.CONSUME_PER_DIE_BASE,          3, 0, null,   null,       "【消耗】本卡每颗骰 → +3 基础分"),
        card("juan_yun_jin_la_ba",   "号角",   CardClass.GONG, EffectType.CONSUME_OPP_DICE_EXTRA,        3, 0, null,   null,       "【消耗】对手每颗骰 → +3 额外分"),

        // ---- 守·衡（10）：对位·顺势·持久 ----
        card("shang_zhou_shi_gong",  "坚垒",   CardClass.SHOU, EffectType.OPP_MORE_DICE_EXTRA,          15, 0, null,   null,       1,     "对手放置骰比你多 → +15 额外分"),
        card("wai_xiao_bi_zhi",      "满阵",   CardClass.SHOU, EffectType.OPP_FIELD_FULL_EXTRA,         20, 0, null,   null,       1,     "对手场上满 4 张 → +20 额外分"),
        card("guang_cai_miao_jin_hu","逆鳞",   CardClass.SHOU, EffectType.LOSE_LAST_EXTRA,              18, 0, null,   null,       1,     "上轮你输 → +18 额外分"),
        card("pang_bei",             "僵局",   CardClass.SHOU, EffectType.DRAW_LAST_EXTRA,              30, 0, null,   null,       1,     "上轮平局 → +30 额外分"),
        card("wei_suo_jia_ju",       "势乘",   CardClass.SHOU, EffectType.WIN_LAST_MULT,                 1, 0, null,   null,       1,     "上轮你赢 → +1 倍率"),
        card("duan_bi_wei_na_si",    "反超",   CardClass.SHOU, EffectType.BEHIND_WINS_EXTRA,            20, 0, null,   null,       1,     "总局数落后 → +20 额外分"),
        card("zeng_hou_yi_bian_zhong","重钟",  CardClass.SHOU, EffectType.CONSUME_EXTRA_DOUBLE,          2, 0, null,   null,       "【消耗】本轮额外分 ×2"),
        card("tao_yuan_xian_jing_tu","老兵",   CardClass.SHOU, EffectType.LASTED_2_EXTRA,               16, 0, null,   null,       "连续在场 ≥2 轮 → +16 额外分"),
        card("mao_gong_ding",        "积淀",   CardClass.SHOU, EffectType.ROUND_GE2_EXTRA,              12, 0, null,   null,       "轮次 ≥2 → +12 额外分"),
        card("han_mo_la_bi_fa_dian", "空手",   CardClass.SHOU, EffectType.HAND_EMPTY_EXTRA,             20, 0, null,   null,       "手牌为 0 → +20 额外分"),

        // ---- 谋·策（12）：全盘骰型 + 槽位阵型 + 献祭 ----
        card("hai_shui_jiang_ya_lu", "对偶",   CardClass.MOU, EffectType.TWO_PAIR_MULT,                  3, 0, null,   null,       1,     "你的骰有两对 → +3 倍率"),
        card("hai_cuo_tu",           "满堂",   CardClass.MOU, EffectType.FULL_HOUSE_MULT,                5, 0, null,   null,       1,     "你的骰满堂彩（三同+一对）→ +5 倍率"),
        card("xi_shan_xing_lv_tu",   "归一",   CardClass.MOU, EffectType.ALL_SAME_MULT,                  5, 0, null,   null,       1,     "你的骰全相同（≥3 颗）→ +5 倍率"),
        card("li_mao_pan",           "含六",   CardClass.MOU, EffectType.HAS_SIX_EXTRA,                 10, 0, null,   null,       1,     "你的骰含 6 → +10 额外分"),
        card("wang_shi_shu_han_juan","两极",   CardClass.MOU, EffectType.SUM_RANGE_EXTRA,               18, 0, null,   null,       1,     "你的骰面和 ≤9 或 ≥18 → +18 额外分"),
        card("bai_shi_san_le",       "连珠",   CardClass.MOU, EffectType.CONSEC_NEAR_EXTRA,             10, 0, null,   null,       1,     "你的骰含相邻两数 → +10 额外分"),
        card("bai_hua_tu_juan",      "伏击·百花", CardClass.MOU, EffectType.FUJI,                   0, 0, null,   null,       0,     "伏击：成功时复制对位骰；失败无收益")
                 .fuJi(null, 0, null, 0)
                 .goldAmbushCopyDice()
                 .goldAmbushFiveBonus(),
        card("zhen_zhu_bao_chuang",  "中枢",   CardClass.MOU, EffectType.CENTER_MULT,                    1, 0, null,   null,       1,     "本卡在中间槽位(1/2) → +1 倍率"),
        card("shui_lian",            "伏击·睡莲", CardClass.MOU, EffectType.FUJI,                   0, 0, null,   null,       0,     "伏击：成功时使对位前2颗骰无效；失败无收益")
                 .fuJiInvalidate(2)
                 .fuJi(null, 0, null, 0),
        card("qiu_cao_bei",          "异阵",   CardClass.MOU, EffectType.ADJ_DIFF_CLASS_EXTRA,          16, 0, null,   null,       1,     "相邻有不同职业卡 → +16 额外分"),
        card("qin_gong_bo",          "同代",   CardClass.MOU, EffectType.ADJ_SAME_DYN_EXTRA,            16, 0, null,   null,       1,     "相邻有同朝代卡 → +16 额外分"),
        card("lin_zhi_ma_ti_jin",    "青花智", CardClass.MOU, EffectType.CONSUME_HAND_MULT,              1, 0, null,   null,       "【消耗】每张手牌 → +1 倍率"),

        // ---- 鼎·盛（10）：资源引擎 + 金质引擎 + 朝代 ----
        card("su_sha_dan_yi",        "握权",   CardClass.DING, EffectType.HAND_CNT_MULT,                 1, 0, null,   null,       "每张手牌 → +1 倍率"),
        card("fu_rong_lu",           "存骰",   CardClass.DING, EffectType.POOL_CNT_BASE,                 3, 0, null,   null,       "每颗未布置骰 → +3 基础分"),
        card("fu_tao_ping",          "蓄骰",   CardClass.DING, EffectType.POOL_CNT_MULT,                 1, 0, null,   null,       "每颗未布置骰 → +1 倍率"),
        card("jia_hu_gu_di",         "余烬",   CardClass.DING, EffectType.DECK_CNT_BASE,                 1, 0, null,   null,       "每张未抽卡组牌 → +1 基础分"),
        card("jiu_zhang",            "共池",   CardClass.DING, EffectType.SHARED_POOL_EXTRA,             2, 0, null,   null,       "共享骰池每颗 → +2 额外分"),
        card("jin_guan_dai",         "金辉",   CardClass.DING, EffectType.GOLD_CNT_BASE,                 8, 0, null,   null,       "每张金质卡 → +8 基础分"),
        card("jin_chan_yu_ye",       "鎏金",   CardClass.DING, EffectType.GOLD_DIE_MULT,                 1, 0, null,   null,       "每颗放在金质卡上的骰 → +1 倍率"),
        card("liu_jin_qi_shi",       "盛象",   CardClass.DING, EffectType.GOLD_DYN_MULT,                 1, 0, null,   null,       "若你有金质卡 → +1 倍率"),
        card("tong_zuo_long",        "金铢",   CardClass.DING, EffectType.GOLD_EXTRA,                   20, 0, null,   null,       "每张金质卡 → +20 额外分"),
        card("niao_yin_shan_shui_zhong","同代应", CardClass.DING, EffectType.ROUND_END_DRAW_IF_LOSE,    2, 0, null,   null,       "结算后：若本轮你输，抽 2 张牌"),

        // ---- 抢骰系（3）：单方扰动 ----
        card("tong_hu_di_lou",       "多势",   CardClass.MOU, EffectType.DRAFT_SELF_TURNS_UP,            1, 0, null,   null,       "【抢骰】本方抓取次数 +1"),
        card("yin_xiang_nang",       "压制",   CardClass.MOU, EffectType.DRAFT_OPP_TURNS_DOWN,           1, 0, null,   null,       "【抢骰】对方抓取次数 -1"),
        card("qing_ci_lian_hua_zun", "连拾",   CardClass.MOU, EffectType.DRAFT_SELF_GRAB_UP,             1, 0, null,   null,       "【抢骰】本方每次抓取颗数 +1"),

        // ---- 第 3 批：新词条（破阵/伏击/激活/激活触发）设计卡（覆盖部分已用 id；数值初版） ----

        // 【破阵】攻·炽（4）：本槽位骰面和 > 对手同槽位 → 削弱其 50%（引擎统一处理）；金卡带独立焕章
        card("ya_chang_niu_zun",     "破阵·牛尊", CardClass.GONG, EffectType.PO_ZHEN_HALVE,              0, 0, null,   null, 0,
                "破阵：本槽位骰面和 > 对手同槽位 → 削弱其 50%",
                EffectType.FLAT_EXTRA, 20, "破阵触发时削弱100%并 +20 额外分").poZhenFull(),
        card("jin_hou_niao_zun",     "破阵·鸟尊", CardClass.GONG, EffectType.PO_ZHEN_HALVE,              0, 0, null,   null, 0,
                "破阵：本槽位骰面和 > 对手同槽位 → 削弱其 50%",
                EffectType.STRAIGHT_MULT, 1, "破阵触发时 +1 倍率"),
        card("yue_wang_gou_jian_jian","破阵·越剑", CardClass.GONG, EffectType.PO_ZHEN_HALVE,             0, 0, null,   null, 0,
                "破阵：本槽位骰面和 > 对手同槽位 → 削弱其 50%",
                EffectType.FLAT_EXTRA, 30, "破阵触发时 +30 额外分"),
        card("yuan_wang_bei",        "破阵·越杯", CardClass.GONG, EffectType.PO_ZHEN_HALVE,              0, 0, null,   null, 0,
                "破阵：本槽位骰面和 > 对手同槽位 → 削弱其 50%",
                EffectType.FLAT_EXTRA, 10, "破阵触发时 +10 额外分"),

        // 【伏击】谋·策（3）：计分揭晓；对位有牌→成功（数值按对位卡骰数缩放，反制高强度对位）/ 无卡→失败（额外分）；金卡拉 焕章 额外效果
        card("dun_huang_fei_tian",   "伏击·飞天", CardClass.MOU,  EffectType.FUJI,                        0, 0, null,   null, 0,
                "伏击：成功无收益；失败+30额外分",
                EffectType.PER_DIE_EXTRA, 4, "成功时每颗对位骰 +4 额外分")
                .fuJi(null, 0, EffectType.FLAT_EXTRA, 30),
        card("bai_hua_tu_juan",      "伏击·百花", CardClass.MOU,  EffectType.FUJI,                        0, 0, null,   null, 0,
                "伏击：成功时对位获基础分→我获一半基础分；无牌→失败+25额外分",
                EffectType.PER_DIE_MULT, 1, "对位获倍率时我获一半倍率")
                .fuJi(null, 0, EffectType.FLAT_EXTRA, 25)
                .fuJiMirror(true, true),
        card("shui_lian",            "伏击·睡莲", CardClass.MOU,  EffectType.FUJI,                        0, 0, null,   null, 0,
                "伏击：成功时对位前2骰无效化；无牌→失败+35额外分",
                EffectType.FLAT_EXTRA, 10, "收回被无效化骰子的基础分")
                .fuJiInvalidate(2)
                .fuJi(null, 0, EffectType.FLAT_EXTRA, 35),

        // 【激活】鼎·盛（4）：被激活累计满 x → 触发奖励；金卡带独立焕章
        cardActivateGold("qian_li_jiang_shan", "激活·江山", CardClass.DING, EffectType.FLAT_EXTRA, 0, 0, "激活2：倍率+2", 2, EffectType.STRAIGHT_MULT, 2,
                EffectType.STRAIGHT_MULT, 2, "焕章：激活达成时再 +2 倍率"),
        cardActivateGold("zeng_hou_yi_bian_zhong", "激活·编钟", CardClass.DING, EffectType.FLAT_EXTRA, 0, 0, "激活3：每颗骰+1倍率", 3, EffectType.PER_DIE_MULT, 1,
                EffectType.PER_DIE_MULT, 1, "焕章：激活达成时每颗骰 +1 倍率"),
        cardActivateGold("guang_cai_miao_jin_hu", "激活·描金壶", CardClass.DING, EffectType.FLAT_EXTRA, 0, 0, "激活1：+3基础分", 1, EffectType.PER_DIE_BASE, 3,
                EffectType.FLAT_EXTRA, 50, "焕章：激活达成时 +50 额外分"),
        cardActivateGold("jin_ou_yong_gu_bei", "激活·永固杯", CardClass.DING, EffectType.FLAT_EXTRA, 0, 0, "激活2：每张金质卡+1倍率", 2, EffectType.GOLD_CNT_MULT, 1,
                EffectType.GOLD_CNT_MULT, 2, "焕章：激活达成时每张金质卡 +2 倍率")
                .activateLeftOnReach(),

        // 【激活触发】给左/右侧相邻卡「激活进度+1」；金卡带独立焕章
        card("xi_shan_xing_lv_tu",   "激活左·溪山", CardClass.SHOU, EffectType.ACTIVATE_LEFT,            0, 0, null,   null, -1, "激活左侧卡牌")
                .fuJiActivateBonus(5),
        card("hun_tian_yi",          "激活左·浑天", CardClass.MOU,  EffectType.ACTIVATE_LEFT,            0, 0, null,   null, 0, "若牌型为顺子，激活右侧卡牌3次")
                .activateRightOnStraight(3),
        card("hai_cuo_tu",           "激活左·海错", CardClass.SHOU, EffectType.ACTIVATE_LEFT,            0, 0, null,   null, 3, "【充能3】激活左侧卡牌")
                .goldRightActivate(),

        // 【激活右·朝代联动】触发右侧卡牌 x 次（x=我方场上最多朝代数）
        card("lu_wang_ben_sheng_tu", "激活右·朝代", CardClass.MOU,  EffectType.ACTIVATE_LEFT,            0, 0, null,   null, 0, "触发右侧卡牌 x 次（x=我方场上最多朝代数）")
                .activateRightByDynastyMax(),

        // ---- 其它机制测试（重骰 / 抓取得分 时机） ----
        card("niao_yin_shan_shui_zhong","重骰·试", CardClass.MOU, EffectType.REROLL_ON_DRAFT,           0, 0, null,   null,       "重骰：抓骰时重骰共享池 > 抓取点数 的骰（本轮限一次）"),
        card("qing_ci_lian_hua_zun", "抓取得分·试", CardClass.MOU, EffectType.DRAFT_SCORE_EXTRA,         5, 0, null,   null,       "抓骰时获得 (6-点数)×5 额外分")
    );

    private static final Map<String, DuelCardData> BY_ID = new HashMap<>();

    static
    {
        for (DuelCardData d : CARDS)
        {
            BY_ID.put(d.cardId, d);
        }
    }

    private DuelCardCatalog() {}

    private static DuelCardData card(String cardId, String name, CardClass cls, EffectType effect,
                                     int p1, int p2, String targetDynasty, CardClass targetClass, String desc)
    {
        return card(cardId, name, cls, effect, p1, p2, targetDynasty, targetClass, 0, desc);
    }

    /**
     * 带【充能】的效果：charge &gt; 0 时本卡需有 ≥1 颗骰才发挥后续效果；
     * charge = x 时效果按 x 次重复。效果描述自动前置「【充能x】」关键词。
     */
    private static DuelCardData card(String cardId, String name, CardClass cls, EffectType effect,
                                     int p1, int p2, String targetDynasty, CardClass targetClass,
                                     int charge, String desc)
    {
        return card(cardId, name, cls, effect, p1, p2, targetDynasty, targetClass, charge, desc,
                null, 0, null);
    }

    /**
     * 带【充能】与金卡特殊效果：金卡在结算阶段额外触发一次（goldDesc 自动前置「金卡特效：」）。
     * 金卡效果只写在金卡上，白卡永不触发。
     */
    private static DuelCardData card(String cardId, String name, CardClass cls, EffectType effect,
                                     int p1, int p2, String targetDynasty, CardClass targetClass,
                                     int charge, String desc,
                                     EffectType goldEffect, int goldP1, String goldDesc)
    {
        if (charge == -1)
        {
            desc = "【充能x】" + desc;   // 充能x：每有一颗骰触发一次
        }
        else if (charge > 0)
        {
            desc = "【充能" + charge + "】" + desc;   // 充能N：至少 N 颗骰才触发一次
        }
        if (goldDesc != null)
        {
            goldDesc = "焕章：" + goldDesc;
        }
        return new DuelCardData(cardId, name, cls, effect, p1, p2, targetDynasty, targetClass,
                charge, desc, goldEffect, goldP1, 0, goldDesc);
    }

    /** 带【激活x】目标的卡：activateCap>0 时，被「激活左侧」效果累积；达到 cap 结算 activateReward（累加额外分）并清零。 */
    private static DuelCardData cardActivate(String cardId, String name, CardClass cls, EffectType effect,
                                             int p1, int p2, String desc,
                                             int activateCap, EffectType activateReward, int activateP1)
    {
        return new DuelCardData(cardId, name, cls, effect, p1, p2, null, null, 0, desc,
                null, 0, 0, null, activateCap, activateReward, activateP1);
    }

    /** 带【激活x】目标 + 独立焕章（金卡额外效果）的卡。 */
    private static DuelCardData cardActivateGold(String cardId, String name, CardClass cls, EffectType effect,
                                                 int p1, int p2, String desc,
                                                 int activateCap, EffectType activateReward, int activateP1,
                                                 EffectType goldEffect, int goldP1, String goldDesc)
    {
        return new DuelCardData(cardId, name, cls, effect, p1, p2, null, null, 0, desc,
                goldEffect, goldP1, 0, goldDesc, activateCap, activateReward, activateP1);
    }

    /** 按文物牌 id 查对战效果；未配置返回 null。 */
    public static DuelCardData byId(String cardId)
    {
        return BY_ID.get(cardId);
    }    /** 从来古牌物品栈解析对战效果；非来古牌或未配置效果返回 null。 */
    public static DuelCardData of(ItemStack stack)
    {
        if (stack == null || stack.isEmpty())
        {
            return null;
        }
        String path = CardNbt.pathOf(stack);
        return BY_ID.get(CardNbt.stripRaritySuffix(path));
    }
}
