package com.mdframe.forge.plugin.ai.session.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AiSessionVO {

    private String id;
    private Long userId;
    private String nickName;
    private String avatar;
    private String sessionName;
    private String agentCode;
    private String status;
    private Integer messageCount;
    private Long tokenUsage;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
