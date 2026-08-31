# -*- coding: utf-8 -*-
"""
来古牌对战效果「触发条件难度」评级模型（可复现）

只评【触发条件】——达成它有多难。触发收益（+X分 / ×Y倍率 / 抽Z张 / 回AP）一律不参与。

模型：D = w_P·f_P + w_R·f_R + w_C·f_C + w_S·f_S + w_T·f_T + w_M·f_M
  f_P 骰型/数值概率因子：f_P = min(1, -log2(p) / 7)，p 为达成概率（6 面骰，默认场上 5 颗骰 k=5）
  f_R 资源投入因子   ：需放几颗骰 / 手牌 / 留骰不布置
  f_C 组合协同因子   ：需同朝代 / 同职业 / 金质卡
  f_S 站位因子       ：槽位位置 / 相邻关系 / 孤立
  f_T 状态时机因子   ：轮次 / 上轮结果 / 连续站场 / 落后 / 手牌空
  f_M 代价因子       ：【消耗】离场 / 自伤
权重：w_P=0.30  w_R=0.20  w_C=0.15  w_S=0.15  w_T=0.10  w_M=0.10

等级：★1 白给 D<0.06 · ★2 简单 <0.15 · ★3 中等 <0.23 · ★4 困难 <0.30 · ★5 极难 ≥0.30
代价标注：无 / ●消耗 / △自伤（消耗已并入 D 的 f_M 项）
"""
import re, math

SRC = r"d:\GuguFiles\AI测试项目\MC卡牌游戏开发\Laigu\src\main\java\com\laigu\laigu\duel\DuelCardCatalog.java"
OUT = r"d:\GuguFiles\AI测试项目\MC卡牌游戏开发\Laigu\docs\触发条件难度模型.md"

W = (0.30, 0.20, 0.15, 0.15, 0.10, 0.10)  # P R C S T M

# ---------- 1. 精确枚举 6 面骰骰型概率（k=3~6） ----------
def probs(k):
    from itertools import product
    n = 0; c = {}
    for t in product(range(1, 7), repeat=k):
        n += 1
        s = sum(t)
        c.setdefault("hasSix", 0); c.setdefault("hasOne", 0); c.setdefault("allHigh", 0)
        c.setdefault("allLow", 0); c.setdefault("allOdd", 0); c.setdefault("allEven", 0)
        c.setdefault("straight", 0); c.setdefault("twoPair", 0); c.setdefault("fullHouse", 0)
        c.setdefault("allSame", 0); c.setdefault("sumRange", 0); c.setdefault("consecNear", 0)
        if 6 in t: c["hasSix"] += 1
        if 1 in t: c["hasOne"] += 1
        if all(v >= 4 for v in t): c["allHigh"] += 1
        if all(v <= 3 for v in t): c["allLow"] += 1
        if all(v % 2 == 1 for v in t): c["allOdd"] += 1
        if all(v % 2 == 0 for v in t): c["allEven"] += 1
        # straight: 互不相同且连续（全骰连续）
        if k >= 3 and len(set(t)) == k and max(t) - min(t) == k - 1: c["straight"] += 1
        # twoPair: 至少两组"同面≥2"
        if k >= 4:
            cnt = [t.count(x) for x in set(t)]
            if sum(1 for x in cnt if x >= 2) >= 2: c["twoPair"] += 1
        # fullHouse: 三同+一对
        if k >= 5:
            cnt = [t.count(x) for x in set(t)]
            if (3 in cnt and 2 in cnt): c["fullHouse"] += 1
        # allSame: 全部相同
        if k >= 3 and len(set(t)) == 1: c["allSame"] += 1
        # sumRange: 和 ≤9 或 ≥18
        if s <= 9 or s >= 18: c["sumRange"] += 1
        # consecNear: 含相邻两数
        if any((v in t) and (v+1 in t) for v in range(1, 6)): c["consecNear"] += 1
    return {kk: vv / n for kk, vv in c.items()}

P5 = probs(5)   # 默认 k=5
def fP(p):
    return min(1.0, -math.log2(p) / 7.0) if p < 1.0 else 0.0

