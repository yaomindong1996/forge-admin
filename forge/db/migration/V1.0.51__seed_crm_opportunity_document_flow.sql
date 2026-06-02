-- Phase 7 CRM 商机单据闭环样板。
-- 使用已有 Flowable 流程模型 leave_multi 作为真实流程入口，不写入模拟流程实例。

SET @sql := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE crm_opportunity ADD COLUMN document_no varchar(64) DEFAULT NULL COMMENT ''商机单据编号'' AFTER opportunity_code',
    'SELECT 1'
  )
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'crm_opportunity'
    AND column_name = 'document_no'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE crm_opportunity ADD COLUMN document_status varchar(32) NOT NULL DEFAULT ''DRAFT'' COMMENT ''商机单据状态'' AFTER status',
    'SELECT 1'
  )
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'crm_opportunity'
    AND column_name = 'document_status'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE crm_opportunity
SET document_status = 'DRAFT'
WHERE tenant_id = 1
  AND (document_status IS NULL OR document_status = '');

INSERT INTO sys_dict_type (tenant_id, dict_name, dict_type, dict_status, remark, create_time, update_time)
SELECT 1, '业务单据状态', 'business_document_status', 1, '低代码业务单据生命周期状态', NOW(), NOW()
WHERE NOT EXISTS (
  SELECT 1
  FROM sys_dict_type
  WHERE tenant_id = 1
    AND dict_type = 'business_document_status'
);

INSERT INTO sys_dict_data (tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class,
                           is_default, dict_status, remark, create_time, update_time)
SELECT seed.tenant_id, seed.dict_sort, seed.dict_label, seed.dict_value, 'business_document_status',
       NULL, seed.list_class, seed.is_default, 1, seed.remark, NOW(), NOW()
FROM (
  SELECT 1 tenant_id, 1 dict_sort, '草稿' dict_label, 'DRAFT' dict_value, 'default' list_class, 'Y' is_default, '草稿状态' remark
  UNION ALL SELECT 1, 2, '已提交', 'SUBMITTED', 'info', 'N', '已提交状态'
  UNION ALL SELECT 1, 3, '流程中', 'IN_PROCESS', 'warning', 'N', '流程流转中'
  UNION ALL SELECT 1, 4, '已通过', 'APPROVED', 'success', 'N', '流程通过'
  UNION ALL SELECT 1, 5, '已驳回', 'REJECTED', 'error', 'N', '流程驳回'
  UNION ALL SELECT 1, 6, '已撤回', 'CANCELED', 'default', 'N', '流程撤回'
  UNION ALL SELECT 1, 7, '已关闭', 'CLOSED', 'default', 'N', '业务关闭'
) seed
WHERE NOT EXISTS (
  SELECT 1
  FROM sys_dict_data d
  WHERE d.tenant_id = seed.tenant_id
    AND d.dict_type = 'business_document_status'
    AND d.dict_value = seed.dict_value
);

