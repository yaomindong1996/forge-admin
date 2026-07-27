package com.mdframe.forge.admin.crypto;

import com.mdframe.forge.starter.core.annotation.api.ApiPermissionIgnore;
import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.core.annotation.crypto.ApiEncrypt;
import com.mdframe.forge.starter.core.domain.RespInfo;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.crypto.migration.CryptoMigrationReport;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 持久化密文盘点与受控迁移入口。
 */
@RestController
@RequestMapping("/api/config/manage/crypto/migration")
@RequiredArgsConstructor
@ApiEncrypt
@ApiDecrypt
@ApiPermissionIgnore
public class CryptoMigrationController {

    private final CryptoMigrationCoordinator coordinator;

    @PostMapping("/inventory")
    public RespInfo<CryptoMigrationReport> inventory(@RequestBody(required = false) CryptoMigrationRequest request) {
        assertPlatformAdmin();
        return RespInfo.success(coordinator.inventory(request));
    }

    @PostMapping("/execute")
    public RespInfo<CryptoMigrationReport> execute(@RequestBody CryptoMigrationRequest request) {
        assertPlatformAdmin();
        return RespInfo.success(coordinator.execute(request));
    }

    private void assertPlatformAdmin() {
        SessionHelper.assertAdmin("只有超级管理员可以执行密文盘点和迁移");
    }
}
