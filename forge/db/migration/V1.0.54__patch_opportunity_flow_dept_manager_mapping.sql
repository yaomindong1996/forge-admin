-- Phase 8 verification fix: leave_multi uses {deptManager} on its first user task.
-- CRM opportunity sample bindings must provide that process variable.

UPDATE ai_business_binding
SET binding_config = JSON_ARRAY_APPEND(
    CAST(binding_config AS JSON),
    '$.variableMapping',
    JSON_OBJECT('formField', 'createBy', 'flowVariable', 'deptManager', 'label', '部门经理')
  ),
  update_by = 1,
  update_time = NOW()
WHERE tenant_id = 1
  AND target_type = 'OBJECT'
  AND target_code = 'OPPORTUNITY'
  AND binding_type = 'FLOW'
  AND binding_key = 'leave_multi'
  AND JSON_VALID(binding_config)
  AND JSON_SEARCH(CAST(binding_config AS JSON), 'one', 'deptManager', NULL, '$.variableMapping[*].flowVariable') IS NULL;

UPDATE ai_business_trigger
SET action_config = JSON_ARRAY_APPEND(
    CAST(action_config AS JSON),
    '$.variableMapping',
    JSON_OBJECT('formField', 'createBy', 'flowVariable', 'deptManager', 'label', '部门经理')
  ),
  update_by = 1,
  update_time = NOW()
WHERE tenant_id = 1
  AND object_code = 'OPPORTUNITY'
  AND action_type = 'START_FLOW'
  AND JSON_VALID(action_config)
  AND JSON_SEARCH(CAST(action_config AS JSON), 'one', 'deptManager', NULL, '$.variableMapping[*].flowVariable') IS NULL;
