# -*- coding: utf-8 -*-
"""
来古牌 · 难度-收益平衡校准（可复现）

把用户设计的「难度 ↔ 收益兑换曲线」落地为收益模型的平衡约束：
  难度 10 → 收益 10 · 难度 20 → 收益 25 · 难度 30 → 收益 30
  基础分收益 = 2（即倍率 m=2：1 点基础分吃倍率后值 2 分）

统一价值标尺（B=25, m=2）：
  1 额外分 = 1 分 · 1 基础分 = m = 2 分 · 1 倍率 = B = 25 分

对每张卡算「触发难度 D → 难度档位 → 收益配额 R」，再比对 V（单次收益）/ E（期望收益）
与配额，判定 超模 / 匹配 / 彩票 / 亏模：
  超模 E/R≥1.5 · 匹配 0.5≤E/R<1.5 · 彩票 E/R<0.5 但 V/R≥1 · 亏模 E/R<0.5 且 V/R<1
"""
import importlib.util

DIFF = r"d:\GuguFiles\AI测试项目\MC卡牌游戏开发\Laigu\docs\工具\难度评级脚本.py"
REW  = r"d:\GuguFiles\AI测试项目\MC卡牌游戏开发\Laigu\docs\工具\收益评级脚本.py"
OUT  = r"d:\GuguFiles\AI测试项目\MC卡牌游戏开发\Laigu\docs\难度-收益平衡校准.md"

def load(path, name):
    spec = importlib.util.spec_from_file_location(name, path)
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    return mod

# 加载两个已定稿模型（会顺带重写各自 md，幂等）
diff = load(DIFF, "laigu_diff")
rew  = load(REW,  "laigu_rew")

dm = {r["name"]: r for r in diff.rows}
rm = {r["name"]: r for r in rew.rows}
names = [r["name"] for r in rew.rows]
assert len(names) == len(set(names)) == 79, f"卡数量异常: {len(names)}"
assert not [n for n in names if n not in dm], "难度侧缺卡"

# ---- 常量（与收益脚本一致） ----
B, M = 25.0, 2.0
HAND, N, CARD_V, DIE_V = 3, 2, 8, 5

# ---- 难度档位 → 配额（兑换曲线） ----
def quota(d):
    if d < 0.12: return (10, 10)    # ★1~★2 白给/简单
    if d < 0.23: return (20, 25)    # ★3 中等
    return (30, 30)                 # ★4~★5 困难/极难

def verdict(m_e, m_v):
    if m_e >= 1.5: return "超模"
    if m_e >= 0.5: return "匹配"
    if m_v >= 1.0: return "彩票"   # 低概率但单次收益对得起难度
    return "亏模"

# 消耗卡机会成本（按 desc 判别的显性消耗；对手骰不计）
def opp_cost(desc):
    if "消耗" not in desc:
        return None
    c = 0
    if "手牌" in desc:
        c += HAND * CARD_V                    # 吃 3 手牌
    elif "对手" not in desc and ("每颗骰" in desc or "每有 1 颗骰" in desc):
        c += N * DIE_V                         # 吃本卡典型 2 骰
    return c

rows = []
for n in names:
    d = dm[n]["d"]; star = dm[n]["star"]; cls = dm[n]["cls"]
    v = rm[n]["v"]; e = rm[n]["e"]; inv = rm[n]["inv"]; desc = rm[n]["desc"]
    ratio = rm[n]["ratio"]
    lvl, R = quota(d)
    m_e, m_v = e / R, v / R
    vd = verdict(m_e, m_v)
    cost = opp_cost(desc)
    e_net = round(e - cost, 1) if cost is not None else None
    rows.append(dict(name=n, cls=cls, d=d, star=star, lvl=lvl, R=R,
                     v=v, e=e, me=m_e, mv=m_v, vd=vd, inv=inv,
                     ratio=ratio, cost=cost, e_net=e_net, desc=desc))

order = ["超模", "匹配", "彩票", "亏模"]
groups = {k: [r for r in rows if r["vd"] == k] for k in order}
for k in groups:
    groups[k].sort(key=lambda r: (-r["me"], r["name"]))

def fmt_r(r):
    ratio = f"{r['ratio']:.1f}" if r["ratio"] is not None else "—"
    c = f"{r['cost']:.0f}" if r["cost"] is not None else "—"
    en = f"{r['e_net']:.1f}" if r["e_net"] is not None else "—"
    return f"| {r['name']} | {r['cls']} | **{r['d']:.3f}** | {r['star']} | {r['lvl']} | {r['R']} | {r['v']:.0f} | {r['e']:.1f} | {r['me']:.2f} | {r['mv']:.2f} | {ratio} | {c} | {en} |"

