package com.laigu.laigu.album;

import com.laigu.laigu.card.CardCatalog;
import com.laigu.laigu.card.CardInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 收藏册分页模型：把全部卡牌按「朝代」分组，每页最多 {@link #SLOTS_PER_PAGE} 格。
 * <p>
 * 朝代顺序按 {@link #DYN_ORDER} 展示；每个朝代内的卡牌按卡牌目录顺序排列。
 * 某朝代卡牌超过一页容量时自动多开一页（{@link Page#pageIndex} 区分）。
 */
public final class AlbumPages
{
    /** 每页卡槽数（4 列 × 3 行） */
    public static final int SLOTS_PER_PAGE = 12;

    /** 朝代展示顺序；未列出的朝代按首次出现顺延追加在最后。 */
    public static final List<String> DYN_ORDER = List.of(
            "新石器时代", "商", "西周", "春秋", "战国", "秦", "汉", "晋",
            "南北朝", "北魏", "隋", "唐", "五代", "宋", "辽", "元", "明", "清",
            "古希腊", "古罗马", "古巴比伦", "近代");

    /** 一页：某朝代的一个分页。 */
    public static final class Page
    {
        /** 所属朝代 */
        public final String dynasty;
        /** 该朝代的第几页（0 起） */
        public final int pageIndex;
        /** 本页卡牌 id 列表（≤ {@link #SLOTS_PER_PAGE} 个，按目录顺序） */
        public final List<String> cards;

        Page(String dynasty, int pageIndex, List<String> cards)
        {
            this.dynasty = dynasty;
            this.pageIndex = pageIndex;
            this.cards = cards;
        }
    }

    /** 全部页面（按朝代顺序，多页朝代顺次展开） */
    private static final List<Page> PAGES = new ArrayList<>();
    /** 朝代 → 全部卡牌 id（目录顺序） */
    private static final Map<String, List<String>> CARD_IDS_BY_DYNASTY = new LinkedHashMap<>();
    /** 朝代 → 该朝代各分页 */
    private static final Map<String, List<Page>> PAGES_BY_DYNASTY = new LinkedHashMap<>();

    static
    {
        Map<String, List<String>> grouped = new LinkedHashMap<>();
        for (String dyn : DYN_ORDER)
        {
            grouped.put(dyn, new ArrayList<>());
        }
        for (String cardId : CardCatalog.CARD_IDS)
        {
            grouped.computeIfAbsent(CardInfo.of(cardId).dynasty, k -> new ArrayList<>()).add(cardId);
        }
        for (Map.Entry<String, List<String>> e : grouped.entrySet())
        {
            List<String> ids = e.getKey().isEmpty() ? List.of() : e.getValue();
            CARD_IDS_BY_DYNASTY.put(e.getKey(), Collections.unmodifiableList(ids));
            List<Page> dynastyPages = new ArrayList<>();
            for (int i = 0; i < ids.size(); i += SLOTS_PER_PAGE)
            {
                Page p = new Page(e.getKey(), dynastyPages.size(),
                        ids.subList(i, Math.min(i + SLOTS_PER_PAGE, ids.size())));
                dynastyPages.add(p);
                PAGES.add(p);
            }
            PAGES_BY_DYNASTY.put(e.getKey(), Collections.unmodifiableList(dynastyPages));
        }
    }

    private AlbumPages()
    {
    }

    /** 全部页面（按朝代顺序） */
    public static List<Page> pages()
    {
        return PAGES;
    }

    /** 某朝代包含的全部卡牌 id（目录顺序，不可变）。 */
    public static List<String> cardIdsOf(String dynasty)
    {
        return CARD_IDS_BY_DYNASTY.getOrDefault(dynasty, List.of());
    }

    /** 某朝代的所有分页。 */
    public static List<Page> pagesOf(String dynasty)
    {
        return PAGES_BY_DYNASTY.getOrDefault(dynasty, List.of());
    }

    /** 卡牌所属朝代。 */
    public static String dynastyOf(String cardId)
    {
        return CardInfo.of(cardId).dynasty;
    }
}
