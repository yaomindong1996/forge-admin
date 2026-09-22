package com.mdframe.forge.plugin.print.service;

import com.mdframe.forge.plugin.print.dto.*;
import com.mdframe.forge.plugin.print.entity.PrintBinding;
import com.mdframe.forge.plugin.print.enums.*;
import com.mdframe.forge.plugin.print.mapper.PrintBindingMapper;
import com.mdframe.forge.plugin.print.mapper.PrintTemplateMapper;
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

    public record Binding(Long id, Long templateId, String templateName, PrintScene scene, boolean isDefault, Integer sortOrder, Integer status, Long bindingRevision) {

        static Binding from(PrintBinding row, String templateName) {
            return new Binding(row.getId(), row.getTemplateId(), templateName, PrintScene.valueOf(row.getScene()), Boolean.TRUE.equals(row.getIsDefault()), row.getSortOrder(), row.getStatus(), row.getBindingRevision());
        }
    }

    public List<Binding> list(PrintBindingQueryDTO dto) {
        identity.validate(dto);
        var actor = identity.require(PrintDesignAction.VIEW.permission());
        access.source(actor, dto.source(), PrintDesignAction.VIEW, false);
        var rows = dto.scene() == null
                ? bindings.selectBySource(actor.tenantId(), dto.applicationId(), dto.source().key())
                : bindings.selectSource(actor.tenantId(), dto.applicationId(), dto.source().key(), dto.scene().getCode());
        return rows.stream()
                .map(row -> Binding.from(row, templateName(actor.tenantId(), row.getTemplateId())))
                .toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public Binding save(PrintBindingSaveDTO dto) {
        identity.validate(dto);
        var actor = identity.require(PrintDesignAction.MANAGE.permission());
        var template = access.open(actor, dto.templateId(), PrintDesignAction.MANAGE, true);
        if (!template.source().source().sameAs(dto.source())) {
            throw PrintFailure.denied();
        }
        var row = dto.id() == null
                ? bindings.selectUnique(actor.tenantId(), dto.source().applicationId(), dto.source().key(), dto.templateId(), dto.scene().getCode())
                : bindings.selectScoped(actor.tenantId(), dto.id());
        if (dto.id() != null && (row == null || !row.getApplicationId().equals(dto.source().applicationId()) || !row.getSourceKey().equals(dto.source().key()) || !row.getTemplateId().equals(dto.templateId()) || !dto.scene().matches(row.getScene()))) {
            throw PrintFailure.denied();
        }
        if (dto.id() != null && !Objects.equals(row.getBindingRevision(), dto.expectedRevision())) {
            throw PrintFailure.conflict();
        }
        boolean makeDefault = dto.isDefault() && EnableStatus.ENABLED.matches(dto.status());
        if (makeDefault && (row == null || !Boolean.TRUE.equals(row.getIsDefault()))) {
            bindings.clearDefault(actor.tenantId(), dto.source().applicationId(), dto.source().key(), dto.scene().getCode(), actor.userId());
        }
        boolean create = row == null;
        if (create) {
            row = PrintAudit.create(new PrintBinding(), actor);
            row.setApplicationId(dto.source().applicationId());
            row.setSourceType(dto.source().sourceType().getCode());
            row.setSourceKey(dto.source().key());
            row.setPageId(dto.source().pageId());
            row.setFormKey(dto.source().formKey());
            row.setObjectCode(dto.source().objectCode());
            row.setTemplateId(dto.templateId());
            row.setScene(dto.scene().getCode());
            row.setBindingRevision(1L);
            row.setDelFlag(0L);
        }
        row.setIsDefault(makeDefault);
        row.setSortOrder(dto.sortOrder());
        row.setStatus(EnableStatus.ENABLED.matches(dto.status()) ? EnableStatus.ENABLED.getCode() : EnableStatus.DISABLED.getCode());
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
