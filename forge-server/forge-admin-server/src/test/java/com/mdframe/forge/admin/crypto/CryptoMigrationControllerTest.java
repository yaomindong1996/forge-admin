package com.mdframe.forge.admin.crypto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.data.service.DataConnectionCryptoMigrationService;
import com.mdframe.forge.plugin.generator.service.crypto.LowcodeCryptoMigrationService;
import com.mdframe.forge.starter.core.annotation.api.ApiPermissionIgnore;
import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.core.annotation.crypto.ApiEncrypt;
import com.mdframe.forge.starter.core.context.ExecutionIdentity;
import com.mdframe.forge.starter.core.context.ExecutionIdentityContextHolder;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.LoginUser;
import com.mdframe.forge.starter.crypto.migration.CryptoMigrationReport;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CryptoMigrationControllerTest {

    @Test
    void shouldExposeEncryptedAdminMigrationEndpoints() throws Exception {
        RequestMapping mapping = CryptoMigrationController.class.getAnnotation(RequestMapping.class);
        assertNotNull(mapping);
        assertArrayEquals(new String[]{"/api/config/manage/crypto/migration"}, mapping.value());
        assertNotNull(CryptoMigrationController.class.getAnnotation(ApiEncrypt.class));
        assertNotNull(CryptoMigrationController.class.getAnnotation(ApiDecrypt.class));
        assertNotNull(CryptoMigrationController.class.getAnnotation(ApiPermissionIgnore.class));

        Method inventory = CryptoMigrationController.class.getDeclaredMethod("inventory", CryptoMigrationRequest.class);
        Method execute = CryptoMigrationController.class.getDeclaredMethod("execute", CryptoMigrationRequest.class);
        assertArrayEquals(new String[]{"/inventory"}, inventory.getAnnotation(PostMapping.class).value());
        assertArrayEquals(new String[]{"/execute"}, execute.getAnnotation(PostMapping.class).value());
    }

    @Test
    void shouldRejectNonAdminBeforeCoordinatorInvocation() {
        TrackingCoordinator coordinator = new TrackingCoordinator();
        CryptoMigrationController controller = new CryptoMigrationController(coordinator);

        try (var ignored = ExecutionIdentityContextHolder.open(identity(2))) {
            assertThrows(BusinessException.class, () -> controller.inventory(null));
        }

        assertFalse(coordinator.invoked);
    }

    @Test
    void executeShouldDefaultToDryRunAndReturnNoSensitivePayload() throws Exception {
        TrackingDataService dataService = new TrackingDataService();
        CryptoMigrationCoordinator coordinator = new CryptoMigrationCoordinator(dataService, null);
        CryptoMigrationRequest request = new CryptoMigrationRequest();
        request.setExpectedActiveKeyId("v2");
        request.setIncludeLowcode(false);

        CryptoMigrationReport report;
        try (var ignored = ExecutionIdentityContextHolder.open(identity(0))) {
            report = coordinator.execute(request);
        }

        assertTrue(dataService.dryRun);
        String json = new ObjectMapper().writeValueAsString(report);
        assertFalse(json.contains("plaintext"));
        assertFalse(json.contains("ciphertext"));
        assertFalse(json.contains("secretKey"));
    }

    @Test
    void executeShouldValidateAllScopesBeforeStartingMigration() {
        TrackingDataService dataService = new TrackingDataService();
        RejectingLowcodeService lowcodeService = new RejectingLowcodeService();
        CryptoMigrationCoordinator coordinator = new CryptoMigrationCoordinator(dataService, lowcodeService);
        CryptoMigrationRequest request = new CryptoMigrationRequest();
        request.setExpectedActiveKeyId("v2");
        request.setDryRun(false);
        request.setConfigKeys(List.of("crm_customer"));

        try (var ignored = ExecutionIdentityContextHolder.open(identity(0))) {
            assertThrows(BusinessException.class, () -> coordinator.execute(request));
        }

        assertFalse(dataService.invoked);
    }

    private ExecutionIdentity identity(int userType) {
        LoginUser user = new LoginUser();
        user.setUserId(1L);
        user.setTenantId(1L);
        user.setUserType(userType);
        return new ExecutionIdentity(user, "USER", 1L, 1L, 1L,
                "test-client", "test-token", Set.of());
    }

    private static final class TrackingCoordinator extends CryptoMigrationCoordinator {

        private boolean invoked;

        private TrackingCoordinator() {
            super(null, null);
        }

        @Override
        public CryptoMigrationReport inventory(CryptoMigrationRequest request) {
            invoked = true;
            return CryptoMigrationReport.of(1L, "ALL");
        }
    }

    private static final class TrackingDataService extends DataConnectionCryptoMigrationService {

        private boolean dryRun;
        private boolean invoked;

        private TrackingDataService() {
            super(null, null, null, null);
        }

        @Override
        public void validateMigrationRequest(String expectedActiveKeyId) {
        }

        @Override
        public CryptoMigrationReport migrate(Long tenantId,
                                             String expectedActiveKeyId,
                                             Integer batchSize,
                                             boolean dryRun) {
            this.invoked = true;
            this.dryRun = dryRun;
            CryptoMigrationReport report = CryptoMigrationReport.of(tenantId, "DATA_CONNECTION");
            report.increment("LEGACY");
            return report;
        }
    }

    private static final class RejectingLowcodeService extends LowcodeCryptoMigrationService {

        private RejectingLowcodeService() {
            super(null, null, null, null, null, null, null);
        }

        @Override
        public void validateMigrationRequest(java.util.Collection<String> configKeys, String expectedActiveKeyId) {
            throw new BusinessException("simulated lowcode preflight failure");
        }
    }
}
