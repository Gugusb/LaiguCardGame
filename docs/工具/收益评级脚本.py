# -*- coding: utf-8 -*-
"""
来古牌对战效果「卡牌收益」评级模型（可复现）

独立于触发条件难度模型：本模型只评【触发后收益的价值】（该效果值多少分/资源），
与达成难度分开评估。收益价值以「典型对局场景」为基准量化。

模型：
  V = 触发收益价值（触发时该效果换算成的分数/资源价值）
  P = 触发概率（骰型用精确枚举概率 k=5；条件/状态用经验概率）
  E = 期望收益 = P × V
  I = 资源投入（本卡吃的骰子数 / 0）
  性价比 = E / I

价值基准（脚本顶部常量，可调；见文档「价值基准」节）：
  典型基础分 B=25 · 典型倍率 m=2 · 场上卡4 · 同职业2 · 同朝代3 · 金质0.5
  手牌3 · 未布置骰1.5 · 共享骰池10 · 卡组未抽6 · 本卡典型骰数2
  卡牌价值8 · 行动力8 · 骰价值5 · 对手骰5 · 典型额外分总和20

收益等级（按 E）：微薄 <5 · 低 5–12 · 中 12–25 · 高 25–60 · 极高 ≥60
"""
import re

SRC = r"d:\GuguFiles\AI测试项目\MC卡牌游戏开发\Laigu\src\main\java\com\laigu\laigu\duel\DuelCardCatalog.java"
OUT = r"d:\GuguFiles\AI测试项目\MC卡牌游戏开发\Laigu\docs\卡牌收益模型.md"

# ---- 典型对局场景基准 ----
B          = 25.0     # 典型基础分（骰面和 17.5 + 基础加成 7.5）
M          = 2.0      # 典型倍率
FIELD      = 4        # 场上我方卡数
CLS        = 2        # 同职业数
DYN        = 3        # 同朝代数（唐）
GOLD       = 0.5      # 金质卡数（期望）
HAND       = 3        # 手牌数
POOL       = 1.5      # 未布置骰（继承）
SHARED     = 10       # 共享骰池大小
DECK_LEFT  = 6        # 卡组未抽牌数
N          = 2        # 本卡典型骰数
CARD_V     = 8        # 1 张卡牌价值
AP_V       = 8        # 1 点行动力价值
DIE_V      = 5        # 1 颗骰价值（3.5 骰面 + 牌型潜力）
OPP_DICE   = 5        # 对手场上骰数
EXTRA_SUM  = 20       # 典型额外分总和（重钟翻倍基数）

# ---- 触发概率（k=5 精确枚举值 + 经验） ----
PROB = {
    "hasSix": 0.5981, "hasOne": 0.5981, "consecNear": 0.8835, "sumRange": 0.5162,
    "twoPair": 0.2701, "fullHouse": 0.0386, "allHigh": 0.0312, "allLow": 0.0312,
    "allOdd": 0.0312, "allEven": 0.0312, "straight": 0.0309, "allSame": 0.0008,
    "diceSame2": 1 / 6, "faceGe8": 21 / 36, "faceOdd": 0.5, "oddFace": 0.5,
    "oppMoreDice": 0.35, "oppFieldFull": 0.4, "wonLast": 0.5, "lostLast": 0.5,
    "drawLast": 0.1, "behind": 0.35, "handEmpty": 0.5, "lasted2": 0.7,
    "onFieldLast": 0.7, "roundGe2": 1.0, "goldAny": 0.3, "winThis": 0.5,
    "loseThis": 0.5, "always": 1.0,
}

def P(key):
    return PROB[key]

# 第3批词条的条件概率（粗估，可按实测调）
PROB.setdefault("oppWeaker", 0.4)
PROB.setdefault("oppCard50", 0.5)
PROB.setdefault("draftOnce", 0.3)

# ---- 收益计算：effect -> (收益说明, V, P_key, I) ----
# I = 本卡资源投入（骰子数；0=无投入）
V = {}
def add(eff, desc, val, pkey, inv):
    V[eff] = (desc, val, pkey, inv)

