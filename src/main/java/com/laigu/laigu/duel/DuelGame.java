package com.laigu.laigu.duel;

import com.laigu.laigu.util.CardNbt;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 对战对局（纯服务端逻辑，不依赖方块实体）。
 *
 * 流程：REGISTER → DEPLOY(行动力 2/3/4) → DRAFT(抢骰) → PLACE(布置) → ROUND_END → [下一轮] → FINISHED
 *  - 揭示在双方确认部署后自动发生（faceDown 清除），仅信息同步，不结算效果。
 *  - 手牌不设上限；每轮抽 1（第 1 轮抽 5），行动力每轮递增（第 1 轮 2、第 2 轮 3、第 3 轮起 4），可放空槽或替换弃置。
 *  - 金卡不能直接召唤：只能替换场上的同名白卡（白卡弃置，槽位骰子继承给金卡）。
 *  - 选骰数 = 本轮目标持有骰数（含继承）；未布置的骰子继承到下一回合。
 *  - 基础分/骰型只算【放置在场卡上的骰】；朝代/职业/金质计数只算【场上卡】。
 *  - 大比分抢二胜制：一方领先 2 分获胜（2:0/3:1/5:3），封顶 5 轮，未拉开则净胜分多者胜、同分平局。
 */
public class DuelGame
{
    public enum Phase { REGISTER, DEPLOY, DRAFT, PLACE, ROUND_END, FINISHED }

    public static final int FIELD_SLOTS = 5;
    public static final int DECK_SIZE = 16;
    public static final int MAX_ROUNDS = 5;         // 大比分抢二胜：封顶轮数，未拉开则净胜分定胜负
    public static final int SHARED_POOL_SIZE = 10;  // 系统默认掷出的骰子数（部分卡可 +N）
    public static final int MAX_DICE_HELD = 8;      // 每方同时持有的骰子上限（含继承）

    /** 每轮行动力：第 1 轮 2、第 2 轮 3、第 3 轮起 4（上限 4）。 */
    public static int maxAp(int round)
    {
        return Math.min(round + 1, 4);
    }
    public static final int FIRST_DRAFT_TURNS = 3;  // 先手默认抓取次数：1/2/…/2/1
    public static final int SECOND_DRAFT_TURNS = 2; // 后手默认抓取次数：2/2/…
    public static final int MAX_HAND = 8;           // 手牌上限：抽多的牌进入弃牌堆

    // ---- 每方状态 ----
    private final List<ItemStack>[] deckOriginal = new List[2];
    private final List<ItemStack>[] deck = new List[2];
    private final List<ItemStack>[] hand = new List[2];
    private final List<ItemStack>[] discard = new List[2];   // 弃牌堆：手牌超上限时溢出的卡
    private final List<FieldCard>[] field = new List[2];
    private final List<Integer>[] pool = new List[2];   // 骰池（含继承的未布置骰）
    private final boolean[] deployDone = new boolean[2];
    private final boolean[] placeDone = new boolean[2];
    private final boolean[] roundEndDone = new boolean[2];
    private final int[] wins = new int[2];
    /** 本局内各侧<b>部署上场过</b>的卡牌唯一编号（用于结算「参战胜利次数」）。 */
    private final Set<String>[] deployedUids = new Set[2];
    private final int[] actionPoints = new int[2];
    private final int[] draftTarget = new int[2];       // 本轮还需抓取的骰数（计划制，逐颗递减）
    // 抢骰计划：抓取次数/每次几颗 由场上「抓取系」卡牌修改
    private final List<int[]> draftPlan = new ArrayList<>(); // 每项 {side, 本次抓几颗}
    private int[] draftFirstSizes = new int[0];             // 先手每次抓取数（1/2/…/2/1）
    private int[] draftSecondSizes = new int[0];            // 后手每次抓取数（2/2/…）
    private int draftPlanIdx = 0;                            // 计划当前执行到第几项
    private int draftTurnRemaining = 0;                      // 当前这次抓取还剩几颗
    private int rerollUses = 0;                              // 本轮已使用的重骰次数
    private final int[] draftPickSerial = new int[2];       // 本轮每方已完成抓骰动作次数
    private final int[] lastBase = new int[2];
    private final int[] lastMult = new int[2];
    private final int[] lastExtra = new int[2];
    private final int[] lastTotal = new int[2];
    /** 得分时机/激活奖励（非最终结算）累计：基础分/倍率加成/额外分，结算时并入本轮总分。 */
    private final int[] timingBase = new int[2];
    private final int[] timingMult = new int[2];
    private final int[] timingExtra = new int[2];
    private final int[] overflowDrawMult = new int[2];
    private final int[] actionPointsSpentThisRound = new int[2];
    private final int[] friendlyActivationsThisRound = new int[2];
    private final int[] activationFailuresThisRound = new int[2];
    private final boolean[] parityOverrideOdd = new boolean[2];
    private final boolean[] parityOverrideEven = new boolean[2];
    /** 本局累计总分（每轮 lastTotal 累加）。打满 5 轮未拉开比分时按净胜分判胜负。 */
    private final int[] totalScore = new int[2];
    /** 本轮计分动画步骤（先手卡自左向右，再后手卡），结算阶段同步给客户端。 */
    private List<ScoreEngine.ScoreStep> lastSteps = new ArrayList<>();

    private final List<Integer> sharedPool = new ArrayList<>();
    private final RandomSource rnd;

    private int round;
    private Phase phase = Phase.REGISTER;
    private int currentPicker;
    private int firstPicker;
    private int winnerLast = -1;
    private boolean started;
    /** 黑暗对决：胜者抢夺败者一张真实卡（由主机在开战前设置）。 */
    public boolean darkMode;
    /** 创造卡组：无视规则（可重复/不足16张/全金卡/金卡可直接放置并仍触发焕章）。 */
    public boolean creative;

    /** 最近一次操作的服务端提示（null=正常），由方块实体转发给玩家。 */
    public String lastMsg;

    /** 左侧战斗播报 log（时间序，客户端滚动展示；保留最近 N 轮）。 */
    public final List<String> battleLog = new ArrayList<>();
    /** 当前 log 条数的上限（超出丢弃最旧）。 */
    public static final int BATTLE_LOG_MAX = 40;

    /** 追加一条战斗播报（我方=绿色 敌方=红色 中立=黄色，客户端按侧染色）。 */
    public void addLog(int side, String msg)
    {
        String prefix = side == 0 ? "A " : (side == 1 ? "B " : "N ");
        battleLog.add(prefix + msg);
        while (battleLog.size() > BATTLE_LOG_MAX) battleLog.remove(0);
    }

    public DuelGame(RandomSource rnd)
    {
        this.rnd = rnd;
        for (int s = 0; s < 2; s++)
        {
            deckOriginal[s] = new ArrayList<>();
            deck[s] = new ArrayList<>();
            hand[s] = new ArrayList<>();
            discard[s] = new ArrayList<>();
            field[s] = new ArrayList<>();
            for (int k = 0; k < FIELD_SLOTS; k++) field[s].add(null);
            pool[s] = new ArrayList<>();
            lastMult[s] = 1;
            deployedUids[s] = new HashSet<>();
        }
    }

    // ================= 卡组校验 =================

    public static boolean isDeckLegal(List<ItemStack> deck)
    {
        if (deck == null || deck.size() != DECK_SIZE) return false;
        Set<String> seen = new HashSet<>();
        for (ItemStack s : deck)
        {
            DuelCardData d = DuelCardCatalog.of(s);
            if (d == null) return false;
            // 完全相同的卡（同一物品）只允许一张；普通/金质为不同卡，可共存
            if (!seen.add(CardNbt.pathOf(s))) return false;
        }
        return true;
    }

    // ================= 登记 / 开局 =================

    public void setDeck(int side, List<ItemStack> d)
    {
        deckOriginal[side] = new ArrayList<>(d);
    }

    public boolean sideReady(int side)
    {
        return deckOriginal[side] != null && !deckOriginal[side].isEmpty();
    }

    /** 登记卡组的副本（对局作废返还 / 黑暗对决兜底用），不暴露内部可变列表。 */
    public List<ItemStack> deckOriginalCopy(int side)
    {
        return deckOriginal[side] == null ? new ArrayList<>() : new ArrayList<>(deckOriginal[side]);
    }

    /** 黑暗对决：从登记卡组（存储在方块内的卡组）移除指定下标的卡，夺卡后卡组少一张。 */
    public void removeDeckCard(int side, int index)
    {
        if (deckOriginal[side] != null && index >= 0 && index < deckOriginal[side].size())
        {
            deckOriginal[side].remove(index);
        }
    }

    public boolean isStarted()
    {
        return started;
    }

    public boolean isFinished()
    {
        return phase == Phase.FINISHED;
    }

    /** 双方卡组就绪后开局（或「再来一局」重新开局）。 */
    public void start()
    {
        started = true;
        round = 1;
        wins[0] = wins[1] = 0;
        totalScore[0] = totalScore[1] = 0;
        winnerLast = -1;
        deployedUids[0].clear();
        deployedUids[1].clear();
        for (int s = 0; s < 2; s++)
        {
            deck[s] = new ArrayList<>(deckOriginal[s]);
            java.util.Collections.shuffle(deck[s], new java.util.Random(rnd.nextLong()));   // 洗牌：抽卡随机
            hand[s].clear();
            discard[s].clear();
            for (int i = 0; i < FIELD_SLOTS; i++) field[s].set(i, null);
            pool[s].clear();
            lastTotal[s] = 0;
        }
        lastSteps = new ArrayList<>();
        battleLog.clear();   // 跨局清空战斗播报
        startRound();
    }

    /** 「再来一局」：同卡组直接重新开局（沿用黑暗对决设置）。 */
    public void rematch()
    {
        if (!isFinished()) return;
        start();
    }

