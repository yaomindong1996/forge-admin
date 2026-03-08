-- 水印模块字典数据初始化

-- 水印内容类型字典
INSERT INTO sys_dict_type (tenant_id, dict_name, dict_type, dict_status, remark) VALUES
(000000, '水印内容类型', 'watermark_content_type', 1, '水印内容显示类型，根据用户登录信息动态生成');

INSERT INTO sys_dict_data (tenant_id, dict_sort, dict_label, dict_value, dict_type, dict_status, remark) VALUES
(000000, 1, '姓名+手机号', 'name_phone', 'watermark_content_type', 1, '显示用户姓名和手机号'),
(000000, 2, '姓名', 'name', 'watermark_content_type', 1, '仅显示用户姓名'),
(000000, 3, '用户名', 'username', 'watermark_content_type', 1, '显示用户名'),
(000000, 4, '手机号', 'phone', 'watermark_content_type', 1, '仅显示手机号'),
(000000, 5, '邮箱', 'email', 'watermark_content_type', 1, '显示邮箱地址'),
(000000, 6, '用户ID', 'user_id', 'watermark_content_type', 1, '显示用户ID');