UPDATE ai_crud_config
SET object_code = 'OPPORTUNITY',
    object_name = '商机',
    domain_code = 'CRM',
    search_schema = '[{"field":"opportunity_name","label":"商机名称","type":"input","props":{"placeholder":"请输入商机名称"}},{"field":"stage","label":"商机阶段","type":"select","props":{"options":[{"label":"初步接触","value":"INITIAL"},{"label":"跟进中","value":"FOLLOWING"},{"label":"商务谈判","value":"NEGOTIATION"},{"label":"赢单","value":"CLOSED_WON"},{"label":"丢单","value":"CLOSED_LOST"}]}},{"field":"document_status","label":"单据状态","type":"select","props":{"options":[{"label":"草稿","value":"DRAFT"},{"label":"流程中","value":"IN_PROCESS"},{"label":"已通过","value":"APPROVED"},{"label":"已驳回","value":"REJECTED"}]}}]',
    columns_schema = '[{"prop":"opportunity_name","label":"商机名称","width":200},{"prop":"customer_id","label":"客户ID","width":100},{"prop":"stage","label":"商机阶段","width":110},{"prop":"amount_cent","label":"预计金额（分）","width":130},{"prop":"probability","label":"赢单概率","width":90},{"prop":"owner_name","label":"负责人","width":100},{"prop":"document_status","label":"单据状态","width":110},{"prop":"create_time","label":"创建时间","width":160}]',
    edit_schema = '[{"field":"customer_id","label":"所属客户","type":"input-number","rules":[{"required":true,"message":"请选择所属客户"}],"props":{"placeholder":"请输入客户ID","min":1}},{"field":"opportunity_name","label":"商机名称","type":"input","rules":[{"required":true,"message":"请输入商机名称"}],"props":{"placeholder":"请输入商机名称"}},{"field":"opportunity_code","label":"商机编码","type":"input","props":{"placeholder":"请输入商机编码"}},{"field":"document_no","label":"单据编号","type":"input","props":{"placeholder":"为空时由业务规则生成"}},{"field":"stage","label":"商机阶段","type":"select","defaultValue":"INITIAL","props":{"options":[{"label":"初步接触","value":"INITIAL"},{"label":"跟进中","value":"FOLLOWING"},{"label":"商务谈判","value":"NEGOTIATION"},{"label":"赢单","value":"CLOSED_WON"},{"label":"丢单","value":"CLOSED_LOST"}]}},{"field":"amount_cent","label":"预计金额（分）","type":"input-number","props":{"placeholder":"请输入预计金额","min":0}},{"field":"probability","label":"赢单概率","type":"input-number","props":{"placeholder":"请输入赢单概率","min":0,"max":100}},{"field":"expected_close_date","label":"预计成交日期","type":"date","props":{"placeholder":"请选择预计成交日期"}},{"field":"owner_id","label":"负责人ID","type":"input-number","props":{"placeholder":"请输入负责人ID","min":1}},{"field":"owner_name","label":"负责人","type":"input","props":{"placeholder":"请输入负责人"}},{"field":"document_status","label":"单据状态","type":"select","defaultValue":"DRAFT","props":{"options":[{"label":"草稿","value":"DRAFT"},{"label":"已提交","value":"SUBMITTED"},{"label":"流程中","value":"IN_PROCESS"},{"label":"已通过","value":"APPROVED"},{"label":"已驳回","value":"REJECTED"},{"label":"已撤回","value":"CANCELED"},{"label":"已关闭","value":"CLOSED"}]}},{"field":"remark","label":"备注","type":"textarea","props":{"placeholder":"请输入备注"}}]',
    model_schema = '{"schemaVersion":2,"domain":{"code":"CRM","name":"CRM"},"object":{"code":"crm_opportunity","name":"商机","description":"CRM商机单据，支持流程审批、状态回写和看板统计"},"appType":"SINGLE","tableMode":"EXISTING","tableName":"crm_opportunity","businessName":"商机","fields":[{"field":"id","columnName":"id","label":"ID","dataType":"bigint","required":true,"listVisible":true,"formVisible":false,"primaryKey":true,"systemField":true,"readonly":true,"autoIncrement":true},{"field":"tenantId","columnName":"tenant_id","label":"租户ID","dataType":"bigint","required":true,"listVisible":false,"formVisible":false,"systemField":true,"readonly":true},{"field":"customerId","columnName":"customer_id","label":"客户ID","dataType":"bigint","required":true,"searchable":true,"listVisible":true,"formVisible":true,"componentType":"number","queryType":"eq","businessFieldType":"REFERENCE","sortOrder":1},{"field":"opportunityName","columnName":"opportunity_name","label":"商机名称","dataType":"varchar","length":200,"required":true,"searchable":true,"listVisible":true,"formVisible":true,"componentType":"input","queryType":"like","businessFieldType":"TEXT","sortOrder":2},{"field":"opportunityCode","columnName":"opportunity_code","label":"商机编码","dataType":"varchar","length":100,"required":false,"searchable":true,"listVisible":true,"formVisible":true,"componentType":"input","queryType":"like","businessFieldType":"TEXT","sortOrder":3},{"field":"documentNo","columnName":"document_no","label":"单据编号","dataType":"varchar","length":64,"required":false,"searchable":true,"listVisible":true,"formVisible":true,"componentType":"input","queryType":"like","businessFieldType":"TEXT","sortOrder":4},{"field":"stage","columnName":"stage","label":"商机阶段","dataType":"varchar","length":50,"required":false,"defaultValue":"INITIAL","searchable":true,"listVisible":true,"formVisible":true,"componentType":"select","queryType":"eq","businessFieldType":"DICT","sortOrder":5},{"field":"amountCent","columnName":"amount_cent","label":"预计金额（分）","dataType":"bigint","required":false,"defaultValue":0,"searchable":false,"listVisible":true,"formVisible":true,"componentType":"number","queryType":"eq","businessFieldType":"MONEY","sortOrder":6},{"field":"probability","columnName":"probability","label":"赢单概率","dataType":"int","required":false,"defaultValue":0,"searchable":false,"listVisible":true,"formVisible":true,"componentType":"number","queryType":"eq","businessFieldType":"NUMBER","sortOrder":7},{"field":"expectedCloseDate","columnName":"expected_close_date","label":"预计成交日期","dataType":"date","required":false,"searchable":true,"listVisible":true,"formVisible":true,"componentType":"date","queryType":"eq","businessFieldType":"DATE","sortOrder":8},{"field":"ownerId","columnName":"owner_id","label":"负责人ID","dataType":"bigint","required":false,"searchable":true,"listVisible":false,"formVisible":true,"componentType":"userSelect","queryType":"eq","businessFieldType":"USER","sortOrder":9},{"field":"ownerName","columnName":"owner_name","label":"负责人","dataType":"varchar","length":100,"required":false,"searchable":true,"listVisible":true,"formVisible":true,"componentType":"input","queryType":"like","businessFieldType":"TEXT","sortOrder":10},{"field":"documentStatus","columnName":"document_status","label":"单据状态","dataType":"varchar","length":32,"required":true,"defaultValue":"DRAFT","searchable":true,"listVisible":true,"formVisible":true,"componentType":"select","queryType":"eq","dictType":"business_document_status","businessFieldType":"DICT","sortOrder":11},{"field":"status","columnName":"status","label":"启用状态","dataType":"tinyint","required":true,"defaultValue":1,"searchable":true,"listVisible":false,"formVisible":false,"componentType":"switch","queryType":"eq","businessFieldType":"STATUS","sortOrder":12},{"field":"remark","columnName":"remark","label":"备注","dataType":"varchar","length":500,"required":false,"searchable":false,"listVisible":true,"formVisible":true,"componentType":"textarea","businessFieldType":"MULTILINE","sortOrder":13},{"field":"createBy","columnName":"create_by","label":"创建人","dataType":"bigint","systemField":true,"readonly":true},{"field":"createTime","columnName":"create_time","label":"创建时间","dataType":"datetime","systemField":true,"readonly":true},{"field":"createDept","columnName":"create_dept","label":"创建部门","dataType":"bigint","systemField":true,"readonly":true},{"field":"updateBy","columnName":"update_by","label":"更新人","dataType":"bigint","systemField":true,"readonly":true},{"field":"updateTime","columnName":"update_time","label":"更新时间","dataType":"datetime","systemField":true,"readonly":true},{"field":"delFlag","columnName":"del_flag","label":"删除标志","dataType":"tinyint","systemField":true,"readonly":true}],"relations":[],"indexes":[],"policies":{"dataScope":"TENANT","userField":"ownerId","userColumn":"owner_id","orgField":"deptId","orgColumn":"dept_id","auditEnabled":true,"primaryKeyStrategy":"AUTO_INCREMENT","primaryKeyField":"id","tenantField":"tenantId","tenantColumn":"tenant_id","logicDeleteField":"delFlag","logicDeleteColumn":"del_flag"},"children":[]}',
    options = JSON_SET(COALESCE(NULLIF(options, ''), '{}'),
      '$.importEnabled', true,
      '$.exportEnabled', true,
      '$.detailEnabled', true,
      '$.documentEnabled', true,
      '$.statsEnabled', true),
    update_time = NOW()