    private void startRound()
    {
        lastSteps = new ArrayList<>();   // 每轮计分步骤重置，避免上一轮动画带到认输结算
        lastBase[0] = lastBase[1] = 0;   // 跨回合清空本轮计分（基础/倍率/额外/总分）
        lastMult[0] = lastMult[1] = 1;
        lastExtra[0] = lastExtra[1] = 0;
        lastTotal[0] = lastTotal[1] = 0;
        for (int s = 0; s < 2; s++)
        {
            deployDone[s] = placeDone[s] = roundEndDone[s] = false;
            actionPoints[s] = maxAp(round);
            // 每轮开始：公共骰池重掷（下面 sharedPool.clear+refill）；【手上未布置的骰子继承到本轮，不清空】
            timingBase[s] = timingMult[s] = timingExtra[s] = 0;
            overflowDrawMult[s] = 0;
            actionPointsSpentThisRound[s] = 0;
            friendlyActivationsThisRound[s] = 0;
            activationFailuresThisRound[s] = 0;
            parityOverrideOdd[s] = parityOverrideEven[s] = false;
            for (int i = 0; i < FIELD_SLOTS; i++)
            {
                FieldCard fc = field[s].get(i);
                if (fc != null)
                {
                    fc.dice.clear();       // 场上骰子每轮重掷
                    fc.consumed = false;
                    fc.lastedLastRound = true; // 幸存到本轮的卡记为"上轮在场"
                    fc.roundsOnField++;    // 连续在场轮数 +1
                }
            }
            drawCards(s, round == 1 ? 5 : 1);
            if (!com.laigu.laigu.duel.newcard.NewCardCoreSwitch.enabled())
                triggerRoundStart(s);   // 每轮开始触发系（场上卡抽卡）——仅回滚模式
            collectTiming(s, EffectType.ROUND_START_SCORE_EXTRA, 0);   // 得分时机：回合开始时
            // 【激活x】每轮开始：清空上轮激活进度（激活在本轮结算时按充能x触发）
            for (int i = 0; i < FIELD_SLOTS; i++)
            {
                FieldCard fc = field[s].get(i);
                if (fc != null) fc.activation = 0;
            }
        }
        // 阶段18：每轮开始的场上卡触发（抽卡类）由新核心派发（含场上卡轮次推进），旧触发系仅回滚模式保留。
        if (lifecycleHook != null && com.laigu.laigu.duel.newcard.NewCardCoreSwitch.enabled())
            lifecycleHook.onRoundStart();
        sharedPool.clear();
        for (int i = 0; i < SHARED_POOL_SIZE; i++) sharedPool.add(rnd.nextInt(6) + 1);
        firstPicker = round == 1 ? rnd.nextInt(2) : (winnerLast == -1 ? rnd.nextInt(2) : 1 - winnerLast);
        currentPicker = firstPicker;
        // 清空上轮抓取计划（本轮在双方部署确认后重建）
        draftPlan.clear();
        draftFirstSizes = new int[0];
        draftSecondSizes = new int[0];
        draftPlanIdx = 0;
        draftTurnRemaining = 0;
        rerollUses = 0;
        draftPickSerial[0] = draftPickSerial[1] = 0;
        draftTarget[0] = draftTarget[1] = 0;
        phase = Phase.DEPLOY;
    }

    private void drawCards(int side, int n)
    {
        // 手牌上限 MAX_HAND：抽到超出的牌自动进入弃牌堆，并发出本轮超限事件。
        for (int i = 0; i < n; i++)
        {
            if (deck[side].isEmpty()) return;
            hand[side].add(deck[side].remove(0));
            if (hand[side].size() > MAX_HAND)
            {
                discard[side].add(hand[side].remove(hand[side].size() - 1));
                for (FieldCard fc : field[side])
                {
                    if (fc == null) continue;
                    DuelCardData d = DuelCardCatalog.of(fc.card);
                    if (d == null) continue;
                    if (DuelCardData.isGold(fc.card) && "song_jin_xiang_shi".equals(d.cardId)) fc.persistentBaseBonus = 3;
                    if (DuelCardData.isGold(fc.card) && "wu_xian_pi_pa".equals(d.cardId)) overflowDrawMult[side] = 1;
                }
            }
        }
    }

    // ================= 操作分发 =================

    public void applyAction(int side, int action, int a, int b)
    {
        lastMsg = null;
        if (!started)
        {
            // 认输后（started=false, phase=FINISHED）仍允许「再来一局」
            if (phase == Phase.FINISHED && action == DuelActions.REMATCH)
            {
                rematch();
                return;
            }
            lastMsg = "对局尚未开始";
            return;
        }
        if (phase == Phase.FINISHED)
        {
            if (action == DuelActions.REMATCH) rematch();
            return;
        }
        switch (action)
        {
            case DuelActions.DEPLOY_PUT -> deployPut(side, a, b);
            case DuelActions.DEPLOY_CONFIRM -> deployConfirm(side);
            case DuelActions.PICK_DIE -> pickDie(side, a);
            case DuelActions.SKIP_DRAFT -> skipDraft(side);
            case DuelActions.PLACE_DIE -> placeDie(side, a, b);
            case DuelActions.PLACE_TAKE_DIE -> placeTakeDie(side, a, b);
            case DuelActions.PLACE_CONFIRM -> placeConfirm(side);
            case DuelActions.NEXT_ROUND -> nextRound(side);
            case DuelActions.FORFEIT -> forfeit(side);
            default -> lastMsg = "未知操作";
        }
    }

    // ================= 部署（行动力） =================

    /**
     * 用手牌替换/放置场上某个位置（从无到有也是替换）：消耗 1 行动力。
     * 槽位原本有卡则直接弃置（不回手）。
     * 金卡不能直接召唤：只能替换场上的同名白卡（替换时槽位骰子继承给金卡）。
     */
    private void deployPut(int side, int handIdx, int slot)
    {
        if (phase != Phase.DEPLOY) { lastMsg = "当前不是部署阶段"; return; }
        if (deployDone[side]) { lastMsg = "你已确认部署"; return; }
        if (actionPoints[side] <= 0) { lastMsg = "行动力已用完"; return; }
        if (handIdx < 0 || handIdx >= hand[side].size()) { lastMsg = "无效手牌"; return; }
        if (slot < 0 || slot >= FIELD_SLOTS) { lastMsg = "无效场上槽位"; return; }
        FieldCard old = field[side].get(slot);
         if (old != null && old.locked)
         {
             lastMsg = "该卡已被封锁，无法更换或焕章";
             return;
         }
        ItemStack c = hand[side].get(handIdx);
        // 金卡召唤限制：只能替换场上同名白卡；放空槽/非同名槽一律拒绝。创造卡组(creative)无视此规则，可直接放金卡。
        if (old != null && old.locked) { lastMsg = "该卡已被封锁，无法更换或焕章"; return; }
        if (!creative && DuelCardData.isGold(c))
        {
            if (old == null)
            {
                lastMsg = "金卡不能直接召唤，需先放同名白卡上场再替换";
                return;
            }
            String newBase = CardNbt.stripRaritySuffix(CardNbt.pathOf(c));
            String oldBase = CardNbt.stripRaritySuffix(CardNbt.pathOf(old.card));
            if (!newBase.equals(oldBase) || DuelCardData.isGold(old.card))
            {
                lastMsg = "金卡只能替换场上的同名白卡";
                return;
            }
        }
        c = hand[side].remove(handIdx);
        FieldCard nf = new FieldCard(c);
        // 固有消耗：带【消耗】词条的卡（isConsume 效果）部署即标记，本轮结束必然移出（无论是否触发）
        DuelCardData nd = DuelCardCatalog.of(c);
        if (nd != null && nd.effectFor(c).isConsume()) nf.intrinsicConsume = true;
        if (old != null && DuelCardData.isGold(c)) nf.dice.addAll(old.dice);   // 金卡替换继承槽位骰子
        // 阶段18：被替换旧卡的离场事件由新核心派发（旧卡仍在场位上，影子可定位）。
        if (old != null && lifecycleHook != null && com.laigu.laigu.duel.newcard.NewCardCoreSwitch.enabled())
            lifecycleHook.onLeave(side, slot);
        field[side].set(slot, nf);
        actionPoints[side]--;
        actionPointsSpentThisRound[side]++;
        // 观星金卡：每次实际消耗行动力，本回合获得+15额外分。
        for (FieldCard spent : field[side])
        {
            if (spent == null || !DuelCardData.isGold(spent.card)) continue;
            DuelCardData spentData = DuelCardCatalog.of(spent.card);
            if (spentData != null && spentData.goldActionExtra > 0)
                timingExtra[side] += spentData.goldActionExtra;
        }
        // 记录本局部署上场的卡牌（含唯一编号），对局获胜时结算「参战胜利次数」
        String uid = CardNbt.uidOf(c);
        if (uid != null) deployedUids[side].add(uid);
        // ---- 触发系：被替换的旧卡离场 → 新卡入场 → 场上「使用其他手牌」类卡触发 ----
        // 阶段18：开关开启时 SUMMON/LEAVE 由 LifecycleHook 派发（LEAVE 已在场位覆盖前派发）；
        // 旧触发系仅回滚模式（开关关闭）保留。
        if (com.laigu.laigu.duel.newcard.NewCardCoreSwitch.enabled())
        {
            if (lifecycleHook != null) lifecycleHook.onSummoned(side, slot);
        }
        else
        {
            if (old != null) triggerLeave(side, old);
            triggerSummon(side, slot);
            triggerOtherUse(side, slot);
        }
        // 得分时机：使用手牌（部署）时
        collectTiming(side, EffectType.USE_HAND_SCORE_EXTRA, 0);
    }

    private void deployConfirm(int side)
    {
        if (phase != Phase.DEPLOY) { lastMsg = "当前不是部署阶段"; return; }
        if (deployDone[side]) return;
        deployDone[side] = true;
        // 部署阶段卡牌对对方隐藏；双方都确认部署后才揭示全部卡牌（再进入抢骰）
        if (deployDone[0] && deployDone[1])
        {
            revealAll();
            enterDraft();
        }
    }

    // ================= 触发系（事件触发：入场/离场/每轮开始/使用手牌/结算后） =================

