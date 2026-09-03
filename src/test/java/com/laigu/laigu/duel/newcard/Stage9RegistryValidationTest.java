package com.laigu.laigu.duel.newcard;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 阶段九：注册和完整性校验（交接清单91 §八）。
 * 最终必须满足：missingVariantIds==0、registeredIds==158、
 * invalidNames/duplicateIds/sameImplementationVariants/unmappedLegacyEffects 均为空。
 */
class Stage9RegistryValidationTest
{
    @BeforeAll
    static void registerCards()
    {
        CardRegistry.initialize();
    }

    @Test
    void allCommonAndGoldVariantsAreRegistered()
    {
        assertEquals(0, CardRegistryValidator.missingVariantIds().size(),
                () -> "缺失变体：" + String.join(", ", CardRegistryValidator.missingVariantIds()));
        assertEquals(158, CardRegistryValidator.registeredIds().size());
    }

    @Test
    void everyCardDisplayNameUsesActualArtifactName()
    {
        assertTrue(CardRegistryValidator.invalidNames().isEmpty(),
                () -> String.join("\n", CardRegistryValidator.invalidNames()));
    }

    @Test
    void registeredIdsAreUniqueAndConsistentWithCardIds()
    {
        assertTrue(CardRegistryValidator.duplicateIds().isEmpty(),
                () -> String.join("\n", CardRegistryValidator.duplicateIds()));
    }

    @Test
    void commonAndGoldVariantsAreIndependentClassesWithCorrectRarity()
    {
        assertTrue(CardRegistryValidator.sameImplementationVariants().isEmpty(),
                () -> String.join("\n", CardRegistryValidator.sameImplementationVariants()));
    }

    @Test
    void everyCardDeclaresAMigratedEffectPathExceptDraftInfrastructure()
    {
        // 阶段16：抢骰基础设施已落地，待迁移清单必须为空（158/158 全部新系统实现）。
        assertTrue(CardRegistryValidator.pendingDraftInfrastructureIds().isEmpty());
        assertTrue(CardRegistryValidator.unmappedLegacyEffects().isEmpty(),
                () -> String.join(" | ", CardRegistryValidator.unmappedLegacyEffects()));
    }

    @Test
    void classNamesUseArtifactPinyin()
    {
        assertTrue(CardRegistryValidator.invalidClassNames().isEmpty(),
                () -> String.join("\n", CardRegistryValidator.invalidClassNames()));
    }

    @Test
    void aggregateValidationPasses()
    {
        assertTrue(CardRegistryValidator.validateRegisteredCards().isEmpty(),
                () -> String.join("\n", CardRegistryValidator.validateRegisteredCards()));
    }
}
