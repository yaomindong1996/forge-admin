package com.mdframe.forge.plugin.ai.invocation.dto;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;
@Data public class AiInvocationPageQuery {
    private Integer pageNum = 1; private Integer pageSize = 20; private String agentCode; private Long providerId; private Long modelId; private String outcome;
    @DateTimeFormat(pattern="yyyy-MM-dd HH:mm:ss") private LocalDateTime startTime;
    @DateTimeFormat(pattern="yyyy-MM-dd HH:mm:ss") private LocalDateTime endTime;
}
