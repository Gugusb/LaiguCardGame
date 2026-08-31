package com.laigu.laigu.duel.newcard;

import com.laigu.laigu.duel.newcard.cards.QingTongXianHeCommonCard;
import com.laigu.laigu.duel.newcard.cards.QingTongXianHeGoldCard;
import com.laigu.laigu.duel.newcard.cards.DunHuangFeiTianCommonCard;
import com.laigu.laigu.duel.newcard.cards.DunHuangFeiTianGoldCard;
import com.laigu.laigu.duel.newcard.cards.QianLiJiangShanCommonCard;
import com.laigu.laigu.duel.newcard.cards.QianLiJiangShanGoldCard;
import com.laigu.laigu.duel.newcard.cards.GuangCaiMiaoJinHuCommonCard;
import com.laigu.laigu.duel.newcard.cards.GuangCaiMiaoJinHuGoldCard;
import com.laigu.laigu.duel.newcard.cards.XiShanXingLvTuCommonCard;
import com.laigu.laigu.duel.newcard.cards.XiShanXingLvTuGoldCard;

/** 集中注册新架构卡牌类。 */
public final class CardRegistry
{
    private static boolean initialized;

    private CardRegistry()
    {
    }

    public static synchronized void initialize()
    {
        if (initialized) return;
        CardFactory.register("qing_tong_xian_he_common", QingTongXianHeCommonCard::new);
        CardFactory.register("qing_tong_xian_he_gold", QingTongXianHeGoldCard::new);
        CardFactory.register("dun_huang_fei_tian_common", DunHuangFeiTianCommonCard::new);
        CardFactory.register("dun_huang_fei_tian_gold", DunHuangFeiTianGoldCard::new);
        CardFactory.register("qian_li_jiang_shan_common", QianLiJiangShanCommonCard::new);
        CardFactory.register("qian_li_jiang_shan_gold", QianLiJiangShanGoldCard::new);
        CardFactory.register("guang_cai_miao_jin_hu_common", GuangCaiMiaoJinHuCommonCard::new);
        CardFactory.register("guang_cai_miao_jin_hu_gold", GuangCaiMiaoJinHuGoldCard::new);
        CardFactory.register("xi_shan_xing_lv_tu_common", XiShanXingLvTuCommonCard::new);
        CardFactory.register("xi_shan_xing_lv_tu_gold", XiShanXingLvTuGoldCard::new);
        initialized = true;
    }
}
