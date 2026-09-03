package com.laigu.laigu.duel.newcard;

import com.laigu.laigu.card.CardCatalog;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 阶段九注册和完整性校验：独立类、独立变体、真实文物名称与拼音类名。 */
public final class CardRegistryValidator
{
    private CardRegistryValidator() {}

    /** 已注册卡牌 ID（应为 158 = 79 文物 × 2 变体）。 */
    public static Set<String> registeredIds() { return CardFactory.registeredIds(); }

    /**
     * displayName 校验：必须使用 ArtifactCardNames 给出的实际文物名
     * （禁止“炎刃”“千钧”等抽象效果名）。
     */
    public static List<String> invalidNames()
    {
        List<String> errors = new ArrayList<>();
        for (String id : CardFactory.registeredIds())
        {
            DuelCard card = CardFactory.create(id);
            String suffix = card.rarity() == CardRarity.GOLD ? "_gold" : "_common";
            String artifactId = id.substring(0, id.length() - suffix.length());
            if (!CardCatalog.CARD_IDS.contains(artifactId)) continue; // 目录外 ID 由 suffix/目录校验负责
            String expected = ArtifactCardNames.variantName(artifactId, card.rarity());
            if (!expected.equals(card.displayName()))
                errors.add(id + " 必须使用实际文物名“" + expected + "”，当前为“" + card.displayName() + "”");
        }
        return List.copyOf(errors);
    }

    /** 阶段13：卡面描述非空校验——每张注册卡必须有主描述；金卡必须有焕章行。 */
    public static List<String> emptyDescriptions()
    {
        List<String> errors = new ArrayList<>();
        for (String id : CardFactory.registeredIds())
        {
            DuelCard card = CardFactory.create(id);
            if (card.description() == null || card.description().isEmpty())
                errors.add(id + " 缺少卡面主描述");
            if (id.endsWith("_gold") && (card.goldDescription() == null || card.goldDescription().isEmpty()))
                errors.add(id + " 缺少焕章描述");
        }
        return List.copyOf(errors);
    }

    /**
     * ID 唯一性与一致性校验：注册键互不相同（CardFactory.register 已强制），
     * 且不同注册键不得声明同一个卡牌 id()。
     */
    public static List<String> duplicateIds()
    {
        List<String> errors = new ArrayList<>();
        Map<String, String> byCardId = new LinkedHashMap<>();
        for (String id : CardFactory.registeredIds())
        {
            DuelCard card = CardFactory.create(id);
            if (!id.equals(card.id()))
                errors.add("注册键 " + id + " 与卡牌 id() " + card.id() + " 不一致");
            String previous = byCardId.putIfAbsent(card.id(), id);
            if (previous != null)
                errors.add("重复卡牌 id()：" + card.id() + "（" + previous + " 与 " + id + "）");
        }
        return List.copyOf(errors);
    }

    /** 普通/金质变体必须是两个不同实现类。 */
    public static List<String> sameImplementationVariants()
    {
        List<String> errors = new ArrayList<>();
        for (String artifactId : CardCatalog.CARD_IDS)
        {
            String common = artifactId + "_common", gold = artifactId + "_gold";
            if (!CardFactory.contains(common) || !CardFactory.contains(gold)) continue;
            DuelCard commonCard = CardFactory.create(common), goldCard = CardFactory.create(gold);
            if (commonCard.getClass() == goldCard.getClass())
                errors.add(artifactId + " 的普通版与金质版不是独立类");
            if (commonCard.rarity() != CardRarity.COMMON || goldCard.rarity() != CardRarity.GOLD)
                errors.add(artifactId + " 的普通/金质稀有度声明错误");
        }
        return List.copyOf(errors);
    }

    /**
     * 已知待迁移清单（阶段16 抢骰基础设施落地后清空 → 不再存在待抢骰基础设施的卡牌）。
     * 后续若新增待迁移卡牌，在此登记。
     */
    private static final java.util.Set<String> PENDING_DRAFT_INFRASTRUCTURE = java.util.Set.of();

    /** 未迁移卡牌中等待抢骰基础设施的部分（阶段九记录在案；骰池落地后必须为空）。 */
    public static List<String> pendingDraftInfrastructureIds()
    {
        List<String> pending = new ArrayList<>();
        for (String entry : unmappedLegacyEffects())
        {
            // unmappedLegacyEffects 的条目带说明文字，先取出卡牌 id 本体。
            int space = entry.indexOf(' ');
            String id = space >= 0 ? entry.substring(0, space) : entry;
            String artifactId = id.replaceAll("_(common|gold)$", "");
            if (PENDING_DRAFT_INFRASTRUCTURE.contains(artifactId)) pending.add(id);
        }
        return List.copyOf(pending);
    }

