package com.laigu.laigu.duel;

import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * 结算引擎。只做一件事：在【结算阶段】按 最终分 = 基础分 × 倍率 + 额外分 计算一方分数。
 * 所有效果在此统一判定，部署/揭示/选骰/布置阶段不结算任何效果。
 *
 * 规则（V2 按玩家反馈定稿）：
 *  - 基础分 = Σ你【放置在场卡上】的骰面 + Σ基础分加成（下限 0）；未布置的继承骰不计分
 *  - 倍率 = 1 + Σ倍率加成（下限 1）
 *  - 额外分 = Σ额外分加成（不吃倍率）
 *  - 顺子 = 骰面互不相同且连续，要求 ≥3 颗骰（严格：全部骰子连续）
 *  - 朝代/职业/金质计数范围 = 【场上卡】（手牌不参与）
 *  - 朝代取文物元数据（CardInfo），金质取物品稀有度后缀
 *  - 空槽位不算相邻；两侧条件中缺侧视为满足
 */
public final class ScoreEngine
{
    /** 结算结果（供 GUI 展示拆解）。 */
    public record ScoreResult(int base, int mult, int extra, int total) {}

    /** 计分动画步骤：某张卡（或基础汇总）结算后，该侧累计到 (base, mult, extra)。 */
    public static class ScoreStep
    {
        /** 0/1 侧；slot=-1 表示非卡牌步骤（基础骰面汇总 / 翻倍）。 */
        public final int side;
        public final int slot;
        /** 卡牌跳动次数（每颗骰/每单位加成才跳几次）。 */
        public final int ticks;
        public final int base, mult, extra;
        /** 特效类型：0=普通 1=激活 2=伏击 3=破阵。 */
        public final int kind;

        public ScoreStep(int side, int slot, int ticks, int base, int mult, int extra)
        {
            this(side, slot, ticks, base, mult, extra, 0);
        }

        public ScoreStep(int side, int slot, int ticks, int base, int mult, int extra, int kind)
        {
            this.side = side; this.slot = slot; this.ticks = ticks;
            this.base = base; this.mult = mult; this.extra = extra;
            this.kind = kind;
        }
    }

    private ScoreEngine() {}

    /** 计算某方本轮最终分；顺带把带【消耗】的场卡标记为 consumed。 */
    public static ScoreResult compute(DuelGame g, int side)
    {
        return run(g, side, null);
    }

    /** 计算最终分，并收集计分动画步骤（steps 传 null 则不收集）。 */
    public static ScoreResult computeWithSteps(DuelGame g, int side, List<ScoreStep> steps)
    {
        return run(g, side, steps);
    }

