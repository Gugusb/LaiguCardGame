package com.laigu.laigu.duel.newcard;

import com.laigu.laigu.duel.newcard.cards.QianLiJiangShanCommonCard;
import com.laigu.laigu.duel.newcard.cards.QingTongXianHeCommonCard;
import com.laigu.laigu.duel.newcard.cards.QingTongXianHeGoldCard;
import com.laigu.laigu.duel.newcard.cards.WuXianPiPaCommonCard;
import com.laigu.laigu.duel.newcard.cards.XiShanXingLvTuGoldCard;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 阶段18：时机事件生命周期接管——派发语义、局内时机通道与溪山金入场激活。 */
final class Stage18LifecycleTakeoverTest
{
    @Test
    void summonBroadcastReachesOtherSlotListenerOnlyForEventTargetInterface()
    {
        NewCardBattle battle = new NewCardBattle();
        // 无限琵琶（普通）：己方其他场位入场时抽 1（onEvent 广播路径）。
        battle.placeCard(0, 0, new WuXianPiPaCommonCard());
        // 溪山行旅图金卡：OnSummon 接口路径——只应对事件目标场位生效。
        battle.placeCard(0, 2, new XiShanXingLvTuGoldCard());
        assertEquals(0, battle.state().handSize(0));
        battle.dispatch(new BattleEvent(BattleEvent.Type.SUMMON, 0, 1));
        // 无限琵琶监听到其他场位入场 → 抽 1；溪山金 OnSummon 未触发（事件目标是场位1）。
        assertEquals(1, battle.state().handSize(0));
    }

    @Test
    void leaveDispatchOnlyFiresForLeavingCard()
    {
        NewCardBattle battle = new NewCardBattle();
        battle.placeCard(0, 0, new QingTongXianHeCommonCard());
        battle.placeCard(0, 1, new QingTongXianHeCommonCard());
        assertEquals(0, battle.state().handSize(0));
        // 广播 LEAVE 会让同侧每只仙鹤都听到（错误语义）；定向派发只有离场卡抽 1。
        battle.dispatchToCard(0, 1, new BattleEvent(BattleEvent.Type.LEAVE, 0, 1));
        assertEquals(1, battle.state().handSize(0));
    }

    @Test
    void goldCraneLeaveWritesRoundTimingMult()
    {
        NewCardBattle battle = new NewCardBattle();
        battle.placeCard(0, 0, new QingTongXianHeGoldCard());
        battle.dispatchToCard(0, 0, new BattleEvent(BattleEvent.Type.LEAVE, 0, 0));
        // 金仙鹤离场：抽 2 + 本回合 4 点倍率写入局内时机通道。
        assertEquals(2, battle.state().handSize(0));
        assertEquals(4, battle.state().timingMult(0));
        // 结算时并入倍率并清零时机通道。
        ScoreSnapshot snap = NewSettlementCalculator.calculate(battle);
        assertEquals(1 + 4, snap.sides().get(0).multiplier());
        assertEquals(0, battle.state().timingMult(0));
        assertEquals(0, battle.state().timingExtra(0));
    }

    @Test
    void xiShanGoldSummonActivatesFriendlyActivatables()
    {
        NewCardBattle battle = new NewCardBattle();
        // 破山（激活2）在场；溪山金入场 → 激活己方全部可激活卡（阈值>0）。
        battle.placeCard(0, 0, new QianLiJiangShanCommonCard());
        battle.placeCard(0, 1, new XiShanXingLvTuGoldCard());
        battle.dispatch(new BattleEvent(BattleEvent.Type.SUMMON, 0, 1));
        // 激活进度 +1（未达阈值 2，不触发奖励；激活推进由新核心承担）。
        assertEquals(1, battle.state().cardStateAt(0, 0).activation());
        // 溪山金自身未实现激活接口 → 不在激活目标中。
        assertEquals(0, battle.state().cardStateAt(0, 1).activation());
    }

    @Test
    void activationThresholdStillGatesRewards()
    {
        NewCardBattle battle = new NewCardBattle();
        battle.placeCard(0, 0, new QianLiJiangShanCommonCard());
        battle.state().cardStateAt(0, 0).incrementActivation();
        battle.state().cardStateAt(0, 0).incrementActivation();
        battle.dispatch(new BattleEvent(BattleEvent.Type.ACTIVATION, 0, 0));
        // 激活2 达阈值 → 触发奖励后进度清零，倍率写入局内时机通道（对齐旧 timingMult）。
        assertEquals(0, battle.state().cardStateAt(0, 0).activation());
        assertEquals(2, battle.state().timingMult(0));
    }
}
