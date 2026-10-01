package com.mdframe.forge.plugin.print.service;

import com.mdframe.forge.plugin.print.dto.*;
import com.mdframe.forge.plugin.print.entity.PrintBinding;
import com.mdframe.forge.plugin.print.enums.*;
import com.mdframe.forge.plugin.print.mapper.PrintBindingMapper;
import com.mdframe.forge.plugin.print.mapper.PrintTemplateMapper;
import com.mdframe.forge.plugin.print.mapper.PrintTemplateVersionMapper;
import com.mdframe.forge.plugin.print.spi.*;
import com.mdframe.forge.starter.core.enums.EnableStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DuplicateKeyException;
import java.util.*;

@Service
@RequiredArgsConstructor
public class PrintBindingService {

    private final PrintIdentity identity;

    private final PrintTemplateAccess access;

    private final PrintBindingMapper bindings;

    private final PrintTemplateMapper templates;

    private final PrintTemplateVersionMapper versions;

    public record Binding(Long id, Long templateId, Long templateVersionId, String templateName,
                          PrintScene scene, boolean isDefault, Integer sortOrder, Integer status,
                          Long bindingRevision) {

        static Binding from(PrintBinding row, String templateName) {
            return new Binding(row.getId(), row.getTemplateId(), row.getTemplateVersionId(),
                    templateName, PrintScene.valueOf(row.getScene()),
                    Boolean.TRUE.equals(row.getIsDefault()), row.getSortOrder(), row.getStatus(),
                    row.getBindingRevision());
        }
    }

    public List<Binding> list(PrintBindingQueryDTO dto) {
        identity.validate(dto);
        var actor = identity.require(PrintDesignAction.VIEW.permission());
        // 列绑定只核对来源身份；业务 Provider 是否可取数在保存/运行时再校验。
        PrintSourceRequest source = viewSource(actor, dto.source());
        var rows = dto.scene() == null
                ? bindings.selectBySource(actor.tenantId(), source.applicationId(),
                source.businessSourceId(), source.key())
                : bindings.selectSource(actor.tenantId(), source.applicationId(),
                source.businessSourceId(), source.key(), dto.scene().getCode());
        return rows.stream()
                .map(row -> Binding.from(row, templateName(actor.tenantId(), row.getTemplateId())))
                .toList();
    }

    private PrintSourceRequest viewSource(PrintActor actor, PrintSourceRequest requested) {
        if (requested.sourceType() != null && requested.sourceType().isStandalone()) {
            PrintSourceRequest canonical = access.requireBusinessSource(actor, requested.businessSourceId());
            if (!canonical.equals(requested)) {
                throw PrintFailure.of(403, "PRINT_ACCESS_DENIED",
                        "打印来源身份与登记信息不一致，请刷新页面后重试");
            }
            return canonical;
        }
        return access.source(actor, requested, PrintDesignAction.VIEW, false).source();
    }

