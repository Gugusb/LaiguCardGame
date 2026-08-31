package com.laigu.laigu.client;

import com.laigu.laigu.Laigu;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.slf4j.Logger;

/**
 * 「开包」第一人称动画（客户端静态状态机）。
 * <p>
 * 开包时卡包出现在镜头前 → 放大并原地旋转一整圈 → 沿视线方向飞远消失，
 * 配合服务端广播的每包专属音效与大量粒子，做出不死图腾发动级别的冲击感。
 * <p>
 * 由 {@code PackOpenEffectPacket} 在客户端触发 {@link #start(ItemStack)}，
 * 每帧在 {@link RenderLevelStageEvent}（AFTER_TRANSLUCENT_BLOCKS）中按游戏刻
 * 计算相位并渲染：位置 = 镜头前方沿视线方向，绕视线轴旋转（像硬币原地转一圈）。
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = Laigu.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class PackOpenAnimator
{
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final float DEG = (float) (Math.PI / 180.0);

    /**
     * 相位时长（tick，20=1 秒）。2026-08-12 用户要求整体提速 50%：
     * 总时长 33t → 22t（各相位 ×2/3）。前段（出现+转圈）15t、后段（飞走）7t。
     */
    private static final int T_APPEAR = 6;                      // 出现
    private static final int T_ROTATE = 9;                      // 原地旋转一圈
    private static final int T_FRONT = T_APPEAR + T_ROTATE;     // 前段合计 15
    private static final int T_FLY = 7;                         // 飞走
    private static final int DURATION = T_FRONT + T_FLY;        // 总 22

    /** 动画状态 */
    private static ItemStack animStack = ItemStack.EMPTY;
    private static long startTick = -1L;
    private static boolean loggedRender = false;

    private PackOpenAnimator()
    {
    }

    /** 由开包数据包在客户端调用，开始播动画。 */
    public static void start(ItemStack packStack)
    {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null)
        {
            return;
        }
        animStack = packStack.copy();
        startTick = mc.level.getGameTime();
        loggedRender = false;
        LOGGER.info("开包动画 start: {}", packStack.getHoverName().getString());
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event)
    {
        // 注意：1.20.1 只分发 AFTER_SKY/AFTER_ENTITIES/AFTER_BLOCK_ENTITIES/AFTER_PARTICLES/AFTER_WEATHER，
        // AFTER_TRANSLUCENT_BLOCKS 定义了但从不触发（踩过坑：动画监听它导致完全不渲染）
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES)
        {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || animStack.isEmpty() || startTick < 0L)
        {
            return;
        }
        float t = (float) (mc.level.getGameTime() + event.getPartialTick() - startTick);
        if (t >= DURATION)
        {
            animStack = ItemStack.EMPTY;
            startTick = -1L;
            return;
        }

        // ---- 分相位计算（按 tick 时长）：距离 / 左右偏移 / 上下偏移 / 缩放 / 旋转角 ----
        float dist, xOff, yOff, scale, yaw;
        if (t < T_APPEAR)
        {
            float q = easeOut(t / T_APPEAR);
            dist = 0.50f + 0.05f * q;
            xOff = 0.16f * (1f - q);
            yOff = -0.12f * (1f - q);
            scale = 0.30f + 0.70f * q;
            yaw = 90f * q;
        }
        else if (t < T_FRONT)
        {
            float q = (t - T_APPEAR) / T_ROTATE;
            dist = 0.55f + 0.08f * q;
            xOff = 0f;
            yOff = Mth.sin(q * Mth.PI) * 0.06f; // 轻微上下浮动
            scale = 1.0f;
            yaw = 90f + 360f * q;                 // 原地旋转一整圈
        }
        else
        {
            float q = easeIn((t - T_FRONT) / T_FLY);
            dist = 0.63f + 2.2f * q;
            xOff = 0f;
            yOff = 0.10f * q;
            scale = 1.0f - 0.4f * q;
            yaw = 450f + 180f * q;                // 继续旋转并飞远
        }

        Camera camera = event.getCamera();
        Vec3 camPos = camera.getPosition();
        // 视线方向正交基（forward/right/up）
        Vec3 fwd = mc.player.getViewVector(event.getPartialTick()).normalize();
        Vec3 right = fwd.cross(new Vec3(0.0, 1.0, 0.0)).normalize();
        if (right.lengthSqr() < 1.0E-6)
        {
            right = new Vec3(1.0, 0.0, 0.0); // 正视上/下时兜底
        }
        Vec3 up = right.cross(fwd).normalize();
        Vec3 target = camPos.add(fwd.scale(dist)).add(right.scale(xOff)).add(up.scale(yOff));

        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        pose.translate(target.x - camPos.x, target.y - camPos.y, target.z - camPos.z);
        pose.scale(scale, scale, scale);
        // 朝向修正：卡面（模型 +Z）始终转向镜头并保持竖直，否则卡面法线经视图矩阵后
        // 恒朝世界正南——玩家面向东西时卡牌会侧立（显示侧面而非正面）。
        // 列基 (right, up, -fwd)：模型 X→镜头右、Y→镜头上、Z→镜头方向。
        Matrix3f basis = new Matrix3f(
                (float) right.x, (float) up.x, (float) -fwd.x,
                (float) right.y, (float) up.y, (float) -fwd.y,
                (float) right.z, (float) up.z, (float) -fwd.z);
        Quaternionf face = new Quaternionf().setFromNormalized(basis);
        face.mul(new Quaternionf().rotationZ(yaw * DEG)); // 先自旋再转面，绕视线轴转圈（像硬币）
        pose.mulPose(face);

        if (!loggedRender)
        {
            loggedRender = true;
            LOGGER.info("开包动画首帧渲染 t={}", t);
        }
        MultiBufferSource.BufferSource buffer = mc.renderBuffers().bufferSource();
        mc.getItemRenderer().renderStatic(animStack, ItemDisplayContext.FIXED,
                0xF000F0, OverlayTexture.NO_OVERLAY, pose, buffer, mc.level, mc.player.getId());
        buffer.endBatch();
        pose.popPose();
    }

    private static float easeOut(float t)
    {
        return 1f - (1f - t) * (1f - t);
    }

    private static float easeIn(float t)
    {
        return t * t;
    }
}