WHERE tenant_id = 1
  AND config_key = 'crm_opportunity';

UPDATE ai_business_object
SET model_code = 'crm_opportunity',
    config_key = 'crm_opportunity',
    display_field = 'opportunityName',
    design_status = 'PUBLISHED',
    last_publish_time = COALESCE(last_publish_time, NOW()),
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND suite_code = 'CRM'
  AND object_code = 'OPPORTUNITY';

SET @opportunity_object_id := (
  SELECT id
  FROM ai_business_object
  WHERE tenant_id = 1
    AND suite_code = 'CRM'
    AND object_code = 'OPPORTUNITY'
  LIMIT 1
);

INSERT INTO ai_business_document_config (id, tenant_id, object_id, suite_code, object_code, config_key,
                                         document_name, document_no_rule, document_enabled, status_field,
                                         starter_field, owner_field, default_flow_key, status_mapping, options,
                                         create_by, create_time, create_dept, update_by, update_time)
SELECT 1910000000000005101, 1, @opportunity_object_id, 'CRM', 'OPPORTUNITY', 'crm_opportunity',
       '商机单据', 'OPP-{yyyyMMdd}-{seq4}', 1, 'documentStatus',
       'createBy', 'ownerId', 'leave_multi',
       '{"DRAFT":"DRAFT","SUBMITTED":"SUBMITTED","IN_PROCESS":"IN_PROCESS","APPROVED":"APPROVED","REJECTED":"REJECTED","CANCELED":"CANCELED","CLOSED":"CLOSED"}',
       '{"sample":true,"scenario":"crm_opportunity_closure","flowModelHint":"使用已部署的 leave_multi 流程模型"}',
       1, NOW(), 1, 1, NOW()