# 攻·炽：吃骰输出
add("PER_DIE_EXTRA",        "每骰+p1 额外分", lambda p1,p2: p1 * N, "always", 2)
add("PER_DIE_BASE",         "每骰+p1 基础分", lambda p1,p2: p1 * N * M, "always", 2)
add("DICE_GE2_EXTRA",       "≥2骰 +p1",        lambda p1,p2: p1, "always", 2)
add("DIE_FIRST_BONUS",      "1骰+p1、2骰再+p2", lambda p1,p2: p1 + p2, "always", 2)
add("SAME_FACE_MULT",       "2骰相同 +p1倍率", lambda p1,p2: B * p1, "diceSame2", 2)
add("ODD_DIE_EXTRA",        "每颗奇数骰+p1",   lambda p1,p2: p1, "always", 2)
add("EVEN_DIE_EXTRA",       "每颗偶数骰+p1",   lambda p1,p2: p1, "always", 2)
add("DIE_GE4_EXTRA",        "每颗≥4骰+p1",     lambda p1,p2: p1, "always", 2)
add("DIE_LE3_EXTRA",        "每颗≤3骰+p1",     lambda p1,p2: p1, "always", 2)
add("DIE_SUM_GE_EXTRA",     "面和≥8 +p2",      lambda p1,p2: p2, "faceGe8", 2)
add("DIE_SUM_ODD_EXTRA",    "面和为奇 +p1",    lambda p1,p2: p1, "faceOdd", 2)
add("DICE_GE1_EXTRA",       "≥1骰 +p1",        lambda p1,p2: p1, "always", 1)
add("ZERO_DICE_EXTRA",      "0骰 +p1",         lambda p1,p2: p1, "always", 0)

# 守·衡：稳定/对位/持久
add("FLAT_EXTRA",           "无条件 +p1",      lambda p1,p2: p1, "always", 0)
add("LAST_ROUND_EXTRA",     "上轮在场 +p1",    lambda p1,p2: p1, "onFieldLast", 0)
add("OPP_MORE_DICE_EXTRA",  "对手骰多 +p1",    lambda p1,p2: p1, "oppMoreDice", 0)
add("OPP_FIELD_FULL_EXTRA", "对手满场 +p1",    lambda p1,p2: p1, "oppFieldFull", 0)
add("LOSE_LAST_EXTRA",      "上轮输 +p1",      lambda p1,p2: p1, "lostLast", 0)
add("DRAW_LAST_EXTRA",      "上轮平 +p1",      lambda p1,p2: p1, "drawLast", 0)
add("WIN_LAST_MULT",        "上轮赢 +p1倍率",  lambda p1,p2: B * p1, "wonLast", 0)
add("BEHIND_WINS_EXTRA",    "落后 +p1",        lambda p1,p2: p1, "behind", 0)
add("HAND_EMPTY_EXTRA",     "手牌0 +p1",       lambda p1,p2: p1, "handEmpty", 0)
add("LASTED_2_EXTRA",       "连续在场2轮 +p1", lambda p1,p2: p1, "lasted2", 0)
add("ROUND_GE2_EXTRA",      "轮次≥2 +p1",      lambda p1,p2: p1, "roundGe2", 0)
add("CONSUME_EXTRA_DOUBLE", "消耗·额外分×2",   lambda p1,p2: EXTRA_SUM, "always", 0)

