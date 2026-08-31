package com.laigu.laigu.duel.newcard;

import java.util.List;
import java.util.Objects;

/** 一条战斗命令的确定性结果。失败不依赖异常，便于网络同步和客户端提示。 */
public record BattleCommandResult(boolean success, String message, List<AnimationEvent> animations)
{
    public BattleCommandResult
    {
        Objects.requireNonNull(message, "message");
        animations = List.copyOf(Objects.requireNonNull(animations, "animations"));
    }

    public static BattleCommandResult success(List<AnimationEvent> animations)
    {
        return new BattleCommandResult(true, "", animations);
    }

    public static BattleCommandResult failure(String message)
    {
        return new BattleCommandResult(false, message, List.of());
    }
}
