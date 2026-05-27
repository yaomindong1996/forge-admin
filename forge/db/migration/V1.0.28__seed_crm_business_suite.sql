-- CRM 样板业务套件、业务对象、关系、能力挂接和应用入口。

INSERT INTO ai_business_suite (id, tenant_id, suite_code, suite_name, icon, description, status, sort_order, options,
                               create_by, create_time, create_dept, update_by, update_time)
SELECT 1910000000000000001, 1, 'CRM', 'CRM', 'ionicons5:PeopleOutline',
       '围绕客户、联系人、线索、商机、合同、回款和销售任务组织的客户关系管理业务套件',
       1, 1, '{"sample":true,"stage":"phase1"}', 1, NOW(), 1, 1, NOW()
WHERE NOT EXISTS (
  SELECT 1
  FROM ai_business_suite
  WHERE tenant_id = 1
    AND suite_code = 'CRM'
);

INSERT INTO ai_business_object (id, tenant_id, suite_code, object_code, object_name, object_type, model_id, model_code,
                                display_field, icon, description, status, sort_order, options, create_by, create_time,
                                create_dept, update_by, update_time)
SELECT seed.id, 1, 'CRM', seed.object_code, seed.object_name, seed.object_type, NULL, seed.model_code,
       seed.display_field, seed.icon, seed.description, 1, seed.sort_order, seed.options,
       1, NOW(), 1, 1, NOW()
FROM (
  SELECT 1910000000000000101 id, 'CUSTOMER' object_code, '客户' object_name, 'MASTER' object_type, 'crm_customer' model_code, 'customerName' display_field, 'ionicons5:BusinessOutline' icon, '管理客户基本信息、客户分级、所属区域和客户生命周期' description, 1 sort_order, '{"sample":true,"capabilityGroup":"customer"}' options
  UNION ALL SELECT 1910000000000000102, 'CONTACT', '联系人', 'DETAIL', 'crm_contact', 'contactName', 'ionicons5:PersonOutline', '管理客户联系人、角色、联系方式和主要联系人标记', 2, '{"sample":true,"capabilityGroup":"customer"}'
  UNION ALL SELECT 1910000000000000103, 'LEAD', '线索', 'MASTER', 'crm_lead', 'leadName', 'ionicons5:SparklesOutline', '管理销售线索、来源、跟进状态和转化进度', 3, '{"sample":true,"capabilityGroup":"sales"}'
  UNION ALL SELECT 1910000000000000104, 'OPPORTUNITY', '商机', 'TRANSACTION', 'crm_opportunity', 'opportunityName', 'ionicons5:TrendingUpOutline', '管理商机阶段、预计金额、赢单概率和推进动作', 4, '{"sample":true,"capabilityGroup":"sales"}'
  UNION ALL SELECT 1910000000000000105, 'CONTRACT', '合同', 'TRANSACTION', 'crm_contract', 'contractName', 'ionicons5:DocumentTextOutline', '管理合同信息、合同审批、归档和履约状态', 5, '{"sample":true,"capabilityGroup":"contract"}'
  UNION ALL SELECT 1910000000000000106, 'CONTRACT_ITEM', '合同明细', 'DETAIL', 'crm_contract_item', 'itemName', 'ionicons5:ListOutline', '管理合同商品、服务、数量、单价和金额明细', 6, '{"sample":true,"capabilityGroup":"contract"}'
  UNION ALL SELECT 1910000000000000107, 'PAYMENT', '回款', 'TRANSACTION', 'crm_payment', 'paymentNo', 'ionicons5:WalletOutline', '管理回款计划、回款记录和逾期提醒', 7, '{"sample":true,"capabilityGroup":"finance"}'
  UNION ALL SELECT 1910000000000000108, 'FOLLOW_RECORD', '跟进记录', 'DETAIL', 'crm_follow_record', 'subject', 'ionicons5:ChatbubblesOutline', '管理客户拜访、电话、会议和商机推进记录', 8, '{"sample":true,"capabilityGroup":"activity"}'
  UNION ALL SELECT 1910000000000000109, 'SALES_TASK', '销售任务', 'TRANSACTION', 'crm_sales_task', 'taskName', 'ionicons5:CheckboxOutline', '管理销售计划、待办任务和任务完成情况', 9, '{"sample":true,"capabilityGroup":"sales"}'
) seed
WHERE NOT EXISTS (
  SELECT 1
  FROM ai_business_object o
  WHERE o.tenant_id = 1
    AND o.suite_code = 'CRM'
    AND o.object_code = seed.object_code
);

