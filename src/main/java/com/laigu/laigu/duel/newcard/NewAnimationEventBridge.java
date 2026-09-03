package com.laigu.laigu.duel.newcard;

import java.util.List;

/** 新动画事件与现有客户端动画系统之间的无副作用桥接入口。 */
public final class NewAnimationEventBridge
{
    private NewAnimationEventBridge() {}

    public static NewAnimationEventPacket toPacket(List<AnimationEvent> events)
    {
        return new NewAnimationEventPacket(events);
    }

    /** 当前旧 DuelScreen 仍消费 DuelStateS2CPacket；此方法仅返回可供新客户端消费的事件批次。 */
    public static List<AnimationEvent> normalize(List<AnimationEvent> events)
    {
        return List.copyOf(events);
    }
}
