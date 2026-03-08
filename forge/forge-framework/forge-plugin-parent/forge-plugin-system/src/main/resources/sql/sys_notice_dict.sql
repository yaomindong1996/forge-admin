-- 通知公告模块字典数据初始化

-- 公告类型字典
INSERT INTO sys_dict_type (tenant_id, dict_name, dict_type, dict_status, remark) VALUES
(000000, '公告类型', 'sys_notice_type', 1, '通知公告类型字典');

INSERT INTO sys_dict_data (tenant_id, dict_sort, dict_label, dict_value, dict_type, dict_status, remark) VALUES
(000000, 1, '通知公告', 'NOTICE', 'sys_notice_type', 1, '普通通知公告'),
(000000, 2, '系统公告', 'ANNOUNCEMENT', 'sys_notice_type', 1, '系统级公告'),
(000000, 3, '新闻动态', 'NEWS', 'sys_notice_type', 1, '新闻动态信息');

-- 公告发布状态字典
INSERT INTO sys_dict_type (tenant_id, dict_name, dict_type, dict_status, remark) VALUES
(000000, '公告发布状态', 'sys_notice_status', 1, '通知公告发布状态字典');

INSERT INTO sys_dict_data (tenant_id, dict_sort, dict_label, dict_value, dict_type, dict_status, remark) VALUES
(000000, 1, '草稿', '0', 'sys_notice_status', 1, '草稿状态'),
(000000, 2, '已发布', '1', 'sys_notice_status', 1, '已发布状态'),
(000000, 3, '已撤回', '2', 'sys_notice_status', 1, '已撤回状态');
