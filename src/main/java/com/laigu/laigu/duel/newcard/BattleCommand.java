package com.laigu.laigu.duel.newcard;

/** 新架构的战斗操作命令；命令本身只描述意图，不执行规则。 */
public sealed interface BattleCommand
        permits BattleCommand.Place, BattleCommand.Leave, BattleCommand.Replace
{
    record Place(int side, int slot, DuelCard card, boolean summon) implements BattleCommand {}
    record Leave(int side, int slot) implements BattleCommand {}
    record Replace(int side, int slot, DuelCard card) implements BattleCommand {}
}
