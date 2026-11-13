-- 为流程监控实例列表接入角色数据权限范围。

INSERT INTO sys_data_scope_config (
  tenant_id,
  resource_code,
  resource_name,
  mapper_method,
  table_alias,
  user_id_column,
  org_id_column,
  tenant_id_column,
  region_code_column,
  user_region_column,
  user_table_alias,
  enabled,
  remark,
  create_by,
  create_time,
  update_by,
  update_time,
  create_dept,
  del_flag
)
SELECT
  1,
  'flow:monitor:view',
  '流程监控实例列表',
  'com.mdframe.forge.starter.flow.mapper.FlowBusinessMapper.selectMonitorBusinessPage',
  '',
  'apply_user_id',
  'apply_dept_id',
  'tenant_id',
  NULL,
  NULL,
  NULL,
  1,
  '流程监控实例列表数据权限控制',
  1,
  NOW(),
  1,
  NOW(),
  1,
  0
WHERE NOT EXISTS (
  SELECT 1
  FROM sys_data_scope_config existing_config
  WHERE existing_config.tenant_id = 1
    AND existing_config.mapper_method = 'com.mdframe.forge.starter.flow.mapper.FlowBusinessMapper.selectMonitorBusinessPage'
    AND existing_config.del_flag = 0
);
