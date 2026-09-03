package com.laigu.laigu.duel.newcard;

import com.laigu.laigu.duel.newcard.cards.CaiFengMingQiCommonCard;
import com.laigu.laigu.duel.newcard.cards.CaiFengMingQiGoldCard;
import com.laigu.laigu.duel.newcard.cards.JinShiLuCommonCard;
import com.laigu.laigu.duel.newcard.cards.LiGuiCommonCard;
import com.laigu.laigu.duel.newcard.cards.JinShiLuGoldCard;
import com.laigu.laigu.duel.newcard.cards.QianLiJiangShanGoldCard;
import com.laigu.laigu.duel.newcard.cards.SongJinXiangShiCommonCard;
import com.laigu.laigu.duel.newcard.cards.SongJinXiangShiGoldCard;
import com.laigu.laigu.duel.newcard.cards.WuXianPiPaCommonCard;
import com.laigu.laigu.duel.newcard.cards.WuXianPiPaGoldCard;
import com.laigu.laigu.duel.newcard.cards.XueJingHanLinTuCommonCard;
import com.laigu.laigu.duel.newcard.cards.XueJingHanLinTuGoldCard;
import com.laigu.laigu.duel.newcard.cards.YinQueShanHanJian2CommonCard;
import com.laigu.laigu.duel.newcard.cards.YinQueShanHanJian2GoldCard;
import com.laigu.laigu.duel.newcard.cards.ZengHouYiBianZhongCommonCard;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** 阶段九缺口修复：原占位卡的接口化迁移测试（计数引擎/老兵/站场抽卡/他卡入场抽卡）。 */
class Stage9RegistryGapMigrationTest
{
    @Test
    void kaiJiangScoresPerGongClassCard()
    {
        NewCardBattle common = new NewCardBattle();
        common.placeCard(0, 0, new YinQueShanHanJian2CommonCard());
        common.placeCard(0, 1, new WuXianPiPaCommonCard());      // 攻·炽
        common.placeCard(0, 2, new LiGuiCommonCard());           // 攻·炽
        common.placeCard(0, 3, new CaiFengMingQiCommonCard());   // 守·衡
        assertEquals(4, NewSettlementCalculator.calculate(common, new SettlementRuleRegistry()).sides().get(0).base());

        NewCardBattle gold = new NewCardBattle();
        gold.placeCard(0, 0, new YinQueShanHanJian2GoldCard());
        gold.placeCard(0, 1, new WuXianPiPaCommonCard());
        gold.placeCard(0, 2, new WuXianPiPaGoldCard());
        assertEquals(8, NewSettlementCalculator.calculate(gold, new SettlementRuleRegistry()).sides().get(0).base());
    }

    @Test
    void taiRanScoresByClassOrGoldCount()
    {
        NewCardBattle common = new NewCardBattle();
        common.placeCard(0, 0, new XueJingHanLinTuCommonCard());
        common.placeCard(0, 1, new ZengHouYiBianZhongCommonCard()); // 鼎·盛
        // 计算器基线倍率 1 + 每鼎·盛卡 +1 × 2 张。
        assertEquals(3, NewSettlementCalculator.calculate(common, new SettlementRuleRegistry()).sides().get(0).multiplier());

        NewCardBattle gold = new NewCardBattle();
        gold.placeCard(0, 0, new XueJingHanLinTuGoldCard());
        gold.placeCard(0, 1, new QianLiJiangShanGoldCard());
        // 清单：金卡独立——主效果每鼎·盛卡+2（两张均鼎·盛）+ 焕章每金卡+1（两张金卡）。
        assertEquals(7, NewSettlementCalculator.calculate(gold, new SettlementRuleRegistry()).sides().get(0).multiplier());
    }

    @Test
    void chiLingScoresByTangDynastyOrGoldCount()
    {
        NewCardBattle common = new NewCardBattle();
        common.placeCard(0, 0, new JinShiLuCommonCard());
        common.placeCard(0, 1, new CaiFengMingQiCommonCard()); // 唐
        common.placeCard(0, 2, new WuXianPiPaCommonCard());    // 唐
        assertEquals(4, NewSettlementCalculator.calculate(common, new SettlementRuleRegistry()).sides().get(0).base());

        NewCardBattle gold = new NewCardBattle();
        gold.placeCard(0, 0, new JinShiLuGoldCard());
        gold.placeCard(0, 1, new QianLiJiangShanGoldCard());
        // 金卡词条替换：每张金质卡 +10 额外分（自身 + 千里江山金卡）。
        assertEquals(20, NewSettlementCalculator.calculate(gold, new SettlementRuleRegistry()).sides().get(0).extra());
    }

    @Test
    void shouWangRewardsSurvivingOneRound()
    {
        NewCardBattle common = new NewCardBattle();
        common.placeCard(0, 0, new CaiFengMingQiCommonCard());
        assertEquals(0, NewSettlementCalculator.calculate(common, new SettlementRuleRegistry()).sides().get(0).extra());
        // 回合开始推进：幸存卡标记上轮在场 → +12。
        common.startRound();
        assertEquals(12, NewSettlementCalculator.calculate(common, new SettlementRuleRegistry()).sides().get(0).extra());

        NewCardBattle gold = new NewCardBattle();
        gold.placeCard(0, 0, new CaiFengMingQiGoldCard());
        gold.startRound();
        assertEquals(24, NewSettlementCalculator.calculate(gold, new SettlementRuleRegistry()).sides().get(0).extra());
    }

    @Test
    void cangFengDrawsByRoundsOnFieldWithCap()
    {
        NewCardBattle common = new NewCardBattle();
        common.placeCard(0, 0, new SongJinXiangShiCommonCard());
        common.startRound(); // 站场轮数 1 → 抽 1
        assertEquals(1, common.state().handSize(0));

        NewCardBattle gold = new NewCardBattle();
        gold.placeCard(0, 0, new SongJinXiangShiGoldCard());
        gold.startRound(); // 金卡站场加成 +1 → 抽 2
        assertEquals(2, gold.state().handSize(0));

        // 继续两轮：站场轮数 2 → 抽 3；站场轮数 3 → 不设上限本应抽 4，被封顶 3。
        gold.startRound();
        gold.startRound();
        assertEquals(8, gold.state().handSize(0)); // 2 + 3 + 3（后两轮均触顶）
    }

    @Test
    void zhuiFengDrawsWhenOtherHandCardDeployed()
    {
        NewCardBattle battle = new NewCardBattle();
        battle.placeCard(0, 0, new WuXianPiPaCommonCard());
        // 己方其他场位入场（带入场事件派发）→ 抽 1。
        battle.placeCard(0, 1, new CaiFengMingQiCommonCard(), true);
        assertEquals(1, battle.state().handSize(0));

        // 自己入场不算“使用其他手牌”。
        NewCardBattle selfPlacement = new NewCardBattle();
        selfPlacement.placeCard(0, 0, new WuXianPiPaCommonCard(), true);
        assertEquals(0, selfPlacement.state().handSize(0));

        // 对手入场不触发。
        NewCardBattle opponent = new NewCardBattle();
        opponent.placeCard(0, 0, new WuXianPiPaGoldCard());
        opponent.placeCard(1, 1, new CaiFengMingQiCommonCard(), true);
        assertEquals(0, opponent.state().handSize(0));
    }
}
