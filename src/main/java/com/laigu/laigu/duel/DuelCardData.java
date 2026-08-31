package com.laigu.laigu.duel;

import com.laigu.laigu.card.CardInfo;
import com.laigu.laigu.util.CardNbt;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 一张对战卡的全部静态数据（来古牌 ↔ 对战效果 的「索引连接」）。
 * <p>
 * 对战卡就是来古牌（文物牌）：本类只描述「某张文物牌 → 对战效果」的映射。
 * 因此不存朝代/金质 —— 朝代取自文物元数据（{@link CardInfo}），金质取自物品
 * id 稀有度后缀（{@code _common/_gold}）。效果归谁随时可改，改这个文件即可。
 * <p>
 * 金卡规则：金卡数值 = 白卡 ×2（触发条件不变，见 {@link #p1For}/{@link #p2For}），
 * 部分金卡另有特殊效果（{@link #goldEffect}，仅金卡在结算阶段额外触发一次）。
 */
public class DuelCardData
{
    public final String cardId;         // 文物牌 id（如 tong_che_ma），对应 <id>_common / <id>_gold 两个物品
    public final String name;           // 对战效果名（展示用，如「炎刃」）
    public final CardClass cls;         // 职业
    public final EffectType effect;     // 效果类型
    public final int p1;                // 主数值（倍率/额外/基础）
    public final int p2;                // 副数值（每张卡/每张条件 的加值）
    public final String targetDynasty;  // 朝代条件目标（DYN_CNT_BASE 用，取值见 CardInfo 朝代）
    public final CardClass targetClass; // 职业条件目标（CLASS_CNT_MULT/CLASS_CNT_BASE 用）
    public final int charge;            // 【充能】：>0 时本卡上需有 ≥1 颗骰才发挥后续效果；=x 时按 x 次重复
    public final String desc;           // 效果描述（写入 tooltip）

    /** 金卡特殊效果（部分金卡配置）：金卡在结算阶段额外触发一次；白卡永不触发。null = 无。 */
    public final EffectType goldEffect;
    public final int goldP1;
    public final int goldP2;
    public final String goldDesc;

    /** 【激活x】目标卡：被「激活左侧」效果激活；达到 cap 后触发奖励并清零。cap=0 表示不是激活目标。 */
    public final int activateCap;
    /** 激活达到 cap 时的奖励效果（复用计分类效果，累加额外/基础/倍率）。null = 无。 */
    public final EffectType activateReward;
    public final int activateP1;
    /** 金卡独立激活奖励；旧目录未配置时按普通奖励兼容。 */
    public EffectType goldActivateReward;
    public int goldActivateP1;
    public int goldLeaveMultiplier;
    public boolean goldOverflowMultiplier;
    public int goldOverflowBaseBonus;
    public boolean goldSummonDraw;
    public int goldSummonDrawAmount;
    public boolean goldSummonActivateAll;
    public int goldSummonActivateTimes;
    public boolean goldAmbushMinDestroy;
    public boolean goldAmbushCopyDice;
    public boolean goldAmbushFiveBonus;
    public boolean goldSettlementEffect;
    public int goldLeftActivationsPerDie = 1;
    public int goldRightStraightActivations;
    public int goldActivateAllCount = 1;
    public int goldActivationFailureBonus;
    public boolean goldDynastyOverride;
    public boolean goldAdjacentHalve;
    public boolean goldGapStraight;
    public int goldRightCountMultiplier = 1;
    public boolean goldActivateAll;
    public int goldActivateAllTimes;
    public int goldActionExtra;

    /** 【破阵】固有词条后续效果：本槽位骰面和 > 对手同槽位时触发（累加对应分量）。null = 无。 */
    public EffectType poZhenReward;
    public int poZhenP1;
    /** 【破阵】金卡焕章：触发破阵时改为削弱 100%（否则 50%）。 */
    public boolean poZhenFullHalve;
    /** 【伏击】睡莲：成功时将对手该槽位卡牌的前 N 颗骰无效化（金卡×2）。 */
    public int fuJiInvalidate;
    /** 【伏击】百花（普卡基础）：成功时对位获得基础分时，我方也获得一半基础分。 */
    public boolean fuJiMirrorBase;
    /** 【伏击】百花焕章（金卡）：成功时对位获得倍率时，我方也获得一半倍率。 */
    public boolean fuJiMirrorMult;
    /** 【伏击】溪山焕章：每次激活触发 +reserve 额外分。 */
    public int fuJiActivateBonus;
    /** 【激活】永固杯焕章：激活达成时，激活左侧卡牌。 */
    public boolean activateLeftOnReach;
    /** 【激活左】海错焕章：触发激活时，右侧相邻卡也 +1 进度。 */
    public boolean goldRightActivate;
    /** 【激活左】浑天焕章：若牌型为顺子，激活右侧卡牌 N 次。 */
    public int activateRightOnStraight;
    /** 【激活左】朝代联动：触发右侧卡牌 x 次，x = 我方场上最多的朝代数（只算有卡的朝代）。 */
    public boolean activateRightByDynastyMax;
    /** 【伏击】固有词条：对位有牌(成功) / 无牌(失败) 各自的后续效果。null = 无。 */
    public EffectType fuJiSuccReward;
    public int fuJiSuccP1;
    public EffectType fuJiFailReward;
    public int fuJiFailP1;

    /** 组合式模型：基础效果仍复用 effect/p1/p2；附加触发器统一承载激活、伏击等机制。 */
    public final EffectDefinition baseEffect;
    public List<EffectTriggerDefinition> triggers;
    /** 金卡独立主效果配置；目录必须显式指定，未指定时仅兼容旧数据。 */
    private CardVariant goldVariant;
    private boolean goldVariantExplicit;

    /** 显式配置金卡主效果。参数均为金卡最终值，不再自动翻倍。 */
    public DuelCardData goldMain(EffectType goldEffect, int goldP1, int goldP2,
                                 String goldTargetDynasty, CardClass goldTargetClass,
                                 int goldCharge, String goldDesc)
    {
        this.goldVariant = new CardVariant(goldEffect, goldP1, goldP2,
                goldTargetDynasty, goldTargetClass, goldCharge, goldDesc);
        this.goldVariantExplicit = true;
        return this;
    }

    public DuelCardData goldActivation(EffectType reward, int value)
    {
        this.goldActivateReward = reward;
        this.goldActivateP1 = value;
        return this;
    }

    public DuelCardData goldLeaveMultiplier(int value)
    {
        this.goldLeaveMultiplier = value;
        return this;
    }

    public DuelCardData goldOverflowMultiplier(int value)
    {
        this.goldOverflowMultiplier = true;
        this.goldOverflowBaseBonus = value;
        return this;
    }

    public DuelCardData goldSummonDraw(int amount)
    {
        this.goldSummonDraw = true;
        this.goldSummonDrawAmount = amount;
        return this;
    }

    public DuelCardData goldSummonActivateAll(int times)
    {
        this.goldSummonActivateAll = true;
        this.goldSummonActivateTimes = times;
        return this;
    }

    public DuelCardData goldAmbushMinDestroy()
    {
        this.goldAmbushMinDestroy = true;
        return this;
    }

    public DuelCardData goldAmbushCopyDice()
    {
        this.goldAmbushCopyDice = true;
        return this;
    }

    public DuelCardData goldAmbushFiveBonus()
    {
        this.goldAmbushFiveBonus = true;
        return this;
    }

    /** 兼容旧目录的别名；新配置请使用 goldMain。 */
    public DuelCardData goldVariant(EffectType goldEffect, int goldP1, int goldP2, int goldCharge, String goldDesc)
    {
        return goldMain(goldEffect, goldP1, goldP2, targetDynasty, targetClass, goldCharge, goldDesc);
    }

    public CardVariant variant(ItemStack stack)
    {
        return isGold(stack) && goldVariant != null ? goldVariant : new CardVariant(effect, p1, p2,
                targetDynasty, targetClass, charge, desc);
    }

    public EffectType effectFor(ItemStack stack) { return variant(stack).effect(); }
    public int chargeFor(ItemStack stack) { return variant(stack).charge(); }
    public String targetDynastyFor(ItemStack stack) { return variant(stack).targetDynasty(); }
    public CardClass targetClassFor(ItemStack stack) { return variant(stack).targetClass(); }
    public String descriptionFor(ItemStack stack) { return variant(stack).description(); }


    public DuelCardData(String cardId, String name, CardClass cls,
                        EffectType effect, int p1, int p2,
                        String targetDynasty, CardClass targetClass, String desc)
    {
        this(cardId, name, cls, effect, p1, p2, targetDynasty, targetClass, 0, desc,
                null, 0, 0, null, 0, null, 0);
    }

    public DuelCardData(String cardId, String name, CardClass cls,
                        EffectType effect, int p1, int p2,
                        String targetDynasty, CardClass targetClass, int charge, String desc)
    {
        this(cardId, name, cls, effect, p1, p2, targetDynasty, targetClass, charge, desc,
                null, 0, 0, null, 0, null, 0);
    }

    public DuelCardData(String cardId, String name, CardClass cls,
                        EffectType effect, int p1, int p2,
                        String targetDynasty, CardClass targetClass, int charge, String desc,
                        EffectType goldEffect, int goldP1, int goldP2, String goldDesc)
    {
        this(cardId, name, cls, effect, p1, p2, targetDynasty, targetClass, charge, desc,
                goldEffect, goldP1, goldP2, goldDesc, 0, null, 0);
    }

    /** 完整构造（含【激活x】目标配置）。激活奖励效果复用计分类效果（累加额外/基础/倍率）。 */
    public DuelCardData(String cardId, String name, CardClass cls,
                        EffectType effect, int p1, int p2,
                        String targetDynasty, CardClass targetClass, int charge, String desc,
                        EffectType goldEffect, int goldP1, int goldP2, String goldDesc,
                        int activateCap, EffectType activateReward, int activateP1)
    {
        this.cardId = cardId;
        this.name = name;
        this.cls = cls;
        this.effect = effect;
        this.p1 = p1;
        this.p2 = p2;
        this.targetDynasty = targetDynasty;
        this.targetClass = targetClass;
        this.charge = charge;
        this.desc = desc;
        this.goldEffect = goldEffect;
        this.goldP1 = goldP1;
        this.goldP2 = goldP2;
        this.goldDesc = goldDesc;
        this.activateCap = activateCap;
        this.activateReward = activateReward;
        this.activateP1 = activateP1;
        this.goldActivateReward = activateReward;
        this.goldActivateP1 = activateP1;
        this.baseEffect = new EffectDefinition(effect, p1, p2);
        // 金卡主效果始终保留普通卡的效果类型；goldEffect 只表示焕章追加效果。
        // 这两个概念必须分离，否则焕章会错误地覆盖金卡主效果。
        // 金卡不再由普通卡自动翻倍。未显式配置时仅使用原值兼容旧目录，目录迁移完成前不会偷偷改变语义。
        // 未迁移的旧目录保留原有兼容规则；goldMain 显式配置后覆盖为最终值且不再缩放。
        this.goldVariant = new CardVariant(effect,
                goldValue(effect, p1, 0), goldValue(effect, p2, 1),
                targetDynasty, targetClass, charge, goldDescription(desc, effect, p1, p2));
        rebuildTriggers();
    }

    private static int goldValue(EffectType type, int value, int index)
    {
        return valueSpec(type, index).goldScale() == GoldScale.DOUBLE ? value * 2 : value;
    }

    private static String goldDescription(String source, EffectType type, int p1, int p2)
    {
        return scaleDescription(source, type, p1, p2);
    }

    private void rebuildTriggers()
    {
        List<EffectTriggerDefinition> triggerDefs = new ArrayList<>();
        if (activateCap > 0 && activateReward != null)
        {
            triggerDefs.add(EffectTriggerDefinition.activation(activateCap,
                    EffectDefinition.of(activateReward, activateP1)));
        }
        if (effect == EffectType.FUJI)
        {
            if (fuJiSuccReward != null) triggerDefs.add(EffectTriggerDefinition.ambushSuccess(
                    EffectDefinition.of(fuJiSuccReward, fuJiSuccP1)));
            if (fuJiFailReward != null) triggerDefs.add(EffectTriggerDefinition.ambushFail(
                    EffectDefinition.of(fuJiFailReward, fuJiFailP1)));
        }
        if (effect == EffectType.PO_ZHEN_HALVE && poZhenReward != null)
        {
            triggerDefs.add(EffectTriggerDefinition.poZhen(
                    new EffectDefinition(poZhenReward, poZhenP1, 0)));
        }
        this.triggers = List.copyOf(triggerDefs);
    }

    public List<EffectDefinition> effectsFor(EffectTrigger trigger)
    {
        for (EffectTriggerDefinition definition : triggers)
            if (definition.trigger() == trigger) return definition.effects();
        return List.of();
    }

    public List<ResolutionModifier> modifiersFor(EffectTrigger trigger)
    {
        return modifiersFor(trigger, null);
    }

    public List<ResolutionModifier> modifiersFor(EffectTrigger trigger, ItemStack stack)
    {
        if (trigger == EffectTrigger.PO_ZHEN && effect == EffectType.PO_ZHEN_HALVE)
        {
            boolean gold = stack != null && isGold(stack);
            int amount = gold && goldVariantExplicit ? 100 : (poZhenFullHalve ? 100 : 50);
            return List.of(new ResolutionModifier(ResolutionModifier.ModifierType.REDUCE_OPPONENT_CONTRIBUTION,
                    amount, 3));
        }
        return List.of();
    }

    /** 参数在某效果中承担的业务角色，用于 JSON 配置迁移和金卡缩放校验。 */
    public enum ValueRole { NONE, REWARD, THRESHOLD, COUNT, FLAG }

    /** 参数的金卡缩放策略。 */
    public enum GoldScale { DOUBLE, RAW }

    /** 主/副参数的语义和缩放策略。 */
    public record ValueSpec(ValueRole role, GoldScale goldScale) {}

    private static final Map<EffectType, ValueSpec[]> VALUE_SPECS = new EnumMap<>(EffectType.class);

    static
    {
        for (EffectType type : EffectType.values()) VALUE_SPECS.put(type, specs(ValueRole.REWARD, GoldScale.DOUBLE, ValueRole.NONE, GoldScale.RAW));

        rawP1(EffectType.BASE_DOUBLE_CONSUME, EffectType.CONSUME_EXTRA_DOUBLE);
        rawBoth(EffectType.SUMMON_DRAW, EffectType.SUMMON_DRAW_IF_LOST_LAST, EffectType.SUMMON_RESTORE_AP,
                EffectType.LEAVE_DRAW, EffectType.ROUND_START_DRAW, EffectType.ROUND_START_DRAW_STAY_TURNS,
                EffectType.OTHER_USE_DRAW, EffectType.ROUND_END_DRAW_IF_WIN, EffectType.ROUND_END_DRAW_IF_LOSE,
                EffectType.DRAFT_POOL_UP, EffectType.REROLL_ON_DRAFT, EffectType.ACTIVATE_LEFT, EffectType.FUJI,
                 EffectType.COPY_CURRENT_BASE_TO_EXTRA, EffectType.ANY_FRIENDLY_ACTIVATE_EXTRA, EffectType.ACTIVATION_FAILED_EXTRA,
                 EffectType.SHARED_POOL_SUM_EXTRA, EffectType.FLAT_BASE, EffectType.OPP_EMPTY_CARD_MULT,
                EffectType.PLACE_SCORE_EXTRA, EffectType.ROUND_START_SCORE_EXTRA,
                EffectType.USE_HAND_SCORE_EXTRA, EffectType.ROUND_END_SCORE_EXTRA);
        set(EffectType.DRAFT_TURNS_DOWN, ValueRole.COUNT, GoldScale.RAW, ValueRole.NONE, GoldScale.RAW);
        set(EffectType.DRAFT_TURNS_UP, ValueRole.COUNT, GoldScale.RAW, ValueRole.NONE, GoldScale.RAW);
        set(EffectType.DRAFT_SELF_TURNS_UP, ValueRole.COUNT, GoldScale.RAW, ValueRole.NONE, GoldScale.RAW);
        set(EffectType.DRAFT_OPP_TURNS_DOWN, ValueRole.COUNT, GoldScale.RAW, ValueRole.NONE, GoldScale.RAW);
        set(EffectType.DRAFT_SELF_GRAB_UP, ValueRole.COUNT, GoldScale.RAW, ValueRole.NONE, GoldScale.RAW);
        set(EffectType.FUJI_FAIL_EXTRA, ValueRole.REWARD, GoldScale.DOUBLE, ValueRole.NONE, GoldScale.RAW);
        set(EffectType.DRAFT_SCORE_EXTRA, ValueRole.REWARD, GoldScale.DOUBLE, ValueRole.NONE, GoldScale.RAW);
        set(EffectType.DIE_SUM_GE_EXTRA, ValueRole.THRESHOLD, GoldScale.RAW, ValueRole.REWARD, GoldScale.DOUBLE);
        set(EffectType.DIE_SUM_GE_MULT, ValueRole.THRESHOLD, GoldScale.RAW, ValueRole.REWARD, GoldScale.DOUBLE);
        set(EffectType.DYN_CNT_BASE, ValueRole.NONE, GoldScale.RAW, ValueRole.REWARD, GoldScale.DOUBLE);
        set(EffectType.CLASS_CNT_MULT, ValueRole.NONE, GoldScale.RAW, ValueRole.REWARD, GoldScale.DOUBLE);
        set(EffectType.CLASS_CNT_BASE, ValueRole.NONE, GoldScale.RAW, ValueRole.REWARD, GoldScale.DOUBLE);
        set(EffectType.ISOLATED_MULT_EXTRA, ValueRole.REWARD, GoldScale.DOUBLE, ValueRole.REWARD, GoldScale.DOUBLE);
        set(EffectType.DIE_FIRST_BONUS, ValueRole.REWARD, GoldScale.DOUBLE, ValueRole.REWARD, GoldScale.DOUBLE);
    }

    private static ValueSpec[] specs(ValueRole p1Role, GoldScale p1Scale, ValueRole p2Role, GoldScale p2Scale)
    {
        return new ValueSpec[] { new ValueSpec(p1Role, p1Scale), new ValueSpec(p2Role, p2Scale) };
    }

    private static void set(EffectType type, ValueRole p1Role, GoldScale p1Scale, ValueRole p2Role, GoldScale p2Scale)
    {
        VALUE_SPECS.put(type, specs(p1Role, p1Scale, p2Role, p2Scale));
    }

    private static void rawP1(EffectType... types)
    {
        for (EffectType type : types) set(type, ValueRole.FLAG, GoldScale.RAW, ValueRole.NONE, GoldScale.RAW);
    }

    private static void rawBoth(EffectType... types)
    {
        for (EffectType type : types) set(type, ValueRole.COUNT, GoldScale.RAW, ValueRole.NONE, GoldScale.RAW);
    }

    /** 读取某效果参数的语义规则。没有登记的效果会在这里直接失败，禁止隐式默认翻倍。 */
    public static ValueSpec valueSpec(EffectType type, int parameterIndex)
    {
        ValueSpec[] specs = VALUE_SPECS.get(type);
        if (specs == null || parameterIndex < 0 || parameterIndex >= specs.length)
        {
            throw new IllegalStateException("缺少效果参数规则：" + type + " / p" + (parameterIndex + 1));
        }
        return specs[parameterIndex];
    }

    /** 金卡读取独立配置中的主数值；金卡最终值只在配置构建时确定一次。 */
    public int p1For(ItemStack stack)
    {
        return variant(stack).p1();
    }

    /** 金卡读取独立配置中的副数值；金卡最终值只在配置构建时确定一次。 */
    public int p2For(ItemStack stack)
    {
        return variant(stack).p2();
    }

    /** 按效果参数语义取得指定稀有度的最终值。激活奖励等独立触发器也必须使用此入口。 */
    public static int goldValueFor(EffectType type, int value, int parameterIndex, ItemStack stack)
    {
        // 目录中的焕章参数也是金卡最终值；显式配置禁止再次自动翻倍。
        return value;
    }

    private static int scaledValue(ItemStack stack, int value, ValueSpec spec)
    {
        return isGold(stack) && spec.goldScale() == GoldScale.DOUBLE ? value * 2 : value;
    }

    /** 金卡描述来自独立配置，普通卡描述保持原样。 */
    public String descFor(ItemStack stack)
    {
        return variant(stack).description();
    }

    public String goldDescFor(ItemStack stack)
    {
        return goldDesc == null ? "焕章：无" : goldDesc;
    }

    public String activationDescFor(ItemStack stack)
    {
        return descFor(stack);
    }

    private static String scaleDescription(String source, EffectType type, int value1, int value2)
    {
        if (source == null || type == null) return source;
        ValueSpec p1Spec = valueSpec(type, 0);
        ValueSpec p2Spec = valueSpec(type, 1);
        String pat = "(?<![\\d\\p{IsHan}])(" + value1 + "|" + value2 + ")(?![\\d\\p{IsHan}])";
        Matcher m = Pattern.compile(pat).matcher(source);
        StringBuilder sb = new StringBuilder();
        while (m.find())
        {
            int value = Integer.parseInt(m.group(1));
            if (value == value1 && p1Spec.goldScale() == GoldScale.DOUBLE) value *= 2;
            else if (value == value2 && p2Spec.goldScale() == GoldScale.DOUBLE) value *= 2;
            m.appendReplacement(sb, String.valueOf(value));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    /** 是否带【充能】关键词（需要本卡上有骰子才发挥效果；充能x/充能N 皆属）。 */
    public boolean isCharged()
    {
        return charge != 0;
    }

    /** 这张卡是金质（物品 id 以 _gold 结尾）。 */
    public static boolean isGold(ItemStack stack)
    {
        String path = CardNbt.pathOf(stack);
        return "gold".equals(CardNbt.rarityOfPath(path));
    }

    /** 这张卡所属朝代（取自文物元数据）。 */
    public static String dynastyOf(ItemStack stack)
    {
        String path = CardNbt.pathOf(stack);
        return CardInfo.dynastyOf(CardNbt.stripRaritySuffix(path));
    }

    /** 是否需要 targetDynasty/targetClass 参数（展示用辅助）。 */
    public boolean usesTarget()
    {
        return targetDynasty != null || targetClass != null;
    }

    /** 该效果是否属于「基础分」类（词条后续效果累计到基础分）。 */
    public static boolean isBaseReward(EffectType e)
    {
        return switch (e)
        {
            case PER_DIE_BASE, DYN_CNT_BASE, CLASS_CNT_BASE, CARD_CNT_BASE, POOL_CNT_BASE,
                 DECK_CNT_BASE, GOLD_CNT_BASE, FLAT_BASE -> true;
            default -> false;
        };
    }

    /** 该效果是否属于「倍率」类（词条后续效果累计到倍率加成）。 */
    public static boolean isMultReward(EffectType e)
    {
        return switch (e)
        {
            case FLAT_MULT, PER_DIE_MULT, STRAIGHT_MULT, ALL_ODD_MULT, NEIGHBOR_MULT, GOLD_CNT_MULT, HAND_CNT_MULT,
                 CLASS_CNT_MULT, POOL_CNT_MULT, GOLD_DIE_MULT, GOLD_DYN_MULT, SAME_FACE_MULT, ALL_HIGH_MULT,
                 WIN_LAST_MULT, CENTER_MULT, ADJ_SAME_CLASS_MULT, OPP_EMPTY_CARD_MULT -> true;
            default -> false;
        };
    }

    /** 链式设置【破阵】金卡焕章：触发破阵时改为削弱 100%。 */
    public DuelCardData poZhenFull()
    {
        this.poZhenFullHalve = true;
        return this;
    }

    /** 链式设置【伏击】睡莲：成功时将对位卡牌前 N 颗骰无效化（金卡×2）。 */
    public DuelCardData fuJiInvalidate(int n)
    {
        this.fuJiInvalidate = n;
        return this;
    }

    /** 链式设置【伏击】百花：成功时镜像对位基础分(mirrorBase) / 倍率(mirrorMult, 金卡焕章)。 */
    public DuelCardData fuJiMirror(boolean mirrorBase, boolean mirrorMult)
    {
        this.fuJiMirrorBase = mirrorBase;
        this.fuJiMirrorMult = mirrorMult;
        return this;
    }

    /** 链式设置【激活左】溪山焕章：每次激活触发额外 +N 额外分。 */
    public DuelCardData fuJiActivateBonus(int n)
    {
        this.fuJiActivateBonus = n;
        return this;
    }

    /** 链式设置【激活】永固杯焕章：激活达成时激活左侧卡牌。 */
    public DuelCardData activateLeftOnReach()
    {
        this.activateLeftOnReach = true;
        return this;
    }

    /** 链式设置【激活左】朝代联动：触发右侧 x 次（x=我方场上最多朝代数）。 */
    public DuelCardData activateRightByDynastyMax()
    {
        this.activateRightByDynastyMax = true;
        return this;
    }

    /** 链式设置【激活左】海错焕章：触发激活时右侧相邻卡也 +1 进度。 */
    public DuelCardData goldRightActivate()
    {
        this.goldRightActivate = true;
        return this;
    }

    public DuelCardData goldActivateAll(int times)
    {
        this.goldActivateAll = true;
        this.goldActivateAllTimes = times;
        return this;
    }

    public DuelCardData goldGapStraight()
    {
        this.goldGapStraight = true;
        return this;
    }

    public DuelCardData goldFiveDiceBonus()
    {
        this.goldAmbushFiveBonus = true;
        return this;
    }

    public DuelCardData goldDynastyOverride()
    {
        this.goldDynastyOverride = true;
        return this;
    }

    public DuelCardData goldLeftActivations(int timesPerDie)
    {
        this.goldLeftActivationsPerDie = timesPerDie;
        return this;
    }

    public DuelCardData goldActionExtra(int value)
    {
        this.goldActionExtra = value;
        return this;
    }

    public DuelCardData goldSettlement()
    {
        this.goldSettlementEffect = true;
        return this;
    }

    /** 链式设置【激活左】浑天焕章：若牌型为顺子，激活右侧卡牌 N 次。 */
    public DuelCardData activateRightOnStraight(int n)
    {
        this.activateRightOnStraight = n;
        return this;
    }

    public DuelCardData goldRightStraightActivations(int n)
    {
        this.goldRightStraightActivations = n;
        return this;
    }

    /** 链式设置【破阵】后续效果：本槽位骰面和 > 对手同槽位时触发（reward：基础/倍率/额外类；基数=本卡骰数）。 */
    public DuelCardData poZhen(EffectType reward, int p)
    {
        this.poZhenReward = reward;
        this.poZhenP1 = p;
        rebuildTriggers();
        return this;
    }

    /** 链式设置【伏击】成功/失败后续效果（基数=本卡骰数）。 */
    public DuelCardData fuJi(EffectType succReward, int succP, EffectType failReward, int failP)
    {
        this.fuJiSuccReward = succReward;
        this.fuJiSuccP1 = succP;
        this.fuJiFailReward = failReward;
        this.fuJiFailP1 = failP;
        rebuildTriggers();
        return this;
    }
}