    /** 除等待抢骰基础设施的卡牌外，不允许存在未迁移卡牌。 */
    public static List<String> unmappedLegacyEffectsBeyondDraft()
    {
        List<String> beyond = new ArrayList<>();
        for (String entry : unmappedLegacyEffects())
        {
            // 条目带说明文字，按 id 本体判断是否属于待迁移清单。
            int space = entry.indexOf(' ');
            String id = space >= 0 ? entry.substring(0, space) : entry;
            String artifactId = id.replaceAll("_(common|gold)$", "");
            if (!PENDING_DRAFT_INFRASTRUCTURE.contains(artifactId)) beyond.add(entry);
        }
        return List.copyOf(beyond);
    }

    /**
     * 未迁移旧效果校验：每张注册卡牌必须有可执行的效果路径——
     * 触发接口（OnSettlement/OnSummon/OnPlace/OnLeave/OnRoundStart/OnRoundEnd/
     * OnActivation/OnAmbushSuccess/OnAmbushFail/OnPoZhen/OnDraft）、
     * 或非空 effects()、或 onEvent/onSettlement 覆写之一（阶段15起不再接受直映射）。
     */
    public static List<String> unmappedLegacyEffects()
    {
        List<String> errors = new ArrayList<>();
        for (String id : CardFactory.registeredIds())
        {
            DuelCard card = CardFactory.create(id);
            if (implementsTriggerInterface(card)) continue;
            if (!card.effects().isEmpty()) continue;
            if (overridesSettlementOrEvent(card)) continue;
            errors.add(id + " 未声明任何触发接口、旧效果映射或词条实现");
        }
        return List.copyOf(errors);
    }

    /** 类名使用实际文物拼音：qian_li_jiang_shan -> QianLiJiangShanCommonCard/GoldCard。 */
    public static List<String> invalidClassNames()
    {
        List<String> errors = new ArrayList<>();
        for (String id : CardFactory.registeredIds())
        {
            DuelCard card = CardFactory.create(id);
            String suffix = card.rarity() == CardRarity.GOLD ? "GoldCard" : "CommonCard";
            String raritySuffix = card.rarity() == CardRarity.GOLD ? "_gold" : "_common";
            String artifactId = id.substring(0, id.length() - raritySuffix.length());
            String expected = pinyinClassName(artifactId) + suffix;
            if (!expected.equals(card.getClass().getSimpleName()))
                errors.add(id + " 的类名应为 " + expected + "，当前为 " + card.getClass().getSimpleName());
        }
        return List.copyOf(errors);
    }

    /** 汇总校验：suffix/目录/名称/独立变体/id 一致性。 */
    public static List<String> validateRegisteredCards()
    {
        List<String> errors = new ArrayList<>();
        for (String id : CardFactory.registeredIds())
        {
            DuelCard card = CardFactory.create(id);
            String suffix = card.rarity() == CardRarity.GOLD ? "_gold" : "_common";
            if (!id.endsWith(suffix)) errors.add(id + " 的 ID 与稀有度不一致");
            String artifactId = id.substring(0, id.length() - suffix.length());
            if (!CardCatalog.CARD_IDS.contains(artifactId)) errors.add(id + " 不在文物目录中");
        }
        errors.addAll(invalidNames());
        errors.addAll(duplicateIds());
        errors.addAll(sameImplementationVariants());
        errors.addAll(emptyDescriptions());
        return List.copyOf(errors);
    }

    public static List<String> missingVariantIds()
    {
        return CardMigrationBaseline.requiredIds().stream()
                .filter(id -> !CardFactory.contains(id)).sorted().toList();
    }

    private static boolean implementsTriggerInterface(DuelCard card)
    {
        return card instanceof OnSettlement || card instanceof OnSummon || card instanceof OnLeave
                || card instanceof OnPlace || card instanceof OnRoundStart || card instanceof OnRoundEnd
                || card instanceof OnActivation || card instanceof OnAmbushSuccess || card instanceof OnAmbushFail
                || card instanceof OnPoZhen || card instanceof OnDraft || card instanceof OnDraftPlan;
    }

    /** 兜底：卡牌类覆写了默认结算/事件入口但没有实现任何触发接口。 */
    private static boolean overridesSettlementOrEvent(DuelCard card)
    {
        try
        {
            Class<?> declaring = card.getClass()
                    .getMethod("onSettlement", SettlementContext.class).getDeclaringClass();
            if (!declaring.isInterface()) return true;
        }
        catch (NoSuchMethodException ignored) { /* DuelCard 默认声明，理论不可达 */ }
        try
        {
            Class<?> declaring = card.getClass()
                    .getMethod("onEvent", BattleEvent.class, CardContext.class).getDeclaringClass();
            if (!declaring.isInterface()) return true;
        }
        catch (NoSuchMethodException ignored) { }
        return false;
    }

    private static String pinyinClassName(String artifactId)
    {
        StringBuilder name = new StringBuilder();
        for (String segment : artifactId.split("_"))
        {
            if (segment.isEmpty()) continue;
            name.append(Character.toUpperCase(segment.charAt(0))).append(segment.substring(1));
        }
        return name.toString();
    }
}
