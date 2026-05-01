-- forge_admin_new.ai_agent definition

CREATE TABLE `ai_agent`
(
    `id`            bigint                                  NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `tenant_id`     bigint                                  NOT NULL DEFAULT '0' COMMENT '租户ID',
    `agent_name`    varchar(100) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'Agent名称',
    `agent_code`    varchar(50) COLLATE utf8mb4_general_ci  NOT NULL COMMENT 'Agent编码（唯一）',
    `description`   varchar(500) COLLATE utf8mb4_general_ci          DEFAULT NULL COMMENT '描述',
    `system_prompt` longtext COLLATE utf8mb4_general_ci     NOT NULL COMMENT '系统提示词模板',
    `provider_id`   bigint                                           DEFAULT NULL COMMENT '关联供应商ID',
    `model_name`    varchar(100) COLLATE utf8mb4_general_ci          DEFAULT NULL COMMENT '使用的模型',
    `temperature`   decimal(3, 2)                                    DEFAULT '0.70' COMMENT '温度参数（0-1）',
    `max_tokens`    int                                              DEFAULT '4000' COMMENT '最大Token数',
    `extra_config`  json                                             DEFAULT NULL COMMENT '扩展配置',
    `status`        char(1) COLLATE utf8mb4_general_ci      NOT NULL DEFAULT '0' COMMENT '状态（0正常 1停用）',
    `create_by`     bigint                                           DEFAULT NULL COMMENT '创建者',
    `create_time`   datetime                                         DEFAULT NULL COMMENT '创建时间',
    `update_by`     bigint                                           DEFAULT NULL COMMENT '更新者',
    `update_time`   datetime                                         DEFAULT NULL COMMENT '更新时间',
    `del_flag`      char(1) COLLATE utf8mb4_general_ci      NOT NULL DEFAULT '0' COMMENT '删除标志（0正常 1删除）',
    `create_dept`   varchar(30) COLLATE utf8mb4_general_ci           DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_agent_code` (`agent_code`),
    KEY             `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='AI Agent配置表';


-- forge_admin_new.ai_chat_record definition

CREATE TABLE `ai_chat_record`
(
    `id`          bigint                                 NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `tenant_id`   bigint                                 NOT NULL DEFAULT '0' COMMENT '租户ID',
    `user_id`     bigint                                 NOT NULL COMMENT '用户ID',
    `agent_code`  varchar(50) COLLATE utf8mb4_general_ci          DEFAULT NULL COMMENT 'Agent编码',
    `session_id`  varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '会话ID',
    `role`        varchar(20) COLLATE utf8mb4_general_ci NOT NULL COMMENT '角色（user/assistant/system）',
    `content`     longtext COLLATE utf8mb4_general_ci    NOT NULL COMMENT '消息内容',
    `token_usage` int                                             DEFAULT NULL COMMENT 'Token消耗',
    `create_time` datetime                                        DEFAULT NULL COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY           `idx_session_id` (`session_id`),
    KEY           `idx_user_id` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2049847087348080643 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='AI对话记录表';


-- forge_admin_new.ai_chat_session definition

CREATE TABLE `ai_chat_session`
(
    `id`           varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '会话ID（UUID，由前端或服务端生成）',
    `tenant_id`    bigint                                 NOT NULL DEFAULT '0' COMMENT '租户ID',
    `user_id`      bigint                                 NOT NULL COMMENT '用户ID',
    `agent_code`   varchar(50) COLLATE utf8mb4_general_ci          DEFAULT NULL COMMENT '关联的 Agent 编码',
    `session_name` varchar(200) COLLATE utf8mb4_general_ci         DEFAULT NULL COMMENT '会话标题（首条消息截取）',
    `status`       char(1) COLLATE utf8mb4_general_ci     NOT NULL DEFAULT '0' COMMENT '状态（0正常 1已删除）',
    `create_time`  datetime                                        DEFAULT NULL COMMENT '创建时间',
    `update_time`  datetime                                        DEFAULT NULL COMMENT '最后更新时间',
    `metadata`     varchar(500) COLLATE utf8mb4_general_ci         DEFAULT NULL COMMENT 'metadata数据JSON',
    PRIMARY KEY (`id`),
    KEY            `idx_user_id` (`user_id`,`status`,`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='AI会话表';


-- forge_admin_new.ai_context_config definition

CREATE TABLE `ai_context_config`
(
    `id`             bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `tenant_id`      bigint                DEFAULT '1' COMMENT '租户ID',
    `agent_code`     varchar(64)  NOT NULL COMMENT '关联Agent编码',
    `config_name`    varchar(128) NOT NULL COMMENT '配置名称',
    `config_content` text         NOT NULL COMMENT '上下文内容',
    `config_type`    varchar(32)  NOT NULL DEFAULT 'SPEC' COMMENT '类型：SPEC/SAMPLE/RULE',
    `sort`           int                   DEFAULT '0' COMMENT '排序',
    `status`         char(1)               DEFAULT '0' COMMENT '状态（0正常 1停用）',
    `create_by`      bigint                DEFAULT NULL COMMENT '创建者',
    `create_time`    datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_dept`    bigint                DEFAULT NULL COMMENT '创建部门ID',
    `update_by`      bigint                DEFAULT NULL COMMENT '更新者',
    `update_time`    datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY              `idx_agent_code` (`agent_code`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI上下文配置表';


-- forge_admin_new.ai_crud_config definition

CREATE TABLE `ai_crud_config`
(
    `id`                 bigint       NOT NULL COMMENT '主键',
    `tenant_id`          bigint                DEFAULT '1' COMMENT '租户ID',
    `config_key`         varchar(64)  NOT NULL COMMENT '配置唯一标识（小写字母+数字+下划线）',
    `table_name`         varchar(128) NOT NULL COMMENT '关联的数据库表名',
    `table_comment`      varchar(255)          DEFAULT '' COMMENT '表描述/功能名称',
    `search_schema`      json                  DEFAULT NULL COMMENT '搜索表单Schema JSON',
    `columns_schema`     json                  DEFAULT NULL COMMENT '表格列Schema JSON',
    `edit_schema`        json                  DEFAULT NULL COMMENT '编辑表单Schema JSON',
    `api_config`         json                  DEFAULT NULL COMMENT 'API配置JSON（method@url格式）',
    `options`            json                  DEFAULT NULL COMMENT '其他AiCrudPage配置（modalType/modalWidth/gridCols等）',
    `mode`               varchar(16)  NOT NULL DEFAULT 'CONFIG' COMMENT '模式：CONFIG-配置驱动 / CODEGEN-代码生成',
    `status`             char(1)      NOT NULL DEFAULT '0' COMMENT '状态（0启用 1停用）',
    `menu_name`          varchar(128)          DEFAULT '' COMMENT '菜单名称',
    `menu_parent_id`     bigint                DEFAULT NULL COMMENT '菜单父级ID',
    `menu_sort`          int                   DEFAULT '0' COMMENT '菜单排序',
    `menu_resource_id`   bigint                DEFAULT NULL COMMENT '关联的sys_resource记录ID',
    `dict_config`        json                  DEFAULT NULL COMMENT '字典数据配置',
    `desensitize_config` json                  DEFAULT NULL COMMENT '字段脱敏配置',
    `encrypt_config`     json                  DEFAULT NULL COMMENT '接口加解密配置',
    `trans_config`       json                  DEFAULT NULL COMMENT '字典翻译配置',
    `layout_type`        varchar(64)           DEFAULT 'simple-crud' COMMENT '页面模板类型（对应 ai_page_template.template_key）',
    `create_by`          bigint                DEFAULT NULL COMMENT '创建者',
    `create_time`        datetime              DEFAULT NULL COMMENT '创建时间',
    `create_dept`        bigint                DEFAULT NULL COMMENT '创建部门',
    `update_by`          bigint                DEFAULT NULL COMMENT '更新者',
    `update_time`        datetime              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_config_key` (`config_key`),
    KEY                  `idx_table_name` (`table_name`),
    KEY                  `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='CRUD配置驱动表';


-- forge_admin_new.ai_model definition

CREATE TABLE `ai_model`
(
    `id`          bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `provider_id` bigint       NOT NULL COMMENT '供应商ID',
    `model_type`  varchar(32)  NOT NULL COMMENT '模型类型(chat/embedding/image/audio)',
    `model_id`    varchar(128) NOT NULL COMMENT '模型标识(如gpt-4o)',
    `model_name`  varchar(128) NOT NULL COMMENT '模型显示名称',
    `description` varchar(512)          DEFAULT NULL COMMENT '描述',
    `max_tokens`  int                   DEFAULT NULL COMMENT '最大Token数',
    `icon`        varchar(512)          DEFAULT NULL COMMENT '模型图标(文件ID或URL)',
    `is_default`  char(1)               DEFAULT '0' COMMENT '是否默认模型(0否1是)',
    `status`      char(1)               DEFAULT '0' COMMENT '状态(0正常1停用)',
    `sort_order`  int                   DEFAULT '0' COMMENT '排序号',
    `tenant_id`   bigint                DEFAULT NULL COMMENT '租户编号',
    `create_by`   bigint                DEFAULT NULL COMMENT '创建人',
    `create_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_dept` bigint unsigned DEFAULT NULL COMMENT '创建组织ID',
    `update_by`   bigint                DEFAULT NULL COMMENT '更新人',
    `update_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `remark`      varchar(512)          DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_ai_model_provider_model` (`provider_id`,`model_id`),
    KEY           `idx_ai_model_provider_id` (`provider_id`),
    KEY           `idx_ai_model_type` (`model_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI模型表';


-- forge_admin_new.ai_page_template definition

CREATE TABLE `ai_page_template`
(
    `id`             bigint       NOT NULL COMMENT '主键',
    `template_key`   varchar(64)  NOT NULL COMMENT '模板唯一标识（如 simple-crud / tree-crud）',
    `template_name`  varchar(128) NOT NULL COMMENT '模板显示名称',
    `description`    varchar(512)          DEFAULT '' COMMENT '模板描述',
    `icon`           varchar(64)           DEFAULT '' COMMENT '图标（mdi: 前缀）',
    `system_prompt`  text COMMENT '该模板专属的 AI system prompt 补充',
    `schema_hint`    json                  DEFAULT NULL COMMENT '告诉 AI 该模板可用的字段约束（JSON）',
    `default_config` json                  DEFAULT NULL COMMENT '默认配置值（JSON，如 modalType/searchGridCols 等）',
    `enabled`        tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否启用（1启用 0停用）',
    `sort`           int          NOT NULL DEFAULT '0' COMMENT '排序',
    `is_builtin`     tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否内置（1内置不可删除）',
    `codegen_type`   varchar(16)  NOT NULL DEFAULT 'TEMPLATE' COMMENT '代码生成策略：TEMPLATE-Velocity模板 / AI-大模型生成',
    `create_by`      bigint                DEFAULT NULL,
    `create_time`    datetime              DEFAULT NULL,
    `create_dept`    bigint                DEFAULT NULL,
    `update_by`      bigint                DEFAULT NULL,
    `update_time`    datetime              DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_template_key` (`template_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI页面模板表（组件市场）';


-- forge_admin_new.ai_provider definition

CREATE TABLE `ai_provider`
(
    `id`            bigint                                  NOT NULL COMMENT '主键ID',
    `tenant_id`     bigint                                  NOT NULL DEFAULT '0' COMMENT '租户ID',
    `provider_name` varchar(50) COLLATE utf8mb4_general_ci  NOT NULL COMMENT '供应商名称（如 阿里百炼、OpenAI）',
    `provider_type` varchar(30) COLLATE utf8mb4_general_ci  NOT NULL COMMENT '类型（openai/azure/dashscope/ollama）',
    `logo`          varchar(512) COLLATE utf8mb4_general_ci          DEFAULT NULL COMMENT '供应商Logo(文件ID或URL)',
    `api_key`       varchar(500) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'API Key',
    `base_url`      varchar(500) COLLATE utf8mb4_general_ci          DEFAULT NULL COMMENT 'API Base URL',
    `models`        json                                             DEFAULT NULL COMMENT '可用模型列表 [{"name":"qwen-plus","enabled":true}]',
    `is_default`    char(1) COLLATE utf8mb4_general_ci      NOT NULL DEFAULT '0' COMMENT '是否默认供应商（0否 1是）',
    `status`        char(1) COLLATE utf8mb4_general_ci      NOT NULL DEFAULT '0' COMMENT '状态（0正常 1停用）',
    `remark`        varchar(500) COLLATE utf8mb4_general_ci          DEFAULT NULL COMMENT '备注',
    `create_by`     bigint                                           DEFAULT NULL COMMENT '创建者',
    `create_time`   datetime                                         DEFAULT NULL COMMENT '创建时间',
    `update_by`     bigint                                           DEFAULT NULL COMMENT '更新者',
    `update_time`   datetime                                         DEFAULT NULL COMMENT '更新时间',
    `del_flag`      char(1) COLLATE utf8mb4_general_ci      NOT NULL DEFAULT '0' COMMENT '删除标志（0正常 1删除）',
    `default_model` varchar(100) COLLATE utf8mb4_general_ci          DEFAULT NULL COMMENT '默认模型名称',
    `create_dept`   varchar(30) COLLATE utf8mb4_general_ci           DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY             `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='AI供应商配置表';


-- forge_admin_new.ai_report_project definition

CREATE TABLE `ai_report_project`
(
    `id`               bigint                                  NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `tenant_id`        bigint                                  NOT NULL DEFAULT '0' COMMENT '租户ID',
    `project_name`     varchar(100) COLLATE utf8mb4_general_ci NOT NULL COMMENT '项目名称',
    `remark`           varchar(500) COLLATE utf8mb4_general_ci          DEFAULT NULL COMMENT '备注',
    `index_img`        varchar(500) COLLATE utf8mb4_general_ci          DEFAULT NULL COMMENT '封面图URL',
    `status`           char(1) COLLATE utf8mb4_general_ci      NOT NULL DEFAULT '0' COMMENT '状态（0正常 1停用）',
    `canvas_width`     int                                     NOT NULL DEFAULT '1920' COMMENT '画布宽度',
    `canvas_height`    int                                     NOT NULL DEFAULT '1080' COMMENT '画布高度',
    `background_color` varchar(20) COLLATE utf8mb4_general_ci           DEFAULT '#1e1e2e' COMMENT '背景颜色',
    `component_data`   longtext COLLATE utf8mb4_general_ci COMMENT '组件列表JSON',
    `publish_status`   char(1) COLLATE utf8mb4_general_ci      NOT NULL DEFAULT '0' COMMENT '发布状态（0未发布 1已发布）',
    `publish_url`      varchar(500) COLLATE utf8mb4_general_ci          DEFAULT NULL COMMENT '发布地址',
    `publish_time`     datetime                                         DEFAULT NULL COMMENT '发布时间',
    `create_by`        bigint                                           DEFAULT NULL COMMENT '创建者',
    `create_time`      datetime                                         DEFAULT NULL COMMENT '创建时间',
    `update_by`        bigint                                           DEFAULT NULL COMMENT '更新者',
    `update_time`      datetime                                         DEFAULT NULL COMMENT '更新时间',
    `create_dept`      bigint                                           DEFAULT NULL COMMENT '创建部门',
    `del_flag`         char(1) COLLATE utf8mb4_general_ci      NOT NULL DEFAULT '0' COMMENT '删除标志（0正常 1删除）',
    PRIMARY KEY (`id`),
    KEY                `idx_tenant_id` (`tenant_id`),
    KEY                `idx_create_by` (`create_by`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='go-view项目表';


-- forge_admin_new.biz_leave_request definition

CREATE TABLE `biz_leave_request`
(
    `id`                  varchar(64)    NOT NULL COMMENT '主键ID',
    `business_key`        varchar(64)    NOT NULL COMMENT '业务Key（关联流程）',
    `process_instance_id` varchar(64)             DEFAULT NULL COMMENT '流程实例ID',
    `apply_user_id`       varchar(64)    NOT NULL COMMENT '申请人ID',
    `apply_user_name`     varchar(100)   NOT NULL COMMENT '申请人姓名',
    `apply_dept_id`       varchar(64)             DEFAULT NULL COMMENT '申请部门ID',
    `apply_dept_name`     varchar(200)            DEFAULT NULL COMMENT '申请部门名称',
    `leave_type`          varchar(20)    NOT NULL COMMENT '请假类型：annual-年假/sick-病假/personal-事假/marriage-婚假/maternity-产假',
    `start_time`          datetime       NOT NULL COMMENT '开始时间',
    `end_time`            datetime       NOT NULL COMMENT '结束时间',
    `duration`            decimal(10, 1) NOT NULL COMMENT '请假天数',
    `reason`              text COMMENT '请假原因',
    `attachments`         text COMMENT '附件JSON列表',
    `status`              varchar(20)    NOT NULL DEFAULT 'draft' COMMENT '状态：draft-草稿/pending-审批中/approved-已通过/rejected-已驳回/canceled-已取消',
    `approve_user_id`     varchar(64)             DEFAULT NULL COMMENT '审批人ID',
    `approve_user_name`   varchar(100)            DEFAULT NULL COMMENT '审批人姓名',
    `approve_time`        datetime                DEFAULT NULL COMMENT '审批时间',
    `approve_comment`     text COMMENT '审批意见',
    `approve_attachments` text COMMENT '审批附件JSON',
    `create_time`         datetime                DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`         datetime                DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`             tinyint(1) DEFAULT '0' COMMENT '删除标记',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_business_key` (`business_key`),
    KEY                   `idx_apply_user` (`apply_user_id`),
    KEY                   `idx_process_instance` (`process_instance_id`),
    KEY                   `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='请假申请表';


-- forge_admin_new.biz_product definition

CREATE TABLE `biz_product`
(
    `id`           bigint       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `tenant_id`    bigint         DEFAULT NULL COMMENT '租户ID',
    `parent_id`    bigint         DEFAULT '0' COMMENT '父级ID',
    `product_name` varchar(255) NOT NULL COMMENT '产品名称',
    `category`     varchar(50)    DEFAULT NULL COMMENT '分类',
    `price`        decimal(10, 2) DEFAULT NULL COMMENT '价格',
    `stock`        int            DEFAULT '0' COMMENT '库存',
    `status`       tinyint(1) DEFAULT '1' COMMENT '状态',
    `sort`         int            DEFAULT '0' COMMENT '排序',
    `create_by`    varchar(64)    DEFAULT '' COMMENT '创建者',
    `create_time`  datetime       DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_dept`  bigint         DEFAULT NULL COMMENT '创建部门',
    `update_by`    varchar(64)    DEFAULT '' COMMENT '更新者',
    `update_time`  datetime       DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `del_flag`     tinyint(1) DEFAULT '0' COMMENT '删除标志',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='产品管理表';


-- forge_admin_new.config_properties definition

CREATE TABLE `config_properties`
(
    `id`          bigint       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `key`         varchar(200) NOT NULL COMMENT '配置键',
    `value`       text COMMENT '配置值',
    `description` varchar(500) DEFAULT NULL COMMENT '描述',
    `group`       varchar(100) DEFAULT 'DEFAULT_GROUP' COMMENT '配置分组',
    `type`        varchar(50)  DEFAULT 'STRING' COMMENT '值类型(STRING/NUMBER/BOOLEAN/JSON)',
    `enabled`     tinyint(1) DEFAULT '1' COMMENT '是否启用',
    `create_time` datetime     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`   varchar(64)  DEFAULT NULL COMMENT '创建人',
    `update_by`   varchar(64)  DEFAULT NULL COMMENT '更新人',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_key` (`key`),
    KEY           `idx_group` (`group`),
    KEY           `idx_enabled` (`enabled`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='配置属性表';

-- forge_admin_new.gen_datasource definition

CREATE TABLE `gen_datasource`
(
    `datasource_id`     bigint       NOT NULL AUTO_INCREMENT COMMENT '数据源ID',
    `datasource_name`   varchar(100) NOT NULL COMMENT '数据源名称',
    `datasource_code`   varchar(50)  NOT NULL COMMENT '数据源编码（唯一标识）',
    `db_type`           varchar(20)  NOT NULL DEFAULT 'MySQL' COMMENT '数据库类型：MySQL/Oracle/PostgreSQL/SQLServer',
    `driver_class_name` varchar(200) NOT NULL COMMENT '驱动类名',
    `url`               varchar(500) NOT NULL COMMENT 'JDBC连接地址',
    `username`          varchar(100) NOT NULL COMMENT '用户名',
    `password`          varchar(200) NOT NULL COMMENT '密码（加密存储）',
    `is_default`        tinyint               DEFAULT '0' COMMENT '是否默认数据源（1-是，0-否）',
    `is_enabled`        tinyint               DEFAULT '1' COMMENT '是否启用（1-启用，0-禁用）',
    `test_query`        varchar(200)          DEFAULT 'SELECT 1' COMMENT '测试查询SQL',
    `sort`              int                   DEFAULT '0' COMMENT '排序',
    `remark`            varchar(500)          DEFAULT NULL COMMENT '备注',
    `create_time`       datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`       datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`         varchar(64)           DEFAULT NULL COMMENT '创建者',
    `update_by`         varchar(64)           DEFAULT NULL COMMENT '更新者',
    PRIMARY KEY (`datasource_id`),
    UNIQUE KEY `uk_datasource_code` (`datasource_code`),
    KEY                 `idx_is_default` (`is_default`),
    KEY                 `idx_is_enabled` (`is_enabled`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='代码生成器数据源配置';


-- forge_admin_new.gen_table definition

CREATE TABLE `gen_table`
(
    `table_id`        bigint       NOT NULL AUTO_INCREMENT COMMENT '表ID',
    `datasource_id`   bigint unsigned DEFAULT NULL COMMENT '数据源ID',
    `table_name`      varchar(200) NOT NULL COMMENT '表名称',
    `table_comment`   varchar(500)          DEFAULT NULL COMMENT '表描述',
    `class_name`      varchar(100) NOT NULL COMMENT '实体类名称',
    `business_name`   varchar(100)          DEFAULT NULL COMMENT '业务名称',
    `function_name`   varchar(100)          DEFAULT NULL COMMENT '功能名称',
    `module_name`     varchar(100)          DEFAULT 'system' COMMENT '模块名称',
    `package_name`    varchar(200)          DEFAULT 'com.mdframe.forge.plugin' COMMENT '包路径',
    `author`          varchar(50)           DEFAULT 'Forge Generator' COMMENT '作者',
    `gen_type`        varchar(20)           DEFAULT 'DOWNLOAD' COMMENT '生成方式：DOWNLOAD-下载代码包/PROJECT-直接生成到项目',
    `gen_path`        varchar(500)          DEFAULT '/' COMMENT '生成路径',
    `template_engine` varchar(20)           DEFAULT 'VELOCITY' COMMENT '模板引擎：VELOCITY/FREEMARKER/AI',
    `options`         json                  DEFAULT NULL COMMENT '其他生成选项',
    `remark`          varchar(500)          DEFAULT NULL COMMENT '备注',
    `create_time`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`       varchar(64)           DEFAULT NULL COMMENT '创建者',
    `update_by`       varchar(64)           DEFAULT NULL COMMENT '更新者',
    PRIMARY KEY (`table_id`),
    UNIQUE KEY `uk_table_name` (`table_name`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='代码生成表配置';


-- forge_admin_new.gen_table_column definition

CREATE TABLE `gen_table_column`
(
    `column_id`      bigint       NOT NULL AUTO_INCREMENT COMMENT '字段ID',
    `table_id`       bigint       NOT NULL COMMENT '表ID',
    `column_name`    varchar(200) NOT NULL COMMENT '字段名称',
    `column_comment` varchar(500)          DEFAULT NULL COMMENT '字段描述',
    `column_type`    varchar(100) NOT NULL COMMENT '字段类型',
    `java_type`      varchar(50)           DEFAULT 'String' COMMENT 'Java类型',
    `java_field`     varchar(100)          DEFAULT NULL COMMENT 'Java字段名',
    `is_pk`          tinyint               DEFAULT '0' COMMENT '是否主键',
    `is_increment`   tinyint               DEFAULT '0' COMMENT '是否自增',
    `is_required`    tinyint               DEFAULT '0' COMMENT '是否必填',
    `is_insert`      tinyint               DEFAULT '1' COMMENT '是否插入字段',
    `is_edit`        tinyint               DEFAULT '1' COMMENT '是否编辑字段',
    `is_list`        tinyint               DEFAULT '1' COMMENT '是否列表字段',
    `is_query`       tinyint               DEFAULT '0' COMMENT '是否查询字段',
    `query_type`     varchar(20)           DEFAULT 'EQ' COMMENT '查询方式：EQ/LIKE/BETWEEN/GT/LT等',
    `html_type`      varchar(20)           DEFAULT 'INPUT' COMMENT '显示类型：INPUT/SELECT/DATETIME/TEXTAREA等',
    `dict_type`      varchar(100)          DEFAULT NULL COMMENT '字典类型',
    `validate_rule`  varchar(500)          DEFAULT NULL COMMENT '验证规则（JSON格式）',
    `ai_recommended` tinyint(1) DEFAULT '0' COMMENT '是否AI推荐（0否 1是）',
    `sort`           int                   DEFAULT '0' COMMENT '排序',
    `create_time`    datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`    datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`column_id`),
    KEY              `idx_table_id` (`table_id`)
) ENGINE=InnoDB AUTO_INCREMENT=60 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='代码生成表字段配置';


-- forge_admin_new.gen_template definition

CREATE TABLE `gen_template`
(
    `template_id`      bigint       NOT NULL AUTO_INCREMENT COMMENT '模板ID',
    `template_name`    varchar(100) NOT NULL COMMENT '模板名称',
    `template_code`    varchar(50)  NOT NULL COMMENT '模板编码',
    `template_type`    varchar(20)  NOT NULL COMMENT '模板类型：ENTITY/MAPPER/SERVICE/CONTROLLER/DTO/VO/SQL',
    `template_engine`  varchar(20)           DEFAULT 'VELOCITY' COMMENT '模板引擎',
    `template_content` longtext     NOT NULL COMMENT '模板内容',
    `file_suffix`      varchar(20)           DEFAULT '.java' COMMENT '生成文件后缀',
    `file_path`        varchar(200)          DEFAULT NULL COMMENT '生成文件路径（相对路径）',
    `is_system`        tinyint               DEFAULT '1' COMMENT '是否系统内置',
    `is_enabled`       tinyint               DEFAULT '1' COMMENT '是否启用',
    `sort`             int                   DEFAULT '0' COMMENT '排序',
    `remark`           varchar(500)          DEFAULT NULL COMMENT '备注',
    `create_time`      datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`      datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `tenant_id`        bigint unsigned DEFAULT NULL,
    PRIMARY KEY (`template_id`),
    UNIQUE KEY `uk_template_code` (`template_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='代码生成模板配置';


-- forge_admin_new.order_manage definition

CREATE TABLE `order_manage`
(
    `id`             bigint      NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `tenant_id`      bigint         DEFAULT NULL COMMENT '租户ID',
    `order_no`       varchar(64) NOT NULL COMMENT '订单编号',
    `customer_name`  varchar(100)   DEFAULT NULL COMMENT '客户姓名',
    `customer_phone` varchar(20)    DEFAULT NULL COMMENT '客户电话',
    `order_amount`   decimal(10, 2) DEFAULT NULL COMMENT '订单金额',
    `order_status`   varchar(20)    DEFAULT '1' COMMENT '订单状态(1:待付款,2:已付款,3:已发货,4:已完成,5:已取消)',
    `order_type`     varchar(20)    DEFAULT '1' COMMENT '订单类型(1:普通订单,2:团购订单,3:秒杀订单)',
    `pay_time`       datetime       DEFAULT NULL COMMENT '支付时间',
    `remark`         varchar(500)   DEFAULT NULL COMMENT '备注',
    `create_by`      varchar(64)    DEFAULT NULL COMMENT '创建人',
    `create_time`    datetime       DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_dept`    bigint         DEFAULT NULL COMMENT '创建部门',
    `update_by`      varchar(64)    DEFAULT NULL COMMENT '更新人',
    `update_time`    datetime       DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `del_flag`       tinyint(1) DEFAULT '0' COMMENT '删除标志(0:正常,1:删除)',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`order_no`),
    KEY              `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='订单管理表';


-- forge_admin_new.qrtz_calendars definition

CREATE TABLE `qrtz_calendars`
(
    `sched_name`    varchar(120) NOT NULL COMMENT '调度名称',
    `calendar_name` varchar(200) NOT NULL COMMENT '日历名称',
    `calendar`      blob         NOT NULL COMMENT '存放持久化calendar对象',
    PRIMARY KEY (`sched_name`, `calendar_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='日历信息表';


-- forge_admin_new.qrtz_fired_triggers definition

CREATE TABLE `qrtz_fired_triggers`
(
    `sched_name`        varchar(120) NOT NULL COMMENT '调度名称',
    `entry_id`          varchar(95)  NOT NULL COMMENT '调度器实例id',
    `trigger_name`      varchar(200) NOT NULL COMMENT 'qrtz_triggers表trigger_name的外键',
    `trigger_group`     varchar(200) NOT NULL COMMENT 'qrtz_triggers表trigger_group的外键',
    `instance_name`     varchar(200) NOT NULL COMMENT '调度器实例名',
    `fired_time`        bigint       NOT NULL COMMENT '触发的时间',
    `sched_time`        bigint       NOT NULL COMMENT '定时器制定的时间',
    `priority`          int          NOT NULL COMMENT '优先级',
    `state`             varchar(16)  NOT NULL COMMENT '状态',
    `job_name`          varchar(200) DEFAULT NULL COMMENT '任务名称',
    `job_group`         varchar(200) DEFAULT NULL COMMENT '任务组名',
    `is_nonconcurrent`  varchar(1)   DEFAULT NULL COMMENT '是否并发',
    `requests_recovery` varchar(1)   DEFAULT NULL COMMENT '是否接受恢复执行',
    PRIMARY KEY (`sched_name`, `entry_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='已触发的触发器表';


-- forge_admin_new.qrtz_job_details definition

CREATE TABLE `qrtz_job_details`
(
    `sched_name`        varchar(120) NOT NULL COMMENT '调度名称',
    `job_name`          varchar(200) NOT NULL COMMENT '任务名称',
    `job_group`         varchar(200) NOT NULL COMMENT '任务组名',
    `description`       varchar(250) DEFAULT NULL COMMENT '相关介绍',
    `job_class_name`    varchar(250) NOT NULL COMMENT '执行任务类名称',
    `is_durable`        varchar(1)   NOT NULL COMMENT '是否持久化',
    `is_nonconcurrent`  varchar(1)   NOT NULL COMMENT '是否并发',
    `is_update_data`    varchar(1)   NOT NULL COMMENT '是否更新数据',
    `requests_recovery` varchar(1)   NOT NULL COMMENT '是否接受恢复执行',
    `job_data`          blob COMMENT '存放持久化job对象',
    PRIMARY KEY (`sched_name`, `job_name`, `job_group`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='任务详细信息表';


-- forge_admin_new.qrtz_locks definition

CREATE TABLE `qrtz_locks`
(
    `sched_name` varchar(120) NOT NULL COMMENT '调度名称',
    `lock_name`  varchar(40)  NOT NULL COMMENT '悲观锁名称',
    PRIMARY KEY (`sched_name`, `lock_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='存储的悲观锁信息表';


-- forge_admin_new.qrtz_paused_trigger_grps definition

CREATE TABLE `qrtz_paused_trigger_grps`
(
    `sched_name`    varchar(120) NOT NULL COMMENT '调度名称',
    `trigger_group` varchar(200) NOT NULL COMMENT 'qrtz_triggers表trigger_group的外键',
    PRIMARY KEY (`sched_name`, `trigger_group`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='暂停的触发器表';


-- forge_admin_new.qrtz_scheduler_state definition

CREATE TABLE `qrtz_scheduler_state`
(
    `sched_name`        varchar(120) NOT NULL COMMENT '调度名称',
    `instance_name`     varchar(200) NOT NULL COMMENT '实例名称',
    `last_checkin_time` bigint       NOT NULL COMMENT '上次检查时间',
    `checkin_interval`  bigint       NOT NULL COMMENT '检查间隔时间',
    PRIMARY KEY (`sched_name`, `instance_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='调度器状态表';


-- forge_admin_new.sys_api_config definition

CREATE TABLE `sys_api_config`
(
    `id`               bigint       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `api_name`         varchar(100) NOT NULL COMMENT '接口名称 (如:查询用户信息)',
    `api_code`         varchar(100) DEFAULT NULL COMMENT '接口编码 (用于程序逻辑引用)',
    `req_method`       varchar(20)  NOT NULL COMMENT '请求方式 (GET, POST, PUT, DELETE, ALL)',
    `url_path`         varchar(255) NOT NULL COMMENT '接口请求路径 (支持Ant风格, 如 /api/user/**)',
    `api_version`      varchar(20)  DEFAULT 'v1.0.0' COMMENT '接口版本号',
    `module_code`      varchar(50)  NOT NULL COMMENT '所属业务模块 (如: sys, order, pay)',
    `service_id`       varchar(100) DEFAULT NULL COMMENT '所属微服务ID (若为微服务架构)',
    `auth_flag`        tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否需认证/鉴权 (Token校验)',
    `encrypt_flag`     tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否需报文加解密 (敏感数据保护)',
    `tenant_flag`      tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否启用租户隔离 (数据权限)',
    `limit_flag`       tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否开启限流',
    `sensitive_fields` text COMMENT '需脱敏字段 (JSON数组存储, 如 ["phone", "id_card"])',
    `status`           tinyint(1) NOT NULL DEFAULT '1' COMMENT '状态 (1-正常, 0-停用)',
    `remark`           varchar(500) DEFAULT NULL COMMENT '备注说明',
    `create_by`        varchar(64)  DEFAULT NULL COMMENT '创建者',
    `create_time`      datetime     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`        varchar(64)  DEFAULT NULL COMMENT '更新者',
    `update_time`      datetime     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `tenant_id`        bigint       DEFAULT NULL COMMENT '租户ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_method_url` (`url_path`,`req_method`) USING BTREE COMMENT '路径与方法组合唯一索引',
    KEY                `idx_module` (`module_code`) USING BTREE COMMENT '模块查询索引',
    KEY                `idx_tenant_id` (`tenant_id`) USING BTREE COMMENT '租户索引'
) ENGINE=InnoDB AUTO_INCREMENT=963 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='REST接口配置管理表';


-- forge_admin_new.sys_auth_online_user definition

CREATE TABLE `sys_auth_online_user`
(
    `id`                 bigint       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `token_value`        varchar(255) NOT NULL COMMENT 'Token值',
    `user_id`            bigint       NOT NULL COMMENT '用户ID',
    `username`           varchar(100) NOT NULL COMMENT '用户名',
    `real_name`          varchar(100) DEFAULT NULL COMMENT '真实姓名',
    `dept_name`          varchar(100) DEFAULT NULL COMMENT '部门名称',
    `ip_address`         varchar(50)  DEFAULT NULL COMMENT '登录IP地址',
    `login_location`     varchar(100) DEFAULT NULL COMMENT '登录地点',
    `browser`            varchar(50)  DEFAULT NULL COMMENT '浏览器类型',
    `os`                 varchar(50)  DEFAULT NULL COMMENT '操作系统',
    `login_time`         datetime     NOT NULL COMMENT '登录时间',
    `last_activity_time` datetime     DEFAULT NULL COMMENT '最后活动时间',
    `expire_time`        datetime     DEFAULT NULL COMMENT 'Token过期时间',
    `status`             tinyint      DEFAULT '1' COMMENT '状态: 1-在线, 0-已下线',
    `logout_time`        datetime     DEFAULT NULL COMMENT '登出时间',
    `logout_type`        tinyint      DEFAULT NULL COMMENT '登出类型: 1-主动登出, 2-被踢下线, 3-被顶下线, 4-Token过期',
    `tenant_id`          bigint       DEFAULT NULL COMMENT '租户ID',
    `create_time`        datetime     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`        datetime     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_token` (`token_value`),
    KEY                  `idx_user_id` (`user_id`),
    KEY                  `idx_status` (`status`),
    KEY                  `idx_login_time` (`login_time`),
    KEY                  `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB AUTO_INCREMENT=970 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='在线用户表';


-- forge_admin_new.sys_auth_online_user_history definition

CREATE TABLE `sys_auth_online_user_history`
(
    `id`              bigint       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id`         bigint       NOT NULL COMMENT '用户ID',
    `username`        varchar(100) NOT NULL COMMENT '用户名',
    `ip_address`      varchar(50)  DEFAULT NULL COMMENT '登录IP地址',
    `login_location`  varchar(100) DEFAULT NULL COMMENT '登录地点',
    `browser`         varchar(50)  DEFAULT NULL COMMENT '浏览器类型',
    `os`              varchar(50)  DEFAULT NULL COMMENT '操作系统',
    `login_time`      datetime     NOT NULL COMMENT '登录时间',
    `logout_time`     datetime     DEFAULT NULL COMMENT '登出时间',
    `online_duration` int          DEFAULT NULL COMMENT '在线时长(秒)',
    `logout_type`     tinyint      DEFAULT NULL COMMENT '登出类型: 1-主动登出, 2-被踢下线, 3-被顶下线, 4-Token过期',
    `tenant_id`       bigint       DEFAULT NULL COMMENT '租户ID',
    `create_time`     datetime     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY               `idx_user_id` (`user_id`),
    KEY               `idx_login_time` (`login_time`),
    KEY               `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='在线用户历史表';


-- forge_admin_new.sys_client definition

CREATE TABLE `sys_client`
(
    `id`                     bigint       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `client_code`            varchar(50)  NOT NULL COMMENT '客户端编码',
    `client_name`            varchar(100) NOT NULL COMMENT '客户端名称',
    `app_id`                 varchar(64)  NOT NULL COMMENT '应用ID',
    `app_secret`             varchar(128) NOT NULL COMMENT '应用密钥（加密存储）',
    `token_timeout`          bigint       DEFAULT '7200' COMMENT 'Token有效期（秒）',
    `token_activity_timeout` bigint       DEFAULT '-1' COMMENT 'Token活跃超时（秒）',
    `token_prefix`           varchar(20)  DEFAULT 'Bearer' COMMENT 'Token前缀',
    `token_name`             varchar(50)  DEFAULT 'Authorization' COMMENT 'Token名称',
    `concurrent_login`       tinyint(1) DEFAULT '0' COMMENT '是否允许并发登录（0-否 1-是）',
    `share_token`            tinyint(1) DEFAULT '0' COMMENT '是否共享Token（0-否 1-是）',
    `enable_ip_limit`        tinyint(1) DEFAULT '0' COMMENT '是否启用IP限制',
    `ip_whitelist`           text COMMENT 'IP白名单（JSON数组）',
    `enable_encrypt`         tinyint(1) DEFAULT '0' COMMENT '是否启用加密传输',
    `encrypt_algorithm`      varchar(50)  DEFAULT 'AES' COMMENT '加密算法',
    `max_user_count`         bigint       DEFAULT '-1' COMMENT '最大用户数（-1不限）',
    `max_online_count`       bigint       DEFAULT '-1' COMMENT '最大在线数（-1不限）',
    `auth_types`             varchar(255) DEFAULT 'password' COMMENT '支持的认证方式（逗号分隔）',
    `status`                 tinyint      DEFAULT '1' COMMENT '状态（0-禁用 1-启用）',
    `description`            varchar(500) DEFAULT NULL COMMENT '客户端描述',
    `tenant_id`              bigint       DEFAULT NULL COMMENT '租户ID',
    `create_time`            datetime     DEFAULT CURRENT_TIMESTAMP,
    `create_by`              bigint       DEFAULT NULL,
    `update_time`            datetime     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `update_by`              bigint       DEFAULT NULL,
    `create_dept`            bigint       DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_client_code` (`client_code`),
    UNIQUE KEY `uk_app_id` (`app_id`),
    KEY                      `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='客户端管理表';


-- forge_admin_new.sys_config definition

CREATE TABLE `sys_config`
(
    `config_id`    bigint   NOT NULL AUTO_INCREMENT COMMENT '参数主键',
    `tenant_id`    bigint            DEFAULT '0' COMMENT '租户编号',
    `config_name`  varchar(100)      DEFAULT '' COMMENT '参数名称',
    `config_key`   varchar(100)      DEFAULT '' COMMENT '参数键名',
    `config_value` varchar(500)      DEFAULT '' COMMENT '参数键值',
    `config_type`  char(1)           DEFAULT 'N' COMMENT '系统内置（Y是 N否）',
    `config_desc`  varchar(500)      DEFAULT NULL COMMENT '参数描述',
    `sort`         int               DEFAULT '0' COMMENT '排序',
    `create_by`    bigint            DEFAULT NULL COMMENT '创建者',
    `create_time`  datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`    bigint            DEFAULT NULL COMMENT '更新者',
    `update_time`  datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_dept`  bigint unsigned DEFAULT NULL COMMENT '创建组织ID',
    PRIMARY KEY (`config_id`),
    UNIQUE KEY `uk_tenant_config_key` (`tenant_id`,`config_key`),
    KEY            `idx_config_type` (`config_type`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统配置表';


-- forge_admin_new.sys_config_group definition

CREATE TABLE `sys_config_group`
(
    `id`           bigint                                                        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `group_code`   varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci  NOT NULL COMMENT '分组编码',
    `group_name`   varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '分组名称',
    `group_icon`   varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci  DEFAULT NULL COMMENT '分组图标',
    `config_value` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '配置值(JSON格式)',
    `sort`         int                                                           DEFAULT '0' COMMENT '排序',
    `status`       tinyint                                                       DEFAULT '1' COMMENT '状态(0-禁用 1-启用)',
    `remark`       varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
    `create_time`  datetime                                                      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`  datetime                                                      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE KEY `uk_group_code` (`group_code`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统配置分组表';


-- forge_admin_new.sys_data_scope_config definition

CREATE TABLE `sys_data_scope_config`
(
    `id`                 bigint       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `tenant_id`          bigint       NOT NULL DEFAULT '0' COMMENT '租户编号',
    `resource_code`      varchar(100) NOT NULL COMMENT '资源编码（对应接口路径或功能模块）',
    `resource_name`      varchar(100)          DEFAULT NULL COMMENT '资源名称',
    `mapper_method`      varchar(200) NOT NULL COMMENT 'Mapper方法（如：com.example.mapper.UserMapper.selectList）',
    `table_alias`        varchar(50)           DEFAULT 't' COMMENT '主表别名',
    `user_id_column`     text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '用户ID字段名',
    `org_id_column`      text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '组织ID字段名',
    `tenant_id_column`   text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '租户ID字段名',
    `region_code_column` varchar(250)          DEFAULT NULL COMMENT '行政区划字段名',
    `user_region_column` varchar(200)          DEFAULT NULL COMMENT '用户表行政区划字段名',
    `user_table_alias`   varchar(100)          DEFAULT NULL COMMENT '用户表别名',
    `enabled`            tinyint               DEFAULT '1' COMMENT '是否启用（0-禁用，1-启用）',
    `remark`             varchar(500)          DEFAULT NULL COMMENT '备注',
    `create_by`          bigint                DEFAULT NULL COMMENT '创建者',
    `create_time`        datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`          bigint                DEFAULT NULL COMMENT '更新者',
    `update_time`        datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_dept`        bigint unsigned DEFAULT NULL COMMENT '创建组织ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_mapper` (`tenant_id`,`mapper_method`),
    KEY                  `idx_resource_code` (`resource_code`),
    KEY                  `idx_enabled` (`enabled`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='数据权限配置表';


-- forge_admin_new.sys_dict_data definition

CREATE TABLE `sys_dict_data`
(
    `dict_code`         bigint   NOT NULL AUTO_INCREMENT COMMENT '字典编码',
    `tenant_id`         bigint            DEFAULT '0' COMMENT '租户编号',
    `dict_sort`         int               DEFAULT '0' COMMENT '字典排序',
    `dict_label`        varchar(100)      DEFAULT '' COMMENT '字典标签',
    `dict_value`        varchar(100)      DEFAULT '' COMMENT '字典键值',
    `dict_type`         varchar(100)      DEFAULT '' COMMENT '字典类型',
    `css_class`         varchar(100)      DEFAULT NULL COMMENT '样式属性（其他样式扩展）',
    `list_class`        varchar(100)      DEFAULT NULL COMMENT '表格回显样式',
    `is_default`        char(1)           DEFAULT 'N' COMMENT '是否默认（Y是 N否）',
    `parent_dict_code`  bigint unsigned DEFAULT NULL COMMENT '上级字典编码',
    `linked_dict_type`  varchar(100)      DEFAULT NULL COMMENT '关联的字典类型',
    `linked_dict_value` varchar(100)      DEFAULT NULL COMMENT '关联的字典值',
    `dict_status`       tinyint           DEFAULT '1' COMMENT '字典状态（0-禁用，1-正常）',
    `remark`            varchar(500)      DEFAULT NULL COMMENT '备注',
    `create_by`         bigint            DEFAULT NULL COMMENT '创建者',
    `create_time`       datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`         bigint            DEFAULT NULL COMMENT '更新者',
    `update_time`       datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_dept`       bigint unsigned DEFAULT NULL COMMENT '创建组织ID',
    PRIMARY KEY (`dict_code`),
    UNIQUE KEY `uk_tenant_dict_data` (`tenant_id`,`dict_type`,`dict_value`),
    KEY                 `idx_tenant_dict_type` (`tenant_id`,`dict_type`),
    KEY                 `idx_parent_dict_code` (`parent_dict_code`),
    KEY                 `idx_linked_dict` (`linked_dict_type`,`linked_dict_value`)
) ENGINE=InnoDB AUTO_INCREMENT=99 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='字典数据表';


-- forge_admin_new.sys_dict_type definition

CREATE TABLE `sys_dict_type`
(
    `dict_id`     bigint   NOT NULL AUTO_INCREMENT COMMENT '字典主键',
    `tenant_id`   bigint            DEFAULT '0' COMMENT '租户编号',
    `dict_name`   varchar(100)      DEFAULT '' COMMENT '字典名称',
    `dict_type`   varchar(100)      DEFAULT '' COMMENT '字典类型（租户内唯一）',
    `dict_status` tinyint           DEFAULT '1' COMMENT '字典状态（0-禁用，1-正常）',
    `remark`      varchar(500)      DEFAULT NULL COMMENT '备注',
    `create_by`   bigint            DEFAULT NULL COMMENT '创建者',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`   bigint            DEFAULT NULL COMMENT '更新者',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_dept` bigint unsigned DEFAULT NULL COMMENT '创建组织ID',
    PRIMARY KEY (`dict_id`),
    UNIQUE KEY `uk_tenant_dict_type` (`tenant_id`,`dict_type`),
    KEY           `idx_dict_status` (`dict_status`)
) ENGINE=InnoDB AUTO_INCREMENT=34 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='字典类型表';


-- forge_admin_new.sys_email_config definition

CREATE TABLE `sys_email_config`
(
    `id`             bigint                                  NOT NULL AUTO_INCREMENT COMMENT '主键',
    `config_id`      varchar(64) COLLATE utf8mb4_unicode_ci  DEFAULT NULL COMMENT '配置标识',
    `smtp_server`    varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'SMTP服务器地址',
    `port`           int                                     DEFAULT '465' COMMENT '端口',
    `username`       varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '用户名',
    `password`       varchar(256) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '密码/授权码',
    `from_address`   varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '发件人地址',
    `from_name`      varchar(64) COLLATE utf8mb4_unicode_ci  DEFAULT NULL COMMENT '发件人名称',
    `is_ssl`         tinyint                                 DEFAULT '1' COMMENT '是否开启SSL',
    `is_auth`        tinyint                                 DEFAULT '1' COMMENT '是否开启验证',
    `retry_interval` int                                     DEFAULT '5' COMMENT '重试间隔(秒)',
    `max_retries`    int                                     DEFAULT '1' COMMENT '最大重试次数',
    `status`         tinyint                                 DEFAULT '0' COMMENT '状态(0禁用 1启用)',
    `tenant_id`      bigint                                  DEFAULT NULL COMMENT '租户ID',
    `create_time`    datetime                                DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`    datetime                                DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`      varchar(64) COLLATE utf8mb4_unicode_ci  DEFAULT NULL COMMENT '创建人',
    `update_by`      varchar(64) COLLATE utf8mb4_unicode_ci  DEFAULT NULL COMMENT '更新人',
    `remark`         varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`id`),
    KEY              `idx_tenant_status` (`tenant_id`,`status`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='邮件配置表';


-- forge_admin_new.sys_employee definition

CREATE TABLE `sys_employee`
(
    `id`          bigint       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `emp_name`    varchar(100) NOT NULL COMMENT '姓名',
    `emp_no`      varchar(50)  NOT NULL COMMENT '工号',
    `dept_id`     bigint       DEFAULT NULL COMMENT '部门ID',
    `position`    varchar(100) DEFAULT NULL COMMENT '职位',
    `hire_date`   date         DEFAULT NULL COMMENT '入职日期',
    `phone`       varchar(20)  DEFAULT NULL COMMENT '手机号',
    `email`       varchar(100) DEFAULT NULL COMMENT '邮箱',
    `status`      tinyint(1) DEFAULT '1' COMMENT '状态(1:正常,0:停用)',
    `tenant_id`   bigint       DEFAULT NULL COMMENT '租户ID',
    `create_by`   varchar(64)  DEFAULT NULL COMMENT '创建者',
    `create_time` datetime     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_dept` bigint       DEFAULT NULL COMMENT '创建部门',
    `update_by`   varchar(64)  DEFAULT NULL COMMENT '更新者',
    `update_time` datetime     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `del_flag`    tinyint(1) DEFAULT '0' COMMENT '删除标志(0:未删除,1:已删除)',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_emp_no` (`emp_no`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='员工信息表';


-- forge_admin_new.sys_excel_column_config definition

CREATE TABLE `sys_excel_column_config`
(
    `id`            bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `config_key`    varchar(100) NOT NULL COMMENT '关联配置键',
    `field_name`    varchar(100) NOT NULL COMMENT '字段名（实体属性名）',
    `column_name`   varchar(200) NOT NULL COMMENT '列名（Excel表头）',
    `width`         int         DEFAULT '20' COMMENT '列宽',
    `order_num`     int         DEFAULT '0' COMMENT '排序（值越小越靠前）',
    `export`        tinyint(1) DEFAULT '1' COMMENT '是否导出（1-是，0-否）',
    `date_format`   varchar(50) DEFAULT NULL COMMENT '日期格式（如：yyyy-MM-dd）',
    `number_format` varchar(50) DEFAULT NULL COMMENT '数字格式',
    `dict_type`     varchar(50) DEFAULT NULL COMMENT '字典类型（关联sys_dict_type）',
    `create_time`   datetime    DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   datetime    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`     bigint      DEFAULT NULL COMMENT '创建者',
    `update_by`     bigint      DEFAULT NULL COMMENT '更新者',
    `create_dept`   bigint unsigned DEFAULT NULL COMMENT '创建组织ID',
    PRIMARY KEY (`id`),
    KEY             `idx_config_key` (`config_key`),
    KEY             `idx_order` (`order_num`)
) ENGINE=InnoDB AUTO_INCREMENT=17 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Excel列配置表';


-- forge_admin_new.sys_excel_export_config definition

CREATE TABLE `sys_excel_export_config`
(
    `id`                 bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `config_key`         varchar(100) NOT NULL COMMENT '配置键（唯一标识）',
    `export_name`        varchar(200) NOT NULL COMMENT '导出名称',
    `sheet_name`         varchar(100) DEFAULT 'Sheet1' COMMENT 'Sheet名称',
    `file_name_template` varchar(200) DEFAULT NULL COMMENT '文件名模板（支持占位符：{date}、{time}）',
    `data_source_bean`   varchar(100) NOT NULL COMMENT '数据源Bean名称（如：userService）',
    `query_method`       varchar(100) NOT NULL COMMENT '数据查询方法名（如：list、page）',
    `auto_trans`         tinyint(1) DEFAULT '1' COMMENT '是否自动翻译字典（1-是，0-否）',
    `pageable`           tinyint(1) DEFAULT '0' COMMENT '是否分页查询（1-是，0-否）',
    `max_rows`           int          DEFAULT '10000' COMMENT '最大导出条数',
    `sort_field`         varchar(50)  DEFAULT NULL COMMENT '排序字段',
    `sort_order`         varchar(10)  DEFAULT NULL COMMENT '排序方向（ASC/DESC）',
    `status`             tinyint(1) DEFAULT '1' COMMENT '状态（1-启用，0-禁用）',
    `remark`             varchar(500) DEFAULT NULL COMMENT '备注',
    `create_time`        datetime     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`        datetime     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`          bigint       DEFAULT NULL COMMENT '创建者',
    `update_by`          bigint       DEFAULT NULL COMMENT '更新者',
    `create_dept`        bigint unsigned DEFAULT NULL COMMENT '创建组织ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `config_key` (`config_key`),
    KEY                  `idx_config_key` (`config_key`)
) ENGINE=InnoDB AUTO_INCREMENT=2013984223186178051 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Excel导出配置表';


-- forge_admin_new.sys_file_group definition

CREATE TABLE `sys_file_group`
(
    `id`          bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `group_name`  varchar(100) NOT NULL COMMENT '分组名称',
    `group_code`  varchar(100) DEFAULT NULL COMMENT '分组编码',
    `group_type`  varchar(50)  DEFAULT 'default' COMMENT '分组类型(document-文档,image-图片,video-视频,audio-音频,archive-压缩包,default-默认)',
    `parent_id`   bigint       DEFAULT NULL COMMENT '父分组ID',
    `sort`        int          DEFAULT '0' COMMENT '排序',
    `icon`        varchar(100) DEFAULT NULL COMMENT '图标',
    `description` varchar(500) DEFAULT NULL COMMENT '描述',
    `status`      tinyint(1) DEFAULT '1' COMMENT '状态(1-正常,0-禁用)',
    `create_time` datetime     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`   bigint       DEFAULT NULL COMMENT '创建者',
    `update_by`   bigint       DEFAULT NULL COMMENT '更新者',
    `create_dept` bigint unsigned DEFAULT NULL COMMENT '创建组织ID',
    `deleted`     tinyint(1) DEFAULT '0' COMMENT '删除标记(0-未删除,1-已删除)',
    PRIMARY KEY (`id`),
    KEY           `idx_parent_id` (`parent_id`),
    KEY           `idx_group_type` (`group_type`),
    KEY           `idx_group_code` (`group_code`)
) ENGINE=InnoDB AUTO_INCREMENT=2028326543566475267 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='文件分组表';


-- forge_admin_new.sys_file_metadata definition

CREATE TABLE `sys_file_metadata`
(
    `id`             bigint        NOT NULL AUTO_INCREMENT COMMENT '主键',
    `file_id`        varchar(64)   NOT NULL COMMENT '文件唯一ID',
    `group_id`       bigint        DEFAULT NULL COMMENT '分组ID',
    `original_name`  varchar(500)  NOT NULL COMMENT '原始文件名',
    `storage_name`   varchar(500)  NOT NULL COMMENT '存储文件名',
    `file_path`      varchar(1000) NOT NULL COMMENT '文件路径',
    `file_size`      bigint        NOT NULL COMMENT '文件大小(字节)',
    `mime_type`      varchar(200)  DEFAULT NULL COMMENT '文件MIME类型',
    `extension`      varchar(50)   DEFAULT NULL COMMENT '文件扩展名',
    `md5`            varchar(64)   DEFAULT NULL COMMENT '文件MD5',
    `storage_type`   varchar(50)   NOT NULL COMMENT '存储策略',
    `bucket`         varchar(100)  DEFAULT NULL COMMENT '存储桶/命名空间',
    `access_url`     varchar(1000) DEFAULT NULL COMMENT '访问URL',
    `thumbnail_url`  varchar(1000) DEFAULT NULL COMMENT '缩略图URL',
    `business_type`  varchar(100)  DEFAULT NULL COMMENT '业务类型',
    `business_id`    varchar(100)  DEFAULT NULL COMMENT '业务ID',
    `uploader_id`    bigint        DEFAULT NULL COMMENT '上传者ID',
    `upload_time`    datetime      DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
    `expire_time`    datetime      DEFAULT NULL COMMENT '过期时间',
    `is_private`     tinyint(1) DEFAULT '0' COMMENT '是否私有',
    `download_count` int           DEFAULT '0' COMMENT '下载次数',
    `status`         tinyint(1) DEFAULT '1' COMMENT '状态(1-正常,0-已删除)',
    `create_time`    datetime      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`    datetime      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`      bigint        DEFAULT NULL COMMENT '创建者',
    `update_by`      bigint        DEFAULT NULL COMMENT '更新者',
    `create_dept`    bigint unsigned DEFAULT NULL COMMENT '创建组织ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `file_id` (`file_id`),
    KEY              `idx_file_id` (`file_id`),
    KEY              `idx_md5` (`md5`),
    KEY              `idx_business` (`business_type`,`business_id`),
    KEY              `idx_uploader` (`uploader_id`),
    KEY              `idx_upload_time` (`upload_time`)
) ENGINE=InnoDB AUTO_INCREMENT=2049682101418110978 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='文件元数据表';


-- forge_admin_new.sys_file_storage_config definition

CREATE TABLE `sys_file_storage_config`
(
    `id`            bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `config_name`   varchar(100) NOT NULL COMMENT '配置名称',
    `storage_type`  varchar(50)  NOT NULL COMMENT '存储类型(local/minio/aliyun_oss等)',
    `is_default`    tinyint(1) DEFAULT '0' COMMENT '是否默认策略',
    `enabled`       tinyint(1) DEFAULT '1' COMMENT '是否启用',
    `endpoint`      varchar(500) DEFAULT NULL COMMENT '访问端点',
    `access_key`    varchar(200) DEFAULT NULL COMMENT '访问密钥ID',
    `secret_key`    varchar(200) DEFAULT NULL COMMENT '访问密钥Secret',
    `bucket_name`   varchar(100) DEFAULT NULL COMMENT '存储桶名称',
    `region`        varchar(100) DEFAULT NULL COMMENT '区域',
    `base_path`     varchar(200) DEFAULT NULL COMMENT '基础路径',
    `domain`        varchar(500) DEFAULT NULL COMMENT '访问域名',
    `use_https`     tinyint(1) DEFAULT '1' COMMENT '是否使用HTTPS',
    `max_file_size` int          DEFAULT '100' COMMENT '最大文件大小(MB)',
    `allowed_types` varchar(500) DEFAULT NULL COMMENT '允许的文件类型(逗号分隔)',
    `order_num`     int          DEFAULT '0' COMMENT '排序',
    `extra_config`  text COMMENT '扩展配置(JSON)',
    `create_time`   datetime     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   datetime     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`     bigint       DEFAULT NULL COMMENT '创建者',
    `update_by`     bigint       DEFAULT NULL COMMENT '更新者',
    `create_dept`   bigint unsigned DEFAULT NULL COMMENT '创建组织ID',
    PRIMARY KEY (`id`),
    KEY             `idx_storage_type` (`storage_type`),
    KEY             `idx_is_default` (`is_default`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='文件存储配置表';


-- forge_admin_new.sys_flow_approval_level definition

CREATE TABLE `sys_flow_approval_level`
(
    `id`             varchar(64) NOT NULL COMMENT '主键',
    `node_config_id` varchar(64) NOT NULL COMMENT '节点配置ID',
    `level_index`    int         NOT NULL COMMENT '层级序号',
    `level_name`     varchar(100) DEFAULT NULL COMMENT '层级名称',
    `assignee_type`  varchar(50)  DEFAULT NULL COMMENT '审批人类型',
    `assignee_value` text COMMENT '审批人值',
    `condition_expr` varchar(500) DEFAULT NULL COMMENT '层级条件表达式',
    `skip_condition` varchar(500) DEFAULT NULL COMMENT '跳过条件',
    `sort_order`     int          DEFAULT '0' COMMENT '排序',
    `create_time`    datetime     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY              `idx_node_config_id` (`node_config_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='审批层级配置表';


-- forge_admin_new.sys_flow_business definition

CREATE TABLE `sys_flow_business`
(
    `id`                  varchar(64)  NOT NULL COMMENT '主键',
    `business_key`        varchar(100) NOT NULL COMMENT '业务Key',
    `business_type`       varchar(50)  NOT NULL COMMENT '业务类型（order/contract等）',
    `process_instance_id` varchar(64)  DEFAULT NULL COMMENT '流程实例ID',
    `process_def_id`      varchar(64)  DEFAULT NULL COMMENT '流程定义ID',
    `process_def_key`     varchar(100) DEFAULT NULL COMMENT '流程定义KEY',
    `title`               varchar(200) DEFAULT NULL COMMENT '流程标题',
    `status`              varchar(50)  DEFAULT 'draft' COMMENT '业务状态（draft-草稿/running-审批中/approved-已通过/rejected-已驳回/canceled-已取消）',
    `apply_user_id`       varchar(64)  DEFAULT NULL COMMENT '申请人',
    `apply_user_name`     varchar(100) DEFAULT NULL COMMENT '申请人姓名',
    `apply_dept_id`       varchar(64)  DEFAULT NULL COMMENT '申请部门ID',
    `apply_dept_name`     varchar(100) DEFAULT NULL COMMENT '申请部门名称',
    `apply_time`          datetime     DEFAULT NULL COMMENT '申请时间',
    `end_time`            datetime     DEFAULT NULL COMMENT '结束时间',
    `duration`            bigint       DEFAULT NULL COMMENT '流程耗时（毫秒）',
    `create_time`         datetime     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`         datetime     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_business_key` (`business_key`),
    KEY                   `idx_process_instance_id` (`process_instance_id`),
    KEY                   `idx_apply_user_id` (`apply_user_id`),
    KEY                   `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='流程业务关联表';


-- forge_admin_new.sys_flow_category definition

CREATE TABLE `sys_flow_category`
(
    `id`            varchar(64)  NOT NULL COMMENT '主键',
    `category_code` varchar(50)  NOT NULL COMMENT '分类编码',
    `category_name` varchar(100) NOT NULL COMMENT '分类名称',
    `description`   varchar(500) DEFAULT NULL COMMENT '描述',
    `sort_order`    int          DEFAULT '0' COMMENT '排序',
    `status`        tinyint      DEFAULT '1' COMMENT '状态（0-禁用/1-启用）',
    `create_time`   datetime     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   datetime     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `category_code` (`category_code`),
    KEY             `idx_category_code` (`category_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='流程分类表';


-- forge_admin_new.sys_flow_cc definition

CREATE TABLE `sys_flow_cc`
(
    `id`                  varchar(64) NOT NULL COMMENT '主键',
    `process_instance_id` varchar(64)  DEFAULT NULL COMMENT '流程实例ID',
    `process_def_key`     varchar(100) DEFAULT NULL COMMENT '流程定义KEY',
    `task_id`             varchar(64)  DEFAULT NULL COMMENT '来源任务ID',
    `title`               varchar(200) DEFAULT NULL COMMENT '标题',
    `content`             text COMMENT '内容摘要',
    `business_key`        varchar(100) DEFAULT NULL COMMENT '业务Key',
    `cc_user_id`          varchar(64) NOT NULL COMMENT '抄送人ID',
    `cc_user_name`        varchar(100) DEFAULT NULL COMMENT '抄送人姓名',
    `send_user_id`        varchar(64)  DEFAULT NULL COMMENT '发送人ID',
    `send_user_name`      varchar(100) DEFAULT NULL COMMENT '发送人姓名',
    `cc_time`             datetime     DEFAULT CURRENT_TIMESTAMP COMMENT '抄送时间',
    `read_time`           datetime     DEFAULT NULL COMMENT '阅读时间',
    `is_read`             tinyint      DEFAULT '0' COMMENT '是否已读（0-未读/1-已读）',
    PRIMARY KEY (`id`),
    KEY                   `idx_process_instance_id` (`process_instance_id`),
    KEY                   `idx_cc_user_id` (`cc_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='流程抄送表';


-- forge_admin_new.sys_flow_comment definition

CREATE TABLE `sys_flow_comment`
(
    `id`                  varchar(64) NOT NULL COMMENT '主键',
    `process_instance_id` varchar(64)  DEFAULT NULL COMMENT '流程实例ID',
    `process_def_key`     varchar(100) DEFAULT NULL COMMENT '流程定义KEY',
    `task_id`             varchar(64)  DEFAULT NULL COMMENT '任务ID',
    `task_name`           varchar(200) DEFAULT NULL COMMENT '任务名称',
    `type`                varchar(20)  DEFAULT 'comment' COMMENT '类型（comment-审批意见/event-流程事件）',
    `message`             text COMMENT '意见内容',
    `user_id`             varchar(64)  DEFAULT NULL COMMENT '用户ID',
    `user_name`           varchar(100) DEFAULT NULL COMMENT '用户姓名',
    `full_message`        text COMMENT '完整消息（JSON）',
    `create_time`         datetime     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY                   `idx_process_instance_id` (`process_instance_id`),
    KEY                   `idx_task_id` (`task_id`),
    KEY                   `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='流程意见表';


-- forge_admin_new.sys_flow_condition_item definition

CREATE TABLE `sys_flow_condition_item`
(
    `id`              varchar(64)  NOT NULL COMMENT '主键ID',
    `rule_id`         varchar(64)  NOT NULL COMMENT '规则ID',
    `field_name`      varchar(100) NOT NULL COMMENT '字段名称',
    `field_label`     varchar(100) DEFAULT NULL COMMENT '字段标签',
    `field_type`      varchar(32)  DEFAULT 'string' COMMENT '字段类型：string/number/date/boolean/user/dept/role',
    `operator`        varchar(32)  NOT NULL COMMENT '操作符：eq/ne/gt/lt/ge/le/contains/startsWith/endsWith/in/notIn/isEmpty/isNotEmpty',
    `value`           text COMMENT '比较值（JSON格式，支持多值）',
    `logic_connector` varchar(10)  DEFAULT 'and' COMMENT '逻辑连接符：and/or',
    `group_id`        varchar(64)  DEFAULT NULL COMMENT '分组ID（用于条件分组）',
    `sort_order`      int          DEFAULT '0' COMMENT '排序号',
    `create_time`     datetime     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     datetime     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY               `idx_rule_id` (`rule_id`),
    KEY               `idx_group_id` (`group_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='条件规则项表';


-- forge_admin_new.sys_flow_condition_rule definition

CREATE TABLE `sys_flow_condition_rule`
(
    `id`                   varchar(64)  NOT NULL COMMENT '主键ID',
    `rule_name`            varchar(100) NOT NULL COMMENT '规则名称',
    `rule_code`            varchar(100) DEFAULT NULL COMMENT '规则编码',
    `model_id`             varchar(64)  DEFAULT NULL COMMENT '模型ID',
    `sequence_flow_id`     varchar(128) NOT NULL COMMENT '序列流ID（BPMN中的SequenceFlow ID）',
    `condition_type`       varchar(32)  DEFAULT 'simple' COMMENT '条件类型：simple-简单条件，composite-组合条件，script-脚本',
    `condition_expression` text COMMENT '条件表达式（JSON格式存储）',
    `priority`             int          DEFAULT '0' COMMENT '优先级（数字越小优先级越高）',
    `is_default`           tinyint(1) DEFAULT '0' COMMENT '是否默认路径',
    `status`               int          DEFAULT '1' COMMENT '状态：0-禁用，1-启用',
    `remark`               varchar(500) DEFAULT NULL COMMENT '备注',
    `create_by`            varchar(64)  DEFAULT NULL COMMENT '创建者',
    `create_time`          datetime     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`            varchar(64)  DEFAULT NULL COMMENT '更新者',
    `update_time`          datetime     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`              tinyint(1) DEFAULT '0' COMMENT '删除标志',
    PRIMARY KEY (`id`),
    KEY                    `idx_model_id` (`model_id`),
    KEY                    `idx_sequence_flow_id` (`sequence_flow_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='流程条件规则表';


-- forge_admin_new.sys_flow_form definition

CREATE TABLE `sys_flow_form`
(
    `id`             bigint       NOT NULL COMMENT '主键ID',
    `form_key`       varchar(100) NOT NULL COMMENT '表单Key（唯一标识）',
    `form_name`      varchar(200) NOT NULL COMMENT '表单名称',
    `form_type`      varchar(50)  DEFAULT 'dynamic' COMMENT '表单类型（dynamic-动态表单/external-外部表单/builtin-内置表单）',
    `form_schema`    text COMMENT '表单Schema（JSON格式）',
    `form_url`       varchar(500) DEFAULT NULL COMMENT '外部表单URL（formType为external时使用）',
    `component_path` varchar(200) DEFAULT NULL COMMENT '内置表单组件路径（formType为builtin时使用）',
    `form_config`    text COMMENT '表单配置（JSON格式，包含校验规则、事件等）',
    `version`        int          DEFAULT '1' COMMENT '版本号',
    `status`         tinyint      DEFAULT '1' COMMENT '状态（0-禁用/1-启用）',
    `description`    varchar(500) DEFAULT NULL COMMENT '描述',
    `tenant_id`      bigint       DEFAULT NULL COMMENT '租户ID',
    `create_by`      varchar(64)  DEFAULT NULL COMMENT '创建者',
    `create_time`    datetime     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`      varchar(64)  DEFAULT NULL COMMENT '更新者',
    `update_time`    datetime     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`        tinyint      DEFAULT '0' COMMENT '删除标志（0-正常/1-删除）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `form_key` (`form_key`),
    KEY              `idx_form_key` (`form_key`),
    KEY              `idx_form_name` (`form_name`),
    KEY              `idx_status` (`status`),
    KEY              `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='流程表单定义表';


-- forge_admin_new.sys_flow_model definition

CREATE TABLE `sys_flow_model`
(
    `id`                    varchar(64)  NOT NULL COMMENT '主键',
    `model_key`             varchar(100) NOT NULL COMMENT '模型标识',
    `model_name`            varchar(200) NOT NULL COMMENT '模型名称',
    `description`           varchar(500) DEFAULT NULL COMMENT '描述',
    `category`              varchar(100) DEFAULT NULL COMMENT '分类',
    `flow_type`             varchar(50)  DEFAULT NULL COMMENT '流程类型（leave-请假/expense-报销/approval-审批）',
    `form_type`             varchar(50)  DEFAULT 'dynamic' COMMENT '表单类型（dynamic-动态表单/custom-业务表单）',
    `form_id`               varchar(64)  DEFAULT NULL COMMENT '表单ID（业务表单时使用）',
    `form_json`             text COMMENT '动态表单JSON配置',
    `version`               int          DEFAULT '1' COMMENT '版本号',
    `process_definition_id` varchar(64)  DEFAULT NULL COMMENT 'Flowable流程定义ID',
    `deployment_id`         varchar(64)  DEFAULT NULL COMMENT 'Flowable部署ID',
    `deployment_key`        varchar(100) DEFAULT NULL COMMENT '部署KEY（发布后生成）',
    `status`                tinyint      DEFAULT '0' COMMENT '状态（0-设计/1-已发布/2-禁用）',
    `deploy_time`           datetime     DEFAULT NULL COMMENT '发布时间',
    `last_update_by`        varchar(64)  DEFAULT NULL COMMENT '最后修改人',
    `create_by`             varchar(64)  DEFAULT NULL COMMENT '创建人',
    `create_time`           datetime     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`           datetime     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `del_flag`              tinyint      DEFAULT '0' COMMENT '删除标志（0-正常/1-删除）',
    `bpmn_xml`              longtext,
    `notify_type`           varchar(30)  DEFAULT NULL COMMENT '事件通知方式：none-不通知 / redis-Redis Pub/Sub / webhook-HTTP Webhook（互斥）',
    `webhook_url`           varchar(500) DEFAULT NULL COMMENT 'Webhook 回调地址，仅 notify_type=webhook 时生效',
    PRIMARY KEY (`id`),
    UNIQUE KEY `model_key` (`model_key`),
    KEY                     `idx_model_key` (`model_key`),
    KEY                     `idx_category` (`category`),
    KEY                     `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='流程模型表';


-- forge_admin_new.sys_flow_node_config definition

CREATE TABLE `sys_flow_node_config`
(
    `id`                    varchar(64)  NOT NULL COMMENT '主键',
    `model_id`              varchar(64)  NOT NULL COMMENT '流程模型ID',
    `node_id`               varchar(100) NOT NULL COMMENT '节点ID',
    `node_name`             varchar(200)                                                 DEFAULT NULL COMMENT '节点名称',
    `node_type`             varchar(50)                                                  DEFAULT 'approval' COMMENT '节点类型',
    `assignee_type`         varchar(50)                                                  DEFAULT NULL COMMENT '审批人类型(user/role/dept/post/expr)',
    `assignee_value`        text COMMENT '审批人值',
    `assignee_expr`         varchar(500)                                                 DEFAULT NULL COMMENT '审批人表达式',
    `multi_instance_type`   varchar(50)                                                  DEFAULT 'none' COMMENT '会签类型(none/sequential/parallel)',
    `completion_condition`  varchar(500)                                                 DEFAULT NULL COMMENT '完成条件',
    `pass_rate`             decimal(5, 2)                                                DEFAULT NULL COMMENT '通过比例',
    `due_date_days`         int                                                          DEFAULT NULL COMMENT '超时天数',
    `due_date_hours`        int                                                          DEFAULT NULL COMMENT '超时小时数',
    `timeout_action`        varchar(50)                                                  DEFAULT NULL COMMENT '超时动作(auto_pass/auto_reject/notify)',
    `allow_delegate`        tinyint                                                      DEFAULT '1' COMMENT '允许转办',
    `allow_transfer`        tinyint                                                      DEFAULT '1' COMMENT '允许转交',
    `allow_add_sign`        tinyint                                                      DEFAULT '0' COMMENT '允许加签',
    `allow_counter_sign`    tinyint                                                      DEFAULT '0' COMMENT '允许减签',
    `form_permission`       text COMMENT '表单字段权限配置JSON',
    `create_time`           datetime                                                     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`           datetime                                                     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `timeout_notify_users`  text,
    `allow_reject`          varchar(10)                                                  DEFAULT NULL,
    `allow_reject_to_start` varchar(10)                                                  DEFAULT NULL,
    `allow_withdraw`        varchar(10)                                                  DEFAULT NULL,
    `form_key`              varchar(30)                                                  DEFAULT NULL,
    `priority`              int                                                          DEFAULT NULL,
    `task_listeners`        text,
    `ext_config`            varchar(100)                                                 DEFAULT NULL,
    `status`                varchar(10)                                                  DEFAULT NULL,
    `last_update_by`        varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '最后修改人',
    `create_by`             varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '创建人',
    `del_flag`              tinyint                                                      DEFAULT '0' COMMENT '删除标志（0-正常/1-删除）',
    `update_by`             varchar(20)                                                  DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_model_node` (`model_id`,`node_id`),
    KEY                     `idx_model_id` (`model_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='审批节点配置表';


-- forge_admin_new.sys_flow_statistics definition

CREATE TABLE `sys_flow_statistics`
(
    `id`                   varchar(64)  NOT NULL COMMENT '主键',
    `process_def_key`      varchar(100) NOT NULL COMMENT '流程定义KEY',
    `stat_date`            date         NOT NULL COMMENT '统计日期',
    `total_instances`      int      DEFAULT '0' COMMENT '总实例数',
    `running_instances`    int      DEFAULT '0' COMMENT '运行中实例数',
    `completed_instances`  int      DEFAULT '0' COMMENT '已完成实例数',
    `terminated_instances` int      DEFAULT '0' COMMENT '已终止实例数',
    `avg_duration`         bigint   DEFAULT NULL COMMENT '平均耗时(毫秒)',
    `max_duration`         bigint   DEFAULT NULL COMMENT '最大耗时',
    `min_duration`         bigint   DEFAULT NULL COMMENT '最小耗时',
    `total_tasks`          int      DEFAULT '0' COMMENT '总任务数',
    `avg_task_duration`    bigint   DEFAULT NULL COMMENT '平均任务耗时',
    `timeout_tasks`        int      DEFAULT '0' COMMENT '超时任务数',
    `create_time`          datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_process_date` (`process_def_key`,`stat_date`),
    KEY                    `idx_stat_date` (`stat_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='流程监控统计表';


-- forge_admin_new.sys_flow_task definition

CREATE TABLE `sys_flow_task`
(
    `id`                  varchar(64)  NOT NULL COMMENT '主键',
    `task_id`             varchar(64)   DEFAULT NULL COMMENT 'Flowable任务ID',
    `task_name`           varchar(200)  DEFAULT NULL COMMENT '任务名称',
    `task_def_key`        varchar(100)  DEFAULT NULL COMMENT '任务定义Key',
    `task_def_id`         varchar(100)  DEFAULT NULL COMMENT '任务定义ID',
    `process_instance_id` varchar(64)   DEFAULT NULL COMMENT '流程实例ID',
    `process_def_id`      varchar(64)   DEFAULT NULL COMMENT '流程定义ID',
    `process_def_key`     varchar(100)  DEFAULT NULL COMMENT '流程定义KEY',
    `business_key`        varchar(100)  DEFAULT NULL COMMENT '业务Key',
    `business_type`       varchar(50)   DEFAULT NULL COMMENT '业务类型',
    `title`               varchar(200) NOT NULL COMMENT '任务标题',
    `assignee`            varchar(64)   DEFAULT NULL COMMENT '处理人（签收后）',
    `assignee_name`       varchar(100)  DEFAULT NULL COMMENT '处理人姓名',
    `candidate_users`     varchar(1000) DEFAULT NULL COMMENT '候选人（逗号分隔）',
    `candidate_groups`    varchar(1000) DEFAULT NULL COMMENT '候选组（逗号分隔）',
    `owner`               varchar(64)   DEFAULT NULL COMMENT '任务拥有人',
    `due_date`            datetime      DEFAULT NULL COMMENT '截止日期',
    `priority`            int           DEFAULT '50' COMMENT '优先级（0-100）',
    `status`              tinyint       DEFAULT '0' COMMENT '状态（0-待办/1-已签收/2-已通过/3-已驳回/4-已转办/5-已委派/6-已撤回）',
    `comment`             text COMMENT '审批意见',
    `attachment_urls`     text COMMENT '附件URL（逗号分隔）',
    `start_user_id`       varchar(64)   DEFAULT NULL COMMENT '流程发起人',
    `start_user_name`     varchar(100)  DEFAULT NULL COMMENT '发起人姓名',
    `start_dept_id`       varchar(64)   DEFAULT NULL COMMENT '发起部门ID',
    `start_dept_name`     varchar(100)  DEFAULT NULL COMMENT '发起部门名称',
    `create_time`         datetime      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `claim_time`          datetime      DEFAULT NULL COMMENT '签收时间',
    `complete_time`       datetime      DEFAULT NULL COMMENT '完成时间',
    PRIMARY KEY (`id`),
    KEY                   `idx_task_id` (`task_id`),
    KEY                   `idx_process_instance_id` (`process_instance_id`),
    KEY                   `idx_assignee` (`assignee`),
    KEY                   `idx_status` (`status`),
    KEY                   `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='流程任务表';


-- forge_admin_new.sys_flow_template definition

CREATE TABLE `sys_flow_template`
(
    `id`            varchar(64)  NOT NULL COMMENT '主键',
    `template_key`  varchar(100) NOT NULL COMMENT '模板标识',
    `template_name` varchar(200) NOT NULL COMMENT '模板名称',
    `category`      varchar(50)  DEFAULT NULL COMMENT '分类',
    `description`   varchar(500) DEFAULT NULL COMMENT '描述',
    `icon`          varchar(100) DEFAULT NULL COMMENT '图标',
    `form_type`     varchar(50)  DEFAULT NULL COMMENT '表单类型',
    `form_json`     text COMMENT '表单JSON',
    `bpmn_xml`      text COMMENT 'BPMN流程XML',
    `thumbnail`     varchar(500) DEFAULT NULL COMMENT '缩略图',
    `variables`     text COMMENT '流程变量定义（JSON）',
    `version`       int          DEFAULT '1' COMMENT '版本',
    `status`        tinyint      DEFAULT '1' COMMENT '状态（0-禁用/1-启用）',
    `usage_count`   int          DEFAULT '0' COMMENT '使用次数',
    `create_by`     varchar(64)  DEFAULT NULL COMMENT '创建人',
    `create_time`   datetime     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   datetime     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_system`     varchar(10)  DEFAULT NULL,
    `sort_order`    int          DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `template_key` (`template_key`),
    KEY             `idx_template_key` (`template_key`),
    KEY             `idx_category` (`category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='流程模板表';


-- forge_admin_new.sys_id_sequence definition

CREATE TABLE `sys_id_sequence`
(
    `biz_key`      varchar(100) NOT NULL COMMENT '业务维度唯一键',
    `max_id`       bigint       NOT NULL DEFAULT '0' COMMENT '当前已分配的最大ID',
    `step`         int          NOT NULL DEFAULT '1000' COMMENT '步长',
    `version`      int          NOT NULL DEFAULT '0' COMMENT '版本（乐观锁）',
    `reset_policy` varchar(20)           DEFAULT 'NONE' COMMENT '重置策略：NONE/DAILY/HOURLY',
    `seq_length`   int                   DEFAULT '8' COMMENT '序列长度（左侧补零）',
    `prefix`       varchar(50)           DEFAULT NULL COMMENT '前缀',
    PRIMARY KEY (`biz_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='分布式ID序列配置表';


-- forge_admin_new.sys_job_config definition

CREATE TABLE `sys_job_config`
(
    `id`               bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `job_name`         varchar(200) NOT NULL COMMENT '任务名称',
    `job_group`        varchar(200) NOT NULL DEFAULT 'DEFAULT' COMMENT '任务分组',
    `description`      varchar(500)          DEFAULT NULL COMMENT '任务描述',
    `executor_bean`    varchar(200)          DEFAULT NULL COMMENT '执行器Bean名称',
    `executor_method`  varchar(200)          DEFAULT NULL COMMENT '执行器方法名',
    `executor_handler` varchar(200)          DEFAULT NULL COMMENT '执行器Handler',
    `executor_service` varchar(200)          DEFAULT NULL COMMENT '执行器服务名（RPC模式）',
    `cron_expression`  varchar(100) NOT NULL COMMENT 'Cron表达式',
    `job_param`        text COMMENT '任务参数',
    `status`           tinyint      NOT NULL DEFAULT '1' COMMENT '状态：0-停止 1-运行',
    `execute_mode`     varchar(20)  NOT NULL DEFAULT 'HANDLER' COMMENT '执行模式：BEAN/HANDLER',
    `retry_count`      int                   DEFAULT '0' COMMENT '失败重试次数',
    `alarm_email`      varchar(500)          DEFAULT NULL COMMENT '告警邮箱',
    `webhook_url`      varchar(500)          DEFAULT NULL COMMENT 'WebHook地址',
    `create_time`      datetime              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`      datetime              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_job_name_group` (`job_name`,`job_group`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='任务配置表';


-- forge_admin_new.sys_job_log definition

CREATE TABLE `sys_job_log`
(
    `id`               bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `job_name`         varchar(200) NOT NULL COMMENT '任务名称',
    `job_group`        varchar(200) NOT NULL COMMENT '任务分组',
    `executor_handler` varchar(200) DEFAULT NULL COMMENT '执行器Handler',
    `job_param`        text COMMENT '任务参数',
    `trigger_time`     datetime     DEFAULT NULL COMMENT '触发时间',
    `start_time`       datetime     DEFAULT NULL COMMENT '开始时间',
    `end_time`         datetime     DEFAULT NULL COMMENT '结束时间',
    `duration`         bigint       DEFAULT NULL COMMENT '执行耗时(ms)',
    `status`           tinyint      NOT NULL COMMENT '状态：1-成功 0-失败',
    `result`           text COMMENT '执行结果',
    `exception_msg`    text COMMENT '异常信息',
    `retry_count`      int          DEFAULT '0' COMMENT '重试次数',
    PRIMARY KEY (`id`),
    KEY                `idx_job_name` (`job_name`),
    KEY                `idx_trigger_time` (`trigger_time`),
    KEY                `idx_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=88 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='任务执行日志表';


-- forge_admin_new.sys_login_log definition

CREATE TABLE `sys_login_log`
(
    `id`             bigint   NOT NULL AUTO_INCREMENT COMMENT '日志ID',
    `tenant_id`      bigint            DEFAULT '0' COMMENT '租户编号',
    `user_id`        bigint            DEFAULT NULL COMMENT '用户ID',
    `username`       varchar(50)       DEFAULT NULL COMMENT '用户名',
    `login_type`     varchar(50)       DEFAULT NULL COMMENT '登录类型（LOGIN/LOGOUT/REGISTER）',
    `login_status`   tinyint           DEFAULT '1' COMMENT '登录状态（0-失败，1-成功）',
    `login_ip`       varchar(50)       DEFAULT NULL COMMENT '登录IP',
    `login_location` varchar(255)      DEFAULT NULL COMMENT '登录地点',
    `browser`        varchar(100)      DEFAULT NULL COMMENT '浏览器',
    `os`             varchar(100)      DEFAULT NULL COMMENT '操作系统',
    `user_agent`     varchar(500)      DEFAULT NULL COMMENT '用户代理',
    `login_message`  varchar(500)      DEFAULT NULL COMMENT '登录信息',
    `login_time`     datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间',
    `client_code`    varchar(50)       DEFAULT 'pc' COMMENT '客户端代码：pc/app',
    PRIMARY KEY (`id`),
    KEY              `idx_tenant_user` (`tenant_id`,`user_id`),
    KEY              `idx_login_time` (`login_time`),
    KEY              `idx_login_status` (`login_status`)
) ENGINE=InnoDB AUTO_INCREMENT=2053 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='登录日志表';


-- forge_admin_new.sys_message definition

CREATE TABLE `sys_message`
(
    `id`              bigint       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `tenant_id`       bigint       NOT NULL DEFAULT '0' COMMENT '租户编号',
    `title`           varchar(200) NOT NULL COMMENT '消息标题',
    `content`         text         NOT NULL COMMENT '消息内容',
    `type`            varchar(20)  NOT NULL DEFAULT 'SYSTEM' COMMENT '消息类型：SYSTEM-系统消息/SMS-短信/EMAIL-邮件/CUSTOM-自定义',
    `send_scope`      varchar(20)  NOT NULL DEFAULT 'USERS' COMMENT '发送范围：ALL-全员/ORG-指定组织/USERS-指定人员',
    `send_channel`    varchar(20)  NOT NULL DEFAULT 'WEB' COMMENT '发送渠道：WEB-站内信/SMS-短信/EMAIL-邮件/PUSH-推送',
    `status`          tinyint      NOT NULL DEFAULT '0' COMMENT '消息状态：0-草稿/1-已发送/2-发送失败',
    `sender_id`       bigint                DEFAULT NULL COMMENT '发送人ID',
    `sender_name`     varchar(100)          DEFAULT NULL COMMENT '发送人姓名',
    `template_code`   varchar(50)           DEFAULT NULL COMMENT '模板编码',
    `template_params` json                  DEFAULT NULL COMMENT '模板参数JSON',
    `biz_type`        varchar(50)           DEFAULT NULL COMMENT '业务类型（如：ORDER、APPROVAL、TASK等）',
    `biz_key`         varchar(100)          DEFAULT NULL COMMENT '业务主键（如：订单ID、流程实例ID等）',
    `create_time`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`       bigint                DEFAULT NULL COMMENT '创建者',
    `update_by`       bigint                DEFAULT NULL COMMENT '更新者',
    `create_dept`     bigint unsigned DEFAULT NULL COMMENT '创建组织ID',
    PRIMARY KEY (`id`),
    KEY               `idx_tenant_type` (`tenant_id`,`type`),
    KEY               `idx_status` (`status`),
    KEY               `idx_create_time` (`create_time`),
    KEY               `idx_biz` (`biz_type`,`biz_key`)
) ENGINE=InnoDB AUTO_INCREMENT=16 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统消息主表';


-- forge_admin_new.sys_message_biz_type definition

CREATE TABLE `sys_message_biz_type`
(
    `id`          bigint       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `tenant_id`   bigint       NOT NULL DEFAULT '0' COMMENT '租户编号',
    `biz_type`    varchar(50)  NOT NULL COMMENT '业务类型编码',
    `biz_name`    varchar(100) NOT NULL COMMENT '业务类型名称',
    `jump_url`    varchar(500)          DEFAULT NULL COMMENT '跳转URL模板，支持变量：${bizKey}、${messageId}',
    `jump_target` varchar(20)           DEFAULT '_self' COMMENT '跳转方式：_self-当前页/_blank-新窗口',
    `icon`        varchar(100)          DEFAULT NULL COMMENT '图标',
    `sort`        int                   DEFAULT '0' COMMENT '排序',
    `enabled`     tinyint      NOT NULL DEFAULT '1' COMMENT '是否启用：0-禁用/1-启用',
    `remark`      varchar(500)          DEFAULT NULL COMMENT '备注说明',
    `create_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`   bigint                DEFAULT NULL COMMENT '创建者',
    `update_by`   bigint                DEFAULT NULL COMMENT '更新者',
    `create_dept` bigint unsigned DEFAULT NULL COMMENT '创建组织ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_type` (`tenant_id`,`biz_type`),
    KEY           `idx_enabled` (`enabled`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='消息业务类型配置表';


-- forge_admin_new.sys_message_receiver definition

CREATE TABLE `sys_message_receiver`
(
    `id`          bigint   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `tenant_id`   bigint   NOT NULL DEFAULT '0' COMMENT '租户编号',
    `message_id`  bigint   NOT NULL COMMENT '消息ID',
    `user_id`     bigint   NOT NULL COMMENT '接收人用户ID',
    `org_id`      bigint            DEFAULT NULL COMMENT '接收人所属组织ID',
    `read_flag`   tinyint  NOT NULL DEFAULT '0' COMMENT '已读标记：0-未读/1-已读',
    `read_time`   datetime          DEFAULT NULL COMMENT '阅读时间',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_message_user` (`message_id`,`user_id`),
    KEY           `idx_user_read` (`user_id`,`read_flag`),
    KEY           `idx_tenant_user` (`tenant_id`,`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=16 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='消息接收人表';


-- forge_admin_new.sys_message_send_record definition

CREATE TABLE `sys_message_send_record`
(
    `id`             bigint      NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `tenant_id`      bigint      NOT NULL DEFAULT '0' COMMENT '租户编号',
    `message_id`     bigint      NOT NULL COMMENT '消息ID',
    `channel`        varchar(20) NOT NULL COMMENT '发送渠道：WEB/SMS/EMAIL/PUSH',
    `receiver_count` int                  DEFAULT '0' COMMENT '接收人数量',
    `success_count`  int                  DEFAULT '0' COMMENT '发送成功数量',
    `fail_count`     int                  DEFAULT '0' COMMENT '发送失败数量',
    `external_id`    varchar(100)         DEFAULT NULL COMMENT '第三方渠道返回的消息ID',
    `status`         tinyint     NOT NULL DEFAULT '0' COMMENT '发送状态：0-发送中/1-成功/2-失败',
    `error_msg`      text COMMENT '错误信息',
    `send_time`      datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
    `create_time`    datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY              `idx_message_id` (`message_id`),
    KEY              `idx_send_time` (`send_time`)
) ENGINE=InnoDB AUTO_INCREMENT=15 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='消息发送记录表';


-- forge_admin_new.sys_message_template definition

CREATE TABLE `sys_message_template`
(
    `id`               bigint       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `tenant_id`        bigint       NOT NULL DEFAULT '0' COMMENT '租户编号',
    `template_code`    varchar(50)  NOT NULL COMMENT '模板编码（唯一）',
    `template_name`    varchar(100) NOT NULL COMMENT '模板名称',
    `type`             varchar(20)  NOT NULL DEFAULT 'SYSTEM' COMMENT '消息类型：SYSTEM/SMS/EMAIL/CUSTOM',
    `title_template`   varchar(200)          DEFAULT NULL COMMENT '标题模板（支持${变量}占位符）',
    `content_template` text         NOT NULL COMMENT '内容模板（支持${变量}占位符）',
    `default_channel`  varchar(20)           DEFAULT 'WEB' COMMENT '默认发送渠道',
    `enabled`          tinyint      NOT NULL DEFAULT '1' COMMENT '是否启用：0-禁用/1-启用',
    `remark`           varchar(500)          DEFAULT NULL COMMENT '备注说明',
    `create_time`      datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`      datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`        bigint                DEFAULT NULL COMMENT '创建者',
    `update_by`        bigint                DEFAULT NULL COMMENT '更新者',
    `create_dept`      bigint unsigned DEFAULT NULL COMMENT '创建组织ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_code` (`tenant_id`,`template_code`),
    KEY                `idx_type` (`type`),
    KEY                `idx_enabled` (`enabled`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='消息模板表';


-- forge_admin_new.sys_notice definition

CREATE TABLE `sys_notice`
(
    `notice_id`       bigint       NOT NULL AUTO_INCREMENT COMMENT '公告ID',
    `tenant_id`       bigint       NOT NULL DEFAULT '0' COMMENT '租户编号',
    `notice_title`    varchar(200) NOT NULL COMMENT '公告标题',
    `notice_content`  text         NOT NULL COMMENT '公告内容',
    `notice_type`     varchar(20)  NOT NULL DEFAULT 'NOTICE' COMMENT '公告类型：NOTICE-通知公告/ANNOUNCEMENT-系统公告/NEWS-新闻动态',
    `publish_status`  tinyint      NOT NULL DEFAULT '0' COMMENT '发布状态：0-草稿/1-已发布/2-已撤回',
    `publish_time`    datetime              DEFAULT NULL COMMENT '发布时间',
    `publisher_id`    bigint                DEFAULT NULL COMMENT '发布人ID',
    `publisher_name`  varchar(100)          DEFAULT NULL COMMENT '发布人姓名',
    `publish_scope`   tinyint      NOT NULL DEFAULT '0' COMMENT '发布范围：0-全部组织/1-指定组织',
    `effective_time`  datetime              DEFAULT NULL COMMENT '生效时间',
    `expiration_time` datetime              DEFAULT NULL COMMENT '失效时间',
    `is_top`          tinyint      NOT NULL DEFAULT '0' COMMENT '是否置顶：0-否/1-是',
    `top_sort`        int                   DEFAULT '0' COMMENT '置顶排序（数字越大越靠前）',
    `attachment_ids`  varchar(500)          DEFAULT NULL COMMENT '附件ID列表（多个附件ID用逗号分隔，关联sys_file_metadata表）',
    `read_count`      int                   DEFAULT '0' COMMENT '阅读次数',
    `remark`          varchar(500)          DEFAULT NULL COMMENT '备注',
    `create_time`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`       bigint                DEFAULT NULL COMMENT '创建者',
    `update_by`       bigint                DEFAULT NULL COMMENT '更新者',
    `create_dept`     bigint unsigned DEFAULT NULL COMMENT '创建组织ID',
    PRIMARY KEY (`notice_id`),
    KEY               `idx_tenant_id` (`tenant_id`),
    KEY               `idx_publish_status` (`publish_status`),
    KEY               `idx_notice_type` (`notice_type`),
    KEY               `idx_effective_time` (`effective_time`),
    KEY               `idx_expiration_time` (`expiration_time`),
    KEY               `idx_is_top_sort` (`is_top`,`top_sort`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='通知公告表';


-- forge_admin_new.sys_notice_org definition

CREATE TABLE `sys_notice_org`
(
    `id`          bigint   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `notice_id`   bigint   NOT NULL COMMENT '公告ID',
    `org_id`      bigint   NOT NULL COMMENT '组织ID',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY           `idx_notice_id` (`notice_id`),
    KEY           `idx_org_id` (`org_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='通知公告组织关联表';


-- forge_admin_new.sys_notice_read_record definition

CREATE TABLE `sys_notice_read_record`
(
    `id`          bigint   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `notice_id`   bigint   NOT NULL COMMENT '公告ID',
    `user_id`     bigint   NOT NULL COMMENT '用户ID',
    `user_name`   varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '用户姓名',
    `org_id`      bigint                                  DEFAULT NULL COMMENT '用户所属组织ID',
    `org_name`    varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '用户所属组织名称',
    `read_time`   datetime NOT NULL                       DEFAULT CURRENT_TIMESTAMP COMMENT '阅读时间',
    `create_time` datetime NOT NULL                       DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_notice_user` (`notice_id`,`user_id`),
    KEY           `idx_notice_id` (`notice_id`),
    KEY           `idx_user_id` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='通知公告已读记录表';


-- forge_admin_new.sys_operation_log definition

CREATE TABLE `sys_operation_log`
(
    `id`                 bigint   NOT NULL AUTO_INCREMENT COMMENT '日志ID',
    `tenant_id`          bigint            DEFAULT '0' COMMENT '租户编号',
    `user_id`            bigint            DEFAULT NULL COMMENT '操作用户ID',
    `username`           varchar(50)       DEFAULT NULL COMMENT '操作用户名',
    `operation_module`   varchar(100)      DEFAULT NULL COMMENT '操作模块',
    `operation_type`     varchar(50)       DEFAULT NULL COMMENT '操作类型（ADD/UPDATE/DELETE/QUERY/EXPORT/IMPORT）',
    `operation_desc`     varchar(500)      DEFAULT NULL COMMENT '操作描述',
    `request_method`     varchar(10)       DEFAULT NULL COMMENT '请求方式（GET/POST/PUT/DELETE）',
    `request_url`        varchar(500)      DEFAULT NULL COMMENT '请求URL',
    `request_params`     text COMMENT '请求参数',
    `response_result`    text COMMENT '响应结果',
    `error_msg`          text COMMENT '错误信息',
    `operation_status`   tinyint           DEFAULT '1' COMMENT '操作状态（0-失败，1-成功）',
    `operation_ip`       varchar(50)       DEFAULT NULL COMMENT '操作IP',
    `operation_location` varchar(255)      DEFAULT NULL COMMENT '操作地点',
    `user_agent`         varchar(500)      DEFAULT NULL COMMENT '用户代理',
    `execute_time`       bigint            DEFAULT '0' COMMENT '执行时长（毫秒）',
    `operation_time`     datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    PRIMARY KEY (`id`),
    KEY                  `idx_tenant_user` (`tenant_id`,`user_id`),
    KEY                  `idx_operation_time` (`operation_time`),
    KEY                  `idx_operation_status` (`operation_status`),
    KEY                  `idx_request_url` (`request_url`(255))
) ENGINE=InnoDB AUTO_INCREMENT=63106 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='操作日志表';


-- forge_admin_new.sys_org definition

CREATE TABLE `sys_org`
(
    `id`          bigint       NOT NULL AUTO_INCREMENT COMMENT '组织ID',
    `tenant_id`   bigint       NOT NULL DEFAULT '0' COMMENT '租户编号',
    `org_name`    varchar(100) NOT NULL COMMENT '组织名称',
    `parent_id`   bigint       NOT NULL DEFAULT '0' COMMENT '父级组织ID（0为顶级）',
    `ancestors`   varchar(500)          DEFAULT NULL COMMENT '祖级编码（逗号分隔，如：1,2,3）',
    `sort`        int                   DEFAULT '0' COMMENT '排序（值越小越靠前）',
    `org_type`    tinyint               DEFAULT '1' COMMENT '组织类型（1-公司，2-部门，3-小组）',
    `org_status`  tinyint      NOT NULL DEFAULT '1' COMMENT '组织状态（0-禁用，1-正常）',
    `leader_id`   bigint                DEFAULT NULL COMMENT '负责人ID（关联sys_user.id）',
    `leader_name` varchar(50)           DEFAULT NULL COMMENT '负责人姓名',
    `phone`       varchar(20)           DEFAULT NULL COMMENT '组织联系电话',
    `address`     varchar(255)          DEFAULT NULL COMMENT '组织地址',
    `region_code` varchar(30)           DEFAULT NULL COMMENT '行政区划编码',
    `remark`      varchar(500)          DEFAULT NULL COMMENT '备注',
    `create_by`   bigint                DEFAULT NULL COMMENT '创建者',
    `create_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`   bigint                DEFAULT NULL COMMENT '更新者',
    `update_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_dept` bigint unsigned DEFAULT NULL COMMENT '创建组织ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_org_name` (`tenant_id`,`org_name`),
    KEY           `idx_tenant_parent` (`tenant_id`,`parent_id`),
    KEY           `idx_org_status` (`org_status`),
    KEY           `idx_ancestors` (`ancestors`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='组织表';


-- forge_admin_new.sys_post definition

CREATE TABLE `sys_post`
(
    `id`          bigint       NOT NULL AUTO_INCREMENT COMMENT '岗位ID',
    `tenant_id`   bigint       NOT NULL DEFAULT '0' COMMENT '租户编号',
    `post_code`   varchar(50)  NOT NULL COMMENT '岗位编码（租户内唯一）',
    `org_id`      bigint       NOT NULL COMMENT '所属组织ID（关联sys_org.id）',
    `post_name`   varchar(100) NOT NULL COMMENT '岗位名称',
    `post_status` tinyint      NOT NULL DEFAULT '1' COMMENT '岗位状态（0-禁用，1-正常）',
    `post_type`   tinyint               DEFAULT '1' COMMENT '岗位类型（1-管理岗，2-技术岗，3-业务岗，4-其他）',
    `sort`        int                   DEFAULT '0' COMMENT '排序（值越小越靠前）',
    `remark`      varchar(500)          DEFAULT NULL COMMENT '备注',
    `create_by`   bigint                DEFAULT NULL COMMENT '创建者',
    `create_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`   bigint                DEFAULT NULL COMMENT '更新者',
    `update_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_dept` bigint unsigned DEFAULT NULL COMMENT '创建组织ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_post_code` (`tenant_id`,`post_code`),
    UNIQUE KEY `uk_tenant_org_post` (`tenant_id`,`org_id`,`post_name`),
    KEY           `idx_tenant_org` (`tenant_id`,`org_id`),
    KEY           `idx_post_status` (`post_status`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='岗位表';


-- forge_admin_new.sys_region_code definition

CREATE TABLE `sys_region_code`
(
    `code`        varchar(10) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '行政区划代码',
    `name`        varchar(100)                                                 NOT NULL COMMENT '行政区划名称',
    `level`       tinyint                                                      NOT NULL COMMENT '行政级别(1-省,2-市,3-区/县,4-街道)',
    `parent_code` varchar(10) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '父级代码',
    `full_name`   varchar(200)                                                 DEFAULT NULL COMMENT '全名',
    `city_code`   varchar(20)                                                  DEFAULT NULL COMMENT '地市编码',
    PRIMARY KEY (`code`),
    KEY           `idx_parent_code` (`parent_code`),
    KEY           `sys_region_code_name_IDX` (`name`) USING BTREE,
    KEY           `idx_parent_level` (`parent_code`,`level`),
    KEY           `idx_code_level` (`code`,`level`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;


-- forge_admin_new.sys_resource definition

CREATE TABLE `sys_resource`
(
    `id`            bigint       NOT NULL AUTO_INCREMENT COMMENT '资源ID',
    `tenant_id`     bigint       NOT NULL DEFAULT '0' COMMENT '租户编号',
    `resource_name` varchar(100) NOT NULL COMMENT '资源名称',
    `parent_id`     bigint       NOT NULL DEFAULT '0' COMMENT '父级资源ID（0为顶级）',
    `resource_type` tinyint      NOT NULL COMMENT '资源类型（1-目录，2-菜单，3-按钮，4-API接口）',
    `sort`          int                   DEFAULT '0' COMMENT '排序（值越小越靠前）',
    `path`          varchar(255)          DEFAULT NULL COMMENT '资源路由（菜单/目录用）',
    `component`     varchar(255)          DEFAULT NULL COMMENT '前端组件路径（菜单用）',
    `is_external`   tinyint               DEFAULT '0' COMMENT '是否外链（0-否，1-是）',
    `is_public`     tinyint               DEFAULT '0' COMMENT '是否公开资源（0-否，1-是，公开资源无需权限验证）',
    `menu_status`   tinyint               DEFAULT '1' COMMENT '菜单状态（0-隐藏，1-显示，仅菜单/目录用）',
    `visible`       tinyint               DEFAULT '1' COMMENT '显示状态（0-隐藏，1-显示，所有资源通用）',
    `perms`         varchar(100)          DEFAULT NULL COMMENT '权限标识（如：sys:user:list，按钮/API用）',
    `icon`          varchar(50)           DEFAULT NULL COMMENT '图标（菜单/目录用）',
    `api_method`    varchar(10)           DEFAULT NULL COMMENT 'API请求方法（GET/POST/PUT/DELETE，仅API用）',
    `api_url`       varchar(255)          DEFAULT NULL COMMENT 'API接口地址（仅API用）',
    `keep_alive`    tinyint               DEFAULT '0' COMMENT '是否缓存（0-否，1-是，菜单用）',
    `always_show`   tinyint               DEFAULT '0' COMMENT '是否总是显示（0-否，1-是，菜单用）',
    `redirect`      varchar(255)          DEFAULT NULL COMMENT '重定向地址（菜单用）',
    `remark`        varchar(500)          DEFAULT NULL COMMENT '备注',
    `create_by`     bigint                DEFAULT NULL COMMENT '创建者',
    `create_time`   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`     bigint                DEFAULT NULL COMMENT '更新者',
    `update_time`   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_dept`   bigint unsigned DEFAULT NULL COMMENT '创建组织ID',
    `client_code`   varchar(50)           DEFAULT 'pc' COMMENT '客户端代码：pc/app',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_resource` (`tenant_id`,`resource_type`,`perms`) COMMENT '租户内资源权限标识唯一',
    KEY             `idx_tenant_parent` (`tenant_id`,`parent_id`),
    KEY             `idx_resource_type` (`resource_type`),
    KEY             `idx_api_url_method` (`api_url`,`api_method`) COMMENT 'API查询优化'
) ENGINE=InnoDB AUTO_INCREMENT=9094 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统资源表（菜单/按钮/API）';


-- forge_admin_new.sys_role definition

CREATE TABLE `sys_role`
(
    `id`          bigint       NOT NULL AUTO_INCREMENT COMMENT '角色ID',
    `tenant_id`   bigint       NOT NULL DEFAULT '0' COMMENT '租户编号',
    `role_name`   varchar(50)  NOT NULL COMMENT '角色名称（租户内唯一）',
    `role_key`    varchar(100) NOT NULL COMMENT '角色权限字符串（如：admin,user:view）',
    `data_scope`  tinyint               DEFAULT '1' COMMENT '权限范围（1-全部数据，2-本租户数据，3-本组织数据，4-本组织及子组织，5-个人数据）',
    `sort`        int                   DEFAULT '0' COMMENT '排序（值越小越靠前）',
    `role_status` tinyint      NOT NULL DEFAULT '1' COMMENT '角色状态（0-禁用，1-正常）',
    `is_system`   tinyint               DEFAULT '0' COMMENT '是否系统内置角色（0-否，1-是）',
    `remark`      varchar(500)          DEFAULT NULL COMMENT '备注',
    `create_by`   bigint                DEFAULT NULL COMMENT '创建者',
    `create_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`   bigint                DEFAULT NULL COMMENT '更新者',
    `update_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_dept` bigint unsigned DEFAULT NULL COMMENT '创建组织ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_role_name` (`tenant_id`,`role_name`),
    UNIQUE KEY `uk_tenant_role_key` (`tenant_id`,`role_key`),
    KEY           `idx_tenant_status` (`tenant_id`,`role_status`),
    KEY           `idx_data_scope` (`data_scope`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色表';


-- forge_admin_new.sys_role_data_scope definition

CREATE TABLE `sys_role_data_scope`
(
    `id`          bigint   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `tenant_id`   bigint   NOT NULL DEFAULT '0' COMMENT '租户编号',
    `role_id`     bigint   NOT NULL COMMENT '角色ID',
    `org_id`      bigint   NOT NULL COMMENT '组织ID',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_org` (`tenant_id`,`role_id`,`org_id`),
    KEY           `idx_role_id` (`role_id`),
    KEY           `idx_org_id` (`org_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色-自定义数据权限关联表';


-- forge_admin_new.sys_role_resource definition

CREATE TABLE `sys_role_resource`
(
    `id`          bigint   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `tenant_id`   bigint   NOT NULL DEFAULT '0' COMMENT '租户编号',
    `role_id`     bigint   NOT NULL COMMENT '角色ID',
    `resource_id` bigint   NOT NULL COMMENT '资源ID',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_resource` (`tenant_id`,`role_id`,`resource_id`),
    KEY           `idx_role_id` (`role_id`),
    KEY           `idx_resource_id` (`resource_id`)
) ENGINE=InnoDB AUTO_INCREMENT=346 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色-资源关联表';


-- forge_admin_new.sys_sms_config definition

CREATE TABLE `sys_sms_config`
(
    `id`                bigint                                 NOT NULL AUTO_INCREMENT COMMENT '主键',
    `config_id`         varchar(64) COLLATE utf8mb4_unicode_ci  DEFAULT NULL COMMENT '配置标识',
    `supplier`          varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '厂商标识(alibaba/tencent/huawei等)',
    `access_key_id`     varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '访问密钥ID',
    `access_key_secret` varchar(256) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '访问密钥Secret',
    `signature`         varchar(64) COLLATE utf8mb4_unicode_ci  DEFAULT NULL COMMENT '短信签名',
    `template_id`       varchar(64) COLLATE utf8mb4_unicode_ci  DEFAULT NULL COMMENT '默认模板ID',
    `weight`            int                                     DEFAULT '1' COMMENT '权重(负载均衡)',
    `retry_interval`    int                                     DEFAULT '5' COMMENT '重试间隔(秒)',
    `max_retries`       int                                     DEFAULT '0' COMMENT '最大重试次数',
    `maximum`           int                                     DEFAULT NULL COMMENT '发送上限',
    `extra_config`      text COLLATE utf8mb4_unicode_ci COMMENT '额外配置(JSON格式)',
    `daily_limit`       int                                     DEFAULT NULL COMMENT '每日发送上限',
    `minute_limit`      int                                     DEFAULT NULL COMMENT '每分钟发送上限',
    `status`            tinyint                                 DEFAULT '0' COMMENT '状态(0禁用 1启用)',
    `tenant_id`         bigint                                  DEFAULT NULL COMMENT '租户ID',
    `create_time`       datetime                                DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`       datetime                                DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`         varchar(64) COLLATE utf8mb4_unicode_ci  DEFAULT NULL COMMENT '创建人',
    `update_by`         varchar(64) COLLATE utf8mb4_unicode_ci  DEFAULT NULL COMMENT '更新人',
    `remark`            varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`id`),
    KEY                 `idx_tenant_status` (`tenant_id`,`status`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='短信配置表';


-- forge_admin_new.sys_social_config definition

CREATE TABLE `sys_social_config`
(
    `id`            bigint       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `platform`      varchar(50)  NOT NULL COMMENT '平台类型（WECHAT、DINGTALK、GITHUB、GITEE等）',
    `platform_name` varchar(100) DEFAULT NULL COMMENT '平台名称',
    `platform_logo` varchar(500) DEFAULT NULL COMMENT '平台Logo',
    `client_id`     varchar(255) NOT NULL COMMENT '应用ID/Key',
    `client_secret` varchar(255) NOT NULL COMMENT '应用Secret',
    `redirect_uri`  varchar(500) DEFAULT NULL COMMENT '回调地址',
    `agent_id`      varchar(100) DEFAULT NULL COMMENT '企业微信AgentId',
    `scope`         varchar(500) DEFAULT NULL COMMENT '授权范围',
    `status`        tinyint(1) NOT NULL DEFAULT '1' COMMENT '状态（1-启用，0-停用）',
    `tenant_id`     bigint       DEFAULT NULL COMMENT '租户ID',
    `remark`        varchar(500) DEFAULT NULL COMMENT '备注说明',
    `create_by`     bigint       DEFAULT NULL COMMENT '创建人ID',
    `create_time`   datetime     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`     bigint       DEFAULT NULL COMMENT '更新人ID',
    `update_time`   datetime     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_platform_tenant` (`platform`,`tenant_id`) USING BTREE,
    KEY             `idx_tenant` (`tenant_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='三方登录配置表';


-- forge_admin_new.sys_tenant definition

CREATE TABLE `sys_tenant`
(
    `id`             bigint       NOT NULL AUTO_INCREMENT COMMENT '租户ID',
    `tenant_name`    varchar(100) NOT NULL COMMENT '租户名称',
    `contact_person` varchar(50)           DEFAULT NULL COMMENT '负责人',
    `contact_phone`  varchar(20)           DEFAULT NULL COMMENT '联系电话',
    `user_limit`     int                   DEFAULT '0' COMMENT '租户人员数量上限（0表示无限制）',
    `tenant_status`  tinyint      NOT NULL DEFAULT '1' COMMENT '租户状态（0-禁用，1-正常）',
    `expire_time`    datetime              DEFAULT NULL COMMENT '过期时间',
    `tenant_desc`    varchar(500)          DEFAULT NULL COMMENT '租户描述',
    `create_by`      bigint                DEFAULT NULL COMMENT '创建者（系统租户ID）',
    `create_time`    datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`      bigint                DEFAULT NULL COMMENT '更新者',
    `update_time`    datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `browser_icon`   varchar(255)          DEFAULT NULL COMMENT '浏览器icon（存储图标URL/Base64）',
    `browser_title`  varchar(100)          DEFAULT NULL COMMENT '浏览器标签名称',
    `system_name`    varchar(100)          DEFAULT NULL COMMENT '系统名称',
    `system_logo`    varchar(255)          DEFAULT NULL COMMENT '系统logo（存储logo URL/Base64）',
    `system_intro`   varchar(500)          DEFAULT NULL COMMENT '系统介绍',
    `copyright_info` varchar(200)          DEFAULT NULL COMMENT '版权显示文本',
    `system_layout`  varchar(50)           DEFAULT 'default' COMMENT '系统布局（default-默认，classic-经典，modern-现代等）',
    `system_theme`   varchar(50)           DEFAULT 'light' COMMENT '系统主题（light-亮色，dark-暗色，auto-跟随系统等）',
    `theme_config`   varchar(1000)         DEFAULT NULL COMMENT '主题配置',
    `create_dept`    bigint unsigned DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_name` (`tenant_name`),
    KEY              `idx_tenant_status` (`tenant_status`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='租户表';


-- forge_admin_new.sys_user definition

CREATE TABLE `sys_user`
(
    `id`              bigint       NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `tenant_id`       bigint       NOT NULL DEFAULT '0' COMMENT '租户编号',
    `username`        varchar(50)  NOT NULL COMMENT '用户名（租户内唯一）',
    `real_name`       varchar(50)           DEFAULT NULL COMMENT '用户真实姓名',
    `user_type`       tinyint               DEFAULT '1' COMMENT '用户类型（0-系统管理员，1-租户管理员，2-普通用户）',
    `user_client`     varchar(20)           DEFAULT NULL COMMENT '用户触点（app/pc/h5/wechat）',
    `email`           varchar(100)          DEFAULT NULL COMMENT '邮箱',
    `phone`           varchar(20)           DEFAULT NULL COMMENT '手机号',
    `id_card`         varchar(18)           DEFAULT NULL COMMENT '身份证号',
    `gender`          tinyint               DEFAULT '0' COMMENT '性别（0-未知，1-男，2-女）',
    `password`        varchar(100) NOT NULL COMMENT '密码（加密存储）',
    `salt`            varchar(50)           DEFAULT NULL COMMENT '密码盐值',
    `user_status`     tinyint      NOT NULL DEFAULT '1' COMMENT '用户状态（0-禁用，1-正常，2-锁定）',
    `avatar`          varchar(255)          DEFAULT NULL COMMENT '头像URL',
    `region_code`     varchar(10)           DEFAULT NULL COMMENT '行政区划编码',
    `last_login_time` datetime              DEFAULT NULL COMMENT '最后登录时间',
    `last_login_ip`   varchar(50)           DEFAULT NULL COMMENT '最后登录IP',
    `login_count`     int                   DEFAULT '0' COMMENT '登录次数',
    `remark`          varchar(500)          DEFAULT NULL COMMENT '备注',
    `create_dept`     bigint                DEFAULT NULL COMMENT '创建部门',
    `create_by`       bigint                DEFAULT NULL COMMENT '创建者',
    `create_time`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`       bigint                DEFAULT NULL COMMENT '更新者',
    `update_time`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `sys_user_unique` (`tenant_id`,`username`),
    KEY               `idx_tenant_status` (`tenant_id`,`user_status`),
    KEY               `idx_user_type` (`user_type`),
    KEY               `idx_region_code` (`region_code`)
) ENGINE=InnoDB AUTO_INCREMENT=23 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户表';


-- forge_admin_new.sys_user_org definition

CREATE TABLE `sys_user_org`
(
    `id`          bigint   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `tenant_id`   bigint   NOT NULL DEFAULT '0' COMMENT '租户编号',
    `user_id`     bigint   NOT NULL COMMENT '用户ID',
    `org_id`      bigint   NOT NULL COMMENT '组织ID',
    `is_main`     tinyint           DEFAULT '0' COMMENT '是否主组织（0-否，1-是）',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_org` (`tenant_id`,`user_id`,`org_id`),
    KEY           `idx_user_id` (`user_id`),
    KEY           `idx_org_id` (`org_id`),
    KEY           `idx_user_main_org` (`user_id`,`is_main`) COMMENT '查询用户主组织优化'
) ENGINE=InnoDB AUTO_INCREMENT=16 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户-组织关联表';


-- forge_admin_new.sys_user_post definition

CREATE TABLE `sys_user_post`
(
    `id`          bigint   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `tenant_id`   bigint   NOT NULL DEFAULT '0' COMMENT '租户编号',
    `user_id`     bigint   NOT NULL COMMENT '用户ID',
    `post_id`     bigint   NOT NULL COMMENT '岗位ID',
    `is_main`     tinyint           DEFAULT '0' COMMENT '是否主岗（0-否，1-是）',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_post` (`tenant_id`,`user_id`,`post_id`),
    KEY           `idx_user_id` (`user_id`),
    KEY           `idx_post_id` (`post_id`),
    KEY           `idx_user_main_post` (`user_id`,`is_main`) COMMENT '查询用户主岗优化'
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户-岗位关联表';


-- forge_admin_new.sys_user_role definition

CREATE TABLE `sys_user_role`
(
    `id`          bigint   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `tenant_id`   bigint   NOT NULL DEFAULT '0' COMMENT '租户编号',
    `user_id`     bigint   NOT NULL COMMENT '用户ID',
    `role_id`     bigint   NOT NULL COMMENT '角色ID',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_role` (`tenant_id`,`user_id`,`role_id`),
    KEY           `idx_user_id` (`user_id`),
    KEY           `idx_role_id` (`role_id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户-角色关联表';


-- forge_admin_new.sys_user_social definition

CREATE TABLE `sys_user_social`
(
    `id`            bigint       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id`       bigint       NOT NULL COMMENT '用户ID',
    `platform`      varchar(50)  NOT NULL COMMENT '平台类型',
    `uuid`          varchar(255) NOT NULL COMMENT '第三方用户唯一标识',
    `username`      varchar(100) DEFAULT NULL COMMENT '第三方用户名',
    `nickname`      varchar(100) DEFAULT NULL COMMENT '第三方昵称',
    `avatar`        varchar(500) DEFAULT NULL COMMENT '头像',
    `email`         varchar(100) DEFAULT NULL COMMENT '邮箱',
    `access_token`  varchar(500) DEFAULT NULL COMMENT '访问令牌',
    `refresh_token` varchar(500) DEFAULT NULL COMMENT '刷新令牌',
    `expire_time`   datetime     DEFAULT NULL COMMENT '令牌过期时间',
    `bind_time`     datetime     DEFAULT CURRENT_TIMESTAMP COMMENT '绑定时间',
    `tenant_id`     bigint       DEFAULT NULL COMMENT '租户ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_platform_uuid` (`platform`,`uuid`) USING BTREE,
    KEY             `idx_user_id` (`user_id`) USING BTREE,
    KEY             `idx_tenant` (`tenant_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户三方账号绑定表';


-- forge_admin_new.worker_node definition

CREATE TABLE `worker_node`
(
    `ID`          bigint      NOT NULL AUTO_INCREMENT COMMENT 'auto increment id',
    `HOST_NAME`   varchar(64) NOT NULL COMMENT 'host name',
    `PORT`        varchar(64) NOT NULL COMMENT 'port',
    `TYPE`        int         NOT NULL COMMENT 'node type: ACTUAL or CONTAINER',
    `LAUNCH_DATE` date        NOT NULL COMMENT 'launch date',
    `MODIFIED`    timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'modified time',
    `CREATED`     timestamp   NOT NULL DEFAULT '0000-00-00 00:00:00' COMMENT 'created time',
    PRIMARY KEY (`ID`)
) ENGINE=InnoDB AUTO_INCREMENT=1524 DEFAULT CHARSET=utf8mb3 COMMENT='DB WorkerID Assigner for UID Generator';


-- forge_admin_new.qrtz_triggers definition

CREATE TABLE `qrtz_triggers`
(
    `sched_name`     varchar(120) NOT NULL COMMENT '调度名称',
    `trigger_name`   varchar(200) NOT NULL COMMENT '触发器的名字',
    `trigger_group`  varchar(200) NOT NULL COMMENT '触发器所属组的名字',
    `job_name`       varchar(200) NOT NULL COMMENT 'qrtz_job_details表job_name的外键',
    `job_group`      varchar(200) NOT NULL COMMENT 'qrtz_job_details表job_group的外键',
    `description`    varchar(250) DEFAULT NULL COMMENT '相关介绍',
    `next_fire_time` bigint       DEFAULT NULL COMMENT '上一次触发时间（毫秒）',
    `prev_fire_time` bigint       DEFAULT NULL COMMENT '下一次触发时间（默认为-1表示不触发）',
    `priority`       int          DEFAULT NULL COMMENT '优先级',
    `trigger_state`  varchar(16)  NOT NULL COMMENT '触发器状态',
    `trigger_type`   varchar(8)   NOT NULL COMMENT '触发器的类型',
    `start_time`     bigint       NOT NULL COMMENT '开始时间',
    `end_time`       bigint       DEFAULT NULL COMMENT '结束时间',
    `calendar_name`  varchar(200) DEFAULT NULL COMMENT '日程表名称',
    `misfire_instr`  smallint     DEFAULT NULL COMMENT '补偿执行的策略',
    `job_data`       blob COMMENT '存放持久化job对象',
    PRIMARY KEY (`sched_name`, `trigger_name`, `trigger_group`),
    KEY              `sched_name` (`sched_name`,`job_name`,`job_group`),
    CONSTRAINT `qrtz_triggers_ibfk_1` FOREIGN KEY (`sched_name`, `job_name`, `job_group`) REFERENCES `qrtz_job_details` (`sched_name`, `job_name`, `job_group`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='触发器详细信息表';


-- forge_admin_new.qrtz_blob_triggers definition

CREATE TABLE `qrtz_blob_triggers`
(
    `sched_name`    varchar(120) NOT NULL COMMENT '调度名称',
    `trigger_name`  varchar(200) NOT NULL COMMENT 'qrtz_triggers表trigger_name的外键',
    `trigger_group` varchar(200) NOT NULL COMMENT 'qrtz_triggers表trigger_group的外键',
    `blob_data`     blob COMMENT '存放持久化Trigger对象',
    PRIMARY KEY (`sched_name`, `trigger_name`, `trigger_group`),
    CONSTRAINT `qrtz_blob_triggers_ibfk_1` FOREIGN KEY (`sched_name`, `trigger_name`, `trigger_group`) REFERENCES `qrtz_triggers` (`sched_name`, `trigger_name`, `trigger_group`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='Blob类型的触发器表';


-- forge_admin_new.qrtz_cron_triggers definition

CREATE TABLE `qrtz_cron_triggers`
(
    `sched_name`      varchar(120) NOT NULL COMMENT '调度名称',
    `trigger_name`    varchar(200) NOT NULL COMMENT 'qrtz_triggers表trigger_name的外键',
    `trigger_group`   varchar(200) NOT NULL COMMENT 'qrtz_triggers表trigger_group的外键',
    `cron_expression` varchar(200) NOT NULL COMMENT 'cron表达式',
    `time_zone_id`    varchar(80) DEFAULT NULL COMMENT '时区',
    PRIMARY KEY (`sched_name`, `trigger_name`, `trigger_group`),
    CONSTRAINT `qrtz_cron_triggers_ibfk_1` FOREIGN KEY (`sched_name`, `trigger_name`, `trigger_group`) REFERENCES `qrtz_triggers` (`sched_name`, `trigger_name`, `trigger_group`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='Cron类型的触发器表';


-- forge_admin_new.qrtz_simple_triggers definition

CREATE TABLE `qrtz_simple_triggers`
(
    `sched_name`      varchar(120) NOT NULL COMMENT '调度名称',
    `trigger_name`    varchar(200) NOT NULL COMMENT 'qrtz_triggers表trigger_name的外键',
    `trigger_group`   varchar(200) NOT NULL COMMENT 'qrtz_triggers表trigger_group的外键',
    `repeat_count`    bigint       NOT NULL COMMENT '重复的次数统计',
    `repeat_interval` bigint       NOT NULL COMMENT '重复的间隔时间',
    `times_triggered` bigint       NOT NULL COMMENT '已经触发的次数',
    PRIMARY KEY (`sched_name`, `trigger_name`, `trigger_group`),
    CONSTRAINT `qrtz_simple_triggers_ibfk_1` FOREIGN KEY (`sched_name`, `trigger_name`, `trigger_group`) REFERENCES `qrtz_triggers` (`sched_name`, `trigger_name`, `trigger_group`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='简单触发器的信息表';


-- forge_admin_new.qrtz_simprop_triggers definition

CREATE TABLE `qrtz_simprop_triggers`
(
    `sched_name`    varchar(120) NOT NULL COMMENT '调度名称',
    `trigger_name`  varchar(200) NOT NULL COMMENT 'qrtz_triggers表trigger_name的外键',
    `trigger_group` varchar(200) NOT NULL COMMENT 'qrtz_triggers表trigger_group的外键',
    `str_prop_1`    varchar(512)   DEFAULT NULL COMMENT 'String类型的trigger的第一个参数',
    `str_prop_2`    varchar(512)   DEFAULT NULL COMMENT 'String类型的trigger的第二个参数',
    `str_prop_3`    varchar(512)   DEFAULT NULL COMMENT 'String类型的trigger的第三个参数',
    `int_prop_1`    int            DEFAULT NULL COMMENT 'int类型的trigger的第一个参数',
    `int_prop_2`    int            DEFAULT NULL COMMENT 'int类型的trigger的第二个参数',
    `long_prop_1`   bigint         DEFAULT NULL COMMENT 'long类型的trigger的第一个参数',
    `long_prop_2`   bigint         DEFAULT NULL COMMENT 'long类型的trigger的第二个参数',
    `dec_prop_1`    decimal(13, 4) DEFAULT NULL COMMENT 'decimal类型的trigger的第一个参数',
    `dec_prop_2`    decimal(13, 4) DEFAULT NULL COMMENT 'decimal类型的trigger的第二个参数',
    `bool_prop_1`   varchar(1)     DEFAULT NULL COMMENT 'Boolean类型的trigger的第一个参数',
    `bool_prop_2`   varchar(1)     DEFAULT NULL COMMENT 'Boolean类型的trigger的第二个参数',
    PRIMARY KEY (`sched_name`, `trigger_name`, `trigger_group`),
    CONSTRAINT `qrtz_simprop_triggers_ibfk_1` FOREIGN KEY (`sched_name`, `trigger_name`, `trigger_group`) REFERENCES `qrtz_triggers` (`sched_name`, `trigger_name`, `trigger_group`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='同步机制的行锁表';


INSERT INTO ai_agent (id, tenant_id, agent_name, agent_code, description, system_prompt, provider_id, model_name,
                      temperature, max_tokens, extra_config, status, create_by, create_time, update_by, update_time,
                      del_flag, create_dept)
VALUES (1, 1, '大屏生成助手', 'dashboard_generator', '根据用户需求自动生成数据可视化大屏布局', '你是一个数据可视化大屏设计专家。用户会描述他们想要的数据大屏，你需要根据描述选择合适的组件并设计布局，输出一个 JSON 对象。

## 画布信息
- 画布尺寸: {{canvasWidth}}px × {{canvasHeight}}px
- 坐标原点在左上角，x 向右增大，y 向下增大
- 组件使用绝对定位，请合理安排布局，不要重叠

## 风格要求
- 当前主题风格: {{styleLabel}}
- 背景建议: {{backgroundSuggestion}}
- 文字建议: {{textColorSuggestion}}

## 用户需求
{{prompt}}

## 当前画布配置
{{canvasContext}}

## 可用组件列表（只能使用以下组件 key）
{{componentCatalog}}

## 输出 JSON 格式
你必须严格输出以下格式的 JSON（不要输出任何其他文字，不要用 markdown 代码块包裹）：

{
  "title": "大屏标题",
  "canvasConfig": {
    "width": {{canvasWidth}},
    "height": {{canvasHeight}},
    "background": "{{backgroundColor}}"
  },
  "components": [
    {
      "key": "组件key",
      "x": 左上角x坐标,
      "y": 左上角y坐标,
      "w": 宽度,
      "h": 高度,
      "title": "组件显示标题",
      "option": { ... 根据组件类型填入，见下方规则 ... }
    }
  ]
}

## 各组件 option 填写规则（非常重要！）

### 图表类组件（BarCommon, BarCrossrange, BarLine, CapsuleChart, LineCommon, LineGradientSingle, LineGradients, PieCommon, PieCircle, ScatterCommon, Heatmap, TreeMap, Funnel, MapBase 等）
这些是 ECharts 图表组件，option 中 **只需要填 dataset 字段**，不需要填 series/tooltip/xAxis/yAxis 等配置（系统会自动补充）。
- dataset 格式: { "dimensions": ["列名1", "列名2", ...], "source": [{"列名1": 值, "列名2": 值}, ...] }
- dimensions 的第一个元素是类目名（如"月份"、"地区"），其余是数据系列名
- 示例: { "option": { "dataset": { "dimensions": ["月份", "销售额", "利润"], "source": [{"月份": "1月", "销售额": 820, "利润": 320}] } } }

### 饼图 PieCommon / PieCircle
- dataset 只需要 2 个 dimensions: 类目名 + 数值
- 示例: { "option": { "dataset": { "dimensions": ["地区", "销售额"], "source": [{"地区": "华东", "销售额": 3350}] } } }

### 雷达图 Radar
- dataset 使用特殊格式: { "radarIndicator": [{ "name": "指标名", "max": 100 }], "seriesData": [{ "name": "系列名", "value": [80, 90] }] }

### 文本组件 TextCommon
- option.dataset 填入要显示的文字内容（字符串）
- 可选: option.fontSize, option.fontColor, option.fontWeight, option.textAlign, option.letterSpacing

### 数字翻牌器 FlipperNumber
- option.dataset 填入要显示的数字，可选 option.unit (单位文字)

### 时钟 Clock / 倒计时 CountDown
- 不需要 option

### 装饰边框 Border01-Border13
- 不需要 option，只需位置和尺寸，用于包裹图表

### 排行榜 TableList
- option.dataset 填入对象数组: [{ "name": "项目名", "value": 数值 }, ...]

### 滚动表格 TableScrollBoard
- option.header 填入表头数组: ["列1", "列2", "列3"]
- option.dataset 填入二维数组（每行是字符串数组）: [["行1列1", "行1列2", "行1列3"], ["行2列1", "行2列2", "行2列3"]]

## 布局设计规则
1. 大屏顶部居中放置一个 TextCommon 作为标题（y=20, fontSize=32-40, fontColor=#ffffff, fontWeight=bold）
2. 标题区高度约 80px，剩余区域分为 2-3 列排列图表
3. 同行组件之间留 20-30px 间距
4. 数据要真实合理，符合大屏主题
5. 只输出纯 JSON，不要有任何额外文字或注释', NULL, 'qwen-plus', 0.70, 4000, NULL, '0', 1, '2026-04-13 17:38:55', 1,
        '2026-04-13 17:38:55', '0', NULL),
       (104, 1, 'CRUD配置生成器', 'crud_config_builder', '根据自然语言描述或数据库表结构生成CRUD配置JSON', '你是一个CRUD配置生成专家。根据用户的描述或数据库表结构，生成符合AiCrudPage组件规范的CRUD配置JSON。

配置JSON必须包含以下字段：
- tableName: 表名
- tableComment: 表描述
- searchSchema: 搜索表单配置数组，每项包含field/label/type，可选dictType
- columnsSchema: 表格列配置数组，每项包含key/title/dataIndex，最后一列为操作列(key=actions)
- editSchema: 编辑表单配置数组，每项包含field/label/type/required，可选dictType/props
- apiConfig: API配置对象

搜索type可选：input/select/daterange/number
编辑type可选：input/textarea/select/radio/checkbox/switch/date/datetime/number/upload/imageUpload/fileUpload/cascader/treeSelect
基类字段(id/tenant_id/create_by/create_time/create_dept/update_by/update_time/del_flag)不进editSchema
有dictType的select字段必须加上dictType属性

请仅输出JSON，不要输出其他内容。', NULL, NULL, 0.30, 4096, NULL, '0', NULL, '2026-04-20 17:47:33', NULL,
        '2026-04-20 17:47:33', '0', NULL),
       (1001, 1, '代码生成字段顾问', 'codegen_column_advisor',
        '根据数据库字段信息推荐Java类型、表单组件、字典类型、验证规则', '你是一个数据库字段配置顾问。根据用户提供的表信息和字段列表，推荐每个字段的最佳配置。

## 输入格式
用户会提供表名、表注释和字段列表（包含字段名、数据库类型、注释）。

## 你需要推荐
对每个字段推荐以下配置：
1. javaType：Java 类型（Integer/Long/String/BigDecimal/LocalDateTime/LocalDate/Boolean）
2. htmlType：表单组件（INPUT/TEXTAREA/SELECT/RADIO/CHECKBOX/DATETIME/IMAGE/FILE/EDITOR）
3. dictType：字典编码（如果字段是枚举类型，推荐合适的字典编码；不是字典类型则为空）
4. queryType：查询方式（EQ/LIKE/BETWEEN/GT/LT/IN）
5. isRequired：是否必填（1必填/0非必填）
6. validateRule：验证规则JSON（如 {"required":true,"pattern":"^1[3-9]\\\\d{9}$","message":"手机号格式不正确"}）

## 推断规则
- 字段名含 status/type/category/level → 推荐 SELECT 组件 + 对应字典
- 字段名含 name/title → 推荐 INPUT + LIKE 查询
- 字段名含 amount/price/money → 推荐 INPUT + BigDecimal + 金额验证
- 字段名含 phone/mobile → 推荐 INPUT + 手机号验证
- 字段名含 email → 推荐 INPUT + 邮箱验证
- 字段名含 description/content/remark 且 varchar>255 → 推荐 TEXTAREA
- 字段名含 time/date → 推荐 DATETIME + BETWEEN 查询
- 字段名含 image/avatar/logo/pic/photo → 推荐 IMAGE
- 字段名含 file/attachment → 推荐 FILE
- 字段名含 content/body 且 text类型 → 推荐 EDITOR
- tinyint(1) → Boolean + RADIO
- 基类字段（id/tenant_id/create_by/create_time/create_dept/update_by/update_time/remark）→ isInsert=0,isEdit=0,isList=0,isQuery=0

## 字典列表
以下是系统中已有的字典编码，请优先使用：
{{dictList}}

## 输出格式
返回 JSON 数组，每个元素对应一个字段的推荐配置：
```json
[
  {
    "columnName": "字段名",
    "javaType": "Java类型",
    "htmlType": "表单组件",
    "dictType": "字典编码或空",
    "queryType": "查询方式",
    "isRequired": 0,
    "validateRule": "{}"
  }
]
```

只返回 JSON 数组，不要返回其他内容。', NULL, NULL, 0.30, 4096, NULL, '0', NULL, '2026-04-20 14:22:46', NULL, NULL, '0',
        NULL),
       (1002, 1, '代码生成Schema构建器', 'codegen_schema_builder', '根据自然语言描述推断数据模型Schema', '你是一个数据模型设计专家。根据用户的自然语言描述，推断出完整的数据表结构。

## 推断规则
1. 根据描述推断表名（小写字母+下划线，如 biz_order）和表注释
2. 推断合理的字段列表，每个字段包含：
   - columnName：字段名（小写字母+下划线）
   - columnComment：字段注释
   - columnType：数据库类型（如 varchar(255)、bigint、int、decimal(10,2)、datetime）
   - javaType：Java 类型
   - javaField：Java 字段名（小驼峰）
   - htmlType：表单组件
   - dictType：字典编码（如适用）
   - isRequired：是否必填
   - validateRule：验证规则JSON
3. 不需要包含基类字段（id/tenant_id/create_by/create_time/create_dept/update_by/update_time），系统会自动追加
4. 主键 id 不需要指定
5. 字段类型选择遵循 MySQL 最佳实践

## 字典列表
以下是系统中已有的字典编码，请优先使用：
{{dictList}}

## 输出格式
返回 JSON 对象：
```json
{
  "tableName": "表名",
  "tableComment": "表注释",
  "columns": [
    {
      "columnName": "字段名",
      "columnComment": "字段注释",
      "columnType": "数据库类型",
      "javaType": "Java类型",
      "javaField": "java字段名",
      "htmlType": "表单组件",
      "dictType": "字典编码或空",
      "isRequired": 0,
      "validateRule": "{}"
    }
  ]
}
```

只返回 JSON 对象，不要返回其他内容。', NULL, NULL, 0.30, 4096, NULL, '0', NULL, '2026-04-20 14:22:46', NULL, NULL, '0',
        NULL);
INSERT INTO ai_chat_record (id, tenant_id, user_id, agent_code, session_id, `role`, content, token_usage, create_time)
VALUES (2044609333588705281, 1, 1, 'dashboard_generator', 'f35a767b-7b0f-4802-aa00-457efafb58c2', 'user', '销售数据大屏，先做一个饼状图展示销售数据

当前项目：销售数据可视化大屏

当前画布已有内容（请在此基础上增量修改，尽量保持现有布局和组件语义一致）：
{
  "canvas": {
    "projectName": "销售数据可视化大屏",
    "width": 1920,
    "height": 1080,
    "background": "#1e1e2e"
  },
  "components": [
    {
      "id": "id_4b00adqbhb6000",
      "isGroup": false,
      "attr": {
        "x": 0,
        "y": 20,
        "w": 1920,
        "h": 60,
        "offsetX": 0,
        "offsetY": 0,
        "zIndex": -1
      },
      "styles": {
        "filterShow": false,
        "hueRotate": 0,
        "saturate": 1,
        "contrast": 1,
        "brightness": 1,
        "opacity": 1,
        "rotateZ": 0,
        "rotateX": 0,
        "rotateY": 0,
        "skewX": 0,
        "skewY": 0,
        "blendMode": "normal",
        "animations": []
      },
      "preview": {
        "overFlowHidden": false
      },
      "status": {
        "lock": false,
        "hide": false
      },
      "request": {
        "requestDataType": 0,
        "requestHttpType": "get",
        "requestUrl": "",
        "requestIntervalUnit": "second",
        "requestContentType": 0,
        "requestParamsBodyType": "none",
        "requestSQLContent": {
          "sql": "select * from  where"
        },
        "requestParams": {
          "Body": {
            "form-data": {},
            "x-www-form-urlencoded": {},
            "json": "",
            "xml": ""
          },
          "Header": {},
          "Params": {}
        }
      },
      "events": {
        "baseEvent": {},
        "advancedEvents": {},
        "interactEvents": []
      },
      "key": "TextCommon",
      "chartConfig": {
        "key": "TextCommon",
        "chartKey": "VTextCommon",
        "conKey": "VCTextCommon",
        "title": "销售数据可视化大屏",
        "category": "Texts",
        "categoryName": "文本",
        "package": "Informations",
        "chartFrame": "common",
        "image": "text_static.png"
      },
      "option": {
        "link": "",
        "linkHead": "http://",
        "dataset": "销售数据可视化大屏",
        "fontSize": 36,
        "fontColor": "#ffffff",
        "paddingX": 10,
        "paddingY": 10,
        "textAlign": "center",
        "fontWeight": "bold",
        "borderWidth": 0,
        "borderColor": "#ffffff",
        "borderRadius": 5,
        "letterSpacing": 5,
        "writingMode": "horizontal-tb",
        "backgroundColor": "#00000000"
      }
    },
    {
      "id": "id_czae8150r4w00",
      "isGroup": false,
      "attr": {
        "x": 50,
        "y": 120,
        "w": 920,
        "h": 900,
        "offsetX": 0,
        "offsetY": 0,
        "zIndex": -1
      },
      "styles": {
        "filterShow": false,
        "hueRotate": 0,
        "saturate": 1,
        "contrast": 1,
        "brightness": 1,
        "opacity": 1,
        "rotateZ": 0,
        "rotateX": 0,
        "rotateY": 0,
        "skewX": 0,
        "skewY": 0,
        "blendMode": "normal",
        "animations": []
      },
      "preview": {
        "overFlowHidden": false
      },
      "status": {
        "lock": false,
        "hide": false
      },
      "request": {
        "requestDataType": 0,
        "requestHttpType": "get",
        "requestUrl": "",
        "requestIntervalUnit": "second",
        "requestContentType": 0,
        "requestParamsBodyType": "none",
        "requestSQLContent": {
          "sql": "select * from  where"
        },
        "requestParams": {
          "Body": {
            "form-data": {},
            "x-www-form-urlencoded": {},
            "json": "",
            "xml": ""
          },
          "Header": {},
          "Params": {}
        }
      },
      "events": {
        "baseEvent": {},
        "advancedEvents": {},
        "interactEvents": []
      },
      "key": "BarCommon",
      "chartConfig": {
        "key": "BarCommon",
        "chartKey": "VBarCommon",
        "conKey": "VCBarCommon",
        "title": "月度销售额统计",
        "category": "Bars",
        "categoryName": "柱状图",
        "package": "Charts",
        "chartFrame": "echarts",
        "image": "bar_x.png"
      },
      "option": {
        "legend": {
          "show": true,
          "type": "scroll",
          "x": "center",
          "y": "top",
          "icon": "circle",
          "orient": "horizontal",
          "textStyle": {
            "color": "#B9B8CE",
            "fontSize": 18
          },
          "itemHeight": 15,
          "itemWidth": 15,
          "pageTextStyle": {
            "color": "#B9B8CE"
          }
        },
        "xAxis": {
          "show": true,
          "name": "",
          "nameGap": 15,
          "nameTextStyle": {
            "color": "#B9B8CE",
            "fontSize": 12
          },
          "inverse": false,
          "axisLabel": {
            "show": true,
            "fontSize": 12,
            "color": "#B9B8CE",
            "rotate": 0
          },
          "position": "bottom",
          "axisLine": {
            "show": true,
            "lineStyle": {
              "color": "#B9B8CE",
              "width": 1
            },
            "onZero": true
          },
          "axisTick": {
            "show": true,
            "length": 5
          },
          "splitLine": {
            "show": false,
            "lineStyle": {
              "color": "#484753",
              "width": 1,
              "type": "solid"
            }
          },
          "type": "category"
        },
        "yAxis": {
          "show": true,
          "name": "",
          "nameGap": 15,
          "nameTextStyle": {
            "color": "#B9B8CE",
            "fontSize": 12
          },
          "inverse": false,
          "axisLabel": {
            "show": true,
            "fontSize": 12,
            "color": "#B9B8CE",
            "rotate": 0
          },
          "position": "left",
          "axisLine": {
            "show": true,
            "lineStyle": {
              "color": "#B9B8CE",
              "width": 1
            },
            "onZero": true
          },
          "axisTick": {
            "show": true,
            "length": 5
          },
          "splitLine": {
            "show": true,
            "lineStyle": {
              "color": "#484753",
              "width": 1,
              "type": "solid"
            }
          },
          "type": "value"
        },
        "grid": {
          "show": false,
          "left": "10%",
          "top": "60",
          "right": "10%",
          "bottom": "60"
        },
        "tooltip": {
          "show": true,
          "trigger": "axis",
          "axisPointer": {
            "show": true,
            "type": "shadow"
          }
        },
        "dataset": {
          "dimensions": [
            "月份",
            "销售额"
          ],
          "source": [
            {
              "月份": "1月",
              "销售额": 320
            },
            {
              "月份": "2月",
              "销售额": 280
            },
            {
              "月份": "3月",
              "销售额": 450
            },
            {
              "月份": "4月",
              "销售额": 390
            },
            {
              "月份": "5月",
              "销售额": 510
            },
            {
              "月份": "6月",
              "销售额": 480
            },
            {
              "月份": "7月",
              "销售额": 620
            },
            {
              "月份": "8月",
              "销售额": 590
            },
            {
              "月份": "9月",
              "销售额": 530
            },
            {
              "月份": "10月",
              "销售额": 680
            },
            {
              "月份": "11月",
              "销售额": 750
            },
            {
              "月份": "12月",
              "销售额": 820
            }
          ]
        },
        "series": [
          {
            "type": "bar",
            "barWidth": 15,
            "label": {
              "show": true,
              "position": "top",
              "color": "#fff",
              "fontSize": 12
            },
            "itemStyle": {
              "color": null,
              "borderRadius": 2
            }
          }
        ],
        "backgroundColor": "rgba(0,0,0,0)"
      }
    },
    {
      "id": "id_2ag6q64ahikg00",
      "isGroup": false,
      "attr": {
        "x": 1020,
        "y": 120,
        "w": 850,
        "h": 900,
        "offsetX": 0,
        "offsetY": 0,
        "zIndex": -1
      },
      "styles": {
        "filterShow": false,
        "hueRotate": 0,
        "saturate": 1,
        "contrast": 1,
        "brightness": 1,
        "opacity": 1,
        "rotateZ": 0,
        "rotateX": 0,
        "rotateY": 0,
        "skewX": 0,
        "skewY": 0,
        "blendMode": "normal",
        "animations": []
      },
      "preview": {
        "overFlowHidden": false
      },
      "status": {
        "lock": false,
        "hide": false
      },
      "request": {
        "requestDataType": 0,
        "requestHttpType": "get",
        "requestUrl": "",
        "requestIntervalUnit": "second",
        "requestContentType": 0,
        "requestParamsBodyType": "none",
        "requestSQLContent": {
          "sql": "select * from  where"
        },
        "requestParams": {
          "Body": {
            "form-data": {},
            "x-www-form-urlencoded": {},
            "json": "",
            "xml": ""
          },
          "Header": {},
          "Params": {}
        }
      },
      "events": {
        "baseEvent": {},
        "advancedEvents": {},
        "interactEvents": []
      },
      "key": "PieCommon",
      "chartConfig": {
        "key": "PieCommon",
        "chartKey": "VPieCommon",
        "conKey": "VCPieCommon",
        "title": "各区域销售占比",
        "category": "Pies",
        "categoryName": "饼图",
        "package": "Charts",
        "chartFrame": "echarts",
        "image": "pie.png"
      },
      "option": {
        "legend": {
          "show": true,
          "type": "scroll",
          "x": "center",
          "y": "top",
          "icon": "circle",
          "orient": "horizontal",
          "textStyle": {
            "color": "#B9B8CE",
            "fontSize": 18
          },
          "itemHeight": 15,
          "itemWidth": 15,
          "pageTextStyle": {
            "color": "#B9B8CE"
          }
        },
        "isCarousel": false,
        "type": "ring",
        "tooltip": {
          "show": true,
          "trigger": "item"
        },
        "dataset": {
          "dimensions": [
            "区域",
            "销售额"
          ],
          "source": [
            {
              "区域": "华东",
              "销售额": 3500
            },
            {
              "区域": "华南",
              "销售额": 2800
            },
            {
              "区域": "华北",
              "销售额": 3100
            },
            {
              "区域": "西南",
              "销售额": 1800
            },
            {
              "区域": "西北",
              "销售额": 1200
            },
            {
              "区域": "东北",
              "销售额": 1600
            }
          ]
        },
        "series": [
          {
            "type": "pie",
            "radius": [
              "40%",
              "65%"
            ],
            "center": [
              "50%",
              "60%"
            ],
            "roseType": false,
            "avoidLabelOverlap": false,
            "itemStyle": {
              "show": true,
              "borderRadius": 10,
              "borderColor": "#fff",
              "borderWidth": 2
            },
            "label": {
              "show": false,
              "position": "center",
              "formatter": "{b}",
              "fontWeight": "normal",
              "fontSize": 14,
              "color": "#454E54",
              "textBorderColor": "#ffffff",
              "textBorderWidth": 1
            },
            "emphasis": {
              "label": {
                "show": true,
                "fontSize": 40,
                "fontWeight": "bold"
              }
            },
            "labelLine": {
              "show": false
            }
          }
        ],
        "backgroundColor": "rgba(0,0,0,0)"
      }
    }
  ]
}

画布尺寸：1920x1080
风格要求：深色主题', NULL, '2026-04-16 10:50:27'),
       (2044609334184296450, 1, 1, 'dashboard_generator', 'f35a767b-7b0f-4802-aa00-457efafb58c2', 'assistant', '{
  "title": "销售数据可视化大屏",
  "canvasConfig": {
    "width": 1920,
    "height": 1080,
    "background": "#1e1e2e"
  },
  "components": [
    {
      "key": "TextCommon",
      "x": 0,
      "y": 20,
      "w": 1920,
      "h": 60,
      "title": "销售数据可视化大屏",
      "option": {
        "dataset": "销售数据可视化大屏",
        "fontSize": 36,
        "fontColor": "#ffffff",
        "fontWeight": "bold",
        "textAlign": "center",
        "letterSpacing": 5
      }
    },
    {
      "key": "BarCommon",
      "x": 50,
      "y": 120,
      "w": 920,
      "h": 900,
      "title": "月度销售额统计",
      "option": {
        "dataset": {
          "dimensions": [
            "月份",
            "销售额"
          ],
          "source": [
            {
              "月份": "1月",
              "销售额": 320
            },
            {
              "月份": "2月",
              "销售额": 280
            },
            {
              "月份": "3月",
              "销售额": 450
            },
            {
              "月份": "4月",
              "销售额": 390
            },
            {
              "月份": "5月",
              "销售额": 510
            },
            {
              "月份": "6月",
              "销售额": 480
            },
            {
              "月份": "7月",
              "销售额": 620
            },
            {
              "月份": "8月",
              "销售额": 590
            },
            {
              "月份": "9月",
              "销售额": 530
            },
            {
              "月份": "10月",
              "销售额": 680
            },
            {
              "月份": "11月",
              "销售额": 750
            },
            {
              "月份": "12月",
              "销售额": 820
            }
          ]
        }
      }
    },
    {
      "key": "PieCommon",
      "x": 1020,
      "y": 120,
      "w": 850,
      "h": 900,
      "title": "各区域销售占比",
      "option": {
        "dataset": {
          "dimensions": [
            "区域",
            "销售额"
          ],
          "source": [
            {
              "区域": "华东",
              "销售额": 3500
            },
            {
              "区域": "华南",
              "销售额": 2800
            },
            {
              "区域": "华北",
              "销售额": 3100
            },
            {
              "区域": "西南",
              "销售额": 1800
            },
            {
              "区域": "西北",
              "销售额": 1200
            },
            {
              "区域": "东北",
              "销售额": 1600
            }
          ]
        }
      }
    }
  ]
}', NULL, '2026-04-16 10:50:27'),
       (2044611006776262657, 1, 1, 'dashboard_generator', 'f4375db7-cd10-407a-a645-fd5fd2de5e55', 'user', '帮我增加一个可以判断销售数据的饼状图+折线图

当前项目：销售数据可视化大屏

当前画布已有内容（请在此基础上增量修改，尽量保持现有布局和组件语义一致）：
{
  "canvas": {
    "projectName": "销售数据可视化大屏",
    "width": 1920,
    "height": 1080,
    "background": "#1e1e2e"
  },
  "components": [
    {
      "id": "id_28ms0u1ztysk00",
      "isGroup": false,
      "attr": {
        "x": 0,
        "y": 20,
        "w": 1920,
        "h": 60,
        "offsetX": 0,
        "offsetY": 0,
        "zIndex": -1
      },
      "styles": {
        "filterShow": false,
        "hueRotate": 0,
        "saturate": 1,
        "contrast": 1,
        "brightness": 1,
        "opacity": 1,
        "rotateZ": 0,
        "rotateX": 0,
        "rotateY": 0,
        "skewX": 0,
        "skewY": 0,
        "blendMode": "normal",
        "animations": []
      },
      "preview": {
        "overFlowHidden": false
      },
      "status": {
        "lock": false,
        "hide": false
      },
      "request": {
        "requestDataType": 0,
        "requestHttpType": "get",
        "requestUrl": "",
        "requestIntervalUnit": "second",
        "requestContentType": 0,
        "requestParamsBodyType": "none",
        "requestSQLContent": {
          "sql": "select * from  where"
        },
        "requestParams": {
          "Body": {
            "form-data": {},
            "x-www-form-urlencoded": {},
            "json": "",
            "xml": ""
          },
          "Header": {},
          "Params": {}
        }
      },
      "events": {
        "baseEvent": {},
        "advancedEvents": {},
        "interactEvents": []
      },
      "key": "TextCommon",
      "chartConfig": {
        "key": "TextCommon",
        "chartKey": "VTextCommon",
        "conKey": "VCTextCommon",
        "title": "销售数据可视化大屏",
        "category": "Texts",
        "categoryName": "文本",
        "package": "Informations",
        "chartFrame": "common",
        "image": "text_static.png"
      },
      "option": {
        "link": "",
        "linkHead": "http://",
        "dataset": "销售数据可视化大屏",
        "fontSize": 36,
        "fontColor": "#ffffff",
        "paddingX": 10,
        "paddingY": 10,
        "textAlign": "center",
        "fontWeight": "bold",
        "borderWidth": 0,
        "borderColor": "#ffffff",
        "borderRadius": 5,
        "letterSpacing": 5,
        "writingMode": "horizontal-tb",
        "backgroundColor": "#00000000"
      }
    },
    {
      "id": "id_56z2itctzog000",
      "isGroup": false,
      "attr": {
        "x": 200,
        "y": 120,
        "w": 1520,
        "h": 800,
        "offsetX": 0,
        "offsetY": 0,
        "zIndex": -1
      },
      "styles": {
        "filterShow": false,
        "hueRotate": 0,
        "saturate": 1,
        "contrast": 1,
        "brightness": 1,
        "opacity": 1,
        "rotateZ": 0,
        "rotateX": 0,
        "rotateY": 0,
        "skewX": 0,
        "skewY": 0,
        "blendMode": "normal",
        "animations": []
      },
      "preview": {
        "overFlowHidden": false
      },
      "status": {
        "lock": false,
        "hide": false
      },
      "request": {
        "requestDataType": 0,
        "requestHttpType": "get",
        "requestUrl": "",
        "requestIntervalUnit": "second",
        "requestContentType": 0,
        "requestParamsBodyType": "none",
        "requestSQLContent": {
          "sql": "select * from  where"
        },
        "requestParams": {
          "Body": {
            "form-data": {},
            "x-www-form-urlencoded": {},
            "json": "",
            "xml": ""
          },
          "Header": {},
          "Params": {}
        }
      },
      "events": {
        "baseEvent": {},
        "advancedEvents": {},
        "interactEvents": []
      },
      "key": "BarCommon",
      "chartConfig": {
        "key": "BarCommon",
        "chartKey": "VBarCommon",
        "conKey": "VCBarCommon",
        "title": "月度销售额统计",
        "category": "Bars",
        "categoryName": "柱状图",
        "package": "Charts",
        "chartFrame": "echarts",
        "image": "bar_x.png"
      },
      "option": {
        "legend": {
          "show": true,
          "type": "scroll",
          "x": "center",
          "y": "top",
          "icon": "circle",
          "orient": "horizontal",
          "textStyle": {
            "color": "#B9B8CE",
            "fontSize": 18
          },
          "itemHeight": 15,
          "itemWidth": 15,
          "pageTextStyle": {
            "color": "#B9B8CE"
          }
        },
        "xAxis": {
          "show": true,
          "name": "",
          "nameGap": 15,
          "nameTextStyle": {
            "color": "#B9B8CE",
            "fontSize": 12
          },
          "inverse": false,
          "axisLabel": {
            "show": true,
            "fontSize": 12,
            "color": "#B9B8CE",
            "rotate": 0
          },
          "position": "bottom",
          "axisLine": {
            "show": true,
            "lineStyle": {
              "color": "#B9B8CE",
              "width": 1
            },
            "onZero": true
          },
          "axisTick": {
            "show": true,
            "length": 5
          },
          "splitLine": {
            "show": false,
            "lineStyle": {
              "color": "#484753",
              "width": 1,
              "type": "solid"
            }
          },
          "type": "category"
        },
        "yAxis": {
          "show": true,
          "name": "",
          "nameGap": 15,
          "nameTextStyle": {
            "color": "#B9B8CE",
            "fontSize": 12
          },
          "inverse": false,
          "axisLabel": {
            "show": true,
            "fontSize": 12,
            "color": "#B9B8CE",
            "rotate": 0
          },
          "position": "left",
          "axisLine": {
            "show": true,
            "lineStyle": {
              "color": "#B9B8CE",
              "width": 1
            },
            "onZero": true
          },
          "axisTick": {
            "show": true,
            "length": 5
          },
          "splitLine": {
            "show": true,
            "lineStyle": {
              "color": "#484753",
              "width": 1,
              "type": "solid"
            }
          },
          "type": "value"
        },
        "grid": {
          "show": false,
          "left": "10%",
          "top": "60",
          "right": "10%",
          "bottom": "60"
        },
        "tooltip": {
          "show": true,
          "trigger": "axis",
          "axisPointer": {
            "show": true,
            "type": "shadow"
          }
        },
        "dataset": {
          "dimensions": [
            "月份",
            "销售额"
          ],
          "source": [
            {
              "月份": "1月",
              "销售额": 320
            },
            {
              "月份": "2月",
              "销售额": 280
            },
            {
              "月份": "3月",
              "销售额": 450
            },
            {
              "月份": "4月",
              "销售额": 390
            },
            {
              "月份": "5月",
              "销售额": 510
            },
            {
              "月份": "6月",
              "销售额": 480
            },
            {
              "月份": "7月",
              "销售额": 620
            },
            {
              "月份": "8月",
              "销售额": 590
            },
            {
              "月份": "9月",
              "销售额": 530
            },
            {
              "月份": "10月",
              "销售额": 680
            },
            {
              "月份": "11月",
              "销售额": 750
            },
            {
              "月份": "12月",
              "销售额": 820
            }
          ]
        },
        "series": [
          {
            "type": "bar",
            "barWidth": 15,
            "label": {
              "show": true,
              "position": "top",
              "color": "#fff",
              "fontSize": 12
            },
            "itemStyle": {
              "color": null,
              "borderRadius": 2
            }
          }
        ],
        "backgroundColor": "rgba(0,0,0,0)"
      }
    }
  ]
}

画布尺寸：1920x1080
风格要求：深色主题', NULL, '2026-04-16 10:57:06'),
       (2044611008080691201, 1, 1, 'dashboard_generator', 'f4375db7-cd10-407a-a645-fd5fd2de5e55', 'assistant', '{
  "title": "销售数据可视化大屏",
  "canvasConfig": {
    "width": 1920,
    "height": 1080,
    "background": "#1e1e2e"
  },
  "components": [
    {
      "key": "TextCommon",
      "x": 0,
      "y": 20,
      "w": 1920,
      "h": 60,
      "title": "销售数据可视化大屏",
      "option": {
        "dataset": "销售数据可视化大屏",
        "fontSize": 36,
        "fontColor": "#ffffff",
        "fontWeight": "bold",
        "textAlign": "center",
        "letterSpacing": 5
      }
    },
    {
      "key": "BarCommon",
      "x": 20,
      "y": 100,
      "w": 950,
      "h": 460,
      "title": "月度销售额统计",
      "option": {
        "dataset": {
          "dimensions": ["月份", "销售额"],
          "source": [
            {"月份": "1月", "销售额": 320},
            {"月份": "2月", "销售额": 280},
            {"月份": "3月", "销售额": 450},
            {"月份": "4月", "销售额": 390},
            {"月份": "5月", "销售额": 510},
            {"月份": "6月", "销售额": 480},
            {"月份": "7月", "销售额": 620},
            {"月份": "8月", "销售额": 590},
            {"月份": "9月", "销售额": 530},
            {"月份": "10月", "销售额": 680},
            {"月份": "11月", "销售额": 750},
            {"月份": "12月", "销售额": 820}
          ]
        }
      }
    },
    {
      "key": "PieCommon",
      "x": 990,
      "y": 100,
      "w": 910,
      "h": 460,
      "title": "销售渠道占比分析",
      "option": {
        "dataset": {
          "dimensions": ["渠道", "销售额"],
          "source": [
            {"渠道": "电商平台", "销售额": 520},
            {"渠道": "线下门店", "销售额": 380},
            {"渠道": "直播带货", "销售额": 410},
            {"渠道": "批发代理", "销售额": 290}
          ]
        }
      }
    },
    {
      "key": "LineCommon",
      "x": 20,
      "y": 580,
      "w": 1880,
      "h": 480,
      "title": "近期销售趋势与利润分析",
      "option": {
        "dataset": {
          "dimensions": ["日期", "销售额", "利润"],
          "source": [
            {"日期": "1日", "销售额": 1200, "利润": 350},
            {"日期": "2日", "销售额": 1350, "利润": 410},
            {"日期": "3日", "销售额": 1100, "利润": 290},
            {"日期": "4日", "销售额": 1450, "利润": 460},
            {"日期": "5日", "销售额": 1600, "利润": 520},
            {"日期": "6日", "销售额": 1300, "利润": 380},
            {"日期": "7日", "销售额": 1750, "利润": 580},
            {"日期": "8日", "销售额": 1550, "利润": 490},
            {"日期": "9日", "销售额": 1400, "利润": 430},
            {"日期": "10日", "销售额": 1650, "利润": 540},
            {"日期": "11日", "销售额": 1800, "利润": 610},
            {"日期": "12日", "销售额": 1950, "利润": 680}
          ]
        }
      }
    }
  ]
}', NULL, '2026-04-16 10:57:06'),
       (2046482797006192642, 1, 1, 'crud_config_builder', 'session_1776747312761_kdvupglex', 'user', '根据以下信息生成CRUD配置JSON：

configKey: order
tableName: order
描述: 开发订单管理页面


请按以下格式输出（每个阶段前加标记）：
[STAGE:searchSchema]
{"searchSchema": [...]}

[STAGE:columnsSchema]
{"columnsSchema": [...]}

[STAGE:editSchema]
{"editSchema": [...]}

[STAGE:apiConfig]
{"apiConfig": {...}}

每个字段项包含：field, label, type
搜索类型：input/select/daterange
编辑类型：input/textarea/select/radio/checkbox/switch/date/datetime/number
表格列包含：key, title, dataIndex，最后加actions列
基类字段不进editSchema
apiConfig格式：{list: ''get@/ai/crud/order/page'', ...}
', NULL, '2026-04-21 14:54:55'),
       (2046482797392068610, 1, 1, 'crud_config_builder', 'session_1776747312761_kdvupglex', 'assistant', '[STAGE:searchSchema]
{"tableName": "order", "tableComment": "订单管理", "searchSchema": [{"field": "orderNo", "label": "订单编号", "type": "input"}, {"field": "customerName", "label": "客户名称", "type": "input"}, {"field": "orderStatus", "label": "订单状态", "type": "select", "dictType": "order_status"}, {"field": "orderDate", "label": "下单时间", "type": "daterange"}]}

[STAGE:columnsSchema]
{"columnsSchema": [{"key": "orderNo", "title": "订单编号", "dataIndex": "orderNo"}, {"key": "customerName", "title": "客户名称", "dataIndex": "customerName"}, {"key": "orderAmount", "title": "订单金额", "dataIndex": "orderAmount"}, {"key": "orderStatus", "title": "订单状态", "dataIndex": "orderStatus"}, {"key": "orderDate", "title": "下单时间", "dataIndex": "orderDate"}, {"key": "actions", "title": "操作", "dataIndex": "actions"}]}

[STAGE:editSchema]
{"editSchema": [{"field": "orderNo", "label": "订单编号", "type": "input", "required": true}, {"field": "customerName", "label": "客户名称", "type": "input", "required": true}, {"field": "orderAmount", "label": "订单金额", "type": "number", "required": true}, {"field": "orderStatus", "label": "订单状态", "type": "select", "required": true, "dictType": "order_status"}, {"field": "orderDate", "label": "下单时间", "type": "datetime", "required": true}, {"field": "remark", "label": "备注", "type": "textarea"}]}

[STAGE:apiConfig]
{"apiConfig": {"list": "get@/ai/crud/order/page", "detail": "get@/ai/crud/order/{id}", "add": "post@/ai/crud/order", "update": "put@/ai/crud/order", "delete": "delete@/ai/crud/order/{ids}"}}',
        NULL, '2026-04-21 14:54:55'),
       (2046487267685437441, 1, 1, 'crud_config_builder', 'session_1776747312761_kdvupglex', 'user', '根据以下信息生成CRUD配置JSON：

configKey: order
tableName: order
描述: 开发订单管理页面

请按以下格式输出（每个阶段前加标记）：
[STAGE:searchSchema]
{"searchSchema": [...]}

[STAGE:columnsSchema]
{"columnsSchema": [...]}

[STAGE:editSchema]
{"editSchema": [...]}

[STAGE:apiConfig]
{"apiConfig": {...}}

每个字段项包含：field, label, type
搜索类型：input/select/daterange
编辑类型：input/textarea/select/radio/checkbox/switch/date/datetime/number
表格列包含：key, title, dataIndex，最后加actions列
基类字段不进editSchema
apiConfig格式：{list: ''get@/ai/crud/order/page'', ...}
', NULL, '2026-04-21 15:12:41'),
       (2046487267953872897, 1, 1, 'crud_config_builder', 'session_1776747312761_kdvupglex', 'assistant', '[STAGE:searchSchema]
{"tableName":"order","tableComment":"订单管理","searchSchema":[{"field":"orderNo","label":"订单号","type":"input"},{"field":"customerName","label":"客户名称","type":"input"},{"field":"status","label":"订单状态","type":"select","dictType":"sys_order_status"},{"field":"orderDate","label":"下单时间","type":"daterange"}]}

[STAGE:columnsSchema]
{"columnsSchema":[{"key":"orderNo","title":"订单号","dataIndex":"orderNo"},{"key":"customerName","title":"客户名称","dataIndex":"customerName"},{"key":"orderAmount","title":"订单金额","dataIndex":"orderAmount"},{"key":"status","title":"订单状态","dataIndex":"status"},{"key":"orderDate","title":"下单时间","dataIndex":"orderDate"},{"key":"createTime","title":"创建时间","dataIndex":"createTime"},{"key":"actions","title":"操作","dataIndex":"actions"}]}

[STAGE:editSchema]
{"editSchema":[{"field":"orderNo","label":"订单号","type":"input","required":true},{"field":"customerName","label":"客户名称","type":"input","required":true},{"field":"orderAmount","label":"订单金额","type":"number","required":true},{"field":"status","label":"订单状态","type":"select","required":true,"dictType":"sys_order_status"},{"field":"orderDate","label":"下单时间","type":"datetime","required":true},{"field":"remark","label":"备注","type":"textarea"}]}

[STAGE:apiConfig]
{"apiConfig":{"list":"get@/ai/crud/order/page","detail":"get@/ai/crud/order/{id}","add":"post@/ai/crud/order","update":"put@/ai/crud/order","delete":"delete@/ai/crud/order/{ids}"}}',
        NULL, '2026-04-21 15:12:41'),
       (2046491795449188354, 1, 1, 'crud_config_builder', 'session_1776756566619_srp4e461a', 'user', '根据以下信息生成CRUD配置JSON：

configKey: biz_order
tableName: biz_order
描述: 订单管理功能

请按以下格式输出（每个阶段前加标记）：
[STAGE:searchSchema]
{"searchSchema": [...]}

[STAGE:columnsSchema]
{"columnsSchema": [...]}

[STAGE:editSchema]
{"editSchema": [...]}

[STAGE:apiConfig]
{"apiConfig": {...}}

每个字段项包含：field, label, type
搜索类型：input/select/daterange
编辑类型：input/textarea/select/radio/checkbox/switch/date/datetime/number
表格列包含：key, title, dataIndex，最后加actions列
基类字段不进editSchema
apiConfig格式：{list: ''get@/ai/crud/biz_order/page'', ...}
', NULL, '2026-04-21 15:30:41'),
       (2046491795746983938, 1, 1, 'crud_config_builder', 'session_1776756566619_srp4e461a', 'assistant', '[STAGE:searchSchema]
{"tableName":"biz_order","tableComment":"订单管理功能","searchSchema":[{"field":"orderNo","label":"订单编号","type":"input"},{"field":"customerName","label":"客户名称","type":"input"},{"field":"status","label":"订单状态","type":"select","dictType":"biz_order_status"},{"field":"orderDate","label":"下单时间","type":"daterange"}]}

[STAGE:columnsSchema]
{"columnsSchema":[{"key":"orderNo","title":"订单编号","dataIndex":"orderNo"},{"key":"customerName","title":"客户名称","dataIndex":"customerName"},{"key":"orderAmount","title":"订单金额","dataIndex":"orderAmount"},{"key":"status","title":"订单状态","dataIndex":"status"},{"key":"orderDate","title":"下单时间","dataIndex":"orderDate"},{"key":"createTime","title":"创建时间","dataIndex":"createTime"},{"key":"actions","title":"操作","dataIndex":"actions"}]}

[STAGE:editSchema]
{"editSchema":[{"field":"orderNo","label":"订单编号","type":"input","required":true},{"field":"customerName","label":"客户名称","type":"input","required":true},{"field":"orderAmount","label":"订单金额","type":"number","required":true},{"field":"status","label":"订单状态","type":"select","required":true,"dictType":"biz_order_status"},{"field":"orderDate","label":"下单时间","type":"datetime","required":true},{"field":"remark","label":"备注","type":"textarea"}]}

[STAGE:apiConfig]
{"apiConfig":{"list":"get@/ai/crud/biz_order/page","detail":"get@/ai/crud/biz_order/{id}","add":"post@/ai/crud/biz_order","update":"put@/ai/crud/biz_order","delete":"delete@/ai/crud/biz_order/{ids}"}}',
        NULL, '2026-04-21 15:30:41');
INSERT INTO ai_chat_record (id, tenant_id, user_id, agent_code, session_id, `role`, content, token_usage, create_time)
VALUES (2046492487278661633, 1, 1, 'crud_config_builder', 'session_1776756704238_hod7nesy2', 'user', '订单管理功能',
        NULL, '2026-04-21 15:33:26'),
       (2046492487979110402, 1, 1, 'crud_config_builder', 'session_1776756704238_hod7nesy2', 'assistant', '[STAGE:searchSchema]
{"tableName": "order", "tableComment": "订单管理功能", "searchSchema": [{"field": "orderNo", "label": "订单编号", "type": "input"}, {"field": "customerName", "label": "客户名称", "type": "input"}, {"field": "orderStatus", "label": "订单状态", "type": "select", "dictType": "sys_order_status"}, {"field": "orderTime", "label": "下单时间", "type": "daterange"}, {"field": "contactPhone", "label": "联系电话", "type": "input"}]}

[STAGE:columnsSchema]
{"columnsSchema": [{"key": "orderNo", "title": "订单编号", "dataIndex": "orderNo"}, {"key": "customerName", "title": "客户名称", "dataIndex": "customerName"}, {"key": "orderStatus", "title": "订单状态", "dataIndex": "orderStatus"}, {"key": "orderAmount", "title": "订单金额", "dataIndex": "orderAmount"}, {"key": "orderTime", "title": "下单时间", "dataIndex": "orderTime"}, {"key": "contactPhone", "title": "联系电话", "dataIndex": "contactPhone"}, {"key": "actions", "title": "操作", "dataIndex": "actions"}]}

[STAGE:editSchema]
{"editSchema": [{"field": "orderNo", "label": "订单编号", "type": "input", "required": true}, {"field": "customerName", "label": "客户名称", "type": "input", "required": true}, {"field": "contactPhone", "label": "联系电话", "type": "input", "required": true}, {"field": "orderStatus", "label": "订单状态", "type": "select", "required": true, "dictType": "sys_order_status"}, {"field": "orderAmount", "label": "订单金额", "type": "number", "required": true}, {"field": "orderTime", "label": "下单时间", "type": "datetime", "required": true}, {"field": "remark", "label": "备注", "type": "textarea", "required": false}]}

[STAGE:apiConfig]
{"apiConfig": {"list": "get@/ai/crud/order/page", "detail": "get@/ai/crud/order/{id}", "add": "post@/ai/crud/order", "update": "put@/ai/crud/order", "delete": "delete@/ai/crud/order/{id}"}}',
        NULL, '2026-04-21 15:33:26'),
       (2046495684453994498, 1, 1, 'crud_config_builder', 'session_1776757442068_wggwvw50m', 'user', '订单管理', NULL,
        '2026-04-21 15:46:08'),
       (2046495684764372993, 1, 1, 'crud_config_builder', 'session_1776757442068_wggwvw50m', 'assistant', '[STAGE:searchSchema]
{"tableName": "order", "tableComment": "订单管理", "searchSchema": [{"field": "orderNo", "label": "订单编号", "type": "input"}, {"field": "customerName", "label": "客户名称", "type": "input"}, {"field": "orderStatus", "label": "订单状态", "type": "select", "dictType": "sys_order_status"}, {"field": "orderTime", "label": "下单时间", "type": "daterange"}, {"field": "contactPhone", "label": "联系电话", "type": "input"}]}

[STAGE:columnsSchema]
{"columnsSchema": [{"key": "orderNo", "title": "订单编号", "dataIndex": "orderNo"}, {"key": "customerName", "title": "客户名称", "dataIndex": "customerName"}, {"key": "orderStatus", "title": "订单状态", "dataIndex": "orderStatus"}, {"key": "orderAmount", "title": "订单金额", "dataIndex": "orderAmount"}, {"key": "orderTime", "title": "下单时间", "dataIndex": "orderTime"}, {"key": "contactPhone", "title": "联系电话", "dataIndex": "contactPhone"}, {"key": "actions", "title": "操作", "dataIndex": "actions"}]}

[STAGE:editSchema]
{"editSchema": [{"field": "orderNo", "label": "订单编号", "type": "input", "required": true}, {"field": "customerName", "label": "客户名称", "type": "input", "required": true}, {"field": "contactPhone", "label": "联系电话", "type": "input", "required": true}, {"field": "orderStatus", "label": "订单状态", "type": "select", "required": true, "dictType": "sys_order_status"}, {"field": "orderAmount", "label": "订单金额", "type": "number", "required": true}, {"field": "orderTime", "label": "下单时间", "type": "datetime", "required": true}, {"field": "remark", "label": "备注", "type": "textarea", "required": false}]}

[STAGE:apiConfig]
{"apiConfig": {"list": "get@/ai/crud/order/page", "detail": "get@/ai/crud/order/{id}", "add": "post@/ai/crud/order", "update": "put@/ai/crud/order", "delete": "delete@/ai/crud/order/{id}"}}',
        NULL, '2026-04-21 15:46:08'),
       (2046496439453880321, 1, 1, 'crud_config_builder', 'session_1776757666909_7udjubkdw', 'user', '订单管理', NULL,
        '2026-04-21 15:49:08'),
       (2046496439747481601, 1, 1, 'crud_config_builder', 'session_1776757666909_7udjubkdw', 'assistant', '[STAGE:searchSchema]
{"searchSchema": [{"field": "orderNo", "label": "订单编号", "type": "input"}, {"field": "customerName", "label": "客户名称", "type": "input"}, {"field": "orderStatus", "label": "订单状态", "type": "select", "dictType": "sys_order_status"}, {"field": "orderTime", "label": "下单时间", "type": "daterange"}, {"field": "contactPhone", "label": "联系电话", "type": "input"}]}

[STAGE:columnsSchema]
{"columnsSchema": [{"key": "orderNo", "title": "订单编号", "dataIndex": "orderNo"}, {"key": "customerName", "title": "客户名称", "dataIndex": "customerName"}, {"key": "orderStatus", "title": "订单状态", "dataIndex": "orderStatus"}, {"key": "orderAmount", "title": "订单金额", "dataIndex": "orderAmount"}, {"key": "orderTime", "title": "下单时间", "dataIndex": "orderTime"}, {"key": "contactPhone", "title": "联系电话", "dataIndex": "contactPhone"}, {"key": "actions", "title": "操作", "dataIndex": "actions"}]}

[STAGE:editSchema]
{"editSchema": [{"field": "orderNo", "label": "订单编号", "type": "input", "required": true}, {"field": "customerName", "label": "客户名称", "type": "input", "required": true}, {"field": "contactPhone", "label": "联系电话", "type": "input", "required": true}, {"field": "orderStatus", "label": "订单状态", "type": "select", "required": true, "dictType": "sys_order_status"}, {"field": "orderAmount", "label": "订单金额", "type": "number", "required": true}, {"field": "orderTime", "label": "下单时间", "type": "datetime", "required": true}, {"field": "remark", "label": "备注", "type": "textarea", "required": false}]}

[STAGE:apiConfig]
{"apiConfig": {"list": "get@/ai/crud/order/page", "detail": "get@/ai/crud/order/{id}", "add": "post@/ai/crud/order", "update": "put@/ai/crud/order", "delete": "delete@/ai/crud/order/{id}"}}',
        NULL, '2026-04-21 15:49:08'),
       (2046502567038488577, 1, 1, 'crud_config_builder', 'session_1776759135433_h417mddxv', 'user', '订单管理', NULL,
        '2026-04-21 16:13:29'),
       (2046502567285952514, 1, 1, 'crud_config_builder', 'session_1776759135433_h417mddxv', 'assistant', '[STAGE:searchSchema]
{"tableName":"order","tableComment":"订单管理","searchSchema":[{"field":"orderNo","label":"订单编号","type":"input"},{"field":"customerName","label":"客户名称","type":"input"},{"field":"orderStatus","label":"订单状态","type":"select","dictType":"order_status"},{"field":"createTime","label":"下单时间","type":"daterange"}]}

[STAGE:columnsSchema]
{"columnsSchema":[{"key":"orderNo","title":"订单编号","dataIndex":"orderNo"},{"key":"customerName","title":"客户名称","dataIndex":"customerName"},{"key":"orderAmount","title":"订单金额","dataIndex":"orderAmount"},{"key":"orderStatus","title":"订单状态","dataIndex":"orderStatus"},{"key":"createTime","title":"下单时间","dataIndex":"createTime"},{"key":"actions","title":"操作","dataIndex":"actions"}]}

[STAGE:editSchema]
{"editSchema":[{"field":"orderNo","label":"订单编号","type":"input","required":true},{"field":"customerName","label":"客户名称","type":"input","required":true},{"field":"orderAmount","label":"订单金额","type":"number","required":true},{"field":"orderStatus","label":"订单状态","type":"select","required":true,"dictType":"order_status"},{"field":"remark","label":"备注","type":"textarea","required":false}]}

[STAGE:apiConfig]
{"apiConfig":{"list":"get@/ai/crud/order/page","info":"get@/ai/crud/order/info","add":"post@/ai/crud/order","edit":"put@/ai/crud/order","delete":"delete@/ai/crud/order"}}',
        NULL, '2026-04-21 16:13:29'),
       (2046503642000855042, 1, 1, 'crud_config_builder', 'session_1776759385931_7nm1hml5v', 'user', '订单管理
', NULL, '2026-04-21 16:17:45'),
       (2046503642294456321, 1, 1, 'crud_config_builder', 'session_1776759385931_7nm1hml5v', 'assistant', '[STAGE:searchSchema]
{"tableName":"order","tableComment":"订单管理","searchSchema":[{"field":"orderNo","label":"订单编号","type":"input"},{"field":"customerName","label":"客户名称","type":"input"},{"field":"orderStatus","label":"订单状态","type":"select","dictType":"order_status"},{"field":"orderTime","label":"下单时间","type":"daterange"}]}

[STAGE:columnsSchema]
{"columnsSchema":[{"key":"orderNo","title":"订单编号","dataIndex":"orderNo"},{"key":"customerName","title":"客户名称","dataIndex":"customerName"},{"key":"orderAmount","title":"订单金额","dataIndex":"orderAmount"},{"key":"orderStatus","title":"订单状态","dataIndex":"orderStatus"},{"key":"orderTime","title":"下单时间","dataIndex":"orderTime"},{"key":"actions","title":"操作","dataIndex":"actions"}]}

[STAGE:editSchema]
{"editSchema":[{"field":"orderNo","label":"订单编号","type":"input","required":true},{"field":"customerName","label":"客户名称","type":"input","required":true},{"field":"orderAmount","label":"订单金额","type":"number","required":true},{"field":"orderStatus","label":"订单状态","type":"select","required":true,"dictType":"order_status"},{"field":"orderTime","label":"下单时间","type":"datetime","required":false},{"field":"remark","label":"备注","type":"textarea","required":false}]}

[STAGE:apiConfig]
{"apiConfig":{"list":"get@/ai/crud/order/page","info":"get@/ai/crud/order/info","add":"post@/ai/crud/order","edit":"put@/ai/crud/order","delete":"delete@/ai/crud/order"}}',
        NULL, '2026-04-21 16:17:45');
INSERT INTO ai_chat_record (id, tenant_id, user_id, agent_code, session_id, `role`, content, token_usage, create_time)
VALUES (2046505933164515329, 1, 1, 'crud_config_builder', 'session_1776759937586_dydqfarx8', 'user', '订单管理', NULL,
        '2026-04-21 16:26:51'),
       (2046505933407784962, 1, 1, 'crud_config_builder', 'session_1776759937586_dydqfarx8', 'assistant', '[STAGE:searchSchema]
{"tableName": "order", "tableComment": "订单管理", "searchSchema": [{"field": "orderNo", "label": "订单编号", "type": "input"}, {"field": "customerName", "label": "客户名称", "type": "input"}, {"field": "orderStatus", "label": "订单状态", "type": "select", "dictType": "order_status"}, {"field": "createTime", "label": "创建时间", "type": "daterange"}]}

[STAGE:columnsSchema]
{"columnsSchema": [{"key": "orderNo", "title": "订单编号", "dataIndex": "orderNo"}, {"key": "customerName", "title": "客户名称", "dataIndex": "customerName"}, {"key": "orderAmount", "title": "订单金额", "dataIndex": "orderAmount"}, {"key": "orderStatus", "title": "订单状态", "dataIndex": "orderStatus"}, {"key": "orderType", "title": "订单类型", "dataIndex": "orderType"}, {"key": "createTime", "title": "创建时间", "dataIndex": "createTime"}, {"key": "actions", "title": "操作", "dataIndex": "actions"}]}

[STAGE:editSchema]
{"editSchema": [{"field": "orderNo", "label": "订单编号", "type": "input", "required": true}, {"field": "customerName", "label": "客户名称", "type": "input", "required": true}, {"field": "orderAmount", "label": "订单金额", "type": "number", "required": true}, {"field": "orderStatus", "label": "订单状态", "type": "select", "required": true, "dictType": "order_status"}, {"field": "orderType", "label": "订单类型", "type": "select", "required": true, "dictType": "order_type"}, {"field": "orderDate", "label": "下单时间", "type": "datetime", "required": true}, {"field": "remark", "label": "备注", "type": "textarea"}]}

[STAGE:apiConfig]
{"apiConfig": {"list": "get@/ai/crud/order/page", "add": "post@/ai/crud/order/add", "update": "put@/ai/crud/order/update", "delete": "delete@/ai/crud/order/delete", "detail": "get@/ai/crud/order/detail"}}',
        NULL, '2026-04-21 16:26:51'),
       (2046511318764097538, 1, 1, 'crud_config_builder', 'session_1776759937586_dydqfarx8', 'user', '订单管理', NULL,
        '2026-04-21 16:48:15'),
       (2046511995057868802, 1, 1, 'crud_config_builder', 'session_1776759937586_dydqfarx8', 'user', '订单管理
', NULL, '2026-04-21 16:50:57'),
       (2046511995397607426, 1, 1, 'crud_config_builder', 'session_1776759937586_dydqfarx8', 'assistant', '[STAGE:searchSchema]
[
  {"field": "orderNo", "label": "订单编号", "type": "input"},
  {"field": "customerName", "label": "客户名称", "type": "input"},
  {"field": "orderStatus", "label": "订单状态", "type": "select", "dictType": "order_status"},
  {"field": "createTime", "label": "创建时间", "type": "daterange"}
]
[STAGE:columnsSchema]
[
  {"key": "orderNo", "title": "订单编号", "dataIndex": "orderNo"},
  {"key": "customerName", "title": "客户名称", "dataIndex": "customerName"},
  {"key": "orderAmount", "title": "订单金额", "dataIndex": "orderAmount"},
  {"key": "orderStatus", "title": "订单状态", "dataIndex": "orderStatus", "render": {"type": "dictTag", "dictType": "order_status"}},
  {"key": "orderType", "title": "订单类型", "dataIndex": "orderType", "render": {"type": "dictTag", "dictType": "order_type"}},
  {"key": "payStatus", "title": "支付状态", "dataIndex": "payStatus", "render": {"type": "dictTag", "dictType": "pay_status"}},
  {"key": "createTime", "title": "创建时间", "dataIndex": "createTime"},
  {"key": "actions", "title": "操作", "dataIndex": "actions", "width": 180, "fixed": "right"}
]
[STAGE:editSchema]
[
  {"field": "orderNo", "label": "订单编号", "type": "input", "required": true},
  {"field": "customerName", "label": "客户名称", "type": "input", "required": true},
  {"field": "customerPhone", "label": "联系电话", "type": "input", "required": true},
  {"field": "orderAmount", "label": "订单金额", "type": "number", "required": true},
  {"field": "orderStatus", "label": "订单状态", "type": "select", "required": true, "dictType": "order_status"},
  {"field": "orderType", "label": "订单类型", "type": "select", "required": true, "dictType": "order_type"},
  {"field": "payStatus", "label": "支付状态", "type": "select", "required": true, "dictType": "pay_status"},
  {"field": "orderDate", "label": "下单时间", "type": "datetime", "required": true},
  {"field": "shippingAddress", "label": "收货地址", "type": "textarea"},
  {"field": "remark", "label": "备注", "type": "textarea"}
]
[STAGE:apiConfig]
{
  "list": "get@/ai/crud/order/page",
  "detail": "get@/ai/crud/order/{id}",
  "create": "post@/ai/crud/order",
  "update": "put@/ai/crud/order",
  "delete": "delete@/ai/crud/order/{id}"
}', NULL, '2026-04-21 16:50:57'),
       (2046538611284803585, 1, 1, 'crud_config_builder', 'session_1776767605200_1qp8rzz2p', 'user', '订单管理页面
', NULL, '2026-04-21 18:36:42'),
       (2046538612161413121, 1, 1, 'crud_config_builder', 'session_1776767605200_1qp8rzz2p', 'assistant', '[STAGE:meta]
{"configKey": "biz_order", "tableName": "biz_order", "tableComment": "订单管理"}

[STAGE:searchSchema]
[{"field": "orderNo", "label": "订单编号", "type": "input"}, {"field": "customerName", "label": "客户名称", "type": "input"}, {"field": "orderStatus", "label": "订单状态", "type": "select", "dictType": "order_status"}, {"field": "orderDate", "label": "下单时间", "type": "daterange"}]

[STAGE:columnsSchema]
[{"key": "orderNo", "title": "订单编号", "dataIndex": "orderNo"}, {"key": "customerName", "title": "客户名称", "dataIndex": "customerName"}, {"key": "orderAmount", "title": "订单金额", "dataIndex": "orderAmount"}, {"key": "orderStatus", "title": "订单状态", "dataIndex": "orderStatus", "render": {"type": "dictTag", "dictType": "order_status"}}, {"key": "orderDate", "title": "下单时间", "dataIndex": "orderDate"}, {"key": "actions", "title": "操作", "dataIndex": "actions", "width": 180, "fixed": "right"}]

[STAGE:editSchema]
[{"field": "orderNo", "label": "订单编号", "type": "input", "required": true}, {"field": "customerName", "label": "客户名称", "type": "input", "required": true}, {"field": "contactPhone", "label": "联系电话", "type": "input", "required": false, "props": {"pattern": "^1[3-9]\\\\d{9}$"}}, {"field": "orderAmount", "label": "订单金额", "type": "number", "required": true, "props": {"precision": 2}}, {"field": "orderType", "label": "订单类型", "type": "select", "required": true, "dictType": "order_type"}, {"field": "orderStatus", "label": "订单状态", "type": "select", "required": true, "dictType": "order_status"}, {"field": "orderDate", "label": "下单时间", "type": "datetime", "required": true}, {"field": "shippingAddress", "label": "收货地址", "type": "textarea", "required": false}, {"field": "remark", "label": "备注", "type": "textarea", "required": false}]

[STAGE:apiConfig]
{"list": "get@/ai/crud/biz_order/page", "detail": "get@/ai/crud/biz_order/{id}", "create": "post@/ai/crud/biz_order", "update": "put@/ai/crud/biz_order", "delete": "delete@/ai/crud/biz_order/{id}"}

[STAGE:createTableSql]
CREATE TABLE `biz_order` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT ''主键ID'',
  `tenant_id` BIGINT DEFAULT NULL COMMENT ''租户ID'',
  `order_no` VARCHAR(64) NOT NULL COMMENT ''订单编号'',
  `customer_name` VARCHAR(100) DEFAULT NULL COMMENT ''客户名称'',
  `contact_phone` VARCHAR(20) DEFAULT NULL COMMENT ''联系电话'',
  `order_amount` DECIMAL(10,2) DEFAULT 0.00 COMMENT ''订单金额'',
  `order_type` TINYINT DEFAULT 0 COMMENT ''订单类型'',
  `order_status` TINYINT DEFAULT 0 COMMENT ''订单状态'',
  `order_date` DATETIME DEFAULT NULL COMMENT ''下单时间'',
  `shipping_address` VARCHAR(255) DEFAULT NULL COMMENT ''收货地址'',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT ''备注'',
  `create_by` VARCHAR(64) DEFAULT NULL COMMENT ''创建者'',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT ''创建时间'',
  `create_dept` BIGINT DEFAULT NULL COMMENT ''创建部门'',
  `update_by` VARCHAR(64) DEFAULT NULL COMMENT ''更新者'',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT ''更新时间'',
  `del_flag` TINYINT DEFAULT 0 COMMENT ''删除标志(0:正常,1:删除)'',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT=''订单管理'';', NULL, '2026-04-21 18:36:42'),
       (2046542122571657218, 1, 1, 'crud_config_builder', 'session_1776767605200_1qp8rzz2p', 'user', '订单信息管理',
        NULL, '2026-04-21 18:50:40'),
       (2046542122873647106, 1, 1, 'crud_config_builder', 'session_1776767605200_1qp8rzz2p', 'assistant', '[STAGE:meta]
{"configKey": "biz_order", "tableName": "biz_order", "tableComment": "订单信息表"}

[STAGE:searchSchema]
[{"field": "orderNo", "label": "订单编号", "type": "input"}, {"field": "customerName", "label": "客户名称", "type": "input"}, {"field": "orderStatus", "label": "订单状态", "type": "select", "dictType": "biz_order_status"}, {"field": "orderDate", "label": "下单时间", "type": "daterange"}]

[STAGE:columnsSchema]
[{"key": "orderNo", "title": "订单编号", "dataIndex": "orderNo"}, {"key": "customerName", "title": "客户名称", "dataIndex": "customerName"}, {"key": "orderAmount", "title": "订单金额", "dataIndex": "orderAmount"}, {"key": "orderStatus", "title": "订单状态", "dataIndex": "orderStatus", "render": {"type": "dictTag", "dictType": "biz_order_status"}}, {"key": "orderType", "title": "订单类型", "dataIndex": "orderType", "render": {"type": "dictTag", "dictType": "biz_order_type"}}, {"key": "orderDate", "title": "下单时间", "dataIndex": "orderDate"}, {"key": "actions", "title": "操作", "dataIndex": "actions", "width": 180, "fixed": "right"}]

[STAGE:editSchema]
[{"field": "orderNo", "label": "订单编号", "type": "input", "required": true}, {"field": "customerName", "label": "客户名称", "type": "input", "required": true}, {"field": "orderAmount", "label": "订单金额", "type": "number", "required": true}, {"field": "orderStatus", "label": "订单状态", "type": "select", "required": true, "dictType": "biz_order_status"}, {"field": "orderType", "label": "订单类型", "type": "select", "dictType": "biz_order_type"}, {"field": "orderDate", "label": "下单时间", "type": "datetime", "required": true}, {"field": "remark", "label": "备注", "type": "textarea"}]

[STAGE:apiConfig]
{"list": "get@/ai/crud/biz_order/page", "detail": "get@/ai/crud/biz_order/{id}", "create": "post@/ai/crud/biz_order", "update": "put@/ai/crud/biz_order", "delete": "delete@/ai/crud/biz_order/{id}"}

[STAGE:createTableSql]
CREATE TABLE `biz_order` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT ''主键ID'',
  `tenant_id` bigint(20) DEFAULT NULL COMMENT ''租户ID'',
  `order_no` varchar(64) NOT NULL COMMENT ''订单编号'',
  `customer_name` varchar(100) DEFAULT NULL COMMENT ''客户名称'',
  `order_amount` decimal(10,2) DEFAULT ''0.00'' COMMENT ''订单金额'',
  `order_status` tinyint(4) DEFAULT ''0'' COMMENT ''订单状态'',
  `order_type` tinyint(4) DEFAULT ''0'' COMMENT ''订单类型'',
  `order_date` datetime DEFAULT NULL COMMENT ''下单时间'',
  `remark` varchar(500) DEFAULT NULL COMMENT ''备注'',
  `create_by` varchar(64) DEFAULT NULL COMMENT ''创建者'',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT ''创建时间'',
  `create_dept` bigint(20) DEFAULT NULL COMMENT ''创建部门'',
  `update_by` varchar(64) DEFAULT NULL COMMENT ''更新者'',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT ''更新时间'',
  `del_flag` tinyint(1) DEFAULT ''0'' COMMENT ''删除标志(0正常 1删除)'',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT=''订单信息表'';', NULL, '2026-04-21 18:50:40'),
       (2046552747003736066, 1, 1, 'crud_config_builder', 'session_1776771097009_35ilx1xck', 'user', '订单管理', NULL,
        '2026-04-21 19:32:53');
INSERT INTO ai_chat_record (id, tenant_id, user_id, agent_code, session_id, `role`, content, token_usage, create_time)
VALUES (2046552747284754433, 1, 1, 'crud_config_builder', 'session_1776771097009_35ilx1xck', 'assistant', '[STAGE:meta]
{"configKey": "biz_order", "tableName": "biz_order", "tableComment": "订单管理"}

[STAGE:searchSchema]
[{"field": "orderNo", "label": "订单编号", "type": "input"}, {"field": "customerName", "label": "客户姓名", "type": "input"}, {"field": "orderStatus", "label": "订单状态", "type": "select", "dictType": "biz_order_status"}, {"field": "orderTime", "label": "下单时间", "type": "daterange"}]

[STAGE:columnsSchema]
[{"key": "orderNo", "title": "订单编号", "dataIndex": "orderNo"}, {"key": "customerName", "title": "客户姓名", "dataIndex": "customerName"}, {"key": "customerPhone", "title": "客户电话", "dataIndex": "customerPhone"}, {"key": "orderAmount", "title": "订单金额", "dataIndex": "orderAmount"}, {"key": "orderStatus", "title": "订单状态", "dataIndex": "orderStatus", "render": {"type": "dictTag", "dictType": "biz_order_status"}}, {"key": "orderType", "title": "订单类型", "dataIndex": "orderType", "render": {"type": "dictTag", "dictType": "biz_order_type"}}, {"key": "orderTime", "title": "下单时间", "dataIndex": "orderTime"}, {"key": "actions", "title": "操作", "dataIndex": "actions", "width": 180, "fixed": "right"}]

[STAGE:editSchema]
[{"field": "orderNo", "label": "订单编号", "type": "input", "required": true}, {"field": "customerName", "label": "客户姓名", "type": "input", "required": true}, {"field": "customerPhone", "label": "客户电话", "type": "input", "required": true}, {"field": "orderAmount", "label": "订单金额", "type": "number", "required": true}, {"field": "orderStatus", "label": "订单状态", "type": "select", "required": true, "dictType": "biz_order_status"}, {"field": "orderType", "label": "订单类型", "type": "select", "required": true, "dictType": "biz_order_type"}, {"field": "orderTime", "label": "下单时间", "type": "datetime", "required": true}, {"field": "remark", "label": "备注", "type": "textarea", "required": false}]

[STAGE:apiConfig]
{"list": "get@/ai/crud/biz_order/page", "detail": "get@/ai/crud/biz_order/{id}", "create": "post@/ai/crud/biz_order", "update": "put@/ai/crud/biz_order", "delete": "delete@/ai/crud/biz_order/{id}"}

[STAGE:createTableSql]
CREATE TABLE `biz_order` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT ''主键ID'',
  `order_no` varchar(64) NOT NULL COMMENT ''订单编号'',
  `customer_name` varchar(100) NOT NULL COMMENT ''客户姓名'',
  `customer_phone` varchar(20) DEFAULT NULL COMMENT ''客户电话'',
  `order_amount` decimal(10,2) DEFAULT NULL COMMENT ''订单金额'',
  `order_status` tinyint(1) DEFAULT ''0'' COMMENT ''订单状态(0:待支付,1:已支付,2:已发货,3:已完成,4:已取消)'',
  `order_type` tinyint(1) DEFAULT ''1'' COMMENT ''订单类型(1:普通,2:团购,3:秒杀)'',
  `order_time` datetime DEFAULT NULL COMMENT ''下单时间'',
  `remark` varchar(500) DEFAULT NULL COMMENT ''备注'',
  `tenant_id` bigint(20) DEFAULT NULL COMMENT ''租户ID'',
  `create_by` varchar(64) DEFAULT NULL COMMENT ''创建人'',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT ''创建时间'',
  `create_dept` bigint(20) DEFAULT NULL COMMENT ''创建部门'',
  `update_by` varchar(64) DEFAULT NULL COMMENT ''更新人'',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT ''更新时间'',
  `del_flag` tinyint(1) DEFAULT ''0'' COMMENT ''删除标志(0:正常,1:删除)'',
  PRIMARY KEY (`id`),
  KEY `idx_order_no` (`order_no`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT=''订单管理'';', NULL, '2026-04-21 19:32:53'),
       (2046798852337475586, 1, 1, 'crud_config_builder', 'session_1776829741539_duoq4dqwg', 'user',
        '帮我实现一个订单管理页面，包含订单核心参数', NULL, '2026-04-22 11:50:49'),
       (2046798852689797122, 1, 1, 'crud_config_builder', 'session_1776829741539_duoq4dqwg', 'assistant', '[STAGE:meta]
{"configKey":"order_manage","tableName":"order_manage","tableComment":"订单管理表"}

[STAGE:searchSchema]
[{"field":"orderNo","label":"订单编号","type":"input"},{"field":"customerName","label":"客户姓名","type":"input"},{"field":"status","label":"订单状态","type":"select","dictType":"order_status"},{"field":"orderTime","label":"下单时间","type":"daterange"}]

[STAGE:columnsSchema]
[{"key":"orderNo","title":"订单编号","dataIndex":"orderNo"},{"key":"customerName","title":"客户姓名","dataIndex":"customerName"},{"key":"customerPhone","title":"联系电话","dataIndex":"customerPhone"},{"key":"status","title":"订单状态","dataIndex":"status","render":{"type":"dictTag","dictType":"order_status"}},{"key":"orderAmount","title":"订单金额","dataIndex":"orderAmount"},{"key":"payType","title":"支付方式","dataIndex":"payType","render":{"type":"dictTag","dictType":"pay_type"}},{"key":"orderTime","title":"下单时间","dataIndex":"orderTime"},{"key":"actions","title":"操作","dataIndex":"actions","width":180,"fixed":"right"}]

[STAGE:editSchema]
[{"field":"customerName","label":"客户姓名","type":"input","required":true},{"field":"customerPhone","label":"联系电话","type":"input","required":true,"props":{"pattern":"^1[3-9]\\\\d{9}$"}},{"field":"status","label":"订单状态","type":"select","dictType":"order_status","required":true},{"field":"orderAmount","label":"订单金额","type":"number","required":true,"props":{"pattern":"^\\\\d+(\\\\.\\\\d{1,2})?$"}},{"field":"payType","label":"支付方式","type":"select","dictType":"pay_type","required":true},{"field":"orderTime","label":"下单时间","type":"datetime","required":true},{"field":"address","label":"收货地址","type":"textarea","required":false},{"field":"remark","label":"备注","type":"textarea","required":false}]

[STAGE:apiConfig]
{"list":"GET@/api/order_manage/list","detail":"GET@/api/order_manage/detail","create":"POST@/api/order_manage/create","update":"PUT@/api/order_manage/update","delete":"DELETE@/api/order_manage/delete"}

[STAGE:dictConfig]
[{"dictType":"order_status","dictName":"订单状态","isNew":true,"items":[{"dictLabel":"待付款","dictValue":"0","dictSort":1},{"dictLabel":"已付款","dictValue":"1","dictSort":2},{"dictLabel":"已发货","dictValue":"2","dictSort":3},{"dictLabel":"已完成","dictValue":"3","dictSort":4},{"dictLabel":"已取消","dictValue":"4","dictSort":5}]},{"dictType":"pay_type","dictName":"支付方式","isNew":true,"items":[{"dictLabel":"支付宝","dictValue":"1","dictSort":1},{"dictLabel":"微信支付","dictValue":"2","dictSort":2},{"dictLabel":"银行卡","dictValue":"3","dictSort":3}]}]

[STAGE:desensitizeConfig]
{"customerPhone":{"type":"PHONE"}}

[STAGE:transConfig]
{"status":{"dictType":"order_status","targetField":"statusName"},"payType":{"dictType":"pay_type","targetField":"payTypeName"}}

[STAGE:createTableSql]
CREATE TABLE `order_manage` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT ''主键ID'',
  `tenant_id` bigint(20) DEFAULT NULL COMMENT ''租户ID'',
  `order_no` varchar(64) NOT NULL COMMENT ''订单编号'',
  `customer_name` varchar(100) DEFAULT NULL COMMENT ''客户姓名'',
  `customer_phone` varchar(20) DEFAULT NULL COMMENT ''联系电话'',
  `status` tinyint(4) DEFAULT ''0'' COMMENT ''订单状态'',
  `order_amount` decimal(10,2) DEFAULT NULL COMMENT ''订单金额'',
  `pay_type` tinyint(4) DEFAULT NULL COMMENT ''支付方式'',
  `order_time` datetime DEFAULT NULL COMMENT ''下单时间'',
  `address` varchar(255) DEFAULT NULL COMMENT ''收货地址'',
  `remark` varchar(500) DEFAULT NULL COMMENT ''备注'',
  `create_by` varchar(64) DEFAULT NULL COMMENT ''创建人'',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT ''创建时间'',
  `create_dept` varchar(64) DEFAULT NULL COMMENT ''创建部门'',
  `update_by` varchar(64) DEFAULT NULL COMMENT ''更新人'',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT ''更新时间'',
  `del_flag` tinyint(1) DEFAULT ''0'' COMMENT ''删除标志'',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT=''订单管理表'';', NULL, '2026-04-22 11:50:49'),
       (2046813772684763137, 1, 1, 'crud_config_builder', 'session_1776833314002_gc50bbkv8', 'user',
        '帮我实现订单管理相关功能', NULL, '2026-04-22 12:50:06'),
       (2046813773234216961, 1, 1, 'crud_config_builder', 'session_1776833314002_gc50bbkv8', 'assistant', '[STAGE:meta]
{"configKey": "order_manage", "tableName": "order_manage", "tableComment": "订单管理表"}

[STAGE:searchSchema]
[{"field": "orderNo", "label": "订单编号", "type": "input"}, {"field": "customerName", "label": "客户名称", "type": "input"}, {"field": "orderStatus", "label": "订单状态", "type": "select", "dictType": "order_status"}, {"field": "orderDate", "label": "下单时间", "type": "daterange"}]

[STAGE:columnsSchema]
[{"key": "orderNo", "title": "订单编号", "dataIndex": "orderNo"}, {"key": "customerName", "title": "客户名称", "dataIndex": "customerName"}, {"key": "orderAmount", "title": "订单金额", "dataIndex": "orderAmount"}, {"key": "orderStatus", "title": "订单状态", "dataIndex": "orderStatus", "render": {"type": "dictTag", "dictType": "order_status"}}, {"key": "orderType", "title": "订单类型", "dataIndex": "orderType", "render": {"type": "dictTag", "dictType": "order_type"}}, {"key": "contactPhone", "title": "联系电话", "dataIndex": "contactPhone"}, {"key": "orderDate", "title": "下单时间", "dataIndex": "orderDate"}, {"key": "actions", "title": "操作", "dataIndex": "actions", "width": 180, "fixed": "right"}]

[STAGE:editSchema]
[{"field": "orderNo", "label": "订单编号", "type": "input", "required": true}, {"field": "customerName", "label": "客户名称", "type": "input", "required": true}, {"field": "orderAmount", "label": "订单金额", "type": "number", "required": true, "props": {"precision": 2}}, {"field": "orderStatus", "label": "订单状态", "type": "select", "required": true, "dictType": "order_status"}, {"field": "orderType", "label": "订单类型", "type": "select", "dictType": "order_type"}, {"field": "contactPhone", "label": "联系电话", "type": "input", "required": true, "props": {"pattern": "^1[3-9]\\\\d{9}$", "message": "请输入正确的手机号"}}, {"field": "shippingAddress", "label": "收货地址", "type": "textarea"}, {"field": "orderDate", "label": "下单时间", "type": "datetime"}, {"field": "remark", "label": "备注", "type": "textarea"}]

[STAGE:apiConfig]
{"list": "GET@/order/manage/list", "detail": "GET@/order/manage/{id}", "create": "POST@/order/manage", "update": "PUT@/order/manage", "delete": "DELETE@/order/manage/{ids}"}

[STAGE:dictConfig]
[{"dictType": "order_status", "dictName": "订单状态", "isNew": true, "items": [{"dictLabel": "待付款", "dictValue": "0", "dictSort": 1}, {"dictLabel": "已付款", "dictValue": "1", "dictSort": 2}, {"dictLabel": "已发货", "dictValue": "2", "dictSort": 3}, {"dictLabel": "已完成", "dictValue": "3", "dictSort": 4}, {"dictLabel": "已取消", "dictValue": "4", "dictSort": 5}]}, {"dictType": "order_type", "dictName": "订单类型", "isNew": true, "items": [{"dictLabel": "普通订单", "dictValue": "1", "dictSort": 1}, {"dictLabel": "团购订单", "dictValue": "2", "dictSort": 2}, {"dictLabel": "预售订单", "dictValue": "3", "dictSort": 3}]}]

[STAGE:desensitizeConfig]
{"contactPhone": {"type": "PHONE"}, "customerName": {"type": "NAME"}}

[STAGE:encryptConfig]
{"enableEncrypt": false, "enableDecrypt": false, "operations": []}

[STAGE:transConfig]
{"orderStatus": {"dictType": "order_status", "targetField": "orderStatusName"}, "orderType": {"dictType": "order_type", "targetField": "orderTypeName"}}

[STAGE:createTableSql]
CREATE TABLE `order_manage` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT ''主键ID'',
  `tenant_id` bigint DEFAULT NULL COMMENT ''租户ID'',
  `order_no` varchar(64) NOT NULL COMMENT ''订单编号'',
  `customer_name` varchar(128) DEFAULT NULL COMMENT ''客户名称'',
  `order_amount` decimal(10,2) DEFAULT ''0.00'' COMMENT ''订单金额'',
  `order_status` tinyint DEFAULT ''0'' COMMENT ''订单状态(0:待付款,1:已付款,2:已发货,3:已完成,4:已取消)'',
  `order_type` tinyint DEFAULT ''1'' COMMENT ''订单类型(1:普通订单,2:团购订单,3:预售订单)'',
  `contact_phone` varchar(20) DEFAULT NULL COMMENT ''联系电话'',
  `shipping_address` varchar(512) DEFAULT NULL COMMENT ''收货地址'',
  `order_date` datetime DEFAULT NULL COMMENT ''下单时间'',
  `remark` varchar(1024) DEFAULT NULL COMMENT ''备注'',
  `create_by` varchar(64) DEFAULT NULL COMMENT ''创建人'',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT ''创建时间'',
  `create_dept` bigint DEFAULT NULL COMMENT ''创建部门'',
  `update_by` varchar(64) DEFAULT NULL COMMENT ''更新人'',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT ''更新时间'',
  `del_flag` tinyint DEFAULT ''0'' COMMENT ''删除标志(0:正常,1:删除)'',
  PRIMARY KEY (`id`),
  KEY `idx_order_no` (`order_no`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT=''订单管理表'';', NULL,
        '2026-04-22 12:50:06'),
       (2046816767682052098, 1, 1, 'crud_config_builder', 'session_1776833314002_gc50bbkv8', 'user', '帮我实现订单管理相关功能
', NULL, '2026-04-22 13:02:00'),
       (2046816768156008450, 1, 1, 'crud_config_builder', 'session_1776833314002_gc50bbkv8', 'assistant', '[STAGE:meta]
{"configKey": "order_manage", "tableName": "order_manage", "tableComment": "订单管理表"}

[STAGE:searchSchema]
[{"field": "orderNo", "label": "订单编号", "type": "input"}, {"field": "customerName", "label": "客户姓名", "type": "input"}, {"field": "orderStatus", "label": "订单状态", "type": "select", "dictType": "order_status"}, {"field": "orderType", "label": "订单类型", "type": "select", "dictType": "order_type"}, {"field": "createTime", "label": "创建时间", "type": "daterange"}]

[STAGE:columnsSchema]
[{"key": "orderNo", "title": "订单编号", "dataIndex": "orderNo"}, {"key": "customerName", "title": "客户姓名", "dataIndex": "customerName"}, {"key": "customerPhone", "title": "客户电话", "dataIndex": "customerPhone"}, {"key": "orderAmount", "title": "订单金额", "dataIndex": "orderAmount"}, {"key": "orderStatus", "title": "订单状态", "dataIndex": "orderStatus", "render": {"type": "dictTag", "dictType": "order_status"}}, {"key": "orderType", "title": "订单类型", "dataIndex": "orderType", "render": {"type": "dictTag", "dictType": "order_type"}}, {"key": "payTime", "title": "支付时间", "dataIndex": "payTime"}, {"key": "createTime", "title": "创建时间", "dataIndex": "createTime"}, {"key": "actions", "title": "操作", "dataIndex": "actions", "width": 180, "fixed": "right"}]

[STAGE:editSchema]
[{"field": "orderNo", "label": "订单编号", "type": "input", "required": true}, {"field": "customerName", "label": "客户姓名", "type": "input", "required": true}, {"field": "customerPhone", "label": "客户电话", "type": "input", "required": true}, {"field": "orderAmount", "label": "订单金额", "type": "number", "required": true}, {"field": "orderStatus", "label": "订单状态", "type": "select", "required": true, "dictType": "order_status"}, {"field": "orderType", "label": "订单类型", "type": "select", "required": true, "dictType": "order_type"}, {"field": "payTime", "label": "支付时间", "type": "datetime", "required": false}, {"field": "remark", "label": "备注", "type": "textarea", "required": false}]

[STAGE:apiConfig]
{"list": "GET@/order/manage/list", "detail": "GET@/order/manage/detail", "create": "POST@/order/manage/create", "update": "PUT@/order/manage/update", "delete": "DELETE@/order/manage/delete"}

[STAGE:dictConfig]
[{"dictType": "order_status", "dictName": "订单状态", "isNew": true, "items": [{"dictLabel": "待付款", "dictValue": "1", "dictSort": 1}, {"dictLabel": "已付款", "dictValue": "2", "dictSort": 2}, {"dictLabel": "已发货", "dictValue": "3", "dictSort": 3}, {"dictLabel": "已完成", "dictValue": "4", "dictSort": 4}, {"dictLabel": "已取消", "dictValue": "5", "dictSort": 5}]}, {"dictType": "order_type", "dictName": "订单类型", "isNew": true, "items": [{"dictLabel": "普通订单", "dictValue": "1", "dictSort": 1}, {"dictLabel": "团购订单", "dictValue": "2", "dictSort": 2}, {"dictLabel": "秒杀订单", "dictValue": "3", "dictSort": 3}]}]

[STAGE:desensitizeConfig]
{"customerPhone": {"type": "PHONE"}}

[STAGE:encryptConfig]
{"enableEncrypt": false, "enableDecrypt": false, "operations": []}

[STAGE:transConfig]
{"orderStatus": {"dictType": "order_status", "targetField": "orderStatusName"}, "orderType": {"dictType": "order_type", "targetField": "orderTypeName"}}

[STAGE:createTableSql]
CREATE TABLE `order_manage` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT ''主键ID'',
  `tenant_id` bigint(20) DEFAULT NULL COMMENT ''租户ID'',
  `order_no` varchar(64) NOT NULL COMMENT ''订单编号'',
  `customer_name` varchar(100) DEFAULT NULL COMMENT ''客户姓名'',
  `customer_phone` varchar(20) DEFAULT NULL COMMENT ''客户电话'',
  `order_amount` decimal(10,2) DEFAULT NULL COMMENT ''订单金额'',
  `order_status` tinyint(1) DEFAULT ''1'' COMMENT ''订单状态(1:待付款,2:已付款,3:已发货,4:已完成,5:已取消)'',
  `order_type` tinyint(1) DEFAULT ''1'' COMMENT ''订单类型(1:普通订单,2:团购订单,3:秒杀订单)'',
  `pay_time` datetime DEFAULT NULL COMMENT ''支付时间'',
  `remark` varchar(500) DEFAULT NULL COMMENT ''备注'',
  `create_by` varchar(64) DEFAULT NULL COMMENT ''创建人'',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT ''创建时间'',
  `create_dept` bigint(20) DEFAULT NULL COMMENT ''创建部门'',
  `update_by` varchar(64) DEFAULT NULL COMMENT ''更新人'',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT ''更新时间'',
  `del_flag` tinyint(1) DEFAULT ''0'' COMMENT ''删除标志(0:正常,1:删除)'',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT=''订单管理表'';', NULL, '2026-04-22 13:02:00'),
       (2048333171038535682, 1, 1, 'crud_config_builder', 'session_1777195540937_lgxbcl431', 'user',
        '员工管理，包含姓名、工号、部门、职位、入职日期、手机号、邮箱、状态', NULL, '2026-04-26 17:27:39'),
       (2048333171357302785, 1, 1, 'crud_config_builder', 'session_1777195540937_lgxbcl431', 'assistant', '[STAGE:meta]
{"configKey": "employee", "tableName": "sys_employee", "tableComment": "员工信息表"}

[STAGE:searchSchema]
[{"field": "empName", "label": "姓名", "type": "input"}, {"field": "empNo", "label": "工号", "type": "input"}, {"field": "deptId", "label": "部门", "type": "select", "dictType": "sys_dept"}, {"field": "status", "label": "状态", "type": "select", "dictType": "sys_normal_disable"}]

[STAGE:columnsSchema]
[{"key": "empName", "title": "姓名", "dataIndex": "empName"}, {"key": "empNo", "title": "工号", "dataIndex": "empNo"}, {"key": "deptName", "title": "部门", "dataIndex": "deptName", "render": {"type": "dictTag", "dictType": "sys_dept"}}, {"key": "position", "title": "职位", "dataIndex": "position"}, {"key": "hireDate", "title": "入职日期", "dataIndex": "hireDate"}, {"key": "phone", "title": "手机号", "dataIndex": "phone"}, {"key": "email", "title": "邮箱", "dataIndex": "email"}, {"key": "status", "title": "状态", "dataIndex": "status", "render": {"type": "dictTag", "dictType": "sys_normal_disable"}}, {"key": "actions", "title": "操作", "dataIndex": "actions"}]

[STAGE:editSchema]
[{"field": "empName", "label": "姓名", "type": "input", "required": true}, {"field": "empNo", "label": "工号", "type": "input", "required": true}, {"field": "deptId", "label": "部门", "type": "select", "dictType": "sys_dept", "required": true}, {"field": "position", "label": "职位", "type": "input", "required": true}, {"field": "hireDate", "label": "入职日期", "type": "date", "required": true}, {"field": "phone", "label": "手机号", "type": "input", "required": true, "props": {"rules": [{"pattern": "^1[3-9]\\\\d{9}$", "message": "请输入正确的手机号"}]}}, {"field": "email", "label": "邮箱", "type": "input", "required": true, "props": {"rules": [{"type": "email", "message": "请输入正确的邮箱"}]}}, {"field": "status", "label": "状态", "type": "select", "dictType": "sys_normal_disable", "required": true}]

[STAGE:apiConfig]
{"list": "get@/ai/crud/employee/page", "detail": "get@/ai/crud/employee/:id", "create": "post@/ai/crud/employee", "update": "put@/ai/crud/employee", "delete": "delete@/ai/crud/employee/:id"}

[STAGE:dictConfig]
[{"dictType": "sys_dept", "dictName": "部门", "isNew": false, "items": [{"dictLabel": "研发部", "dictValue": "1", "dictSort": 1}, {"dictLabel": "市场部", "dictValue": "2", "dictSort": 2}, {"dictLabel": "人事部", "dictValue": "3", "dictSort": 3}]}, {"dictType": "sys_normal_disable", "dictName": "通用状态", "isNew": false, "items": [{"dictLabel": "正常", "dictValue": "1", "dictSort": 1}, {"dictLabel": "停用", "dictValue": "0", "dictSort": 2}]}]

[STAGE:desensitizeConfig]
{"phone": {"type": "PHONE"}, "email": {"type": "EMAIL"}}

[STAGE:encryptConfig]
{"enableEncrypt": false, "enableDecrypt": false, "operations": []}

[STAGE:transConfig]
{"deptId": {"dictType": "sys_dept", "targetField": "deptName"}, "status": {"dictType": "sys_normal_disable", "targetField": "statusName"}}

[STAGE:createTableSql]
CREATE TABLE `sys_employee` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT ''主键ID'',
  `emp_name` varchar(100) NOT NULL COMMENT ''姓名'',
  `emp_no` varchar(50) NOT NULL COMMENT ''工号'',
  `dept_id` bigint(20) DEFAULT NULL COMMENT ''部门ID'',
  `position` varchar(100) DEFAULT NULL COMMENT ''职位'',
  `hire_date` date DEFAULT NULL COMMENT ''入职日期'',
  `phone` varchar(20) DEFAULT NULL COMMENT ''手机号'',
  `email` varchar(100) DEFAULT NULL COMMENT ''邮箱'',
  `status` tinyint(1) DEFAULT ''1'' COMMENT ''状态(1:正常,0:停用)'',
  `tenant_id` bigint(20) DEFAULT NULL COMMENT ''租户ID'',
  `create_by` varchar(64) DEFAULT NULL COMMENT ''创建者'',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT ''创建时间'',
  `create_dept` bigint(20) DEFAULT NULL COMMENT ''创建部门'',
  `update_by` varchar(64) DEFAULT NULL COMMENT ''更新者'',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT ''更新时间'',
  `del_flag` tinyint(1) DEFAULT ''0'' COMMENT ''删除标志(0:未删除,1:已删除)'',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_emp_no` (`emp_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT=''员工信息表'';', NULL, '2026-04-26 17:27:39'),
       (2048343139166883842, 1, 1, 'crud_config_builder', 'session_1777197900986_a4h0w6t4u', 'user',
        '产品管理，包含产品名称、分类、价格、库存、状态、创建时间', NULL, '2026-04-26 18:07:15');
INSERT INTO ai_chat_record (id, tenant_id, user_id, agent_code, session_id, `role`, content, token_usage, create_time)
VALUES (2048343139556954114, 1, 1, 'crud_config_builder', 'session_1777197900986_a4h0w6t4u', 'assistant', '[STAGE:meta]
{"configKey": "biz_product", "tableName": "biz_product", "tableComment": "产品管理表", "mode": "modal"}

[STAGE:searchSchema]
[{"field": "productName", "label": "产品名称", "type": "input"}, {"field": "category", "label": "分类", "type": "select", "dictType": "product_category"}, {"field": "status", "label": "状态", "type": "select", "dictType": "common_status"}, {"field": "createTime", "label": "创建时间", "type": "daterange"}]

[STAGE:columnsSchema]
[{"key": "productName", "title": "产品名称", "dataIndex": "productName"}, {"key": "category", "title": "分类", "dataIndex": "category", "render": {"type": "dictTag", "dictType": "product_category"}}, {"key": "price", "title": "价格", "dataIndex": "price"}, {"key": "stock", "title": "库存", "dataIndex": "stock"}, {"key": "status", "title": "状态", "dataIndex": "status", "render": {"type": "dictTag", "dictType": "common_status"}}, {"key": "parentName", "title": "父级名称", "dataIndex": "parentName"}, {"key": "createTime", "title": "创建时间", "dataIndex": "createTime"}, {"key": "actions", "title": "操作", "dataIndex": "actions"}]

[STAGE:editSchema]
[{"field": "productName", "label": "产品名称", "type": "input", "required": true}, {"field": "parentId", "label": "上级产品", "type": "treeSelect", "required": false}, {"field": "category", "label": "分类", "type": "select", "required": true, "dictType": "product_category"}, {"field": "price", "label": "价格", "type": "number", "required": true}, {"field": "stock", "label": "库存", "type": "number", "required": true}, {"field": "status", "label": "状态", "type": "select", "required": true, "dictType": "common_status"}, {"field": "sort", "label": "排序", "type": "number", "required": false}]

[STAGE:apiConfig]
{"list": "get@/ai/crud/biz_product/page", "detail": "get@/ai/crud/biz_product/:id", "create": "post@/ai/crud/biz_product", "update": "put@/ai/crud/biz_product", "delete": "delete@/ai/crud/biz_product/:id", "tree": "get@/ai/crud/biz_product/tree", "treeConfig": {"keyField": "id", "parentField": "parentId", "labelField": "productName"}}

[STAGE:dictConfig]
[{"dictType": "product_category", "dictName": "产品分类", "isNew": true, "items": [{"dictLabel": "电子产品", "dictValue": "1", "dictSort": 1}, {"dictLabel": "服装鞋帽", "dictValue": "2", "dictSort": 2}, {"dictLabel": "家居日用", "dictValue": "3", "dictSort": 3}]}, {"dictType": "common_status", "dictName": "通用状态", "isNew": false, "items": [{"dictLabel": "正常", "dictValue": "1", "dictSort": 1}, {"dictLabel": "停用", "dictValue": "0", "dictSort": 2}]}]

[STAGE:desensitizeConfig]
{}

[STAGE:encryptConfig]
{"enableEncrypt": false, "enableDecrypt": false, "operations": []}

[STAGE:transConfig]
{"status": {"dictType": "common_status", "targetField": "statusName"}, "category": {"dictType": "product_category", "targetField": "categoryName"}}

[STAGE:createTableSql]
CREATE TABLE `biz_product` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT ''主键ID'',
  `tenant_id` bigint(20) DEFAULT NULL COMMENT ''租户ID'',
  `parent_id` bigint(20) DEFAULT 0 COMMENT ''父级ID'',
  `product_name` varchar(255) NOT NULL COMMENT ''产品名称'',
  `category` varchar(50) DEFAULT NULL COMMENT ''分类'',
  `price` decimal(10,2) DEFAULT NULL COMMENT ''价格'',
  `stock` int(11) DEFAULT 0 COMMENT ''库存'',
  `status` tinyint(1) DEFAULT 1 COMMENT ''状态'',
  `sort` int(11) DEFAULT 0 COMMENT ''排序'',
  `create_by` varchar(64) DEFAULT '''' COMMENT ''创建者'',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT ''创建时间'',
  `create_dept` bigint(20) DEFAULT NULL COMMENT ''创建部门'',
  `update_by` varchar(64) DEFAULT '''' COMMENT ''更新者'',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT ''更新时间'',
  `del_flag` tinyint(1) DEFAULT 0 COMMENT ''删除标志'',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT=''产品管理表'';', NULL, '2026-04-26 18:07:15'),
       (2048760453655465985, 1, 1, 'crud_config_builder', 'session_1777297448667_wcykofoap', 'user', '订单管理，包含订单号、客户名称、订单金额、支付状态、下单时间、备注
', NULL, '2026-04-27 21:45:31'),
       (2048760453923901441, 1, 1, 'crud_config_builder', 'session_1777297448667_wcykofoap', 'assistant', '[STAGE:meta]
{"configKey": "biz_order", "tableName": "biz_order", "tableComment": "订单管理"}

[STAGE:searchSchema]
[
  {"field": "orderNo", "label": "订单号", "type": "input"},
  {"field": "customerName", "label": "客户名称", "type": "input"},
  {"field": "payStatus", "label": "支付状态", "type": "select", "dictType": "pay_status"},
  {"field": "orderTime", "label": "下单时间", "type": "daterange"}
]

[STAGE:columnsSchema]
[
  {"key": "orderNo", "title": "订单号", "dataIndex": "orderNo"},
  {"key": "customerName", "title": "客户名称", "dataIndex": "customerName"},
  {"key": "orderAmount", "title": "订单金额", "dataIndex": "orderAmount"},
  {"key": "payStatus", "title": "支付状态", "dataIndex": "payStatus", "render": {"type": "dictTag", "dictType": "pay_status"}},
  {"key": "orderTime", "title": "下单时间", "dataIndex": "orderTime"},
  {"key": "remark", "title": "备注", "dataIndex": "remark"},
  {"key": "actions", "title": "操作", "dataIndex": "actions"}
]

[STAGE:editSchema]
[
  {"field": "orderNo", "label": "订单号", "type": "input", "required": true},
  {"field": "customerName", "label": "客户名称", "type": "input", "required": true},
  {"field": "orderAmount", "label": "订单金额", "type": "number", "required": true, "props": {"precision": 2, "min": 0}},
  {"field": "payStatus", "label": "支付状态", "type": "select", "required": true, "dictType": "pay_status"},
  {"field": "orderTime", "label": "下单时间", "type": "datetime", "required": true},
  {"field": "remark", "label": "备注", "type": "textarea", "required": false}
]

[STAGE:apiConfig]
{
  "list": "get@/ai/crud/biz_order/page",
  "detail": "get@/ai/crud/biz_order/:id",
  "create": "post@/ai/crud/biz_order",
  "update": "put@/ai/crud/biz_order",
  "delete": "delete@/ai/crud/biz_order/:id"
}

[STAGE:dictConfig]
[
  {
    "dictType": "pay_status",
    "dictName": "支付状态",
    "isNew": true,
    "items": [
      {"dictLabel": "未支付", "dictValue": "0", "dictSort": 0},
      {"dictLabel": "已支付", "dictValue": "1", "dictSort": 1},
      {"dictLabel": "支付失败", "dictValue": "2", "dictSort": 2}
    ]
  }
]

[STAGE:desensitizeConfig]
{}

[STAGE:encryptConfig]
{
  "enableEncrypt": false,
  "enableDecrypt": false,
  "operations": []
}

[STAGE:transConfig]
{
  "payStatus": {
    "dictType": "pay_status",
    "targetField": "payStatusName"
  }
}

[STAGE:createTableSql]
CREATE TABLE `biz_order` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT ''主键'',
  `tenant_id` bigint DEFAULT NULL COMMENT ''租户ID'',
  `create_by` bigint DEFAULT NULL COMMENT ''创建人'',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT ''创建时间'',
  `create_dept` bigint DEFAULT NULL COMMENT ''创建部门'',
  `update_by` bigint DEFAULT NULL COMMENT ''更新人'',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT ''更新时间'',
  `del_flag` tinyint(1) DEFAULT ''0'' COMMENT ''删除标志（0-正常 1-删除）'',
  `order_no` varchar(64) NOT NULL COMMENT ''订单号'',
  `customer_name` varchar(100) NOT NULL COMMENT ''客户名称'',
  `order_amount` decimal(12,2) NOT NULL COMMENT ''订单金额'',
  `pay_status` varchar(10) NOT NULL COMMENT ''支付状态（0-未支付 1-已支付 2-支付失败）'',
  `order_time` datetime NOT NULL COMMENT ''下单时间'',
  `remark` varchar(500) DEFAULT NULL COMMENT ''备注'',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT=''订单管理'';', NULL, '2026-04-27 21:45:31'),
       (2049837082217627650, 1, 1, 'crud_config_builder', 'session_1777554136235_3slajqloa', 'user',
        '产品管理，包含产品名称、分类、价格、库存、状态、创建时间', NULL, '2026-04-30 21:03:39'),
       (2049837082456702977, 1, 1, 'crud_config_builder', 'session_1777554136235_3slajqloa', 'assistant', '[STAGE:meta]
{"configKey": "biz_product", "tableName": "biz_product", "tableComment": "产品管理"}

[STAGE:searchSchema]
[{"field": "productName", "label": "产品名称", "type": "input"}, {"field": "category", "label": "分类", "type": "select", "dictType": "product_category"}, {"field": "status", "label": "状态", "type": "select", "dictType": "product_status"}, {"field": "createTime", "label": "创建时间", "type": "daterange"}]

[STAGE:columnsSchema]
[{"key": "productName", "title": "产品名称", "dataIndex": "productName"}, {"key": "category", "title": "分类", "dataIndex": "category", "render": {"type": "dictTag", "dictType": "product_category"}}, {"key": "price", "title": "价格", "dataIndex": "price"}, {"key": "stock", "title": "库存", "dataIndex": "stock"}, {"key": "status", "title": "状态", "dataIndex": "status", "render": {"type": "dictTag", "dictType": "product_status"}}, {"key": "sort", "title": "排序", "dataIndex": "sort"}, {"key": "createTime", "title": "创建时间", "dataIndex": "createTime"}, {"key": "actions", "title": "操作", "dataIndex": "actions"}]

[STAGE:editSchema]
[{"field": "productName", "label": "产品名称", "type": "input", "required": true}, {"field": "parentId", "label": "父级ID", "type": "number", "required": false}, {"field": "category", "label": "分类", "type": "select", "dictType": "product_category", "required": true}, {"field": "price", "label": "价格", "type": "number", "required": true, "props": {"precision": 2}}, {"field": "stock", "label": "库存", "type": "number", "required": true}, {"field": "status", "label": "状态", "type": "radio", "dictType": "product_status", "required": true}, {"field": "sort", "label": "排序", "type": "number", "required": false}]

[STAGE:apiConfig]
{"list": "get@/ai/crud/biz_product/page", "detail": "get@/ai/crud/biz_product/:id", "create": "post@/ai/crud/biz_product", "update": "put@/ai/crud/biz_product", "delete": "delete@/ai/crud/biz_product/:id"}

[STAGE:dictConfig]
[{"dictType": "product_category", "dictName": "产品分类", "isNew": true, "items": [{"dictLabel": "电子产品", "dictValue": "electronics", "dictSort": 1}, {"dictLabel": "服装鞋帽", "dictValue": "clothing", "dictSort": 2}, {"dictLabel": "家居日用", "dictValue": "home", "dictSort": 3}]}, {"dictType": "product_status", "dictName": "产品状态", "isNew": true, "items": [{"dictLabel": "上架", "dictValue": "1", "dictSort": 1}, {"dictLabel": "下架", "dictValue": "0", "dictSort": 2}]}]

[STAGE:desensitizeConfig]
{}

[STAGE:encryptConfig]
{"enableEncrypt": false, "enableDecrypt": false, "operations": []}

[STAGE:transConfig]
{"category": {"dictType": "product_category", "targetField": "categoryName"}, "status": {"dictType": "product_status", "targetField": "statusName"}}

[STAGE:createTableSql]
CREATE TABLE `biz_product` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT ''主键ID'',
  `tenant_id` bigint DEFAULT NULL COMMENT ''租户ID'',
  `parent_id` bigint DEFAULT NULL COMMENT ''父级ID'',
  `product_name` varchar(255) NOT NULL COMMENT ''产品名称'',
  `category` varchar(50) DEFAULT NULL COMMENT ''分类'',
  `price` decimal(10,2) DEFAULT NULL COMMENT ''价格'',
  `stock` int DEFAULT NULL COMMENT ''库存'',
  `status` tinyint(1) DEFAULT ''1'' COMMENT ''状态'',
  `sort` int DEFAULT ''0'' COMMENT ''排序'',
  `create_by` varchar(64) DEFAULT NULL COMMENT ''创建者'',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT ''创建时间'',
  `create_dept` bigint DEFAULT NULL COMMENT ''创建部门'',
  `update_by` varchar(64) DEFAULT NULL COMMENT ''更新者'',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT ''更新时间'',
  `del_flag` tinyint(1) DEFAULT ''0'' COMMENT ''删除标志'',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT=''产品管理'';', NULL, '2026-04-30 21:03:39'),
       (2049838218844643330, 1, 1, 'crud_config_builder', 'session_1777554136235_3slajqloa', 'user',
        '产品管理，包含产品名称、分类、价格、库存、状态、创建时间', NULL, '2026-04-30 21:08:10'),
       (2049838219175993346, 1, 1, 'crud_config_builder', 'session_1777554136235_3slajqloa', 'assistant', '[STAGE:meta]
{"configKey": "biz_product", "tableName": "biz_product", "tableComment": "产品管理"}

[STAGE:searchSchema]
[{"field": "productName", "label": "产品名称", "type": "input"}, {"field": "category", "label": "分类", "type": "select", "dictType": "biz_product_category"}, {"field": "status", "label": "状态", "type": "select", "dictType": "biz_product_status"}, {"field": "createTime", "label": "创建时间", "type": "daterange"}]

[STAGE:columnsSchema]
[{"key": "productName", "title": "产品名称", "dataIndex": "productName"}, {"key": "category", "title": "分类", "dataIndex": "category", "render": {"type": "dictTag", "dictType": "biz_product_category"}}, {"key": "price", "title": "价格", "dataIndex": "price"}, {"key": "stock", "title": "库存", "dataIndex": "stock"}, {"key": "status", "title": "状态", "dataIndex": "status", "render": {"type": "dictTag", "dictType": "biz_product_status"}}, {"key": "sort", "title": "排序", "dataIndex": "sort"}, {"key": "createTime", "title": "创建时间", "dataIndex": "createTime"}, {"key": "actions", "title": "操作", "dataIndex": "actions"}]

[STAGE:editSchema]
[{"field": "parentId", "label": "父级ID", "type": "number", "required": false}, {"field": "productName", "label": "产品名称", "type": "input", "required": true}, {"field": "category", "label": "分类", "type": "select", "dictType": "biz_product_category", "required": true}, {"field": "price", "label": "价格", "type": "number", "required": true, "props": {"pattern": "^\\\\d+(\\\\.\\\\d{1,2})?$"}}, {"field": "stock", "label": "库存", "type": "number", "required": true}, {"field": "status", "label": "状态", "type": "radio", "dictType": "biz_product_status", "required": true}, {"field": "sort", "label": "排序", "type": "number", "required": false}]

[STAGE:apiConfig]
{"list": "get@/ai/crud/biz_product/page", "detail": "get@/ai/crud/biz_product/:id", "create": "post@/ai/crud/biz_product", "update": "put@/ai/crud/biz_product", "delete": "delete@/ai/crud/biz_product/:id"}

[STAGE:dictConfig]
[{"dictType": "biz_product_category", "dictName": "产品分类", "isNew": true, "items": [{"dictLabel": "电子产品", "dictValue": "1", "dictSort": 1}, {"dictLabel": "服装鞋帽", "dictValue": "2", "dictSort": 2}, {"dictLabel": "日用百货", "dictValue": "3", "dictSort": 3}]}, {"dictType": "biz_product_status", "dictName": "产品状态", "isNew": true, "items": [{"dictLabel": "上架", "dictValue": "1", "dictSort": 1}, {"dictLabel": "下架", "dictValue": "0", "dictSort": 2}]}]

[STAGE:desensitizeConfig]
{}

[STAGE:encryptConfig]
{"enableEncrypt": false, "enableDecrypt": false, "operations": []}

[STAGE:transConfig]
{"category": {"dictType": "biz_product_category", "targetField": "categoryName"}, "status": {"dictType": "biz_product_status", "targetField": "statusName"}}

[STAGE:createTableSql]
CREATE TABLE `biz_product` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT ''主键ID'',
  `tenant_id` bigint DEFAULT NULL COMMENT ''租户ID'',
  `parent_id` bigint DEFAULT NULL COMMENT ''父级ID'',
  `product_name` varchar(255) NOT NULL COMMENT ''产品名称'',
  `category` varchar(50) DEFAULT NULL COMMENT ''分类'',
  `price` decimal(10,2) DEFAULT NULL COMMENT ''价格'',
  `stock` int DEFAULT NULL COMMENT ''库存'',
  `status` tinyint(1) DEFAULT ''1'' COMMENT ''状态'',
  `sort` int DEFAULT ''0'' COMMENT ''排序'',
  `create_by` varchar(64) DEFAULT NULL COMMENT ''创建者'',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT ''创建时间'',
  `create_dept` bigint DEFAULT NULL COMMENT ''创建部门'',
  `update_by` varchar(64) DEFAULT NULL COMMENT ''更新者'',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT ''更新时间'',
  `del_flag` tinyint(1) DEFAULT ''0'' COMMENT ''删除标志'',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT=''产品管理'';', NULL, '2026-04-30 21:08:10'),
       (2049839649567813633, 1, 1, 'crud_config_builder', 'session_1777554136235_3slajqloa', 'user',
        '搜索列表增加产品ID', NULL, '2026-04-30 21:13:51'),
       (2049839650184376321, 1, 1, 'crud_config_builder', 'session_1777554136235_3slajqloa', 'assistant', '【思考过程】
Here''s a thinking process:

1.  **Analyze User Input:**
    - **configKey:** `biz_product`
    - **tableName:** `biz_product`
    - **Requirement:** 搜索列表增加产品ID (Add product ID to the search list)
    - **Table Structure:**
        - `id` (bigint, PK)
        - `tenant_id` (bigint) - `parent_id` (bigint)
        - `product_name` (varchar(255), required)
        - `category` (varchar(50))
        - `price` (decimal(10,2)) - `stock` (int)
        - `status` (tinyint(1))
        - `sort` (int)
        - Base fields: `create_by`, `create_time`, `create_dept`, `update_by`, `update_time`, `del_flag`
    - **Template Constraints:** Standard CRUD, columns include content + actions, editSchema simple, modalType drawer, 3-5 search fields.

2.  **Map Fields to Config Components:**
    - **searchSchema:** Need 3-5 fields. User explicitly requested "产品ID" (product ID). So `id` should be in search. Others: `product_name` (input), `category` (select/input), `status` (select). Let''s pick: `id` (number/input), `product_name` (input), `category` (input/select), `status` (select). I''ll stick to 4.
    - **columnsSchema:** All relevant fields except base fields. `id`, `productName`, `parentId`, `category`, `price`, `stock`, `status`, `sort`, `createTime`. Plus `actions` at the end. `status` needs dictTag.
    - **editSchema:** Exclude base fields (`id`, `tenantId`, `createBy`, `createTime`, `createDept`, `updateBy`, `updateTime`, `delFlag`). Include: `parentId`, `productName` (required), `category`, `price`, `stock`, `status`, `sort`. Map types: `parentId` (number/treeSelect? I''ll use number or input), `productName` (input), `category` (input/select), `price` (number), `stock` (number), `status` (radio/select), `sort` (number). I''ll use `input` for name/category, `number` for price/stock/sort/parentId, `select` for status.
    - **apiConfig:** Standard CRUD paths based on `configKey`.
    - **dictConfig:** `status` field implies a dictionary. I''ll create `biz_product_status` with items like 启用/停用 (1/0).
    - **desensitizeConfig:** No obvious sensitive fields like phone/email. Maybe skip or leave empty `{}`. - **encryptConfig:** Default `{ "enableEncrypt": false, "enableDecrypt": false, "operations": [] }`.
    - **transConfig:** `status` -> `statusName`.
    - **createTableSql:** Generate MySQL DDL with all fields, base fields included, utf8mb4, InnoDB.

3.  **Draft Generation (Mental Refinement):**

    *meta:*
    `{"configKey": "biz_product", "tableName": "biz_product", "tableComment": "产品信息表"}`

    *searchSchema:*
    Need `id` as requested. `product_name`, `category`, `status`.
    ```json
    [
      {"field": "id", "label": "产品ID", "type": "number"},
      {"field": "productName", "label": "产品名称", "type": "input"},
      {"field": "category", "label": "分类", "type": "input"},
      {"field": "status", "label": "状态", "type": "select", "dictType": "biz_product_status"}
    ]
    ```

    *columnsSchema:*
    ```json
    [
      {"key": "id", "title": "产品ID", "dataIndex": "id"},
      {"key": "productName", "title": "产品名称", "dataIndex": "productName"},
      {"key": "parentId", "title": "父级ID", "dataIndex": "parentId"},
      {"key": "category", "title": "分类", "dataIndex": "category"},
      {"key": "price", "title": "价格", "dataIndex": "price"},
      {"key": "stock", "title": "库存", "dataIndex": "stock"},
      {"key": "status", "title": "状态", "dataIndex": "status", "render": {"type": "dictTag", "dictType": "biz_product_status"}},
      {"key": "sort", "title": "排序", "dataIndex": "sort"},
      {"key": "createTime", "title": "创建时间", "dataIndex": "createTime"},
      {"key": "actions", "title": "操作", "dataIndex": "actions"}
    ]
    ```

    *editSchema:*
    Exclude base fields. `id` is usually auto-generated, so not in edit. `parentId`, `productName` (required), `category`, `price`, `stock`, `status` (required), `sort`.
    ```json
    [
      {"field": "parentId", "label": "父级ID", "type": "number", "required": false},
      {"field": "productName", "label": "产品名称", "type": "input", "required": true},
      {"field": "category", "label": "分类", "type": "input", "required": false},
      {"field": "price", "label": "价格", "type": "number", "required": false, "props": {"precision": 2}},
      {"field": "stock", "label": "库存", "type": "number", "required": false},
      {"field": "status", "label": "状态", "type": "radio", "required": true, "dictType": "biz_product_status"},
      {"field": "sort", "label": "排序", "type": "number", "required": false}
    ]
    ```

    *apiConfig:*
    ```json
    {
      "list": "get@/ai/crud/biz_product/page",
      "detail": "get@/ai/crud/biz_product/:id",
      "create": "post@/ai/crud/biz_product",
      "update": "put@/ai/crud/biz_product",
      "delete": "delete@/ai/crud/biz_product/:id"
    }
    ```

    *dictConfig:*
    ```json
    [
      {
        "dictType": "biz_product_status",
        "dictName": "产品状态",
        "isNew": true,
        "items": [
          {"dictLabel": "启用", "dictValue": "1", "dictSort": 1},
          {"dictLabel": "停用", "dictValue": "0", "dictSort": 2}
        ]
      }
    ]
    ```

    *desensitizeConfig:* `{}` (No sensitive fields)    *encryptConfig:* `{"enableEncrypt": false, "enableDecrypt": false, "operations": []}`

    *transConfig:*
    ```json
    {
      "status": { "dictType": "biz_product_status",
        "targetField": "statusName"
      }
    }
    ```

    *createTableSql:*
    Need to construct standard MySQL DDL.
    ```sql
    CREATE TABLE `biz_product` (
      `id` bigint NOT NULL AUTO_INCREMENT COMMENT ''主键ID'',
      `tenant_id` bigint DEFAULT NULL COMMENT ''租户ID'',
      `parent_id` bigint DEFAULT NULL COMMENT ''父级ID'',
      `product_name` varchar(255) NOT NULL COMMENT ''产品名称'',
      `category` varchar(50) DEFAULT NULL COMMENT ''分类'',
      `price` decimal(10,2) DEFAULT NULL COMMENT ''价格'',
      `stock` int DEFAULT NULL COMMENT ''库存'',
      `status` tinyint(1) DEFAULT ''1'' COMMENT ''状态'',
      `sort` int DEFAULT ''0'' COMMENT ''排序'',
      `create_by` varchar(64) DEFAULT NULL COMMENT ''创建者'',
      `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT ''创建时间'',
      `create_dept` bigint DEFAULT NULL COMMENT ''创建部门'',
      `update_by` varchar(64) DEFAULT NULL COMMENT ''更新者'',
      `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT ''更新时间'',
      `del_flag` tinyint(1) DEFAULT ''0'' COMMENT ''删除标志'',
      PRIMARY KEY (`id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT=''产品信息表'';
    ```

4.  **Check Constraints & Refine:**
    - `configKey`: `biz_product` (matches)
    - `tableName`: `biz_product` (matches)
    - `searchSchema`: 4 fields, includes `id` as requested. Types correct. `status` has `dictType`.
    - `columnsSchema`: Includes all relevant, `status` has `render`, ends with `actions`.
    - `editSchema`: Base fields excluded. Types match spec. `status` uses `radio` (spec says boolean/enum -> radio/select, I''ll stick to radio or select. I''ll use `select` to be safe, or `radio` as per spec "布尔选择 → RADIO". Status is tinyint(1), so radio is fine. I''ll use `select` for broader compatibility, but spec says "状态/类型等有限枚举 → SELECT". I''ll change to `select` for status in editSchema to align with spec preference, or keep `radio`. Actually, spec says "状态/类型等有限枚举 → SELECT". I''ll use `select`.)
    - `apiConfig`: Matches format exactly.
    - `dictConfig`: Extracted from schema.
    - `desensitizeConfig`: `{}`
    - `encryptConfig`: Matches format.
    - `transConfig`: Matches format.
    - `createTableSql`: Includes all base fields, correct types, utf8mb4, InnoDB.
    - Format: Strictly `[STAGE:xxx]\\n{content}`. No markdown code blocks.
 - Check field names: camelCase in JSON, snake_case in SQL.
    - `searchSchema` field names: `id`, `productName`, `category`, `status`.
    - `columnsSchema` keys: `id`, `productName`, `parentId`, `category`, `price`, `stock`, `status`, `sort`, `createTime`, `actions`.
    - `editSchema` fields: `parentId`, `productName`, `category`, `price`, `stock`, `status`, `sort`.
    - All good.

    One minor thing: `price` type in editSchema should probably be `number` with props for precision. The spec says `number` is valid. I''ll add `props: {"precision": 2}`.
    `status` in editSchema: `type: "select"` is safer. I''ll use `select`.
    `searchSchema` `status`: `type: "select"`, `dictType: "biz_product_status"`.
    Check spec: "有dictType的select字段必须加上dictType属性". Done.
    Check spec: "基类字段(id/tenant_id/create_by/create_time/create_dept/update_by/update_time/del_flag)不进editSchema". Done.
    Check spec: "最后一列为操作列(key=actions)". Done.
    Check spec: "apiConfig format". Done.
    Check spec: "输出格式要求". Strictly follow.

    Let''s assemble carefully. Ensure no markdown formatting around the JSON/SQL. Just raw text as requested.
    I will double-check the exact output format requested:
    [STAGE:meta]
    {...}
    [STAGE:searchSchema]
    [...]
    etc.

    All set. Generating.
    Self-Correction/Verification during thought:
    - `price` in table is `decimal(10,2)`. In editSchema, type `number` is fine. I''ll add `props: {"step": "0.01", "precision": 2}`.
    - `status` is `tinyint(1)`. In Java it maps to Boolean, but here it''s used as status (1:正常,0:停用). I''ll treat it as enum.
    - `searchSchema` limit: 3-5 fields. I have 4. Good.
    - `columnsSchema` render for dict: `{"type": "dictTag", "dictType": "biz_product_status"}`. Good.
    - `apiConfig` keys: list, detail, create, update, delete. Good.
    - `dictConfig` items: dictLabel, dictValue, dictSort. Good.
    - `transConfig`: key is field name, value has dictType, targetField. Good.
    - `createTableSql`: Must include base fields. I''ll make sure `tenant_id`, `create_by`, etc. are there.
    - Output must be strictly JSON/SQL per stage, no markdown.

    One detail: `searchSchema` `id` field type. The spec says `number` is allowed. I''ll use `number`.
    `editSchema` `parentId` type: `number` or `treeSelect`. I''ll use `number` for simplicity as per table.
    Everything aligns. Proceed.
    Output matches exactly.
    Generating response.
    *(Self-Correction/Refinement during drafting)*:
    I need to make sure the JSON is valid and strictly follows the requested structure. I will format it carefully.    `apiConfig` format in prompt: `list"："get@/ai/crud/" + configKey + "/page"` -> I will output exactly `{"list": "get@/ai/crud/biz_product/page", ...}`
    `desensitizeConfig` and `encryptConfig` and `transConfig` should be JSON objects. I will produce the exact text.
    Ready.
    Output Generation.
    [STAGE:meta]
    {"configKey":"biz_product","tableName":"biz_product","tableComment":"产品信息表"}
    [STAGE:searchSchema]
    [{"field":"id","label":"产品ID","type":"number"},{"field":"productName","label":"产品名称","type":"input"},{"field":"category","label":"分类","type":"input"},{"field":"status","label":"状态","type":"select","dictType":"biz_product_status"}]
    [STAGE:columnsSchema]
    [{"key":"id","title":"产品ID","dataIndex":"id"},{"key":"productName","title":"产品名称","dataIndex":"productName"},{"key":"parentId","title":"父级ID","dataIndex":"parentId"},{"key":"category","title":"分类","dataIndex":"category"},{"key":"price","title":"价格","dataIndex":"price"},{"key":"stock","title":"库存","dataIndex":"stock"},{"key":"status","title":"状态","dataIndex":"status","render":{"type":"dictTag","dictType":"biz_product_status"}},{"key":"sort","title":"排序","dataIndex":"sort"},{"key":"createTime","title":"创建时间","dataIndex":"createTime"},{"key":"actions","title":"操作","dataIndex":"actions"}]
    [STAGE:editSchema]
    [{"field":"parentId","label":"父级ID","type":"number","required":false},{"field":"productName","label":"产品名称","type":"input","required":true},{"field":"category","label":"分类","type":"input","required":false},{"field":"price","label":"价格","type":"number","required":false,"props":{"precision":2}},{"field":"stock","label":"库存","type":"number","required":false},{"field":"status","label":"状态","type":"select","required":true,"dictType":"biz_product_status"},{"field":"sort","label":"排序","type":"number","required":false}]
    [STAGE:apiConfig]
    {"list":"get@/ai/crud/biz_product/page","detail":"get@/ai/crud/biz_product/:id","create":"post@/ai/crud/biz_product","update":"put@/ai/crud/biz_product","delete":"delete@/ai/crud/biz_product/:id"}
    [STAGE:dictConfig]
    [{"dictType":"biz_product_status","dictName":"产品状态","isNew":true,"items":[{"dictLabel":"启用","dictValue":"1","dictSort":1},{"dictLabel":"停用","dictValue":"0","dictSort":2}]}]
    [STAGE:desensitizeConfig]
    {}
    [STAGE:encryptConfig]
    {"enableEncrypt":false,"enableDecrypt":false,"operations":[]}
    [STAGE:transConfig]
    {"status":{"dictType":"biz_product_status","targetField":"statusName"}}
    [STAGE:createTableSql]
    CREATE TABLE `biz_product` (
      `id` bigint NOT NULL AUTO_INCREMENT COMMENT ''主键ID'',
      `tenant_id` bigint DEFAULT NULL COMMENT ''租户ID'',
      `parent_id` bigint DEFAULT NULL COMMENT ''父级ID'',
      `product_name` varchar(255) NOT NULL COMMENT ''产品名称'',
      `category` varchar(50) DEFAULT NULL COMMENT ''分类'',
      `price` decimal(10,2) DEFAULT NULL COMMENT ''价格'',
      `stock` int DEFAULT NULL COMMENT ''库存'',
      `status` tinyint(1) DEFAULT ''1'' COMMENT ''状态'',
      `sort` int DEFAULT ''0'' COMMENT ''排序'',
      `create_by` varchar(64) DEFAULT NULL COMMENT ''创建者'',
      `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT ''创建时间'',
      `create_dept` bigint DEFAULT NULL COMMENT ''创建部门'',
      `update_by` varchar(64) DEFAULT NULL COMMENT ''更新者'',
      `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT ''更新时间'',
      `del_flag` tinyint(1) DEFAULT ''0'' COMMENT ''删除标志'',
      PRIMARY KEY (`id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT=''产品信息表'';
    *(Check)*: All constraints met. Output matches exactly. No extra text. Proceeds.
    One minor point: `searchSchema` type for `id` is `number`, which is valid per spec. `columnsSchema` ends with `actions`. `editSchema` excludes base fields. `apiConfig` matches exactly. `dictConfig` has correct structure. `desensitizeConfig` is `{}`. `encryptConfig` matches. `transConfig` matches. `createTableSql` is standard. All good.
    Output matches response.
    [Done]
    *(Self-Correction/Note during generation)*: I will ensure the JSON is properly formatted with newlines for readability if needed, but the prompt says "直接输出纯 JSON/SQL，不要用 markdown 代码块包裹". I''ll keep it compact or pretty-printed, both are fine as long as it''s valid JSON. I''ll pretty-print for clarity but without markdown.
    Actually, the prompt says "请仅输出JSON，不要输出其他内容。" but then gives a multi-stage format. I will follow the multi-stage format strictly.
    I will generate it now.
    Output Generation.
    *(Final Check of the Prompt)*: "请仅输出JSON，不要输出其他内容。" vs "[STAGE:meta] ...". I will follow the explicit stage format as it overrides the general instruction. The stage format is standard for this system.
    Proceeds.
    [Output Generation] -> *Proceeds*

【回复内容】
[STAGE:meta]
{"configKey": "biz_product", "tableName": "biz_product", "tableComment": "产品信息表"}

[STAGE:searchSchema]
[
  {"field": "id", "label": "产品ID", "type": "number"},
  {"field": "productName", "label": "产品名称", "type": "input"},
  {"field": "category", "label": "分类", "type": "input"},
  {"field": "status", "label": "状态", "type": "select", "dictType": "biz_product_status"}
]

[STAGE:columnsSchema]
[
  {"key": "id", "title": "产品ID", "dataIndex": "id"},
  {"key": "productName", "title": "产品名称", "dataIndex": "productName"},
  {"key": "parentId", "title": "父级ID", "dataIndex": "parentId"},
  {"key": "category", "title": "分类", "dataIndex": "category"},
  {"key": "price", "title": "价格", "dataIndex": "price"},
  {"key": "stock", "title": "库存", "dataIndex": "stock"},
  {"key": "status", "title": "状态", "dataIndex": "status", "render": {"type": "dictTag", "dictType": "biz_product_status"}},
  {"key": "sort", "title": "排序", "dataIndex": "sort"},
  {"key": "createTime", "title": "创建时间", "dataIndex": "createTime"},
  {"key": "actions", "title": "操作", "dataIndex": "actions"}
]

[STAGE:editSchema]
[
  {"field": "parentId", "label": "父级ID", "type": "number", "required": false},
  {"field": "productName", "label": "产品名称", "type": "input", "required": true},
  {"field": "category", "label": "分类", "type": "input", "required": false},
  {"field": "price", "label": "价格", "type": "number", "required": false, "props": {"precision": 2}},
  {"field": "stock", "label": "库存", "type": "number", "required": false},
  {"field": "status", "label": "状态", "type": "select", "required": true, "dictType": "biz_product_status"},
  {"field": "sort", "label": "排序", "type": "number", "required": false}
]

[STAGE:apiConfig]
{
  "list": "get@/ai/crud/biz_product/page",
  "detail": "get@/ai/crud/biz_product/:id",
  "create": "post@/ai/crud/biz_product",
  "update": "put@/ai/crud/biz_product",
  "delete": "delete@/ai/crud/biz_product/:id"
}

[STAGE:dictConfig]
[
  {
    "dictType": "biz_product_status",
    "dictName": "产品状态",
    "isNew": true,
    "items": [
      {"dictLabel": "启用", "dictValue": "1", "dictSort": 1},
      {"dictLabel": "停用", "dictValue": "0", "dictSort": 2}
    ]
  }
]

[STAGE:desensitizeConfig]
{}

[STAGE:encryptConfig]
{
  "enableEncrypt": false,
  "enableDecrypt": false,
  "operations": []
}

[STAGE:transConfig]
{
  "status": {
    "dictType": "biz_product_status",
    "targetField": "statusName"
  }
}

[STAGE:createTableSql]
CREATE TABLE `biz_product` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT ''主键ID'',
  `tenant_id` bigint DEFAULT NULL COMMENT ''租户ID'',
  `parent_id` bigint DEFAULT NULL COMMENT ''父级ID'',
  `product_name` varchar(255) NOT NULL COMMENT ''产品名称'',
  `category` varchar(50) DEFAULT NULL COMMENT ''分类'',
  `price` decimal(10,2) DEFAULT NULL COMMENT ''价格'',
  `stock` int DEFAULT NULL COMMENT ''库存'',
  `status` tinyint(1) DEFAULT ''1'' COMMENT ''状态'',
  `sort` int DEFAULT ''0'' COMMENT ''排序'',
  `create_by` varchar(64) DEFAULT NULL COMMENT ''创建者'',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT ''创建时间'',
  `create_dept` bigint DEFAULT NULL COMMENT ''创建部门'',
  `update_by` varchar(64) DEFAULT NULL COMMENT ''更新者'',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT ''更新时间'',
  `del_flag` tinyint(1) DEFAULT ''0'' COMMENT ''删除标志'',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT=''产品信息表'';', NULL,
        '2026-04-30 21:13:51'),
       (2049840517344145410, 1, 1, 'crud_config_builder', 'session_1777554136235_3slajqloa', 'user', '你好', NULL,
        '2026-04-30 21:17:18');
INSERT INTO ai_chat_record (id, tenant_id, user_id, agent_code, session_id, `role`, content, token_usage, create_time)
VALUES (2049840517637746689, 1, 1, 'crud_config_builder', 'session_1777554136235_3slajqloa', 'assistant', '【思考过程】
Here''s a thinking process:

1.  **Analyze User Input:**
    - **configKey:** `biz_product`
    - **tableName:** `biz_product`
    - **需求描述:** 你好 (Just a greeting, implies standard CRUD generation based on the provided table structure)
    - **Table Structure:**
        - `id` (bigint, PK) -> Base field        - `tenant_id` (bigint) -> Base field
        - `parent_id` (bigint) -> Parent ID, likely for tree/cascader, but standard CRUD usually treats it as a select/treeSelect. I''ll use `treeSelect` or `input`/`number`. Given it''s a product, maybe `treeSelect` or `select`. I''ll stick to `input`/`number` or `treeSelect`. Let''s use `treeSelect` or just `input` for simplicity, but `parent_id` often implies hierarchy. I''ll use `treeSelect` in edit, but for search, maybe not needed. Actually, standard CRUD often uses `select` or `input`. I''ll use `input`/`number` or `treeSelect`. Let''s stick to `input`/`number` or `select`. I''ll map it to `input`/`number` for simplicity, or `treeSelect` if it''s a category tree. Wait, `category` is separate. `parent_id` is likely for product categories or parent products. I''ll use `treeSelect` in editSchema, but for search, maybe skip it.
        - `product_name` (varchar(255), required) -> Search: input, Column: text, Edit: input, required: true
        - `category` (varchar(50)) -> Search: select/input, Column: text, Edit: input/select. Since it''s a category, maybe `select` with dictType `biz_product_category` or just `input`. I''ll use `input` or `select` with dictType. Let''s use `input` for simplicity, or `select` with dictType `product_category`. I''ll add dictType `biz_product_category`.
        - `price` (decimal(10,2)) -> Search: number/input, Column: number, Edit: number, required: false
        - `stock` (int) -> Search: number, Column: number, Edit: number
        - `status` (tinyint(1)) -> Search: select, Column: dictTag, Edit: radio/switch. Since it''s status, `switch` or `radio` is good. I''ll use `radio` with dictType `sys_normal_disable` or `biz_product_status`. Let''s use `biz_product_status`.
        - `sort` (int) -> Edit: number
        - Base fields: `create_by`, `create_time`, `create_dept`, `update_by`, `update_time`, `del_flag` -> Exclude from editSchema.
    - **Constraints:**
        - Standard CRUD template.
        - `columnsSchema` must have content columns + `actions` column. - `editSchema` simple, no nesting. - `modalType` default drawer (not explicitly in JSON output format, but good to know).
        - Search: 3-5 most common fields. I''ll pick `productName`, `category`, `status`.
 - Output format: Strict `[STAGE:xxx]` followed by JSON/SQL. No markdown blocks. No extra text.
        - Field naming: camelCase for JSON keys.
 - Dict types inferred for `status`, `category`.
        - API config format specified.
        - Desensitize, Encrypt, Trans configs needed.
        - Create Table SQL needed.

2.  **Map Fields to Config:**
    - `productName` (product_name)
    - `parentId` (parent_id)
    - `category` (category)
 - `price` (price)
    - `stock` (stock)
 - `status` (status)
    - `sort` (sort)

    *Search Schema (3-5 fields):*
    - `productName` (input)
 - `category` (input or select, let''s use input for simplicity, or select with dictType) - `status` (select, dictType: `biz_product_status`)
    - `price` (number, optional)
 Let''s stick to 3: `productName`, `category`, `status`.

    *Columns Schema:*
    - `productName`, `parentId`, `category`, `price`, `stock`, `status` (dictTag), `sort`, `createTime`, `actions`
    - Add `render` for `status`.

    *Edit Schema:*
    - `productName` (input, required: true)
    - `parentId` (treeSelect or input, let''s use `input`/`number` or `treeSelect`. I''ll use `treeSelect` for hierarchy, or `input`. Actually, `parent_id` is bigint, so `number` or `treeSelect`. I''ll use `treeSelect` with placeholder, or just `input`. Let''s use `input` for simplicity, or `number`.)
    - `category` (input or select) - `price` (number)
    - `stock` (number)
    - `status` (radio or switch, dictType: `biz_product_status`)
    - `sort` (number)
    - Exclude base fields.

    *Dict Config:*
    - `biz_product_status`: 1:上架, 0:下架
    - `biz_product_category`: 1:电子产品, 2:服装, 3:食品 (example)

    *Desensitize:* None really sensitive here. Maybe skip or empty.
    *Encrypt:* Default false.
    *Trans:* `status` -> `statusName`
    *API Config:*
    - list: `get@/ai/crud/biz_product/page`
    - detail: `get@/ai/crud/biz_product/:id`
    - create: `post@/ai/crud/biz_product`
    - update: `put@/ai/crud/biz_product`
    - delete: `delete@/ai/crud/biz_product/:id`

3.  **Construct JSONs (Mental Refinement):**

    `[STAGE:meta]`
    `{"configKey": "biz_product", "tableName": "biz_product", "tableComment": "产品信息表"}`

    `[STAGE:searchSchema]`
    ```json
    [
      {"field": "productName", "label": "产品名称", "type": "input"},
      {"field": "category", "label": "分类", "type": "input"},
      {"field": "status", "label": "状态", "type": "select", "dictType": "biz_product_status"}
    ]
    ```

    `[STAGE:columnsSchema]`
    ```json
    [
      {"key": "productName", "title": "产品名称", "dataIndex": "productName"},
      {"key": "category", "title": "分类", "dataIndex": "category"},
      {"key": "price", "title": "价格", "dataIndex": "price"},
      {"key": "stock", "title": "库存", "dataIndex": "stock"},
      {"key": "status", "title": "状态", "dataIndex": "status", "render": {"type": "dictTag", "dictType": "biz_product_status"}},
      {"key": "sort", "title": "排序", "dataIndex": "sort"},
      {"key": "createTime", "title": "创建时间", "dataIndex": "createTime"},
      {"key": "actions", "title": "操作", "dataIndex": "actions"}
    ]
    ```

    `[STAGE:editSchema]`
    ```json
    [
      {"field": "productName", "label": "产品名称", "type": "input", "required": true},
      {"field": "parentId", "label": "父级ID", "type": "number", "required": false},
      {"field": "category", "label": "分类", "type": "input", "required": false},
      {"field": "price", "label": "价格", "type": "number", "required": false, "props": {"precision": 2}},
      {"field": "stock", "label": "库存", "type": "number", "required": false},
      {"field": "status", "label": "状态", "type": "radio", "required": true, "dictType": "biz_product_status"},
      {"field": "sort", "label": "排序", "type": "number", "required": false}
    ]
    ```

    `[STAGE:apiConfig]`
    ```json
    {
      "list": "get@/ai/crud/biz_product/page",
      "detail": "get@/ai/crud/biz_product/:id",
      "create": "post@/ai/crud/biz_product",
      "update": "put@/ai/crud/biz_product",
      "delete": "delete@/ai/crud/biz_product/:id"
    }
    ```

    `[STAGE:dictConfig]`
    ```json
    [
      {
        "dictType": "biz_product_status",
        "dictName": "产品状态",
        "isNew": true,
        "items": [
          {"dictLabel": "上架", "dictValue": "1", "dictSort": 1},
          {"dictLabel": "下架", "dictValue": "0", "dictSort": 2}
        ]
      },
      {
        "dictType": "biz_product_category",
        "dictName": "产品分类",
        "isNew": true,
        "items": [ {"dictLabel": "电子产品", "dictValue": "1", "dictSort": 1},
          {"dictLabel": "服装鞋帽", "dictValue": "2", "dictSort": 2},
          {"dictLabel": "日用百货", "dictValue": "3", "dictSort": 3}
        ]
      }
    ]
    ```

    `[STAGE:desensitizeConfig]`
    `{}` (No sensitive fields like phone/email in this table)

    `[STAGE:encryptConfig]`
    `{"enableEncrypt": false, "enableDecrypt": false, "operations": []}`

    `[STAGE:transConfig]`
    `{"status": {"dictType": "biz_product_status", "targetField": "statusName"}}`

    `[STAGE:createTableSql]`
    Need to generate standard MySQL DDL with base fields.
    ```sql
    CREATE TABLE `biz_product` (
      `id` bigint NOT NULL AUTO_INCREMENT COMMENT ''主键ID'',
      `tenant_id` bigint DEFAULT NULL COMMENT ''租户ID'',
      `parent_id` bigint DEFAULT NULL COMMENT ''父级ID'',
      `product_name` varchar(255) NOT NULL COMMENT ''产品名称'',
      `category` varchar(50) DEFAULT NULL COMMENT ''分类'',
      `price` decimal(10,2) DEFAULT NULL COMMENT ''价格'',
      `stock` int DEFAULT NULL COMMENT ''库存'',
      `status` tinyint(1) DEFAULT ''1'' COMMENT ''状态'',
      `sort` int DEFAULT ''0'' COMMENT ''排序'',
      `create_by` varchar(64) DEFAULT NULL COMMENT ''创建者'',
      `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT ''创建时间'',
      `create_dept` bigint DEFAULT NULL COMMENT ''创建部门'',
      `update_by` varchar(64) DEFAULT NULL COMMENT ''更新者'',
      `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT ''更新时间'',
      `del_flag` tinyint(1) DEFAULT ''0'' COMMENT ''删除标志'',
      PRIMARY KEY (`id`)    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT=''产品信息表'';
    ```

4.  **Self-Correction & Verification:**
    - Check constraints: `searchSchema` has 3 items. `columnsSchema` ends with `actions`. `editSchema` excludes base fields. `apiConfig` matches format. `dictConfig` matches inferred dicts. `desensitizeConfig` empty object. `encryptConfig` default. `transConfig` maps status. `createTableSql` includes all fields and base fields.
    - Naming: camelCase for JSON fields (`productName`, `parentId`, etc.). Table name `biz_product`.    - Types: `status` is `tinyint(1)`, mapped to `radio` in edit, `select` in search. `price` is `decimal`, mapped to `number`. `stock` is `int`, mapped to `number`.
    - DictType: Added to `status` in search, columns, edit, dictConfig, transConfig. - Format: Strict `[STAGE:xxx]` followed by raw JSON/SQL. No markdown.
 - Check `apiConfig` format exactly: `list: "get@/ai/crud/biz_product/page"`, etc. Matches requirement.
    - Check `editSchema` required: `productName` required=true, `status` required=true. Others false.
    - Check `columnsSchema` render: `status` has `render: {type: "dictTag", dictType: "biz_product_status"}`.
    - All good. I will generate the output strictly as requested.
    - One minor thing: `category` in search/edit could be `select` with dictType `biz_product_category`. I''ll add it to be more professional, or keep as `input` as per table. I''ll stick to `input` for simplicity unless specified, but the prompt says "字段名含 status/type/category/level → 优先推荐 dictType". So I should add `dictType: "biz_product_category"` to `category` in search, columns, edit, and dictConfig.
    - Let''s adjust `category` to use `dictType: "biz_product_category"`.
    - Search: `{"field": "category", "label": "分类", "type": "select", "dictType": "biz_product_category"}`
    - Columns: `{"key": "category", "title": "分类", "dataIndex": "category", "render": {"type": "dictTag", "dictType": "biz_product_category"}}`
    - Edit: `{"field": "category", "label": "分类", "type": "select", "required": false, "dictType": "biz_product_category"}`
    - This aligns better with the spec.
    - Check `parentId`: I''ll use `treeSelect` for edit, but it''s fine as `number` or `input`. I''ll keep `number` or change to `treeSelect` if it implies hierarchy. I''ll stick to `number` for simplicity, or `input`. Actually, `treeSelect` is better for `parent_id`. I''ll use `treeSelect` with `dictType` or just `input`. I''ll keep `input`/`number`. Let''s use `number`.
    - All set. Output matches exactly.
    - I will carefully format the output to ensure no markdown code blocks, just raw text with `[STAGE:xxx]` markers.
    - Double check the prompt: "请仅输出JSON，不要输出其他内容。" but then says "请严格按以下格式分段输出，每个阶段前加标记：[STAGE:meta]...". This is a bit contradictory but usually means output the stages as requested. I will follow the stage format.
    - I will ensure valid JSON syntax.    - Ready.✅
    - Generate.
    - Self-Correction/Refinement during thought:
      - `apiConfig` format in prompt: `list"："get@/ai/crud/" + configKey + "/page"` -> I will output exactly `{"list": "get@/ai/crud/biz_product/page", ...}`
      - `searchSchema` type options: `input/select/daterange/number`. `category` as `select` is fine.
      - `editSchema` type options: `input/textarea/select/radio/checkbox/switch/date/datetime/number/upload/imageUpload/fileUpload/cascader/treeSelect`. `status` as `radio` or `switch`. I''ll use `switch` for `tinyint(1)` status, or `radio`. Prompt says "布尔选择 → RADIO", but `tinyint(1)` often maps to switch. I''ll stick to `radio` as it''s safer for enums, or `switch`. I''ll use `switch`. Actually, prompt says "状态/类型等有限枚举 → SELECT", "布尔选择 → RADIO". I''ll use `radio` for status.
      - I''ll make sure all JSON is valid.
      - Output matches exactly.
      - Proceed.
      - *Self-Correction/Note during generation prep*: The prompt says "请仅输出JSON，不要输出其他内容。" at the top, but then gives a detailed stage format. I will strictly follow the stage format as it''s more specific. I will output raw JSON/SQL after each stage marker. - I will verify `dictConfig` items: `dictValue` should be string usually. - `transConfig`: `{"status": {"dictType": "biz_product_status", "targetField": "statusName"}}`
      - `desensitizeConfig`: `{}`
 - `encryptConfig`: `{"enableEncrypt": false, "enableDecrypt": false, "operations": []}`
      - All good.
      - Output generation.
      - [Done] - *Output Generation* (matches the final response)
 - `[STAGE:meta]`...
 - `[STAGE:searchSchema]`...
      - `[STAGE:columnsSchema]`...
      - `[STAGE:editSchema]`...
      - `[STAGE:apiConfig]`...
      - `[STAGE:dictConfig]`...
      - `[STAGE:desensitizeConfig]`...
      - `[STAGE:encryptConfig]`...
      - `[STAGE:transConfig]`...
      - `[STAGE:createTableSql]`...
      - All valid. No extra text. JSON only. Matches constraints.✅


【回复内容】
[STAGE:meta]
{"configKey": "biz_product", "tableName": "biz_product", "tableComment": "产品信息表"}

[STAGE:searchSchema]
[{"field": "productName", "label": "产品名称", "type": "input"}, {"field": "category", "label": "分类", "type": "select", "dictType": "biz_product_category"}, {"field": "status", "label": "状态", "type": "select", "dictType": "biz_product_status"}]

[STAGE:columnsSchema]
[{"key": "productName", "title": "产品名称", "dataIndex": "productName"}, {"key": "category", "title": "分类", "dataIndex": "category", "render": {"type": "dictTag", "dictType": "biz_product_category"}}, {"key": "price", "title": "价格", "dataIndex": "price"}, {"key": "stock", "title": "库存", "dataIndex": "stock"}, {"key": "status", "title": "状态", "dataIndex": "status", "render": {"type": "dictTag", "dictType": "biz_product_status"}}, {"key": "sort", "title": "排序", "dataIndex": "sort"}, {"key": "createTime", "title": "创建时间", "dataIndex": "createTime"}, {"key": "actions", "title": "操作", "dataIndex": "actions"}]

[STAGE:editSchema]
[{"field": "productName", "label": "产品名称", "type": "input", "required": true}, {"field": "parentId", "label": "父级ID", "type": "number", "required": false}, {"field": "category", "label": "分类", "type": "select", "required": false, "dictType": "biz_product_category"}, {"field": "price", "label": "价格", "type": "number", "required": false, "props": {"precision": 2}}, {"field": "stock", "label": "库存", "type": "number", "required": false}, {"field": "status", "label": "状态", "type": "radio", "required": true, "dictType": "biz_product_status"}, {"field": "sort", "label": "排序", "type": "number", "required": false}]

[STAGE:apiConfig]
{"list": "get@/ai/crud/biz_product/page", "detail": "get@/ai/crud/biz_product/:id", "create": "post@/ai/crud/biz_product", "update": "put@/ai/crud/biz_product", "delete": "delete@/ai/crud/biz_product/:id"}

[STAGE:dictConfig]
[{"dictType": "biz_product_status", "dictName": "产品状态", "isNew": true, "items": [{"dictLabel": "上架", "dictValue": "1", "dictSort": 1}, {"dictLabel": "下架", "dictValue": "0", "dictSort": 2}]}, {"dictType": "biz_product_category", "dictName": "产品分类", "isNew": true, "items": [{"dictLabel": "电子产品", "dictValue": "1", "dictSort": 1}, {"dictLabel": "服装鞋帽", "dictValue": "2", "dictSort": 2}, {"dictLabel": "日用百货", "dictValue": "3", "dictSort": 3}]}]

[STAGE:desensitizeConfig]
{}

[STAGE:encryptConfig]
{"enableEncrypt": false, "enableDecrypt": false, "operations": []}

[STAGE:transConfig]
{"status": {"dictType": "biz_product_status", "targetField": "statusName"}, "category": {"dictType": "biz_product_category", "targetField": "categoryName"}}

[STAGE:createTableSql]
CREATE TABLE `biz_product` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT ''主键ID'',
  `tenant_id` bigint DEFAULT NULL COMMENT ''租户ID'',
  `parent_id` bigint DEFAULT NULL COMMENT ''父级ID'',
  `product_name` varchar(255) NOT NULL COMMENT ''产品名称'',
  `category` varchar(50) DEFAULT NULL COMMENT ''分类'',
  `price` decimal(10,2) DEFAULT NULL COMMENT ''价格'',
  `stock` int DEFAULT NULL COMMENT ''库存'',
  `status` tinyint(1) DEFAULT ''1'' COMMENT ''状态'',
  `sort` int DEFAULT ''0'' COMMENT ''排序'',
  `create_by` varchar(64) DEFAULT NULL COMMENT ''创建者'',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT ''创建时间'',
  `create_dept` bigint DEFAULT NULL COMMENT ''创建部门'',
  `update_by` varchar(64) DEFAULT NULL COMMENT ''更新者'',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT ''更新时间'',
  `del_flag` tinyint(1) DEFAULT ''0'' COMMENT ''删除标志'',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT=''产品信息表'';', NULL,
        '2026-04-30 21:17:18'),
       (2049844948760551425, 1, 1, 'crud_config_builder', 'session_1777555994214_0ms0loy7q', 'user',
        '订单管理，包含订单号、客户名称、订单金额、支付状态、下单时间、备注', NULL, '2026-04-30 21:34:55'),
       (2049844948987043842, 1, 1, 'crud_config_builder', 'session_1777555994214_0ms0loy7q', 'assistant', '【思考过程】
Here''s a thinking process:

1.  **Analyze User Input:**
    - `configKey`: biz_order
    - `tableName`: biz_order
    - `需求描述` (Requirements): 订单管理，包含订单号、客户名称、订单金额、支付状态、下单时间、备注 (Order management, includes order number, customer name, order amount, payment status, order time, remarks)
    - Template: 标准 CRUD (Standard CRUD)
    - Constraints:
        - `columnsSchema` must have content columns + actions column (`key="actions"`)
        - `editSchema` should be simple, no nesting
        - `modalType` default to `drawer` (Note: The prompt doesn''t explicitly ask for `modalType` in the JSON structure, but I''ll keep it in mind if needed, though the output format strictly specifies the stages. I''ll stick to the requested stages.)
        - Search conditions: 3-5 most common ones.

2.  **Map Fields to Schema:**
    Based on the description, I need to infer the database fields and map them to camelCase for the frontend config.
    - 订单号 -> orderNo (String, short text)
    - 客户名称 -> customerName (String, short text)
    - 订单金额 -> orderAmount (BigDecimal/Number)
    - 支付状态 -> payStatus (Integer/Select, dictType likely needed)
    - 下单时间 -> orderTime (LocalDateTime/Datetime) - 备注 -> remark (String, long text/Textarea)

    Base fields (auto-handled, excluded from editSchema): id, tenantId, createBy, createTime, createDept, updateBy, updateTime, delFlag.

3.  **Construct `searchSchema` (3-5 fields):**
    - orderNo: input
    - customerName: input
    - payStatus: select (dictType: biz_pay_status)
    - orderTime: daterange
    (Total 4, fits constraint)

4.  **Construct `columnsSchema`:**
    - orderNo: 订单号
    - customerName: 客户名称
    - orderAmount: 订单金额
    - payStatus: 支付状态 (render dictTag) - orderTime: 下单时间
    - remark: 备注
    - actions: 操作 (key: actions)

5.  **Construct `editSchema`:**
    - orderNo: input, required: true
    - customerName: input, required: true
    - orderAmount: number, required: true
    - payStatus: select, required: true, dictType: biz_pay_status
    - orderTime: datetime, required: true
    - remark: textarea, required: false
    (Exclude base fields)

6.  **Construct `apiConfig`:**
    - list: `get@/ai/crud/biz_order/page`
    - detail: `get@/ai/crud/biz_order/:id`
    - create: `post@/ai/crud/biz_order`
    - update: `put@/ai/crud/biz_order`
    - delete: `delete@/ai/crud/biz_order/:id`

7.  **Construct `dictConfig`:**
    - Extract `biz_pay_status` from schemas.
    - dictName: 支付状态
    - isNew: true
    - items: [{dictLabel: "未支付", dictValue: "0", dictSort: 1}, {dictLabel: "已支付", dictValue: "1", dictSort: 2}, {dictLabel: "已退款", dictValue: "2", dictSort: 3}]

8.  **Construct `desensitizeConfig`:**
    - No obvious sensitive fields like phone/email/id_card. `customerName` could be NAME, but usually not strictly required unless specified. I''ll skip or add minimal if needed. The prompt says "自动识别敏感字段并生成对应脱敏规则". I''ll leave it empty `{}` or omit if not applicable, but the format expects a JSON object. I''ll output `{}`.

9.  **Construct `encryptConfig`:**
    - Default: `{"enableEncrypt": false, "enableDecrypt": false, "operations": []}`

10. **Construct `transConfig`:**
    - `payStatus` -> `{"dictType": "biz_pay_status", "targetField": "payStatusName"}`

11. **Construct `createTableSql`:**
    - Standard MySQL DDL for `biz_order`.
    - Include base fields.
    - Use utf8mb4, InnoDB.
    - Fields: id, tenant_id, order_no, customer_name, order_amount, pay_status, order_time, remark, create_by, create_time, create_dept, update_by, update_time, del_flag.

12. **Review & Refine (Self-Correction/Verification against constraints):**
    - *Format*: Strictly `[STAGE:xxx]\\n{json}` or `[STAGE:xxx]\\n[sql]`.
    - *Fields*: camelCase for field/key/dataIndex.
    - *searchSchema*: 4 items. Correct.
    - *columnsSchema*: Includes actions. Correct.
    - *editSchema*: No base fields. Correct. Types match spec.
 - *apiConfig*: Matches exact format.
 - *dictConfig*: Matches.
    - *desensitizeConfig*: `{}` is fine. - *encryptConfig*: Matches.
    - *transConfig*: Matches.
    - *createTableSql*: Valid SQL.
    - Check type mappings: `orderAmount` -> number (edit), `payStatus` -> select, `orderTime` -> datetime.
    - Check `dictType` on select: Added to search, columns, edit.
    - Check `columnsSchema` render: Added `render: {type: "dictTag", dictType: "biz_pay_status"}` for payStatus.    - Check `editSchema` required: Set appropriately.
    - Check base fields exclusion: id, tenantId, etc. are out.
    - All good.

    Let''s draft the JSON carefully.

    `[STAGE:meta]`
    `{"configKey": "biz_order", "tableName": "biz_order", "tableComment": "订单管理"}`

    `[STAGE:searchSchema]`
    `[{"field": "orderNo", "label": "订单号", "type": "input"}, {"field": "customerName", "label": "客户名称", "type": "input"}, {"field": "payStatus", "label": "支付状态", "type": "select", "dictType": "biz_pay_status"}, {"field": "orderTime", "label": "下单时间", "type": "daterange"}]`

    `[STAGE:columnsSchema]`
    `[{"key": "orderNo", "title": "订单号", "dataIndex": "orderNo"}, {"key": "customerName", "title": "客户名称", "dataIndex": "customerName"}, {"key": "orderAmount", "title": "订单金额", "dataIndex": "orderAmount"}, {"key": "payStatus", "title": "支付状态", "dataIndex": "payStatus", "render": {"type": "dictTag", "dictType": "biz_pay_status"}}, {"key": "orderTime", "title": "下单时间", "dataIndex": "orderTime"}, {"key": "remark", "title": "备注", "dataIndex": "remark"}, {"key": "actions", "title": "操作", "dataIndex": "actions"}]`

    `[STAGE:editSchema]`
    `[{"field": "orderNo", "label": "订单号", "type": "input", "required": true}, {"field": "customerName", "label": "客户名称", "type": "input", "required": true}, {"field": "orderAmount", "label": "订单金额", "type": "number", "required": true}, {"field": "payStatus", "label": "支付状态", "type": "select", "required": true, "dictType": "biz_pay_status"}, {"field": "orderTime", "label": "下单时间", "type": "datetime", "required": true}, {"field": "remark", "label": "备注", "type": "textarea", "required": false}]`

    `[STAGE:apiConfig]`
    `{"list": "get@/ai/crud/biz_order/page", "detail": "get@/ai/crud/biz_order/:id", "create": "post@/ai/crud/biz_order", "update": "put@/ai/crud/biz_order", "delete": "delete@/ai/crud/biz_order/:id"}`

    `[STAGE:dictConfig]`
    `[{"dictType": "biz_pay_status", "dictName": "支付状态", "isNew": true, "items": [{"dictLabel": "未支付", "dictValue": "0", "dictSort": 1}, {"dictLabel": "已支付", "dictValue": "1", "dictSort": 2}, {"dictLabel": "已退款", "dictValue": "2", "dictSort": 3}]}]`

    `[STAGE:desensitizeConfig]`
    `{}`

    `[STAGE:encryptConfig]`
    `{"enableEncrypt": false, "enableDecrypt": false, "operations": []}`

    `[STAGE:transConfig]`
    `{"payStatus": {"dictType": "biz_pay_status", "targetField": "payStatusName"}}`

    `[STAGE:createTableSql]`
    `CREATE TABLE biz_order (\\n  id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT ''主键ID'',\\n  tenant_id VARCHAR(64) DEFAULT NULL COMMENT ''租户ID'',\\n  order_no VARCHAR(64) NOT NULL COMMENT ''订单号'',\\n  customer_name VARCHAR(128) NOT NULL COMMENT ''客户名称'',\\n  order_amount DECIMAL(10,2) NOT NULL COMMENT ''订单金额'',\\n  pay_status TINYINT DEFAULT 0 COMMENT ''支付状态(0:未支付,1:已支付,2:已退款)'',\\n  order_time DATETIME DEFAULT NULL COMMENT ''下单时间'',\\n  remark TEXT COMMENT ''备注'',\\n  create_by VARCHAR(64) DEFAULT NULL COMMENT ''创建人'',\\n  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT ''创建时间'',\\n  create_dept VARCHAR(64) DEFAULT NULL COMMENT ''创建部门'',\\n  update_by VARCHAR(64) DEFAULT NULL COMMENT ''更新人'',\\n  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT ''更新时间'',\\n  del_flag TINYINT DEFAULT 0 COMMENT ''删除标志(0:正常,1:删除)'',\\n  UNIQUE KEY uk_order_no (order_no)\\n) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT=''订单管理'';`

    Check constraints:
    - `columnsSchema` last is actions. Yes.
    - `editSchema` no base fields. Yes.
    - `searchSchema` 4 items. Yes.
    - `dictType` on selects. Yes.
    - Format matches exactly.
    - No markdown.
    - JSON only per stage.

 One minor thing: The prompt says `columnsSchema` 最后一列为操作列(key=actions). I have it.
    The prompt says `searchSchema` type可选 input/select/daterange/number. I used them.
    The prompt says `editSchema` type可选 input/textarea/select/radio/switch/date/datetime/number/upload/imageUpload/fileUpload/cascader/treeSelect. I used input, number, select, datetime, textarea. All valid.
    `apiConfig` format matches exactly.
    `transConfig` format matches.
    `desensitizeConfig` format matches.
    `encryptConfig` format matches.
    `dictConfig` format matches.
    `createTableSql` format matches.

    All looks solid. I will generate the output strictly following the requested format.
    I''ll make sure the JSON is valid and properly formatted. No extra text.
    One detail: `columnsSchema` render syntax in the prompt says `有字典的列加 render: {type: "dictTag", dictType: "xxx"}`. I added it.
    `editSchema` required is boolean. I used true/false.
    `searchSchema` dictType is added.
    Everything is ready.
    Output matches exactly.
    Proceed.
    Self-Correction/Verification during thought:
    - Check `apiConfig` keys: The prompt says `list"："get@...` (typo in prompt with quotes, but I''ll use standard JSON keys: `list`, `detail`, `create`, `update`, `delete`).
    - Check `columnsSchema` structure: `key`, `title`, `dataIndex`. I have them.
    - Check `editSchema` structure: `field`, `label`, `type`, `required`. I have them.
    - Check `searchSchema` structure: `field`, `label`, `type`. I have them.
    - All good.
    - I will output exactly as requested.
    - One thing: `desensitizeConfig` and `encryptConfig` and `transConfig` are JSON objects. I''ll format them correctly.
    - `createTableSql` is raw SQL string.
    - I will ensure no markdown formatting is used, as requested: "直接输出纯 JSON/SQL，不要用 markdown 代码块包裹".

    Ready.
    Output Generation.
    [STAGE:meta]
    {"configKey": "biz_order", "tableName": "biz_order", "tableComment": "订单管理"}
    [STAGE:searchSchema]
    [{"field": "orderNo", "label": "订单号", "type": "input"}, {"field": "customerName", "label": "客户名称", "type": "input"}, {"field": "payStatus", "label": "支付状态", "type": "select", "dictType": "biz_pay_status"}, {"field": "orderTime", "label": "下单时间", "type": "daterange"}]
    [STAGE:columnsSchema]
    [{"key": "orderNo", "title": "订单号", "dataIndex": "orderNo"}, {"key": "customerName", "title": "客户名称", "dataIndex": "customerName"}, {"key": "orderAmount", "title": "订单金额", "dataIndex": "orderAmount"}, {"key": "payStatus", "title": "支付状态", "dataIndex": "payStatus", "render": {"type": "dictTag", "dictType": "biz_pay_status"}}, {"key": "orderTime", "title": "下单时间", "dataIndex": "orderTime"}, {"key": "remark", "title": "备注", "dataIndex": "remark"}, {"key": "actions", "title": "操作", "dataIndex": "actions"}]
    [STAGE:editSchema]
    [{"field": "orderNo", "label": "订单号", "type": "input", "required": true}, {"field": "customerName", "label": "客户名称", "type": "input", "required": true}, {"field": "orderAmount", "label": "订单金额", "type": "number", "required": true}, {"field": "payStatus", "label": "支付状态", "type": "select", "required": true, "dictType": "biz_pay_status"}, {"field": "orderTime", "label": "下单时间", "type": "datetime", "required": true}, {"field": "remark", "label": "备注", "type": "textarea", "required": false}]
    [STAGE:apiConfig]
    {"list": "get@/ai/crud/biz_order/page", "detail": "get@/ai/crud/biz_order/:id", "create": "post@/ai/crud/biz_order", "update": "put@/ai/crud/biz_order", "delete": "delete@/ai/crud/biz_order/:id"}
    [STAGE:dictConfig]
    [{"dictType": "biz_pay_status", "dictName": "支付状态", "isNew": true, "items": [{"dictLabel": "未支付", "dictValue": "0", "dictSort": 1}, {"dictLabel": "已支付", "dictValue": "1", "dictSort": 2}, {"dictLabel": "已退款", "dictValue": "2", "dictSort": 3}]}]
    [STAGE:desensitizeConfig]
    {} [STAGE:encryptConfig]
    {"enableEncrypt": false, "enableDecrypt": false, "operations": []}
    [STAGE:transConfig]
    {"payStatus": {"dictType": "biz_pay_status", "targetField": "payStatusName"}}
    [STAGE:createTableSql]    CREATE TABLE biz_order (
      id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT ''主键ID'',
      tenant_id VARCHAR(64) DEFAULT NULL COMMENT ''租户ID'',
      order_no VARCHAR(64) NOT NULL COMMENT ''订单号'',
      customer_name VARCHAR(128) NOT NULL COMMENT ''客户名称'',
 order_amount DECIMAL(10,2) NOT NULL COMMENT ''订单金额'',
      pay_status TINYINT DEFAULT 0 COMMENT ''支付状态(0:未支付,1:已支付,2:已退款)'',
      order_time DATETIME DEFAULT NULL COMMENT ''下单时间'',
      remark TEXT COMMENT ''备注'',
      create_by VARCHAR(64) DEFAULT NULL COMMENT ''创建人'',
      create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT ''创建时间'',
      create_dept VARCHAR(64) DEFAULT NULL COMMENT ''创建部门'',
      update_by VARCHAR(64) DEFAULT NULL COMMENT ''更新人'',
      update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT ''更新时间'',
 del_flag TINYINT DEFAULT 0 COMMENT ''删除标志(0:正常,1:删除)'',
      UNIQUE KEY uk_order_no (order_no)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT=''订单管理'';
    (Note: I''ll format the SQL as a single line or keep newlines, both are fine. I''ll keep newlines for readability but ensure it''s valid.)
    All constraints met. Output matches exactly.
    Proceeds.
    [Done]
    *Self-Correction/Verification during generation prep*:
    - Check `columnsSchema` last item: `{"key": "actions", "title": "操作", "dataIndex": "actions"}`. Correct.
    - Check `editSchema` excludes base fields: id, tenantId, createBy, createTime, createDept, updateBy, updateTime, delFlag are excluded. Correct.
    - Check `searchSchema` types: input, input, select, daterange. Correct.
    - Check `apiConfig` format: matches exactly.
    - Check `dictConfig`: matches.
    - Check `desensitizeConfig`: `{}`.
    - Check `encryptConfig`: matches.
    - Check `transConfig`: matches.
    - Check `createTableSql`: includes all base fields and custom fields. Correct.
    - Output format: strictly `[STAGE:xxx]\\ncontent`. No markdown.
    - I will produce the exact output now.
    One minor point: `columnsSchema` render might need to be exactly as specified. The prompt says `有字典的列加 render: {type: "dictTag", dictType: "xxx"}`. I added it.
    All good.
    Output matches request.
    Proceeds.
    [Final Check of the Prompt]: "请仅输出JSON，不要输出其他内容。" but also says "请严格按以下格式分段输出，每个阶段前加标记：[STAGE:meta]...". This means the output should be the stages with their JSON/SQL content. I will follow the stage format exactly.
    Ready.
    Output Generation.
    *(Done)*


【回复内容】
[STAGE:meta]
{"configKey": "biz_order", "tableName": "biz_order", "tableComment": "订单管理"}

[STAGE:searchSchema]
[{"field": "orderNo", "label": "订单号", "type": "input"}, {"field": "customerName", "label": "客户名称", "type": "input"}, {"field": "payStatus", "label": "支付状态", "type": "select", "dictType": "biz_pay_status"}, {"field": "orderTime", "label": "下单时间", "type": "daterange"}]

[STAGE:columnsSchema]
[{"key": "orderNo", "title": "订单号", "dataIndex": "orderNo"}, {"key": "customerName", "title": "客户名称", "dataIndex": "customerName"}, {"key": "orderAmount", "title": "订单金额", "dataIndex": "orderAmount"}, {"key": "payStatus", "title": "支付状态", "dataIndex": "payStatus", "render": {"type": "dictTag", "dictType": "biz_pay_status"}}, {"key": "orderTime", "title": "下单时间", "dataIndex": "orderTime"}, {"key": "remark", "title": "备注", "dataIndex": "remark"}, {"key": "actions", "title": "操作", "dataIndex": "actions"}]

[STAGE:editSchema]
[{"field": "orderNo", "label": "订单号", "type": "input", "required": true}, {"field": "customerName", "label": "客户名称", "type": "input", "required": true}, {"field": "orderAmount", "label": "订单金额", "type": "number", "required": true}, {"field": "payStatus", "label": "支付状态", "type": "select", "required": true, "dictType": "biz_pay_status"}, {"field": "orderTime", "label": "下单时间", "type": "datetime", "required": true}, {"field": "remark", "label": "备注", "type": "textarea", "required": false}]

[STAGE:apiConfig]
{"list": "get@/ai/crud/biz_order/page", "detail": "get@/ai/crud/biz_order/:id", "create": "post@/ai/crud/biz_order", "update": "put@/ai/crud/biz_order", "delete": "delete@/ai/crud/biz_order/:id"}

[STAGE:dictConfig]
[{"dictType": "biz_pay_status", "dictName": "支付状态", "isNew": true, "items": [{"dictLabel": "未支付", "dictValue": "0", "dictSort": 1}, {"dictLabel": "已支付", "dictValue": "1", "dictSort": 2}, {"dictLabel": "已退款", "dictValue": "2", "dictSort": 3}]}]

[STAGE:desensitizeConfig]
{}

[STAGE:encryptConfig]
{"enableEncrypt": false, "enableDecrypt": false, "operations": []}

[STAGE:transConfig]
{"payStatus": {"dictType": "biz_pay_status", "targetField": "payStatusName"}}

[STAGE:createTableSql]
CREATE TABLE biz_order (
  id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT ''主键ID'',
  tenant_id VARCHAR(64) DEFAULT NULL COMMENT ''租户ID'',
  order_no VARCHAR(64) NOT NULL COMMENT ''订单号'',
  customer_name VARCHAR(128) NOT NULL COMMENT ''客户名称'',
  order_amount DECIMAL(10,2) NOT NULL COMMENT ''订单金额'',
  pay_status TINYINT DEFAULT 0 COMMENT ''支付状态(0:未支付,1:已支付,2:已退款)'',
  order_time DATETIME DEFAULT NULL COMMENT ''下单时间'',
  remark TEXT COMMENT ''备注'',
  create_by VARCHAR(64) DEFAULT NULL COMMENT ''创建人'',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT ''创建时间'',
  create_dept VARCHAR(64) DEFAULT NULL COMMENT ''创建部门'',
  update_by VARCHAR(64) DEFAULT NULL COMMENT ''更新人'',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT ''更新时间'',
  del_flag TINYINT DEFAULT 0 COMMENT ''删除标志(0:正常,1:删除)'',
  UNIQUE KEY uk_order_no (order_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT=''订单管理'';', NULL, '2026-04-30 21:34:55'),
       (2049846515995795458, 1, 1, 'crud_config_builder', 'session_1777556382717_ilc10t62t', 'user',
        '订单管理，包含订单号、客户名称、订单金额、支付状态、下单时间、备注', NULL, '2026-04-30 21:41:08'),
       (2049846516289396738, 1, 1, 'crud_config_builder', 'session_1777556382717_ilc10t62t', 'assistant', '【思考过程】
Here''s a thinking process:

1.  **Analyze User Input:**
    - `configKey`: `biz_order`
    - `tableName`: `biz_order`
    - `需求描述` (Requirements): 订单管理 (Order Management),包含订单号 (Order No.), 客户名称 (Customer Name), 订单金额 (Order Amount), 支付状态 (Payment Status), 下单时间 (Order Time), 备注 (Remarks)
    - Template: 标准 CRUD (Standard CRUD)
    - Constraints: columnsSchema includes content + actions, editSchema simple, modalType drawer, searchSchema 3-5 fields.

2.  **Map Fields to Schema:**
    Based on requirements and naming conventions:
 - 订单号 -> `orderNo` (String, required, search, column, edit)
    - 客户名称 -> `customerName` (String, search, column, edit)
    - 订单金额 -> `orderAmount` (BigDecimal/Number, column, edit, validation regex `^\\d+(\\.\\d{1,2})?$`)
    - 支付状态 -> `payStatus` (Integer/Select, dictType: `biz_pay_status`, search, column, edit)
    - 下单时间 -> `orderTime` (LocalDateTime/Datetime, search(daterange), column, edit)
    - 备注 -> `remark` (String/Textarea, column, edit)
    - Base fields: `id`, `tenantId`, `createBy`, `createTime`, `createDept`, `updateBy`, `updateTime`, `delFlag` (auto-handled, excluded from editSchema)

3.  **Construct `searchSchema` (3-5 fields):**
    - `orderNo` (input) - `customerName` (input)
    - `payStatus` (select, dictType: `biz_pay_status`) - `orderTime` (daterange)
    *(4 fields, fits constraint)*

4.  **Construct `columnsSchema`:**
    - `orderNo`
    - `customerName`
    - `orderAmount`
    - `payStatus` (render dictTag)
 - `orderTime`
    - `remark`
    - `actions` (key: "actions")

5.  **Construct `editSchema`:**
    - `orderNo` (input, required)
    - `customerName` (input, required)
    - `orderAmount` (number, required)
    - `payStatus` (select, required, dictType: `biz_pay_status`)
    - `orderTime` (datetime, required)
    - `remark` (textarea)
    *(Exclude base fields)*

6.  **Construct `apiConfig`:**
    - `list`: `get@/ai/crud/biz_order/page`
    - `detail`: `get@/ai/crud/biz_order/:id`
    - `create`: `post@/ai/crud/biz_order`
    - `update`: `put@/ai/crud/biz_order`
    - `delete`: `delete@/ai/crud/biz_order/:id`

7.  **Construct `dictConfig`:**
    - `biz_pay_status`: 支付状态
    - Items: 待支付(0), 已支付(1), 已取消(2), 退款中(3), 已退款(4) -> I''ll keep it simple: 未支付(0), 已支付(1), 已取消(2)    - `isNew`: true

8.  **Construct `desensitizeConfig`:**
    - `customerName` -> NAME
    - `orderNo` -> CUSTOM? Or maybe not needed. I''ll skip unless obvious. Actually, phone/email not present. I''ll leave it empty or just map `customerName` to NAME if needed, but it''s optional. I''ll provide an empty object `{}` or skip sensitive fields if none are strictly sensitive. Let''s stick to `{}` or omit if not specified. The prompt says "自动识别敏感字段", but none are explicitly sensitive except maybe `customerName`. I''ll leave it `{}`.

9.  **Construct `encryptConfig`:**
    - `enableEncrypt`: false
    - `enableDecrypt`: false
    - `operations`: []

10. **Construct `transConfig`:**
    - `payStatus`: `dictType`: `biz_pay_status`, `targetField`: `payStatusName`

11. **Construct `createTableSql`:**
    - Standard MySQL with base fields.
 - `id` BIGINT AUTO_INCREMENT PRIMARY KEY
    - `order_no` VARCHAR(64) - `customer_name` VARCHAR(100)
    - `order_amount` DECIMAL(10,2)
    - `pay_status` TINYINT DEFAULT 0
    - `order_time` DATETIME
    - `remark` VARCHAR(500)
    - Base fields: `tenant_id`, `create_by`, `create_time`, `create_dept`, `update_by`, `update_time`, `del_flag`
    - Engine InnoDB, Charset utf8mb4.

12. **Format Output according to SPEC:**
    Need to strictly follow the `[STAGE:xxx]` format.
    Check constraints:
    - `searchSchema`: field, label, type, optional dictType.
    - `columnsSchema`: key, title, dataIndex, optional render.
    - `editSchema`: field, label, type, required, optional dictType/props.
    - `apiConfig`: method@url format.
    - JSON only per stage.
    - camelCase for fields.

    Let''s draft carefully.

    `[STAGE:meta]`
    `{"configKey": "biz_order", "tableName": "biz_order", "tableComment": "订单管理"}`

    `[STAGE:searchSchema]`
    `[{"field":"orderNo","label":"订单号","type":"input"},{"field":"customerName","label":"客户名称","type":"input"},{"field":"payStatus","label":"支付状态","type":"select","dictType":"biz_pay_status"},{"field":"orderTime","label":"下单时间","type":"daterange"}]`

    `[STAGE:columnsSchema]`
    `[{"key":"orderNo","title":"订单号","dataIndex":"orderNo"},{"key":"customerName","title":"客户名称","dataIndex":"customerName"},{"key":"orderAmount","title":"订单金额","dataIndex":"orderAmount"},{"key":"payStatus","title":"支付状态","dataIndex":"payStatus","render":{"type":"dictTag","dictType":"biz_pay_status"}},{"key":"orderTime","title":"下单时间","dataIndex":"orderTime"},{"key":"remark","title":"备注","dataIndex":"remark"},{"key":"actions","title":"操作","dataIndex":"actions"}]`

    `[STAGE:editSchema]`
    `[{"field":"orderNo","label":"订单号","type":"input","required":true},{"field":"customerName","label":"客户名称","type":"input","required":true},{"field":"orderAmount","label":"订单金额","type":"number","required":true,"props":{"precision":2}},{"field":"payStatus","label":"支付状态","type":"select","required":true,"dictType":"biz_pay_status"},{"field":"orderTime","label":"下单时间","type":"datetime","required":true},{"field":"remark","label":"备注","type":"textarea","required":false}]`

    `[STAGE:apiConfig]`
    `{"list":"get@/ai/crud/biz_order/page","detail":"get@/ai/crud/biz_order/:id","create":"post@/ai/crud/biz_order","update":"put@/ai/crud/biz_order","delete":"delete@/ai/crud/biz_order/:id"}`

    `[STAGE:dictConfig]`
    `[{"dictType":"biz_pay_status","dictName":"支付状态","isNew":true,"items":[{"dictLabel":"未支付","dictValue":"0","dictSort":1},{"dictLabel":"已支付","dictValue":"1","dictSort":2},{"dictLabel":"已取消","dictValue":"2","dictSort":3}]}]`

    `[STAGE:desensitizeConfig]`
    `{}` (or maybe `{"customerName":{"type":"NAME"}}`? I''ll stick to `{}` as it''s safer unless explicitly requested, but spec says "自动识别". I''ll leave it empty or add customerName. Actually, spec says key is field name. I''ll output `{}`.)

    `[STAGE:encryptConfig]`
    `{"enableEncrypt":false,"enableDecrypt":false,"operations":[]}`

    `[STAGE:transConfig]`
    `{"payStatus":{"dictType":"biz_pay_status","targetField":"payStatusName"}}`

    `[STAGE:createTableSql]`
    `CREATE TABLE biz_order ( id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT ''主键ID'', tenant_id BIGINT COMMENT ''租户ID'', order_no VARCHAR(64) NOT NULL COMMENT ''订单号'', customer_name VARCHAR(100) COMMENT ''客户名称'', order_amount DECIMAL(10,2) COMMENT ''订单金额'', pay_status TINYINT DEFAULT 0 COMMENT ''支付状态'', order_time DATETIME COMMENT ''下单时间'', remark VARCHAR(500) COMMENT ''备注'', create_by VARCHAR(64) COMMENT ''创建者'', create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT ''创建时间'', create_dept VARCHAR(64) COMMENT ''创建部门'', update_by VARCHAR(64) COMMENT ''更新者'', update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT ''更新时间'', del_flag TINYINT DEFAULT 0 COMMENT ''删除标志'' ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT=''订单管理'';`

    Check constraints:
    - `searchSchema` type options: input/select/daterange/number. Used correctly.
    - `editSchema` type options: input/textarea/select/radio/switch/date/datetime/number/upload/imageUpload/fileUpload/cascader/treeSelect. Used correctly.
 - Base fields excluded from editSchema. Checked.
    - `dictType` on select fields. Checked.
    - `apiConfig` format matches exactly.
    - `columnsSchema` has actions. Checked.
    - `modalType` constraint mentioned in prompt but not in output format. I''ll ignore unless it''s part of meta, but spec says meta only has configKey, tableName, tableComment. I''ll stick to spec.
 - JSON only per stage. Checked.
    - camelCase for fields. Checked.    One minor thing: `orderAmount` validation regex `^\\d+(\\.\\d{1,2})?$` is mentioned in spec for amount fields. I should add it to `editSchema` props if possible, or just rely on type="number". The spec says `props` is optional. I''ll add `props: { pattern: "^\\\\d+(\\\\.\\\\d{1,2})?$" }` or just keep it simple. I''ll stick to `type: "number"`.

    Let''s verify `apiConfig` keys: `list`, `detail`, `create`, `update`, `delete`. Matches spec.
    `dictConfig` items: `dictLabel`, `dictValue`, `dictSort`. Matches spec.
    `transConfig` format: `{"payStatus":{"dictType":"biz_pay_status","targetField":"payStatusName"}}`. Matches spec.
    `desensitizeConfig`: `{}`. Matches spec.
    `encryptConfig`: `{"enableEncrypt":false,"enableDecrypt":false,"operations":[]}`. Matches spec.
    `createTableSql`: Standard MySQL. Includes base fields. Matches spec.

    All looks solid. I will generate the output strictly following the format.
    One detail: `searchSchema` type `daterange` is valid.
    `editSchema` type `datetime` is valid.
    I will ensure JSON is valid and properly escaped if needed. I''ll output exactly as requested.
    Ready.
    Self-Correction/Refinement during thought:
    - Check `apiConfig` format in spec: `list"："get@/ai/crud/" + configKey + "/page"` -> The quotes in spec are weird (`list"："get@...`), but it clearly means `"list": "get@/ai/crud/biz_order/page"`.
    - `columnsSchema` render: `{"type":"dictTag","dictType":"biz_pay_status"}` is correct.
    - `editSchema` props for amount: I''ll add `props: {"precision": 2}` for better UX.
    - `desensitizeConfig`: I''ll leave it `{}` as no explicit phone/email/id is present.
    - `transConfig`: `{"payStatus": {"dictType": "biz_pay_status", "targetField": "payStatusName"}}`
    - `createTableSql`: Ensure `tenant_id` etc. are present.
    - All JSON arrays/objects must be valid.

    Output matches exactly. Proceeds.
    Generating response.
    [STAGE:meta]
    {"configKey":"biz_order","tableName":"biz_order","tableComment":"订单管理"}
    [STAGE:searchSchema]
    [{"field":"orderNo","label":"订单号","type":"input"},{"field":"customerName","label":"客户名称","type":"input"},{"field":"payStatus","label":"支付状态","type":"select","dictType":"biz_pay_status"},{"field":"orderTime","label":"下单时间","type":"daterange"}]
    [STAGE:columnsSchema]
    [{"key":"orderNo","title":"订单号","dataIndex":"orderNo"},{"key":"customerName","title":"客户名称","dataIndex":"customerName"},{"key":"orderAmount","title":"订单金额","dataIndex":"orderAmount"},{"key":"payStatus","title":"支付状态","dataIndex":"payStatus","render":{"type":"dictTag","dictType":"biz_pay_status"}},{"key":"orderTime","title":"下单时间","dataIndex":"orderTime"},{"key":"remark","title":"备注","dataIndex":"remark"},{"key":"actions","title":"操作","dataIndex":"actions"}]
    [STAGE:editSchema]
    [{"field":"orderNo","label":"订单号","type":"input","required":true},{"field":"customerName","label":"客户名称","type":"input","required":true},{"field":"orderAmount","label":"订单金额","type":"number","required":true,"props":{"precision":2}},{"field":"payStatus","label":"支付状态","type":"select","required":true,"dictType":"biz_pay_status"},{"field":"orderTime","label":"下单时间","type":"datetime","required":true},{"field":"remark","label":"备注","type":"textarea","required":false}] [STAGE:apiConfig]
    {"list":"get@/ai/crud/biz_order/page","detail":"get@/ai/crud/biz_order/:id","create":"post@/ai/crud/biz_order","update":"put@/ai/crud/biz_order","delete":"delete@/ai/crud/biz_order/:id"}
    [STAGE:dictConfig]
    [{"dictType":"biz_pay_status","dictName":"支付状态","isNew":true,"items":[{"dictLabel":"未支付","dictValue":"0","dictSort":1},{"dictLabel":"已支付","dictValue":"1","dictSort":2},{"dictLabel":"已取消","dictValue":"2","dictSort":3}]}]
    [STAGE:desensitizeConfig]
    {}
    [STAGE:encryptConfig]
    {"enableEncrypt":false,"enableDecrypt":false,"operations":[]}
    [STAGE:transConfig]
    {"payStatus":{"dictType":"biz_pay_status","targetField":"payStatusName"}}
    [STAGE:createTableSql]
    CREATE TABLE biz_order ( id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT ''主键ID'', tenant_id BIGINT COMMENT ''租户ID'', order_no VARCHAR(64) NOT NULL COMMENT ''订单号'', customer_name VARCHAR(100) COMMENT ''客户名称'', order_amount DECIMAL(10,2) COMMENT ''订单金额'', pay_status TINYINT DEFAULT 0 COMMENT ''支付状态'', order_time DATETIME COMMENT ''下单时间'', remark VARCHAR(500) COMMENT ''备注'', create_by VARCHAR(64) COMMENT ''创建者'', create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT ''创建时间'', create_dept VARCHAR(64) COMMENT ''创建部门'', update_by VARCHAR(64) COMMENT ''更新者'', update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT ''更新时间'', del_flag TINYINT DEFAULT 0 COMMENT ''删除标志'' ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT=''订单管理'';
    *(Self-Correction/Verification during drafting)*
    - Check `searchSchema` types: `input`, `input`, `select`, `daterange`. Valid.
    - Check `editSchema` types: `input`, `input`, `number`, `select`, `datetime`, `textarea`. Valid.
    - Check `columnsSchema` last item: `{"key":"actions","title":"操作","dataIndex":"actions"}`. Valid.
    - Check `apiConfig` format: matches exactly.
    - Check `dictConfig` structure: matches.
    - Check `createTableSql`: includes all base fields, correct types, utf8mb4, InnoDB.
    - All constraints met. Output matches spec exactly. No markdown blocks. Direct text.
    Proceeds.
    Output Generation.
    *(Note: I will ensure the JSON is strictly valid and follows the exact stage markers requested.)*
    All good.
    [Output Generation] -> *Proceeds*


【回复内容】
[STAGE:meta]
{"configKey":"biz_order","tableName":"biz_order","tableComment":"订单管理"}

[STAGE:searchSchema]
[{"field":"orderNo","label":"订单号","type":"input"},{"field":"customerName","label":"客户名称","type":"input"},{"field":"payStatus","label":"支付状态","type":"select","dictType":"biz_pay_status"},{"field":"orderTime","label":"下单时间","type":"daterange"}]

[STAGE:columnsSchema]
[{"key":"orderNo","title":"订单号","dataIndex":"orderNo"},{"key":"customerName","title":"客户名称","dataIndex":"customerName"},{"key":"orderAmount","title":"订单金额","dataIndex":"orderAmount"},{"key":"payStatus","title":"支付状态","dataIndex":"payStatus","render":{"type":"dictTag","dictType":"biz_pay_status"}},{"key":"orderTime","title":"下单时间","dataIndex":"orderTime"},{"key":"remark","title":"备注","dataIndex":"remark"},{"key":"actions","title":"操作","dataIndex":"actions"}]

[STAGE:editSchema]
[{"field":"orderNo","label":"订单号","type":"input","required":true},{"field":"customerName","label":"客户名称","type":"input","required":true},{"field":"orderAmount","label":"订单金额","type":"number","required":true,"props":{"precision":2}},{"field":"payStatus","label":"支付状态","type":"select","required":true,"dictType":"biz_pay_status"},{"field":"orderTime","label":"下单时间","type":"datetime","required":true},{"field":"remark","label":"备注","type":"textarea","required":false}]

[STAGE:apiConfig]
{"list":"get@/ai/crud/biz_order/page","detail":"get@/ai/crud/biz_order/:id","create":"post@/ai/crud/biz_order","update":"put@/ai/crud/biz_order","delete":"delete@/ai/crud/biz_order/:id"}

[STAGE:dictConfig]
[{"dictType":"biz_pay_status","dictName":"支付状态","isNew":true,"items":[{"dictLabel":"未支付","dictValue":"0","dictSort":1},{"dictLabel":"已支付","dictValue":"1","dictSort":2},{"dictLabel":"已取消","dictValue":"2","dictSort":3}]}]

[STAGE:desensitizeConfig]
{}

[STAGE:encryptConfig]
{"enableEncrypt":false,"enableDecrypt":false,"operations":[]}

[STAGE:transConfig]
{"payStatus":{"dictType":"biz_pay_status","targetField":"payStatusName"}}

[STAGE:createTableSql]
CREATE TABLE biz_order ( id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT ''主键ID'', tenant_id BIGINT COMMENT ''租户ID'', order_no VARCHAR(64) NOT NULL COMMENT ''订单号'', customer_name VARCHAR(100) COMMENT ''客户名称'', order_amount DECIMAL(10,2) COMMENT ''订单金额'', pay_status TINYINT DEFAULT 0 COMMENT ''支付状态'', order_time DATETIME COMMENT ''下单时间'', remark VARCHAR(500) COMMENT ''备注'', create_by VARCHAR(64) COMMENT ''创建者'', create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT ''创建时间'', create_dept VARCHAR(64) COMMENT ''创建部门'', update_by VARCHAR(64) COMMENT ''更新者'', update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT ''更新时间'', del_flag TINYINT DEFAULT 0 COMMENT ''删除标志'' ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT=''订单管理'';',
        NULL, '2026-04-30 21:41:08'),
       (2049847087071256577, 1, 1, 'crud_config_builder', 'session_1777556382717_ilc10t62t', 'user',
        '增加产品价格在搜索栏', NULL, '2026-04-30 21:43:25'),
       (2049847087348080642, 1, 1, 'crud_config_builder', 'session_1777556382717_ilc10t62t', 'assistant', '[STAGE:meta]
{"configKey":"biz_order","tableName":"biz_order","tableComment":"订单管理"}

[STAGE:searchSchema]
[{"field":"orderNo","label":"订单号","type":"input"},{"field":"customerName","label":"客户名称","type":"input"},{"field":"payStatus","label":"支付状态","type":"select","dictType":"biz_pay_status"},{"field":"orderTime","label":"下单时间","type":"daterange"}]

[STAGE:columnsSchema]
[{"key":"orderNo","title":"订单号","dataIndex":"orderNo"},{"key":"customerName","title":"客户名称","dataIndex":"customerName"},{"key":"orderAmount","title":"订单金额","dataIndex":"orderAmount"},{"key":"payStatus","title":"支付状态","dataIndex":"payStatus","render":{"type":"dictTag","dictType":"biz_pay_status"}},{"key":"orderTime","title":"下单时间","dataIndex":"orderTime"},{"key":"remark","title":"备注","dataIndex":"remark"},{"key":"actions","title":"操作","dataIndex":"actions"}]

[STAGE:editSchema]
[{"field":"orderNo","label":"订单号","type":"input","required":true},{"field":"customerName","label":"客户名称","type":"input","required":true},{"field":"orderAmount","label":"订单金额","type":"number","required":true,"props":{"precision":2}},{"field":"payStatus","label":"支付状态","type":"select","required":true,"dictType":"biz_pay_status"},{"field":"orderTime","label":"下单时间","type":"datetime","required":true},{"field":"remark","label":"备注","type":"textarea","required":false}]

[STAGE:apiConfig]
{"list":"get@/ai/crud/biz_order/page","detail":"get@/ai/crud/biz_order/:id","create":"post@/ai/crud/biz_order","update":"put@/ai/crud/biz_order","delete":"delete@/ai/crud/biz_order/:id"}

[STAGE:dictConfig]
[{"dictType":"biz_pay_status","dictName":"支付状态","isNew":true,"items":[{"dictLabel":"未支付","dictValue":"0","dictSort":1},{"dictLabel":"已支付","dictValue":"1","dictSort":2},{"dictLabel":"已取消","dictValue":"2","dictSort":3}]}]

[STAGE:desensitizeConfig]
{}

[STAGE:encryptConfig]
{"enableEncrypt":false,"enableDecrypt":false,"operations":[]}

[STAGE:transConfig]
{"payStatus":{"dictType":"biz_pay_status","targetField":"payStatusName"}}

[STAGE:createTableSql]
CREATE TABLE biz_order ( id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT ''主键ID'', tenant_id BIGINT COMMENT ''租户ID'', order_no VARCHAR(64) NOT NULL COMMENT ''订单号'', customer_name VARCHAR(100) COMMENT ''客户名称'', order_amount DECIMAL(10,2) COMMENT ''订单金额'', pay_status TINYINT DEFAULT 0 COMMENT ''支付状态'', order_time DATETIME COMMENT ''下单时间'', remark VARCHAR(500) COMMENT ''备注'', create_by VARCHAR(64) COMMENT ''创建者'', create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT ''创建时间'', create_dept VARCHAR(64) COMMENT ''创建部门'', update_by VARCHAR(64) COMMENT ''更新者'', update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT ''更新时间'', del_flag TINYINT DEFAULT 0 COMMENT ''删除标志'' ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT=''订单管理'';',
        NULL, '2026-04-30 21:43:25');
INSERT INTO ai_chat_session (id, tenant_id, user_id, agent_code, session_name, status, create_time, update_time,
                             metadata)
VALUES ('f35a767b-7b0f-4802-aa00-457efafb58c2', 1, 1, 'dashboard_generator', '销售数据大屏，先做一个饼状图展示销售数据

当前项目：销售数据可视化大屏

当前画布已有内容（请在此…', '1', '2026-04-16 10:49:31', '2026-04-21 11:06:02', NULL),
       ('f4375db7-cd10-407a-a645-fd5fd2de5e55', 1, 1, 'dashboard_generator', '帮我增加一个可以判断销售数据的饼状图+折线图

当前项目：销售数据可视化大屏

当前画布已有内容（请…', '1', '2026-04-16 10:55:29', '2026-04-21 10:58:23', NULL),
       ('session_1776747312761_kdvupglex', 1, 1, 'crud_config_builder', '根据以下信息生成CRUD配置JSON：

configKey: biz_order
tableNam…', '0', '2026-04-21 12:58:14', '2026-04-21 12:58:14', NULL),
       ('session_1776756566619_srp4e461a', 1, 1, 'crud_config_builder', '根据以下信息生成CRUD配置JSON：

configKey: biz_order
tableNam…', '0', '2026-04-21 15:29:27', '2026-04-21 15:30:41', NULL),
       ('session_1776756704238_hod7nesy2', 1, 1, 'crud_config_builder', '根据以下信息生成CRUD配置JSON：

configKey: order
tableName: o…', '0', '2026-04-21 15:31:45', '2026-04-21 15:31:45', NULL),
       ('session_1776757442068_wggwvw50m', 1, 1, 'crud_config_builder', '根据以下信息生成CRUD配置JSON：

configKey: order
tableName: o…', '0', '2026-04-21 15:44:48', '2026-04-21 15:46:08', NULL),
       ('session_1776757666909_7udjubkdw', 1, 1, 'crud_config_builder', '根据以下信息生成CRUD配置JSON：

configKey: order
tableName: o…', '0', '2026-04-21 15:47:48', '2026-04-21 15:49:08', NULL),
       ('session_1776759135433_h417mddxv', 1, 1, 'crud_config_builder', '根据以下信息生成CRUD配置JSON：

configKey: order
tableName: o…', '0', '2026-04-21 16:12:16', '2026-04-21 16:12:16', NULL),
       ('session_1776759385931_7nm1hml5v', 1, 1, 'crud_config_builder', '根据以下信息生成CRUD配置JSON：

configKey: order
tableName: o…', '0', '2026-04-21 16:16:26', '2026-04-21 16:17:45', NULL),
       ('session_1776759937586_dydqfarx8', 1, 1, 'crud_config_builder', '订单管理', '1', '2026-04-21 16:25:39',
        '2026-04-21 18:33:42', NULL);
INSERT INTO ai_chat_session (id, tenant_id, user_id, agent_code, session_name, status, create_time, update_time,
                             metadata)
VALUES ('session_1776767605200_1qp8rzz2p', 1, 1, 'crud_config_builder', '订单管理页面
', '1', '2026-04-21 18:35:29', '2026-04-22 11:45:31', NULL),
       ('session_1776771097009_35ilx1xck', 1, 1, 'crud_config_builder', '订单管理', '1', '2026-04-21 19:31:37',
        '2026-04-22 10:43:14', '{"configKey":"biz_order","tableName":"biz_order","description":"订单管理"}'),
       ('session_1776829741539_duoq4dqwg', 1, 1, 'crud_config_builder', '帮我实现一个订单管理页面，包含订单核心参数',
        '0', '2026-04-22 11:49:03', '2026-04-22 11:49:03',
        '{"configKey":"order_manage","tableName":"order_manage","description":"订单管理表"}'),
       ('session_1776833314002_gc50bbkv8', 1, 1, 'crud_config_builder', '帮我实现订单管理相关功能', '0',
        '2026-04-22 12:48:35', '2026-04-22 12:48:35',
        '{"configKey":"order_manage","tableName":"order_manage","description":"订单管理表"}'),
       ('session_1777195540937_lgxbcl431', 1, 1, 'crud_config_builder',
        '员工管理，包含姓名、工号、部门、职位、入职日期、手机号、邮箱、状态', '0', '2026-04-26 17:25:42', '2026-04-26 17:27:39',
        '{"configKey":"employee","tableName":"sys_employee","description":"员工信息表"}'),
       ('session_1777197900986_a4h0w6t4u', 1, 1, 'crud_config_builder',
        '产品管理，包含产品名称、分类、价格、库存、状态、创建时间', '0', '2026-04-26 18:05:02', '2026-04-26 18:07:16',
        '{"configKey":"biz_product","tableName":"biz_product","description":"产品管理表"}'),
       ('session_1777297363598_9e5hykk7h', 1, 1, 'crud_config_builder', '你好', '0', '2026-04-27 21:42:56',
        '2026-04-27 21:42:56', NULL),
       ('session_1777297448667_wcykofoap', 1, 1, 'crud_config_builder',
        '订单管理，包含订单号、客户名称、订单金额、支付状态、下单时间、备注', '0', '2026-04-27 21:44:09',
        '2026-04-27 21:44:58',
        '{"configKey":"biz_order","tableName":"biz_order","description":"订单管理，包含订单号、客户名称、订单金额、支付状态、下单时间、备注","providerId":"2044308970805420033","modelId":"2045845428322123777"}'),
       ('session_1777554136235_3slajqloa', 1, 1, 'crud_config_builder',
        '产品管理，包含产品名称、分类、价格、库存、状态、创建时间', '0', '2026-04-30 21:02:17', '2026-04-30 21:15:44',
        '{"configKey":"biz_product","tableName":"biz_product","description":"你好","providerId":"2044308970805420033","modelId":"2045806046227730433"}'),
       ('session_1777555994214_0ms0loy7q', 1, 1, 'crud_config_builder',
        '订单管理，包含订单号、客户名称、订单金额、支付状态、下单时间、备注', '0', '2026-04-30 21:33:15',
        '2026-04-30 21:33:15',
        '{"configKey":"biz_order","tableName":"biz_order","description":"订单管理，包含订单号、客户名称、订单金额、支付状态、下单时间、备注","providerId":"2044308970805420033","modelId":"2045806046227730433"}');
INSERT INTO ai_chat_session (id, tenant_id, user_id, agent_code, session_name, status, create_time, update_time,
                             metadata)
VALUES ('session_1777556382717_ilc10t62t', 1, 1, 'crud_config_builder',
        '订单管理，包含订单号、客户名称、订单金额、支付状态、下单时间、备注', '0', '2026-04-30 21:39:43',
        '2026-04-30 21:43:25',
        '{"configKey":"biz_order","tableName":"biz_order","description":"增加产品价格在搜索栏","providerId":"2044308970805420033","modelId":"2045845428322123777"}');
INSERT INTO ai_context_config (id, tenant_id, agent_code, config_name, config_content, config_type, sort, status,
                               create_by, create_time, create_dept, update_by, update_time)
VALUES (2, 1, 'crud_config_builder', '工程编码规范', '# ForgeAdmin 项目编码规范

## 命名规范
- 表名：小写字母+下划线，如 `sys_user`、`biz_order`
- Java 类名：大驼峰，如 `SysUser`、`BizOrder`
- Java 字段名：小驼峰，如 `userName`、`orderAmount`
- 方法名：小驼峰，如 `getUserById`、`createOrder`

## 组件映射偏好
- 短文本输入（≤255字符）→ INPUT
- 长文本（>255字符）→ TEXTAREA
- 状态/类型等有限枚举 → SELECT
- 布尔选择 → RADIO
- 日期时间 → DATETIME
- 图片上传 → IMAGE
- 文件上传 → FILE
- 富文本 → EDITOR

## 验证规则偏好
- 手机号：正则 `^1[3-9]\\d{9}$`
- 邮箱：正则标准格式
- 必填字段：required=true
- 金额字段：正则 `^\\d+(\\.\\d{1,2})?$`
- 身份证号：正则18位

## Java 类型映射
- int/smallint → Integer
- bigint → Long
- varchar/char → String
- decimal/numeric → BigDecimal
- datetime/timestamp → LocalDateTime
- date → LocalDate
- tinyint(1) → Boolean

## 基类字段（自动包含，不需要用户指定）
- id, tenant_id, create_by, create_time, create_dept, update_by, update_time

## 字典类型推断
- 字段名含 status/type/category/level → 优先推荐 dictType
- 字段注释含括号枚举 (1:正常,0:停用) → 自动识别为字典
', 'RULE', 1, '0', NULL, '2026-04-22 08:45:05', NULL, NULL, '2026-04-22 08:45:05'),
       (3, 1, 'crud_config_builder', 'CRUD配置输出格式规范', '## 输出格式要求
请严格按以下格式分段输出，每个阶段前加标记：

[STAGE:meta]
{"configKey": "xxx", "tableName": "xxx", "tableComment": "xxx"}

[STAGE:searchSchema]
搜索表单配置 JSON 数组

[STAGE:columnsSchema]
表格列配置 JSON 数组

[STAGE:editSchema]
编辑表单配置 JSON 数组

[STAGE:apiConfig]
API配置 JSON 对象

[STAGE:dictConfig]
字典数据配置 JSON 数组，每项包含 dictType, dictName, isNew, items[]

[STAGE:desensitizeConfig]
字段脱敏配置 JSON 对象，key 为字段名，value 包含 type(PHONE/ID_CARD/EMAIL/BANK_CARD/NAME/ADDRESS/PASSWORD/CAR_LICENSE/CUSTOM)

[STAGE:encryptConfig]
接口加解密配置 JSON 对象，包含 enableEncrypt, enableDecrypt, operations[]

[STAGE:transConfig]
字典翻译配置 JSON 对象，key 为字段名，value 包含 dictType, targetField

[STAGE:createTableSql]
CREATE TABLE 语句（如没有关联表或表不存在，请生成建表SQL）

## 字段规范

### meta
- configKey: 小写字母开头，仅含小写字母+数字+下划线
- tableName: 数据库表名，小写，下划线分隔
- tableComment: 表的中文描述

### searchSchema
- 每项包含: field(字段名 camelCase), label(中文标签), type(input/select/daterange/number)
- 有字典的 select 字段必须加 dictType 属性
- 只选择最常用的 3-5 个搜索条件

### columnsSchema
- 每项包含: key(camelCase), title(中文标签), dataIndex(同 key)
- 有字典的列加 render: {type: "dictTag", dictType: "xxx"}

### editSchema
- 每项包含: field(camelCase), label(中文标签), type(input/textarea/select/radio/switch/date/datetime/number), required(boolean)
- 有字典的字段加 dictType 属性
- 基类字段不包含: id, tenantId, createBy, createTime, createDept, updateBy, updateTime, delFlag

### apiConfig
- 格式为 method@url
- 必须包含:
list(分页)、detail(详情)、create(新廿)、update(编辑)、delete(删除)
list"："get@/ai/crud/" + configKey + "/page"
detail"： "get@/ai/crud/" + configKey + "/:id")
create"："post@/ai/crud/" + configKey)
update"："put@/ai/crud/" + configKey)
delete"："delete@/ai/crud/" + configKey + "/:id")

### dictConfig
- 从 schema 中的 dictType 提取，生成字典类型和字典项
- isNew=true 表示需要新建到字典表；isNew=false 表示映射已有字典
- items 中每项包含 dictLabel, dictValue, dictSort

### desensitizeConfig
- 自动识别敏感字段并生成对应脱敏规则
- PHONE(手机号): 186****2213
- ID_CARD(身份证): 110101********0011
- EMAIL(邮箱): t***@example.com
- BANK_CARD(银行卡): 6222 **** **** 8888
- NAME(姓名): 张**
- ADDRESS(地址): 北京市海淀区******
- PASSWORD(密码): ********
- CUSTOM(自定义): 需指定 prefixKeep 和 suffixKeep

### encryptConfig
- enableEncrypt: 是否启用响应加密
- enableDecrypt: 是否启用请求解密
- operations: 需要加解密的操作列表，空数组表示全部 ["list","detail","create","update","delete"]

### transConfig
- key 为原始字段名（如 status）
- dictType: 对应字典类型
- targetField: 翻译后的字段名（如 statusName），默认 原字段名 + Name

### createTableSql
- 生成标准 MySQL CREATE TABLE 语句
- 必须包含基类字段: id(主键自增), tenant_id, create_by, create_time, create_dept, update_by, update_time, del_flag
- 使用 utf8mb4 字符集和 InnoDB 引擎

## 重要约束
- 直接输出纯 JSON/SQL，不要用 markdown 代码块包裹
- 不要输出任何解释性文字，只输出 [STAGE:xxx] 标记和内容
- 每个 STAGE 后面紧跟该段的内容，不要包在外层对象中
- field/key/dataIndex 使用 camelCase 命名', 'SPEC', 1, '0', NULL, '2026-04-22 11:42:32', NULL, NULL,
        '2026-04-23 15:12:20');
INSERT INTO ai_crud_config (id, tenant_id, config_key, table_name, table_comment, search_schema, columns_schema,
                            edit_schema, api_config, `options`, mode, status, menu_name, menu_parent_id, menu_sort,
                            menu_resource_id, dict_config, desensitize_config, encrypt_config, trans_config,
                            layout_type, create_by, create_time, create_dept, update_by, update_time)
VALUES (2048333908904689665, 1, 'employee', 'sys_employee', '员工信息表', '[
  {
    "type": "input",
    "field": "empName",
    "label": "姓名"
  },
  {
    "type": "input",
    "field": "empNo",
    "label": "工号"
  },
  {
    "type": "select",
    "field": "deptId",
    "label": "部门",
    "dictType": "sys_dept"
  },
  {
    "type": "select",
    "field": "status",
    "label": "状态",
    "dictType": "sys_normal_disable"
  }
]', '[
  {
    "key": "empName",
    "title": "姓名",
    "dataIndex": "empName"
  },
  {
    "key": "empNo",
    "title": "工号",
    "dataIndex": "empNo"
  },
  {
    "key": "deptId",
    "title": "部门",
    "render": {
      "type": "dictTag",
      "dictType": "sys_dept"
    },
    "dataIndex": "deptId"
  },
  {
    "key": "position",
    "title": "职位",
    "dataIndex": "position"
  },
  {
    "key": "hireDate",
    "title": "入职日期",
    "dataIndex": "hireDate"
  },
  {
    "key": "phone",
    "title": "手机号",
    "dataIndex": "phone"
  },
  {
    "key": "email",
    "title": "邮箱",
    "dataIndex": "email"
  },
  {
    "key": "status",
    "title": "状态",
    "render": {
      "type": "dictTag",
      "dictType": "sys_normal_disable"
    },
    "dataIndex": "status"
  }
]', '[
  {
    "type": "input",
    "field": "empName",
    "label": "姓名",
    "required": true
  },
  {
    "type": "input",
    "field": "empNo",
    "label": "工号",
    "required": true
  },
  {
    "type": "select",
    "field": "deptId",
    "label": "部门",
    "dictType": "sys_dept",
    "required": true
  },
  {
    "type": "input",
    "field": "position",
    "label": "职位",
    "required": true
  },
  {
    "type": "date",
    "field": "hireDate",
    "label": "入职日期",
    "required": true
  },
  {
    "type": "input",
    "field": "phone",
    "label": "手机号",
    "props": {
      "rules": [
        {
          "message": "请输入正确的手机号",
          "pattern": "^1[3-9]\\\\d{9}$"
        }
      ]
    },
    "required": true
  },
  {
    "type": "input",
    "field": "email",
    "label": "邮箱",
    "props": {
      "rules": [
        {
          "type": "email",
          "message": "请输入正确的邮箱"
        }
      ]
    },
    "required": true
  },
  {
    "type": "select",
    "field": "status",
    "label": "状态",
    "dictType": "sys_normal_disable",
    "required": true
  }
]', '{
  "list": "get@/ai/crud/employee/page",
  "create": "post@/ai/crud/employee",
  "delete": "delete@/ai/crud/employee/:id",
  "detail": "get@/ai/crud/employee/:id",
  "update": "put@/ai/crud/employee"
}', NULL, 'CONFIG', '0', '员工管理测试', 9067, 0, 9089, '[
  {
    "items": [
      {
        "dictSort": 1,
        "dictLabel": "研发部",
        "dictValue": "1",
        "dictStatus": 1
      },
      {
        "dictSort": 2,
        "dictLabel": "市场部",
        "dictValue": "2",
        "dictStatus": 1
      },
      {
        "dictSort": 3,
        "dictLabel": "人事部",
        "dictValue": "3",
        "dictStatus": 1
      }
    ],
    "dictName": "部门",
    "dictType": "sys_dept"
  },
  {
    "items": [
      {
        "dictSort": 1,
        "dictLabel": "正常",
        "dictValue": "1",
        "dictStatus": 1
      },
      {
        "dictSort": 2,
        "dictLabel": "停用",
        "dictValue": "0",
        "dictStatus": 1
      }
    ],
    "dictName": "通用状态",
    "dictType": "sys_normal_disable"
  }
]', '{
  "email": {
    "type": "EMAIL",
    "label": "邮箱"
  },
  "phone": {
    "type": "PHONE",
    "label": "手机号"
  },
  "empName": {
    "type": "NAME",
    "label": "姓名"
  },
  "deptIdName": {
    "type": "NAME",
    "label": "部门"
  }
}', '{
  "operations": [],
  "enableDecrypt": false,
  "enableEncrypt": false
}', '{
  "deptId": {
    "dictType": "sys_dept",
    "targetField": "deptIdName"
  },
  "status": {
    "dictType": "sys_normal_disable",
    "targetField": "statusName"
  }
}', 'simple-crud', 1, '2026-04-26 17:30:35', 2, 1, '2026-04-26 20:16:21'),
       (2048343313050144769, 1, 'biz_product', 'biz_product', '产品管理表', '[
         {
           "type": "input",
           "field": "productName",
           "label": "产品名称"
         },
         {
           "type": "select",
           "field": "category",
           "label": "分类",
           "dictType": "product_category"
         },
         {
           "type": "select",
           "field": "status",
           "label": "状态",
           "dictType": "common_status"
         },
         {
           "type": "daterange",
           "field": "createTime",
           "label": "创建时间"
         }
       ]', '[
         {
           "key": "productName",
           "title": "产品名称",
           "dataIndex": "productName"
         },
         {
           "key": "category",
           "title": "分类",
           "render": {
             "type": "dictTag",
             "dictType": "product_category"
           },
           "dataIndex": "category"
         },
         {
           "key": "price",
           "title": "价格",
           "dataIndex": "price"
         },
         {
           "key": "stock",
           "title": "库存",
           "dataIndex": "stock"
         },
         {
           "key": "status",
           "title": "状态",
           "render": {
             "type": "dictTag",
             "dictType": "common_status"
           },
           "dataIndex": "status"
         },
         {
           "key": "parentName",
           "title": "父级名称",
           "dataIndex": "parentName"
         },
         {
           "key": "createTime",
           "title": "创建时间",
           "dataIndex": "createTime"
         }
       ]', '[
         {
           "type": "input",
           "field": "productName",
           "label": "产品名称",
           "required": true
         },
         {
           "type": "treeSelect",
           "field": "parentId",
           "label": "上级产品",
           "required": false
         },
         {
           "type": "select",
           "field": "category",
           "label": "分类",
           "dictType": "product_category",
           "required": true
         },
         {
           "type": "number",
           "field": "price",
           "label": "价格",
           "required": true
         },
         {
           "type": "number",
           "field": "stock",
           "label": "库存",
           "required": true
         },
         {
           "type": "select",
           "field": "status",
           "label": "状态",
           "dictType": "common_status",
           "required": true
         },
         {
           "type": "number",
           "field": "sort",
           "label": "排序",
           "required": false
         }
       ]', '{
         "list": "get@/ai/crud/biz_product/page",
         "tree": "get@/ai/crud/biz_product/tree",
         "create": "post@/ai/crud/biz_product",
         "delete": "delete@/ai/crud/biz_product/:id",
         "detail": "get@/ai/crud/biz_product/:id",
         "update": "put@/ai/crud/biz_product",
         "treeConfig": {
           "keyField": "id",
           "labelField": "productName",
           "parentField": "parentId"
         }
       }', NULL, 'CONFIG', '0', '产品管理', 9067, 0, 9090, '[
         {
           "items": [
             {
               "dictSort": 1,
               "dictLabel": "电子产品",
               "dictValue": "1",
               "dictStatus": 1
             },
             {
               "dictSort": 2,
               "dictLabel": "服装鞋帽",
               "dictValue": "2",
               "dictStatus": 1
             },
             {
               "dictSort": 3,
               "dictLabel": "家居日用",
               "dictValue": "3",
               "dictStatus": 1
             }
           ],
           "dictName": "产品分类",
           "dictType": "product_category"
         },
         {
           "items": [
             {
               "dictSort": 1,
               "dictLabel": "正常",
               "dictValue": "1",
               "dictStatus": 1
             },
             {
               "dictSort": 2,
               "dictLabel": "停用",
               "dictValue": "0",
               "dictStatus": 1
             }
           ],
           "dictName": "通用状态",
           "dictType": "common_status"
         }
       ]', '{
         "parentName": {
           "type": "NAME",
           "label": "父级名称"
         },
         "productName": {
           "type": "NAME",
           "label": "产品名称"
         }
       }', '{
         "operations": [],
         "enableDecrypt": true,
         "enableEncrypt": true
       }', '{
         "status": {
           "dictType": "common_status",
           "targetField": "statusName"
         },
         "category": {
           "dictType": "product_category",
           "targetField": "categoryName"
         }
       }', 'tree-crud', 1, '2026-04-26 18:07:57', 2, 1, '2026-04-26 18:50:43');
INSERT INTO ai_model (id, provider_id, model_type, model_id, model_name, description, max_tokens, icon, is_default,
                      status, sort_order, tenant_id, create_by, create_time, create_dept, update_by, update_time,
                      remark)
VALUES (2045806046227730433, 2044308970805420033, 'chat', 'qwen3.6-plus', 'qwen3.6-plus', NULL, NULL, NULL, '1', '0', 0,
        1, 1, '2026-04-19 18:05:45', 2, 1, '2026-04-19 18:05:45', NULL),
       (2045845428322123777, 2044308970805420033, 'chat', 'qwen3-max', 'qwen3-max', '', NULL, '', '0', '0', 1, 1, 1,
        '2026-04-19 20:42:15', 2, 1, '2026-04-19 20:42:15', NULL);
INSERT INTO ai_page_template (id, template_key, template_name, description, icon, system_prompt, schema_hint,
                              default_config, enabled, sort, is_builtin, codegen_type, create_by, create_time,
                              create_dept, update_by, update_time)
VALUES (1, 'simple-crud', '标准 CRUD', '适合平坦型数据的属性增删改查，支持搜索、表格列表、弹窗/抒屉表单编辑', 'mdi:table', '当前模板为「标准 CRUD」，请确保：
1. columnsSchema 包含内容列 + 操作列(actions)
2. editSchema 简洁清晰，不要嵌套
3. modalType 默认为 drawer
4. 尽量岑减搜索条件，保留最常用的3-5个', NULL, '{
  "modalType": "drawer",
  "modalWidth": "800px",
  "editGridCols": 1,
  "searchGridCols": 4
}', 1, 1, 1, 'TEMPLATE', NULL, '2026-04-26 17:05:23', NULL, 1, '2026-04-26 17:20:22'),
       (2, 'tree-crud', '树形 CRUD', '适合具有父子层级结构的数据，如部门结构、分类管理等，左树右表格布局', 'mdi:file-tree', '当前模板为「树形 CRUD」，请确保：
1. editSchema 中必须包含 parentId 字段（type: treeSelect）
2. editSchema 中包含 sort 排序字段
3. columnsSchema 中包含 层级深度或父层名称列
4. apiConfig 中包含 tree 接口（查询树形结构）
5. treeConfig 配置：{"keyField": "id", "parentField": "parentId", "labelField": "name"}
6. 数据必须有 parent_id 字段，不要导入进 editSchema
7. 模式必须为 modal', '{
         "treeConfig": {
           "keyField": "id",
           "labelField": "name",
           "parentField": "parentId"
         }
       }', '{
         "modalType": "modal",
         "modalWidth": "600px",
         "editGridCols": 1,
         "searchGridCols": 3
       }', 1, 2, 1, 'TEMPLATE', NULL, '2026-04-26 17:05:23', NULL, 1, '2026-04-30 15:53:18');
INSERT INTO ai_provider (id, tenant_id, provider_name, provider_type, logo, api_key, base_url, models, is_default,
                         status, remark, create_by, create_time, update_by, update_time, del_flag, default_model,
                         create_dept)
VALUES (2044308970805420033, 1, '阿里百炼', 'dashscope', '428fbf9216444306ac6065002ff923d7',
        'sk-35b5bb9ac4944ad7a54f0f7d71d0eaf1', 'https://dashscope.aliyuncs.com/compatible-mode', '[
    "qwen3.6-plus",
    "qwen3-max"
  ]', '1', '0', '11', 1, '2026-04-15 14:56:55', 1, '2026-04-17 17:40:44', '0', 'qwen3.6-plus', '2');
INSERT INTO ai_report_project (id, tenant_id, project_name, remark, index_img, status, canvas_width, canvas_height,
                               background_color, component_data, publish_status, publish_url, publish_time, create_by,
                               create_time, update_by, update_time, create_dept, del_flag)
VALUES (2044231509371568130, 1, '销售数据可视化大屏', NULL,
        '/goview-api/api/file/download/8d82c28036d845b3b254fabcfbc49c41', '0', 1920, 1080, '#1e1e2e',
        '{"editCanvasConfig":{"projectName":"销售数据可视化大屏","width":1920,"height":1080,"filterShow":false,"hueRotate":0,"saturate":1,"contrast":1,"brightness":1,"opacity":1,"rotateZ":0,"rotateX":0,"rotateY":0,"skewX":0,"skewY":0,"blendMode":"normal","background":"#1e1e2e","selectColor":true,"chartThemeColor":"dark","chartThemeSetting":{"title":{"show":true,"textStyle":{"color":"#BFBFBF","fontSize":18},"subtextStyle":{"color":"#A2A2A2","fontSize":14}},"xAxis":{"show":true,"name":"","nameGap":15,"nameTextStyle":{"color":"#B9B8CE","fontSize":12},"inverse":false,"axisLabel":{"show":true,"fontSize":12,"color":"#B9B8CE","rotate":0},"position":"bottom","axisLine":{"show":true,"lineStyle":{"color":"#B9B8CE","width":1},"onZero":true},"axisTick":{"show":true,"length":5},"splitLine":{"show":false,"lineStyle":{"color":"#484753","width":1,"type":"solid"}}},"yAxis":{"show":true,"name":"","nameGap":15,"nameTextStyle":{"color":"#B9B8CE","fontSize":12},"inverse":false,"axisLabel":{"show":true,"fontSize":12,"color":"#B9B8CE","rotate":0},"position":"left","axisLine":{"show":true,"lineStyle":{"color":"#B9B8CE","width":1},"onZero":true},"axisTick":{"show":true,"length":5},"splitLine":{"show":true,"lineStyle":{"color":"#484753","width":1,"type":"solid"}}},"legend":{"show":true,"type":"scroll","x":"center","y":"top","icon":"circle","orient":"horizontal","textStyle":{"color":"#B9B8CE","fontSize":18},"itemHeight":15,"itemWidth":15,"pageTextStyle":{"color":"#B9B8CE"}},"grid":{"show":false,"left":"10%","top":"60","right":"10%","bottom":"60"},"dataset":null,"renderer":"svg"},"vChartThemeName":"vScreenVolcanoBlue","previewScaleType":"fit"},"componentList":[{"id":"id_51002jzz2ns000","isGroup":false,"attr":{"x":0,"y":20,"w":1920,"h":60,"offsetX":0,"offsetY":0,"zIndex":-1},"styles":{"filterShow":false,"hueRotate":0,"saturate":1,"contrast":1,"brightness":1,"opacity":1,"rotateZ":0,"rotateX":0,"rotateY":0,"skewX":0,"skewY":0,"blendMode":"normal","animations":[]},"preview":{"overFlowHidden":false},"status":{"lock":false,"hide":false},"request":{"requestDataType":0,"requestHttpType":"get","requestUrl":"","requestIntervalUnit":"second","requestContentType":0,"requestParamsBodyType":"none","requestSQLContent":{"sql":"select * from  where"},"requestParams":{"Body":{"form-data":{},"x-www-form-urlencoded":{},"json":"","xml":""},"Header":{},"Params":{}}},"events":{"baseEvent":{},"advancedEvents":{},"interactEvents":[]},"key":"TextCommon","chartConfig":{"key":"TextCommon","chartKey":"VTextCommon","conKey":"VCTextCommon","title":"销售数据可视化大屏","category":"Texts","categoryName":"文本","package":"Informations","chartFrame":"common","image":"text_static.png"},"option":{"link":"","linkHead":"http://","dataset":"销售数据可视化大屏","fontSize":36,"fontColor":"#ffffff","paddingX":10,"paddingY":10,"textAlign":"center","fontWeight":"bold","borderWidth":0,"borderColor":"#ffffff","borderRadius":5,"letterSpacing":5,"writingMode":"horizontal-tb","backgroundColor":"#00000000"}},{"id":"id_2aw11zdg2xc000","isGroup":false,"attr":{"x":20,"y":100,"w":950,"h":460,"offsetX":0,"offsetY":0,"zIndex":-1},"styles":{"filterShow":false,"hueRotate":0,"saturate":1,"contrast":1,"brightness":1,"opacity":1,"rotateZ":0,"rotateX":0,"rotateY":0,"skewX":0,"skewY":0,"blendMode":"normal","animations":[]},"preview":{"overFlowHidden":false},"status":{"lock":false,"hide":false},"request":{"requestDataType":0,"requestHttpType":"get","requestUrl":"","requestIntervalUnit":"second","requestContentType":0,"requestParamsBodyType":"none","requestSQLContent":{"sql":"select * from  where"},"requestParams":{"Body":{"form-data":{},"x-www-form-urlencoded":{},"json":"","xml":""},"Header":{},"Params":{}}},"events":{"baseEvent":{},"advancedEvents":{},"interactEvents":[]},"key":"BarCommon","chartConfig":{"key":"BarCommon","chartKey":"VBarCommon","conKey":"VCBarCommon","title":"月度销售额统计","category":"Bars","categoryName":"柱状图","package":"Charts","chartFrame":"echarts","image":"bar_x.png"},"option":{"legend":{"show":true,"type":"scroll","x":"center","y":"top","icon":"circle","orient":"horizontal","textStyle":{"color":"#B9B8CE","fontSize":18},"itemHeight":15,"itemWidth":15,"pageTextStyle":{"color":"#B9B8CE"}},"xAxis":{"show":true,"name":"","nameGap":15,"nameTextStyle":{"color":"#B9B8CE","fontSize":12},"inverse":false,"axisLabel":{"show":true,"fontSize":12,"color":"#B9B8CE","rotate":0},"position":"bottom","axisLine":{"show":true,"lineStyle":{"color":"#B9B8CE","width":1},"onZero":true},"axisTick":{"show":true,"length":5},"splitLine":{"show":false,"lineStyle":{"color":"#484753","width":1,"type":"solid"}},"type":"category"},"yAxis":{"show":true,"name":"","nameGap":15,"nameTextStyle":{"color":"#B9B8CE","fontSize":12},"inverse":false,"axisLabel":{"show":true,"fontSize":12,"color":"#B9B8CE","rotate":0},"position":"left","axisLine":{"show":true,"lineStyle":{"color":"#B9B8CE","width":1},"onZero":true},"axisTick":{"show":true,"length":5},"splitLine":{"show":true,"lineStyle":{"color":"#484753","width":1,"type":"solid"}},"type":"value"},"grid":{"show":false,"left":"10%","top":"60","right":"10%","bottom":"60"},"tooltip":{"show":true,"trigger":"axis","axisPointer":{"show":true,"type":"shadow"}},"dataset":{"dimensions":["月份","销售额"],"source":[{"月份":"1月","销售额":320},{"月份":"2月","销售额":280},{"月份":"3月","销售额":450},{"月份":"4月","销售额":390},{"月份":"5月","销售额":510},{"月份":"6月","销售额":480},{"月份":"7月","销售额":620},{"月份":"8月","销售额":590},{"月份":"9月","销售额":530},{"月份":"10月","销售额":680},{"月份":"11月","销售额":750},{"月份":"12月","销售额":820}]},"series":[{"type":"bar","barWidth":15,"label":{"show":true,"position":"top","color":"#fff","fontSize":12},"itemStyle":{"color":null,"borderRadius":2}}],"backgroundColor":"rgba(0,0,0,0)"}},{"id":"id_2mx5tfhv0as000","isGroup":false,"attr":{"x":990,"y":100,"w":910,"h":460,"offsetX":0,"offsetY":0,"zIndex":-1},"styles":{"filterShow":false,"hueRotate":0,"saturate":1,"contrast":1,"brightness":1,"opacity":1,"rotateZ":0,"rotateX":0,"rotateY":0,"skewX":0,"skewY":0,"blendMode":"normal","animations":[]},"preview":{"overFlowHidden":false},"status":{"lock":false,"hide":false},"request":{"requestDataType":0,"requestHttpType":"get","requestUrl":"","requestIntervalUnit":"second","requestContentType":0,"requestParamsBodyType":"none","requestSQLContent":{"sql":"select * from  where"},"requestParams":{"Body":{"form-data":{},"x-www-form-urlencoded":{},"json":"","xml":""},"Header":{},"Params":{}}},"events":{"baseEvent":{},"advancedEvents":{},"interactEvents":[]},"key":"PieCommon","chartConfig":{"key":"PieCommon","chartKey":"VPieCommon","conKey":"VCPieCommon","title":"销售渠道占比分析","category":"Pies","categoryName":"饼图","package":"Charts","chartFrame":"echarts","image":"pie.png"},"option":{"legend":{"show":true,"type":"scroll","x":"center","y":"top","icon":"circle","orient":"horizontal","textStyle":{"color":"#B9B8CE","fontSize":18},"itemHeight":15,"itemWidth":15,"pageTextStyle":{"color":"#B9B8CE"}},"isCarousel":false,"type":"ring","tooltip":{"show":true,"trigger":"item"},"dataset":{"dimensions":["渠道","销售额"],"source":[{"渠道":"电商平台","销售额":520},{"渠道":"线下门店","销售额":380},{"渠道":"直播带货","销售额":410},{"渠道":"批发代理","销售额":290}]},"series":[{"type":"pie","radius":["40%","65%"],"center":["50%","60%"],"roseType":false,"avoidLabelOverlap":false,"itemStyle":{"show":true,"borderRadius":10,"borderColor":"#fff","borderWidth":2},"label":{"show":false,"position":"center","formatter":"{b}","fontWeight":"normal","fontSize":14,"color":"#454E54","textBorderColor":"#ffffff","textBorderWidth":1},"emphasis":{"label":{"show":true,"fontSize":40,"fontWeight":"bold"}},"labelLine":{"show":false}}],"backgroundColor":"rgba(0,0,0,0)"}},{"id":"id_3s54eqi5vje000","isGroup":false,"attr":{"x":20,"y":580,"w":1880,"h":480,"offsetX":0,"offsetY":0,"zIndex":-1},"styles":{"filterShow":false,"hueRotate":0,"saturate":1,"contrast":1,"brightness":1,"opacity":1,"rotateZ":0,"rotateX":0,"rotateY":0,"skewX":0,"skewY":0,"blendMode":"normal","animations":[]},"preview":{"overFlowHidden":false},"status":{"lock":false,"hide":false},"request":{"requestDataType":0,"requestHttpType":"get","requestUrl":"","requestIntervalUnit":"second","requestContentType":0,"requestParamsBodyType":"none","requestSQLContent":{"sql":"select * from  where"},"requestParams":{"Body":{"form-data":{},"x-www-form-urlencoded":{},"json":"","xml":""},"Header":{},"Params":{}}},"events":{"baseEvent":{},"advancedEvents":{},"interactEvents":[]},"key":"LineCommon","chartConfig":{"key":"LineCommon","chartKey":"VLineCommon","conKey":"VCLineCommon","title":"近期销售趋势与利润分析","category":"Lines","categoryName":"折线图","package":"Charts","chartFrame":"echarts","image":"line.png"},"option":{"legend":{"show":true,"type":"scroll","x":"center","y":"top","icon":"circle","orient":"horizontal","textStyle":{"color":"#B9B8CE","fontSize":18},"itemHeight":15,"itemWidth":15,"pageTextStyle":{"color":"#B9B8CE"}},"xAxis":{"show":true,"name":"","nameGap":15,"nameTextStyle":{"color":"#B9B8CE","fontSize":12},"inverse":false,"axisLabel":{"show":true,"fontSize":12,"color":"#B9B8CE","rotate":0},"position":"bottom","axisLine":{"show":true,"lineStyle":{"color":"#B9B8CE","width":1},"onZero":true},"axisTick":{"show":true,"length":5},"splitLine":{"show":false,"lineStyle":{"color":"#484753","width":1,"type":"solid"}},"type":"category"},"yAxis":{"show":true,"name":"","nameGap":15,"nameTextStyle":{"color":"#B9B8CE","fontSize":12},"inverse":false,"axisLabel":{"show":true,"fontSize":12,"color":"#B9B8CE","rotate":0},"position":"left","axisLine":{"show":true,"lineStyle":{"color":"#B9B8CE","width":1},"onZero":true},"axisTick":{"show":true,"length":5},"splitLine":{"show":true,"lineStyle":{"color":"#484753","width":1,"type":"solid"}},"type":"value"},"grid":{"show":false,"left":"10%","top":"60","right":"10%","bottom":"60"},"tooltip":{"show":true,"trigger":"axis","axisPointer":{"type":"line"}},"dataset":{"dimensions":["日期","销售额","利润"],"source":[{"日期":"1日","销售额":1200,"利润":350},{"日期":"2日","销售额":1350,"利润":410},{"日期":"3日","销售额":1100,"利润":290},{"日期":"4日","销售额":1450,"利润":460},{"日期":"5日","销售额":1600,"利润":520},{"日期":"6日","销售额":1300,"利润":380},{"日期":"7日","销售额":1750,"利润":580},{"日期":"8日","销售额":1550,"利润":490},{"日期":"9日","销售额":1400,"利润":430},{"日期":"10日","销售额":1650,"利润":540},{"日期":"11日","销售额":1800,"利润":610},{"日期":"12日","销售额":1950,"利润":680}]},"series":[{"type":"line","label":{"show":true,"position":"top","color":"#fff","fontSize":12},"symbolSize":5,"itemStyle":{"color":null,"borderRadius":0},"lineStyle":{"type":"solid","width":3,"color":null}},{"type":"line","label":{"show":true,"position":"top","color":"#fff","fontSize":12},"symbolSize":5,"itemStyle":{"color":null,"borderRadius":0},"lineStyle":{"type":"solid","width":3,"color":null}}],"backgroundColor":"rgba(0,0,0,0)"}}],"requestGlobalConfig":{"requestDataPond":[],"requestOriginUrl":"","requestInterval":30,"requestIntervalUnit":"second","requestParams":{"Body":{"form-data":{},"x-www-form-urlencoded":{},"json":"","xml":""},"Header":{},"Params":{}}}}',
        '1', 'http://localhost:3021#/chart/preview/2044231509371568130', '2026-04-16 13:04:27', 1,
        '2026-04-15 09:49:06', 1, '2026-04-16 13:04:27', 2, '0');
INSERT INTO config_properties (id, `key`, value, description, `group`, `type`, enabled, create_time, update_time,
                               create_by, update_by)
VALUES (1, 'app.name', 'Forge Framework1111', '应用名称', 'APPLICATION', 'STRING', 1, '2025-12-01 17:19:23',
        '2025-12-01 17:23:25', NULL, NULL),
       (2, 'app.version', '1.0.0', '应用版本', 'APPLICATION', 'STRING', 1, '2025-12-01 17:19:23', '2025-12-01 17:19:23',
        NULL, NULL),
       (10, 'forge.auth.maxLoginAttempts', '5', '最大登录失败尝试次数', 'DEFAULT_GROUP', 'STRING', 1,
        '2025-12-15 17:12:04', '2025-12-15 18:12:31', NULL, NULL);
INSERT INTO gen_datasource (datasource_id, datasource_name, datasource_code, db_type, driver_class_name, url, username,
                            password, is_default, is_enabled, test_query, sort, remark, create_time, update_time,
                            create_by, update_by)
VALUES (1, '默认数据源', 'default', 'MySQL', 'com.mysql.cj.jdbc.Driver',
        'jdbc:mysql://120.48.96.178:3306/forge_admin_new?useUnicode=true&characterEncoding=utf8&zeroDateTimeBehavior=convertToNull&useSSL=true&serverTimezone=GMT%2B8&autoReconnect=true&rewriteBatchedStatements=true&allowPublicKeyRetrieval=true&nullCatalogMeansCurrent=true',
        'rdsroot', '90a5f41a94a9e875a7936330223a4dd1', 1, 1, 'SELECT 1', 0, '当前应用默认数据源', '2025-12-05 17:14:46',
        '2025-12-05 17:15:48', NULL, NULL);
INSERT INTO gen_table (table_id, datasource_id, table_name, table_comment, class_name, business_name, function_name,
                       module_name, package_name, author, gen_type, gen_path, template_engine, `options`, remark,
                       create_time, update_time, create_by, update_by)
VALUES (1, 1, 'sys_user', '用户表', 'User', 'user', '用户管理', 'system', 'com.mdframe.forge.plugin', 'Forge Generator',
        'DOWNLOAD', '/', 'VELOCITY', NULL, NULL, '2025-11-12 14:22:37', '2025-12-05 17:20:06', NULL, NULL),
       (5, 1, 'sys_file_storage_config', '文件存储配置表', 'FileStorageConfig', 'fileStorageConfig', '文件存储配置表',
        'system', 'com.mdframe.forge.plugin', 'Forge Generator', 'DOWNLOAD', '/', 'VELOCITY', NULL, NULL,
        '2026-01-20 12:58:52', '2026-01-20 20:09:17', NULL, NULL);
INSERT INTO gen_table_column (column_id, table_id, column_name, column_comment, column_type, java_type, java_field,
                              is_pk, is_increment, is_required, is_insert, is_edit, is_list, is_query, query_type,
                              html_type, dict_type, validate_rule, ai_recommended, sort, create_time, update_time)
VALUES (1, 1, 'id', '用户ID', 'bigint', 'Long', 'id', 1, 1, 0, 1, 1, 1, 1, 'EQ', 'INPUT', NULL, NULL, 0, 12,
        '2025-12-05 11:07:57', '2025-12-05 11:07:57'),
       (2, 1, 'tenant_id', '租户编号', 'bigint', 'Long', 'tenantId', 0, 0, 1, 1, 1, 1, 1, 'EQ', 'INPUT', NULL, NULL, 0,
        22, '2025-12-05 11:07:58', '2025-12-05 11:07:58'),
       (3, 1, 'username', '用户名（租户内唯一）', 'varchar(50)', 'String', 'username', 0, 0, 1, 1, 1, 1, 1, 'LIKE',
        'INPUT', NULL, NULL, 0, 21, '2025-12-05 11:07:58', '2025-12-05 11:07:58'),
       (4, 1, 'real_name', '用户真实姓名', 'varchar(50)', 'String', 'realName', 0, 0, 0, 1, 1, 1, 1, 'LIKE', 'INPUT',
        NULL, NULL, 0, 20, '2025-12-05 11:07:58', '2025-12-05 11:07:58'),
       (5, 1, 'user_type', '用户类型（0-系统管理员，1-租户管理员，2-普通用户）', 'tinyint', 'Integer', 'userType', 0, 0, 0,
        1, 1, 1, 1, 'EQ', 'INPUT', NULL, NULL, 0, 19, '2025-12-05 11:07:58', '2025-12-05 11:07:58'),
       (6, 1, 'user_client', '用户触点（app/pc/h5/wechat）', 'varchar(20)', 'String', 'userClient', 0, 0, 0, 1, 1, 1, 1,
        'LIKE', 'INPUT', NULL, NULL, 0, 18, '2025-12-05 11:07:58', '2025-12-05 11:07:58'),
       (7, 1, 'email', '邮箱', 'varchar(100)', 'String', 'email', 0, 0, 0, 1, 1, 1, 1, 'LIKE', 'INPUT', NULL, NULL, 0,
        17, '2025-12-05 11:07:58', '2025-12-05 11:07:58'),
       (8, 1, 'phone', '手机号', 'varchar(20)', 'String', 'phone', 0, 0, 0, 1, 1, 1, 1, 'LIKE', 'INPUT', NULL, NULL, 0,
        16, '2025-12-05 11:07:58', '2025-12-05 11:07:58'),
       (9, 1, 'id_card', '身份证号', 'varchar(18)', 'String', 'idCard', 0, 0, 0, 1, 1, 1, 1, 'LIKE', 'INPUT', NULL,
        NULL, 0, 15, '2025-12-05 11:07:58', '2025-12-05 11:07:58'),
       (10, 1, 'gender', '性别（0-未知，1-男，2-女）', 'tinyint', 'Integer', 'gender', 0, 0, 0, 1, 1, 1, 1, 'EQ', 'INPUT',
        NULL, NULL, 0, 14, '2025-12-05 11:07:58', '2025-12-05 11:07:58');
INSERT INTO gen_table_column (column_id, table_id, column_name, column_comment, column_type, java_type, java_field,
                              is_pk, is_increment, is_required, is_insert, is_edit, is_list, is_query, query_type,
                              html_type, dict_type, validate_rule, ai_recommended, sort, create_time, update_time)
VALUES (11, 1, 'password', '密码（加密存储）', 'varchar(100)', 'String', 'password', 0, 0, 1, 1, 1, 1, 1, 'LIKE', 'INPUT',
        NULL, NULL, 0, 13, '2025-12-05 11:07:58', '2025-12-05 11:07:58'),
       (12, 1, 'salt', '密码盐值', 'varchar(50)', 'String', 'salt', 0, 0, 0, 1, 1, 1, 1, 'LIKE', 'INPUT', NULL, NULL, 0,
        0, '2025-12-05 11:07:58', '2025-12-05 11:07:58'),
       (13, 1, 'user_status', '用户状态（0-禁用，1-正常，2-锁定）', 'tinyint', 'Integer', 'userStatus', 0, 0, 1, 1, 1, 1, 1,
        'EQ', 'INPUT', NULL, NULL, 0, 11, '2025-12-05 11:07:58', '2025-12-05 11:07:58'),
       (14, 1, 'avatar', '头像URL', 'varchar(255)', 'String', 'avatar', 0, 0, 0, 1, 1, 1, 1, 'LIKE', 'INPUT', NULL,
        NULL, 0, 10, '2025-12-05 11:07:58', '2025-12-05 11:07:58'),
       (15, 1, 'last_login_time', '最后登录时间', 'datetime', 'LocalDateTime', 'lastLoginTime', 0, 0, 0, 1, 1, 1, 1,
        'EQ', 'DATETIME', NULL, NULL, 0, 9, '2025-12-05 11:07:58', '2025-12-05 11:07:58'),
       (16, 1, 'last_login_ip', '最后登录IP', 'varchar(50)', 'String', 'lastLoginIp', 0, 0, 0, 1, 1, 1, 1, 'LIKE',
        'INPUT', NULL, NULL, 0, 8, '2025-12-05 11:07:58', '2025-12-05 11:07:58'),
       (17, 1, 'login_count', '登录次数', 'int', 'Integer', 'loginCount', 0, 0, 0, 1, 1, 1, 1, 'EQ', 'INPUT', NULL,
        NULL, 0, 7, '2025-12-05 11:07:58', '2025-12-05 11:07:58'),
       (18, 1, 'remark', '备注', 'varchar(500)', 'String', 'remark', 0, 0, 0, 0, 0, 0, 0, 'LIKE', 'INPUT', NULL, NULL,
        0, 6, '2025-12-05 11:07:59', '2025-12-05 11:07:59'),
       (19, 1, 'create_dept', '创建部门', 'bigint', 'Long', 'createDept', 0, 0, 0, 1, 1, 1, 1, 'EQ', 'INPUT', NULL,
        NULL, 0, 5, '2025-12-05 11:07:59', '2025-12-05 11:07:59'),
       (20, 1, 'create_by', '创建者', 'bigint', 'Long', 'createBy', 0, 0, 0, 0, 0, 0, 0, 'EQ', 'INPUT', NULL, NULL, 0,
        4, '2025-12-05 11:07:59', '2025-12-05 11:07:59');
INSERT INTO gen_table_column (column_id, table_id, column_name, column_comment, column_type, java_type, java_field,
                              is_pk, is_increment, is_required, is_insert, is_edit, is_list, is_query, query_type,
                              html_type, dict_type, validate_rule, ai_recommended, sort, create_time, update_time)
VALUES (21, 1, 'create_time', '创建时间', 'datetime', 'LocalDateTime', 'createTime', 0, 0, 1, 0, 0, 0, 0, 'EQ',
        'DATETIME', NULL, NULL, 0, 3, '2025-12-05 11:07:59', '2025-12-05 11:07:59'),
       (22, 1, 'update_by', '更新者', 'bigint', 'Long', 'updateBy', 0, 0, 0, 0, 0, 0, 0, 'EQ', 'INPUT', NULL, NULL, 0,
        2, '2025-12-05 11:07:59', '2025-12-05 11:07:59'),
       (23, 1, 'update_time', '更新时间', 'datetime', 'LocalDateTime', 'updateTime', 0, 0, 1, 0, 0, 0, 0, 'EQ',
        'DATETIME', NULL, NULL, 0, 1, '2025-12-05 11:07:59', '2025-12-05 11:07:59'),
       (38, 5, 'id', '主键', 'bigint', 'Long', 'id', 1, 1, 0, 1, 1, 1, 1, 'EQ', 'INPUT', NULL, NULL, 0, 0,
        '2026-01-20 20:09:17', '2026-01-20 20:09:17'),
       (39, 5, 'config_name', '配置名称', 'varchar(100)', 'String', 'configName', 0, 0, 1, 1, 1, 1, 1, 'LIKE', 'INPUT',
        NULL, NULL, 0, 0, '2026-01-20 20:09:17', '2026-01-20 20:09:17'),
       (40, 5, 'storage_type', '存储类型(local/minio/aliyun_oss等)', 'varchar(50)', 'String', 'storageType', 0, 0, 1, 1,
        1, 1, 1, 'LIKE', 'INPUT', NULL, NULL, 0, 0, '2026-01-20 20:09:17', '2026-01-20 20:09:17'),
       (41, 5, 'is_default', '是否默认策略', 'tinyint(1)', 'Integer', 'isDefault', 0, 0, 0, 1, 1, 1, 1, 'EQ', 'INPUT',
        NULL, NULL, 0, 0, '2026-01-20 20:09:17', '2026-01-20 20:09:17'),
       (42, 5, 'enabled', '是否启用', 'tinyint(1)', 'Integer', 'enabled', 0, 0, 0, 1, 1, 1, 1, 'EQ', 'INPUT', NULL,
        NULL, 0, 0, '2026-01-20 20:09:17', '2026-01-20 20:09:17'),
       (43, 5, 'endpoint', '访问端点', 'varchar(500)', 'String', 'endpoint', 0, 0, 0, 1, 1, 1, 1, 'LIKE', 'INPUT', NULL,
        NULL, 0, 0, '2026-01-20 20:09:17', '2026-01-20 20:09:17'),
       (44, 5, 'access_key', '访问密钥ID', 'varchar(200)', 'String', 'accessKey', 0, 0, 0, 1, 1, 1, 1, 'LIKE', 'INPUT',
        NULL, NULL, 0, 0, '2026-01-20 20:09:17', '2026-01-20 20:09:17');
INSERT INTO gen_table_column (column_id, table_id, column_name, column_comment, column_type, java_type, java_field,
                              is_pk, is_increment, is_required, is_insert, is_edit, is_list, is_query, query_type,
                              html_type, dict_type, validate_rule, ai_recommended, sort, create_time, update_time)
VALUES (45, 5, 'secret_key', '访问密钥Secret', 'varchar(200)', 'String', 'secretKey', 0, 0, 0, 1, 1, 1, 1, 'LIKE',
        'INPUT', NULL, NULL, 0, 0, '2026-01-20 20:09:17', '2026-01-20 20:09:17'),
       (46, 5, 'bucket_name', '存储桶名称', 'varchar(100)', 'String', 'bucketName', 0, 0, 0, 1, 1, 1, 1, 'LIKE',
        'INPUT', NULL, NULL, 0, 0, '2026-01-20 20:09:17', '2026-01-20 20:09:17'),
       (47, 5, 'region', '区域', 'varchar(100)', 'String', 'region', 0, 0, 0, 1, 1, 1, 1, 'LIKE', 'INPUT', NULL, NULL,
        0, 0, '2026-01-20 20:09:17', '2026-01-20 20:09:17'),
       (48, 5, 'base_path', '基础路径', 'varchar(200)', 'String', 'basePath', 0, 0, 0, 1, 1, 1, 1, 'LIKE', 'INPUT',
        NULL, NULL, 0, 0, '2026-01-20 20:09:17', '2026-01-20 20:09:17'),
       (49, 5, 'domain', '访问域名', 'varchar(500)', 'String', 'domain', 0, 0, 0, 1, 1, 1, 1, 'LIKE', 'INPUT', NULL,
        NULL, 0, 0, '2026-01-20 20:09:17', '2026-01-20 20:09:17'),
       (50, 5, 'use_https', '是否使用HTTPS', 'tinyint(1)', 'Integer', 'useHttps', 0, 0, 0, 1, 1, 1, 1, 'EQ', 'INPUT',
        NULL, NULL, 0, 0, '2026-01-20 20:09:17', '2026-01-20 20:09:17'),
       (51, 5, 'max_file_size', '最大文件大小(MB)', 'int', 'Integer', 'maxFileSize', 0, 0, 0, 1, 1, 1, 1, 'EQ', 'INPUT',
        NULL, NULL, 0, 0, '2026-01-20 20:09:17', '2026-01-20 20:09:17'),
       (52, 5, 'allowed_types', '允许的文件类型(逗号分隔)', 'varchar(500)', 'String', 'allowedTypes', 0, 0, 0, 1, 1, 1,
        1, 'LIKE', 'INPUT', NULL, NULL, 0, 0, '2026-01-20 20:09:17', '2026-01-20 20:09:17'),
       (53, 5, 'order_num', '排序', 'int', 'Integer', 'orderNum', 0, 0, 0, 1, 1, 1, 1, 'EQ', 'INPUT', NULL, NULL, 0, 0,
        '2026-01-20 20:09:17', '2026-01-20 20:09:17'),
       (54, 5, 'extra_config', '扩展配置(JSON)', 'text', 'String', 'extraConfig', 0, 0, 0, 1, 1, 1, 1, 'LIKE', 'INPUT',
        NULL, NULL, 0, 0, '2026-01-20 20:09:17', '2026-01-20 20:09:17');
INSERT INTO gen_table_column (column_id, table_id, column_name, column_comment, column_type, java_type, java_field,
                              is_pk, is_increment, is_required, is_insert, is_edit, is_list, is_query, query_type,
                              html_type, dict_type, validate_rule, ai_recommended, sort, create_time, update_time)
VALUES (55, 5, 'create_time', '创建时间', 'datetime', 'LocalDateTime', 'createTime', 0, 0, 0, 0, 0, 0, 0, 'EQ',
        'DATETIME', NULL, NULL, 0, 0, '2026-01-20 20:09:17', '2026-01-20 20:09:17'),
       (56, 5, 'update_time', '更新时间', 'datetime', 'LocalDateTime', 'updateTime', 0, 0, 0, 0, 0, 0, 0, 'EQ',
        'DATETIME', NULL, NULL, 0, 0, '2026-01-20 20:09:18', '2026-01-20 20:09:18'),
       (57, 5, 'create_by', '创建者', 'bigint', 'Long', 'createBy', 0, 0, 0, 0, 0, 0, 0, 'EQ', 'INPUT', NULL, NULL, 0,
        0, '2026-01-20 20:09:18', '2026-01-20 20:09:18'),
       (58, 5, 'update_by', '更新者', 'bigint', 'Long', 'updateBy', 0, 0, 0, 0, 0, 0, 0, 'EQ', 'INPUT', NULL, NULL, 0,
        0, '2026-01-20 20:09:18', '2026-01-20 20:09:18'),
       (59, 5, 'create_dept', '创建组织ID', 'bigint unsigned', 'Long', 'createDept', 0, 0, 0, 1, 1, 1, 1, 'EQ', 'INPUT',
        NULL, NULL, 0, 0, '2026-01-20 20:09:18', '2026-01-20 20:09:18');
INSERT INTO sys_client (id, client_code, client_name, app_id, app_secret, token_timeout, token_activity_timeout,
                        token_prefix, token_name, concurrent_login, share_token, enable_ip_limit, ip_whitelist,
                        enable_encrypt, encrypt_algorithm, max_user_count, max_online_count, auth_types, status,
                        description, tenant_id, create_time, create_by, update_time, update_by, create_dept)
VALUES (6, 'pc', 'PC端', 'pc', 'forage_pc123', 2592000, 7200, 'Bearer', 'Authorization', 1, 0, 0, NULL, 0, 'AES', -1,
        -1, 'password,password_captcha', 1, NULL, 1, '2026-04-08 09:37:10', 1, '2026-04-30 16:17:48', 1, 2),
       (8, 'forge_report', '报表系统', 'forge_report', 'forage_pc123', 2592000, 7200, 'Bearer', 'Authorization', 0, 0,
        0, NULL, 0, 'AES', -1, -1, 'password,password_captcha', 1, NULL, 1, '2026-04-08 09:37:10', 1,
        '2026-04-08 10:20:58', 1, 2),
       (9, 'app', 'APP端', 'forge_app', 'forge_app123', 7200, -1, 'Bearer', 'Authorization', 0, 0, 0, NULL, 0, 'AES',
        -1, -1, 'password,password_captcha,phone_captcha', 1, NULL, 1, '2026-04-29 14:44:57', 1, '2026-04-29 14:44:57',
        1, 2);
INSERT INTO sys_config (config_id, tenant_id, config_name, config_key, config_value, config_type, config_desc, sort,
                        create_by, create_time, update_by, update_time, create_dept)
VALUES (1, 1, '用户初始密码', 'sys.user.initPassword', '!forgeAdmin123', 'Y', '用户注册时的默认密码', 1, NULL,
        '2025-11-12 17:41:18', NULL, '2026-04-01 14:16:46', NULL),
       (2, 1, '账号自助注册', 'sys.account.registerUser', 'false', 'Y', '是否开启用户自助注册功能', 2, NULL,
        '2025-11-12 17:41:18', NULL, '2025-11-12 17:41:18', NULL);
INSERT INTO sys_config_group (id, group_code, group_name, group_icon, config_value, sort, status, remark, create_time,
                              update_time)
VALUES (2, 'login', '登录配置', 'lock',
        '{"enableCaptcha":true,"captchaType":"slider","enableRememberMe":true,"rememberMeDays":30,"enableLoginLog":true,"enableIpLimit":false,"ipWhitelist":""}',
        2, 1, '登录相关配置', '2026-02-25 18:07:35', '2026-03-02 10:53:22'),
       (4, 'watermark', '水印配置', 'eye',
        '{"enable":true,"content":"name_phone","opacity":0.3,"fontSize":16,"fontColor":"#cccccc","rotate":-20,"gapX":100,"gapY":100,"offsetX":0,"offsetY":0,"showTimestamp":false,"timestampFormat":"yyyy-MM-dd HH:mm:ss","zindex":1000}',
        4, 1, '水印相关配置', '2026-02-25 18:07:35', '2026-03-02 15:05:11'),
       (5, 'crypto', '加解密配置', 'eye',
        '{"enabled":true,"algorithm":"SM4","secretKey":null,"enableDynamicKey":true,"rsaPublicKey":null,"rsaPrivateKey":null,"sessionKeyExpire":7200,"enableApiCrypto":true,"enableFieldCrypto":true,"enableReplayProtection":true,"replayTimeWindow":300,"replayIncludePaths":[],"replayExcludePaths":["/auth/captcha","/crypto/public-key","/ws/**","/api/flow/instance/**","/api/file/**","/system/demo/**"],"excludePaths":[],"enableDesensitize":true}',
        4, 1, '加解密配置', '2026-02-25 18:07:35', '2026-04-29 14:43:48'),
       (6, 'auth', '认证配置', 'eye',
        '{"enableApiPermission":true,"apiPermissionExcludePaths":["/auth/**"],"enableLoginLock":true,"maxLoginAttempts":4,"lockDuration":30,"failRecordExpire":15,"sameAccountLoginStrategy":"allow_concurrent","enableOnlineUserManagement":true,"enableClientValidation":true}',
        4, 1, '认证配置', '2026-02-25 18:07:35', '2026-04-30 15:26:59'),
       (7, 'log', '日志配置', 'eye',
        '{"enableOperationLog":true,"enableLoginLog":true,"requestParamsMaxLength":2000,"responseResultMaxLength":2000,"excludePaths":["/auth/captcha","/actuator/**","/swagger-ui/**","/v3/api-docs/**"],"printOperationLog":true,"printLoginLog":true,"threadPoolCoreSize":2,"threadPoolMaxSize":5,"threadPoolQueueCapacity":500}',
        4, 1, '日志配置', '2026-02-25 18:07:35', '2026-02-26 12:57:22'),
       (8, 'security', '安全配置', 'eye',
        '{"saToken":{"timeout":2592000,"activityTimeout":-1,"isConcurrent":true,"isShare":false,"isReadBody":true,"isReadHeader":true,"isReadCookie":false,"tokenPrefix":"Bearer","tokenName":"Authorization"},"passwordPolicy":{"minLength":8,"requireUppercase":true,"requireLowercase":true,"requireNumbers":true,"requireSpecialChars":false,"expireDays":90,"historyCount":5}}',
        4, 1, '安全配置', '2026-02-25 18:07:35', '2026-04-30 15:27:16');
INSERT INTO sys_data_scope_config (id, tenant_id, resource_code, resource_name, mapper_method, table_alias,
                                   user_id_column, org_id_column, tenant_id_column, region_code_column,
                                   user_region_column, user_table_alias, enabled, remark, create_by, create_time,
                                   update_by, update_time, create_dept)
VALUES (1, 1, 'system:user:list', '用户列表查询', 'com.mdframe.forge.plugin.system.mapper.SysUserMapper.selectUserPage',
        'u', 'id', 'create_dept', 'tenant_id', 'region_code', 'region_code', NULL, 1, '用户列表数据权限控制', NULL,
        '2025-12-03 15:48:28', 1, '2026-04-28 17:42:26', NULL),
       (6, 1, 'system:listSysRegion', '查询行政区划',
        'com.mdframe.forge.plugin.system.mapper.SysRegionMapper.listSysRegion', '', '', '', '', 'code', 'code', '', 1,
        '', NULL, '2026-04-28 16:27:40', NULL, '2026-04-28 16:51:36', NULL),
       (7, 1, 'system:deptPage:list', '组织列表查询-分页',
        'com.mdframe.forge.plugin.system.mapper.SysOrgMapper.selectOrgPage', '', '', 'id', 'tenant_id', 'region_code',
        'region_code', NULL, 1, '用户列表数据权限控制', NULL, '2025-12-03 15:48:28', 1, '2026-01-20 15:18:48', NULL),
       (8, 1, 'system:dept:list', '组织列表查询', 'com.mdframe.forge.plugin.system.mapper.SysOrgMapper.selectOrgList',
        '', '', 'id', 'tenant_id', 'region_code', 'region_code', NULL, 1, '用户列表数据权限控制', NULL,
        '2025-12-03 15:48:28', 1, '2026-01-20 15:18:48', NULL);
INSERT INTO sys_dict_data (dict_code, tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class,
                           is_default, parent_dict_code, linked_dict_type, linked_dict_value, dict_status, remark,
                           create_by, create_time, update_by, update_time, create_dept)
VALUES (1, 1, 1, '未知', '0', 'sys_org_type', '', 'success', 'Y', NULL, NULL, NULL, 1, '性别未知', NULL,
        '2025-11-12 17:41:18', NULL, '2025-12-12 10:50:32', NULL),
       (2, 1, 2, '男', '1', 'sys_user_sex', '', '', 'N', NULL, NULL, NULL, 1, '性别男', NULL, '2025-11-12 17:41:18',
        NULL, '2025-11-12 17:41:18', NULL),
       (3, 1, 3, '女', '2', 'sys_user_sex', '', '', 'N', NULL, NULL, NULL, 1, '性别女', NULL, '2025-11-12 17:41:18',
        NULL, '2025-11-12 17:41:18', NULL),
       (4, 1, 1, '禁用', '0', 'sys_user_status', '', 'danger', 'N', NULL, NULL, NULL, 1, '用户禁用状态', NULL,
        '2025-11-12 17:41:18', NULL, '2025-11-12 17:41:18', NULL),
       (5, 1, 2, '正常', '1', 'sys_user_status', '', 'success', 'Y', NULL, NULL, NULL, 1, '用户正常状态', NULL,
        '2025-11-12 17:41:18', NULL, '2025-11-12 17:41:18', NULL),
       (6, 1, 3, '锁定', '2', 'sys_user_status', '', 'warning', 'N', NULL, NULL, NULL, 1, '用户锁定状态', NULL,
        '2025-11-12 17:41:18', NULL, '2025-11-12 17:41:18', NULL),
       (7, 1, 1, '公司', '1', 'sys_org_type', '', 'info', 'Y', NULL, NULL, NULL, 1, '公司组织', NULL,
        '2025-11-12 17:41:18', NULL, '2025-12-12 10:46:21', NULL),
       (8, 1, 2, '部门', '2', 'sys_org_type', '', 'success', 'N', NULL, NULL, NULL, 1, '部门组织', NULL,
        '2025-11-12 17:41:18', NULL, '2025-11-12 17:41:18', NULL),
       (9, 1, 3, '小组', '3', 'sys_org_type', '', 'info', 'N', NULL, NULL, NULL, 1, '小组组织', NULL,
        '2025-11-12 17:41:18', NULL, '2025-11-12 17:41:18', NULL),
       (10, 1, 1, '是', 'Y', 'yes_no', '', '', 'N', NULL, NULL, NULL, 1, '小组组织', NULL, '2025-11-12 17:41:18', NULL,
        '2025-11-12 17:41:18', NULL);
INSERT INTO sys_dict_data (dict_code, tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class,
                           is_default, parent_dict_code, linked_dict_type, linked_dict_value, dict_status, remark,
                           create_by, create_time, update_by, update_time, create_dept)
VALUES (11, 1, 2, '否', 'N', 'yes_no', '', '', 'N', NULL, NULL, NULL, 1, '小组组织', NULL, '2025-11-12 17:41:18', NULL,
        '2025-11-12 17:41:18', NULL),
       (12, 1, 1, '通知公告', 'NOTICE', 'sys_notice_type', NULL, NULL, 'N', NULL, NULL, NULL, 1, '普通通知公告', NULL,
        '2025-12-04 10:57:35', NULL, '2025-12-08 16:31:36', NULL),
       (13, 1, 2, '系统公告', 'ANNOUNCEMENT', 'sys_notice_type', NULL, NULL, 'N', NULL, NULL, NULL, 1, '系统级公告',
        NULL, '2025-12-04 10:57:35', NULL, '2025-12-08 16:31:36', NULL),
       (14, 1, 3, '新闻动态', 'NEWS', 'sys_notice_type', NULL, NULL, 'N', NULL, NULL, NULL, 1, '新闻动态信息', NULL,
        '2025-12-04 10:57:35', NULL, '2025-12-08 16:31:37', NULL),
       (15, 1, 1, '草稿', '0', 'sys_notice_status', NULL, NULL, 'N', NULL, NULL, NULL, 1, '草稿状态', NULL,
        '2025-12-04 10:57:35', NULL, '2025-12-08 16:31:37', NULL),
       (16, 1, 2, '已发布', '1', 'sys_notice_status', NULL, NULL, 'N', NULL, NULL, NULL, 1, '已发布状态', NULL,
        '2025-12-04 10:57:35', NULL, '2025-12-08 16:31:37', NULL),
       (17, 1, 3, '已撤回', '2', 'sys_notice_status', NULL, NULL, 'N', NULL, NULL, NULL, 1, '已撤回状态', NULL,
        '2025-12-04 10:57:35', NULL, '2025-12-08 16:31:37', NULL),
       (18, 1, 1, '待办', '1', 'case_status', NULL, NULL, 'N', NULL, NULL, NULL, 1, '', NULL, '2025-12-05 14:37:28',
        NULL, '2025-12-08 16:31:37', NULL),
       (19, 1, 2, '已办', '0', 'case_status', NULL, NULL, 'N', NULL, NULL, NULL, 1, '', NULL, '2025-12-05 14:37:28',
        NULL, '2025-12-08 16:31:37', NULL),
       (21, 0, 1, 'GET', 'GET', 'req_method', NULL, NULL, 'N', NULL, NULL, NULL, 1, 'GET请求', NULL,
        '2026-02-06 14:59:30', NULL, '2026-02-06 14:59:30', NULL);
INSERT INTO sys_dict_data (dict_code, tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class,
                           is_default, parent_dict_code, linked_dict_type, linked_dict_value, dict_status, remark,
                           create_by, create_time, update_by, update_time, create_dept)
VALUES (22, 0, 2, 'POST', 'POST', 'req_method', NULL, NULL, 'N', NULL, NULL, NULL, 1, 'POST请求', NULL,
        '2026-02-06 14:59:30', NULL, '2026-02-06 14:59:30', NULL),
       (23, 0, 3, 'PUT', 'PUT', 'req_method', NULL, NULL, 'N', NULL, NULL, NULL, 1, 'PUT请求', NULL,
        '2026-02-06 14:59:30', NULL, '2026-02-06 14:59:30', NULL),
       (24, 0, 4, 'DELETE', 'DELETE', 'req_method', NULL, NULL, 'N', NULL, NULL, NULL, 1, 'DELETE请求', NULL,
        '2026-02-06 14:59:30', NULL, '2026-02-06 14:59:30', NULL),
       (25, 0, 5, 'PATCH', 'PATCH', 'req_method', NULL, NULL, 'N', NULL, NULL, NULL, 1, 'PATCH请求', NULL,
        '2026-02-06 14:59:30', NULL, '2026-02-06 14:59:30', NULL),
       (26, 0, 6, 'ALL', 'ALL', 'req_method', NULL, NULL, 'N', NULL, NULL, NULL, 1, '所有请求方式', NULL,
        '2026-02-06 14:59:30', NULL, '2026-02-06 14:59:30', NULL),
       (27, 0, 1, '系统管理', 'sys', 'module_code', NULL, NULL, 'N', NULL, NULL, NULL, 1, '系统管理模块', NULL,
        '2026-02-06 14:59:37', NULL, '2026-02-06 14:59:37', NULL),
       (28, 0, 2, '用户管理', 'user', 'module_code', NULL, NULL, 'N', NULL, NULL, NULL, 1, '用户管理模块', NULL,
        '2026-02-06 14:59:37', NULL, '2026-02-06 14:59:37', NULL),
       (29, 0, 3, '角色管理', 'role', 'module_code', NULL, NULL, 'N', NULL, NULL, NULL, 1, '角色管理模块', NULL,
        '2026-02-06 14:59:37', NULL, '2026-02-06 14:59:37', NULL),
       (30, 0, 4, '菜单管理', 'menu', 'module_code', NULL, NULL, 'N', NULL, NULL, NULL, 1, '菜单管理模块', NULL,
        '2026-02-06 14:59:37', NULL, '2026-02-06 14:59:37', NULL),
       (31, 0, 5, '部门管理', 'dept', 'module_code', NULL, NULL, 'N', NULL, NULL, NULL, 1, '部门管理模块', NULL,
        '2026-02-06 14:59:37', NULL, '2026-02-06 14:59:37', NULL);
INSERT INTO sys_dict_data (dict_code, tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class,
                           is_default, parent_dict_code, linked_dict_type, linked_dict_value, dict_status, remark,
                           create_by, create_time, update_by, update_time, create_dept)
VALUES (32, 0, 6, '岗位管理', 'post', 'module_code', NULL, NULL, 'N', NULL, NULL, NULL, 1, '岗位管理模块', NULL,
        '2026-02-06 14:59:37', NULL, '2026-02-06 14:59:37', NULL),
       (33, 0, 7, '字典管理', 'dict', 'module_code', NULL, NULL, 'N', NULL, NULL, NULL, 1, '字典管理模块', NULL,
        '2026-02-06 14:59:37', NULL, '2026-02-06 14:59:37', NULL),
       (34, 0, 8, '参数管理', 'config', 'module_code', NULL, NULL, 'N', NULL, NULL, NULL, 1, '参数管理模块', NULL,
        '2026-02-06 14:59:37', NULL, '2026-02-06 14:59:37', NULL),
       (35, 0, 9, '通知管理', 'notice', 'module_code', NULL, NULL, 'N', NULL, NULL, NULL, 1, '通知管理模块', NULL,
        '2026-02-06 14:59:37', NULL, '2026-02-06 14:59:37', NULL),
       (36, 0, 10, '日志管理', 'log', 'module_code', NULL, NULL, 'N', NULL, NULL, NULL, 1, '日志管理模块', NULL,
        '2026-02-06 14:59:37', NULL, '2026-02-06 14:59:37', NULL),
       (37, 0, 11, '文件管理', 'file', 'module_code', NULL, NULL, 'N', NULL, NULL, NULL, 1, '文件管理模块', NULL,
        '2026-02-06 14:59:37', NULL, '2026-02-06 14:59:37', NULL),
       (38, 0, 12, '任务管理', 'job', 'module_code', NULL, NULL, 'N', NULL, NULL, NULL, 1, '任务管理模块', NULL,
        '2026-02-06 14:59:37', NULL, '2026-02-06 14:59:37', NULL),
       (39, 0, 13, '数据权限', 'datascope', 'module_code', NULL, NULL, 'N', NULL, NULL, NULL, 1, '数据权限模块', NULL,
        '2026-02-06 14:59:37', NULL, '2026-02-06 14:59:37', NULL),
       (40, 0, 14, 'API配置', 'apiconfig', 'module_code', NULL, NULL, 'N', NULL, NULL, NULL, 1, 'API配置管理模块', NULL,
        '2026-02-06 14:59:37', NULL, '2026-02-06 14:59:37', NULL),
       (41, 1, 0, '图形验证码', 'graphical', 'captcha_type', NULL, 'default', 'N', NULL, NULL, NULL, 1, NULL, 1,
        '2026-02-26 10:13:12', 1, '2026-02-26 10:13:12', 2);
INSERT INTO sys_dict_data (dict_code, tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class,
                           is_default, parent_dict_code, linked_dict_type, linked_dict_value, dict_status, remark,
                           create_by, create_time, update_by, update_time, create_dept)
VALUES (42, 1, 0, '滑块验证码', 'slider', 'captcha_type', NULL, 'default', 'N', NULL, NULL, NULL, 1, NULL, 1,
        '2026-02-26 10:13:28', 1, '2026-02-26 10:13:28', 2),
       (43, 1, 0, '短信验证码', 'sms', 'captcha_type', NULL, 'default', 'N', NULL, NULL, NULL, 1, NULL, 1,
        '2026-02-26 10:13:42', 1, '2026-02-26 10:13:42', 2),
       (44, 1, 0, '姓名+手机号', 'name_phone', 'water_marker_content', NULL, 'default', 'N', NULL, NULL, NULL, 1, NULL,
        1, '2026-02-26 10:19:16', 1, '2026-02-26 10:19:16', 2),
       (45, 1, 1, '微信', 'WECHAT', 'sys_social_platform', NULL, NULL, 'N', NULL, NULL, NULL, 1, '社交平台-微信', NULL,
        '2026-04-01 14:46:00', NULL, '2026-04-01 14:47:32', NULL),
       (46, 1, 2, '微信开放平台', 'WECHAT_OPEN', 'sys_social_platform', NULL, NULL, 'N', NULL, NULL, NULL, 1,
        '社交平台-微信开放平台', NULL, '2026-04-01 14:46:00', NULL, '2026-04-01 14:47:33', NULL),
       (47, 1, 3, '微信小程序', 'WECHAT_MINI', 'sys_social_platform', NULL, NULL, 'N', NULL, NULL, NULL, 1,
        '社交平台-微信小程序', NULL, '2026-04-01 14:46:00', NULL, '2026-04-01 14:47:33', NULL),
       (48, 1, 4, '钉钉', 'DINGTALK', 'sys_social_platform', NULL, NULL, 'N', NULL, NULL, NULL, 1, '社交平台-钉钉',
        NULL, '2026-04-01 14:46:00', NULL, '2026-04-01 14:47:33', NULL),
       (49, 1, 5, '企业微信', 'WECHAT_ENTERPRISE', 'sys_social_platform', NULL, NULL, 'N', NULL, NULL, NULL, 1,
        '社交平台-企业微信', NULL, '2026-04-01 14:46:00', NULL, '2026-04-01 14:47:33', NULL),
       (50, 1, 6, 'GitHub', 'GITHUB', 'sys_social_platform', NULL, NULL, 'N', NULL, NULL, NULL, 1, '社交平台-GitHub',
        NULL, '2026-04-01 14:46:00', NULL, '2026-04-01 14:47:33', NULL),
       (51, 1, 7, 'Gitee', 'GITEE', 'sys_social_platform', NULL, NULL, 'N', NULL, NULL, NULL, 1, '社交平台-Gitee', NULL,
        '2026-04-01 14:46:00', NULL, '2026-04-01 14:47:33', NULL);
INSERT INTO sys_dict_data (dict_code, tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class,
                           is_default, parent_dict_code, linked_dict_type, linked_dict_value, dict_status, remark,
                           create_by, create_time, update_by, update_time, create_dept)
VALUES (52, 1, 8, 'QQ', 'QQ', 'sys_social_platform', NULL, NULL, 'N', NULL, NULL, NULL, 1, '社交平台-QQ', NULL,
        '2026-04-01 14:46:00', NULL, '2026-04-01 14:47:33', NULL),
       (53, 1, 9, '微博', 'WEIBO', 'sys_social_platform', NULL, NULL, 'N', NULL, NULL, NULL, 1, '社交平台-微博', NULL,
        '2026-04-01 14:46:00', NULL, '2026-04-01 14:47:33', NULL),
       (54, 1, 10, '支付宝', 'ALIPAY', 'sys_social_platform', NULL, NULL, 'N', NULL, NULL, NULL, 1, '社交平台-支付宝',
        NULL, '2026-04-01 14:46:00', NULL, '2026-04-01 14:47:33', NULL),
       (55, 1, 11, '百度', 'BAIDU', 'sys_social_platform', NULL, NULL, 'N', NULL, NULL, NULL, 1, '社交平台-百度', NULL,
        '2026-04-01 14:46:00', NULL, '2026-04-01 14:47:33', NULL),
       (56, 1, 12, '谷歌', 'GOOGLE', 'sys_social_platform', NULL, NULL, 'N', NULL, NULL, NULL, 1, '社交平台-谷歌', NULL,
        '2026-04-01 14:46:00', NULL, '2026-04-01 14:47:33', NULL),
       (57, 1, 13, 'Facebook', 'FACEBOOK', 'sys_social_platform', NULL, NULL, 'N', NULL, NULL, NULL, 1,
        '社交平台-Facebook', NULL, '2026-04-01 14:46:00', NULL, '2026-04-01 14:47:34', NULL),
       (58, 1, 14, 'Twitter', 'TWITTER', 'sys_social_platform', NULL, NULL, 'N', NULL, NULL, NULL, 1,
        '社交平台-Twitter', NULL, '2026-04-01 14:46:00', NULL, '2026-04-01 14:47:34', NULL),
       (59, 1, 15, '小米', 'XIAOMI', 'sys_social_platform', NULL, NULL, 'N', NULL, NULL, NULL, 1, '社交平台-小米', NULL,
        '2026-04-01 14:46:00', NULL, '2026-04-01 14:47:34', NULL),
       (60, 1, 16, '华为', 'HUAWEI', 'sys_social_platform', NULL, NULL, 'N', NULL, NULL, NULL, 1, '社交平台-华为', NULL,
        '2026-04-01 14:46:00', NULL, '2026-04-01 14:47:34', NULL),
       (61, 1, 17, '飞书', 'FEISHU', 'sys_social_platform', NULL, NULL, 'N', NULL, NULL, NULL, 1, '社交平台-飞书', NULL,
        '2026-04-01 14:46:00', NULL, '2026-04-01 14:47:34', NULL);
INSERT INTO sys_dict_data (dict_code, tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class,
                           is_default, parent_dict_code, linked_dict_type, linked_dict_value, dict_status, remark,
                           create_by, create_time, update_by, update_time, create_dept)
VALUES (62, 1, 18, '钉钉企业内部', 'DINGTALK_ACCOUNT', 'sys_social_platform', NULL, NULL, 'N', NULL, NULL, NULL, 1,
        '社交平台-钉钉企业内部', NULL, '2026-04-01 14:46:00', NULL, '2026-04-01 14:47:34', NULL),
       (63, 1, 19, '自定义', 'CUSTOM', 'sys_social_platform', NULL, NULL, 'N', NULL, NULL, NULL, 1, '社交平台-自定义',
        NULL, '2026-04-01 14:46:00', NULL, '2026-04-01 14:47:34', NULL),
       (64, 1, 2, '停用', '0', 'sys_normal_disable', NULL, 'default', 'N', NULL, NULL, NULL, 1, '社交平台-自定义', NULL,
        '2026-04-01 14:46:00', 1, '2026-04-26 17:28:46', NULL),
       (65, 1, 1, '正常', '1', 'sys_normal_disable', NULL, 'default', 'N', NULL, NULL, NULL, 1, '社交平台-自定义', NULL,
        '2026-04-01 14:46:00', 1, '2026-04-26 17:28:46', NULL),
       (66, 1, 1, '阿里百炼', 'dashscope', 'ai_provider_type', NULL, 'default', 'N', NULL, NULL, NULL, 1,
        '阿里百炼平台', NULL, '2026-04-17 16:28:59', NULL, '2026-04-17 17:04:24', NULL),
       (67, 1, 2, 'OpenAI', 'openai', 'ai_provider_type', NULL, 'success', 'N', NULL, NULL, NULL, 1, 'OpenAI平台', NULL,
        '2026-04-17 16:28:59', NULL, '2026-04-17 17:03:34', NULL),
       (68, 1, 3, '智谱 AI', 'zhipu', 'ai_provider_type', NULL, 'info', 'N', NULL, NULL, NULL, 1, '智谱AI平台', NULL,
        '2026-04-17 16:28:59', NULL, '2026-04-17 17:03:34', NULL),
       (69, 1, 4, 'Moonshot', 'moonshot', 'ai_provider_type', NULL, 'default', 'N', NULL, NULL, NULL, 1, 'Moonshot平台',
        NULL, '2026-04-17 16:28:59', NULL, '2026-04-17 17:03:34', NULL),
       (70, 1, 5, 'DeepSeek', 'deepseek', 'ai_provider_type', NULL, 'default', 'N', NULL, NULL, NULL, 1, 'DeepSeek平台',
        NULL, '2026-04-17 16:28:59', NULL, '2026-04-17 17:03:34', NULL),
       (71, 1, 6, 'Ollama（本地）', 'ollama', 'ai_provider_type', NULL, 'warning', 'N', NULL, NULL, NULL, 1,
        'Ollama本地部署', NULL, '2026-04-17 16:28:59', NULL, '2026-04-17 17:03:34', NULL);
INSERT INTO sys_dict_data (dict_code, tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class,
                           is_default, parent_dict_code, linked_dict_type, linked_dict_value, dict_status, remark,
                           create_by, create_time, update_by, update_time, create_dept)
VALUES (72, 1, 7, 'Azure', 'azure', 'ai_provider_type', NULL, 'info', 'N', NULL, NULL, NULL, 1, 'Azure OpenAI', NULL,
        '2026-04-17 16:28:59', NULL, '2026-04-17 17:03:34', NULL),
       (73, 1, 8, '自定义', 'custom', 'ai_provider_type', NULL, 'default', 'N', NULL, NULL, NULL, 1, '自定义供应商',
        NULL, '2026-04-17 16:28:59', NULL, '2026-04-17 17:03:34', NULL),
       (74, 1, 1, '对话', 'chat', 'ai_model_type', NULL, 'success', 'N', NULL, NULL, NULL, 1, '对话模型', NULL,
        '2026-04-17 16:28:59', NULL, '2026-04-17 17:03:34', NULL),
       (75, 1, 2, '向量化', 'embedding', 'ai_model_type', NULL, 'info', 'N', NULL, NULL, NULL, 1, '向量化模型', NULL,
        '2026-04-17 16:28:59', NULL, '2026-04-17 17:03:34', NULL),
       (76, 1, 3, '图像生成', 'image', 'ai_model_type', NULL, 'warning', 'N', NULL, NULL, NULL, 1, '图像生成模型', NULL,
        '2026-04-17 16:28:59', NULL, '2026-04-17 17:03:34', NULL),
       (77, 1, 4, '语音', 'audio', 'ai_model_type', NULL, 'default', 'N', NULL, NULL, NULL, 1, '语音模型', NULL,
        '2026-04-17 16:28:59', NULL, '2026-04-17 17:03:34', NULL),
       (78, 1, 1, '正常', '0', 'ai_status', NULL, 'success', 'Y', NULL, NULL, NULL, 1, '正常状态', NULL,
        '2026-04-17 16:28:59', NULL, '2026-04-17 17:03:35', NULL),
       (79, 1, 2, '停用', '1', 'ai_status', NULL, 'error', 'N', NULL, NULL, NULL, 1, '停用状态', NULL,
        '2026-04-17 16:28:59', NULL, '2026-04-17 17:03:35', NULL),
       (80, 1, 1, '否', '0', 'ai_is_default', NULL, 'default', 'Y', NULL, NULL, NULL, 1, '非默认', NULL,
        '2026-04-17 16:28:59', NULL, '2026-04-17 17:03:35', NULL),
       (81, 1, 2, '是', '1', 'ai_is_default', NULL, 'success', 'N', NULL, NULL, NULL, 1, '默认', NULL,
        '2026-04-17 16:28:59', NULL, '2026-04-17 17:03:35', NULL);
INSERT INTO sys_dict_data (dict_code, tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class,
                           is_default, parent_dict_code, linked_dict_type, linked_dict_value, dict_status, remark,
                           create_by, create_time, update_by, update_time, create_dept)
VALUES (82, 1, 1, '待付款', '1', 'order_status', NULL, 'default', 'N', NULL, NULL, NULL, 1, NULL, 1,
        '2026-04-22 13:02:15', 1, '2026-04-22 13:02:15', 2),
       (83, 1, 2, '已付款', '2', 'order_status', NULL, 'default', 'N', NULL, NULL, NULL, 1, NULL, 1,
        '2026-04-22 13:02:15', 1, '2026-04-22 13:02:15', 2),
       (84, 1, 3, '已发货', '3', 'order_status', NULL, 'default', 'N', NULL, NULL, NULL, 1, NULL, 1,
        '2026-04-22 13:02:16', 1, '2026-04-22 13:02:16', 2),
       (87, 1, 1, '普通订单', '1', 'order_type', NULL, 'default', 'N', NULL, NULL, NULL, 1, NULL, 1,
        '2026-04-22 13:02:16', 1, '2026-04-22 13:02:16', 2),
       (88, 1, 2, '团购订单', '2', 'order_type', NULL, 'default', 'N', NULL, NULL, NULL, 1, NULL, 1,
        '2026-04-22 13:02:16', 1, '2026-04-22 13:02:16', 2),
       (89, 1, 3, '秒杀订单', '3', 'order_type', NULL, 'default', 'N', NULL, NULL, NULL, 1, NULL, 1,
        '2026-04-22 13:02:16', 1, '2026-04-22 13:02:16', 2),
       (90, 1, 4, '测试', '4', 'order_type', NULL, 'default', 'N', NULL, NULL, NULL, 1, NULL, 1, '2026-04-22 14:45:50',
        1, '2026-04-22 14:45:50', 2),
       (91, 1, 1, '研发部', '1', 'sys_dept', NULL, 'default', 'N', NULL, NULL, NULL, 1, NULL, 1, '2026-04-26 17:28:46',
        1, '2026-04-26 17:28:46', 2),
       (92, 1, 2, '市场部', '2', 'sys_dept', NULL, 'default', 'N', NULL, NULL, NULL, 1, NULL, 1, '2026-04-26 17:28:46',
        1, '2026-04-26 17:28:46', 2),
       (93, 1, 3, '人事部', '3', 'sys_dept', NULL, 'default', 'N', NULL, NULL, NULL, 1, NULL, 1, '2026-04-26 17:28:46',
        1, '2026-04-26 17:28:46', 2);
INSERT INTO sys_dict_data (dict_code, tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class,
                           is_default, parent_dict_code, linked_dict_type, linked_dict_value, dict_status, remark,
                           create_by, create_time, update_by, update_time, create_dept)
VALUES (94, 1, 1, '电子产品', '1', 'product_category', NULL, 'default', 'N', NULL, NULL, NULL, 1, NULL, 1,
        '2026-04-26 18:09:11', 1, '2026-04-26 18:09:11', 2),
       (95, 1, 2, '服装鞋帽', '2', 'product_category', NULL, 'default', 'N', NULL, NULL, NULL, 1, NULL, 1,
        '2026-04-26 18:09:11', 1, '2026-04-26 18:09:11', 2),
       (96, 1, 3, '家居日用', '3', 'product_category', NULL, 'default', 'N', NULL, NULL, NULL, 1, NULL, 1,
        '2026-04-26 18:09:11', 1, '2026-04-26 18:09:11', 2),
       (97, 1, 1, '正常', '1', 'common_status', NULL, 'default', 'N', NULL, NULL, NULL, 1, NULL, 1,
        '2026-04-26 18:09:12', 1, '2026-04-26 18:09:12', 2),
       (98, 1, 2, '停用', '0', 'common_status', NULL, 'default', 'N', NULL, NULL, NULL, 1, NULL, 1,
        '2026-04-26 18:09:12', 1, '2026-04-26 18:09:12', 2);
INSERT INTO sys_dict_type (dict_id, tenant_id, dict_name, dict_type, dict_status, remark, create_by, create_time,
                           update_by, update_time, create_dept)
VALUES (1, 1, '用户性别', 'sys_user_sex', 1, '用户性别列表', NULL, '2025-11-12 17:41:18', NULL, '2025-11-12 17:41:18',
        NULL),
       (2, 1, '用户状态', 'sys_user_status', 1, '用户状态列表', NULL, '2025-11-12 17:41:18', NULL,
        '2025-11-12 17:41:18', NULL),
       (3, 1, '组织类型', 'sys_org_type', 1, '组织类型列表', NULL, '2025-11-12 17:41:18', NULL, '2025-11-12 17:41:18',
        NULL),
       (4, 1, '是否', 'yes_no', 1, '是否', NULL, '2025-11-12 17:41:18', NULL, '2025-11-12 17:41:18', NULL),
       (5, 0, '公告类型', 'sys_notice_type', 1, '通知公告类型字典', NULL, '2025-12-04 10:57:35', NULL,
        '2025-12-04 10:57:35', NULL),
       (6, 0, '公告发布状态', 'sys_notice_status', 1, '通知公告发布状态字典', NULL, '2025-12-04 10:57:35', NULL,
        '2025-12-04 10:57:35', NULL),
       (7, 0, '案件状态', 'case_status', 1, '案件信息案件状态字典', NULL, '2025-12-05 14:37:28', NULL,
        '2025-12-05 14:37:28', NULL),
       (8, 1, '123123', '123123123123', 1, NULL, NULL, '2025-12-11 16:34:43', NULL, '2025-12-11 16:34:43', NULL),
       (9, 1, '测试', '121', 1, NULL, 1, '2026-01-20 14:34:25', 1, '2026-01-20 14:34:25', 1),
       (10, 0, '请求方式', 'req_method', 1, 'API请求方式字典', NULL, '2026-02-06 14:58:52', NULL, '2026-02-06 14:58:52',
        NULL);
INSERT INTO sys_dict_type (dict_id, tenant_id, dict_name, dict_type, dict_status, remark, create_by, create_time,
                           update_by, update_time, create_dept)
VALUES (11, 0, '业务模块', 'module_code', 1, '业务模块编码字典', NULL, '2026-02-06 14:59:33', NULL,
        '2026-02-06 14:59:33', NULL),
       (12, 1, '验证码类型', 'captcha_type', 1, NULL, 1, '2026-02-26 10:12:04', 1, '2026-02-26 10:12:04', 2),
       (13, 1, '水印内容', 'water_marker_content', 1, NULL, 1, '2026-02-26 10:18:47', 1, '2026-02-26 10:18:47', 2),
       (14, 1, '三方登录平台', 'sys_social_platform', 1, NULL, 1, '2026-02-26 10:18:47', 1, '2026-02-26 10:18:47', 2),
       (15, 1, '通用状态', 'sys_normal_disable', 1, NULL, 1, '2026-02-26 10:18:47', 1, '2026-02-26 10:18:47', 2),
       (16, 1, 'AI供应商类型', 'ai_provider_type', 1, 'AI供应商类型列表', NULL, '2026-04-17 16:28:58', NULL,
        '2026-04-17 17:03:19', NULL),
       (17, 1, 'AI模型类型', 'ai_model_type', 1, 'AI模型类型列表', NULL, '2026-04-17 16:28:58', NULL,
        '2026-04-17 17:03:19', NULL),
       (18, 1, 'AI状态', 'ai_status', 1, 'AI模块状态(0正常 1停用)', NULL, '2026-04-17 16:28:59', NULL,
        '2026-04-17 17:03:19', NULL),
       (19, 1, 'AI是否默认', 'ai_is_default', 1, 'AI模块是否默认(0否 1是)', NULL, '2026-04-17 16:28:59', NULL,
        '2026-04-17 17:03:19', NULL),
       (22, 1, '订单状态', 'order_status', 1, 'AI CRUD 生成器自动创建', 1, '2026-04-22 13:02:15', 1,
        '2026-04-22 13:02:15', 2);
INSERT INTO sys_dict_type (dict_id, tenant_id, dict_name, dict_type, dict_status, remark, create_by, create_time,
                           update_by, update_time, create_dept)
VALUES (23, 1, '订单类型', 'order_type', 1, 'AI CRUD 生成器自动创建', 1, '2026-04-22 13:02:16', 1,
        '2026-04-22 13:02:16', 2),
       (29, 1, '订单状态', '订单状态 (order_status)', 1, 'AI CRUD 生成器自动创建', 1, '2026-04-22 15:08:42', 1,
        '2026-04-22 15:08:42', 2),
       (30, 1, '订单类型', '订单类型 (order_type)', 1, 'AI CRUD 生成器自动创建', 1, '2026-04-22 15:08:42', 1,
        '2026-04-22 15:08:42', 2),
       (32, 1, '产品分类', 'product_category', 1, 'AI CRUD 生成器自动创建', 1, '2026-04-26 18:09:11', 1,
        '2026-04-26 18:09:11', 2),
       (33, 1, '通用状态', 'common_status', 1, 'AI CRUD 生成器自动创建', 1, '2026-04-26 18:09:12', 1,
        '2026-04-26 18:09:12', 2);
INSERT INTO sys_email_config (id, config_id, smtp_server, port, username, password, from_address, from_name, is_ssl,
                              is_auth, retry_interval, max_retries, status, tenant_id, create_time, update_time,
                              create_by, update_by, remark)
VALUES (1, '12', '1', 465, '1', '12', '12', NULL, 1, 1, 5, 1, 1, 1, '2026-04-02 10:32:42', '2026-04-02 10:32:42', NULL,
        NULL, NULL);
INSERT INTO sys_excel_column_config (id, config_key, field_name, column_name, width, order_num, export, date_format,
                                     number_format, dict_type, create_time, update_time, create_by, update_by,
                                     create_dept)
VALUES (7, 'order_list_export', 'orderId', '订单编号', 25, 1, 1, NULL, NULL, NULL, '2025-11-28 15:24:53',
        '2025-11-28 15:24:53', NULL, NULL, NULL),
       (8, 'order_list_export', 'userName', '用户名', 20, 2, 1, NULL, NULL, NULL, '2025-11-28 15:24:53',
        '2025-11-28 15:24:53', NULL, NULL, NULL),
       (9, 'order_list_export', 'totalAmount', '订单金额', 15, 3, 1, NULL, NULL, NULL, '2025-11-28 15:24:53',
        '2025-11-28 15:24:53', NULL, NULL, NULL),
       (10, 'order_list_export', 'orderStatus', '订单状态', 15, 4, 1, NULL, NULL, 'order_status', '2025-11-28 15:24:53',
        '2025-11-28 15:24:53', NULL, NULL, NULL),
       (11, 'order_list_export', 'orderStatusName', '状态说明', 15, 5, 1, NULL, NULL, NULL, '2025-11-28 15:24:53',
        '2025-11-28 15:24:53', NULL, NULL, NULL),
       (12, 'order_list_export', 'createTime', '下单时间', 25, 6, 1, NULL, NULL, NULL, '2025-11-28 15:24:53',
        '2025-11-28 15:24:53', NULL, NULL, NULL),
       (13, 'test_export', 'configName', '参数名称', 25, 6, 1, NULL, NULL, NULL, '2025-11-28 15:24:53',
        '2025-11-28 15:24:53', NULL, NULL, NULL),
       (14, 'test_export', 'configTypeName', '系统内置', 25, 6, 1, NULL, NULL, NULL, '2025-11-28 15:24:53',
        '2025-11-28 15:24:53', NULL, NULL, NULL);
INSERT INTO sys_excel_export_config (id, config_key, export_name, sheet_name, file_name_template, data_source_bean,
                                     query_method, auto_trans, pageable, max_rows, sort_field, sort_order, status,
                                     remark, create_time, update_time, create_by, update_by, create_dept)
VALUES (2, 'order_list_export', '订单列表导出', '订单数据', '订单列表_{date}_{time}.xlsx', 'orderService', 'queryList',
        1, 1, 100000, NULL, NULL, 1, NULL, '2025-11-28 15:24:53', '2025-11-28 15:24:53', NULL, NULL, NULL),
       (3, 'test_export', '测试列表导出', '测试数据', '订单列表_{date}_{time}.xlsx', 'sysConfigServiceImpl',
        'selectConfigList', 0, 0, 100000, NULL, NULL, 1, NULL, '2025-11-28 15:24:53', '2025-11-28 15:24:53', NULL, NULL,
        NULL);
INSERT INTO sys_file_group (id, group_name, group_code, group_type, parent_id, sort, icon, description, status,
                            create_time, update_time, create_by, update_by, create_dept, deleted)
VALUES (2028326543566475266, '测试', 'group_1772425707002', 'default', 0, 0, NULL, NULL, 1, '2026-03-02 12:28:27',
        '2026-03-02 12:28:27', 1, 1, 2, 0);
INSERT INTO sys_file_metadata (id, file_id, group_id, original_name, storage_name, file_path, file_size, mime_type,
                               extension, md5, storage_type, bucket, access_url, thumbnail_url, business_type,
                               business_id, uploader_id, upload_time, expire_time, is_private, download_count, status,
                               create_time, update_time, create_by, update_by, create_dept)
VALUES (1995383607610363905, '894a3029cb8a4a68ac49909604b82e76', NULL, '订单列表_20251128_160153.xlsx',
        '799fd49d92a1492eae1c781f953d0a59.xlsx', 'test/2025/12/01/799fd49d92a1492eae1c781f953d0a59.xlsx', 8805,
        'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', 'xlsx', 'ce59f3c2161d26c1e21ba348e44367d8',
        'local', NULL, NULL, NULL, 'test', '12313', NULL, '2025-12-01 14:44:59', NULL, 0, 3, 1, '2025-12-01 14:44:59',
        '2026-01-21 16:52:27', NULL, NULL, NULL),
       (1998573617505304578, '7038e1f773974ecb8b4df2c1305f8c66', NULL, '加载失败.png',
        'a111492084ab4a5fb3f149f76b5f8c4c.png', 'tenant-logo/2025/12/10/a111492084ab4a5fb3f149f76b5f8c4c.png', 10982,
        'image/png', 'png', '1019f22d9f7d142b3b55779a2670958b', 'local', NULL, NULL, NULL, 'tenant-logo', '', NULL,
        '2025-12-10 10:00:57', NULL, 0, 48, 1, '2025-12-10 10:00:56', '2025-12-22 11:27:05', NULL, NULL, NULL),
       (1998574675875004418, '9742f806833b4766aa1e6b5d0bb9cc77', NULL, 'shuangyinhao-left.png',
        '6eb0ab691c3545c7a86ef864c0d06265.png', 'tenant-logo/2025/12/10/6eb0ab691c3545c7a86ef864c0d06265.png', 2301,
        'image/png', 'png', '1d54939820770ee76fbfbaf799e7b43a', 'local', NULL, NULL, NULL, 'tenant-logo', '', NULL,
        '2025-12-10 10:05:09', NULL, 0, 0, 1, '2025-12-10 10:05:09', '2025-12-10 10:05:09', NULL, NULL, NULL),
       (1998575436570755073, 'bd6f3afbbaa747c4abac5aea919903a9', NULL, 'WX20251205-172243@2x.png',
        'e7a3a608d16a47d9a68c5e48f2e1d008.png', 'tenant-logo/2025/12/10/e7a3a608d16a47d9a68c5e48f2e1d008.png', 127920,
        'image/png', 'png', '46459966fb0ff4b140fe563312a4cc37', 'local', NULL, NULL, NULL, 'tenant-logo', '', NULL,
        '2025-12-10 10:08:10', NULL, 0, 8, 1, '2025-12-10 10:08:10', '2025-12-15 15:32:32', NULL, NULL, NULL),
       (1998580016318709761, 'd032857c6a5b4c9cb53a7f1cf48c3f03', NULL, '授权失败.png',
        '3c9d3da67951469bb48fa827fa4a63bb.png', 'tenant-logo/2025/12/10/3c9d3da67951469bb48fa827fa4a63bb.png', 10390,
        'image/png', 'png', '1635f617779fc0bde2a4bb511ad10d3a', 'local', NULL, NULL, NULL, 'tenant-logo', '', NULL,
        '2025-12-10 10:26:22', NULL, 0, 20, 1, '2025-12-10 10:26:22', '2025-12-15 15:49:41', NULL, NULL, NULL),
       (1998583764134854657, '50adbeedf9a24f26af8b25ba2c92e875', NULL, '404.png',
        '64328e986faa4d14bf00fd374dbd009a.png', 'tenant-logo/2025/12/10/64328e986faa4d14bf00fd374dbd009a.png', 44997,
        'image/png', 'png', '5d015704860486d2a4511f5823a598b1', 'local', NULL, NULL, NULL, 'tenant-logo', '', NULL,
        '2025-12-10 10:41:16', NULL, 0, 0, 1, '2025-12-10 10:41:16', '2025-12-10 10:41:16', NULL, NULL, NULL),
       (1998684468870590465, 'e0a0ada8fb534f5a8ed968dac8dc4d16', NULL, 'WechatIMG2751.jpg',
        'fcd8f2cacc59440fb2f95ac0b644fd77.jpg', 'tenant-logo/2025/12/10/fcd8f2cacc59440fb2f95ac0b644fd77.jpg', 168006,
        'image/jpeg', 'jpg', '5abdfe8c2dd523a7b5d9adff29841307', 'local', NULL, NULL, NULL, 'tenant-logo', '', NULL,
        '2025-12-10 17:21:26', NULL, 0, 4, 1, '2025-12-10 17:21:25', '2025-12-15 15:17:24', NULL, NULL, NULL),
       (2000459995732258818, 'bb827371e38243b7bd93aed3fb3b3eaf', NULL, '20250915155404.jpg',
        '782172a7247048ec8eb6b8ecbe17784b.jpg', 'tenant-logo/2025/12/15/782172a7247048ec8eb6b8ecbe17784b.jpg', 25510,
        'image/jpeg', 'jpg', '5295bc1edadd7a5cc71e75e06b1e1fe9', 'local', NULL, NULL, NULL, 'tenant-logo', '', NULL,
        '2025-12-15 14:56:44', NULL, 0, 10, 1, '2025-12-15 14:56:44', '2025-12-15 15:42:47', NULL, NULL, NULL),
       (2000461309581209602, '98b6c6e5800942838017377c01e0cd3a', NULL, '箭头.png',
        '57e6ebfc5ab14ed1b75d58984d3e78f4.png', 'tenant-logo/2025/12/15/57e6ebfc5ab14ed1b75d58984d3e78f4.png', 1174,
        'image/png', 'png', '9da239f7e939e80d84937c8aa929d84e', 'local', NULL, NULL, NULL, 'tenant-logo', '', NULL,
        '2025-12-15 15:01:58', NULL, 0, 1, 1, '2025-12-15 15:01:58', '2025-12-15 15:01:58', NULL, NULL, NULL),
       (2000461328061313025, 'be2b996b38124fa49836b613da3e3275', NULL, 'WX20250211-100736@2x.png',
        'ad7d959c40ff434c9a9c3efcf40be1cf.png', 'tenant-logo/2025/12/15/ad7d959c40ff434c9a9c3efcf40be1cf.png', 18398,
        'image/png', 'png', '9043ab2fa05297924ff5179b774e6f3c', 'local', NULL, NULL, NULL, 'tenant-logo', '', NULL,
        '2025-12-15 15:02:02', NULL, 0, 1, 1, '2025-12-15 15:02:02', '2025-12-15 15:02:02', NULL, NULL, NULL);
INSERT INTO sys_file_metadata (id, file_id, group_id, original_name, storage_name, file_path, file_size, mime_type,
                               extension, md5, storage_type, bucket, access_url, thumbnail_url, business_type,
                               business_id, uploader_id, upload_time, expire_time, is_private, download_count, status,
                               create_time, update_time, create_by, update_by, create_dept)
VALUES (2000461876865019906, '1c509478280c4235afea45cd844393d1', NULL, '封面4.png',
        'b3ba5074969a402181b1c3b959d96c63.png', 'tenant-logo/2025/12/15/b3ba5074969a402181b1c3b959d96c63.png', 952400,
        'image/png', 'png', 'c89176b5c887c7f27f5bec84b0ca1230', 'local', NULL, NULL, NULL, 'tenant-logo', '', NULL,
        '2025-12-15 15:04:13', NULL, 0, 1, 1, '2025-12-15 15:04:13', '2025-12-15 15:04:13', NULL, NULL, NULL),
       (2000462497668149250, '59fff30d10fa4d3d9d40385b5c1ec881', NULL, '小区.png',
        '12d834608a7243a89317d980f9d198c4.png', 'tenant-logo/2025/12/15/12d834608a7243a89317d980f9d198c4.png', 2631,
        'image/png', 'png', 'fc68f176ee1dff005097846a206f5c10', 'local', NULL, NULL, NULL, 'tenant-logo', '', NULL,
        '2025-12-15 15:06:41', NULL, 0, 1, 1, '2025-12-15 15:06:41', '2025-12-15 15:06:41', NULL, NULL, NULL),
       (2013872365678669825, '87eb1f5068934e64afdd5366cf199a02', NULL, 'AGENTS.md',
        'e560acb3794c431b90b9d63554a5fc14.md', 'notice/2026/01/21/e560acb3794c431b90b9d63554a5fc14.md', 7404,
        'application/octet-stream', 'md', '4afedec3116c0b200dc32ddfb56e1242', 'local', NULL, NULL, NULL, 'notice', '',
        NULL, '2026-01-21 15:12:43', NULL, 0, 0, 0, '2026-01-21 15:12:43', '2026-01-21 16:47:21', 1, 1, 1),
       (2013898112174702593, 'b061a31437704322b73fd8082013ba2d', NULL, 'aaa.png',
        '1447d4eeb81841799562580e25191c2e.png', 'common/2026/01/21/1447d4eeb81841799562580e25191c2e.png', 33602,
        'image/png', 'png', '164f6b490c03dd0de816275996c2640a', 'local', NULL, NULL, NULL, 'common', '', NULL,
        '2026-01-21 16:55:01', NULL, 0, 8, 1, '2026-01-21 16:55:01', '2026-04-30 16:32:48', 1, 1, 1),
       (2018867842172018690, 'dab7a91179814cd4aecab3064485be30', NULL, '测试2.png',
        'aa363ba19e4f46709e8a793ff025e273.png', 'notice/2026/02/04/aa363ba19e4f46709e8a793ff025e273.png', 74068,
        'image/png', 'png', 'bddaaadbac2a128dde41275556e71e73', 'local', NULL, NULL, NULL, 'notice', '', NULL,
        '2026-02-04 10:02:57', NULL, 0, 0, 1, '2026-02-04 10:02:57', '2026-02-04 10:02:57', 1, 1, 2),
       (2018875660086239233, '06dbb2a0ef77405bbcad2d60fb1d1ad3', 2028326543566475266, 'ceshi.png',
        'd9eb2d3d1dea40d69d125a7086488226.png', 'notice/2026/02/04/d9eb2d3d1dea40d69d125a7086488226.png', 227550,
        'image/png', 'png', 'fe0ad5e51ad816ad3ba06597fb84c4f8', 'local', NULL, NULL, NULL, 'notice', '', NULL,
        '2026-02-04 10:34:01', NULL, 0, 0, 1, '2026-02-04 10:34:01', '2026-03-02 12:28:38', 1, 1, 2),
       (2037815561079451650, 'd80d8edb35b64c8d9e8aa4a9b8db3742', NULL, '6cd18c655f7b34c5acea68e00c7169b0.jpg',
        '1d6387fb548d4457a755c8fb45746680.jpg', 'common/2026/03/28/1d6387fb548d4457a755c8fb45746680.jpg', 3229654,
        'image/jpeg', 'jpg', '7cf0ae993d5126fc12bd38d54d72290d', 'local', NULL, NULL, NULL, 'common', '', NULL,
        '2026-03-28 16:54:25', NULL, 0, 7, 0, '2026-03-28 16:54:25', '2026-03-29 09:00:26', 1, 1, 2),
       (2038058688415440897, '452e56168abd42a489da1f37ddc91f51', NULL, '6cd18c655f7b34c5acea68e00c7169b0.jpg',
        '5ecd44513c414970954bbf65c0bf7e81.jpg', 'common/2026/03/29/5ecd44513c414970954bbf65c0bf7e81.jpg', 3229654,
        'image/jpeg', 'jpg', '7cf0ae993d5126fc12bd38d54d72290d', 'local', NULL, NULL, NULL, 'common', '', NULL,
        '2026-03-29 09:00:31', NULL, 0, 2, 0, '2026-03-29 09:00:31', '2026-03-29 09:33:31', 1, 1, 2),
       (2038075374384357377, '037456d8c761445191e124dc169f2563', NULL, '6cd18c655f7b34c5acea68e00c7169b0.jpg',
        '503247ed57eb49d2ac7cec1b71161cea.jpg', 'common/2026/03/29/503247ed57eb49d2ac7cec1b71161cea.jpg', 3229654,
        'image/jpeg', 'jpg', '7cf0ae993d5126fc12bd38d54d72290d', 'local', NULL, NULL, NULL, 'common', '', NULL,
        '2026-03-29 10:06:50', NULL, 0, 0, 0, '2026-03-29 10:06:50', '2026-03-29 10:06:54', 1, 1, 2),
       (2038076910762115074, 'fc31f00d8ad147e2a0321095081dbe2f', NULL, '6cd18c655f7b34c5acea68e00c7169b0.jpg',
        'common/2026/03/29/796712ba46b145b1877f8d0ac43eb79b.jpg',
        'common/2026/03/29/796712ba46b145b1877f8d0ac43eb79b.jpg', 3229654, 'image/jpeg', 'jpg',
        '7cf0ae993d5126fc12bd38d54d72290d', 'rustfs', 'forge-files', NULL, NULL, 'common', '', NULL,
        '2026-03-29 10:12:56', NULL, 0, 6, 0, '2026-03-29 10:12:56', '2026-04-07 17:06:44', 1, 1, 2);
INSERT INTO sys_file_metadata (id, file_id, group_id, original_name, storage_name, file_path, file_size, mime_type,
                               extension, md5, storage_type, bucket, access_url, thumbnail_url, business_type,
                               business_id, uploader_id, upload_time, expire_time, is_private, download_count, status,
                               create_time, update_time, create_by, update_by, create_dept)
VALUES (2039251663436414977, 'b4427cadbc764298b3a4c7160605dfff', NULL, '5d0d6bd5969dd810f891312640df13c0.jpeg',
        'de6a44b391434ddf8b63ee95bd3815a0.jpeg', 'social-logo/2026/04/01/de6a44b391434ddf8b63ee95bd3815a0.jpeg', 8932,
        'image/jpeg', 'jpeg', '9052c34d6e2cc34f321db38fbaad1f52', 'local', NULL, NULL, NULL, 'social-logo', '', NULL,
        '2026-04-01 16:00:59', NULL, 0, 21, 0, '2026-04-01 16:00:59', '2026-04-07 17:06:40', 1, 1, 2),
       (2041442590568796161, '3d66b8501e874b9085faa98913daf559', NULL, '1231.jpeg',
        'f5d29fb95a1d4e4cb9963c4a2a1ecd7e.jpeg', 'social-logo/2026/04/07/f5d29fb95a1d4e4cb9963c4a2a1ecd7e.jpeg', 8932,
        'image/jpeg', 'jpeg', '9052c34d6e2cc34f321db38fbaad1f52', 'local', NULL, NULL, NULL, 'social-logo', '', NULL,
        '2026-04-07 17:06:56', NULL, 0, 202, 1, '2026-04-07 17:06:56', '2026-05-01 14:15:10', 1, 1, 2),
       (2044642209451929602, '8d82c28036d845b3b254fabcfbc49c41', NULL, 'screenshot-2026-04-16T05-01-04-002Z.png',
        '17575bf113de44ffafc3c902e9535db2.png', 'project_screenshot/2026/04/16/17575bf113de44ffafc3c902e9535db2.png',
        16189, 'image/png', 'png', 'f91c51d4f4674af1a125433e08a6a708', 'local', NULL, NULL, NULL, 'project_screenshot',
        NULL, NULL, '2026-04-16 13:01:05', NULL, 0, 12, 1, '2026-04-16 13:01:05', '2026-04-30 16:32:48', 1, 1, 2),
       (2045067040317784066, '428fbf9216444306ac6065002ff923d7', NULL,
        'O1CN01IU2US71Ciicsi3Br3_!!6000000000115-55-tps-357-76.svg', '563fd4f76a13472183918fadc65ec4ac.svg',
        'ai-provider-logo/2026/04/17/563fd4f76a13472183918fadc65ec4ac.svg', 4602, 'image/svg+xml', 'svg',
        'ea2c3b53bf829310249feaf44dd39280', 'local', NULL, NULL, NULL, 'ai-provider-logo', '', NULL,
        '2026-04-17 17:09:13', NULL, 0, 94, 1, '2026-04-17 17:09:13', '2026-04-30 20:40:44', 1, 1, 2),
       (2049401873680642050, '754c997a39774477b36b350457084048', NULL, '培训计划.jpeg',
        '96199f6e5d3f4038b5b4ccdafcbc950b.jpeg', 'menu-icon/2026/04/29/96199f6e5d3f4038b5b4ccdafcbc950b.jpeg', 128950,
        'image/jpeg', 'jpeg', '9c6846f24f5971c81782217f91621bd2', 'local', NULL, NULL, NULL, 'menu-icon', '', NULL,
        '2026-04-29 16:14:17', NULL, 0, 2, 1, '2026-04-29 16:14:17', '2026-04-30 16:32:48', 1, 1, 2),
       (2049682101418110977, '39bd7a7d2d654222ae98d2c0a4a5173a', NULL, '豆包.png',
        '85aeac18ec774152afa59b2a60bccd2b.png', 'avatar/2026/04/30/85aeac18ec774152afa59b2a60bccd2b.png', 4194601,
        'image/png', 'png', 'e4f5f0a17fd91837b2928267aea70a78', 'local', NULL, NULL, NULL, 'avatar', NULL, NULL,
        '2026-04-30 10:47:49', NULL, 0, 187, 1, '2026-04-30 10:47:49', '2026-04-30 21:39:36', 1, 1, 2);
INSERT INTO sys_file_storage_config (id, config_name, storage_type, is_default, enabled, endpoint, access_key,
                                     secret_key, bucket_name, region, base_path, `domain`, use_https, max_file_size,
                                     allowed_types, order_num, extra_config, create_time, update_time, create_by,
                                     update_by, create_dept)
VALUES (1, '本地存储', 'local', 1, 1, NULL, NULL, NULL, NULL, NULL, '/var/tmp/', NULL, 1, 100,
        'jpg,jpeg,png,gif,pdf,doc,docx,xls,xlsx,zip,rar,md,svg', 1, NULL, '2025-11-28 16:44:18', '2026-04-17 17:07:41',
        NULL, 1, NULL),
       (3, 'RustFS存储', 'rustfs', 0, 1, 'http://127.0.0.1:9000', 'rustfsadmin', 'rustfsadmin', 'forge-files', NULL,
        NULL, NULL, 0, 500, 'jpg,jpeg,png,gif,pdf,doc,docx,xls,xlsx,zip,rar,mp4,mp3', 3, NULL, '2026-03-28 16:25:28',
        '2026-03-29 08:56:07', NULL, 1, NULL);
INSERT INTO sys_flow_business (id, business_key, business_type, process_instance_id, process_def_id, process_def_key,
                               title, status, apply_user_id, apply_user_name, apply_dept_id, apply_dept_name,
                               apply_time, end_time, duration, create_time, update_time)
VALUES ('0d3d53c3c1429a488889215722b064e9', '34232132131', 'todo_flow', 'd092ca7b-24fe-11f1-9590-c68306b388f0',
        'todo_flow:3:6dccf7a9-24f6-11f1-b21b-c68306b388f0', 'todo_flow', 'todo_flow流程', 'running', '1', '超级管理员',
        '2', '研发部', '2026-03-21 16:20:27', NULL, NULL, '2026-03-21 16:20:27', '2026-03-21 16:20:27'),
       ('16ce785b5f039dcd2965973ba17177c5', '6666767', 'todo_flow', '81c54e55-24e8-11f1-aa0e-c68306b388f0',
        'todo_flow:1:4c4d01de-2119-11f1-80d9-b69ad982d03a', 'todo_flow', 'todo_flow流程', 'running', '1', '超级管理员',
        '2', NULL, '2026-03-21 13:40:46', NULL, NULL, '2026-03-21 13:40:46', '2026-03-21 13:40:46'),
       ('17e98ebc181e288631150b3c99c3ea3c', 'LEAVE_B6EC0266969B4DAA', 'leave', '44fd4163-2901-11f1-9444-16f80eb69e69',
        'leave_process:10:08700850-28fe-11f1-9674-e6e756836190', 'leave_process', 'admin 的请假申请', 'approved', '1',
        '超级管理员', '2', '研发部', '2026-03-26 18:48:05', '2026-03-26 18:49:07', 61767, '2026-03-26 18:48:05',
        '2026-03-26 18:48:05'),
       ('2a4d038ea7c2717cacb569b922a7854f', '1231321', 'todo_flow', 'cc59d769-24e0-11f1-9aa5-c68306b388f0',
        'todo_flow:1:4c4d01de-2119-11f1-80d9-b69ad982d03a', 'todo_flow', 'todo_flow流程', 'running', '1', '超级管理员',
        '2', NULL, '2026-03-21 12:45:35', NULL, NULL, '2026-03-21 12:45:35', '2026-03-21 12:45:35'),
       ('33cbab9a661c4a8d2e0ae83c92f837a1', '111111112', 'todo_flow', '4279da9d-24f7-11f1-b04e-c68306b388f0',
        'todo_flow:3:6dccf7a9-24f6-11f1-b21b-c68306b388f0', 'todo_flow', 'todo_flow流程', 'running', '1', '超级管理员',
        '2', NULL, '2026-03-21 15:26:22', NULL, NULL, '2026-03-21 15:26:22', '2026-03-21 15:26:22'),
       ('36351e2b4e099a077cb930c37e93eb88', '121212121', 'todo_flow', '384f1154-2516-11f1-b539-c68306b388f0',
        'todo_flow:6:079e9e98-2516-11f1-a15c-c68306b388f0', 'todo_flow', 'todo_flow流程', 'running', '1', '超级管理员',
        '2', '研发部', '2026-03-21 19:07:59', NULL, NULL, '2026-03-21 19:07:59', '2026-03-21 19:07:59'),
       ('3820414105bb49af01e20ff0aa26063a', 'LEAVE_7EC50E86F49B4A09', 'leave', '5ccfc6c6-2902-11f1-a6c9-16f80eb69e69',
        'leave_process:10:08700850-28fe-11f1-9674-e6e756836190', 'leave_process', 'admin 的请假申请', 'approved', '1',
        '超级管理员', '2', '研发部', '2026-03-26 18:55:55', '2026-03-26 18:56:09', 13990, '2026-03-26 18:55:55',
        '2026-03-26 18:55:55'),
       ('433d7f4ac055f2f9fa7fabc5a683fad5', '9999', 'todo_flow', 'f1517a16-24e7-11f1-98d7-c68306b388f0',
        'todo_flow:1:4c4d01de-2119-11f1-80d9-b69ad982d03a', 'todo_flow', 'todo_flow流程', 'running', '1', '超级管理员',
        '2', NULL, '2026-03-21 13:36:43', NULL, NULL, '2026-03-21 13:36:43', '2026-03-21 13:36:43'),
       ('455cfc4987087e2551c201e68e9d2d3c', '1213123123123', 'todo_flow', '58b694d1-252e-11f1-b539-c68306b388f0',
        'todo_flow:34:42bb9900-252e-11f1-b539-c68306b388f0', 'todo_flow', 'todo_flow流程', 'running', '1', '超级管理员',
        '2', '研发部', '2026-03-21 22:00:41', NULL, NULL, '2026-03-21 22:00:41', '2026-03-21 22:00:41'),
       ('50b523a61298e40928a40a720910fbb7', 'LEAVE_9833FDF482904B01', 'leave', '55bfce06-2903-11f1-a6c9-16f80eb69e69',
        'leave_process:10:08700850-28fe-11f1-9674-e6e756836190', 'leave_process', 'admin 的请假申请', 'approved', '1',
        '超级管理员', '2', '研发部', '2026-03-26 19:02:52', '2026-03-26 19:03:04', 12352, '2026-03-26 19:02:52',
        '2026-03-26 19:02:52');
INSERT INTO sys_flow_business (id, business_key, business_type, process_instance_id, process_def_id, process_def_key,
                               title, status, apply_user_id, apply_user_name, apply_dept_id, apply_dept_name,
                               apply_time, end_time, duration, create_time, update_time)
VALUES ('58d9750786edf2f2437b5aa29b0a8ae6', '11212121', 'todo_flow', '4e074215-24f2-11f1-9a51-c68306b388f0',
        'todo_flow:2:f07729f9-24e8-11f1-870a-c68306b388f0', 'todo_flow', 'todo_flow流程', 'running', '1', '超级管理员',
        '2', NULL, '2026-03-21 14:50:54', NULL, NULL, '2026-03-21 14:50:54', '2026-03-21 14:50:54'),
       ('6319b90709b0576c77cdc570016d6a5b', '12321312312312321', 'todo_flow', '97752e96-24f6-11f1-85ed-c68306b388f0',
        'todo_flow:3:6dccf7a9-24f6-11f1-b21b-c68306b388f0', 'todo_flow', 'todo_flow流程', 'running', '1', '超级管理员',
        '2', NULL, '2026-03-21 15:21:35', NULL, NULL, '2026-03-21 15:21:35', '2026-03-21 15:21:35'),
       ('661994c312949e301f21e9225e35b960', 'hhh12313', 'todo_flow', 'fcd0f320-24f7-11f1-828d-c68306b388f0',
        'todo_flow:3:6dccf7a9-24f6-11f1-b21b-c68306b388f0', 'todo_flow', 'todo_flow流程', 'running', '1', '超级管理员',
        '2', NULL, '2026-03-21 15:31:35', NULL, NULL, '2026-03-21 15:31:35', '2026-03-21 15:31:35'),
       ('6639876d01d30132a5eaa58f263914db', 'LEAVE_8E319D3F139243D6', 'leave', 'c2e261ed-2904-11f1-b7a8-16f80eb69e69',
        'leave_process:10:08700850-28fe-11f1-9674-e6e756836190', 'leave_process', 'admin 的请假申请', 'approved', '1',
        '超级管理员', '2', '研发部', '2026-03-26 19:13:05', '2026-03-26 19:13:17', 12435, '2026-03-26 19:13:05',
        '2026-03-26 19:13:05'),
       ('6a04fe90d4d6defd5c937aaed98d71fb', '12121212', 'todo_flow', 'faef3734-24f3-11f1-8215-c68306b388f0',
        'todo_flow:2:f07729f9-24e8-11f1-870a-c68306b388f0', 'todo_flow', 'todo_flow流程', 'running', '1', '超级管理员',
        '2', NULL, '2026-03-21 15:02:54', NULL, NULL, '2026-03-21 15:02:54', '2026-03-21 15:02:54'),
       ('6ca5ba085fc63773d9b34ff0e54d5f46', '7777', 'todo_flow', '5a18dc50-24e7-11f1-98d7-c68306b388f0',
        'todo_flow:1:4c4d01de-2119-11f1-80d9-b69ad982d03a', 'todo_flow', 'todo_flow流程', 'running', '1', '超级管理员',
        '2', NULL, '2026-03-21 13:32:30', NULL, NULL, '2026-03-21 13:32:30', '2026-03-21 13:32:30'),
       ('6e3b60716f613b2e197d2814230bb97c', 'LEAVE_84CB073379C3414D', 'leave', '2144b101-28fe-11f1-9674-e6e756836190',
        'leave_process:10:08700850-28fe-11f1-9674-e6e756836190', 'leave_process', 'admin 的请假申请', 'approved', '1',
        '超级管理员', '2', '研发部', '2026-03-26 18:25:37', '2026-03-26 18:37:04', 687340, '2026-03-26 18:25:37',
        '2026-03-26 18:25:37'),
       ('7065183acfc2c60ebb4957519003bef7', 'LEAVE_AD395CD3AF304AD1', 'leave', 'dad177a6-2902-11f1-a6c9-16f80eb69e69',
        'leave_process:10:08700850-28fe-11f1-9674-e6e756836190', 'leave_process', 'admin 的请假申请', 'approved', '1',
        '超级管理员', '2', '研发部', '2026-03-26 18:59:26', '2026-03-26 18:59:39', 12865, '2026-03-26 18:59:26',
        '2026-03-26 18:59:26'),
       ('70e13ba7636a4238fd26890391505c67', 'LEAVE_9BA9DECBFDB34DED', 'leave', 'cb32264b-2906-11f1-b933-16f80eb69e69',
        'leave_process:10:08700850-28fe-11f1-9674-e6e756836190', 'leave_process', 'admin 的请假申请', 'approved', '1',
        '超级管理员', '2', '研发部', '2026-03-26 19:27:38', '2026-03-26 19:29:20', 102002, '2026-03-26 19:27:38',
        '2026-03-26 19:27:38'),
       ('77405c3cd1770cf36bdc944193289c97', '111222', 'todo_flow', '03235aea-24e7-11f1-98d7-c68306b388f0',
        'todo_flow:1:4c4d01de-2119-11f1-80d9-b69ad982d03a', 'todo_flow', 'todo_flow流程', 'running', '1', '超级管理员',
        '2', NULL, '2026-03-21 13:30:04', NULL, NULL, '2026-03-21 13:30:04', '2026-03-21 13:30:04');
INSERT INTO sys_flow_business (id, business_key, business_type, process_instance_id, process_def_id, process_def_key,
                               title, status, apply_user_id, apply_user_name, apply_dept_id, apply_dept_name,
                               apply_time, end_time, duration, create_time, update_time)
VALUES ('7a73fa38227bc712393c0b608a2eea20', 'LEAVE_F752BF1F5D234E34', 'leave', 'cd7973ce-2905-11f1-a323-16f80eb69e69',
        'leave_process:10:08700850-28fe-11f1-9674-e6e756836190', 'leave_process', 'admin 的请假申请', 'approved', '1',
        '超级管理员', '2', '研发部', '2026-03-26 19:20:32', '2026-03-26 19:23:55', 202875, '2026-03-26 19:20:32',
        '2026-03-26 19:20:32'),
       ('7dab5f277675b3fa85231447cdc951e4', 'LEAVE_C9B9D2736FE14D0C', 'leave', 'f9112374-25ee-11f1-a191-c68306b388f0',
        'leave_process:2:8cbcd859-25e9-11f1-90d0-c68306b388f0', 'leave_process', '年假申请 - admin', 'running', '1',
        'admin', NULL, NULL, '2026-03-22 20:59:33', NULL, NULL, '2026-03-22 20:59:33', '2026-03-22 20:59:33'),
       ('90596d537711b26335e3d6967fa4dbff', 'LEAVE_1CB98984B0B542AB', 'leave', 'cee537c7-2910-11f1-88d0-16f80eb69e69',
        'leave_process:10:08700850-28fe-11f1-9674-e6e756836190', 'leave_process', 'admin 的请假申请', 'approved', '1',
        '超级管理员', '2', '研发部', '2026-03-26 20:39:19', '2026-04-15 19:49:02', 1724982771, '2026-03-26 20:39:19',
        '2026-03-26 20:39:19'),
       ('9348aa43b1f7c8b7c4c76183f0f227c9', '1111', 'todo_flow', '55136be2-24e6-11f1-9c9f-c68306b388f0',
        'todo_flow:1:4c4d01de-2119-11f1-80d9-b69ad982d03a', 'todo_flow', 'todo_flow流程', 'running', '1', '超级管理员',
        '2', NULL, '2026-03-21 13:25:12', NULL, NULL, '2026-03-21 13:25:12', '2026-03-21 13:25:12'),
       ('9590dc0e307d13e928c4d39f058806c0', '111', 'todo_flow', '99fecdce-2437-11f1-87d2-b69ad982d03a',
        'todo_flow:1:4c4d01de-2119-11f1-80d9-b69ad982d03a', 'todo_flow', 'todo_flow流程', 'running', '1', '超级管理员',
        '2', NULL, '2026-03-20 16:34:26', NULL, NULL, '2026-03-20 16:34:26', '2026-03-20 16:34:26'),
       ('9a838cef01a9f733cb8f535209acca49', 'LEAVE_C055B79F68ED4079', 'leave', '861f7966-2909-11f1-9585-16f80eb69e69',
        'leave_process:10:08700850-28fe-11f1-9674-e6e756836190', 'leave_process', 'admin 的请假申请', 'approved', '1',
        '超级管理员', '2', '研发部', '2026-03-26 19:47:10', '2026-04-28 19:35:46', 2850516169, '2026-03-26 19:47:10',
        '2026-03-26 19:47:10'),
       ('a15ee68d36d2a17f915debfbb0a1b268', '989897', 'todo_flow', '145487aa-24e9-11f1-870a-c68306b388f0',
        'todo_flow:2:f07729f9-24e8-11f1-870a-c68306b388f0', 'todo_flow', 'todo_flow流程', 'running', '1', '超级管理员',
        '2', NULL, '2026-03-21 13:44:52', NULL, NULL, '2026-03-21 13:44:52', '2026-03-21 13:44:52'),
       ('abf781c245e8ecec7aaa4228c7b5ffe1', 'LEAVE_83FB20E0F70E4ACD', 'leave', '077c0183-28f2-11f1-8984-e6e756836190',
        'leave_process:7:edaf9ac8-28eb-11f1-810f-e6e756836190', 'leave_process', 'admin 的请假申请', 'approved', '1',
        '超级管理员', '2', '研发部', '2026-03-26 16:58:59', '2026-03-26 17:16:30', 1051081, '2026-03-26 16:58:59',
        '2026-03-26 16:58:59'),
       ('ae72f5780d03c1fce29f664112e86a00', 'LEAVE_5835B6CF47C547D7', 'leave', '31b7ec44-28fd-11f1-9674-e6e756836190',
        'leave_process:9:2a237673-28fd-11f1-9674-e6e756836190', 'leave_process', 'admin 的请假申请', 'running', '1',
        '超级管理员', '2', '研发部', '2026-03-26 18:18:55', NULL, NULL, '2026-03-26 18:18:55', '2026-03-26 18:18:55'),
       ('ce92043e4e2cdfa50f234dd1a53515a3', 'LEAVE_71D5E9DF8ACD47CF', 'leave', 'c5d263f7-2909-11f1-950e-16f80eb69e69',
        'leave_process:10:08700850-28fe-11f1-9674-e6e756836190', 'leave_process', 'admin 的请假申请', 'approved', '1',
        '超级管理员', '2', '研发部', '2026-03-26 19:48:57', '2026-03-26 19:50:01', 64199, '2026-03-26 19:48:57',
        '2026-03-26 19:48:57');
INSERT INTO sys_flow_business (id, business_key, business_type, process_instance_id, process_def_id, process_def_key,
                               title, status, apply_user_id, apply_user_name, apply_dept_id, apply_dept_name,
                               apply_time, end_time, duration, create_time, update_time)
VALUES ('d8659d4ca4e7704e27a338ced3184ee8', 'hjkhsahdak', 'todo_flow', 'a249f101-24f8-11f1-90cd-c68306b388f0',
        'todo_flow:3:6dccf7a9-24f6-11f1-b21b-c68306b388f0', 'todo_flow', 'todo_flow流程', 'running', '1', '超级管理员',
        '2', NULL, '2026-03-21 15:36:12', NULL, NULL, '2026-03-21 15:36:12', '2026-03-21 15:36:12'),
       ('d8afe6519f86357cdb8f606c33c1cd58', 'LEAVE_76B0FB0F24504EDD', 'leave', '18949516-2902-11f1-a6c9-16f80eb69e69',
        'leave_process:10:08700850-28fe-11f1-9674-e6e756836190', 'leave_process', 'admin 的请假申请', 'approved', '1',
        '超级管理员', '2', '研发部', '2026-03-26 18:54:00', '2026-03-26 18:54:15', 14693, '2026-03-26 18:54:00',
        '2026-03-26 18:54:00'),
       ('db1a0938ad20c512831afe63d2864d3a', '1231312', 'todo_flow', '35cb69e1-250a-11f1-b85c-c68306b388f0',
        'todo_flow:3:6dccf7a9-24f6-11f1-b21b-c68306b388f0', 'todo_flow', 'todo_flow流程', 'running', '1', '超级管理员',
        '2', '研发部', '2026-03-21 17:42:01', NULL, NULL, '2026-03-21 17:42:01', '2026-03-21 17:42:01'),
       ('e6a0e67b61363cf6f2184afd27dd42f7', 'LEAVE_3804972B33BE410F', 'leave', 'a362c0b3-2901-11f1-9444-16f80eb69e69',
        'leave_process:10:08700850-28fe-11f1-9674-e6e756836190', 'leave_process', 'admin 的请假申请', 'approved', '1',
        '超级管理员', '2', '研发部', '2026-03-26 18:50:44', '2026-03-26 18:50:55', 10930, '2026-03-26 18:50:44',
        '2026-03-26 18:50:44');
INSERT INTO sys_flow_category (id, category_code, category_name, description, sort_order, status, create_time,
                               update_time)
VALUES ('1', 'leave', '请假流程', '员工请假申请审批', 1, 1, '2026-03-15 18:29:16', '2026-03-15 18:29:16'),
       ('3a3f1c1ad95be8cb6a3dc16be414a790', '234243', '测试分类', NULL, 0, 1, '2026-04-30 10:17:23',
        '2026-04-30 10:17:23');
INSERT INTO sys_flow_form (id, form_key, form_name, form_type, form_schema, form_url, component_path, form_config,
                           version, status, description, tenant_id, create_by, create_time, update_by, update_time,
                           deleted)
VALUES (1, 'leave_form', '请假申请表单', 'dynamic',
        '[{"type":"input","field":"title","label":"申请标题","props":{"placeholder":"请输入标题"},"rules":[{"required":true,"message":"请输入标题"}]},{"type":"select","field":"leaveType","label":"请假类型","props":{"options":[{"label":"事假","value":"1"},{"label":"病假","value":"2"},{"label":"年假","value":"3"},{"label":"婚假","value":"4"},{"label":"产假","value":"5"}]},"rules":[{"required":true,"message":"请选择请假类型"}]},{"type":"datePicker","field":"startDate","label":"开始日期","rules":[{"required":true,"message":"请选择开始日期"}]},{"type":"datePicker","field":"endDate","label":"结束日期","rules":[{"required":true,"message":"请选择结束日期"}]},{"type":"inputNumber","field":"days","label":"请假天数","props":{"min":0.5,"precision":1},"rules":[{"required":true,"message":"请输入请假天数"}]},{"type":"textarea","field":"reason","label":"请假原因","props":{"placeholder":"请输入请假原因","rows":3},"rules":[{"required":true,"message":"请输入请假原因"}]}]',
        NULL, NULL, NULL, 1, 1, '员工请假申请表单', NULL, NULL, '2026-03-19 14:33:12', NULL, '2026-03-19 14:33:12', 0),
       (2, 'expense_form', '报销申请表单', 'dynamic',
        '[{"type":"input","field":"title","label":"报销标题","props":{"placeholder":"请输入报销标题"},"rules":[{"required":true,"message":"请输入报销标题"}]},{"type":"select","field":"expenseType","label":"报销类型","props":{"options":[{"label":"差旅费","value":"1"},{"label":"办公费","value":"2"},{"label":"招待费","value":"3"},{"label":"交通费","value":"4"},{"label":"其他","value":"5"}]},"rules":[{"required":true,"message":"请选择报销类型"}]},{"type":"inputNumber","field":"amount","label":"报销金额","props":{"min":0,"precision":2,"prefix":"¥"},"rules":[{"required":true,"message":"请输入报销金额"}]},{"type":"datePicker","field":"expenseDate","label":"报销日期","rules":[{"required":true,"message":"请选择报销日期"}]},{"type":"textarea","field":"description","label":"报销说明","props":{"placeholder":"请输入报销说明","rows":3}},{"type":"upload","field":"attachments","label":"附件","props":{"multiple":true,"accept":".jpg,.jpeg,.png,.pdf"}}]',
        NULL, NULL, NULL, 1, 1, '费用报销申请表单', NULL, NULL, '2026-03-19 14:33:12', NULL, '2026-03-19 14:33:12', 0),
       (3, 'approval_form', '通用审批表单', 'dynamic',
        '[{"type":"input","field":"title","label":"申请标题","props":{"placeholder":"请输入申请标题"},"rules":[{"required":true,"message":"请输入申请标题"}]},{"type":"select","field":"approvalType","label":"审批类型","props":{"options":[{"label":"通用审批","value":"1"},{"label":"合同审批","value":"2"},{"label":"项目审批","value":"3"},{"label":"其他","value":"4"}]},"rules":[{"required":true,"message":"请选择审批类型"}]},{"type":"textarea","field":"content","label":"申请内容","props":{"placeholder":"请输入申请内容","rows":4},"rules":[{"required":true,"message":"请输入申请内容"}]},{"type":"upload","field":"attachments","label":"附件","props":{"multiple":true}}]',
        NULL, NULL, NULL, 1, 1, '通用审批申请表单', NULL, NULL, '2026-03-19 14:33:12', NULL, '2026-03-19 14:33:12', 0);
INSERT INTO sys_flow_model (id, model_key, model_name, description, category, flow_type, form_type, form_id, form_json,
                            version, process_definition_id, deployment_id, deployment_key, status, deploy_time,
                            last_update_by, create_by, create_time, update_time, del_flag, bpmn_xml, notify_type,
                            webhook_url)
VALUES ('176ede959303191c4d226471e2558fbc', 'le-app', '流程', '', '234243', 'app', 'dynamic', NULL, NULL, 1, NULL, NULL,
        NULL, 0, NULL, NULL, NULL, '2026-04-30 14:40:58', '2026-04-30 14:40:58', 0, NULL, 'none', ''),
       ('2572074a70f3c1bac8d6d3bfe2484454', 'todo_flow', '请假流程', '', 'leave', '', 'none', NULL, '', 68,
        'todo_flow:34:42bb9900-252e-11f1-b539-c68306b388f0', '42b5f3ad-252e-11f1-b539-c68306b388f0', 'todo_flow_v67', 1,
        '2026-03-21 22:00:05', NULL, NULL, '2026-03-16 16:56:23', '2026-03-21 22:00:04', 0, '<?xml version="1.0" encoding="UTF-8"?>
<bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL" xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI" xmlns:dc="http://www.omg.org/spec/DD/20100524/DC" xmlns:di="http://www.omg.org/spec/DD/20100524/DI" xmlns:flowable="http://flowable.org/bpmn" targetNamespace="http://flowable.org/processdef">
  <bpmn:process id="Process_1" name="新流程" isExecutable="true">
    <bpmn:startEvent id="StartEvent_1" name="开始">
      <bpmn:outgoing>Flow_0cw8tps</bpmn:outgoing>
    </bpmn:startEvent>
    <bpmn:sequenceFlow id="Flow_0cw8tps" sourceRef="StartEvent_1" targetRef="Activity_1lfbh0g" />
    <bpmn:userTask id="Activity_1lfbh0g" name="用户任务" flowable:assignee="${initiator}" flowable:candidateUsers="" flowable:formUrl="/test/approve12" flowable:formTarget="modal">
      <bpmn:extensionElements>
        <flowable:taskListener event="create" class="com.mdframe.forge.starter.flow.listener.TestListener" />
        <flowable:executionListener event="start" class="com.mdframe.forge.starter.flow.listener.TestListener" />
      </bpmn:extensionElements>
      <bpmn:incoming>Flow_0cw8tps</bpmn:incoming>
      <bpmn:outgoing>Flow_05q0uln</bpmn:outgoing>
    </bpmn:userTask>
    <bpmn:sequenceFlow id="Flow_05q0uln" sourceRef="Activity_1lfbh0g" targetRef="Event_0esht4l" />
    <bpmn:endEvent id="Event_0esht4l" name="结束">
      <bpmn:incoming>Flow_05q0uln</bpmn:incoming>
      <bpmn:terminateEventDefinition id="TerminateEventDefinition_0wpbthp" />
    </bpmn:endEvent>
  </bpmn:process>
  <bpmndi:BPMNDiagram id="BPMNDiagram_1">
    <bpmndi:BPMNPlane id="BPMNPlane_1" bpmnElement="Process_1">
      <bpmndi:BPMNShape id="StartEvent_1_di" bpmnElement="StartEvent_1">
        <dc:Bounds x="172" y="262" width="36" height="36" />
        <bpmndi:BPMNLabel>
          <dc:Bounds x="179" y="298" width="22" height="14" />
        </bpmndi:BPMNLabel>
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape id="Activity_1k77281_di" bpmnElement="Activity_1lfbh0g">
        <dc:Bounds x="260" y="240" width="100" height="80" />
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape id="Event_1ceflgz_di" bpmnElement="Event_0esht4l">
        <dc:Bounds x="412" y="262" width="36" height="36" />
      </bpmndi:BPMNShape>
      <bpmndi:BPMNEdge id="Flow_0cw8tps_di" bpmnElement="Flow_0cw8tps">
        <di:waypoint x="208" y="280" />
        <di:waypoint x="260" y="280" />
      </bpmndi:BPMNEdge>
      <bpmndi:BPMNEdge id="Flow_05q0uln_di" bpmnElement="Flow_05q0uln">
        <di:waypoint x="360" y="280" />
        <di:waypoint x="412" y="280" />
      </bpmndi:BPMNEdge>
    </bpmndi:BPMNPlane>
  </bpmndi:BPMNDiagram>
</bpmn:definitions>
', NULL, NULL),
       ('4326b53aac9f5a50666e33fc2ecfcf79', '1', '1', '', 'leave', '', 'dynamic', NULL, '', 1, NULL, NULL, NULL, 0,
        NULL, NULL, NULL, '2026-04-17 10:55:43', '2026-04-23 14:37:33', 0, '<?xml version="1.0" encoding="UTF-8"?>
<bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL" xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI" xmlns:dc="http://www.omg.org/spec/DD/20100524/DC" targetNamespace="http://flowable.org/processdef">
  <bpmn:process id="Process_1" name="新流程" isExecutable="true">
    <bpmn:startEvent id="StartEvent_1" name="开始" />
  </bpmn:process>
  <bpmndi:BPMNDiagram id="BPMNDiagram_1">
    <bpmndi:BPMNPlane id="BPMNPlane_1" bpmnElement="Process_1">
      <bpmndi:BPMNShape id="StartEvent_1_di" bpmnElement="StartEvent_1">
        <dc:Bounds x="180" y="160" width="36" height="36" />
        <bpmndi:BPMNLabel>
          <dc:Bounds x="187" y="196" width="22" height="14" />
        </bpmndi:BPMNLabel>
      </bpmndi:BPMNShape>
    </bpmndi:BPMNPlane>
  </bpmndi:BPMNDiagram>
</bpmn:definitions>
', 'none', ''),
       ('559ff4deba36ef466bffedd658c4e107', '22', '22', '2', 'leave', '', 'dynamic', NULL, NULL, 1, NULL, NULL, NULL, 0,
        NULL, NULL, NULL, '2026-03-25 17:07:52', '2026-03-25 17:16:45', 1, NULL, 'none', ''),
       ('5cc2f8f2f4b7391347c7de4f46b85b71', 'it', 'IT信息上报', '', '234243', '', 'dynamic', NULL,
        '[{"type":"description","field":"field_1","label":"测","defaultValue":null,"required":false,"disabled":false,"props":{"text":"描述内容"},"rules":[{"required":false,"message":"请输入描述文本"}]},{"type":"input","field":"用户名","label":"用户名","defaultValue":null,"required":true,"disabled":false,"props":{"placeholder":"请输入","clearable":true,"maxLength":null,"showCount":false},"rules":[{"required":false,"message":"请输入输入框"}]},{"type":"inputNumber","field":"field_3","label":"性别","defaultValue":null,"required":true,"disabled":false,"props":{"placeholder":"请输入数字","min":null,"max":null,"precision":0,"step":1},"rules":[{"required":false,"message":"请输入数字输入"}]}]',
        1, NULL, NULL, NULL, 0, NULL, NULL, NULL, '2026-04-30 10:17:56', '2026-04-30 22:32:38', 0, '<?xml version="1.0" encoding="UTF-8"?>
<bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL" xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI" xmlns:dc="http://www.omg.org/spec/DD/20100524/DC" targetNamespace="http://flowable.org/processdef">
  <bpmn:process id="Process_1" name="新流程" isExecutable="true">
    <bpmn:startEvent id="StartEvent_1" name="开始" />
    <bpmn:exclusiveGateway id="Gateway_0sj11dp" />
  </bpmn:process>
  <bpmndi:BPMNDiagram id="BPMNDiagram_1">
    <bpmndi:BPMNPlane id="BPMNPlane_1" bpmnElement="Process_1">
      <bpmndi:BPMNShape id="StartEvent_1_di" bpmnElement="StartEvent_1">
        <dc:Bounds x="180" y="160" width="36" height="36" />
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape id="Gateway_0sj11dp_di" bpmnElement="Gateway_0sj11dp" isMarkerVisible="true">
        <dc:Bounds x="495" y="225" width="50" height="50" />
      </bpmndi:BPMNShape>
    </bpmndi:BPMNPlane>
  </bpmndi:BPMNDiagram>
</bpmn:definitions>
', 'none', ''),
       ('f1c16641298465456fa120d9b59eb635', '11', '11', '', 'leave', '', 'none', NULL, NULL, 1, NULL, NULL, NULL, 0,
        NULL, NULL, NULL, '2026-03-25 17:07:44', '2026-03-25 17:16:42', 1, NULL, 'none', ''),
       ('ffd827049ebaee51c8564e12101eaadb', 'leave_process', '请假流程测试', '请假流程测试', 'leave', NULL, 'dynamic',
        NULL, '', 15, 'leave_process:10:08700850-28fe-11f1-9674-e6e756836190', '0869518d-28fe-11f1-9674-e6e756836190',
        'leave_process_v14', 1, '2026-03-26 18:24:56', NULL, NULL, '2026-03-22 20:16:51', '2026-04-30 11:10:44', 0, '<?xml version="1.0" encoding="UTF-8"?>
<bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL" xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI" xmlns:dc="http://www.omg.org/spec/DD/20100524/DC" xmlns:di="http://www.omg.org/spec/DD/20100524/DI" xmlns:flowable="http://flowable.org/bpmn" xmlns:modeler="http://flowable.org/modeler" id="Definitions_1" targetNamespace="http://flowable.org/demo" exporter="Flowable Modeler" exporterVersion="1.0">
  <bpmn:process id="leave_process" name="请假流程" isExecutable="true">
    <bpmn:startEvent id="startEvent" name="开始">
      <bpmn:outgoing>flow_start_to_apply</bpmn:outgoing>
    </bpmn:startEvent>
    <bpmn:userTask id="approveTask" name="部门领导审批" flowable:assignee="${initiator}" flowable:formUrl="/leave/LeaveApproveForm">
      <bpmn:extensionElements />
      <bpmn:incoming>flow_start_to_apply</bpmn:incoming>
      <bpmn:outgoing>flow_approve_to_end</bpmn:outgoing>
    </bpmn:userTask>
    <bpmn:endEvent id="endEvent" name="结束">
      <bpmn:incoming>flow_approve_to_end</bpmn:incoming>
    </bpmn:endEvent>
    <bpmn:sequenceFlow id="flow_start_to_apply" name="提交申请" sourceRef="startEvent" targetRef="approveTask" />
    <bpmn:sequenceFlow id="flow_approve_to_end" name="完成" sourceRef="approveTask" targetRef="endEvent" />
  </bpmn:process>
  <bpmndi:BPMNDiagram id="BPMNDiagram_leave_process">
    <bpmndi:BPMNPlane id="BPMNPlane_leave_process" bpmnElement="leave_process">
      <bpmndi:BPMNShape id="BPMNShape_startEvent" bpmnElement="startEvent">
        <dc:Bounds x="100" y="200" width="36" height="36" />
        <bpmndi:BPMNLabel>
          <dc:Bounds x="108" y="246" width="22" height="14" />
        </bpmndi:BPMNLabel>
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape id="BPMNShape_approveTask" bpmnElement="approveTask">
        <dc:Bounds x="240" y="178" width="100" height="80" />
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape id="BPMNShape_endEvent" bpmnElement="endEvent">
        <dc:Bounds x="560" y="200" width="36" height="36" />
        <bpmndi:BPMNLabel>
          <dc:Bounds x="568" y="246" width="22" height="14" />
        </bpmndi:BPMNLabel>
      </bpmndi:BPMNShape>
      <bpmndi:BPMNEdge id="BPMNEdge_flow_start_to_apply" bpmnElement="flow_start_to_apply">
        <di:waypoint x="136" y="218" />
        <di:waypoint x="240" y="218" />
        <bpmndi:BPMNLabel>
          <dc:Bounds x="134" y="200" width="44" height="14" />
        </bpmndi:BPMNLabel>
      </bpmndi:BPMNEdge>
      <bpmndi:BPMNEdge id="BPMNEdge_flow_approve_to_end" bpmnElement="flow_approve_to_end">
        <di:waypoint x="340" y="218" />
        <di:waypoint x="560" y="218" />
        <bpmndi:BPMNLabel>
          <dc:Bounds x="431" y="200" width="22" height="14" />
        </bpmndi:BPMNLabel>
      </bpmndi:BPMNEdge>
    </bpmndi:BPMNPlane>
  </bpmndi:BPMNDiagram>
</bpmn:definitions>
', 'webhook', 'http://127.0.0.1:8080/leave/test');
INSERT INTO sys_flow_task (id, task_id, task_name, task_def_key, task_def_id, process_instance_id, process_def_id,
                           process_def_key, business_key, business_type, title, assignee, assignee_name,
                           candidate_users, candidate_groups, owner, due_date, priority, status, comment,
                           attachment_urls, start_user_id, start_user_name, start_dept_id, start_dept_name, create_time,
                           claim_time, complete_time)
VALUES ('0704f28c1cf702da3297459635b89564', '4500c3e9-2901-11f1-9444-16f80eb69e69', '部门领导审批', 'approveTask', NULL,
        '44fd4163-2901-11f1-9444-16f80eb69e69', 'leave_process:10:08700850-28fe-11f1-9674-e6e756836190',
        'leave_process', 'LEAVE_B6EC0266969B4DAA', 'leave', 'admin 的请假申请', '1', NULL, NULL, NULL, NULL, NULL, 50,
        2, '21313', NULL, '1', '超级管理员', '2', '研发部', '2026-03-26 18:48:06', NULL, '2026-03-26 18:49:08'),
       ('0764edb04a264f83326667cc308bec48', 'd0953b8a-24fe-11f1-9590-c68306b388f0', '用户任务', 'Activity_1lfbh0g',
        NULL, 'd092ca7b-24fe-11f1-9590-c68306b388f0', 'todo_flow:3:6dccf7a9-24f6-11f1-b21b-c68306b388f0', 'todo_flow',
        NULL, NULL, '用户任务 - todo_flow', '1', NULL, NULL, NULL, NULL, NULL, 50, 2, '121', NULL, NULL, NULL, NULL,
        NULL, '2026-03-21 16:20:27', NULL, '2026-03-21 16:43:25'),
       ('13b92f6d426ce892da288387709d6387', 'a24dc1a0-24f8-11f1-90cd-c68306b388f0', '用户任务', 'Activity_1lfbh0g',
        NULL, 'a249f101-24f8-11f1-90cd-c68306b388f0', 'todo_flow:3:6dccf7a9-24f6-11f1-b21b-c68306b388f0', 'todo_flow',
        NULL, NULL, '用户任务 - todo_flow', '1', NULL, NULL, NULL, NULL, NULL, 50, 2, '1231', NULL, NULL, NULL, NULL,
        NULL, '2026-03-21 15:36:12', NULL, '2026-03-21 17:41:32'),
       ('1fba444c987d7e0fae01c9200458ea36', '35cd8cd0-250a-11f1-b85c-c68306b388f0', '用户任务', 'Activity_1lfbh0g',
        NULL, '35cb69e1-250a-11f1-b85c-c68306b388f0', 'todo_flow:3:6dccf7a9-24f6-11f1-b21b-c68306b388f0', 'todo_flow',
        '1231312', 'todo_flow', 'todo_flow流程', '1', NULL, NULL, NULL, NULL, NULL, 50, 2, '额外企鹅群', NULL, '1',
        '超级管理员', '2', '研发部', '2026-03-21 17:42:01', NULL, '2026-03-21 17:54:08'),
       ('22c41eb2c68abe86a1314d8c6e49d11d', '3851a975-2516-11f1-b539-c68306b388f0', '用户任务', 'Activity_1lfbh0g',
        NULL, '384f1154-2516-11f1-b539-c68306b388f0', 'todo_flow:6:079e9e98-2516-11f1-a15c-c68306b388f0', 'todo_flow',
        '121212121', 'todo_flow', 'todo_flow流程', '1', NULL, NULL, NULL, NULL, NULL, 50, 0, NULL, NULL, '1',
        '超级管理员', '2', '研发部', '2026-03-21 19:07:59', NULL, NULL),
       ('369cbb00b3d260793a63f9d8c4f0f089', '55c0df8c-2903-11f1-a6c9-16f80eb69e69', '部门领导审批', 'approveTask', NULL,
        '55bfce06-2903-11f1-a6c9-16f80eb69e69', 'leave_process:10:08700850-28fe-11f1-9674-e6e756836190',
        'leave_process', 'LEAVE_9833FDF482904B01', 'leave', 'admin 的请假申请', '1', NULL, NULL, NULL, NULL, NULL, 50,
        2, '12313', NULL, '1', '超级管理员', '2', '研发部', '2026-03-26 19:02:53', NULL, '2026-03-26 19:03:05'),
       ('3be40254f9cd3016179fbd051cd3b423', '8621ea77-2909-11f1-9585-16f80eb69e69', '部门领导审批', 'approveTask', NULL,
        '861f7966-2909-11f1-9585-16f80eb69e69', 'leave_process:10:08700850-28fe-11f1-9674-e6e756836190',
        'leave_process', 'LEAVE_C055B79F68ED4079', 'leave', 'admin 的请假申请', '1', NULL, NULL, NULL, NULL, NULL, 50,
        2, '123', NULL, '1', '超级管理员', '2', '研发部', '2026-03-26 19:47:11', NULL, '2026-04-28 19:35:46'),
       ('4351f8a62f615cbf76b87e81f2c1e58f', 'c5d57148-2909-11f1-950e-16f80eb69e69', '部门领导审批', 'approveTask', NULL,
        'c5d263f7-2909-11f1-950e-16f80eb69e69', 'leave_process:10:08700850-28fe-11f1-9674-e6e756836190',
        'leave_process', 'LEAVE_71D5E9DF8ACD47CF', 'leave', 'admin 的请假申请', '1', NULL, NULL, NULL, NULL, NULL, 50,
        2, '12313', NULL, '1', '超级管理员', '2', '研发部', '2026-03-26 19:48:58', NULL, '2026-03-26 19:50:02'),
       ('46607e6e103a159c685c546a60300076', '58bf2060-252e-11f1-b539-c68306b388f0', '用户任务', 'Activity_1lfbh0g',
        NULL, '58b694d1-252e-11f1-b539-c68306b388f0', 'todo_flow:34:42bb9900-252e-11f1-b539-c68306b388f0', 'todo_flow',
        '1213123123123', 'todo_flow', 'todo_flow流程', '1', NULL, NULL, NULL, NULL, NULL, 50, 0, NULL, NULL, '1',
        '超级管理员', '2', '研发部', '2026-03-21 22:00:41', NULL, NULL),
       ('4f7b2b1308a02a2247f3e0c9ef4d89d1', 'dad23b0c-2902-11f1-a6c9-16f80eb69e69', '部门领导审批', 'approveTask', NULL,
        'dad177a6-2902-11f1-a6c9-16f80eb69e69', 'leave_process:10:08700850-28fe-11f1-9674-e6e756836190',
        'leave_process', 'LEAVE_AD395CD3AF304AD1', 'leave', 'admin 的请假申请', '1', NULL, NULL, NULL, NULL, NULL, 50,
        2, '12313', NULL, '1', '超级管理员', '2', '研发部', '2026-03-26 18:59:26', NULL, '2026-03-26 18:59:40');
INSERT INTO sys_flow_task (id, task_id, task_name, task_def_key, task_def_id, process_instance_id, process_def_id,
                           process_def_key, business_key, business_type, title, assignee, assignee_name,
                           candidate_users, candidate_groups, owner, due_date, priority, status, comment,
                           attachment_urls, start_user_id, start_user_name, start_dept_id, start_dept_name, create_time,
                           claim_time, complete_time)
VALUES ('551fc1528b2d9115f6e2782cc83f1686', 'cee7cfe8-2910-11f1-88d0-16f80eb69e69', '部门领导审批', 'approveTask', NULL,
        'cee537c7-2910-11f1-88d0-16f80eb69e69', 'leave_process:10:08700850-28fe-11f1-9674-e6e756836190',
        'leave_process', 'LEAVE_1CB98984B0B542AB', 'leave', 'admin 的请假申请', '1', NULL, NULL, NULL, NULL, NULL, 50,
        2, '同意', NULL, '1', '超级管理员', '2', '研发部', '2026-03-26 20:39:19', NULL, '2026-04-15 19:49:02'),
       ('708398d6705e0039dfb07150ce3f5be5', 'cb35f6f1-2906-11f1-b933-16f80eb69e69', '部门领导审批', 'approveTask', NULL,
        'cb32264b-2906-11f1-b933-16f80eb69e69', 'leave_process:10:08700850-28fe-11f1-9674-e6e756836190',
        'leave_process', 'LEAVE_9BA9DECBFDB34DED', 'leave', 'admin 的请假申请', '1', NULL, NULL, NULL, NULL, NULL, 50,
        2, '111', NULL, '1', '超级管理员', '2', '研发部', '2026-03-26 19:27:38', NULL, '2026-03-26 19:29:21'),
       ('75d792db8d0d57183e0d1c828db743d5', 'fcd511df-24f7-11f1-828d-c68306b388f0', '用户任务', 'Activity_1lfbh0g',
        NULL, 'fcd0f320-24f7-11f1-828d-c68306b388f0', 'todo_flow:3:6dccf7a9-24f6-11f1-b21b-c68306b388f0', 'todo_flow',
        NULL, NULL, '用户任务 - todo_flow', '1', NULL, NULL, NULL, NULL, NULL, 50, 2, '同意', NULL, NULL, NULL, NULL,
        NULL, '2026-03-21 15:31:34', NULL, '2026-04-30 09:20:00'),
       ('83b6abd0cb731f8d00054f06f6c08b1d', '21452647-28fe-11f1-9674-e6e756836190', '部门领导审批', 'approveTask', NULL,
        '2144b101-28fe-11f1-9674-e6e756836190', 'leave_process:10:08700850-28fe-11f1-9674-e6e756836190',
        'leave_process', 'LEAVE_84CB073379C3414D', 'leave', 'admin 的请假申请', '1', NULL, NULL, NULL, NULL, NULL, 50,
        3, '111', NULL, '1', '超级管理员', '2', '研发部', '2026-03-26 18:25:37', NULL, '2026-03-26 18:37:05'),
       ('966b04f8bebd3b9acfb7f7c4a5de67af', 'f9151b26-25ee-11f1-a191-c68306b388f0', '请假申请', 'applyTask', NULL,
        'f9112374-25ee-11f1-a191-c68306b388f0', 'leave_process:2:8cbcd859-25e9-11f1-90d0-c68306b388f0', 'leave_process',
        'LEAVE_C9B9D2736FE14D0C', 'leave', '年假申请 - admin', '1', NULL, NULL, NULL, NULL, NULL, 50, 0, NULL, NULL,
        '1', 'admin', NULL, NULL, '2026-03-22 20:59:34', NULL, NULL),
       ('a2cb8426f6e38b998d73b47f0cad2e70', '31bc321a-28fd-11f1-9674-e6e756836190', '部门领导审批', 'approveTask', NULL,
        '31b7ec44-28fd-11f1-9674-e6e756836190', 'leave_process:9:2a237673-28fd-11f1-9674-e6e756836190', 'leave_process',
        'LEAVE_5835B6CF47C547D7', 'leave', 'admin 的请假申请', '1', NULL, NULL, NULL, NULL, NULL, 50, 0, NULL, NULL,
        '1', '超级管理员', '2', '研发部', '2026-03-26 18:18:55', NULL, NULL),
       ('aef2a367039cb78b1fc8a03e437a516a', '5cd03c0c-2902-11f1-a6c9-16f80eb69e69', '部门领导审批', 'approveTask', NULL,
        '5ccfc6c6-2902-11f1-a6c9-16f80eb69e69', 'leave_process:10:08700850-28fe-11f1-9674-e6e756836190',
        'leave_process', 'LEAVE_7EC50E86F49B4A09', 'leave', 'admin 的请假申请', '1', NULL, NULL, NULL, NULL, NULL, 50,
        2, '12313', NULL, '1', '超级管理员', '2', '研发部', '2026-03-26 18:55:55', NULL, '2026-03-26 18:56:13'),
       ('afd6728312c6879f2b5ee38c9d1938b9', '078158c9-28f2-11f1-8984-e6e756836190', '部门领导审批', 'approveTask', NULL,
        '077c0183-28f2-11f1-8984-e6e756836190', 'leave_process:7:edaf9ac8-28eb-11f1-810f-e6e756836190', 'leave_process',
        'LEAVE_83FB20E0F70E4ACD', 'leave', 'admin 的请假申请', '2', NULL, NULL, NULL, NULL, NULL, 50, 2, '1231', NULL,
        '1', '超级管理员', '2', '研发部', '2026-03-26 16:59:00', NULL, '2026-03-26 17:16:31'),
       ('ba542fa02ac5cc7c7eee5c4d575185a2', 'cd7d6b84-2905-11f1-a323-16f80eb69e69', '部门领导审批', 'approveTask', NULL,
        'cd7973ce-2905-11f1-a323-16f80eb69e69', 'leave_process:10:08700850-28fe-11f1-9674-e6e756836190',
        'leave_process', 'LEAVE_F752BF1F5D234E34', 'leave', 'admin 的请假申请', '1', NULL, NULL, NULL, NULL, NULL, 50,
        2, '1231', NULL, '1', '超级管理员', '2', '研发部', '2026-03-26 19:20:33', NULL, '2026-03-26 19:24:30'),
       ('e7ac7185200a8f4d765cfc97721e0e88', 'a3638419-2901-11f1-9444-16f80eb69e69', '部门领导审批', 'approveTask', NULL,
        'a362c0b3-2901-11f1-9444-16f80eb69e69', 'leave_process:10:08700850-28fe-11f1-9674-e6e756836190',
        'leave_process', 'LEAVE_3804972B33BE410F', 'leave', 'admin 的请假申请', '1', NULL, NULL, NULL, NULL, NULL, 50,
        2, '12313', NULL, '1', '超级管理员', '2', '研发部', '2026-03-26 18:50:44', NULL, '2026-03-26 18:50:56');
INSERT INTO sys_flow_task (id, task_id, task_name, task_def_key, task_def_id, process_instance_id, process_def_id,
                           process_def_key, business_key, business_type, title, assignee, assignee_name,
                           candidate_users, candidate_groups, owner, due_date, priority, status, comment,
                           attachment_urls, start_user_id, start_user_name, start_dept_id, start_dept_name, create_time,
                           claim_time, complete_time)
VALUES ('ef526e05eafe14e084847816b88fe23e', '1898179c-2902-11f1-a6c9-16f80eb69e69', '部门领导审批', 'approveTask', NULL,
        '18949516-2902-11f1-a6c9-16f80eb69e69', 'leave_process:10:08700850-28fe-11f1-9674-e6e756836190',
        'leave_process', 'LEAVE_76B0FB0F24504EDD', 'leave', 'admin 的请假申请', '1', NULL, NULL, NULL, NULL, NULL, 50,
        2, '12313', NULL, '1', '超级管理员', '2', '研发部', '2026-03-26 18:54:01', NULL, '2026-03-26 18:54:48'),
       ('fb982b1257c7e8ed793e1e8374c591d2', 'c2e6a7c3-2904-11f1-b7a8-16f80eb69e69', '部门领导审批', 'approveTask', NULL,
        'c2e261ed-2904-11f1-b7a8-16f80eb69e69', 'leave_process:10:08700850-28fe-11f1-9674-e6e756836190',
        'leave_process', 'LEAVE_8E319D3F139243D6', 'leave', 'admin 的请假申请', '1', NULL, NULL, NULL, NULL, NULL, 50,
        2, '1231', NULL, '1', '超级管理员', '2', '研发部', '2026-03-26 19:13:05', NULL, '2026-03-26 19:13:18');
INSERT INTO sys_flow_template (id, template_key, template_name, category, description, icon, form_type, form_json,
                               bpmn_xml, thumbnail, variables, version, status, usage_count, create_by, create_time,
                               update_time, is_system, sort_order)
VALUES ('1', 'leave_simple', '简单请假流程', 'leave', '适用于短期请假（3天以内），只需直属领导审批',
        'i-material-symbols:event', 'dynamic',
        '[{"type":"input","field":"title","label":"申请标题","props":{"placeholder":"请输入标题"},"rules":[{"required":true,"message":"请输入标题"}]},{"type":"select","field":"leaveType","label":"请假类型","props":{"options":[{"label":"事假","value":"1"},{"label":"病假","value":"2"},{"label":"年假","value":"3"}]}},{"type":"datePicker","field":"startDate","label":"开始日期"},{"type":"datePicker","field":"endDate","label":"结束日期"},{"type":"inputNumber","field":"days","label":"请假天数"},{"type":"textarea","field":"reason","label":"请假原因"}]',
        '<?xml version="1.0" encoding="UTF-8"?><bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL" xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI" xmlns:dc="http://www.omg.org/spec/DD/20100524/DC" xmlns:flowable="http://flowable.org/bpmn" targetNamespace="http://flowable.org/processdef"><bpmn:process id="leave_simple" name="简单请假流程" isExecutable="true"><bpmn:startEvent id="startEvent" name="开始" flowable:initiator="initiator"/><bpmn:userTask id="approveTask" name="直属领导审批" flowable:assignee="${initiatorLeader}"/><bpmn:endEvent id="endEvent" name="结束"/><bpmn:sequenceFlow id="flow1" sourceRef="startEvent" targetRef="approveTask"/><bpmn:sequenceFlow id="flow2" sourceRef="approveTask" targetRef="endEvent"/></bpmn:process><bpmndi:BPMNDiagram id="BPMNDiagram_1"><bpmndi:BPMNPlane id="BPMNPlane_1" bpmnElement="leave_simple"><bpmndi:BPMNShape id="startEvent_di" bpmnElement="startEvent"><dc:Bounds x="180" y="160" width="36" height="36"/></bpmndi:BPMNShape><bpmndi:BPMNShape id="approveTask_di" bpmnElement="approveTask"><dc:Bounds x="280" y="138" width="100" height="80"/></bpmndi:BPMNShape><bpmndi:BPMNShape id="endEvent_di" bpmnElement="endEvent"><dc:Bounds x="460" y="160" width="36" height="36"/></bpmndi:BPMNShape></bpmndi:BPMNPlane></bpmndi:BPMNDiagram></bpmn:definitions>',
        NULL, NULL, 1, 1, 0, NULL, '2026-03-19 14:33:12', '2026-03-19 14:33:12', '1', 1),
       ('2', 'leave_multi', '多级请假流程', 'leave', '适用于长期请假（3天以上），需要多级领导审批',
        'i-material-symbols:event-note', 'dynamic',
        '[{"type":"input","field":"title","label":"申请标题","props":{"placeholder":"请输入标题"},"rules":[{"required":true,"message":"请输入标题"}]},{"type":"select","field":"leaveType","label":"请假类型","props":{"options":[{"label":"事假","value":"1"},{"label":"病假","value":"2"},{"label":"年假","value":"3"}]}},{"type":"datePicker","field":"startDate","label":"开始日期"},{"type":"datePicker","field":"endDate","label":"结束日期"},{"type":"inputNumber","field":"days","label":"请假天数"},{"type":"textarea","field":"reason","label":"请假原因"}]',
        '<?xml version="1.0" encoding="UTF-8"?><bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL" xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI" xmlns:dc="http://www.omg.org/spec/DD/20100524/DC" xmlns:flowable="http://flowable.org/bpmn" targetNamespace="http://flowable.org/processdef"><bpmn:process id="leave_multi" name="多级请假流程" isExecutable="true"><bpmn:startEvent id="startEvent" name="开始" flowable:initiator="initiator"/><bpmn:userTask id="deptApprove" name="部门经理审批" flowable:assignee="${deptManager}"/><bpmn:userTask id="hrApprove" name="HR审批" flowable:candidateGroups="${hr}"/><bpmn:endEvent id="endEvent" name="结束"/><bpmn:sequenceFlow id="flow1" sourceRef="startEvent" targetRef="deptApprove"/><bpmn:sequenceFlow id="flow2" sourceRef="deptApprove" targetRef="hrApprove"/><bpmn:sequenceFlow id="flow3" sourceRef="hrApprove" targetRef="endEvent"/></bpmn:process><bpmndi:BPMNDiagram id="BPMNDiagram_1"><bpmndi:BPMNPlane id="BPMNPlane_1" bpmnElement="leave_multi"><bpmndi:BPMNShape id="startEvent_di" bpmnElement="startEvent"><dc:Bounds x="180" y="160" width="36" height="36"/></bpmndi:BPMNShape><bpmndi:BPMNShape id="deptApprove_di" bpmnElement="deptApprove"><dc:Bounds x="280" y="138" width="100" height="80"/></bpmndi:BPMNShape><bpmndi:BPMNShape id="hrApprove_di" bpmnElement="hrApprove"><dc:Bounds x="440" y="138" width="100" height="80"/></bpmndi:BPMNShape><bpmndi:BPMNShape id="endEvent_di" bpmnElement="endEvent"><dc:Bounds x="620" y="160" width="36" height="36"/></bpmndi:BPMNShape></bpmndi:BPMNPlane></bpmndi:BPMNDiagram></bpmn:definitions>',
        NULL, NULL, 1, 1, 0, NULL, '2026-03-19 14:33:12', '2026-03-19 14:33:12', '1', 2),
       ('3', 'expense_common', '通用报销流程', 'expense', '适用于日常费用报销，如差旅费、办公费等',
        'i-material-symbols:payments', 'dynamic',
        '[{"type":"input","field":"title","label":"报销标题","rules":[{"required":true,"message":"请输入标题"}]},{"type":"select","field":"expenseType","label":"报销类型","props":{"options":[{"label":"差旅费","value":"1"},{"label":"办公费","value":"2"},{"label":"招待费","value":"3"},{"label":"其他","value":"4"}]}},{"type":"inputNumber","field":"amount","label":"报销金额","props":{"precision":2}},{"type":"datePicker","field":"expenseDate","label":"报销日期"},{"type":"textarea","field":"description","label":"报销说明"},{"type":"upload","field":"attachments","label":"附件","props":{"multiple":true}}]',
        '<?xml version="1.0" encoding="UTF-8"?><bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL" xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI" xmlns:dc="http://www.omg.org/spec/DD/20100524/DC" xmlns:flowable="http://flowable.org/bpmn" targetNamespace="http://flowable.org/processdef"><bpmn:process id="expense_common" name="通用报销流程" isExecutable="true"><bpmn:startEvent id="startEvent" name="开始" flowable:initiator="initiator"/><bpmn:userTask id="approveTask" name="审批" flowable:assignee="${initiatorLeader}"/><bpmn:userTask id="financeTask" name="财务审核" flowable:candidateGroups="finance"/><bpmn:endEvent id="endEvent" name="结束"/><bpmn:sequenceFlow id="flow1" sourceRef="startEvent" targetRef="approveTask"/><bpmn:sequenceFlow id="flow2" sourceRef="approveTask" targetRef="financeTask"/><bpmn:sequenceFlow id="flow3" sourceRef="financeTask" targetRef="endEvent"/></bpmn:process><bpmndi:BPMNDiagram id="BPMNDiagram_1"><bpmndi:BPMNPlane id="BPMNPlane_1" bpmnElement="expense_common"><bpmndi:BPMNShape id="startEvent_di" bpmnElement="startEvent"><dc:Bounds x="180" y="160" width="36" height="36"/></bpmndi:BPMNShape><bpmndi:BPMNShape id="approveTask_di" bpmnElement="approveTask"><dc:Bounds x="280" y="138" width="100" height="80"/></bpmndi:BPMNShape><bpmndi:BPMNShape id="financeTask_di" bpmnElement="financeTask"><dc:Bounds x="440" y="138" width="100" height="80"/></bpmndi:BPMNShape><bpmndi:BPMNShape id="endEvent_di" bpmnElement="endEvent"><dc:Bounds x="620" y="160" width="36" height="36"/></bpmndi:BPMNShape></bpmndi:BPMNPlane></bpmndi:BPMNDiagram></bpmn:definitions>',
        NULL, NULL, 1, 1, 0, NULL, '2026-03-19 14:33:12', '2026-03-19 14:33:12', '1', 3),
       ('4', 'approval_simple', '通用审批流程', 'approval', '适用于简单的审批场景，单级审批',
        'i-material-symbols:approval', 'dynamic',
        '[{"type":"input","field":"title","label":"申请标题","rules":[{"required":true,"message":"请输入标题"}]},{"type":"textarea","field":"content","label":"申请内容"},{"type":"upload","field":"attachments","label":"附件"}]',
        '<?xml version="1.0" encoding="UTF-8"?><bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL" xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI" xmlns:dc="http://www.omg.org/spec/DD/20100524/DC" xmlns:flowable="http://flowable.org/bpmn" targetNamespace="http://flowable.org/processdef"><bpmn:process id="approval_simple" name="通用审批流程" isExecutable="true"><bpmn:startEvent id="startEvent" name="开始" flowable:initiator="initiator"/><bpmn:userTask id="approveTask" name="审批" flowable:assignee="${approver}"/><bpmn:endEvent id="endEvent" name="结束"/><bpmn:sequenceFlow id="flow1" sourceRef="startEvent" targetRef="approveTask"/><bpmn:sequenceFlow id="flow2" sourceRef="approveTask" targetRef="endEvent"/></bpmn:process><bpmndi:BPMNDiagram id="BPMNDiagram_1"><bpmndi:BPMNPlane id="BPMNPlane_1" bpmnElement="approval_simple"><bpmndi:BPMNShape id="startEvent_di" bpmnElement="startEvent"><dc:Bounds x="180" y="160" width="36" height="36"/></bpmndi:BPMNShape><bpmndi:BPMNShape id="approveTask_di" bpmnElement="approveTask"><dc:Bounds x="280" y="138" width="100" height="80"/></bpmndi:BPMNShape><bpmndi:BPMNShape id="endEvent_di" bpmnElement="endEvent"><dc:Bounds x="460" y="160" width="36" height="36"/></bpmndi:BPMNShape></bpmndi:BPMNPlane></bpmndi:BPMNDiagram></bpmn:definitions>',
        NULL, NULL, 1, 1, 0, NULL, '2026-03-19 14:33:12', '2026-03-19 14:33:12', '1', 4);
INSERT INTO sys_id_sequence (biz_key, max_id, step, version, reset_policy, seq_length, prefix)
VALUES ('order', 1000, 1000, 1, 'DAILY', 6, 'ORD'),
       ('product', 500, 500, 1, 'HOURLY', 8, 'PRD'),
       ('product:2025120217', 1000, 1000, 1, 'NONE', 8, NULL),
       ('user', 0, 1000, 0, 'NONE', 10, 'U');
INSERT INTO sys_job_config (id, job_name, job_group, description, executor_bean, executor_method, executor_handler,
                            executor_service, cron_expression, job_param, status, execute_mode, retry_count,
                            alarm_email, webhook_url, create_time, update_time)
VALUES (3, 'paramTask', 'DEFAULT', '每10分钟执行', 'jobExamples', 'taskWithParam', NULL, NULL, '0 0/10 * * * ?', NULL,
        0, 'BEAN', 0, NULL, NULL, '2025-12-02 15:44:08', '2025-12-02 15:44:08'),
       (4, 'simpleTask', 'DEFAULT', '每5秒执行一次', 'jobExamples', 'simpleTask', NULL, NULL, '0/5 * * * * ?', NULL, 0,
        'BEAN', 0, NULL, NULL, '2025-12-02 15:44:09', '2025-12-02 15:44:09');
INSERT INTO sys_message (id, tenant_id, title, content, `type`, send_scope, send_channel, status, sender_id,
                         sender_name, template_code, template_params, biz_type, biz_key, create_time, update_time,
                         create_by, update_by, create_dept)
VALUES (1, 1, '12', '12', 'SYSTEM', 'USERS', 'WEB', 0, 1, NULL, NULL, NULL, NULL, NULL, '2026-01-31 20:25:57',
        '2026-01-31 20:45:35', NULL, NULL, NULL),
       (2, 1, '测试', '测试', 'CUSTOM', 'USERS', 'SMS', 2, NULL, NULL, NULL, NULL, NULL, NULL, '2026-04-02 12:50:38',
        '2026-04-02 12:50:38', 1, 1, 2),
       (3, 1, '12313', '31232', 'CUSTOM', 'USERS', 'SMS', 2, NULL, NULL, NULL, NULL, NULL, NULL, '2026-04-02 13:02:21',
        '2026-04-02 13:02:22', 1, 1, 2),
       (4, 1, '123213', '12313', 'CUSTOM', 'USERS', 'SMS', 2, NULL, NULL, NULL, NULL, NULL, NULL, '2026-04-02 14:35:31',
        '2026-04-02 14:35:32', 1, 1, 2),
       (5, 1, '11', '11', 'SYSTEM', 'USERS', 'SMS', 2, NULL, NULL, NULL, NULL, NULL, NULL, '2026-04-02 14:40:08',
        '2026-04-02 14:41:53', 1, 1, 2),
       (6, 1, '11', '11', 'SYSTEM', 'USERS', 'SMS', 2, NULL, NULL, NULL, NULL, NULL, NULL, '2026-04-02 15:01:28',
        '2026-04-02 15:01:32', 1, 1, 2),
       (7, 1, '1231', '12313', 'SYSTEM', 'USERS', 'SMS', 2, NULL, NULL, NULL, NULL, NULL, NULL, '2026-04-02 15:02:50',
        '2026-04-02 15:02:51', 1, 1, 2),
       (8, 1, '11', '11', 'SYSTEM', 'USERS', 'SMS', 2, NULL, NULL, NULL, NULL, NULL, NULL, '2026-04-02 15:03:49',
        '2026-04-02 15:04:17', 1, 1, 2),
       (9, 1, '11', '11', 'SYSTEM', 'USERS', 'SMS', 2, NULL, NULL, NULL, NULL, NULL, NULL, '2026-04-02 15:04:45',
        '2026-04-02 15:05:40', 1, 1, 2),
       (10, 1, '11', '11', 'SYSTEM', 'USERS', 'SMS', 2, NULL, NULL, '11', NULL, NULL, NULL, '2026-04-02 15:07:16',
        '2026-04-02 15:07:16', 1, 1, 2);
INSERT INTO sys_message (id, tenant_id, title, content, `type`, send_scope, send_channel, status, sender_id,
                         sender_name, template_code, template_params, biz_type, biz_key, create_time, update_time,
                         create_by, update_by, create_dept)
VALUES (11, 1, '121', '21', 'SYSTEM', 'USERS', 'SMS', 2, NULL, NULL, '11', NULL, NULL, NULL, '2026-04-02 15:10:15',
        '2026-04-02 15:10:15', 1, 1, 2),
       (12, 1, '11', '11', 'SYSTEM', 'USERS', 'EMAIL', 2, NULL, NULL, '11', NULL, NULL, NULL, '2026-04-02 15:44:12',
        '2026-04-02 15:44:13', 1, 1, 2),
       (13, 1, '11', '22', 'SYSTEM', 'USERS', 'WEB', 1, NULL, NULL, '', NULL, 'ANNOUNCEMENT', '1231',
        '2026-04-02 16:54:27', '2026-04-02 16:54:27', 1, 1, 2),
       (14, 1, '11', '222121', 'SYSTEM', 'USERS', 'WEB', 1, NULL, NULL, '', NULL, 'test', '2121', '2026-04-02 16:57:06',
        '2026-04-02 16:57:06', 1, 1, 2),
       (15, 1, '测试', '测试', 'SYSTEM', 'USERS', 'WEB', 1, NULL, NULL, '', NULL, NULL, '', '2026-04-30 16:38:09',
        '2026-04-30 16:38:10', 1, 1, 2);
INSERT INTO sys_message_biz_type (id, tenant_id, biz_type, biz_name, jump_url, jump_target, icon, sort, enabled, remark,
                                  create_time, update_time, create_by, update_by, create_dept)
VALUES (1, 1, 'ORDER', '订单消息', '/order/detail?id=${bizKey}', '_self', 'order', 0, 1, '订单相关消息',
        '2026-04-02 16:11:33', '2026-04-02 16:29:24', NULL, NULL, NULL),
       (3, 1, 'TASK', '任务消息', '/task/detail?id=${bizKey}', '_self', 'task', 0, 1, '任务相关消息',
        '2026-04-02 16:11:33', '2026-04-02 16:29:24', NULL, NULL, NULL),
       (4, 1, 'ANNOUNCEMENT', '公告消息', '/notice/detail?id=${bizKey}', '_self', 'notice', 0, 1, '公告通知消息',
        '2026-04-02 16:11:33', '2026-04-02 16:29:24', NULL, NULL, NULL),
       (5, 1, 'SYSTEM', '系统消息', NULL, '_self', 'system', 0, 1, '系统通知，无需跳转', '2026-04-02 16:11:33',
        '2026-04-02 16:29:24', NULL, NULL, NULL),
       (6, 1, 'test', '测试', '/system/notice', '_self', NULL, 0, 1, NULL, '2026-04-02 16:53:36', '2026-04-02 17:01:32',
        1, 1, 2);
INSERT INTO sys_message_template (id, tenant_id, template_code, template_name, `type`, title_template, content_template,
                                  default_channel, enabled, remark, create_time, update_time, create_by, update_by,
                                  create_dept)
VALUES (1, 1, 'SYSTEM_NOTICE', '系统通知', 'SYSTEM', '系统通知', '尊敬的${userName}，${content}', 'WEB', 1,
        '通用系统通知模板', '2025-12-04 10:20:06', '2026-01-31 20:34:02', NULL, NULL, NULL),
       (2, 1, 'TASK_ASSIGN', '任务分配通知', 'SYSTEM', '您有新的任务',
        '${userName}，您有一个新任务：${taskName}，请及时处理。截止时间：${deadline}', 'WEB', 1, '任务分配通知模板',
        '2025-12-04 10:20:06', '2026-01-31 20:34:02', NULL, NULL, NULL),
       (3, 1, 'SMS_VERIFY_CODE', '短信验证码', 'SMS', NULL,
        '【系统】您的验证码是${code}，${expireMinutes}分钟内有效，请勿泄露。', 'SMS', 1, '短信验证码模板',
        '2025-12-04 10:20:06', '2026-01-31 20:34:02', NULL, NULL, NULL),
       (4, 1, 'APPROVAL_PASS', '审批通过通知', 'SYSTEM', '审批结果通知',
        '${userName}，您提交的${flowName}已审批通过。审批人：${approver}，审批时间：${approveTime}', 'WEB', 1,
        '审批通过通知模板', '2025-12-04 10:20:06', '2026-01-31 20:34:02', NULL, NULL, NULL);
INSERT INTO sys_org (id, tenant_id, org_name, parent_id, ancestors, sort, org_type, org_status, leader_id, leader_name,
                     phone, address, region_code, remark, create_by, create_time, update_by, update_time, create_dept)
VALUES (1, 1, '默认集团', 0, '0', 1, 1, 1, 1, '超级管理员', NULL, NULL, NULL, NULL, NULL, '2025-11-12 17:41:18', NULL,
        '2025-11-12 17:41:18', NULL),
       (2, 1, '研发部', 1, '0,1', 1, 2, 1, 2, '测试用户', NULL, NULL, NULL, NULL, NULL, '2025-11-12 17:41:18', NULL,
        '2025-11-12 17:41:18', NULL),
       (3, 1, '测试组', 2, '0,1,2', 1, 3, 1, 2, '测试用户', NULL, NULL, NULL, NULL, NULL, '2025-11-12 17:41:18', NULL,
        '2025-12-09 17:19:53', NULL),
       (5, 1, '测试组织', 1, '0,1', 0, 2, 1, NULL, NULL, NULL, NULL, NULL, NULL, 1, '2026-04-23 16:45:26', 1,
        '2026-04-24 14:13:50', 2),
       (6, 1, '内蒙分公司', 0, '0', 0, 1, 1, NULL, NULL, NULL, NULL, NULL, NULL, 1, '2026-04-24 14:14:23', 1,
        '2026-04-24 14:14:23', 2),
       (7, 1, '呼市分公司', 6, '0,6', 0, 1, 1, NULL, NULL, NULL, NULL, '150100', NULL, 1, '2026-04-24 15:15:02', 1,
        '2026-04-28 15:39:47', 2);
INSERT INTO sys_post (id, tenant_id, post_code, org_id, post_name, post_status, post_type, sort, remark, create_by,
                      create_time, update_by, update_time, create_dept)
VALUES (1, 1, 'CEO', 1, '首席执行官11', 1, 1, 1, '公司最高管理者', NULL, '2025-11-12 17:41:18', NULL,
        '2025-12-09 17:27:13', NULL),
       (2, 1, 'CTO', 1, '技术总监', 1, 1, 2, '技术部门负责人', NULL, '2025-11-12 17:41:18', NULL, '2025-11-12 17:41:18',
        NULL),
       (3, 1, 'DEV', 2, '开发工程师', 1, 2, 1, '软件开发工程师', NULL, '2025-11-12 17:41:18', NULL,
        '2025-11-12 17:41:18', NULL),
       (4, 1, 'QA', 2, '测试工程师', 1, 2, 2, '软件测试工程师', NULL, '2025-11-12 17:41:18', NULL,
        '2025-11-12 17:41:18', NULL);
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('110000', '北京市', 1, NULL, '北京市', '110000'),
       ('110101', '东城区', 2, '110000', '北京市东城区', '110101'),
       ('110102', '西城区', 2, '110000', '北京市西城区', '110102'),
       ('110105', '朝阳区', 2, '110000', '北京市朝阳区', '110105'),
       ('110106', '丰台区', 2, '110000', '北京市丰台区', '110106'),
       ('110107', '石景山区', 2, '110000', '北京市石景山区', '110107'),
       ('110108', '海淀区', 2, '110000', '北京市海淀区', '110108'),
       ('110109', '门头沟区', 2, '110000', '北京市门头沟区', '110109'),
       ('110111', '房山区', 2, '110000', '北京市房山区', '110111'),
       ('110112', '通州区', 2, '110000', '北京市通州区', '110112');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('110113', '顺义区', 2, '110000', '北京市顺义区', '110113'),
       ('110114', '昌平区', 2, '110000', '北京市昌平区', '110114'),
       ('110115', '大兴区', 2, '110000', '北京市大兴区', '110115'),
       ('110116', '怀柔区', 2, '110000', '北京市怀柔区', '110116'),
       ('110117', '平谷区', 2, '110000', '北京市平谷区', '110117'),
       ('110118', '密云区', 2, '110000', '北京市密云区', '110118'),
       ('110119', '延庆区', 2, '110000', '北京市延庆区', '110119'),
       ('120000', '天津市', 1, NULL, '天津市', '120000'),
       ('120101', '和平区', 2, '120000', '天津市和平区', '120101'),
       ('120102', '河东区', 2, '120000', '天津市河东区', '120102');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('120103', '河西区', 2, '120000', '天津市河西区', '120103'),
       ('120104', '南开区', 2, '120000', '天津市南开区', '120104'),
       ('120105', '河北区', 2, '120000', '天津市河北区', '120105'),
       ('120106', '红桥区', 2, '120000', '天津市红桥区', '120106'),
       ('120110', '东丽区', 2, '120000', '天津市东丽区', '120110'),
       ('120111', '西青区', 2, '120000', '天津市西青区', '120111'),
       ('120112', '津南区', 2, '120000', '天津市津南区', '120112'),
       ('120113', '北辰区', 2, '120000', '天津市北辰区', '120113'),
       ('120114', '武清区', 2, '120000', '天津市武清区', '120114'),
       ('120115', '宝坻区', 2, '120000', '天津市宝坻区', '120115');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('120116', '滨海新区', 2, '120000', '天津市滨海新区', '120116'),
       ('120117', '宁河区', 2, '120000', '天津市宁河区', '120117'),
       ('120118', '静海区', 2, '120000', '天津市静海区', '120118'),
       ('120119', '蓟州区', 2, '120000', '天津市蓟州区', '120119'),
       ('130000', '河北省', 1, NULL, '河北省', '130000'),
       ('130100', '石家庄市', 2, '130000', '河北省石家庄市', '130100'),
       ('130102', '长安区', 3, '130100', '河北省石家庄市长安区', '130100'),
       ('130104', '桥西区', 3, '130100', '河北省石家庄市桥西区', '130100'),
       ('130105', '新华区', 3, '130100', '河北省石家庄市新华区', '130100'),
       ('130107', '井陉矿区', 3, '130100', '河北省石家庄市井陉矿区', '130100');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('130108', '裕华区', 3, '130100', '河北省石家庄市裕华区', '130100'),
       ('130109', '藁城区', 3, '130100', '河北省石家庄市藁城区', '130100'),
       ('130110', '鹿泉区', 3, '130100', '河北省石家庄市鹿泉区', '130100'),
       ('130111', '栾城区', 3, '130100', '河北省石家庄市栾城区', '130100'),
       ('130121', '井陉县', 3, '130100', '河北省石家庄市井陉县', '130100'),
       ('130123', '正定县', 3, '130100', '河北省石家庄市正定县', '130100'),
       ('130125', '行唐县', 3, '130100', '河北省石家庄市行唐县', '130100'),
       ('130126', '灵寿县', 3, '130100', '河北省石家庄市灵寿县', '130100'),
       ('130127', '高邑县', 3, '130100', '河北省石家庄市高邑县', '130100'),
       ('130128', '深泽县', 3, '130100', '河北省石家庄市深泽县', '130100');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('130129', '赞皇县', 3, '130100', '河北省石家庄市赞皇县', '130100'),
       ('130130', '无极县', 3, '130100', '河北省石家庄市无极县', '130100'),
       ('130131', '平山县', 3, '130100', '河北省石家庄市平山县', '130100'),
       ('130132', '元氏县', 3, '130100', '河北省石家庄市元氏县', '130100'),
       ('130133', '赵县', 3, '130100', '河北省石家庄市赵县', '130100'),
       ('130181', '辛集市', 3, '130100', '河北省石家庄市辛集市', '130100'),
       ('130183', '晋州市', 3, '130100', '河北省石家庄市晋州市', '130100'),
       ('130184', '新乐市', 3, '130100', '河北省石家庄市新乐市', '130100'),
       ('130200', '唐山市', 2, '130000', '河北省唐山市', '130200'),
       ('130202', '路南区', 3, '130200', '河北省唐山市路南区', '130200');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('130203', '路北区', 3, '130200', '河北省唐山市路北区', '130200'),
       ('130204', '古冶区', 3, '130200', '河北省唐山市古冶区', '130200'),
       ('130205', '开平区', 3, '130200', '河北省唐山市开平区', '130200'),
       ('130207', '丰南区', 3, '130200', '河北省唐山市丰南区', '130200'),
       ('130208', '丰润区', 3, '130200', '河北省唐山市丰润区', '130200'),
       ('130209', '曹妃甸区', 3, '130200', '河北省唐山市曹妃甸区', '130200'),
       ('130224', '滦南县', 3, '130200', '河北省唐山市滦南县', '130200'),
       ('130225', '乐亭县', 3, '130200', '河北省唐山市乐亭县', '130200'),
       ('130227', '迁西县', 3, '130200', '河北省唐山市迁西县', '130200'),
       ('130229', '玉田县', 3, '130200', '河北省唐山市玉田县', '130200');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('130281', '遵化市', 3, '130200', '河北省唐山市遵化市', '130200'),
       ('130283', '迁安市', 3, '130200', '河北省唐山市迁安市', '130200'),
       ('130284', '滦州市', 3, '130200', '河北省唐山市滦州市', '130200'),
       ('130300', '秦皇岛市', 2, '130000', '河北省秦皇岛市', '130300'),
       ('130302', '海港区', 3, '130300', '河北省秦皇岛市海港区', '130300'),
       ('130303', '山海关区', 3, '130300', '河北省秦皇岛市山海关区', '130300'),
       ('130304', '北戴河区', 3, '130300', '河北省秦皇岛市北戴河区', '130300'),
       ('130306', '抚宁区', 3, '130300', '河北省秦皇岛市抚宁区', '130300'),
       ('130321', '青龙满族自治县', 3, '130300', '河北省秦皇岛市青龙满族自治县', '130300'),
       ('130322', '昌黎县', 3, '130300', '河北省秦皇岛市昌黎县', '130300');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('130324', '卢龙县', 3, '130300', '河北省秦皇岛市卢龙县', '130300'),
       ('130400', '邯郸市', 2, '130000', '河北省邯郸市', '130400'),
       ('130402', '邯山区', 3, '130400', '河北省邯郸市邯山区', '130400'),
       ('130403', '丛台区', 3, '130400', '河北省邯郸市丛台区', '130400'),
       ('130404', '复兴区', 3, '130400', '河北省邯郸市复兴区', '130400'),
       ('130406', '峰峰矿区', 3, '130400', '河北省邯郸市峰峰矿区', '130400'),
       ('130407', '肥乡区', 3, '130400', '河北省邯郸市肥乡区', '130400'),
       ('130408', '永年区', 3, '130400', '河北省邯郸市永年区', '130400'),
       ('130423', '临漳县', 3, '130400', '河北省邯郸市临漳县', '130400'),
       ('130424', '成安县', 3, '130400', '河北省邯郸市成安县', '130400');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('130425', '大名县', 3, '130400', '河北省邯郸市大名县', '130400'),
       ('130426', '涉县', 3, '130400', '河北省邯郸市涉县', '130400'),
       ('130427', '磁县', 3, '130400', '河北省邯郸市磁县', '130400'),
       ('130430', '邱县', 3, '130400', '河北省邯郸市邱县', '130400'),
       ('130431', '鸡泽县', 3, '130400', '河北省邯郸市鸡泽县', '130400'),
       ('130432', '广平县', 3, '130400', '河北省邯郸市广平县', '130400'),
       ('130433', '馆陶县', 3, '130400', '河北省邯郸市馆陶县', '130400'),
       ('130434', '魏县', 3, '130400', '河北省邯郸市魏县', '130400'),
       ('130435', '曲周县', 3, '130400', '河北省邯郸市曲周县', '130400'),
       ('130481', '武安市', 3, '130400', '河北省邯郸市武安市', '130400');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('130500', '邢台市', 2, '130000', '河北省邢台市', '130500'),
       ('130502', '襄都区', 3, '130500', '河北省邢台市襄都区', '130500'),
       ('130503', '信都区', 3, '130500', '河北省邢台市信都区', '130500'),
       ('130505', '任泽区', 3, '130500', '河北省邢台市任泽区', '130500'),
       ('130506', '南和区', 3, '130500', '河北省邢台市南和区', '130500'),
       ('130522', '临城县', 3, '130500', '河北省邢台市临城县', '130500'),
       ('130523', '内丘县', 3, '130500', '河北省邢台市内丘县', '130500'),
       ('130524', '柏乡县', 3, '130500', '河北省邢台市柏乡县', '130500'),
       ('130525', '隆尧县', 3, '130500', '河北省邢台市隆尧县', '130500'),
       ('130528', '宁晋县', 3, '130500', '河北省邢台市宁晋县', '130500');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('130529', '巨鹿县', 3, '130500', '河北省邢台市巨鹿县', '130500'),
       ('130530', '新河县', 3, '130500', '河北省邢台市新河县', '130500'),
       ('130531', '广宗县', 3, '130500', '河北省邢台市广宗县', '130500'),
       ('130532', '平乡县', 3, '130500', '河北省邢台市平乡县', '130500'),
       ('130533', '威县', 3, '130500', '河北省邢台市威县', '130500'),
       ('130534', '清河县', 3, '130500', '河北省邢台市清河县', '130500'),
       ('130535', '临西县', 3, '130500', '河北省邢台市临西县', '130500'),
       ('130581', '南宫市', 3, '130500', '河北省邢台市南宫市', '130500'),
       ('130582', '沙河市', 3, '130500', '河北省邢台市沙河市', '130500'),
       ('130600', '保定市', 2, '130000', '河北省保定市', '130600');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('130602', '竞秀区', 3, '130600', '河北省保定市竞秀区', '130600'),
       ('130606', '莲池区', 3, '130600', '河北省保定市莲池区', '130600'),
       ('130607', '满城区', 3, '130600', '河北省保定市满城区', '130600'),
       ('130608', '清苑区', 3, '130600', '河北省保定市清苑区', '130600'),
       ('130609', '徐水区', 3, '130600', '河北省保定市徐水区', '130600'),
       ('130623', '涞水县', 3, '130600', '河北省保定市涞水县', '130600'),
       ('130624', '阜平县', 3, '130600', '河北省保定市阜平县', '130600'),
       ('130626', '定兴县', 3, '130600', '河北省保定市定兴县', '130600'),
       ('130627', '唐县', 3, '130600', '河北省保定市唐县', '130600'),
       ('130628', '高阳县', 3, '130600', '河北省保定市高阳县', '130600');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('130629', '容城县', 3, '130600', '河北省保定市容城县', '130600'),
       ('130630', '涞源县', 3, '130600', '河北省保定市涞源县', '130600'),
       ('130631', '望都县', 3, '130600', '河北省保定市望都县', '130600'),
       ('130632', '安新县', 3, '130600', '河北省保定市安新县', '130600'),
       ('130633', '易县', 3, '130600', '河北省保定市易县', '130600'),
       ('130634', '曲阳县', 3, '130600', '河北省保定市曲阳县', '130600'),
       ('130635', '蠡县', 3, '130600', '河北省保定市蠡县', '130600'),
       ('130636', '顺平县', 3, '130600', '河北省保定市顺平县', '130600'),
       ('130637', '博野县', 3, '130600', '河北省保定市博野县', '130600'),
       ('130638', '雄县', 3, '130600', '河北省保定市雄县', '130600');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('130681', '涿州市', 3, '130600', '河北省保定市涿州市', '130600'),
       ('130682', '定州市', 3, '130600', '河北省保定市定州市', '130600'),
       ('130683', '安国市', 3, '130600', '河北省保定市安国市', '130600'),
       ('130684', '高碑店市', 3, '130600', '河北省保定市高碑店市', '130600'),
       ('130700', '张家口市', 2, '130000', '河北省张家口市', '130700'),
       ('130702', '桥东区', 3, '130700', '河北省张家口市桥东区', '130700'),
       ('130703', '桥西区', 3, '130700', '河北省张家口市桥西区', '130700'),
       ('130705', '宣化区', 3, '130700', '河北省张家口市宣化区', '130700'),
       ('130706', '下花园区', 3, '130700', '河北省张家口市下花园区', '130700'),
       ('130708', '万全区', 3, '130700', '河北省张家口市万全区', '130700');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('130709', '崇礼区', 3, '130700', '河北省张家口市崇礼区', '130700'),
       ('130722', '张北县', 3, '130700', '河北省张家口市张北县', '130700'),
       ('130723', '康保县', 3, '130700', '河北省张家口市康保县', '130700'),
       ('130724', '沽源县', 3, '130700', '河北省张家口市沽源县', '130700'),
       ('130725', '尚义县', 3, '130700', '河北省张家口市尚义县', '130700'),
       ('130726', '蔚县', 3, '130700', '河北省张家口市蔚县', '130700'),
       ('130727', '阳原县', 3, '130700', '河北省张家口市阳原县', '130700'),
       ('130728', '怀安县', 3, '130700', '河北省张家口市怀安县', '130700'),
       ('130730', '怀来县', 3, '130700', '河北省张家口市怀来县', '130700'),
       ('130731', '涿鹿县', 3, '130700', '河北省张家口市涿鹿县', '130700');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('130732', '赤城县', 3, '130700', '河北省张家口市赤城县', '130700'),
       ('130800', '承德市', 2, '130000', '河北省承德市', '130800'),
       ('130802', '双桥区', 3, '130800', '河北省承德市双桥区', '130800'),
       ('130803', '双滦区', 3, '130800', '河北省承德市双滦区', '130800'),
       ('130804', '鹰手营子矿区', 3, '130800', '河北省承德市鹰手营子矿区', '130800'),
       ('130821', '承德县', 3, '130800', '河北省承德市承德县', '130800'),
       ('130822', '兴隆县', 3, '130800', '河北省承德市兴隆县', '130800'),
       ('130824', '滦平县', 3, '130800', '河北省承德市滦平县', '130800'),
       ('130825', '隆化县', 3, '130800', '河北省承德市隆化县', '130800'),
       ('130826', '丰宁满族自治县', 3, '130800', '河北省承德市丰宁满族自治县', '130800');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('130827', '宽城满族自治县', 3, '130800', '河北省承德市宽城满族自治县', '130800'),
       ('130828', '围场满族蒙古族自治县', 3, '130800', '河北省承德市围场满族蒙古族自治县', '130800'),
       ('130881', '平泉市', 3, '130800', '河北省承德市平泉市', '130800'),
       ('130900', '沧州市', 2, '130000', '河北省沧州市', '130900'),
       ('130902', '新华区', 3, '130900', '河北省沧州市新华区', '130900'),
       ('130903', '运河区', 3, '130900', '河北省沧州市运河区', '130900'),
       ('130921', '沧县', 3, '130900', '河北省沧州市沧县', '130900'),
       ('130922', '青县', 3, '130900', '河北省沧州市青县', '130900'),
       ('130923', '东光县', 3, '130900', '河北省沧州市东光县', '130900'),
       ('130924', '海兴县', 3, '130900', '河北省沧州市海兴县', '130900');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('130925', '盐山县', 3, '130900', '河北省沧州市盐山县', '130900'),
       ('130926', '肃宁县', 3, '130900', '河北省沧州市肃宁县', '130900'),
       ('130927', '南皮县', 3, '130900', '河北省沧州市南皮县', '130900'),
       ('130928', '吴桥县', 3, '130900', '河北省沧州市吴桥县', '130900'),
       ('130929', '献县', 3, '130900', '河北省沧州市献县', '130900'),
       ('130930', '孟村回族自治县', 3, '130900', '河北省沧州市孟村回族自治县', '130900'),
       ('130981', '泊头市', 3, '130900', '河北省沧州市泊头市', '130900'),
       ('130982', '任丘市', 3, '130900', '河北省沧州市任丘市', '130900'),
       ('130983', '黄骅市', 3, '130900', '河北省沧州市黄骅市', '130900'),
       ('130984', '河间市', 3, '130900', '河北省沧州市河间市', '130900');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('131000', '廊坊市', 2, '130000', '河北省廊坊市', '131000'),
       ('131002', '安次区', 3, '131000', '河北省廊坊市安次区', '131000'),
       ('131003', '广阳区', 3, '131000', '河北省廊坊市广阳区', '131000'),
       ('131022', '固安县', 3, '131000', '河北省廊坊市固安县', '131000'),
       ('131023', '永清县', 3, '131000', '河北省廊坊市永清县', '131000'),
       ('131024', '香河县', 3, '131000', '河北省廊坊市香河县', '131000'),
       ('131025', '大城县', 3, '131000', '河北省廊坊市大城县', '131000'),
       ('131026', '文安县', 3, '131000', '河北省廊坊市文安县', '131000'),
       ('131028', '大厂回族自治县', 3, '131000', '河北省廊坊市大厂回族自治县', '131000'),
       ('131081', '霸州市', 3, '131000', '河北省廊坊市霸州市', '131000');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('131082', '三河市', 3, '131000', '河北省廊坊市三河市', '131000'),
       ('131100', '衡水市', 2, '130000', '河北省衡水市', '131100'),
       ('131102', '桃城区', 3, '131100', '河北省衡水市桃城区', '131100'),
       ('131103', '冀州区', 3, '131100', '河北省衡水市冀州区', '131100'),
       ('131121', '枣强县', 3, '131100', '河北省衡水市枣强县', '131100'),
       ('131122', '武邑县', 3, '131100', '河北省衡水市武邑县', '131100'),
       ('131123', '武强县', 3, '131100', '河北省衡水市武强县', '131100'),
       ('131124', '饶阳县', 3, '131100', '河北省衡水市饶阳县', '131100'),
       ('131125', '安平县', 3, '131100', '河北省衡水市安平县', '131100'),
       ('131126', '故城县', 3, '131100', '河北省衡水市故城县', '131100');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('131127', '景县', 3, '131100', '河北省衡水市景县', '131100'),
       ('131128', '阜城县', 3, '131100', '河北省衡水市阜城县', '131100'),
       ('131182', '深州市', 3, '131100', '河北省衡水市深州市', '131100'),
       ('140000', '山西省', 1, NULL, '山西省', '140000'),
       ('140100', '太原市', 2, '140000', '山西省太原市', '140100'),
       ('140105', '小店区', 3, '140100', '山西省太原市小店区', '140100'),
       ('140106', '迎泽区', 3, '140100', '山西省太原市迎泽区', '140100'),
       ('140107', '杏花岭区', 3, '140100', '山西省太原市杏花岭区', '140100'),
       ('140108', '尖草坪区', 3, '140100', '山西省太原市尖草坪区', '140100'),
       ('140109', '万柏林区', 3, '140100', '山西省太原市万柏林区', '140100');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('140110', '晋源区', 3, '140100', '山西省太原市晋源区', '140100'),
       ('140121', '清徐县', 3, '140100', '山西省太原市清徐县', '140100'),
       ('140122', '阳曲县', 3, '140100', '山西省太原市阳曲县', '140100'),
       ('140123', '娄烦县', 3, '140100', '山西省太原市娄烦县', '140100'),
       ('140181', '古交市', 3, '140100', '山西省太原市古交市', '140100'),
       ('140200', '大同市', 2, '140000', '山西省大同市', '140200'),
       ('140212', '新荣区', 3, '140200', '山西省大同市新荣区', '140200'),
       ('140213', '平城区', 3, '140200', '山西省大同市平城区', '140200'),
       ('140214', '云冈区', 3, '140200', '山西省大同市云冈区', '140200'),
       ('140215', '云州区', 3, '140200', '山西省大同市云州区', '140200');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('140221', '阳高县', 3, '140200', '山西省大同市阳高县', '140200'),
       ('140222', '天镇县', 3, '140200', '山西省大同市天镇县', '140200'),
       ('140223', '广灵县', 3, '140200', '山西省大同市广灵县', '140200'),
       ('140224', '灵丘县', 3, '140200', '山西省大同市灵丘县', '140200'),
       ('140225', '浑源县', 3, '140200', '山西省大同市浑源县', '140200'),
       ('140226', '左云县', 3, '140200', '山西省大同市左云县', '140200'),
       ('140300', '阳泉市', 2, '140000', '山西省阳泉市', '140300'),
       ('140302', '城区', 3, '140300', '山西省阳泉市城区', '140300'),
       ('140303', '矿区', 3, '140300', '山西省阳泉市矿区', '140300'),
       ('140311', '郊区', 3, '140300', '山西省阳泉市郊区', '140300');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('140321', '平定县', 3, '140300', '山西省阳泉市平定县', '140300'),
       ('140322', '盂县', 3, '140300', '山西省阳泉市盂县', '140300'),
       ('140400', '长治市', 2, '140000', '山西省长治市', '140400'),
       ('140403', '潞州区', 3, '140400', '山西省长治市潞州区', '140400'),
       ('140404', '上党区', 3, '140400', '山西省长治市上党区', '140400'),
       ('140405', '屯留区', 3, '140400', '山西省长治市屯留区', '140400'),
       ('140406', '潞城区', 3, '140400', '山西省长治市潞城区', '140400'),
       ('140423', '襄垣县', 3, '140400', '山西省长治市襄垣县', '140400'),
       ('140425', '平顺县', 3, '140400', '山西省长治市平顺县', '140400'),
       ('140426', '黎城县', 3, '140400', '山西省长治市黎城县', '140400');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('140427', '壶关县', 3, '140400', '山西省长治市壶关县', '140400'),
       ('140428', '长子县', 3, '140400', '山西省长治市长子县', '140400'),
       ('140429', '武乡县', 3, '140400', '山西省长治市武乡县', '140400'),
       ('140430', '沁县', 3, '140400', '山西省长治市沁县', '140400'),
       ('140431', '沁源县', 3, '140400', '山西省长治市沁源县', '140400'),
       ('140500', '晋城市', 2, '140000', '山西省晋城市', '140500'),
       ('140502', '城区', 3, '140500', '山西省晋城市城区', '140500'),
       ('140521', '沁水县', 3, '140500', '山西省晋城市沁水县', '140500'),
       ('140522', '阳城县', 3, '140500', '山西省晋城市阳城县', '140500'),
       ('140524', '陵川县', 3, '140500', '山西省晋城市陵川县', '140500');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('140525', '泽州县', 3, '140500', '山西省晋城市泽州县', '140500'),
       ('140581', '高平市', 3, '140500', '山西省晋城市高平市', '140500'),
       ('140600', '朔州市', 2, '140000', '山西省朔州市', '140600'),
       ('140602', '朔城区', 3, '140600', '山西省朔州市朔城区', '140600'),
       ('140603', '平鲁区', 3, '140600', '山西省朔州市平鲁区', '140600'),
       ('140621', '山阴县', 3, '140600', '山西省朔州市山阴县', '140600'),
       ('140622', '应县', 3, '140600', '山西省朔州市应县', '140600'),
       ('140623', '右玉县', 3, '140600', '山西省朔州市右玉县', '140600'),
       ('140681', '怀仁市', 3, '140600', '山西省朔州市怀仁市', '140600'),
       ('140700', '晋中市', 2, '140000', '山西省晋中市', '140700');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('140702', '榆次区', 3, '140700', '山西省晋中市榆次区', '140700'),
       ('140703', '太谷区', 3, '140700', '山西省晋中市太谷区', '140700'),
       ('140721', '榆社县', 3, '140700', '山西省晋中市榆社县', '140700'),
       ('140722', '左权县', 3, '140700', '山西省晋中市左权县', '140700'),
       ('140723', '和顺县', 3, '140700', '山西省晋中市和顺县', '140700'),
       ('140724', '昔阳县', 3, '140700', '山西省晋中市昔阳县', '140700'),
       ('140725', '寿阳县', 3, '140700', '山西省晋中市寿阳县', '140700'),
       ('140727', '祁县', 3, '140700', '山西省晋中市祁县', '140700'),
       ('140728', '平遥县', 3, '140700', '山西省晋中市平遥县', '140700'),
       ('140729', '灵石县', 3, '140700', '山西省晋中市灵石县', '140700');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('140781', '介休市', 3, '140700', '山西省晋中市介休市', '140700'),
       ('140800', '运城市', 2, '140000', '山西省运城市', '140800'),
       ('140802', '盐湖区', 3, '140800', '山西省运城市盐湖区', '140800'),
       ('140821', '临猗县', 3, '140800', '山西省运城市临猗县', '140800'),
       ('140822', '万荣县', 3, '140800', '山西省运城市万荣县', '140800'),
       ('140823', '闻喜县', 3, '140800', '山西省运城市闻喜县', '140800'),
       ('140824', '稷山县', 3, '140800', '山西省运城市稷山县', '140800'),
       ('140825', '新绛县', 3, '140800', '山西省运城市新绛县', '140800'),
       ('140826', '绛县', 3, '140800', '山西省运城市绛县', '140800'),
       ('140827', '垣曲县', 3, '140800', '山西省运城市垣曲县', '140800');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('140828', '夏县', 3, '140800', '山西省运城市夏县', '140800'),
       ('140829', '平陆县', 3, '140800', '山西省运城市平陆县', '140800'),
       ('140830', '芮城县', 3, '140800', '山西省运城市芮城县', '140800'),
       ('140881', '永济市', 3, '140800', '山西省运城市永济市', '140800'),
       ('140882', '河津市', 3, '140800', '山西省运城市河津市', '140800'),
       ('140900', '忻州市', 2, '140000', '山西省忻州市', '140900'),
       ('140902', '忻府区', 3, '140900', '山西省忻州市忻府区', '140900'),
       ('140921', '定襄县', 3, '140900', '山西省忻州市定襄县', '140900'),
       ('140922', '五台县', 3, '140900', '山西省忻州市五台县', '140900'),
       ('140923', '代县', 3, '140900', '山西省忻州市代县', '140900');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('140924', '繁峙县', 3, '140900', '山西省忻州市繁峙县', '140900'),
       ('140925', '宁武县', 3, '140900', '山西省忻州市宁武县', '140900'),
       ('140926', '静乐县', 3, '140900', '山西省忻州市静乐县', '140900'),
       ('140927', '神池县', 3, '140900', '山西省忻州市神池县', '140900'),
       ('140928', '五寨县', 3, '140900', '山西省忻州市五寨县', '140900'),
       ('140929', '岢岚县', 3, '140900', '山西省忻州市岢岚县', '140900'),
       ('140930', '河曲县', 3, '140900', '山西省忻州市河曲县', '140900'),
       ('140931', '保德县', 3, '140900', '山西省忻州市保德县', '140900'),
       ('140932', '偏关县', 3, '140900', '山西省忻州市偏关县', '140900'),
       ('140981', '原平市', 3, '140900', '山西省忻州市原平市', '140900');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('141000', '临汾市', 2, '140000', '山西省临汾市', '141000'),
       ('141002', '尧都区', 3, '141000', '山西省临汾市尧都区', '141000'),
       ('141021', '曲沃县', 3, '141000', '山西省临汾市曲沃县', '141000'),
       ('141022', '翼城县', 3, '141000', '山西省临汾市翼城县', '141000'),
       ('141023', '襄汾县', 3, '141000', '山西省临汾市襄汾县', '141000'),
       ('141024', '洪洞县', 3, '141000', '山西省临汾市洪洞县', '141000'),
       ('141025', '古县', 3, '141000', '山西省临汾市古县', '141000'),
       ('141026', '安泽县', 3, '141000', '山西省临汾市安泽县', '141000'),
       ('141027', '浮山县', 3, '141000', '山西省临汾市浮山县', '141000'),
       ('141028', '吉县', 3, '141000', '山西省临汾市吉县', '141000');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('141029', '乡宁县', 3, '141000', '山西省临汾市乡宁县', '141000'),
       ('141030', '大宁县', 3, '141000', '山西省临汾市大宁县', '141000'),
       ('141031', '隰县', 3, '141000', '山西省临汾市隰县', '141000'),
       ('141032', '永和县', 3, '141000', '山西省临汾市永和县', '141000'),
       ('141033', '蒲县', 3, '141000', '山西省临汾市蒲县', '141000'),
       ('141034', '汾西县', 3, '141000', '山西省临汾市汾西县', '141000'),
       ('141081', '侯马市', 3, '141000', '山西省临汾市侯马市', '141000'),
       ('141082', '霍州市', 3, '141000', '山西省临汾市霍州市', '141000'),
       ('141100', '吕梁市', 2, '140000', '山西省吕梁市', '141100'),
       ('141102', '离石区', 3, '141100', '山西省吕梁市离石区', '141100');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('141121', '文水县', 3, '141100', '山西省吕梁市文水县', '141100'),
       ('141122', '交城县', 3, '141100', '山西省吕梁市交城县', '141100'),
       ('141123', '兴县', 3, '141100', '山西省吕梁市兴县', '141100'),
       ('141124', '临县', 3, '141100', '山西省吕梁市临县', '141100'),
       ('141125', '柳林县', 3, '141100', '山西省吕梁市柳林县', '141100'),
       ('141126', '石楼县', 3, '141100', '山西省吕梁市石楼县', '141100'),
       ('141127', '岚县', 3, '141100', '山西省吕梁市岚县', '141100'),
       ('141128', '方山县', 3, '141100', '山西省吕梁市方山县', '141100'),
       ('141129', '中阳县', 3, '141100', '山西省吕梁市中阳县', '141100'),
       ('141130', '交口县', 3, '141100', '山西省吕梁市交口县', '141100');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('141181', '孝义市', 3, '141100', '山西省吕梁市孝义市', '141100'),
       ('141182', '汾阳市', 3, '141100', '山西省吕梁市汾阳市', '141100'),
       ('150000', '内蒙古自治区', 1, NULL, '内蒙古自治区', '150000'),
       ('150100', '呼和浩特市', 2, '150000', '内蒙古自治区呼和浩特市', '150100'),
       ('150102', '新城区', 3, '150100', '内蒙古自治区呼和浩特市新城区', '150100'),
       ('150103', '回民区', 3, '150100', '内蒙古自治区呼和浩特市回民区', '150100'),
       ('150104', '玉泉区', 3, '150100', '内蒙古自治区呼和浩特市玉泉区', '150100'),
       ('150105', '赛罕区', 3, '150100', '内蒙古自治区呼和浩特市赛罕区', '150100'),
       ('150121', '土默特左旗', 3, '150100', '内蒙古自治区呼和浩特市土默特左旗', '150100'),
       ('150122', '托克托县', 3, '150100', '内蒙古自治区呼和浩特市托克托县', '150100');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('150123', '和林格尔县', 3, '150100', '内蒙古自治区呼和浩特市和林格尔县', '150100'),
       ('150124', '清水河县', 3, '150100', '内蒙古自治区呼和浩特市清水河县', '150100'),
       ('150125', '武川县', 3, '150100', '内蒙古自治区呼和浩特市武川县', '150100'),
       ('150200', '包头市', 2, '150000', '内蒙古自治区包头市', '150200'),
       ('150202', '东河区', 3, '150200', '内蒙古自治区包头市东河区', '150200'),
       ('150203', '昆都仑区', 3, '150200', '内蒙古自治区包头市昆都仑区', '150200'),
       ('150204', '青山区', 3, '150200', '内蒙古自治区包头市青山区', '150200'),
       ('150205', '石拐区', 3, '150200', '内蒙古自治区包头市石拐区', '150200'),
       ('150206', '白云鄂博矿区', 3, '150200', '内蒙古自治区包头市白云鄂博矿区', '150200'),
       ('150207', '九原区', 3, '150200', '内蒙古自治区包头市九原区', '150200');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('150221', '土默特右旗', 3, '150200', '内蒙古自治区包头市土默特右旗', '150200'),
       ('150222', '固阳县', 3, '150200', '内蒙古自治区包头市固阳县', '150200'),
       ('150223', '达尔罕茂明安联合旗', 3, '150200', '内蒙古自治区包头市达尔罕茂明安联合旗', '150200'),
       ('150300', '乌海市', 2, '150000', '内蒙古自治区乌海市', '150300'),
       ('150302', '海勃湾区', 3, '150300', '内蒙古自治区乌海市海勃湾区', '150300'),
       ('150303', '海南区', 3, '150300', '内蒙古自治区乌海市海南区', '150300'),
       ('150304', '乌达区', 3, '150300', '内蒙古自治区乌海市乌达区', '150300'),
       ('150400', '赤峰市', 2, '150000', '内蒙古自治区赤峰市', '150400'),
       ('150402', '红山区', 3, '150400', '内蒙古自治区赤峰市红山区', '150400'),
       ('150403', '元宝山区', 3, '150400', '内蒙古自治区赤峰市元宝山区', '150400');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('150404', '松山区', 3, '150400', '内蒙古自治区赤峰市松山区', '150400'),
       ('150421', '阿鲁科尔沁旗', 3, '150400', '内蒙古自治区赤峰市阿鲁科尔沁旗', '150400'),
       ('150422', '巴林左旗', 3, '150400', '内蒙古自治区赤峰市巴林左旗', '150400'),
       ('150423', '巴林右旗', 3, '150400', '内蒙古自治区赤峰市巴林右旗', '150400'),
       ('150424', '林西县', 3, '150400', '内蒙古自治区赤峰市林西县', '150400'),
       ('150425', '克什克腾旗', 3, '150400', '内蒙古自治区赤峰市克什克腾旗', '150400'),
       ('150426', '翁牛特旗', 3, '150400', '内蒙古自治区赤峰市翁牛特旗', '150400'),
       ('150428', '喀喇沁旗', 3, '150400', '内蒙古自治区赤峰市喀喇沁旗', '150400'),
       ('150429', '宁城县', 3, '150400', '内蒙古自治区赤峰市宁城县', '150400'),
       ('150430', '敖汉旗', 3, '150400', '内蒙古自治区赤峰市敖汉旗', '150400');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('150500', '通辽市', 2, '150000', '内蒙古自治区通辽市', '150500'),
       ('150502', '科尔沁区', 3, '150500', '内蒙古自治区通辽市科尔沁区', '150500'),
       ('150521', '科尔沁左翼中旗', 3, '150500', '内蒙古自治区通辽市科尔沁左翼中旗', '150500'),
       ('150522', '科尔沁左翼后旗', 3, '150500', '内蒙古自治区通辽市科尔沁左翼后旗', '150500'),
       ('150523', '开鲁县', 3, '150500', '内蒙古自治区通辽市开鲁县', '150500'),
       ('150524', '库伦旗', 3, '150500', '内蒙古自治区通辽市库伦旗', '150500'),
       ('150525', '奈曼旗', 3, '150500', '内蒙古自治区通辽市奈曼旗', '150500'),
       ('150526', '扎鲁特旗', 3, '150500', '内蒙古自治区通辽市扎鲁特旗', '150500'),
       ('150571', '通辽经济技术开发区', 3, '150500', '内蒙古自治区通辽市经济技术开发区', '150500'),
       ('150581', '霍林郭勒市', 3, '150500', '内蒙古自治区通辽市霍林郭勒市', '150500');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('150600', '鄂尔多斯市', 2, '150000', '内蒙古自治区鄂尔多斯市', '150600'),
       ('150602', '东胜区', 3, '150600', '内蒙古自治区鄂尔多斯市东胜区', '150600'),
       ('150603', '康巴什区', 3, '150600', '内蒙古自治区鄂尔多斯市康巴什区', '150600'),
       ('150621', '达拉特旗', 3, '150600', '内蒙古自治区鄂尔多斯市达拉特旗', '150600'),
       ('150622', '准格尔旗', 3, '150600', '内蒙古自治区鄂尔多斯市准格尔旗', '150600'),
       ('150623', '鄂托克前旗', 3, '150600', '内蒙古自治区鄂尔多斯市鄂托克前旗', '150600'),
       ('150624', '鄂托克旗', 3, '150600', '内蒙古自治区鄂尔多斯市鄂托克旗', '150600'),
       ('150625', '杭锦旗', 3, '150600', '内蒙古自治区鄂尔多斯市杭锦旗', '150600'),
       ('150626', '乌审旗', 3, '150600', '内蒙古自治区鄂尔多斯市乌审旗', '150600'),
       ('150627', '伊金霍洛旗', 3, '150600', '内蒙古自治区鄂尔多斯市伊金霍洛旗', '150600');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('150700', '呼伦贝尔市', 2, '150000', '内蒙古自治区呼伦贝尔市', '150700'),
       ('150702', '海拉尔区', 3, '150700', '内蒙古自治区呼伦贝尔市海拉尔区', '150700'),
       ('150703', '扎赉诺尔区', 3, '150700', '内蒙古自治区呼伦贝尔市扎赉诺尔区', '150700'),
       ('150721', '阿荣旗', 3, '150700', '内蒙古自治区呼伦贝尔市阿荣旗', '150700'),
       ('150722', '莫力达瓦达斡尔族自治旗', 3, '150700', '内蒙古自治区呼伦贝尔市莫力达瓦达斡尔族自治旗', '150700'),
       ('150723', '鄂伦春自治旗', 3, '150700', '内蒙古自治区呼伦贝尔市鄂伦春自治旗', '150700'),
       ('150724', '鄂温克族自治旗', 3, '150700', '内蒙古自治区呼伦贝尔市鄂温克族自治旗', '150700'),
       ('150725', '陈巴尔虎旗', 3, '150700', '内蒙古自治区呼伦贝尔市陈巴尔虎旗', '150700'),
       ('150726', '新巴尔虎左旗', 3, '150700', '内蒙古自治区呼伦贝尔市新巴尔虎左旗', '150700'),
       ('150727', '新巴尔虎右旗', 3, '150700', '内蒙古自治区呼伦贝尔市新巴尔虎右旗', '150700');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('150781', '满洲里市', 3, '150700', '内蒙古自治区呼伦贝尔市满洲里市', '150700'),
       ('150782', '牙克石市', 3, '150700', '内蒙古自治区呼伦贝尔市牙克石市', '150700'),
       ('150783', '扎兰屯市', 3, '150700', '内蒙古自治区呼伦贝尔市扎兰屯市', '150700'),
       ('150784', '额尔古纳市', 3, '150700', '内蒙古自治区呼伦贝尔市额尔古纳市', '150700'),
       ('150785', '根河市', 3, '150700', '内蒙古自治区呼伦贝尔市根河市', '150700'),
       ('150800', '巴彦淖尔市', 2, '150000', '内蒙古自治区巴彦淖尔市', '150800'),
       ('150802', '临河区', 3, '150800', '内蒙古自治区巴彦淖尔市临河区', '150800'),
       ('150821', '五原县', 3, '150800', '内蒙古自治区巴彦淖尔市五原县', '150800'),
       ('150822', '磴口县', 3, '150800', '内蒙古自治区巴彦淖尔市磴口县', '150800'),
       ('150823', '乌拉特前旗', 3, '150800', '内蒙古自治区巴彦淖尔市乌拉特前旗', '150800');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('150824', '乌拉特中旗', 3, '150800', '内蒙古自治区巴彦淖尔市乌拉特中旗', '150800'),
       ('150825', '乌拉特后旗', 3, '150800', '内蒙古自治区巴彦淖尔市乌拉特后旗', '150800'),
       ('150826', '杭锦后旗', 3, '150800', '内蒙古自治区巴彦淖尔市杭锦后旗', '150800'),
       ('150871', '巴彦淖尔经济开发区', 3, '150800', '内蒙古自治区巴彦淖尔市经济开发区', '150800'),
       ('150900', '乌兰察布市', 2, '150000', '内蒙古自治区乌兰察布市', '150900'),
       ('150902', '集宁区', 3, '150900', '内蒙古自治区乌兰察布市集宁区', '150900'),
       ('150921', '卓资县', 3, '150900', '内蒙古自治区乌兰察布市卓资县', '150900'),
       ('150922', '化德县', 3, '150900', '内蒙古自治区乌兰察布市化德县', '150900'),
       ('150923', '商都县', 3, '150900', '内蒙古自治区乌兰察布市商都县', '150900'),
       ('150924', '兴和县', 3, '150900', '内蒙古自治区乌兰察布市兴和县', '150900');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('150925', '凉城县', 3, '150900', '内蒙古自治区乌兰察布市凉城县', '150900'),
       ('150926', '察哈尔右翼前旗', 3, '150900', '内蒙古自治区乌兰察布市察哈尔右翼前旗', '150900'),
       ('150927', '察哈尔右翼中旗', 3, '150900', '内蒙古自治区乌兰察布市察哈尔右翼中旗', '150900'),
       ('150928', '察哈尔右翼后旗', 3, '150900', '内蒙古自治区乌兰察布市察哈尔右翼后旗', '150900'),
       ('150929', '四子王旗', 3, '150900', '内蒙古自治区乌兰察布市四子王旗', '150900'),
       ('150981', '丰镇市', 3, '150900', '内蒙古自治区乌兰察布市丰镇市', '150900'),
       ('152200', '兴安盟', 2, '150000', '内蒙古自治区兴安盟', '152200'),
       ('152201', '乌兰浩特市', 3, '152200', '内蒙古自治区兴安盟乌兰浩特市', '152200'),
       ('152202', '阿尔山', 3, '152200', '内蒙古自治区兴安盟阿尔山市', '152200'),
       ('152221', '科尔沁右翼前旗', 3, '152200', '内蒙古自治区兴安盟科尔沁右翼前旗', '152200');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('152222', '科尔沁右翼中旗', 3, '152200', '内蒙古自治区兴安盟科尔沁右翼中旗', '152200'),
       ('152223', '扎赉特旗', 3, '152200', '内蒙古自治区兴安盟扎赉特旗', '152200'),
       ('152224', '突泉县', 3, '152200', '内蒙古自治区兴安盟突泉县', '152200'),
       ('152500', '锡林郭勒盟', 2, '150000', '内蒙古自治区锡林郭勒盟', '152500'),
       ('152501', '二连浩特市', 3, '152500', '内蒙古自治区锡林郭勒盟二连浩特市', '152500'),
       ('152502', '锡林浩特市', 3, '152500', '内蒙古自治区锡林郭勒盟锡林浩特市', '152500'),
       ('152522', '阿巴嘎旗', 3, '152500', '内蒙古自治区锡林郭勒盟阿巴嘎旗', '152500'),
       ('152523', '苏尼特左旗', 3, '152500', '内蒙古自治区锡林郭勒盟苏尼特左旗', '152500'),
       ('152524', '苏尼特右旗', 3, '152500', '内蒙古自治区锡林郭勒盟苏尼特右旗', '152500'),
       ('152525', '东乌珠穆沁旗', 3, '152500', '内蒙古自治区锡林郭勒盟东乌珠穆沁旗', '152500');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('152526', '西乌珠穆沁旗', 3, '152500', '内蒙古自治区锡林郭勒盟西乌珠穆沁旗', '152500'),
       ('152527', '太仆寺旗', 3, '152500', '内蒙古自治区锡林郭勒盟太仆寺旗', '152500'),
       ('152528', '镶黄旗', 3, '152500', '内蒙古自治区锡林郭勒盟镶黄旗', '152500'),
       ('152529', '正镶白旗', 3, '152500', '内蒙古自治区锡林郭勒盟正镶白旗', '152500'),
       ('152530', '正蓝旗', 3, '152500', '内蒙古自治区锡林郭勒盟正蓝旗', '152500'),
       ('152531', '多伦县', 3, '152500', '内蒙古自治区锡林郭勒盟多伦县', '152500'),
       ('152571', '乌拉盖管理区', 3, '152500', '内蒙古自治区锡林郭勒盟乌拉盖管理区', '152500'),
       ('152900', '阿拉善盟', 2, '150000', '内蒙古自治区阿拉善盟', '152900'),
       ('152921', '阿拉善左旗', 3, '152900', '内蒙古自治区阿拉善盟阿拉善左旗', '152900'),
       ('152922', '阿拉善右旗', 3, '152900', '内蒙古自治区阿拉善盟阿拉善右旗', '152900');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('152923', '额济纳旗', 3, '152900', '内蒙古自治区阿拉善盟额济纳旗', '152900'),
       ('152924', '阿拉善经济开发区', 3, '152900', '内蒙古自治区阿拉善经济开发区', '152900'),
       ('210000', '辽宁省', 1, NULL, '辽宁省', '210000'),
       ('210100', '沈阳市', 2, '210000', '辽宁省沈阳市', '210100'),
       ('210102', '和平区', 3, '210100', '辽宁省沈阳市和平区', '210100'),
       ('210103', '沈河区', 3, '210100', '辽宁省沈阳市沈河区', '210100'),
       ('210104', '大东区', 3, '210100', '辽宁省沈阳市大东区', '210100'),
       ('210105', '皇姑区', 3, '210100', '辽宁省沈阳市皇姑区', '210100'),
       ('210106', '铁西区', 3, '210100', '辽宁省沈阳市铁西区', '210100'),
       ('210111', '苏家屯区', 3, '210100', '辽宁省沈阳市苏家屯区', '210100');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('210112', '浑南区', 3, '210100', '辽宁省沈阳市浑南区', '210100'),
       ('210113', '沈北新区', 3, '210100', '辽宁省沈阳市沈北新区', '210100'),
       ('210114', '于洪区', 3, '210100', '辽宁省沈阳市于洪区', '210100'),
       ('210115', '辽中区', 3, '210100', '辽宁省沈阳市辽中区', '210100'),
       ('210123', '康平县', 3, '210100', '辽宁省沈阳市康平县', '210100'),
       ('210124', '法库县', 3, '210100', '辽宁省沈阳市法库县', '210100'),
       ('210181', '新民市', 3, '210100', '辽宁省沈阳市新民市', '210100'),
       ('210200', '大连市', 2, '210000', '辽宁省大连市', '210200'),
       ('210202', '中山区', 3, '210200', '辽宁省大连市中山区', '210200'),
       ('210203', '西岗区', 3, '210200', '辽宁省大连市西岗区', '210200');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('210204', '沙河口区', 3, '210200', '辽宁省大连市沙河口区', '210200'),
       ('210211', '甘井子区', 3, '210200', '辽宁省大连市甘井子区', '210200'),
       ('210212', '旅顺口区', 3, '210200', '辽宁省大连市旅顺口区', '210200'),
       ('210213', '金州区', 3, '210200', '辽宁省大连市金州区', '210200'),
       ('210214', '普兰店区', 3, '210200', '辽宁省大连市普兰店区', '210200'),
       ('210224', '长海县', 3, '210200', '辽宁省大连市长海县', '210200'),
       ('210281', '瓦房店市', 3, '210200', '辽宁省大连市瓦房店市', '210200'),
       ('210283', '庄河市', 3, '210200', '辽宁省大连市庄河市', '210200'),
       ('210300', '鞍山市', 2, '210000', '辽宁省鞍山市', '210300'),
       ('210302', '铁东区', 3, '210300', '辽宁省鞍山市铁东区', '210300');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('210303', '铁西区', 3, '210300', '辽宁省鞍山市铁西区', '210300'),
       ('210304', '立山区', 3, '210300', '辽宁省鞍山市立山区', '210300'),
       ('210311', '千山区', 3, '210300', '辽宁省鞍山市千山区', '210300'),
       ('210321', '台安县', 3, '210300', '辽宁省鞍山市台安县', '210300'),
       ('210323', '岫岩满族自治县', 3, '210300', '辽宁省鞍山市岫岩满族自治县', '210300'),
       ('210381', '海城市', 3, '210300', '辽宁省鞍山市海城市', '210300'),
       ('210400', '抚顺市', 2, '210000', '辽宁省抚顺市', '210400'),
       ('210402', '新抚区', 3, '210400', '辽宁省抚顺市新抚区', '210400'),
       ('210403', '东洲区', 3, '210400', '辽宁省抚顺市东洲区', '210400'),
       ('210404', '望花区', 3, '210400', '辽宁省抚顺市望花区', '210400');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('210411', '顺城区', 3, '210400', '辽宁省抚顺市顺城区', '210400'),
       ('210421', '抚顺县', 3, '210400', '辽宁省抚顺市抚顺县', '210400'),
       ('210422', '新宾满族自治县', 3, '210400', '辽宁省抚顺市新宾满族自治县', '210400'),
       ('210423', '清原满族自治县', 3, '210400', '辽宁省抚顺市清原满族自治县', '210400'),
       ('210500', '本溪市', 2, '210000', '辽宁省本溪市', '210500'),
       ('210502', '平山区', 3, '210500', '辽宁省本溪市平山区', '210500'),
       ('210503', '溪湖区', 3, '210500', '辽宁省本溪市溪湖区', '210500'),
       ('210504', '明山区', 3, '210500', '辽宁省本溪市明山区', '210500'),
       ('210505', '南芬区', 3, '210500', '辽宁省本溪市南芬区', '210500'),
       ('210521', '本溪满族自治县', 3, '210500', '辽宁省本溪市本溪满族自治县', '210500');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('210522', '桓仁满族自治县', 3, '210500', '辽宁省本溪市桓仁满族自治县', '210500'),
       ('210600', '丹东市', 2, '210000', '辽宁省丹东市', '210600'),
       ('210602', '元宝区', 3, '210600', '辽宁省丹东市元宝区', '210600'),
       ('210603', '振兴区', 3, '210600', '辽宁省丹东市振兴区', '210600'),
       ('210604', '振安区', 3, '210600', '辽宁省丹东市振安区', '210600'),
       ('210624', '宽甸满族自治县', 3, '210600', '辽宁省丹东市宽甸满族自治县', '210600'),
       ('210681', '东港市', 3, '210600', '辽宁省丹东市东港市', '210600'),
       ('210682', '凤城市', 3, '210600', '辽宁省丹东市凤城市', '210600'),
       ('210700', '锦州市', 2, '210000', '辽宁省锦州市', '210700'),
       ('210702', '古塔区', 3, '210700', '辽宁省锦州市古塔区', '210700');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('210703', '凌河区', 3, '210700', '辽宁省锦州市凌河区', '210700'),
       ('210711', '太和区', 3, '210700', '辽宁省锦州市太和区', '210700'),
       ('210726', '黑山县', 3, '210700', '辽宁省锦州市黑山县', '210700'),
       ('210727', '义县', 3, '210700', '辽宁省锦州市义县', '210700'),
       ('210781', '凌海市', 3, '210700', '辽宁省锦州市凌海市', '210700'),
       ('210782', '北镇市', 3, '210700', '辽宁省锦州市北镇市', '210700'),
       ('210800', '营口市', 2, '210000', '辽宁省营口市', '210800'),
       ('210802', '站前区', 3, '210800', '辽宁省营口市站前区', '210800'),
       ('210803', '西市区', 3, '210800', '辽宁省营口市西市区', '210800'),
       ('210804', '鲅鱼圈区', 3, '210800', '辽宁省营口市鲅鱼圈区', '210800');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('210811', '老边区', 3, '210800', '辽宁省营口市老边区', '210800'),
       ('210881', '盖州市', 3, '210800', '辽宁省营口市盖州市', '210800'),
       ('210882', '大石桥市', 3, '210800', '辽宁省营口市大石桥市', '210800'),
       ('210900', '阜新市', 2, '210000', '辽宁省阜新市', '210900'),
       ('210902', '海州区', 3, '210900', '辽宁省阜新市海州区', '210900'),
       ('210903', '新邱区', 3, '210900', '辽宁省阜新市新邱区', '210900'),
       ('210904', '太平区', 3, '210900', '辽宁省阜新市太平区', '210900'),
       ('210905', '清河门区', 3, '210900', '辽宁省阜新市清河门区', '210900'),
       ('210911', '细河区', 3, '210900', '辽宁省阜新市细河区', '210900'),
       ('210921', '阜新蒙古族自治县', 3, '210900', '辽宁省阜新市阜新蒙古族自治县', '210900');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('210922', '彰武县', 3, '210900', '辽宁省阜新市彰武县', '210900'),
       ('211000', '辽阳市', 2, '210000', '辽宁省辽阳市', '211000'),
       ('211002', '白塔区', 3, '211000', '辽宁省辽阳市白塔区', '211000'),
       ('211003', '文圣区', 3, '211000', '辽宁省辽阳市文圣区', '211000'),
       ('211004', '宏伟区', 3, '211000', '辽宁省辽阳市宏伟区', '211000'),
       ('211005', '弓长岭区', 3, '211000', '辽宁省辽阳市弓长岭区', '211000'),
       ('211011', '太子河区', 3, '211000', '辽宁省辽阳市太子河区', '211000'),
       ('211021', '辽阳县', 3, '211000', '辽宁省辽阳市辽阳县', '211000'),
       ('211081', '灯塔市', 3, '211000', '辽宁省辽阳市灯塔市', '211000'),
       ('211100', '盘锦市', 2, '210000', '辽宁省盘锦市', '211100');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('211102', '双台子区', 3, '211100', '辽宁省盘锦市双台子区', '211100'),
       ('211103', '兴隆台区', 3, '211100', '辽宁省盘锦市兴隆台区', '211100'),
       ('211104', '大洼区', 3, '211100', '辽宁省盘锦市大洼区', '211100'),
       ('211122', '盘山县', 3, '211100', '辽宁省盘锦市盘山县', '211100'),
       ('211200', '铁岭市', 2, '210000', '辽宁省铁岭市', '211200'),
       ('211202', '银州区', 3, '211200', '辽宁省铁岭市银州区', '211200'),
       ('211204', '清河区', 3, '211200', '辽宁省铁岭市清河区', '211200'),
       ('211221', '铁岭县', 3, '211200', '辽宁省铁岭市铁岭县', '211200'),
       ('211223', '西丰县', 3, '211200', '辽宁省铁岭市西丰县', '211200'),
       ('211224', '昌图县', 3, '211200', '辽宁省铁岭市昌图县', '211200');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('211281', '调兵山市', 3, '211200', '辽宁省铁岭市调兵山市', '211200'),
       ('211282', '开原市', 3, '211200', '辽宁省铁岭市开原市', '211200'),
       ('211300', '朝阳市', 2, '210000', '辽宁省朝阳市', '211300'),
       ('211302', '双塔区', 3, '211300', '辽宁省朝阳市双塔区', '211300'),
       ('211303', '龙城区', 3, '211300', '辽宁省朝阳市龙城区', '211300'),
       ('211321', '朝阳县', 3, '211300', '辽宁省朝阳市朝阳县', '211300'),
       ('211322', '建平县', 3, '211300', '辽宁省朝阳市建平县', '211300'),
       ('211324', '喀喇沁左翼蒙古族自治县', 3, '211300', '辽宁省朝阳市喀喇沁左翼蒙古族自治县', '211300'),
       ('211381', '北票市', 3, '211300', '辽宁省朝阳市北票市', '211300'),
       ('211382', '凌源市', 3, '211300', '辽宁省朝阳市凌源市', '211300');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('211400', '葫芦岛市', 2, '210000', '辽宁省葫芦岛市', '211400'),
       ('211402', '连山区', 3, '211400', '辽宁省葫芦岛市连山区', '211400'),
       ('211403', '龙港区', 3, '211400', '辽宁省葫芦岛市龙港区', '211400'),
       ('211404', '南票区', 3, '211400', '辽宁省葫芦岛市南票区', '211400'),
       ('211421', '绥中县', 3, '211400', '辽宁省葫芦岛市绥中县', '211400'),
       ('211422', '建昌县', 3, '211400', '辽宁省葫芦岛市建昌县', '211400'),
       ('211481', '兴城市', 3, '211400', '辽宁省葫芦岛市兴城市', '211400'),
       ('220000', '吉林省', 1, NULL, '吉林省', '220000'),
       ('220100', '长春市', 2, '220000', '吉林省长春市', '220100'),
       ('220102', '南关区', 3, '220100', '吉林省长春市南关区', '220100');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('220103', '宽城区', 3, '220100', '吉林省长春市宽城区', '220100'),
       ('220104', '朝阳区', 3, '220100', '吉林省长春市朝阳区', '220100'),
       ('220105', '二道区', 3, '220100', '吉林省长春市二道区', '220100'),
       ('220106', '绿园区', 3, '220100', '吉林省长春市绿园区', '220100'),
       ('220112', '双阳区', 3, '220100', '吉林省长春市双阳区', '220100'),
       ('220113', '九台区', 3, '220100', '吉林省长春市九台区', '220100'),
       ('220122', '农安县', 3, '220100', '吉林省长春市农安县', '220100'),
       ('220182', '榆树市', 3, '220100', '吉林省长春市榆树市', '220100'),
       ('220183', '德惠市', 3, '220100', '吉林省长春市德惠市', '220100'),
       ('220184', '公主岭市', 3, '220100', '吉林省长春市公主岭市', '220100');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('220200', '吉林市', 2, '220000', '吉林省吉林市', '220200'),
       ('220202', '昌邑区', 3, '220200', '吉林省吉林市昌邑区', '220200'),
       ('220203', '龙潭区', 3, '220200', '吉林省吉林市龙潭区', '220200'),
       ('220204', '船营区', 3, '220200', '吉林省吉林市船营区', '220200'),
       ('220211', '丰满区', 3, '220200', '吉林省吉林市丰满区', '220200'),
       ('220221', '永吉县', 3, '220200', '吉林省吉林市永吉县', '220200'),
       ('220281', '蛟河市', 3, '220200', '吉林省吉林市蛟河市', '220200'),
       ('220282', '桦甸市', 3, '220200', '吉林省吉林市桦甸市', '220200'),
       ('220283', '舒兰市', 3, '220200', '吉林省吉林市舒兰市', '220200'),
       ('220284', '磐石市', 3, '220200', '吉林省吉林市磐石市', '220200');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('220300', '四平市', 2, '220000', '吉林省四平市', '220300'),
       ('220302', '铁西区', 3, '220300', '吉林省四平市铁西区', '220300'),
       ('220303', '铁东区', 3, '220300', '吉林省四平市铁东区', '220300'),
       ('220322', '梨树县', 3, '220300', '吉林省四平市梨树县', '220300'),
       ('220323', '伊通满族自治县', 3, '220300', '吉林省四平市伊通满族自治县', '220300'),
       ('220382', '双辽市', 3, '220300', '吉林省四平市双辽市', '220300'),
       ('220400', '辽源市', 2, '220000', '吉林省辽源市', '220400'),
       ('220402', '龙山区', 3, '220400', '吉林省辽源市龙山区', '220400'),
       ('220403', '西安区', 3, '220400', '吉林省辽源市西安区', '220400'),
       ('220421', '东丰县', 3, '220400', '吉林省辽源市东丰县', '220400');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('220422', '东辽县', 3, '220400', '吉林省辽源市东辽县', '220400'),
       ('220500', '通化市', 2, '220000', '吉林省通化市', '220500'),
       ('220502', '东昌区', 3, '220500', '吉林省通化市东昌区', '220500'),
       ('220503', '二道江区', 3, '220500', '吉林省通化市二道江区', '220500'),
       ('220521', '通化县', 3, '220500', '吉林省通化市通化县', '220500'),
       ('220523', '辉南县', 3, '220500', '吉林省通化市辉南县', '220500'),
       ('220524', '柳河县', 3, '220500', '吉林省通化市柳河县', '220500'),
       ('220581', '梅河口市', 3, '220500', '吉林省通化市梅河口市', '220500'),
       ('220582', '集安市', 3, '220500', '吉林省通化市集安市', '220500'),
       ('220600', '白山市', 2, '220000', '吉林省白山市', '220600');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('220602', '浑江区', 3, '220600', '吉林省白山市浑江区', '220600'),
       ('220605', '江源区', 3, '220600', '吉林省白山市江源区', '220600'),
       ('220621', '抚松县', 3, '220600', '吉林省白山市抚松县', '220600'),
       ('220622', '靖宇县', 3, '220600', '吉林省白山市靖宇县', '220600'),
       ('220623', '长白朝鲜族自治县', 3, '220600', '吉林省白山市长白朝鲜族自治县', '220600'),
       ('220681', '临江市', 3, '220600', '吉林省白山市临江市', '220600'),
       ('220700', '松原市', 2, '220000', '吉林省松原市', '220700'),
       ('220702', '宁江区', 3, '220700', '吉林省松原市宁江区', '220700'),
       ('220721', '前郭尔罗斯蒙古族自治县', 3, '220700', '吉林省松原市前郭尔罗斯蒙古族自治县', '220700'),
       ('220722', '长岭县', 3, '220700', '吉林省松原市长岭县', '220700');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('220723', '乾安县', 3, '220700', '吉林省松原市乾安县', '220700'),
       ('220781', '扶余市', 3, '220700', '吉林省松原市扶余市', '220700'),
       ('220800', '白城市', 2, '220000', '吉林省白城市', '220800'),
       ('220802', '洮北区', 3, '220800', '吉林省白城市洮北区', '220800'),
       ('220821', '镇赉县', 3, '220800', '吉林省白城市镇赉县', '220800'),
       ('220822', '通榆县', 3, '220800', '吉林省白城市通榆县', '220800'),
       ('220881', '洮南市', 3, '220800', '吉林省白城市洮南市', '220800'),
       ('220882', '大安市', 3, '220800', '吉林省白城市大安市', '220800'),
       ('222400', '延边朝鲜族自治州', 2, '220000', '吉林省延边朝鲜族自治州', '222400'),
       ('222401', '延吉市', 3, '222400', '吉林省延边朝鲜族自治州延吉市', '222400');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('222402', '图们市', 3, '222400', '吉林省延边朝鲜族自治州图们市', '222400'),
       ('222403', '敦化市', 3, '222400', '吉林省延边朝鲜族自治州敦化市', '222400'),
       ('222404', '珲春市', 3, '222400', '吉林省延边朝鲜族自治州珲春市', '222400'),
       ('222405', '龙井市', 3, '222400', '吉林省延边朝鲜族自治州龙井市', '222400'),
       ('222406', '和龙市', 3, '222400', '吉林省延边朝鲜族自治州和龙市', '222400'),
       ('222424', '汪清县', 3, '222400', '吉林省延边朝鲜族自治州汪清县', '222400'),
       ('222426', '安图县', 3, '222400', '吉林省延边朝鲜族自治州安图县', '222400'),
       ('230000', '黑龙江省', 1, NULL, '黑龙江省', '230000'),
       ('230100', '哈尔滨市', 2, '230000', '黑龙江省哈尔滨市', '230100'),
       ('230102', '道里区', 3, '230100', '黑龙江省哈尔滨市道里区', '230100');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('230103', '南岗区', 3, '230100', '黑龙江省哈尔滨市南岗区', '230100'),
       ('230104', '道外区', 3, '230100', '黑龙江省哈尔滨市道外区', '230100'),
       ('230108', '平房区', 3, '230100', '黑龙江省哈尔滨市平房区', '230100'),
       ('230109', '松北区', 3, '230100', '黑龙江省哈尔滨市松北区', '230100'),
       ('230110', '香坊区', 3, '230100', '黑龙江省哈尔滨市香坊区', '230100'),
       ('230111', '呼兰区', 3, '230100', '黑龙江省哈尔滨市呼兰区', '230100'),
       ('230112', '阿城区', 3, '230100', '黑龙江省哈尔滨市阿城区', '230100'),
       ('230113', '双城区', 3, '230100', '黑龙江省哈尔滨市双城区', '230100'),
       ('230123', '依兰县', 3, '230100', '黑龙江省哈尔滨市依兰县', '230100'),
       ('230124', '方正县', 3, '230100', '黑龙江省哈尔滨市方正县', '230100');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('230125', '宾县', 3, '230100', '黑龙江省哈尔滨市宾县', '230100'),
       ('230126', '巴彦县', 3, '230100', '黑龙江省哈尔滨市巴彦县', '230100'),
       ('230127', '木兰县', 3, '230100', '黑龙江省哈尔滨市木兰县', '230100'),
       ('230128', '通河县', 3, '230100', '黑龙江省哈尔滨市通河县', '230100'),
       ('230129', '延寿县', 3, '230100', '黑龙江省哈尔滨市延寿县', '230100'),
       ('230183', '尚志市', 3, '230100', '黑龙江省哈尔滨市尚志市', '230100'),
       ('230184', '五常市', 3, '230100', '黑龙江省哈尔滨市五常市', '230100'),
       ('230200', '齐齐哈尔市', 2, '230000', '黑龙江省齐齐哈尔市', '230200'),
       ('230202', '龙沙区', 3, '230200', '黑龙江省齐齐哈尔市龙沙区', '230200'),
       ('230203', '建华区', 3, '230200', '黑龙江省齐齐哈尔市建华区', '230200');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('230204', '铁锋区', 3, '230200', '黑龙江省齐齐哈尔市铁锋区', '230200'),
       ('230205', '昂昂溪区', 3, '230200', '黑龙江省齐齐哈尔市昂昂溪区', '230200'),
       ('230206', '富拉尔基区', 3, '230200', '黑龙江省齐齐哈尔市富拉尔基区', '230200'),
       ('230207', '碾子山区', 3, '230200', '黑龙江省齐齐哈尔市碾子山区', '230200'),
       ('230208', '梅里斯达斡尔族区', 3, '230200', '黑龙江省齐齐哈尔市梅里斯达斡尔族区', '230200'),
       ('230221', '龙江县', 3, '230200', '黑龙江省齐齐哈尔市龙江县', '230200'),
       ('230223', '依安县', 3, '230200', '黑龙江省齐齐哈尔市依安县', '230200'),
       ('230224', '泰来县', 3, '230200', '黑龙江省齐齐哈尔市泰来县', '230200'),
       ('230225', '甘南县', 3, '230200', '黑龙江省齐齐哈尔市甘南县', '230200'),
       ('230227', '富裕县', 3, '230200', '黑龙江省齐齐哈尔市富裕县', '230200');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('230229', '克山县', 3, '230200', '黑龙江省齐齐哈尔市克山县', '230200'),
       ('230230', '克东县', 3, '230200', '黑龙江省齐齐哈尔市克东县', '230200'),
       ('230231', '拜泉县', 3, '230200', '黑龙江省齐齐哈尔市拜泉县', '230200'),
       ('230281', '讷河市', 3, '230200', '黑龙江省齐齐哈尔市讷河市', '230200'),
       ('230300', '鸡西市', 2, '230000', '黑龙江省鸡西市', '230300'),
       ('230302', '鸡冠区', 3, '230300', '黑龙江省鸡西市鸡冠区', '230300'),
       ('230303', '恒山区', 3, '230300', '黑龙江省鸡西市恒山区', '230300'),
       ('230304', '滴道区', 3, '230300', '黑龙江省鸡西市滴道区', '230300'),
       ('230305', '梨树区', 3, '230300', '黑龙江省鸡西市梨树区', '230300'),
       ('230306', '城子河区', 3, '230300', '黑龙江省鸡西市城子河区', '230300');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('230307', '麻山区', 3, '230300', '黑龙江省鸡西市麻山区', '230300'),
       ('230321', '鸡东县', 3, '230300', '黑龙江省鸡西市鸡东县', '230300'),
       ('230381', '虎林市', 3, '230300', '黑龙江省鸡西市虎林市', '230300'),
       ('230382', '密山市', 3, '230300', '黑龙江省鸡西市密山市', '230300'),
       ('230400', '鹤岗市', 2, '230000', '黑龙江省鹤岗市', '230400'),
       ('230402', '向阳区', 3, '230400', '黑龙江省鹤岗市向阳区', '230400'),
       ('230403', '工农区', 3, '230400', '黑龙江省鹤岗市工农区', '230400'),
       ('230404', '南山区', 3, '230400', '黑龙江省鹤岗市南山区', '230400'),
       ('230405', '兴安区', 3, '230400', '黑龙江省鹤岗市兴安区', '230400'),
       ('230406', '东山区', 3, '230400', '黑龙江省鹤岗市东山区', '230400');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('230407', '兴山区', 3, '230400', '黑龙江省鹤岗市兴山区', '230400'),
       ('230421', '萝北县', 3, '230400', '黑龙江省鹤岗市萝北县', '230400'),
       ('230422', '绥滨县', 3, '230400', '黑龙江省鹤岗市绥滨县', '230400'),
       ('230500', '双鸭山市', 2, '230000', '黑龙江省双鸭山市', '230500'),
       ('230502', '尖山区', 3, '230500', '黑龙江省双鸭山市尖山区', '230500'),
       ('230503', '岭东区', 3, '230500', '黑龙江省双鸭山市岭东区', '230500'),
       ('230505', '四方台区', 3, '230500', '黑龙江省双鸭山市四方台区', '230500'),
       ('230506', '宝山区', 3, '230500', '黑龙江省双鸭山市宝山区', '230500'),
       ('230521', '集贤县', 3, '230500', '黑龙江省双鸭山市集贤县', '230500'),
       ('230522', '友谊县', 3, '230500', '黑龙江省双鸭山市友谊县', '230500');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('230523', '宝清县', 3, '230500', '黑龙江省双鸭山市宝清县', '230500'),
       ('230524', '饶河县', 3, '230500', '黑龙江省双鸭山市饶河县', '230500'),
       ('230600', '大庆市', 2, '230000', '黑龙江省大庆市', '230600'),
       ('230602', '萨尔图区', 3, '230600', '黑龙江省大庆市萨尔图区', '230600'),
       ('230603', '龙凤区', 3, '230600', '黑龙江省大庆市龙凤区', '230600'),
       ('230604', '让胡路区', 3, '230600', '黑龙江省大庆市让胡路区', '230600'),
       ('230605', '红岗区', 3, '230600', '黑龙江省大庆市红岗区', '230600'),
       ('230606', '大同区', 3, '230600', '黑龙江省大庆市大同区', '230600'),
       ('230621', '肇州县', 3, '230600', '黑龙江省大庆市肇州县', '230600'),
       ('230622', '肇源县', 3, '230600', '黑龙江省大庆市肇源县', '230600');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('230623', '林甸县', 3, '230600', '黑龙江省大庆市林甸县', '230600'),
       ('230624', '杜尔伯特蒙古族自治县', 3, '230600', '黑龙江省大庆市杜尔伯特蒙古族自治县', '230600'),
       ('230700', '伊春市', 2, '230000', '黑龙江省伊春市', '230700'),
       ('230717', '伊美区', 3, '230700', '黑龙江省伊春市伊美区', '230700'),
       ('230718', '乌翠区', 3, '230700', '黑龙江省伊春市乌翠区', '230700'),
       ('230719', '友好区', 3, '230700', '黑龙江省伊春市友好区', '230700'),
       ('230722', '嘉荫县', 3, '230700', '黑龙江省伊春市嘉荫县', '230700'),
       ('230723', '汤旺县', 3, '230700', '黑龙江省伊春市汤旺县', '230700'),
       ('230724', '丰林县', 3, '230700', '黑龙江省伊春市丰林县', '230700'),
       ('230725', '大箐山县', 3, '230700', '黑龙江省伊春市大箐山县', '230700');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('230726', '南岔县', 3, '230700', '黑龙江省伊春市南岔县', '230700'),
       ('230751', '金林区', 3, '230700', '黑龙江省伊春市金林区', '230700'),
       ('230781', '铁力市', 3, '230700', '黑龙江省伊春市铁力市', '230700'),
       ('230800', '佳木斯市', 2, '230000', '黑龙江省佳木斯市', '230800'),
       ('230803', '向阳区', 3, '230800', '黑龙江省佳木斯市向阳区', '230800'),
       ('230804', '前进区', 3, '230800', '黑龙江省佳木斯市前进区', '230800'),
       ('230805', '东风区', 3, '230800', '黑龙江省佳木斯市东风区', '230800'),
       ('230811', '郊区', 3, '230800', '黑龙江省佳木斯市郊区', '230800'),
       ('230822', '桦南县', 3, '230800', '黑龙江省佳木斯市桦南县', '230800'),
       ('230826', '桦川县', 3, '230800', '黑龙江省佳木斯市桦川县', '230800');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('230828', '汤原县', 3, '230800', '黑龙江省佳木斯市汤原县', '230800'),
       ('230881', '同江市', 3, '230800', '黑龙江省佳木斯市同江市', '230800'),
       ('230882', '富锦市', 3, '230800', '黑龙江省佳木斯市富锦市', '230800'),
       ('230883', '抚远市', 3, '230800', '黑龙江省佳木斯市抚远市', '230800'),
       ('230900', '七台河市', 2, '230000', '黑龙江省七台河市', '230900'),
       ('230902', '新兴区', 3, '230900', '黑龙江省七台河市新兴区', '230900'),
       ('230903', '桃山区', 3, '230900', '黑龙江省七台河市桃山区', '230900'),
       ('230904', '茄子河区', 3, '230900', '黑龙江省七台河市茄子河区', '230900'),
       ('230921', '勃利县', 3, '230900', '黑龙江省七台河市勃利县', '230900'),
       ('231000', '牡丹江市', 2, '230000', '黑龙江省牡丹江市', '231000');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('231002', '东安区', 3, '231000', '黑龙江省牡丹江市东安区', '231000'),
       ('231003', '阳明区', 3, '231000', '黑龙江省牡丹江市阳明区', '231000'),
       ('231004', '爱民区', 3, '231000', '黑龙江省牡丹江市爱民区', '231000'),
       ('231005', '西安区', 3, '231000', '黑龙江省牡丹江市西安区', '231000'),
       ('231025', '林口县', 3, '231000', '黑龙江省牡丹江市林口县', '231000'),
       ('231081', '绥芬河市', 3, '231000', '黑龙江省牡丹江市绥芬河市', '231000'),
       ('231083', '海林市', 3, '231000', '黑龙江省牡丹江市海林市', '231000'),
       ('231084', '宁安市', 3, '231000', '黑龙江省牡丹江市宁安市', '231000'),
       ('231085', '穆棱市', 3, '231000', '黑龙江省牡丹江市穆棱市', '231000'),
       ('231086', '东宁市', 3, '231000', '黑龙江省牡丹江市东宁市', '231000');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('231100', '黑河市', 2, '230000', '黑龙江省黑河市', '231100'),
       ('231102', '爱辉区', 3, '231100', '黑龙江省黑河市爱辉区', '231100'),
       ('231123', '逊克县', 3, '231100', '黑龙江省黑河市逊克县', '231100'),
       ('231124', '孙吴县', 3, '231100', '黑龙江省黑河市孙吴县', '231100'),
       ('231181', '北安市', 3, '231100', '黑龙江省黑河市北安市', '231100'),
       ('231182', '五大连池市', 3, '231100', '黑龙江省黑河市五大连池市', '231100'),
       ('231183', '嫩江市', 3, '231100', '黑龙江省黑河市嫩江市', '231100'),
       ('231200', '绥化市', 2, '230000', '黑龙江省绥化市', '231200'),
       ('231202', '北林区', 3, '231200', '黑龙江省绥化市北林区', '231200'),
       ('231221', '望奎县', 3, '231200', '黑龙江省绥化市望奎县', '231200');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('231222', '兰西县', 3, '231200', '黑龙江省绥化市兰西县', '231200'),
       ('231223', '青冈县', 3, '231200', '黑龙江省绥化市青冈县', '231200'),
       ('231224', '庆安县', 3, '231200', '黑龙江省绥化市庆安县', '231200'),
       ('231225', '明水县', 3, '231200', '黑龙江省绥化市明水县', '231200'),
       ('231226', '绥棱县', 3, '231200', '黑龙江省绥化市绥棱县', '231200'),
       ('231281', '安达市', 3, '231200', '黑龙江省绥化市安达市', '231200'),
       ('231282', '肇东市', 3, '231200', '黑龙江省绥化市肇东市', '231200'),
       ('231283', '海伦市', 3, '231200', '黑龙江省绥化市海伦市', '231200'),
       ('232700', '大兴安岭地区', 2, '230000', '黑龙江省大兴安岭地区', '232700'),
       ('232701', '漠河市', 3, '232700', '黑龙江省大兴安岭地区漠河市', '232700');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('232718', '加格达奇区', 3, '232700', '黑龙江省大兴安岭地区加格达奇区', '232700'),
       ('232721', '呼玛县', 3, '232700', '黑龙江省大兴安岭地区呼玛县', '232700'),
       ('232722', '塔河县', 3, '232700', '黑龙江省大兴安岭地区塔河县', '232700'),
       ('310000', '上海市', 1, NULL, '上海市', '310000'),
       ('310101', '黄浦区', 2, '310000', '上海市黄浦区', '310101'),
       ('310104', '徐汇区', 2, '310000', '上海市徐汇区', '310104'),
       ('310105', '长宁区', 2, '310000', '上海市长宁区', '310105'),
       ('310106', '静安区', 2, '310000', '上海市静安区', '310106'),
       ('310107', '普陀区', 2, '310000', '上海市普陀区', '310107'),
       ('310109', '虹口区', 2, '310000', '上海市虹口区', '310109');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('310110', '杨浦区', 2, '310000', '上海市杨浦区', '310110'),
       ('310112', '闵行区', 2, '310000', '上海市闵行区', '310112'),
       ('310113', '宝山区', 2, '310000', '上海市宝山区', '310113'),
       ('310114', '嘉定区', 2, '310000', '上海市嘉定区', '310114'),
       ('310115', '浦东新区', 2, '310000', '上海市浦东新区', '310115'),
       ('310116', '金山区', 2, '310000', '上海市金山区', '310116'),
       ('310117', '松江区', 2, '310000', '上海市松江区', '310117'),
       ('310118', '青浦区', 2, '310000', '上海市青浦区', '310118'),
       ('310120', '奉贤区', 2, '310000', '上海市奉贤区', '310120'),
       ('310151', '崇明区', 2, '310000', '上海市崇明区', '310151');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('320000', '江苏省', 1, NULL, '江苏省', '320000'),
       ('320100', '南京市', 2, '320000', '江苏省南京市', '320100'),
       ('320102', '玄武区', 3, '320100', '江苏省南京市玄武区', '320100'),
       ('320104', '秦淮区', 3, '320100', '江苏省南京市秦淮区', '320100'),
       ('320105', '建邺区', 3, '320100', '江苏省南京市建邺区', '320100'),
       ('320106', '鼓楼区', 3, '320100', '江苏省南京市鼓楼区', '320100'),
       ('320111', '浦口区', 3, '320100', '江苏省南京市浦口区', '320100'),
       ('320113', '栖霞区', 3, '320100', '江苏省南京市栖霞区', '320100'),
       ('320114', '雨花台区', 3, '320100', '江苏省南京市雨花台区', '320100'),
       ('320115', '江宁区', 3, '320100', '江苏省南京市江宁区', '320100');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('320116', '六合区', 3, '320100', '江苏省南京市六合区', '320100'),
       ('320117', '溧水区', 3, '320100', '江苏省南京市溧水区', '320100'),
       ('320118', '高淳区', 3, '320100', '江苏省南京市高淳区', '320100'),
       ('320200', '无锡市', 2, '320000', '江苏省无锡市', '320200'),
       ('320205', '锡山区', 3, '320200', '江苏省无锡市锡山区', '320200'),
       ('320206', '惠山区', 3, '320200', '江苏省无锡市惠山区', '320200'),
       ('320211', '滨湖区', 3, '320200', '江苏省无锡市滨湖区', '320200'),
       ('320213', '梁溪区', 3, '320200', '江苏省无锡市梁溪区', '320200'),
       ('320214', '新吴区', 3, '320200', '江苏省无锡市新吴区', '320200'),
       ('320281', '江阴市', 3, '320200', '江苏省无锡市江阴市', '320200');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('320282', '宜兴市', 3, '320200', '江苏省无锡市宜兴市', '320200'),
       ('320300', '徐州市', 2, '320000', '江苏省徐州市', '320300'),
       ('320302', '鼓楼区', 3, '320300', '江苏省徐州市鼓楼区', '320300'),
       ('320303', '云龙区', 3, '320300', '江苏省徐州市云龙区', '320300'),
       ('320305', '贾汪区', 3, '320300', '江苏省徐州市贾汪区', '320300'),
       ('320311', '泉山区', 3, '320300', '江苏省徐州市泉山区', '320300'),
       ('320312', '铜山区', 3, '320300', '江苏省徐州市铜山区', '320300'),
       ('320321', '丰县', 3, '320300', '江苏省徐州市丰县', '320300'),
       ('320322', '沛县', 3, '320300', '江苏省徐州市沛县', '320300'),
       ('320324', '睢宁县', 3, '320300', '江苏省徐州市睢宁县', '320300');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('320381', '新沂市', 3, '320300', '江苏省徐州市新沂市', '320300'),
       ('320382', '邳州市', 3, '320300', '江苏省徐州市邳州市', '320300'),
       ('320400', '常州市', 2, '320000', '江苏省常州市', '320400'),
       ('320402', '天宁区', 3, '320400', '江苏省常州市天宁区', '320400'),
       ('320404', '钟楼区', 3, '320400', '江苏省常州市钟楼区', '320400'),
       ('320411', '新北区', 3, '320400', '江苏省常州市新北区', '320400'),
       ('320412', '武进区', 3, '320400', '江苏省常州市武进区', '320400'),
       ('320413', '金坛区', 3, '320400', '江苏省常州市金坛区', '320400'),
       ('320481', '溧阳市', 3, '320400', '江苏省常州市溧阳市', '320400'),
       ('320500', '苏州市', 2, '320000', '江苏省苏州市', '320500');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('320505', '虎丘区', 3, '320500', '江苏省苏州市虎丘区', '320500'),
       ('320506', '吴中区', 3, '320500', '江苏省苏州市吴中区', '320500'),
       ('320507', '相城区', 3, '320500', '江苏省苏州市相城区', '320500'),
       ('320508', '姑苏区', 3, '320500', '江苏省苏州市姑苏区', '320500'),
       ('320509', '吴江区', 3, '320500', '江苏省苏州市吴江区', '320500'),
       ('320581', '常熟市', 3, '320500', '江苏省苏州市常熟市', '320500'),
       ('320582', '张家港市', 3, '320500', '江苏省苏州市张家港市', '320500'),
       ('320583', '昆山市', 3, '320500', '江苏省苏州市昆山市', '320500'),
       ('320585', '太仓市', 3, '320500', '江苏省苏州市太仓市', '320500'),
       ('320600', '南通市', 2, '320000', '江苏省南通市', '320600');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('320602', '崇川区', 3, '320600', '江苏省南通市崇川区', '320600'),
       ('320612', '通州区', 3, '320600', '江苏省南通市通州区', '320600'),
       ('320623', '如东县', 3, '320600', '江苏省南通市如东县', '320600'),
       ('320681', '启东市', 3, '320600', '江苏省南通市启东市', '320600'),
       ('320682', '如皋市', 3, '320600', '江苏省南通市如皋市', '320600'),
       ('320684', '海门区', 3, '320600', '江苏省南通市海门区', '320600'),
       ('320685', '海安市', 3, '320600', '江苏省南通市海安市', '320600'),
       ('320700', '连云港市', 2, '320000', '江苏省连云港市', '320700'),
       ('320703', '连云区', 3, '320700', '江苏省连云港市连云区', '320700'),
       ('320706', '海州区', 3, '320700', '江苏省连云港市海州区', '320700');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('320707', '赣榆区', 3, '320700', '江苏省连云港市赣榆区', '320700'),
       ('320722', '东海县', 3, '320700', '江苏省连云港市东海县', '320700'),
       ('320723', '灌云县', 3, '320700', '江苏省连云港市灌云县', '320700'),
       ('320724', '灌南县', 3, '320700', '江苏省连云港市灌南县', '320700'),
       ('320800', '淮安市', 2, '320000', '江苏省淮安市', '320800'),
       ('320803', '淮安区', 3, '320800', '江苏省淮安市淮安区', '320800'),
       ('320804', '淮阴区', 3, '320800', '江苏省淮安市淮阴区', '320800'),
       ('320812', '清江浦区', 3, '320800', '江苏省淮安市清江浦区', '320800'),
       ('320813', '洪泽区', 3, '320800', '江苏省淮安市洪泽区', '320800'),
       ('320826', '涟水县', 3, '320800', '江苏省淮安市涟水县', '320800');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('320830', '盱眙县', 3, '320800', '江苏省淮安市盱眙县', '320800'),
       ('320831', '金湖县', 3, '320800', '江苏省淮安市金湖县', '320800'),
       ('320900', '盐城市', 2, '320000', '江苏省盐城市', '320900'),
       ('320902', '亭湖区', 3, '320900', '江苏省盐城市亭湖区', '320900'),
       ('320903', '盐都区', 3, '320900', '江苏省盐城市盐都区', '320900'),
       ('320904', '大丰区', 3, '320900', '江苏省盐城市大丰区', '320900'),
       ('320921', '响水县', 3, '320900', '江苏省盐城市响水县', '320900'),
       ('320922', '滨海县', 3, '320900', '江苏省盐城市滨海县', '320900'),
       ('320923', '阜宁县', 3, '320900', '江苏省盐城市阜宁县', '320900'),
       ('320924', '射阳县', 3, '320900', '江苏省盐城市射阳县', '320900');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('320925', '建湖县', 3, '320900', '江苏省盐城市建湖县', '320900'),
       ('320981', '东台市', 3, '320900', '江苏省盐城市东台市', '320900'),
       ('321000', '扬州市', 2, '320000', '江苏省扬州市', '321000'),
       ('321002', '广陵区', 3, '321000', '江苏省扬州市广陵区', '321000'),
       ('321003', '邗江区', 3, '321000', '江苏省扬州市邗江区', '321000'),
       ('321012', '江都区', 3, '321000', '江苏省扬州市江都区', '321000'),
       ('321023', '宝应县', 3, '321000', '江苏省扬州市宝应县', '321000'),
       ('321081', '仪征市', 3, '321000', '江苏省扬州市仪征市', '321000'),
       ('321084', '高邮市', 3, '321000', '江苏省扬州市高邮市', '321000'),
       ('321100', '镇江市', 2, '320000', '江苏省镇江市', '321100');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('321102', '京口区', 3, '321100', '江苏省镇江市京口区', '321100'),
       ('321111', '润州区', 3, '321100', '江苏省镇江市润州区', '321100'),
       ('321112', '丹徒区', 3, '321100', '江苏省镇江市丹徒区', '321100'),
       ('321181', '丹阳市', 3, '321100', '江苏省镇江市丹阳市', '321100'),
       ('321182', '扬中市', 3, '321100', '江苏省镇江市扬中市', '321100'),
       ('321183', '句容市', 3, '321100', '江苏省镇江市句容市', '321100'),
       ('321200', '泰州市', 2, '320000', '江苏省泰州市', '321200'),
       ('321202', '海陵区', 3, '321200', '江苏省泰州市海陵区', '321200'),
       ('321203', '高港区', 3, '321200', '江苏省泰州市高港区', '321200'),
       ('321204', '姜堰区', 3, '321200', '江苏省泰州市姜堰区', '321200');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('321281', '兴化市', 3, '321200', '江苏省泰州市兴化市', '321200'),
       ('321282', '靖江市', 3, '321200', '江苏省泰州市靖江市', '321200'),
       ('321283', '泰兴市', 3, '321200', '江苏省泰州市泰兴市', '321200'),
       ('321300', '宿迁市', 2, '320000', '江苏省宿迁市', '321300'),
       ('321302', '宿城区', 3, '321300', '江苏省宿迁市宿城区', '321300'),
       ('321311', '宿豫区', 3, '321300', '江苏省宿迁市宿豫区', '321300'),
       ('321322', '沭阳县', 3, '321300', '江苏省宿迁市沭阳县', '321300'),
       ('321323', '泗阳县', 3, '321300', '江苏省宿迁市泗阳县', '321300'),
       ('321324', '泗洪县', 3, '321300', '江苏省宿迁市泗洪县', '321300'),
       ('330000', '浙江省', 1, NULL, '浙江省', '330000');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('330100', '杭州市', 2, '330000', '浙江省杭州市', '330100'),
       ('330102', '上城区', 3, '330100', '浙江省杭州市上城区', '330100'),
       ('330105', '拱墅区', 3, '330100', '浙江省杭州市拱墅区', '330100'),
       ('330106', '西湖区', 3, '330100', '浙江省杭州市西湖区', '330100'),
       ('330108', '滨江区', 3, '330100', '浙江省杭州市滨江区', '330100'),
       ('330109', '萧山区', 3, '330100', '浙江省杭州市萧山区', '330100'),
       ('330110', '余杭区', 3, '330100', '浙江省杭州市余杭区', '330100'),
       ('330111', '富阳区', 3, '330100', '浙江省杭州市富阳区', '330100'),
       ('330112', '临安区', 3, '330100', '浙江省杭州市临安区', '330100'),
       ('330113', '临平区', 3, '330100', '浙江省杭州市临平区', '330100');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('330114', '钱塘区', 3, '330100', '浙江省杭州市钱塘区', '330100'),
       ('330122', '桐庐县', 3, '330100', '浙江省杭州市桐庐县', '330100'),
       ('330127', '淳安县', 3, '330100', '浙江省杭州市淳安县', '330100'),
       ('330182', '建德市', 3, '330100', '浙江省杭州市建德市', '330100'),
       ('330200', '宁波市', 2, '330000', '浙江省宁波市', '330200'),
       ('330203', '海曙区', 3, '330200', '浙江省宁波市海曙区', '330200'),
       ('330205', '江北区', 3, '330200', '浙江省宁波市江北区', '330200'),
       ('330206', '北仑区', 3, '330200', '浙江省宁波市北仑区', '330200'),
       ('330211', '镇海区', 3, '330200', '浙江省宁波市镇海区', '330200'),
       ('330212', '鄞州区', 3, '330200', '浙江省宁波市鄞州区', '330200');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('330213', '奉化区', 3, '330200', '浙江省宁波市奉化区', '330200'),
       ('330225', '象山县', 3, '330200', '浙江省宁波市象山县', '330200'),
       ('330226', '宁海县', 3, '330200', '浙江省宁波市宁海县', '330200'),
       ('330281', '余姚市', 3, '330200', '浙江省宁波市余姚市', '330200'),
       ('330282', '慈溪市', 3, '330200', '浙江省宁波市慈溪市', '330200'),
       ('330300', '温州市', 2, '330000', '浙江省温州市', '330300'),
       ('330302', '鹿城区', 3, '330300', '浙江省温州市鹿城区', '330300'),
       ('330303', '龙湾区', 3, '330300', '浙江省温州市龙湾区', '330300'),
       ('330304', '瓯海区', 3, '330300', '浙江省温州市瓯海区', '330300'),
       ('330305', '洞头区', 3, '330300', '浙江省温州市洞头区', '330300');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('330324', '永嘉县', 3, '330300', '浙江省温州市永嘉县', '330300'),
       ('330326', '平阳县', 3, '330300', '浙江省温州市平阳县', '330300'),
       ('330327', '苍南县', 3, '330300', '浙江省温州市苍南县', '330300'),
       ('330328', '文成县', 3, '330300', '浙江省温州市文成县', '330300'),
       ('330329', '泰顺县', 3, '330300', '浙江省温州市泰顺县', '330300'),
       ('330381', '瑞安市', 3, '330300', '浙江省温州市瑞安市', '330300'),
       ('330382', '乐清市', 3, '330300', '浙江省温州市乐清市', '330300'),
       ('330383', '龙港市', 3, '330300', '浙江省温州市龙港市', '330300'),
       ('330400', '嘉兴市', 2, '330000', '浙江省嘉兴市', '330400'),
       ('330402', '南湖区', 3, '330400', '浙江省嘉兴市南湖区', '330400');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('330411', '秀洲区', 3, '330400', '浙江省嘉兴市秀洲区', '330400'),
       ('330421', '嘉善县', 3, '330400', '浙江省嘉兴市嘉善县', '330400'),
       ('330424', '海盐县', 3, '330400', '浙江省嘉兴市海盐县', '330400'),
       ('330481', '海宁市', 3, '330400', '浙江省嘉兴市海宁市', '330400'),
       ('330482', '平湖市', 3, '330400', '浙江省嘉兴市平湖市', '330400'),
       ('330483', '桐乡市', 3, '330400', '浙江省嘉兴市桐乡市', '330400'),
       ('330500', '湖州市', 2, '330000', '浙江省湖州市', '330500'),
       ('330502', '吴兴区', 3, '330500', '浙江省湖州市吴兴区', '330500'),
       ('330503', '南浔区', 3, '330500', '浙江省湖州市南浔区', '330500'),
       ('330521', '德清县', 3, '330500', '浙江省湖州市德清县', '330500');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('330522', '长兴县', 3, '330500', '浙江省湖州市长兴县', '330500'),
       ('330523', '安吉县', 3, '330500', '浙江省湖州市安吉县', '330500'),
       ('330600', '绍兴市', 2, '330000', '浙江省绍兴市', '330600'),
       ('330602', '越城区', 3, '330600', '浙江省绍兴市越城区', '330600'),
       ('330603', '柯桥区', 3, '330600', '浙江省绍兴市柯桥区', '330600'),
       ('330604', '上虞区', 3, '330600', '浙江省绍兴市上虞区', '330600'),
       ('330624', '新昌县', 3, '330600', '浙江省绍兴市新昌县', '330600'),
       ('330681', '诸暨市', 3, '330600', '浙江省绍兴市诸暨市', '330600'),
       ('330683', '嵊州市', 3, '330600', '浙江省绍兴市嵊州市', '330600'),
       ('330700', '金华市', 2, '330000', '浙江省金华市', '330700');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('330702', '婺城区', 3, '330700', '浙江省金华市婺城区', '330700'),
       ('330703', '金东区', 3, '330700', '浙江省金华市金东区', '330700'),
       ('330723', '武义县', 3, '330700', '浙江省金华市武义县', '330700'),
       ('330726', '浦江县', 3, '330700', '浙江省金华市浦江县', '330700'),
       ('330727', '磐安县', 3, '330700', '浙江省金华市磐安县', '330700'),
       ('330781', '兰溪市', 3, '330700', '浙江省金华市兰溪市', '330700'),
       ('330782', '义乌市', 3, '330700', '浙江省金华市义乌市', '330700'),
       ('330783', '东阳市', 3, '330700', '浙江省金华市东阳市', '330700'),
       ('330784', '永康市', 3, '330700', '浙江省金华市永康市', '330700'),
       ('330800', '衢州市', 2, '330000', '浙江省衢州市', '330800');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('330802', '柯城区', 3, '330800', '浙江省衢州市柯城区', '330800'),
       ('330803', '衢江区', 3, '330800', '浙江省衢州市衢江区', '330800'),
       ('330822', '常山县', 3, '330800', '浙江省衢州市常山县', '330800'),
       ('330824', '开化县', 3, '330800', '浙江省衢州市开化县', '330800'),
       ('330825', '龙游县', 3, '330800', '浙江省衢州市龙游县', '330800'),
       ('330881', '江山市', 3, '330800', '浙江省衢州市江山市', '330800'),
       ('330900', '舟山市', 2, '330000', '浙江省舟山市', '330900'),
       ('330902', '定海区', 3, '330900', '浙江省舟山市定海区', '330900'),
       ('330903', '普陀区', 3, '330900', '浙江省舟山市普陀区', '330900'),
       ('330921', '岱山县', 3, '330900', '浙江省舟山市岱山县', '330900');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('330922', '嵊泗县', 3, '330900', '浙江省舟山市嵊泗县', '330900'),
       ('331000', '台州市', 2, '330000', '浙江省台州市', '331000'),
       ('331002', '椒江区', 3, '331000', '浙江省台州市椒江区', '331000'),
       ('331003', '黄岩区', 3, '331000', '浙江省台州市黄岩区', '331000'),
       ('331004', '路桥区', 3, '331000', '浙江省台州市路桥区', '331000'),
       ('331022', '三门县', 3, '331000', '浙江省台州市三门县', '331000'),
       ('331023', '天台县', 3, '331000', '浙江省台州市天台县', '331000'),
       ('331024', '仙居县', 3, '331000', '浙江省台州市仙居县', '331000'),
       ('331081', '温岭市', 3, '331000', '浙江省台州市温岭市', '331000'),
       ('331082', '临海市', 3, '331000', '浙江省台州市临海市', '331000');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('331083', '玉环市', 3, '331000', '浙江省台州市玉环市', '331000'),
       ('331100', '丽水市', 2, '330000', '浙江省丽水市', '331100'),
       ('331102', '莲都区', 3, '331100', '浙江省丽水市莲都区', '331100'),
       ('331121', '青田县', 3, '331100', '浙江省丽水市青田县', '331100'),
       ('331122', '缙云县', 3, '331100', '浙江省丽水市缙云县', '331100'),
       ('331123', '遂昌县', 3, '331100', '浙江省丽水市遂昌县', '331100'),
       ('331124', '松阳县', 3, '331100', '浙江省丽水市松阳县', '331100'),
       ('331125', '云和县', 3, '331100', '浙江省丽水市云和县', '331100'),
       ('331126', '庆元县', 3, '331100', '浙江省丽水市庆元县', '331100'),
       ('331127', '景宁畲族自治县', 3, '331100', '浙江省丽水市景宁畲族自治县', '331100');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('331181', '龙泉市', 3, '331100', '浙江省丽水市龙泉市', '331100'),
       ('340000', '安徽省', 1, NULL, '安徽省', '340000'),
       ('340100', '合肥市', 2, '340000', '安徽省合肥市', '340100'),
       ('340102', '瑶海区', 3, '340100', '安徽省合肥市瑶海区', '340100'),
       ('340103', '庐阳区', 3, '340100', '安徽省合肥市庐阳区', '340100'),
       ('340104', '蜀山区', 3, '340100', '安徽省合肥市蜀山区', '340100'),
       ('340111', '包河区', 3, '340100', '安徽省合肥市包河区', '340100'),
       ('340121', '长丰县', 3, '340100', '安徽省合肥市长丰县', '340100'),
       ('340122', '肥东县', 3, '340100', '安徽省合肥市肥东县', '340100'),
       ('340123', '肥西县', 3, '340100', '安徽省合肥市肥西县', '340100');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('340124', '庐江县', 3, '340100', '安徽省合肥市庐江县', '340100'),
       ('340181', '巢湖市', 3, '340100', '安徽省合肥市巢湖市', '340100'),
       ('340200', '芜湖市', 2, '340000', '安徽省芜湖市', '340200'),
       ('340202', '镜湖区', 3, '340200', '安徽省芜湖市镜湖区', '340200'),
       ('340207', '鸠江区', 3, '340200', '安徽省芜湖市鸠江区', '340200'),
       ('340209', '弋江区', 3, '340200', '安徽省芜湖市弋江区', '340200'),
       ('340210', '湾沚区', 3, '340200', '安徽省芜湖市湾沚区', '340200'),
       ('340211', '繁昌区', 3, '340200', '安徽省芜湖市繁昌区', '340200'),
       ('340223', '南陵县', 3, '340200', '安徽省芜湖市南陵县', '340200'),
       ('340281', '无为市', 3, '340200', '安徽省芜湖市无为市', '340200');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('340300', '蚌埠市', 2, '340000', '安徽省蚌埠市', '340300'),
       ('340302', '龙子湖区', 3, '340300', '安徽省蚌埠市龙子湖区', '340300'),
       ('340303', '蚌山区', 3, '340300', '安徽省蚌埠市蚌山区', '340300'),
       ('340304', '禹会区', 3, '340300', '安徽省蚌埠市禹会区', '340300'),
       ('340311', '淮上区', 3, '340300', '安徽省蚌埠市淮上区', '340300'),
       ('340321', '怀远县', 3, '340300', '安徽省蚌埠市怀远县', '340300'),
       ('340322', '五河县', 3, '340300', '安徽省蚌埠市五河县', '340300'),
       ('340323', '固镇县', 3, '340300', '安徽省蚌埠市固镇县', '340300'),
       ('340400', '淮南市', 2, '340000', '安徽省淮南市', '340400'),
       ('340402', '大通区', 3, '340400', '安徽省淮南市大通区', '340400');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('340403', '田家庵区', 3, '340400', '安徽省淮南市田家庵区', '340400'),
       ('340404', '谢家集区', 3, '340400', '安徽省淮南市谢家集区', '340400'),
       ('340405', '八公山区', 3, '340400', '安徽省淮南市八公山区', '340400'),
       ('340406', '潘集区', 3, '340400', '安徽省淮南市潘集区', '340400'),
       ('340421', '凤台县', 3, '340400', '安徽省淮南市凤台县', '340400'),
       ('340422', '寿县', 3, '340400', '安徽省淮南市寿县', '340400'),
       ('340500', '马鞍山市', 2, '340000', '安徽省马鞍山市', '340500'),
       ('340503', '花山区', 3, '340500', '安徽省马鞍山市花山区', '340500'),
       ('340504', '雨山区', 3, '340500', '安徽省马鞍山市雨山区', '340500'),
       ('340506', '博望区', 3, '340500', '安徽省马鞍山市博望区', '340500');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('340521', '当涂县', 3, '340500', '安徽省马鞍山市当涂县', '340500'),
       ('340522', '含山县', 3, '340500', '安徽省马鞍山市含山县', '340500'),
       ('340523', '和县', 3, '340500', '安徽省马鞍山市和县', '340500'),
       ('340600', '淮北市', 2, '340000', '安徽省淮北市', '340600'),
       ('340602', '杜集区', 3, '340600', '安徽省淮北市杜集区', '340600'),
       ('340603', '相山区', 3, '340600', '安徽省淮北市相山区', '340600'),
       ('340604', '烈山区', 3, '340600', '安徽省淮北市烈山区', '340600'),
       ('340621', '濉溪县', 3, '340600', '安徽省淮北市濉溪县', '340600'),
       ('340700', '铜陵市', 2, '340000', '安徽省铜陵市', '340700'),
       ('340705', '铜官区', 3, '340700', '安徽省铜陵市铜官区', '340700');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('340706', '义安区', 3, '340700', '安徽省铜陵市义安区', '340700'),
       ('340711', '郊区', 3, '340700', '安徽省铜陵市郊区', '340700'),
       ('340722', '枞阳县', 3, '340700', '安徽省铜陵市枞阳县', '340700'),
       ('340800', '安庆市', 2, '340000', '安徽省安庆市', '340800'),
       ('340802', '迎江区', 3, '340800', '安徽省安庆市迎江区', '340800'),
       ('340803', '大观区', 3, '340800', '安徽省安庆市大观区', '340800'),
       ('340811', '宜秀区', 3, '340800', '安徽省安庆市宜秀区', '340800'),
       ('340822', '怀宁县', 3, '340800', '安徽省安庆市怀宁县', '340800'),
       ('340825', '太湖县', 3, '340800', '安徽省安庆市太湖县', '340800'),
       ('340826', '宿松县', 3, '340800', '安徽省安庆市宿松县', '340800');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('340827', '望江县', 3, '340800', '安徽省安庆市望江县', '340800'),
       ('340828', '岳西县', 3, '340800', '安徽省安庆市岳西县', '340800'),
       ('340881', '桐城市', 3, '340800', '安徽省安庆市桐城市', '340800'),
       ('340882', '潜山市', 3, '340800', '安徽省安庆市潜山市', '340800'),
       ('341000', '黄山市', 2, '340000', '安徽省黄山市', '341000'),
       ('341002', '屯溪区', 3, '341000', '安徽省黄山市屯溪区', '341000'),
       ('341003', '黄山区', 3, '341000', '安徽省黄山市黄山区', '341000'),
       ('341004', '徽州区', 3, '341000', '安徽省黄山市徽州区', '341000'),
       ('341021', '歙县', 3, '341000', '安徽省黄山市歙县', '341000'),
       ('341022', '休宁县', 3, '341000', '安徽省黄山市休宁县', '341000');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('341023', '黟县', 3, '341000', '安徽省黄山市黟县', '341000'),
       ('341024', '祁门县', 3, '341000', '安徽省黄山市祁门县', '341000'),
       ('341100', '滁州市', 2, '340000', '安徽省滁州市', '341100'),
       ('341102', '琅琊区', 3, '341100', '安徽省滁州市琅琊区', '341100'),
       ('341103', '南谯区', 3, '341100', '安徽省滁州市南谯区', '341100'),
       ('341122', '来安县', 3, '341100', '安徽省滁州市来安县', '341100'),
       ('341124', '全椒县', 3, '341100', '安徽省滁州市全椒县', '341100'),
       ('341125', '定远县', 3, '341100', '安徽省滁州市定远县', '341100'),
       ('341126', '凤阳县', 3, '341100', '安徽省滁州市凤阳县', '341100'),
       ('341181', '天长市', 3, '341100', '安徽省滁州市天长市', '341100');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('341182', '明光市', 3, '341100', '安徽省滁州市明光市', '341100'),
       ('341200', '阜阳市', 2, '340000', '安徽省阜阳市', '341200'),
       ('341202', '颍州区', 3, '341200', '安徽省阜阳市颍州区', '341200'),
       ('341203', '颍东区', 3, '341200', '安徽省阜阳市颍东区', '341200'),
       ('341204', '颍泉区', 3, '341200', '安徽省阜阳市颍泉区', '341200'),
       ('341221', '临泉县', 3, '341200', '安徽省阜阳市临泉县', '341200'),
       ('341222', '太和县', 3, '341200', '安徽省阜阳市太和县', '341200'),
       ('341225', '阜南县', 3, '341200', '安徽省阜阳市阜南县', '341200'),
       ('341226', '颍上县', 3, '341200', '安徽省阜阳市颍上县', '341200'),
       ('341282', '界首市', 3, '341200', '安徽省阜阳市界首市', '341200');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('341300', '宿州市', 2, '340000', '安徽省宿州市', '341300'),
       ('341302', '埇桥区', 3, '341300', '安徽省宿州市埇桥区', '341300'),
       ('341321', '砀山县', 3, '341300', '安徽省宿州市砀山县', '341300'),
       ('341322', '萧县', 3, '341300', '安徽省宿州市萧县', '341300'),
       ('341323', '灵璧县', 3, '341300', '安徽省宿州市灵璧县', '341300'),
       ('341324', '泗县', 3, '341300', '安徽省宿州市泗县', '341300'),
       ('341500', '六安市', 2, '340000', '安徽省六安市', '341500'),
       ('341502', '金安区', 3, '341500', '安徽省六安市金安区', '341500'),
       ('341503', '裕安区', 3, '341500', '安徽省六安市裕安区', '341500'),
       ('341504', '叶集区', 3, '341500', '安徽省六安市叶集区', '341500');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('341522', '霍邱县', 3, '341500', '安徽省六安市霍邱县', '341500'),
       ('341523', '舒城县', 3, '341500', '安徽省六安市舒城县', '341500'),
       ('341524', '金寨县', 3, '341500', '安徽省六安市金寨县', '341500'),
       ('341525', '霍山县', 3, '341500', '安徽省六安市霍山县', '341500'),
       ('341600', '亳州市', 2, '340000', '安徽省亳州市', '341600'),
       ('341602', '谯城区', 3, '341600', '安徽省亳州市谯城区', '341600'),
       ('341621', '涡阳县', 3, '341600', '安徽省亳州市涡阳县', '341600'),
       ('341622', '蒙城县', 3, '341600', '安徽省亳州市蒙城县', '341600'),
       ('341623', '利辛县', 3, '341600', '安徽省亳州市利辛县', '341600'),
       ('341700', '池州市', 2, '340000', '安徽省池州市', '341700');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('341702', '贵池区', 3, '341700', '安徽省池州市贵池区', '341700'),
       ('341721', '东至县', 3, '341700', '安徽省池州市东至县', '341700'),
       ('341722', '石台县', 3, '341700', '安徽省池州市石台县', '341700'),
       ('341723', '青阳县', 3, '341700', '安徽省池州市青阳县', '341700'),
       ('341800', '宣城市', 2, '340000', '安徽省宣城市', '341800'),
       ('341802', '宣州区', 3, '341800', '安徽省宣城市宣州区', '341800'),
       ('341821', '郎溪县', 3, '341800', '安徽省宣城市郎溪县', '341800'),
       ('341823', '泾县', 3, '341800', '安徽省宣城市泾县', '341800'),
       ('341824', '绩溪县', 3, '341800', '安徽省宣城市绩溪县', '341800'),
       ('341825', '旌德县', 3, '341800', '安徽省宣城市旌德县', '341800');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('341881', '宁国市', 3, '341800', '安徽省宣城市宁国市', '341800'),
       ('341882', '广德市', 3, '341800', '安徽省宣城市广德市', '341800'),
       ('350000', '福建省', 1, NULL, '福建省', '350000'),
       ('350100', '福州市', 2, '350000', '福建省福州市', '350100'),
       ('350102', '鼓楼区', 3, '350100', '福建省福州市鼓楼区', '350100'),
       ('350103', '台江区', 3, '350100', '福建省福州市台江区', '350100'),
       ('350104', '仓山区', 3, '350100', '福建省福州市仓山区', '350100'),
       ('350105', '马尾区', 3, '350100', '福建省福州市马尾区', '350100'),
       ('350111', '晋安区', 3, '350100', '福建省福州市晋安区', '350100'),
       ('350112', '长乐区', 3, '350100', '福建省福州市长乐区', '350100');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('350121', '闽侯县', 3, '350100', '福建省福州市闽侯县', '350100'),
       ('350122', '连江县', 3, '350100', '福建省福州市连江县', '350100'),
       ('350123', '罗源县', 3, '350100', '福建省福州市罗源县', '350100'),
       ('350124', '闽清县', 3, '350100', '福建省福州市闽清县', '350100'),
       ('350125', '永泰县', 3, '350100', '福建省福州市永泰县', '350100'),
       ('350128', '平潭县', 3, '350100', '福建省福州市平潭县', '350100'),
       ('350181', '福清市', 3, '350100', '福建省福州市福清市', '350100'),
       ('350200', '厦门市', 2, '350000', '福建省厦门市', '350200'),
       ('350203', '思明区', 3, '350200', '福建省厦门市思明区', '350200'),
       ('350205', '海沧区', 3, '350200', '福建省厦门市海沧区', '350200');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('350206', '湖里区', 3, '350200', '福建省厦门市湖里区', '350200'),
       ('350211', '集美区', 3, '350200', '福建省厦门市集美区', '350200'),
       ('350212', '同安区', 3, '350200', '福建省厦门市同安区', '350200'),
       ('350213', '翔安区', 3, '350200', '福建省厦门市翔安区', '350200'),
       ('350300', '莆田市', 2, '350000', '福建省莆田市', '350300'),
       ('350302', '城厢区', 3, '350300', '福建省莆田市城厢区', '350300'),
       ('350303', '涵江区', 3, '350300', '福建省莆田市涵江区', '350300'),
       ('350304', '荔城区', 3, '350300', '福建省莆田市荔城区', '350300'),
       ('350305', '秀屿区', 3, '350300', '福建省莆田市秀屿区', '350300'),
       ('350322', '仙游县', 3, '350300', '福建省莆田市仙游县', '350300');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('350400', '三明市', 2, '350000', '福建省三明市', '350400'),
       ('350403', '三元区', 3, '350400', '福建省三明市三元区', '350400'),
       ('350421', '明溪县', 3, '350400', '福建省三明市明溪县', '350400'),
       ('350423', '清流县', 3, '350400', '福建省三明市清流县', '350400'),
       ('350424', '宁化县', 3, '350400', '福建省三明市宁化县', '350400'),
       ('350425', '大田县', 3, '350400', '福建省三明市大田县', '350400'),
       ('350426', '尤溪县', 3, '350400', '福建省三明市尤溪县', '350400'),
       ('350427', '沙县区', 3, '350400', '福建省三明市沙县区', '350400'),
       ('350428', '将乐县', 3, '350400', '福建省三明市将乐县', '350400'),
       ('350429', '泰宁县', 3, '350400', '福建省三明市泰宁县', '350400');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('350430', '建宁县', 3, '350400', '福建省三明市建宁县', '350400'),
       ('350481', '永安市', 3, '350400', '福建省三明市永安市', '350400'),
       ('350500', '泉州市', 2, '350000', '福建省泉州市', '350500'),
       ('350502', '鲤城区', 3, '350500', '福建省泉州市鲤城区', '350500'),
       ('350503', '丰泽区', 3, '350500', '福建省泉州市丰泽区', '350500'),
       ('350504', '洛江区', 3, '350500', '福建省泉州市洛江区', '350500'),
       ('350505', '泉港区', 3, '350500', '福建省泉州市泉港区', '350500'),
       ('350521', '惠安县', 3, '350500', '福建省泉州市惠安县', '350500'),
       ('350524', '安溪县', 3, '350500', '福建省泉州市安溪县', '350500'),
       ('350525', '永春县', 3, '350500', '福建省泉州市永春县', '350500');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('350526', '德化县', 3, '350500', '福建省泉州市德化县', '350500'),
       ('350527', '金门县', 3, '350500', '福建省泉州市金门县', '350500'),
       ('350581', '石狮市', 3, '350500', '福建省泉州市石狮市', '350500'),
       ('350582', '晋江市', 3, '350500', '福建省泉州市晋江市', '350500'),
       ('350583', '南安市', 3, '350500', '福建省泉州市南安市', '350500'),
       ('350600', '漳州市', 2, '350000', '福建省漳州市', '350600'),
       ('350602', '芗城区', 3, '350600', '福建省漳州市芗城区', '350600'),
       ('350603', '龙文区', 3, '350600', '福建省漳州市龙文区', '350600'),
       ('350622', '云霄县', 3, '350600', '福建省漳州市云霄县', '350600'),
       ('350623', '漳浦县', 3, '350600', '福建省漳州市漳浦县', '350600');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('350624', '诏安县', 3, '350600', '福建省漳州市诏安县', '350600'),
       ('350625', '长泰区', 3, '350600', '福建省漳州市长泰区', '350600'),
       ('350626', '东山县', 3, '350600', '福建省漳州市东山县', '350600'),
       ('350627', '南靖县', 3, '350600', '福建省漳州市南靖县', '350600'),
       ('350628', '平和县', 3, '350600', '福建省漳州市平和县', '350600'),
       ('350629', '华安县', 3, '350600', '福建省漳州市华安县', '350600'),
       ('350681', '龙海区', 3, '350600', '福建省漳州市龙海区', '350600'),
       ('350700', '南平市', 2, '350000', '福建省南平市', '350700'),
       ('350702', '延平区', 3, '350700', '福建省南平市延平区', '350700'),
       ('350703', '建阳区', 3, '350700', '福建省南平市建阳区', '350700');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('350721', '顺昌县', 3, '350700', '福建省南平市顺昌县', '350700'),
       ('350722', '浦城县', 3, '350700', '福建省南平市浦城县', '350700'),
       ('350723', '光泽县', 3, '350700', '福建省南平市光泽县', '350700'),
       ('350724', '松溪县', 3, '350700', '福建省南平市松溪县', '350700'),
       ('350725', '政和县', 3, '350700', '福建省南平市政和县', '350700'),
       ('350781', '邵武市', 3, '350700', '福建省南平市邵武市', '350700'),
       ('350782', '武夷山市', 3, '350700', '福建省南平市武夷山市', '350700'),
       ('350783', '建瓯市', 3, '350700', '福建省南平市建瓯市', '350700'),
       ('350800', '龙岩市', 2, '350000', '福建省龙岩市', '350800'),
       ('350802', '新罗区', 3, '350800', '福建省龙岩市新罗区', '350800');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('350803', '永定区', 3, '350800', '福建省龙岩市永定区', '350800'),
       ('350821', '长汀县', 3, '350800', '福建省龙岩市长汀县', '350800'),
       ('350823', '上杭县', 3, '350800', '福建省龙岩市上杭县', '350800'),
       ('350824', '武平县', 3, '350800', '福建省龙岩市武平县', '350800'),
       ('350825', '连城县', 3, '350800', '福建省龙岩市连城县', '350800'),
       ('350881', '漳平市', 3, '350800', '福建省龙岩市漳平市', '350800'),
       ('350900', '宁德市', 2, '350000', '福建省宁德市', '350900'),
       ('350902', '蕉城区', 3, '350900', '福建省宁德市蕉城区', '350900'),
       ('350921', '霞浦县', 3, '350900', '福建省宁德市霞浦县', '350900'),
       ('350922', '古田县', 3, '350900', '福建省宁德市古田县', '350900');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('350923', '屏南县', 3, '350900', '福建省宁德市屏南县', '350900'),
       ('350924', '寿宁县', 3, '350900', '福建省宁德市寿宁县', '350900'),
       ('350925', '周宁县', 3, '350900', '福建省宁德市周宁县', '350900'),
       ('350926', '柘荣县', 3, '350900', '福建省宁德市柘荣县', '350900'),
       ('350981', '福安市', 3, '350900', '福建省宁德市福安市', '350900'),
       ('350982', '福鼎市', 3, '350900', '福建省宁德市福鼎市', '350900'),
       ('360000', '江西省', 1, NULL, '江西省', '360000'),
       ('360100', '南昌市', 2, '360000', '江西省南昌市', '360100'),
       ('360102', '东湖区', 3, '360100', '江西省南昌市东湖区', '360100'),
       ('360103', '西湖区', 3, '360100', '江西省南昌市西湖区', '360100');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('360104', '青云谱区', 3, '360100', '江西省南昌市青云谱区', '360100'),
       ('360111', '青山湖区', 3, '360100', '江西省南昌市青山湖区', '360100'),
       ('360112', '新建区', 3, '360100', '江西省南昌市新建区', '360100'),
       ('360113', '红谷滩区', 3, '360100', '江西省南昌市红谷滩区', '360100'),
       ('360121', '南昌县', 3, '360100', '江西省南昌市南昌县', '360100'),
       ('360123', '安义县', 3, '360100', '江西省南昌市安义县', '360100'),
       ('360124', '进贤县', 3, '360100', '江西省南昌市进贤县', '360100'),
       ('360200', '景德镇市', 2, '360000', '江西省景德镇市', '360200'),
       ('360202', '昌江区', 3, '360200', '江西省景德镇市昌江区', '360200'),
       ('360203', '珠山区', 3, '360200', '江西省景德镇市珠山区', '360200');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('360222', '浮梁县', 3, '360200', '江西省景德镇市浮梁县', '360200'),
       ('360281', '乐平市', 3, '360200', '江西省景德镇市乐平市', '360200'),
       ('360300', '萍乡市', 2, '360000', '江西省萍乡市', '360300'),
       ('360302', '安源区', 3, '360300', '江西省萍乡市安源区', '360300'),
       ('360313', '湘东区', 3, '360300', '江西省萍乡市湘东区', '360300'),
       ('360321', '莲花县', 3, '360300', '江西省萍乡市莲花县', '360300'),
       ('360322', '上栗县', 3, '360300', '江西省萍乡市上栗县', '360300'),
       ('360323', '芦溪县', 3, '360300', '江西省萍乡市芦溪县', '360300'),
       ('360400', '九江市', 2, '360000', '江西省九江市', '360400'),
       ('360402', '濂溪区', 3, '360400', '江西省九江市濂溪区', '360400');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('360403', '浔阳区', 3, '360400', '江西省九江市浔阳区', '360400'),
       ('360404', '柴桑区', 3, '360400', '江西省九江市柴桑区', '360400'),
       ('360423', '武宁县', 3, '360400', '江西省九江市武宁县', '360400'),
       ('360424', '修水县', 3, '360400', '江西省九江市修水县', '360400'),
       ('360425', '永修县', 3, '360400', '江西省九江市永修县', '360400'),
       ('360426', '德安县', 3, '360400', '江西省九江市德安县', '360400'),
       ('360428', '都昌县', 3, '360400', '江西省九江市都昌县', '360400'),
       ('360429', '湖口县', 3, '360400', '江西省九江市湖口县', '360400'),
       ('360430', '彭泽县', 3, '360400', '江西省九江市彭泽县', '360400'),
       ('360481', '瑞昌市', 3, '360400', '江西省九江市瑞昌市', '360400');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('360482', '共青城市', 3, '360400', '江西省九江市共青城市', '360400'),
       ('360483', '庐山市', 3, '360400', '江西省九江市庐山市', '360400'),
       ('360500', '新余市', 2, '360000', '江西省新余市', '360500'),
       ('360502', '渝水区', 3, '360500', '江西省新余市渝水区', '360500'),
       ('360521', '分宜县', 3, '360500', '江西省新余市分宜县', '360500'),
       ('360600', '鹰潭市', 2, '360000', '江西省鹰潭市', '360600'),
       ('360602', '月湖区', 3, '360600', '江西省鹰潭市月湖区', '360600'),
       ('360603', '余江区', 3, '360600', '江西省鹰潭市余江区', '360600'),
       ('360681', '贵溪市', 3, '360600', '江西省鹰潭市贵溪市', '360600'),
       ('360700', '赣州市', 2, '360000', '江西省赣州市', '360700');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('360702', '章贡区', 3, '360700', '江西省赣州市章贡区', '360700'),
       ('360703', '南康区', 3, '360700', '江西省赣州市南康区', '360700'),
       ('360704', '赣县区', 3, '360700', '江西省赣州市赣县区', '360700'),
       ('360722', '信丰县', 3, '360700', '江西省赣州市信丰县', '360700'),
       ('360723', '大余县', 3, '360700', '江西省赣州市大余县', '360700'),
       ('360724', '上犹县', 3, '360700', '江西省赣州市上犹县', '360700'),
       ('360725', '崇义县', 3, '360700', '江西省赣州市崇义县', '360700'),
       ('360726', '安远县', 3, '360700', '江西省赣州市安远县', '360700'),
       ('360728', '定南县', 3, '360700', '江西省赣州市定南县', '360700'),
       ('360729', '全南县', 3, '360700', '江西省赣州市全南县', '360700');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('360730', '宁都县', 3, '360700', '江西省赣州市宁都县', '360700'),
       ('360731', '于都县', 3, '360700', '江西省赣州市于都县', '360700'),
       ('360732', '兴国县', 3, '360700', '江西省赣州市兴国县', '360700'),
       ('360733', '会昌县', 3, '360700', '江西省赣州市会昌县', '360700'),
       ('360734', '寻乌县', 3, '360700', '江西省赣州市寻乌县', '360700'),
       ('360735', '石城县', 3, '360700', '江西省赣州市石城县', '360700'),
       ('360781', '瑞金市', 3, '360700', '江西省赣州市瑞金市', '360700'),
       ('360783', '龙南市', 3, '360700', '江西省赣州市龙南市', '360700'),
       ('360800', '吉安市', 2, '360000', '江西省吉安市', '360800'),
       ('360802', '吉州区', 3, '360800', '江西省吉安市吉州区', '360800');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('360803', '青原区', 3, '360800', '江西省吉安市青原区', '360800'),
       ('360821', '吉安县', 3, '360800', '江西省吉安市吉安县', '360800'),
       ('360822', '吉水县', 3, '360800', '江西省吉安市吉水县', '360800'),
       ('360823', '峡江县', 3, '360800', '江西省吉安市峡江县', '360800'),
       ('360824', '新干县', 3, '360800', '江西省吉安市新干县', '360800'),
       ('360825', '永丰县', 3, '360800', '江西省吉安市永丰县', '360800'),
       ('360826', '泰和县', 3, '360800', '江西省吉安市泰和县', '360800'),
       ('360827', '遂川县', 3, '360800', '江西省吉安市遂川县', '360800'),
       ('360828', '万安县', 3, '360800', '江西省吉安市万安县', '360800'),
       ('360829', '安福县', 3, '360800', '江西省吉安市安福县', '360800');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('360830', '永新县', 3, '360800', '江西省吉安市永新县', '360800'),
       ('360881', '井冈山市', 3, '360800', '江西省吉安市井冈山市', '360800'),
       ('360900', '宜春市', 2, '360000', '江西省宜春市', '360900'),
       ('360902', '袁州区', 3, '360900', '江西省宜春市袁州区', '360900'),
       ('360921', '奉新县', 3, '360900', '江西省宜春市奉新县', '360900'),
       ('360922', '万载县', 3, '360900', '江西省宜春市万载县', '360900'),
       ('360923', '上高县', 3, '360900', '江西省宜春市上高县', '360900'),
       ('360924', '宜丰县', 3, '360900', '江西省宜春市宜丰县', '360900'),
       ('360925', '靖安县', 3, '360900', '江西省宜春市靖安县', '360900'),
       ('360926', '铜鼓县', 3, '360900', '江西省宜春市铜鼓县', '360900');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('360981', '丰城市', 3, '360900', '江西省宜春市丰城市', '360900'),
       ('360982', '樟树市', 3, '360900', '江西省宜春市樟树市', '360900'),
       ('360983', '高安市', 3, '360900', '江西省宜春市高安市', '360900'),
       ('361000', '抚州市', 2, '360000', '江西省抚州市', '361000'),
       ('361002', '临川区', 3, '361000', '江西省抚州市临川区', '361000'),
       ('361003', '东乡区', 3, '361000', '江西省抚州市东乡区', '361000'),
       ('361021', '南城县', 3, '361000', '江西省抚州市南城县', '361000'),
       ('361022', '黎川县', 3, '361000', '江西省抚州市黎川县', '361000'),
       ('361023', '南丰县', 3, '361000', '江西省抚州市南丰县', '361000'),
       ('361024', '崇仁县', 3, '361000', '江西省抚州市崇仁县', '361000');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('361025', '乐安县', 3, '361000', '江西省抚州市乐安县', '361000'),
       ('361026', '宜黄县', 3, '361000', '江西省抚州市宜黄县', '361000'),
       ('361027', '金溪县', 3, '361000', '江西省抚州市金溪县', '361000'),
       ('361028', '资溪县', 3, '361000', '江西省抚州市资溪县', '361000'),
       ('361030', '广昌县', 3, '361000', '江西省抚州市广昌县', '361000'),
       ('361100', '上饶市', 2, '360000', '江西省上饶市', '361100'),
       ('361102', '信州区', 3, '361100', '江西省上饶市信州区', '361100'),
       ('361103', '广丰区', 3, '361100', '江西省上饶市广丰区', '361100'),
       ('361104', '广信区', 3, '361100', '江西省上饶市广信区', '361100'),
       ('361123', '玉山县', 3, '361100', '江西省上饶市玉山县', '361100');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('361124', '铅山县', 3, '361100', '江西省上饶市铅山县', '361100'),
       ('361125', '横峰县', 3, '361100', '江西省上饶市横峰县', '361100'),
       ('361126', '弋阳县', 3, '361100', '江西省上饶市弋阳县', '361100'),
       ('361127', '余干县', 3, '361100', '江西省上饶市余干县', '361100'),
       ('361128', '鄱阳县', 3, '361100', '江西省上饶市鄱阳县', '361100'),
       ('361129', '万年县', 3, '361100', '江西省上饶市万年县', '361100'),
       ('361130', '婺源县', 3, '361100', '江西省上饶市婺源县', '361100'),
       ('361181', '德兴市', 3, '361100', '江西省上饶市德兴市', '361100'),
       ('370000', '山东省', 1, NULL, '山东省', '370000'),
       ('370100', '济南市', 2, '370000', '山东省济南市', '370100');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('370102', '历下区', 3, '370100', '山东省济南市历下区', '370100'),
       ('370103', '市中区', 3, '370100', '山东省济南市市中区', '370100'),
       ('370104', '槐荫区', 3, '370100', '山东省济南市槐荫区', '370100'),
       ('370105', '天桥区', 3, '370100', '山东省济南市天桥区', '370100'),
       ('370112', '历城区', 3, '370100', '山东省济南市历城区', '370100'),
       ('370113', '长清区', 3, '370100', '山东省济南市长清区', '370100'),
       ('370114', '章丘区', 3, '370100', '山东省济南市章丘区', '370100'),
       ('370115', '济阳区', 3, '370100', '山东省济南市济阳区', '370100'),
       ('370116', '莱芜区', 3, '370100', '山东省济南市莱芜区', '370100'),
       ('370117', '钢城区', 3, '370100', '山东省济南市钢城区', '370100');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('370124', '平阴县', 3, '370100', '山东省济南市平阴县', '370100'),
       ('370126', '商河县', 3, '370100', '山东省济南市商河县', '370100'),
       ('370200', '青岛市', 2, '370000', '山东省青岛市', '370200'),
       ('370202', '市南区', 3, '370200', '山东省青岛市市南区', '370200'),
       ('370203', '市北区', 3, '370200', '山东省青岛市市北区', '370200'),
       ('370211', '黄岛区', 3, '370200', '山东省青岛市黄岛区', '370200'),
       ('370212', '崂山区', 3, '370200', '山东省青岛市崂山区', '370200'),
       ('370213', '李沧区', 3, '370200', '山东省青岛市李沧区', '370200'),
       ('370214', '城阳区', 3, '370200', '山东省青岛市城阳区', '370200'),
       ('370215', '即墨区', 3, '370200', '山东省青岛市即墨区', '370200');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('370281', '胶州市', 3, '370200', '山东省青岛市胶州市', '370200'),
       ('370283', '平度市', 3, '370200', '山东省青岛市平度市', '370200'),
       ('370285', '莱西市', 3, '370200', '山东省青岛市莱西市', '370200'),
       ('370300', '淄博市', 2, '370000', '山东省淄博市', '370300'),
       ('370302', '淄川区', 3, '370300', '山东省淄博市淄川区', '370300'),
       ('370303', '张店区', 3, '370300', '山东省淄博市张店区', '370300'),
       ('370304', '博山区', 3, '370300', '山东省淄博市博山区', '370300'),
       ('370305', '临淄区', 3, '370300', '山东省淄博市临淄区', '370300'),
       ('370306', '周村区', 3, '370300', '山东省淄博市周村区', '370300'),
       ('370321', '桓台县', 3, '370300', '山东省淄博市桓台县', '370300');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('370322', '高青县', 3, '370300', '山东省淄博市高青县', '370300'),
       ('370323', '沂源县', 3, '370300', '山东省淄博市沂源县', '370300'),
       ('370400', '枣庄市', 2, '370000', '山东省枣庄市', '370400'),
       ('370402', '市中区', 3, '370400', '山东省枣庄市市中区', '370400'),
       ('370403', '薛城区', 3, '370400', '山东省枣庄市薛城区', '370400'),
       ('370404', '峄城区', 3, '370400', '山东省枣庄市峄城区', '370400'),
       ('370405', '台儿庄区', 3, '370400', '山东省枣庄市台儿庄区', '370400'),
       ('370406', '山亭区', 3, '370400', '山东省枣庄市山亭区', '370400'),
       ('370481', '滕州市', 3, '370400', '山东省枣庄市滕州市', '370400'),
       ('370500', '东营市', 2, '370000', '山东省东营市', '370500');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('370502', '东营区', 3, '370500', '山东省东营市东营区', '370500'),
       ('370503', '河口区', 3, '370500', '山东省东营市河口区', '370500'),
       ('370505', '垦利区', 3, '370500', '山东省东营市垦利区', '370500'),
       ('370522', '利津县', 3, '370500', '山东省东营市利津县', '370500'),
       ('370523', '广饶县', 3, '370500', '山东省东营市广饶县', '370500'),
       ('370600', '烟台市', 2, '370000', '山东省烟台市', '370600'),
       ('370602', '芝罘区', 3, '370600', '山东省烟台市芝罘区', '370600'),
       ('370611', '福山区', 3, '370600', '山东省烟台市福山区', '370600'),
       ('370612', '牟平区', 3, '370600', '山东省烟台市牟平区', '370600'),
       ('370613', '莱山区', 3, '370600', '山东省烟台市莱山区', '370600');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('370614', '蓬莱区', 3, '370600', '山东省烟台市蓬莱区', '370600'),
       ('370681', '龙口市', 3, '370600', '山东省烟台市龙口市', '370600'),
       ('370682', '莱阳市', 3, '370600', '山东省烟台市莱阳市', '370600'),
       ('370683', '莱州市', 3, '370600', '山东省烟台市莱州市', '370600'),
       ('370685', '招远市', 3, '370600', '山东省烟台市招远市', '370600'),
       ('370686', '栖霞市', 3, '370600', '山东省烟台市栖霞市', '370600'),
       ('370687', '海阳市', 3, '370600', '山东省烟台市海阳市', '370600'),
       ('370700', '潍坊市', 2, '370000', '山东省潍坊市', '370700'),
       ('370702', '潍城区', 3, '370700', '山东省潍坊市潍城区', '370700'),
       ('370703', '寒亭区', 3, '370700', '山东省潍坊市寒亭区', '370700');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('370704', '坊子区', 3, '370700', '山东省潍坊市坊子区', '370700'),
       ('370705', '奎文区', 3, '370700', '山东省潍坊市奎文区', '370700'),
       ('370724', '临朐县', 3, '370700', '山东省潍坊市临朐县', '370700'),
       ('370725', '昌乐县', 3, '370700', '山东省潍坊市昌乐县', '370700'),
       ('370781', '青州市', 3, '370700', '山东省潍坊市青州市', '370700'),
       ('370782', '诸城市', 3, '370700', '山东省潍坊市诸城市', '370700'),
       ('370783', '寿光市', 3, '370700', '山东省潍坊市寿光市', '370700'),
       ('370784', '安丘市', 3, '370700', '山东省潍坊市安丘市', '370700'),
       ('370785', '高密市', 3, '370700', '山东省潍坊市高密市', '370700'),
       ('370786', '昌邑市', 3, '370700', '山东省潍坊市昌邑市', '370700');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('370800', '济宁市', 2, '370000', '山东省济宁市', '370800'),
       ('370811', '任城区', 3, '370800', '山东省济宁市任城区', '370800'),
       ('370812', '兖州区', 3, '370800', '山东省济宁市兖州区', '370800'),
       ('370826', '微山县', 3, '370800', '山东省济宁市微山县', '370800'),
       ('370827', '鱼台县', 3, '370800', '山东省济宁市鱼台县', '370800'),
       ('370828', '金乡县', 3, '370800', '山东省济宁市金乡县', '370800'),
       ('370829', '嘉祥县', 3, '370800', '山东省济宁市嘉祥县', '370800'),
       ('370830', '汶上县', 3, '370800', '山东省济宁市汶上县', '370800'),
       ('370831', '泗水县', 3, '370800', '山东省济宁市泗水县', '370800'),
       ('370832', '梁山县', 3, '370800', '山东省济宁市梁山县', '370800');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('370881', '曲阜市', 3, '370800', '山东省济宁市曲阜市', '370800'),
       ('370883', '邹城市', 3, '370800', '山东省济宁市邹城市', '370800'),
       ('370900', '泰安市', 2, '370000', '山东省泰安市', '370900'),
       ('370902', '泰山区', 3, '370900', '山东省泰安市泰山区', '370900'),
       ('370911', '岱岳区', 3, '370900', '山东省泰安市岱岳区', '370900'),
       ('370921', '宁阳县', 3, '370900', '山东省泰安市宁阳县', '370900'),
       ('370923', '东平县', 3, '370900', '山东省泰安市东平县', '370900'),
       ('370982', '新泰市', 3, '370900', '山东省泰安市新泰市', '370900'),
       ('370983', '肥城市', 3, '370900', '山东省泰安市肥城市', '370900'),
       ('371000', '威海市', 2, '370000', '山东省威海市', '371000');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('371002', '环翠区', 3, '371000', '山东省威海市环翠区', '371000'),
       ('371003', '文登区', 3, '371000', '山东省威海市文登区', '371000'),
       ('371082', '荣成市', 3, '371000', '山东省威海市荣成市', '371000'),
       ('371083', '乳山市', 3, '371000', '山东省威海市乳山市', '371000'),
       ('371100', '日照市', 2, '370000', '山东省日照市', '371100'),
       ('371102', '东港区', 3, '371100', '山东省日照市东港区', '371100'),
       ('371103', '岚山区', 3, '371100', '山东省日照市岚山区', '371100'),
       ('371121', '五莲县', 3, '371100', '山东省日照市五莲县', '371100'),
       ('371122', '莒县', 3, '371100', '山东省日照市莒县', '371100'),
       ('371300', '临沂市', 2, '370000', '山东省临沂市', '371300');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('371302', '兰山区', 3, '371300', '山东省临沂市兰山区', '371300'),
       ('371311', '罗庄区', 3, '371300', '山东省临沂市罗庄区', '371300'),
       ('371312', '河东区', 3, '371300', '山东省临沂市河东区', '371300'),
       ('371321', '沂南县', 3, '371300', '山东省临沂市沂南县', '371300'),
       ('371322', '郯城县', 3, '371300', '山东省临沂市郯城县', '371300'),
       ('371323', '沂水县', 3, '371300', '山东省临沂市沂水县', '371300'),
       ('371324', '兰陵县', 3, '371300', '山东省临沂市兰陵县', '371300'),
       ('371325', '费县', 3, '371300', '山东省临沂市费县', '371300'),
       ('371326', '平邑县', 3, '371300', '山东省临沂市平邑县', '371300'),
       ('371327', '莒南县', 3, '371300', '山东省临沂市莒南县', '371300');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('371328', '蒙阴县', 3, '371300', '山东省临沂市蒙阴县', '371300'),
       ('371329', '临沭县', 3, '371300', '山东省临沂市临沭县', '371300'),
       ('371400', '德州市', 2, '370000', '山东省德州市', '371400'),
       ('371402', '德城区', 3, '371400', '山东省德州市德城区', '371400'),
       ('371403', '陵城区', 3, '371400', '山东省德州市陵城区', '371400'),
       ('371422', '宁津县', 3, '371400', '山东省德州市宁津县', '371400'),
       ('371423', '庆云县', 3, '371400', '山东省德州市庆云县', '371400'),
       ('371424', '临邑县', 3, '371400', '山东省德州市临邑县', '371400'),
       ('371425', '齐河县', 3, '371400', '山东省德州市齐河县', '371400'),
       ('371426', '平原县', 3, '371400', '山东省德州市平原县', '371400');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('371427', '夏津县', 3, '371400', '山东省德州市夏津县', '371400'),
       ('371428', '武城县', 3, '371400', '山东省德州市武城县', '371400'),
       ('371481', '乐陵市', 3, '371400', '山东省德州市乐陵市', '371400'),
       ('371482', '禹城市', 3, '371400', '山东省德州市禹城市', '371400'),
       ('371500', '聊城市', 2, '370000', '山东省聊城市', '371500'),
       ('371502', '东昌府区', 3, '371500', '山东省聊城市东昌府区', '371500'),
       ('371503', '茌平区', 3, '371500', '山东省聊城市茌平区', '371500'),
       ('371521', '阳谷县', 3, '371500', '山东省聊城市阳谷县', '371500'),
       ('371522', '莘县', 3, '371500', '山东省聊城市莘县', '371500'),
       ('371524', '东阿县', 3, '371500', '山东省聊城市东阿县', '371500');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('371525', '冠县', 3, '371500', '山东省聊城市冠县', '371500'),
       ('371526', '高唐县', 3, '371500', '山东省聊城市高唐县', '371500'),
       ('371581', '临清市', 3, '371500', '山东省聊城市临清市', '371500'),
       ('371600', '滨州市', 2, '370000', '山东省滨州市', '371600'),
       ('371602', '滨城区', 3, '371600', '山东省滨州市滨城区', '371600'),
       ('371603', '沾化区', 3, '371600', '山东省滨州市沾化区', '371600'),
       ('371621', '惠民县', 3, '371600', '山东省滨州市惠民县', '371600'),
       ('371622', '阳信县', 3, '371600', '山东省滨州市阳信县', '371600'),
       ('371623', '无棣县', 3, '371600', '山东省滨州市无棣县', '371600'),
       ('371625', '博兴县', 3, '371600', '山东省滨州市博兴县', '371600');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('371681', '邹平市', 3, '371600', '山东省滨州市邹平市', '371600'),
       ('371700', '菏泽市', 2, '370000', '山东省菏泽市', '371700'),
       ('371702', '牡丹区', 3, '371700', '山东省菏泽市牡丹区', '371700'),
       ('371703', '定陶区', 3, '371700', '山东省菏泽市定陶区', '371700'),
       ('371721', '曹县', 3, '371700', '山东省菏泽市曹县', '371700'),
       ('371722', '单县', 3, '371700', '山东省菏泽市单县', '371700'),
       ('371723', '成武县', 3, '371700', '山东省菏泽市成武县', '371700'),
       ('371724', '巨野县', 3, '371700', '山东省菏泽市巨野县', '371700'),
       ('371725', '郓城县', 3, '371700', '山东省菏泽市郓城县', '371700'),
       ('371726', '鄄城县', 3, '371700', '山东省菏泽市鄄城县', '371700');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('371728', '东明县', 3, '371700', '山东省菏泽市东明县', '371700'),
       ('410000', '河南省', 1, NULL, '河南省', '410000'),
       ('410100', '郑州市', 2, '410000', '河南省郑州市', '410100'),
       ('410102', '中原区', 3, '410100', '河南省郑州市中原区', '410100'),
       ('410103', '二七区', 3, '410100', '河南省郑州市二七区', '410100'),
       ('410104', '管城回族区', 3, '410100', '河南省郑州市管城回族区', '410100'),
       ('410105', '金水区', 3, '410100', '河南省郑州市金水区', '410100'),
       ('410106', '上街区', 3, '410100', '河南省郑州市上街区', '410100'),
       ('410108', '惠济区', 3, '410100', '河南省郑州市惠济区', '410100'),
       ('410122', '中牟县', 3, '410100', '河南省郑州市中牟县', '410100');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('410181', '巩义市', 3, '410100', '河南省郑州市巩义市', '410100'),
       ('410182', '荥阳市', 3, '410100', '河南省郑州市荥阳市', '410100'),
       ('410183', '新密市', 3, '410100', '河南省郑州市新密市', '410100'),
       ('410184', '新郑市', 3, '410100', '河南省郑州市新郑市', '410100'),
       ('410185', '登封市', 3, '410100', '河南省郑州市登封市', '410100'),
       ('410200', '开封市', 2, '410000', '河南省开封市', '410200'),
       ('410202', '龙亭区', 3, '410200', '河南省开封市龙亭区', '410200'),
       ('410203', '顺河回族区', 3, '410200', '河南省开封市顺河回族区', '410200'),
       ('410204', '鼓楼区', 3, '410200', '河南省开封市鼓楼区', '410200'),
       ('410205', '禹王台区', 3, '410200', '河南省开封市禹王台区', '410200');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('410212', '祥符区', 3, '410200', '河南省开封市祥符区', '410200'),
       ('410221', '杞县', 3, '410200', '河南省开封市杞县', '410200'),
       ('410222', '通许县', 3, '410200', '河南省开封市通许县', '410200'),
       ('410223', '尉氏县', 3, '410200', '河南省开封市尉氏县', '410200'),
       ('410225', '兰考县', 3, '410200', '河南省开封市兰考县', '410200'),
       ('410300', '洛阳市', 2, '410000', '河南省洛阳市', '410300'),
       ('410302', '老城区', 3, '410300', '河南省洛阳市老城区', '410300'),
       ('410303', '西工区', 3, '410300', '河南省洛阳市西工区', '410300'),
       ('410304', '瀍河回族区', 3, '410300', '河南省洛阳市瀍河回族区', '410300'),
       ('410305', '涧西区', 3, '410300', '河南省洛阳市涧西区', '410300');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('410306', '孟津区', 3, '410300', '河南省洛阳市孟津区', '410300'),
       ('410311', '洛龙区', 3, '410300', '河南省洛阳市洛龙区', '410300'),
       ('410323', '新安县', 3, '410300', '河南省洛阳市新安县', '410300'),
       ('410324', '栾川县', 3, '410300', '河南省洛阳市栾川县', '410300'),
       ('410325', '嵩县', 3, '410300', '河南省洛阳市嵩县', '410300'),
       ('410326', '汝阳县', 3, '410300', '河南省洛阳市汝阳县', '410300'),
       ('410327', '宜阳县', 3, '410300', '河南省洛阳市宜阳县', '410300'),
       ('410328', '洛宁县', 3, '410300', '河南省洛阳市洛宁县', '410300'),
       ('410329', '伊川县', 3, '410300', '河南省洛阳市伊川县', '410300'),
       ('410381', '偃师区', 3, '410300', '河南省洛阳市偃师区', '410300');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('410400', '平顶山市', 2, '410000', '河南省平顶山市', '410400'),
       ('410402', '新华区', 3, '410400', '河南省平顶山市新华区', '410400'),
       ('410403', '卫东区', 3, '410400', '河南省平顶山市卫东区', '410400'),
       ('410404', '石龙区', 3, '410400', '河南省平顶山市石龙区', '410400'),
       ('410411', '湛河区', 3, '410400', '河南省平顶山市湛河区', '410400'),
       ('410421', '宝丰县', 3, '410400', '河南省平顶山市宝丰县', '410400'),
       ('410422', '叶县', 3, '410400', '河南省平顶山市叶县', '410400'),
       ('410423', '鲁山县', 3, '410400', '河南省平顶山市鲁山县', '410400'),
       ('410425', '郏县', 3, '410400', '河南省平顶山市郏县', '410400'),
       ('410481', '舞钢市', 3, '410400', '河南省平顶山市舞钢市', '410400');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('410482', '汝州市', 3, '410400', '河南省平顶山市汝州市', '410400'),
       ('410500', '安阳市', 2, '410000', '河南省安阳市', '410500'),
       ('410502', '文峰区', 3, '410500', '河南省安阳市文峰区', '410500'),
       ('410503', '北关区', 3, '410500', '河南省安阳市北关区', '410500'),
       ('410505', '殷都区', 3, '410500', '河南省安阳市殷都区', '410500'),
       ('410506', '龙安区', 3, '410500', '河南省安阳市龙安区', '410500'),
       ('410522', '安阳县', 3, '410500', '河南省安阳市安阳县', '410500'),
       ('410523', '汤阴县', 3, '410500', '河南省安阳市汤阴县', '410500'),
       ('410526', '滑县', 3, '410500', '河南省安阳市滑县', '410500'),
       ('410527', '内黄县', 3, '410500', '河南省安阳市内黄县', '410500');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('410581', '林州市', 3, '410500', '河南省安阳市林州市', '410500'),
       ('410600', '鹤壁市', 2, '410000', '河南省鹤壁市', '410600'),
       ('410602', '鹤山区', 3, '410600', '河南省鹤壁市鹤山区', '410600'),
       ('410603', '山城区', 3, '410600', '河南省鹤壁市山城区', '410600'),
       ('410611', '淇滨区', 3, '410600', '河南省鹤壁市淇滨区', '410600'),
       ('410621', '浚县', 3, '410600', '河南省鹤壁市浚县', '410600'),
       ('410622', '淇县', 3, '410600', '河南省鹤壁市淇县', '410600'),
       ('410700', '新乡市', 2, '410000', '河南省新乡市', '410700'),
       ('410702', '红旗区', 3, '410700', '河南省新乡市红旗区', '410700'),
       ('410703', '卫滨区', 3, '410700', '河南省新乡市卫滨区', '410700');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('410704', '凤泉区', 3, '410700', '河南省新乡市凤泉区', '410700'),
       ('410711', '牧野区', 3, '410700', '河南省新乡市牧野区', '410700'),
       ('410721', '新乡县', 3, '410700', '河南省新乡市新乡县', '410700'),
       ('410724', '获嘉县', 3, '410700', '河南省新乡市获嘉县', '410700'),
       ('410725', '原阳县', 3, '410700', '河南省新乡市原阳县', '410700'),
       ('410726', '延津县', 3, '410700', '河南省新乡市延津县', '410700'),
       ('410727', '封丘县', 3, '410700', '河南省新乡市封丘县', '410700'),
       ('410781', '卫辉市', 3, '410700', '河南省新乡市卫辉市', '410700'),
       ('410782', '辉县市', 3, '410700', '河南省新乡市辉县市', '410700'),
       ('410783', '长垣市', 3, '410700', '河南省新乡市长垣市', '410700');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('410800', '焦作市', 2, '410000', '河南省焦作市', '410800'),
       ('410802', '解放区', 3, '410800', '河南省焦作市解放区', '410800'),
       ('410803', '中站区', 3, '410800', '河南省焦作市中站区', '410800'),
       ('410804', '马村区', 3, '410800', '河南省焦作市马村区', '410800'),
       ('410811', '山阳区', 3, '410800', '河南省焦作市山阳区', '410800'),
       ('410821', '修武县', 3, '410800', '河南省焦作市修武县', '410800'),
       ('410822', '博爱县', 3, '410800', '河南省焦作市博爱县', '410800'),
       ('410823', '武陟县', 3, '410800', '河南省焦作市武陟县', '410800'),
       ('410825', '温县', 3, '410800', '河南省焦作市温县', '410800'),
       ('410882', '沁阳市', 3, '410800', '河南省焦作市沁阳市', '410800');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('410883', '孟州市', 3, '410800', '河南省焦作市孟州市', '410800'),
       ('410900', '濮阳市', 2, '410000', '河南省濮阳市', '410900'),
       ('410902', '华龙区', 3, '410900', '河南省濮阳市华龙区', '410900'),
       ('410922', '清丰县', 3, '410900', '河南省濮阳市清丰县', '410900'),
       ('410923', '南乐县', 3, '410900', '河南省濮阳市南乐县', '410900'),
       ('410926', '范县', 3, '410900', '河南省濮阳市范县', '410900'),
       ('410927', '台前县', 3, '410900', '河南省濮阳市台前县', '410900'),
       ('410928', '濮阳县', 3, '410900', '河南省濮阳市濮阳县', '410900'),
       ('411000', '许昌市', 2, '410000', '河南省许昌市', '411000'),
       ('411002', '魏都区', 3, '411000', '河南省许昌市魏都区', '411000');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('411003', '建安区', 3, '411000', '河南省许昌市建安区', '411000'),
       ('411024', '鄢陵县', 3, '411000', '河南省许昌市鄢陵县', '411000'),
       ('411025', '襄城县', 3, '411000', '河南省许昌市襄城县', '411000'),
       ('411081', '禹州市', 3, '411000', '河南省许昌市禹州市', '411000'),
       ('411082', '长葛市', 3, '411000', '河南省许昌市长葛市', '411000'),
       ('411100', '漯河市', 2, '410000', '河南省漯河市', '411100'),
       ('411102', '源汇区', 3, '411100', '河南省漯河市源汇区', '411100'),
       ('411103', '郾城区', 3, '411100', '河南省漯河市郾城区', '411100'),
       ('411104', '召陵区', 3, '411100', '河南省漯河市召陵区', '411100'),
       ('411121', '舞阳县', 3, '411100', '河南省漯河市舞阳县', '411100');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('411122', '临颍县', 3, '411100', '河南省漯河市临颍县', '411100'),
       ('411200', '三门峡市', 2, '410000', '河南省三门峡市', '411200'),
       ('411202', '湖滨区', 3, '411200', '河南省三门峡市湖滨区', '411200'),
       ('411203', '陕州区', 3, '411200', '河南省三门峡市陕州区', '411200'),
       ('411221', '渑池县', 3, '411200', '河南省三门峡市渑池县', '411200'),
       ('411224', '卢氏县', 3, '411200', '河南省三门峡市卢氏县', '411200'),
       ('411281', '义马市', 3, '411200', '河南省三门峡市义马市', '411200'),
       ('411282', '灵宝市', 3, '411200', '河南省三门峡市灵宝市', '411200'),
       ('411300', '南阳市', 2, '410000', '河南省南阳市', '411300'),
       ('411302', '宛城区', 3, '411300', '河南省南阳市宛城区', '411300');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('411303', '卧龙区', 3, '411300', '河南省南阳市卧龙区', '411300'),
       ('411321', '南召县', 3, '411300', '河南省南阳市南召县', '411300'),
       ('411322', '方城县', 3, '411300', '河南省南阳市方城县', '411300'),
       ('411323', '西峡县', 3, '411300', '河南省南阳市西峡县', '411300'),
       ('411324', '镇平县', 3, '411300', '河南省南阳市镇平县', '411300'),
       ('411325', '内乡县', 3, '411300', '河南省南阳市内乡县', '411300'),
       ('411326', '淅川县', 3, '411300', '河南省南阳市淅川县', '411300'),
       ('411327', '社旗县', 3, '411300', '河南省南阳市社旗县', '411300'),
       ('411328', '唐河县', 3, '411300', '河南省南阳市唐河县', '411300'),
       ('411329', '新野县', 3, '411300', '河南省南阳市新野县', '411300');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('411330', '桐柏县', 3, '411300', '河南省南阳市桐柏县', '411300'),
       ('411381', '邓州市', 3, '411300', '河南省南阳市邓州市', '411300'),
       ('411400', '商丘市', 2, '410000', '河南省商丘市', '411400'),
       ('411402', '梁园区', 3, '411400', '河南省商丘市梁园区', '411400'),
       ('411403', '睢阳区', 3, '411400', '河南省商丘市睢阳区', '411400'),
       ('411421', '民权县', 3, '411400', '河南省商丘市民权县', '411400'),
       ('411422', '睢县', 3, '411400', '河南省商丘市睢县', '411400'),
       ('411423', '宁陵县', 3, '411400', '河南省商丘市宁陵县', '411400'),
       ('411424', '柘城县', 3, '411400', '河南省商丘市柘城县', '411400'),
       ('411425', '虞城县', 3, '411400', '河南省商丘市虞城县', '411400');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('411426', '夏邑县', 3, '411400', '河南省商丘市夏邑县', '411400'),
       ('411481', '永城市', 3, '411400', '河南省商丘市永城市', '411400'),
       ('411500', '信阳市', 2, '410000', '河南省信阳市', '411500'),
       ('411502', '浉河区', 3, '411500', '河南省信阳市浉河区', '411500'),
       ('411503', '平桥区', 3, '411500', '河南省信阳市平桥区', '411500'),
       ('411521', '罗山县', 3, '411500', '河南省信阳市罗山县', '411500'),
       ('411522', '光山县', 3, '411500', '河南省信阳市光山县', '411500'),
       ('411523', '新县', 3, '411500', '河南省信阳市新县', '411500'),
       ('411524', '商城县', 3, '411500', '河南省信阳市商城县', '411500'),
       ('411525', '固始县', 3, '411500', '河南省信阳市固始县', '411500');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('411526', '潢川县', 3, '411500', '河南省信阳市潢川县', '411500'),
       ('411527', '淮滨县', 3, '411500', '河南省信阳市淮滨县', '411500'),
       ('411528', '息县', 3, '411500', '河南省信阳市息县', '411500'),
       ('411600', '周口市', 2, '410000', '河南省周口市', '411600'),
       ('411602', '川汇区', 3, '411600', '河南省周口市川汇区', '411600'),
       ('411603', '淮阳区', 3, '411600', '河南省周口市淮阳区', '411600'),
       ('411621', '扶沟县', 3, '411600', '河南省周口市扶沟县', '411600'),
       ('411622', '西华县', 3, '411600', '河南省周口市西华县', '411600'),
       ('411623', '商水县', 3, '411600', '河南省周口市商水县', '411600'),
       ('411624', '沈丘县', 3, '411600', '河南省周口市沈丘县', '411600');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('411625', '郸城县', 3, '411600', '河南省周口市郸城县', '411600'),
       ('411627', '太康县', 3, '411600', '河南省周口市太康县', '411600'),
       ('411628', '鹿邑县', 3, '411600', '河南省周口市鹿邑县', '411600'),
       ('411681', '项城市', 3, '411600', '河南省周口市项城市', '411600'),
       ('411700', '驻马店市', 2, '410000', '河南省驻马店市', '411700'),
       ('411702', '驿城区', 3, '411700', '河南省驻马店市驿城区', '411700'),
       ('411721', '西平县', 3, '411700', '河南省驻马店市西平县', '411700'),
       ('411722', '上蔡县', 3, '411700', '河南省驻马店市上蔡县', '411700'),
       ('411723', '平舆县', 3, '411700', '河南省驻马店市平舆县', '411700'),
       ('411724', '正阳县', 3, '411700', '河南省驻马店市正阳县', '411700');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('411725', '确山县', 3, '411700', '河南省驻马店市确山县', '411700'),
       ('411726', '泌阳县', 3, '411700', '河南省驻马店市泌阳县', '411700'),
       ('411727', '汝南县', 3, '411700', '河南省驻马店市汝南县', '411700'),
       ('411728', '遂平县', 3, '411700', '河南省驻马店市遂平县', '411700'),
       ('411729', '新蔡县', 3, '411700', '河南省驻马店市新蔡县', '411700'),
       ('419001', '济源市', 2, '410000', '河南省济源市', '419001'),
       ('420000', '湖北省', 1, NULL, '湖北省', '420000'),
       ('420100', '武汉市', 2, '420000', '湖北省武汉市', '420100'),
       ('420102', '江岸区', 3, '420100', '湖北省武汉市江岸区', '420100'),
       ('420103', '江汉区', 3, '420100', '湖北省武汉市江汉区', '420100');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('420104', '硚口区', 3, '420100', '湖北省武汉市硚口区', '420100'),
       ('420105', '汉阳区', 3, '420100', '湖北省武汉市汉阳区', '420100'),
       ('420106', '武昌区', 3, '420100', '湖北省武汉市武昌区', '420100'),
       ('420107', '青山区', 3, '420100', '湖北省武汉市青山区', '420100'),
       ('420111', '洪山区', 3, '420100', '湖北省武汉市洪山区', '420100'),
       ('420112', '东西湖区', 3, '420100', '湖北省武汉市东西湖区', '420100'),
       ('420113', '汉南区', 3, '420100', '湖北省武汉市汉南区', '420100'),
       ('420114', '蔡甸区', 3, '420100', '湖北省武汉市蔡甸区', '420100'),
       ('420115', '江夏区', 3, '420100', '湖北省武汉市江夏区', '420100'),
       ('420116', '黄陂区', 3, '420100', '湖北省武汉市黄陂区', '420100');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('420117', '新洲区', 3, '420100', '湖北省武汉市新洲区', '420100'),
       ('420200', '黄石市', 2, '420000', '湖北省黄石市', '420200'),
       ('420202', '黄石港区', 3, '420200', '湖北省黄石市黄石港区', '420200'),
       ('420203', '西塞山区', 3, '420200', '湖北省黄石市西塞山区', '420200'),
       ('420204', '下陆区', 3, '420200', '湖北省黄石市下陆区', '420200'),
       ('420205', '铁山区', 3, '420200', '湖北省黄石市铁山区', '420200'),
       ('420222', '阳新县', 3, '420200', '湖北省黄石市阳新县', '420200'),
       ('420281', '大冶市', 3, '420200', '湖北省黄石市大冶市', '420200'),
       ('420300', '十堰市', 2, '420000', '湖北省十堰市', '420300'),
       ('420302', '茅箭区', 3, '420300', '湖北省十堰市茅箭区', '420300');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('420303', '张湾区', 3, '420300', '湖北省十堰市张湾区', '420300'),
       ('420304', '郧阳区', 3, '420300', '湖北省十堰市郧阳区', '420300'),
       ('420322', '郧西县', 3, '420300', '湖北省十堰市郧西县', '420300'),
       ('420323', '竹山县', 3, '420300', '湖北省十堰市竹山县', '420300'),
       ('420324', '竹溪县', 3, '420300', '湖北省十堰市竹溪县', '420300'),
       ('420325', '房县', 3, '420300', '湖北省十堰市房县', '420300'),
       ('420381', '丹江口市', 3, '420300', '湖北省十堰市丹江口市', '420300'),
       ('420500', '宜昌市', 2, '420000', '湖北省宜昌市', '420500'),
       ('420502', '西陵区', 3, '420500', '湖北省宜昌市西陵区', '420500'),
       ('420503', '伍家岗区', 3, '420500', '湖北省宜昌市伍家岗区', '420500');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('420504', '点军区', 3, '420500', '湖北省宜昌市点军区', '420500'),
       ('420505', '猇亭区', 3, '420500', '湖北省宜昌市猇亭区', '420500'),
       ('420506', '夷陵区', 3, '420500', '湖北省宜昌市夷陵区', '420500'),
       ('420525', '远安县', 3, '420500', '湖北省宜昌市远安县', '420500'),
       ('420526', '兴山县', 3, '420500', '湖北省宜昌市兴山县', '420500'),
       ('420527', '秭归县', 3, '420500', '湖北省宜昌市秭归县', '420500'),
       ('420528', '长阳土家族自治县', 3, '420500', '湖北省宜昌市长阳土家族自治县', '420500'),
       ('420529', '五峰土家族自治县', 3, '420500', '湖北省宜昌市五峰土家族自治县', '420500'),
       ('420581', '宜都市', 3, '420500', '湖北省宜昌市宜都市', '420500'),
       ('420582', '当阳市', 3, '420500', '湖北省宜昌市当阳市', '420500');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('420583', '枝江市', 3, '420500', '湖北省宜昌市枝江市', '420500'),
       ('420600', '襄阳市', 2, '420000', '湖北省襄阳市', '420600'),
       ('420602', '襄城区', 3, '420600', '湖北省襄阳市襄城区', '420600'),
       ('420606', '樊城区', 3, '420600', '湖北省襄阳市樊城区', '420600'),
       ('420607', '襄州区', 3, '420600', '湖北省襄阳市襄州区', '420600'),
       ('420624', '南漳县', 3, '420600', '湖北省襄阳市南漳县', '420600'),
       ('420625', '谷城县', 3, '420600', '湖北省襄阳市谷城县', '420600'),
       ('420626', '保康县', 3, '420600', '湖北省襄阳市保康县', '420600'),
       ('420682', '老河口市', 3, '420600', '湖北省襄阳市老河口市', '420600'),
       ('420683', '枣阳市', 3, '420600', '湖北省襄阳市枣阳市', '420600');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('420684', '宜城市', 3, '420600', '湖北省襄阳市宜城市', '420600'),
       ('420700', '鄂州市', 2, '420000', '湖北省鄂州市', '420700'),
       ('420702', '梁子湖区', 3, '420700', '湖北省鄂州市梁子湖区', '420700'),
       ('420703', '华容区', 3, '420700', '湖北省鄂州市华容区', '420700'),
       ('420704', '鄂城区', 3, '420700', '湖北省鄂州市鄂城区', '420700'),
       ('420800', '荆门市', 2, '420000', '湖北省荆门市', '420800'),
       ('420802', '东宝区', 3, '420800', '湖北省荆门市东宝区', '420800'),
       ('420804', '掇刀区', 3, '420800', '湖北省荆门市掇刀区', '420800'),
       ('420822', '沙洋县', 3, '420800', '湖北省荆门市沙洋县', '420800'),
       ('420881', '钟祥市', 3, '420800', '湖北省荆门市钟祥市', '420800');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('420882', '京山市', 3, '420800', '湖北省荆门市京山市', '420800'),
       ('420900', '孝感市', 2, '420000', '湖北省孝感市', '420900'),
       ('420902', '孝南区', 3, '420900', '湖北省孝感市孝南区', '420900'),
       ('420921', '孝昌县', 3, '420900', '湖北省孝感市孝昌县', '420900'),
       ('420922', '大悟县', 3, '420900', '湖北省孝感市大悟县', '420900'),
       ('420923', '云梦县', 3, '420900', '湖北省孝感市云梦县', '420900'),
       ('420981', '应城市', 3, '420900', '湖北省孝感市应城市', '420900'),
       ('420982', '安陆市', 3, '420900', '湖北省孝感市安陆市', '420900'),
       ('420984', '汉川市', 3, '420900', '湖北省孝感市汉川市', '420900'),
       ('421000', '荆州市', 2, '420000', '湖北省荆州市', '421000');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('421002', '沙市区', 3, '421000', '湖北省荆州市沙市区', '421000'),
       ('421003', '荆州区', 3, '421000', '湖北省荆州市荆州区', '421000'),
       ('421022', '公安县', 3, '421000', '湖北省荆州市公安县', '421000'),
       ('421023', '监利市', 3, '421000', '湖北省荆州市监利市', '421000'),
       ('421024', '江陵县', 3, '421000', '湖北省荆州市江陵县', '421000'),
       ('421081', '石首市', 3, '421000', '湖北省荆州市石首市', '421000'),
       ('421083', '洪湖市', 3, '421000', '湖北省荆州市洪湖市', '421000'),
       ('421087', '松滋市', 3, '421000', '湖北省荆州市松滋市', '421000'),
       ('421100', '黄冈市', 2, '420000', '湖北省黄冈市', '421100'),
       ('421102', '黄州区', 3, '421100', '湖北省黄冈市黄州区', '421100');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('421121', '团风县', 3, '421100', '湖北省黄冈市团风县', '421100'),
       ('421122', '红安县', 3, '421100', '湖北省黄冈市红安县', '421100'),
       ('421123', '罗田县', 3, '421100', '湖北省黄冈市罗田县', '421100'),
       ('421124', '英山县', 3, '421100', '湖北省黄冈市英山县', '421100'),
       ('421125', '浠水县', 3, '421100', '湖北省黄冈市浠水县', '421100'),
       ('421126', '蕲春县', 3, '421100', '湖北省黄冈市蕲春县', '421100'),
       ('421127', '黄梅县', 3, '421100', '湖北省黄冈市黄梅县', '421100'),
       ('421181', '麻城市', 3, '421100', '湖北省黄冈市麻城市', '421100'),
       ('421182', '武穴市', 3, '421100', '湖北省黄冈市武穴市', '421100'),
       ('421200', '咸宁市', 2, '420000', '湖北省咸宁市', '421200');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('421202', '咸安区', 3, '421200', '湖北省咸宁市咸安区', '421200'),
       ('421221', '嘉鱼县', 3, '421200', '湖北省咸宁市嘉鱼县', '421200'),
       ('421222', '通城县', 3, '421200', '湖北省咸宁市通城县', '421200'),
       ('421223', '崇阳县', 3, '421200', '湖北省咸宁市崇阳县', '421200'),
       ('421224', '通山县', 3, '421200', '湖北省咸宁市通山县', '421200'),
       ('421281', '赤壁市', 3, '421200', '湖北省咸宁市赤壁市', '421200'),
       ('421300', '随州市', 2, '420000', '湖北省随州市', '421300'),
       ('421303', '曾都区', 3, '421300', '湖北省随州市曾都区', '421300'),
       ('421321', '随县', 3, '421300', '湖北省随州市随县', '421300'),
       ('421381', '广水市', 3, '421300', '湖北省随州市广水市', '421300');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('422800', '恩施土家族苗族自治州', 2, '420000', '湖北省恩施土家族苗族自治州', '422800'),
       ('422801', '恩施市', 3, '422800', '湖北省恩施土家族苗族自治州恩施市', '422800'),
       ('422802', '利川市', 3, '422800', '湖北省恩施土家族苗族自治州利川市', '422800'),
       ('422822', '建始县', 3, '422800', '湖北省恩施土家族苗族自治州建始县', '422800'),
       ('422823', '巴东县', 3, '422800', '湖北省恩施土家族苗族自治州巴东县', '422800'),
       ('422825', '宣恩县', 3, '422800', '湖北省恩施土家族苗族自治州宣恩县', '422800'),
       ('422826', '咸丰县', 3, '422800', '湖北省恩施土家族苗族自治州咸丰县', '422800'),
       ('422827', '来凤县', 3, '422800', '湖北省恩施土家族苗族自治州来凤县', '422800'),
       ('422828', '鹤峰县', 3, '422800', '湖北省恩施土家族苗族自治州鹤峰县', '422800'),
       ('429004', '仙桃市', 2, '420000', '湖北省仙桃市', '429004');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('429005', '潜江市', 2, '420000', '湖北省潜江市', '429005'),
       ('429006', '天门市', 2, '420000', '湖北省天门市', '429006'),
       ('429021', '神农架林区', 2, '420000', '湖北省神农架林区', '429021'),
       ('430000', '湖南省', 1, NULL, '湖南省', '430000'),
       ('430100', '长沙市', 2, '430000', '湖南省长沙市', '430100'),
       ('430102', '芙蓉区', 3, '430100', '湖南省长沙市芙蓉区', '430100'),
       ('430103', '天心区', 3, '430100', '湖南省长沙市天心区', '430100'),
       ('430104', '岳麓区', 3, '430100', '湖南省长沙市岳麓区', '430100'),
       ('430105', '开福区', 3, '430100', '湖南省长沙市开福区', '430100'),
       ('430111', '雨花区', 3, '430100', '湖南省长沙市雨花区', '430100');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('430112', '望城区', 3, '430100', '湖南省长沙市望城区', '430100'),
       ('430121', '长沙县', 3, '430100', '湖南省长沙市长沙县', '430100'),
       ('430181', '浏阳市', 3, '430100', '湖南省长沙市浏阳市', '430100'),
       ('430182', '宁乡市', 3, '430100', '湖南省长沙市宁乡市', '430100'),
       ('430200', '株洲市', 2, '430000', '湖南省株洲市', '430200'),
       ('430202', '荷塘区', 3, '430200', '湖南省株洲市荷塘区', '430200'),
       ('430203', '芦淞区', 3, '430200', '湖南省株洲市芦淞区', '430200'),
       ('430204', '石峰区', 3, '430200', '湖南省株洲市石峰区', '430200'),
       ('430211', '天元区', 3, '430200', '湖南省株洲市天元区', '430200'),
       ('430212', '渌口区', 3, '430200', '湖南省株洲市渌口区', '430200');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('430223', '攸县', 3, '430200', '湖南省株洲市攸县', '430200'),
       ('430224', '茶陵县', 3, '430200', '湖南省株洲市茶陵县', '430200'),
       ('430225', '炎陵县', 3, '430200', '湖南省株洲市炎陵县', '430200'),
       ('430281', '醴陵市', 3, '430200', '湖南省株洲市醴陵市', '430200'),
       ('430300', '湘潭市', 2, '430000', '湖南省湘潭市', '430300'),
       ('430302', '雨湖区', 3, '430300', '湖南省湘潭市雨湖区', '430300'),
       ('430304', '岳塘区', 3, '430300', '湖南省湘潭市岳塘区', '430300'),
       ('430321', '湘潭县', 3, '430300', '湖南省湘潭市湘潭县', '430300'),
       ('430381', '湘乡市', 3, '430300', '湖南省湘潭市湘乡市', '430300'),
       ('430382', '韶山市', 3, '430300', '湖南省湘潭市韶山市', '430300');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('430400', '衡阳市', 2, '430000', '湖南省衡阳市', '430400'),
       ('430405', '珠晖区', 3, '430400', '湖南省衡阳市珠晖区', '430400'),
       ('430406', '雁峰区', 3, '430400', '湖南省衡阳市雁峰区', '430400'),
       ('430407', '石鼓区', 3, '430400', '湖南省衡阳市石鼓区', '430400'),
       ('430408', '蒸湘区', 3, '430400', '湖南省衡阳市蒸湘区', '430400'),
       ('430412', '南岳区', 3, '430400', '湖南省衡阳市南岳区', '430400'),
       ('430421', '衡阳县', 3, '430400', '湖南省衡阳市衡阳县', '430400'),
       ('430422', '衡南县', 3, '430400', '湖南省衡阳市衡南县', '430400'),
       ('430423', '衡山县', 3, '430400', '湖南省衡阳市衡山县', '430400'),
       ('430424', '衡东县', 3, '430400', '湖南省衡阳市衡东县', '430400');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('430426', '祁东县', 3, '430400', '湖南省衡阳市祁东县', '430400'),
       ('430481', '耒阳市', 3, '430400', '湖南省衡阳市耒阳市', '430400'),
       ('430482', '常宁市', 3, '430400', '湖南省衡阳市常宁市', '430400'),
       ('430500', '邵阳市', 2, '430000', '湖南省邵阳市', '430500'),
       ('430502', '双清区', 3, '430500', '湖南省邵阳市双清区', '430500'),
       ('430503', '大祥区', 3, '430500', '湖南省邵阳市大祥区', '430500'),
       ('430511', '北塔区', 3, '430500', '湖南省邵阳市北塔区', '430500'),
       ('430522', '新邵县', 3, '430500', '湖南省邵阳市新邵县', '430500'),
       ('430523', '邵阳县', 3, '430500', '湖南省邵阳市邵阳县', '430500'),
       ('430524', '隆回县', 3, '430500', '湖南省邵阳市隆回县', '430500');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('430525', '洞口县', 3, '430500', '湖南省邵阳市洞口县', '430500'),
       ('430527', '绥宁县', 3, '430500', '湖南省邵阳市绥宁县', '430500'),
       ('430528', '新宁县', 3, '430500', '湖南省邵阳市新宁县', '430500'),
       ('430529', '城步苗族自治县', 3, '430500', '湖南省邵阳市城步苗族自治县', '430500'),
       ('430581', '武冈市', 3, '430500', '湖南省邵阳市武冈市', '430500'),
       ('430582', '邵东市', 3, '430500', '湖南省邵阳市邵东市', '430500'),
       ('430600', '岳阳市', 2, '430000', '湖南省岳阳市', '430600'),
       ('430602', '岳阳楼区', 3, '430600', '湖南省岳阳市岳阳楼区', '430600'),
       ('430603', '云溪区', 3, '430600', '湖南省岳阳市云溪区', '430600'),
       ('430611', '君山区', 3, '430600', '湖南省岳阳市君山区', '430600');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('430621', '岳阳县', 3, '430600', '湖南省岳阳市岳阳县', '430600'),
       ('430623', '华容县', 3, '430600', '湖南省岳阳市华容县', '430600'),
       ('430624', '湘阴县', 3, '430600', '湖南省岳阳市湘阴县', '430600'),
       ('430626', '平江县', 3, '430600', '湖南省岳阳市平江县', '430600'),
       ('430681', '汨罗市', 3, '430600', '湖南省岳阳市汨罗市', '430600'),
       ('430682', '临湘市', 3, '430600', '湖南省岳阳市临湘市', '430600'),
       ('430700', '常德市', 2, '430000', '湖南省常德市', '430700'),
       ('430702', '武陵区', 3, '430700', '湖南省常德市武陵区', '430700'),
       ('430703', '鼎城区', 3, '430700', '湖南省常德市鼎城区', '430700'),
       ('430721', '安乡县', 3, '430700', '湖南省常德市安乡县', '430700');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('430722', '汉寿县', 3, '430700', '湖南省常德市汉寿县', '430700'),
       ('430723', '澧县', 3, '430700', '湖南省常德市澧县', '430700'),
       ('430724', '临澧县', 3, '430700', '湖南省常德市临澧县', '430700'),
       ('430725', '桃源县', 3, '430700', '湖南省常德市桃源县', '430700'),
       ('430726', '石门县', 3, '430700', '湖南省常德市石门县', '430700'),
       ('430781', '津市市', 3, '430700', '湖南省常德市津市市', '430700'),
       ('430800', '张家界市', 2, '430000', '湖南省张家界市', '430800'),
       ('430802', '永定区', 3, '430800', '湖南省张家界市永定区', '430800'),
       ('430811', '武陵源区', 3, '430800', '湖南省张家界市武陵源区', '430800'),
       ('430821', '慈利县', 3, '430800', '湖南省张家界市慈利县', '430800');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('430822', '桑植县', 3, '430800', '湖南省张家界市桑植县', '430800'),
       ('430900', '益阳市', 2, '430000', '湖南省益阳市', '430900'),
       ('430902', '资阳区', 3, '430900', '湖南省益阳市资阳区', '430900'),
       ('430903', '赫山区', 3, '430900', '湖南省益阳市赫山区', '430900'),
       ('430921', '南县', 3, '430900', '湖南省益阳市南县', '430900'),
       ('430922', '桃江县', 3, '430900', '湖南省益阳市桃江县', '430900'),
       ('430923', '安化县', 3, '430900', '湖南省益阳市安化县', '430900'),
       ('430981', '沅江市', 3, '430900', '湖南省益阳市沅江市', '430900'),
       ('431000', '郴州市', 2, '430000', '湖南省郴州市', '431000'),
       ('431002', '北湖区', 3, '431000', '湖南省郴州市北湖区', '431000');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('431003', '苏仙区', 3, '431000', '湖南省郴州市苏仙区', '431000'),
       ('431021', '桂阳县', 3, '431000', '湖南省郴州市桂阳县', '431000'),
       ('431022', '宜章县', 3, '431000', '湖南省郴州市宜章县', '431000'),
       ('431023', '永兴县', 3, '431000', '湖南省郴州市永兴县', '431000'),
       ('431024', '嘉禾县', 3, '431000', '湖南省郴州市嘉禾县', '431000'),
       ('431025', '临武县', 3, '431000', '湖南省郴州市临武县', '431000'),
       ('431026', '汝城县', 3, '431000', '湖南省郴州市汝城县', '431000'),
       ('431027', '桂东县', 3, '431000', '湖南省郴州市桂东县', '431000'),
       ('431028', '安仁县', 3, '431000', '湖南省郴州市安仁县', '431000'),
       ('431081', '资兴市', 3, '431000', '湖南省郴州市资兴市', '431000');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('431100', '永州市', 2, '430000', '湖南省永州市', '431100'),
       ('431102', '零陵区', 3, '431100', '湖南省永州市零陵区', '431100'),
       ('431103', '冷水滩区', 3, '431100', '湖南省永州市冷水滩区', '431100'),
       ('431121', '祁阳市', 3, '431100', '湖南省永州市祁阳市', '431100'),
       ('431122', '东安县', 3, '431100', '湖南省永州市东安县', '431100'),
       ('431123', '双牌县', 3, '431100', '湖南省永州市双牌县', '431100'),
       ('431124', '道县', 3, '431100', '湖南省永州市道县', '431100'),
       ('431125', '江永县', 3, '431100', '湖南省永州市江永县', '431100'),
       ('431126', '宁远县', 3, '431100', '湖南省永州市宁远县', '431100'),
       ('431127', '蓝山县', 3, '431100', '湖南省永州市蓝山县', '431100');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('431128', '新田县', 3, '431100', '湖南省永州市新田县', '431100'),
       ('431129', '江华瑶族自治县', 3, '431100', '湖南省永州市江华瑶族自治县', '431100'),
       ('431200', '怀化市', 2, '430000', '湖南省怀化市', '431200'),
       ('431202', '鹤城区', 3, '431200', '湖南省怀化市鹤城区', '431200'),
       ('431221', '中方县', 3, '431200', '湖南省怀化市中方县', '431200'),
       ('431222', '沅陵县', 3, '431200', '湖南省怀化市沅陵县', '431200'),
       ('431223', '辰溪县', 3, '431200', '湖南省怀化市辰溪县', '431200'),
       ('431224', '溆浦县', 3, '431200', '湖南省怀化市溆浦县', '431200'),
       ('431225', '会同县', 3, '431200', '湖南省怀化市会同县', '431200'),
       ('431226', '麻阳苗族自治县', 3, '431200', '湖南省怀化市麻阳苗族自治县', '431200');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('431227', '新晃侗族自治县', 3, '431200', '湖南省怀化市新晃侗族自治县', '431200'),
       ('431228', '芷江侗族自治县', 3, '431200', '湖南省怀化市芷江侗族自治县', '431200'),
       ('431229', '靖州苗族侗族自治县', 3, '431200', '湖南省怀化市靖州苗族侗族自治县', '431200'),
       ('431230', '通道侗族自治县', 3, '431200', '湖南省怀化市通道侗族自治县', '431200'),
       ('431281', '洪江市', 3, '431200', '湖南省怀化市洪江市', '431200'),
       ('431300', '娄底市', 2, '430000', '湖南省娄底市', '431300'),
       ('431302', '娄星区', 3, '431300', '湖南省娄底市娄星区', '431300'),
       ('431321', '双峰县', 3, '431300', '湖南省娄底市双峰县', '431300'),
       ('431322', '新化县', 3, '431300', '湖南省娄底市新化县', '431300'),
       ('431381', '冷水江市', 3, '431300', '湖南省娄底市冷水江市', '431300');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('431382', '涟源市', 3, '431300', '湖南省娄底市涟源市', '431300'),
       ('433100', '湘西土家族苗族自治州', 2, '430000', '湖南省湘西土家族苗族自治州', '433100'),
       ('433101', '吉首市', 3, '433100', '湖南省湘西土家族苗族自治州吉首市', '433100'),
       ('433122', '泸溪县', 3, '433100', '湖南省湘西土家族苗族自治州泸溪县', '433100'),
       ('433123', '凤凰县', 3, '433100', '湖南省湘西土家族苗族自治州凤凰县', '433100'),
       ('433124', '花垣县', 3, '433100', '湖南省湘西土家族苗族自治州花垣县', '433100'),
       ('433125', '保靖县', 3, '433100', '湖南省湘西土家族苗族自治州保靖县', '433100'),
       ('433126', '古丈县', 3, '433100', '湖南省湘西土家族苗族自治州古丈县', '433100'),
       ('433127', '永顺县', 3, '433100', '湖南省湘西土家族苗族自治州永顺县', '433100'),
       ('433130', '龙山县', 3, '433100', '湖南省湘西土家族苗族自治州龙山县', '433100');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('440000', '广东省', 1, NULL, '广东省', '440000'),
       ('440100', '广州市', 2, '440000', '广东省广州市', '440100'),
       ('440103', '荔湾区', 3, '440100', '广东省广州市荔湾区', '440100'),
       ('440104', '越秀区', 3, '440100', '广东省广州市越秀区', '440100'),
       ('440105', '海珠区', 3, '440100', '广东省广州市海珠区', '440100'),
       ('440106', '天河区', 3, '440100', '广东省广州市天河区', '440100'),
       ('440111', '白云区', 3, '440100', '广东省广州市白云区', '440100'),
       ('440112', '黄埔区', 3, '440100', '广东省广州市黄埔区', '440100'),
       ('440113', '番禺区', 3, '440100', '广东省广州市番禺区', '440100'),
       ('440114', '花都区', 3, '440100', '广东省广州市花都区', '440100');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('440115', '南沙区', 3, '440100', '广东省广州市南沙区', '440100'),
       ('440117', '从化区', 3, '440100', '广东省广州市从化区', '440100'),
       ('440118', '增城区', 3, '440100', '广东省广州市增城区', '440100'),
       ('440200', '韶关市', 2, '440000', '广东省韶关市', '440200'),
       ('440203', '武江区', 3, '440200', '广东省韶关市武江区', '440200'),
       ('440204', '浈江区', 3, '440200', '广东省韶关市浈江区', '440200'),
       ('440205', '曲江区', 3, '440200', '广东省韶关市曲江区', '440200'),
       ('440222', '始兴县', 3, '440200', '广东省韶关市始兴县', '440200'),
       ('440224', '仁化县', 3, '440200', '广东省韶关市仁化县', '440200'),
       ('440229', '翁源县', 3, '440200', '广东省韶关市翁源县', '440200');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('440232', '乳源瑶族自治县', 3, '440200', '广东省韶关市乳源瑶族自治县', '440200'),
       ('440233', '新丰县', 3, '440200', '广东省韶关市新丰县', '440200'),
       ('440281', '乐昌市', 3, '440200', '广东省韶关市乐昌市', '440200'),
       ('440282', '南雄市', 3, '440200', '广东省韶关市南雄市', '440200'),
       ('440300', '深圳市', 2, '440000', '广东省深圳市', '440300'),
       ('440303', '罗湖区', 3, '440300', '广东省深圳市罗湖区', '440300'),
       ('440304', '福田区', 3, '440300', '广东省深圳市福田区', '440300'),
       ('440305', '南山区', 3, '440300', '广东省深圳市南山区', '440300'),
       ('440306', '宝安区', 3, '440300', '广东省深圳市宝安区', '440300'),
       ('440307', '龙岗区', 3, '440300', '广东省深圳市龙岗区', '440300');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('440308', '盐田区', 3, '440300', '广东省深圳市盐田区', '440300'),
       ('440309', '龙华区', 3, '440300', '广东省深圳市龙华区', '440300'),
       ('440310', '坪山区', 3, '440300', '广东省深圳市坪山区', '440300'),
       ('440311', '光明区', 3, '440300', '广东省深圳市光明区', '440300'),
       ('440400', '珠海市', 2, '440000', '广东省珠海市', '440400'),
       ('440402', '香洲区', 3, '440400', '广东省珠海市香洲区', '440400'),
       ('440403', '斗门区', 3, '440400', '广东省珠海市斗门区', '440400'),
       ('440404', '金湾区', 3, '440400', '广东省珠海市金湾区', '440400'),
       ('440500', '汕头市', 2, '440000', '广东省汕头市', '440500'),
       ('440507', '龙湖区', 3, '440500', '广东省汕头市龙湖区', '440500');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('440511', '金平区', 3, '440500', '广东省汕头市金平区', '440500'),
       ('440512', '濠江区', 3, '440500', '广东省汕头市濠江区', '440500'),
       ('440513', '潮阳区', 3, '440500', '广东省汕头市潮阳区', '440500'),
       ('440514', '潮南区', 3, '440500', '广东省汕头市潮南区', '440500'),
       ('440515', '澄海区', 3, '440500', '广东省汕头市澄海区', '440500'),
       ('440523', '南澳县', 3, '440500', '广东省汕头市南澳县', '440500'),
       ('440600', '佛山市', 2, '440000', '广东省佛山市', '440600'),
       ('440604', '禅城区', 3, '440600', '广东省佛山市禅城区', '440600'),
       ('440605', '南海区', 3, '440600', '广东省佛山市南海区', '440600'),
       ('440606', '顺德区', 3, '440600', '广东省佛山市顺德区', '440600');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('440607', '三水区', 3, '440600', '广东省佛山市三水区', '440600'),
       ('440608', '高明区', 3, '440600', '广东省佛山市高明区', '440600'),
       ('440700', '江门市', 2, '440000', '广东省江门市', '440700'),
       ('440703', '蓬江区', 3, '440700', '广东省江门市蓬江区', '440700'),
       ('440704', '江海区', 3, '440700', '广东省江门市江海区', '440700'),
       ('440705', '新会区', 3, '440700', '广东省江门市新会区', '440700'),
       ('440781', '台山市', 3, '440700', '广东省江门市台山市', '440700'),
       ('440783', '开平市', 3, '440700', '广东省江门市开平市', '440700'),
       ('440784', '鹤山市', 3, '440700', '广东省江门市鹤山市', '440700'),
       ('440785', '恩平市', 3, '440700', '广东省江门市恩平市', '440700');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('440800', '湛江市', 2, '440000', '广东省湛江市', '440800'),
       ('440802', '赤坎区', 3, '440800', '广东省湛江市赤坎区', '440800'),
       ('440803', '霞山区', 3, '440800', '广东省湛江市霞山区', '440800'),
       ('440804', '坡头区', 3, '440800', '广东省湛江市坡头区', '440800'),
       ('440811', '麻章区', 3, '440800', '广东省湛江市麻章区', '440800'),
       ('440823', '遂溪县', 3, '440800', '广东省湛江市遂溪县', '440800'),
       ('440825', '徐闻县', 3, '440800', '广东省湛江市徐闻县', '440800'),
       ('440881', '廉江市', 3, '440800', '广东省湛江市廉江市', '440800'),
       ('440882', '雷州市', 3, '440800', '广东省湛江市雷州市', '440800'),
       ('440883', '吴川市', 3, '440800', '广东省湛江市吴川市', '440800');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('440900', '茂名市', 2, '440000', '广东省茂名市', '440900'),
       ('440902', '茂南区', 3, '440900', '广东省茂名市茂南区', '440900'),
       ('440904', '电白区', 3, '440900', '广东省茂名市电白区', '440900'),
       ('440981', '高州市', 3, '440900', '广东省茂名市高州市', '440900'),
       ('440982', '化州市', 3, '440900', '广东省茂名市化州市', '440900'),
       ('440983', '信宜市', 3, '440900', '广东省茂名市信宜市', '440900'),
       ('441200', '肇庆市', 2, '440000', '广东省肇庆市', '441200'),
       ('441202', '端州区', 3, '441200', '广东省肇庆市端州区', '441200'),
       ('441203', '鼎湖区', 3, '441200', '广东省肇庆市鼎湖区', '441200'),
       ('441204', '高要区', 3, '441200', '广东省肇庆市高要区', '441200');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('441223', '广宁县', 3, '441200', '广东省肇庆市广宁县', '441200'),
       ('441224', '怀集县', 3, '441200', '广东省肇庆市怀集县', '441200'),
       ('441225', '封开县', 3, '441200', '广东省肇庆市封开县', '441200'),
       ('441226', '德庆县', 3, '441200', '广东省肇庆市德庆县', '441200'),
       ('441284', '四会市', 3, '441200', '广东省肇庆市四会市', '441200'),
       ('441300', '惠州市', 2, '440000', '广东省惠州市', '441300'),
       ('441302', '惠城区', 3, '441300', '广东省惠州市惠城区', '441300'),
       ('441303', '惠阳区', 3, '441300', '广东省惠州市惠阳区', '441300'),
       ('441322', '博罗县', 3, '441300', '广东省惠州市博罗县', '441300'),
       ('441323', '惠东县', 3, '441300', '广东省惠州市惠东县', '441300');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('441324', '龙门县', 3, '441300', '广东省惠州市龙门县', '441300'),
       ('441400', '梅州市', 2, '440000', '广东省梅州市', '441400'),
       ('441402', '梅江区', 3, '441400', '广东省梅州市梅江区', '441400'),
       ('441403', '梅县区', 3, '441400', '广东省梅州市梅县区', '441400'),
       ('441422', '大埔县', 3, '441400', '广东省梅州市大埔县', '441400'),
       ('441423', '丰顺县', 3, '441400', '广东省梅州市丰顺县', '441400'),
       ('441424', '五华县', 3, '441400', '广东省梅州市五华县', '441400'),
       ('441426', '平远县', 3, '441400', '广东省梅州市平远县', '441400'),
       ('441427', '蕉岭县', 3, '441400', '广东省梅州市蕉岭县', '441400'),
       ('441481', '兴宁市', 3, '441400', '广东省梅州市兴宁市', '441400');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('441500', '汕尾市', 2, '440000', '广东省汕尾市', '441500'),
       ('441502', '城区', 3, '441500', '广东省汕尾市城区', '441500'),
       ('441521', '海丰县', 3, '441500', '广东省汕尾市海丰县', '441500'),
       ('441523', '陆河县', 3, '441500', '广东省汕尾市陆河县', '441500'),
       ('441581', '陆丰市', 3, '441500', '广东省汕尾市陆丰市', '441500'),
       ('441600', '河源市', 2, '440000', '广东省河源市', '441600'),
       ('441602', '源城区', 3, '441600', '广东省河源市源城区', '441600'),
       ('441621', '紫金县', 3, '441600', '广东省河源市紫金县', '441600'),
       ('441622', '龙川县', 3, '441600', '广东省河源市龙川县', '441600'),
       ('441623', '连平县', 3, '441600', '广东省河源市连平县', '441600');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('441624', '和平县', 3, '441600', '广东省河源市和平县', '441600'),
       ('441625', '东源县', 3, '441600', '广东省河源市东源县', '441600'),
       ('441700', '阳江市', 2, '440000', '广东省阳江市', '441700'),
       ('441702', '江城区', 3, '441700', '广东省阳江市江城区', '441700'),
       ('441704', '阳东区', 3, '441700', '广东省阳江市阳东区', '441700'),
       ('441721', '阳西县', 3, '441700', '广东省阳江市阳西县', '441700'),
       ('441781', '阳春市', 3, '441700', '广东省阳江市阳春市', '441700'),
       ('441800', '清远市', 2, '440000', '广东省清远市', '441800'),
       ('441802', '清城区', 3, '441800', '广东省清远市清城区', '441800'),
       ('441803', '清新区', 3, '441800', '广东省清远市清新区', '441800');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('441821', '佛冈县', 3, '441800', '广东省清远市佛冈县', '441800'),
       ('441823', '阳山县', 3, '441800', '广东省清远市阳山县', '441800'),
       ('441825', '连山壮族瑶族自治县', 3, '441800', '广东省清远市连山壮族瑶族自治县', '441800'),
       ('441826', '连南瑶族自治县', 3, '441800', '广东省清远市连南瑶族自治县', '441800'),
       ('441881', '英德市', 3, '441800', '广东省清远市英德市', '441800'),
       ('441882', '连州市', 3, '441800', '广东省清远市连州市', '441800'),
       ('441900', '东莞市', 2, '440000', '广东省东莞市', '441900'),
       ('442000', '中山市', 2, '440000', '广东省中山市', '442000'),
       ('445100', '潮州市', 2, '440000', '广东省潮州市', '445100'),
       ('445102', '湘桥区', 3, '445100', '广东省潮州市湘桥区', '445100');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('445103', '潮安区', 3, '445100', '广东省潮州市潮安区', '445100'),
       ('445122', '饶平县', 3, '445100', '广东省潮州市饶平县', '445100'),
       ('445200', '揭阳市', 2, '440000', '广东省揭阳市', '445200'),
       ('445202', '榕城区', 3, '445200', '广东省揭阳市榕城区', '445200'),
       ('445203', '揭东区', 3, '445200', '广东省揭阳市揭东区', '445200'),
       ('445222', '揭西县', 3, '445200', '广东省揭阳市揭西县', '445200'),
       ('445224', '惠来县', 3, '445200', '广东省揭阳市惠来县', '445200'),
       ('445281', '普宁市', 3, '445200', '广东省揭阳市普宁市', '445200'),
       ('445300', '云浮市', 2, '440000', '广东省云浮市', '445300'),
       ('445302', '云城区', 3, '445300', '广东省云浮市云城区', '445300');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('445303', '云安区', 3, '445300', '广东省云浮市云安区', '445300'),
       ('445321', '新兴县', 3, '445300', '广东省云浮市新兴县', '445300'),
       ('445322', '郁南县', 3, '445300', '广东省云浮市郁南县', '445300'),
       ('445381', '罗定市', 3, '445300', '广东省云浮市罗定市', '445300'),
       ('450000', '广西壮族自治区', 1, NULL, '广西壮族自治区', '450000'),
       ('450100', '南宁市', 2, '450000', '广西壮族自治区南宁市', '450100'),
       ('450102', '兴宁区', 3, '450100', '广西壮族自治区南宁市兴宁区', '450100'),
       ('450103', '青秀区', 3, '450100', '广西壮族自治区南宁市青秀区', '450100'),
       ('450105', '江南区', 3, '450100', '广西壮族自治区南宁市江南区', '450100'),
       ('450107', '西乡塘区', 3, '450100', '广西壮族自治区南宁市西乡塘区', '450100');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('450108', '良庆区', 3, '450100', '广西壮族自治区南宁市良庆区', '450100'),
       ('450109', '邕宁区', 3, '450100', '广西壮族自治区南宁市邕宁区', '450100'),
       ('450110', '武鸣区', 3, '450100', '广西壮族自治区南宁市武鸣区', '450100'),
       ('450123', '隆安县', 3, '450100', '广西壮族自治区南宁市隆安县', '450100'),
       ('450124', '马山县', 3, '450100', '广西壮族自治区南宁市马山县', '450100'),
       ('450125', '上林县', 3, '450100', '广西壮族自治区南宁市上林县', '450100'),
       ('450126', '宾阳县', 3, '450100', '广西壮族自治区南宁市宾阳县', '450100'),
       ('450127', '横州市', 3, '450100', '广西壮族自治区南宁市横州市', '450100'),
       ('450200', '柳州市', 2, '450000', '广西壮族自治区柳州市', '450200'),
       ('450202', '城中区', 3, '450200', '广西壮族自治区柳州市城中区', '450200');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('450203', '鱼峰区', 3, '450200', '广西壮族自治区柳州市鱼峰区', '450200'),
       ('450204', '柳南区', 3, '450200', '广西壮族自治区柳州市柳南区', '450200'),
       ('450205', '柳北区', 3, '450200', '广西壮族自治区柳州市柳北区', '450200'),
       ('450206', '柳江区', 3, '450200', '广西壮族自治区柳州市柳江区', '450200'),
       ('450222', '柳城县', 3, '450200', '广西壮族自治区柳州市柳城县', '450200'),
       ('450223', '鹿寨县', 3, '450200', '广西壮族自治区柳州市鹿寨县', '450200'),
       ('450224', '融安县', 3, '450200', '广西壮族自治区柳州市融安县', '450200'),
       ('450225', '融水苗族自治县', 3, '450200', '广西壮族自治区柳州市融水苗族自治县', '450200'),
       ('450226', '三江侗族自治县', 3, '450200', '广西壮族自治区柳州市三江侗族自治县', '450200'),
       ('450300', '桂林市', 2, '450000', '广西壮族自治区桂林市', '450300');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('450302', '秀峰区', 3, '450300', '广西壮族自治区桂林市秀峰区', '450300'),
       ('450303', '叠彩区', 3, '450300', '广西壮族自治区桂林市叠彩区', '450300'),
       ('450304', '象山区', 3, '450300', '广西壮族自治区桂林市象山区', '450300'),
       ('450305', '七星区', 3, '450300', '广西壮族自治区桂林市七星区', '450300'),
       ('450311', '雁山区', 3, '450300', '广西壮族自治区桂林市雁山区', '450300'),
       ('450312', '临桂区', 3, '450300', '广西壮族自治区桂林市临桂区', '450300'),
       ('450321', '阳朔县', 3, '450300', '广西壮族自治区桂林市阳朔县', '450300'),
       ('450323', '灵川县', 3, '450300', '广西壮族自治区桂林市灵川县', '450300'),
       ('450324', '全州县', 3, '450300', '广西壮族自治区桂林市全州县', '450300'),
       ('450325', '兴安县', 3, '450300', '广西壮族自治区桂林市兴安县', '450300');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('450326', '永福县', 3, '450300', '广西壮族自治区桂林市永福县', '450300'),
       ('450327', '灌阳县', 3, '450300', '广西壮族自治区桂林市灌阳县', '450300'),
       ('450328', '龙胜各族自治县', 3, '450300', '广西壮族自治区桂林市龙胜各族自治县', '450300'),
       ('450329', '资源县', 3, '450300', '广西壮族自治区桂林市资源县', '450300'),
       ('450330', '平乐县', 3, '450300', '广西壮族自治区桂林市平乐县', '450300'),
       ('450332', '恭城瑶族自治县', 3, '450300', '广西壮族自治区桂林市恭城瑶族自治县', '450300'),
       ('450381', '荔浦市', 3, '450300', '广西壮族自治区桂林市荔浦市', '450300'),
       ('450400', '梧州市', 2, '450000', '广西壮族自治区梧州市', '450400'),
       ('450403', '万秀区', 3, '450400', '广西壮族自治区梧州市万秀区', '450400'),
       ('450405', '长洲区', 3, '450400', '广西壮族自治区梧州市长洲区', '450400');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('450406', '龙圩区', 3, '450400', '广西壮族自治区梧州市龙圩区', '450400'),
       ('450421', '苍梧县', 3, '450400', '广西壮族自治区梧州市苍梧县', '450400'),
       ('450422', '藤县', 3, '450400', '广西壮族自治区梧州市藤县', '450400'),
       ('450423', '蒙山县', 3, '450400', '广西壮族自治区梧州市蒙山县', '450400'),
       ('450481', '岑溪市', 3, '450400', '广西壮族自治区梧州市岑溪市', '450400'),
       ('450500', '北海市', 2, '450000', '广西壮族自治区北海市', '450500'),
       ('450502', '海城区', 3, '450500', '广西壮族自治区北海市海城区', '450500'),
       ('450503', '银海区', 3, '450500', '广西壮族自治区北海市银海区', '450500'),
       ('450512', '铁山港区', 3, '450500', '广西壮族自治区北海市铁山港区', '450500'),
       ('450521', '合浦县', 3, '450500', '广西壮族自治区北海市合浦县', '450500');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('450600', '防城港市', 2, '450000', '广西壮族自治区防城港市', '450600'),
       ('450602', '港口区', 3, '450600', '广西壮族自治区防城港市港口区', '450600'),
       ('450603', '防城区', 3, '450600', '广西壮族自治区防城港市防城区', '450600'),
       ('450621', '上思县', 3, '450600', '广西壮族自治区防城港市上思县', '450600'),
       ('450681', '东兴市', 3, '450600', '广西壮族自治区防城港市东兴市', '450600'),
       ('450700', '钦州市', 2, '450000', '广西壮族自治区钦州市', '450700'),
       ('450702', '钦南区', 3, '450700', '广西壮族自治区钦州市钦南区', '450700'),
       ('450703', '钦北区', 3, '450700', '广西壮族自治区钦州市钦北区', '450700'),
       ('450721', '灵山县', 3, '450700', '广西壮族自治区钦州市灵山县', '450700'),
       ('450722', '浦北县', 3, '450700', '广西壮族自治区钦州市浦北县', '450700');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('450800', '贵港市', 2, '450000', '广西壮族自治区贵港市', '450800'),
       ('450802', '港北区', 3, '450800', '广西壮族自治区贵港市港北区', '450800'),
       ('450803', '港南区', 3, '450800', '广西壮族自治区贵港市港南区', '450800'),
       ('450804', '覃塘区', 3, '450800', '广西壮族自治区贵港市覃塘区', '450800'),
       ('450821', '平南县', 3, '450800', '广西壮族自治区贵港市平南县', '450800'),
       ('450881', '桂平市', 3, '450800', '广西壮族自治区贵港市桂平市', '450800'),
       ('450900', '玉林市', 2, '450000', '广西壮族自治区玉林市', '450900'),
       ('450902', '玉州区', 3, '450900', '广西壮族自治区玉林市玉州区', '450900'),
       ('450903', '福绵区', 3, '450900', '广西壮族自治区玉林市福绵区', '450900'),
       ('450921', '容县', 3, '450900', '广西壮族自治区玉林市容县', '450900');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('450922', '陆川县', 3, '450900', '广西壮族自治区玉林市陆川县', '450900'),
       ('450923', '博白县', 3, '450900', '广西壮族自治区玉林市博白县', '450900'),
       ('450924', '兴业县', 3, '450900', '广西壮族自治区玉林市兴业县', '450900'),
       ('450981', '北流市', 3, '450900', '广西壮族自治区玉林市北流市', '450900'),
       ('451000', '百色市', 2, '450000', '广西壮族自治区百色市', '451000'),
       ('451002', '右江区', 3, '451000', '广西壮族自治区百色市右江区', '451000'),
       ('451003', '田阳区', 3, '451000', '广西壮族自治区百色市田阳区', '451000'),
       ('451022', '田东县', 3, '451000', '广西壮族自治区百色市田东县', '451000'),
       ('451024', '德保县', 3, '451000', '广西壮族自治区百色市德保县', '451000'),
       ('451026', '那坡县', 3, '451000', '广西壮族自治区百色市那坡县', '451000');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('451027', '凌云县', 3, '451000', '广西壮族自治区百色市凌云县', '451000'),
       ('451028', '乐业县', 3, '451000', '广西壮族自治区百色市乐业县', '451000'),
       ('451029', '田林县', 3, '451000', '广西壮族自治区百色市田林县', '451000'),
       ('451030', '西林县', 3, '451000', '广西壮族自治区百色市西林县', '451000'),
       ('451031', '隆林各族自治县', 3, '451000', '广西壮族自治区百色市隆林各族自治县', '451000'),
       ('451081', '靖西市', 3, '451000', '广西壮族自治区百色市靖西市', '451000'),
       ('451082', '平果市', 3, '451000', '广西壮族自治区百色市平果市', '451000'),
       ('451100', '贺州市', 2, '450000', '广西壮族自治区贺州市', '451100'),
       ('451102', '八步区', 3, '451100', '广西壮族自治区贺州市八步区', '451100'),
       ('451103', '平桂区', 3, '451100', '广西壮族自治区贺州市平桂区', '451100');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('451121', '昭平县', 3, '451100', '广西壮族自治区贺州市昭平县', '451100'),
       ('451122', '钟山县', 3, '451100', '广西壮族自治区贺州市钟山县', '451100'),
       ('451123', '富川瑶族自治县', 3, '451100', '广西壮族自治区贺州市富川瑶族自治县', '451100'),
       ('451200', '河池市', 2, '450000', '广西壮族自治区河池市', '451200'),
       ('451202', '金城江区', 3, '451200', '广西壮族自治区河池市金城江区', '451200'),
       ('451203', '宜州区', 3, '451200', '广西壮族自治区河池市宜州区', '451200'),
       ('451221', '南丹县', 3, '451200', '广西壮族自治区河池市南丹县', '451200'),
       ('451222', '天峨县', 3, '451200', '广西壮族自治区河池市天峨县', '451200'),
       ('451223', '凤山县', 3, '451200', '广西壮族自治区河池市凤山县', '451200'),
       ('451224', '东兰县', 3, '451200', '广西壮族自治区河池市东兰县', '451200');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('451225', '罗城仫佬族自治县', 3, '451200', '广西壮族自治区河池市罗城仫佬族自治县', '451200'),
       ('451226', '环江毛南族自治县', 3, '451200', '广西壮族自治区河池市环江毛南族自治县', '451200'),
       ('451227', '巴马瑶族自治县', 3, '451200', '广西壮族自治区河池市巴马瑶族自治县', '451200'),
       ('451228', '都安瑶族自治县', 3, '451200', '广西壮族自治区河池市都安瑶族自治县', '451200'),
       ('451229', '大化瑶族自治县', 3, '451200', '广西壮族自治区河池市大化瑶族自治县', '451200'),
       ('451300', '来宾市', 2, '450000', '广西壮族自治区来宾市', '451300'),
       ('451302', '兴宾区', 3, '451300', '广西壮族自治区来宾市兴宾区', '451300'),
       ('451321', '忻城县', 3, '451300', '广西壮族自治区来宾市忻城县', '451300'),
       ('451322', '象州县', 3, '451300', '广西壮族自治区来宾市象州县', '451300'),
       ('451323', '武宣县', 3, '451300', '广西壮族自治区来宾市武宣县', '451300');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('451324', '金秀瑶族自治县', 3, '451300', '广西壮族自治区来宾市金秀瑶族自治县', '451300'),
       ('451381', '合山市', 3, '451300', '广西壮族自治区来宾市合山市', '451300'),
       ('451400', '崇左市', 2, '450000', '广西壮族自治区崇左市', '451400'),
       ('451402', '江州区', 3, '451400', '广西壮族自治区崇左市江州区', '451400'),
       ('451421', '扶绥县', 3, '451400', '广西壮族自治区崇左市扶绥县', '451400'),
       ('451422', '宁明县', 3, '451400', '广西壮族自治区崇左市宁明县', '451400'),
       ('451423', '龙州县', 3, '451400', '广西壮族自治区崇左市龙州县', '451400'),
       ('451424', '大新县', 3, '451400', '广西壮族自治区崇左市大新县', '451400'),
       ('451425', '天等县', 3, '451400', '广西壮族自治区崇左市天等县', '451400'),
       ('451481', '凭祥市', 3, '451400', '广西壮族自治区崇左市凭祥市', '451400');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('460000', '海南省', 1, NULL, '海南省', '460000'),
       ('460100', '海口市', 2, '460000', '海南省海口市', '460100'),
       ('460105', '秀英区', 3, '460100', '海南省海口市秀英区', '460100'),
       ('460106', '龙华区', 3, '460100', '海南省海口市龙华区', '460100'),
       ('460107', '琼山区', 3, '460100', '海南省海口市琼山区', '460100'),
       ('460108', '美兰区', 3, '460100', '海南省海口市美兰区', '460100'),
       ('460200', '三亚市', 2, '460000', '海南省三亚市', '460200'),
       ('460202', '海棠区', 3, '460200', '海南省三亚市海棠区', '460200'),
       ('460203', '吉阳区', 3, '460200', '海南省三亚市吉阳区', '460200'),
       ('460204', '天涯区', 3, '460200', '海南省三亚市天涯区', '460200');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('460205', '崖州区', 3, '460200', '海南省三亚市崖州区', '460200'),
       ('460300', '三沙市', 2, '460000', '海南省三沙市', '460300'),
       ('460301', '西沙区', 3, '460300', '海南省三沙市西沙区', '460300'),
       ('460302', '南沙区', 3, '460300', '海南省三沙市南沙区', '460300'),
       ('460400', '儋州市', 2, '460000', '海南省儋州市', '460400'),
       ('469001', '五指山市', 2, '460000', '海南省五指山市', '469001'),
       ('469002', '琼海市', 2, '460000', '海南省琼海市', '469002'),
       ('469005', '文昌市', 2, '460000', '海南省文昌市', '469005'),
       ('469006', '万宁市', 2, '460000', '海南省万宁市', '469006'),
       ('469007', '东方市', 2, '460000', '海南省东方市', '469007');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('469021', '定安县', 2, '460000', '海南省定安县', '469021'),
       ('469022', '屯昌县', 2, '460000', '海南省屯昌县', '469022'),
       ('469023', '澄迈县', 2, '460000', '海南省澄迈县', '469023'),
       ('469024', '临高县', 2, '460000', '海南省临高县', '469024'),
       ('469025', '白沙黎族自治县', 2, '460000', '海南省白沙黎族自治县', '469025'),
       ('469026', '昌江黎族自治县', 2, '460000', '海南省昌江黎族自治县', '469026'),
       ('469027', '乐东黎族自治县', 2, '460000', '海南省乐东黎族自治县', '469027'),
       ('469028', '陵水黎族自治县', 2, '460000', '海南省陵水黎族自治县', '469028'),
       ('469029', '保亭黎族苗族自治县', 2, '460000', '海南省保亭黎族苗族自治县', '469029'),
       ('469030', '琼中黎族苗族自治县', 2, '460000', '海南省琼中黎族苗族自治县', '469030');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('500000', '重庆市', 1, NULL, '重庆市', '500000'),
       ('500101', '万州区', 2, '500000', '重庆市万州区', '500101'),
       ('500102', '涪陵区', 2, '500000', '重庆市涪陵区', '500102'),
       ('500103', '渝中区', 2, '500000', '重庆市渝中区', '500103'),
       ('500104', '大渡口区', 2, '500000', '重庆市大渡口区', '500104'),
       ('500105', '江北区', 2, '500000', '重庆市江北区', '500105'),
       ('500106', '沙坪坝区', 2, '500000', '重庆市沙坪坝区', '500106'),
       ('500107', '九龙坡区', 2, '500000', '重庆市九龙坡区', '500107'),
       ('500108', '南岸区', 2, '500000', '重庆市南岸区', '500108'),
       ('500109', '北碚区', 2, '500000', '重庆市北碚区', '500109');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('500110', '綦江区', 2, '500000', '重庆市綦江区', '500110'),
       ('500111', '大足区', 2, '500000', '重庆市大足区', '500111'),
       ('500112', '渝北区', 2, '500000', '重庆市渝北区', '500112'),
       ('500113', '巴南区', 2, '500000', '重庆市巴南区', '500113'),
       ('500114', '黔江区', 2, '500000', '重庆市黔江区', '500114'),
       ('500115', '长寿区', 2, '500000', '重庆市长寿区', '500115'),
       ('500116', '江津区', 2, '500000', '重庆市江津区', '500116'),
       ('500117', '合川区', 2, '500000', '重庆市合川区', '500117'),
       ('500118', '永川区', 2, '500000', '重庆市永川区', '500118'),
       ('500119', '南川区', 2, '500000', '重庆市南川区', '500119');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('500120', '璧山区', 2, '500000', '重庆市璧山区', '500120'),
       ('500151', '铜梁区', 2, '500000', '重庆市铜梁区', '500151'),
       ('500152', '潼南区', 2, '500000', '重庆市潼南区', '500152'),
       ('500153', '荣昌区', 2, '500000', '重庆市荣昌区', '500153'),
       ('500154', '开州区', 2, '500000', '重庆市开州区', '500154'),
       ('500155', '梁平区', 2, '500000', '重庆市梁平区', '500155'),
       ('500156', '武隆区', 2, '500000', '重庆市武隆区', '500156'),
       ('500229', '城口县', 2, '500000', '重庆市城口县', '500229'),
       ('500230', '丰都县', 2, '500000', '重庆市丰都县', '500230'),
       ('500231', '垫江县', 2, '500000', '重庆市垫江县', '500231');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('500233', '忠县', 2, '500000', '重庆市忠县', '500233'),
       ('500235', '云阳县', 2, '500000', '重庆市云阳县', '500235'),
       ('500236', '奉节县', 2, '500000', '重庆市奉节县', '500236'),
       ('500237', '巫山县', 2, '500000', '重庆市巫山县', '500237'),
       ('500238', '巫溪县', 2, '500000', '重庆市巫溪县', '500238'),
       ('500240', '石柱土家族自治县', 2, '500000', '重庆市石柱土家族自治县', '500240'),
       ('500241', '秀山土家族苗族自治县', 2, '500000', '重庆市秀山土家族苗族自治县', '500241'),
       ('500242', '酉阳土家族苗族自治县', 2, '500000', '重庆市酉阳土家族苗族自治县', '500242'),
       ('500243', '彭水苗族土家族自治县', 2, '500000', '重庆市彭水苗族土家族自治县', '500243'),
       ('510000', '四川省', 1, NULL, '四川省', '510000');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('510100', '成都市', 2, '510000', '四川省成都市', '510100'),
       ('510104', '锦江区', 3, '510100', '四川省成都市锦江区', '510100'),
       ('510105', '青羊区', 3, '510100', '四川省成都市青羊区', '510100'),
       ('510106', '金牛区', 3, '510100', '四川省成都市金牛区', '510100'),
       ('510107', '武侯区', 3, '510100', '四川省成都市武侯区', '510100'),
       ('510108', '成华区', 3, '510100', '四川省成都市成华区', '510100'),
       ('510112', '龙泉驿区', 3, '510100', '四川省成都市龙泉驿区', '510100'),
       ('510113', '青白江区', 3, '510100', '四川省成都市青白江区', '510100'),
       ('510114', '新都区', 3, '510100', '四川省成都市新都区', '510100'),
       ('510115', '温江区', 3, '510100', '四川省成都市温江区', '510100');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('510116', '双流区', 3, '510100', '四川省成都市双流区', '510100'),
       ('510117', '郫都区', 3, '510100', '四川省成都市郫都区', '510100'),
       ('510118', '新津区', 3, '510100', '四川省成都市新津区', '510100'),
       ('510121', '金堂县', 3, '510100', '四川省成都市金堂县', '510100'),
       ('510129', '大邑县', 3, '510100', '四川省成都市大邑县', '510100'),
       ('510131', '蒲江县', 3, '510100', '四川省成都市蒲江县', '510100'),
       ('510181', '都江堰市', 3, '510100', '四川省成都市都江堰市', '510100'),
       ('510182', '彭州市', 3, '510100', '四川省成都市彭州市', '510100'),
       ('510183', '邛崃市', 3, '510100', '四川省成都市邛崃市', '510100'),
       ('510184', '崇州市', 3, '510100', '四川省成都市崇州市', '510100');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('510185', '简阳市', 3, '510100', '四川省成都市简阳市', '510100'),
       ('510300', '自贡市', 2, '510000', '四川省自贡市', '510300'),
       ('510302', '自流井区', 3, '510300', '四川省自贡市自流井区', '510300'),
       ('510303', '贡井区', 3, '510300', '四川省自贡市贡井区', '510300'),
       ('510304', '大安区', 3, '510300', '四川省自贡市大安区', '510300'),
       ('510311', '沿滩区', 3, '510300', '四川省自贡市沿滩区', '510300'),
       ('510321', '荣县', 3, '510300', '四川省自贡市荣县', '510300'),
       ('510322', '富顺县', 3, '510300', '四川省自贡市富顺县', '510300'),
       ('510400', '攀枝花市', 2, '510000', '四川省攀枝花市', '510400'),
       ('510402', '东区', 3, '510400', '四川省攀枝花市东区', '510400');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('510403', '西区', 3, '510400', '四川省攀枝花市西区', '510400'),
       ('510411', '仁和区', 3, '510400', '四川省攀枝花市仁和区', '510400'),
       ('510421', '米易县', 3, '510400', '四川省攀枝花市米易县', '510400'),
       ('510422', '盐边县', 3, '510400', '四川省攀枝花市盐边县', '510400'),
       ('510500', '泸州市', 2, '510000', '四川省泸州市', '510500'),
       ('510502', '江阳区', 3, '510500', '四川省泸州市江阳区', '510500'),
       ('510503', '纳溪区', 3, '510500', '四川省泸州市纳溪区', '510500'),
       ('510504', '龙马潭区', 3, '510500', '四川省泸州市龙马潭区', '510500'),
       ('510521', '泸县', 3, '510500', '四川省泸州市泸县', '510500'),
       ('510522', '合江县', 3, '510500', '四川省泸州市合江县', '510500');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('510524', '叙永县', 3, '510500', '四川省泸州市叙永县', '510500'),
       ('510525', '古蔺县', 3, '510500', '四川省泸州市古蔺县', '510500'),
       ('510600', '德阳市', 2, '510000', '四川省德阳市', '510600'),
       ('510603', '旌阳区', 3, '510600', '四川省德阳市旌阳区', '510600'),
       ('510604', '罗江区', 3, '510600', '四川省德阳市罗江区', '510600'),
       ('510623', '中江县', 3, '510600', '四川省德阳市中江县', '510600'),
       ('510681', '广汉市', 3, '510600', '四川省德阳市广汉市', '510600'),
       ('510682', '什邡市', 3, '510600', '四川省德阳市什邡市', '510600'),
       ('510683', '绵竹市', 3, '510600', '四川省德阳市绵竹市', '510600'),
       ('510700', '绵阳市', 2, '510000', '四川省绵阳市', '510700');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('510703', '涪城区', 3, '510700', '四川省绵阳市涪城区', '510700'),
       ('510704', '游仙区', 3, '510700', '四川省绵阳市游仙区', '510700'),
       ('510705', '安州区', 3, '510700', '四川省绵阳市安州区', '510700'),
       ('510722', '三台县', 3, '510700', '四川省绵阳市三台县', '510700'),
       ('510723', '盐亭县', 3, '510700', '四川省绵阳市盐亭县', '510700'),
       ('510725', '梓潼县', 3, '510700', '四川省绵阳市梓潼县', '510700'),
       ('510726', '北川羌族自治县', 3, '510700', '四川省绵阳市北川羌族自治县', '510700'),
       ('510727', '平武县', 3, '510700', '四川省绵阳市平武县', '510700'),
       ('510781', '江油市', 3, '510700', '四川省绵阳市江油市', '510700'),
       ('510800', '广元市', 2, '510000', '四川省广元市', '510800');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('510802', '利州区', 3, '510800', '四川省广元市利州区', '510800'),
       ('510811', '昭化区', 3, '510800', '四川省广元市昭化区', '510800'),
       ('510812', '朝天区', 3, '510800', '四川省广元市朝天区', '510800'),
       ('510821', '旺苍县', 3, '510800', '四川省广元市旺苍县', '510800'),
       ('510822', '青川县', 3, '510800', '四川省广元市青川县', '510800'),
       ('510823', '剑阁县', 3, '510800', '四川省广元市剑阁县', '510800'),
       ('510824', '苍溪县', 3, '510800', '四川省广元市苍溪县', '510800'),
       ('510900', '遂宁市', 2, '510000', '四川省遂宁市', '510900'),
       ('510903', '船山区', 3, '510900', '四川省遂宁市船山区', '510900'),
       ('510904', '安居区', 3, '510900', '四川省遂宁市安居区', '510900');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('510921', '蓬溪县', 3, '510900', '四川省遂宁市蓬溪县', '510900'),
       ('510923', '大英县', 3, '510900', '四川省遂宁市大英县', '510900'),
       ('510981', '射洪市', 3, '510900', '四川省遂宁市射洪市', '510900'),
       ('511000', '内江市', 2, '510000', '四川省内江市', '511000'),
       ('511002', '市中区', 3, '511000', '四川省内江市市中区', '511000'),
       ('511011', '东兴区', 3, '511000', '四川省内江市东兴区', '511000'),
       ('511024', '威远县', 3, '511000', '四川省内江市威远县', '511000'),
       ('511025', '资中县', 3, '511000', '四川省内江市资中县', '511000'),
       ('511083', '隆昌市', 3, '511000', '四川省内江市隆昌市', '511000'),
       ('511100', '乐山市', 2, '510000', '四川省乐山市', '511100');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('511102', '市中区', 3, '511100', '四川省乐山市市中区', '511100'),
       ('511111', '沙湾区', 3, '511100', '四川省乐山市沙湾区', '511100'),
       ('511112', '五通桥区', 3, '511100', '四川省乐山市五通桥区', '511100'),
       ('511113', '金口河区', 3, '511100', '四川省乐山市金口河区', '511100'),
       ('511123', '犍为县', 3, '511100', '四川省乐山市犍为县', '511100'),
       ('511124', '井研县', 3, '511100', '四川省乐山市井研县', '511100'),
       ('511126', '夹江县', 3, '511100', '四川省乐山市夹江县', '511100'),
       ('511129', '沐川县', 3, '511100', '四川省乐山市沐川县', '511100'),
       ('511132', '峨边彝族自治县', 3, '511100', '四川省乐山市峨边彝族自治县', '511100'),
       ('511133', '马边彝族自治县', 3, '511100', '四川省乐山市马边彝族自治县', '511100');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('511181', '峨眉山市', 3, '511100', '四川省乐山市峨眉山市', '511100'),
       ('511300', '南充市', 2, '510000', '四川省南充市', '511300'),
       ('511302', '顺庆区', 3, '511300', '四川省南充市顺庆区', '511300'),
       ('511303', '高坪区', 3, '511300', '四川省南充市高坪区', '511300'),
       ('511304', '嘉陵区', 3, '511300', '四川省南充市嘉陵区', '511300'),
       ('511321', '南部县', 3, '511300', '四川省南充市南部县', '511300'),
       ('511322', '营山县', 3, '511300', '四川省南充市营山县', '511300'),
       ('511323', '蓬安县', 3, '511300', '四川省南充市蓬安县', '511300'),
       ('511324', '仪陇县', 3, '511300', '四川省南充市仪陇县', '511300'),
       ('511325', '西充县', 3, '511300', '四川省南充市西充县', '511300');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('511381', '阆中市', 3, '511300', '四川省南充市阆中市', '511300'),
       ('511400', '眉山市', 2, '510000', '四川省眉山市', '511400'),
       ('511402', '东坡区', 3, '511400', '四川省眉山市东坡区', '511400'),
       ('511403', '彭山区', 3, '511400', '四川省眉山市彭山区', '511400'),
       ('511421', '仁寿县', 3, '511400', '四川省眉山市仁寿县', '511400'),
       ('511423', '洪雅县', 3, '511400', '四川省眉山市洪雅县', '511400'),
       ('511424', '丹棱县', 3, '511400', '四川省眉山市丹棱县', '511400'),
       ('511425', '青神县', 3, '511400', '四川省眉山市青神县', '511400'),
       ('511500', '宜宾市', 2, '510000', '四川省宜宾市', '511500'),
       ('511502', '翠屏区', 3, '511500', '四川省宜宾市翠屏区', '511500');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('511503', '南溪区', 3, '511500', '四川省宜宾市南溪区', '511500'),
       ('511504', '叙州区', 3, '511500', '四川省宜宾市叙州区', '511500'),
       ('511523', '江安县', 3, '511500', '四川省宜宾市江安县', '511500'),
       ('511524', '长宁县', 3, '511500', '四川省宜宾市长宁县', '511500'),
       ('511525', '高县', 3, '511500', '四川省宜宾市高县', '511500'),
       ('511526', '珙县', 3, '511500', '四川省宜宾市珙县', '511500'),
       ('511527', '筠连县', 3, '511500', '四川省宜宾市筠连县', '511500'),
       ('511528', '兴文县', 3, '511500', '四川省宜宾市兴文县', '511500'),
       ('511529', '屏山县', 3, '511500', '四川省宜宾市屏山县', '511500'),
       ('511600', '广安市', 2, '510000', '四川省广安市', '511600');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('511602', '广安区', 3, '511600', '四川省广安市广安区', '511600'),
       ('511603', '前锋区', 3, '511600', '四川省广安市前锋区', '511600'),
       ('511621', '岳池县', 3, '511600', '四川省广安市岳池县', '511600'),
       ('511622', '武胜县', 3, '511600', '四川省广安市武胜县', '511600'),
       ('511623', '邻水县', 3, '511600', '四川省广安市邻水县', '511600'),
       ('511681', '华蓥市', 3, '511600', '四川省广安市华蓥市', '511600'),
       ('511700', '达州市', 2, '510000', '四川省达州市', '511700'),
       ('511702', '通川区', 3, '511700', '四川省达州市通川区', '511700'),
       ('511703', '达川区', 3, '511700', '四川省达州市达川区', '511700'),
       ('511722', '宣汉县', 3, '511700', '四川省达州市宣汉县', '511700');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('511723', '开江县', 3, '511700', '四川省达州市开江县', '511700'),
       ('511724', '大竹县', 3, '511700', '四川省达州市大竹县', '511700'),
       ('511725', '渠县', 3, '511700', '四川省达州市渠县', '511700'),
       ('511781', '万源市', 3, '511700', '四川省达州市万源市', '511700'),
       ('511800', '雅安市', 2, '510000', '四川省雅安市', '511800'),
       ('511802', '雨城区', 3, '511800', '四川省雅安市雨城区', '511800'),
       ('511803', '名山区', 3, '511800', '四川省雅安市名山区', '511800'),
       ('511822', '荥经县', 3, '511800', '四川省雅安市荥经县', '511800'),
       ('511823', '汉源县', 3, '511800', '四川省雅安市汉源县', '511800'),
       ('511824', '石棉县', 3, '511800', '四川省雅安市石棉县', '511800');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('511825', '天全县', 3, '511800', '四川省雅安市天全县', '511800'),
       ('511826', '芦山县', 3, '511800', '四川省雅安市芦山县', '511800'),
       ('511827', '宝兴县', 3, '511800', '四川省雅安市宝兴县', '511800'),
       ('511900', '巴中市', 2, '510000', '四川省巴中市', '511900'),
       ('511902', '巴州区', 3, '511900', '四川省巴中市巴州区', '511900'),
       ('511903', '恩阳区', 3, '511900', '四川省巴中市恩阳区', '511900'),
       ('511921', '通江县', 3, '511900', '四川省巴中市通江县', '511900'),
       ('511922', '南江县', 3, '511900', '四川省巴中市南江县', '511900'),
       ('511923', '平昌县', 3, '511900', '四川省巴中市平昌县', '511900'),
       ('512000', '资阳市', 2, '510000', '四川省资阳市', '512000');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('512002', '雁江区', 3, '512000', '四川省资阳市雁江区', '512000'),
       ('512021', '安岳县', 3, '512000', '四川省资阳市安岳县', '512000'),
       ('512022', '乐至县', 3, '512000', '四川省资阳市乐至县', '512000'),
       ('513200', '阿坝藏族羌族自治州', 2, '510000', '四川省阿坝藏族羌族自治州', '513200'),
       ('513201', '马尔康市', 3, '513200', '四川省阿坝藏族羌族自治州马尔康市', '513200'),
       ('513221', '汶川县', 3, '513200', '四川省阿坝藏族羌族自治州汶川县', '513200'),
       ('513222', '理县', 3, '513200', '四川省阿坝藏族羌族自治州理县', '513200'),
       ('513223', '茂县', 3, '513200', '四川省阿坝藏族羌族自治州茂县', '513200'),
       ('513224', '松潘县', 3, '513200', '四川省阿坝藏族羌族自治州松潘县', '513200'),
       ('513225', '九寨沟县', 3, '513200', '四川省阿坝藏族羌族自治州九寨沟县', '513200');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('513226', '金川县', 3, '513200', '四川省阿坝藏族羌族自治州金川县', '513200'),
       ('513227', '小金县', 3, '513200', '四川省阿坝藏族羌族自治州小金县', '513200'),
       ('513228', '黑水县', 3, '513200', '四川省阿坝藏族羌族自治州黑水县', '513200'),
       ('513230', '壤塘县', 3, '513200', '四川省阿坝藏族羌族自治州壤塘县', '513200'),
       ('513231', '阿坝县', 3, '513200', '四川省阿坝藏族羌族自治州阿坝县', '513200'),
       ('513232', '若尔盖县', 3, '513200', '四川省阿坝藏族羌族自治州若尔盖县', '513200'),
       ('513233', '红原县', 3, '513200', '四川省阿坝藏族羌族自治州红原县', '513200'),
       ('513300', '甘孜藏族自治州', 2, '510000', '四川省甘孜藏族自治州', '513300'),
       ('513301', '康定市', 3, '513300', '四川省甘孜藏族自治州康定市', '513300'),
       ('513322', '泸定县', 3, '513300', '四川省甘孜藏族自治州泸定县', '513300');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('513323', '丹巴县', 3, '513300', '四川省甘孜藏族自治州丹巴县', '513300'),
       ('513324', '九龙县', 3, '513300', '四川省甘孜藏族自治州九龙县', '513300'),
       ('513325', '雅江县', 3, '513300', '四川省甘孜藏族自治州雅江县', '513300'),
       ('513326', '道孚县', 3, '513300', '四川省甘孜藏族自治州道孚县', '513300'),
       ('513327', '炉霍县', 3, '513300', '四川省甘孜藏族自治州炉霍县', '513300'),
       ('513328', '甘孜县', 3, '513300', '四川省甘孜藏族自治州甘孜县', '513300'),
       ('513329', '新龙县', 3, '513300', '四川省甘孜藏族自治州新龙县', '513300'),
       ('513330', '德格县', 3, '513300', '四川省甘孜藏族自治州德格县', '513300'),
       ('513331', '白玉县', 3, '513300', '四川省甘孜藏族自治州白玉县', '513300'),
       ('513332', '石渠县', 3, '513300', '四川省甘孜藏族自治州石渠县', '513300');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('513333', '色达县', 3, '513300', '四川省甘孜藏族自治州色达县', '513300'),
       ('513334', '理塘县', 3, '513300', '四川省甘孜藏族自治州理塘县', '513300'),
       ('513335', '巴塘县', 3, '513300', '四川省甘孜藏族自治州巴塘县', '513300'),
       ('513336', '乡城县', 3, '513300', '四川省甘孜藏族自治州乡城县', '513300'),
       ('513337', '稻城县', 3, '513300', '四川省甘孜藏族自治州稻城县', '513300'),
       ('513338', '得荣县', 3, '513300', '四川省甘孜藏族自治州得荣县', '513300'),
       ('513400', '凉山彝族自治州', 2, '510000', '四川省凉山彝族自治州', '513400'),
       ('513401', '西昌市', 3, '513400', '四川省凉山彝族自治州西昌市', '513400'),
       ('513422', '木里藏族自治县', 3, '513400', '四川省凉山彝族自治州木里藏族自治县', '513400'),
       ('513423', '盐源县', 3, '513400', '四川省凉山彝族自治州盐源县', '513400');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('513424', '德昌县', 3, '513400', '四川省凉山彝族自治州德昌县', '513400'),
       ('513425', '会理市', 3, '513400', '四川省凉山彝族自治州会理市', '513400'),
       ('513426', '会东县', 3, '513400', '四川省凉山彝族自治州会东县', '513400'),
       ('513427', '宁南县', 3, '513400', '四川省凉山彝族自治州宁南县', '513400'),
       ('513428', '普格县', 3, '513400', '四川省凉山彝族自治州普格县', '513400'),
       ('513429', '布拖县', 3, '513400', '四川省凉山彝族自治州布拖县', '513400'),
       ('513430', '金阳县', 3, '513400', '四川省凉山彝族自治州金阳县', '513400'),
       ('513431', '昭觉县', 3, '513400', '四川省凉山彝族自治州昭觉县', '513400'),
       ('513432', '喜德县', 3, '513400', '四川省凉山彝族自治州喜德县', '513400'),
       ('513433', '冕宁县', 3, '513400', '四川省凉山彝族自治州冕宁县', '513400');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('513434', '越西县', 3, '513400', '四川省凉山彝族自治州越西县', '513400'),
       ('513435', '甘洛县', 3, '513400', '四川省凉山彝族自治州甘洛县', '513400'),
       ('513436', '美姑县', 3, '513400', '四川省凉山彝族自治州美姑县', '513400'),
       ('513437', '雷波县', 3, '513400', '四川省凉山彝族自治州雷波县', '513400'),
       ('520000', '贵州省', 1, NULL, '贵州省', '520000'),
       ('520100', '贵阳市', 2, '520000', '贵州省贵阳市', '520100'),
       ('520102', '南明区', 3, '520100', '贵州省贵阳市南明区', '520100'),
       ('520103', '云岩区', 3, '520100', '贵州省贵阳市云岩区', '520100'),
       ('520111', '花溪区', 3, '520100', '贵州省贵阳市花溪区', '520100'),
       ('520112', '乌当区', 3, '520100', '贵州省贵阳市乌当区', '520100');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('520113', '白云区', 3, '520100', '贵州省贵阳市白云区', '520100'),
       ('520115', '观山湖区', 3, '520100', '贵州省贵阳市观山湖区', '520100'),
       ('520121', '开阳县', 3, '520100', '贵州省贵阳市开阳县', '520100'),
       ('520122', '息烽县', 3, '520100', '贵州省贵阳市息烽县', '520100'),
       ('520123', '修文县', 3, '520100', '贵州省贵阳市修文县', '520100'),
       ('520181', '清镇市', 3, '520100', '贵州省贵阳市清镇市', '520100'),
       ('520200', '六盘水市', 2, '520000', '贵州省六盘水市', '520200'),
       ('520201', '钟山区', 3, '520200', '贵州省六盘水市钟山区', '520200'),
       ('520203', '六枝特区', 3, '520200', '贵州省六盘水市六枝特区', '520200'),
       ('520221', '水城区', 3, '520200', '贵州省六盘水市水城区', '520200');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('520281', '盘州市', 3, '520200', '贵州省六盘水市盘州市', '520200'),
       ('520300', '遵义市', 2, '520000', '贵州省遵义市', '520300'),
       ('520302', '红花岗区', 3, '520300', '贵州省遵义市红花岗区', '520300'),
       ('520303', '汇川区', 3, '520300', '贵州省遵义市汇川区', '520300'),
       ('520304', '播州区', 3, '520300', '贵州省遵义市播州区', '520300'),
       ('520322', '桐梓县', 3, '520300', '贵州省遵义市桐梓县', '520300'),
       ('520323', '绥阳县', 3, '520300', '贵州省遵义市绥阳县', '520300'),
       ('520324', '正安县', 3, '520300', '贵州省遵义市正安县', '520300'),
       ('520325', '道真仡佬族苗族自治县', 3, '520300', '贵州省遵义市道真仡佬族苗族自治县', '520300'),
       ('520326', '务川仡佬族苗族自治县', 3, '520300', '贵州省遵义市务川仡佬族苗族自治县', '520300');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('520327', '凤冈县', 3, '520300', '贵州省遵义市凤冈县', '520300'),
       ('520328', '湄潭县', 3, '520300', '贵州省遵义市湄潭县', '520300'),
       ('520329', '余庆县', 3, '520300', '贵州省遵义市余庆县', '520300'),
       ('520330', '习水县', 3, '520300', '贵州省遵义市习水县', '520300'),
       ('520381', '赤水市', 3, '520300', '贵州省遵义市赤水市', '520300'),
       ('520382', '仁怀市', 3, '520300', '贵州省遵义市仁怀市', '520300'),
       ('520400', '安顺市', 2, '520000', '贵州省安顺市', '520400'),
       ('520402', '西秀区', 3, '520400', '贵州省安顺市西秀区', '520400'),
       ('520403', '平坝区', 3, '520400', '贵州省安顺市平坝区', '520400'),
       ('520422', '普定县', 3, '520400', '贵州省安顺市普定县', '520400');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('520423', '镇宁布依族苗族自治县', 3, '520400', '贵州省安顺市镇宁布依族苗族自治县', '520400'),
       ('520424', '关岭布依族苗族自治县', 3, '520400', '贵州省安顺市关岭布依族苗族自治县', '520400'),
       ('520425', '紫云苗族布依族自治县', 3, '520400', '贵州省安顺市紫云苗族布依族自治县', '520400'),
       ('520500', '毕节市', 2, '520000', '贵州省毕节市', '520500'),
       ('520502', '七星关区', 3, '520500', '贵州省毕节市七星关区', '520500'),
       ('520521', '大方县', 3, '520500', '贵州省毕节市大方县', '520500'),
       ('520522', '黔西市', 3, '520500', '贵州省毕节市黔西市', '520500'),
       ('520523', '金沙县', 3, '520500', '贵州省毕节市金沙县', '520500'),
       ('520524', '织金县', 3, '520500', '贵州省毕节市织金县', '520500'),
       ('520525', '纳雍县', 3, '520500', '贵州省毕节市纳雍县', '520500');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('520526', '威宁彝族回族苗族自治县', 3, '520500', '贵州省毕节市威宁彝族回族苗族自治县', '520500'),
       ('520527', '赫章县', 3, '520500', '贵州省毕节市赫章县', '520500'),
       ('520600', '铜仁市', 2, '520000', '贵州省铜仁市', '520600'),
       ('520602', '碧江区', 3, '520600', '贵州省铜仁市碧江区', '520600'),
       ('520603', '万山区', 3, '520600', '贵州省铜仁市万山区', '520600'),
       ('520621', '江口县', 3, '520600', '贵州省铜仁市江口县', '520600'),
       ('520622', '玉屏侗族自治县', 3, '520600', '贵州省铜仁市玉屏侗族自治县', '520600'),
       ('520623', '石阡县', 3, '520600', '贵州省铜仁市石阡县', '520600'),
       ('520624', '思南县', 3, '520600', '贵州省铜仁市思南县', '520600'),
       ('520625', '印江土家族苗族自治县', 3, '520600', '贵州省铜仁市印江土家族苗族自治县', '520600');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('520626', '德江县', 3, '520600', '贵州省铜仁市德江县', '520600'),
       ('520627', '沿河土家族自治县', 3, '520600', '贵州省铜仁市沿河土家族自治县', '520600'),
       ('520628', '松桃苗族自治县', 3, '520600', '贵州省铜仁市松桃苗族自治县', '520600'),
       ('522300', '黔西南布依族苗族自治州', 2, '520000', '贵州省黔西南布依族苗族自治州', '522300'),
       ('522301', '兴义市', 3, '522300', '贵州省黔西南布依族苗族自治州兴义市', '522300'),
       ('522302', '兴仁市', 3, '522300', '贵州省黔西南布依族苗族自治州兴仁市', '522300'),
       ('522323', '普安县', 3, '522300', '贵州省黔西南布依族苗族自治州普安县', '522300'),
       ('522324', '晴隆县', 3, '522300', '贵州省黔西南布依族苗族自治州晴隆县', '522300'),
       ('522325', '贞丰县', 3, '522300', '贵州省黔西南布依族苗族自治州贞丰县', '522300'),
       ('522326', '望谟县', 3, '522300', '贵州省黔西南布依族苗族自治州望谟县', '522300');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('522327', '册亨县', 3, '522300', '贵州省黔西南布依族苗族自治州册亨县', '522300'),
       ('522328', '安龙县', 3, '522300', '贵州省黔西南布依族苗族自治州安龙县', '522300'),
       ('522600', '黔东南苗族侗族自治州', 2, '520000', '贵州省黔东南苗族侗族自治州', '522600'),
       ('522601', '凯里市', 3, '522600', '贵州省黔东南苗族侗族自治州凯里市', '522600'),
       ('522622', '黄平县', 3, '522600', '贵州省黔东南苗族侗族自治州黄平县', '522600'),
       ('522623', '施秉县', 3, '522600', '贵州省黔东南苗族侗族自治州施秉县', '522600'),
       ('522624', '三穗县', 3, '522600', '贵州省黔东南苗族侗族自治州三穗县', '522600'),
       ('522625', '镇远县', 3, '522600', '贵州省黔东南苗族侗族自治州镇远县', '522600'),
       ('522626', '岑巩县', 3, '522600', '贵州省黔东南苗族侗族自治州岑巩县', '522600'),
       ('522627', '天柱县', 3, '522600', '贵州省黔东南苗族侗族自治州天柱县', '522600');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('522628', '锦屏县', 3, '522600', '贵州省黔东南苗族侗族自治州锦屏县', '522600'),
       ('522629', '剑河县', 3, '522600', '贵州省黔东南苗族侗族自治州剑河县', '522600'),
       ('522630', '台江县', 3, '522600', '贵州省黔东南苗族侗族自治州台江县', '522600'),
       ('522631', '黎平县', 3, '522600', '贵州省黔东南苗族侗族自治州黎平县', '522600'),
       ('522632', '榕江县', 3, '522600', '贵州省黔东南苗族侗族自治州榕江县', '522600'),
       ('522633', '从江县', 3, '522600', '贵州省黔东南苗族侗族自治州从江县', '522600'),
       ('522634', '雷山县', 3, '522600', '贵州省黔东南苗族侗族自治州雷山县', '522600'),
       ('522635', '麻江县', 3, '522600', '贵州省黔东南苗族侗族自治州麻江县', '522600'),
       ('522636', '丹寨县', 3, '522600', '贵州省黔东南苗族侗族自治州丹寨县', '522600'),
       ('522700', '黔南布依族苗族自治州', 2, '520000', '贵州省黔南布依族苗族自治州', '522700');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('522701', '都匀市', 3, '522700', '贵州省黔南布依族苗族自治州都匀市', '522700'),
       ('522702', '福泉市', 3, '522700', '贵州省黔南布依族苗族自治州福泉市', '522700'),
       ('522722', '荔波县', 3, '522700', '贵州省黔南布依族苗族自治州荔波县', '522700'),
       ('522723', '贵定县', 3, '522700', '贵州省黔南布依族苗族自治州贵定县', '522700'),
       ('522725', '瓮安县', 3, '522700', '贵州省黔南布依族苗族自治州瓮安县', '522700'),
       ('522726', '独山县', 3, '522700', '贵州省黔南布依族苗族自治州独山县', '522700'),
       ('522727', '平塘县', 3, '522700', '贵州省黔南布依族苗族自治州平塘县', '522700'),
       ('522728', '罗甸县', 3, '522700', '贵州省黔南布依族苗族自治州罗甸县', '522700'),
       ('522729', '长顺县', 3, '522700', '贵州省黔南布依族苗族自治州长顺县', '522700'),
       ('522730', '龙里县', 3, '522700', '贵州省黔南布依族苗族自治州龙里县', '522700');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('522731', '惠水县', 3, '522700', '贵州省黔南布依族苗族自治州惠水县', '522700'),
       ('522732', '三都水族自治县', 3, '522700', '贵州省黔南布依族苗族自治州三都水族自治县', '522700'),
       ('530000', '云南省', 1, NULL, '云南省', '530000'),
       ('530100', '昆明市', 2, '530000', '云南省昆明市', '530100'),
       ('530102', '五华区', 3, '530100', '云南省昆明市五华区', '530100'),
       ('530103', '盘龙区', 3, '530100', '云南省昆明市盘龙区', '530100'),
       ('530111', '官渡区', 3, '530100', '云南省昆明市官渡区', '530100'),
       ('530112', '西山区', 3, '530100', '云南省昆明市西山区', '530100'),
       ('530113', '东川区', 3, '530100', '云南省昆明市东川区', '530100'),
       ('530114', '呈贡区', 3, '530100', '云南省昆明市呈贡区', '530100');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('530115', '晋宁区', 3, '530100', '云南省昆明市晋宁区', '530100'),
       ('530124', '富民县', 3, '530100', '云南省昆明市富民县', '530100'),
       ('530125', '宜良县', 3, '530100', '云南省昆明市宜良县', '530100'),
       ('530126', '石林彝族自治县', 3, '530100', '云南省昆明市石林彝族自治县', '530100'),
       ('530127', '嵩明县', 3, '530100', '云南省昆明市嵩明县', '530100'),
       ('530128', '禄劝彝族苗族自治县', 3, '530100', '云南省昆明市禄劝彝族苗族自治县', '530100'),
       ('530129', '寻甸回族彝族自治县', 3, '530100', '云南省昆明市寻甸回族彝族自治县', '530100'),
       ('530181', '安宁市', 3, '530100', '云南省昆明市安宁市', '530100'),
       ('530300', '曲靖市', 2, '530000', '云南省曲靖市', '530300'),
       ('530302', '麒麟区', 3, '530300', '云南省曲靖市麒麟区', '530300');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('530303', '沾益区', 3, '530300', '云南省曲靖市沾益区', '530300'),
       ('530304', '马龙区', 3, '530300', '云南省曲靖市马龙区', '530300'),
       ('530322', '陆良县', 3, '530300', '云南省曲靖市陆良县', '530300'),
       ('530323', '师宗县', 3, '530300', '云南省曲靖市师宗县', '530300'),
       ('530324', '罗平县', 3, '530300', '云南省曲靖市罗平县', '530300'),
       ('530325', '富源县', 3, '530300', '云南省曲靖市富源县', '530300'),
       ('530326', '会泽县', 3, '530300', '云南省曲靖市会泽县', '530300'),
       ('530381', '宣威市', 3, '530300', '云南省曲靖市宣威市', '530300'),
       ('530400', '玉溪市', 2, '530000', '云南省玉溪市', '530400'),
       ('530402', '红塔区', 3, '530400', '云南省玉溪市红塔区', '530400');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('530403', '江川区', 3, '530400', '云南省玉溪市江川区', '530400'),
       ('530423', '通海县', 3, '530400', '云南省玉溪市通海县', '530400'),
       ('530424', '华宁县', 3, '530400', '云南省玉溪市华宁县', '530400'),
       ('530425', '易门县', 3, '530400', '云南省玉溪市易门县', '530400'),
       ('530426', '峨山彝族自治县', 3, '530400', '云南省玉溪市峨山彝族自治县', '530400'),
       ('530427', '新平彝族傣族自治县', 3, '530400', '云南省玉溪市新平彝族傣族自治县', '530400'),
       ('530428', '元江哈尼族彝族傣族自治县', 3, '530400', '云南省玉溪市元江哈尼族彝族傣族自治县', '530400'),
       ('530481', '澄江市', 3, '530400', '云南省玉溪市澄江市', '530400'),
       ('530500', '保山市', 2, '530000', '云南省保山市', '530500'),
       ('530502', '隆阳区', 3, '530500', '云南省保山市隆阳区', '530500');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('530521', '施甸县', 3, '530500', '云南省保山市施甸县', '530500'),
       ('530523', '龙陵县', 3, '530500', '云南省保山市龙陵县', '530500'),
       ('530524', '昌宁县', 3, '530500', '云南省保山市昌宁县', '530500'),
       ('530581', '腾冲市', 3, '530500', '云南省保山市腾冲市', '530500'),
       ('530600', '昭通市', 2, '530000', '云南省昭通市', '530600'),
       ('530602', '昭阳区', 3, '530600', '云南省昭通市昭阳区', '530600'),
       ('530621', '鲁甸县', 3, '530600', '云南省昭通市鲁甸县', '530600'),
       ('530622', '巧家县', 3, '530600', '云南省昭通市巧家县', '530600'),
       ('530623', '盐津县', 3, '530600', '云南省昭通市盐津县', '530600'),
       ('530624', '大关县', 3, '530600', '云南省昭通市大关县', '530600');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('530625', '永善县', 3, '530600', '云南省昭通市永善县', '530600'),
       ('530626', '绥江县', 3, '530600', '云南省昭通市绥江县', '530600'),
       ('530627', '镇雄县', 3, '530600', '云南省昭通市镇雄县', '530600'),
       ('530628', '彝良县', 3, '530600', '云南省昭通市彝良县', '530600'),
       ('530629', '威信县', 3, '530600', '云南省昭通市威信县', '530600'),
       ('530681', '水富市', 3, '530600', '云南省昭通市水富市', '530600'),
       ('530700', '丽江市', 2, '530000', '云南省丽江市', '530700'),
       ('530702', '古城区', 3, '530700', '云南省丽江市古城区', '530700'),
       ('530721', '玉龙纳西族自治县', 3, '530700', '云南省丽江市玉龙纳西族自治县', '530700'),
       ('530722', '永胜县', 3, '530700', '云南省丽江市永胜县', '530700');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('530723', '华坪县', 3, '530700', '云南省丽江市华坪县', '530700'),
       ('530724', '宁蒗彝族自治县', 3, '530700', '云南省丽江市宁蒗彝族自治县', '530700'),
       ('530800', '普洱市', 2, '530000', '云南省普洱市', '530800'),
       ('530802', '思茅区', 3, '530800', '云南省普洱市思茅区', '530800'),
       ('530821', '宁洱哈尼族彝族自治县', 3, '530800', '云南省普洱市宁洱哈尼族彝族自治县', '530800'),
       ('530822', '墨江哈尼族自治县', 3, '530800', '云南省普洱市墨江哈尼族自治县', '530800'),
       ('530823', '景东彝族自治县', 3, '530800', '云南省普洱市景东彝族自治县', '530800'),
       ('530824', '景谷傣族彝族自治县', 3, '530800', '云南省普洱市景谷傣族彝族自治县', '530800'),
       ('530825', '镇沅彝族哈尼族拉祜族自治县', 3, '530800', '云南省普洱市镇沅彝族哈尼族拉祜族自治县', '530800'),
       ('530826', '江城哈尼族彝族自治县', 3, '530800', '云南省普洱市江城哈尼族彝族自治县', '530800');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('530827', '孟连傣族拉祜族佤族自治县', 3, '530800', '云南省普洱市孟连傣族拉祜族佤族自治县', '530800'),
       ('530828', '澜沧拉祜族自治县', 3, '530800', '云南省普洱市澜沧拉祜族自治县', '530800'),
       ('530829', '西盟佤族自治县', 3, '530800', '云南省普洱市西盟佤族自治县', '530800'),
       ('530900', '临沧市', 2, '530000', '云南省临沧市', '530900'),
       ('530902', '临翔区', 3, '530900', '云南省临沧市临翔区', '530900'),
       ('530921', '凤庆县', 3, '530900', '云南省临沧市凤庆县', '530900'),
       ('530922', '云县', 3, '530900', '云南省临沧市云县', '530900'),
       ('530923', '永德县', 3, '530900', '云南省临沧市永德县', '530900'),
       ('530924', '镇康县', 3, '530900', '云南省临沧市镇康县', '530900'),
       ('530925', '双江拉祜族佤族布朗族傣族自治县', 3, '530900', '云南省临沧市双江拉祜族佤族布朗族傣族自治县',
        '530900');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('530926', '耿马傣族佤族自治县', 3, '530900', '云南省临沧市耿马傣族佤族自治县', '530900'),
       ('530927', '沧源佤族自治县', 3, '530900', '云南省临沧市沧源佤族自治县', '530900'),
       ('532300', '楚雄彝族自治州', 2, '530000', '云南省楚雄彝族自治州', '532300'),
       ('532301', '楚雄市', 3, '532300', '云南省楚雄彝族自治州楚雄市', '532300'),
       ('532322', '双柏县', 3, '532300', '云南省楚雄彝族自治州双柏县', '532300'),
       ('532323', '牟定县', 3, '532300', '云南省楚雄彝族自治州牟定县', '532300'),
       ('532324', '南华县', 3, '532300', '云南省楚雄彝族自治州南华县', '532300'),
       ('532325', '姚安县', 3, '532300', '云南省楚雄彝族自治州姚安县', '532300'),
       ('532326', '大姚县', 3, '532300', '云南省楚雄彝族自治州大姚县', '532300'),
       ('532327', '永仁县', 3, '532300', '云南省楚雄彝族自治州永仁县', '532300');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('532328', '元谋县', 3, '532300', '云南省楚雄彝族自治州元谋县', '532300'),
       ('532329', '武定县', 3, '532300', '云南省楚雄彝族自治州武定县', '532300'),
       ('532331', '禄丰市', 3, '532300', '云南省楚雄彝族自治州禄丰市', '532300'),
       ('532500', '红河哈尼族彝族自治州', 2, '530000', '云南省红河哈尼族彝族自治州', '532500'),
       ('532501', '个旧市', 3, '532500', '云南省红河哈尼族彝族自治州个旧市', '532500'),
       ('532502', '开远市', 3, '532500', '云南省红河哈尼族彝族自治州开远市', '532500'),
       ('532503', '蒙自市', 3, '532500', '云南省红河哈尼族彝族自治州蒙自市', '532500'),
       ('532504', '弥勒市', 3, '532500', '云南省红河哈尼族彝族自治州弥勒市', '532500'),
       ('532523', '屏边苗族自治县', 3, '532500', '云南省红河哈尼族彝族自治州屏边苗族自治县', '532500'),
       ('532524', '建水县', 3, '532500', '云南省红河哈尼族彝族自治州建水县', '532500');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('532525', '石屏县', 3, '532500', '云南省红河哈尼族彝族自治州石屏县', '532500'),
       ('532527', '泸西县', 3, '532500', '云南省红河哈尼族彝族自治州泸西县', '532500'),
       ('532528', '元阳县', 3, '532500', '云南省红河哈尼族彝族自治州元阳县', '532500'),
       ('532529', '红河县', 3, '532500', '云南省红河哈尼族彝族自治州红河县', '532500'),
       ('532530', '金平苗族瑶族傣族自治县', 3, '532500', '云南省红河哈尼族彝族自治州金平苗族瑶族傣族自治县', '532500'),
       ('532531', '绿春县', 3, '532500', '云南省红河哈尼族彝族自治州绿春县', '532500'),
       ('532532', '河口瑶族自治县', 3, '532500', '云南省红河哈尼族彝族自治州河口瑶族自治县', '532500'),
       ('532600', '文山壮族苗族自治州', 2, '530000', '云南省文山壮族苗族自治州', '532600'),
       ('532601', '文山市', 3, '532600', '云南省文山壮族苗族自治州文山市', '532600'),
       ('532622', '砚山县', 3, '532600', '云南省文山壮族苗族自治州砚山县', '532600');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('532623', '西畴县', 3, '532600', '云南省文山壮族苗族自治州西畴县', '532600'),
       ('532624', '麻栗坡县', 3, '532600', '云南省文山壮族苗族自治州麻栗坡县', '532600'),
       ('532625', '马关县', 3, '532600', '云南省文山壮族苗族自治州马关县', '532600'),
       ('532626', '丘北县', 3, '532600', '云南省文山壮族苗族自治州丘北县', '532600'),
       ('532627', '广南县', 3, '532600', '云南省文山壮族苗族自治州广南县', '532600'),
       ('532628', '富宁县', 3, '532600', '云南省文山壮族苗族自治州富宁县', '532600'),
       ('532800', '西双版纳傣族自治州', 2, '530000', '云南省西双版纳傣族自治州', '532800'),
       ('532801', '景洪市', 3, '532800', '云南省西双版纳傣族自治州景洪市', '532800'),
       ('532822', '勐海县', 3, '532800', '云南省西双版纳傣族自治州勐海县', '532800'),
       ('532823', '勐腊县', 3, '532800', '云南省西双版纳傣族自治州勐腊县', '532800');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('532900', '大理白族自治州', 2, '530000', '云南省大理白族自治州', '532900'),
       ('532901', '大理市', 3, '532900', '云南省大理白族自治州大理市', '532900'),
       ('532922', '漾濞彝族自治县', 3, '532900', '云南省大理白族自治州漾濞彝族自治县', '532900'),
       ('532923', '祥云县', 3, '532900', '云南省大理白族自治州祥云县', '532900'),
       ('532924', '宾川县', 3, '532900', '云南省大理白族自治州宾川县', '532900'),
       ('532925', '弥渡县', 3, '532900', '云南省大理白族自治州弥渡县', '532900'),
       ('532926', '南涧彝族自治县', 3, '532900', '云南省大理白族自治州南涧彝族自治县', '532900'),
       ('532927', '巍山彝族回族自治县', 3, '532900', '云南省大理白族自治州巍山彝族回族自治县', '532900'),
       ('532928', '永平县', 3, '532900', '云南省大理白族自治州永平县', '532900'),
       ('532929', '云龙县', 3, '532900', '云南省大理白族自治州云龙县', '532900');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('532930', '洱源县', 3, '532900', '云南省大理白族自治州洱源县', '532900'),
       ('532931', '剑川县', 3, '532900', '云南省大理白族自治州剑川县', '532900'),
       ('532932', '鹤庆县', 3, '532900', '云南省大理白族自治州鹤庆县', '532900'),
       ('533100', '德宏傣族景颇族自治州', 2, '530000', '云南省德宏傣族景颇族自治州', '533100'),
       ('533102', '瑞丽市', 3, '533100', '云南省德宏傣族景颇族自治州瑞丽市', '533100'),
       ('533103', '芒市', 3, '533100', '云南省德宏傣族景颇族自治州芒市', '533100'),
       ('533122', '梁河县', 3, '533100', '云南省德宏傣族景颇族自治州梁河县', '533100'),
       ('533123', '盈江县', 3, '533100', '云南省德宏傣族景颇族自治州盈江县', '533100'),
       ('533124', '陇川县', 3, '533100', '云南省德宏傣族景颇族自治州陇川县', '533100'),
       ('533300', '怒江傈僳族自治州', 2, '530000', '云南省怒江傈僳族自治州', '533300');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('533301', '泸水市', 3, '533300', '云南省怒江傈僳族自治州泸水市', '533300'),
       ('533323', '福贡县', 3, '533300', '云南省怒江傈僳族自治州福贡县', '533300'),
       ('533324', '贡山独龙族怒族自治县', 3, '533300', '云南省怒江傈僳族自治州贡山独龙族怒族自治县', '533300'),
       ('533325', '兰坪白族普米族自治县', 3, '533300', '云南省怒江傈僳族自治州兰坪白族普米族自治县', '533300'),
       ('533400', '迪庆藏族自治州', 2, '530000', '云南省迪庆藏族自治州', '533400'),
       ('533401', '香格里拉市', 3, '533400', '云南省迪庆藏族自治州香格里拉市', '533400'),
       ('533422', '德钦县', 3, '533400', '云南省迪庆藏族自治州德钦县', '533400'),
       ('533423', '维西傈僳族自治县', 3, '533400', '云南省迪庆藏族自治州维西傈僳族自治县', '533400'),
       ('540000', '西藏自治区', 1, NULL, '西藏自治区', '540000'),
       ('540100', '拉萨市', 2, '540000', '西藏自治区拉萨市', '540100');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('540102', '城关区', 3, '540100', '西藏自治区拉萨市城关区', '540100'),
       ('540103', '堆龙德庆区', 3, '540100', '西藏自治区拉萨市堆龙德庆区', '540100'),
       ('540104', '达孜区', 3, '540100', '西藏自治区拉萨市达孜区', '540100'),
       ('540121', '林周县', 3, '540100', '西藏自治区拉萨市林周县', '540100'),
       ('540122', '当雄县', 3, '540100', '西藏自治区拉萨市当雄县', '540100'),
       ('540123', '尼木县', 3, '540100', '西藏自治区拉萨市尼木县', '540100'),
       ('540124', '曲水县', 3, '540100', '西藏自治区拉萨市曲水县', '540100'),
       ('540127', '墨竹工卡县', 3, '540100', '西藏自治区拉萨市墨竹工卡县', '540100'),
       ('540200', '日喀则市', 2, '540000', '西藏自治区日喀则市', '540200'),
       ('540202', '桑珠孜区', 3, '540200', '西藏自治区日喀则市桑珠孜区', '540200');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('540221', '南木林县', 3, '540200', '西藏自治区日喀则市南木林县', '540200'),
       ('540222', '江孜县', 3, '540200', '西藏自治区日喀则市江孜县', '540200'),
       ('540223', '定日县', 3, '540200', '西藏自治区日喀则市定日县', '540200'),
       ('540224', '萨迦县', 3, '540200', '西藏自治区日喀则市萨迦县', '540200'),
       ('540225', '拉孜县', 3, '540200', '西藏自治区日喀则市拉孜县', '540200'),
       ('540226', '昂仁县', 3, '540200', '西藏自治区日喀则市昂仁县', '540200'),
       ('540227', '谢通门县', 3, '540200', '西藏自治区日喀则市谢通门县', '540200'),
       ('540228', '白朗县', 3, '540200', '西藏自治区日喀则市白朗县', '540200'),
       ('540229', '仁布县', 3, '540200', '西藏自治区日喀则市仁布县', '540200'),
       ('540230', '康马县', 3, '540200', '西藏自治区日喀则市康马县', '540200');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('540231', '定结县', 3, '540200', '西藏自治区日喀则市定结县', '540200'),
       ('540232', '仲巴县', 3, '540200', '西藏自治区日喀则市仲巴县', '540200'),
       ('540233', '亚东县', 3, '540200', '西藏自治区日喀则市亚东县', '540200'),
       ('540234', '吉隆县', 3, '540200', '西藏自治区日喀则市吉隆县', '540200'),
       ('540235', '聂拉木县', 3, '540200', '西藏自治区日喀则市聂拉木县', '540200'),
       ('540236', '萨嘎县', 3, '540200', '西藏自治区日喀则市萨嘎县', '540200'),
       ('540237', '岗巴县', 3, '540200', '西藏自治区日喀则市岗巴县', '540200'),
       ('540300', '昌都市', 2, '540000', '西藏自治区昌都市', '540300'),
       ('540302', '卡若区', 3, '540300', '西藏自治区昌都市卡若区', '540300'),
       ('540321', '江达县', 3, '540300', '西藏自治区昌都市江达县', '540300');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('540322', '贡觉县', 3, '540300', '西藏自治区昌都市贡觉县', '540300'),
       ('540323', '类乌齐县', 3, '540300', '西藏自治区昌都市类乌齐县', '540300'),
       ('540324', '丁青县', 3, '540300', '西藏自治区昌都市丁青县', '540300'),
       ('540325', '察雅县', 3, '540300', '西藏自治区昌都市察雅县', '540300'),
       ('540326', '八宿县', 3, '540300', '西藏自治区昌都市八宿县', '540300'),
       ('540327', '左贡县', 3, '540300', '西藏自治区昌都市左贡县', '540300'),
       ('540328', '芒康县', 3, '540300', '西藏自治区昌都市芒康县', '540300'),
       ('540329', '洛隆县', 3, '540300', '西藏自治区昌都市洛隆县', '540300'),
       ('540330', '边坝县', 3, '540300', '西藏自治区昌都市边坝县', '540300'),
       ('540400', '林芝市', 2, '540000', '西藏自治区林芝市', '540400');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('540402', '巴宜区', 3, '540400', '西藏自治区林芝市巴宜区', '540400'),
       ('540421', '工布江达县', 3, '540400', '西藏自治区林芝市工布江达县', '540400'),
       ('540422', '米林县', 3, '540400', '西藏自治区林芝市米林县', '540400'),
       ('540423', '墨脱县', 3, '540400', '西藏自治区林芝市墨脱县', '540400'),
       ('540424', '波密县', 3, '540400', '西藏自治区林芝市波密县', '540400'),
       ('540425', '察隅县', 3, '540400', '西藏自治区林芝市察隅县', '540400'),
       ('540426', '朗县', 3, '540400', '西藏自治区林芝市朗县', '540400'),
       ('540500', '山南市', 2, '540000', '西藏自治区山南市', '540500'),
       ('540502', '乃东区', 3, '540500', '西藏自治区山南市乃东区', '540500'),
       ('540521', '扎囊县', 3, '540500', '西藏自治区山南市扎囊县', '540500');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('540522', '贡嘎县', 3, '540500', '西藏自治区山南市贡嘎县', '540500'),
       ('540523', '桑日县', 3, '540500', '西藏自治区山南市桑日县', '540500'),
       ('540524', '琼结县', 3, '540500', '西藏自治区山南市琼结县', '540500'),
       ('540525', '曲松县', 3, '540500', '西藏自治区山南市曲松县', '540500'),
       ('540526', '措美县', 3, '540500', '西藏自治区山南市措美县', '540500'),
       ('540527', '洛扎县', 3, '540500', '西藏自治区山南市洛扎县', '540500'),
       ('540528', '加查县', 3, '540500', '西藏自治区山南市加查县', '540500'),
       ('540529', '隆子县', 3, '540500', '西藏自治区山南市隆子县', '540500'),
       ('540530', '错那县', 3, '540500', '西藏自治区山南市错那县', '540500'),
       ('540531', '浪卡子县', 3, '540500', '西藏自治区山南市浪卡子县', '540500');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('540600', '那曲市', 2, '540000', '西藏自治区那曲市', '540600'),
       ('540602', '色尼区', 3, '540600', '西藏自治区那曲市色尼区', '540600'),
       ('540621', '嘉黎县', 3, '540600', '西藏自治区那曲市嘉黎县', '540600'),
       ('540622', '比如县', 3, '540600', '西藏自治区那曲市比如县', '540600'),
       ('540623', '聂荣县', 3, '540600', '西藏自治区那曲市聂荣县', '540600'),
       ('540624', '安多县', 3, '540600', '西藏自治区那曲市安多县', '540600'),
       ('540625', '申扎县', 3, '540600', '西藏自治区那曲市申扎县', '540600'),
       ('540626', '索县', 3, '540600', '西藏自治区那曲市索县', '540600'),
       ('540627', '班戈县', 3, '540600', '西藏自治区那曲市班戈县', '540600'),
       ('540628', '巴青县', 3, '540600', '西藏自治区那曲市巴青县', '540600');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('540629', '尼玛县', 3, '540600', '西藏自治区那曲市尼玛县', '540600'),
       ('540630', '双湖县', 3, '540600', '西藏自治区那曲市双湖县', '540600'),
       ('542500', '阿里地区', 2, '540000', '西藏自治区阿里地区', '542500'),
       ('542521', '普兰县', 3, '542500', '西藏自治区阿里地区普兰县', '542500'),
       ('542522', '札达县', 3, '542500', '西藏自治区阿里地区札达县', '542500'),
       ('542523', '噶尔县', 3, '542500', '西藏自治区阿里地区噶尔县', '542500'),
       ('542524', '日土县', 3, '542500', '西藏自治区阿里地区日土县', '542500'),
       ('542525', '革吉县', 3, '542500', '西藏自治区阿里地区革吉县', '542500'),
       ('542526', '改则县', 3, '542500', '西藏自治区阿里地区改则县', '542500'),
       ('542527', '措勤县', 3, '542500', '西藏自治区阿里地区措勤县', '542500');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('610000', '陕西省', 1, NULL, '陕西省', '610000'),
       ('610100', '西安市', 2, '610000', '陕西省西安市', '610100'),
       ('610102', '新城区', 3, '610100', '陕西省西安市新城区', '610100'),
       ('610103', '碑林区', 3, '610100', '陕西省西安市碑林区', '610100'),
       ('610104', '莲湖区', 3, '610100', '陕西省西安市莲湖区', '610100'),
       ('610111', '灞桥区', 3, '610100', '陕西省西安市灞桥区', '610100'),
       ('610112', '未央区', 3, '610100', '陕西省西安市未央区', '610100'),
       ('610113', '雁塔区', 3, '610100', '陕西省西安市雁塔区', '610100'),
       ('610114', '阎良区', 3, '610100', '陕西省西安市阎良区', '610100'),
       ('610115', '临潼区', 3, '610100', '陕西省西安市临潼区', '610100');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('610116', '长安区', 3, '610100', '陕西省西安市长安区', '610100'),
       ('610117', '高陵区', 3, '610100', '陕西省西安市高陵区', '610100'),
       ('610118', '鄠邑区', 3, '610100', '陕西省西安市鄠邑区', '610100'),
       ('610122', '蓝田县', 3, '610100', '陕西省西安市蓝田县', '610100'),
       ('610124', '周至县', 3, '610100', '陕西省西安市周至县', '610100'),
       ('610200', '铜川市', 2, '610000', '陕西省铜川市', '610200'),
       ('610202', '王益区', 3, '610200', '陕西省铜川市王益区', '610200'),
       ('610203', '印台区', 3, '610200', '陕西省铜川市印台区', '610200'),
       ('610204', '耀州区', 3, '610200', '陕西省铜川市耀州区', '610200'),
       ('610222', '宜君县', 3, '610200', '陕西省铜川市宜君县', '610200');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('610300', '宝鸡市', 2, '610000', '陕西省宝鸡市', '610300'),
       ('610302', '渭滨区', 3, '610300', '陕西省宝鸡市渭滨区', '610300'),
       ('610303', '金台区', 3, '610300', '陕西省宝鸡市金台区', '610300'),
       ('610304', '陈仓区', 3, '610300', '陕西省宝鸡市陈仓区', '610300'),
       ('610322', '凤翔区', 3, '610300', '陕西省宝鸡市凤翔区', '610300'),
       ('610323', '岐山县', 3, '610300', '陕西省宝鸡市岐山县', '610300'),
       ('610324', '扶风县', 3, '610300', '陕西省宝鸡市扶风县', '610300'),
       ('610326', '眉县', 3, '610300', '陕西省宝鸡市眉县', '610300'),
       ('610327', '陇县', 3, '610300', '陕西省宝鸡市陇县', '610300'),
       ('610328', '千阳县', 3, '610300', '陕西省宝鸡市千阳县', '610300');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('610329', '麟游县', 3, '610300', '陕西省宝鸡市麟游县', '610300'),
       ('610330', '凤县', 3, '610300', '陕西省宝鸡市凤县', '610300'),
       ('610331', '太白县', 3, '610300', '陕西省宝鸡市太白县', '610300'),
       ('610400', '咸阳市', 2, '610000', '陕西省咸阳市', '610400'),
       ('610402', '秦都区', 3, '610400', '陕西省咸阳市秦都区', '610400'),
       ('610403', '杨陵区', 3, '610400', '陕西省咸阳市杨陵区', '610400'),
       ('610404', '渭城区', 3, '610400', '陕西省咸阳市渭城区', '610400'),
       ('610422', '三原县', 3, '610400', '陕西省咸阳市三原县', '610400'),
       ('610423', '泾阳县', 3, '610400', '陕西省咸阳市泾阳县', '610400'),
       ('610424', '乾县', 3, '610400', '陕西省咸阳市乾县', '610400');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('610425', '礼泉县', 3, '610400', '陕西省咸阳市礼泉县', '610400'),
       ('610426', '永寿县', 3, '610400', '陕西省咸阳市永寿县', '610400'),
       ('610428', '长武县', 3, '610400', '陕西省咸阳市长武县', '610400'),
       ('610429', '旬邑县', 3, '610400', '陕西省咸阳市旬邑县', '610400'),
       ('610430', '淳化县', 3, '610400', '陕西省咸阳市淳化县', '610400'),
       ('610431', '武功县', 3, '610400', '陕西省咸阳市武功县', '610400'),
       ('610481', '兴平市', 3, '610400', '陕西省咸阳市兴平市', '610400'),
       ('610482', '彬州市', 3, '610400', '陕西省咸阳市彬州市', '610400'),
       ('610500', '渭南市', 2, '610000', '陕西省渭南市', '610500'),
       ('610502', '临渭区', 3, '610500', '陕西省渭南市临渭区', '610500');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('610503', '华州区', 3, '610500', '陕西省渭南市华州区', '610500'),
       ('610522', '潼关县', 3, '610500', '陕西省渭南市潼关县', '610500'),
       ('610523', '大荔县', 3, '610500', '陕西省渭南市大荔县', '610500'),
       ('610524', '合阳县', 3, '610500', '陕西省渭南市合阳县', '610500'),
       ('610525', '澄城县', 3, '610500', '陕西省渭南市澄城县', '610500'),
       ('610526', '蒲城县', 3, '610500', '陕西省渭南市蒲城县', '610500'),
       ('610527', '白水县', 3, '610500', '陕西省渭南市白水县', '610500'),
       ('610528', '富平县', 3, '610500', '陕西省渭南市富平县', '610500'),
       ('610581', '韩城市', 3, '610500', '陕西省渭南市韩城市', '610500'),
       ('610582', '华阴市', 3, '610500', '陕西省渭南市华阴市', '610500');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('610600', '延安市', 2, '610000', '陕西省延安市', '610600'),
       ('610602', '宝塔区', 3, '610600', '陕西省延安市宝塔区', '610600'),
       ('610603', '安塞区', 3, '610600', '陕西省延安市安塞区', '610600'),
       ('610621', '延长县', 3, '610600', '陕西省延安市延长县', '610600'),
       ('610622', '延川县', 3, '610600', '陕西省延安市延川县', '610600'),
       ('610625', '志丹县', 3, '610600', '陕西省延安市志丹县', '610600'),
       ('610626', '吴起县', 3, '610600', '陕西省延安市吴起县', '610600'),
       ('610627', '甘泉县', 3, '610600', '陕西省延安市甘泉县', '610600'),
       ('610628', '富县', 3, '610600', '陕西省延安市富县', '610600'),
       ('610629', '洛川县', 3, '610600', '陕西省延安市洛川县', '610600');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('610630', '宜川县', 3, '610600', '陕西省延安市宜川县', '610600'),
       ('610631', '黄龙县', 3, '610600', '陕西省延安市黄龙县', '610600'),
       ('610632', '黄陵县', 3, '610600', '陕西省延安市黄陵县', '610600'),
       ('610681', '子长市', 3, '610600', '陕西省延安市子长市', '610600'),
       ('610700', '汉中市', 2, '610000', '陕西省汉中市', '610700'),
       ('610702', '汉台区', 3, '610700', '陕西省汉中市汉台区', '610700'),
       ('610703', '南郑区', 3, '610700', '陕西省汉中市南郑区', '610700'),
       ('610722', '城固县', 3, '610700', '陕西省汉中市城固县', '610700'),
       ('610723', '洋县', 3, '610700', '陕西省汉中市洋县', '610700'),
       ('610724', '西乡县', 3, '610700', '陕西省汉中市西乡县', '610700');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('610725', '勉县', 3, '610700', '陕西省汉中市勉县', '610700'),
       ('610726', '宁强县', 3, '610700', '陕西省汉中市宁强县', '610700'),
       ('610727', '略阳县', 3, '610700', '陕西省汉中市略阳县', '610700'),
       ('610728', '镇巴县', 3, '610700', '陕西省汉中市镇巴县', '610700'),
       ('610729', '留坝县', 3, '610700', '陕西省汉中市留坝县', '610700'),
       ('610730', '佛坪县', 3, '610700', '陕西省汉中市佛坪县', '610700'),
       ('610800', '榆林市', 2, '610000', '陕西省榆林市', '610800'),
       ('610802', '榆阳区', 3, '610800', '陕西省榆林市榆阳区', '610800'),
       ('610803', '横山区', 3, '610800', '陕西省榆林市横山区', '610800'),
       ('610822', '府谷县', 3, '610800', '陕西省榆林市府谷县', '610800');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('610824', '靖边县', 3, '610800', '陕西省榆林市靖边县', '610800'),
       ('610825', '定边县', 3, '610800', '陕西省榆林市定边县', '610800'),
       ('610826', '绥德县', 3, '610800', '陕西省榆林市绥德县', '610800'),
       ('610827', '米脂县', 3, '610800', '陕西省榆林市米脂县', '610800'),
       ('610828', '佳县', 3, '610800', '陕西省榆林市佳县', '610800'),
       ('610829', '吴堡县', 3, '610800', '陕西省榆林市吴堡县', '610800'),
       ('610830', '清涧县', 3, '610800', '陕西省榆林市清涧县', '610800'),
       ('610831', '子洲县', 3, '610800', '陕西省榆林市子洲县', '610800'),
       ('610881', '神木市', 3, '610800', '陕西省榆林市神木市', '610800'),
       ('610900', '安康市', 2, '610000', '陕西省安康市', '610900');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('610902', '汉滨区', 3, '610900', '陕西省安康市汉滨区', '610900'),
       ('610921', '汉阴县', 3, '610900', '陕西省安康市汉阴县', '610900'),
       ('610922', '石泉县', 3, '610900', '陕西省安康市石泉县', '610900'),
       ('610923', '宁陕县', 3, '610900', '陕西省安康市宁陕县', '610900'),
       ('610924', '紫阳县', 3, '610900', '陕西省安康市紫阳县', '610900'),
       ('610925', '岚皋县', 3, '610900', '陕西省安康市岚皋县', '610900'),
       ('610926', '平利县', 3, '610900', '陕西省安康市平利县', '610900'),
       ('610927', '镇坪县', 3, '610900', '陕西省安康市镇坪县', '610900'),
       ('610928', '旬阳市', 3, '610900', '陕西省安康市旬阳市', '610900'),
       ('610929', '白河县', 3, '610900', '陕西省安康市白河县', '610900');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('611000', '商洛市', 2, '610000', '陕西省商洛市', '611000'),
       ('611002', '商州区', 3, '611000', '陕西省商洛市商州区', '611000'),
       ('611021', '洛南县', 3, '611000', '陕西省商洛市洛南县', '611000'),
       ('611022', '丹凤县', 3, '611000', '陕西省商洛市丹凤县', '611000'),
       ('611023', '商南县', 3, '611000', '陕西省商洛市商南县', '611000'),
       ('611024', '山阳县', 3, '611000', '陕西省商洛市山阳县', '611000'),
       ('611025', '镇安县', 3, '611000', '陕西省商洛市镇安县', '611000'),
       ('611026', '柞水县', 3, '611000', '陕西省商洛市柞水县', '611000'),
       ('620000', '甘肃省', 1, NULL, '甘肃省', '620000'),
       ('620100', '兰州市', 2, '620000', '甘肃省兰州市', '620100');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('620102', '城关区', 3, '620100', '甘肃省兰州市城关区', '620100'),
       ('620103', '七里河区', 3, '620100', '甘肃省兰州市七里河区', '620100'),
       ('620104', '西固区', 3, '620100', '甘肃省兰州市西固区', '620100'),
       ('620105', '安宁区', 3, '620100', '甘肃省兰州市安宁区', '620100'),
       ('620111', '红古区', 3, '620100', '甘肃省兰州市红古区', '620100'),
       ('620121', '永登县', 3, '620100', '甘肃省兰州市永登县', '620100'),
       ('620122', '皋兰县', 3, '620100', '甘肃省兰州市皋兰县', '620100'),
       ('620123', '榆中县', 3, '620100', '甘肃省兰州市榆中县', '620100'),
       ('620200', '嘉峪关市', 2, '620000', '甘肃省嘉峪关市', '620200'),
       ('620300', '金昌市', 2, '620000', '甘肃省金昌市', '620300');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('620302', '金川区', 3, '620300', '甘肃省金昌市金川区', '620300'),
       ('620321', '永昌县', 3, '620300', '甘肃省金昌市永昌县', '620300'),
       ('620400', '白银市', 2, '620000', '甘肃省白银市', '620400'),
       ('620402', '白银区', 3, '620400', '甘肃省白银市白银区', '620400'),
       ('620403', '平川区', 3, '620400', '甘肃省白银市平川区', '620400'),
       ('620421', '靖远县', 3, '620400', '甘肃省白银市靖远县', '620400'),
       ('620422', '会宁县', 3, '620400', '甘肃省白银市会宁县', '620400'),
       ('620423', '景泰县', 3, '620400', '甘肃省白银市景泰县', '620400'),
       ('620500', '天水市', 2, '620000', '甘肃省天水市', '620500'),
       ('620502', '秦州区', 3, '620500', '甘肃省天水市秦州区', '620500');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('620503', '麦积区', 3, '620500', '甘肃省天水市麦积区', '620500'),
       ('620521', '清水县', 3, '620500', '甘肃省天水市清水县', '620500'),
       ('620522', '秦安县', 3, '620500', '甘肃省天水市秦安县', '620500'),
       ('620523', '甘谷县', 3, '620500', '甘肃省天水市甘谷县', '620500'),
       ('620524', '武山县', 3, '620500', '甘肃省天水市武山县', '620500'),
       ('620525', '张家川回族自治县', 3, '620500', '甘肃省天水市张家川回族自治县', '620500'),
       ('620600', '武威市', 2, '620000', '甘肃省武威市', '620600'),
       ('620602', '凉州区', 3, '620600', '甘肃省武威市凉州区', '620600'),
       ('620621', '民勤县', 3, '620600', '甘肃省武威市民勤县', '620600'),
       ('620622', '古浪县', 3, '620600', '甘肃省武威市古浪县', '620600');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('620623', '天祝藏族自治县', 3, '620600', '甘肃省武威市天祝藏族自治县', '620600'),
       ('620700', '张掖市', 2, '620000', '甘肃省张掖市', '620700'),
       ('620702', '甘州区', 3, '620700', '甘肃省张掖市甘州区', '620700'),
       ('620721', '肃南裕固族自治县', 3, '620700', '甘肃省张掖市肃南裕固族自治县', '620700'),
       ('620722', '民乐县', 3, '620700', '甘肃省张掖市民乐县', '620700'),
       ('620723', '临泽县', 3, '620700', '甘肃省张掖市临泽县', '620700'),
       ('620724', '高台县', 3, '620700', '甘肃省张掖市高台县', '620700'),
       ('620725', '山丹县', 3, '620700', '甘肃省张掖市山丹县', '620700'),
       ('620800', '平凉市', 2, '620000', '甘肃省平凉市', '620800'),
       ('620802', '崆峒区', 3, '620800', '甘肃省平凉市崆峒区', '620800');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('620821', '泾川县', 3, '620800', '甘肃省平凉市泾川县', '620800'),
       ('620822', '灵台县', 3, '620800', '甘肃省平凉市灵台县', '620800'),
       ('620823', '崇信县', 3, '620800', '甘肃省平凉市崇信县', '620800'),
       ('620825', '庄浪县', 3, '620800', '甘肃省平凉市庄浪县', '620800'),
       ('620826', '静宁县', 3, '620800', '甘肃省平凉市静宁县', '620800'),
       ('620881', '华亭市', 3, '620800', '甘肃省平凉市华亭市', '620800'),
       ('620900', '酒泉市', 2, '620000', '甘肃省酒泉市', '620900'),
       ('620902', '肃州区', 3, '620900', '甘肃省酒泉市肃州区', '620900'),
       ('620921', '金塔县', 3, '620900', '甘肃省酒泉市金塔县', '620900'),
       ('620922', '瓜州县', 3, '620900', '甘肃省酒泉市瓜州县', '620900');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('620923', '肃北蒙古族自治县', 3, '620900', '甘肃省酒泉市肃北蒙古族自治县', '620900'),
       ('620924', '阿克塞哈萨克族自治县', 3, '620900', '甘肃省酒泉市阿克塞哈萨克族自治县', '620900'),
       ('620981', '玉门市', 3, '620900', '甘肃省酒泉市玉门市', '620900'),
       ('620982', '敦煌市', 3, '620900', '甘肃省酒泉市敦煌市', '620900'),
       ('621000', '庆阳市', 2, '620000', '甘肃省庆阳市', '621000'),
       ('621002', '西峰区', 3, '621000', '甘肃省庆阳市西峰区', '621000'),
       ('621021', '庆城县', 3, '621000', '甘肃省庆阳市庆城县', '621000'),
       ('621022', '环县', 3, '621000', '甘肃省庆阳市环县', '621000'),
       ('621023', '华池县', 3, '621000', '甘肃省庆阳市华池县', '621000'),
       ('621024', '合水县', 3, '621000', '甘肃省庆阳市合水县', '621000');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('621025', '正宁县', 3, '621000', '甘肃省庆阳市正宁县', '621000'),
       ('621026', '宁县', 3, '621000', '甘肃省庆阳市宁县', '621000'),
       ('621027', '镇原县', 3, '621000', '甘肃省庆阳市镇原县', '621000'),
       ('621100', '定西市', 2, '620000', '甘肃省定西市', '621100'),
       ('621102', '安定区', 3, '621100', '甘肃省定西市安定区', '621100'),
       ('621121', '通渭县', 3, '621100', '甘肃省定西市通渭县', '621100'),
       ('621122', '陇西县', 3, '621100', '甘肃省定西市陇西县', '621100'),
       ('621123', '渭源县', 3, '621100', '甘肃省定西市渭源县', '621100'),
       ('621124', '临洮县', 3, '621100', '甘肃省定西市临洮县', '621100'),
       ('621125', '漳县', 3, '621100', '甘肃省定西市漳县', '621100');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('621126', '岷县', 3, '621100', '甘肃省定西市岷县', '621100'),
       ('621200', '陇南市', 2, '620000', '甘肃省陇南市', '621200'),
       ('621202', '武都区', 3, '621200', '甘肃省陇南市武都区', '621200'),
       ('621221', '成县', 3, '621200', '甘肃省陇南市成县', '621200'),
       ('621222', '文县', 3, '621200', '甘肃省陇南市文县', '621200'),
       ('621223', '宕昌县', 3, '621200', '甘肃省陇南市宕昌县', '621200'),
       ('621224', '康县', 3, '621200', '甘肃省陇南市康县', '621200'),
       ('621225', '西和县', 3, '621200', '甘肃省陇南市西和县', '621200'),
       ('621226', '礼县', 3, '621200', '甘肃省陇南市礼县', '621200'),
       ('621227', '徽县', 3, '621200', '甘肃省陇南市徽县', '621200');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('621228', '两当县', 3, '621200', '甘肃省陇南市两当县', '621200'),
       ('622900', '临夏回族自治州', 2, '620000', '甘肃省临夏回族自治州', '622900'),
       ('622901', '临夏市', 3, '622900', '甘肃省临夏回族自治州临夏市', '622900'),
       ('622921', '临夏县', 3, '622900', '甘肃省临夏回族自治州临夏县', '622900'),
       ('622922', '康乐县', 3, '622900', '甘肃省临夏回族自治州康乐县', '622900'),
       ('622923', '永靖县', 3, '622900', '甘肃省临夏回族自治州永靖县', '622900'),
       ('622924', '广河县', 3, '622900', '甘肃省临夏回族自治州广河县', '622900'),
       ('622925', '和政县', 3, '622900', '甘肃省临夏回族自治州和政县', '622900'),
       ('622926', '东乡族自治县', 3, '622900', '甘肃省临夏回族自治州东乡族自治县', '622900'),
       ('622927', '积石山保安族东乡族撒拉族自治县', 3, '622900', '甘肃省临夏回族自治州积石山保安族东乡族撒拉族自治县',
        '622900');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('623000', '甘南藏族自治州', 2, '620000', '甘肃省甘南藏族自治州', '623000'),
       ('623001', '合作市', 3, '623000', '甘肃省甘南藏族自治州合作市', '623000'),
       ('623021', '临潭县', 3, '623000', '甘肃省甘南藏族自治州临潭县', '623000'),
       ('623022', '卓尼县', 3, '623000', '甘肃省甘南藏族自治州卓尼县', '623000'),
       ('623023', '舟曲县', 3, '623000', '甘肃省甘南藏族自治州舟曲县', '623000'),
       ('623024', '迭部县', 3, '623000', '甘肃省甘南藏族自治州迭部县', '623000'),
       ('623025', '玛曲县', 3, '623000', '甘肃省甘南藏族自治州玛曲县', '623000'),
       ('623026', '碌曲县', 3, '623000', '甘肃省甘南藏族自治州碌曲县', '623000'),
       ('623027', '夏河县', 3, '623000', '甘肃省甘南藏族自治州夏河县', '623000'),
       ('630000', '青海省', 1, NULL, '青海省', '630000');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('630100', '西宁市', 2, '630000', '青海省西宁市', '630100'),
       ('630102', '城东区', 3, '630100', '青海省西宁市城东区', '630100'),
       ('630103', '城中区', 3, '630100', '青海省西宁市城中区', '630100'),
       ('630104', '城西区', 3, '630100', '青海省西宁市城西区', '630100'),
       ('630105', '城北区', 3, '630100', '青海省西宁市城北区', '630100'),
       ('630106', '湟中区', 3, '630100', '青海省西宁市湟中区', '630100'),
       ('630121', '大通回族土族自治县', 3, '630100', '青海省西宁市大通回族土族自治县', '630100'),
       ('630123', '湟源县', 3, '630100', '青海省西宁市湟源县', '630100'),
       ('630200', '海东市', 2, '630000', '青海省海东市', '630200'),
       ('630202', '乐都区', 3, '630200', '青海省海东市乐都区', '630200');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('630203', '平安区', 3, '630200', '青海省海东市平安区', '630200'),
       ('630222', '民和回族土族自治县', 3, '630200', '青海省海东市民和回族土族自治县', '630200'),
       ('630223', '互助土族自治县', 3, '630200', '青海省海东市互助土族自治县', '630200'),
       ('630224', '化隆回族自治县', 3, '630200', '青海省海东市化隆回族自治县', '630200'),
       ('630225', '循化撒拉族自治县', 3, '630200', '青海省海东市循化撒拉族自治县', '630200'),
       ('632200', '海北藏族自治州', 2, '630000', '青海省海北藏族自治州', '632200'),
       ('632221', '门源回族自治县', 3, '632200', '青海省海北藏族自治州门源回族自治县', '632200'),
       ('632222', '祁连县', 3, '632200', '青海省海北藏族自治州祁连县', '632200'),
       ('632223', '海晏县', 3, '632200', '青海省海北藏族自治州海晏县', '632200'),
       ('632224', '刚察县', 3, '632200', '青海省海北藏族自治州刚察县', '632200');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('632300', '黄南藏族自治州', 2, '630000', '青海省黄南藏族自治州', '632300'),
       ('632301', '同仁市', 3, '632300', '青海省黄南藏族自治州同仁市', '632300'),
       ('632322', '尖扎县', 3, '632300', '青海省黄南藏族自治州尖扎县', '632300'),
       ('632323', '泽库县', 3, '632300', '青海省黄南藏族自治州泽库县', '632300'),
       ('632324', '河南蒙古族自治县', 3, '632300', '青海省黄南藏族自治州河南蒙古族自治县', '632300'),
       ('632500', '海南藏族自治州', 2, '630000', '青海省海南藏族自治州', '632500'),
       ('632521', '共和县', 3, '632500', '青海省海南藏族自治州共和县', '632500'),
       ('632522', '同德县', 3, '632500', '青海省海南藏族自治州同德县', '632500'),
       ('632523', '贵德县', 3, '632500', '青海省海南藏族自治州贵德县', '632500'),
       ('632524', '兴海县', 3, '632500', '青海省海南藏族自治州兴海县', '632500');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('632525', '贵南县', 3, '632500', '青海省海南藏族自治州贵南县', '632500'),
       ('632600', '果洛藏族自治州', 2, '630000', '青海省果洛藏族自治州', '632600'),
       ('632621', '玛沁县', 3, '632600', '青海省果洛藏族自治州玛沁县', '632600'),
       ('632622', '班玛县', 3, '632600', '青海省果洛藏族自治州班玛县', '632600'),
       ('632623', '甘德县', 3, '632600', '青海省果洛藏族自治州甘德县', '632600'),
       ('632624', '达日县', 3, '632600', '青海省果洛藏族自治州达日县', '632600'),
       ('632625', '久治县', 3, '632600', '青海省果洛藏族自治州久治县', '632600'),
       ('632626', '玛多县', 3, '632600', '青海省果洛藏族自治州玛多县', '632600'),
       ('632700', '玉树藏族自治州', 2, '630000', '青海省玉树藏族自治州', '632700'),
       ('632701', '玉树市', 3, '632700', '青海省玉树藏族自治州玉树市', '632700');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('632722', '杂多县', 3, '632700', '青海省玉树藏族自治州杂多县', '632700'),
       ('632723', '称多县', 3, '632700', '青海省玉树藏族自治州称多县', '632700'),
       ('632724', '治多县', 3, '632700', '青海省玉树藏族自治州治多县', '632700'),
       ('632725', '囊谦县', 3, '632700', '青海省玉树藏族自治州囊谦县', '632700'),
       ('632726', '曲麻莱县', 3, '632700', '青海省玉树藏族自治州曲麻莱县', '632700'),
       ('632800', '海西蒙古族藏族自治州', 2, '630000', '青海省海西蒙古族藏族自治州', '632800'),
       ('632801', '格尔木市', 3, '632800', '青海省海西蒙古族藏族自治州格尔木市', '632800'),
       ('632802', '德令哈市', 3, '632800', '青海省海西蒙古族藏族自治州德令哈市', '632800'),
       ('632803', '茫崖市', 3, '632800', '青海省海西蒙古族藏族自治州茫崖市', '632800'),
       ('632821', '乌兰县', 3, '632800', '青海省海西蒙古族藏族自治州乌兰县', '632800');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('632822', '都兰县', 3, '632800', '青海省海西蒙古族藏族自治州都兰县', '632800'),
       ('632823', '天峻县', 3, '632800', '青海省海西蒙古族藏族自治州天峻县', '632800'),
       ('632825', '海西蒙古族藏族自治州直辖', 3, '632800', '青海省海西蒙古族藏族自治州海西蒙古族藏族自治州直辖',
        '632800'),
       ('640000', '宁夏回族自治区', 1, NULL, '宁夏回族自治区', '640000'),
       ('640100', '银川市', 2, '640000', '宁夏回族自治区银川市', '640100'),
       ('640104', '兴庆区', 3, '640100', '宁夏回族自治区银川市兴庆区', '640100'),
       ('640105', '西夏区', 3, '640100', '宁夏回族自治区银川市西夏区', '640100'),
       ('640106', '金凤区', 3, '640100', '宁夏回族自治区银川市金凤区', '640100'),
       ('640121', '永宁县', 3, '640100', '宁夏回族自治区银川市永宁县', '640100'),
       ('640122', '贺兰县', 3, '640100', '宁夏回族自治区银川市贺兰县', '640100');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('640181', '灵武市', 3, '640100', '宁夏回族自治区银川市灵武市', '640100'),
       ('640200', '石嘴山市', 2, '640000', '宁夏回族自治区石嘴山市', '640200'),
       ('640202', '大武口区', 3, '640200', '宁夏回族自治区石嘴山市大武口区', '640200'),
       ('640205', '惠农区', 3, '640200', '宁夏回族自治区石嘴山市惠农区', '640200'),
       ('640221', '平罗县', 3, '640200', '宁夏回族自治区石嘴山市平罗县', '640200'),
       ('640300', '吴忠市', 2, '640000', '宁夏回族自治区吴忠市', '640300'),
       ('640302', '利通区', 3, '640300', '宁夏回族自治区吴忠市利通区', '640300'),
       ('640303', '红寺堡区', 3, '640300', '宁夏回族自治区吴忠市红寺堡区', '640300'),
       ('640323', '盐池县', 3, '640300', '宁夏回族自治区吴忠市盐池县', '640300'),
       ('640324', '同心县', 3, '640300', '宁夏回族自治区吴忠市同心县', '640300');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('640381', '青铜峡市', 3, '640300', '宁夏回族自治区吴忠市青铜峡市', '640300'),
       ('640400', '固原市', 2, '640000', '宁夏回族自治区固原市', '640400'),
       ('640402', '原州区', 3, '640400', '宁夏回族自治区固原市原州区', '640400'),
       ('640422', '西吉县', 3, '640400', '宁夏回族自治区固原市西吉县', '640400'),
       ('640423', '隆德县', 3, '640400', '宁夏回族自治区固原市隆德县', '640400'),
       ('640424', '泾源县', 3, '640400', '宁夏回族自治区固原市泾源县', '640400'),
       ('640425', '彭阳县', 3, '640400', '宁夏回族自治区固原市彭阳县', '640400'),
       ('640500', '中卫市', 2, '640000', '宁夏回族自治区中卫市', '640500'),
       ('640502', '沙坡头区', 3, '640500', '宁夏回族自治区中卫市沙坡头区', '640500'),
       ('640521', '中宁县', 3, '640500', '宁夏回族自治区中卫市中宁县', '640500');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('640522', '海原县', 3, '640500', '宁夏回族自治区中卫市海原县', '640500'),
       ('650000', '新疆维吾尔自治区', 1, NULL, '新疆维吾尔自治区', '650000'),
       ('650100', '乌鲁木齐市', 2, '650000', '新疆维吾尔自治区乌鲁木齐市', '650100'),
       ('650102', '天山区', 3, '650100', '新疆维吾尔自治区乌鲁木齐市天山区', '650100'),
       ('650103', '沙依巴克区', 3, '650100', '新疆维吾尔自治区乌鲁木齐市沙依巴克区', '650100'),
       ('650104', '新市区', 3, '650100', '新疆维吾尔自治区乌鲁木齐市新市区', '650100'),
       ('650105', '水磨沟区', 3, '650100', '新疆维吾尔自治区乌鲁木齐市水磨沟区', '650100'),
       ('650106', '头屯河区', 3, '650100', '新疆维吾尔自治区乌鲁木齐市头屯河区', '650100'),
       ('650107', '达坂城区', 3, '650100', '新疆维吾尔自治区乌鲁木齐市达坂城区', '650100'),
       ('650109', '米东区', 3, '650100', '新疆维吾尔自治区乌鲁木齐市米东区', '650100');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('650121', '乌鲁木齐县', 3, '650100', '新疆维吾尔自治区乌鲁木齐市乌鲁木齐县', '650100'),
       ('650200', '克拉玛依市', 2, '650000', '新疆维吾尔自治区克拉玛依市', '650200'),
       ('650202', '独山子区', 3, '650200', '新疆维吾尔自治区克拉玛依市独山子区', '650200'),
       ('650203', '克拉玛依区', 3, '650200', '新疆维吾尔自治区克拉玛依市克拉玛依区', '650200'),
       ('650204', '白碱滩区', 3, '650200', '新疆维吾尔自治区克拉玛依市白碱滩区', '650200'),
       ('650205', '乌尔禾区', 3, '650200', '新疆维吾尔自治区克拉玛依市乌尔禾区', '650200'),
       ('650400', '吐鲁番市', 2, '650000', '新疆维吾尔自治区吐鲁番市', '650400'),
       ('650402', '高昌区', 3, '650400', '新疆维吾尔自治区吐鲁番市高昌区', '650400'),
       ('650421', '鄯善县', 3, '650400', '新疆维吾尔自治区吐鲁番市鄯善县', '650400'),
       ('650422', '托克逊县', 3, '650400', '新疆维吾尔自治区吐鲁番市托克逊县', '650400');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('650500', '哈密市', 2, '650000', '新疆维吾尔自治区哈密市', '650500'),
       ('650502', '伊州区', 3, '650500', '新疆维吾尔自治区哈密市伊州区', '650500'),
       ('650521', '巴里坤哈萨克自治县', 3, '650500', '新疆维吾尔自治区哈密市巴里坤哈萨克自治县', '650500'),
       ('650522', '伊吾县', 3, '650500', '新疆维吾尔自治区哈密市伊吾县', '650500'),
       ('652300', '昌吉回族自治州', 2, '650000', '新疆维吾尔自治区昌吉回族自治州', '652300'),
       ('652301', '昌吉市', 3, '652300', '新疆维吾尔自治区昌吉回族自治州昌吉市', '652300'),
       ('652302', '阜康市', 3, '652300', '新疆维吾尔自治区昌吉回族自治州阜康市', '652300'),
       ('652323', '呼图壁县', 3, '652300', '新疆维吾尔自治区昌吉回族自治州呼图壁县', '652300'),
       ('652324', '玛纳斯县', 3, '652300', '新疆维吾尔自治区昌吉回族自治州玛纳斯县', '652300'),
       ('652325', '奇台县', 3, '652300', '新疆维吾尔自治区昌吉回族自治州奇台县', '652300');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('652327', '吉木萨尔县', 3, '652300', '新疆维吾尔自治区昌吉回族自治州吉木萨尔县', '652300'),
       ('652328', '木垒哈萨克自治县', 3, '652300', '新疆维吾尔自治区昌吉回族自治州木垒哈萨克自治县', '652300'),
       ('652700', '博尔塔拉蒙古自治州', 2, '650000', '新疆维吾尔自治区博尔塔拉蒙古自治州', '652700'),
       ('652701', '博乐市', 3, '652700', '新疆维吾尔自治区博尔塔拉蒙古自治州博乐市', '652700'),
       ('652702', '阿拉山口市', 3, '652700', '新疆维吾尔自治区博尔塔拉蒙古自治州阿拉山口市', '652700'),
       ('652722', '精河县', 3, '652700', '新疆维吾尔自治区博尔塔拉蒙古自治州精河县', '652700'),
       ('652723', '温泉县', 3, '652700', '新疆维吾尔自治区博尔塔拉蒙古自治州温泉县', '652700'),
       ('652800', '巴音郭楞蒙古自治州', 2, '650000', '新疆维吾尔自治区巴音郭楞蒙古自治州', '652800'),
       ('652801', '库尔勒市', 3, '652800', '新疆维吾尔自治区巴音郭楞蒙古自治州库尔勒市', '652800'),
       ('652822', '轮台县', 3, '652800', '新疆维吾尔自治区巴音郭楞蒙古自治州轮台县', '652800');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('652823', '尉犁县', 3, '652800', '新疆维吾尔自治区巴音郭楞蒙古自治州尉犁县', '652800'),
       ('652824', '若羌县', 3, '652800', '新疆维吾尔自治区巴音郭楞蒙古自治州若羌县', '652800'),
       ('652825', '且末县', 3, '652800', '新疆维吾尔自治区巴音郭楞蒙古自治州且末县', '652800'),
       ('652826', '焉耆回族自治县', 3, '652800', '新疆维吾尔自治区巴音郭楞蒙古自治州焉耆回族自治县', '652800'),
       ('652827', '和静县', 3, '652800', '新疆维吾尔自治区巴音郭楞蒙古自治州和静县', '652800'),
       ('652828', '和硕县', 3, '652800', '新疆维吾尔自治区巴音郭楞蒙古自治州和硕县', '652800'),
       ('652829', '博湖县', 3, '652800', '新疆维吾尔自治区巴音郭楞蒙古自治州博湖县', '652800'),
       ('652900', '阿克苏地区', 2, '650000', '新疆维吾尔自治区阿克苏地区', '652900'),
       ('652901', '阿克苏市', 3, '652900', '新疆维吾尔自治区阿克苏地区阿克苏市', '652900'),
       ('652902', '库车市', 3, '652900', '新疆维吾尔自治区阿克苏地区库车市', '652900');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('652922', '温宿县', 3, '652900', '新疆维吾尔自治区阿克苏地区温宿县', '652900'),
       ('652924', '沙雅县', 3, '652900', '新疆维吾尔自治区阿克苏地区沙雅县', '652900'),
       ('652925', '新和县', 3, '652900', '新疆维吾尔自治区阿克苏地区新和县', '652900'),
       ('652926', '拜城县', 3, '652900', '新疆维吾尔自治区阿克苏地区拜城县', '652900'),
       ('652927', '乌什县', 3, '652900', '新疆维吾尔自治区阿克苏地区乌什县', '652900'),
       ('652928', '阿瓦提县', 3, '652900', '新疆维吾尔自治区阿克苏地区阿瓦提县', '652900'),
       ('652929', '柯坪县', 3, '652900', '新疆维吾尔自治区阿克苏地区柯坪县', '652900'),
       ('653000', '克孜勒苏柯尔克孜自治州', 2, '650000', '新疆维吾尔自治区克孜勒苏柯尔克孜自治州', '653000'),
       ('653001', '阿图什市', 3, '653000', '新疆维吾尔自治区克孜勒苏柯尔克孜自治州阿图什市', '653000'),
       ('653022', '阿克陶县', 3, '653000', '新疆维吾尔自治区克孜勒苏柯尔克孜自治州阿克陶县', '653000');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('653023', '阿合奇县', 3, '653000', '新疆维吾尔自治区克孜勒苏柯尔克孜自治州阿合奇县', '653000'),
       ('653024', '乌恰县', 3, '653000', '新疆维吾尔自治区克孜勒苏柯尔克孜自治州乌恰县', '653000'),
       ('653100', '喀什地区', 2, '650000', '新疆维吾尔自治区喀什地区', '653100'),
       ('653101', '喀什市', 3, '653100', '新疆维吾尔自治区喀什地区喀什市', '653100'),
       ('653121', '疏附县', 3, '653100', '新疆维吾尔自治区喀什地区疏附县', '653100'),
       ('653122', '疏勒县', 3, '653100', '新疆维吾尔自治区喀什地区疏勒县', '653100'),
       ('653123', '英吉沙县', 3, '653100', '新疆维吾尔自治区喀什地区英吉沙县', '653100'),
       ('653124', '泽普县', 3, '653100', '新疆维吾尔自治区喀什地区泽普县', '653100'),
       ('653125', '莎车县', 3, '653100', '新疆维吾尔自治区喀什地区莎车县', '653100'),
       ('653126', '叶城县', 3, '653100', '新疆维吾尔自治区喀什地区叶城县', '653100');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('653127', '麦盖提县', 3, '653100', '新疆维吾尔自治区喀什地区麦盖提县', '653100'),
       ('653128', '岳普湖县', 3, '653100', '新疆维吾尔自治区喀什地区岳普湖县', '653100'),
       ('653129', '伽师县', 3, '653100', '新疆维吾尔自治区喀什地区伽师县', '653100'),
       ('653130', '巴楚县', 3, '653100', '新疆维吾尔自治区喀什地区巴楚县', '653100'),
       ('653131', '塔什库尔干塔吉克自治县', 3, '653100', '新疆维吾尔自治区喀什地区塔什库尔干塔吉克自治县', '653100'),
       ('653200', '和田地区', 2, '650000', '新疆维吾尔自治区和田地区', '653200'),
       ('653201', '和田市', 3, '653200', '新疆维吾尔自治区和田地区和田市', '653200'),
       ('653221', '和田县', 3, '653200', '新疆维吾尔自治区和田地区和田县', '653200'),
       ('653222', '墨玉县', 3, '653200', '新疆维吾尔自治区和田地区墨玉县', '653200'),
       ('653223', '皮山县', 3, '653200', '新疆维吾尔自治区和田地区皮山县', '653200');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('653224', '洛浦县', 3, '653200', '新疆维吾尔自治区和田地区洛浦县', '653200'),
       ('653225', '策勒县', 3, '653200', '新疆维吾尔自治区和田地区策勒县', '653200'),
       ('653226', '于田县', 3, '653200', '新疆维吾尔自治区和田地区于田县', '653200'),
       ('653227', '民丰县', 3, '653200', '新疆维吾尔自治区和田地区民丰县', '653200'),
       ('654000', '伊犁哈萨克自治州', 2, '650000', '新疆维吾尔自治区伊犁哈萨克自治州', '654000'),
       ('654002', '伊宁市', 3, '654000', '新疆维吾尔自治区伊犁哈萨克自治州伊宁市', '654000'),
       ('654003', '奎屯市', 3, '654000', '新疆维吾尔自治区伊犁哈萨克自治州奎屯市', '654000'),
       ('654004', '霍尔果斯市', 3, '654000', '新疆维吾尔自治区伊犁哈萨克自治州霍尔果斯市', '654000'),
       ('654021', '伊宁县', 3, '654000', '新疆维吾尔自治区伊犁哈萨克自治州伊宁县', '654000'),
       ('654022', '察布查尔锡伯自治县', 3, '654000', '新疆维吾尔自治区伊犁哈萨克自治州察布查尔锡伯自治县', '654000');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('654023', '霍城县', 3, '654000', '新疆维吾尔自治区伊犁哈萨克自治州霍城县', '654000'),
       ('654024', '巩留县', 3, '654000', '新疆维吾尔自治区伊犁哈萨克自治州巩留县', '654000'),
       ('654025', '新源县', 3, '654000', '新疆维吾尔自治区伊犁哈萨克自治州新源县', '654000'),
       ('654026', '昭苏县', 3, '654000', '新疆维吾尔自治区伊犁哈萨克自治州昭苏县', '654000'),
       ('654027', '特克斯县', 3, '654000', '新疆维吾尔自治区伊犁哈萨克自治州特克斯县', '654000'),
       ('654028', '尼勒克县', 3, '654000', '新疆维吾尔自治区伊犁哈萨克自治州尼勒克县', '654000'),
       ('654200', '塔城地区', 2, '650000', '新疆维吾尔自治区塔城地区', '654200'),
       ('654201', '塔城市', 3, '654200', '新疆维吾尔自治区塔城地区塔城市', '654200'),
       ('654202', '乌苏市', 3, '654200', '新疆维吾尔自治区塔城地区乌苏市', '654200'),
       ('654221', '额敏县', 3, '654200', '新疆维吾尔自治区塔城地区额敏县', '654200');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('654223', '沙湾市', 3, '654200', '新疆维吾尔自治区塔城地区沙湾市', '654200'),
       ('654224', '托里县', 3, '654200', '新疆维吾尔自治区塔城地区托里县', '654200'),
       ('654225', '裕民县', 3, '654200', '新疆维吾尔自治区塔城地区裕民县', '654200'),
       ('654226', '和布克赛尔蒙古自治县', 3, '654200', '新疆维吾尔自治区塔城地区和布克赛尔蒙古自治县', '654200'),
       ('654300', '阿勒泰地区', 2, '650000', '新疆维吾尔自治区阿勒泰地区', '654300'),
       ('654301', '阿勒泰市', 3, '654300', '新疆维吾尔自治区阿勒泰地区阿勒泰市', '654300'),
       ('654321', '布尔津县', 3, '654300', '新疆维吾尔自治区阿勒泰地区布尔津县', '654300'),
       ('654322', '富蕴县', 3, '654300', '新疆维吾尔自治区阿勒泰地区富蕴县', '654300'),
       ('654323', '福海县', 3, '654300', '新疆维吾尔自治区阿勒泰地区福海县', '654300'),
       ('654324', '哈巴河县', 3, '654300', '新疆维吾尔自治区阿勒泰地区哈巴河县', '654300');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('654325', '青河县', 3, '654300', '新疆维吾尔自治区阿勒泰地区青河县', '654300'),
       ('654326', '吉木乃县', 3, '654300', '新疆维吾尔自治区阿勒泰地区吉木乃县', '654300'),
       ('659001', '石河子市', 2, '650000', '新疆维吾尔自治区石河子市', '659001'),
       ('659002', '阿拉尔市', 2, '650000', '新疆维吾尔自治区阿拉尔市', '659002'),
       ('659003', '图木舒克市', 2, '650000', '新疆维吾尔自治区图木舒克市', '659003'),
       ('659004', '五家渠市', 2, '650000', '新疆维吾尔自治区五家渠市', '659004'),
       ('659005', '北屯市', 2, '650000', '新疆维吾尔自治区北屯市', '659005'),
       ('659006', '铁门关市', 2, '650000', '新疆维吾尔自治区铁门关市', '659006'),
       ('659007', '双河市', 2, '650000', '新疆维吾尔自治区双河市', '659007'),
       ('659008', '可克达拉市', 2, '650000', '新疆维吾尔自治区可克达拉市', '659008');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('659009', '昆玉市', 2, '650000', '新疆维吾尔自治区昆玉市', '659009'),
       ('659010', '胡杨河市', 2, '650000', '新疆维吾尔自治区胡杨河市', '659010'),
       ('710000', '台湾省', 1, NULL, '台湾省', '710000'),
       ('810000', '香港特别行政区', 1, NULL, '香港特别行政区', '810000'),
       ('810001', '中西区', 2, '810000', '香港特别行政区中西区', '810001'),
       ('810002', '湾仔区', 2, '810000', '香港特别行政区湾仔区', '810002'),
       ('810003', '东区', 2, '810000', '香港特别行政区东区', '810003'),
       ('810004', '南区', 2, '810000', '香港特别行政区南区', '810004'),
       ('810005', '油尖旺区', 2, '810000', '香港特别行政区油尖旺区', '810005'),
       ('810006', '深水埗区', 2, '810000', '香港特别行政区深水埗区', '810006');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('810007', '九龙城区', 2, '810000', '香港特别行政区九龙城区', '810007'),
       ('810008', '黄大仙区', 2, '810000', '香港特别行政区黄大仙区', '810008'),
       ('810009', '观塘区', 2, '810000', '香港特别行政区观塘区', '810009'),
       ('810010', '荃湾区', 2, '810000', '香港特别行政区荃湾区', '810010'),
       ('810011', '屯门区', 2, '810000', '香港特别行政区屯门区', '810011'),
       ('810012', '元朗区', 2, '810000', '香港特别行政区元朗区', '810012'),
       ('810013', '北区', 2, '810000', '香港特别行政区北区', '810013'),
       ('810014', '大埔区', 2, '810000', '香港特别行政区大埔区', '810014'),
       ('810015', '西贡区', 2, '810000', '香港特别行政区西贡区', '810015'),
       ('810016', '沙田区', 2, '810000', '香港特别行政区沙田区', '810016');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('810017', '葵青区', 2, '810000', '香港特别行政区葵青区', '810017'),
       ('810018', '离岛区', 2, '810000', '香港特别行政区离岛区', '810018'),
       ('820000', '澳门特别行政区', 1, NULL, '澳门特别行政区', '820000'),
       ('820001', '花地玛堂区', 2, '820000', '澳门特别行政区花地玛堂区', '820001'),
       ('820002', '花王堂区', 2, '820000', '澳门特别行政区花王堂区', '820002'),
       ('820003', '望德堂区', 2, '820000', '澳门特别行政区望德堂区', '820003'),
       ('820004', '大堂区', 2, '820000', '澳门特别行政区大堂区', '820004'),
       ('820005', '风顺堂区', 2, '820000', '澳门特别行政区风顺堂区', '820005'),
       ('820006', '嘉模堂区', 2, '820000', '澳门特别行政区嘉模堂区', '820006'),
       ('820007', '路凼填海区', 2, '820000', '澳门特别行政区路凼填海区', '820007');
INSERT INTO sys_region_code (code, name, `level`, parent_code, full_name, city_code)
VALUES ('820008', '圣方济各堂区', 2, '820000', '澳门特别行政区圣方济各堂区', '820008');
INSERT INTO sys_resource (id, tenant_id, resource_name, parent_id, resource_type, sort, `path`, component, is_external,
                          is_public, menu_status, visible, perms, icon, api_method, api_url, keep_alive, always_show,
                          redirect, remark, create_by, create_time, update_by, update_time, create_dept, client_code)
VALUES (1, 1, '系统管理', 0, 1, 1, '/system', NULL, 0, 0, 1, 1, NULL, 'ionicons5:Albums', NULL, NULL, 0, 1, NULL,
        '系统管理目录', NULL, '2025-11-12 17:41:18', 1, '2026-04-10 09:27:02', NULL, 'pc'),
       (2, 1, '用户管理', 1, 2, 1, '/system/user', 'system/user', 0, 0, 1, 1, 'system:user:list',
        'ionicons5:AccessibilityOutline', NULL, NULL, 1, 0, NULL, '用户管理菜单', NULL, '2025-11-12 17:41:18', NULL,
        '2026-03-22 17:57:19', NULL, 'pc'),
       (3, 1, '用户查询', 2, 3, 1, NULL, NULL, 0, 0, 1, 1, 'system:user:query', NULL, NULL, NULL, 0, 0, NULL,
        '用户查询按钮', NULL, '2025-11-12 17:41:18', NULL, '2025-11-12 17:41:18', NULL, 'pc'),
       (4, 1, '用户新增', 2, 3, 2, NULL, NULL, 0, 0, 1, 1, 'system:user:add', NULL, NULL, NULL, 0, 0, NULL,
        '用户新增按钮', NULL, '2025-11-12 17:41:18', NULL, '2025-11-12 17:41:18', NULL, 'pc'),
       (5, 1, '用户修改', 2, 3, 3, NULL, NULL, 0, 0, 1, 1, 'system:user:edit', NULL, NULL, NULL, 0, 0, NULL,
        '用户修改按钮', NULL, '2025-11-12 17:41:18', NULL, '2025-11-12 17:41:18', NULL, 'pc'),
       (6, 1, '用户删除', 2, 3, 4, NULL, NULL, 0, 0, 1, 1, 'system:user:remove', NULL, NULL, NULL, 0, 0, NULL,
        '用户删除按钮', NULL, '2025-11-12 17:41:18', NULL, '2025-11-12 17:41:18', NULL, 'pc'),
       (7, 1, '用户分页查询API', 2, 4, 1, NULL, NULL, 0, 0, 1, 1, 'system:user:page', NULL, 'GET', '/system/user/page',
        0, 0, NULL, '用户分页查询接口', NULL, '2025-11-12 17:41:18', NULL, '2025-11-12 17:41:18', NULL, 'pc'),
       (8, 1, '用户新增API', 2, 4, 2, NULL, NULL, 0, 0, 1, 1, 'system:user:add', NULL, 'POST', '/system/user/add', 0, 0,
        NULL, '用户新增接口', NULL, '2025-11-12 17:41:18', NULL, '2025-11-12 17:41:18', NULL, 'pc'),
       (9, 1, '角色管理', 1, 2, 2, '/system/role', 'system/role', 0, 0, 1, 1, 'system:role:list',
        'local:ai-icon:layers', NULL, NULL, 1, 0, NULL, '角色管理菜单', NULL, '2025-11-12 17:41:18', NULL,
        '2025-12-09 17:18:50', NULL, 'pc'),
       (10, 1, '角色查询', 9, 3, 1, NULL, NULL, 0, 0, 1, 1, 'system:role:query', NULL, NULL, NULL, 0, 0, NULL,
        '角色查询按钮', NULL, '2025-11-12 17:41:18', NULL, '2025-11-12 17:41:18', NULL, 'pc');
INSERT INTO sys_resource (id, tenant_id, resource_name, parent_id, resource_type, sort, `path`, component, is_external,
                          is_public, menu_status, visible, perms, icon, api_method, api_url, keep_alive, always_show,
                          redirect, remark, create_by, create_time, update_by, update_time, create_dept, client_code)
VALUES (11, 1, '角色新增', 9, 3, 2, NULL, NULL, 0, 0, 1, 1, 'system:role:add', NULL, NULL, NULL, 0, 0, NULL,
        '角色新增按钮', NULL, '2025-11-12 17:41:18', NULL, '2025-11-12 17:41:18', NULL, 'pc'),
       (12, 1, '角色修改', 9, 3, 3, NULL, NULL, 0, 0, 1, 1, 'system:role:edit', NULL, NULL, NULL, 0, 0, NULL,
        '角色修改按钮', NULL, '2025-11-12 17:41:18', NULL, '2025-11-12 17:41:18', NULL, 'pc'),
       (13, 1, '角色删除', 9, 3, 4, NULL, NULL, 0, 0, 1, 1, 'system:role:remove', NULL, NULL, NULL, 0, 0, NULL,
        '角色删除按钮', NULL, '2025-11-12 17:41:18', NULL, '2025-11-12 17:41:18', NULL, 'pc'),
       (14, 1, '组织管理', 1, 2, 3, '/system/org', 'system/org', 0, 0, 1, 1, 'system:org:list',
        'local:ai-icon:briefcase', NULL, NULL, 1, 0, NULL, '组织管理菜单', NULL, '2025-11-12 17:41:18', NULL,
        '2025-12-09 17:17:10', NULL, 'pc'),
       (15, 1, '组织查询', 14, 3, 1, NULL, NULL, 0, 0, 1, 1, 'system:org:query', NULL, NULL, NULL, 0, 0, NULL,
        '组织查询按钮', NULL, '2025-11-12 17:41:18', NULL, '2025-11-12 17:41:18', NULL, 'pc'),
       (16, 1, '组织新增', 14, 3, 2, NULL, NULL, 0, 0, 1, 1, 'system:org:add', NULL, NULL, NULL, 0, 0, NULL,
        '组织新增按钮', NULL, '2025-11-12 17:41:18', NULL, '2025-11-12 17:41:18', NULL, 'pc'),
       (17, 1, '岗位管理', 1, 2, 4, '/system/post', 'system/post', 0, 0, 1, 1, 'system:post:list', 'ionicons5:Albums',
        NULL, NULL, 1, 0, NULL, '岗位管理菜单', NULL, '2025-11-12 17:41:18', NULL, '2025-12-09 17:23:11', NULL, 'pc'),
       (18, 1, '岗位查询', 17, 3, 1, NULL, NULL, 0, 0, 1, 1, 'system:post:query', NULL, NULL, NULL, 0, 0, NULL,
        '岗位查询按钮', NULL, '2025-11-12 17:41:18', NULL, '2025-11-12 17:41:18', NULL, 'pc'),
       (20, 1, '查询案件信息', 19, 3, 1, NULL, NULL, 0, 0, 1, 1, 'system:legalCase:query', NULL, NULL, NULL, 0, 0, NULL,
        '查询案件信息按钮', NULL, '2025-12-05 14:37:17', NULL, '2025-12-09 10:28:31', NULL, 'pc'),
       (21, 1, '新增案件信息', 19, 3, 2, NULL, NULL, 0, 0, 1, 1, 'system:legalCase:add', NULL, NULL, NULL, 0, 0, NULL,
        '新增案件信息按钮', NULL, '2025-12-05 14:37:17', NULL, '2025-12-08 16:04:23', NULL, 'pc');
INSERT INTO sys_resource (id, tenant_id, resource_name, parent_id, resource_type, sort, `path`, component, is_external,
                          is_public, menu_status, visible, perms, icon, api_method, api_url, keep_alive, always_show,
                          redirect, remark, create_by, create_time, update_by, update_time, create_dept, client_code)
VALUES (22, 1, '修改案件信息', 19, 3, 3, NULL, NULL, 0, 0, 1, 1, 'system:legalCase:edit', NULL, NULL, NULL, 0, 0, NULL,
        '修改案件信息按钮', NULL, '2025-12-05 14:37:17', NULL, '2025-12-08 16:04:24', NULL, 'pc'),
       (23, 1, '删除案件信息', 19, 3, 4, NULL, NULL, 0, 0, 1, 1, 'system:legalCase:remove', NULL, NULL, NULL, 0, 0,
        NULL, '删除案件信息按钮', NULL, '2025-12-05 14:37:17', NULL, '2025-12-08 16:04:24', NULL, 'pc'),
       (24, 1, '菜单管理', 1, 2, 4, '/system/menu', 'system/menu', 0, 0, 1, 1, 'system:menu:list', 'ionicons5:Menu',
        NULL, NULL, 1, 0, NULL, '菜单管理', NULL, '2025-11-12 17:41:18', NULL, '2025-12-09 17:17:48', NULL, 'pc'),
       (26, 1, '租户管理', 1, 2, 6, '/system/tenant', 'system/tenant', 0, 0, 1, 1, NULL, 'ionicons5:MapOutline', 'GET',
        NULL, 0, 0, NULL, NULL, NULL, '2025-12-09 17:32:06', NULL, '2025-12-09 17:37:07', NULL, 'pc'),
       (27, 1, '字典管理', 44, 2, 7, '/system/dictType', 'system/dictType', 0, 0, 1, 1, NULL,
        'ionicons5:MedicalOutline', 'GET', NULL, 0, 0, NULL, NULL, NULL, '2025-12-11 16:01:06', 1,
        '2026-01-21 17:37:37', NULL, 'pc'),
       (28, 1, '字典数据', 44, 2, 8, '/system/dictData', 'system/dictData', 0, 0, 0, 0, NULL, 'ionicons5:Dice', 'GET',
        NULL, 0, 0, NULL, NULL, NULL, '2025-12-11 16:06:09', 1, '2026-03-02 16:15:24', NULL, 'pc'),
       (29, 1, '参数设置', 44, 2, 8, '/system/config', 'system/config', 0, 0, 1, 1, NULL, 'ionicons5:Apps', 'GET', NULL,
        0, 0, NULL, NULL, NULL, '2025-12-11 17:21:09', 1, '2026-01-21 17:37:43', NULL, 'pc'),
       (30, 1, '通知公告', 43, 2, 9, '/system/notice', 'system/notice', 0, 0, 1, 1, NULL,
        'ionicons5:NotificationsOutline', 'GET', NULL, 0, 0, NULL, NULL, NULL, '2025-12-12 09:34:50', 1,
        '2026-01-21 15:55:48', NULL, 'pc'),
       (31, 1, '数据权限配置', 44, 2, 5, '/system/dataScopeConfig', 'system/dataScopeConfig', 0, 0, 1, 1,
        'system:dataScope:list', 'ionicons5:HandRight', NULL, NULL, 0, 0, NULL, '数据权限配置管理', 1,
        '2026-01-20 15:06:29', 1, '2026-03-16 17:28:08', NULL, 'pc'),
       (39, 1, 'AI代码生成', 0, 1, 2, '/generator', NULL, 0, 0, 1, 1, NULL, 'ionicons5:LogoCodepen', 'GET', NULL, 0, 0,
        NULL, NULL, 1, '2026-01-20 16:50:19', 1, '2026-04-27 09:45:17', 1, 'pc');
INSERT INTO sys_resource (id, tenant_id, resource_name, parent_id, resource_type, sort, `path`, component, is_external,
                          is_public, menu_status, visible, perms, icon, api_method, api_url, keep_alive, always_show,
                          redirect, remark, create_by, create_time, update_by, update_time, create_dept, client_code)
VALUES (40, 1, '数据源配置', 39, 2, 0, '/generator/datasource', 'generator/datasource', 0, 0, 1, 1, NULL,
        'ionicons5:GridSharp', 'GET', NULL, 0, 0, NULL, NULL, 1, '2026-01-20 16:51:58', 1, '2026-01-20 16:51:58', 1,
        'pc'),
       (41, 1, '表模型管理', 39, 2, 0, '/generator/table', 'generator/table', 0, 0, 1, 1, NULL,
        'ionicons5:TabletLandscape', 'GET', NULL, 0, 0, NULL, NULL, 1, '2026-01-20 16:53:04', 1, '2026-01-20 16:53:04',
        1, 'pc'),
       (43, 1, '日常办公', 0, 1, 3, '/work', NULL, 0, 0, 1, 1, NULL, 'ionicons5:BagCheck', 'GET', NULL, 0, 0, NULL,
        NULL, 1, '2026-01-21 15:55:31', 1, '2026-01-21 15:55:31', 1, 'pc'),
       (44, 1, '配置管理', 0, 1, 4, '/config', NULL, 0, 0, 1, 1, NULL, 'ionicons5:ConstructSharp', 'GET', NULL, 0, 0,
        NULL, NULL, 1, '2026-01-21 16:24:25', 1, '2026-01-21 16:24:25', 1, 'pc'),
       (45, 1, '存储配置', 44, 2, 1, '/system/storage-config', 'system/storage-config', 0, 0, 1, 1, NULL,
        'ionicons5:FileTray', 'GET', NULL, 0, 0, NULL, NULL, 1, '2026-01-21 16:24:55', 1, '2026-01-21 16:24:55', 1,
        'pc'),
       (46, 1, '文件管理', 44, 2, 2, '/system/file-list', 'system/file-list', 0, 0, 1, 1, NULL,
        'ionicons5:FileTrayStackedSharp', 'GET', NULL, 0, 0, NULL, NULL, 1, '2026-01-21 16:25:41', 1,
        '2026-01-21 16:26:24', 1, 'pc'),
       (47, 1, 'Excel导出配置', 44, 2, 6, '/system/excel-export-config', 'system/excel-export-config', 0, 0, 1, 1, NULL,
        'ionicons5:ExitSharp', 'GET', NULL, 0, 0, NULL, NULL, 1, '2026-01-21 22:34:08', 1, '2026-01-21 22:34:08', 2,
        'pc'),
       (48, 1, '消息管理', 0, 1, 5, '/message', NULL, 0, 0, 1, 1, NULL, 'ionicons5:NotificationsCircleSharp', 'GET',
        NULL, 0, 0, NULL, NULL, 1, '2026-01-31 20:19:24', 1, '2026-02-03 17:24:35', 2, 'pc'),
       (49, 1, '模版配置', 48, 2, 0, '/message/template-list', 'message/template-list', 0, 0, 1, 1, NULL,
        'ionicons5:AddCircleSharp', 'GET', NULL, 0, 0, NULL, NULL, 1, '2026-01-31 20:19:59', 1, '2026-02-03 17:24:54',
        2, 'pc'),
       (50, 1, '消息列表', 48, 2, 0, '/message/message-list', 'message/message-list', 0, 0, 1, 1, NULL,
        'ionicons5:TabletLandscape', 'GET', NULL, 0, 0, NULL, NULL, 1, '2026-01-31 20:20:39', 1, '2026-02-03 17:26:33',
        2, 'pc');
INSERT INTO sys_resource (id, tenant_id, resource_name, parent_id, resource_type, sort, `path`, component, is_external,
                          is_public, menu_status, visible, perms, icon, api_method, api_url, keep_alive, always_show,
                          redirect, remark, create_by, create_time, update_by, update_time, create_dept, client_code)
VALUES (53, 1, '测试222', 52, 2, 0, '/test/template', 'test/template', 0, 0, 1, 1, NULL, NULL, 'GET', NULL, 0, 0, NULL,
        NULL, 1, '2026-02-03 17:25:57', 1, '2026-02-03 17:37:12', 2, 'pc'),
       (54, 1, '系统监控', 0, 1, 7, '/system/monitor', NULL, 0, 0, 1, 1, NULL, 'ionicons5:InvertModeSharp', 'GET', NULL,
        0, 0, NULL, NULL, 1, '2026-02-04 11:52:09', 1, '2026-02-04 11:52:17', 2, 'pc'),
       (55, 1, '定时任务配置', 44, 2, 0, '/system/job-config', 'system/job-config', 0, 0, 1, 1, NULL,
        'ionicons5:TimeSharp', 'GET', NULL, 0, 0, NULL, NULL, 1, '2026-02-04 11:52:50', 1, '2026-03-02 14:27:13', 2,
        'pc'),
       (56, 1, '登录日志', 54, 2, 3, '/system/login-log', 'system/login-log', 0, 0, 1, 1, NULL,
        'ionicons5:LogoFirebase', 'GET', NULL, 0, 0, NULL, NULL, 1, '2026-02-04 14:40:48', 1, '2026-03-02 16:13:25', 2,
        'pc'),
       (57, 1, '操作日志', 54, 2, 2, '/system/operation-log', 'system/operation-log', 0, 0, 1, 1, NULL,
        'ionicons5:LogoInstagram', 'GET', NULL, 0, 0, NULL, NULL, 1, '2026-02-04 14:41:26', 1, '2026-02-04 14:41:26', 2,
        'pc'),
       (58, 1, '缓存监控', 54, 2, 1, '/system/cache', 'system/cache', 0, 0, 1, 1, 'system:cache:list',
        'ionicons5:LogoGooglePlaystore', NULL, NULL, 1, 0, NULL, '缓存管理菜单', 1, '2026-02-04 15:25:05', 1,
        '2026-03-02 16:13:22', NULL, 'pc'),
       (59, 1, '缓存查询', 58, 3, 1, NULL, NULL, 0, 0, 1, 1, 'system:cache:query', NULL, NULL, NULL, 0, 0, NULL,
        '查询缓存按钮', 1, '2026-02-04 15:25:05', 1, '2026-02-04 15:25:27', NULL, 'pc'),
       (60, 1, '缓存删除', 58, 3, 2, NULL, NULL, 0, 0, 1, 1, 'system:cache:remove', NULL, NULL, NULL, 0, 0, NULL,
        '删除缓存按钮', 1, '2026-02-04 15:25:06', 1, '2026-02-04 15:25:27', NULL, 'pc'),
       (61, 1, '清空缓存', 58, 3, 3, NULL, NULL, 0, 0, 1, 1, 'system:cache:clear', NULL, NULL, NULL, 0, 0, NULL,
        '清空缓存按钮', 1, '2026-02-04 15:25:06', 1, '2026-02-04 15:25:27', NULL, 'pc'),
       (62, 1, '缓存-分页查询', 58, 4, 1, NULL, NULL, 0, 0, 1, 1, 'system:cache:page', NULL, 'GET',
        '/system/cache/page', 0, 0, NULL, '分页查询缓存列表', 1, '2026-02-04 15:25:06', 1, '2026-02-04 15:25:27', NULL,
        'pc');
INSERT INTO sys_resource (id, tenant_id, resource_name, parent_id, resource_type, sort, `path`, component, is_external,
                          is_public, menu_status, visible, perms, icon, api_method, api_url, keep_alive, always_show,
                          redirect, remark, create_by, create_time, update_by, update_time, create_dept, client_code)
VALUES (63, 1, '缓存-详情查询', 58, 4, 2, NULL, NULL, 0, 0, 1, 1, 'system:cache:getInfo', NULL, 'POST',
        '/system/cache/getInfo', 0, 0, NULL, '查询缓存详细信息', 1, '2026-02-04 15:25:06', 1, '2026-02-04 15:25:27',
        NULL, 'pc'),
       (64, 1, '缓存-删除', 58, 4, 3, NULL, NULL, 0, 0, 1, 1, 'system:cache:remove', NULL, 'POST',
        '/system/cache/remove', 0, 0, NULL, '删除单个缓存', 1, '2026-02-04 15:25:06', 1, '2026-02-04 15:25:27', NULL,
        'pc'),
       (65, 1, '缓存-批量删除', 58, 4, 4, NULL, NULL, 0, 0, 1, 1, 'system:cache:removeBatch', NULL, 'POST',
        '/system/cache/removeBatch', 0, 0, NULL, '批量删除缓存', 1, '2026-02-04 15:25:06', 1, '2026-02-04 15:25:27',
        NULL, 'pc'),
       (66, 1, '缓存-清空', 58, 4, 5, NULL, NULL, 0, 0, 1, 1, 'system:cache:clear', NULL, 'POST', '/system/cache/clear',
        0, 0, NULL, '清空所有缓存', 1, '2026-02-04 15:25:06', 1, '2026-02-04 15:25:27', NULL, 'pc'),
       (67, 1, '在线用户', 54, 2, 4, '/system/online', 'system/online', 0, 0, 1, 1, NULL, 'ionicons5:ApertureOutline',
        'GET', NULL, 0, 0, NULL, NULL, 1, '2026-02-05 12:02:09', 1, '2026-04-09 21:47:00', 2, 'pc'),
       (68, 1, 'API配置管理', 44, 2, 6, '/system/apiConfig', 'system/apiConfig', 0, 0, 1, 1, 'system:apiConfig:list',
        'ionicons5:BuildSharp', NULL, NULL, 0, 0, NULL, 'API接口配置管理', 1, '2026-02-09 16:29:33', 1,
        '2026-03-16 17:28:08', NULL, 'pc'),
       (69, 1, 'API配置管理-分页查询', 68, 4, 1, NULL, NULL, 0, 0, 1, 1, 'system:apiConfig:page', NULL, 'GET',
        '/system/apiConfig/page', 0, 0, NULL, '分页查询API配置列表', 1, '2026-02-09 16:29:33', 1, '2026-02-09 16:31:21',
        NULL, 'pc'),
       (70, 1, 'API配置管理-列表查询', 68, 4, 2, NULL, NULL, 0, 0, 1, 1, 'system:apiConfig:list', NULL, 'GET',
        '/system/apiConfig/list', 0, 0, NULL, '查询API配置列表', 1, '2026-02-09 16:29:33', 1, '2026-02-09 16:31:21',
        NULL, 'pc'),
       (71, 1, 'API配置管理-查询详情', 68, 4, 3, NULL, NULL, 0, 0, 1, 1, 'system:apiConfig:query', NULL, 'GET',
        '/system/apiConfig/getById', 0, 0, NULL, '根据ID查询API配置详情', 1, '2026-02-09 16:29:33', 1,
        '2026-02-09 16:31:21', NULL, 'pc'),
       (72, 1, 'API配置管理-新增', 68, 4, 4, NULL, NULL, 0, 0, 1, 1, 'system:apiConfig:add', NULL, 'POST',
        '/system/apiConfig/add', 0, 0, NULL, '新增API配置', 1, '2026-02-09 16:29:33', 1, '2026-02-09 16:31:21', NULL,
        'pc');
INSERT INTO sys_resource (id, tenant_id, resource_name, parent_id, resource_type, sort, `path`, component, is_external,
                          is_public, menu_status, visible, perms, icon, api_method, api_url, keep_alive, always_show,
                          redirect, remark, create_by, create_time, update_by, update_time, create_dept, client_code)
VALUES (73, 1, 'API配置管理-修改', 68, 4, 5, NULL, NULL, 0, 0, 1, 1, 'system:apiConfig:edit', NULL, 'POST',
        '/system/apiConfig/edit', 0, 0, NULL, '修改API配置', 1, '2026-02-09 16:29:33', 1, '2026-02-09 16:31:21', NULL,
        'pc'),
       (74, 1, 'API配置管理-删除', 68, 4, 6, NULL, NULL, 0, 0, 1, 1, 'system:apiConfig:remove', NULL, 'POST',
        '/system/apiConfig/remove', 0, 0, NULL, '删除API配置', 1, '2026-02-09 16:29:33', 1, '2026-02-09 16:31:21', NULL,
        'pc'),
       (76, 1, 'API配置管理-刷新缓存', 68, 4, 8, NULL, NULL, 0, 0, 1, 1, 'system:apiConfig:refresh', NULL, 'POST',
        '/apiConfig/refresh', 0, 0, NULL, '刷新API配置缓存', 1, '2026-02-09 16:29:57', 1, '2026-02-09 16:31:21', NULL,
        'pc'),
       (77, 1, '系统配置', 1, 2, 11, '/system/config-center', 'system/config-center', 0, 0, 1, 1, NULL,
        'ionicons5:ConstructOutline', 'GET', NULL, 0, 0, NULL, NULL, 1, '2026-02-26 09:49:55', 1, '2026-02-26 09:49:55',
        2, 'pc'),
       (78, 1, '服务监控', 54, 2, 0, '/system/monitor', 'system/monitor', 0, 0, 1, 1, NULL, 'ionicons5:Earth', 'GET',
        NULL, 0, 0, NULL, NULL, 1, '2026-03-02 14:28:26', 1, '2026-03-02 14:28:26', 2, 'pc'),
       (79, 1, '流程管理', 0, 1, 50, '/flow', NULL, 0, 0, 1, 1, NULL, 'ionicons5:Flower', NULL, NULL, 0, 0, NULL,
        '流程管理目录', NULL, '2026-03-15 18:32:12', 1, '2026-03-15 20:14:29', NULL, 'pc'),
       (80, 1, '我的待办', 9045, 2, 1, '/flow/todo', '/flow/todo', 0, 0, 1, 1, NULL, 'ionicons5:LogoMastodon', NULL,
        NULL, 0, 0, NULL, '我的待办任务', NULL, '2026-03-15 18:32:12', 1, '2026-03-26 17:24:18', NULL, 'pc'),
       (81, 1, '我的已办', 9045, 2, 2, '/flow/done', '/flow/done', 0, 0, 1, 1, NULL, 'ionicons5:CheckmarkDoneCircle',
        NULL, NULL, 0, 0, NULL, '我的已办任务', NULL, '2026-03-15 18:32:12', 1, '2026-03-26 17:24:24', NULL, 'pc'),
       (82, 1, '我发起的', 9045, 2, 3, '/flow/started', '/flow/started', 0, 0, 1, 1, NULL, 'ionicons5:Send', NULL, NULL,
        0, 0, NULL, '我发起的流程', NULL, '2026-03-15 18:32:12', 1, '2026-03-26 17:24:32', NULL, 'pc'),
       (83, 1, '我的抄送', 9045, 2, 4, '/flow/cc', '/flow/cc', 0, 0, 1, 1, NULL, 'ionicons5:SendOutline', NULL, NULL, 0,
        0, NULL, '我的抄送', NULL, '2026-03-15 18:32:13', 1, '2026-03-26 17:24:47', NULL, 'pc');
INSERT INTO sys_resource (id, tenant_id, resource_name, parent_id, resource_type, sort, `path`, component, is_external,
                          is_public, menu_status, visible, perms, icon, api_method, api_url, keep_alive, always_show,
                          redirect, remark, create_by, create_time, update_by, update_time, create_dept, client_code)
VALUES (85, 1, '流程模型', 79, 2, 1, '/flow/model', 'flow/model', 0, 0, 1, 1, NULL, 'ionicons5:Create', NULL, NULL, 0,
        0, NULL, '流程模型管理', NULL, '2026-03-15 18:32:13', 1, '2026-04-30 10:14:11', NULL, 'pc'),
       (86, 1, '流程分类', 79, 2, 2, '/flow/category', '/flow/category', 0, 0, 1, 1, NULL, 'ionicons5:DuplicateOutline',
        NULL, NULL, 0, 0, NULL, '流程分类管理', NULL, '2026-03-15 18:32:13', 1, '2026-04-30 10:14:17', NULL, 'pc'),
       (87, 1, '创建模型', 85, 3, 1, NULL, NULL, 0, 0, 1, 1, 'flow:model:create', NULL, NULL, NULL, 0, 0, NULL,
        '创建流程模型', NULL, '2026-03-15 18:32:13', NULL, '2026-03-15 18:32:39', NULL, 'pc'),
       (88, 1, '编辑模型', 85, 3, 2, NULL, NULL, 0, 0, 1, 1, 'flow:model:update', NULL, NULL, NULL, 0, 0, NULL,
        '编辑流程模型', NULL, '2026-03-15 18:32:13', NULL, '2026-03-15 18:32:39', NULL, 'pc'),
       (89, 1, '删除模型', 85, 3, 3, NULL, NULL, 0, 0, 1, 1, 'flow:model:delete', NULL, NULL, NULL, 0, 0, NULL,
        '删除流程模型', NULL, '2026-03-15 18:32:13', NULL, '2026-03-15 18:32:39', NULL, 'pc'),
       (90, 1, '部署模型', 85, 3, 4, NULL, NULL, 0, 0, 1, 1, 'flow:model:deploy', NULL, NULL, NULL, 0, 0, NULL,
        '部署流程模型', NULL, '2026-03-15 18:32:13', NULL, '2026-03-15 18:32:39', NULL, 'pc'),
       (91, 1, '创建分类', 86, 3, 1, NULL, NULL, 0, 0, 1, 1, 'flow:category:create', NULL, NULL, NULL, 0, 0, NULL,
        '创建流程分类', NULL, '2026-03-15 18:32:13', NULL, '2026-03-15 18:32:39', NULL, 'pc'),
       (92, 1, '编辑分类', 86, 3, 2, NULL, NULL, 0, 0, 1, 1, 'flow:category:update', NULL, NULL, NULL, 0, 0, NULL,
        '编辑流程分类', NULL, '2026-03-15 18:32:13', NULL, '2026-03-15 18:32:39', NULL, 'pc'),
       (93, 1, '删除分类', 86, 3, 3, NULL, NULL, 0, 0, 1, 1, 'flow:category:delete', NULL, NULL, NULL, 0, 0, NULL,
        '删除流程分类', NULL, '2026-03-15 18:32:13', NULL, '2026-03-15 18:32:39', NULL, 'pc'),
       (94, 1, '待办任务查询', 85, 4, 10, NULL, NULL, 0, 0, 1, 1, 'flow:task:todo', NULL, 'GET', '/api/flow/task/todo',
        0, 0, NULL, '查询待办任务', NULL, '2026-03-15 18:32:13', NULL, '2026-03-15 18:32:39', NULL, 'pc');
INSERT INTO sys_resource (id, tenant_id, resource_name, parent_id, resource_type, sort, `path`, component, is_external,
                          is_public, menu_status, visible, perms, icon, api_method, api_url, keep_alive, always_show,
                          redirect, remark, create_by, create_time, update_by, update_time, create_dept, client_code)
VALUES (95, 1, '已办任务查询', 85, 4, 11, NULL, NULL, 0, 0, 1, 1, 'flow:task:done', NULL, 'GET', '/api/flow/task/done',
        0, 0, NULL, '查询已办任务', NULL, '2026-03-15 18:32:13', NULL, '2026-03-15 18:32:39', NULL, 'pc'),
       (96, 1, '发起的流程查询', 85, 4, 12, NULL, NULL, 0, 0, 1, 1, 'flow:task:started', NULL, 'GET',
        '/api/flow/task/started', 0, 0, NULL, '查询发起的流程', NULL, '2026-03-15 18:32:13', NULL,
        '2026-03-15 18:32:39', NULL, 'pc'),
       (97, 1, '任务签收', 85, 4, 13, NULL, NULL, 0, 0, 1, 1, 'flow:task:claim', NULL, 'POST', '/api/flow/task/claim',
        0, 0, NULL, '签收任务', NULL, '2026-03-15 18:32:13', NULL, '2026-03-15 18:32:39', NULL, 'pc'),
       (98, 1, '任务审批', 85, 4, 14, NULL, NULL, 0, 0, 1, 1, 'flow:task:approve', NULL, 'POST',
        '/api/flow/task/approve', 0, 0, NULL, '审批任务', NULL, '2026-03-15 18:32:13', NULL, '2026-03-15 18:32:39',
        NULL, 'pc'),
       (99, 1, '任务驳回', 85, 4, 15, NULL, NULL, 0, 0, 1, 1, 'flow:task:reject', NULL, 'POST', '/api/flow/task/reject',
        0, 0, NULL, '驳回任务', NULL, '2026-03-15 18:32:13', NULL, '2026-03-15 18:32:39', NULL, 'pc'),
       (100, 1, '任务催办', 85, 4, 16, NULL, NULL, 0, 0, 1, 1, 'flow:task:remind', NULL, 'POST',
        '/api/flow/task/remind', 0, 0, NULL, '催办任务', NULL, '2026-03-15 18:32:13', NULL, '2026-03-15 18:32:40', NULL,
        'pc'),
       (101, 1, '流程设计', 79, 2, 3, '/flow/design', '/flow/design', 0, 0, 1, 0, NULL, 'mdi:pencil-ruler', NULL, NULL,
        0, 0, NULL, '流程设计器', NULL, '2026-03-16 16:45:04', NULL, '2026-03-16 16:45:09', NULL, 'pc'),
       (900, 1, '请假管理', 9045, 1, 90, '/leave', NULL, 0, 0, 1, 1, NULL, 'ionicons5:BarChartSharp', NULL, NULL, 0, 0,
        NULL, '请假管理目录', NULL, '2026-03-22 17:58:08', 1, '2026-03-26 17:25:13', NULL, 'pc'),
       (901, 1, '请假申请', 900, 2, 1, '/leave/apply', '/leave/apply', 0, 0, 1, 1, 'leave:apply:view',
        'ionicons5:CogSharp', NULL, NULL, 0, 0, NULL, '请假申请页面', NULL, '2026-03-22 17:58:08', 1,
        '2026-03-22 18:02:20', NULL, 'pc'),
       (902, 1, '我的请假', 900, 2, 2, '/leave/list', '/leave/list', 0, 0, 1, 1, 'leave:list:view',
        'ionicons5:BarbellSharp', NULL, NULL, 0, 0, NULL, '我的请假列表页面', NULL, '2026-03-22 17:58:08', 1,
        '2026-03-22 18:02:05', NULL, 'pc');
INSERT INTO sys_resource (id, tenant_id, resource_name, parent_id, resource_type, sort, `path`, component, is_external,
                          is_public, menu_status, visible, perms, icon, api_method, api_url, keep_alive, always_show,
                          redirect, remark, create_by, create_time, update_by, update_time, create_dept, client_code)
VALUES (9011, 1, '提交申请', 901, 3, 1, NULL, NULL, 0, 0, 1, 1, 'leave:apply:submit', NULL, NULL, NULL, 0, 0, NULL, '',
        NULL, '2026-03-22 17:58:08', NULL, '2026-03-22 17:58:53', NULL, 'pc'),
       (9012, 1, '保存草稿', 901, 3, 2, NULL, NULL, 0, 0, 1, 1, 'leave:apply:draft', NULL, NULL, NULL, 0, 0, NULL, '',
        NULL, '2026-03-22 17:58:08', NULL, '2026-03-22 17:58:53', NULL, 'pc'),
       (9021, 1, '查看详情', 902, 3, 1, NULL, NULL, 0, 0, 1, 1, 'leave:list:detail', NULL, NULL, NULL, 0, 0, NULL, '',
        NULL, '2026-03-22 17:58:08', NULL, '2026-03-22 17:58:53', NULL, 'pc'),
       (9022, 1, '撤销申请', 902, 3, 2, NULL, NULL, 0, 0, 1, 1, 'leave:list:cancel', NULL, NULL, NULL, 0, 0, NULL, '',
        NULL, '2026-03-22 17:58:08', NULL, '2026-03-22 17:58:53', NULL, 'pc'),
       (9023, 1, '删除申请', 902, 3, 3, NULL, NULL, 0, 0, 1, 1, 'leave:list:delete', NULL, NULL, NULL, 0, 0, NULL, '',
        NULL, '2026-03-22 17:58:08', NULL, '2026-03-22 17:58:53', NULL, 'pc'),
       (9031, 1, '提交请假申请', 901, 4, 1, NULL, NULL, 0, 0, 1, 1, 'leave:apply:submit', NULL, 'POST', '/leave/submit',
        0, 0, NULL, '', NULL, '2026-03-22 17:58:08', NULL, '2026-03-22 17:58:54', NULL, 'pc'),
       (9032, 1, '保存草稿', 901, 4, 2, NULL, NULL, 0, 0, 1, 1, 'leave:apply:draft', NULL, 'POST', '/leave/draft', 0, 0,
        NULL, '', NULL, '2026-03-22 17:58:08', NULL, '2026-03-22 17:58:54', NULL, 'pc'),
       (9041, 1, '获取请假详情', 902, 4, 1, NULL, NULL, 0, 0, 1, 1, 'leave:list:detail', NULL, 'GET', '/leave/detail/*',
        0, 0, NULL, '', NULL, '2026-03-22 17:58:24', NULL, '2026-03-22 17:58:54', NULL, 'pc'),
       (9042, 1, '分页查询请假列表', 902, 4, 2, NULL, NULL, 0, 0, 1, 1, 'leave:list:view', NULL, 'GET', '/leave/page',
        0, 0, NULL, '', NULL, '2026-03-22 17:58:24', NULL, '2026-03-22 17:58:54', NULL, 'pc'),
       (9043, 1, '撤销申请', 902, 4, 3, NULL, NULL, 0, 0, 1, 1, 'leave:list:cancel', NULL, 'POST', '/leave/cancel/*', 0,
        0, NULL, '', NULL, '2026-03-22 17:58:24', NULL, '2026-03-22 17:58:54', NULL, 'pc');
INSERT INTO sys_resource (id, tenant_id, resource_name, parent_id, resource_type, sort, `path`, component, is_external,
                          is_public, menu_status, visible, perms, icon, api_method, api_url, keep_alive, always_show,
                          redirect, remark, create_by, create_time, update_by, update_time, create_dept, client_code)
VALUES (9044, 1, '删除申请', 902, 4, 4, NULL, NULL, 0, 0, 1, 1, 'leave:list:delete', NULL, 'DELETE', '/leave/*', 0, 0,
        NULL, '', NULL, '2026-03-22 17:58:25', NULL, '2026-03-22 17:58:54', NULL, 'pc'),
       (9045, 1, '个人中心', 0, 1, 8, '/system/usercenter', NULL, 0, 0, 1, 1, NULL, 'ionicons5:Accessibility', 'GET',
        NULL, 0, 0, NULL, NULL, 1, '2026-03-26 17:23:58', 1, '2026-03-26 17:23:58', 2, 'pc'),
       (9046, 1, '流程监控', 85, 2, 2, '/flow/monitor', 'flow/monitor', 0, 0, 0, 0, NULL, NULL, 'GET', NULL, 0, 0, NULL,
        NULL, 1, '2026-03-26 20:19:24', 1, '2026-03-26 20:19:24', 2, 'pc'),
       (9047, 1, '三方登录配置', 44, 2, 12, '/system/socialConfig', 'system/socialConfig', 0, 0, 1, 1, NULL,
        'ionicons5:LogoWechat', 'GET', NULL, 0, 0, NULL, NULL, 1, '2026-04-01 14:56:15', 1, '2026-04-01 14:56:15', 2,
        'pc'),
       (9048, 1, '渠道配置', 48, 2, 3, '/message/messageConfig', 'message/messageConfig', 0, 0, 1, 1, NULL,
        'ionicons5:ChatboxEllipses', 'GET', NULL, 0, 0, NULL, NULL, 1, '2026-04-02 10:31:27', 1, '2026-04-02 11:20:14',
        2, 'pc'),
       (9049, 1, '消息监控', 48, 2, 5, '/message/manage', '/message/manage', 0, 0, 1, 1, NULL, 'ionicons5:FlameSharp',
        'GET', NULL, 0, 0, NULL, NULL, 1, '2026-04-02 11:20:54', 1, '2026-04-02 11:20:54', 2, 'pc'),
       (9050, 1, '业务配置', 48, 2, 6, '/message/biz-type', '/message/biz-type', 0, 0, 1, 1, NULL,
        'ionicons5:PrismSharp', 'GET', NULL, 0, 0, NULL, NULL, 1, '2026-04-02 16:28:51', 1, '2026-04-02 16:28:51', 2,
        'pc'),
       (9051, 1, '客户端管理', 1, 2, 8, '/system/client', 'system/client', 0, 0, 1, 1, NULL, 'ionicons5:LogoTableau',
        NULL, NULL, 0, 0, NULL, NULL, 1, '2026-04-07 10:43:18', 1, '2026-04-07 18:53:21', NULL, 'pc'),
       (9067, 1, 'AI管理', 0, 1, 60, '/ai', NULL, 0, 0, 1, 1, NULL, 'ionicons5:ColorWandSharp', NULL, NULL, 0, 1, NULL,
        'AI管理目录', NULL, '2026-04-17 16:19:34', 1, '2026-04-17 16:51:49', NULL, 'pc'),
       (9068, 1, '供应商管理', 9067, 2, 1, '/ai/provider-model', '/ai/provider-model', 0, 0, 1, 1, NULL,
        'ionicons5:ExtensionPuzzle', NULL, NULL, 0, 0, NULL, 'AI供应商管理', NULL, '2026-04-17 16:19:34', 1,
        '2026-04-19 20:01:45', NULL, 'pc');
INSERT INTO sys_resource (id, tenant_id, resource_name, parent_id, resource_type, sort, `path`, component, is_external,
                          is_public, menu_status, visible, perms, icon, api_method, api_url, keep_alive, always_show,
                          redirect, remark, create_by, create_time, update_by, update_time, create_dept, client_code)
VALUES (9070, 1, '供应商新增', 9068, 3, 1, NULL, NULL, 0, 0, 1, 1, 'ai:provider:add', NULL, NULL, NULL, 0, 0, NULL,
        '新增AI供应商', NULL, '2026-04-17 16:19:34', NULL, '2026-04-17 16:20:45', NULL, 'pc'),
       (9071, 1, '供应商编辑', 9068, 3, 2, NULL, NULL, 0, 0, 1, 1, 'ai:provider:edit', NULL, NULL, NULL, 0, 0, NULL,
        '编辑AI供应商', NULL, '2026-04-17 16:19:34', NULL, '2026-04-17 16:20:46', NULL, 'pc'),
       (9072, 1, '供应商删除', 9068, 3, 3, NULL, NULL, 0, 0, 1, 1, 'ai:provider:delete', NULL, NULL, NULL, 0, 0, NULL,
        '删除AI供应商', NULL, '2026-04-17 16:19:34', NULL, '2026-04-17 16:20:46', NULL, 'pc'),
       (9073, 1, '供应商测试', 9068, 3, 4, NULL, NULL, 0, 0, 1, 1, 'ai:provider:test', NULL, NULL, NULL, 0, 0, NULL,
        '测试供应商连接', NULL, '2026-04-17 16:19:34', NULL, '2026-04-17 16:20:46', NULL, 'pc'),
       (9074, 1, '模型新增', 9069, 3, 1, NULL, NULL, 0, 0, 1, 1, 'ai:model:add', NULL, NULL, NULL, 0, 0, NULL,
        '新增AI模型', NULL, '2026-04-17 16:19:34', NULL, '2026-04-17 16:20:46', NULL, 'pc'),
       (9075, 1, '模型编辑', 9069, 3, 2, NULL, NULL, 0, 0, 1, 1, 'ai:model:edit', NULL, NULL, NULL, 0, 0, NULL,
        '编辑AI模型', NULL, '2026-04-17 16:19:34', NULL, '2026-04-17 16:20:46', NULL, 'pc'),
       (9076, 1, '模型删除', 9069, 3, 3, NULL, NULL, 0, 0, 1, 1, 'ai:model:delete', NULL, NULL, NULL, 0, 0, NULL,
        '删除AI模型', NULL, '2026-04-17 16:19:34', NULL, '2026-04-17 16:20:46', NULL, 'pc'),
       (9077, 1, '会话管理', 9067, 2, 3, '/ai/session', '/ai/session', 0, 0, 1, 1, NULL, 'ionicons5:ChatboxEllipses',
        NULL, NULL, 0, 0, NULL, 'AI会话管理', NULL, '2026-04-19 19:05:28', 1, '2026-04-19 19:41:03', NULL, 'pc'),
       (9078, 1, '会话查看', 9077, 3, 1, NULL, NULL, 0, 0, 1, 1, 'ai:session:query', NULL, NULL, NULL, 0, 0, NULL,
        '查看AI会话', NULL, '2026-04-19 19:05:28', NULL, '2026-04-19 19:05:28', NULL, 'pc'),
       (9079, 1, '会话删除', 9077, 3, 2, NULL, NULL, 0, 0, 1, 1, 'ai:session:delete', NULL, NULL, NULL, 0, 0, NULL,
        '删除AI会话', NULL, '2026-04-19 19:05:28', NULL, '2026-04-19 19:05:28', NULL, 'pc');
INSERT INTO sys_resource (id, tenant_id, resource_name, parent_id, resource_type, sort, `path`, component, is_external,
                          is_public, menu_status, visible, perms, icon, api_method, api_url, keep_alive, always_show,
                          redirect, remark, create_by, create_time, update_by, update_time, create_dept, client_code)
VALUES (9080, 1, 'AI表单生编辑', 39, 2, 10, '/ai/crud-generator', 'ai/crud-generator', 0, 0, 0, 0, 'ai:crud-gen:list',
        'ionicons5:BonfireSharp', NULL, NULL, 0, 0, NULL, 'CRUD配置管理', NULL, '2026-04-20 17:46:45', 1,
        '2026-04-27 09:44:14', NULL, 'pc'),
       (9081, 1, '通用表单', 39, 2, 99, '/ai/crud-page', 'ai/crud-page', 0, 1, 0, 0, NULL, NULL, NULL, NULL, 0, 0, NULL,
        '通用表单', NULL, '2026-04-20 17:46:45', 1, '2026-04-27 09:45:29', NULL, 'pc'),
       (9085, 1, 'AI表单生成', 39, 2, 10, '/ai/crud-config', 'ai/crud-config', 0, 0, 1, 1, 'ai:crud-config:list',
        'ionicons5:ColorWand', NULL, NULL, 0, 0, NULL, 'CRUD配置管理', NULL, '2026-04-20 17:46:45', 1,
        '2026-04-27 09:44:21', NULL, 'pc'),
       (9088, 1, '模版市场', 39, 2, 13, '/ai/page-template', 'ai/page-template', 0, 0, 1, 1, 'ai:page-template:list',
        'mdi:puzzle-outline', NULL, NULL, 0, 0, NULL, 'AI页面模板管理', NULL, '2026-04-26 17:09:26', 1,
        '2026-04-27 09:44:29', NULL, 'pc'),
       (9089, 1, '员工管理测试', 9067, 2, 0, '/employee', 'employee', 0, 0, 1, 1, 'ai:crud:employee', NULL, NULL, NULL,
        0, 0, NULL, NULL, 1, '2026-04-26 17:30:35', 1, '2026-04-26 20:16:21', 2, 'pc'),
       (9090, 1, '产品管理', 9067, 2, 0, '/ai/crud-page/biz_product', 'ai/crud-page', 0, 0, 1, 1, 'ai:crud:biz_product',
        NULL, NULL, NULL, 0, 0, NULL, NULL, 1, '2026-04-26 18:07:57', 1, '2026-04-26 18:50:43', 2, 'pc'),
       (9091, 1, '行政区划', 1, 2, 11, '/system/region', 'system/region', 0, 0, 1, 1, NULL, 'ionicons5:TriangleSharp',
        'GET', NULL, 0, 0, NULL, NULL, 1, '2026-04-28 13:06:30', 1, '2026-04-28 13:06:30', 2, 'pc'),
       (9092, 1, 'APP测试', 0, 1, 0, '/app', NULL, 0, 0, 1, 1, NULL, NULL, 'GET', NULL, 0, 1, NULL, NULL, 1,
        '2026-04-29 16:25:30', 1, '2026-04-29 17:10:21', 2, 'app'),
       (9093, 1, '测试APP菜单', 9092, 2, 2, '/cac', '/cac', 0, 0, 1, 1, NULL, NULL, 'GET', NULL, 0, 0, NULL, NULL, 1,
        '2026-04-29 16:33:05', 1, '2026-04-29 16:33:05', 2, 'app');
INSERT INTO sys_role (id, tenant_id, role_name, role_key, data_scope, sort, role_status, is_system, remark, create_by,
                      create_time, update_by, update_time, create_dept)
VALUES (1, 1, '超级管理员', 'admin', 1, 1, 1, 1, '超级管理员角色，拥有所有权限', NULL, '2025-11-12 17:41:18', NULL,
        '2025-11-12 17:41:18', NULL),
       (2, 1, '普通用户', 'user', 7, 2, 1, 0, '普通用户角色，只有查询权限', NULL, '2025-11-12 17:41:18', 1,
        '2026-04-28 16:52:59', NULL),
       (3, 1, '部门管理员', 'dept_admin', 3, 3, 1, 0, '部门管理员，管理本部门数据', NULL, '2025-11-12 17:41:18', NULL,
        '2025-11-12 17:41:18', NULL),
       (4, 0, 'ceshi', '123', 2, 0, 1, 0, NULL, NULL, '2025-12-09 12:00:19', NULL, '2025-12-09 12:00:19', NULL);
INSERT INTO sys_role_resource (id, tenant_id, role_id, resource_id, create_time)
VALUES (29, 1, 3, 1, '2025-11-12 17:41:18'),
       (30, 1, 3, 2, '2025-11-12 17:41:18'),
       (31, 1, 3, 3, '2025-11-12 17:41:18'),
       (32, 1, 3, 4, '2025-11-12 17:41:18'),
       (33, 1, 3, 5, '2025-11-12 17:41:18'),
       (34, 1, 3, 14, '2025-11-12 17:41:18'),
       (35, 1, 3, 15, '2025-11-12 17:41:18'),
       (36, 1, 3, 16, '2025-11-12 17:41:18'),
       (37, 0, 4, 19, '2025-12-09 12:00:27'),
       (38, 0, 4, 20, '2025-12-09 12:00:27');
INSERT INTO sys_role_resource (id, tenant_id, role_id, resource_id, create_time)
VALUES (39, 0, 4, 21, '2025-12-09 12:00:27'),
       (40, 0, 4, 22, '2025-12-09 12:00:27'),
       (41, 0, 4, 23, '2025-12-09 12:00:27'),
       (42, 0, 4, 1, '2025-12-09 12:00:27'),
       (43, 0, 4, 2, '2025-12-09 12:00:27'),
       (44, 0, 4, 3, '2025-12-09 12:00:27'),
       (45, 0, 4, 7, '2025-12-09 12:00:27'),
       (46, 0, 4, 8, '2025-12-09 12:00:27'),
       (47, 0, 4, 4, '2025-12-09 12:00:27'),
       (48, 0, 4, 5, '2025-12-09 12:00:27');
INSERT INTO sys_role_resource (id, tenant_id, role_id, resource_id, create_time)
VALUES (49, 0, 4, 6, '2025-12-09 12:00:27'),
       (50, 0, 4, 9, '2025-12-09 12:00:27'),
       (51, 0, 4, 10, '2025-12-09 12:00:27'),
       (52, 0, 4, 11, '2025-12-09 12:00:27'),
       (53, 0, 4, 12, '2025-12-09 12:00:28'),
       (54, 0, 4, 13, '2025-12-09 12:00:28'),
       (55, 0, 4, 14, '2025-12-09 12:00:28'),
       (56, 0, 4, 15, '2025-12-09 12:00:28'),
       (57, 0, 4, 16, '2025-12-09 12:00:28'),
       (58, 0, 4, 24, '2025-12-09 12:00:28');
INSERT INTO sys_role_resource (id, tenant_id, role_id, resource_id, create_time)
VALUES (59, 0, 4, 17, '2025-12-09 12:00:28'),
       (60, 0, 4, 18, '2025-12-09 12:00:28'),
       (110, 1, 1, 3, '2025-12-09 17:32:21'),
       (111, 1, 1, 4, '2025-12-09 17:32:22'),
       (112, 1, 1, 5, '2025-12-09 17:32:22'),
       (113, 1, 1, 6, '2025-12-09 17:32:22'),
       (114, 1, 1, 7, '2025-12-09 17:32:22'),
       (115, 1, 1, 8, '2025-12-09 17:32:22'),
       (116, 1, 1, 10, '2025-12-09 17:32:22'),
       (117, 1, 1, 11, '2025-12-09 17:32:22');
INSERT INTO sys_role_resource (id, tenant_id, role_id, resource_id, create_time)
VALUES (118, 1, 1, 12, '2025-12-09 17:32:22'),
       (119, 1, 1, 13, '2025-12-09 17:32:22'),
       (120, 1, 1, 15, '2025-12-09 17:32:22'),
       (121, 1, 1, 16, '2025-12-09 17:32:22'),
       (122, 1, 1, 18, '2025-12-09 17:32:22'),
       (123, 1, 1, 24, '2025-12-09 17:32:22'),
       (124, 1, 1, 26, '2025-12-09 17:32:22'),
       (228, 0, 1, 79, '2026-03-15 18:32:13'),
       (229, 0, 1, 80, '2026-03-15 18:32:13'),
       (230, 0, 1, 81, '2026-03-15 18:32:13');
INSERT INTO sys_role_resource (id, tenant_id, role_id, resource_id, create_time)
VALUES (231, 0, 1, 82, '2026-03-15 18:32:13'),
       (232, 0, 1, 83, '2026-03-15 18:32:13'),
       (233, 0, 1, 84, '2026-03-15 18:32:13'),
       (234, 0, 1, 85, '2026-03-15 18:32:13'),
       (235, 0, 1, 86, '2026-03-15 18:32:13'),
       (236, 0, 1, 87, '2026-03-15 18:32:13'),
       (237, 0, 1, 88, '2026-03-15 18:32:13'),
       (238, 0, 1, 89, '2026-03-15 18:32:13'),
       (239, 0, 1, 90, '2026-03-15 18:32:13'),
       (240, 0, 1, 94, '2026-03-15 18:32:13');
INSERT INTO sys_role_resource (id, tenant_id, role_id, resource_id, create_time)
VALUES (241, 0, 1, 95, '2026-03-15 18:32:13'),
       (242, 0, 1, 96, '2026-03-15 18:32:13'),
       (243, 0, 1, 97, '2026-03-15 18:32:13'),
       (244, 0, 1, 98, '2026-03-15 18:32:13'),
       (245, 0, 1, 99, '2026-03-15 18:32:13'),
       (246, 0, 1, 100, '2026-03-15 18:32:13'),
       (247, 0, 1, 91, '2026-03-15 18:32:13'),
       (248, 0, 1, 92, '2026-03-15 18:32:13'),
       (249, 0, 1, 93, '2026-03-15 18:32:13'),
       (296, 1, 2, 1, '2026-03-26 17:15:21');
INSERT INTO sys_role_resource (id, tenant_id, role_id, resource_id, create_time)
VALUES (297, 1, 2, 2, '2026-03-26 17:15:21'),
       (298, 1, 2, 3, '2026-03-26 17:15:21'),
       (299, 1, 2, 4, '2026-03-26 17:15:21'),
       (300, 1, 2, 5, '2026-03-26 17:15:21'),
       (301, 1, 2, 6, '2026-03-26 17:15:21'),
       (302, 1, 2, 7, '2026-03-26 17:15:21'),
       (303, 1, 2, 8, '2026-03-26 17:15:21'),
       (304, 1, 2, 9, '2026-03-26 17:15:21'),
       (305, 1, 2, 10, '2026-03-26 17:15:21'),
       (306, 1, 2, 11, '2026-03-26 17:15:21');
INSERT INTO sys_role_resource (id, tenant_id, role_id, resource_id, create_time)
VALUES (307, 1, 2, 12, '2026-03-26 17:15:21'),
       (308, 1, 2, 13, '2026-03-26 17:15:21'),
       (309, 1, 2, 14, '2026-03-26 17:15:21'),
       (310, 1, 2, 15, '2026-03-26 17:15:22'),
       (311, 1, 2, 16, '2026-03-26 17:15:22'),
       (312, 1, 2, 17, '2026-03-26 17:15:22'),
       (313, 1, 2, 18, '2026-03-26 17:15:22'),
       (314, 1, 2, 19, '2026-03-26 17:15:22'),
       (315, 1, 2, 20, '2026-03-26 17:15:22'),
       (316, 1, 2, 21, '2026-03-26 17:15:22');
INSERT INTO sys_role_resource (id, tenant_id, role_id, resource_id, create_time)
VALUES (317, 1, 2, 24, '2026-03-26 17:15:22'),
       (318, 1, 2, 26, '2026-03-26 17:15:22'),
       (319, 1, 2, 27, '2026-03-26 17:15:22'),
       (320, 1, 2, 28, '2026-03-26 17:15:22'),
       (321, 1, 2, 29, '2026-03-26 17:15:22'),
       (322, 1, 2, 31, '2026-03-26 17:15:22'),
       (323, 1, 2, 44, '2026-03-26 17:15:22'),
       (324, 1, 2, 45, '2026-03-26 17:15:22'),
       (325, 1, 2, 46, '2026-03-26 17:15:22'),
       (326, 1, 2, 47, '2026-03-26 17:15:22');
INSERT INTO sys_role_resource (id, tenant_id, role_id, resource_id, create_time)
VALUES (327, 1, 2, 79, '2026-03-26 17:15:22'),
       (328, 1, 2, 80, '2026-03-26 17:15:22'),
       (329, 1, 2, 81, '2026-03-26 17:15:23'),
       (330, 1, 2, 82, '2026-03-26 17:15:23'),
       (331, 1, 2, 83, '2026-03-26 17:15:23'),
       (332, 1, 2, 84, '2026-03-26 17:15:23'),
       (333, 1, 2, 85, '2026-03-26 17:15:23'),
       (334, 1, 2, 87, '2026-03-26 17:15:23'),
       (335, 1, 2, 88, '2026-03-26 17:15:23'),
       (336, 1, 2, 89, '2026-03-26 17:15:23');
INSERT INTO sys_role_resource (id, tenant_id, role_id, resource_id, create_time)
VALUES (337, 1, 2, 90, '2026-03-26 17:15:23'),
       (338, 1, 2, 94, '2026-03-26 17:15:23'),
       (339, 1, 2, 95, '2026-03-26 17:15:23'),
       (340, 1, 2, 96, '2026-03-26 17:15:23'),
       (341, 1, 2, 97, '2026-03-26 17:15:23'),
       (342, 1, 2, 98, '2026-03-26 17:15:23'),
       (343, 1, 2, 99, '2026-03-26 17:15:23'),
       (344, 1, 2, 100, '2026-03-26 17:15:23'),
       (345, 1, 2, 101, '2026-03-26 17:15:23');
INSERT INTO sys_sms_config (id, config_id, supplier, access_key_id, access_key_secret, signature, template_id, weight,
                            retry_interval, max_retries, maximum, extra_config, daily_limit, minute_limit, status,
                            tenant_id, create_time, update_time, create_by, update_by, remark)
VALUES (1, 'alibaba', 'alibaba', '1231', '12313', '121', NULL, 1, 5, 0, 3,
        '{"alibaba":{"templateName":"12121","requestUrl":"dysmsapi.aliyuncs.com","action":"SendSms","version":"2017-05-25","regionId":"cn-hangzhou"}}',
        1, 2, 1, 1, '2026-04-02 10:32:21', '2026-04-02 15:02:02', NULL, NULL, NULL);
INSERT INTO sys_social_config (id, platform, platform_name, platform_logo, client_id, client_secret, redirect_uri,
                               agent_id, `scope`, status, tenant_id, remark, create_by, create_time, update_by,
                               update_time)
VALUES (3, 'GITEE', 'gitee', '3d66b8501e874b9085faa98913daf559',
        '19d4370bac595bf2ff38b01e2ce6f1f1b4c95b4c8da62fc6cefbf5006118d493',
        'c62f394c859be2d14b39298b58b52420591110b2c98f57228b1beb25a01f58c8',
        'http://81.70.22.48:8084/forge/login/callback', '', NULL, 1, 1, NULL, NULL, '2026-04-01 16:05:14', NULL,
        '2026-04-01 17:10:19');
INSERT INTO sys_tenant (id, tenant_name, contact_person, contact_phone, user_limit, tenant_status, expire_time,
                        tenant_desc, create_by, create_time, update_by, update_time, browser_icon, browser_title,
                        system_name, system_logo, system_intro, copyright_info, system_layout, system_theme,
                        theme_config, create_dept)
VALUES (1, '默认租户', '系统管理员', '13800138000', 0, 1, '2099-12-31 23:59:59', '系统默认租户', NULL,
        '2025-11-12 17:41:17', 1, '2026-04-24 15:46:47', NULL, NULL, '企业级中后台管理系统', '', NULL, NULL, 'nexus',
        '#4242F7',
        '{"primaryColor":"#4242F7","header":{"backgroundColor":"#4242F7","textColor":"#FFFFFF","height":"60px"},"topMenu":{"textColor":"#FFFFFF","textColorActive":"#060606","backgroundColorActive":"#ffffff","iconColor":"#FFFFFF","iconActiveColor":"#000204"},"sideMenu":{"backgroundColor":"#ffffff","textColor":"#FFFFFF","textColorActive":"#FFFFFF","backgroundColorActive":"#4242F7","iconColor":"#040404","iconColorActive":"#F9F9F9","width":"220px","collapsedWidth":"64px"}}',
        NULL);
INSERT INTO sys_user (id, tenant_id, username, real_name, user_type, user_client, email, phone, id_card, gender,
                      password, salt, user_status, avatar, region_code, last_login_time, last_login_ip, login_count,
                      remark, create_dept, create_by, create_time, update_by, update_time)
VALUES (1, 1, 'admin', '超级管理员', 0, NULL, 'admin@example.com', '13800138000', NULL, 1,
        '$2a$10$KRFFrq5raOH4OOCQV59yau99awPftAYgLMt0gCv5XpWH9E.6WYS/O', NULL, 1, '39bd7a7d2d654222ae98d2c0a4a5173a',
        '150000', NULL, NULL, 0, NULL, NULL, NULL, '2025-11-12 17:41:17', 1, '2026-04-30 14:07:49'),
       (2, 1, 'test', '测试用户', 2, NULL, 'test@example.com', '13800138001', NULL, 1,
        '$2a$10$wKXHZxnK7zRhTRanAltp5O1js.1UkfEoFCkDSRPxhJ4GIFX02zHqu', NULL, 1, NULL, '150100', NULL, NULL, 0, NULL,
        NULL, NULL, '2025-11-12 17:41:17', 1, '2026-04-28 16:52:43'),
       (21, 0, 'gitee_4998937', 'yaomd', 2, NULL, NULL, NULL, NULL, 0,
        '$2a$10$E6g4WTBthEb6FvphyApHy.Pe/jGrIakdspNONxTsesF1/BfzxsJui', NULL, 1,
        'https://foruda.gitee.com/avatar/1677030453322039345/4998937_yaomd_1605663300.png', NULL, NULL, NULL, 0, NULL,
        NULL, NULL, '2026-04-01 17:59:25', NULL, '2026-04-01 17:59:25'),
       (22, 0, 'gitee_5359992', 'ml331', 2, NULL, NULL, NULL, NULL, 0,
        '$2a$10$YdK6cUzJ709aWARbl3Pq3eQ.XKyKhye2K79.nPQMn63DyhkTjVIOK', NULL, 1,
        'https://gitee.com/assets/no_portrait.png', NULL, NULL, NULL, 0, NULL, NULL, NULL, '2026-04-07 17:50:00', NULL,
        '2026-04-07 17:50:00');
INSERT INTO sys_user_org (id, tenant_id, user_id, org_id, is_main, create_time)
VALUES (13, 1, 1, 2, 1, '2026-01-20 15:35:55'),
       (14, 1, 1, 3, 0, '2026-01-20 15:36:03'),
       (15, 1, 2, 5, 0, '2026-04-23 16:45:51');
INSERT INTO sys_user_post (id, tenant_id, user_id, post_id, is_main, create_time)
VALUES (1, 1, 1, 1, 1, '2025-11-12 17:41:18'),
       (2, 1, 2, 3, 1, '2025-11-12 17:41:18'),
       (3, 1, 2, 4, 0, '2025-11-12 17:41:18');
INSERT INTO sys_user_role (id, tenant_id, user_id, role_id, create_time)
VALUES (1, 1, 1, 1, '2025-11-12 17:41:18'),
       (2, 1, 2, 2, '2025-11-12 17:41:18');
INSERT INTO sys_user_social (id, user_id, platform, uuid, username, nickname, avatar, email, access_token,
                             refresh_token, expire_time, bind_time, tenant_id)
VALUES (2, 21, 'GITEE', '4998937', 'gitee_4998937', 'yaomd',
        'https://foruda.gitee.com/avatar/1677030453322039345/4998937_yaomd_1605663300.png', NULL, NULL, NULL, NULL,
        '2026-04-01 17:59:25', NULL),
       (3, 22, 'GITEE', '5359992', 'gitee_5359992', 'ml331', 'https://gitee.com/assets/no_portrait.png', NULL, NULL,
        NULL, NULL, '2026-04-07 17:50:00', NULL);
