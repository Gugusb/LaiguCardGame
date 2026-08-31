package com.laigu.laigu.event;

import com.laigu.laigu.Laigu;
import com.laigu.laigu.buff.CardSynergy;
import com.laigu.laigu.capability.PlayerCodexProvider;
import com.laigu.laigu.item.CardPouchItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * 通用事件：卡牌羁绊 buff、玩家图鉴能力挂载。
 * <p>
 * 副手持卡袋时，每 2 秒按袋内卡牌重算羁绊并施加药水效果（服务端）。
 * 玩家图鉴能力（{@link PlayerCodexProvider}）附加到每个玩家实体，
 * 数据随玩家存档持久化（见 {@code ModCapabilities}）。
 */
@Mod.EventBusSubscriber(modid = Laigu.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ModEvents
{
    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event)
    {
        if (event.getObject() instanceof Player)
        {
            event.addCapability(ResourceLocation.fromNamespaceAndPath(Laigu.MODID, "codex"),
                    new PlayerCodexProvider());
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END)
        {
            return;
        }
        Player player = event.player;
        if (player.level().isClientSide)
        {
            return;
        }
        // 每 2 秒（40 tick）检查一次；按玩家 id 错峰，避免同刻集中运算
        if ((player.level().getGameTime() + player.getId()) % 40 != 0)
        {
            return;
        }

        ItemStack pouch = player.getOffhandItem();
        if (!(pouch.getItem() instanceof CardPouchItem))
        {
            return;
        }

        List<MobEffectInstance> effects = CardSynergy.computeEffects(pouch);
        for (MobEffectInstance effect : effects)
        {
            player.addEffect(effect);
        }
    }
}
