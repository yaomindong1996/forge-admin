-- 将前端运行时业务枚举迁入系统字典；协议型 dict_value 与现有持久化值保持一致。

INSERT INTO sys_dict_type (
    tenant_id, dict_name, dict_type, dict_status, remark,
    create_by, create_time, update_by, update_time, create_dept
)
SELECT seed.tenant_id, seed.dict_name, seed.dict_type, 1, seed.remark,
       1, NOW(), 1, NOW(), 1
FROM (
    SELECT 1 tenant_id, '外部系统认证类型' dict_name, 'external_auth_type' dict_type,
           '外部系统调用认证协议' remark
    UNION ALL SELECT 1, 'API Key位置', 'external_api_key_position', '外部系统API Key注入位置'
    UNION ALL SELECT 1, '外部请求内容类型', 'external_request_content_type', '外部接口请求Content-Type'
    UNION ALL SELECT 1, '外部调用状态', 'external_call_status', '外部接口调用结果'
    UNION ALL SELECT 1, '外部调用类型', 'external_call_type', '区分调试和正式调用'
    UNION ALL SELECT 1, '生成器数据源用途', 'gen_datasource_usage_scope', '数据源允许运行时或开发导入的用途范围'
    UNION ALL SELECT 1, '生成器数据源风险等级', 'gen_datasource_risk_level', '数据源运行时写入风险等级'
    UNION ALL SELECT 1, '代码模板类型', 'gen_template_type', '代码生成模板产物类型'
    UNION ALL SELECT 1, '代码模板引擎', 'gen_template_engine', '代码生成模板渲染引擎'
    UNION ALL SELECT 1, '代码生成方式', 'gen_generation_type', '下载代码包或生成到项目'
    UNION ALL SELECT 1, '数据集结果编码', 'data_result_encoding', '数据集查询结果字符编码'
    UNION ALL SELECT 1, '数据集行权限属性', 'data_row_scope_attribute', '行权限与当前身份匹配的属性'
    UNION ALL SELECT 1, '数据集行权限逻辑', 'data_row_scope_logic', '多条行权限条件组合逻辑'
    UNION ALL SELECT 1, '数据集字段数据类型', 'data_field_data_type', '数据集参数和字段标准数据类型'
    UNION ALL SELECT 1, '数据集脱敏规则', 'data_mask_rule', '数据集字段脱敏正则预设'
    UNION ALL SELECT 1, '数据集日期格式', 'data_date_format', '数据集字段日期展示格式'
    UNION ALL SELECT 1, '数据集数据单位', 'data_unit', '数据集字段展示单位'
    UNION ALL SELECT 1, '客户端登录认证方式', 'sys_client_login_auth_type', '客户端允许使用的登录认证策略'
    UNION ALL SELECT 1, '客户端验证码覆盖', 'sys_client_captcha_type', '客户端覆盖全局验证码类型'
    UNION ALL SELECT 1, '对称加密算法', 'sys_crypto_algorithm', '系统配置中心支持的对称加密算法'
    UNION ALL SELECT 1, '同账号登录策略', 'sys_same_account_login_strategy', '同一账号重复登录时的处理策略'
    UNION ALL SELECT 1, '流程设计器类型', 'flow_designer_type', '审批流程或业务流程设计器'
    UNION ALL SELECT 1, '流程重复审批模式', 'flow_auto_approval_mode', '同一审批人重复出现时的自动审批策略'
    UNION ALL SELECT 1, '业务触发器接收规则', 'ai_business_trigger_receiver_rule', '触发器消息动作的接收人规则'
    UNION ALL SELECT 1, '业务触发器类型', 'ai_business_trigger_type', '事件触发或定时触发'
    UNION ALL SELECT 1, '业务触发器事件', 'ai_business_trigger_event_type', '业务记录和流程事件类型'
    UNION ALL SELECT 1, '业务触发器动作', 'ai_business_trigger_action_type', '触发器执行动作类型'
    UNION ALL SELECT 1, '业务触发器执行状态', 'ai_business_trigger_execute_status', '触发器执行日志状态'
    UNION ALL SELECT 1, '业务应用使用模式', 'ai_business_app_mode', '在线运行或下载代码模式'
    UNION ALL SELECT 1, '访问入口打开方式', 'ai_business_app_entry_mode', '业务应用入口打开方式'
    UNION ALL SELECT 1, '业务应用挂载位置', 'ai_business_app_mount_target', '管理端、移动端或接口资源'
    UNION ALL SELECT 1, '业务应用入口类型', 'ai_business_app_entry_type', '业务应用入口承载的页面或资源类型'
    UNION ALL SELECT 1, '业务应用移动场景', 'ai_business_app_mobile_scene', '移动应用入口场景'
    UNION ALL SELECT 1, '业务应用可见范围', 'ai_business_app_visible_scope', '移动应用入口可见范围'
    UNION ALL SELECT 1, '业务应用集成类型', 'ai_business_app_platform_type', '接口、Webhook、协同平台或外部系统'
    UNION ALL SELECT 1, '业务应用运行打开方式', 'ai_business_app_runtime_open_mode', '业务运行入口打开列表、填报或详情'
) seed
WHERE NOT EXISTS (
    SELECT 1
    FROM sys_dict_type data
    WHERE data.tenant_id = seed.tenant_id
      AND data.dict_type = seed.dict_type
);

