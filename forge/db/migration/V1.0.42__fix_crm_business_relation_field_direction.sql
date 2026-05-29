-- 修正 CRM 样板关系字段方向。
-- CHILD_LIST / DETAIL 关系约定：source_field_code 为当前对象字段，target_field_code 为目标对象字段。

UPDATE ai_business_object_relation
SET source_field_code = 'id',
    target_field_code = 'customerId',
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND suite_code = 'CRM'
  AND source_object_code = 'CUSTOMER'
  AND target_object_code IN ('CONTACT', 'OPPORTUNITY', 'FOLLOW_RECORD')
  AND relation_type = 'CHILD_LIST'
  AND source_field_code = 'customerId'
  AND target_field_code = 'id';

UPDATE ai_business_object_relation
SET source_field_code = 'id',
    target_field_code = 'opportunityId',
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND suite_code = 'CRM'
  AND source_object_code = 'OPPORTUNITY'
  AND target_object_code = 'FOLLOW_RECORD'
  AND relation_type = 'CHILD_LIST'
  AND source_field_code = 'opportunityId'
  AND target_field_code = 'id';
