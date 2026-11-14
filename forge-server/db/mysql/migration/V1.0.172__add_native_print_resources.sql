-- 仅登记字典和权限资源，不自动向任何角色授予权限，不新增未完成的页面菜单。
INSERT INTO sys_dict_type (tenant_id, dict_name, dict_type, dict_status, remark, create_by, create_time, update_by, update_time, create_dept)
SELECT 1, '打印设计状态', 'sys_print_design_status', 1, 'Forge 原生打印', 1, NOW(), 1, NOW(), 1
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE tenant_id = 1 AND dict_type = 'sys_print_design_status');

INSERT INTO sys_dict_data (tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, dict_status, remark, create_by, create_time, update_by, update_time, create_dept)
SELECT 1, 1, '草稿', 'DRAFT', 'sys_print_design_status', NULL, 'default', 'N', 1, 'Forge 原生打印', 1, NOW(), 1, NOW(), 1
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE tenant_id = 1 AND dict_type = 'sys_print_design_status' AND dict_value = 'DRAFT');

INSERT INTO sys_dict_data (tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, dict_status, remark, create_by, create_time, update_by, update_time, create_dept)
SELECT 1, 2, '已发布', 'PUBLISHED', 'sys_print_design_status', NULL, 'success', 'N', 1, 'Forge 原生打印', 1, NOW(), 1, NOW(), 1
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE tenant_id = 1 AND dict_type = 'sys_print_design_status' AND dict_value = 'PUBLISHED');

INSERT INTO sys_dict_data (tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, dict_status, remark, create_by, create_time, update_by, update_time, create_dept)
SELECT 1, 3, '有未发布变更', 'CHANGED', 'sys_print_design_status', NULL, 'warning', 'N', 1, 'Forge 原生打印', 1, NOW(), 1, NOW(), 1
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE tenant_id = 1 AND dict_type = 'sys_print_design_status' AND dict_value = 'CHANGED');

INSERT INTO sys_dict_type (tenant_id, dict_name, dict_type, dict_status, remark, create_by, create_time, update_by, update_time, create_dept)
SELECT 1, '打印执行事件', 'sys_print_execution_result', 1, 'Forge 原生打印', 1, NOW(), 1, NOW(), 1
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE tenant_id = 1 AND dict_type = 'sys_print_execution_result');

INSERT INTO sys_dict_data (tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, dict_status, remark, create_by, create_time, update_by, update_time, create_dept)
SELECT 1, 1, '已准备', 'PREPARED', 'sys_print_execution_result', NULL, 'info', 'N', 1, 'Forge 原生打印', 1, NOW(), 1, NOW(), 1
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE tenant_id = 1 AND dict_type = 'sys_print_execution_result' AND dict_value = 'PREPARED');

INSERT INTO sys_dict_data (tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, dict_status, remark, create_by, create_time, update_by, update_time, create_dept)
SELECT 1, 2, '已调用打印对话框', 'DIALOG_OPENED', 'sys_print_execution_result', NULL, 'info', 'N', 1, 'Forge 原生打印', 1, NOW(), 1, NOW(), 1
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE tenant_id = 1 AND dict_type = 'sys_print_execution_result' AND dict_value = 'DIALOG_OPENED');

INSERT INTO sys_dict_data (tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, dict_status, remark, create_by, create_time, update_by, update_time, create_dept)
SELECT 1, 3, '失败', 'FAILED', 'sys_print_execution_result', NULL, 'error', 'N', 1, 'Forge 原生打印', 1, NOW(), 1, NOW(), 1
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE tenant_id = 1 AND dict_type = 'sys_print_execution_result' AND dict_value = 'FAILED');

INSERT INTO sys_dict_type (tenant_id, dict_name, dict_type, dict_status, remark, create_by, create_time, update_by, update_time, create_dept)
SELECT 1, '打印使用场景', 'sys_print_scene', 1, 'Forge 原生打印', 1, NOW(), 1, NOW(), 1
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE tenant_id = 1 AND dict_type = 'sys_print_scene');

INSERT INTO sys_dict_data (tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, dict_status, remark, create_by, create_time, update_by, update_time, create_dept)
SELECT 1, 1, '列表', 'LIST', 'sys_print_scene', NULL, 'default', 'N', 1, 'Forge 原生打印', 1, NOW(), 1, NOW(), 1
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE tenant_id = 1 AND dict_type = 'sys_print_scene' AND dict_value = 'LIST');

INSERT INTO sys_dict_data (tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, dict_status, remark, create_by, create_time, update_by, update_time, create_dept)
SELECT 1, 2, '详情', 'DETAIL', 'sys_print_scene', NULL, 'default', 'N', 1, 'Forge 原生打印', 1, NOW(), 1, NOW(), 1
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE tenant_id = 1 AND dict_type = 'sys_print_scene' AND dict_value = 'DETAIL');

INSERT INTO sys_dict_data (tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, dict_status, remark, create_by, create_time, update_by, update_time, create_dept)
SELECT 1, 3, '待办', 'FLOW_TODO', 'sys_print_scene', NULL, 'default', 'N', 1, 'Forge 原生打印', 1, NOW(), 1, NOW(), 1
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE tenant_id = 1 AND dict_type = 'sys_print_scene' AND dict_value = 'FLOW_TODO');

