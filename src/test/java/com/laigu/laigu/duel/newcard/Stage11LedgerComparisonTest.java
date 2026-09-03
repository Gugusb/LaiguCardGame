package com.laigu.laigu.duel.newcard;

import com.laigu.laigu.duel.DuelCardCatalog;
import com.laigu.laigu.duel.DuelCardData;
import com.laigu.laigu.duel.EffectType;
import com.laigu.laigu.duel.newcard.cards.BaiHuaTuJuanGoldCard;
import com.laigu.laigu.duel.newcard.cards.QianLiJiangShanGoldCard;
import com.laigu.laigu.duel.newcard.cards.QingTongXianHeCommonCard;
import com.laigu.laigu.duel.newcard.cards.HunTianYiCommonCard;
import com.laigu.laigu.duel.newcard.cards.HunTianYiGoldCard;
import com.laigu.laigu.duel.newcard.cards.JinOuYongGuBeiGoldCard;
import com.laigu.laigu.duel.newcard.cards.ShuiLianGoldCard;
import com.laigu.laigu.duel.newcard.cards.YunLeiWenDaNaoCommonCard;
import com.laigu.laigu.duel.newcard.cards.ZengHouYiBianZhongCommonCard;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 阶段十一：新旧结果对照台账（执行型）。
 *
 * 约束：单测环境无法初始化 Forge 物品注册表（EventBus 依赖模组加载），
 * 旧引擎 ScoreEngine 的真实 ItemStack 全量对照在生产端由
 * DuelGameNewCardShadowAdapter.compareScore 并行承担。
 * 本测试在数据层对照：旧语义源 = DuelCardCatalog 静态目录数据
 * （effect/p1/p2 + 金卡缩放规则），差异按交接清单分为四类：
 * 完全兼容 / 设计变更 / 旧版缺陷修复 / 尚未迁移。
 */
class Stage11LedgerComparisonTest
{
    @BeforeAll
    static void registerCards()
    {
        CardRegistry.initialize();
    }


    /** 追锋类卡通过覆写 DuelCard.onEvent 承载事件词条（非触发接口）。 */
    private static boolean hasOnEventOverride(DuelCard card)
    {
        for (java.lang.reflect.Method m : card.getClass().getDeclaredMethods())
            if ("onEvent".equals(m.getName()) && m.getParameterCount() == 2) return true;
        // 清单对齐后部分卡改走触发接口承载行为。
        return card instanceof com.laigu.laigu.duel.newcard.OnSettlement
                || card instanceof com.laigu.laigu.duel.newcard.OnSummon
                || card instanceof com.laigu.laigu.duel.newcard.OnAmbushSuccess
                || card instanceof com.laigu.laigu.duel.newcard.OnAmbushFail
                || card instanceof com.laigu.laigu.duel.newcard.OnActivation;
    }

    // ================= A. 尚未迁移 = 空 =================

    /**
     * 尚未迁移清单：阶段16 抢骰基础设施落地后清空 → 158/158 全部由新系统实现。
     * 后续若新增未迁移卡牌，在此登记（与 NewCardCoreSwitch.UNMIGRATED_IDS 保持一致）。
     */
    private static final java.util.Set<String> UNMIGRATED_IDS = java.util.Set.of();

    @Test
    void unmigratedSetIsExplicitAndEverythingElseHasEffectPath()
    {
        assertEquals(158, CardFactory.registeredIds().size());
        assertTrue(CardRegistryValidator.missingVariantIds().isEmpty(),
                () -> "未注册变体：" + CardRegistryValidator.missingVariantIds());
        for (String id : CardFactory.registeredIds())
        {
            DuelCard card = CardFactory.create(id);
            boolean hasEffectPath = card instanceof OnSummon || card instanceof OnLeave
                    || card instanceof OnRoundStart || card instanceof OnRoundEnd
                    || card instanceof OnActivation || card instanceof OnSettlement
                    || card instanceof OnAmbushSuccess || card instanceof OnAmbushFail
                    || card instanceof OnPoZhen || card instanceof OnDraft || card instanceof OnPlace
                    || card instanceof OnDraftPlan
                    || !card.effects().isEmpty()
                    || hasOnEventOverride(card)
                    // 空壳类但行为由框架5规则承载（批次六候选）。
                    || SETTLEMENT_RULES.contains(id) || EVENT_RULES.contains(id);
            if (UNMIGRATED_IDS.contains(id))
                assertFalse(hasEffectPath, id + " 已有效果路径，请从尚未迁移清单移除");
            else
                assertTrue(hasEffectPath, id + " 没有任何迁移效果路径（应加入尚未迁移清单）");
        }
    }

    private static final SettlementRuleRegistry SETTLEMENT_RULES = FrameworkFiveRules.defaultRegistry();
    private static final CardEventRuleRegistry EVENT_RULES = FrameworkFiveEventRules.defaultRegistry();

