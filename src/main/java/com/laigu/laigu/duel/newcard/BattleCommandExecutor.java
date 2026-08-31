package com.laigu.laigu.duel.newcard;

import java.util.List;
import java.util.Objects;

/** 执行新架构命令，并统一返回本次命令产生的动画事件。 */
public final class BattleCommandExecutor
{
    private final NewCardBattle battle;

    public BattleCommandExecutor(NewCardBattle battle)
    {
        this.battle = Objects.requireNonNull(battle);
    }

    public BattleCommandResult execute(BattleCommand command)
    {
        if (command == null) return BattleCommandResult.failure("战斗命令不能为空");
        try
        {
            if (command instanceof BattleCommand.Place place)
                return BattleCommandResult.success(
                        battle.placeCard(place.side(), place.slot(), place.card(), place.summon()));
            if (command instanceof BattleCommand.Leave leave)
                return BattleCommandResult.success(battle.leaveCard(leave.side(), leave.slot()));
            if (command instanceof BattleCommand.Replace replace)
                return BattleCommandResult.success(battle.replaceCard(replace.side(), replace.slot(), replace.card()));
            return BattleCommandResult.failure("未知战斗命令：" + command);
        }
        catch (IllegalArgumentException exception)
        {
            return BattleCommandResult.failure(exception.getMessage());
        }
    }
}
