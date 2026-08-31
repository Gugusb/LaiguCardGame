package com.laigu.laigu.duel;

import com.laigu.laigu.registry.ModItems;
import com.laigu.laigu.util.CardNbt;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * 人机 AI（V1 弱智能，供单人自测）。
 * 规则：部署按行动力放价值最高的手牌（无空位时只换明显更好的）；抢骰贪大面；布置优先喂"每骰"类卡、
 * 避开"0 骰"类卡，放不完的骰子继承到下轮。
 * act() 一次调用把一个阶段推进到位（部署/布置是完整的，选骰只拿一颗）。
 */
public final class DuelAi
{
    public static final String AI_NAME = "AI·来古";

    private DuelAi() {}

    /** 预置卡组（16 张来古牌：攻2/守5/谋2/鼎4 + 追加6张）。 */
    public static List<ItemStack> presetDeck()
    {
        return List.of(
                stack("tong_che_ma", "common"),             // 攻：每骰 +5 额外
                stack("qian_li_jiang_shan", "common"),      // 攻：入场若上轮输抽 2
                stack("qing_tong_xian_he", "common"),       // 守：离场抽 1
                stack("shang_yang_tai_tie", "common"),      // 守：0骰 +10 额外
                stack("si_long_si_feng_zuo", "common"),     // 谋：入场回 1 行动力
                stack("xiao_song_xiang_lu", "common"),      // 谋：结算赢抽 2
                stack("xue_jing_han_lin_tu", "common"),     // 鼎：每鼎卡 +1 倍率
                stack("yuan_wang_bei", "common"),           // 鼎：每张卡 +1 基础
                stack("luo_shen_fu_tu", "common"),          // 鼎：基础 ×2（消耗）
                stack("tai_yang_shen_niao", "common"),      // 鼎：每金卡 +1 倍率
                stack("yue_wang_gou_jian_jian", "common"),  // 攻：入场抽 1
                stack("mo_gao_ku_ji", "common"),            // 守：全小 +30 额外（充能1）
                stack("cai_feng_ming_qi", "common"),        // 守：上轮在场 +12 额外
                stack("shui_jing_bei", "common"),           // 守：每轮抽 1
                stack("wan_he_song_feng_tu", "common"),     // 守：无条件 +6 额外
                stack("jin_shi_lu", "common")               // 鼎：每唐代卡 +2 基础
        );
    }

    private static ItemStack stack(String cardId, String rarity)
    {
        return new ItemStack(ModItems.getCardItem(cardId, rarity));
    }

    /** 执行 AI 一方当前需要的一个阶段动作。 */
    public static void act(DuelGame g, int side)
    {
        switch (g.phase())
        {
            case DEPLOY -> deploy(g, side);
            case DRAFT -> pickDie(g, side);
            case PLACE -> place(g, side);
            case ROUND_END -> g.applyAction(side, DuelActions.NEXT_ROUND, 0, 0);
            default -> {}
        }
    }

    // ---- 部署：用光行动力放价值最高的手牌，然后确认 ----

    private static void deploy(DuelGame g, int side)
    {
        if (g.deployDone(side)) return;
        int ap = g.actionPoints(side);
        for (int used = 0; used < ap; used++)
        {
            if (g.deployDone(side)) break;
            int best = bestHandIdx(g, side);
            if (best < 0) break;
            ItemStack h = g.hand(side).get(best);
            int target;
            if (DuelCardData.isGold(h))
            {
                // 金卡只能替换同名白卡
                target = goldUpgradeSlot(g, side, h);
                if (target < 0) break;
            }
            else
            {
                target = firstEmptySlot(g, side);
                if (target < 0)
                {
                    // 无空位：只有新卡明显比最差场卡更好才替换
                    int worst = worstFieldSlot(g, side);
                    if (worst < 0) break;
                    DuelCardData bestD = DuelCardCatalog.of(h);
                    DuelCardData worstD = DuelCardCatalog.of(g.field(side).get(worst).card);
                    if (bestD == null || worstD == null || cardValue(bestD) <= cardValue(worstD) + 2) break;
                    target = worst;
                }
            }
            g.applyAction(side, DuelActions.DEPLOY_PUT, best, target);
            if (g.lastMsg != null) return;
        }
        if (g.phase() == DuelGame.Phase.DEPLOY && !g.deployDone(side))
        {
            g.applyAction(side, DuelActions.DEPLOY_CONFIRM, 0, 0);
        }
    }

