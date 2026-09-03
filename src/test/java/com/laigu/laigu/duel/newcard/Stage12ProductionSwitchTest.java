package com.laigu.laigu.duel.newcard;

import com.laigu.laigu.duel.DuelGame;
import com.laigu.laigu.duel.ScoreEngine;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 阶段十二：生产入口切换。
 * DuelTableBlockEntity 在开局/读档时安装 RoundSettlementHook；
 * 新核心可用时替换本回合 (base, mult, extra)，否则回退旧引擎。
 */
class Stage12ProductionSwitchTest
{
    @BeforeAll
    static void registerCards()
    {
        CardRegistry.initialize();
    }

    private static NewCardBattle battleWith(DuelCard... cards)
    {
        NewCardBattle battle = new NewCardBattle();
        int slot = 0;
        for (DuelCard card : cards) battle.placeCard(0, slot++, card);
        return battle;
    }

    @Test
    void canSettleAcceptsDraftCardsAfterStage16()
    {
        // 阶段16：抢骰族 6 文物×2 已由新系统实现，不再回退旧引擎。
        NewCardBattle battle = battleWith(CardFactory.create("tong_che_ma_common"),
                CardFactory.create("xing_yue_ye_common"));
        assertTrue(NewCardCoreSwitch.canSettle(battle), "抢骰族已迁移，新核心可正常结算");
    }

    @Test
    void canSettleAcceptsMappedInterfaceAndFrameworkCarriedCards()
    {
        assertTrue(NewCardCoreSwitch.canSettle(battleWith(CardFactory.create("tong_che_ma_common"))),
                "LegacyMappedCard 直映射");
        assertTrue(NewCardCoreSwitch.canSettle(battleWith(CardFactory.create("zeng_hou_yi_bian_zhong_common"))),
                "接口化迁移卡");
        assertTrue(NewCardCoreSwitch.canSettle(battleWith(CardFactory.create("dun_huang_fei_tian_common"))),
                "框架5规则承载卡");
    }

    @Test
    void switchToggleSupportsRollback()
    {
        boolean before = NewCardCoreSwitch.enabled();
        try
        {
            NewCardCoreSwitch.setEnabled(false);
            assertFalse(NewCardCoreSwitch.enabled());
            NewCardCoreSwitch.setEnabled(true);
            assertTrue(NewCardCoreSwitch.enabled());
        }
        finally
        {
            NewCardCoreSwitch.setEnabled(before);
        }
    }

    /** 反射调用私有 settleRound（空场对局），验证钩子结果接管 lastBase/lastMult/lastExtra。 */
    private static void settleRound(DuelGame game) throws Exception
    {
        Method settle = DuelGame.class.getDeclaredMethod("settleRound");
        settle.setAccessible(true);
        settle.invoke(game);
    }

    @Test
    void settlementHookOverridesLegacyScore() throws Exception
    {
        DuelGame game = new DuelGame(net.minecraft.util.RandomSource.create());
        game.setRoundSettlementHook((side, legacy) ->
                side == 0 ? new ScoreEngine.ScoreResult(5, 2, 3, 13) : null);
        settleRound(game);
        assertEquals(5, game.lastBase(0));
        assertEquals(2, game.lastMult(0));
        assertEquals(3, game.lastExtra(0));
        // 侧 1 钩子返回 null → 回退旧引擎（空场 0/1/0）。
        assertEquals(0, game.lastBase(1));
        assertEquals(1, game.lastMult(1));
        // 总分驱动胜场：13 > 0。
        assertEquals(1, game.wins(0));
    }

    @Test
    void withoutHookLegacyEngineRemainsAuthoritative() throws Exception
    {
        DuelGame game = new DuelGame(net.minecraft.util.RandomSource.create());
        settleRound(game);
        assertEquals(0, game.lastBase(0));
        assertEquals(1, game.lastMult(0));
        assertEquals(0, game.lastExtra(0));
        // 空场平局：双方均无胜场。
        assertEquals(-1, game.winnerLast());
    }
}
