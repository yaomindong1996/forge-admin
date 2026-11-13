ALTER TABLE sys_tenant
    MODIFY COLUMN system_layout VARCHAR(50) NULL DEFAULT 'business-workbench'
    COMMENT '系统布局（默认 business-workbench）';

UPDATE sys_tenant
SET system_layout = 'business-workbench',
    update_by = COALESCE(update_by, 1),
    update_time = CURRENT_TIMESTAMP
WHERE del_flag = 0
  AND (
    system_layout IS NULL
    OR TRIM(system_layout) = ''
    OR system_layout = 'default'
    OR id = 1
  );