# ---------- 2. 触发条件定义（effect -> (条件名, fP, fR, fC, fS, fT, fM)） ----------
E = {}
def add(eff, name, fp, fr, fc, fs, ft, fm):
    E[eff] = (name, fp, fr, fc, fs, ft, fm)

# 无条件固定收益（长城：无条件但带充能）
add("FLAT_EXTRA", "无条件（固定额外分）", 0, 0, 0, 0, 0, 0)

# 入场/离场/每轮/使用手牌——自动触发（白给）
for e, nm, t in [
    ("SUMMON_DRAW", "入场时", 0), ("SUMMON_RESTORE_AP", "入场时", 0),
    ("LEAVE_DRAW", "离场时", 0), ("ROUND_START_DRAW", "每轮开始时", 0),
    ("ROUND_END_DRAW_IF_WIN", "结算后本轮赢", 0.2), ("ROUND_END_DRAW_IF_LOSE", "结算后本轮输", 0.2),
    ("OTHER_USE_DRAW", "使用其他手牌时", 0), ("SUMMON_DRAW_IF_LOST_LAST", "入场时且上轮输", 0.2),
]:
    add(e, nm, 0, 0, 0, 0, t, 0)
add("ROUND_START_DRAW_STAY_TURNS", "每轮开始（站场轮数越多收益越大）", 0, 0.2, 0, 0, 0, 0)

# 吃骰输出（触发只需本卡≥1骰）
for e, nm in [("PER_DIE_EXTRA", "本卡≥1颗骰（吃骰）"), ("PER_DIE_BASE", "本卡≥1颗骰（吃骰）"),
              ("PER_DIE_MULT", "本卡≥1颗骰（吃骰）"), ("DICE_GE1_EXTRA", "本卡≥1颗骰")]:
    add(e, nm, 0, 0.2, 0, 0, 0, 0)
add("DICE_GE2_EXTRA", "本卡≥2颗骰", 0, 0.35, 0, 0, 0, 0)
add("DIE_FIRST_BONUS", "本卡第1颗骰（第2颗吃满）", 0, 0.35, 0, 0, 0, 0)
add("SAME_FACE_MULT", "本卡恰好2颗骰且相同", fP(1/6), 0.4, 0, 0, 0, 0)
add("ODD_DIE_EXTRA",  "本卡≥1颗奇数骰", fP(0.5), 0.2, 0, 0, 0, 0)
add("EVEN_DIE_EXTRA", "本卡≥1颗偶数骰", fP(0.5), 0.2, 0, 0, 0, 0)
add("DIE_GE4_EXTRA",  "本卡≥1颗大点数骰(≥4)", fP(0.5), 0.2, 0, 0, 0, 0)
add("DIE_LE3_EXTRA",  "本卡≥1颗小点数骰(≤3)", fP(0.5), 0.2, 0, 0, 0, 0)
add("DIE_SUM_GE_EXTRA", "本卡骰面和≥8", fP(21/36), 0.25, 0, 0, 0, 0)
add("DIE_SUM_ODD_EXTRA", "本卡骰面和为奇数", fP(0.5), 0.25, 0, 0, 0, 0)
add("ZERO_DICE_EXTRA", "本卡0颗骰", 0, 0.1, 0, 0, 0, 0)
add("NO_DICE_MULT", "本卡0颗骰", 0, 0.1, 0, 0, 0, 0)

