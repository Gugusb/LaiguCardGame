package com.laigu.laigu.client;

import com.laigu.laigu.item.CardItem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

/**
 * 「端详」动画（第一人称）。
 * <p>
 * 手持卡牌右键（按住）后，卡牌在约 1 秒内由普通持握姿势抬起、转向玩家并放大，
 * 在镜头前停留（轻微摆动）——<b>只要不松开右键就一直保持端详</b>；
 * 松开右键后约 0.5 秒落回原位。
 * <p>
 * 通过 Forge 的 {@code IClientItemExtensions#applyForgeHandTransform} 挂接：
 * 返回 true 时本类完全接管第一人称手部姿态，用
 * {@code 净位姿 = 手臂偏移 · E(p) · base⁻¹} 组合，
 * 保证 p=0 / p=1 时与原版持握完全一致（无跳变、无缝衔接）。
 *
 * <h3>调参说明</h3>
 * 下面的 INSPECT_* 常量是「端详目标位姿」（与 base 同一单位：
 * 平移为 1/16 格、旋转为角度、缩放为倍数），需要按实际观感微调。
 */
@OnlyIn(Dist.CLIENT)
public final class InspectAnimator
{
    private static final float DEG = (float) (Math.PI / 180.0);

    /** 抬起动画时长（tick，20 tick=1 秒；用户反馈前摇过长，从 20 减半到 10） */
    private static final float RISE_TICKS = 10f;
    /** 松手回落动画时长（tick） */
    private static final float FALL_TICKS = 10f;

    /** 端详目标位姿（平移 1/16 格、旋转角度、缩放倍数；base 为普通持握值） */
    private static final float INSPECT_TX = 0.0f;     // base 右/左手 x ≈ ±0.0706
    private static final float INSPECT_TY = 0.0f;     // base y ≈ 0.2
    private static final float INSPECT_TZ = 0.12f;    // base z ≈ 0.0706，向镜头方向略移
    private static final float INSPECT_RX = 0.0f;
    private static final float INSPECT_RY = 0.0f;     // base yaw ≈ -90°，转正对玩家
    private static final float INSPECT_RZ = 0.0f;     // base roll ≈ 25°
    private static final float INSPECT_SCALE = 0.40f; // 用户要求端详时再缩小约 20%，从 0.50 调到 0.40

    /** 原版持握手臂偏移（{@code applyItemArmTransform} 稳态值，块单位） */
    private static final float ARM_X = 0.56f;
    private static final float ARM_Y = -0.52f;
    private static final float ARM_Z = -0.72f;

    /** 端详时手臂锚点目标（块单位）：原版手在屏幕右下，端详时把手臂抬起，
     *  使卡牌移到屏幕中央。x=0 水平居中、y=0 垂直居中（准星处）、
     *  z=-0.55 比原版(-0.72)更贴近镜头，便于看清卡面。可调。 */
    private static final float CENTER_ARM_X = 0.0f;
    private static final float CENTER_ARM_Y = 0.0f;
    private static final float CENTER_ARM_Z = -0.55f;

    /** 端详状态（客户端静态，仅本地玩家）：最近一次端详用的手，以及松手时刻（gameTime tick） */
    private static boolean wasUsing = false;
    private static HumanoidArm usedArmDuring = HumanoidArm.RIGHT;
    private static long releaseTick = -1L;
    /** 端详中的那张卡牌（回落期间也保留，用于 3D 模型 override 判定） */
    private static ItemStack inspectStack = ItemStack.EMPTY;

    private InspectAnimator()
    {
    }

