package com.laigu.laigu.duel.newcard;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** 阶段13：卡面描述新系统化——书目覆盖全部注册卡、设计变更卡文案活断言。 */
class Stage13DescriptionTest
{
    @Test
    void bookCoversEveryRegisteredCardWithNonEmptyDescription()
    {
        List<String> gaps = new ArrayList<>();
        for (String id : CardFactory.registeredIds())
        {
            DuelCard card = CardFactory.create(id);
            if (card == null)
            {
                gaps.add(id + ":create-null");
                continue;
            }
            if (card.description() == null || card.description().isEmpty())
                gaps.add(id + ":empty-description");
            if (id.endsWith("_gold"))
            {
                String g = card.goldDescription();
                if (g == null || g.isEmpty()) gaps.add(id + ":empty-gold-description");
            }
        }
        assertTrue(gaps.isEmpty(), "描述缺失: " + gaps);
    }

    @Test
    void designChangedCardsCarryCorrectedTexts()
    {
        // 清单口径：金卡独立成卡，主效果+焕章均直接取清单文本
        DuelCard jiangshan = CardFactory.create("qian_li_jiang_shan_gold");
        assertEquals("激活2：+4 倍率", jiangshan.description());
        assertEquals("焕章：我方有卡被激活时 +5 额外分", jiangshan.goldDescription());

        // 永固杯金
        DuelCard cup = CardFactory.create("jin_ou_yong_gu_bei_gold");
        assertEquals("激活2：获得2*x倍率，x为对方场上未放有骰子的卡牌数", cup.description());
        assertEquals("焕章：回合结算时，我方每个金色卡牌提供+1倍率", cup.goldDescription());

        // 编钟金
        DuelCard zhong = CardFactory.create("zeng_hou_yi_bian_zhong_gold");
        assertEquals("激活3：本卡上每颗骰 +2 倍率", zhong.description());
        assertEquals("焕章：入场：本回合抓骰次数+1", zhong.goldDescription());

        // 浑天金
        DuelCard huntian = CardFactory.create("hun_tian_yi_gold");
        assertEquals("我方牌型为顺子时，激活右侧卡牌6次", huntian.description());
        assertEquals("焕章：我方顺子可以间隔1", huntian.goldDescription());

        // 睡莲金
        DuelCard shuilian = CardFactory.create("shui_lian_gold");
        assertEquals("伏击：成功时使对位前4颗骰无效；失败无收益", shuilian.description());
        assertEquals("焕章：伏击成功时收回被无效骰对应的基础分", shuilian.goldDescription());
    }

    @Test
    void goldValueCardsShowScaledMainText()
    {
        // 清单口径：分值类金卡主文案直接取清单数值（不再按 VALUE_SPECS 缩放）
        assertEquals("每颗本卡骰 +12 额外分",
                CardFactory.create("tong_che_ma_gold").description());
        assertEquals("无条件 +20 额外分",
                CardFactory.create("wan_he_song_feng_tu_gold").description());
        assertEquals("每轮开始按站场轮数+1抽牌（最大抽3）",
                CardFactory.create("song_jin_xiang_shi_gold").description());
        // 普通卡焕章行为空串
        assertEquals("", CardFactory.create("tong_che_ma_common").goldDescription());
    }

    @Test
    void descriptionBookIsIndependentFromLegacyCatalog()
    {
        // 书目是静态快照，不依赖旧目录运行期数据（仅抽验两条与清单一致）
        assertEquals("激活2：+4 倍率", CardDescriptionBook.description("qian_li_jiang_shan_gold"));
        assertEquals("充能2：激活我方所有卡牌1次", CardDescriptionBook.description("hai_cuo_tu_common"));
    }
}
