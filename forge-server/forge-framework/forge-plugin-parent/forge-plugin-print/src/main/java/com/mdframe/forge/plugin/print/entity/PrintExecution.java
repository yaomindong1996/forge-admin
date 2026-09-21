package com.mdframe.forge.plugin.print.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mdframe.forge.starter.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

/**
 * sys_print_execution，租户及审计字段由服务端填充。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_print_execution")
public class PrintExecution extends TenantEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 应用 ID。
     */
    private Long applicationId;

    /**
     * 应用发布版本。
     */
    private Long applicationVersionId;

    /**
     * 模板 ID。
     */
    private Long templateId;

    /**
     * 实际打印版本。
     */
    private Long templateVersionId;

    /**
     * 规范化来源。
     */
    private String sourceKey;

    /**
     * 对象编码。
     */
    private String objectCode;

    /**
     * 记录 ID。
     */
    private String recordId;

    /**
     * 运行场景。
     */
    private String scene;

    /**
     * 流程任务 ID。
     */
    private String taskId;

    /**
     * 流程实例 ID。
     */
    private String processInstanceId;

    /**
     * 业务流程运行 ID。
     */
    private Long processRunId;

    /**
     * 服务端登录操作者。
     */
    private Long actor;

    /**
     * 数据模式。
     */
    private String dataMode;

    /**
     * 数据生成时刻。
     */
    private LocalDateTime generatedAt;

    /**
     * PREPARED/DIALOG_OPENED/PDF_DOWNLOADED/FAILED，不代表物理出纸。
     */
    private String result;

    /**
     * 受限错误码，不存消息正文。
     */
    private String errorCode;

    /**
     * 准备后页数，最大 50。
     */
    private Integer pageCount;

    /**
     * 逻辑删除。
     */
    @TableLogic
    private Integer delFlag;
}
