package com.mdframe.forge.plugin.generator.vo.businessapp;

import lombok.Data;

/**
 * 发起审批目录的业务流程查询行；schemaJson 仅供服务端判断手动开始节点，不返回前端。
 */
@Data
public class BusinessStartableProcessRow {

    private String objectCode;

    private String objectName;

    private String objectIcon;

    private String configKey;

    private Long applicationId;

    private String applicationCode;

    private String applicationName;

    private String applicationIcon;

    private Integer sortOrder;

    private String processCode;

    private String processName;

    private String schemaJson;
}
