package com.mdframe.forge.plugin.ai.provider.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 不暴露明文凭据的 AI 供应商视图。
 */
@Data
public class AiProviderVO {

    private Long id;

    private Long tenantId;

    private String providerName;

    private String providerType;

    private String adapterCode;

    private String logo;

    private String apiKey;

    private String baseUrl;

    private String models;

    private String defaultModel;

    private String isDefault;

    private String status;

    private String remark;

    private Long createBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    private Long createDept;

    private Long updateBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
