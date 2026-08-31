package com.laigu.laigu.capability;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import org.jetbrains.annotations.Nullable;

/**
 * 模组能力（Capability）注册入口。
 * <p>
 * 注册时机：
 * <ul>
 *   <li>MOD 总线 {@link RegisterCapabilitiesEvent}：{@link #register}（Laigu 构造器里挂监听）。</li>
 *   <li>FORGE 总线 {@code AttachCapabilitiesEvent}：把 {@link PlayerCodexProvider}
 *       附加到玩家实体（见 {@code ModEvents.onAttachCapabilities}）。</li>
 * </ul>
 */
public final class ModCapabilities
{
    /** 玩家图鉴能力（玩家个人，持久化）。 */
    public static final Capability<IPlayerCodex> PLAYER_CODEX =
            CapabilityManager.get(new CapabilityToken<>() {});

    private ModCapabilities()
    {
    }

    /** 取玩家图鉴；无则返回 null（正常情况服务端玩家始终有）。 */
    @Nullable
    public static IPlayerCodex of(Player player)
    {
        return player.getCapability(PLAYER_CODEX).resolve().orElse(null);
    }

    /** MOD 总线：注册能力类型（须与 {@link #PLAYER_CODEX} 同类型名，二者指向同一 Capability）。 */
    public static void register(RegisterCapabilitiesEvent event)
    {
        event.register(IPlayerCodex.class);
    }
}