# 全盘骰型（场上所有放置骰）
add("ALL_HIGH_EXTRA", "场上所有骰≥4（全大）", fP(P5["allHigh"]), 0, 0, 0, 0, 0)
add("ALL_HIGH_MULT",  "场上所有骰≥4（全大）", fP(P5["allHigh"]), 0, 0, 0, 0, 0)
add("ALL_LOW_EXTRA",  "场上所有骰≤3（全小）", fP(P5["allLow"]), 0, 0, 0, 0, 0)
add("ALL_ODD_MULT",   "场上所有骰为奇数", fP(P5["allOdd"]), 0, 0, 0, 0, 0)
add("ALL_EVEN_EXTRA", "场上所有骰为偶数", fP(P5["allEven"]), 0, 0, 0, 0, 0)
add("STRAIGHT_MULT",  "场上所有骰为顺子", fP(P5["straight"]), 0, 0, 0, 0, 0)
add("FULL_HOUSE_MULT","场上骰满堂彩(三同+一对)", fP(P5["fullHouse"]), 0, 0, 0, 0, 0)
add("TWO_PAIR_MULT",  "场上骰有两对", fP(P5["twoPair"]), 0, 0, 0, 0, 0)
add("ALL_SAME_MULT",  "场上所有骰相同", fP(P5["allSame"]), 0, 0, 0, 0, 0)
add("HAS_SIX_EXTRA",  "场上骰含6", fP(P5["hasSix"]), 0, 0, 0, 0, 0)
add("HAS_ONE_EXTRA",  "场上骰含1", fP(P5["hasOne"]), 0, 0, 0, 0, 0)
add("SUM_RANGE_EXTRA","场上骰面和≤9或≥18", fP(P5["sumRange"]), 0, 0, 0, 0, 0)
add("CONSEC_NEAR_EXTRA", "场上骰含相邻两数", fP(P5["consecNear"]), 0, 0, 0, 0, 0)

# 站位
add("EDGE_EXTRA", "本卡在两端槽位", 0, 0, 0, 0.15, 0, 0)
add("CENTER_MULT", "本卡在中间槽位", 0, 0, 0, 0.3, 0, 0)
add("ADJ_DIFF_CLASS_EXTRA", "相邻有不同职业卡", 0, 0, 0, 0.2, 0, 0)
add("ADJ_SAME_CLASS_MULT",  "相邻有同职业卡", 0, 0, 0, 0.35, 0, 0)
add("ADJ_SAME_DYN_EXTRA",   "相邻有同朝代卡", 0, 0, 0, 0.4, 0, 0)
add("ISOLATED_MULT_EXTRA",  "两侧均非本卡朝代（缺侧视为满足）", 0, 0, 0, 0.15, 0, 0)
add("NEIGHBOR_MULT", "相邻有任意卡", 0, 0, 0, 0.1, 0, 0)

# 组合（计数引擎：本卡自己通常算 1 → 触发几乎无条件）
add("CARD_CNT_BASE", "无条件（我方场卡计数）", 0, 0, 0, 0, 0, 0)
add("HAND_CNT_MULT", "无条件（手牌计数）", 0, 0, 0, 0, 0, 0)
add("POOL_CNT_BASE", "留有未布置骰（牺牲布置收益）", 0, 0.2, 0, 0, 0, 0)
add("POOL_CNT_MULT", "留有未布置骰（牺牲布置收益）", 0, 0.2, 0, 0, 0, 0)
add("DECK_CNT_BASE", "无条件（卡组计数）", 0, 0, 0, 0, 0, 0)
add("SHARED_POOL_EXTRA", "无条件（共享骰池计数）", 0, 0, 0, 0, 0, 0)
add("CLASS_CNT_MULT", "场上≥1张本职业卡（本卡算1）", 0, 0, 0, 0, 0, 0)
add("CLASS_CNT_BASE", "场上≥1张目标职业卡", 0, 0, 0.1, 0, 0, 0)
add("DYN_CNT_BASE", "场上≥1张目标朝代卡", 0, 0, 0.2, 0, 0, 0)
for e in ["GOLD_CNT_BASE", "GOLD_CNT_MULT", "GOLD_EXTRA", "GOLD_DYN_MULT"]:
    add(e, "场上≥1张金质卡", 0, 0, 0.5, 0, 0, 0)
add("GOLD_DIE_MULT", "金质卡上有骰", 0, 0.2, 0.5, 0, 0, 0)