    @Test
    void frameworkRulesCarryTheRemainingEmptyClasses()
    {
        // 阶段14：框架5注册表已清空，4 文物 ×2 的行为已内联到卡类 onEvent 覆写，行为不缺失。
        for (String artifact : List.of("dun_huang_fei_tian", "hai_cuo_tu",
                "qing_tong_xian_he", "xi_shan_xing_lv_tu"))
        {
            for (String rarity : List.of("common", "gold"))
            {
                String id = artifact + "_" + rarity;
                DuelCard card = CardFactory.create(id);
                assertTrue(hasOnEventOverride(card),
                        id + " 框架5规则已移除，行为必须由 onEvent 覆写或触发接口承载");
                assertFalse(SETTLEMENT_RULES.contains(id) || EVENT_RULES.contains(id),
                        id + " 不应再注册在框架5注册表中");
            }
        }
    }

    // ================= B. 完全兼容：LegacyMappedCard 与旧目录数据 =================

    /** 金卡最终值按目录参数语义缩放（与 DuelCardData.p1For/p2For 构建期规则一致）。 */
    private static int goldScaled(int base, EffectType type, int parameterIndex, boolean gold)
    {
        DuelCardData.ValueSpec spec = DuelCardData.valueSpec(type, parameterIndex);
        return gold && spec.goldScale() == DuelCardData.GoldScale.DOUBLE ? base * 2 : base;
    }

    @Test
    void formerLegacyMappedCardsAreNowIndependentOnSettlementClasses()
    {
        // 阶段15：直映射路径已删除；原 10 文物 × 2 变体全部改为显式 OnSettlement 独立实现
        // （逐卡行为等价性由 Stage15ExplicitSettlementTest 行为断言承担）。
        List<String> formerMapped = List.of(
                "t_xing_bo_hua", "san_tu_zao_jing", "shang_yang_tai_tie", "shi_er_hua_hui_bei",
                "tu_xing_tao_xun", "wan_gong_jiao", "wan_he_song_feng_tu",
                "wu_xing_chu_dong_fang", "yun_lei_wen_da_nao", "zhen_zhu_bao_chuang");
        int checked = 0;
        for (String artifact : formerMapped)
        {
            for (String rarity : List.of("common", "gold"))
            {
                String id = artifact + "_" + rarity;
                DuelCard card = CardFactory.create(id);
                assertTrue(card instanceof OnSettlement, id + " 应实现 OnSettlement 独立接口");
                checked++;
            }
        }
        assertEquals(20, checked);
    }

    // ================= C. 设计变更（执行型台账） =================

    /** 一条设计变更：旧语义到新语义 + 变更依据。 */
    private record DesignChange(String artifactId, String oldSemantics, String newSemantics, String reason) {}

    private static List<DesignChange> designChangeLedger()
    {
        return List.of(
                new DesignChange("zeng_hou_yi_bian_zhong",
                        "消耗系 FLAT_EXTRA（误迁移词条）",
                        "DING 激活卡：阈值 3，每骰 +1 倍率（金 +2）",
                        "用户拍板：旧目录 id 重复条目以第 3 批词条为准"),
                new DesignChange("hun_tian_yi",
                        "金卡数值 = 普通 ×2",
                        "金卡与普通相同（顺子激活右侧 3 次）",
                        "第 3 批目录未配置浑天仪金质加成"),
                new DesignChange("shui_lian",
                        "失败奖励仅普通卡生效（旧引擎 !gold 守卫）",
                        "金卡失败同样 +35 额外分",
                        "第 3 批词条未设稀有度限制"),
                new DesignChange("bai_hua_tu_juan",
                        "失败奖励仅普通卡生效（旧引擎 !gold 守卫）",
                        "金卡失败同样 +25 额外分",
                        "第 3 批词条未设稀有度限制"),
                new DesignChange("jin_ou_yong_gu_bei",
                        "金卡激活奖励 = 普通奖励 ×2（每金卡 +2 倍率）",
                        "金卡激活奖励 = 普通 + 焕章（每金卡 +3 倍率）",
                        "激活奖励加法约定：金卡 = 普通主效果 + 焕章加值"),
                new DesignChange("qian_li_jiang_shan",
                        "金卡激活奖励 = 普通奖励 ×2（+4 倍率）",
                        "金卡激活奖励 = 普通 + 焕章（2+2=4 倍率）",
                        "激活奖励加法约定（数值与 ×2 相同，语义来源不同）"));
    }