INSERT INTO sys_dict_data (tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, dict_status, remark, create_by, create_time, update_by, update_time, create_dept)
SELECT 1, 4, '已办', 'FLOW_DONE', 'sys_print_scene', NULL, 'default', 'N', 1, 'Forge 原生打印', 1, NOW(), 1, NOW(), 1
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE tenant_id = 1 AND dict_type = 'sys_print_scene' AND dict_value = 'FLOW_DONE');

INSERT INTO sys_dict_data (tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, dict_status, remark, create_by, create_time, update_by, update_time, create_dept)
SELECT 1, 5, '我发起', 'FLOW_STARTED', 'sys_print_scene', NULL, 'default', 'N', 1, 'Forge 原生打印', 1, NOW(), 1, NOW(), 1
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE tenant_id = 1 AND dict_type = 'sys_print_scene' AND dict_value = 'FLOW_STARTED');

INSERT INTO sys_dict_type (tenant_id, dict_name, dict_type, dict_status, remark, create_by, create_time, update_by, update_time, create_dept)
SELECT 1, '打印来源类型', 'sys_print_source_type', 1, 'Forge 原生打印', 1, NOW(), 1, NOW(), 1
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE tenant_id = 1 AND dict_type = 'sys_print_source_type');

INSERT INTO sys_dict_data (tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, dict_status, remark, create_by, create_time, update_by, update_time, create_dept)
SELECT 1, 1, '低代码表单', 'LOWCODE', 'sys_print_source_type', NULL, 'default', 'N', 1, 'Forge 原生打印', 1, NOW(), 1, NOW(), 1
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE tenant_id = 1 AND dict_type = 'sys_print_source_type' AND dict_value = 'LOWCODE');

INSERT INTO sys_dict_data (tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, dict_status, remark, create_by, create_time, update_by, update_time, create_dept)
SELECT 1, 2, '代码业务表单', 'CODE', 'sys_print_source_type', NULL, 'default', 'N', 1, 'Forge 原生打印', 1, NOW(), 1, NOW(), 1
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE tenant_id = 1 AND dict_type = 'sys_print_source_type' AND dict_value = 'CODE');

INSERT INTO sys_dict_type (tenant_id, dict_name, dict_type, dict_status, remark, create_by, create_time, update_by, update_time, create_dept)
SELECT 1, '打印数据模式', 'sys_print_data_mode', 1, 'Forge 原生打印', 1, NOW(), 1, NOW(), 1
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE tenant_id = 1 AND dict_type = 'sys_print_data_mode');

INSERT INTO sys_dict_data (tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, dict_status, remark, create_by, create_time, update_by, update_time, create_dept)
SELECT 1, 1, '当前数据', 'CURRENT', 'sys_print_data_mode', NULL, 'info', 'N', 1, 'Forge 原生打印', 1, NOW(), 1, NOW(), 1
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE tenant_id = 1 AND dict_type = 'sys_print_data_mode' AND dict_value = 'CURRENT');

SET @print_application_menu_id = (SELECT id FROM sys_resource WHERE tenant_id = 1 AND path = '/app-center' AND resource_type = 2 AND del_flag = 0 ORDER BY id LIMIT 1);

INSERT INTO sys_resource (tenant_id, resource_name, parent_id, resource_type, sort, is_external, open_target, is_public, menu_status, visible, perms, keep_alive, always_show, remark, create_by, create_time, update_by, update_time, create_dept, client_code)
SELECT 1, '查看打印模板', @print_application_menu_id, 3, 91, 0, '_self', 0, 1, 1, 'print:template:view', 0, 0, '还须通过应用、来源和记录授权', 1, NOW(), 1, NOW(), 1, 'pc'
WHERE @print_application_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_resource WHERE tenant_id = 1 AND perms = 'print:template:view' AND del_flag = 0);

INSERT INTO sys_resource (tenant_id, resource_name, parent_id, resource_type, sort, is_external, open_target, is_public, menu_status, visible, perms, keep_alive, always_show, remark, create_by, create_time, update_by, update_time, create_dept, client_code)
SELECT 1, '管理打印模板', @print_application_menu_id, 3, 92, 0, '_self', 0, 1, 1, 'print:template:manage', 0, 0, '还须通过应用、来源和记录授权', 1, NOW(), 1, NOW(), 1, 'pc'
WHERE @print_application_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_resource WHERE tenant_id = 1 AND perms = 'print:template:manage' AND del_flag = 0);

INSERT INTO sys_resource (tenant_id, resource_name, parent_id, resource_type, sort, is_external, open_target, is_public, menu_status, visible, perms, keep_alive, always_show, remark, create_by, create_time, update_by, update_time, create_dept, client_code)
SELECT 1, '发布打印模板', @print_application_menu_id, 3, 93, 0, '_self', 0, 1, 1, 'print:template:publish', 0, 0, '还须通过应用、来源和记录授权', 1, NOW(), 1, NOW(), 1, 'pc'
WHERE @print_application_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_resource WHERE tenant_id = 1 AND perms = 'print:template:publish' AND del_flag = 0);

INSERT INTO sys_resource (tenant_id, resource_name, parent_id, resource_type, sort, is_external, open_target, is_public, menu_status, visible, perms, keep_alive, always_show, remark, create_by, create_time, update_by, update_time, create_dept, client_code)
SELECT 1, '使用打印', @print_application_menu_id, 3, 94, 0, '_self', 0, 1, 1, 'print:execute', 0, 0, '还须通过应用、来源和记录授权', 1, NOW(), 1, NOW(), 1, 'pc'
WHERE @print_application_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_resource WHERE tenant_id = 1 AND perms = 'print:execute' AND del_flag = 0);