# 状态/对位
add("OPP_MORE_DICE_EXTRA", "对手放置骰比你多", fP(0.35), 0, 0, 0, 0, 0)
add("OPP_FIELD_FULL_EXTRA", "对手场上满4张", fP(0.4), 0, 0, 0, 0, 0)
add("LOSE_LAST_EXTRA", "上轮你输", 0, 0, 0, 0, 0.2, 0)
add("DRAW_LAST_EXTRA", "上轮平局", 0, 0, 0, 0, 0.35, 0)
add("WIN_LAST_MULT", "上轮你赢", 0, 0, 0, 0, 0.2, 0)
add("BEHIND_WINS_EXTRA", "总局数落后", 0, 0, 0, 0, 0.3, 0)
add("HAND_EMPTY_EXTRA", "手牌为0", 0, 0.35, 0, 0, 0, 0)
add("LASTED_2_EXTRA", "连续在场≥2轮", 0, 0.2, 0, 0, 0.3, 0)
add("LAST_ROUND_EXTRA", "上轮也在场", 0, 0.2, 0, 0, 0.25, 0)
add("ROUND_GE2_EXTRA", "轮次≥2", 0, 0, 0, 0, 0.05, 0)
add("ROUND_EVEN_MULT", "偶数轮", 0, 0, 0, 0, 0.1, 0)
add("FIRST_PICK_MULT", "本方先手", 0, 0, 0, 0, 0.15, 0)

# 献祭（消耗代价 ●，并入 f_M）
add("STRAIGHT_PER_DIE_MULT_CONSUME", "场上骰为顺子 + 本卡≥1颗骰", fP(P5["straight"]), 0.2, 0, 0, 0, 0.4)
add("CONSUME_PER_DIE_MULT", "本卡≥1颗骰", 0, 0.2, 0, 0, 0, 0.4)
add("CONSUME_BASE_FLAT", "无条件", 0, 0, 0, 0, 0, 0.4)
add("BASE_DOUBLE_CONSUME", "无条件", 0, 0, 0, 0, 0, 0.4)
add("CONSUME_EXTRA_DOUBLE", "无条件", 0, 0, 0, 0, 0, 0.4)
add("CONSUME_HAND_MULT", "无条件（按手牌计数）", 0, 0, 0, 0, 0, 0.4)
add("CONSUME_OPP_DICE_EXTRA", "对手有骰", fP(0.9), 0, 0, 0, 0, 0.4)

# 抢骰（自动生效）
for e in ["DRAFT_POOL_UP", "DRAFT_TURNS_UP", "DRAFT_SELF_TURNS_UP", "DRAFT_OPP_TURNS_DOWN", "DRAFT_SELF_GRAB_UP"]:
    add(e, "抢骰阶段自动生效", 0, 0, 0, 0, 0, 0)
add("DRAFT_TURNS_DOWN", "抢骰阶段自动（双方少抓=自伤）", 0, 0, 0, 0, 0, 0.2)

# ---------- 3. 解析 catalog，逐卡评级 ----------
def grade(d):
    return ("★5 极难" if d >= 0.30 else "★4 困难" if d >= 0.23 else
            "★3 中等" if d >= 0.12 else "★2 简单" if d >= 0.06 else "★1 白给")

def star(d):
    return ("★5" if d >= 0.30 else "★4" if d >= 0.23 else "★3" if d >= 0.12 else "★2" if d >= 0.06 else "★1")

