package com.laigu.laigu.duel.newcard;

import net.minecraft.network.FriendlyByteBuf;

import java.util.List;
import java.util.Objects;

/** 新版规则动画事件的可序列化批次；阶段6先提供传输模型，不改变旧 UI。 */
public record NewAnimationEventPacket(List<AnimationEvent> events)
{
    public NewAnimationEventPacket
    {
        events = List.copyOf(Objects.requireNonNull(events));
    }

    public void encode(FriendlyByteBuf buf)
    {
        buf.writeVarInt(events.size());
        for (AnimationEvent event : events)
        {
            buf.writeEnum(event.type());
            buf.writeVarInt(event.side());
            buf.writeVarInt(event.slot());
            buf.writeUtf(event.cardId());
            buf.writeVarInt(event.value());
        }
    }

    public static NewAnimationEventPacket decode(FriendlyByteBuf buf)
    {
        int count = buf.readVarInt();
        if (count < 0 || count > 256) throw new IllegalArgumentException("动画事件数量非法：" + count);
        java.util.ArrayList<AnimationEvent> events = new java.util.ArrayList<>();
        for (int i = 0; i < count; i++)
            events.add(new AnimationEvent(buf.readEnum(AnimationEvent.Type.class), buf.readVarInt(), buf.readVarInt(), buf.readUtf(128), buf.readVarInt()));
        return new NewAnimationEventPacket(events);
    }
}
