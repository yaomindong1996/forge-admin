-- 用户管理导入导出配置。
-- 说明：
-- 1. sys_user_export：用户列表导出，data_source_bean 指向 sysUserServiceImpl，query_method=selectExportList（不分页）。
-- 2. sys_user_import：用户批量导入模板配置，allow_import=1，导入接口由 SysUserController 自定义实现。
-- 3. 字典列（gender/userType/userStatus）配置 dict_type，导出时自动翻译为中文标签，导入时自动将标签转换为字典值。

-- ============ 导出配置 ============
INSERT INTO sys_excel_export_config (
  config_key, config_type, export_name, sheet_name, file_name_template, data_source_bean, query_method,
  auto_trans, pageable, max_rows, sort_field, sort_order, status, include_sample, allow_import,
  remark, create_time, update_time, create_by, update_by, create_dept
)
SELECT seed.config_key, seed.config_type, seed.export_name, seed.sheet_name, seed.file_name_template, seed.data_source_bean, seed.query_method,
       seed.auto_trans, seed.pageable, seed.max_rows, seed.sort_field, seed.sort_order, seed.status, seed.include_sample, seed.allow_import,
       seed.remark, NOW(), NOW(), 1, 1, 1
FROM (
  SELECT 'sys_user_export' config_key,
         'EXPORT' config_type,
         '用户列表导出' export_name,
         '用户列表' sheet_name,
         '用户列表_{date}.xlsx' file_name_template,
         'sysUserServiceImpl' data_source_bean,
         'selectExportList' query_method,
         1 auto_trans,
         0 pageable,
         50000 max_rows,
         'create_time' sort_field,
         'DESC' sort_order,
         1 status,
         0 include_sample,
         0 allow_import,
         '用户列表导出配置' remark
) seed
WHERE NOT EXISTS (
  SELECT 1
  FROM sys_excel_export_config c
  WHERE c.config_key = seed.config_key
);

-- 导出列配置
INSERT INTO sys_excel_column_config (
  config_key, field_name, column_name, width, order_num, export, date_format, number_format,
  dict_type, importable, required, example_value, validation_rule, validation_message,
  create_time, update_time, create_by, update_by, create_dept
)
SELECT seed.config_key, seed.field_name, seed.column_name, seed.width, seed.order_num, 1, seed.date_format, NULL,
       seed.dict_type, 0, 0, NULL, NULL, NULL, NOW(), NOW(), 1, 1, 1
FROM (
  SELECT 'sys_user_export' config_key, 'username' field_name, '用户名' column_name, 18 width, 1 order_num, NULL date_format, NULL dict_type
  UNION ALL SELECT 'sys_user_export', 'realName', '真实姓名', 18, 2, NULL, NULL
  UNION ALL SELECT 'sys_user_export', 'phone', '手机号', 18, 3, NULL, NULL
  UNION ALL SELECT 'sys_user_export', 'email', '邮箱', 24, 4, NULL, NULL
  UNION ALL SELECT 'sys_user_export', 'gender', '性别', 10, 5, NULL, 'sys_user_sex'
  UNION ALL SELECT 'sys_user_export', 'userType', '用户类型', 12, 6, NULL, 'sys_user_type'
  UNION ALL SELECT 'sys_user_export', 'orgName', '所属组织', 24, 7, NULL, NULL
  UNION ALL SELECT 'sys_user_export', 'postName', '岗位', 24, 8, NULL, NULL
  UNION ALL SELECT 'sys_user_export', 'tenantName', '租户', 18, 9, NULL, NULL
  UNION ALL SELECT 'sys_user_export', 'userStatus', '状态', 10, 10, NULL, 'sys_user_status'
  UNION ALL SELECT 'sys_user_export', 'remark', '备注', 30, 11, NULL, NULL
  UNION ALL SELECT 'sys_user_export', 'createTime', '创建时间', 22, 12, 'yyyy-MM-dd HH:mm:ss', NULL
) seed
WHERE NOT EXISTS (
  SELECT 1
  FROM sys_excel_column_config c
  WHERE c.config_key = seed.config_key
    AND c.field_name = seed.field_name
);

-- ============ 导入配置 ============
INSERT INTO sys_excel_export_config (
  config_key, config_type, export_name, sheet_name, file_name_template, data_source_bean, query_method,
  auto_trans, pageable, max_rows, sort_field, sort_order, status, include_sample, allow_import,
  remark, create_time, update_time, create_by, update_by, create_dept
)
SELECT seed.config_key, seed.config_type, seed.export_name, seed.sheet_name, seed.file_name_template, seed.data_source_bean, seed.query_method,
       seed.auto_trans, seed.pageable, seed.max_rows, seed.sort_field, seed.sort_order, seed.status, seed.include_sample, seed.allow_import,
       seed.remark, NOW(), NOW(), 1, 1, 1
FROM (
  SELECT 'sys_user_import' config_key,
         'IMPORT' config_type,
         '用户批量导入' export_name,
         '用户导入' sheet_name,
         '用户导入模板.xlsx' file_name_template,
         NULL data_source_bean,
         NULL query_method,
         1 auto_trans,
         0 pageable,
         5000 max_rows,
         NULL sort_field,
         NULL sort_order,
         1 status,
         1 include_sample,
         1 allow_import,
         '用户批量导入模板配置' remark
) seed
WHERE NOT EXISTS (
  SELECT 1
  FROM sys_excel_export_config c
  WHERE c.config_key = seed.config_key
);

-- 导入列配置
INSERT INTO sys_excel_column_config (
  config_key, field_name, column_name, width, order_num, export, date_format, number_format,
  dict_type, importable, required, example_value, validation_rule, validation_message,
  create_time, update_time, create_by, update_by, create_dept
)
SELECT seed.config_key, seed.field_name, seed.column_name, seed.width, seed.order_num, 0, NULL, NULL,
       seed.dict_type, 1, seed.required, seed.example_value, seed.validation_rule, seed.validation_message,
       NOW(), NOW(), 1, 1, 1
FROM (
  SELECT 'sys_user_import' config_key, 'username' field_name, '用户名' column_name, 18 width, 1 order_num, NULL dict_type, 1 required, 'admin' example_value, NULL validation_rule, NULL validation_message
  UNION ALL SELECT 'sys_user_import', 'realName', '真实姓名', 18, 2, NULL, 1, '管理员', NULL, NULL
  UNION ALL SELECT 'sys_user_import', 'phone', '手机号', 18, 3, NULL, 1, '13800138000', '^1[3-9]\\d{9}$', '请输入正确的手机号'
  UNION ALL SELECT 'sys_user_import', 'password', '密码', 18, 4, NULL, 1, '123456', NULL, NULL
  UNION ALL SELECT 'sys_user_import', 'email', '邮箱', 24, 5, NULL, 0, 'admin@example.com', NULL, NULL
  UNION ALL SELECT 'sys_user_import', 'gender', '性别', 10, 6, 'sys_user_sex', 0, '男', NULL, NULL
  UNION ALL SELECT 'sys_user_import', 'userType', '用户类型', 12, 7, 'sys_user_type', 0, '普通用户', NULL, NULL
  UNION ALL SELECT 'sys_user_import', 'remark', '备注', 30, 8, NULL, 0, NULL, NULL, NULL
) seed
WHERE NOT EXISTS (
  SELECT 1
  FROM sys_excel_column_config c
  WHERE c.config_key = seed.config_key
    AND c.field_name = seed.field_name
);