    /** 新卡入场触发。 */
    private void triggerSummon(int side, int slot)
    {
        FieldCard fc = field[side].get(slot);
        if (fc == null) return;
        DuelCardData d = DuelCardCatalog.of(fc.card);
        if (d == null) return;
        switch (d.effectFor(fc.card))
        {
            case SUMMON_DRAW -> drawCards(side, d.p1For(fc.card));
            case SUMMON_DRAW_IF_LOST_LAST ->
            {
                if (winnerLast == 1 - side) drawCards(side, d.p1For(fc.card));
            }
            case SUMMON_RESTORE_AP ->
                    actionPoints[side] = Math.min(maxAp(round), actionPoints[side] + d.p1For(fc.card));
            default -> {}
        }
        if (DuelCardData.isGold(fc.card))
        {
            if ("shui_jing_bei".equals(d.cardId)) drawCards(side, 2);
            if ("wan_gong_jiao".equals(d.cardId))
            {
                parityOverrideOdd[side] = round % 2 == 1;
                parityOverrideEven[side] = !parityOverrideOdd[side];
            }
            if ("xi_shan_xing_lv_tu".equals(d.cardId))
                for (FieldCard target : field[side]) if (target != null) activateCardDirect(side, target);
        }
    }

    private void activateCardDirect(int side, FieldCard target)
    {
        DuelCardData d = DuelCardCatalog.of(target.card);
        if (d == null || d.activateCap <= 0) { activationFailuresThisRound[side]++; return; }
        friendlyActivationsThisRound[side]++;
        target.activation++;
        resolveActivationTarget(side, target, d);
    }

    /** 卡牌离场触发（被替换弃置 / 消耗移出）。 */
    private void triggerLeave(int side, FieldCard fc)
    {
        DuelCardData d = DuelCardCatalog.of(fc.card);
        if (d != null && d.effectFor(fc.card) == EffectType.LEAVE_DRAW)
        {
            drawCards(side, d.p1For(fc.card));
            if (DuelCardData.isGold(fc.card) && "qing_tong_xian_he".equals(d.cardId))
                timingMult[side] += 4;
        }
    }

    /** 你使用一张手牌（部署）时，场上「使用其他手牌」类卡触发（不含刚下的新卡）。 */
    private void triggerOtherUse(int side, int placedSlot)
    {
        for (int i = 0; i < FIELD_SLOTS; i++)
        {
            if (i == placedSlot) continue;
            FieldCard fc = field[side].get(i);
            if (fc == null) continue;
            DuelCardData d = DuelCardCatalog.of(fc.card);
            if (d != null && d.effectFor(fc.card) == EffectType.OTHER_USE_DRAW) drawCards(side, d.p1For(fc.card));
        }
    }

    /** 每轮开始时场上卡的抽卡触发（在常规抽卡之后）。 */
    private void triggerRoundStart(int side)
    {
        for (int i = 0; i < FIELD_SLOTS; i++)
        {
            FieldCard fc = field[side].get(i);
            if (fc == null) continue;
            DuelCardData d = DuelCardCatalog.of(fc.card);
            if (d == null) continue;
            if (d.effectFor(fc.card) == EffectType.ROUND_START_DRAW) drawCards(side, d.p1For(fc.card));
            else if (d.effectFor(fc.card) == EffectType.ROUND_START_DRAW_STAY_TURNS)
            {
                int amount = Math.min(3, fc.roundsOnField + (DuelCardData.isGold(fc.card) ? 1 : 0));
                drawCards(side, amount);
            }
        }
    }

    /** 本轮结算后按胜负触发的抽卡。 */
    private void triggerRoundEnd(int side)
    {
        for (int i = 0; i < FIELD_SLOTS; i++)
        {
            FieldCard fc = field[side].get(i);
            if (fc == null) continue;
            DuelCardData d = DuelCardCatalog.of(fc.card);
            if (d == null) continue;
            else if (d.effectFor(fc.card) == EffectType.ROUND_END_DRAW_IF_WIN && winnerLast == side) drawCards(side, d.p1For(fc.card));
            else if (d.effectFor(fc.card) == EffectType.ROUND_END_DRAW_IF_LOSE && winnerLast == 1 - side) drawCards(side, d.p1For(fc.card));
        }
    }

    // ================= 抢骰 =================

    private void pickDie(int side, int poolIdx)
    {
        if (phase != Phase.DRAFT) { lastMsg = "当前不是选骰阶段"; return; }
        if (currentPicker != side) { lastMsg = "现在不是你的选骰回合"; return; }
        if (draftTurnRemaining <= 0) { lastMsg = "本轮骰子已选够"; return; }
        if (poolIdx < 0 || poolIdx >= sharedPool.size()) { lastMsg = "无效骰位"; return; }
        if (pool[side].size() >= MAX_DICE_HELD) { lastMsg = "已达骰子上限(" + MAX_DICE_HELD + ")，无法再摸"; return; }
        int grabbedFace = sharedPool.remove(poolIdx);
        pool[side].add(grabbedFace);
        draftTarget[side]--;
        draftTurnRemaining--;
        draftPickSerial[side]++;
        // 阶段16：开关开时抓骰副作用由新核心承担（星月夜补池/重骰/抓取得分）。
        if (draftHook != null && com.laigu.laigu.duel.newcard.NewCardCoreSwitch.enabled())
        {
            int[] fx = draftHook.onGrabbed(side, grabbedFace);
            for (int i = 0; i < fx[0]; i++) sharedPool.add(rnd.nextInt(6) + 1);
            if (fx[1] != 0) timingExtra[side] += fx[1];
        }
        else
        {
            // 星月夜金卡：任一方每次抓骰后向公共池加入一个随机骰。
            for (int owner = 0; owner < 2; owner++)
                for (ItemStack st : fieldCards(owner))
                {
                    DuelCardData dd = DuelCardCatalog.of(st);
                    if (dd != null && "xing_yue_ye".equals(dd.cardId) && DuelCardData.isGold(st))
                    {
                        sharedPool.add(rnd.nextInt(6) + 1);
                        break;
                    }
                }
            // 【重骰】本轮限一次：抓取到点数 x 的骰时，重骰共享池中所有 > x 的骰（由本方场上的「重骰」卡触发）
            int rerollLimit = 0;
            for (ItemStack st : fieldCards(side))
            {
                DuelCardData dd = DuelCardCatalog.of(st);
                if (dd != null && dd.effectFor(st) == EffectType.REROLL_ON_DRAFT)
                    rerollLimit += DuelCardData.isGold(st) ? 2 : 1;
            }
            if (rerollUses < rerollLimit)
            {
                for (int k = 0; k < sharedPool.size(); k++)
                    if (sharedPool.get(k) > grabbedFace) sharedPool.set(k, rnd.nextInt(6) + 1);
                rerollUses++;
                lastMsg = "重骰生效：共享池中大于 " + grabbedFace + " 的骰已重掷";
            }
            // 得分时机：抓取骰子时（场上「抓取」效果 → (6-抓取点数)*p1 额外分）
            collectTiming(side, EffectType.DRAFT_SCORE_EXTRA, grabbedFace);
        }
        // 本次抓取取完 / 共享池取空 / 已持满上限 → 前进/自动结束（满8不再卡住，跳过余下次数）
        if (draftTurnRemaining <= 0 || sharedPool.isEmpty() || pool[side].size() >= MAX_DICE_HELD)
        {
            draftPlanIdx++;
            advanceDraftTurn();
        }
    }

    /** 放弃本次拿骰：跳过当前抓取，直接前进到下一次抓取/布置。 */
    private void skipDraft(int side)
    {
        if (phase != Phase.DRAFT) { lastMsg = "当前不是选骰阶段"; return; }
        if (currentPicker != side) { lastMsg = "现在不是你的选骰回合"; return; }
        if (draftTurnRemaining <= 0) { lastMsg = "本轮骰子已选够"; return; }
        draftTurnRemaining = 0;
        draftPlanIdx++;
        advanceDraftTurn();
        lastMsg = "你放弃了本次拿骰";
    }

    // ================= 抢骰计划（先手 1/2/…/2/1，后手 2/2/…） =================

    /** 双方部署完成 → 依据场上「抓取系」卡牌构建抓取计划并进入抢骰/直接布置。 */
    private void enterDraft()
    {
        buildDraftPlan();
        draftTarget[0] = draftTarget[1] = 0;
        for (int[] e : draftPlan) draftTarget[e[0]] += e[1];
        if (draftPlan.isEmpty())
        {
            phase = Phase.PLACE;
            lastMsg = "双方部署完成，本轮无需抢骰，直接布置。";
        }
        else
        {
            phase = Phase.DRAFT;
            draftPlanIdx = 0;
            advanceDraftTurn();
            lastMsg = "双方部署完成，揭示！开始轮流抢骰。";
        }
    }

    /** 读取双方场上卡的抓取效果（各少/多抓、单方扰动、共享池 +N），生成抓取计划。 */
    private void buildDraftPlan()
    {
        // 阶段16：新核心抢骰钩子优先（开关开 + 钩子返回非 null）。
        if (draftHook != null && com.laigu.laigu.duel.newcard.NewCardCoreSwitch.enabled())
        {
            int[][] plan = draftHook.buildPlan(firstPicker);
            if (plan != null)
            {
                draftFirstSizes = plan[0];
                draftSecondSizes = plan[1];
                rebuildPlan();
                return;
            }
        }
        int fMod = 0, sMod = 0, poolExtra = 0; // 双方同效（抓取次数）
        int[] selfMod = new int[2];             // 单方多抓（DRAFT_SELF_TURNS_UP）
        int[] oppMod = new int[2];              // 单方压制对方（DRAFT_OPP_TURNS_DOWN）
        int[] grabMod = new int[2];             // 单方每次抓取 +N 颗（DRAFT_SELF_GRAB_UP）
        for (int s = 0; s < 2; s++)
        {
            for (ItemStack stack : fieldCards(s))
            {
                DuelCardData d = DuelCardCatalog.of(stack);
                if (d == null) continue;
                switch (d.effectFor(stack))
                {
                    case DRAFT_TURNS_DOWN: fMod -= d.p1For(stack); sMod -= d.p1For(stack); break;
                    case DRAFT_TURNS_UP:   fMod += d.p1For(stack); sMod += d.p1For(stack); break;
                    case DRAFT_POOL_UP:    poolExtra += d.p1For(stack); break;
                    case DRAFT_SELF_TURNS_UP: selfMod[s] += d.p1For(stack); break;
                    case DRAFT_OPP_TURNS_DOWN: oppMod[1 - s] -= d.p1For(stack); break;
                    case DRAFT_SELF_GRAB_UP: grabMod[s] += d.p1For(stack); break;
                    default: break;
                }
            }
        }
        // 金卡独立主效果的动态加成在建计划时读取，避免继续沿用普通卡自动翻倍。
        for (int s = 0; s < 2; s++)
            for (FieldCard fc : field[s])
                if (fc != null && DuelCardData.isGold(fc.card)
                        && ("t_xing_bo_hua".equals(DuelCardCatalog.of(fc.card).cardId)
                        || "zeng_hou_yi_bian_zhong".equals(DuelCardCatalog.of(fc.card).cardId))) selfMod[s]++;
        // 系统掷出的骰子 +poolExtra 颗（本轮补入共享池）
        for (int i = 0; i < poolExtra; i++) sharedPool.add(rnd.nextInt(6) + 1);
        // 先手侧：双方同效 + 先手单方 + 对方压制的先手侧分量
        int fTurns = Math.max(0, FIRST_DRAFT_TURNS + fMod + selfMod[firstPicker] + oppMod[firstPicker]);
        int sTurns = Math.max(0, SECOND_DRAFT_TURNS + sMod + selfMod[1 - firstPicker] + oppMod[1 - firstPicker]);
        draftFirstSizes = bumpSizes(firstSizes(fTurns), grabMod[firstPicker]);
        draftSecondSizes = bumpSizes(secondSizes(sTurns), grabMod[1 - firstPicker]);
        rebuildPlan();
    }