WHERE NOT EXISTS (
  SELECT 1
  FROM ai_business_document_config
  WHERE tenant_id = 1
    AND object_code = 'OPPORTUNITY'
);

INSERT INTO ai_business_binding (id, tenant_id, target_type, target_id, target_code, binding_type, binding_key,
                                 binding_name, binding_config, description, status, sort_order, create_by,
                                 create_time, create_dept, update_by, update_time)
SELECT 1910000000000005102, 1, 'OBJECT', @opportunity_object_id, 'OPPORTUNITY', 'FLOW', 'leave_multi',
       '商机流程审批',
       '{"flowModelKey":"leave_multi","flowModelName":"多级审批流程","titleTemplate":"{opportunityName}-商机审批","startMode":"MANUAL_AND_TRIGGER","variableMapping":[{"formField":"opportunityName","flowVariable":"opportunityName","label":"商机名称"},{"formField":"customerId","flowVariable":"customerId","label":"客户ID"},{"formField":"amountCent","flowVariable":"amountCent","label":"预计金额分"},{"formField":"stage","flowVariable":"stage","label":"商机阶段"},{"formField":"ownerId","flowVariable":"ownerId","label":"负责人"}],"conditionFlows":[],"options":{"sample":true,"businessKeyPattern":"OPPORTUNITY:{recordId}"}}',
       '商机单据默认流程绑定，手动按钮和触发器共用', 1, 1, 1, NOW(), 1, 1, NOW()
WHERE NOT EXISTS (
  SELECT 1
  FROM ai_business_binding
  WHERE tenant_id = 1
    AND target_type = 'OBJECT'
    AND target_code = 'OPPORTUNITY'
    AND binding_type = 'FLOW'
    AND binding_key = 'leave_multi'
);

INSERT INTO ai_business_binding (id, tenant_id, target_type, target_id, target_code, binding_type, binding_key,
                                 binding_name, binding_config, description, status, sort_order, create_by,
                                 create_time, create_dept, update_by, update_time)
SELECT 1910000000000005103, 1, 'OBJECT', @opportunity_object_id, 'OPPORTUNITY', 'REPORT', 'opportunity_document_dashboard',
       '商机单据看板',
       '{"configKey":"crm_opportunity","metrics":["OVERVIEW","STAGE_DISTRIBUTION","SUM","FLOW_RESULT","TREND"],"stageField":"stage","amountField":"amountCent","statusField":"documentStatus"}',
       '商机数量、阶段、金额和流程结果统计', 1, 2, 1, NOW(), 1, 1, NOW()
WHERE NOT EXISTS (
  SELECT 1
  FROM ai_business_binding
  WHERE tenant_id = 1
    AND target_type = 'OBJECT'
    AND target_code = 'OPPORTUNITY'
    AND binding_type = 'REPORT'
    AND binding_key = 'opportunity_document_dashboard'
);

INSERT INTO ai_business_trigger (id, tenant_id, suite_code, object_code, trigger_name, trigger_desc, trigger_type,
                                 scenario_type, blocking_mode, developer_mode, event_type, event_condition,
                                 action_type, action_config, status, sort_order, create_by, create_time,
                                 create_dept, update_by, update_time)
