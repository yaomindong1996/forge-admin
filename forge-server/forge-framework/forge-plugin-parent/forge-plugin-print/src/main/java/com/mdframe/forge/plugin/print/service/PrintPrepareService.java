package com.mdframe.forge.plugin.print.service;

import com.mdframe.forge.plugin.print.dto.*;
import com.mdframe.forge.plugin.print.entity.PrintTemplateVersion;
import com.mdframe.forge.plugin.print.enums.*;
import com.mdframe.forge.plugin.print.mapper.*;
import com.mdframe.forge.plugin.print.protocol.PrintProtocolValidator;
import com.mdframe.forge.plugin.print.spi.*;
import com.mdframe.forge.plugin.print.vo.*;
import com.mdframe.forge.starter.core.enums.EnableStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class PrintPrepareService {

    private final PrintIdentity identity;

    private final PrintProviderRegistry registry;

    private final PrintTemplateAccess access;

    private final PrintTemplateMapper templates;

    private final PrintTemplateVersionMapper versions;

    private final PrintProtocolValidator protocol;

    private final PrintDocumentAccess documents;

    private final PrintDataProjector projector;

    private final PrintExecutionService executions;

    public PrintFieldCatalogVO catalog(PrintCatalogQueryDTO dto) {
        identity.validate(dto);
        PrintFieldCatalogVO result;
        if (dto.source() != null) {
            var actor = identity.require(PrintDesignAction.VIEW.permission());
            var source = access.source(actor, dto.source(), PrintDesignAction.VIEW, false);
            result = registry.provider(dto.source()).catalog(source);
        } else {
            result = authorized(dto.record()).catalog();
        }
        documents.catalog(result);
        return result;
    }

    private AuthorizedPrintContext authorized(PrintRecordRequest request) {
        identity.validate(request);
        var context = registry.authorize(identity.require("print:execute"), request);
        identity.validate(context.record());
        documents.catalog(context.catalog());
        return context;
    }

    public List<PrintAvailableTemplateVO> available(PrintAvailableTemplatesDTO dto) {
        identity.validate(dto);
        var context = authorized(dto.record());
        var result = new ArrayList<PrintAvailableTemplateVO>();
        for (var ref : context.versions()) {
            var template = templates.selectScoped(context.actor().tenantId(), ref.templateId());
            if (template == null || !EnableStatus.ENABLED.matches(template.getStatus())) {
                continue;
            }
            if (!Objects.equals(template.getApplicationId(), context.record().source().applicationId()) || !context.record().source().key().equals(template.getSourceKey())) {
                throw PrintFailure.denied();
            }
            var version = versions.selectScoped(context.actor().tenantId(), ref.templateId(), ref.templateVersionId());
            if (version == null) {
                continue;
            }
            var document = protocol.validate(version.getSchemaJson());
            if (!document.schemaHash().equals(version.getSchemaHash())) {
                throw PrintFailure.of(409, "PRINT_VERSION_INVALID", "发布模板完整性校验失败");
            }
            documents.requirements(document, context.catalog());
            result.add(new PrintAvailableTemplateVO(template.getId(), template.getTemplateName(), version.getId(), version.getVersionNo(), ref.isDefault(), ref.sortOrder()));
        }
        result.sort(Comparator.comparing(PrintAvailableTemplateVO::isDefault).reversed().thenComparingInt(PrintAvailableTemplateVO::sortOrder).thenComparing(PrintAvailableTemplateVO::id));
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public PrintContextVO prepare(PrintPrepareDTO dto) {
        identity.validate(dto);
        var context = authorized(dto.record());
        var actor = context.actor();
        var ref = context.versions().stream().filter(v -> v.templateId().equals(dto.templateId())).findFirst().orElseThrow(PrintFailure::denied);
        var template = templates.lockScoped(actor.tenantId(), ref.templateId());
        if (template == null || !EnableStatus.ENABLED.matches(template.getStatus())) {
            throw PrintFailure.missing();
        }
        if (!Objects.equals(template.getApplicationId(), dto.record().source().applicationId()) || !dto.record().source().key().equals(template.getSourceKey())) {
            throw PrintFailure.denied();
        }
        PrintTemplateVersion version = versions.selectScoped(actor.tenantId(), ref.templateId(), ref.templateVersionId());
        if (version == null) {
            throw PrintFailure.missing();
        }
        var document = protocol.validate(version.getSchemaJson());
        if (!document.schemaHash().equals(version.getSchemaHash())) {
            throw PrintFailure.of(409, "PRINT_VERSION_INVALID", "发布模板完整性校验失败");
        }
        var requirements = documents.requirements(document, context.catalog());
        var provider = registry.provider(dto.record().source());
        var selection = new PrintBindingSelection(ref, requirements.fields(), requirements.collections(), requirements.staticFileIds());
        var data = provider.load(context, selection);
        var projected = projector.project(data, requirements, context.catalog());
        provider.validateRuntimeResources(context, projected.fileIds());
        var generatedAt = LocalDateTime.now();
        var executionId = executions.prepared(context, version, generatedAt);
        return new PrintContextVO(executionId, context.applicationVersionId(), template.getId(), version.getId(), version.getVersionNo(), document.canonicalJson(), projected.data(), context.catalog(), PrintDataMode.CURRENT.getCode(), generatedAt);
    }
}