# 谋·策：全盘骰型 + 站位
add("ALL_LOW_EXTRA",        "全小 +p1",        lambda p1,p2: p1, "allLow", 0)
add("ALL_ODD_MULT",         "全奇 +p1倍率",    lambda p1,p2: B * p1, "allOdd", 0)
add("ALL_EVEN_EXTRA",       "全偶 +p1",        lambda p1,p2: p1, "allEven", 0)
add("STRAIGHT_MULT",        "顺子 +p1倍率",    lambda p1,p2: B * p1, "straight", 0)
add("ALL_HIGH_MULT",        "全大 +p1倍率",    lambda p1,p2: B * p1, "allHigh", 0)
add("FULL_HOUSE_MULT",      "满堂 +p1倍率",    lambda p1,p2: B * p1, "fullHouse", 0)
add("TWO_PAIR_MULT",        "两对 +p1倍率",    lambda p1,p2: B * p1, "twoPair", 0)
add("ALL_SAME_MULT",        "全同 +p1倍率",    lambda p1,p2: B * p1, "allSame", 0)
add("HAS_SIX_EXTRA",        "含6 +p1",         lambda p1,p2: p1, "hasSix", 0)
add("SUM_RANGE_EXTRA",      "面和≤9/≥18 +p1", lambda p1,p2: p1, "sumRange", 0)
add("CONSEC_NEAR_EXTRA",    "含相邻两数 +p1",  lambda p1,p2: p1, "consecNear", 0)
add("EDGE_EXTRA",           "两端槽位 +p1",    lambda p1,p2: p1, "roundGe2", 0)   # 端位可规划 ≈ 高概率
add("CENTER_MULT",          "中间槽 +p1倍率",  lambda p1,p2: B * p1, "lasted2", 0)
add("ADJ_SAME_CLASS_MULT",  "相邻同职业 +p1倍率", lambda p1,p2: B * p1, "oppFieldFull", 0)
add("ADJ_DIFF_CLASS_EXTRA", "相邻异职业 +p1",  lambda p1,p2: p1, "oppFieldFull", 0)
add("ADJ_SAME_DYN_EXTRA",   "相邻同朝代 +p1",  lambda p1,p2: p1, "wonLast", 0)
add("ISOLATED_MULT_EXTRA",  "孤立 +p1倍率+p2", lambda p1,p2: B * p1 + p2, "roundGe2", 0)
add("NEIGHBOR_MULT",        "相邻任意卡 +p1倍率", lambda p1,p2: B * p1, "always", 0)

# 鼎·盛：资源引擎
add("DYN_CNT_BASE",         "每张唐卡+p2基础", lambda p1,p2: p2 * M * DYN, "always", 0)
add("CLASS_CNT_MULT",       "每张鼎卡+p2倍率", lambda p1,p2: B * p2 * CLS, "always", 0)
add("CLASS_CNT_BASE",       "每张攻卡+p2基础", lambda p1,p2: p2 * M * CLS, "always", 0)
add("CARD_CNT_BASE",        "每张场卡+p1基础", lambda p1,p2: p1 * M * FIELD, "always", 0)
add("GOLD_CNT_MULT",        "每张金质卡+p1倍率", lambda p1,p2: B * p1 * GOLD, "always", 0)
add("GOLD_CNT_BASE",        "每张金质卡+p1基础", lambda p1,p2: p1 * M * GOLD, "always", 0)
add("GOLD_EXTRA",           "每张金质卡+p1额外", lambda p1,p2: p1 * GOLD, "always", 0)
add("GOLD_DYN_MULT",        "有金质卡+p1倍率", lambda p1,p2: B * p1, "goldAny", 0)
add("GOLD_DIE_MULT",        "金质上每骰+p1倍率", lambda p1,p2: B * p1 * GOLD, "always", 0)
add("HAND_CNT_MULT",        "每张手牌+p1倍率", lambda p1,p2: B * p1 * HAND, "always", 0)
add("POOL_CNT_BASE",        "每颗未布置+p1基础", lambda p1,p2: p1 * M * POOL, "always", 0)
add("POOL_CNT_MULT",        "每颗未布置+p1倍率", lambda p1,p2: B * p1 * POOL, "always", 0)
add("DECK_CNT_BASE",        "每张未抽卡+p1基础", lambda p1,p2: p1 * M * DECK_LEFT, "always", 0)
add("SHARED_POOL_EXTRA",    "共享池每骰+p1额外", lambda p1,p2: p1 * SHARED, "always", 0)