    private static ScoreResult run(DuelGame g, int side, List<ScoreStep> steps)
    {
        List<FieldCard> field = g.field(side);
        List<ItemStack> owned = g.fieldCards(side);
        List<Integer> dice = g.fieldDice(side);

        int base = 0;
        for (FieldCard card : field)
            if (card != null) for (int v : card.activeDice()) base += v;
        // 基础分只统计每张卡的有效骰；被睡莲无效化的骰不再参与任何基础结算。
        int mult = 1;
        int extra = 0;
        mult += g.overflowDrawMultiplier(side);
        if (steps != null) steps.add(new ScoreStep(side, -1, 1, base, 1, 0));
        boolean baseDouble = false;
        boolean extraDouble = false;

        // 【破阵】固有词条：本槽位骰面和 > 对手同槽位骰面和 → 把对手该槽位卡牌的效果削弱。
        // 金卡基础数值×2：金破阵卡削弱 100%，普破阵卡削弱 50%。
        List<FieldCard> oppField = g.field(1 - side);
        double[] halvePct = new double[field.size()];
        for (int i = 0; i < field.size(); i++)
        {
            FieldCard my = field.get(i), op = i < oppField.size() ? oppField.get(i) : null;
            if (my == null || op == null) continue;
            DuelCardData opd = DuelCardCatalog.of(op.card);
            if (opd != null && opd.effectFor(op.card) == EffectType.PO_ZHEN_HALVE
                    && (op.poZhenAlwaysSuccess || sumDice(op) > sumDice(my)) )
            {
                if (DuelCardData.isGold(op.card) && "yuan_wang_bei".equals(opd.cardId))
                {
                    if (i > 0) halvePct[i - 1] = Math.max(halvePct[i - 1], 0.5);
                    if (i + 1 < halvePct.length) halvePct[i + 1] = Math.max(halvePct[i + 1], 0.5);
                }
                List<ResolutionModifier> modifiers = opd.modifiersFor(EffectTrigger.PO_ZHEN, op.card);
                for (ResolutionModifier modifier : modifiers)
                    if (modifier.type() == ResolutionModifier.ModifierType.REDUCE_OPPONENT_CONTRIBUTION)
                        halvePct[i] = modifier.p1() / 100.0;
            }
        }

        for (int i = 0; i < field.size(); i++)
        {
            FieldCard fc = field.get(i);
            if (fc == null) continue;
            DuelCardData d = DuelCardCatalog.of(fc.card);
            if (d == null) continue;
            List<Integer> cardDice = fc.activeDice();
            int n = cardDice.size();
            // 【充能】语义：charge==-1 = 充能x（每有一颗骰触发一次 → 贡献按骰数缩放）；
            //             charge>=1 = 充能N（门槛：至少 N 颗骰才触发一次，不缩放）。
            if (d.chargeFor(fc.card) == -1) { if (n < 1) continue; }
            else if (d.chargeFor(fc.card) > 0) { if (n < d.chargeFor(fc.card)) continue; }
            int prevBase = base, prevMult = mult, prevExtra = extra;
            int ticks = 1;
            base += fc.persistentBaseBonus;
            extra += fc.roundExtraBonus;
            mult += fc.roundMultBonus;

            switch (d.effectFor(fc.card))
            {
                // ---- 攻·炽：吃骰输出 ----
                case PER_DIE_EXTRA:            extra += d.p1For(fc.card) * n; ticks = n; break;
                case PER_DIE_BASE:             base  += d.p1For(fc.card) * n; ticks = n; break;
                case DICE_GE2_EXTRA:           if (n >= 2) extra += d.p1For(fc.card); break;
                case ALL_HIGH_EXTRA:           if (Conds.allHigh(cardDice)) extra += d.p1For(fc.card); break;
                case STRAIGHT_PER_DIE_MULT_CONSUME:
                    if (Conds.isStraight(cardDice)) { mult += d.p1For(fc.card) * n; fc.consumed = true; }
                    break;

                // ---- 守·衡：稳定保底 ----
                case FLAT_EXTRA:               extra += d.p1For(fc.card); break;
                case ZERO_DICE_EXTRA:          if (n == 0) extra += d.p1For(fc.card); break;
                case HAS_ONE_EXTRA:            if (Conds.hasOne(cardDice)) extra += d.p1For(fc.card); break;
                case ALL_LOW_EXTRA:            if (Conds.allLow(cardDice)) extra += d.p1For(fc.card); break;
                case LAST_ROUND_EXTRA:         if (fc.lastedLastRound) extra += d.p1For(fc.card); break;

                // ---- 谋·策：条件 / 位置收益 ----
                case STRAIGHT_MULT:            if (Conds.isStraight(cardDice)) mult += d.p1For(fc.card); break;
                case ALL_ODD_MULT:             if (Conds.allOdd(cardDice)) mult += d.p1For(fc.card); break;
                case NEIGHBOR_MULT:            if (hasAdjacent(field, i)) mult += d.p1For(fc.card); break;
                case ISOLATED_MULT_EXTRA:
                    if (bothSidesNotDynasty(field, i, DuelCardData.dynastyOf(fc.card))) { mult += d.p1For(fc.card); extra += d.p2For(fc.card); }
                    break;
                case ALL_EVEN_EXTRA:           if (Conds.allEven(cardDice)) extra += d.p1For(fc.card); break;
                case PER_DIE_MULT:             mult += d.p1For(fc.card) * n; ticks = n; break;

                // ---- 鼎·盛：基础 / 倍率引擎 ----
                case DYN_CNT_BASE:             base  += d.p2For(fc.card) * countDynasty(owned, d.targetDynastyFor(fc.card)); ticks = countDynasty(owned, d.targetDynastyFor(fc.card)); break;
                case CLASS_CNT_MULT:           mult  += d.p2For(fc.card) * countClass(owned, d.targetClassFor(fc.card)); ticks = countClass(owned, d.targetClassFor(fc.card)); break;
                case BASE_DOUBLE_CONSUME:      baseDouble = true; fc.consumed = true; break;
                case CARD_CNT_BASE:            base  += d.p1For(fc.card) * owned.size(); ticks = owned.size(); break;
                case ALL_HIGH_MULT:            if (Conds.allHigh(cardDice)) mult += d.p1For(fc.card); break;
                case CLASS_CNT_BASE:           base  += d.p2For(fc.card) * countClass(owned, d.targetClassFor(fc.card)); ticks = countClass(owned, d.targetClassFor(fc.card)); break;
                case GOLD_CNT_MULT:            mult  += d.p1For(fc.card) * countGold(owned); ticks = countGold(owned); break;

                // ---- 攻·炽 第2批：吃骰质变（本卡单骰/双骰判定） ----
                case DIE_FIRST_BONUS:
                    if (n >= 1) extra += d.p1For(fc.card);
                    if (n >= 2) extra += d.p2For(fc.card);
                    break;
                case SAME_FACE_EXP_EXTRA:
                    if (n > 0 && fc.activeDice().stream().distinct().count() == 1)
                    {
                        int factor = 1;
                        for (int k = 0; k < n; k++)
                        {
                            extra += d.p1For(fc.card) * factor;
                            factor *= d.p2For(fc.card);
                        }
                    }
                    break;
                case TOGGLE_PARITY_EXTRA:
                    boolean wantOdd = g.round() % 2 == 1;
                    for (int v : fc.activeDice())
                        if ((v % 2 == 1) == wantOdd) extra += d.p1For(fc.card);
                    break;
                case ODD_DIE_EXTRA:
                {
                    int cnt = 0;
                    for (int v : cardDice) if (v % 2 == 1) { extra += d.p1For(fc.card); cnt++; }
                    ticks = cnt;
                    break;
                }
                case EVEN_DIE_EXTRA:
                {
                    int cnt = 0;
                    for (int v : cardDice) if (v % 2 == 0) { extra += d.p1For(fc.card); cnt++; }
                    ticks = cnt;
                    break;
                }
                case DIE_GE4_EXTRA:
                {
                    int cnt = 0;
                    for (int v : fc.dice) if (v >= 4) { extra += d.p1For(fc.card); cnt++; }
                    ticks = cnt;
                    break;
                }
                case DIE_LE3_EXTRA:
                {
                    int cnt = 0;
                    for (int v : fc.dice) if (v <= 3) { extra += d.p1For(fc.card); cnt++; }
                    ticks = cnt;
                    break;
                }
                case DIE_SUM_GE_EXTRA:
                    if (sumDice(fc) >= d.p1For(fc.card)) extra += d.p2For(fc.card);
                    break;
                case DIE_SUM_GE_MULT:
                    if (sumDice(fc) >= d.p1For(fc.card)) mult += d.p2For(fc.card);
                    break;
                case DICE_GE1_EXTRA:
                    if (n >= 1) extra += d.p1For(fc.card);
                    break;
                case DIE_SUM_ODD_EXTRA:
                    if (sumDice(fc) % 2 == 1) extra += d.p1For(fc.card);
                    break;
                case SAME_FACE_MULT:
                    if (n == 2 && fc.dice.get(0).equals(fc.dice.get(1))) mult += d.p1For(fc.card);
                    break;

                // ---- 守·衡 第2批：对位·顺势·持久 ----
                case OPP_MORE_DICE_EXTRA:
                    if (g.fieldDice(1 - side).size() > dice.size()) extra += d.p1For(fc.card);
                    break;
                case OPP_FIELD_FULL_EXTRA:
                    if (g.occupiedCount(1 - side) >= DuelGame.FIELD_SLOTS) extra += d.p1For(fc.card);
                    break;
                case LOSE_LAST_EXTRA:
                    if (g.winnerLast() == 1 - side) extra += d.p1For(fc.card);
                    break;
                case DRAW_LAST_EXTRA:
                    if (g.winnerLast() == -1 && g.round() > 1) extra += d.p1For(fc.card);
                    break;
                case WIN_LAST_MULT:
                    if (g.winnerLast() == side) mult += d.p1For(fc.card);
                    break;
                case BEHIND_WINS_EXTRA:
                    if (g.wins(1 - side) > g.wins(side)) extra += d.p1For(fc.card);
                    break;
                case HAND_EMPTY_EXTRA:
                    if (g.hand(side).isEmpty()) extra += d.p1For(fc.card);
                    break;
                case LASTED_2_EXTRA:
                    if (fc.roundsOnField >= 2) extra += d.p1For(fc.card);
                    break;
                case ROUND_GE2_EXTRA:
                    if (g.round() >= 2) extra += d.p1For(fc.card);
                    break;

                // ---- 谋·策 第2批：全盘骰型 + 槽位阵型 ----
                case TWO_PAIR_MULT:
                    if (Conds.twoPair(dice)) mult += d.p1For(fc.card);
                    break;
                case FULL_HOUSE_MULT:
                    if (Conds.fullHouse(dice)) mult += d.p1For(fc.card);
                    break;
                case ALL_SAME_MULT:
                    if (Conds.allSame(dice)) mult += d.p1For(fc.card);
                    break;
                case HAS_SIX_EXTRA:
                    if (Conds.hasSix(dice)) extra += d.p1For(fc.card);
                    break;
                case SUM_RANGE_EXTRA:
                    if (Conds.sumRange(dice)) extra += d.p1For(fc.card);
                    break;
                case CONSEC_NEAR_EXTRA:
                    if (Conds.consecNear(dice)) extra += d.p1For(fc.card);
                    break;
                case EDGE_EXTRA:
                    if (i == 0 || i == DuelGame.FIELD_SLOTS - 1) extra += d.p1For(fc.card);
                    break;
                case CENTER_MULT:
                    // 中间位置：奇数槽取正中间一格，偶数槽取中间两格
                    if (DuelGame.FIELD_SLOTS % 2 == 0
                            ? (i == DuelGame.FIELD_SLOTS / 2 - 1 || i == DuelGame.FIELD_SLOTS / 2)
                            : i == DuelGame.FIELD_SLOTS / 2) mult += d.p1For(fc.card);
                    break;
                case ADJ_SAME_CLASS_MULT:
                    if (hasAdjacentClass(field, i, d.cls)) mult += d.p1For(fc.card);
                    break;
                case ADJ_DIFF_CLASS_EXTRA:
                    if (hasAdjacentDiffClass(field, i, d.cls)) extra += d.p1For(fc.card);
                    break;

                // ---- 鼎·盛 第2批：资源引擎 + 金质引擎 + 朝代 ----
                case HAND_CNT_MULT:
                    mult += d.p1For(fc.card) * g.hand(side).size();
                    break;
                case POOL_CNT_BASE:
                    base += d.p1For(fc.card) * g.pool(side).size();
                    break;
                case POOL_CNT_MULT:
                    mult += d.p1For(fc.card) * g.pool(side).size();
                    break;
                case DECK_CNT_BASE:
                    base += d.p1For(fc.card) * g.deckCount(side);
                    break;
                case SHARED_POOL_EXTRA:
                    extra += d.p1For(fc.card) * g.sharedPool().size();
                    break;
                case SHARED_POOL_SUM_EXTRA:
                    for (int v : g.sharedPool()) extra += v;
                    break;
                case DICE_GT_MULTI_REWARD:
                    if (n > d.p1For(fc.card))
                    {
                        base += d.p2For(fc.card);
                        mult += d.p2For(fc.card);
                        extra += d.p2For(fc.card);
                    }
                    break;
                case ANY_FRIENDLY_ACTIVATE_EXTRA:
                    extra += g.friendlyActivationsThisRound(side) * d.p1For(fc.card);
                    break;
                case ACTIVATION_FAILED_EXTRA:
                    extra += g.activationFailuresThisRound(side) * d.p1For(fc.card);
                    break;
                case COPY_CURRENT_BASE_TO_EXTRA:
                    // 该效果只在激活奖励窗口执行，常规结算不重复触发。
                    break;
                case GOLD_CNT_BASE:
                    base += d.p1For(fc.card) * countGold(owned);
                    break;
                case GOLD_DIE_MULT:
                    mult += d.p1For(fc.card) * countGoldDice(field);
                    break;
                case GOLD_DYN_MULT:
                    if (countGold(owned) > 0) mult += d.p1For(fc.card);
                    break;
                case GOLD_EXTRA:
                    extra += d.p1For(fc.card) * countGold(owned);
                    break;
                case ADJ_SAME_DYN_EXTRA:
                    if (hasAdjacentDynasty(field, i, DuelCardData.dynastyOf(fc.card))) extra += d.p1For(fc.card);
                    break;

                // ---- 献祭：消耗系 ----
                case CONSUME_PER_DIE_MULT:
                    mult += d.p1For(fc.card) * n;
                    fc.consumed = true;
                    break;
                case CONSUME_PER_DIE_BASE:
                    base += d.p1For(fc.card) * n;
                    fc.consumed = true;
                    break;
                case CONSUME_BASE_FLAT:
                    base += d.p1For(fc.card);
                    fc.consumed = true;
                    break;
                case CONSUME_EXTRA_DOUBLE:
                    extraDouble = true;
                    fc.consumed = true;
                    break;
                case CONSUME_HAND_MULT:
                    mult += d.p1For(fc.card) * g.hand(side).size();
                    fc.consumed = true;
                    break;
                case CONSUME_OPP_DICE_EXTRA:
                    extra += d.p1For(fc.card) * g.fieldDice(1 - side).size();
                    fc.consumed = true;
                    break;

                // ---- 节奏：轮次/先手/空卡 ----
                case ROUND_EVEN_MULT:
                    if (g.round() % 2 == 0) mult += d.p1For(fc.card);
                    break;
                case FIRST_PICK_MULT:
                    if (g.firstPicker() == side) mult += d.p1For(fc.card);
                    break;
                case NO_DICE_MULT:
                    if (n == 0) mult += d.p1For(fc.card);
                    break;
            }

            // 【充能x】（charge==-1）：每有一颗骰触发一次 → 本卡贡献按骰数缩放。
            // 门槛型 充能N（charge>=1）：已在顶部判定至少 N 颗骰，触发一次，不做缩放。
            // 注意在 base/extra 翻倍（baseDouble/extraDouble）之前缩放，缩放只作用于本卡贡献。
            if (d.chargeFor(fc.card) == -1 && n > 1)
            {
                base  = prevBase  + (base  - prevBase)  * n;
                mult  = prevMult  + (mult  - prevMult)  * n;
                extra = prevExtra + (extra - prevExtra) * n;
            }

            // 【破阵】本卡被削弱（50% 或金卡100%），只作用于本卡的本轮贡献
            if (halvePct[i] > 0)
            {
                double keep = 1.0 - halvePct[i];
                base  = prevBase  + (int) Math.floor((base  - prevBase)  * keep);
                mult  = prevMult  + (int) Math.floor((mult  - prevMult)  * keep);
                extra = prevExtra + (int) Math.floor((extra - prevExtra) * keep);
            }

            // 【破阵】发动者（本卡）触发破阵条件时跳一下（即使自身无得分贡献）
            if (d.effectFor(fc.card) == EffectType.PO_ZHEN_HALVE && steps != null)
            {
                FieldCard po = i < oppField.size() ? oppField.get(i) : null;
                if (po != null && (fc.poZhenAlwaysSuccess || sumDice(fc) > sumDice(po)))
                {
                    if (DuelCardData.isGold(fc.card) && "yue_wang_gou_jian_jian".equals(d.cardId))
                        fc.poZhenAlwaysSuccess = true;
                    if (DuelCardData.isGold(fc.card) && "ya_chang_niu_zun".equals(d.cardId))
                        po.locked = true;
                    List<ResolutionModifier> modifiers = d.modifiersFor(EffectTrigger.PO_ZHEN, fc.card);
                    if (modifiers.stream().anyMatch(m -> m.type() == ResolutionModifier.ModifierType.REDUCE_OPPONENT_CONTRIBUTION))
                    {
                        steps.add(new ScoreStep(side, i, 1, base, mult, extra, 3));
                        g.addLog(side, "破阵·" + fc.card.getHoverName().getString() + "：削弱本槽位效果");
                    }
                }
            }
            // 只有目录明确标记为结算型的金卡焕章才在此处执行；入场/离场/激活/伏击焕章不走这里。
            if (DuelCardData.isGold(fc.card) && d.goldSettlementEffect)
            {
                if (d.goldEffect == EffectType.SHARED_POOL_SUM_EXTRA || d.goldEffect == EffectType.SHARED_POOL_SUM_EXTRA)
                    for (int die : g.sharedPool()) extra += die;
            }
            if (steps != null && (base != prevBase || mult != prevMult || extra != prevExtra))
            {
                steps.add(new ScoreStep(side, i, Math.max(1, ticks), base, mult, extra));
            }
        }

        if (baseDouble) base *= 2;
        if (extraDouble) extra *= 2;
        if (steps != null && (baseDouble || extraDouble))
        {
            steps.add(new ScoreStep(side, -1, 1, base, mult, extra));
        }
        base = Math.max(0, base);
        mult = Math.max(1, mult);
        int total = base * mult + extra;
        return new ScoreResult(base, mult, extra, total);
    }