    /** 场上与金卡同名的白卡槽位（金卡替换目标）；无则 -1。 */
    private static int goldUpgradeSlot(DuelGame g, int side, ItemStack gold)
    {
        String base = CardNbt.stripRaritySuffix(CardNbt.pathOf(gold));
        for (int i = 0; i < DuelGame.FIELD_SLOTS; i++)
        {
            FieldCard fc = g.field(side).get(i);
            if (fc == null || DuelCardData.isGold(fc.card)) continue;
            if (base.equals(CardNbt.stripRaritySuffix(CardNbt.pathOf(fc.card)))) return i;
        }
        return -1;
    }

    private static int bestHandIdx(DuelGame g, int side)
    {
        List<ItemStack> hand = g.hand(side);
        int best = -1, bestV = Integer.MIN_VALUE;
        for (int i = 0; i < hand.size(); i++)
        {
            ItemStack h = hand.get(i);
            DuelCardData d = DuelCardCatalog.of(h);
            if (d == null) continue;
            boolean gold = DuelCardData.isGold(h);
            if (gold && goldUpgradeSlot(g, side, h) < 0) continue; // 金卡无同名白卡：跳过
            int v = cardValue(d) + (gold ? 6 : 0);
            if (v > bestV) { bestV = v; best = i; }
        }
        return best;
    }

    private static int worstFieldSlot(DuelGame g, int side)
    {
        int worst = -1, worstV = Integer.MAX_VALUE;
        for (int i = 0; i < DuelGame.FIELD_SLOTS; i++)
        {
            FieldCard fc = g.field(side).get(i);
            if (fc == null) continue;
            DuelCardData d = DuelCardCatalog.of(fc.card);
            int v = d == null ? 0 : cardValue(d);
            if (v < worstV) { worstV = v; worst = i; }
        }
        return worst;
    }

    private static int firstEmptySlot(DuelGame g, int side)
    {
        for (int i = 0; i < DuelGame.FIELD_SLOTS; i++)
        {
            if (g.field(side).get(i) == null) return i;
        }
        return -1;
    }

    /** 部署价值粗估：引擎 > 吃骰 > 保底。 */
    private static int cardValue(DuelCardData d)
    {
        int v = 0;
        switch (d.effect)
        {
            case STRAIGHT_MULT, ALL_ODD_MULT, ALL_HIGH_MULT, GOLD_CNT_MULT, CLASS_CNT_MULT,
                 TWO_PAIR_MULT, FULL_HOUSE_MULT, ALL_SAME_MULT,
                 CONSUME_PER_DIE_MULT, CONSUME_PER_DIE_BASE, CONSUME_BASE_FLAT, CONSUME_EXTRA_DOUBLE,
                 CONSUME_HAND_MULT, CONSUME_OPP_DICE_EXTRA,
                 GOLD_CNT_BASE, GOLD_DIE_MULT, GOLD_DYN_MULT, GOLD_EXTRA:
                v += 5; break;
            case PER_DIE_EXTRA, PER_DIE_BASE, PER_DIE_MULT, DYN_CNT_BASE, CLASS_CNT_BASE, CARD_CNT_BASE,
                 ODD_DIE_EXTRA, EVEN_DIE_EXTRA, DIE_GE4_EXTRA, DIE_LE3_EXTRA,
                 DIE_FIRST_BONUS, DICE_GE1_EXTRA,
                 HAND_CNT_MULT, POOL_CNT_BASE, POOL_CNT_MULT, DECK_CNT_BASE, SHARED_POOL_EXTRA:
                v += 4; break;
            case DICE_GE2_EXTRA, ALL_HIGH_EXTRA, ALL_EVEN_EXTRA, ALL_LOW_EXTRA,
                 DIE_SUM_GE_EXTRA, DIE_SUM_ODD_EXTRA, SAME_FACE_MULT,
                 HAS_SIX_EXTRA, SUM_RANGE_EXTRA, CONSEC_NEAR_EXTRA,
                 EDGE_EXTRA, CENTER_MULT, ADJ_SAME_CLASS_MULT, ADJ_DIFF_CLASS_EXTRA, ADJ_SAME_DYN_EXTRA:
                v += 2; break;
            case FLAT_EXTRA, LAST_ROUND_EXTRA, ZERO_DICE_EXTRA, HAS_ONE_EXTRA,
                 OPP_MORE_DICE_EXTRA, OPP_FIELD_FULL_EXTRA, LOSE_LAST_EXTRA, DRAW_LAST_EXTRA,
                 WIN_LAST_MULT, BEHIND_WINS_EXTRA, HAND_EMPTY_EXTRA,
                 LASTED_2_EXTRA, ROUND_GE2_EXTRA, ROUND_EVEN_MULT, FIRST_PICK_MULT, NO_DICE_MULT:
                v += 1; break;
            case DRAFT_POOL_UP, DRAFT_TURNS_DOWN, DRAFT_TURNS_UP,
                 DRAFT_SELF_TURNS_UP, DRAFT_OPP_TURNS_DOWN, DRAFT_SELF_GRAB_UP:
                v += 3; break; // 抓取系：改变抢骰计划，值得上场
            case SUMMON_DRAW, ROUND_START_DRAW, ROUND_START_DRAW_STAY_TURNS,
                 SUMMON_DRAW_IF_LOST_LAST, OTHER_USE_DRAW, LEAVE_DRAW,
                 ROUND_END_DRAW_IF_WIN, ROUND_END_DRAW_IF_LOSE:
                v += 3; break; // 触发系：抽卡引擎 / 节奏，值得上场
            case SUMMON_RESTORE_AP:
                v += 2; break; // 触发系：回复行动力
            default:
                break;
        }
        return v;
    }

