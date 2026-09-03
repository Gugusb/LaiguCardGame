package com.laigu.laigu.duel.newcard;

import com.laigu.laigu.duel.DuelCardCatalog;
import com.laigu.laigu.duel.DuelCardData;
import com.laigu.laigu.duel.EffectType;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 临时工具：把旧目录卡面描述导出为 CardDescriptionBook 源码草稿（生成后删除）。 */
class DescriptionDumpToolTest
{
    @Test
    void dumpCatalogDescriptions() throws Exception
    {
        Map<String, DuelCardData> byId = new LinkedHashMap<>();
        for (DuelCardData d : DuelCardCatalog.CARDS) byId.put(d.cardId, d);

        StringBuilder out = new StringBuilder();
        out.append("package com.laigu.laigu.duel.newcard;\n\n");
        out.append("import java.util.HashMap;\n");
        out.append("import java.util.Map;\n\n");
        out.append("/** 卡面描述书：新系统的卡面文案源。 */\n");
        out.append("public final class CardDescriptionBook\n");
        out.append("{\n");
        out.append("    private static final Map<String, String> MAIN = new HashMap<>();\n");
        out.append("    private static final Map<String, String> GOLD = new HashMap<>();\n");
        out.append("\n");
        out.append("    static\n");
        out.append("    {\n");
        for (Map.Entry<String, DuelCardData> e : byId.entrySet())
        {
            DuelCardData d = e.getValue();
            String id = e.getKey();
            String mainCommon = d.desc == null ? "" : d.desc;
            String mainGold = scaleDescription(d.desc, d.effect, d.p1, d.p2);
            String goldLine = d.goldDesc == null ? "焕章：无" : d.goldDesc;

            out.append("        MAIN.put(\"").append(id).append("_common\", \"").append(esc(mainCommon)).append("\");\n");
            out.append("        MAIN.put(\"").append(id).append("_gold\", \"").append(esc(mainGold)).append("\");\n");
            out.append("        GOLD.put(\"").append(id).append("_gold\", \"").append(esc(goldLine)).append("\");\n");
            out.append("        GOLD.put(\"").append(id).append("_common\", \"\");\n");
        }

        out.append("    }\n\n");
        out.append("    private CardDescriptionBook() {}\n\n");
        out.append("    /** 主描述；未收录的 id 返回空串（校验测试兜底）。 */\n");
        out.append("    public static String description(String variantId)\n");
        out.append("    {\n");
        out.append("        return MAIN.getOrDefault(variantId, \"\");\n");
        out.append("    }\n\n");
        out.append("    /** 焕章行；普通卡空串，金卡无加成返回「焕章：无」。 */\n");
        out.append("    public static String goldDescription(String variantId)\n");
        out.append("    {\n");
        out.append("        return GOLD.getOrDefault(variantId, \"\");\n");
        out.append("    }\n");

        Files.createDirectories(Path.of("build/generated"));
        Files.writeString(Path.of("build/generated/CardDescriptionBook.java.txt"), out.toString(), StandardCharsets.UTF_8);
    }

    private static String scaleDescription(String source, EffectType type, int value1, int value2)
    {
        if (source == null || type == null) return source;
        DuelCardData.ValueSpec p1Spec = DuelCardData.valueSpec(type, 0);
        DuelCardData.ValueSpec p2Spec = DuelCardData.valueSpec(type, 1);
        String pat = "(?<![\\d\\p{IsHan}])(" + value1 + "|" + value2 + ")(?![\\d\\p{IsHan}])";
        Matcher m = Pattern.compile(pat).matcher(source);
        StringBuilder sb = new StringBuilder();
        while (m.find())
        {
            int value = Integer.parseInt(m.group(1));
            if (value == value1 && p1Spec.goldScale() == DuelCardData.GoldScale.DOUBLE) value *= 2;
            else if (value == value2 && p2Spec.goldScale() == DuelCardData.GoldScale.DOUBLE) value *= 2;
            m.appendReplacement(sb, String.valueOf(value));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private static String esc(String s)
    {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray())
        {
            if (c == '"') sb.append("\\\"");
            else if (c == '\\') sb.append("\\\\");
            else sb.append(c);
        }
        return sb.toString();
    }
}