SELECT 1910000000000005104, 1, 'CRM', 'OPPORTUNITY', '高金额商机自动发起流程',
       '新增商机且预计金额大于 50000 元时自动发起已配置流程', 'EVENT',
       'RECORD_CREATED_START_FLOW', 'ASYNC', 0, 'RECORD_CREATED',
       '{"and":[{"field":"amountCent","op":"gt","value":5000000},{"field":"documentStatus","op":"neq","value":"IN_PROCESS"}]}',
       'START_FLOW',
       '{"flowModelKey":"leave_multi","titleTemplate":"{opportunityName}-商机审批","variableMapping":[{"formField":"opportunityName","flowVariable":"opportunityName","label":"商机名称"},{"formField":"customerId","flowVariable":"customerId","label":"客户ID"},{"formField":"amountCent","flowVariable":"amountCent","label":"预计金额分"},{"formField":"stage","flowVariable":"stage","label":"商机阶段"},{"formField":"ownerId","flowVariable":"ownerId","label":"负责人"}]}',
       1, 1, 1, NOW(), 1, 1, NOW()
WHERE NOT EXISTS (
  SELECT 1
  FROM ai_business_trigger
  WHERE tenant_id = 1
    AND object_code = 'OPPORTUNITY'
    AND trigger_name = '高金额商机自动发起流程'
);

INSERT INTO sys_message_template (id, tenant_id, template_code, template_name, type, title_template,
                                  content_template, default_channel, enabled, remark, create_by, create_time,
                                  update_by, update_time)
SELECT 1910000000000005105, 1, 'business_opportunity_flow_result', '商机流程结果通知', 'SYSTEM',
       '商机流程结果：{opportunityName}',
       '商机「{opportunityName}」流程已结束，当前单据状态：{documentStatus}，商机阶段：{stage}。',
       'WEB', 1, '低代码商机单据审批结果站内信模板', 1, NOW(), 1, NOW()
WHERE NOT EXISTS (
  SELECT 1
  FROM sys_message_template
  WHERE tenant_id = 1
    AND template_code = 'business_opportunity_flow_result'
);

INSERT INTO ai_business_trigger (id, tenant_id, suite_code, object_code, trigger_name, trigger_desc, trigger_type,
                                 scenario_type, blocking_mode, developer_mode, event_type, event_condition,
                                 action_type, action_config, status, sort_order, create_by, create_time,
                                 create_dept, update_by, update_time)
SELECT seed.id, 1, 'CRM', 'OPPORTUNITY', seed.trigger_name, seed.trigger_desc, 'EVENT',
       seed.scenario_type, 'ASYNC', 0, seed.event_type, NULL,
       'SEND_MESSAGE',
       '{"templateCode":"business_opportunity_flow_result","receiverRule":"STARTER","channelCode":"internal_websocket"}',
       1, seed.sort_order, 1, NOW(), 1, 1, NOW()
FROM (
  SELECT 1910000000000005106 id, '商机流程通过后通知发起人' trigger_name,
         '商机审批通过后发送站内信给发起人' trigger_desc, 'FLOW_APPROVED_SEND_MESSAGE' scenario_type,
         'FLOW_APPROVED' event_type, 2 sort_order
  UNION ALL SELECT 1910000000000005107, '商机流程驳回后通知发起人',
         '商机审批驳回后发送站内信给发起人', 'FLOW_REJECTED_SEND_MESSAGE',
         'FLOW_REJECTED', 3
) seed
WHERE NOT EXISTS (
  SELECT 1
  FROM ai_business_trigger t
  WHERE t.tenant_id = 1
    AND t.object_code = 'OPPORTUNITY'
    AND t.trigger_name = seed.trigger_name
);

INSERT INTO ai_business_app (id, tenant_id, app_code, app_name, app_type, suite_code, object_code, entry_mode,
                             entry_url, config_key, icon, description, status, sort_order, options, create_by,
                             create_time, create_dept, update_by, update_time)
SELECT 1910000000000005108, 1, 'CRM_OPPORTUNITY_STATS', '商机看板', 'BUSINESS', 'CRM', 'OPPORTUNITY', 'ROUTE',
       '/app-center/stats?configKey=crm_opportunity&objectCode=OPPORTUNITY&suiteCode=CRM',
       'crm_opportunity', 'ionicons5:StatsChartOutline', '商机阶段、金额和流程结果统计看板',
       1, 40, '{"sample":true,"entryGroup":"crm-analytics"}', 1, NOW(), 1, 1, NOW()
WHERE NOT EXISTS (
  SELECT 1
  FROM ai_business_app
  WHERE tenant_id = 1
    AND app_code = 'CRM_OPPORTUNITY_STATS'
);
