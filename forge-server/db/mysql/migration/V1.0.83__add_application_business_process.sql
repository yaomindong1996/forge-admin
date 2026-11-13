-- 应用级业务流程编排器：流程定义、不可变版本、运行实例与节点运行记录。
-- 正式运行只读取应用发布快照；本脚本不转换任何旧触发器、流程绑定或动作 JSON。

CREATE TABLE IF NOT EXISTS ai_business_process (
  id bigint NOT NULL COMMENT '主键ID',
  tenant_id bigint NOT NULL DEFAULT 1 COMMENT '租户ID',
  application_id bigint NOT NULL COMMENT '所属业务应用ID',
  process_code varchar(128) NOT NULL COMMENT '应用内稳定流程编码，创建后不可修改',
  process_name varchar(128) NOT NULL COMMENT '业务流程名称',
  process_description varchar(500) DEFAULT NULL COMMENT '业务流程说明',
  subject_object_id bigint NOT NULL COMMENT '主业务对象ID',
  subject_object_code varchar(128) NOT NULL COMMENT '主业务对象编码',
  draft_schema_json longtext NOT NULL COMMENT 'businessProcessJson草稿，不保存敏感配置',
  draft_schema_hash char(64) NOT NULL COMMENT '规范化草稿SHA-256摘要',
  design_status varchar(32) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/VALIDATED/PUBLISHED/CHANGED',
  current_version int NOT NULL DEFAULT 0 COMMENT '当前已生成流程版本号',
  published_version int DEFAULT NULL COMMENT '当前应用快照引用的流程版本号',
  status tinyint NOT NULL DEFAULT 1 COMMENT '状态：1启用 0停用',
  legacy_source_type varchar(32) DEFAULT NULL COMMENT '旧来源类型：TRIGGER/FLOW_BINDING/AUTOMATION_ACTION',
  legacy_source_id varchar(128) DEFAULT NULL COMMENT '旧来源稳定ID',
  del_flag bigint NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0正常，删除后写主键',
  create_by bigint DEFAULT NULL COMMENT '创建人',
  create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  create_dept bigint DEFAULT NULL COMMENT '创建部门',
  update_by bigint DEFAULT NULL COMMENT '更新人',
  update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_ai_business_process_code_active (
    tenant_id, application_id, process_code, del_flag
  ),
  UNIQUE KEY uk_ai_business_process_legacy_active (
    tenant_id, legacy_source_type, legacy_source_id, del_flag
  ),
  KEY idx_ai_business_process_application (
    tenant_id, application_id, status, design_status, update_time, del_flag
  ),
  KEY idx_ai_business_process_subject (
    tenant_id, subject_object_id, status, del_flag
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='应用级业务流程定义与设计草稿';

CREATE TABLE IF NOT EXISTS ai_business_process_version (
  id bigint NOT NULL COMMENT '主键ID',
  tenant_id bigint NOT NULL DEFAULT 1 COMMENT '租户ID',
  application_id bigint NOT NULL COMMENT '发布时所属业务应用ID',
  process_id bigint NOT NULL COMMENT '业务流程定义ID',
  process_code varchar(128) NOT NULL COMMENT '发布时流程编码',
  version_no int NOT NULL COMMENT '流程内单调版本号',
  application_version int NOT NULL COMMENT '生成该版本的应用版本号',
  publish_run_id bigint DEFAULT NULL COMMENT '应用协调发布运行单ID',
  schema_version varchar(16) NOT NULL COMMENT 'businessProcessJson协议版本',
  schema_json longtext NOT NULL COMMENT '不可变业务流程协议，不保存敏感配置',
  schema_hash char(64) NOT NULL COMMENT '规范化协议SHA-256摘要',
  dependency_snapshot_json longtext NOT NULL COMMENT '对象、审批模型、表单、动作和能力版本快照',
  publish_time datetime NOT NULL COMMENT '发布时间',
  published_by bigint DEFAULT NULL COMMENT '可信发布人ID',
  status tinyint NOT NULL DEFAULT 1 COMMENT '状态：1有效 0停用',
  del_flag bigint NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0正常，删除后写主键',
  create_by bigint DEFAULT NULL COMMENT '创建人',
  create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  create_dept bigint DEFAULT NULL COMMENT '创建部门',
  update_by bigint DEFAULT NULL COMMENT '更新人',
  update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_ai_business_process_version_active (
    tenant_id, process_id, version_no, del_flag
  ),
  UNIQUE KEY uk_ai_business_process_app_version_active (
    tenant_id, process_id, application_version, del_flag
  ),
  KEY idx_ai_business_process_version_application (
    tenant_id, application_id, application_version, status, del_flag
  ),
  KEY idx_ai_business_process_version_hash (
    tenant_id, process_id, schema_hash, del_flag
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='应用级业务流程不可变发布版本';

CREATE TABLE IF NOT EXISTS ai_business_process_run (
  id bigint NOT NULL COMMENT '主键ID',
  tenant_id bigint NOT NULL DEFAULT 1 COMMENT '租户ID',
  application_id bigint NOT NULL COMMENT '业务应用ID',
  process_id bigint NOT NULL COMMENT '业务流程定义ID',
  process_version_id bigint NOT NULL COMMENT '固定业务流程版本ID',
  process_code varchar(128) NOT NULL COMMENT '业务流程编码',
  subject_object_code varchar(128) NOT NULL COMMENT '主业务对象编码',
  subject_record_id varchar(128) NOT NULL COMMENT '业务记录ID字符串，禁止损失长整型精度',
  business_key varchar(256) NOT NULL COMMENT '业务Key：objectCode:recordId',
  trigger_type varchar(32) NOT NULL COMMENT 'MANUAL/EVENT/SCHEDULE/PROCESS_CALLBACK/EXTERNAL',
  source_event_id varchar(128) DEFAULT NULL COMMENT '来源事件稳定ID',
  idempotency_key varchar(128) NOT NULL COMMENT '流程版本内稳定幂等键',
  actor_type varchar(32) NOT NULL COMMENT 'USER/SERVICE/CALLBACK',
  actor_user_id bigint DEFAULT NULL COMMENT '可信执行用户ID',
  active_org_id bigint DEFAULT NULL COMMENT '可信当前组织ID',
  status varchar(24) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/RUNNING/WAITING/SUCCESS/FAILED/CANCELED',
  current_node_id varchar(128) DEFAULT NULL COMMENT '当前检查点节点ID',
  flow_process_instance_id varchar(128) DEFAULT NULL COMMENT '当前等待的Flowable流程实例ID',
  context_snapshot longtext NOT NULL COMMENT '脱敏运行上下文快照，不保存Token或Secret',
  retry_count int NOT NULL DEFAULT 0 COMMENT '人工或自动重试次数',
  next_retry_time datetime DEFAULT NULL COMMENT '下一次自动重试时间',
  error_code varchar(64) DEFAULT NULL COMMENT '安全错误码',
  error_summary varchar(1000) DEFAULT NULL COMMENT '脱敏且截断的错误摘要',
  start_time datetime DEFAULT NULL COMMENT '开始执行时间',
  end_time datetime DEFAULT NULL COMMENT '结束时间',
  create_by bigint DEFAULT NULL COMMENT '创建人',
  create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  create_dept bigint DEFAULT NULL COMMENT '创建部门',
  update_by bigint DEFAULT NULL COMMENT '更新人',
  update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_ai_business_process_run_idempotency (
    tenant_id, process_version_id, idempotency_key
  ),
  KEY idx_ai_business_process_run_application (
    tenant_id, application_id, status, create_time
  ),
  KEY idx_ai_business_process_run_process (
    tenant_id, process_id, status, update_time
  ),
  KEY idx_ai_business_process_run_business (
    tenant_id, business_key, status, update_time
  ),
  UNIQUE KEY uk_ai_business_process_run_flow (
    tenant_id, flow_process_instance_id
  ),
  KEY idx_ai_business_process_run_recovery (
    tenant_id, status, next_retry_time, update_time
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='应用级业务流程持久化运行实例';

CREATE TABLE IF NOT EXISTS ai_business_process_node_run (
  id bigint NOT NULL COMMENT '主键ID',
  tenant_id bigint NOT NULL DEFAULT 1 COMMENT '租户ID',
  run_id bigint NOT NULL COMMENT '业务流程运行实例ID',
  node_id varchar(128) NOT NULL COMMENT '版本协议中的节点ID',
  node_type varchar(32) NOT NULL COMMENT '节点类型',
  attempt_no int NOT NULL COMMENT '节点尝试序号，从1开始',
  status varchar(24) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/RUNNING/WAITING/SUCCESS/FAILED/CANCELED',
  idempotency_key varchar(256) NOT NULL COMMENT '稳定副作用幂等键，重试保持不变',
  correlation_id varchar(256) DEFAULT NULL COMMENT 'Flowable实例、子流程run或外部调用关联ID',
  input_summary varchar(2000) DEFAULT NULL COMMENT '脱敏输入摘要，不保存完整业务报文',
  output_summary varchar(2000) DEFAULT NULL COMMENT '脱敏结果摘要，不保存完整业务报文',
  error_code varchar(64) DEFAULT NULL COMMENT '安全错误码',
  error_summary varchar(1000) DEFAULT NULL COMMENT '脱敏且截断的错误摘要',
  next_retry_time datetime DEFAULT NULL COMMENT '下一次自动重试时间',
  start_time datetime DEFAULT NULL COMMENT '开始执行时间',
  end_time datetime DEFAULT NULL COMMENT '结束时间',
  create_by bigint DEFAULT NULL COMMENT '创建人',
  create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  create_dept bigint DEFAULT NULL COMMENT '创建部门',
  update_by bigint DEFAULT NULL COMMENT '更新人',
  update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_ai_business_process_node_attempt (
    tenant_id, run_id, node_id, attempt_no
  ),
  KEY idx_ai_business_process_node_timeline (
    tenant_id, run_id, create_time
  ),
  KEY idx_ai_business_process_node_idempotency (
    tenant_id, idempotency_key
  ),
  KEY idx_ai_business_process_node_correlation (
    tenant_id, correlation_id, status
  ),
  KEY idx_ai_business_process_node_retry (
    tenant_id, status, next_retry_time, update_time
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='应用级业务流程节点运行与重试时间线';