# 抽卡 / 行动力
add("SUMMON_DRAW",            "入场抽p1张",     lambda p1,p2: p1 * CARD_V, "always", 0)
add("SUMMON_DRAW_IF_LOST_LAST", "入场且上轮输抽2", lambda p1,p2: p1 * CARD_V, "lostLast", 0)
add("LEAVE_DRAW",             "离场抽1张",      lambda p1,p2: p1 * CARD_V, "always", 0)
add("ROUND_START_DRAW",       "每轮抽1张",      lambda p1,p2: p1 * CARD_V, "always", 0)
add("ROUND_START_DRAW_STAY_TURNS", "每轮抽x张(x=站场)", lambda p1,p2: 1.5 * CARD_V, "always", 0)
add("OTHER_USE_DRAW",         "用其他手牌抽1",  lambda p1,p2: p1 * CARD_V, "always", 0)
add("ROUND_END_DRAW_IF_WIN",  "结算后赢抽1",    lambda p1,p2: p1 * CARD_V, "winThis", 0)
add("ROUND_END_DRAW_IF_LOSE", "结算后输抽1",    lambda p1,p2: p1 * CARD_V, "loseThis", 0)
add("SUMMON_RESTORE_AP",      "入场回1AP",      lambda p1,p2: p1 * AP_V, "always", 0)

# 献祭（消耗）
add("BASE_DOUBLE_CONSUME",    "消耗·基础分×2", lambda p1,p2: B, "always", 0)
add("CONSUME_PER_DIE_MULT",   "消耗·每骰+p1倍率", lambda p1,p2: B * p1 * N, "always", 1)
add("CONSUME_HAND_MULT",      "消耗·每手牌+p1倍率", lambda p1,p2: B * p1 * HAND, "always", 0)
add("CONSUME_OPP_DICE_EXTRA", "消耗·对手每骰+p1", lambda p1,p2: p1 * OPP_DICE, "always", 0)
add("STRAIGHT_PER_DIE_MULT_CONSUME", "消耗·顺子每骰+p1倍率", lambda p1,p2: B * p1 * N, "straight", 1)

# 抢骰
add("DRAFT_POOL_UP",          "抢骰·骰池+p1",  lambda p1,p2: p1 * DIE_V, "always", 0)
add("DRAFT_TURNS_UP",         "抢骰·双方+1次", lambda p1,p2: p1 * DIE_V, "always", 0)
add("DRAFT_TURNS_DOWN",       "抢骰·双方-1次(自伤)", lambda p1,p2: 0, "always", 0)
add("DRAFT_SELF_TURNS_UP",    "抢骰·本方+1次", lambda p1,p2: p1 * DIE_V, "always", 0)
add("DRAFT_OPP_TURNS_DOWN",   "抢骰·对方-1次", lambda p1,p2: p1 * DIE_V, "always", 0)
add("DRAFT_SELF_GRAB_UP",     "抢骰·本方每抓+1颗", lambda p1,p2: 3 * DIE_V, "always", 0)

# 节奏（未使用的效果，占位避免告警）
add("PER_DIE_MULT",       "每骰+p1倍率", lambda p1,p2: B * p1 * N, "always", 2)
add("CONSUME_BASE_FLAT",  "消耗·+p1基础", lambda p1,p2: 0, "always", 0)
add("ROUND_EVEN_MULT",    "偶数轮+p1倍率", lambda p1,p2: B * p1, "roundGe2", 0)
add("FIRST_PICK_MULT",    "先手+p1倍率", lambda p1,p2: B * p1, "wonLast", 0)
add("NO_DICE_MULT",       "0骰+p1倍率", lambda p1,p2: B * p1, "always", 0)

