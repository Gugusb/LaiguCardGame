package com.laigu.laigu.duel.newcard;

import com.laigu.laigu.duel.newcard.cards.SanTuZaoJingCommonCard;
import com.laigu.laigu.duel.newcard.cards.SanTuZaoJingGoldCard;
import com.laigu.laigu.duel.newcard.cards.ShangYangTaiTieCommonCard;
import com.laigu.laigu.duel.newcard.cards.ShangYangTaiTieGoldCard;
import com.laigu.laigu.duel.newcard.cards.ShiErHuaHuiBeiCommonCard;
import com.laigu.laigu.duel.newcard.cards.ShiErHuaHuiBeiGoldCard;
import com.laigu.laigu.duel.newcard.cards.TXingBoHuaCommonCard;
import com.laigu.laigu.duel.newcard.cards.TXingBoHuaGoldCard;
import com.laigu.laigu.duel.newcard.cards.TuXingTaoXunCommonCard;
import com.laigu.laigu.duel.newcard.cards.TuXingTaoXunGoldCard;
import com.laigu.laigu.duel.newcard.cards.WanGongJiaoCommonCard;
import com.laigu.laigu.duel.newcard.cards.WanGongJiaoGoldCard;
import com.laigu.laigu.duel.newcard.cards.WanHeSongFengTuCommonCard;
import com.laigu.laigu.duel.newcard.cards.WanHeSongFengTuGoldCard;
import com.laigu.laigu.duel.newcard.cards.WuXingChuDongFangCommonCard;
import com.laigu.laigu.duel.newcard.cards.WuXingChuDongFangGoldCard;
import com.laigu.laigu.duel.newcard.cards.YunLeiWenDaNaoCommonCard;
import com.laigu.laigu.duel.newcard.cards.YunLeiWenDaNaoGoldCard;
import com.laigu.laigu.duel.newcard.cards.ZhenZhuBaoChuangCommonCard;
import com.laigu.laigu.duel.newcard.cards.ZhenZhuBaoChuangGoldCard;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 阶段15：原直映射卡独立 OnSettlement 实现的行为断言（普通+金卡缩放）。 */
class Stage15ExplicitSettlementTest
{

    private ScoreSnapshot score(DuelCard card, java.util.List<Integer> dice, int slot)
    {
        NewCardBattle battle = new NewCardBattle();
        battle.placeCard(0, slot, card);
        battle.state().cardStateAt(0, slot).setDice(dice);
        return NewSettlementCalculator.calculate(battle, new SettlementRuleRegistry());
    }

    @Test
    void flatExtraAndGe1Dice()
    {
        // 万壑松风图：无条件 +10/+20 额外分（清单为准）。
        assertEquals(10, score(new WanHeSongFengTuCommonCard(), List.of(2, 5), 0).sides().get(0).extra());
        assertEquals(20, score(new WanHeSongFengTuGoldCard(), List.of(2, 5), 0).sides().get(0).extra());
        // 土形陶埙：≥1 颗骰 → +12/+24。
        assertEquals(12, score(new TuXingTaoXunCommonCard(), List.of(2, 5), 0).sides().get(0).extra());
        assertEquals(24, score(new TuXingTaoXunGoldCard(), List.of(2, 5), 0).sides().get(0).extra());
        assertEquals(0, score(new TuXingTaoXunCommonCard(), List.of(), 0).sides().get(0).extra());
    }

