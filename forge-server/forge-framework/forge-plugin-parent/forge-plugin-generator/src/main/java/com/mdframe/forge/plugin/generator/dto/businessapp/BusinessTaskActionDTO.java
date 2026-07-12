package com.mdframe.forge.plugin.generator.dto.businessapp;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 业务待办办理参数。
 */
@Data
public class BusinessTaskActionDTO {

    private String action;

    private String taskId;

    private String businessKey;

    private String processInstanceId;

    private String processDefKey;

    private String taskDefKey;

    private String objectCode;

    private Long recordId;

    private String formKey;

    private String userId;

    /**
     * 由受控能力执行身份解析的可信租户，不接受普通前端表单覆盖。
     */
    private Long tenantId;

    /**
     * 受控流程动作远程幂等凭证。
     */
    private String idempotencyKey;

    /**
     * 受控流程动作规范请求摘要。
     */
    private String requestDigest;

    private String comment;

    private String signature;

    private Map<String, Object> variables = new LinkedHashMap<>();

    private Map<String, Object> data = new LinkedHashMap<>();
}