o = []
o.append("# 来古牌 · 难度-收益平衡校准")
o.append("")
o.append("> 生成脚本：`docs/工具/平衡校准脚本.py`（可复现，复用难度+收益两模型数据）。")
o.append("> 兑换曲线（用户设计）：**难度10→收益10 · 难度20→收益25 · 难度30→收益30**；基础分收益=2（倍率 m=2）。")
o.append("")
o.append("## 一、统一价值标尺")
o.append("")
o.append("| 收益类型 | 1 点的分值 | 依据 |")
o.append("|---|---|---|")
o.append("| 额外分 +X | X 分 | 不吃倍率，1:1 |")
o.append("| 基础分 +X | X×m = 2X 分 | 吃倍率（m=2，用户基准「基础分收益=2」） |")
o.append("| 倍率 +Y | B×Y = 25Y 分 | 作用于全场典型基础分 B=25 |")
o.append("")
o.append("基础分收益=2 基准档的等价换算：**基础分 +2 ＝ 额外分 +4 ＝ 倍率 +0.16**。")
o.append("")
o.append("## 二、每档标准数值配方（B=25, m=2）")
o.append("")
o.append("| 难度档 | 覆盖星级 | 收益配额 R | 额外分 | 基础分 | 倍率 |")
o.append("|---|---|---|---|---|---|")
o.append("| 10 | ★1~★2（D<0.12） | 10 | +10 | +5 | +0.4 |")
o.append("| 20 | ★3（0.12≤D<0.23） | 25 | +25 | +12.5 | **+1** |")
o.append("| 30 | ★4~★5（D≥0.23） | 30 | +30 | +15 | +1.2 |")
o.append("")
o.append("> 关键锚点：**+1 倍率 = 25 分 = 难度20 档配额**，B=25 与兑换曲线自洽，维持不变。")
o.append("")
o.append("## 三、匹配审计总览（79 张）")
o.append("")
tot = {k: len(groups[k]) for k in order}
o.append(f"| 判定 | 数量 | 占比 | 含义 |")
o.append(f"|---|---|---|---|")
o.append(f"| 超模 | {tot['超模']} | {tot['超模']/79:.0%} | 期望收益 ≥1.5×配额：收益配不上难度，需削数值或提高难度 |")
o.append(f"| 匹配 | {tot['匹配']} | {tot['匹配']/79:.0%} | 期望收益在 0.5~1.5×配额：设计合理 |")
o.append(f"| 彩票 | {tot['彩票']} | {tot['彩票']/79:.0%} | 低概率但单次收益对得起难度：赌上限（稀有骰型/平局补偿） |")
o.append(f"| 亏模 | {tot['亏模']} | {tot['亏模']/79:.0%} | 单次、期望都低于配额：弱卡，可增强 |")
o.append("")
# 每档配额下的判定分布
o.append("| 难度档 | 超模 | 匹配 | 彩票 | 亏模 | 小计 |")
o.append("|---|---|---|---|---|---|")
for lvl in (10, 20, 30):
    g = [r for r in rows if r["lvl"] == lvl]
    cnt = {k: sum(1 for r in g if r["vd"] == k) for k in order}
    o.append(f"| {lvl} | {cnt['超模']} | {cnt['匹配']} | {cnt['彩票']} | {cnt['亏模']} | {len(g)} |")
o.append("")
o.append("## 四、全卡校准表（按判定分组）")
o.append("")
o.append("列：难度D·星级·档位·配额R·V单次·E期望·E/R·V/R·性价比·机会成本·净收益E_net")
o.append("")
for k in order:
    g = groups[k]
    o.append(f"### {k}（{len(g)}）")
    o.append("")
    o.append("| 卡牌 | 职业 | D | 星级 | 档 | R | V | E | E/R | V/R | 性价比 | 成本 | E_net |")
    o.append("|---|---|---|---|---|---|---|---|---|---|---|---|---|")
    for r in g:
        o.append(fmt_r(r))
    o.append("")
o.append("## 五、消耗卡机会成本修正")
o.append("")
o.append("【消耗】卡在收益脚本里 `I=0`（未扣机会成本），这里按显性消耗修正：吃手牌 3×8=24、吃本卡典型 2 骰=10、对手骰不计。净收益 E_net = E − 成本：")
o.append("")
o.append("- 青花智 150 → **126**（吃 3 手牌 24）：仍是最强，但不像表面 15 倍超模那么离谱")
o.append("- 破门 100 → **90**（吃 2 骰 10）")
o.append("- 号角 40 → 40（吃对手骰，自损 0）")
o.append("- 倍增 25 → 25 · 重钟 20 → 20（无显性消耗）")
o.append("- 天火贯日 E=1.5 → **−8.5**（顺子概率 3.1%，每次触发还烧 2 骰，纯负期望彩票）")
o.append("")
o.append("## 六、校准结论与再平衡建议")
o.append("")
o.append("**价值基准**：B=25、m=2 与兑换曲线自洽（+1倍率=25分=难度20配额），确认保留。")
o.append("")
o.append("**超模主因**：无条件/低难度「计数引擎」给了高倍率（每手牌/每骰/每池 +倍率），配额档位却按 D=0 落在难度10。再平衡两选一：① 提高难度（加【消耗】/站位/骰型条件）② 削减倍率档位（如每手牌 +1 倍率降为 +0.5）。")
o.append("")
o.append("**难度低估**：触发难度模型的 f_M 把一切【消耗】统一记 0.4，未区分消耗代价——青花智吃 3 手牌（24 分机会成本）却只评★1、D=0.04，难度被低估导致收益看着 15 倍超模。建议 f_M 分级：仅离场 0.4 · 吃骰 0.45 · 吃手牌 0.5，并同步把收益脚本的 `I` 纳入机会成本。")
o.append("")
o.append("**彩票主因**：全盘稀有骰型（顺子/全奇/全偶/满堂/全同）单次给 +3~+5 倍率，P 低到 E 垫底。若保留「赌上限」定位可不动；若要纳入配额曲线，需把收益提高到 E≈30 的水平（+8~+10 倍率）或放宽概率（顺子降为 3 连）。")
o.append("")
o.append("**亏模主因**：无条件小额收益（金辉/金铢/存骰）与自伤卡（天球仪/僵局）。建议上调基础数值或改判为功能性（资源价值不体现在分数）。")
o.append("")
o.append("**阈值建议**：收益等级仍按 E 绝对值分档，但语义与配额对齐——档10≈微薄~低、档20≈中、档30≈高；「极高」只应出现在吃手牌/吃骰的献祭型或高倍率彩票型。")
o.append("")

open(OUT, "w", encoding="utf-8").write("\n".join(o))
print("生成完成：", len(rows), "张卡")
print("判定分布：", tot)