INSERT INTO ai_business_object_relation (id, tenant_id, suite_code, source_object_code, target_object_code,
                                         relation_type, relation_name, source_field_code, target_field_code,
                                         relation_config, description, status, sort_order, create_by, create_time,
                                         create_dept, update_by, update_time)
SELECT seed.id, 1, 'CRM', seed.source_object_code, seed.target_object_code, seed.relation_type, seed.relation_name,
       seed.source_field_code, seed.target_field_code, seed.relation_config, seed.description, 1, seed.sort_order,
       1, NOW(), 1, 1, NOW()
FROM (
  SELECT 1910000000000000201 id, 'CUSTOMER' source_object_code, 'CONTACT' target_object_code, 'CHILD_LIST' relation_type, '客户联系人' relation_name, 'customerId' source_field_code, 'id' target_field_code, '{"detailTab":"联系人","businessText":"客户详情中展示联系人列表"}' relation_config, '客户详情中展示联系人列表' description, 1 sort_order
  UNION ALL SELECT 1910000000000000202, 'CUSTOMER', 'OPPORTUNITY', 'CHILD_LIST', '客户商机', 'customerId', 'id', '{"detailTab":"商机","businessText":"客户详情中展示相关商机"}', '客户详情中展示相关商机', 2
  UNION ALL SELECT 1910000000000000203, 'OPPORTUNITY', 'CONTRACT', 'REFERENCE', '商机关联合同', 'id', 'opportunityId', '{"businessText":"合同可关联来源商机"}', '合同可关联来源商机', 3
  UNION ALL SELECT 1910000000000000204, 'CONTRACT', 'CONTRACT_ITEM', 'DETAIL', '合同明细', 'id', 'contractId', '{"detailTab":"合同明细","summaryField":"amount"}', '合同明细依附合同并支持金额汇总', 4
  UNION ALL SELECT 1910000000000000205, 'CONTRACT', 'PAYMENT', 'CHILD_LIST', '合同回款', 'id', 'contractId', '{"detailTab":"回款","businessText":"合同详情中展示回款计划或记录"}', '合同详情中展示回款计划或回款记录', 5
  UNION ALL SELECT 1910000000000000206, 'CUSTOMER', 'FOLLOW_RECORD', 'CHILD_LIST', '客户跟进记录', 'customerId', 'id', '{"detailTab":"跟进记录","businessText":"客户详情中展示拜访、电话、会议记录"}', '客户详情中展示拜访、电话、会议记录', 6
  UNION ALL SELECT 1910000000000000207, 'OPPORTUNITY', 'FOLLOW_RECORD', 'CHILD_LIST', '商机跟进记录', 'opportunityId', 'id', '{"detailTab":"跟进记录","businessText":"商机详情中展示推进记录"}', '商机详情中展示推进记录', 7
) seed
WHERE NOT EXISTS (
  SELECT 1
  FROM ai_business_object_relation r
  WHERE r.tenant_id = 1
    AND r.suite_code = 'CRM'
    AND r.source_object_code = seed.source_object_code
    AND r.target_object_code = seed.target_object_code
    AND r.relation_type = seed.relation_type
    AND r.relation_name = seed.relation_name
);

INSERT INTO ai_business_binding (id, tenant_id, target_type, target_id, target_code, binding_type, binding_key,
                                 binding_name, binding_config, description, status, sort_order, create_by,
                                 create_time, create_dept, update_by, update_time)
