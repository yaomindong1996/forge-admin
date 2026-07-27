package com.mdframe.forge.starter.crypto.migration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CryptoMigrationReportTest {

    @Test
    void shouldBlockKeyRetirementUntilHistoricalCiphertextsAreZero() {
        CryptoMigrationReport report = CryptoMigrationReport.of(1L, "ALL");

        assertTrue(report.canRetireLegacy());

        report.increment("HISTORICAL");

        assertFalse(report.canRetireLegacy());
    }

    @Test
    void mergeShouldRetainActiveKeyMetadata() {
        CryptoMigrationReport report = CryptoMigrationReport.of(1L, "ALL");
        CryptoMigrationReport child = CryptoMigrationReport.of(1L, "DATA_CONNECTION");
        child.setActiveKeyId("v2");

        report.merge(child);

        assertEquals("v2", report.getActiveKeyId());
    }
}
