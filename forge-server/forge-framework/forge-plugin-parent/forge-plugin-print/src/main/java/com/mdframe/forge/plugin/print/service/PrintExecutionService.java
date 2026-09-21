package com.mdframe.forge.plugin.print.service;

import com.mdframe.forge.plugin.print.dto.PrintExecutionEventDTO;
import com.mdframe.forge.plugin.print.entity.*;
import com.mdframe.forge.plugin.print.enums.*;
import com.mdframe.forge.plugin.print.mapper.PrintExecutionMapper;
import com.mdframe.forge.plugin.print.spi.AuthorizedPrintContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class PrintExecutionService {

    private final PrintIdentity identity;

    private final PrintExecutionMapper executions;

    /**
     * 仅由 prepare 在数据与资源授权成功后调用，不接收客户端身份或结果。
     */
    Long prepared(AuthorizedPrintContext context, PrintTemplateVersion version, LocalDateTime generatedAt) {
        var row = PrintAudit.create(new PrintExecution(), context.actor());
        var record = context.record();
        row.setApplicationId(record.source().applicationId());
        row.setApplicationVersionId(context.applicationVersionId());
        row.setTemplateId(version.getTemplateId());
        row.setTemplateVersionId(version.getId());
        row.setSourceKey(record.source().key());
        row.setObjectCode(record.source().objectCode());
        row.setRecordId(record.recordId());
        row.setScene(record.scene().getCode());
        row.setTaskId(record.taskId());
        row.setProcessInstanceId(record.processInstanceId());
        row.setProcessRunId(record.processRunId());
        row.setActor(context.actor().userId());
        row.setDataMode(PrintDataMode.CURRENT.getCode());
        row.setGeneratedAt(generatedAt);
        row.setResult(PrintExecutionResult.PREPARED.getCode());
        row.setDelFlag(0);
        executions.insert(row);
        return row.getId();
    }

    /**
     * physicalOutputConfirmed is deliberately always false: browsers do not
     * expose a trustworthy signal that paper was produced.
     */
    public record Event(Long executionId, String result, Integer pageCount, String errorCode,
                        boolean physicalOutputConfirmed) {
    }

    @Transactional(rollbackFor = Exception.class)
    public Event record(Long id, PrintExecutionEventDTO dto) {
        identity.validate(dto);
        var actor = identity.require("print:execute");
        var row = executions.selectOwned(actor.tenantId(), id, actor.userId());
        if (row == null) {
            throw PrintFailure.missing();
        }
        if (PrintExecutionResult.PREPARED.matches(row.getResult())) {
            executions.recordEvent(actor.tenantId(), id, actor.userId(), dto.result().getCode(), dto.pageCount(), dto.errorCode());
            row = executions.selectOwned(actor.tenantId(), id, actor.userId());
        }
        if (row == null || !dto.result().matches(row.getResult()) || !Objects.equals(dto.pageCount(), row.getPageCount()) || !Objects.equals(dto.errorCode(), row.getErrorCode())) {
            throw PrintFailure.of(409, "PRINT_EVENT_CONFLICT", "此打印会话已记录其他终态");
        }
        return new Event(id, row.getResult(), row.getPageCount(), row.getErrorCode(), false);
    }
}
