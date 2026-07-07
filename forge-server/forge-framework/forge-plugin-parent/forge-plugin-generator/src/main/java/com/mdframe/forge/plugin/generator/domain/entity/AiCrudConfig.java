package com.mdframe.forge.plugin.generator.domain.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mdframe.forge.starter.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_crud_config")
public class AiCrudConfig extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id")
    private Long id;

    private String configKey;
    private String tableName;
    private String tableComment;
    /** 应用显示名称，面向低代码业务人员 */
    private String appName;
    private String searchSchema;
    private String columnsSchema;
    private String editSchema;
    private String apiConfig;
    private String options;
    private String mode;
    /** 构建模式：AI-智能生成，LOWCODE-可视化低代码，CODEGEN-代码生成 */
    private String buildMode;
    private String status;
    /** 发布状态：DRAFT-草稿，PUBLISHED-已发布，STOPPED-已停用 */
    private String publishStatus;
    private String menuName;
    private Long menuParentId;
    private Integer menuSort;
    private Long menuResourceId;
    private String dictConfig;
    private String desensitizeConfig;
    private String encryptConfig;
    private String transConfig;
    /** 页面模板类型，对应 ai_page_template.template_key */
    private String layoutType;
    /** 可视化数据模型协议 */
    private String modelSchema;
    /** 可视化页面搭建协议 */
    private String pageSchema;
    /** 草稿版本号 */
    private Integer draftVersion;
    /** 已发布版本号 */
    private Integer publishedVersion;
    /** 发布时间 */
    private LocalDateTime publishTime;
    /** 发布人 */
    private Long publishBy;
    /** 业务领域ID */
    private Long domainId;
    /** 业务领域编码 */
    private String domainCode;
    /** 业务对象编码 */
    private String objectCode;
    /** 业务对象名称 */
    private String objectName;

    /** 运行数据源ID */
    private Long runtimeDatasourceId;
    /** 运行数据源编码 */
    private String runtimeDatasourceCode;
    /** 运行数据源快照，不含密码 */
    private String runtimeDatasourceSnapshot;
    /** 运行表名 */
    private String runtimeTableName;
    /** 主键字段名 */
    private String primaryKeyField;
    /** 主键列名 */
    private String primaryKeyColumn;
    /** 主键类型 */
    private String primaryKeyType;
    /** 租户隔离策略 */
    private String tenantStrategy;
    /** 审计字段策略 */
    private String auditStrategy;
    /** 逻辑删除策略 */
    private String logicDeleteStrategy;
    /** 删除标志（0正常 1删除） */
    @TableLogic
    private String delFlag;
}