SELECT seed.id, 1, 'OBJECT', seed.target_id, seed.target_code, seed.binding_type, seed.binding_key,
       seed.binding_name, seed.binding_config, seed.description, 1, seed.sort_order, 1, NOW(), 1, 1, NOW()
FROM (
  SELECT 1910000000000000401 id, 1910000000000000101 target_id, 'CUSTOMER' target_code, 'IMPORT' binding_type, 'import_customer' binding_key, '导入客户' binding_name, '{"template":"customer_import"}' binding_config, '客户对象导入能力' description, 1 sort_order
  UNION ALL SELECT 1910000000000000402, 1910000000000000102, 'CONTACT', 'IMPORT', 'import_contact', '导入联系人', '{"template":"contact_import"}', '联系人对象导入能力', 2
  UNION ALL SELECT 1910000000000000403, 1910000000000000105, 'CONTRACT', 'IMPORT', 'import_contract', '导入合同', '{"template":"contract_import"}', '合同对象导入能力', 3
  UNION ALL SELECT 1910000000000000404, 1910000000000000101, 'CUSTOMER', 'EXPORT', 'export_customer', '导出客户', '{"template":"customer_export"}', '客户对象导出能力', 4
  UNION ALL SELECT 1910000000000000405, 1910000000000000102, 'CONTACT', 'EXPORT', 'export_contact', '导出联系人', '{"template":"contact_export"}', '联系人对象导出能力', 5
  UNION ALL SELECT 1910000000000000406, 1910000000000000105, 'CONTRACT', 'EXPORT', 'export_contract', '导出合同', '{"template":"contract_export"}', '合同对象导出能力', 6
  UNION ALL SELECT 1910000000000000407, 1910000000000000101, 'CUSTOMER', 'REPORT', 'customer_report', '客户报表', '{"reportType":"CUSTOMER_GROWTH"}', '客户增长和客户结构分析报表', 7
  UNION ALL SELECT 1910000000000000408, 1910000000000000104, 'OPPORTUNITY', 'REPORT', 'sales_funnel_report', '销售漏斗', '{"reportType":"SALES_FUNNEL"}', '商机阶段和销售漏斗分析报表', 8
  UNION ALL SELECT 1910000000000000409, 1910000000000000107, 'PAYMENT', 'REPORT', 'payment_report', '回款报表', '{"reportType":"PAYMENT_ANALYSIS"}', '回款计划、回款记录和逾期分析报表', 9
  UNION ALL SELECT 1910000000000000410, 1910000000000000105, 'CONTRACT', 'TRIGGER', 'contract_amount_summary', '合同金额汇总', '{"action":"SUM_DETAIL_AMOUNT"}', '根据合同明细汇总合同金额', 10
  UNION ALL SELECT 1910000000000000411, 1910000000000000104, 'OPPORTUNITY', 'TRIGGER', 'opportunity_stage_reminder', '商机阶段提醒', '{"action":"STAGE_CHANGE_MESSAGE"}', '商机阶段变化后生成提醒', 11
  UNION ALL SELECT 1910000000000000412, 1910000000000000107, 'PAYMENT', 'TRIGGER', 'payment_overdue_reminder', '回款逾期提醒', '{"action":"OVERDUE_TODO"}', '回款逾期后生成待办提醒', 12
  UNION ALL SELECT 1910000000000000413, 1910000000000000105, 'CONTRACT', 'APPROVAL', 'contract_approval', '合同审批', '{"flowKey":"crm_contract_approval"}', '合同提交后进入审批流程', 13
  UNION ALL SELECT 1910000000000000414, 1910000000000000104, 'OPPORTUNITY', 'MESSAGE', 'opportunity_follow_reminder', '商机跟进提醒', '{"messageType":"FOLLOW_REMINDER"}', '商机长期未跟进时提醒销售人员', 14
  UNION ALL SELECT 1910000000000000415, 1910000000000000107, 'PAYMENT', 'MESSAGE', 'payment_overdue_message', '回款逾期通知', '{"messageType":"OVERDUE_PAYMENT"}', '回款逾期后发送消息通知', 15
  UNION ALL SELECT 1910000000000000416, 1910000000000000101, 'CUSTOMER', 'PERMISSION', 'customer_permission', '客户对象权限', '{"scope":"OWNER_DEPT_REGION"}', '客户按负责人、部门和区域进行数据可见控制', 16
  UNION ALL SELECT 1910000000000000417, 1910000000000000105, 'CONTRACT', 'PERMISSION', 'contract_permission', '合同对象权限', '{"scope":"OWNER_DEPT"}', '合同按负责人和部门进行数据可见控制', 17
  UNION ALL SELECT 1910000000000000418, 1910000000000000107, 'PAYMENT', 'PERMISSION', 'payment_permission', '回款对象权限', '{"scope":"CONTRACT_SCOPE"}', '回款继承合同范围的数据可见控制', 18
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

INSERT INTO ai_business_app (id, tenant_id, app_code, app_name, app_type, suite_code, object_code, entry_mode,
                             entry_url, config_key, icon, description, status, sort_order, options, create_by,
                             create_time, create_dept, update_by, update_time)
SELECT seed.id, 1, seed.app_code, seed.app_name, 'BUSINESS', 'CRM', seed.object_code, 'RUNTIME',
       seed.entry_url, seed.config_key, seed.icon, seed.description, 1, seed.sort_order,
       '{"sample":true,"entryGroup":"crm-standard"}', 1, NOW(), 1, 1, NOW()
FROM (
  SELECT 1910000000000000301 id, 'CRM_CUSTOMER_MANAGE' app_code, '客户管理' app_name, 'CUSTOMER' object_code, '/ai/crud-page/crm_customer' entry_url, 'crm_customer' config_key, 'ionicons5:BusinessOutline' icon, '客户标准后台业务入口' description, 1 sort_order
  UNION ALL SELECT 1910000000000000302, 'CRM_CONTACT_MANAGE', '联系人管理', 'CONTACT', '/ai/crud-page/crm_contact', 'crm_contact', 'ionicons5:PersonOutline', '联系人标准后台业务入口', 2
  UNION ALL SELECT 1910000000000000303, 'CRM_LEAD_MANAGE', '线索管理', 'LEAD', '/ai/crud-page/crm_lead', 'crm_lead', 'ionicons5:SparklesOutline', '线索标准后台业务入口', 3
  UNION ALL SELECT 1910000000000000304, 'CRM_OPPORTUNITY_MANAGE', '商机管理', 'OPPORTUNITY', '/ai/crud-page/crm_opportunity', 'crm_opportunity', 'ionicons5:TrendingUpOutline', '商机标准后台业务入口', 4
  UNION ALL SELECT 1910000000000000305, 'CRM_CONTRACT_MANAGE', '合同管理', 'CONTRACT', '/ai/crud-page/crm_contract', 'crm_contract', 'ionicons5:DocumentTextOutline', '合同标准后台业务入口', 5
  UNION ALL SELECT 1910000000000000306, 'CRM_PAYMENT_MANAGE', '回款管理', 'PAYMENT', '/ai/crud-page/crm_payment', 'crm_payment', 'ionicons5:WalletOutline', '回款标准后台业务入口', 6
  UNION ALL SELECT 1910000000000000307, 'CRM_FOLLOW_RECORD_MANAGE', '跟进记录', 'FOLLOW_RECORD', '/ai/crud-page/crm_follow_record', 'crm_follow_record', 'ionicons5:ChatbubblesOutline', '跟进记录标准后台业务入口', 7
  UNION ALL SELECT 1910000000000000308, 'CRM_SALES_TASK_MANAGE', '销售任务', 'SALES_TASK', '/ai/crud-page/crm_sales_task', 'crm_sales_task', 'ionicons5:CheckboxOutline', '销售任务标准后台业务入口', 8
) seed
WHERE NOT EXISTS (
  SELECT 1
  FROM ai_business_app a
  WHERE a.tenant_id = 1
    AND a.app_code = seed.app_code
);