# 第3批新词条（破阵/伏击/激活/重骰/抓取得分）——粗测基线，按「保留少数强卡」偏好给保守估值
add("PO_ZHEN_HALVE",     "破阵·压制对位50%", lambda p1,p2: 18, "oppWeaker", 0)
add("FUJI",               "伏击·成功反制/失败保底", lambda p1,p2: 15, "oppCard50", 0)
add("ACTIVATE_LEFT",   "激活相邻+进度",   lambda p1,p2: 12, "always", 1)
add("REROLL_ON_DRAFT",  "重骰·抓骰后重掷", lambda p1,p2: 8, "draftOnce", 0)
add("DRAFT_SCORE_EXTRA","抓骰时(6-点数)*p1额", lambda p1,p2: 2.5 * p1, "always", 0)
add("PLACE_SCORE_EXTRA","布置时+p1额",   lambda p1,p2: p1, "always", 0)
add("ROUND_START_SCORE_EXTRA","回合开始+p1额", lambda p1,p2: p1, "always", 0)
add("USE_HAND_SCORE_EXTRA","用牌+p1额",  lambda p1,p2: p1, "always", 0)
add("ROUND_END_SCORE_EXTRA","回合结束+p1额", lambda p1,p2: p1, "always", 0)
add("HAS_ONE_EXTRA",      "含1 +p1", lambda p1,p2: p1, "hasOne", 0)
add("ALL_HIGH_EXTRA",     "全大 +p1", lambda p1,p2: p1, "allHigh", 0)

def grade_earn(e):
    return ("极高" if e >= 60 else "高" if e >= 25 else "中" if e >= 12 else "低" if e >= 5 else "微薄")

def cost_label(fm_desc):
    if "消耗" in fm_desc: return "●"
    if "自伤" in fm_desc: return "△"
    return ""

rows, unruled = [], set()
for ln in open(SRC, encoding="utf-8").read().splitlines():
    if not ln.strip().startswith("card("):
        continue
    m = re.search(r'card\(\s*"([^"]+)",\s*"([^"]+)",\s*CardClass\.(\w+),\s*EffectType\.(\w+),\s*(\d+),\s*(\d+),\s*(?:"[^"]*"|null),\s*(?:CardClass\.\w+|null)(?:\s*,\s*(\d+))?\s*,\s*"([^"]*)"', ln)
    if not m:
        continue
    name, cls, eff = m.group(2), m.group(3), m.group(4)
    p1, p2 = int(m.group(5)), int(m.group(6))
    desc = m.group(8)
    if eff not in V:
        unruled.add(eff); continue
    ddesc, vfun, pkey, inv = V[eff]
    val = vfun(p1, p2)
    prob = P(pkey)
    earn = round(val * prob, 1)
    ratio = (round(earn / inv, 1) if inv > 0 else None)
    rows.append({"name": name, "cls": {"GONG":"攻","SHOU":"守","MOU":"谋","DING":"鼎"}[cls],
                 "desc": desc, "v": round(val, 1), "prob": round(prob, 3), "e": earn,
                 "inv": inv, "ratio": ratio, "grade": grade_earn(earn)})
rows.sort(key=lambda x: (-x["e"], x["name"]))

dist = {}
for x in rows: dist[x["grade"]] = dist.get(x["grade"], 0) + 1

