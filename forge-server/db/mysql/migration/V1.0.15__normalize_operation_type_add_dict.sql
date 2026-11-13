-- 统一操作日志新增类型字典值，避免 INSERT/ADD 同时展示为两个“新增”。

SET @operation_log_exists = (
  SELECT COUNT(1)
  FROM information_schema.tables
  WHERE table_schema = DATABASE()
    AND table_name = 'sys_operation_log'
);

SET @sql = IF(@operation_log_exists > 0,
  'UPDATE sys_operation_log
   SET operation_type = ''ADD''
   WHERE operation_type = ''INSERT''',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- sys_dict_data 存在 (tenant_id, dict_type, dict_value) 唯一键：
-- 有 ADD 时不能先把 INSERT 改成 ADD，否则会触发唯一键冲突。
UPDATE sys_dict_data
SET dict_label = '新增',
    dict_sort = 2,
    list_class = 'success',
    dict_status = 1,
    del_flag = 0,
    remark = '新增操作',
    update_by = 1,
    update_time = NOW()
WHERE dict_type = 'sys_operation_type'
  AND dict_value = 'ADD';

UPDATE sys_dict_data insert_item
JOIN sys_dict_data add_item ON add_item.tenant_id = insert_item.tenant_id
                           AND add_item.dict_type = 'sys_operation_type'
                           AND add_item.dict_value = 'ADD'
                           AND add_item.del_flag = 0
SET insert_item.dict_status = 0,
    insert_item.del_flag = 1,
    insert_item.remark = '历史 INSERT 新增类型已归并到 ADD',
    insert_item.update_by = 1,
    insert_item.update_time = NOW()
WHERE insert_item.dict_type = 'sys_operation_type'
  AND insert_item.dict_value = 'INSERT'
  AND insert_item.del_flag = 0;

UPDATE sys_dict_data insert_item
LEFT JOIN sys_dict_data add_item ON add_item.tenant_id = insert_item.tenant_id
                                AND add_item.dict_type = 'sys_operation_type'
                                AND add_item.dict_value = 'ADD'
SET insert_item.dict_label = '新增',
    insert_item.dict_value = 'ADD',
    insert_item.dict_sort = 2,
    insert_item.list_class = 'success',
    insert_item.dict_status = 1,
    insert_item.remark = '新增操作',
    insert_item.update_by = 1,
    insert_item.update_time = NOW()
WHERE insert_item.dict_type = 'sys_operation_type'
  AND insert_item.dict_value = 'INSERT'
  AND insert_item.del_flag = 0
  AND add_item.dict_code IS NULL;
