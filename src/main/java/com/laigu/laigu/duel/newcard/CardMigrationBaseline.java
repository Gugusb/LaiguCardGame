package com.laigu.laigu.duel.newcard;

import com.laigu.laigu.card.CardCatalog;

import java.util.LinkedHashSet;
import java.util.Set;

/** M1 基线：79 个实际文物 ID × 普通/金质两个独立变体。 */
public final class CardMigrationBaseline
{
    private CardMigrationBaseline() {}

    public static Set<String> requiredIds()
    {
        Set<String> ids = new LinkedHashSet<>();
        for (String cardId : CardCatalog.CARD_IDS)
        {
            ids.add(cardId + "_common");
            ids.add(cardId + "_gold");
        }
        return Set.copyOf(ids);
    }

    public static int artifactCount() { return CardCatalog.CARD_IDS.size(); }
    public static int variantCount() { return requiredIds().size(); }
}
