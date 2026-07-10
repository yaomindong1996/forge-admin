package com.mdframe.forge.plugin.ai.provider.support;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiProviderSecretMaskerTest {

    @Test
    void longSecretShouldKeepOnlyFourCharacterEdges() {
        assertEquals("sk-a****7890", AiProviderSecretMasker.mask("sk-abcdefghij7890"));
    }

    @Test
    void shortSecretShouldBeFullyMasked() {
        assertEquals("****", AiProviderSecretMasker.mask("short"));
    }

    @Test
    void emptySecretShouldRemainEmpty() {
        assertNull(AiProviderSecretMasker.mask(null));
        assertEquals("", AiProviderSecretMasker.mask(""));
    }

    @Test
    void submittedMaskShouldBeRecognizedAsUnchanged() {
        String persisted = "sk-abcdefghij7890";

        assertTrue(AiProviderSecretMasker.isUnchangedMask(AiProviderSecretMasker.mask(persisted), persisted));
        assertFalse(AiProviderSecretMasker.isUnchangedMask("sk-new-secret", persisted));
    }
}
