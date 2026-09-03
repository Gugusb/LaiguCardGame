package com.laigu.laigu.duel.newcard;

/** 新版对战卡变体；普通版和金质版必须由不同的 DuelCard 实现类承载。 */
public enum CardRarity
{
    COMMON("common", "普通"),
    GOLD("gold", "金质");

    private final String idSuffix;
    private final String displaySuffix;

    CardRarity(String idSuffix, String displaySuffix)
    {
        this.idSuffix = idSuffix;
        this.displaySuffix = displaySuffix;
    }

    public String idSuffix() { return idSuffix; }
    public String displaySuffix() { return displaySuffix; }
}