o = []
o.append("# 来古牌 · 卡牌收益评级模型")
o.append("")
o.append("> 生成脚本：`docs/工具/收益评级脚本.py`（可复现）。数据源：`DuelCardCatalog` 79 张卡。")
o.append("> **只评触发后的收益价值**，与「触发条件难度」（见 `docs/触发条件难度模型.md`）分开评估。")
o.append("")
o.append("## 一、模型")
o.append("")
o.append("每张卡的效果按典型对局场景量化：`V`=触发收益价值，`P`=触发概率，`E = P×V`=期望收益，`I`=资源投入（本卡吃骰数），`性价比 = E/I`。")
o.append("")
o.append("| 符号 | 含义 |")
o.append("|---|---|")
o.append("| V 收益价值 | 触发后该效果换算的分值：额外分+X→X · 基础分+X→X×倍率 · 倍率+Y→基础分×Y · 抽Z张→Z×8 · 回AP→8 · 骰+N→N×5 |")
o.append("| P 触发概率 | 骰型用 6 面骰精确枚举（k=5）；状态/对位用经验概率（上轮输赢0.5、上轮平0.1、金质≥1 0.3 等） |")
o.append("| E 期望收益 | `P×V`，核心评级指标 |")
o.append("| I 资源投入 | 吃骰卡=2（本卡典型骰数）、充能/单骰=1、纯自动/引擎=0 |")
o.append("| 性价比 | `E/I`，每投入 1 颗骰获得的期望分 |")
o.append("")
o.append("**价值基准**（典型对局，脚本顶部常量可调）：基础分 B=25 · 倍率 m=2 · 场上4卡 · 同职业2 · 同朝代3 · 金质0.5 · 手牌3 · 未布置骰1.5 · 共享池10 · 卡组未抽6 · 卡牌8分 · AP8分 · 骰5分")
o.append("")
o.append("**收益等级**（按 E）：微薄 <5 · 低 5–12 · 中 12–25 · 高 25–60 · 极高 ≥60")
o.append("")
o.append("## 二、收益分布")
o.append("")
o.append("| 等级 | 数量 | 含义 |")
o.append("|---|---|---|")
o.append(f"| 微薄 | {dist.get('微薄',0)} | 几乎赚不到（多为稀有骰型，触发期望极低） |")
o.append(f"| 低 | {dist.get('低',0)} | 少量稳定收益或中等概率的较小收益 |")
o.append(f"| 中 | {dist.get('中',0)} | 稳定吃骰输出或资源引擎 |")
o.append(f"| 高 | {dist.get('高',0)} | 高倍率/高额收益引擎 |")
o.append(f"| 极高 | {dist.get('极高',0)} | 吃手牌/吃骰的献祭爆发 |")
o.append("")
o.append("## 三、全卡收益表（按期望收益降序）")
o.append("")
o.append("| 卡牌 | 职业 | 效果 | V | P | E | I | 性价比 | 等级 |")
o.append("|---|---|---|---|---|---|---|---|---|")
for x in rows:
    ratio = f"{x['ratio']:.1f}/骰" if x["ratio"] is not None else "—"
    o.append(f"| {x['name']} | {x['cls']} | {x['desc']} | {x['v']:.0f} | {x['prob']:.3f} | **{x['e']:.1f}** | {x['inv']} | {ratio} | {x['grade']} |")
o.append("")
o.append("## 四、收益 × 难度 综合定位（结合触发条件难度模型）")
o.append("")
o.append("- **稳定型**：低难度 + 中高收益（吃骰输出 / 资源引擎）——性价比之王，卡组主力。")
o.append("- **爆发型**：高难度 + 高收益（献祭吃手牌/吃骰）——触发条件简单但代价大（消耗）。")
o.append("- **彩票型**：高难度 + 期望收益低（顺子/全奇/满堂等稀有骰型）——单次收益极高但期望垫底，赌上限。")
o.append("- **功能性**：收益不体现为分数（抽牌/回AP/抢骰），价值在资源与节奏。")
o.append("")
o.append("## 五、备注与假设")
o.append("")
o.append("- 所有收益为「典型场景」期望值；实际对局中倍率越高，倍率类卡的 V 越大，吃骰基础分越高。")
o.append("- 抽牌价值按 8 分/张估算（可部署为场上收益 + 配合握权/青花智/空手等），AP 同 8 分。")
o.append("- 金质引擎的 V 含「金质卡 0.5 张」期望；若卡组金质密度不同，`GOLD` 常量可调。")
o.append("- 收益等级是相对档位；脚本顶部常量全部可调，改后重跑即得新版本。")
o.append("")

open(OUT, "w", encoding="utf-8").write("\n".join(o))
print("生成完成：", len(rows), "张卡")
print("分布：", dict(sorted(dist.items())))
if unruled:
    print("未配置规则的效果类型：", sorted(unruled))
