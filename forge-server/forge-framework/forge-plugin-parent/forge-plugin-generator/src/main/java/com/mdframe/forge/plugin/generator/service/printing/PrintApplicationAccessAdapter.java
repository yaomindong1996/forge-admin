package com.mdframe.forge.plugin.generator.service.printing;

import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessApplication;
import com.mdframe.forge.plugin.generator.mapper.BusinessApplicationMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessApplicationVersionMapper;
import com.mdframe.forge.plugin.generator.service.businessapp.BusinessApplicationService;
import com.mdframe.forge.plugin.print.enums.PrintDesignAction;
import com.mdframe.forge.plugin.print.service.PrintFailure;
import com.mdframe.forge.plugin.print.service.PrintIdentity;
import com.mdframe.forge.plugin.print.spi.PrintActor;
import com.mdframe.forge.plugin.print.spi.PrintApplicationAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 打印设计的应用范围适配；业务记录运行授权由独立 Provider 实现。 */
@Component
@RequiredArgsConstructor
public class PrintApplicationAccessAdapter implements PrintApplicationAccess {
    private final PrintIdentity identity;
    private final BusinessApplicationMapper applications;
    private final BusinessApplicationService applicationService;
    private final BusinessApplicationVersionMapper versions;
    private final PrintApplicationLock applicationLock;
    private final PrintApplicationSnapshotCodec snapshots;

    @Override
    public void authorize(PrintActor actor, Long applicationId, PrintDesignAction action) {
        if (actor == null || action == null || applicationId == null || applicationId <= 0) {
            throw PrintFailure.denied();
        }
        String permission = switch (action) {
            case VIEW -> "ai:businessApplication:list";
            case MANAGE -> "ai:businessApplication:edit";
            case PUBLISH -> "ai:businessApplication:publish";
        };
        if (!actor.equals(identity.require(action.permission())) || !actor.equals(identity.require(permission))) {
            throw PrintFailure.denied();
        }
        requireVisible(applications.selectEntityById(actor.tenantId(), applicationId));
    }

    @Override
    public void lockApplication(PrintActor actor, Long applicationId) {
        if (actor == null || !actor.equals(identity.current())) {
            throw PrintFailure.denied();
        }
        requireVisible(applicationLock.lock(actor.tenantId(), applicationId));
    }

    @Override
    public void assertTemplateUnreferenced(PrintActor actor, Long applicationId, Long templateId) {
        authorize(actor, applicationId, PrintDesignAction.MANAGE);
        lockApplication(actor, applicationId);
        for (var version : versions.lockRetainedSnapshots(actor.tenantId(), applicationId)) {
            if (snapshots.read(version.getSnapshotJson(), applicationId).stream()
                    .anyMatch(binding -> binding.templateId().equals(templateId))) {
                throw PrintFailure.of(409, "PRINT_TEMPLATE_REFERENCED", "应用历史发布版本仍引用此模板，不能删除");
            }
        }
    }

    private void requireVisible(AiBusinessApplication application) {
        if (application == null || !applicationService.canCurrentUserAccessPortal(application.getPortalConfig())) {
            throw PrintFailure.denied();
        }
    }
}
