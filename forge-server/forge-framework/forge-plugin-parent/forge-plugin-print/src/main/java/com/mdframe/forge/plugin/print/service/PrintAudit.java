package com.mdframe.forge.plugin.print.service;

import com.mdframe.forge.plugin.print.spi.PrintActor;
import com.mdframe.forge.starter.tenant.core.TenantEntity;
import java.time.LocalDateTime;

final class PrintAudit {

    private PrintAudit() {
    }

    static <T extends TenantEntity> T create(T row, PrintActor actor) {
        row.setTenantId(actor.tenantId());
        row.setCreateBy(actor.userId());
        row.setUpdateBy(actor.userId());
        row.setCreateDept(actor.departmentId());
        row.setCreateTime(LocalDateTime.now());
        row.setUpdateTime(row.getCreateTime());
        return row;
    }
}