    // ---- 条件判定 ----

    private static boolean hasAdjacent(List<FieldCard> field, int i)
    {
        boolean l = i - 1 >= 0 && field.get(i - 1) != null;
        boolean r = i + 1 < field.size() && field.get(i + 1) != null;
        return l || r;
    }

    /** 两侧均非本卡朝代；缺侧视为满足。 */
    private static boolean bothSidesNotDynasty(List<FieldCard> field, int i, String dynasty)
    {
        boolean leftOk = true;
        boolean rightOk = true;
        if (i - 1 >= 0 && field.get(i - 1) != null)
        {
            leftOk = !dynasty.equals(DuelCardData.dynastyOf(field.get(i - 1).card));
        }
        if (i + 1 < field.size() && field.get(i + 1) != null)
        {
            rightOk = !dynasty.equals(DuelCardData.dynastyOf(field.get(i + 1).card));
        }
        return leftOk && rightOk;
    }

    // ---- 计数（只算场上卡） ----

    private static int countDynasty(List<ItemStack> owned, String dynasty)
    {
        int c = 0;
        for (ItemStack s : owned)
        {
            if (dynasty != null && dynasty.equals(DuelCardData.dynastyOf(s))) c++;
        }
        return c;
    }

    private static int countClass(List<ItemStack> owned, CardClass cls)
    {
        int c = 0;
        for (ItemStack s : owned)
        {
            DuelCardData d = DuelCardCatalog.of(s);
            if (d != null && d.cls == cls) c++;
        }
        return c;
    }