    @Test
    void designChangeLedgerIsDocumentedAndLive()
    {
        List<DesignChange> ledger = designChangeLedger();
        assertFalse(ledger.isEmpty());
        // 台账关键条目的新行为必须可复现：编钟按第 3 批为激活卡（阈值 3）。
        assertEquals(3, ((OnActivation) CardFactory.create("zeng_hou_yi_bian_zhong_common")).activationThreshold());
        assertEquals(3, ((OnActivation) CardFactory.create("zeng_hou_yi_bian_zhong_gold")).activationThreshold());
        for (DesignChange change : ledger)
            assertFalse(DuelCardCatalog.byId(change.artifactId()) == null,
                    "台账条目指向不存在的文物：" + change.artifactId());
    }

    @Test
    void designChangeHunTianGoldSixActivations()
    {
        // 清单：顺子激活右侧3次（金=6次），不再与普通版一致。
        NewCardBattle common = new NewCardBattle();
        common.placeCard(0, 0, new HunTianYiCommonCard());
        common.placeCard(0, 1, new ZengHouYiBianZhongCommonCard());
        common.state().cardStateAt(0, 0).setDice(List.of(1, 2));
        common.state().cardStateAt(0, 1).setDice(List.of(3, 4));
        NewSettlementCalculator.calculate(common, new SettlementRuleRegistry());
        assertEquals(3, common.state().multiplier(0), "hun_tian_yi_common");

        NewCardBattle gold = new NewCardBattle();
        gold.placeCard(0, 0, new HunTianYiGoldCard());
        gold.placeCard(0, 1, new ZengHouYiBianZhongCommonCard());
        gold.state().cardStateAt(0, 0).setDice(List.of(1, 2));
        gold.state().cardStateAt(0, 1).setDice(List.of(3, 4));
        NewSettlementCalculator.calculate(gold, new SettlementRuleRegistry());
        // 6 次激活：编钟在第 3、6 次达阈值各触发一次（2骰 ×1 倍率）→ 1+4=5。
        assertEquals(5, gold.state().multiplier(0), "hun_tian_yi_gold");
    }

    @Test
    void designChangeAmbushFailYieldsNoReward()
    {
        // 清单口径（推翻批次五裁定）：睡莲/百花伏击失败均无收益，不再实现 OnAmbushFail。
        Class<?> failHook = com.laigu.laigu.duel.newcard.OnAmbushFail.class;
        org.junit.jupiter.api.Assertions.assertFalse(failHook.isInstance(new ShuiLianGoldCard()), "睡莲金失败无收益");
        org.junit.jupiter.api.Assertions.assertFalse(failHook.isInstance(new com.laigu.laigu.duel.newcard.cards.ShuiLianCommonCard()), "睡莲普失败无收益");
        org.junit.jupiter.api.Assertions.assertFalse(failHook.isInstance(new BaiHuaTuJuanGoldCard()), "百花金失败无收益");
        org.junit.jupiter.api.Assertions.assertFalse(failHook.isInstance(new com.laigu.laigu.duel.newcard.cards.BaiHuaTuJuanCommonCard()), "百花普失败无收益");
    }

    @Test
    void designChangeGoldCupRewardsOpponentEmptySlotsAndGolds()
    {
        // 清单：永固杯金=激活2 → 2x 倍率（x=对方无骰卡牌数）；焕章=结算时每金卡+1倍率。
        NewCardBattle battle = new NewCardBattle();
        battle.placeCard(0, 0, new JinOuYongGuBeiGoldCard());
        battle.placeCard(1, 0, new QingTongXianHeCommonCard());
        battle.state().cardStateAt(0, 0).setActivation(2);
        battle.dispatch(new BattleEvent(BattleEvent.Type.ACTIVATION, 0, 0));
        assertEquals(2, battle.state().timingMult(0));

        // 焕章：两张金卡（自身+千里江山金）→ 结算 +2 倍率，合计 1+2+2=5。
        battle.placeCard(0, 1, new QianLiJiangShanGoldCard());
        NewSettlementCalculator.calculate(battle, new SettlementRuleRegistry());
        assertEquals(5, battle.state().multiplier(0));
    }

    // ================= D. 旧版缺陷修复 =================

    @Test
    void dieThresholdEffectsCountActiveDiceOnly()
    {
        // 旧 ScoreEngine 的 DIE_GE4_EXTRA 用原始骰（含被睡莲无效化的骰）；
        // 新引擎统一用有效骰：无效化 1 颗后 [4,5] 只剩 [5]，+12（旧缺陷应为 +24）。
        NewCardBattle battle = new NewCardBattle();
        battle.placeCard(0, 0, new YunLeiWenDaNaoCommonCard());
        CardRuntimeState self = battle.state().cardStateAt(0, 0);
        self.setDice(List.of(4, 5));
        self.invalidateLeadingDice(1);
        ScoreSnapshot snapshot = NewSettlementCalculator.calculate(battle, new SettlementRuleRegistry());
        assertEquals(12, snapshot.sides().get(0).extra());
    }
}
