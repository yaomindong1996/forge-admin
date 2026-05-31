-- Add the reverse CRM relation used by the contact list to display customer names.
-- CUSTOMER -> CONTACT is the detail relation; CONTACT -> CUSTOMER is the lookup relation.

UPDATE ai_business_object_relation
SET relation_config = JSON_SET(
        COALESCE(relation_config, JSON_OBJECT()),
        '$.displayField', 'contactName',
        '$.detailTabTitle', COALESCE(
            JSON_UNQUOTE(JSON_EXTRACT(relation_config, '$.detailTabTitle')),
            JSON_UNQUOTE(JSON_EXTRACT(relation_config, '$.detailTab')),
            '联系人'
        )
    ),
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND suite_code = 'CRM'
  AND source_object_code = 'CUSTOMER'
  AND target_object_code = 'CONTACT'
  AND relation_type = 'CHILD_LIST';

INSERT INTO ai_business_object_relation (
    id,
    tenant_id,
    suite_code,
    source_object_code,
    target_object_code,
    relation_type,
    relation_name,
    source_field_code,
    target_field_code,
    relation_config,
    description,
    status,
    sort_order,
    create_by,
    create_time,
    create_dept,
    update_by,
    update_time
)
SELECT
    1910000000000000210,
    1,
    'CRM',
    'CONTACT',
    'CUSTOMER',
    'REFERENCE',
    '联系人属于客户',
    'customerId',
    'id',
    JSON_OBJECT(
        'displayField', 'customerName',
        'businessText', '联系人列表通过客户名称回显所属客户',
        'showInDetail', false,
        'inlineCreateEnabled', false,
        'inlineEditEnabled', false
    ),
    '联系人通过 customerId 归属客户，运行态列表回显客户名称',
    1,
    10,
    1,
    NOW(),
    1,
    1,
    NOW()
WHERE NOT EXISTS (
    SELECT 1
    FROM ai_business_object_relation
    WHERE tenant_id = 1
      AND suite_code = 'CRM'
      AND source_object_code = 'CONTACT'
      AND target_object_code = 'CUSTOMER'
      AND relation_type = 'REFERENCE'
      AND relation_name = '联系人属于客户'
)
AND NOT EXISTS (
    SELECT 1
    FROM ai_business_object_relation
    WHERE tenant_id = 1
      AND id = 1910000000000000210
);

UPDATE ai_business_object_relation
SET source_field_code = 'customerId',
    target_field_code = 'id',
    relation_config = JSON_SET(
        COALESCE(relation_config, JSON_OBJECT()),
        '$.displayField', 'customerName'
    ),
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND suite_code = 'CRM'
  AND source_object_code = 'CONTACT'
  AND target_object_code = 'CUSTOMER'
  AND relation_type = 'REFERENCE';
