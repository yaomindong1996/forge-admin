package com.mdframe.forge.plugin.generator.service.printing;

import com.mdframe.forge.plugin.print.mapper.PrintTemplateMapper;
import com.mdframe.forge.plugin.print.mapper.PrintTemplateVersionMapper;
import com.mdframe.forge.plugin.print.protocol.PrintProtocolValidator;
import com.mdframe.forge.plugin.print.service.PrintFailure;
import com.mdframe.forge.plugin.print.service.PrintIdentity;
import com.mdframe.forge.plugin.print.spi.PrintSourceRequest;
import com.mdframe.forge.starter.core.enums.EnableStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 最终提交的引用守卫；候选阶段的业务字段检查不能替代此处锁内校验。 */
@Component
@RequiredArgsConstructor
public class PrintApplicationVersionGuard {
    private final PrintIdentity identity;
    private final PrintApplicationLock applicationLock;
    private final PrintApplicationSnapshotCodec snapshots;
    private final PrintTemplateMapper templates;
    private final PrintTemplateVersionMapper versions;
    private final PrintProtocolValidator protocol;
    private final PrintBindingValidationService validation;
    private final PrintMetadataResolver metadata;

    public void lockAndValidate(Long applicationId, String snapshotJson) {
        var actor = identity.current();
        applicationLock.lock(actor.tenantId(), applicationId);
        for (var binding : snapshots.read(snapshotJson, applicationId)) {
            var template = templates.lockScoped(actor.tenantId(), binding.templateId());
            if (template == null || !EnableStatus.ENABLED.matches(template.getStatus())
                    || !binding.source().equals(PrintSourceRequest.from(template))
                    || !binding.source().key().equals(template.getSourceKey())) {
                throw PrintFailure.missing();
            }
            // 不使用 template.publishedVersionId；草稿和后续发布都不能改变此快照。
            var version = versions.selectScoped(actor.tenantId(), binding.templateId(), binding.templateVersionId());
            if (version == null || !binding.schemaHash().equals(version.getSchemaHash())
                    || !binding.schemaHash().equals(protocol.validate(version.getSchemaJson()).schemaHash())) {
                throw PrintFailure.of(409, "PRINT_APPLICATION_VERSION_INVALID", "应用引用的打印版本不存在或内容校验失败");
            }
            validation.validate(actor, binding, version.getSchemaJson(), metadata.parse(snapshotJson), true);
        }
    }
}
