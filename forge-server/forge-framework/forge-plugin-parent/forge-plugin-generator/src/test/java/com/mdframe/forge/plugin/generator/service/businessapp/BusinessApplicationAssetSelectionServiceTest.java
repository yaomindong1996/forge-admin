package com.mdframe.forge.plugin.generator.service.businessapp;

import com.mdframe.forge.plugin.generator.constant.BusinessExtensionStatus;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessApp;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("BusinessApplicationAssetSelectionService")
class BusinessApplicationAssetSelectionServiceTest {

    @Test
    @DisplayName("default publish selection skips untested extension drafts")
    void defaultSelectionSkipsUntestedDrafts() {
        Set<Long> selected = BusinessApplicationAssetSelectionService.defaultPublishableExtensionIds(List.of(
                extension(1L, BusinessExtensionStatus.DRAFT),
                extension(2L, BusinessExtensionStatus.TESTED),
                extension(3L, BusinessExtensionStatus.ENABLED),
                extension(4L, BusinessExtensionStatus.DISABLED)
        ));

        assertEquals(Set.of(2L, 3L, 4L), selected);
    }

    @Test
    @DisplayName("default entry selection skips disabled and incomplete runtime entries")
    void defaultEntrySelectionOnlyIncludesPublishableEntries() {
        Set<Long> selected = BusinessApplicationAssetSelectionService.defaultPublishableEntryIds(List.of(
                entry(1L, 0, "ROUTE", null),
                entry(2L, 1, "RUNTIME", null),
                entry(3L, 1, "RUNTIME", "crm_customer"),
                entry(4L, 1, "ROUTE", null)
        ));

        assertEquals(Set.of(3L, 4L), selected);
    }

    private AiBusinessExtension extension(Long id, String status) {
        AiBusinessExtension extension = new AiBusinessExtension();
        extension.setId(id);
        extension.setStatus(status);
        return extension;
    }

    private AiBusinessApp entry(Long id, Integer status, String entryMode, String configKey) {
        AiBusinessApp entry = new AiBusinessApp();
        entry.setId(id);
        entry.setStatus(status);
        entry.setEntryMode(entryMode);
        entry.setConfigKey(configKey);
        return entry;
    }
}