    private static int countGold(List<ItemStack> owned)
    {
        int c = 0;
        for (ItemStack s : owned)
        {
            if (DuelCardData.isGold(s)) c++;
        }
        return c;
    }

    // ---- 第 2 批：新增辅助判定 ----

    /** 本卡骰面和。 */
    private static int sumDice(FieldCard fc)
    {
        int s = 0;
        for (int v : fc.activeDice()) s += v;
        return s;
    }

    /** 相邻(左/右)有同职业卡。 */
    private static boolean hasAdjacentClass(List<FieldCard> field, int i, CardClass cls)
    {
        boolean l = i - 1 >= 0 && field.get(i - 1) != null
                && DuelCardCatalog.of(field.get(i - 1).card) != null
                && DuelCardCatalog.of(field.get(i - 1).card).cls == cls;
        boolean r = i + 1 < field.size() && field.get(i + 1) != null
                && DuelCardCatalog.of(field.get(i + 1).card) != null
                && DuelCardCatalog.of(field.get(i + 1).card).cls == cls;
        return l || r;
    }

    /** 相邻(左/右)有不同职业卡。 */
    private static boolean hasAdjacentDiffClass(List<FieldCard> field, int i, CardClass cls)
    {
        boolean l = i - 1 >= 0 && field.get(i - 1) != null
                && DuelCardCatalog.of(field.get(i - 1).card) != null
                && DuelCardCatalog.of(field.get(i - 1).card).cls != cls;
        boolean r = i + 1 < field.size() && field.get(i + 1) != null
                && DuelCardCatalog.of(field.get(i + 1).card) != null
                && DuelCardCatalog.of(field.get(i + 1).card).cls != cls;
        return l || r;
    }

