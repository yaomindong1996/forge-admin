package com.mdframe.forge.plugin.print.service;

import com.mdframe.forge.plugin.print.spi.PrintActor;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class PrintIdentity {

    private final Validator validator;

    public PrintActor require(String permission) {
        PrintActor actor = current();
        if (!SessionHelper.hasPermission(permission)) {
            throw PrintFailure.denied();
        }
        return actor;
    }

    /** 只读取可信登录身份；调用方仍须按场景检查权限。 */
    public PrintActor current() {
        var user = SessionHelper.getLoginUser();
        if (user == null || user.getUserId() == null || user.getTenantId() == null || user.getUserId() <= 0 || user.getTenantId() <= 0) {
            throw PrintFailure.denied();
        }
        Long tenant = TenantContextHolder.getTenantId();
        if (tenant != null && !Objects.equals(tenant, user.getTenantId())) {
            throw PrintFailure.denied();
        }
        return new PrintActor(user.getTenantId(), user.getUserId(), user.getMainOrgId());
    }

    public <T> T validate(T request) {
        if (request == null || !validator.validate(request).isEmpty()) {
            throw PrintFailure.of(400, "PRINT_INVALID_REQUEST", "打印请求参数无效");
        }
        return request;
    }
}