    /** 每次抓取颗数整体 +add（每次抓取 +N 颗）。 */
    private static int[] bumpSizes(int[] sizes, int add)
    {
        if (add <= 0) return sizes;
        int[] out = new int[sizes.length];
        for (int i = 0; i < sizes.length; i++) out[i] = Math.max(1, sizes[i] + add);
        return out;
    }

    private void rebuildPlan()
    {
        draftPlan.clear();
        int i = 0, j = 0;
        while (i < draftFirstSizes.length || j < draftSecondSizes.length)
        {
            if (i < draftFirstSizes.length) { draftPlan.add(new int[]{firstPicker, draftFirstSizes[i]}); i++; }
            if (j < draftSecondSizes.length) { draftPlan.add(new int[]{1 - firstPicker, draftSecondSizes[j]}); j++; }
        }
    }

    /** 前进到下一次抓取；骰池空或计划耗尽 → 进入布置。 */
    private void advanceDraftTurn()
    {
        while (draftPlanIdx < draftPlan.size() && !sharedPool.isEmpty())
        {
            int[] e = draftPlan.get(draftPlanIdx);
            if (pool[e[0]].size() >= MAX_DICE_HELD)
            {
                draftTarget[e[0]] -= e[1]; // 已持满，跳过该次抓取
                draftPlanIdx++;
                continue;
            }
            currentPicker = e[0];
            draftTurnRemaining = e[1];
            return;
        }
        draftTurnRemaining = 0;
        phase = Phase.PLACE;
        lastMsg = "选骰结束，开始布置骰子（未布置的将继承到下轮）。";
    }

    /** 先手每次抓取数：首尾各 1、中间 2（1/2/…/2/1）。 */
    private static int[] firstSizes(int n)
    {
        if (n <= 0) return new int[0];
        int[] a = new int[n];
        for (int k = 0; k < n; k++) a[k] = (k == 0 || k == n - 1) ? 1 : 2;
        return a;
    }

    /** 后手每次抓取数：全部 2（2/2/…）。 */
    private static int[] secondSizes(int n)
    {
        if (n <= 0) return new int[0];
        int[] a = new int[n];
        for (int k = 0; k < n; k++) a[k] = 2;
        return a;
    }

    // ================= 布置 =================

    private void placeDie(int side, int poolIdx, int slot)
    {
        if (phase != Phase.PLACE) { lastMsg = "当前不是布置阶段"; return; }
        if (poolIdx < 0 || poolIdx >= pool[side].size()) { lastMsg = "无效骰位"; return; }
        if (slot < 0 || slot >= FIELD_SLOTS) { lastMsg = "无效场上槽位"; return; }
        FieldCard fc = field[side].get(slot);
        if (fc == null) { lastMsg = "该槽位没有卡"; return; }
        if (!fc.canAddDie()) { lastMsg = "该卡最多 2 颗骰"; return; }
        int v = pool[side].remove(poolIdx);
        fc.dice.add(v);
        // 得分时机：布置骰子时
        collectTiming(side, EffectType.PLACE_SCORE_EXTRA, 0);
    }

    /** 确认布置前，把已放到某张卡上的第 dieIdx 颗骰取下，退回骰池（继承到下轮）。 */
    private void placeTakeDie(int side, int slot, int dieIdx)
    {
        if (phase != Phase.PLACE) { lastMsg = "当前不是布置阶段"; return; }
        if (placeDone[side]) { lastMsg = "你已确认布置"; return; }
        if (slot < 0 || slot >= FIELD_SLOTS) { lastMsg = "无效场上槽位"; return; }
        FieldCard fc = field[side].get(slot);
        if (fc == null) { lastMsg = "该槽位没有卡"; return; }
        if (dieIdx < 0 || dieIdx >= fc.dice.size()) { lastMsg = "无效骰位"; return; }
        int v = fc.dice.remove(dieIdx);
        pool[side].add(v);
    }

    private void placeConfirm(int side)
    {
        if (phase != Phase.PLACE) { lastMsg = "当前不是布置阶段"; return; }
        placeDone[side] = true;
        // 未布置的骰子留在骰池，继承到下一回合
        if (placeDone[0] && placeDone[1]) settleRound();
    }

    /** 双方都确认布置后：全部卡牌翻开（计分动画在揭示后的牌面上进行）。伏击卡保持背面直到计分。 */
    private void revealAll()
    {
        for (int s = 0; s < 2; s++)
            for (int i = 0; i < FIELD_SLOTS; i++)
            {
                FieldCard fc = field[s].get(i);
                if (fc == null) continue;
                DuelCardData d = DuelCardCatalog.of(fc.card);
                if (d != null && d.effectFor(fc.card) == EffectType.FUJI) continue;   // 伏击卡计分才揭晓
                fc.faceDown = false;
            }
    }

    // ================= 结算 =================

    /**
     * 阶段十二生产切换钩子：旧引擎照常计算并保留其副作用（消耗标记/动画步骤/激活链），
     * 钩子可用新核心结果替换本侧的 (base, mult, extra)；返回 null 表示本回合回退旧引擎。
     * lastTotal 由调用方合并时机加成后重算，钩子的 total() 不参与。
     */
    public interface RoundSettlementHook
    {
        ScoreEngine.ScoreResult settleSide(int side, ScoreEngine.ScoreResult legacyResult);
    }

    private RoundSettlementHook roundSettlementHook;

    /** 安装新卡核心结算钩子（传 null 卸载）。由 DuelTableBlockEntity 在开局/读档时安装。 */
    public void setRoundSettlementHook(RoundSettlementHook hook) { this.roundSettlementHook = hook; }

    /**
     * 阶段16抢骰钩子：开关开时抓取计划与抓骰副作用由新核心承担，
     * 旧 DuelGame 只保留状态存储与回合时序。由 DuelTableBlockEntity 安装。
     */
    public interface DraftHook
    {
        /** 构建抓取计划；返回 {先手每次颗数[], 后手每次颗数[]}，null = 交回旧路径。 */
        int[][] buildPlan(int firstPicker);

        /** 抓骰副作用；返回 {向共享池补入的随机骰颗数, 抓取得分额外分}。 */
        int[] onGrabbed(int side, int face);
    }

    private DraftHook draftHook;

    /** 安装新卡核心抢骰钩子（传 null 卸载）。 */
    public void setDraftHook(DraftHook hook) { this.draftHook = hook; }

    /**
     * 阶段18：时机事件生命周期钩子——开关开启时 SUMMON/LEAVE/ROUND_START 由新核心派发，
     * 旧触发系（triggerSummon/triggerLeave/triggerOtherUse/triggerRoundStart）仅回滚模式保留。
     * 实现方负责影子同步、事件派发与实态增量回写（抽牌/回复行动力/激活进度）。
     */
    public interface LifecycleHook
    {
        void onSummoned(int side, int slot);
        void onLeave(int side, int slot);
        void onRoundStart();
    }

    private LifecycleHook lifecycleHook;

    /** 安装新卡核心生命周期钩子（传 null 卸载）。 */
    public void setLifecycleHook(LifecycleHook hook) { this.lifecycleHook = hook; }

    /** 对齐清单（2026-09-03）：伏击结算由新核心卡类承担（奖励/骰子复制/无效化/破坏），实态桥由实现方回写。 */
    public interface AmbushHook
    {
        void onAmbushResolved(int side, int slot, boolean success);
    }

    private AmbushHook ambushHook;

    public void setAmbushHook(AmbushHook hook) { this.ambushHook = hook; }

