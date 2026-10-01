package com.mdframe.forge.plugin.print.service;

import com.mdframe.forge.plugin.print.dto.PrintBusinessSourceCreateDTO;
import com.mdframe.forge.plugin.print.dto.PrintBusinessSourceQueryDTO;
import com.mdframe.forge.plugin.print.dto.PrintBusinessSourceStatusDTO;
import com.mdframe.forge.plugin.print.dto.PrintBusinessSourceUpdateDTO;
import com.mdframe.forge.plugin.print.entity.PrintBusinessSource;
import com.mdframe.forge.plugin.print.enums.PrintBusinessSourceType;
import com.mdframe.forge.plugin.print.mapper.PrintBusinessSourceMapper;
import com.mdframe.forge.plugin.print.vo.PrintBusinessSourceVO;
import com.mdframe.forge.starter.core.enums.EnableStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class PrintBusinessSourceService {

    private final PrintIdentity identity;

    private final PrintBusinessSourceMapper sources;

    private final PrintSourceConfigValidator configValidator;

    public PrintBusinessSourceVO.Page page(PrintBusinessSourceQueryDTO query) {
        identity.validate(query);
        var actor = identity.require("print:source:view");
        int pageNum = query.resolvedPageNum();
        int pageSize = query.resolvedPageSize();
        String type = query.sourceType() == null ? null : query.sourceType().getCode();
        List<PrintBusinessSourceVO> records = sources.selectPage(
                        actor.tenantId(), normalize(query.sourceName()), type, query.status(),
                        (long) (pageNum - 1) * pageSize, pageSize)
                .stream()
                .map(PrintBusinessSourceVO::from)
                .toList();
        long total = sources.countPage(actor.tenantId(), normalize(query.sourceName()), type, query.status());
        return new PrintBusinessSourceVO.Page(records, total, pageNum, pageSize);
    }

    public List<PrintBusinessSourceVO.Option> options() {
        var actor = identity.require("print:source:view");
        return sources.selectPage(actor.tenantId(), null, null, EnableStatus.ENABLED.getCode(), 0, 1000)
                .stream()
                .map(PrintBusinessSourceVO.Option::from)
                .toList();
    }

    /**
     * 业务页面只保存稳定来源编码，运行时再在当前租户内解析服务端身份。
     */
    public PrintBusinessSourceVO.Option resolveRuntime(String sourceCode) {
        var actor = identity.require("print:execute");
        String code = normalize(sourceCode);
        if (code == null || !code.matches("[A-Za-z][A-Za-z0-9_-]{0,79}")) {
            throw missing();
        }
        PrintBusinessSource row = sources.selectByCode(actor.tenantId(), code);
        if (row == null || !EnableStatus.ENABLED.matches(row.getStatus())) {
            throw missing();
        }
        return PrintBusinessSourceVO.Option.from(row);
    }

    public PrintBusinessSourceVO detail(Long id) {
        var actor = identity.require("print:source:view");
        return PrintBusinessSourceVO.from(require(actor.tenantId(), id, false));
    }

    @Transactional(rollbackFor = Exception.class)
    public PrintBusinessSourceVO create(PrintBusinessSourceCreateDTO dto) {
        identity.validate(dto);
        var actor = identity.require("print:source:manage");
        configValidator.validate(dto.sourceType(), normalize(dto.providerCode()), dto.datasetId(),
                dto.parameterSchemaJson(), dto.mappingJson());
        var row = PrintAudit.create(new PrintBusinessSource(), actor);
        row.setSourceCode(dto.sourceCode().strip());
        row.setSourceName(dto.sourceName().strip());
        row.setSourceType(dto.sourceType().getCode());
        row.setProviderCode(normalize(dto.providerCode()));
        row.setDatasetId(dto.datasetId());
        row.setObjectCode(dto.objectCode().strip());
        row.setParameterSchemaJson(normalizeJson(dto.parameterSchemaJson()));
        row.setMappingJson(normalizeJson(dto.mappingJson()));
        row.setSourceRevision(1L);
        row.setStatus(EnableStatus.DISABLED.getCode());
        row.setDelFlag(0L);
        try {
            sources.insert(row);
        } catch (DuplicateKeyException error) {
            throw PrintFailure.of(409, "PRINT_SOURCE_CODE_EXISTS", "此租户已存在相同的打印来源编码");
        }
        return PrintBusinessSourceVO.from(row);
    }

    @Transactional(rollbackFor = Exception.class)
    public PrintBusinessSourceVO update(Long id, PrintBusinessSourceUpdateDTO dto) {
        identity.validate(dto);
        var actor = identity.require("print:source:manage");
        var row = require(actor.tenantId(), id, true);
        requireRevision(row, dto.expectedRevision());
        var type = PrintBusinessSourceType.valueOf(row.getSourceType());
        configValidator.validate(type, normalize(dto.providerCode()), dto.datasetId(),
                dto.parameterSchemaJson(), dto.mappingJson());
        row.setSourceName(dto.sourceName().strip());
        row.setProviderCode(normalize(dto.providerCode()));
        row.setDatasetId(dto.datasetId());
        row.setObjectCode(dto.objectCode().strip());
        row.setParameterSchemaJson(normalizeJson(dto.parameterSchemaJson()));
        row.setMappingJson(normalizeJson(dto.mappingJson()));
        row.setUpdateBy(actor.userId());
        changed(sources.updateSource(row, dto.expectedRevision()));
        return PrintBusinessSourceVO.from(require(actor.tenantId(), id, false));
    }

    @Transactional(rollbackFor = Exception.class)
    public PrintBusinessSourceVO status(Long id, PrintBusinessSourceStatusDTO dto) {
        identity.validate(dto);
        var actor = identity.require("print:source:manage");
        var row = require(actor.tenantId(), id, true);
        requireRevision(row, dto.expectedRevision());
        var status = EnableStatus.ENABLED.matches(dto.status()) ? EnableStatus.ENABLED : EnableStatus.DISABLED;
        if (status == EnableStatus.ENABLED) {
            configValidator.validate(PrintBusinessSourceType.valueOf(row.getSourceType()), row.getProviderCode(),
                    row.getDatasetId(), row.getParameterSchemaJson(), row.getMappingJson());
        }
        changed(sources.changeStatus(actor.tenantId(), id, dto.expectedRevision(), status.getCode(), actor.userId()));
        return PrintBusinessSourceVO.from(require(actor.tenantId(), id, false));
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id, Long expectedRevision) {
        var actor = identity.require("print:source:manage");
        var row = require(actor.tenantId(), id, true);
        requireRevision(row, expectedRevision);
        if (sources.countTemplateReferences(actor.tenantId(), id) > 0) {
            throw PrintFailure.of(409, "PRINT_SOURCE_REFERENCED", "请先删除此业务来源下的打印模板");
        }
        changed(sources.softDelete(actor.tenantId(), id, expectedRevision, actor.userId()));
    }

    private PrintBusinessSource require(Long tenantId, Long id, boolean lock) {
        if (id == null || id <= 0) {
            throw missing();
        }
        PrintBusinessSource row = lock ? sources.lockScoped(tenantId, id) : sources.selectScoped(tenantId, id);
        if (row == null) {
            throw missing();
        }
        return row;
    }

    private void requireRevision(PrintBusinessSource row, Long expectedRevision) {
        if (expectedRevision == null || expectedRevision <= 0
                || expectedRevision == Long.MAX_VALUE
                || !Objects.equals(row.getSourceRevision(), expectedRevision)) {
            throw revisionConflict();
        }
    }

    private void changed(int count) {
        if (count != 1) {
            throw revisionConflict();
        }
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    private String normalizeJson(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    private RuntimeException missing() {
        return PrintFailure.of(404, "PRINT_SOURCE_UNAVAILABLE", "打印业务来源不存在或已删除");
    }

    private RuntimeException revisionConflict() {
        return PrintFailure.of(409, "PRINT_SOURCE_REVISION_CONFLICT", "打印来源已发生变化，请重新载入后重试");
    }
}