    @Test
    void oddEvenAndThresholdDice()
    {
        // 万工轿：每颗匹配骰 +12/+24（清单口径）。
        assertEquals(24, score(new WanGongJiaoCommonCard(), List.of(1, 2, 5), 0).sides().get(0).extra());
        assertEquals(48, score(new WanGongJiaoGoldCard(), List.of(1, 2, 5), 0).sides().get(0).extra());
        // 三兔藻井：每颗偶数骰 +8/+16。
        assertEquals(16, score(new SanTuZaoJingCommonCard(), List.of(1, 2, 4), 0).sides().get(0).extra());
        assertEquals(32, score(new SanTuZaoJingGoldCard(), List.of(1, 2, 4), 0).sides().get(0).extra());
        // 云雷纹大铙：每颗 ≥4 骰 +12/+24（统一有效骰）。
        assertEquals(24, score(new YunLeiWenDaNaoCommonCard(), List.of(4, 5, 2), 0).sides().get(0).extra());
        assertEquals(48, score(new YunLeiWenDaNaoGoldCard(), List.of(4, 5, 2), 0).sides().get(0).extra());
        // 五星出东方：每颗 ≤3 骰 +12/+24。
        assertEquals(24, score(new WuXingChuDongFangCommonCard(), List.of(1, 2, 5), 0).sides().get(0).extra());
        assertEquals(48, score(new WuXingChuDongFangGoldCard(), List.of(1, 2, 5), 0).sides().get(0).extra());
    }

    @Test
    void sumOddAndZeroDice()
    {
        // 十二花卉杯：骰面和为奇数 → +14/+28。
        assertEquals(14, score(new ShiErHuaHuiBeiCommonCard(), List.of(1, 2, 4), 0).sides().get(0).extra());
        assertEquals(28, score(new ShiErHuaHuiBeiGoldCard(), List.of(1, 2, 4), 0).sides().get(0).extra());
        assertEquals(0, score(new ShiErHuaHuiBeiCommonCard(), List.of(1, 2, 3), 0).sides().get(0).extra());
        // 商阳台帖：无骰 → +14/+28（旧引擎 ZERO_DICE 词条缺失的缺陷修复）。
        assertEquals(14, score(new ShangYangTaiTieCommonCard(), List.of(), 0).sides().get(0).extra());
        assertEquals(28, score(new ShangYangTaiTieGoldCard(), List.of(), 0).sides().get(0).extra());
        assertEquals(0, score(new ShangYangTaiTieCommonCard(), List.of(3), 0).sides().get(0).extra());
    }

    @Test
    void centerMultiplierAndFirstDieBonus()
    {
        // 珍宝贝钏：中位（槽 1/2）→ 倍率 +1/+2（multiplier 含基础值 1）。
        assertEquals(2, score(new ZhenZhuBaoChuangCommonCard(), List.of(3), 1).sides().get(0).multiplier());
        assertEquals(3, score(new ZhenZhuBaoChuangGoldCard(), List.of(3), 2).sides().get(0).multiplier());
        assertEquals(1, score(new ZhenZhuBaoChuangCommonCard(), List.of(3), 0).sides().get(0).multiplier());
        // 梯火：点数全相同 → 第 N 颗骰 +4×2^(N-1)；点数不同无收益。
        assertEquals(4, score(new TXingBoHuaCommonCard(), List.of(3), 0).sides().get(0).extra());
        assertEquals(12, score(new TXingBoHuaCommonCard(), List.of(3, 3), 0).sides().get(0).extra());
        assertEquals(28, score(new TXingBoHuaCommonCard(), List.of(3, 3, 3), 0).sides().get(0).extra());
        assertEquals(0, score(new TXingBoHuaCommonCard(), List.of(3, 6), 0).sides().get(0).extra());
        assertEquals(4, score(new TXingBoHuaGoldCard(), List.of(3), 0).sides().get(0).extra());
        assertEquals(16, score(new TXingBoHuaGoldCard(), List.of(3, 3), 0).sides().get(0).extra());
        assertEquals(52, score(new TXingBoHuaGoldCard(), List.of(3, 3, 3), 0).sides().get(0).extra());
        assertEquals(0, score(new TXingBoHuaGoldCard(), List.of(3, 6), 0).sides().get(0).extra());
        assertTrue(new TXingBoHuaCommonCard() instanceof OnSettlement);
    }
}