    private void settleRound()
    {
        // 计分动画：先结算本轮先手，再结算后手；每侧卡牌自左向右依次结算
        lastSteps = new ArrayList<>();
        // 伏击卡在计分时揭晓并结算（成功取共享骰/失败加额外分）
        resolveAmbush();
        // 【激活x】结算时触发：场上「激活左侧」卡按充能语义——充能x每骰触发一次；充能N至少N骰触发一次。
        // 特殊：浑天(顺子→激活右侧N次)、溪山(每次激活+额外,金卡)、海错(金卡右侧也+1)。
        for (int s = 0; s < 2; s++)
            for (FieldCard fc : new ArrayList<>(field[s]))
            {
                if (fc == null) continue;
                DuelCardData dd = DuelCardCatalog.of(fc.card);
                if (dd == null) continue;
                boolean gold = DuelCardData.isGold(fc.card);
                if (dd.effectFor(fc.card) == EffectType.ACTIVATE_LEFT)
                {
                    int n = fc.diceCount();
                    int count = (dd.chargeFor(fc.card) == -1) ? n : ((dd.chargeFor(fc.card) > 0 && n >= dd.chargeFor(fc.card)) ? 1 : 0);
                    if (gold && "xi_shan_xing_lv_tu".equals(dd.cardId)) count *= 2;
                    int slot = field[s].indexOf(fc);
                    if ("hai_cuo_tu".equals(dd.cardId))
                    {
                        int beforeFailures = activationFailuresThisRound[s];
                        for (int k = 0; k < count * (gold ? 2 : 1); k++)
                            activateAllCards(s);
                        if (gold && activationFailuresThisRound[s] > beforeFailures)
                            timingExtra[s] += 10;
                    }
                    else for (int k = 0; k < count; k++)
                    {
                        fireActivate(s, fc);
                        // 激活触发者跳跃动画
                        if (slot >= 0) lastSteps.add(new ScoreEngine.ScoreStep(s, slot, 1, timingBase[s], timingMult[s], timingExtra[s], 1)); // 激活特效
                    }
                }
                // 浑天焕章：金卡且牌型为顺子时，激活右侧卡牌 N 次。
                if (dd.activateRightOnStraight > 0 && ScoreEngine.Conds.isStraight(fieldDice(s), gold && dd.goldGapStraight))
                {
                    int slot = field[s].indexOf(fc);
                    int n = dd.activateRightOnStraight;
                    if (gold && dd.goldRightStraightActivations > 0) n = dd.goldRightStraightActivations;
                    for (int k = 0; k < n; k++)
                    {
                        fireActivateRight(s, fc);
                        if (slot >= 0) lastSteps.add(new ScoreEngine.ScoreStep(s, slot, 1, timingBase[s], timingMult[s], timingExtra[s], 1)); // 激活特效
                    }
                }
                // 朝代联动：触发右侧 x 次（x=我方场上最多朝代数）
                if (dd.activateRightByDynastyMax)
                {
                    int x = maxDynastyOnField(s);
                    if (gold) x *= 2;
                    int slot = field[s].indexOf(fc);
                    for (int k = 0; k < x; k++)
                    {
                        fireActivateRight(s, fc);
                        if (slot >= 0) lastSteps.add(new ScoreEngine.ScoreStep(s, slot, 1, timingBase[s], timingMult[s], timingExtra[s], 1)); // 激活特效
                    }
                }
            }
        int[] order = {firstPicker, 1 - firstPicker};
        for (int s : order)
        {
            // 回合结束得分时机（场上「回合结束」效果的额外分）
            collectTiming(s, EffectType.ROUND_END_SCORE_EXTRA, 0);
            ScoreEngine.ScoreResult engineResult = ScoreEngine.computeWithSteps(this, s, lastSteps);
            ScoreEngine.ScoreResult hookResult = roundSettlementHook == null ? null
                    : roundSettlementHook.settleSide(s, engineResult);
            ScoreEngine.ScoreResult r = hookResult != null ? hookResult : engineResult;
            lastBase[s] = r.base() + timingBase[s];
            lastMult[s] = r.mult() + timingMult[s];
            // 非结算得分时机（抓取/布置/回合开始/使用手牌/回合结束）+ 激活奖励 贡献并入本轮总分
            lastExtra[s] = r.extra() + timingExtra[s];
            // 胜负判定/局内累计总分必须用真实值：钩子命中（新核心接管）时其结果已含全部时机贡献，
            // 上式的旧 timing 叠加仅服务于展示链路（用户拍板接受其偏高），不得进入胜负口径，
            // 否则激活奖励双算会直接改判输赢。钩子未命中（回退旧引擎）时维持原公式。
            lastTotal[s] = hookResult != null
                    ? r.base() * r.mult() + r.extra()
                    : lastBase[s] * lastMult[s] + lastExtra[s];
            // 把时机/词条贡献并入计分步骤末尾（供右侧积分栏/动画展示；slot=-1 表示非卡牌步骤）
            if (timingBase[s] != 0 || timingMult[s] != 0 || timingExtra[s] != 0)
            {
                lastSteps.add(new ScoreEngine.ScoreStep(s, -1, 1, lastBase[s], lastMult[s], lastExtra[s]));
            }
        }
        if (lastTotal[0] > lastTotal[1]) { wins[0]++; winnerLast = 0; }
        else if (lastTotal[1] > lastTotal[0]) { wins[1]++; winnerLast = 1; }
        else winnerLast = -1; // 平局：双方不加胜场，继续下一轮
        addLog(0, "第 " + round + " 轮 A：基础" + lastBase[0] + " × 倍率" + lastMult[0] + " + 额外" + lastExtra[0] + " = " + lastTotal[0]);
        addLog(1, "第 " + round + " 轮 B：基础" + lastBase[1] + " × 倍率" + lastMult[1] + " + 额外" + lastExtra[1] + " = " + lastTotal[1]);
        // 大比分播报
        addLog(-1, "当前大比分 " + wins[0] + ":" + wins[1] + (winnerLast >= 0
                ? "（" + (winnerLast == 0 ? "A" : "B") + " 领先）" : "（本轮平局）"));
        totalScore[0] += lastTotal[0];
        totalScore[1] += lastTotal[1];

        // 结算后触发（本轮胜负相关的抽卡）
        for (int s = 0; s < 2; s++) triggerRoundEnd(s);

        // 伏击焕章：飞天金卡标记的对位卡在回合结算后摧毁。
        for (int s = 0; s < 2; s++)
            for (int i = 0; i < FIELD_SLOTS; i++)
            {
                FieldCard fc = field[s].get(i);
                if (fc != null && fc.destroyAtRoundEnd) removeFieldCard(s, i, fc, false);
            }

        // 【消耗】结算后移出对局（槽位空置不填补）；消耗离场也触发离场抽卡。
        // 固有消耗卡（【消耗】词条）本轮结束必然移出；条件消耗卡仅在效果实际触发后由 consumed 标记移出。
        for (int s = 0; s < 2; s++)
        {
            for (int i = 0; i < FIELD_SLOTS; i++)
            {
                FieldCard fc = field[s].get(i);
                if (fc != null && (fc.consumed || fc.intrinsicConsume))
                {
                    removeFieldCard(s, i, fc, false);
                }
            }
        }

        // 大比分抢二胜：领先 2 分即胜；封顶 5 轮，未拉开则按净胜分判胜负（平局 =-1）
        if (Math.abs(wins[0] - wins[1]) >= 2)
        {
            phase = Phase.FINISHED;   // winnerLast 已由本轮胜者设置
        }
        else if (round >= MAX_ROUNDS)
        {
            if (totalScore[0] > totalScore[1]) winnerLast = 0;
            else if (totalScore[1] > totalScore[0]) winnerLast = 1;
            else winnerLast = -1;   // 同净胜分：平局终局
            phase = Phase.FINISHED;
        }
        else
        {
            phase = Phase.ROUND_END;
        }
    }

    private void resolveActivationTarget(int side, FieldCard target, DuelCardData d)
    {
        if (target.activation < d.activateCap) return;
        // 阶段18：开关开启且新核心结算钩子在位时，激活奖励由新核心结算承担（carry 通道并入新结果），
        // 旧 applyReward 不再写入 timing 计数器，防止最终展示分双算；进度重置保留。
        if (com.laigu.laigu.duel.newcard.NewCardCoreSwitch.enabled() && roundSettlementHook != null)
        {
            target.activation = 0;
            return;
        }
        EffectType reward = DuelCardData.isGold(target.card) ? d.goldActivateReward : d.activateReward;
        int value = DuelCardData.isGold(target.card) ? d.goldActivateP1 : d.activateP1;
        if (reward != null) applyReward(reward, value, target, side);
        if (DuelCardData.isGold(target.card))
            for (EffectDefinition e : triggerEffects(d, EffectTrigger.GOLD_ACTIVATION))
                applyReward(e.type(), e.p1(), target, side);
        target.activation = 0;
    }

    /** 卡牌离场统一处理：非布置阶段移出/回到手中时，其上骰子直接移除（不参与基础分/牌型）。 */
    private void removeFieldCard(int side, int slot, FieldCard fc, boolean toHand)
    {
        // 先按离场卡自身的最终变体执行离场效果，再清除场上骰子和槽位状态。
        // 阶段18：开关开启时 LEAVE 由 LifecycleHook 派发（离场卡仍在场位上，影子可定位）。
        if (lifecycleHook != null && com.laigu.laigu.duel.newcard.NewCardCoreSwitch.enabled())
            lifecycleHook.onLeave(side, slot);
        else
            triggerLeave(side, fc);
        fc.dice.clear();
        if (toHand) hand[side].add(fc.card);
        field[side].set(slot, null);
    }