    @Transactional(rollbackFor = Exception.class)
    public Binding save(PrintBindingSaveDTO dto) {
        identity.validate(dto);
        var actor = identity.require(PrintDesignAction.MANAGE.permission());
        var template = access.open(actor, dto.templateId(), PrintDesignAction.MANAGE, true);
        if (!template.source().source().sameAs(dto.source())) {
            throw PrintFailure.denied();
        }
        if (EnableStatus.ENABLED.matches(dto.status())
                && !EnableStatus.ENABLED.matches(template.row().getStatus())) {
            throw PrintFailure.of(409, "PRINT_TEMPLATE_DISABLED", "停用模板不能创建启用绑定");
        }
        var source = template.source().source();
        Long versionId = bindingVersion(actor.tenantId(), source, dto);
        var row = dto.id() == null
                ? bindings.selectUnique(actor.tenantId(), source.applicationId(), source.businessSourceId(),
                source.key(), dto.templateId(), dto.scene().getCode())
                : bindings.selectScoped(actor.tenantId(), dto.id());
        if (dto.id() != null && !matches(row, source, dto)) {
            throw PrintFailure.denied();
        }
        if (dto.id() != null && !Objects.equals(row.getBindingRevision(), dto.expectedRevision())) {
            throw PrintFailure.conflict();
        }
        boolean makeDefault = dto.isDefault() && EnableStatus.ENABLED.matches(dto.status());
        if (makeDefault && (row == null || !Boolean.TRUE.equals(row.getIsDefault()))) {
            bindings.clearDefault(actor.tenantId(), source.applicationId(), source.businessSourceId(),
                    source.key(), dto.scene().getCode(), actor.userId());
        }
        boolean create = row == null;
        if (create) {
            row = PrintAudit.create(new PrintBinding(), actor);
            row.setApplicationId(source.applicationId());
            row.setBusinessSourceId(source.businessSourceId());
            row.setSourceCode(source.sourceCode());
            row.setSourceType(source.sourceType().getCode());
            row.setSourceKey(source.key());
            row.setPageId(source.pageId());
            row.setFormKey(source.formKey());
            row.setObjectCode(source.objectCode());
            row.setTemplateId(dto.templateId());
            row.setScene(dto.scene().getCode());
            row.setBindingRevision(1L);
            row.setDelFlag(0L);
        }
        row.setTemplateVersionId(versionId);
        row.setIsDefault(makeDefault);
        row.setSortOrder(dto.sortOrder());
        row.setStatus(EnableStatus.ENABLED.matches(dto.status())
                ? EnableStatus.ENABLED.getCode()
                : EnableStatus.DISABLED.getCode());
        row.setUpdateBy(actor.userId());
        try {
            if (create) {
                bindings.insert(row);
            } else {
                Long revision = dto.id() == null ? row.getBindingRevision() : dto.expectedRevision();
                access.changed(bindings.updateBinding(row, revision));
            }
        } catch (DuplicateKeyException ex) {
            org.slf4j.LoggerFactory.getLogger(getClass()).debug("打印来源场景已绑定该模板");
            throw PrintFailure.of(409, "PRINT_BINDING_EXISTS", "此来源和场景已经绑定该模板");
        }
        var saved = bindings.selectScoped(actor.tenantId(), row.getId());
        return Binding.from(saved, templateName(actor.tenantId(), saved.getTemplateId()));
    }

    private Long bindingVersion(Long tenantId, PrintSourceRequest source, PrintBindingSaveDTO dto) {
        if (!source.sourceType().isStandalone()) {
            return null;
        }
        var version = versions.selectScoped(tenantId, dto.templateId(), dto.templateVersionId());
        if (version == null) {
            throw PrintFailure.of(400, "PRINT_VERSION_INVALID", "请选择此模板已经发布的固定版本");
        }
        return version.getId();
    }

    private boolean matches(PrintBinding row, PrintSourceRequest source, PrintBindingSaveDTO dto) {
        return row != null
                && Objects.equals(row.getApplicationId(), source.applicationId())
                && Objects.equals(row.getBusinessSourceId(), source.businessSourceId())
                && Objects.equals(row.getSourceKey(), source.key())
                && Objects.equals(row.getTemplateId(), dto.templateId())
                && dto.scene().matches(row.getScene());
    }

    private String templateName(Long tenantId, Long templateId) {
        if (templateId == null) {
            return null;
        }
        var template = templates.selectScoped(tenantId, templateId);
        return template == null ? null : template.getTemplateName();
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id, Long expectedRevision) {
        var actor = identity.require(PrintDesignAction.MANAGE.permission());
        var row = bindings.selectScoped(actor.tenantId(), id);
        if (row == null) {
            throw PrintFailure.missing();
        }
        access.open(actor, row.getTemplateId(), PrintDesignAction.MANAGE, true);
        if (expectedRevision == null || expectedRevision < 1 || expectedRevision == Long.MAX_VALUE) {
            throw PrintFailure.conflict();
        }
        access.changed(bindings.softDelete(actor.tenantId(), id, expectedRevision, actor.userId()));
    }
}