    /**
     * 端详 3D 判定：当前第一人称渲染的卡牌是否正处于「端详（含回落）」。
     * 由物品模型 override 的 laigu:inspecting predicate 调用：返回 true 时把
     * 卡牌模型从扁平切换到 3D 牌框模型（见 assets/laigu/models/item/card_3d.json）。
     * 端详中按 {@code player.getUseItem() == stack}（同一实例），回落中按
     * {@code inspectStack == stack}（最近端详过的那张）判定，避免副手/背包误触发。
     */
    public static boolean isInspectionVisual(ItemStack stack)
    {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || stack.isEmpty())
        {
            return false;
        }
        LocalPlayer player = mc.player;
        if (player.isUsingItem()
                && (player.getUseItem().getItem() instanceof CardItem)
                && player.getUseItem() == stack)
        {
            return true;
        }
        return releaseTick >= 0L && inspectStack == stack;
    }

    /**
     * 端详保持阶段当前端详的卡牌（抬起已到位、卡停稳在屏幕中央）。
     * <p>
     * 供 HUD 名牌（{@link InspectNameHud}）取卡名用：仅在保持阶段返回卡，
     * 抬起进行中 / 松手回落 / 未端详时返回 EMPTY——避免名字在卡未停稳时
     * 浮在屏幕中央造成错位感。
     */
    public static ItemStack getHoldingInspectStack()
    {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || !player.isUsingItem()
                || !(player.getUseItem().getItem() instanceof CardItem))
        {
            return ItemStack.EMPTY;
        }
        ItemStack stack = player.getUseItem();
        int elapsed = stack.getUseDuration() - player.getUseItemRemainingTicks();
        return elapsed >= (int) RISE_TICKS ? stack : ItemStack.EMPTY;
    }

    /** Forge 第一人称手部姿态钩子：端详中或回落中接管姿态返回 true，否则交还原版。 */
    public static boolean apply(PoseStack pose, LocalPlayer player, HumanoidArm arm,
            ItemStack stack, float partialTick)
    {
        boolean usingCard = player.isUsingItem()
                && (player.getUseItem().getItem() instanceof CardItem);
        long gameTick = player.level().getGameTime();

        float p;
        if (usingCard)
        {
            // 只接管正在使用卡牌的那只手
            InteractionHand usedHand = player.getUsedItemHand();
            HumanoidArm usedArm = usedHand == InteractionHand.MAIN_HAND
                    ? player.getMainArm() : player.getMainArm().getOpposite();
            if (arm != usedArm)
            {
                return false;
            }
            wasUsing = true;
            usedArmDuring = arm;
            releaseTick = -1L;
            inspectStack = stack;

            // 抬起用固定 RISE_TICKS，随后保持 1（按住右键期间一直端详）
            int duration = player.getUseItem().getUseDuration();
            int elapsed = duration - player.getUseItemRemainingTicks();
            p = Mth.clamp((float) elapsed / RISE_TICKS, 0f, 1f);
        }
        else
        {
            // 不在端详：刚松开右键则记录时刻并播放回落动画
            if (wasUsing)
            {
                wasUsing = false;
                releaseTick = gameTick;
            }
            if (releaseTick < 0L || arm != usedArmDuring)
            {
                return false;
            }
            long since = gameTick - releaseTick;
            if (since > (long) FALL_TICKS)
            {
                releaseTick = -1L;
                inspectStack = ItemStack.EMPTY;
                return false;
            }
            p = 1f - (float) since / FALL_TICKS; // 1 → 0 回落
        }

        boolean leftHand = arm == HumanoidArm.LEFT;

        // 1) 该手对应的原版 FIRST_PERSON 基础变换（含左右镜像），取自实际模型，保证与渲染完全一致
        BakedModel model = Minecraft.getInstance().getItemRenderer()
                .getModel(stack, player.level(), player, Item.getId(stack.getItem()));
        ItemTransform baseT = model.getTransforms().getTransform(
                leftHand ? ItemDisplayContext.FIRST_PERSON_LEFT_HAND : ItemDisplayContext.FIRST_PERSON_RIGHT_HAND);
        PoseStack scratch = new PoseStack();
        baseT.apply(leftHand, scratch);
        Matrix4f base = scratch.last().pose();
        Matrix4f baseInv = base.invert(new Matrix4f());

        // 2) 基础变换分量（左手按 ItemTransform.apply 镜像：x 平移、y/z 旋转取反）
        float bx = leftHand ? -baseT.translation.x() : baseT.translation.x();
        float by = baseT.translation.y();
        float bz = baseT.translation.z();
        float brx = baseT.rotation.x();
        float bry = leftHand ? -baseT.rotation.y() : baseT.rotation.y();
        float brz = leftHand ? -baseT.rotation.z() : baseT.rotation.z();
        float bs = baseT.scale.x();

        // 3) 缓动与端详保持阶段的轻微摆动（摆动用游戏刻计时，保持期持续轻微摆动）
        float amount = amount(usingCard, p);
        float holdEnv = usingCard ? Mth.clamp((p - 0.85f) * 6.7f, 0f, 1f) : 0f;
        float bob = holdEnv * Mth.sin(gameTick * 0.4f) * 0.03f;

        // 4) 逐分量插值：base -> 端详目标
        float tx = Mth.lerp(amount, bx, INSPECT_TX);
        float ty = Mth.lerp(amount, by, INSPECT_TY);
        float tz = Mth.lerp(amount, bz, INSPECT_TZ);
        float rx = Mth.lerp(amount, brx, INSPECT_RX);
        float ry = Mth.lerp(amount, bry, INSPECT_RY);
        float rz = Mth.lerp(amount, brz, INSPECT_RZ) + bob;
        float sc = Mth.lerp(amount, bs, INSPECT_SCALE);

        Matrix4f e = new Matrix4f().identity()
                .translate(tx, ty, tz)
                .rotate(new Quaternionf().rotationXYZ(rx * DEG, ry * DEG, rz * DEG))
                .scale(sc);

        // 5) 手臂锚点随端详进度从原版右下抬向屏幕中央（同一缓动；左手 x 镜像），
        //    否则卡牌只会停在右下角（锚点位移量远大于 E 内的小幅平移）
        float armX = leftHand ? -ARM_X : ARM_X;
        float centerArmX = leftHand ? -CENTER_ARM_X : CENTER_ARM_X;
        float ax = Mth.lerp(amount, armX, centerArmX);
        float ay = Mth.lerp(amount, ARM_Y, CENTER_ARM_Y);
        float az = Mth.lerp(amount, ARM_Z, CENTER_ARM_Z);
        Matrix4f armMat = new Matrix4f().translation(ax, ay, az);

        // 6) 姿态 = 手臂 · E · base⁻¹；随后原版在 handleCameraTransforms 再叠 base，净结果 = 手臂 · E
        Matrix4f poseMatrix = armMat.mul(e, new Matrix4f()).mul(baseInv, new Matrix4f());
        pose.mulPoseMatrix(poseMatrix);
        return true;
    }

    /** 缓动：端详时抬起缓出、到位后保持；回落后缓入。 */
    private static float amount(boolean usingCard, float p)
    {
        if (usingCard)
        {
            return p >= 1f ? 1f : (1f - (1f - p) * (1f - p));
        }
        return p * p;
    }
}
