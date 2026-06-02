-- Phase 7 离职申请单据闭环样板。
-- 验证“工作表事件触发流程，流程通过后自动生成交接记录”。

CREATE TABLE IF NOT EXISTS `hr_leave_application` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户ID',
  `application_no` varchar(64) DEFAULT NULL COMMENT '申请单号',
  `employee_id` bigint DEFAULT NULL COMMENT '员工ID',
  `employee_name` varchar(100) NOT NULL COMMENT '员工姓名',
  `department_id` bigint DEFAULT NULL COMMENT '部门ID',
  `department_name` varchar(100) DEFAULT NULL COMMENT '部门名称',
  `position_name` varchar(100) DEFAULT NULL COMMENT '岗位名称',
  `leave_reason` varchar(500) DEFAULT NULL COMMENT '离职原因',
  `last_work_date` date NOT NULL COMMENT '最后工作日',
  `handover_owner_id` bigint DEFAULT NULL COMMENT '交接负责人ID',
  `handover_owner_name` varchar(100) DEFAULT NULL COMMENT '交接负责人',
  `document_status` varchar(32) NOT NULL DEFAULT 'DRAFT' COMMENT '单据状态',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：0停用 1启用',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint NOT NULL DEFAULT 0 COMMENT '删除标记：0正常 1删除',
  PRIMARY KEY (`id`),
  KEY `idx_hr_leave_application_tenant` (`tenant_id`),
  KEY `idx_hr_leave_application_status` (`tenant_id`, `document_status`),
  KEY `idx_hr_leave_application_last_date` (`tenant_id`, `last_work_date`),
  KEY `idx_hr_leave_application_employee` (`tenant_id`, `employee_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='HR离职申请表';

CREATE TABLE IF NOT EXISTS `hr_leave_handover` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户ID',
  `leave_application_id` bigint NOT NULL COMMENT '离职申请ID',
  `handover_title` varchar(200) NOT NULL COMMENT '交接标题',
  `employee_name` varchar(100) NOT NULL COMMENT '员工姓名',
  `department_name` varchar(100) DEFAULT NULL COMMENT '部门名称',
  `position_name` varchar(100) DEFAULT NULL COMMENT '岗位名称',
  `last_work_date` date DEFAULT NULL COMMENT '最后工作日',
  `handover_owner_id` bigint DEFAULT NULL COMMENT '交接负责人ID',
  `handover_owner_name` varchar(100) DEFAULT NULL COMMENT '交接负责人',
  `handover_status` varchar(32) NOT NULL DEFAULT 'PENDING' COMMENT '交接状态',
  `completed_time` datetime DEFAULT NULL COMMENT '完成时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint NOT NULL DEFAULT 0 COMMENT '删除标记：0正常 1删除',
  PRIMARY KEY (`id`),
  KEY `idx_hr_leave_handover_tenant` (`tenant_id`),
  KEY `idx_hr_leave_handover_application` (`tenant_id`, `leave_application_id`),
  KEY `idx_hr_leave_handover_status` (`tenant_id`, `handover_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='HR离职交接表';

INSERT INTO sys_dict_type (tenant_id, dict_name, dict_type, dict_status, remark, create_time, update_time)
SELECT 1, '离职交接状态', 'hr_leave_handover_status', 1, '离职交接任务状态', NOW(), NOW()
WHERE NOT EXISTS (
  SELECT 1
  FROM sys_dict_type
  WHERE tenant_id = 1
    AND dict_type = 'hr_leave_handover_status'
);

INSERT INTO sys_dict_data (tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class,
                           is_default, dict_status, remark, create_time, update_time)
SELECT seed.tenant_id, seed.dict_sort, seed.dict_label, seed.dict_value, 'hr_leave_handover_status',
       NULL, seed.list_class, seed.is_default, 1, seed.remark, NOW(), NOW()
FROM (
  SELECT 1 tenant_id, 1 dict_sort, '待交接' dict_label, 'PENDING' dict_value, 'warning' list_class, 'Y' is_default, '待交接状态' remark
  UNION ALL SELECT 1, 2, '交接中', 'PROCESSING', 'info', 'N', '交接中状态'
  UNION ALL SELECT 1, 3, '已完成', 'COMPLETED', 'success', 'N', '已完成状态'
) seed
WHERE NOT EXISTS (
  SELECT 1
  FROM sys_dict_data d
  WHERE d.tenant_id = seed.tenant_id
    AND d.dict_type = 'hr_leave_handover_status'
    AND d.dict_value = seed.dict_value
);

INSERT INTO ai_business_suite (id, tenant_id, suite_code, suite_name, icon, description, status, sort_order, options,
                               create_by, create_time, create_dept, update_by, update_time)
SELECT 1910000000000005201, 1, 'HR', 'HR人事', 'ionicons5:PeopleCircleOutline',
       '人事管理样板套件，包含离职申请和离职交接闭环示例', 1, 20,
       '{"sample":true,"stage":"phase7"}', 1, NOW(), 1, 1, NOW()
WHERE NOT EXISTS (
  SELECT 1
  FROM ai_business_suite
  WHERE tenant_id = 1
    AND suite_code = 'HR'
);

INSERT INTO ai_business_object (id, tenant_id, suite_code, object_code, object_name, object_type, model_id,
                                model_code, display_field, icon, description, status, sort_order, options,
                                design_status, config_key, last_publish_time, last_publish_version,
                                create_by, create_time, create_dept, update_by, update_time)
SELECT seed.id, 1, 'HR', seed.object_code, seed.object_name, seed.object_type, NULL,
       seed.model_code, seed.display_field, seed.icon, seed.description, 1, seed.sort_order,
       '{"sample":true,"stage":"phase7"}', 'PUBLISHED', seed.config_key, NOW(), 1,
       1, NOW(), 1, 1, NOW()
FROM (
  SELECT 1910000000000005202 id, 'LEAVE_APPLICATION' object_code, '离职申请' object_name,
         'TRANSACTION' object_type, 'hr_leave_application' model_code, 'employeeName' display_field,
         'ionicons5:ExitOutline' icon, '员工离职申请单据，支持流程流转和状态回写' description,
         1 sort_order, 'hr_leave_application' config_key
  UNION ALL SELECT 1910000000000005203, 'LEAVE_HANDOVER', '离职交接',
         'TRANSACTION', 'hr_leave_handover', 'handoverTitle',
         'ionicons5:ClipboardOutline', '离职申请通过后自动生成的交接记录',
         2, 'hr_leave_handover'
) seed
WHERE NOT EXISTS (
  SELECT 1
  FROM ai_business_object o
  WHERE o.tenant_id = 1
    AND o.suite_code = 'HR'
    AND o.object_code = seed.object_code
);

INSERT INTO ai_crud_config (id, tenant_id, config_key, table_name, table_comment, app_name, mode, build_mode,
                            status, publish_status, object_code, object_name, domain_code, search_schema,
                            columns_schema, edit_schema, api_config, options, model_schema, create_by,
                            create_time, update_by, update_time)
SELECT 1910000000000005204, 1, 'hr_leave_application', 'hr_leave_application', 'HR离职申请表',
       '离职申请', 'CONFIG', 'LOWCODE', '0', 'PUBLISHED', 'LEAVE_APPLICATION', '离职申请', 'HR',
       '[{"field":"employee_name","label":"员工姓名","type":"input","props":{"placeholder":"请输入员工姓名"}},{"field":"document_status","label":"单据状态","type":"select","props":{"options":[{"label":"草稿","value":"DRAFT"},{"label":"流程中","value":"IN_PROCESS"},{"label":"已通过","value":"APPROVED"},{"label":"已驳回","value":"REJECTED"}]}},{"field":"last_work_date","label":"最后工作日","type":"date","props":{"placeholder":"请选择最后工作日"}}]',
       '[{"prop":"application_no","label":"申请单号","width":140},{"prop":"employee_name","label":"员工姓名","width":120},{"prop":"department_name","label":"部门","width":120},{"prop":"position_name","label":"岗位","width":120},{"prop":"last_work_date","label":"最后工作日","width":120},{"prop":"handover_owner_name","label":"交接负责人","width":120},{"prop":"document_status","label":"单据状态","width":110},{"prop":"create_time","label":"创建时间","width":160}]',
       '[{"field":"application_no","label":"申请单号","type":"input","props":{"placeholder":"请输入申请单号"}},{"field":"employee_id","label":"员工ID","type":"input-number","props":{"placeholder":"请输入员工ID","min":1}},{"field":"employee_name","label":"员工姓名","type":"input","rules":[{"required":true,"message":"请输入员工姓名"}],"props":{"placeholder":"请输入员工姓名"}},{"field":"department_id","label":"部门ID","type":"input-number","props":{"placeholder":"请输入部门ID","min":1}},{"field":"department_name","label":"部门名称","type":"input","props":{"placeholder":"请输入部门名称"}},{"field":"position_name","label":"岗位名称","type":"input","props":{"placeholder":"请输入岗位名称"}},{"field":"last_work_date","label":"最后工作日","type":"date","rules":[{"required":true,"message":"请选择最后工作日"}],"props":{"placeholder":"请选择最后工作日"}},{"field":"handover_owner_id","label":"交接负责人ID","type":"input-number","props":{"placeholder":"请输入交接负责人ID","min":1}},{"field":"handover_owner_name","label":"交接负责人","type":"input","props":{"placeholder":"请输入交接负责人"}},{"field":"leave_reason","label":"离职原因","type":"textarea","props":{"placeholder":"请输入离职原因"}},{"field":"document_status","label":"单据状态","type":"select","defaultValue":"DRAFT","props":{"options":[{"label":"草稿","value":"DRAFT"},{"label":"已提交","value":"SUBMITTED"},{"label":"流程中","value":"IN_PROCESS"},{"label":"已通过","value":"APPROVED"},{"label":"已驳回","value":"REJECTED"},{"label":"已撤回","value":"CANCELED"},{"label":"已关闭","value":"CLOSED"}]}},{"field":"remark","label":"备注","type":"textarea","props":{"placeholder":"请输入备注"}}]',
       '{"list":"get@/ai/crud/hr_leave_application/page","detail":"get@/ai/crud/hr_leave_application/:id","add":"post@/ai/crud/hr_leave_application","update":"put@/ai/crud/hr_leave_application","delete":"delete@/ai/crud/hr_leave_application/:id","importTemplate":"get@/ai/crud/hr_leave_application/import-template","import":"post@/ai/crud/hr_leave_application/import","export":"post@/ai/crud/hr_leave_application/export"}',
       '{"importEnabled":true,"exportEnabled":true,"detailEnabled":true,"documentEnabled":true}',
       '{"schemaVersion":2,"domain":{"code":"HR","name":"HR人事"},"object":{"code":"hr_leave_application","name":"离职申请","description":"员工离职申请单据"},"appType":"SINGLE","tableMode":"EXISTING","tableName":"hr_leave_application","businessName":"离职申请","fields":[{"field":"id","columnName":"id","label":"ID","dataType":"bigint","required":true,"primaryKey":true,"systemField":true,"readonly":true,"autoIncrement":true},{"field":"tenantId","columnName":"tenant_id","label":"租户ID","dataType":"bigint","required":true,"systemField":true,"readonly":true},{"field":"applicationNo","columnName":"application_no","label":"申请单号","dataType":"varchar","length":64,"searchable":true,"listVisible":true,"formVisible":true,"componentType":"input","businessFieldType":"TEXT","sortOrder":1},{"field":"employeeId","columnName":"employee_id","label":"员工ID","dataType":"bigint","searchable":true,"listVisible":false,"formVisible":true,"componentType":"userSelect","businessFieldType":"USER","sortOrder":2},{"field":"employeeName","columnName":"employee_name","label":"员工姓名","dataType":"varchar","length":100,"required":true,"searchable":true,"listVisible":true,"formVisible":true,"componentType":"input","businessFieldType":"TEXT","sortOrder":3},{"field":"departmentId","columnName":"department_id","label":"部门ID","dataType":"bigint","searchable":true,"listVisible":false,"formVisible":true,"componentType":"orgTreeSelect","businessFieldType":"DEPT","sortOrder":4},{"field":"departmentName","columnName":"department_name","label":"部门名称","dataType":"varchar","length":100,"searchable":true,"listVisible":true,"formVisible":true,"componentType":"input","businessFieldType":"TEXT","sortOrder":5},{"field":"positionName","columnName":"position_name","label":"岗位名称","dataType":"varchar","length":100,"searchable":true,"listVisible":true,"formVisible":true,"componentType":"input","businessFieldType":"TEXT","sortOrder":6},{"field":"leaveReason","columnName":"leave_reason","label":"离职原因","dataType":"varchar","length":500,"searchable":false,"listVisible":false,"formVisible":true,"componentType":"textarea","businessFieldType":"MULTILINE","sortOrder":7},{"field":"lastWorkDate","columnName":"last_work_date","label":"最后工作日","dataType":"date","required":true,"searchable":true,"listVisible":true,"formVisible":true,"componentType":"date","businessFieldType":"DATE","sortOrder":8},{"field":"handoverOwnerId","columnName":"handover_owner_id","label":"交接负责人ID","dataType":"bigint","searchable":true,"listVisible":false,"formVisible":true,"componentType":"userSelect","businessFieldType":"USER","sortOrder":9},{"field":"handoverOwnerName","columnName":"handover_owner_name","label":"交接负责人","dataType":"varchar","length":100,"searchable":true,"listVisible":true,"formVisible":true,"componentType":"input","businessFieldType":"TEXT","sortOrder":10},{"field":"documentStatus","columnName":"document_status","label":"单据状态","dataType":"varchar","length":32,"required":true,"defaultValue":"DRAFT","searchable":true,"listVisible":true,"formVisible":true,"componentType":"select","dictType":"business_document_status","businessFieldType":"DICT","sortOrder":11},{"field":"remark","columnName":"remark","label":"备注","dataType":"varchar","length":500,"searchable":false,"listVisible":true,"formVisible":true,"componentType":"textarea","businessFieldType":"MULTILINE","sortOrder":12},{"field":"createBy","columnName":"create_by","label":"创建人","dataType":"bigint","systemField":true,"readonly":true},{"field":"createTime","columnName":"create_time","label":"创建时间","dataType":"datetime","systemField":true,"readonly":true},{"field":"createDept","columnName":"create_dept","label":"创建部门","dataType":"bigint","systemField":true,"readonly":true},{"field":"updateBy","columnName":"update_by","label":"更新人","dataType":"bigint","systemField":true,"readonly":true},{"field":"updateTime","columnName":"update_time","label":"更新时间","dataType":"datetime","systemField":true,"readonly":true},{"field":"delFlag","columnName":"del_flag","label":"删除标志","dataType":"tinyint","systemField":true,"readonly":true}],"relations":[],"indexes":[],"policies":{"dataScope":"TENANT","userField":"employeeId","userColumn":"employee_id","orgField":"departmentId","orgColumn":"department_id","auditEnabled":true,"primaryKeyStrategy":"AUTO_INCREMENT","primaryKeyField":"id","tenantField":"tenantId","tenantColumn":"tenant_id","logicDeleteField":"delFlag","logicDeleteColumn":"del_flag"},"children":[]}',
       1, NOW(), 1, NOW()
WHERE NOT EXISTS (
  SELECT 1
  FROM ai_crud_config
  WHERE tenant_id = 1
    AND config_key = 'hr_leave_application'
);

INSERT INTO ai_crud_config (id, tenant_id, config_key, table_name, table_comment, app_name, mode, build_mode,
                            status, publish_status, object_code, object_name, domain_code, search_schema,
                            columns_schema, edit_schema, api_config, options, model_schema, create_by,
                            create_time, update_by, update_time)
SELECT 1910000000000005205, 1, 'hr_leave_handover', 'hr_leave_handover', 'HR离职交接表',
       '离职交接', 'CONFIG', 'LOWCODE', '0', 'PUBLISHED', 'LEAVE_HANDOVER', '离职交接', 'HR',
       '[{"field":"employee_name","label":"员工姓名","type":"input","props":{"placeholder":"请输入员工姓名"}},{"field":"handover_status","label":"交接状态","type":"select","props":{"options":[{"label":"待交接","value":"PENDING"},{"label":"交接中","value":"PROCESSING"},{"label":"已完成","value":"COMPLETED"}]}}]',
       '[{"prop":"handover_title","label":"交接标题","width":200},{"prop":"employee_name","label":"员工姓名","width":120},{"prop":"department_name","label":"部门","width":120},{"prop":"last_work_date","label":"最后工作日","width":120},{"prop":"handover_owner_name","label":"交接负责人","width":120},{"prop":"handover_status","label":"交接状态","width":110},{"prop":"create_time","label":"创建时间","width":160}]',
       '[{"field":"leave_application_id","label":"离职申请ID","type":"input-number","rules":[{"required":true,"message":"请输入离职申请ID"}],"props":{"min":1}},{"field":"handover_title","label":"交接标题","type":"input","rules":[{"required":true,"message":"请输入交接标题"}],"props":{"placeholder":"请输入交接标题"}},{"field":"employee_name","label":"员工姓名","type":"input","rules":[{"required":true,"message":"请输入员工姓名"}],"props":{"placeholder":"请输入员工姓名"}},{"field":"department_name","label":"部门名称","type":"input","props":{"placeholder":"请输入部门名称"}},{"field":"position_name","label":"岗位名称","type":"input","props":{"placeholder":"请输入岗位名称"}},{"field":"last_work_date","label":"最后工作日","type":"date","props":{"placeholder":"请选择最后工作日"}},{"field":"handover_owner_id","label":"交接负责人ID","type":"input-number","props":{"min":1}},{"field":"handover_owner_name","label":"交接负责人","type":"input","props":{"placeholder":"请输入交接负责人"}},{"field":"handover_status","label":"交接状态","type":"select","defaultValue":"PENDING","props":{"options":[{"label":"待交接","value":"PENDING"},{"label":"交接中","value":"PROCESSING"},{"label":"已完成","value":"COMPLETED"}]}},{"field":"remark","label":"备注","type":"textarea","props":{"placeholder":"请输入备注"}}]',
       '{"list":"get@/ai/crud/hr_leave_handover/page","detail":"get@/ai/crud/hr_leave_handover/:id","add":"post@/ai/crud/hr_leave_handover","update":"put@/ai/crud/hr_leave_handover","delete":"delete@/ai/crud/hr_leave_handover/:id","importTemplate":"get@/ai/crud/hr_leave_handover/import-template","import":"post@/ai/crud/hr_leave_handover/import","export":"post@/ai/crud/hr_leave_handover/export"}',
       '{"importEnabled":true,"exportEnabled":true,"detailEnabled":true}',
       '{"schemaVersion":2,"domain":{"code":"HR","name":"HR人事"},"object":{"code":"hr_leave_handover","name":"离职交接","description":"离职申请通过后自动生成的交接记录"},"appType":"SINGLE","tableMode":"EXISTING","tableName":"hr_leave_handover","businessName":"离职交接","fields":[{"field":"id","columnName":"id","label":"ID","dataType":"bigint","required":true,"primaryKey":true,"systemField":true,"readonly":true,"autoIncrement":true},{"field":"tenantId","columnName":"tenant_id","label":"租户ID","dataType":"bigint","required":true,"systemField":true,"readonly":true},{"field":"leaveApplicationId","columnName":"leave_application_id","label":"离职申请ID","dataType":"bigint","required":true,"searchable":true,"listVisible":false,"formVisible":true,"componentType":"number","businessFieldType":"REFERENCE","sortOrder":1},{"field":"handoverTitle","columnName":"handover_title","label":"交接标题","dataType":"varchar","length":200,"required":true,"searchable":true,"listVisible":true,"formVisible":true,"componentType":"input","businessFieldType":"TEXT","sortOrder":2},{"field":"employeeName","columnName":"employee_name","label":"员工姓名","dataType":"varchar","length":100,"required":true,"searchable":true,"listVisible":true,"formVisible":true,"componentType":"input","businessFieldType":"TEXT","sortOrder":3},{"field":"departmentName","columnName":"department_name","label":"部门名称","dataType":"varchar","length":100,"searchable":true,"listVisible":true,"formVisible":true,"componentType":"input","businessFieldType":"TEXT","sortOrder":4},{"field":"positionName","columnName":"position_name","label":"岗位名称","dataType":"varchar","length":100,"searchable":true,"listVisible":true,"formVisible":true,"componentType":"input","businessFieldType":"TEXT","sortOrder":5},{"field":"lastWorkDate","columnName":"last_work_date","label":"最后工作日","dataType":"date","searchable":true,"listVisible":true,"formVisible":true,"componentType":"date","businessFieldType":"DATE","sortOrder":6},{"field":"handoverOwnerId","columnName":"handover_owner_id","label":"交接负责人ID","dataType":"bigint","searchable":true,"listVisible":false,"formVisible":true,"componentType":"userSelect","businessFieldType":"USER","sortOrder":7},{"field":"handoverOwnerName","columnName":"handover_owner_name","label":"交接负责人","dataType":"varchar","length":100,"searchable":true,"listVisible":true,"formVisible":true,"componentType":"input","businessFieldType":"TEXT","sortOrder":8},{"field":"handoverStatus","columnName":"handover_status","label":"交接状态","dataType":"varchar","length":32,"required":true,"defaultValue":"PENDING","searchable":true,"listVisible":true,"formVisible":true,"componentType":"select","dictType":"hr_leave_handover_status","businessFieldType":"DICT","sortOrder":9},{"field":"remark","columnName":"remark","label":"备注","dataType":"varchar","length":500,"listVisible":true,"formVisible":true,"componentType":"textarea","businessFieldType":"MULTILINE","sortOrder":10},{"field":"createBy","columnName":"create_by","label":"创建人","dataType":"bigint","systemField":true,"readonly":true},{"field":"createTime","columnName":"create_time","label":"创建时间","dataType":"datetime","systemField":true,"readonly":true},{"field":"createDept","columnName":"create_dept","label":"创建部门","dataType":"bigint","systemField":true,"readonly":true},{"field":"updateBy","columnName":"update_by","label":"更新人","dataType":"bigint","systemField":true,"readonly":true},{"field":"updateTime","columnName":"update_time","label":"更新时间","dataType":"datetime","systemField":true,"readonly":true},{"field":"delFlag","columnName":"del_flag","label":"删除标志","dataType":"tinyint","systemField":true,"readonly":true}],"relations":[],"indexes":[],"policies":{"dataScope":"TENANT","auditEnabled":true,"primaryKeyStrategy":"AUTO_INCREMENT","primaryKeyField":"id","tenantField":"tenantId","tenantColumn":"tenant_id","logicDeleteField":"delFlag","logicDeleteColumn":"del_flag"},"children":[]}',
       1, NOW(), 1, NOW()
WHERE NOT EXISTS (
  SELECT 1
  FROM ai_crud_config
  WHERE tenant_id = 1
    AND config_key = 'hr_leave_handover'
);

INSERT INTO ai_business_app (id, tenant_id, app_code, app_name, app_type, suite_code, object_code, entry_mode,
                             entry_url, config_key, icon, description, status, sort_order, options, create_by,
                             create_time, create_dept, update_by, update_time)
SELECT seed.id, 1, seed.app_code, seed.app_name, 'BUSINESS', 'HR', seed.object_code, 'RUNTIME',
       seed.entry_url, seed.config_key, seed.icon, seed.description, 1, seed.sort_order,
       '{"sample":true,"entryGroup":"hr-leave"}', 1, NOW(), 1, 1, NOW()
FROM (
  SELECT 1910000000000005206 id, 'HR_LEAVE_APPLICATION_MANAGE' app_code, '离职申请' app_name,
         'LEAVE_APPLICATION' object_code, '/ai/crud-page/hr_leave_application' entry_url,
         'hr_leave_application' config_key, 'ionicons5:ExitOutline' icon, '离职申请单据入口' description, 1 sort_order
  UNION ALL SELECT 1910000000000005207, 'HR_LEAVE_HANDOVER_MANAGE', '离职交接',
         'LEAVE_HANDOVER', '/ai/crud-page/hr_leave_handover',
         'hr_leave_handover', 'ionicons5:ClipboardOutline', '离职交接记录入口', 2
) seed
WHERE NOT EXISTS (
  SELECT 1
  FROM ai_business_app a
  WHERE a.tenant_id = 1
    AND a.app_code = seed.app_code
);

SET @leave_application_object_id := (
  SELECT id
  FROM ai_business_object
  WHERE tenant_id = 1
    AND suite_code = 'HR'
    AND object_code = 'LEAVE_APPLICATION'
  LIMIT 1
);

SET @leave_handover_object_id := (
  SELECT id
  FROM ai_business_object
  WHERE tenant_id = 1
    AND suite_code = 'HR'
    AND object_code = 'LEAVE_HANDOVER'
  LIMIT 1
);

INSERT INTO ai_business_document_config (id, tenant_id, object_id, suite_code, object_code, config_key,
                                         document_name, document_no_rule, document_enabled, status_field,
                                         starter_field, owner_field, default_flow_key, status_mapping, options,
                                         create_by, create_time, create_dept, update_by, update_time)
SELECT 1910000000000005208, 1, @leave_application_object_id, 'HR', 'LEAVE_APPLICATION', 'hr_leave_application',
       '离职申请单据', 'LEAVE-{yyyyMMdd}-{seq4}', 1, 'documentStatus',
       'createBy', 'handoverOwnerId', 'leave_multi',
       '{"DRAFT":"DRAFT","SUBMITTED":"SUBMITTED","IN_PROCESS":"IN_PROCESS","APPROVED":"APPROVED","REJECTED":"REJECTED","CANCELED":"CANCELED","CLOSED":"CLOSED"}',
       '{"sample":true,"scenario":"leave_application_closure","autoStartBeforeDate":"2026-12-31"}',
       1, NOW(), 1, 1, NOW()
WHERE NOT EXISTS (
  SELECT 1
  FROM ai_business_document_config
  WHERE tenant_id = 1
    AND object_code = 'LEAVE_APPLICATION'
);

INSERT INTO ai_business_binding (id, tenant_id, target_type, target_id, target_code, binding_type, binding_key,
                                 binding_name, binding_config, description, status, sort_order, create_by,
                                 create_time, create_dept, update_by, update_time)
SELECT seed.id, 1, 'OBJECT', seed.target_id, seed.target_code, seed.binding_type, seed.binding_key,
       seed.binding_name, seed.binding_config, seed.description, 1, seed.sort_order, 1, NOW(), 1, 1, NOW()
FROM (
  SELECT 1910000000000005209 id, @leave_application_object_id target_id, 'LEAVE_APPLICATION' target_code,
         'FLOW' binding_type, 'leave_multi' binding_key, '离职审批流程' binding_name,
         '{"flowModelKey":"leave_multi","flowModelName":"多级审批流程","titleTemplate":"{employeeName}-离职申请","startMode":"MANUAL_AND_TRIGGER","variableMapping":[{"formField":"employeeName","flowVariable":"employeeName","label":"员工姓名"},{"formField":"departmentName","flowVariable":"departmentName","label":"部门"},{"formField":"lastWorkDate","flowVariable":"lastWorkDate","label":"最后工作日"},{"formField":"leaveReason","flowVariable":"reason","label":"离职原因"},{"formField":"handoverOwnerId","flowVariable":"deptManager","label":"部门主管"}],"conditionFlows":[],"options":{"sample":true,"businessKeyPattern":"LEAVE_APPLICATION:{recordId}"}}' binding_config,
         '离职申请默认流程绑定' description, 1 sort_order
  UNION ALL SELECT 1910000000000005210, @leave_handover_object_id, 'LEAVE_HANDOVER',
         'REPORT', 'leave_handover_dashboard', '离职交接看板',
         '{"configKey":"hr_leave_handover","metrics":["OVERVIEW","STATUS_DISTRIBUTION"],"statusField":"handoverStatus"}',
         '离职交接处理状态统计', 2
) seed
WHERE NOT EXISTS (
  SELECT 1
  FROM ai_business_binding b
  WHERE b.tenant_id = 1
    AND b.target_type = 'OBJECT'
    AND b.target_code = seed.target_code
    AND b.binding_type = seed.binding_type
    AND b.binding_key = seed.binding_key
);

INSERT INTO ai_business_trigger (id, tenant_id, suite_code, object_code, trigger_name, trigger_desc, trigger_type,
                                 scenario_type, blocking_mode, developer_mode, event_type, event_condition,
                                 action_type, action_config, status, sort_order, create_by, create_time,
                                 create_dept, update_by, update_time)
SELECT 1910000000000005211, 1, 'HR', 'LEAVE_APPLICATION', '离职申请到期自动发起流程',
       '新增离职申请且最后工作日小于等于 2026-12-31 时自动发起离职审批流程', 'EVENT',
       'RECORD_CREATED_START_FLOW', 'ASYNC', 0, 'RECORD_CREATED',
       '{"and":[{"field":"lastWorkDate","op":"lte","value":"2026-12-31"},{"field":"documentStatus","op":"neq","value":"IN_PROCESS"}]}',
       'START_FLOW',
       '{"flowModelKey":"leave_multi","titleTemplate":"{employeeName}-离职申请","variableMapping":[{"formField":"employeeName","flowVariable":"employeeName","label":"员工姓名"},{"formField":"departmentName","flowVariable":"departmentName","label":"部门"},{"formField":"lastWorkDate","flowVariable":"lastWorkDate","label":"最后工作日"},{"formField":"leaveReason","flowVariable":"reason","label":"离职原因"},{"formField":"handoverOwnerId","flowVariable":"deptManager","label":"部门主管"}]}',
       1, 1, 1, NOW(), 1, 1, NOW()
WHERE NOT EXISTS (
  SELECT 1
  FROM ai_business_trigger
  WHERE tenant_id = 1
    AND object_code = 'LEAVE_APPLICATION'
    AND trigger_name = '离职申请到期自动发起流程'
);

INSERT INTO ai_business_trigger (id, tenant_id, suite_code, object_code, trigger_name, trigger_desc, trigger_type,
                                 scenario_type, blocking_mode, developer_mode, event_type, event_condition,
                                 action_type, action_config, status, sort_order, create_by, create_time,
                                 create_dept, update_by, update_time)
SELECT 1910000000000005212, 1, 'HR', 'LEAVE_APPLICATION', '离职审批通过后创建交接记录',
       '离职申请流程通过后自动生成离职交接记录', 'EVENT',
       'FLOW_APPROVED_CREATE_RECORD', 'ASYNC', 0, 'FLOW_APPROVED', NULL,
       'CREATE_RECORD',
       '{"targetConfigKey":"hr_leave_handover","fieldMapping":[{"sourceField":"id","targetField":"leaveApplicationId"},{"sourceField":"employeeName","targetField":"employeeName"},{"sourceField":"departmentName","targetField":"departmentName"},{"sourceField":"positionName","targetField":"positionName"},{"sourceField":"lastWorkDate","targetField":"lastWorkDate"},{"sourceField":"handoverOwnerId","targetField":"handoverOwnerId"},{"sourceField":"handoverOwnerName","targetField":"handoverOwnerName"}],"staticValues":{"handoverTitle":"离职交接待办","handoverStatus":"PENDING","remark":"由离职流程通过事件自动生成"}}',
       1, 2, 1, NOW(), 1, 1, NOW()
WHERE NOT EXISTS (
  SELECT 1
  FROM ai_business_trigger
  WHERE tenant_id = 1
    AND object_code = 'LEAVE_APPLICATION'
    AND trigger_name = '离职审批通过后创建交接记录'
);