    /** 伏击结算：计分时揭晓伏击卡。对位有卡 → 成功（数值按「对位卡牌骰数」缩放，反制高强度对位）；无卡 → 失败（额外分）。 */
    private void resolveAmbush()
    {
        for (int s = 0; s < 2; s++)
        {
            List<FieldCard> opp = field[1 - s];
            for (int i = 0; i < FIELD_SLOTS; i++)
            {
                FieldCard fc = field[s].get(i);
                if (fc == null) continue;
                DuelCardData d = DuelCardCatalog.of(fc.card);
                if (d == null || d.effectFor(fc.card) != EffectType.FUJI) continue;
                fc.faceDown = false;   // 计分时揭晓
                FieldCard oppFc = i < opp.size() ? opp.get(i) : null;
                boolean gold = DuelCardData.isGold(fc.card);
                if (ambushHook != null && com.laigu.laigu.duel.newcard.NewCardCoreSwitch.enabled())
                {
                    // 对齐清单：伏击语义（含金卡焕章）由新核心卡类实现，旧硬编码仅回滚模式保留。
                    ambushHook.onAmbushResolved(s, i, oppFc != null);
                    lastSteps.add(new ScoreEngine.ScoreStep(s, i, 1, timingBase[s], timingMult[s], timingExtra[s], 2));
                    addLog(s, "伏击·" + fc.card.getHoverName().getString() + "：" + (oppFc != null ? "成功" : "失败"));
                    continue;
                }
                if (oppFc != null)
                {
                    if (gold && "dun_huang_fei_tian".equals(d.cardId))
                     {
                         timingExtra[s] += 60;
                         int min = Integer.MAX_VALUE;
                         for (FieldCard candidate : opp) if (candidate != null) min = Math.min(min, candidate.activeDice().size());
                         if (oppFc.activeDice().size() == min) oppFc.destroyAtRoundEnd = true;
                     }
                     boolean handled = false;
                     if (gold && "bai_hua_tu_juan".equals(d.cardId))
                     {
                         for (int die : oppFc.activeDice()) if (fc.canAddDie()) fc.dice.add(die);
                         if (fc.activeDice().size() > 4)
                         {
                             timingBase[s] += 5;
                             timingMult[s] += 5;
                             timingExtra[s] += 5;
                         }
                         handled = true;
                     }
                    // 睡莲：成功时将对位卡牌前 N 颗骰无效化（金卡×2）；金卡还收回被无效化骰子的基础分
                    if (d.fuJiInvalidate > 0)
                    {
                        int n = Math.min(d.fuJiInvalidate * (gold ? 2 : 1), oppFc.dice.size());
                        oppFc.invalidatedCount = n;
                        if (gold)
                        {
                            int sum = 0;
                            for (int k = 0; k < n; k++) sum += oppFc.dice.get(k);
                            timingBase[s] += sum;
                            lastMsg = "伏击成功：对位前 " + n + " 骰无效化并收回其基础分";
                        }
                        else lastMsg = "伏击成功：对位前 " + n + " 骰无效化";
                        handled = true;
                    }
                    // 百花：成功时镜像——普卡对位获基础分时我也获一半基础分；金卡焕章 对位获倍率时我也获一半倍率
                    else if (d.fuJiMirrorBase || (gold && d.fuJiMirrorMult))
                    {
                        int oppBase = 0;
                        for (int v : oppFc.activeDice()) oppBase += v;
                        if (d.fuJiMirrorBase) timingBase[s] += oppBase / 2;
                        if (gold && d.fuJiMirrorMult) timingMult[s] += Math.max(1, oppFc.activeDice().size()) / 2;
                        lastMsg = "伏击成功：镜像（对位基础/倍率）";
                        handled = true;
                    }
                    // 通用成功：按对位卡牌骰数缩放配置效果
                    if (!handled)
                    {
                        List<EffectDefinition> successEffects = triggerEffects(d, EffectTrigger.AMBUSH_SUCCESS);
                        if (!successEffects.isEmpty())
                        {
                            int oppDice = oppFc.activeDice().size();
                            for (EffectDefinition reward : successEffects)
                            {
                                int p = reward.p1();
                                if (DuelCardData.isBaseReward(reward.type())) timingBase[s] += p * oppDice;
                                else if (DuelCardData.isMultReward(reward.type())) timingMult[s] += p * oppDice;
                                else timingExtra[s] += p * oppDice;
                                lastSteps.add(new ScoreEngine.ScoreStep(s, i, 1, timingBase[s], timingMult[s], timingExtra[s], 2));
                            }
                            lastMsg = "伏击成功：按对位 " + oppDice + " 颗有效骰加成";
                        }
                        else if (d.fuJiSuccReward != null)
                        {
                            int oppDice = oppFc.activeDice().size();
                            int p = d.fuJiSuccP1;
                            if (DuelCardData.isBaseReward(d.fuJiSuccReward)) timingBase[s] += p * oppDice;
                            else if (DuelCardData.isMultReward(d.fuJiSuccReward)) timingMult[s] += p * oppDice;
                            else timingExtra[s] += p * oppDice;
                            lastMsg = "伏击成功：按对位 " + oppDice + " 颗骰加成";
                        }
                        else lastMsg = "伏击成功（无额外收益）";
                    }
                }
                else
                {
                    List<EffectDefinition> failEffects = triggerEffects(d, EffectTrigger.AMBUSH_FAIL);
                    if (!failEffects.isEmpty())
                    {
                        for (EffectDefinition reward : failEffects)
                        {
                            int p = reward.p1();
                            if (DuelCardData.isBaseReward(reward.type())) timingBase[s] += p;
                            else if (DuelCardData.isMultReward(reward.type())) timingMult[s] += p;
                            else timingExtra[s] += p;
                            lastSteps.add(new ScoreEngine.ScoreStep(s, i, 1, timingBase[s], timingMult[s], timingExtra[s], 2));
                        }
                        lastMsg = "伏击失败：通用触发效果生效";
                    }
                    else if (d.fuJiFailReward != null && !gold)
                    {
                        int p = d.fuJiFailP1;
                        timingExtra[s] += p;
                        lastMsg = "伏击失败：+" + p + " 额外分";
                    }
                }
                // 触发卡牌跳跃动画（该槽位卡跳一下，右侧积分栏随 timing 累计更新）伏击特效=2
                lastSteps.add(new ScoreEngine.ScoreStep(s, i, 1, timingBase[s], timingMult[s], timingExtra[s], 2));
                addLog(s, "伏击·" + fc.card.getHoverName().getString() + "：" + (oppFc != null ? "成功" : "失败"));
            }
        }
    }

    /** 【激活x】：激活左侧相邻卡牌 +1；达到其 cap 后结算奖励并清零。无左侧/左侧非目标则忽略。 */
    private void fireActivate(int side, FieldCard src)
    {
        activateCard(side, src, -1);
    }

    private void activateAllCards(int side)
    {
        for (FieldCard target : new ArrayList<>(field[side]))
            if (target != null) activateCardDirect(side, target);
    }

    /** 【激活x·右侧】：激活右侧相邻卡牌 +1（浑天顺子/海错焕章用）。 */
    private void fireActivateRight(int side, FieldCard src)
    {
        activateCard(side, src, +1);
    }

    /** 通用：激活 src 的相邻(offset=-1 左 / +1 右)卡牌；达到其 cap 结算奖励；若该目标为金卡且带「达成激活左」焕章，再递归激活更左。 */
    private void activateCard(int side, FieldCard src, int offset)
    {
        int slot = field[side].indexOf(src);
        int tgt = slot + offset;
        if (tgt < 0 || tgt >= FIELD_SLOTS) return;
        FieldCard target = field[side].get(tgt);
        if (target == null) return;
        DuelCardData ld = DuelCardCatalog.of(target.card);
        if (ld == null || ld.activateCap <= 0)
        {
            activationFailuresThisRound[side]++;
            return;
        }
        friendlyActivationsThisRound[side]++;
        target.activation++;
        if (target.activation >= ld.activateCap)
        {
            List<EffectDefinition> activationEffects = triggerEffects(ld, EffectTrigger.ACTIVATION);
            if (DuelCardData.isGold(target.card) && ld.goldActivateReward != null)
                activationEffects = List.of(new EffectDefinition(ld.goldActivateReward, ld.goldActivateP1, 0));
            for (EffectDefinition reward : activationEffects)
            {
                applyReward(reward.type(),
                        DuelCardData.isGold(target.card)
                                ? DuelCardData.goldValueFor(reward.type(), reward.p1(), 0, target.card)
                                : reward.p1(),
                        target, side);
                int tSlot = field[side].indexOf(target);
                if (tSlot >= 0) lastSteps.add(new ScoreEngine.ScoreStep(side, tSlot, 1,
                        timingBase[side], timingMult[side], timingExtra[side], 1));
            }
            // 兼容旧字段：没有归一化触发器时仍执行原激活奖励。
            if (activationEffects.isEmpty() && ld.activateReward != null)
            {
                applyReward(ld.activateReward,
                        (DuelCardData.isGold(target.card) && ld.goldActivateReward != null ? ld.goldActivateP1 : ld.activateP1),
                        target, side);
                int tSlot = field[side].indexOf(target);
                if (tSlot >= 0) lastSteps.add(new ScoreEngine.ScoreStep(side, tSlot, 1,
                        timingBase[side], timingMult[side], timingExtra[side], 1));
            }
            if (DuelCardData.isGold(target.card))
            {
                for (EffectDefinition reward : triggerEffects(ld, EffectTrigger.GOLD_ACTIVATION))
                {
                    applyReward(reward.type(),
                            DuelCardData.goldValueFor(reward.type(), reward.p1(), 0, target.card),
                            target, side);
                    int tSlot = field[side].indexOf(target);
                    if (tSlot >= 0) lastSteps.add(new ScoreEngine.ScoreStep(side, tSlot, 1,
                            timingBase[side], timingMult[side], timingExtra[side], 1));
                }
            }
            lastMsg = "激活目标达成（" + ld.activateCap + "）";
            addLog(side, "激活达成·" + target.card.getHoverName().getString());
            // 永固杯焕章：激活达成时，激活左侧卡牌
            if (ld.activateLeftOnReach) activateCard(side, target, -1);
            target.activation = 0;
        }
    }

    /** 取指定触发器的效果列表；返回不可变空列表表示未配置。 */
    private List<EffectDefinition> triggerEffects(DuelCardData data, EffectTrigger trigger)
    {
        return data.effectsFor(trigger);
    }

    /** 按效果参数登记表计算金卡焕章值；触发阈值/次数等 RAW 参数保持不变。 */
    private int scaleTriggerValue(EffectDefinition effect, ItemStack stack)
    {
        return effect.p1();
    }

    /** 把激活/时机奖励写入本侧本轮累计分量；效果定义统一复用同一执行入口。 */
    private void applyReward(EffectType reward, int p, FieldCard target, int side)
    {
        int base = 1;
        if (reward == EffectType.PER_DIE_MULT || reward == EffectType.PER_DIE_BASE || reward == EffectType.PER_DIE_EXTRA)
        {
            base = Math.max(1, target.diceCount());
        }
        if (reward == EffectType.ANY_FRIENDLY_ACTIVATE_EXTRA)
            timingExtra[side] += p;
        else if (reward == EffectType.COPY_CURRENT_BASE_TO_EXTRA)
            timingExtra[side] += timingBase[side];
        else if (reward == EffectType.OPP_EMPTY_CARD_MULT)
        {
            int count = 0;
            for (FieldCard fc : field[1 - side]) if (fc != null && fc.activeDice().isEmpty()) count++;
            timingMult[side] += p * count;
        }
        else if (reward == EffectType.FLAT_MULT) timingMult[side] += p;
        else if (DuelCardData.isBaseReward(reward)) timingBase[side] += p * base;
        else if (DuelCardData.isMultReward(reward)) timingMult[side] += p * base;
        else timingExtra[side] += p * base;
    }

