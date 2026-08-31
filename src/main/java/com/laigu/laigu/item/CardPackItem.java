package com.laigu.laigu.item;

import com.laigu.laigu.card.CardCatalog;
import com.laigu.laigu.card.CodexHelper;
import com.laigu.laigu.network.ModPackets;
import com.laigu.laigu.network.PackOpenEffectPacket;
import com.laigu.laigu.registry.ModItems;
import com.laigu.laigu.util.CardNbt;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/**
 * 卡牌包：右键开包，按包类型随机产出卡牌。
 * <p>
 * 当前为初版规则（后续战利品表再细化）：
 * <ul>
 *   <li>普通包：固定 5 张；普通 99.8% / 金质 0.2%；任意版本都有 1% 概率附魔光泽。</li>
 *   <li>末影/炫彩/金质包：占位概率，可后续调（见 {@link PackType}）。</li>
 * </ul>
 * 产出的卡牌会标记所有者（开包玩家），tooltip 上以「所有者」标签显示。
 */
public class CardPackItem extends Item
{
    public static final int CARDS_PER_PACK = 5;

    public enum PackType
    {
        COMMON(0.002, 0.01, SoundEvents.BOOK_PAGE_TURN, ParticleTypes.END_ROD),
        ENDER(0.1, 0.02, SoundEvents.ENDERMAN_TELEPORT, ParticleTypes.PORTAL),
        RAINBOW(0.05, 0.05, SoundEvents.FIREWORK_ROCKET_BLAST, ParticleTypes.FIREWORK),
        GOLD(1.0, 0.02, SoundEvents.PLAYER_LEVELUP, ParticleTypes.TOTEM_OF_UNDYING);

        /** 出金质的概率 */
        public final double goldChance;
        /** 任意版本附魔光泽概率 */
        public final double glintChance;
        /** 开包专属音效（各包不同） */
        public final SoundEvent openSound;
        /** 开包专属粒子（各包不同） */
        public final ParticleOptions openParticle;

        PackType(double goldChance, double glintChance, SoundEvent openSound, ParticleOptions openParticle)
        {
            this.goldChance = goldChance;
            this.glintChance = glintChance;
            this.openSound = openSound;
            this.openParticle = openParticle;
        }
    }

    private final PackType packType;

    public CardPackItem(Properties properties, PackType packType)
    {
        super(properties);
        this.packType = packType;
    }

    public PackType getPackType()
    {
        return packType;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
    {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide)
        {
            return InteractionResultHolder.sidedSuccess(stack, true);
        }

        List<ItemStack> cards = rollCards(player);
        for (ItemStack card : cards)
        {
            if (!player.getInventory().add(card))
            {
                player.drop(card, false);
            }
        }
        // 开包动画需要包面图像，必须在 shrink 前留副本（shrink 后 stack 已空，copy 会是空栈导致动画不显示）
        ItemStack packVisual = stack.copy();
        stack.shrink(1);
        player.displayClientMessage(Component.translatable("message.laigu.pack_opened", cards.size()), true);
        // 开包产出的都是署名卡牌：首次获得时解锁对应图鉴条目（服务端）
        if (player instanceof ServerPlayer serverPlayer)
        {
            CodexHelper.scanAndUnlock(serverPlayer);
            // 开包特效：专属音效 + 专属粒子广播给附近玩家，并对开包玩家单独下发包面放大动画
            ServerLevel serverLevel = serverPlayer.serverLevel();
            serverLevel.playSound(null, player.getX(), player.getY() + 1.0, player.getZ(),
                    packType.openSound, SoundSource.PLAYERS, 1.0F, 1.0F);
            serverLevel.sendParticles(packType.openParticle,
                    player.getX(), player.getY() + 1.2, player.getZ(),
                    48, 0.7, 0.6, 0.7, 0.2);
            ModPackets.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer),
                    new PackOpenEffectPacket(packVisual));
        }
        return InteractionResultHolder.sidedSuccess(stack, true);
    }

    /** 按包类型概率开出若干张卡。 */
    private List<ItemStack> rollCards(Player player)
    {
        List<ItemStack> out = new ArrayList<>();
        List<String> pool = CardCatalog.CARD_IDS;
        for (int i = 0; i < CARDS_PER_PACK; i++)
        {
            String cardId = pool.get(player.getRandom().nextInt(pool.size()));
            String rarity = player.getRandom().nextDouble() < packType.goldChance ? "gold" : "common";

            ItemStack card = new ItemStack(ModItems.getCardItem(cardId, rarity));
            if (player.getRandom().nextDouble() < packType.glintChance)
            {
                CardNbt.setGlinted(card, true);
            }
            CardNbt.setOwner(card, player);
            CardNbt.ensureInstance(card); // 获得日期 + 唯一编号 + 胜利次数
            out.add(card);
        }
        return out;
    }
}
