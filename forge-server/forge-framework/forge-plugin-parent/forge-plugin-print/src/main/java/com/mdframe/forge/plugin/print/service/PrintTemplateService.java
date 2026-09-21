package com.mdframe.forge.plugin.print.service;

import com.mdframe.forge.plugin.print.dto.*;
import com.mdframe.forge.plugin.print.entity.PrintTemplate;
import com.mdframe.forge.plugin.print.enums.*;
import com.mdframe.forge.plugin.print.mapper.PrintTemplateMapper;
import com.mdframe.forge.plugin.print.protocol.PrintProtocolValidator;
import com.mdframe.forge.plugin.print.spi.*;
import com.mdframe.forge.plugin.print.vo.PrintTemplateVO;
import com.mdframe.forge.starter.core.enums.EnableStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DuplicateKeyException;

@Service
@RequiredArgsConstructor
public class PrintTemplateService {

    private final PrintIdentity identity;

    private final PrintTemplateMapper templates;

    private final com.mdframe.forge.plugin.print.mapper.PrintBindingMapper bindings;

    private final PrintTemplateAccess access;

    private final PrintProviderRegistry registry;

    private final PrintProtocolValidator protocol;

    private final PrintDocumentAccess documents;

    public PrintTemplateVO.Page page(Long applicationId, int pageNum, int pageSize) {
        var actor = identity.require(PrintDesignAction.VIEW.permission());
        if (applicationId == null || applicationId < 1 || pageNum < 1 || pageSize < 1 || pageSize > 100) {
            throw PrintFailure.of(400, "PRINT_INVALID_REQUEST", "应用或分页参数无效");
        }
        registry.application().authorize(actor, applicationId, PrintDesignAction.VIEW);
        return new PrintTemplateVO.Page(templates.selectApplication(actor.tenantId(), applicationId, (long) (pageNum - 1) * pageSize, pageSize).stream().map(row -> PrintTemplateVO.from(row, false)).toList(), templates.countApplication(actor.tenantId(), applicationId), pageNum, pageSize);
    }

    public PrintTemplateVO detail(Long id) {
        return PrintTemplateVO.from(access.open(identity.require(PrintDesignAction.VIEW.permission()), id, PrintDesignAction.VIEW, false).row(), true);
    }

    @Transactional(rollbackFor = Exception.class)
    public PrintTemplateVO create(PrintTemplateCreateDTO dto) {
        identity.validate(dto);
        var actor = identity.require(PrintDesignAction.MANAGE.permission());
        var source = access.source(actor, PrintSourceRequest.from(dto), PrintDesignAction.MANAGE, true);
        var document = protocol.validate(dto.schemaJson());
        documents.design(source, registry.provider(source.source()), document);
        return insert(actor, source.source(), dto.templateCode(), dto.templateName(), document.canonicalJson());
    }

    private PrintTemplateVO insert(PrintActor actor, PrintSourceRequest source, String code, String name, String json) {
        var row = PrintAudit.create(new PrintTemplate(), actor);
        row.setApplicationId(source.applicationId());
        row.setSourceType(source.sourceType().getCode());
        row.setSourceKey(source.key());
        row.setPageId(source.pageId());
        row.setFormKey(source.formKey());
        row.setObjectCode(source.objectCode());
        row.setTemplateCode(code);
        row.setTemplateName(name.strip());
        row.setDraftSchema(json);
        row.setDraftRevision(1L);
        row.setDesignStatus(PrintDesignStatus.DRAFT.getCode());
        row.setStatus(EnableStatus.ENABLED.getCode());
        row.setDelFlag(0L);
        try {
            templates.insert(row);
        } catch (DuplicateKeyException ex) {
            org.slf4j.LoggerFactory.getLogger(getClass()).debug("打印模板编码重复");
            throw PrintFailure.of(409, "PRINT_CODE_EXISTS", "此应用内已存在相同的模板编码");
        }
        return PrintTemplateVO.from(row, true);
    }

    @Transactional(rollbackFor = Exception.class)
    public PrintTemplateVO update(Long id, PrintTemplateUpdateDTO dto) {
        identity.validate(dto);
        var actor = identity.require(PrintDesignAction.MANAGE.permission());
        var current = access.open(actor, id, PrintDesignAction.MANAGE, true);
        access.revision(current.row(), dto.expectedRevision());
        var document = protocol.validate(dto.schemaJson());
        documents.design(current.source(), current.provider(), document);
        current.row().setTemplateName(dto.templateName().strip());
        current.row().setDraftSchema(document.canonicalJson());
        current.row().setUpdateBy(actor.userId());
        access.changed(templates.updateDraft(current.row(), dto.expectedRevision()));
        return PrintTemplateVO.from(templates.selectScoped(actor.tenantId(), id), true);
    }

    @Transactional(rollbackFor = Exception.class)
    public PrintTemplateVO copy(Long id, PrintTemplateCopyDTO dto) {
        identity.validate(dto);
        var actor = identity.require(PrintDesignAction.MANAGE.permission());
        var current = access.open(actor, id, PrintDesignAction.MANAGE, true);
        access.revision(current.row(), dto.expectedRevision());
        var document = protocol.validate(current.row().getDraftSchema());
        documents.design(current.source(), current.provider(), document);
        return insert(actor, current.source().source(), dto.templateCode(), dto.templateName(), document.canonicalJson());
    }

    @Transactional(rollbackFor = Exception.class)
    public PrintTemplateVO status(Long id, PrintTemplateStatusDTO dto) {
        identity.validate(dto);
        var actor = identity.require(PrintDesignAction.MANAGE.permission());
        var current = access.open(actor, id, PrintDesignAction.MANAGE, true);
        access.revision(current.row(), dto.expectedRevision());
        var status = EnableStatus.ENABLED.matches(dto.status()) ? EnableStatus.ENABLED : EnableStatus.DISABLED;
        access.changed(templates.changeStatus(actor.tenantId(), id, dto.expectedRevision(), status.getCode(), actor.userId()));
        return PrintTemplateVO.from(templates.selectScoped(actor.tenantId(), id), true);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id, Long revision) {
        var actor = identity.require(PrintDesignAction.MANAGE.permission());
        var current = access.open(actor, id, PrintDesignAction.MANAGE, true);
        access.revision(current.row(), revision);
        registry.application().assertTemplateUnreferenced(actor, current.row().getApplicationId(), id);
        if (bindings.countTemplateReferences(actor.tenantId(), id) > 0) {
            throw PrintFailure.of(409, "PRINT_TEMPLATE_REFERENCED", "请先移除此模板的来源绑定");
        }
        access.changed(templates.softDelete(actor.tenantId(), id, revision, actor.userId()));
    }
}