    private int maxDynastyOnField(int side)
    {
        Map<String, Integer> cnt = new HashMap<>();
        for (int i = 0; i < FIELD_SLOTS; i++)
        {
            FieldCard fc = field[side].get(i);
            if (fc == null) continue;
            String dyn = DuelCardData.dynastyOf(fc.card);
            if (dyn != null) cnt.merge(dyn, 1, Integer::sum);
        }
        return cnt.size();
    }

    /** 非结算得分时机：统计本侧场上带该时机效果的卡，累加额外分到 timingExtra（结算时并入本轮总分）。 */
    private void collectTiming(int side, EffectType type, int dieFace)
    {
        int amt = 0;
        for (ItemStack st : fieldCards(side))
        {
            DuelCardData d = DuelCardCatalog.of(st);
            if (d == null || d.effectFor(st) != type) continue;
            if (type == EffectType.DRAFT_SCORE_EXTRA) amt += d.p1For(st) * (6 - dieFace);
            else amt += d.p1For(st);
        }
        if (amt != 0) timingExtra[side] += amt;
    }

    private void nextRound(int side)
    {
        if (phase != Phase.ROUND_END) { lastMsg = "当前不是回合结算"; return; }
        if (round >= MAX_ROUNDS) { phase = Phase.FINISHED; return; }   // 第 5 轮已由结算判定终局，防御
        if (roundEndDone[side]) return;
        roundEndDone[side] = true;
        if (roundEndDone[0] && roundEndDone[1])
        {
            round++;
            startRound();
        }
    }

    public void forfeit(int side)
    {
        winnerLast = 1 - side;
        wins[1 - side] = wins[side] + 2;   // 直接终局，比分差保持 2（与抢二胜自洽）
        started = false;
        phase = Phase.FINISHED;
        lastMsg = "你认输了，对局结束。"; // 发给主动认输的一方；获胜方提示见方块实体的 duel_forfeit_win
    }

    // ================= 访问器 =================

    /** 阶段18：新核心生命周期桥接——把新核心事件产生的抽牌写入实态（仅供 LifecycleHook 实现调用）。 */
    public void applyNewCoreDraw(int side, int amount)
    {
        drawCards(side, amount);
    }

    /** 阶段18：新核心生命周期桥接——回复行动力（不超本轮上限；对齐旧 SUMMON_RESTORE_AP 语义）。 */
    /** 本轮已消耗行动力（观星金焕章「消耗行动力时+15」取值）。 */
    public int actionPointsSpentThisRound(int side) { return actionPointsSpentThisRound[side]; }

    public void applyNewCoreActionPoints(int side, int amount)
    {
        actionPoints[side] = Math.min(maxAp(round), actionPoints[side] + amount);
    }

    public List<FieldCard> field(int side) { return field[side]; }
    public List<ItemStack> hand(int side) { return hand[side]; }
    public List<Integer> pool(int side) { return pool[side]; }
    public List<Integer> sharedPool() { return sharedPool; }
    public Phase phase() { return phase; }
    public int round() { return round; }
    public int wins(int side) { return wins[side]; }
    public int lastBase(int side) { return lastBase[side]; }
    public int lastMult(int side) { return lastMult[side]; }
    public int lastExtra(int side) { return lastExtra[side]; }
    public int lastTotal(int side) { return lastTotal[side]; }
    public int winnerLast() { return winnerLast; }
    public int currentPicker() { return currentPicker; }
    public int firstPicker() { return firstPicker; }
    public int actionPoints(int side) { return actionPoints[side]; }
    public int overflowDrawMultiplier(int side) { return overflowDrawMult[side]; }
    public int friendlyActivationsThisRound(int side) { return friendlyActivationsThisRound[side]; }
    public int activationFailuresThisRound(int side) { return activationFailuresThisRound[side]; }
    public boolean deployDone(int side) { return deployDone[side]; }
    public boolean placeDone(int side) { return placeDone[side]; }
    public boolean roundEndDone(int side) { return roundEndDone[side]; }

    /** 本轮还需抓取的骰数（计划制，逐颗递减）；0 = 抓取计划已尽。 */
    public int pickTarget(int side)
    {
        return draftTarget[side];
    }

    /** 某方本轮抓取计划（每次几颗，用于界面展示；计划重建前为空）。 */
    public int[] draftSizes(int side)
    {
        int n = 0;
        for (int[] e : draftPlan) if (e[0] == side) n++;
        int[] a = new int[n];
        int k = 0;
        for (int[] e : draftPlan) if (e[0] == side) a[k++] = e[1];
        return a;
    }

    public int occupiedCount(int side)
    {
        int c = 0;
        for (int i = 0; i < FIELD_SLOTS; i++) if (field[side].get(i) != null) c++;
        return c;
    }

    /** 放置在场卡上的全部骰子（基础分与骰型判定用，只算放置的）。 */
    public List<Integer> fieldDice(int side)
    {
        List<Integer> all = new ArrayList<>();
        for (int i = 0; i < FIELD_SLOTS; i++)
        {
            FieldCard fc = field[side].get(i);
            if (fc != null) all.addAll(fc.activeDice());
        }
        return all;
    }

    /** 我方未抽的卡组牌数（卡组剩余；资源系效果用）。 */
    public int deckCount(int side)
    {
        return deck[side].size();
    }

    /** 我方场上卡（朝代/职业/金质计数范围：只算场上）。 */
    public List<ItemStack> fieldCards(int side)
    {
        List<ItemStack> all = new ArrayList<>();
        for (int i = 0; i < FIELD_SLOTS; i++)
        {
            FieldCard fc = field[side].get(i);
            if (fc != null) all.add(fc.card);
        }
        return all;
    }

    /** 本局部署上场过的卡牌唯一编号集合（对局获胜时结算「参战胜利次数」用）。 */
    public Set<String> deployedUids(int side)
    {
        return deployedUids[side];
    }

    // ================= 序列化：客户端视角 =================

    /** 生成某方视角的完整状态（手牌只发给持有者；对方暗置卡隐藏）。 */
    public CompoundTag serializeState(int side)
    {
        return serializeState(side, false);
    }

    /**
     * 生成某方视角的完整状态。spectate=true（观战）时双方透明：
     * 对方部署中的暗置卡与布置阶段的骰子全部揭示。
     */
    public CompoundTag serializeState(int side, boolean spectate)
    {
        CompoundTag t = new CompoundTag();
        t.putInt("round", round);
        t.putInt("phase", phase.ordinal());
        t.putInt("mySide", side);
        t.putInt("wins0", wins[0]);
        t.putInt("wins1", wins[1]);
        t.putInt("winnerLast", winnerLast);
        t.putInt("currentPicker", currentPicker);
        t.putInt("pickTarget", pickTarget(side));
        t.putIntArray("myDraft", draftSizes(side));
        t.putIntArray("oppDraft", draftSizes(1 - side));
        t.putInt("draftTurnLeft", draftTurnRemaining);
        t.putBoolean("started", started);
        t.putBoolean("darkMode", darkMode);
        t.putBoolean("creative", creative);
        t.putBoolean("spectate", spectate);
        t.putInt("myAp", actionPoints[side]);
        t.putInt("myApMax", maxAp(round));
        t.putBoolean("myDeployDone", deployDone[side]);
        t.putBoolean("oppDeployDone", deployDone[1 - side]);
        t.putBoolean("myPlaceDone", placeDone[side]);
        t.putBoolean("oppPlaceDone", placeDone[1 - side]);
        t.putBoolean("myRoundEndDone", roundEndDone[side]);
        t.putBoolean("oppRoundEndDone", roundEndDone[1 - side]);
        t.putInt("deckCount", deck[side].size());   // 牌库剩余（抽卡动画/计数展示用）

        // 手牌（本人，无上限）
        ListTag handTag = new ListTag();
        for (ItemStack s : hand[side]) handTag.add(s.save(new CompoundTag()));
        t.put("hand", handTag);
        // 我方场上（全量，自己总能看到自己的卡）
        t.put("myField", fieldNbt(side, true, true));
        // 对方场上：部署阶段暗置隐藏（双方确认部署揭示后才可见）；布置骰子阶段不发送对方骰子；
        // 观战者透视（spectate=true）——暗置卡与对方骰子全部揭示，双方操作透明。
        t.put("oppField", fieldNbt(1 - side, spectate, spectate || phase != Phase.PLACE));
        // 骰池（选骰公开，双方都可见；含继承骰）
        t.putIntArray("myPool", toIntArray(pool[side]));
        t.putIntArray("oppPool", toIntArray(pool[1 - side]));
        t.putIntArray("shared", toIntArray(sharedPool));
        // 结算拆解：始终发送上一轮的 base/mult/extra（计分区展示；无上一轮则为 0）
        t.putInt("lastBase", lastBase[side]);
        t.putInt("lastMult", lastMult[side]);
        t.putInt("lastExtra", lastExtra[side]);
        t.putInt("myLastTotal", lastTotal[side]);
        t.putInt("oppLastTotal", lastTotal[1 - side]);
        t.putInt("oppBase", lastBase[1 - side]);
        t.putInt("oppMult", lastMult[1 - side]);
        t.putInt("oppExtra", lastExtra[1 - side]);
        // 抓骰/部署等非结算时机的实时累计分，供客户端即时显示。
        t.putInt("timingBase", timingBase[side]);
        t.putInt("timingMult", timingMult[side]);
        t.putInt("timingExtra", timingExtra[side]);
        t.putInt("oppTimingBase", timingBase[1 - side]);
        t.putInt("oppTimingMult", timingMult[1 - side]);
        t.putInt("oppTimingExtra", timingExtra[1 - side]);
         t.putInt("overflowMult", overflowDrawMult[side]);
         t.putBoolean("parityOverrideOdd", parityOverrideOdd[side]);
         t.putBoolean("parityOverrideEven", parityOverrideEven[side]);
        t.putInt("draftPickSerial", draftPickSerial[side]);
        t.putInt("oppDraftPickSerial", draftPickSerial[1 - side]);
        // 计分动画步骤（仅回合结算 / 结束时下发）
        if (phase == Phase.ROUND_END || phase == Phase.FINISHED)
        {
            ListTag st = new ListTag();
            for (ScoreEngine.ScoreStep step : lastSteps)
            {
                CompoundTag c = new CompoundTag();
                c.putInt("side", step.side);
                c.putInt("slot", step.slot);
                c.putInt("ticks", step.ticks);
                c.putInt("base", step.base);
                c.putInt("mult", step.mult);
                c.putInt("extra", step.extra);
                c.putInt("kind", step.kind);
                st.add(c);
            }
            t.put("scoreSteps", st);
        }
        // 战斗播报（客户端左侧滚动展示）
        ListTag bl = new ListTag();
        for (String s : battleLog) bl.add(StringTag.valueOf(s));
        t.put("battleLog", bl);
        return t;
    }

