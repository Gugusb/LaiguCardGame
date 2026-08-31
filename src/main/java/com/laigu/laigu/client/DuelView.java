package com.laigu.laigu.client;

import com.laigu.laigu.duel.DuelGame;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

/**
 * 客户端视角的对局状态（由 {@link com.laigu.laigu.network.DuelStateS2CPacket} 的 NBT 解析）。
 * 手牌只含持有者自己的；对方暗置卡为 hidden。
 */
@OnlyIn(Dist.CLIENT)
public class DuelView
{
    /** 场上一张卡（按槽位 0-3）。hidden 时 card 为空。 */
    public static class FieldView
    {
        public final int slot;
        public final ItemStack card;
        public final int[] dice;
        public final boolean hidden;
        public final boolean lasted;
        public final int activation;   // 【激活x】当前进度（UI 进度条）

        FieldView(int slot, ItemStack card, int[] dice, boolean hidden, boolean lasted, int activation)
        {
            this.slot = slot;
            this.card = card;
            this.dice = dice;
            this.hidden = hidden;
            this.lasted = lasted;
            this.activation = activation;
        }
    }

    /** 计分动画步骤：某卡/基础汇总结算后，该侧累计到 (base, mult, extra)。 */
    public static class ScoreStepView
    {
        public final int side;
        public final int slot;
        public final int ticks;
        public final int base, mult, extra;
        public final int kind;   // 0=普通 1=激活 2=伏击 3=破阵

        ScoreStepView(int side, int slot, int ticks, int base, int mult, int extra, int kind)
        {
            this.side = side; this.slot = slot; this.ticks = ticks;
            this.base = base; this.mult = mult; this.extra = extra;
            this.kind = kind;
        }
    }

    public final int round;
    public final DuelGame.Phase phase;
    public final int mySide;
    public final int wins0;
    public final int wins1;
    public final int winnerLast;
    public final int currentPicker;
    public final int pickTarget;
    public final int[] myDraft;
    public final int[] oppDraft;
    public final int draftTurnLeft;
    public final boolean started;
    /** 观战模式：默认主机视角、双方操作透明，不可操作（无手牌/骰池操作，仅可退出）。 */
    public final boolean spectate;
    public final boolean darkMode;
    public final boolean creative;
    public final int myAp;
    /** 我方每轮行动力上限（第1轮2、第2轮3、第3轮起4）。旧服务端无此键 → 2。 */
    public final int myApMax;
    public final boolean myDeployDone;
    public final boolean oppDeployDone;
    public final boolean myPlaceDone;
    public final boolean oppPlaceDone;
    public final boolean myRoundEndDone;
    public final boolean oppRoundEndDone;
    /** 牌库剩余张数（左下角牌库展示 / 抽卡动画用）。 */
    public final int deckCount;
    /** 我方/对方玩家名字（服务端登记名；AI 为 AI·来古）。 */
    public final String myName;
    public final String oppName;

    public final List<ItemStack> hand = new ArrayList<>();
    public final List<FieldView> myField = new ArrayList<>();
    public final List<FieldView> oppField = new ArrayList<>();
    public final List<Integer> myPool = new ArrayList<>();
    public final List<Integer> oppPool = new ArrayList<>();
    public final List<Integer> shared = new ArrayList<>();

    public final boolean hasResult;
    public final int lastBase;
    public final int lastMult;
    public final int lastExtra;
    public final int myLastTotal;
    public final int oppLastTotal;
    public final int oppBase;
    public final int oppMult;
    public final int oppExtra;
    public final int timingBase;
    public final int timingMult;
    public final int timingExtra;
    public final int oppTimingBase;
    public final int oppTimingMult;
    public final int oppTimingExtra;
    public final int draftPickSerial;
    public final int oppDraftPickSerial;

    public final List<ScoreStepView> scoreSteps = new ArrayList<>();
    public final List<String> battleLog = new ArrayList<>();

