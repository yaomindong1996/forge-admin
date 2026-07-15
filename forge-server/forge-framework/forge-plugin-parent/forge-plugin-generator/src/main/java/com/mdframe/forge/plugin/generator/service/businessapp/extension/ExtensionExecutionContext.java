package com.mdframe.forge.plugin.generator.service.businessapp.extension;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 服务端扩展可信执行上下文。
 */
@Data
public class ExtensionExecutionContext {

    private Long tenantId;

    private Long actorUserId;

    private Long applicationId;

    private Long objectId;

    private Long entryId;

    private Long extensionId;

    private String extensionCode;

    private Integer versionNo;

    private String handlerCode;

    private String hookCode;

    private Map<String, Object> input = new LinkedHashMap<>();
}