INSERT INTO sys_dict_data (
    tenant_id, dict_sort, dict_label, dict_value, dict_type,
    css_class, list_class, is_default, dict_status, remark,
    create_by, create_time, update_by, update_time, create_dept
)
SELECT seed.tenant_id, seed.dict_sort, seed.dict_label, seed.dict_value, seed.dict_type,
       NULL, seed.list_class, seed.is_default, 1, seed.remark,
       1, NOW(), 1, NOW(), 1
FROM (
    SELECT 1 tenant_id, 1 dict_sort, '无认证' dict_label, 'none' dict_value,
           'external_auth_type' dict_type, 'default' list_class, 'Y' is_default, '不附加认证信息' remark
    UNION ALL SELECT 1, 2, 'Basic', 'basic', 'external_auth_type', 'info', 'N', 'HTTP Basic认证'
    UNION ALL SELECT 1, 3, 'Token', 'token', 'external_auth_type', 'warning', 'N', '固定Token认证'
    UNION ALL SELECT 1, 4, '当前用户Token透传', 'current_token', 'external_auth_type', 'info', 'N', '透传当前登录用户Token'
    UNION ALL SELECT 1, 5, 'OAuth2', 'oauth2', 'external_auth_type', 'success', 'N', 'OAuth2认证'
    UNION ALL SELECT 1, 6, 'API Key', 'api_key', 'external_auth_type', 'warning', 'N', 'API Key认证'
    UNION ALL SELECT 1, 7, '自定义认证', 'custom', 'external_auth_type', 'default', 'N', '使用自定义认证适配器'

    UNION ALL SELECT 1, 1, 'Header', 'header', 'external_api_key_position', 'info', 'Y', '写入HTTP Header'
    UNION ALL SELECT 1, 2, 'Query', 'query', 'external_api_key_position', 'warning', 'N', '写入URL Query'
    UNION ALL SELECT 1, 3, 'Body', 'body', 'external_api_key_position', 'default', 'N', '写入请求体'

    UNION ALL SELECT 1, 1, 'application/json', 'application/json', 'external_request_content_type', 'info', 'Y', 'JSON请求体'
    UNION ALL SELECT 1, 2, 'application/x-www-form-urlencoded', 'application/x-www-form-urlencoded', 'external_request_content_type', 'warning', 'N', '表单编码请求体'
    UNION ALL SELECT 1, 3, 'text/plain', 'text/plain', 'external_request_content_type', 'default', 'N', '纯文本请求体'

    UNION ALL SELECT 1, 1, '成功', '1', 'external_call_status', 'success', 'Y', '调用成功'
    UNION ALL SELECT 1, 2, '失败', '0', 'external_call_status', 'error', 'N', '调用失败'
    UNION ALL SELECT 1, 1, '调试', 'true', 'external_call_type', 'warning', 'N', '调试调用'
    UNION ALL SELECT 1, 2, '正式调用', 'false', 'external_call_type', 'info', 'Y', '正式业务调用'

    UNION ALL SELECT 1, 1, '低代码运行', 'LOWCODE_RUNTIME', 'gen_datasource_usage_scope', 'success', 'N', '仅供低代码运行时访问'
    UNION ALL SELECT 1, 2, '租户业务', 'TENANT_BUSINESS', 'gen_datasource_usage_scope', 'info', 'N', '供租户业务数据访问'
    UNION ALL SELECT 1, 3, '开发导入', 'DEVELOPER_IMPORT', 'gen_datasource_usage_scope', 'warning', 'N', '仅供开发期导入表结构'
    UNION ALL SELECT 1, 4, '通用', 'BOTH', 'gen_datasource_usage_scope', 'default', 'Y', '兼容运行时和开发导入'
    UNION ALL SELECT 1, 1, '低', 'LOW', 'gen_datasource_risk_level', 'success', 'Y', '低风险数据源'
    UNION ALL SELECT 1, 2, '中', 'MEDIUM', 'gen_datasource_risk_level', 'warning', 'N', '中风险数据源'
    UNION ALL SELECT 1, 3, '高', 'HIGH', 'gen_datasource_risk_level', 'error', 'N', '高风险数据源'

    UNION ALL SELECT 1, 1, 'Entity实体', 'ENTITY', 'gen_template_type', 'info', 'Y', 'Java实体模板'
    UNION ALL SELECT 1, 2, 'Mapper接口', 'MAPPER', 'gen_template_type', 'info', 'N', 'MyBatis Mapper接口模板'
    UNION ALL SELECT 1, 3, 'Mapper XML', 'MAPPER_XML', 'gen_template_type', 'info', 'N', 'MyBatis Mapper XML模板'
    UNION ALL SELECT 1, 4, 'Service接口', 'SERVICE', 'gen_template_type', 'success', 'N', 'Service接口模板'
    UNION ALL SELECT 1, 5, 'Service实现', 'SERVICE_IMPL', 'gen_template_type', 'success', 'N', 'Service实现模板'
    UNION ALL SELECT 1, 6, 'Controller', 'CONTROLLER', 'gen_template_type', 'warning', 'N', 'Controller模板'
    UNION ALL SELECT 1, 7, 'DTO', 'DTO', 'gen_template_type', 'default', 'N', '请求DTO模板'
    UNION ALL SELECT 1, 8, 'VO', 'VO', 'gen_template_type', 'default', 'N', '响应VO模板'
    UNION ALL SELECT 1, 9, 'Query查询', 'QUERY', 'gen_template_type', 'default', 'N', '查询对象模板'
    UNION ALL SELECT 1, 10, 'SQL脚本', 'SQL', 'gen_template_type', 'error', 'N', '数据库脚本模板'
    UNION ALL SELECT 1, 1, 'Velocity', 'VELOCITY', 'gen_template_engine', 'info', 'Y', 'Velocity模板引擎'
    UNION ALL SELECT 1, 2, 'Freemarker', 'FREEMARKER', 'gen_template_engine', 'success', 'N', 'Freemarker模板引擎'
    UNION ALL SELECT 1, 1, '下载代码包', 'DOWNLOAD', 'gen_generation_type', 'info', 'Y', '生成ZIP代码包'
    UNION ALL SELECT 1, 2, '生成到项目', 'PROJECT', 'gen_generation_type', 'warning', 'N', '直接写入项目目录'

    UNION ALL SELECT 1, 1, 'UTF-8', 'UTF-8', 'data_result_encoding', 'success', 'Y', 'UTF-8字符编码'
    UNION ALL SELECT 1, 2, 'GBK', 'GBK', 'data_result_encoding', 'warning', 'N', 'GBK字符编码'
    UNION ALL SELECT 1, 1, '租户 ID', 'tenantColumn', 'data_row_scope_attribute', 'info', 'Y', '匹配当前登录租户'
    UNION ALL SELECT 1, 2, '组织 ID', 'orgColumn', 'data_row_scope_attribute', 'success', 'N', '匹配用户所属组织'
    UNION ALL SELECT 1, 3, '用户 ID', 'userColumn', 'data_row_scope_attribute', 'warning', 'N', '匹配当前登录用户'
    UNION ALL SELECT 1, 4, '行政区划', 'regionColumn', 'data_row_scope_attribute', 'default', 'N', '匹配地市或区县编码'
    UNION ALL SELECT 1, 1, 'AND', 'AND', 'data_row_scope_logic', 'info', 'Y', '所有条件同时满足'
    UNION ALL SELECT 1, 2, 'OR', 'OR', 'data_row_scope_logic', 'warning', 'N', '任一条件满足'
    UNION ALL SELECT 1, 1, '文本 STRING', 'STRING', 'data_field_data_type', 'default', 'Y', '文本值'
    UNION ALL SELECT 1, 2, '数值 NUMBER', 'NUMBER', 'data_field_data_type', 'info', 'N', '数值'
    UNION ALL SELECT 1, 3, '日期 DATE', 'DATE', 'data_field_data_type', 'success', 'N', '日期'
    UNION ALL SELECT 1, 4, '日期时间 DATETIME', 'DATETIME', 'data_field_data_type', 'success', 'N', '日期时间'
    UNION ALL SELECT 1, 5, '布尔 BOOLEAN', 'BOOLEAN', 'data_field_data_type', 'warning', 'N', '布尔值'
    UNION ALL SELECT 1, 1, '默认：保留前2后2', '__DEFAULT__', 'data_mask_rule', 'default', 'Y', '系统默认脱敏规则'
    UNION ALL SELECT 1, 2, '手机号：隐藏中间4位',
                     CONCAT('(?<=', CHAR(92), 'd{3})', CHAR(92), 'd{4}(?=', CHAR(92), 'd{4})'),
                     'data_mask_rule', 'warning', 'N', '手机号脱敏正则'
    UNION ALL SELECT 1, 3, '身份证：隐藏出生日期',
                     CONCAT('(?<=', CHAR(92), 'd{6})', CHAR(92), 'd{8}(?=', CHAR(92), 'd{4})'),
                     'data_mask_rule', 'warning', 'N', '身份证脱敏正则'
    UNION ALL SELECT 1, 4, '银行卡：保留前4后4',
                     CONCAT('(?<=', CHAR(92), 'd{4})', CHAR(92), 'd+(?=', CHAR(92), 'd{4})'),
                     'data_mask_rule', 'warning', 'N', '银行卡脱敏正则'
    UNION ALL SELECT 1, 1, 'yyyy-MM-dd', 'yyyy-MM-dd', 'data_date_format', 'default', 'Y', '标准日期格式'
    UNION ALL SELECT 1, 2, 'yyyy-MM-dd HH:mm:ss', 'yyyy-MM-dd HH:mm:ss', 'data_date_format', 'default', 'N', '标准日期时间格式'
    UNION ALL SELECT 1, 3, 'yyyy/MM/dd', 'yyyy/MM/dd', 'data_date_format', 'default', 'N', '斜线日期格式'
    UNION ALL SELECT 1, 4, 'yyyy年MM月dd日', 'yyyy年MM月dd日', 'data_date_format', 'default', 'N', '中文日期格式'
    UNION ALL SELECT 1, 1, '元', '元', 'data_unit', 'default', 'Y', '金额单位元'
    UNION ALL SELECT 1, 2, '万元', '万元', 'data_unit', 'default', 'N', '金额单位万元'
    UNION ALL SELECT 1, 3, '%', '%', 'data_unit', 'default', 'N', '百分比'
    UNION ALL SELECT 1, 4, '人', '人', 'data_unit', 'default', 'N', '人数'
    UNION ALL SELECT 1, 5, '次', '次', 'data_unit', 'default', 'N', '次数'
    UNION ALL SELECT 1, 6, '件', '件', 'data_unit', 'default', 'N', '件数'
    UNION ALL SELECT 1, 7, '天', '天', 'data_unit', 'default', 'N', '天数'

    UNION ALL SELECT 1, 1, '用户名密码', 'password', 'sys_client_login_auth_type', 'info', 'Y', '用户名密码登录'
    UNION ALL SELECT 1, 2, '用户名密码+验证码', 'password_captcha', 'sys_client_login_auth_type', 'success', 'N', '用户名密码并校验验证码'
    UNION ALL SELECT 1, 3, '手机验证码', 'phone_captcha', 'sys_client_login_auth_type', 'warning', 'N', '手机短信验证码登录'
    UNION ALL SELECT 1, 4, '微信登录', 'wechat', 'sys_client_login_auth_type', 'success', 'N', '微信登录'
    UNION ALL SELECT 1, 5, '邮箱验证码', 'email_captcha', 'sys_client_login_auth_type', 'default', 'N', '邮箱验证码登录'
    UNION ALL SELECT 1, 1, '继承全局配置', '', 'sys_client_captcha_type', 'default', 'Y', '不覆盖全局验证码策略'
    UNION ALL SELECT 1, 2, '图形验证码', 'graphical', 'sys_client_captcha_type', 'success', 'N', '图形字符验证码'
    UNION ALL SELECT 1, 3, '滑块验证码', 'slider', 'sys_client_captcha_type', 'info', 'N', '滑块验证码'
    UNION ALL SELECT 1, 4, '短信验证码', 'sms', 'sys_client_captcha_type', 'warning', 'N', '短信验证码'

    UNION ALL SELECT 1, 1, 'SM4', 'SM4', 'sys_crypto_algorithm', 'success', 'Y', 'SM4对称加密'
    UNION ALL SELECT 1, 2, 'AES', 'AES', 'sys_crypto_algorithm', 'info', 'N', 'AES对称加密'
    UNION ALL SELECT 1, 1, '允许并发登录', 'allow_concurrent', 'sys_same_account_login_strategy', 'success', 'Y', '允许同账号同时在线'
    UNION ALL SELECT 1, 2, '新登录踢出旧登录', 'replace_old', 'sys_same_account_login_strategy', 'warning', 'N', '新会话替换旧会话'
    UNION ALL SELECT 1, 3, '拒绝新登录', 'reject_new', 'sys_same_account_login_strategy', 'error', 'N', '已有会话时拒绝新登录'

    UNION ALL SELECT 1, 1, '审批流程', 'approval', 'flow_designer_type', 'info', 'Y', '标准审批流程'
    UNION ALL SELECT 1, 2, '业务流程', 'business', 'flow_designer_type', 'success', 'N', '绑定业务对象的流程'
    UNION ALL SELECT 1, 1, '仅首个节点需审批，后续审批节点自动同意', 'firstOnly', 'flow_auto_approval_mode', 'info', 'N', '同一审批人完成一次后自动同意后续节点'
    UNION ALL SELECT 1, 2, '仅连续审批时自动同意', 'consecutive', 'flow_auto_approval_mode', 'warning', 'N', '仅相邻节点审批人相同时自动同意'
    UNION ALL SELECT 1, 3, '每个节点都需要审批', 'none', 'flow_auto_approval_mode', 'default', 'Y', '所有审批节点均人工处理'

    UNION ALL SELECT 1, 1, '记录创建人', 'CREATOR', 'ai_business_trigger_receiver_rule', 'info', 'Y', '发送给记录创建人'
    UNION ALL SELECT 1, 2, '记录负责人', 'OWNER', 'ai_business_trigger_receiver_rule', 'success', 'N', '发送给记录负责人'
    UNION ALL SELECT 1, 3, '指定用户', 'USERS', 'ai_business_trigger_receiver_rule', 'warning', 'N', '发送给动作配置中的用户'
    UNION ALL SELECT 1, 4, '全部用户', 'ALL', 'ai_business_trigger_receiver_rule', 'error', 'N', '发送给全部用户'
    UNION ALL SELECT 1, 1, '事件触发', 'EVENT', 'ai_business_trigger_type', 'info', 'Y', '业务事件触发'
    UNION ALL SELECT 1, 2, '定时触发', 'SCHEDULE', 'ai_business_trigger_type', 'warning', 'N', '按定时扫描触发'
    UNION ALL SELECT 1, 1, '记录创建', 'RECORD_CREATED', 'ai_business_trigger_event_type', 'success', 'Y', '记录创建事件'
    UNION ALL SELECT 1, 2, '记录更新', 'RECORD_UPDATED', 'ai_business_trigger_event_type', 'info', 'N', '记录更新事件'
    UNION ALL SELECT 1, 3, '记录删除', 'RECORD_DELETED', 'ai_business_trigger_event_type', 'error', 'N', '记录删除事件'
    UNION ALL SELECT 1, 4, '状态变更', 'STATUS_CHANGED', 'ai_business_trigger_event_type', 'warning', 'N', '状态字段变更事件'
    UNION ALL SELECT 1, 5, '字段变更', 'FIELD_CHANGED', 'ai_business_trigger_event_type', 'default', 'N', '指定字段变更事件'
    UNION ALL SELECT 1, 6, '流程通过', 'FLOW_APPROVED', 'ai_business_trigger_event_type', 'success', 'N', '流程审批通过事件'
    UNION ALL SELECT 1, 7, '流程驳回', 'FLOW_REJECTED', 'ai_business_trigger_event_type', 'error', 'N', '流程审批驳回事件'
    UNION ALL SELECT 1, 8, '到期提醒', 'SCHEDULED_DUE', 'ai_business_trigger_event_type', 'warning', 'N', '到期扫描事件'
    UNION ALL SELECT 1, 9, '流程取消', 'FLOW_CANCELED', 'ai_business_trigger_event_type', 'default', 'N', '流程被取消事件'
    UNION ALL SELECT 1, 1, '执行对象动作', 'BUSINESS_ACTION', 'ai_business_trigger_action_type', 'info', 'Y', '调用业务对象动作'
    UNION ALL SELECT 1, 2, '发起主流程', 'START_FLOW', 'ai_business_trigger_action_type', 'success', 'N', '发起绑定主流程'
    UNION ALL SELECT 1, 3, '发送消息', 'SEND_MESSAGE', 'ai_business_trigger_action_type', 'warning', 'N', '发送业务消息'
    UNION ALL SELECT 1, 4, '创建记录', 'CREATE_RECORD', 'ai_business_trigger_action_type', 'success', 'N', '创建目标业务记录'
    UNION ALL SELECT 1, 5, '更新字段', 'UPDATE_FIELD', 'ai_business_trigger_action_type', 'info', 'N', '更新当前记录字段'
    UNION ALL SELECT 1, 6, 'Webhook', 'WEBHOOK', 'ai_business_trigger_action_type', 'default', 'N', '调用Webhook'
    UNION ALL SELECT 1, 1, '成功', 'SUCCESS', 'ai_business_trigger_execute_status', 'success', 'N', '执行成功'
    UNION ALL SELECT 1, 2, '失败', 'FAILED', 'ai_business_trigger_execute_status', 'error', 'N', '执行失败'
    UNION ALL SELECT 1, 3, '待执行', 'TODO', 'ai_business_trigger_execute_status', 'warning', 'Y', '等待执行'
    UNION ALL SELECT 1, 4, '已跳过', 'SKIPPED', 'ai_business_trigger_execute_status', 'default', 'N', '条件不满足时跳过执行'

    UNION ALL SELECT 1, 1, '在线运行', 'DYNAMIC_RENDER', 'ai_business_app_mode', 'success', 'Y', '在线搭建并由平台动态运行'
    UNION ALL SELECT 1, 2, '下载代码', 'CODE_DOWNLOAD', 'ai_business_app_mode', 'info', 'N', '下载完整功能代码后导入本地工程二次开发'
    UNION ALL SELECT 1, 1, '业务页面', 'RUNTIME', 'ai_business_app_entry_mode', 'success', 'Y', '打开平台托管的业务页面'
    UNION ALL SELECT 1, 2, '内部路由', 'ROUTE', 'ai_business_app_entry_mode', 'info', 'N', '打开平台内部路由'
    UNION ALL SELECT 1, 3, '内嵌页面', 'IFRAME', 'ai_business_app_entry_mode', 'primary', 'N', '通过iframe内嵌访问'
    UNION ALL SELECT 1, 4, '外部打开', 'EXTERNAL', 'ai_business_app_entry_mode', 'warning', 'N', '新窗口打开外部地址'
    UNION ALL SELECT 1, 5, 'H5 入口', 'H5', 'ai_business_app_entry_mode', 'default', 'N', '移动H5入口'
    UNION ALL SELECT 1, 6, '接口入口', 'API', 'ai_business_app_entry_mode', 'info', 'N', '登记接口或集成资源'
    UNION ALL SELECT 1, 1, '管理端菜单', 'ADMIN', 'ai_business_app_mount_target', 'info', 'Y', '在PC管理端生成入口菜单'
    UNION ALL SELECT 1, 2, '移动应用', 'MOBILE', 'ai_business_app_mount_target', 'success', 'N', '登记H5或移动端业务入口'
    UNION ALL SELECT 1, 3, '外部接口', 'API', 'ai_business_app_mount_target', 'warning', 'N', '登记API、Webhook或外部系统资源'
    UNION ALL SELECT 1, 1, '对象列表入口', 'OBJECT_LIST', 'ai_business_app_entry_type', 'info', 'Y', '业务对象列表入口'
    UNION ALL SELECT 1, 2, '新增表单入口', 'CREATE_FORM', 'ai_business_app_entry_type', 'success', 'N', '直接打开新增表单'
    UNION ALL SELECT 1, 3, '详情页入口', 'DETAIL_PAGE', 'ai_business_app_entry_type', 'default', 'N', '业务记录详情入口'
    UNION ALL SELECT 1, 4, '审批/待办入口', 'APPROVAL_TODO', 'ai_business_app_entry_type', 'warning', 'N', '审批或待办入口'
    UNION ALL SELECT 1, 5, '报表/看板入口', 'REPORT_DASHBOARD', 'ai_business_app_entry_type', 'success', 'N', '报表或看板入口'
    UNION ALL SELECT 1, 6, '外链/API入口', 'EXTERNAL_OR_API', 'ai_business_app_entry_type', 'default', 'N', '外部链接或API资源入口'
    UNION ALL SELECT 1, 1, 'H5入口', 'h5', 'ai_business_app_mobile_scene', 'info', 'Y', '通用H5入口'
    UNION ALL SELECT 1, 2, '移动待办', 'todo', 'ai_business_app_mobile_scene', 'warning', 'N', '移动端待办入口'
    UNION ALL SELECT 1, 3, '移动流程待办', 'approval', 'ai_business_app_mobile_scene', 'success', 'N', '移动端流程审批入口'
    UNION ALL SELECT 1, 4, '移动业务', 'business', 'ai_business_app_mobile_scene', 'default', 'N', '移动端业务入口'
    UNION ALL SELECT 1, 1, '全部用户', 'all', 'ai_business_app_visible_scope', 'info', 'Y', '对全部用户可见'
    UNION ALL SELECT 1, 2, '指定角色', 'role', 'ai_business_app_visible_scope', 'success', 'N', '对指定角色可见'
    UNION ALL SELECT 1, 3, '指定部门', 'dept', 'ai_business_app_visible_scope', 'warning', 'N', '对指定部门可见'
    UNION ALL SELECT 1, 4, '负责人范围', 'owner', 'ai_business_app_visible_scope', 'default', 'N', '按业务负责人范围可见'
    UNION ALL SELECT 1, 1, '标准接口', 'api', 'ai_business_app_platform_type', 'info', 'Y', '标准开放接口'
    UNION ALL SELECT 1, 2, 'Webhook', 'webhook', 'ai_business_app_platform_type', 'warning', 'N', 'Webhook集成'
    UNION ALL SELECT 1, 3, '企微/飞书/钉钉', 'collaboration', 'ai_business_app_platform_type', 'success', 'N', '协同办公平台集成'
    UNION ALL SELECT 1, 4, '外部系统', 'external', 'ai_business_app_platform_type', 'default', 'N', '其它外部系统集成'
    UNION ALL SELECT 1, 1, '列表管理', 'LIST', 'ai_business_app_runtime_open_mode', 'info', 'Y', '显示列表、搜索和操作列'
    UNION ALL SELECT 1, 2, '单据填报', 'CREATE_FORM', 'ai_business_app_runtime_open_mode', 'success', 'N', '直接显示新增表单'
    UNION ALL SELECT 1, 3, '详情查看', 'DETAIL', 'ai_business_app_runtime_open_mode', 'default', 'N', '按记录ID显示详情'

    UNION ALL SELECT 1, 6, 'PATCH', 'PATCH', 'sys_req_method', 'warning', 'N', 'PATCH请求'
    UNION ALL SELECT 1, 4, '业务应用表单', 'business', 'flow_process_form_type', 'success', 'N', '绑定业务应用表单'
) seed
WHERE NOT EXISTS (
    SELECT 1
    FROM sys_dict_data data
    WHERE data.tenant_id = seed.tenant_id
      AND data.dict_type = seed.dict_type
      AND data.dict_value = seed.dict_value
);
