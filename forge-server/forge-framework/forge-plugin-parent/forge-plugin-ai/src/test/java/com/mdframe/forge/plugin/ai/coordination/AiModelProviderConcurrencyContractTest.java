package com.mdframe.forge.plugin.ai.coordination;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class AiModelProviderConcurrencyContractTest {

    @Test
    void modelAndProviderRowsShouldBeLockedBeforeSummarySynchronization() throws IOException {
        String manager = Files.readString(Path.of(
                "src/main/java/com/mdframe/forge/plugin/ai/coordination/AiModelProviderManager.java"));
        String providerController = Files.readString(Path.of(
                "src/main/java/com/mdframe/forge/plugin/ai/provider/controller/AiProviderController.java"));
        String providerService = Files.readString(Path.of(
                "src/main/java/com/mdframe/forge/plugin/ai/provider/service/AiProviderService.java"));
        String modelMapper = Files.readString(Path.of("src/main/resources/mapper/AiModelMapper.xml"));
        String providerMapper = Files.readString(Path.of("src/main/resources/mapper/AiProviderMapper.xml"));

        assertThat(manager)
                .contains("getByIdForUpdate", "lockProvider", "lockProviders", "new TreeSet<>(providerIds)",
                        "verifyProviderSnapshot", "public void deleteProvider")
                .contains("providerService.lockForModelSummary(providerId)");
        assertProviderLockPrecedesModelLock(methodBody(manager, "updateModel", "deleteModel"));
        assertProviderLockPrecedesModelLock(methodBody(manager, "deleteModel", "updateProvider"));

        String deleteProvider = methodBody(manager, "deleteProvider", "lockProviders");
        assertThat(deleteProvider.indexOf("lockProvider(providerId)"))
                .isLessThan(deleteProvider.indexOf("countByProviderId(providerId)"));
        assertThat(deleteProvider.indexOf("countByProviderId(providerId)"))
                .isLessThan(deleteProvider.indexOf("providerService.deleteProvider(providerId)"));
        assertThat(providerController)
                .contains("modelProviderManager.deleteProvider(id)")
                .doesNotContain("modelService.countByProviderId(id)");

        String setDefaultProvider = methodBody(manager, "setDefaultProvider", "lockProviders");
        assertThat(setDefaultProvider)
                .contains("requireTenantId()", "providerService.lockAllForDefaultSwitch(tenantId)",
                        "providerService.switchDefaultProvider(tenantId, providerId)");
        assertThat(setDefaultProvider.indexOf("lockAllForDefaultSwitch(tenantId)"))
                .isLessThan(setDefaultProvider.indexOf("switchDefaultProvider(tenantId, providerId)"));
        assertThat(providerController)
                .contains("modelProviderManager.setDefaultProvider(id)")
                .doesNotContain("providerService.setDefault(id)");

        String applySaveRequest = methodBody(providerService, "applySaveRequest", "setIfPresent");
        assertThat(applySaveRequest).doesNotContain("getIsDefault", "setIsDefault");
        assertThat(methodBody(providerService, "createProvider", "updateProvider"))
                .contains("provider.setIsDefault(AiConstants.IS_DEFAULT_NO)");
        assertThat(modelMapper)
                .contains("id=\"selectByIdForUpdate\"", "FOR UPDATE");
        assertThat(providerMapper)
                .contains("id=\"selectIdForUpdate\"", "id=\"selectIdsForDefaultSwitch\"",
                        "tenant_id = #{tenantId}", "ORDER BY id ASC", "FOR UPDATE")
                .contains("<update id=\"clearDefaultProviders\">", "<update id=\"markDefaultProvider\">");
    }

    private void assertProviderLockPrecedesModelLock(String methodBody) {
        int providerLock = Math.max(methodBody.indexOf("lockProvider("), methodBody.indexOf("lockProviders("));
        int modelLock = methodBody.indexOf("getByIdForUpdate(");
        assertThat(providerLock).isGreaterThanOrEqualTo(0).isLessThan(modelLock);
    }

    private String methodBody(String source, String methodName, String nextMethodName) {
        int start = source.indexOf("void " + methodName + "(");
        int end = source.indexOf("void " + nextMethodName + "(", start + 1);
        assertThat(start).isGreaterThanOrEqualTo(0);
        assertThat(end).isGreaterThan(start);
        return source.substring(start, end);
    }
}