    // ---- 抢骰：贪大面 ----

    private static void pickDie(DuelGame g, int side)
    {
        if (g.currentPicker() != side) return;
        if (g.pickTarget(side) <= 0) return; // 本轮抓取计划已尽
        List<Integer> shared = g.sharedPool();
        if (shared.isEmpty()) return;
        int best = 0;
        for (int i = 1; i < shared.size(); i++)
        {
            if (shared.get(i) > shared.get(best)) best = i;
        }
        g.applyAction(side, DuelActions.PICK_DIE, best, 0);
    }

    // ---- 布置：把骰喂给"每骰"类卡，避开"0 骰"类卡；放不完的继承 ----

    private static void place(DuelGame g, int side)
    {
        if (g.placeDone(side)) return;
        while (!g.pool(side).isEmpty())
        {
            int slot = bestDieSlot(g, side);
            if (slot < 0) break;
            g.applyAction(side, DuelActions.PLACE_DIE, 0, slot);
            if (g.lastMsg != null) return;
        }
        if (g.phase() == DuelGame.Phase.PLACE && !g.placeDone(side))
        {
            g.applyAction(side, DuelActions.PLACE_CONFIRM, 0, 0);
        }
    }

    private static int bestDieSlot(DuelGame g, int side)
    {
        int best = -1, bestV = Integer.MIN_VALUE;
        for (int i = 0; i < DuelGame.FIELD_SLOTS; i++)
        {
            FieldCard fc = g.field(side).get(i);
            if (fc == null || !fc.canAddDie()) continue;
            DuelCardData d = DuelCardCatalog.of(fc.card);
            int v = d == null ? 0 : dieSlotValue(d.effect);
            if (v > bestV) { bestV = v; best = i; }
        }
        return best;
    }

    private static int dieSlotValue(EffectType e)
    {
        return switch (e)
        {
            case PER_DIE_EXTRA, PER_DIE_BASE, PER_DIE_MULT,
                 ODD_DIE_EXTRA, EVEN_DIE_EXTRA, DIE_GE4_EXTRA, DIE_LE3_EXTRA,
                 DIE_FIRST_BONUS, DICE_GE1_EXTRA, DIE_SUM_GE_EXTRA, DIE_SUM_ODD_EXTRA,
                 SAME_FACE_MULT, CONSUME_PER_DIE_MULT, CONSUME_PER_DIE_BASE, GOLD_DIE_MULT -> 10;
            case DICE_GE2_EXTRA -> 6;
            case ZERO_DICE_EXTRA, NO_DICE_MULT -> -8; // 尽量留空
            default -> 2;
        };
    }
}