rows, unruled = [], set()
for ln in open(SRC, encoding="utf-8").read().splitlines():
    if not ln.strip().startswith("card("):
        continue
    m = re.search(r'card\(\s*"([^"]+)",\s*"([^"]+)",\s*CardClass\.(\w+),\s*EffectType\.(\w+),\s*(\d+),\s*(\d+),\s*(?:"[^"]*"|null),\s*(?:CardClass\.\w+|null)(?:\s*,\s*(\d+))?\s*,\s*"([^"]*)"', ln)
    if not m:
        continue
    name, cls, eff = m.group(2), m.group(3), m.group(4)
    charge = int(m.group(7)) if m.group(7) else 0
    desc = m.group(8)
    if eff not in E:
        unruled.add(eff); continue
    cond, fp, fr, fc, fs, ft, fm = E[eff]
    # 【充能】= 需本卡≥1颗骰激活
    if charge > 0:
        fr = max(fr, 0.2)
        cond = cond + "；且本卡≥1颗骰（充能）"
    d = (W[0]*fp + W[1]*fr + W[2]*fc + W[3]*fs + W[4]*ft + W[5]*fm)
    rows.append({"name": name, "cls": {"GONG":"攻","SHOU":"守","MOU":"谋","DING":"鼎"}[cls],
                 "cond": cond, "desc": desc, "charge": charge,
                 "fp": fp, "fr": fr, "fc": fc, "fs": fs, "ft": ft, "fm": fm,
                 "d": round(d, 3), "star": star(d),
                 "cost": "●消耗" if fm >= 0.4 else ("△自伤" if fm == 0.2 and eff == "DRAFT_TURNS_DOWN" else "无")})
rows.sort(key=lambda x: (-x["d"], x["name"]))

dist = {}
for x in rows: dist[x["star"]] = dist.get(x["star"], 0) + 1

# ---------- 4. 输出 markdown ----------
o = []
o.append("# 来古牌 · 触发条件难度评级模型")
o.append("")
o.append("> 生成脚本：`docs/工具/难度评级脚本.py`（可复现）。数据源：`DuelCardCatalog` 79 张卡。")
o.append("> **只评「触发条件」的达成难度，不评触发收益。** 收益（+X分/×Y倍率/抽Z张/回AP）不参与任何评分。")
o.append("")
o.append("## 一、模型")
o.append("")
o.append("每个触发条件按六类难度因子量化，加权求和：")
o.append("")
o.append("$$ D = w_P·f_P + w_R·f_R + w_C·f_C + w_S·f_S + w_T·f_T + w_M·f_M $$")
o.append("")
o.append("| 因子 | 权重 | 含义 | 量化规则 |")
o.append("|---|---|---|---|")
o.append("| f_P 骰型概率 | 0.30 | 达成骰型/数值条件的概率困难度 | `f_P = min(1, -log2(p)/7)`，p 为 6 面骰达成概率（默认场上 k=5 颗骰） |")
o.append("| f_R 资源投入 | 0.20 | 需投入的骰子/手牌/留骰 | 本卡≥1骰 0.2 · ≥2骰/恰好2骰 0.35~0.4 · 手牌0 0.35 · 留骰不布置 0.2 |")
o.append("| f_C 组合协同 | 0.15 | 需场上同朝代/同职业/金质卡 | 同职业 0.1 · 同朝代 0.2 · 金质 0.5（金质=稀有度，获取概率低） |")
o.append("| f_S 站位 | 0.15 | 槽位/相邻/孤立约束 | 两端 0.15 · 中间 0.3 · 相邻异职业 0.2 · 相邻同职业 0.35 · 相邻同朝代 0.4 · 孤立 0.15 |")
o.append("| f_T 状态时机 | 0.10 | 依赖轮次/上轮结果/站场 | 上轮赢输 0.2 · 上轮平 0.35 · 落后 0.3 · 连续站场2轮 0.3 · 轮次≥2 0.05 |")
o.append("| f_M 代价 | 0.10 | 【消耗】离场 / 自伤 | 消耗 0.4 · 天球仪自伤 0.2 |")
o.append("")
o.append("**等级**（D 越大越难触发）：★1 白给 <0.06 · ★2 简单 <0.12 · ★3 中等 <0.23 · ★4 困难 <0.30 · ★5 极难 ≥0.30")
o.append("")
o.append("> 代价标注：●=【消耗】结算后离场（一次性，已并入 D）；△=副作用自伤。")
o.append("")
o.append("## 二、骰型概率基准（精确枚举 6^k）")
o.append("")
o.append("| 骰型 | p(k=3) | p(k=4) | p(k=5) | p(k=6) | f_P(k=5) | 解读 |")
o.append("|---|---|---|---|---|---|---|")
for key, nm, ex in [
    ("hasSix", "含6", "有任意骰为6，很常见"),
    ("hasOne", "含1", "有任意骰为1，很常见"),
    ("consecNear", "含相邻两数", "几乎总是满足"),
    ("sumRange", "面和≤9或≥18", "两极，约一半对局满足"),
    ("twoPair", "两对(≥4颗)", "需要至少2组同面"),
    ("fullHouse", "满堂(三同+一对)", "严格牌型，少见"),
    ("allHigh", "全大(全部≥4)", "全部骰≥4"),
    ("allLow", "全小(全部≤3)", "全部骰≤3"),
    ("allOdd", "全奇", "全部骰为奇数"),
    ("allEven", "全偶", "全部骰为偶数"),
    ("straight", "顺子(互异连续)", "全部骰连续"),
    ("allSame", "全同(全部相同)", "全部骰同面，几乎不可能"),
]:
    p3, p4, p5, p6 = probs(3)[key], probs(4)[key], probs(5)[key], probs(6)[key]
    o.append(f"| {nm} | {p3:.4f} | {p4:.4f} | {p5:.4f} | {p6:.4f} | {fP(p5):.3f} | {ex} |")
