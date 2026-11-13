-- 预售应用：把 3 个既有业务动作（提交预售单/登记提货/登记退货）编排进业务流程。
-- 每条流程均为：手动开始 → 执行业务动作 → 结束；动作执行细节仍由对象动作配置承载。
-- draft_schema_hash 仅作为草稿乐观锁令牌（SHA-256 十六进制），首次保存时后端会重算规范化摘要覆盖。
-- 幂等：按 (application_id, process_code) 防重复；对象/应用缺失时跳过插入。

SET @ps_process_application_id := (
  SELECT app_row.id
  FROM ai_business_application app_row
  WHERE app_row.tenant_id = 1
    AND app_row.application_code = 'PRESALE_REGISTRATION_APP'
    AND app_row.del_flag = 0
  LIMIT 1
);

SET @ps_process_subject_id := (
  SELECT object_row.id
  FROM ai_business_object object_row
  WHERE object_row.tenant_id = 1
    AND object_row.suite_code = 'PRESALE_REGISTRATION'
    AND object_row.object_code = 'PS_PRESALE_ORDER'
    AND object_row.del_flag = 0
  LIMIT 1
);

CREATE TEMPORARY TABLE IF NOT EXISTS tmp_presale_process_seed (
  id bigint PRIMARY KEY,
  process_code varchar(128) NOT NULL,
  process_name varchar(128) NOT NULL,
  process_description varchar(500),
  action_code varchar(128) NOT NULL,
  action_name varchar(64) NOT NULL,
  action_node_id varchar(64) NOT NULL,
  schema_json longtext NULL
);

DELETE FROM tmp_presale_process_seed;

INSERT INTO tmp_presale_process_seed (
  id, process_code, process_name, process_description,
  action_code, action_name, action_node_id
) VALUES
  (1990000000000001101, 'presale_submit', '预售单提交流程',
   '门店提交预售单，校验商品与数量后将单据推进到待提货状态',
   'submit_presale', '提交预售单', 'act_submit'),
  (1990000000000001102, 'presale_pickup', '预售提货登记流程',
   '门店对预售商品分批提货，扣减待提数量并写入操作日志',
   'record_pickup', '登记提货', 'act_pickup'),
  (1990000000000001103, 'presale_return', '预售退货登记流程',
   '门店对已提货商品办理退货，回补数量并写入操作日志',
   'record_return', '登记退货', 'act_return');

-- 与前端 createBusinessProcessSchema 输出结构保持一致；
-- objectVersionId / edge.isDefault 省略（JSON_OBJECT 忽略 NULL），前后端 normalize 均会补默认值。
UPDATE tmp_presale_process_seed seed
SET seed.schema_json = JSON_OBJECT(
  'schemaVersion', '1.0',
  'processCode', seed.process_code,
  'subject', JSON_OBJECT(
    'objectId', CAST(@ps_process_subject_id AS CHAR),
    'objectCode', 'PS_PRESALE_ORDER',
    'recordIdSource', 'RUNTIME_RECORD'
  ),
  'nodes', JSON_ARRAY(
    JSON_OBJECT(
      'id', 'start_manual', 'type', 'START_MANUAL', 'name', '手动开始',
      'ports', JSON_ARRAY(),
      'config', JSON_OBJECT(
        'positions', JSON_ARRAY('ROW', 'DETAIL'),
        'permission', 'ai:businessProcess:start'
      )
    ),
    JSON_OBJECT(
      'id', seed.action_node_id, 'type', 'ACTION', 'name', seed.action_name,
      'ports', JSON_ARRAY(),
      'config', JSON_OBJECT(
        'actionType', 'BUSINESS_ACTION',
        'businessActionCode', seed.action_code
      )
    ),
    JSON_OBJECT(
      'id', 'end_success', 'type', 'END', 'name', '完成',
      'ports', JSON_ARRAY(),
      'config', JSON_OBJECT('result', 'SUCCESS')
    )
  ),
  'edges', JSON_ARRAY(
    JSON_OBJECT(
      'id', 'edge_start_action', 'source', 'start_manual',
      'target', seed.action_node_id, 'sourcePort', 'NEXT'
    ),
    JSON_OBJECT(
      'id', 'edge_action_end', 'source', seed.action_node_id,
      'target', 'end_success', 'sourcePort', 'NEXT'
    )
  ),
  'policies', JSON_OBJECT(
    'approvalConcurrency', 'ONE_ACTIVE_PER_BUSINESS_KEY',
    'maxSubProcessDepth', 5,
    'retry', JSON_OBJECT(
      'mode', 'LIMITED', 'maxAttempts', 3,
      'backoffSeconds', JSON_ARRAY(30, 120, 600)
    )
  ),
  'dependencies', JSON_OBJECT(
    'objects', JSON_ARRAY('PS_PRESALE_ORDER'),
    'flowModels', JSON_ARRAY(),
    'formAssets', JSON_ARRAY(),
    'businessActions', JSON_ARRAY(seed.action_code),
    'messageTemplates', JSON_ARRAY(),
    'capabilities', JSON_ARRAY(),
    'subProcesses', JSON_ARRAY()
  ),
  'metadata', JSON_OBJECT()
)
WHERE @ps_process_application_id IS NOT NULL
  AND @ps_process_subject_id IS NOT NULL;

INSERT INTO ai_business_process (
  id, tenant_id, application_id, process_code, process_name, process_description,
  subject_object_id, subject_object_code,
  draft_schema_json, draft_schema_hash,
  design_status, current_version, published_version, status,
  legacy_source_type, legacy_source_id,
  del_flag, create_by, create_time, create_dept, update_by, update_time
)
SELECT seed.id, 1, @ps_process_application_id,
       seed.process_code, seed.process_name, seed.process_description,
       @ps_process_subject_id, 'PS_PRESALE_ORDER',
       seed.schema_json, SHA2(seed.schema_json, 256),
       'DRAFT', 0, NULL, 1,
       NULL, NULL,
       0, 1, NOW(), 1, 1, NOW()
FROM tmp_presale_process_seed seed
WHERE @ps_process_application_id IS NOT NULL
  AND @ps_process_subject_id IS NOT NULL
  AND seed.schema_json IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM ai_business_process existing
    WHERE existing.tenant_id = 1
      AND existing.application_id = @ps_process_application_id
      AND existing.process_code = seed.process_code
      AND existing.del_flag = 0
  );

DROP TEMPORARY TABLE IF EXISTS tmp_presale_process_seed;