    private ListTag fieldNbt(int side, boolean reveal, boolean sendDice)
    {
        ListTag ft = new ListTag();
        for (int i = 0; i < FIELD_SLOTS; i++)
        {
            FieldCard fc = field[side].get(i);
            if (fc == null) continue;
            CompoundTag c = new CompoundTag();
            c.putInt("slot", i);
            boolean visible = reveal || !fc.faceDown;
            if (visible)
            {
                c.put("card", fc.card.save(new CompoundTag()));
                if (sendDice) c.putIntArray("dice", toIntArray(fc.dice));
                c.putBoolean("lasted", fc.lastedLastRound);
                c.putInt("rounds", fc.roundsOnField);
                c.putBoolean("consumed", fc.consumed);
                c.putInt("activation", fc.activation);          // 激活进度（UI 进度条）
                c.putBoolean("intrinsicConsume", fc.intrinsicConsume);
                c.putInt("invalidatedCount", fc.invalidatedCount);
                 c.putBoolean("locked", fc.locked);
                 c.putBoolean("poZhenAlwaysSuccess", fc.poZhenAlwaysSuccess);
                 c.putInt("persistentBaseBonus", fc.persistentBaseBonus);
            }
            else
            {
                c.putBoolean("hidden", true);
            }
            ft.add(c);
        }
        return ft;
    }

    // ================= 序列化：完整持久化（方块 NBT） =================

    public CompoundTag toNbt()
    {
        CompoundTag t = new CompoundTag();
        t.putInt("round", round);
        t.putInt("phase", phase.ordinal());
        t.putInt("currentPicker", currentPicker);
        t.putInt("firstPicker", firstPicker);
        t.putInt("winnerLast", winnerLast);
        t.putBoolean("started", started);
        t.putBoolean("darkMode", darkMode);
        t.putBoolean("creative", creative);
        t.putIntArray("wins", wins);
        t.putIntArray("totalScore", totalScore);
        t.putIntArray("actionPoints", actionPoints);
        t.putIntArray("draftTarget", draftTarget);
        t.putIntArray("draftF", draftFirstSizes);
        t.putIntArray("draftS", draftSecondSizes);
        t.putInt("draftPlanIdx", draftPlanIdx);
        t.putInt("draftTurnRemaining", draftTurnRemaining);
        t.putIntArray("lastBase", lastBase);
        t.putIntArray("lastMult", lastMult);
        t.putIntArray("lastExtra", lastExtra);
        t.putIntArray("lastTotal", lastTotal);
        t.putIntArray("timingBase", timingBase);
        t.putIntArray("timingMult", timingMult);
        t.putIntArray("timingExtra", timingExtra);
        t.putIntArray("overflowDrawMult", overflowDrawMult);
        t.putIntArray("shared", toIntArray(sharedPool));
        for (int s = 0; s < 2; s++)
        {
            CompoundTag st = new CompoundTag();
            st.put("deckOriginal", itemsNbt(deckOriginal[s]));
            st.put("deck", itemsNbt(deck[s]));
            st.put("hand", itemsNbt(hand[s]));
            st.put("discard", itemsNbt(discard[s]));
            st.putIntArray("pool", toIntArray(pool[s]));
            st.putBoolean("deployDone", deployDone[s]);
            st.putBoolean("placeDone", placeDone[s]);
            st.putBoolean("roundEndDone", roundEndDone[s]);
            ListTag ft = new ListTag();
            for (int i = 0; i < FIELD_SLOTS; i++)
            {
                FieldCard fc = field[s].get(i);
                if (fc == null) continue;
                CompoundTag c = new CompoundTag();
                c.putInt("slot", i);
                c.put("card", fc.card.save(new CompoundTag()));
                c.putIntArray("dice", toIntArray(fc.dice));
                c.putBoolean("faceDown", fc.faceDown);
                c.putBoolean("lasted", fc.lastedLastRound);
                c.putInt("rounds", fc.roundsOnField);
                c.putBoolean("consumed", fc.consumed);
                c.putInt("activation", fc.activation);
                c.putBoolean("intrinsicConsume", fc.intrinsicConsume);
                c.putInt("invalidatedCount", fc.invalidatedCount);
                 c.putBoolean("locked", fc.locked);
                 c.putBoolean("poZhenAlwaysSuccess", fc.poZhenAlwaysSuccess);
                 c.putInt("persistentBaseBonus", fc.persistentBaseBonus);
                ft.add(c);
            }
            st.put("field", ft);
            t.put("side" + s, st);
        }
        // 战斗播报持久化
        ListTag bl = new ListTag();
        for (String s : battleLog) bl.add(StringTag.valueOf(s));
        t.put("battleLog", bl);
        return t;
    }

    private static List<String> logsNbt(ListTag tag)
    {
        List<String> out = new ArrayList<>();
        for (int i = 0; i < tag.size(); i++) out.add(tag.getString(i));
        return out;
    }

    public static DuelGame fromNbt(CompoundTag t, RandomSource rnd)
    {
        DuelGame g = new DuelGame(rnd);
        g.round = t.getInt("round");
        g.phase = Phase.values()[t.getInt("phase")];
        g.currentPicker = t.getInt("currentPicker");
        g.firstPicker = t.getInt("firstPicker");
        g.winnerLast = t.getInt("winnerLast");
        g.started = t.getBoolean("started");
        g.darkMode = t.getBoolean("darkMode");
        g.creative = t.getBoolean("creative");
        copyInts(t.getIntArray("wins"), g.wins);
        copyInts(t.getIntArray("totalScore"), g.totalScore);   // 旧档缺键 → 空数组，安全 no-op
        copyInts(t.getIntArray("actionPoints"), g.actionPoints);
        copyInts(t.getIntArray("draftTarget"), g.draftTarget);
        g.draftFirstSizes = t.getIntArray("draftF");
        g.draftSecondSizes = t.getIntArray("draftS");
        g.draftPlanIdx = t.getInt("draftPlanIdx");
        g.draftTurnRemaining = t.getInt("draftTurnRemaining");
        g.rebuildPlan();
        copyInts(t.getIntArray("lastBase"), g.lastBase);
        copyInts(t.getIntArray("lastMult"), g.lastMult);
        copyInts(t.getIntArray("lastExtra"), g.lastExtra);
        copyInts(t.getIntArray("lastTotal"), g.lastTotal);
        copyInts(t.getIntArray("timingBase"), g.timingBase);
        copyInts(t.getIntArray("timingMult"), g.timingMult);
        copyInts(t.getIntArray("timingExtra"), g.timingExtra);
        copyInts(t.getIntArray("overflowDrawMult"), g.overflowDrawMult);
        for (int v : t.getIntArray("shared")) g.sharedPool.add(v);
        for (int s = 0; s < 2; s++)
        {
            CompoundTag st = t.getCompound("side" + s);
            g.deckOriginal[s] = stacksNbt(st.getList("deckOriginal", Tag.TAG_COMPOUND));
            g.deck[s] = stacksNbt(st.getList("deck", Tag.TAG_COMPOUND));
            g.hand[s] = stacksNbt(st.getList("hand", Tag.TAG_COMPOUND));
            g.discard[s] = stacksNbt(st.getList("discard", Tag.TAG_COMPOUND));
            for (int v : st.getIntArray("pool")) g.pool[s].add(v);
            g.deployDone[s] = st.getBoolean("deployDone");
            g.placeDone[s] = st.getBoolean("placeDone");
            g.roundEndDone[s] = st.getBoolean("roundEndDone");
            ListTag ft = st.getList("field", Tag.TAG_COMPOUND);
            for (int i = 0; i < ft.size(); i++)
            {
                CompoundTag c = ft.getCompound(i);
                int slot = c.getInt("slot");
                FieldCard fc = new FieldCard(ItemStack.of(c.getCompound("card")));
                for (int v : c.getIntArray("dice")) fc.dice.add(v);
                fc.faceDown = c.getBoolean("faceDown");
                fc.lastedLastRound = c.getBoolean("lasted");
                fc.roundsOnField = c.getInt("rounds");
                fc.consumed = c.getBoolean("consumed");
                fc.activation = c.getInt("activation");
                fc.intrinsicConsume = c.getBoolean("intrinsicConsume");
                fc.invalidatedCount = c.getInt("invalidatedCount");
                 fc.locked = c.getBoolean("locked");
                 fc.poZhenAlwaysSuccess = c.getBoolean("poZhenAlwaysSuccess");
                 fc.persistentBaseBonus = c.getInt("persistentBaseBonus");
                g.field[s].set(slot, fc);
            }
        }
        g.battleLog.addAll(logsNbt(t.getList("battleLog", Tag.TAG_STRING)));
        return g;
    }

    // ---- NBT 小工具 ----

    private static ListTag itemsNbt(List<ItemStack> list)
    {
        ListTag tag = new ListTag();
        for (ItemStack s : list) tag.add(s.save(new CompoundTag()));
        return tag;
    }

    private static List<ItemStack> stacksNbt(ListTag tag)
    {
        List<ItemStack> list = new ArrayList<>();
        for (int i = 0; i < tag.size(); i++) list.add(ItemStack.of(tag.getCompound(i)));
        return list;
    }

    private static int[] toIntArray(List<Integer> list)
    {
        int[] arr = new int[list.size()];
        for (int i = 0; i < list.size(); i++) arr[i] = list.get(i);
        return arr;
    }

    private static void copyInts(int[] src, int[] dst)
    {
        for (int i = 0; i < Math.min(src.length, dst.length); i++) dst[i] = src[i];
    }
}
