package com.mdframe.forge.plugin.generator.vo.businessapp;

import lombok.Data;

/**
 * 移动端发起审批目录项：启用了审批流程的业务单据，只含展示元数据。
 * 对象主流程条目 processCode 为空；应用业务流程条目带 processCode，前端据此走业务流程发起。
 */
@Data
public class BusinessStartableObjectVO {

    private String objectCode;

    private String objectName;

    private String objectIcon;

    private String configKey;

    /** 对象未挂到任何应用时为空，前端归入"其他"分组。 */
    private Long applicationId;

    private String applicationName;

    private String applicationIcon;

    private Integer sortOrder;

    private String processCode;

    /** 业务流程启动按钮文案，与单据页按钮一致。 */
    private String processName;
}
