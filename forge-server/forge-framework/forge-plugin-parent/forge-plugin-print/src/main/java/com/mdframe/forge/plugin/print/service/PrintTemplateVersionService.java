package com.mdframe.forge.plugin.print.service;

import com.mdframe.forge.plugin.print.dto.PrintTemplatePublishDTO;
import com.mdframe.forge.plugin.print.entity.PrintTemplateVersion;
import com.mdframe.forge.plugin.print.enums.*;
import com.mdframe.forge.plugin.print.mapper.*;
import com.mdframe.forge.plugin.print.protocol.PrintProtocolValidator;
import com.mdframe.forge.plugin.print.vo.*;
import com.mdframe.forge.starter.core.enums.EnableStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PrintTemplateVersionService {

    private final PrintIdentity identity;

    private final PrintTemplateAccess access;

    private final PrintTemplateMapper templates;

    private final PrintTemplateVersionMapper versions;

    private final PrintProtocolValidator protocol;

    private final PrintDocumentAccess documents;

    private final ObjectMapper mapper;

    public record Publication(PrintTemplateVO template, PrintVersionVO version) {
    }

    @Transactional(rollbackFor = Exception.class)
    public Publication publish(Long id, PrintTemplatePublishDTO dto) {
        identity.validate(dto);
        var actor = identity.require(PrintDesignAction.PUBLISH.permission());
        var current = access.open(actor, id, PrintDesignAction.PUBLISH, true);
        access.revision(current.row(), dto.expectedRevision());
        if (!EnableStatus.ENABLED.matches(current.row().getStatus())) {
            throw PrintFailure.missing();
        }
        var document = protocol.validate(current.row().getDraftSchema());
        var requirements = documents.design(current.source(), current.provider(), document);
        var previous = current.row().getPublishedVersionId() == null ? null : versions.selectScoped(actor.tenantId(), id, current.row().getPublishedVersionId());
        PrintTemplateVersion selected = previous;
        if (previous == null || !previous.getSchemaHash().equals(document.schemaHash())) {
            int number = versions.selectMaxVersionNo(actor.tenantId(), id);
            if (number == Integer.MAX_VALUE) {
                throw PrintFailure.of(409, "PRINT_VERSION_LIMIT", "模板版本号已达到上限");
            }
            selected = PrintAudit.create(new PrintTemplateVersion(), actor);
            selected.setTemplateId(id);
            selected.setVersionNo(number + 1);
            selected.setSchemaVersion(document.document().schemaVersion());
            selected.setSchemaJson(document.canonicalJson());
            selected.setSchemaHash(document.schemaHash());
            try {
                selected.setResourceManifest(mapper.writeValueAsString(requirements.staticFileIds().stream().sorted().toList()));
            } catch (java.io.IOException ex) {
                org.slf4j.LoggerFactory.getLogger(getClass()).error("打印资源清单编码失败");
                throw new IllegalStateException("无法编码打印文件清单", ex);
            }
            selected.setPublishTime(LocalDateTime.now());
            selected.setDelFlag(0);
            versions.insert(selected);
        }
        if (previous == null || !selected.getId().equals(previous.getId()) || !PrintDesignStatus.PUBLISHED.matches(current.row().getDesignStatus())) {
            access.changed(templates.publish(actor.tenantId(), id, dto.expectedRevision(), selected.getId(), actor.userId()));
        }
        return new Publication(PrintTemplateVO.from(templates.selectScoped(actor.tenantId(), id), true), PrintVersionVO.from(selected, false));
    }

    public List<PrintVersionVO> list(Long id) {
        var actor = identity.require(PrintDesignAction.VIEW.permission());
        access.open(actor, id, PrintDesignAction.VIEW, false);
        return versions.selectVersions(actor.tenantId(), id).stream().map(row -> PrintVersionVO.from(row, false)).toList();
    }

    public PrintVersionVO detail(Long id, Long versionId) {
        var actor = identity.require(PrintDesignAction.VIEW.permission());
        access.open(actor, id, PrintDesignAction.VIEW, false);
        var version = versions.selectScoped(actor.tenantId(), id, versionId);
        if (version == null) {
            throw PrintFailure.missing();
        }
        return PrintVersionVO.from(version, true);
    }
}
