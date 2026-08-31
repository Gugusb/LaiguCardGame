package com.laigu.laigu.buff;

import com.laigu.laigu.card.CardInfo;
import com.laigu.laigu.container.CardPouchContainer;
import com.laigu.laigu.item.CardPouchItem;
import com.laigu.laigu.util.CardNbt;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 卡牌羁绊：副手持卡袋时，依据袋内卡牌的「朝代 / 类型 / 稀有度」组合提供药水 buff。
 * <p>
 * 规则先定一版，后续可按策划调整。判定基于袋内 <b>不同</b> 的卡（同名不重复计数）。
 *
 * <h3>羁绊表（初版）</h3>
 * <ul>
 *   <li><b>同朝六珍</b>：同一朝代 ≥6 张不同卡 → 夜视 I</li>
 *   <li><b>钟鼎之和</b>：青铜器 ≥4 张 → 力量 I</li>
 *   <li><b>翰墨流芳</b>：书画 ≥4 张 → 急迫 I</li>
 *   <li><b>云锦天章</b>：织绣 ≥4 张 → 生命恢复 I</li>
 *   <li><b>盛世琳琅</b>：不同卡 ≥10 张（主副两袋）→ 抗性提升 I</li>
 *   <li><b>金玉满堂</b>：金质不同卡 ≥6 张 → 幸运 I</li>
 * </ul>
 */
public final class CardSynergy
{
    /** buff 时长（tick）：每 2 秒重刷，取一个远大于刷新间隔的时长即可持续生效 */
    public static final int BUFF_DURATION = 220;

    private CardSynergy()
    {
    }

    /** 读取卡袋内的卡牌（只取 CardItem）。 */
    public static List<ItemStack> readCards(ItemStack pouchStack)
    {
        List<ItemStack> out = new ArrayList<>();
        if (!(pouchStack.getItem() instanceof CardPouchItem) || pouchStack.getTag() == null)
        {
            return out;
        }
        ListTag list = pouchStack.getTag().getList(CardPouchContainer.TAG_ITEMS, Tag.TAG_COMPOUND);
        for (Tag t : list)
        {
            if (t instanceof CompoundTag)
            {
                ItemStack stack = ItemStack.of((CompoundTag) t);
                if (stack != null && !stack.isEmpty())
                {
                    out.add(stack);
                }
            }
        }
        return out;
    }

    /** 依据袋内卡牌计算生效的 buff 列表。 */
    public static List<MobEffectInstance> computeEffects(ItemStack pouchStack)
    {
        List<MobEffectInstance> out = new ArrayList<>();
        List<ItemStack> cards = readCards(pouchStack);
        if (cards.isEmpty())
        {
            return out;
        }

        // 统计（按「不同卡」去重）
        Map<String, Set<String>> dynastyCards = new HashMap<>(); // 朝代 -> 该朝不同卡 id 集合
        Map<String, Integer> typeDistinct = new HashMap<>();     // 类型 -> 不同卡数
        Set<String> distinctIds = new HashSet<>();
        Set<String> distinctGoldIds = new HashSet<>();

        for (ItemStack card : cards)
        {
            CardInfo info = CardInfo.of(card);
            dynastyCards.computeIfAbsent(info.dynasty, k -> new HashSet<>()).add(info.cardId);
            distinctIds.add(info.cardId);
            if ("gold".equals(rarityOf(card)))
            {
                distinctGoldIds.add(info.cardId);
            }
            // 类型按「不同卡」计：用 cardId 避免同名重复
            if (isType(card, "青铜器"))
            {
                typeDistinct.put("青铜器", typeDistinct.getOrDefault("青铜器", 0) + 1);
            }
            if (isType(card, "书画"))
            {
                typeDistinct.put("书画", typeDistinct.getOrDefault("书画", 0) + 1);
            }
            if (isType(card, "织绣"))
            {
                typeDistinct.put("织绣", typeDistinct.getOrDefault("织绣", 0) + 1);
            }
        }

        int maxDynastyDistinct = 0;
        for (Set<String> s : dynastyCards.values())
        {
            maxDynastyDistinct = Math.max(maxDynastyDistinct, s.size());
        }

        // 1. 同朝六珍：同一朝代 ≥6 张不同卡 → 夜视
        if (maxDynastyDistinct >= 6)
        {
            out.add(new MobEffectInstance(MobEffects.NIGHT_VISION, BUFF_DURATION, 0, false, false, true));
        }
        // 2. 钟鼎之和：青铜器 ≥4 张 → 力量 I
        if (typeDistinct.getOrDefault("青铜器", 0) >= 4)
        {
            out.add(new MobEffectInstance(MobEffects.DAMAGE_BOOST, BUFF_DURATION, 0, false, false, true));
        }
        // 3. 翰墨流芳：书画 ≥4 张 → 急迫 I
        if (typeDistinct.getOrDefault("书画", 0) >= 4)
        {
            out.add(new MobEffectInstance(MobEffects.DIG_SPEED, BUFF_DURATION, 0, false, false, true));
        }
        // 4. 云锦天章：织绣 ≥4 张 → 生命恢复 I
        if (typeDistinct.getOrDefault("织绣", 0) >= 4)
        {
            out.add(new MobEffectInstance(MobEffects.REGENERATION, BUFF_DURATION, 0, false, false, true));
        }
        // 5. 盛世琳琅：不同卡 ≥10 张 → 抗性提升 I
        if (distinctIds.size() >= 10)
        {
            out.add(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, BUFF_DURATION, 0, false, false, true));
        }
        // 6. 金玉满堂：金质不同卡 ≥6 张 → 幸运 I
        if (distinctGoldIds.size() >= 6)
        {
            out.add(new MobEffectInstance(MobEffects.LUCK, BUFF_DURATION, 0, false, false, true));
        }

        return out;
    }

    private static boolean isType(ItemStack card, String type)
    {
        return type.equals(CardInfo.of(card).type);
    }

    private static String rarityOf(ItemStack card)
    {
        return CardNbt.rarityOfPath(CardNbt.pathOf(card));
    }
}
