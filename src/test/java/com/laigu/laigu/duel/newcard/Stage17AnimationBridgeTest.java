package com.laigu.laigu.duel.newcard;

import com.laigu.laigu.duel.newcard.cards.WanHeSongFengTuGoldCard;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 阶段17：动画事件在新系统的产生与广播前状态（触发/弹出/取出清空/包往返）。 */
class Stage17AnimationBridgeTest
{
    @BeforeAll
    static void registerCards() { CardRegistry.initialize(); }

    @Test
    void settlementEmitsCardTriggerAndScorePopups()
    {
        // 万壑松风金：结算 +20 额外分（清单口径）→ 每张卡先发 CARD_TRIGGER，加分时发 SCORE_POPUP(+20)。
        NewCardBattle battle = new NewCardBattle();
        battle.placeCard(0, 0, new WanHeSongFengTuGoldCard());
        battle.state().cardStateAt(0, 0).setDice(java.util.List.of(2));
        NewSettlementCalculator.calculate(battle, new SettlementRuleRegistry());
        assertTrue(battle.state().animations().stream()
                .anyMatch(e -> e.type() == AnimationEvent.Type.CARD_TRIGGER
                        && e.side() == 0 && e.slot() == 0 && "wan_he_song_feng_tu_gold".equals(e.cardId())),
                "结算应产生 CARD_TRIGGER");
        assertTrue(battle.state().animations().stream()
                .anyMatch(e -> e.type() == AnimationEvent.Type.SCORE_POPUP && e.value() == 20),
                "加分应产生 SCORE_POPUP(+20)");
    }

    @Test
    void drainAnimationsClearsAccumulatedEvents()
    {
        NewCardBattle battle = new NewCardBattle();
        battle.placeCard(0, 0, new WanHeSongFengTuGoldCard());
        battle.state().cardStateAt(0, 0).setDice(java.util.List.of(2));
        NewSettlementCalculator.calculate(battle, new SettlementRuleRegistry());
        assertTrue(!battle.state().animations().isEmpty());
        assertTrue(battle.state().drainAnimations().size() > 0);
        // 结算后清空：跨轮不累积。
        assertTrue(battle.state().animations().isEmpty());
    }

    @Test
    void activationAndMultiplierPopupsCarryValues()
    {
        // 编钟金（激活3，每骰 +2 倍率）激活 → CARD_ACTIVATE + MULTIPLIER_POPUP(+2)。
        NewCardBattle battle = new NewCardBattle();
        battle.placeCard(0, 0, new com.laigu.laigu.duel.newcard.cards.ZengHouYiBianZhongGoldCard());
        battle.state().cardStateAt(0, 0).setDice(java.util.List.of(3));
        // 激活进度达阈值 3 后触发（对齐旧引擎逐次激活语义）。
        for (int i = 0; i < 3; i++) battle.state().cardStateAt(0, 0).incrementActivation();
        battle.dispatch(new BattleEvent(BattleEvent.Type.ACTIVATION, 0, 0));
        assertTrue(battle.state().animations().stream()
                .anyMatch(e -> e.type() == AnimationEvent.Type.CARD_ACTIVATE && e.value() == 0));
        // 编钟激活词条自发 MULTIPLIER_POPUP（CardContext 事件路径，数值由卡面语义携带）。
        assertTrue(battle.state().animations().stream()
                .anyMatch(e -> e.type() == AnimationEvent.Type.MULTIPLIER_POPUP && e.side() == 0 && e.slot() == 0));
    }
}