    public DuelView(CompoundTag t)
    {
        round = t.getInt("round");
        int ph = t.getInt("phase");
        DuelGame.Phase[] phases = DuelGame.Phase.values();
        phase = ph >= 0 && ph < phases.length ? phases[ph] : DuelGame.Phase.REGISTER;
        mySide = t.getInt("mySide");
        wins0 = t.getInt("wins0");
        wins1 = t.getInt("wins1");
        winnerLast = t.getInt("winnerLast");
        currentPicker = t.getInt("currentPicker");
        pickTarget = t.getInt("pickTarget");
        myDraft = t.getIntArray("myDraft");
        oppDraft = t.getIntArray("oppDraft");
        draftTurnLeft = t.getInt("draftTurnLeft");
        started = t.getBoolean("started");
        spectate = t.getBoolean("spectate");
        darkMode = t.getBoolean("darkMode");
        creative = t.getBoolean("creative");
        myAp = t.getInt("myAp");
        myApMax = t.contains("myApMax") ? t.getInt("myApMax") : 2;
        myDeployDone = t.getBoolean("myDeployDone");
        oppDeployDone = t.getBoolean("oppDeployDone");
        myPlaceDone = t.getBoolean("myPlaceDone");
        oppPlaceDone = t.getBoolean("oppPlaceDone");
        myRoundEndDone = t.getBoolean("myRoundEndDone");
        oppRoundEndDone = t.getBoolean("oppRoundEndDone");
        deckCount = t.getInt("deckCount");
        myName = t.getString("myName");
        oppName = t.getString("oppName");

        ListTag handTag = t.getList("hand", Tag.TAG_COMPOUND);
        for (int i = 0; i < handTag.size(); i++)
        {
            hand.add(ItemStack.of(handTag.getCompound(i)));
        }
        myField.addAll(parseField(t.getList("myField", Tag.TAG_COMPOUND)));
        oppField.addAll(parseField(t.getList("oppField", Tag.TAG_COMPOUND)));
        for (int v : t.getIntArray("myPool")) myPool.add(v);
        for (int v : t.getIntArray("oppPool")) oppPool.add(v);
        for (int v : t.getIntArray("shared")) shared.add(v);

        hasResult = phase == DuelGame.Phase.ROUND_END || phase == DuelGame.Phase.FINISHED;
        ListTag stepTag = t.getList("scoreSteps", Tag.TAG_COMPOUND);
        for (int i = 0; i < stepTag.size(); i++)
        {
            CompoundTag c = stepTag.getCompound(i);
            scoreSteps.add(new ScoreStepView(
                    c.getInt("side"), c.getInt("slot"), Math.max(1, c.getInt("ticks")),
                    c.getInt("base"), c.getInt("mult"), c.getInt("extra"), c.getInt("kind")));
        }
        lastBase = t.getInt("lastBase");
        lastMult = t.getInt("lastMult");
        lastExtra = t.getInt("lastExtra");
        myLastTotal = t.getInt("myLastTotal");
        oppLastTotal = t.getInt("oppLastTotal");
        oppBase = t.getInt("oppBase");
        oppMult = t.getInt("oppMult");
        oppExtra = t.getInt("oppExtra");
        timingBase = t.getInt("timingBase");
        timingMult = t.getInt("timingMult");
        timingExtra = t.getInt("timingExtra");
        oppTimingBase = t.getInt("oppTimingBase");
        oppTimingMult = t.getInt("oppTimingMult");
        oppTimingExtra = t.getInt("oppTimingExtra");
        draftPickSerial = t.getInt("draftPickSerial");
        oppDraftPickSerial = t.getInt("oppDraftPickSerial");
        // 战斗播报
        ListTag logTag = t.getList("battleLog", Tag.TAG_STRING);
        for (int i = 0; i < logTag.size(); i++) battleLog.add(logTag.getString(i));
    }

    /** 我方胜场（大比分按己方视角展示，避免 A/B 绝对侧错位）。 */
    public int myWins()
    {
        return mySide == 0 ? wins0 : wins1;
    }

    /** 对方胜场。 */
    public int oppWins()
    {
        return mySide == 0 ? wins1 : wins0;
    }

    /** 我方显示名（空则回退"你"）。 */
    public String displayMyName()
    {
        return myName == null || myName.isEmpty() ? "你" : myName;
    }

    /** 对方显示名（空则回退"对手"）。 */
    public String displayOppName()
    {
        return oppName == null || oppName.isEmpty() ? "对手" : oppName;
    }

    private static List<FieldView> parseField(ListTag tag)
    {
        List<FieldView> list = new ArrayList<>();
        for (int i = 0; i < tag.size(); i++)
        {
            CompoundTag c = tag.getCompound(i);
            boolean hidden = c.getBoolean("hidden");
            list.add(new FieldView(
                    c.getInt("slot"),
                    hidden ? ItemStack.EMPTY : ItemStack.of(c.getCompound("card")),
                    c.getIntArray("dice"),
                    hidden,
                    c.getBoolean("lasted"),
                    c.getInt("activation")));
        }
        return list;
    }
}