o.append("")
o.append("> f_P 采用 k=5 档。场上骰子越多，全盘骰型越难达成；玩家实际放置骰数（一般 3~6）可在上表插值估计。")
o.append("")
o.append("## 三、难度分布")
o.append("")
o.append("| 等级 | 数量 | 含义 |")
o.append("|---|---|---|")
o.append(f"| ★1 白给 | {dist.get('★1',0)} | 自动触发 / 无条件 / 放1颗骰即可 |")
o.append(f"| ★2 简单 | {dist.get('★2',0)} | 大概率或小额投入 / 金质 / 站位 |")
o.append(f"| ★3 中等 | {dist.get('★3',0)} | 需要具体牌型或恰好2骰 |")
o.append(f"| ★4 困难 | {dist.get('★4',0)} | 全奇/全偶/顺子/满堂等稀有牌型 |")
o.append(f"| ★5 极难 | {dist.get('★5',0)} | 全同（0.08%）等极端牌型 |")
o.append("")
o.append("## 四、全卡触发条件难度表（按难度降序）")
o.append("")
o.append("| 卡牌 | 职业 | 触发条件 | f_P | f_R | f_C | f_S | f_T | f_M | D | 等级 | 代价 |")
o.append("|---|---|---|---|---|---|---|---|---|---|---|---|")
for x in rows:
    o.append(f"| {x['name']} | {x['cls']} | {x['cond']} | {x['fp']:.3f} | {x['fr']:.2f} | {x['fc']:.2f} | {x['fs']:.2f} | {x['ft']:.2f} | {x['fm']:.2f} | **{x['d']:.3f}** | {x['star']} | {x['cost']} |")
o.append("")
o.append("## 五、备注与假设")
o.append("")
o.append("- f_P 前提：默认我方场上 5 颗骰。顺子/全奇等全盘骰型的实际难度随放置骰数变化（见第二节概率表）。")
o.append("- 「充能」统一折算为 f_R=0.2：需先放 ≥1 颗骰才结算效果。")
o.append("- 金质卡是稀有度，出现概率低于普卡，故金质引擎类（金辉/盛世/鎏金/盛象）f_C=0.5。")
o.append("- 同职业计数（泰然）本卡自己算 1 → 触发无条件；目标职业/朝代计数（开疆/敕令）需场上另有 1 张。")
o.append("- 对位条件（坚垒/满阵/号角）用经验概率 0.9/0.4/0.35 估算，受对手打法定量影响。")
o.append("- 本模型为 v1 校准版：权重与阈值可随实测对局数据调整（改 `W` 与 `grade` 阈值后重跑脚本即得）。")
o.append("")

open(OUT, "w", encoding="utf-8").write("\n".join(o))
print("生成完成：", len(rows), "张卡")
print("分布：", dict(sorted(dist.items())))
if unruled:
    print("未配置规则的效果类型：", sorted(unruled))
print("P5 概率：", {k: round(v, 4) for k, v in P5.items()})