    /** 相邻(左/右)有同朝代卡。 */
    private static boolean hasAdjacentDynasty(List<FieldCard> field, int i, String dynasty)
    {
        boolean l = i - 1 >= 0 && field.get(i - 1) != null
                && dynasty.equals(DuelCardData.dynastyOf(field.get(i - 1).card));
        boolean r = i + 1 < field.size() && field.get(i + 1) != null
                && dynasty.equals(DuelCardData.dynastyOf(field.get(i + 1).card));
        return l || r;
    }

    /** 放在金质卡上的骰数。 */
    private static int countGoldDice(List<FieldCard> field)
    {
        int c = 0;
        for (FieldCard fc : field)
        {
            if (fc != null && DuelCardData.isGold(fc.card)) c += fc.diceCount();
        }
        return c;
    }

    /** 骰型条件（在"你放置的骰"上判定）。 */
    public static final class Conds
    {
        private Conds() {}

        /** 顺子 = 互不相同且连续，要求 ≥3 颗骰（严格：所有骰子连续）。 */
        public static boolean isStraight(List<Integer> dice)
        {
            return isStraight(dice, false);
        }

        public static boolean isStraight(List<Integer> dice, boolean allowGap)
        {
            if (dice == null || dice.size() < 3) return false;
            int min = 7, max = 0;
            boolean[] seen = new boolean[7];
            for (int v : dice)
            {
                if (v < 1 || v > 6 || seen[v]) return false;
                seen[v] = true;
                if (v < min) min = v;
                if (v > max) max = v;
            }
            return max - min <= dice.size() - 1 + (allowGap ? 1 : 0);
        }

