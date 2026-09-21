-- 打印预览「PDF」下载是客户端输出事件，不是物理出纸。
INSERT INTO sys_dict_data (tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, dict_status, remark, create_by, create_time, update_by, update_time, create_dept)
SELECT 1, 4, '已下载 PDF', 'PDF_DOWNLOADED', 'sys_print_execution_result', NULL, 'info', 'N', 1, 'Forge 原生打印', 1, NOW(), 1, NOW(), 1
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE tenant_id = 1 AND dict_type = 'sys_print_execution_result' AND dict_value = 'PDF_DOWNLOADED');