        public static boolean allHigh(List<Integer> dice)
        {
            if (dice == null || dice.isEmpty()) return false;
            for (int v : dice) if (v < 4) return false;
            return true;
        }

        public static boolean allLow(List<Integer> dice)
        {
            if (dice == null || dice.isEmpty()) return false;
            for (int v : dice) if (v > 3) return false;
            return true;
        }

        public static boolean allOdd(List<Integer> dice)
        {
            if (dice == null || dice.isEmpty()) return false;
            for (int v : dice) if (v % 2 == 0) return false;
            return true;
        }

        public static boolean allEven(List<Integer> dice)
        {
            if (dice == null || dice.isEmpty()) return false;
            for (int v : dice) if (v % 2 == 1) return false;
            return true;
        }

        public static boolean hasOne(List<Integer> dice)
        {
            if (dice == null) return false;
            for (int v : dice) if (v == 1) return true;
            return false;
        }

        // ---- 第 2 批：新骰型 ----

        /** 两对：至少两组"相同两面"。 */
        public static boolean twoPair(List<Integer> dice)
        {
            if (dice == null || dice.size() < 4) return false;
            int[] cnt = new int[7];
            for (int v : dice) if (v >= 1 && v <= 6) cnt[v]++;
            int pairs = 0;
            for (int c : cnt) if (c >= 2) pairs++;
            return pairs >= 2;
        }

        /** 满堂彩：三同 + 一对。 */
        public static boolean fullHouse(List<Integer> dice)
        {
            if (dice == null || dice.size() < 5) return false;
            int[] cnt = new int[7];
            for (int v : dice) if (v >= 1 && v <= 6) cnt[v]++;
            boolean three = false, pair = false;
            for (int c : cnt)
            {
                if (c >= 3) three = true;
                else if (c == 2) pair = true;
            }
            return three && pair;
        }

        /** 全相同（≥3 颗）。 */
        public static boolean allSame(List<Integer> dice)
        {
            if (dice == null || dice.size() < 3) return false;
            int first = dice.get(0);
            for (int v : dice) if (v != first) return false;
            return true;
        }

        /** 含 6。 */
        public static boolean hasSix(List<Integer> dice)
        {
            if (dice == null) return false;
            for (int v : dice) if (v == 6) return true;
            return false;
        }

        /** 骰面和 ≤9 或 ≥18（两极）。 */
        public static boolean sumRange(List<Integer> dice)
        {
            if (dice == null || dice.isEmpty()) return false;
            int s = 0;
            for (int v : dice) s += v;
            return s <= 9 || s >= 18;
        }

        /** 含相邻两数（如 2 和 3）。 */
        public static boolean consecNear(List<Integer> dice)
        {
            if (dice == null || dice.isEmpty()) return false;
            boolean[] seen = new boolean[7];
            for (int v : dice) if (v >= 1 && v <= 6) seen[v] = true;
            for (int v = 1; v < 6; v++) if (seen[v] && seen[v + 1]) return true;
            return false;
        }
    }
}
